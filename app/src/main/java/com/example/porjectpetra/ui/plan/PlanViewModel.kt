package com.example.porjectpetra.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.PlannedWorkout
import com.example.porjectpetra.data.model.TrainingPlanner
import com.example.porjectpetra.data.preferences.UserPreferencesRepository
import com.example.porjectpetra.data.repository.PlanRepository
import com.example.porjectpetra.data.repository.ProgramRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanItemUi(
    val planned: PlannedWorkout,
    val programName: String,
    val category: String,
    val isDone: Boolean
)

data class PlanDayUi(
    val epochDay: Long,
    val weekIndex: Int,
    val isToday: Boolean,
    val isPast: Boolean,
    val items: List<PlanItemUi>
)

data class PlanUiState(
    val isLoading: Boolean = true,
    val today: Long = PetraDates.epochDay(),
    /** Semaine en cours puis semaine suivante (14 jours à partir du lundi). */
    val days: List<PlanDayUi> = emptyList(),
    val programs: List<WorkoutProgram> = emptyList(),
    val plannedThisWeek: Int = 0,
    val doneThisWeek: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val planRepository: PlanRepository,
    programRepository: ProgramRepository,
    workoutSessionRepository: WorkoutSessionRepository,
    private val preferences: UserPreferencesRepository,
    private val today: StateFlow<Long>
) : ViewModel() {

    init {
        // Première ouverture : la semaine en cours devient la semaine A.
        viewModelScope.launch { preferences.planAnchorMonday(PetraDates.startOfWeek(today.value)) }
    }

    val uiState: StateFlow<PlanUiState> = combine(today, preferences.planAnchor.filterNotNull()) { d, a -> d to a }
        .flatMapLatest { (day, anchor) ->
            val start = PetraDates.startOfWeek(day)
            val end = start + 13
            combine(
                planRepository.getSlots(),
                planRepository.getOverridesBetween(start, end),
                programRepository.getAllPrograms(),
                workoutSessionRepository.getAllSessions()
            ) { slots, overrides, programs, sessions ->
                val byId = programs.associateBy { it.id }
                val completedByDay = sessions.filter { it.isCompleted && it.programId != null }
                    .groupBy({ PetraDates.epochDay(it.startTime) }, { it.programId!! })
                val days = (start..end).map { d ->
                    PlanDayUi(
                        epochDay = d,
                        weekIndex = TrainingPlanner.weekIndex(d, anchor),
                        isToday = d == day,
                        isPast = d < day,
                        items = TrainingPlanner.planDay(d, anchor, slots, overrides).mapNotNull { p ->
                            val program = byId[p.programId] ?: return@mapNotNull null
                            PlanItemUi(p, program.name, program.category, TrainingPlanner.isDone(p, completedByDay))
                        }
                    )
                }
                val thisWeek = days.take(7).flatMap { it.items }.filterNot { it.planned.isCancelled }
                PlanUiState(
                    isLoading = false,
                    today = day,
                    days = days,
                    programs = programs,
                    plannedThisWeek = thisWeek.size,
                    doneThisWeek = thisWeek.count { it.isDone }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun addSession(day: PlanDayUi, programId: Long, timeMinutes: Int?, recurring: Boolean) {
        viewModelScope.launch {
            if (recurring) {
                planRepository.addToCycle(day.weekIndex, PetraDates.dayIndex(day.epochDay), programId, timeMinutes)
            } else {
                planRepository.addForDay(day.epochDay, programId, timeMinutes)
            }
        }
    }

    fun removeFromCycle(item: PlanItemUi) {
        val slotId = item.planned.slotId ?: return
        viewModelScope.launch { planRepository.removeFromCycle(slotId) }
    }

    fun setCancelledForDay(item: PlanItemUi, cancelled: Boolean) {
        viewModelScope.launch { planRepository.setCancelledForDay(item.planned, cancelled) }
    }

    fun removeDayOnly(item: PlanItemUi) {
        val overrideId = item.planned.overrideId ?: return
        viewModelScope.launch { planRepository.removeDayOnly(overrideId) }
    }

    fun swapWeeks() {
        viewModelScope.launch { preferences.swapPlanWeeks(PetraDates.startOfWeek(today.value)) }
    }

    companion object {
        fun provideFactory(app: FitnessApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlanViewModel(
                    app.planRepository, app.programRepository, app.workoutSessionRepository,
                    app.userPreferences, app.todayProvider.today
                ) as T
        }
    }
}
