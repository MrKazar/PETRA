package com.example.porjectpetra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog
import com.example.porjectpetra.data.local.entity.WorkoutSession
import com.example.porjectpetra.data.model.CompletionSummary
import com.example.porjectpetra.data.model.DailyPlanner
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.PlannedTask
import com.example.porjectpetra.data.model.TaskStatus
import com.example.porjectpetra.data.preferences.UserPreferencesRepository
import com.example.porjectpetra.data.preferences.UserSettings
import com.example.porjectpetra.data.repository.ProgramRepository
import com.example.porjectpetra.data.repository.RoutineRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Une section de l'accueil : une routine du jour, ou les tâches indépendantes (routine = null). */
data class HomeSection(val routine: Routine?, val tasks: List<PlannedTask>) {
    val summary: CompletionSummary get() = DailyPlanner.summarize(tasks)
}

data class SuggestedWorkout(
    val programId: Long,
    val name: String,
    val category: String,
    val exerciseCount: Int,
    /** Jours écoulés depuis la dernière séance de ce programme ; null = jamais réalisé. */
    val daysSinceLast: Long?
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val today: Long = PetraDates.epochDay(),
    val displayName: String = "",
    val sections: List<HomeSection> = emptyList(),
    val daySummary: CompletionSummary = CompletionSummary(0, 0, 0, 0),
    val nextTask: PlannedTask? = null,
    val weekSummary: CompletionSummary = CompletionSummary(0, 0, 0, 0),
    val weekSessions: Int = 0,
    val weeklySessionGoal: Int = UserSettings.DEFAULT_WEEKLY_SESSION_GOAL,
    val suggestedWorkout: SuggestedWorkout? = null,
    val hasPrograms: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val routineRepository: RoutineRepository,
    programRepository: ProgramRepository,
    workoutSessionRepository: WorkoutSessionRepository,
    userPreferences: UserPreferencesRepository,
    today: StateFlow<Long>
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = today.flatMapLatest { day ->
        val weekStart = PetraDates.startOfWeek(day)
        val tasksFlow = combine(
            routineRepository.getAllRoutines(),
            routineRepository.getAllTasks(),
            // Veille du lundi incluse : nécessaire pour retrouver les tâches reportées.
            routineRepository.getLogsBetween(weekStart - 1, day)
        ) { routines, tasks, logs -> Triple(routines, tasks, logs) }

        combine(
            tasksFlow,
            workoutSessionRepository.getAllSessions(),
            programRepository.getAllProgramsWithExercises(),
            userPreferences.settings
        ) { (routines, tasks, logs), sessions, programs, settings ->
            buildState(day, weekStart, routines, tasks, logs, sessions, programs, settings)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(
        day: Long,
        weekStart: Long,
        routines: List<Routine>,
        tasks: List<DailyTask>,
        logs: List<TaskLog>,
        sessions: List<WorkoutSession>,
        programs: List<ProgramWithExercises>,
        settings: UserSettings
    ): HomeUiState {
        val planned = DailyPlanner.planDay(day, tasks, routines, logs)

        // Sections dans l'ordre de la journée ; les tâches indépendantes sont regroupées à part.
        val sections = planned
            .groupBy { it.routine?.id }
            .map { (_, items) -> HomeSection(items.first().routine, items) }
            .sortedWith(compareBy({ it.routine == null }, { DailyPlanner.sortMinutes(it.tasks.first()) }))

        val completed = sessions.filter { it.isCompleted }
        val weekSessions = completed.count { PetraDates.epochDay(it.startTime) in weekStart..day }

        return HomeUiState(
            isLoading = false,
            today = day,
            displayName = settings.displayName,
            sections = sections,
            daySummary = DailyPlanner.summarize(planned),
            nextTask = DailyPlanner.nextPending(planned),
            weekSummary = DailyPlanner.summarizeRange(weekStart, day, tasks, routines, logs),
            weekSessions = weekSessions,
            weeklySessionGoal = settings.weeklySessionGoal,
            suggestedWorkout = suggestWorkout(day, programs, completed),
            hasPrograms = programs.isNotEmpty()
        )
    }

    /** Programme non vide le moins récemment réalisé (en attendant le calendrier sportif). */
    private fun suggestWorkout(
        day: Long,
        programs: List<ProgramWithExercises>,
        completed: List<WorkoutSession>
    ): SuggestedWorkout? {
        val lastByProgram = completed
            .filter { it.programId != null }
            .groupBy { it.programId!! }
            .mapValues { (_, s) -> s.maxOf { it.startTime } }
        val candidate = programs
            .filter { it.exercises.isNotEmpty() }
            .minByOrNull { lastByProgram[it.program.id] ?: Long.MIN_VALUE }
            ?: return null
        val last = lastByProgram[candidate.program.id]
        return SuggestedWorkout(
            programId = candidate.program.id,
            name = candidate.program.name,
            category = candidate.program.category,
            exerciseCount = candidate.exercises.size,
            daysSinceLast = last?.let { day - PetraDates.epochDay(it) }
        )
    }

    fun setTaskStatus(item: PlannedTask, status: TaskStatus?) {
        viewModelScope.launch {
            routineRepository.setTaskStatus(item.task.id, uiState.value.today, status)
        }
    }

    fun saveTask(task: DailyTask) {
        viewModelScope.launch { routineRepository.saveTask(task) }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { routineRepository.deleteTask(taskId) }
    }

    companion object {
        fun provideFactory(app: FitnessApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(
                    routineRepository = app.routineRepository,
                    programRepository = app.programRepository,
                    workoutSessionRepository = app.workoutSessionRepository,
                    userPreferences = app.userPreferences,
                    today = app.todayProvider.today
                ) as T
        }
    }
}
