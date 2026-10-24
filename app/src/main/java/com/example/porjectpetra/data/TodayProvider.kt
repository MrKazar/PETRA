package com.example.porjectpetra.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.porjectpetra.data.model.PetraDates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Source unique du « jour courant ». Se met à jour :
 * - à minuit (minuterie),
 * - quand l'heure, la date ou le fuseau du téléphone change (diffusion système),
 * - à chaque retour au premier plan ([refresh]), car la minuterie peut dériver pendant la veille.
 */
class TodayProvider(context: Context, scope: CoroutineScope) {
    private val _today = MutableStateFlow(PetraDates.epochDay())
    val today: StateFlow<Long> = _today.asStateFlow()

    init {
        scope.launch {
            while (true) {
                delay(PetraDates.millisUntilNextDay() + 500L)
                refresh()
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context.applicationContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun refresh() {
        _today.value = PetraDates.epochDay()
    }
}
