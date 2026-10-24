package com.example.porjectpetra.ui.workout

import com.example.porjectpetra.data.catalog.SetPhase
import com.example.porjectpetra.data.catalog.testCatalog
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.SessionWithSets
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.data.local.entity.WorkoutSession
import com.example.porjectpetra.data.repository.ProgramRepository
import com.example.porjectpetra.data.repository.WorkoutSessionRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWorkoutSessionRepository : WorkoutSessionRepository {
    val sessions = mutableListOf<WorkoutSession>()
    val loggedSets = mutableListOf<LoggedSet>()
    private var nextSessionId = 1L

    override fun getAllSessions(): Flow<List<WorkoutSession>> = MutableStateFlow(sessions)

    override fun getAllSessionsWithSets(): Flow<List<SessionWithSets>> {
        return MutableStateFlow(sessions.map { s ->
            SessionWithSets(s, loggedSets.filter { it.sessionId == s.id })
        })
    }

    override fun getSessionWithSetsById(sessionId: Long): Flow<SessionWithSets?> {
        return MutableStateFlow(
            sessions.find { it.id == sessionId }?.let { s ->
                SessionWithSets(s, loggedSets.filter { it.sessionId == s.id })
            }
        )
    }

    override fun getActiveSession(): Flow<WorkoutSession?> = MutableStateFlow(sessions.find { !it.isCompleted })

    override suspend fun insertSession(session: WorkoutSession): Long {
        val id = if (session.id == 0L) nextSessionId++ else session.id
        val newS = session.copy(id = id)
        sessions.add(newS)
        return id
    }

    override suspend fun updateSession(session: WorkoutSession) {
        val idx = sessions.indexOfFirst { it.id == session.id }
        if (idx >= 0) sessions[idx] = session
    }

    override suspend fun deleteSession(session: WorkoutSession) {
        deleteSessionById(session.id)
    }

    override suspend fun deleteSessionById(sessionId: Long) {
        sessions.removeAll { it.id == sessionId }
        loggedSets.removeAll { it.sessionId == sessionId }
    }

    override suspend fun deleteAllSessions() {
        sessions.clear()
        loggedSets.clear()
    }

    override suspend fun insertLoggedSet(loggedSet: LoggedSet): Long {
        loggedSets.add(loggedSet)
        return loggedSet.id
    }

    override suspend fun insertLoggedSets(loggedSets: List<LoggedSet>) {
        this.loggedSets.addAll(loggedSets)
    }

    override suspend fun updateLoggedSet(loggedSet: LoggedSet) {
        val idx = this.loggedSets.indexOfFirst { it.id == loggedSet.id }
        if (idx >= 0) this.loggedSets[idx] = loggedSet
    }

    override suspend fun deleteLoggedSet(loggedSet: LoggedSet) {
        this.loggedSets.removeAll { it.id == loggedSet.id }
    }

    override suspend fun deleteLoggedSetsForSession(sessionId: Long) {
        this.loggedSets.removeAll { it.sessionId == sessionId }
    }

    override suspend fun saveCompletedWorkoutSession(
        session: WorkoutSession,
        loggedSets: List<LoggedSet>
    ): Long {
        val sessionId = insertSession(session)
        val mapped = loggedSets.map { it.copy(sessionId = sessionId) }
        insertLoggedSets(mapped)
        return sessionId
    }
}

class FakeProgramRepositoryForWorkout : ProgramRepository {
    val programs = mutableListOf<WorkoutProgram>()
    val exercises = mutableListOf<Exercise>()

    override fun getAllPrograms(): Flow<List<WorkoutProgram>> = MutableStateFlow(programs)

    override fun getAllProgramsWithExercises(): Flow<List<ProgramWithExercises>> {
        return MutableStateFlow(programs.map { p ->
            ProgramWithExercises(p, exercises.filter { it.programId == p.id })
        })
    }

    override fun getProgramById(programId: Long): Flow<WorkoutProgram?> {
        return MutableStateFlow(programs.find { it.id == programId })
    }

    override fun getProgramWithExercisesById(programId: Long): Flow<ProgramWithExercises?> {
        return MutableStateFlow(
            programs.find { it.id == programId }?.let { p ->
                ProgramWithExercises(p, exercises.filter { it.programId == p.id })
            }
        )
    }

    override fun getExercisesForProgram(programId: Long): Flow<List<Exercise>> {
        return MutableStateFlow(exercises.filter { it.programId == programId })
    }

    override suspend fun insertProgram(program: WorkoutProgram): Long {
        programs.add(program)
        return program.id
    }

    override suspend fun updateProgram(program: WorkoutProgram) {}
    override suspend fun deleteProgram(program: WorkoutProgram) {}
    override suspend fun deleteProgramById(programId: Long) {}
    override suspend fun insertExercise(exercise: Exercise): Long {
        exercises.add(exercise)
        return exercise.id
    }
    override suspend fun updateExercise(exercise: Exercise) {
        val index = exercises.indexOfFirst { it.id == exercise.id }
        if (index >= 0) exercises[index] = exercise
    }
    override suspend fun deleteExercise(exercise: Exercise) {}
    override suspend fun deleteExerciseById(exerciseId: Long) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var programRepository: FakeProgramRepositoryForWorkout
    private lateinit var workoutSessionRepository: FakeWorkoutSessionRepository
    private lateinit var viewModel: ActiveWorkoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        programRepository = FakeProgramRepositoryForWorkout()
        workoutSessionRepository = FakeWorkoutSessionRepository()
        viewModel = ActiveWorkoutViewModel(programRepository, workoutSessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startSessionForProgram_loadsExercisesAndPrefillsSets() = runTest {
        val program = WorkoutProgram(id = 10, name = "Full Body")
        val ex1 = Exercise(id = 1, programId = 10, name = "Squat", muscleGroup = "Legs", targetSets = 3, targetReps = 10, targetWeightKg = 80.0, restTimeSeconds = 90)
        programRepository.programs.add(program)
        programRepository.exercises.add(ex1)

        viewModel.startSessionForProgram(10)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        val state = viewModel.uiState.value
        assertEquals(10L, state.programId)
        assertEquals("Full Body", state.workoutTitle)
        assertEquals(1, state.exercises.size)

        val exerciseState = state.exercises[0]
        assertEquals("Squat", exerciseState.name)
        assertEquals(3, exerciseState.sets.size)
        assertEquals("10", exerciseState.sets[0].actualReps)
        assertEquals("80.0", exerciseState.sets[0].actualWeightKg)
    }

    @Test
    fun toggleSetCompleted_triggersRestTimerAndUpdatesState() = runTest {
        val program = WorkoutProgram(id = 1, name = "Upper Body")
        val ex = Exercise(id = 10, programId = 1, name = "Bench Press", muscleGroup = "Chest", targetSets = 2, restTimeSeconds = 60)
        programRepository.programs.add(program)
        programRepository.exercises.add(ex)

        viewModel.startSessionForProgram(1)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        viewModel.toggleSetCompleted(0, 0)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        val state = viewModel.uiState.value
        assertTrue(state.exercises[0].sets[0].isCompleted)
        assertTrue(state.isRestTimerActive)
        assertEquals("Bench Press", state.restTimerExerciseName)
        assertEquals(60, state.restTimerTotalSeconds)
    }

    @Test
    fun startSession_setsAndRepsAreFixedByProgram() = runTest {
        val program = WorkoutProgram(id = 1, name = "Haut du corps")
        val ex = Exercise(id = 10, programId = 1, name = "Développé couché", targetSets = 4, targetReps = 8)
        programRepository.programs.add(program)
        programRepository.exercises.add(ex)

        viewModel.startSessionForProgram(1)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        val sets = viewModel.uiState.value.exercises[0].sets
        assertEquals(4, sets.size)
        assertEquals(listOf(1, 2, 3, 4), sets.map { it.setNumber })
        assertTrue(sets.all { it.targetReps == 8 && it.actualReps == "8" })

        // Valider une série ne change ni le nombre de séries ni les répétitions
        viewModel.toggleSetCompleted(0, 0)
        val after = viewModel.uiState.value.exercises[0].sets
        assertEquals(4, after.size)
        assertEquals("8", after[0].actualReps)
    }

    @Test
    fun catalogExercise_followsFixedCycle_andLoadsDeriveFromWorkLoad() = runTest {
        val vm = ActiveWorkoutViewModel(programRepository, workoutSessionRepository, testCatalog)
        programRepository.programs.add(WorkoutProgram(id = 1, name = "Pectoraux"))
        programRepository.exercises.add(
            Exercise(id = 5, programId = 1, name = "Développé couché à la barre", catalogId = "dev_couche_barre", targetWeightKg = 60.0)
        )

        vm.startSessionForProgram(1)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        val sets = vm.uiState.value.exercises[0].sets
        assertEquals(listOf(SetPhase.WARMUP, SetPhase.WORK, SetPhase.WORK, SetPhase.HYPERTROPHY), sets.map { it.phase })
        assertEquals(listOf(12, 6, 6, 10), sets.map { it.targetReps })
        assertEquals(listOf(30.0, 60.0, 60.0, 48.0), sets.map { it.targetWeightKg })
        assertEquals(listOf(45, 180, 180, 120), sets.map { it.restSeconds })

        vm.toggleSetCompleted(0, 1)
        assertEquals(180, vm.uiState.value.restTimerTotalSeconds)

        // Nouvelle charge de travail : toutes les charges suivent, les séries validées le restent
        vm.updateWorkLoad(0, "100")
        val updated = vm.uiState.value.exercises[0].sets
        assertEquals(listOf(50.0, 100.0, 100.0, 80.0), updated.map { it.targetWeightKg })
        assertTrue(updated[1].isCompleted)

        vm.confirmFinishAndSaveWorkout()
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        assertEquals(listOf("WARMUP", "WORK", "WORK", "HYPERTROPHY"), workoutSessionRepository.loggedSets.map { it.phase })
        assertEquals("100", workoutSessionRepository.loggedSets[1].weightKg.toInt().toString())
        // La charge de travail est mémorisée dans le programme
        assertEquals(100.0, programRepository.exercises.single().targetWeightKg, 0.0)
    }

    @Test
    fun addCatalogExercise_addsFixedCycleToSession() = runTest {
        val vm = ActiveWorkoutViewModel(programRepository, workoutSessionRepository, testCatalog)
        vm.addCatalogExercise(testCatalog.byId("pompes")!!, 0.0)

        val exercise = vm.uiState.value.exercises.single()
        assertEquals("Pompes", exercise.name)
        assertEquals(4, exercise.sets.size)
        assertFalse(exercise.usesLoad)
        assertEquals(listOf(15, 15, 15, 25), exercise.sets.map { it.targetReps })
    }

    @Test
    fun confirmFinishAndSaveWorkout_savesSessionAndLoggedSetsToRepository() = runTest {
        val program = WorkoutProgram(id = 1, name = "Full Body")
        val ex = Exercise(id = 10, programId = 1, name = "Squat", targetSets = 2, targetReps = 10, targetWeightKg = 100.0)
        programRepository.programs.add(program)
        programRepository.exercises.add(ex)

        viewModel.startSessionForProgram(1)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        // Complete set 1
        viewModel.toggleSetCompleted(0, 0)
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        viewModel.confirmFinishAndSaveWorkout()
        testDispatcher.scheduler.runCurrent() // pas advanceUntilIdle : le chronomètre de séance tourne sans fin

        assertTrue(viewModel.uiState.value.isSessionSaved)
        assertEquals(1, workoutSessionRepository.sessions.size)
        assertEquals("Full Body", workoutSessionRepository.sessions[0].programName)
        assertEquals(2, workoutSessionRepository.loggedSets.size)
        assertTrue(workoutSessionRepository.loggedSets[0].isCompleted)
    }

    @Test
    fun confirmCancelWorkout_cancelsSession() = runTest {
        viewModel.confirmCancelWorkout()
        assertTrue(viewModel.uiState.value.isSessionCancelled)
    }
}
