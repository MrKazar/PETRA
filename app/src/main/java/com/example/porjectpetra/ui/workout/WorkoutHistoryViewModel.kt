package com.example.porjectpetra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryExercise(val name: String, val sets: List<LoggedSet>)

data class HistorySession(
    val id: Long,
    val title: String,
    val startTime: Long,
    val durationMinutes: Long,
    val completedSets: Int,
    val totalSets: Int,
    val volumeKg: Double,
    val notes: String,
    val exercises: List<HistoryExercise>
)

data class WorkoutHistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<HistorySession> = emptyList()
)

class WorkoutHistoryViewModel(repository: WorkoutSessionRepository) : ViewModel() {

    val uiState: StateFlow<WorkoutHistoryUiState> = repository.getAllSessionsWithSets()
        .map { all ->
            WorkoutHistoryUiState(
                isLoading = false,
                sessions = all
                    .filter { it.session.isCompleted }
                    .map { item ->
                        val s = item.session
                        val sets = item.loggedSets.sortedWith(compareBy({ it.timestamp }, { it.setNumber }))
                        val done = sets.filter { it.isCompleted }
                        HistorySession(
                            id = s.id,
                            title = s.programName,
                            startTime = s.startTime,
                            durationMinutes = ((s.endTime ?: s.startTime) - s.startTime) / 60_000,
                            completedSets = done.size,
                            totalSets = sets.size,
                            volumeKg = done.sumOf { it.weightKg * it.repsCompleted },
                            notes = s.notes,
                            exercises = sets.groupBy { it.exerciseName }.map { (name, list) -> HistoryExercise(name, list) }
                        )
                    }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutHistoryUiState())

    companion object {
        fun provideFactory(app: FitnessApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WorkoutHistoryViewModel(app.workoutSessionRepository) as T
        }
    }
}
