package com.example.porjectpetra.workout

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ce que le widget flottant et la notification affichent de la séance en cours. */
data class LiveWorkoutState(
    val title: String,
    val elapsed: String,
    /** Exercice et série à faire maintenant (vides si tout est fait). */
    val exerciseName: String,
    val setLabel: String,
    val setDetail: String,
    val doneCount: Int,
    val totalCount: Int,
    val isResting: Boolean,
    val restRemainingSeconds: Int,
    val restTotalSeconds: Int,
    /** Incrémenté quand un repos arrive à zéro (sert à déclencher bip et vibration). */
    val restCompletedCount: Int,
    val isFinished: Boolean
) {
    val hasCurrentSet: Boolean get() = exerciseName.isNotEmpty()
}

/** Actions déclenchées depuis le widget ou la notification, exécutées par l'écran de séance. */
enum class LiveWorkoutCommand { COMPLETE_CURRENT_SET, SKIP_REST, ADD_REST_15S }

/**
 * Lien entre la séance (ActiveWorkoutViewModel, source de vérité) et le service de premier plan
 * qui l'affiche hors de l'application. Tout vit dans le même processus.
 */
object LiveWorkout {
    private val _state = MutableStateFlow<LiveWorkoutState?>(null)
    val state: StateFlow<LiveWorkoutState?> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<LiveWorkoutCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<LiveWorkoutCommand> = _commands.asSharedFlow()

    /** Faux quand PETRA passe en arrière-plan : c'est là que le widget flottant apparaît. */
    val appInForeground = MutableStateFlow(true)

    fun publish(state: LiveWorkoutState?) {
        _state.value = state
    }

    fun send(command: LiveWorkoutCommand) {
        _commands.tryEmit(command)
    }
}
