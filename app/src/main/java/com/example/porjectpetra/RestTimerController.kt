package com.example.porjectpetra.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object RestTimerController {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /**
     * @param steps         séries de la séance (chacune avec son repos)
     * @param startRestNow  true = lance directement le repos de steps[0] (bouton « repos » simple)
     *                      false = affiche d'abord la 1re série et attend « Série faite »
     */
    fun start(context: Context, steps: List<RestStep>, startRestNow: Boolean) {
        if (steps.isEmpty()) return
        val intent = Intent(context, RestTimerService::class.java)
            .setAction(RestTimerService.ACTION_START)
            .putStringArrayListExtra(RestTimerService.EXTRA_TITLES, ArrayList(steps.map { it.title }))
            .putStringArrayListExtra(RestTimerService.EXTRA_DETAILS, ArrayList(steps.map { it.detail }))
            .putExtra(RestTimerService.EXTRA_RESTS, steps.map { it.restSeconds }.toIntArray())
            .putExtra(RestTimerService.EXTRA_START_REST_NOW, startRestNow)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, RestTimerService::class.java))
    }
}
