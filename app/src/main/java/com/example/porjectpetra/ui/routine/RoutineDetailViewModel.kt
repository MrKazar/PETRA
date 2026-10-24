package com.example.porjectpetra.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.model.CompletionSummary
import com.example.porjectpetra.data.model.DailyPlanner
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.PlannedTask
import com.example.porjectpetra.data.model.TaskStatus
import com.example.porjectpetra.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bilan d'un jour passé pour l'historique de la routine. null = routine non prévue ce jour-là. */
data class RoutineDayHistory(val epochDay: Long, val summary: CompletionSummary?)

data class RoutineDetailUiState(
    val isLoading: Boolean = true,
    val today: Long = PetraDates.epochDay(),
    val routine: Routine? = null,
    val tasks: List<DailyTask> = emptyList(),
    /** Tâches de la routine prévues aujourd'hui (vide si la routine n'est pas active ce jour). */
    val todayTasks: List<PlannedTask> = emptyList(),
    val todaySummary: CompletionSummary = CompletionSummary(0, 0, 0, 0),
    val history: List<RoutineDayHistory> = emptyList(),
    val isDeleted: Boolean = false
)

private const val HISTORY_DAYS = 14

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineDetailViewModel(
    private val repository: RoutineRepository,
    private val routineId: Long,
    today: StateFlow<Long>
) : ViewModel() {

    val uiState: StateFlow<RoutineDetailUiState> = today.flatMapLatest { day ->
        combine(
            repository.getRoutineById(routineId),
            repository.getTasksForRoutine(routineId),
            repository.getLogsBetween(day - HISTORY_DAYS, day)
        ) { routine, tasks, logs ->
            if (routine == null) {
                return@combine RoutineDetailUiState(isLoading = false, today = day, isDeleted = true)
            }
            val routines = listOf(routine)
            val todayTasks = DailyPlanner.planDay(day, tasks, routines, logs)
            RoutineDetailUiState(
                isLoading = false,
                today = day,
                routine = routine,
                tasks = tasks,
                todayTasks = todayTasks,
                todaySummary = DailyPlanner.summarize(todayTasks),
                history = ((day - HISTORY_DAYS + 1)..day).map { d ->
                    val planned = DailyPlanner.planDay(d, tasks, routines, logs)
                    RoutineDayHistory(d, if (planned.isEmpty()) null else DailyPlanner.summarize(planned))
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineDetailUiState())

    fun saveRoutine(routine: Routine) {
        viewModelScope.launch { repository.saveRoutine(routine) }
    }

    fun deleteRoutine() {
        viewModelScope.launch { repository.deleteRoutine(routineId) }
    }

    fun saveTask(task: DailyTask) {
        viewModelScope.launch { repository.saveTask(task.copy(routineId = routineId)) }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    fun moveTask(task: DailyTask, up: Boolean) {
        viewModelScope.launch { repository.moveTask(task, up) }
    }

    fun setTaskStatus(item: PlannedTask, status: TaskStatus?) {
        viewModelScope.launch { repository.setTaskStatus(item.task.id, uiState.value.today, status) }
    }

    /** Remet toutes les tâches de la routine à « à faire » pour aujourd'hui. */
    fun resetToday() {
        viewModelScope.launch {
            val state = uiState.value
            state.todayTasks.forEach { repository.setTaskStatus(it.task.id, state.today, null) }
        }
    }

    companion object {
        fun provideFactory(app: FitnessApplication, routineId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RoutineDetailViewModel(app.routineRepository, routineId, app.todayProvider.today) as T
            }
    }
}
