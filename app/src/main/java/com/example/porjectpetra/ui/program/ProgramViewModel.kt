package com.example.porjectpetra.ui.program

import com.example.porjectpetra.data.catalog.CatalogExercise
import com.example.porjectpetra.data.catalog.EmptyExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.data.repository.ProgramRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgramUiState(
    val selectedProgramId: Long? = null,
    val selectedCategoryFilter: String = "Tout", // All
    val showProgramDialog: Boolean = false,
    val programToEdit: WorkoutProgram? = null,
    val showExerciseDialog: Boolean = false,
    val exerciseToEdit: Exercise? = null,
    val userMessage: String? = null
)

class ProgramViewModel(
    private val repository: ProgramRepository,
    val catalog: ExerciseCatalog = EmptyExerciseCatalog
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgramUiState())
    val uiState: StateFlow<ProgramUiState> = _uiState.asStateFlow()

    val programsWithExercises: StateFlow<List<ProgramWithExercises>> =
        repository.getAllProgramsWithExercises()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedProgramWithExercises: StateFlow<ProgramWithExercises?> =
        _uiState.flatMapLatest { state ->
            val id = state.selectedProgramId
            if (id != null) {
                repository.getProgramWithExercisesById(id)
            } else {
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun selectProgram(programId: Long?) {
        _uiState.value = _uiState.value.copy(selectedProgramId = programId)
    }

    fun setCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
    }

    // Program Dialog Management
    fun openAddProgramDialog() {
        _uiState.value = _uiState.value.copy(
            showProgramDialog = true,
            programToEdit = null
        )
    }

    fun openEditProgramDialog(program: WorkoutProgram) {
        _uiState.value = _uiState.value.copy(
            showProgramDialog = true,
            programToEdit = program
        )
    }

    fun closeProgramDialog() {
        _uiState.value = _uiState.value.copy(
            showProgramDialog = false,
            programToEdit = null
        )
    }

    fun saveProgram(
        name: String,
        description: String,
        category: String,
        targetFrequency: Int
    ) {
        viewModelScope.launch {
            val currentToEdit = _uiState.value.programToEdit
            if (currentToEdit != null) {
                repository.updateProgram(
                    currentToEdit.copy(
                        name = name.ifBlank { "Programme d'entraînement" },
                        description = description,
                        category = category,
                        targetFrequencyPerWeek = targetFrequency,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                showUserMessage("Programme mis à jour avec succès")
            } else {
                val newId = repository.insertProgram(
                    WorkoutProgram(
                        name = name.ifBlank { "Nouveau Programme" },
                        description = description,
                        category = category,
                        targetFrequencyPerWeek = targetFrequency
                    )
                )
                _uiState.value = _uiState.value.copy(selectedProgramId = newId)
                showUserMessage("Nouveau programme créé")
            }
            closeProgramDialog()
        }
    }

    fun deleteProgram(programId: Long) {
        viewModelScope.launch {
            repository.deleteProgramById(programId)
            if (_uiState.value.selectedProgramId == programId) {
                _uiState.value = _uiState.value.copy(selectedProgramId = null)
            }
            showUserMessage("Programme supprimé")
        }
    }

    // Exercise Dialog Management
    fun openAddExerciseDialog() {
        _uiState.value = _uiState.value.copy(
            showExerciseDialog = true,
            exerciseToEdit = null
        )
    }

    fun openEditExerciseDialog(exercise: Exercise) {
        _uiState.value = _uiState.value.copy(
            showExerciseDialog = true,
            exerciseToEdit = exercise
        )
    }

    fun closeExerciseDialog() {
        _uiState.value = _uiState.value.copy(
            showExerciseDialog = false,
            exerciseToEdit = null
        )
    }

    fun saveExercise(
        name: String,
        muscleGroup: String,
        targetSets: Int,
        targetReps: Int,
        targetWeightKg: Double,
        targetDurationSeconds: Int,
        restTimeSeconds: Int,
        notes: String
    ) {
        val programId = _uiState.value.selectedProgramId ?: return
        viewModelScope.launch {
            val currentToEdit = _uiState.value.exerciseToEdit
            if (currentToEdit != null) {
                repository.updateExercise(
                    currentToEdit.copy(
                        name = name.ifBlank { "Exercice" },
                        muscleGroup = muscleGroup,
                        targetSets = targetSets,
                        targetReps = targetReps,
                        targetWeightKg = targetWeightKg,
                        targetDurationSeconds = targetDurationSeconds,
                        restTimeSeconds = restTimeSeconds,
                        notes = notes
                    )
                )
                showUserMessage("Exercice mis à jour")
            } else {
                val currentExercisesCount = selectedProgramWithExercises.value?.exercises?.size ?: 0
                repository.insertExercise(
                    Exercise(
                        programId = programId,
                        name = name.ifBlank { "Exercice" },
                        muscleGroup = muscleGroup,
                        targetSets = targetSets,
                        targetReps = targetReps,
                        targetWeightKg = targetWeightKg,
                        targetDurationSeconds = targetDurationSeconds,
                        restTimeSeconds = restTimeSeconds,
                        orderIndex = currentExercisesCount,
                        notes = notes
                    )
                )
                showUserMessage("Exercice ajouté au programme")
            }
            closeExerciseDialog()
        }
    }

    /**
     * Ajoute ou modifie un exercice du catalogue. Séries, répétitions et repos suivent le cycle fixe du
     * catalogue ; seule la charge de travail (qui détermine les autres charges) et les notes sont libres.
     */
    fun saveCatalogExercise(exercise: CatalogExercise, workLoadKg: Double, notes: String) {
        val programId = _uiState.value.selectedProgramId ?: return
        viewModelScope.launch {
            val base = _uiState.value.exerciseToEdit
                ?: Exercise(programId = programId, name = exercise.name, orderIndex = selectedProgramWithExercises.value?.exercises?.size ?: 0)
            val updated = base.copy(
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                catalogId = exercise.id,
                targetSets = ExerciseCycle.TOTAL_SETS,
                targetReps = exercise.work.reps,
                targetWeightKg = if (exercise.usesLoad) workLoadKg.coerceAtLeast(0.0) else 0.0,
                targetDurationSeconds = 0,
                restTimeSeconds = exercise.work.restSeconds ?: 120,
                notes = notes
            )
            if (base.id == 0L) {
                repository.insertExercise(updated)
                showUserMessage("Exercice ajouté au programme")
            } else {
                repository.updateExercise(updated)
                showUserMessage("Exercice mis à jour")
            }
            closeExerciseDialog()
        }
    }

    fun deleteExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteExerciseById(exerciseId)
            showUserMessage("Exercice supprimé")
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    private fun showUserMessage(msg: String) {
        _uiState.value = _uiState.value.copy(userMessage = msg)
    }

    companion object {
        fun provideFactory(
            repository: ProgramRepository,
            catalog: ExerciseCatalog = EmptyExerciseCatalog
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgramViewModel(repository, catalog) as T
            }
        }
    }
}
