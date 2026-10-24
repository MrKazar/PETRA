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

data class RoutineSummary(
    val routine: Routine,
    val taskCount: Int,
    val totalMinutes: Int,
    val dueToday: Boolean,
    val today: CompletionSummary
)

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val today: Long = PetraDates.epochDay(),
    val routines: List<RoutineSummary> = emptyList(),
    /** Toutes les tâches indépendantes (pas seulement celles du jour). */
    val standaloneTasks: List<DailyTask> = emptyList(),
    /** Tâches indépendantes prévues aujourd'hui, avec leur état. */
    val standaloneToday: List<PlannedTask> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<Long>
) : ViewModel() {

    val uiState: StateFlow<RoutinesUiState> = today.flatMapLatest { day ->
        combine(
            repository.getAllRoutines(),
            repository.getAllTasks(),
            repository.getLogsBetween(day - 1, day)
        ) { routines, tasks, logs ->
            val planned = DailyPlanner.planDay(day, tasks, routines, logs)
            RoutinesUiState(
                isLoading = false,
                today = day,
                routines = routines.map { routine ->
                    val routineTasks = tasks.filter { it.routineId == routine.id }
                    val plannedForRoutine = planned.filter { it.routine?.id == routine.id }
                    RoutineSummary(
                        routine = routine,
                        taskCount = routineTasks.size,
                        totalMinutes = routineTasks.sumOf { it.estimatedMinutes },
                        dueToday = plannedForRoutine.isNotEmpty(),
                        today = DailyPlanner.summarize(plannedForRoutine)
                    )
                },
                standaloneTasks = tasks.filter { it.routineId == null },
                standaloneToday = planned.filter { it.task.routineId == null }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    fun saveRoutine(routine: Routine, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch { onSaved(repository.saveRoutine(routine)) }
    }

    fun saveTask(task: DailyTask) {
        viewModelScope.launch { repository.saveTask(task) }
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

    companion object {
        fun provideFactory(app: FitnessApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RoutinesViewModel(app.routineRepository, app.todayProvider.today) as T
        }
    }
}
