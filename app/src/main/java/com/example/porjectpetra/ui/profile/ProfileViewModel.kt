package com.example.porjectpetra.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.data.preferences.UserPreferencesRepository
import com.example.porjectpetra.data.preferences.UserSettings
import com.example.porjectpetra.data.repository.RoutineRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val preferences: UserPreferencesRepository,
    private val routineRepository: RoutineRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setDisplayName(name: String) {
        viewModelScope.launch { preferences.setDisplayName(name) }
    }

    fun setWeeklySessionGoal(goal: Int) {
        viewModelScope.launch { preferences.setWeeklySessionGoal(goal) }
    }

    fun clearTaskHistory() {
        viewModelScope.launch {
            routineRepository.clearTaskHistory()
            _message.value = "Historique des tâches effacé. Tes routines sont conservées."
        }
    }

    fun clearWorkoutHistory() {
        viewModelScope.launch {
            workoutSessionRepository.deleteAllSessions()
            _message.value = "Historique des séances effacé. Tes programmes sont conservés."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun provideFactory(app: FitnessApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(app.userPreferences, app.routineRepository, app.workoutSessionRepository) as T
        }
    }
}
