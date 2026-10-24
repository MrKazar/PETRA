package com.example.porjectpetra.ui.program

import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.data.repository.ProgramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeProgramRepository : ProgramRepository {
    private val programsFlow = MutableStateFlow<List<WorkoutProgram>>(emptyList())
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(emptyList())
    private var nextProgramId = 1L
    private var nextExerciseId = 1L

    override fun getAllPrograms(): Flow<List<WorkoutProgram>> = programsFlow

    override fun getAllProgramsWithExercises(): Flow<List<ProgramWithExercises>> {
        return programsFlow.map { programs ->
            programs.map { p ->
                ProgramWithExercises(
                    program = p,
                    exercises = exercisesFlow.value.filter { it.programId == p.id }
                )
            }
        }
    }

    override fun getProgramById(programId: Long): Flow<WorkoutProgram?> {
        return programsFlow.map { list -> list.find { it.id == programId } }
    }

    override fun getProgramWithExercisesById(programId: Long): Flow<ProgramWithExercises?> {
        return programsFlow.map { list ->
            val p = list.find { it.id == programId } ?: return@map null
            ProgramWithExercises(
                program = p,
                exercises = exercisesFlow.value.filter { it.programId == p.id }
            )
        }
    }

    override fun getExercisesForProgram(programId: Long): Flow<List<Exercise>> {
        return exercisesFlow.map { list -> list.filter { it.programId == programId } }
    }

    override suspend fun insertProgram(program: WorkoutProgram): Long {
        val id = if (program.id == 0L) nextProgramId++ else program.id
        val newP = program.copy(id = id)
        programsFlow.value = programsFlow.value + newP
        return id
    }

    override suspend fun updateProgram(program: WorkoutProgram) {
        programsFlow.value = programsFlow.value.map { if (it.id == program.id) program else it }
    }

    override suspend fun deleteProgram(program: WorkoutProgram) {
        deleteProgramById(program.id)
    }

    override suspend fun deleteProgramById(programId: Long) {
        programsFlow.value = programsFlow.value.filter { it.id != programId }
        exercisesFlow.value = exercisesFlow.value.filter { it.programId != programId }
    }

    override suspend fun insertExercise(exercise: Exercise): Long {
        val id = if (exercise.id == 0L) nextExerciseId++ else exercise.id
        val newE = exercise.copy(id = id)
        exercisesFlow.value = exercisesFlow.value + newE
        return id
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exercisesFlow.value = exercisesFlow.value.map { if (it.id == exercise.id) exercise else it }
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        deleteExerciseById(exercise.id)
    }

    override suspend fun deleteExerciseById(exerciseId: Long) {
        exercisesFlow.value = exercisesFlow.value.filter { it.id != exerciseId }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeProgramRepository
    private lateinit var viewModel: ProgramViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeProgramRepository()
        viewModel = ProgramViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveProgram_createsNewProgram() = runTest {
        viewModel.saveProgram("Hypertrophy Push", "Chest & Triceps Focus", "Upper Body", 3)
        testDispatcher.scheduler.advanceUntilIdle()

        val programs = viewModel.programsWithExercises.value
        assertEquals(1, programs.size)
        assertEquals("Hypertrophy Push", programs[0].program.name)
        assertEquals("Upper Body", programs[0].program.category)
    }

    @Test
    fun selectProgram_updatesSelectedState() = runTest {
        viewModel.saveProgram("Full Body", "Description", "Full Body", 3)
        testDispatcher.scheduler.advanceUntilIdle()

        val programId = viewModel.programsWithExercises.value.first().program.id
        viewModel.selectProgram(programId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(programId, viewModel.uiState.value.selectedProgramId)
    }

    @Test
    fun saveExercise_addsExerciseToSelectedProgram() = runTest {
        viewModel.saveProgram("Leg Day", "Quads & Hamstrings", "Lower Body", 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val programId = viewModel.programsWithExercises.value.first().program.id
        viewModel.selectProgram(programId)

        viewModel.saveExercise(
            name = "Squat",
            muscleGroup = "Legs",
            targetSets = 4,
            targetReps = 10,
            targetWeightKg = 80.0,
            targetDurationSeconds = 0,
            restTimeSeconds = 90,
            notes = "Depth below parallel"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val selectedWithExercises = viewModel.selectedProgramWithExercises.value
        assertNotNull(selectedWithExercises)
        assertEquals(1, selectedWithExercises!!.exercises.size)
        assertEquals("Squat", selectedWithExercises.exercises.first().name)
    }

    @Test
    fun deleteProgram_removesProgramAndResetsSelection() = runTest {
        viewModel.saveProgram("To Delete", "Temp", "Cardio", 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val programId = viewModel.programsWithExercises.value.first().program.id
        viewModel.selectProgram(programId)

        viewModel.deleteProgram(programId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.programsWithExercises.value.size)
        assertNull(viewModel.uiState.value.selectedProgramId)
    }
}
