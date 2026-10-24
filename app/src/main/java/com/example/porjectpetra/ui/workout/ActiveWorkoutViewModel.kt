package com.example.porjectpetra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.data.catalog.CatalogExercise
import com.example.porjectpetra.data.catalog.CycleSet
import com.example.porjectpetra.data.catalog.EmptyExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCycle
import com.example.porjectpetra.data.catalog.SetPhase
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.local.entity.WorkoutSession
import com.example.porjectpetra.data.repository.ProgramRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveSetUiState(
    val tempId: String = UUID.randomUUID().toString(),
    val id: Long = 0,
    val setNumber: Int,
    val targetReps: Int = 10,
    val targetWeightKg: Double = 0.0,
    val targetDurationSeconds: Int = 0,
    val actualReps: String = "",
    val actualWeightKg: String = "",
    val actualDurationMins: String = "",
    val actualDistanceKm: String = "",
    val actualCalories: String = "",
    val isCompleted: Boolean = false,
    /** Phase du cycle (exercices du catalogue) ; null pour un exercice libre. */
    val phase: SetPhase? = null,
    val phaseLabel: String = "",
    /** Repos après cette série (exercices du catalogue). */
    val restSeconds: Int = 0,
    /** Vrai si [targetReps] est une durée en secondes (gainage). */
    val isSeconds: Boolean = false
)

data class ActiveExerciseUiState(
    val exerciseId: Long? = null,
    val name: String,
    val muscleGroup: String = "Corps entier",
    val isCardio: Boolean = false,
    val restTimeSeconds: Int = 60,
    val sets: List<ActiveSetUiState> = emptyList(),
    val notes: String = "",
    /** Exercice du catalogue : cycle fixe, charges déduites de [workLoadKg]. */
    val catalogId: String? = null,
    /** Charge de travail saisie (texte du champ). */
    val workLoadKg: String = "",
    val usesLoad: Boolean = true,
    val isUnilateral: Boolean = false
) {
    val isCycle: Boolean get() = catalogId != null
}

data class ActiveWorkoutUiState(
    val programId: Long? = null,
    val workoutTitle: String = "Entraînement Libre",
    val startTimeMs: Long = System.currentTimeMillis(),
    val elapsedTimeSeconds: Long = 0,
    val isStopwatchRunning: Boolean = true,
    val exercises: List<ActiveExerciseUiState> = emptyList(),
    val notes: String = "",
    // Rest Timer
    val restTimerTotalSeconds: Int = 0,
    val restTimerRemainingSeconds: Int = 0,
    val isRestTimerActive: Boolean = false,
    val isRestTimerPaused: Boolean = false,
    val restTimerExerciseName: String = "",
    // Dialogs & Navigation flags
    val showFinishSummaryDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val showAddExerciseDialog: Boolean = false,
    val isSessionSaved: Boolean = false,
    val isSessionCancelled: Boolean = false,
    val userMessage: String? = null
) {
    val completedSetsCount: Int
        get() = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

    val totalSetsCount: Int
        get() = exercises.sumOf { it.sets.size }

    val totalVolumeLiftedKg: Double
        get() = exercises.filter { !it.isCardio }.sumOf { ex ->
            ex.sets.filter { it.isCompleted }.sumOf { s ->
                val weight = s.actualWeightKg.toDoubleOrNull() ?: 0.0
                val reps = s.actualReps.toIntOrNull() ?: 0
                weight * reps
            }
        }

    val formattedElapsedTime: String
        get() {
            val hours = elapsedTimeSeconds / 3600
            val minutes = (elapsedTimeSeconds % 3600) / 60
            val seconds = elapsedTimeSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedRestTime: String
        get() {
            val minutes = restTimerRemainingSeconds / 60
            val seconds = restTimerRemainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

class ActiveWorkoutViewModel(
    private val programRepository: ProgramRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val catalog: ExerciseCatalog = EmptyExerciseCatalog
) : ViewModel() {

    /** Exercices du programme chargé, pour mémoriser la charge de travail à la fin de la séance. */
    private var programExercises: Map<Long, Exercise> = emptyMap()

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var stopwatchJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        startStopwatch()
    }

    fun startSessionForProgram(programId: Long?) {
        viewModelScope.launch {
            if (programId == null || programId <= 0) {
                _uiState.update {
                    it.copy(
                        programId = null,
                        workoutTitle = "Entraînement Libre",
                        startTimeMs = System.currentTimeMillis(),
                        elapsedTimeSeconds = 0
                    )
                }
            } else {
                val programWithExercises = programRepository.getProgramWithExercisesById(programId).firstOrNull()
                if (programWithExercises != null) {
                    programExercises = programWithExercises.exercises.associateBy { it.id }
                    val activeExercises = programWithExercises.exercises.map { exercise ->
                        catalog.byId(exercise.catalogId)?.let { catalogExercise ->
                            return@map cycleExercise(catalogExercise, exercise.targetWeightKg, exerciseId = exercise.id, notes = exercise.notes)
                        }
                        val isCardio = exercise.muscleGroup.equals("Cardio", ignoreCase = true) || exercise.targetDurationSeconds > 0
                        val sets = (1..exercise.targetSets).map { setIndex ->
                            ActiveSetUiState(
                                setNumber = setIndex,
                                targetReps = exercise.targetReps,
                                targetWeightKg = exercise.targetWeightKg,
                                targetDurationSeconds = exercise.targetDurationSeconds,
                                actualReps = if (exercise.targetReps > 0) exercise.targetReps.toString() else "",
                                actualWeightKg = if (exercise.targetWeightKg > 0) exercise.targetWeightKg.toString() else "",
                                actualDurationMins = if (exercise.targetDurationSeconds > 0) (exercise.targetDurationSeconds / 60).toString() else ""
                            )
                        }
                        ActiveExerciseUiState(
                            exerciseId = exercise.id,
                            name = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            isCardio = isCardio,
                            restTimeSeconds = exercise.restTimeSeconds,
                            sets = sets,
                            notes = exercise.notes
                        )
                    }

                    _uiState.update {
                        it.copy(
                            programId = programWithExercises.program.id,
                            workoutTitle = programWithExercises.program.name,
                            startTimeMs = System.currentTimeMillis(),
                            elapsedTimeSeconds = 0,
                            exercises = activeExercises
                        )
                    }
                }
            }
        }
    }

    private fun cycleExercise(
        catalogExercise: CatalogExercise,
        workLoadKg: Double,
        exerciseId: Long? = null,
        notes: String = ""
    ) = ActiveExerciseUiState(
        exerciseId = exerciseId,
        name = catalogExercise.name,
        muscleGroup = catalogExercise.muscleGroup,
        isCardio = false,
        restTimeSeconds = catalogExercise.work.restSeconds ?: 120,
        sets = ExerciseCycle.build(catalogExercise, workLoadKg).map { it.toUiState() },
        notes = notes,
        catalogId = catalogExercise.id,
        workLoadKg = if (catalogExercise.usesLoad && workLoadKg > 0) ExerciseCycle.formatKg(workLoadKg).replace(',', '.') else "",
        usesLoad = catalogExercise.usesLoad,
        isUnilateral = catalogExercise.isUnilateral
    )

    private fun CycleSet.toUiState(previous: ActiveSetUiState? = null) = ActiveSetUiState(
        tempId = previous?.tempId ?: UUID.randomUUID().toString(),
        setNumber = setNumber,
        targetReps = reps,
        targetWeightKg = weightKg,
        targetDurationSeconds = if (isSeconds) reps else 0,
        actualReps = if (isSeconds) "" else reps.toString(),
        actualWeightKg = if (weightKg > 0) ExerciseCycle.formatKg(weightKg).replace(',', '.') else "",
        isCompleted = previous?.isCompleted ?: false,
        phase = phase,
        phaseLabel = label,
        restSeconds = restSeconds,
        isSeconds = isSeconds
    )

    /**
     * Charge de travail d'un exercice du catalogue : recalcule toutes les charges du cycle
     * (chauffe et hypertrophie en pourcentage de la charge de travail).
     */
    fun updateWorkLoad(exerciseIndex: Int, text: String) {
        val cleaned = text.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { exIdx, exercise ->
                val catalogExercise = catalog.byId(exercise.catalogId)
                if (exIdx != exerciseIndex || catalogExercise == null) return@mapIndexed exercise
                val cycle = ExerciseCycle.build(catalogExercise, cleaned.toDoubleOrNull() ?: 0.0)
                exercise.copy(
                    workLoadKg = cleaned,
                    sets = cycle.mapIndexed { i, set -> set.toUiState(exercise.sets.getOrNull(i)) }
                )
            })
        }
    }

    private fun startStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_uiState.value.isStopwatchRunning) {
                    _uiState.update { state ->
                        val now = System.currentTimeMillis()
                        val elapsed = (now - state.startTimeMs) / 1000
                        state.copy(elapsedTimeSeconds = elapsed)
                    }
                }
            }
        }
    }

    fun toggleStopwatch() {
        _uiState.update { it.copy(isStopwatchRunning = !it.isStopwatchRunning) }
    }

    // Set Data Update Handlers
    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weightKg: String) {
        updateSet(exerciseIndex, setIndex) { it.copy(actualWeightKg = weightKg) }
    }

    fun updateSetDuration(exerciseIndex: Int, setIndex: Int, durationMins: String) {
        updateSet(exerciseIndex, setIndex) { it.copy(actualDurationMins = durationMins) }
    }

    fun updateSetDistance(exerciseIndex: Int, setIndex: Int, distanceKm: String) {
        updateSet(exerciseIndex, setIndex) { it.copy(actualDistanceKm = distanceKm) }
    }

    fun updateSetCalories(exerciseIndex: Int, setIndex: Int, calories: String) {
        updateSet(exerciseIndex, setIndex) { it.copy(actualCalories = calories) }
    }

    fun toggleSetCompleted(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.mapIndexed { exIdx, exercise ->
                if (exIdx == exerciseIndex) {
                    val updatedSets = exercise.sets.mapIndexed { sIdx, setItem ->
                        if (sIdx == setIndex) {
                            val nextState = !setItem.isCompleted
                            // Exercice du catalogue : repos propre à chaque série du cycle.
                            val rest = if (exercise.isCycle) setItem.restSeconds else exercise.restTimeSeconds
                            if (nextState && rest > 0) {
                                startRestTimer(rest, if (exercise.isCycle) "${exercise.name} · ${setItem.phaseLabel}" else exercise.name)
                            }
                            setItem.copy(isCompleted = nextState)
                        } else setItem
                    }
                    exercise.copy(sets = updatedSets)
                } else exercise
            }
            state.copy(exercises = updatedExercises)
        }
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (ActiveSetUiState) -> ActiveSetUiState) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.mapIndexed { exIdx, exercise ->
                if (exIdx == exerciseIndex) {
                    val updatedSets = exercise.sets.mapIndexed { sIdx, setItem ->
                        if (sIdx == setIndex) transform(setItem) else setItem
                    }
                    exercise.copy(sets = updatedSets)
                } else exercise
            }
            state.copy(exercises = updatedExercises)
        }
    }

    // Le nombre de séries et de répétitions est fixé par le programme : pas d'ajout, de retrait
    // ni de modification des répétitions pendant la séance. Seule la charge reste ajustable.

    fun removeExercise(exerciseIndex: Int) {
        _uiState.update { state ->
            val filtered = state.exercises.filterIndexed { index, _ -> index != exerciseIndex }
            state.copy(exercises = filtered)
        }
    }

    // Add new exercise on the fly
    fun openAddExerciseDialog() {
        _uiState.update { it.copy(showAddExerciseDialog = true) }
    }

    fun closeAddExerciseDialog() {
        _uiState.update { it.copy(showAddExerciseDialog = false) }
    }

    /** Ajoute en cours de séance un exercice du catalogue, avec son cycle fixe. */
    fun addCatalogExercise(catalogExercise: CatalogExercise, workLoadKg: Double) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises + cycleExercise(catalogExercise, workLoadKg),
                showAddExerciseDialog = false
            )
        }
    }

    fun updateSessionNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    // Rest Timer Logic
    private fun startRestTimer(seconds: Int, exerciseName: String) {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(
                restTimerTotalSeconds = seconds,
                restTimerRemainingSeconds = seconds,
                isRestTimerActive = true,
                isRestTimerPaused = false,
                restTimerExerciseName = exerciseName
            )
        }
        restTimerJob = viewModelScope.launch {
            while (_uiState.value.isRestTimerActive && _uiState.value.restTimerRemainingSeconds > 0) {
                delay(1000L)
                if (!_uiState.value.isRestTimerPaused) {
                    _uiState.update { state ->
                        val remaining = state.restTimerRemainingSeconds - 1
                        if (remaining <= 0) {
                            state.copy(
                                restTimerRemainingSeconds = 0,
                                isRestTimerActive = false,
                                userMessage = "Repos terminé !"
                            )
                        } else {
                            state.copy(restTimerRemainingSeconds = remaining)
                        }
                    }
                }
            }
        }
    }

    fun pauseRestTimer() {
        _uiState.update { it.copy(isRestTimerPaused = true) }
    }

    fun resumeRestTimer() {
        _uiState.update { it.copy(isRestTimerPaused = false) }
    }

    fun addRestTime(additionalSeconds: Int = 30) {
        _uiState.update {
            it.copy(
                restTimerRemainingSeconds = it.restTimerRemainingSeconds + additionalSeconds,
                restTimerTotalSeconds = it.restTimerTotalSeconds + additionalSeconds
            )
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isRestTimerActive = false,
                restTimerRemainingSeconds = 0
            )
        }
    }

    // Finish & Save Session
    fun openFinishSummaryDialog() {
        _uiState.update { it.copy(showFinishSummaryDialog = true) }
    }

    fun dismissFinishSummaryDialog() {
        _uiState.update { it.copy(showFinishSummaryDialog = false) }
    }

    fun confirmFinishAndSaveWorkout() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val session = WorkoutSession(
                programId = currentState.programId,
                programName = currentState.workoutTitle,
                startTime = currentState.startTimeMs,
                endTime = System.currentTimeMillis(),
                isCompleted = true,
                notes = currentState.notes
            )

            val loggedSets = mutableListOf<LoggedSet>()
            currentState.exercises.forEach { exercise ->
                exercise.sets.forEach { setItem ->
                    val durationSec = if (setItem.isSeconds) {
                        setItem.targetDurationSeconds
                    } else if (exercise.isCardio) {
                        ((setItem.actualDurationMins.toDoubleOrNull() ?: 0.0) * 60).toInt()
                    } else {
                        setItem.targetDurationSeconds
                    }
                    loggedSets.add(
                        LoggedSet(
                            sessionId = 0, // set in repository
                            exerciseId = exercise.exerciseId,
                            exerciseName = exercise.name,
                            setNumber = setItem.setNumber,
                            repsCompleted = setItem.actualReps.toIntOrNull() ?: 0,
                            weightKg = setItem.actualWeightKg.toDoubleOrNull() ?: 0.0,
                            durationSeconds = durationSec,
                            distanceKm = setItem.actualDistanceKm.toDoubleOrNull() ?: 0.0,
                            calories = setItem.actualCalories.toIntOrNull() ?: 0,
                            isCompleted = setItem.isCompleted,
                            phase = setItem.phase?.name ?: ""
                        )
                    )
                }
            }

            workoutSessionRepository.saveCompletedWorkoutSession(session, loggedSets)

            // La charge de travail saisie devient celle du programme pour la prochaine séance.
            currentState.exercises.filter { it.isCycle && it.usesLoad }.forEach { ex ->
                val original = ex.exerciseId?.let { programExercises[it] } ?: return@forEach
                val load = ex.workLoadKg.toDoubleOrNull() ?: return@forEach
                if (load != original.targetWeightKg) {
                    programRepository.updateExercise(original.copy(targetWeightKg = load))
                }
            }
            _uiState.update {
                it.copy(
                    showFinishSummaryDialog = false,
                    isSessionSaved = true
                )
            }
        }
    }

    // Cancel Session
    fun openCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun confirmCancelWorkout() {
        restTimerJob?.cancel()
        stopwatchJob?.cancel()
        _uiState.update {
            it.copy(
                showCancelDialog = false,
                isSessionCancelled = true
            )
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            programRepository: ProgramRepository,
            workoutSessionRepository: WorkoutSessionRepository,
            catalog: ExerciseCatalog = EmptyExerciseCatalog,
            programId: Long? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val vm = ActiveWorkoutViewModel(programRepository, workoutSessionRepository, catalog)
                if (programId != null) {
                    vm.startSessionForProgram(programId)
                }
                return vm as T
            }
        }
    }
}
