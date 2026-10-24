package com.example.porjectpetra.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.porjectpetra.MainActivity
import com.example.porjectpetra.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Séance visible hors de PETRA : notification permanente (toujours) et widget flottant au-dessus des
 * autres applications (si l'autorisation « Afficher par-dessus d'autres applis » est accordée).
 * Le widget n'apparaît que lorsque PETRA est en arrière-plan ; il permet de valider la série, d'ajouter
 * du repos ou de le passer, sans revenir dans l'application. La séance elle-même reste gérée par l'écran
 * de séance (ActiveWorkoutViewModel) : ce service ne fait qu'afficher [LiveWorkout.state] et envoyer des commandes.
 */
class WorkoutOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var overlay: LinearLayout? = null
    private lateinit var titleView: TextView
    private lateinit var mainView: TextView
    private lateinit var subView: TextView
    private lateinit var progressView: TextView
    private lateinit var primaryBtn: TextView
    private lateinit var secondaryBtn: TextView

    private var current: LiveWorkoutState? = null
    private var seenSession = false
    private var dismissedByUser = false
    private var lastRestCompleted = -1
    private var lastNotificationKey: String? = null
    private var tone: ToneGenerator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }
        startAsForeground(buildNotification(LiveWorkout.state.value))

        scope.launch {
            combine(LiveWorkout.state, LiveWorkout.appInForeground) { s, fg -> s to fg }.collect { (state, foreground) ->
                onState(state, foreground)
            }
        }
        // Filet de sécurité : si aucune séance n'est publiée peu après le démarrage, on s'arrête.
        scope.launch {
            delay(5_000)
            if (!seenSession) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COMPLETE_SET -> LiveWorkout.send(LiveWorkoutCommand.COMPLETE_CURRENT_SET)
            ACTION_SKIP_REST -> LiveWorkout.send(LiveWorkoutCommand.SKIP_REST)
            ACTION_ADD_REST -> LiveWorkout.send(LiveWorkoutCommand.ADD_REST_15S)
            ACTION_SHOW_WIDGET -> {
                dismissedByUser = false
                onState(current, LiveWorkout.appInForeground.value)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        tone?.release()
        tone = null
        super.onDestroy()
    }

    private fun onState(state: LiveWorkoutState?, appInForeground: Boolean) {
        if (state == null) {
            // Séance enregistrée ou abandonnée
            if (seenSession) stopSelf()
            return
        }
        seenSession = true
        current = state

        if (lastRestCompleted >= 0 && state.restCompletedCount > lastRestCompleted) beepAndVibrate()
        lastRestCompleted = state.restCompletedCount

        // De retour dans PETRA : le widget disparaît, et il pourra revenir à la prochaine sortie.
        if (appInForeground) dismissedByUser = false
        val showOverlay = !appInForeground && !dismissedByUser && Settings.canDrawOverlays(this)
        if (showOverlay) {
            if (overlay == null) showOverlay()
            render(state)
        } else {
            removeOverlay()
        }
        updateNotification(state)
    }

    // ---------- Widget flottant ----------

    private fun render(s: LiveWorkoutState) {
        titleView.text = s.title
        progressView.text = "${s.doneCount}/${s.totalCount} · ${s.elapsed}"
        when {
            s.isResting -> {
                mainView.text = formatClock(s.restRemainingSeconds)
                mainView.textSize = 40f
                subView.text = if (s.hasCurrentSet) "Repos · ensuite ${s.exerciseName} · ${s.setLabel} · ${s.setDetail}" else "Repos"
                primaryBtn.text = "+15 s"
                secondaryBtn.text = "Passer"
                secondaryBtn.visibility = View.VISIBLE
            }
            s.hasCurrentSet -> {
                mainView.text = s.setDetail.ifEmpty { s.setLabel }
                mainView.textSize = 24f
                subView.text = "${s.exerciseName} · ${s.setLabel}"
                primaryBtn.text = "✓  Série faite"
                secondaryBtn.visibility = View.GONE
            }
            s.isFinished -> {
                mainView.text = "Séance terminée"
                mainView.textSize = 22f
                subView.text = "Ouvre PETRA pour l'enregistrer."
                primaryBtn.text = "Ouvrir PETRA"
                secondaryBtn.visibility = View.GONE
            }
            else -> {
                mainView.text = "Séance en cours"
                mainView.textSize = 22f
                subView.text = "Ajoute un exercice dans PETRA."
                primaryBtn.text = "Ouvrir PETRA"
                secondaryBtn.visibility = View.GONE
            }
        }
    }

    private fun onPrimaryClick() {
        val s = current ?: return
        when {
            s.isResting -> LiveWorkout.send(LiveWorkoutCommand.ADD_REST_15S)
            s.hasCurrentSet -> LiveWorkout.send(LiveWorkoutCommand.COMPLETE_CURRENT_SET)
            else -> openApp()
        }
    }

    private fun buildOverlay(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumWidth = dp(240)
            setPadding(dp(14), dp(10), dp(14), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(COLOR_SURFACE)
                setStroke(dp(1), COLOR_BORDER)
            }
            elevation = dp(8).toFloat()
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.petra_emblem)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
            setOnClickListener { openApp() }
        }
        titleView = TextView(this).apply {
            setTextColor(COLOR_KHAKI)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { openApp() }
        }
        progressView = TextView(this).apply {
            setTextColor(COLOR_MUTED)
            textSize = 12f
            setPadding(dp(8), 0, 0, 0)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(COLOR_MUTED)
            textSize = 16f
            setPadding(dp(12), dp(2), dp(2), dp(2))
            contentDescription = "Masquer le widget"
            setOnClickListener {
                // Masque seulement le widget : la séance continue, la notification reste.
                dismissedByUser = true
                removeOverlay()
            }
        }
        topRow.addView(logo)
        topRow.addView(titleView)
        topRow.addView(progressView)
        topRow.addView(close)

        mainView = TextView(this).apply {
            setTextColor(COLOR_TEXT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        subView = TextView(this).apply {
            setTextColor(COLOR_MUTED)
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        primaryBtn = pill(primary = true).apply { setOnClickListener { onPrimaryClick() } }
        secondaryBtn = pill(primary = false).apply {
            setOnClickListener { LiveWorkout.send(LiveWorkoutCommand.SKIP_REST) }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        buttons.addView(primaryBtn)
        buttons.addView(
            secondaryBtn,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(10) }
        )

        root.addView(topRow)
        root.addView(mainView)
        root.addView(subView)
        root.addView(buttons)
        enableDrag(root)
        return root
    }

    private fun pill(primary: Boolean) = TextView(this).apply {
        setTextColor(COLOR_TEXT)
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(10), dp(18), dp(10))
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            if (primary) setColor(COLOR_OLIVE) else {
                setColor(COLOR_SURFACE_ALT)
                setStroke(dp(1), COLOR_TEXT)
            }
        }
        isClickable = true
    }

    private fun enableDrag(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = e.rawX; touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - touchX).toInt()
                    params.y = startY + (e.rawY - touchY).toInt()
                    overlay?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun showOverlay() {
        val view = buildOverlay()
        try {
            windowManager.addView(view, params)
            overlay = view
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun removeOverlay() {
        overlay?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        overlay = null
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    // ---------- Notification ----------

    private fun updateNotification(s: LiveWorkoutState) {
        // Le compte à rebours de la notification avance seul (chronomètre) : on ne la met à jour
        // que lorsque son contenu change vraiment.
        val key = listOf(s.exerciseName, s.setLabel, s.setDetail, s.isResting, s.restTotalSeconds, s.doneCount, s.isFinished,
            if (s.isResting) s.restRemainingSeconds / 15 else 0).joinToString("|")
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(s))
    }

    private fun buildNotification(s: LiveWorkoutState?): android.app.Notification {
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (s == null) {
            return builder.setContentTitle("Séance en cours").build()
        }
        builder.setContentTitle(s.title).setProgress(s.totalCount.coerceAtLeast(1), s.doneCount, false)
        when {
            s.isResting -> {
                builder.setContentText("Repos · ensuite ${s.exerciseName} — ${s.setLabel} ${s.setDetail}".trim())
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(System.currentTimeMillis() + s.restRemainingSeconds * 1000L)
                    .addAction(0, "+15 s", serviceIntent(ACTION_ADD_REST, 2))
                    .addAction(0, "Passer", serviceIntent(ACTION_SKIP_REST, 3))
            }
            s.hasCurrentSet -> {
                builder.setContentText("${s.exerciseName} — ${s.setLabel} · ${s.setDetail}")
                    .setShowWhen(false)
                    .addAction(0, "Série faite", serviceIntent(ACTION_COMPLETE_SET, 1))
            }
            else -> builder.setContentText(if (s.isFinished) "Séance terminée : ouvre PETRA pour l'enregistrer." else "Séance en cours")
                .setShowWhen(false)
        }
        if (Settings.canDrawOverlays(this)) {
            builder.addAction(0, "Widget", serviceIntent(ACTION_SHOW_WIDGET, 4))
        }
        return builder.build()
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, WorkoutOverlayService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Séance en cours", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Suivi de la séance et du repos quand PETRA est en arrière-plan"
                }
            )
        }
    }

    private fun startAsForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    // ---------- Bip + vibration en fin de repos ----------

    private fun beepAndVibrate() {
        try {
            tone?.release()
            val t = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tone = t
            scope.launch {
                repeat(3) {
                    t.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    delay(350)
                }
            }
        } catch (_: RuntimeException) {
        }
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 300, 150, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun formatClock(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

    companion object {
        private const val ACTION_COMPLETE_SET = "com.example.porjectpetra.workout.COMPLETE_SET"
        private const val ACTION_SKIP_REST = "com.example.porjectpetra.workout.SKIP_REST"
        private const val ACTION_ADD_REST = "com.example.porjectpetra.workout.ADD_REST"
        private const val ACTION_SHOW_WIDGET = "com.example.porjectpetra.workout.SHOW_WIDGET"

        private const val CHANNEL_ID = "seance_en_cours"
        private const val NOTIF_ID = 4201

        // Palette PETRA
        private const val COLOR_SURFACE = 0xF2171A17.toInt()
        private const val COLOR_SURFACE_ALT = 0xFF20241F.toInt()
        private const val COLOR_BORDER = 0xFF34382F.toInt()
        private const val COLOR_OLIVE = 0xFF4A543A.toInt()
        private const val COLOR_KHAKI = 0xFFB7A98A.toInt()
        private const val COLOR_TEXT = 0xFFE8E7DE.toInt()
        private const val COLOR_MUTED = 0xFFA7A99D.toInt()

        /** À appeler quand la séance démarre (PETRA au premier plan). */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutOverlayService::class.java))
        }

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        fun overlaySettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
    }
}
