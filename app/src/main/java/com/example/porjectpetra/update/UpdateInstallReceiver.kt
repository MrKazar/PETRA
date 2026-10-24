package com.example.porjectpetra.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.example.porjectpetra.FitnessApplication

/** Résultat de l'installation d'une mise à jour, renvoyé par l'installeur Android. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val manager = (context.applicationContext as FitnessApplication).updateManager
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Android demande la confirmation de l'utilisateur : on affiche son écran.
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // En cas de succès, Android remplace PETRA par la nouvelle version (l'app redémarre).
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> manager.onInstallFailed("Mise à jour annulée.")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                manager.onInstallFailed(
                    "Installation refusée : la nouvelle version n'est pas signée avec la même clé que celle installée."
                )
            PackageInstaller.STATUS_FAILURE_STORAGE -> manager.onInstallFailed("Espace de stockage insuffisant.")
            else -> manager.onInstallFailed(
                "Échec de l'installation (code $status). " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "")
            )
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.example.porjectpetra.update.INSTALL_STATUS"
    }
}
