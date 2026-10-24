package com.example.porjectpetra.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * Minuteur de repos flottant (au-dessus des autres apps).
 *
 * Déroulé :
 *  SET  : affiche la série à faire + bouton « ✓ Série faite »
 *  REST : compte à rebours (+15 s / Passer). À la fin : bips + vibration
 *  puis on repasse en SET pour la série suivante (avec SON propre temps de repos), etc.
 *  DONE : plus de série -> « Terminé », fermeture automatique.
 */
class RestTimerService : Service() {

    private enum class Phase { SET, REST, DONE }

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    private var overlay: LinearLayout? = null
    private lateinit var titleView: TextView
    private lateinit var mainView: TextView
    private lateinit var subView: TextView
    private lateinit var primaryBtn: TextView
    private lateinit var secondaryBtn: TextView

    private var steps: List<RestStep> = emptyList()
    private var index = 0
    private var phase = Phase.SET
    private var renderedPhase: Phase? = null
    private var restEndAt = 0L
    private var singleRest = false
    private var tone: ToneGenerator? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (phase != Phase.REST) return
            if (restEndAt - SystemClock.elapsedRealtime() <= 0) {
                finishRest(withAlert = true)
            } else {
                render()
                handler.postDelayed(this, 250)
            }
        }
    }
    private val autoClose = Runnable { stopSelf() }

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
            x = dp(16)
            y = dp(140)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        when (intent.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                startAsForeground()
                val titles = intent.getStringArrayListExtra(EXTRA_TITLES).orEmpty()
                val details = intent.getStringArrayListExtra(EXTRA_DETAILS).orEmpty()
                val rests = intent.getIntArrayExtra(EXTRA_RESTS) ?: IntArray(0)
                steps = titles.indices.map { i ->
                    RestStep(titles[i], details.getOrElse(i) { "" }, rests.getOrElse(i) { 0 })
                }
                if (steps.isEmpty() || !Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val startRestNow = intent.getBooleanExtra(EXTRA_START_REST_NOW, false)
                singleRest = startRestNow && steps.size == 1

                handler.removeCallbacks(ticker)
                handler.removeCallbacks(autoClose)
                index = 0
                phase = Phase.SET
                showOverlay()
                if (startRestNow) onSetDone() else render()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        tone?.release()
        tone = null
        super.onDestroy()
    }

    // ---------- Logique ----------

    private fun onSetDone() {
        val rest = steps[index].restSeconds
        index++
        if (rest <= 0) {
            advance()
        } else {
            phase = Phase.REST
            restEndAt = SystemClock.elapsedRealtime() + rest * 1000L
            render()
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
    }

    private fun finishRest(withAlert: Boolean) {
        handler.removeCallbacks(ticker)
        if (withAlert) beepAndVibrate()
        advance()
    }

    private fun advance() {
        if (index >= steps.size) {
            phase = Phase.DONE
            render()
            handler.removeCallbacks(autoClose)
            handler.postDelayed(autoClose, 10_000)
        } else {
            phase = Phase.SET
            render()
        }
    }

    // ---------- Affichage ----------

    private fun render() {
        val root = overlay ?: return
        val bg = root.background as? GradientDrawable
        val phaseChanged = renderedPhase != phase
        renderedPhase = phase

        when (phase) {
            Phase.SET -> {
                val s = steps[index]
                bg?.setColor(COLOR_SET)
                titleView.text = s.title
                mainView.text = if (s.detail.isBlank()) "Prêt ?" else s.detail
                subView.text = "Série ${index + 1} / ${steps.size}"
                primaryBtn.text = "✓  Série faite"
                if (phaseChanged) {
                    mainView.textSize = 30f
                    secondaryBtn.visibility = View.GONE
                }
            }
            Phase.REST -> {
                val remainingMs = max(0L, restEndAt - SystemClock.elapsedRealtime())
                val secs = ceil(remainingMs / 1000.0).toInt()
                bg?.setColor(COLOR_REST)
                titleView.text = "Repos"
                mainView.text = String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60)
                subView.text = if (index < steps.size) {
                    val next = steps[index]
                    "Ensuite : ${next.title}" + if (next.detail.isNotBlank()) " · ${next.detail}" else ""
                } else ""
                if (phaseChanged) {
                    mainView.textSize = 46f
                    primaryBtn.text = "+15 s"
                    secondaryBtn.text = "Passer"
                    secondaryBtn.visibility = View.VISIBLE
                }
            }
            Phase.DONE -> {
                bg?.setColor(COLOR_DONE)
                titleView.text = if (singleRest) "Repos terminé" else "Séance terminée"
                mainView.text = if (singleRest) "Go !" else "Bravo 💪"
                subView.text = ""
                primaryBtn.text = "Fermer"
                if (phaseChanged) {
                    mainView.textSize = 34f
                    secondaryBtn.visibility = View.GONE
                }
            }
        }
    }

    private fun buildOverlay(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumWidth = dp(230)
            setPadding(dp(16), dp(10), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(COLOR_REST)
            }
            elevation = dp(8).toFloat()
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = TextView(this).apply {
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 13f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(14), dp(2), dp(2), dp(2))
            isClickable = true
            setOnClickListener { stopSelf() }
        }
        topRow.addView(titleView)
        topRow.addView(close)

        mainView = TextView(this).apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        subView = TextView(this).apply {
            setTextColor(0xB3FFFFFF.toInt())
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        primaryBtn = pill(primary = true).apply {
            setOnClickListener {
                when (phase) {
                    Phase.SET -> onSetDone()
                    Phase.REST -> { restEndAt += 15_000; render() }
                    Phase.DONE -> stopSelf()
                }
            }
        }
        secondaryBtn = pill(primary = false).apply {
            setOnClickListener { if (phase == Phase.REST) finishRest(withAlert = false) }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        buttons.addView(primaryBtn)
        buttons.addView(secondaryBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(10) })

        root.addView(topRow)
        root.addView(mainView)
        root.addView(subView)
        root.addView(buttons)
        enableDrag(root)
        return root
    }

    private fun pill(primary: Boolean) = TextView(this).apply {
        setTextColor(if (primary) 0xFF1B1B1B.toInt() else Color.WHITE)
        textSize = if (primary) 18f else 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(20), dp(12), dp(20), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(26).toFloat()
            setColor(if (primary) Color.WHITE else 0x33FFFFFF)
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
        removeOverlay()
        renderedPhase = null
        val view = buildOverlay()
        try {
            windowManager.addView(view, params)
            overlay = view
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun removeOverlay() {
        overlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlay = null
    }

    // ---------- Bip + vibration ----------

    private fun beepAndVibrate() {
        try {
            tone?.release()
            val t = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tone = t
            repeat(3) { i ->
                handler.postDelayed({ t.startTone(ToneGenerator.TONE_PROP_BEEP, 250) }, i * 400L)
            }
        } catch (_: RuntimeException) {
        }
        vibrate()
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 300, 150, 300, 150, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }

    // ---------- Service au premier plan ----------

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Minuteur de repos", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, RestTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Séance en cours")
            .setContentText("Minuteur de repos flottant actif")
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(0, "Arrêter", stop)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_START = "com.example.porjectpetra.timer.START"
        const val ACTION_STOP = "com.example.porjectpetra.timer.STOP"
        const val EXTRA_TITLES = "titles"
        const val EXTRA_DETAILS = "details"
        const val EXTRA_RESTS = "rests"
        const val EXTRA_START_REST_NOW = "start_rest_now"

        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIF_ID = 4201

        private const val COLOR_SET = 0xF01B5E20.toInt()
        private const val COLOR_REST = 0xF0212121.toInt()
        private const val COLOR_DONE = 0xF00D47A1.toInt()
    }
}
