package com.example.porjectpetra.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.porjectpetra.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

// Réponse de l'API GitHub « dernière release »
@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    val name: String,
    val size: Long = 0,
    @Json(name = "browser_download_url") val downloadUrl: String
)

data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    /** L'installeur Android a pris le relais (confirmation éventuelle à l'écran). */
    data class Installing(val info: UpdateInfo) : UpdateState
    /** PETRA doit être autorisée à installer des applications (réglage « Sources inconnues »). */
    data class NeedsInstallPermission(val info: UpdateInfo) : UpdateState
    data class Error(val message: String, val info: UpdateInfo? = null) : UpdateState
}

/**
 * Canal de mise à jour de PETRA : la dernière release GitHub de [BuildConfig.UPDATE_REPO] contient l'APK
 * signé de la version approuvée. Si son numéro est plus récent que la version installée, PETRA propose de
 * la télécharger puis la confie à l'installeur Android (confirmation demandée par Android si nécessaire).
 */
class UpdateManager(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val releaseAdapter = Moshi.Builder().build().adapter(GitHubRelease::class.java)
    private var checkedThisLaunch = false

    /** Vérification automatique : une fois par lancement de l'application. */
    fun checkOnLaunch() {
        if (checkedThisLaunch) return
        checkedThisLaunch = true
        check(silent = true)
    }

    /** [silent] : en cas d'erreur réseau, ne rien afficher (vérification automatique). */
    fun check(silent: Boolean = false) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        scope.launch {
            _state.value = UpdateState.Checking
            _state.value = try {
                val release = withContext(Dispatchers.IO) { fetchLatestRelease() }
                val apk = release?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                val remoteVersion = release?.tagName?.removePrefix("v")
                if (release == null || apk == null || remoteVersion == null || !AppVersion.isNewer(remoteVersion, currentVersion)) {
                    UpdateState.UpToDate
                } else {
                    UpdateState.Available(UpdateInfo(remoteVersion, release.body.orEmpty().trim(), apk.downloadUrl, apk.size))
                }
            } catch (e: Exception) {
                if (silent) UpdateState.Idle else UpdateState.Error("Impossible de vérifier les mises à jour (${e.message ?: "réseau"}).")
            }
        }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    /** Télécharge l'APK de [info] puis lance l'installation. */
    fun downloadAndInstall(info: UpdateInfo) {
        if (!canInstallPackages()) {
            _state.value = UpdateState.NeedsInstallPermission(info)
            return
        }
        scope.launch {
            try {
                _state.value = UpdateState.Downloading(info, 0f)
                val apk = withContext(Dispatchers.IO) {
                    download(info) { p -> _state.value = UpdateState.Downloading(info, p) }
                }
                _state.value = UpdateState.Installing(info)
                withContext(Dispatchers.IO) { install(apk) }
            } catch (e: Exception) {
                _state.value = UpdateState.Error("Échec du téléchargement (${e.message ?: "réseau"}).", info)
            }
        }
    }

    /** Appelé par [UpdateInstallReceiver] quand l'installation n'aboutit pas. */
    fun onInstallFailed(message: String) {
        val info = (state.value as? UpdateState.Installing)?.info
        _state.value = UpdateState.Error(message, info)
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Réglage Android « Installer des applis inconnues » pour PETRA. */
    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun fetchLatestRelease(): GitHubRelease? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "PETRA-Android/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null // aucune release publiée
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return releaseAdapter.fromJson(response.body.string())?.takeIf { !it.draft && !it.prerelease }
        }
    }

    private fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "PETRA-${info.versionName}.apk")
        val request = Request.Builder().url(info.apkUrl).header("User-Agent", "PETRA-Android").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 } ?: info.apkSize
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastReported = -1
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        }
        return target
    }

    private fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            // Android 12+ : pas de confirmation si PETRA est déjà l'installeur de sa version actuelle.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("petra.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(
                context, sessionId,
                Intent(context, UpdateInstallReceiver::class.java).setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS),
                flags
            )
            session.commit(callback.intentSender)
        }
    }
}

/** Comparaison de numéros de version « 1.10.2 » (chiffres seulement, segments manquants = 0). */
object AppVersion {
    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").split('.', '-', '+').map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
