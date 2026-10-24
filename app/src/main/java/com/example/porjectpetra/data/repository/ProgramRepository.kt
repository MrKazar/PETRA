package com.example.porjectpetra.data.repository

import com.example.porjectpetra.data.local.dao.ProgramDao
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun getAllPrograms(): Flow<List<WorkoutProgram>>
    fun getAllProgramsWithExercises(): Flow<List<ProgramWithExercises>>
    fun getProgramById(programId: Long): Flow<WorkoutProgram?>
    fun getProgramWithExercisesById(programId: Long): Flow<ProgramWithExercises?>
    fun getExercisesForProgram(programId: Long): Flow<List<Exercise>>

    suspend fun insertProgram(program: WorkoutProgram): Long
    suspend fun updateProgram(program: WorkoutProgram)
    suspend fun deleteProgram(program: WorkoutProgram)
    suspend fun deleteProgramById(programId: Long)

    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
    suspend fun deleteExerciseById(exerciseId: Long)
}

class ProgramRepositoryImpl(
    private val programDao: ProgramDao
) : ProgramRepository {
    override fun getAllPrograms(): Flow<List<WorkoutProgram>> =
        programDao.getAllPrograms()

    override fun getAllProgramsWithExercises(): Flow<List<ProgramWithExercises>> =
        programDao.getAllProgramsWithExercises()

    override fun getProgramById(programId: Long): Flow<WorkoutProgram?> =
        programDao.getProgramById(programId)

    override fun getProgramWithExercisesById(programId: Long): Flow<ProgramWithExercises?> =
        programDao.getProgramWithExercisesById(programId)

    override fun getExercisesForProgram(programId: Long): Flow<List<Exercise>> =
        programDao.getExercisesForProgram(programId)

    override suspend fun insertProgram(program: WorkoutProgram): Long =
        programDao.insertProgram(program)

    override suspend fun updateProgram(program: WorkoutProgram) =
        programDao.updateProgram(program)

    override suspend fun deleteProgram(program: WorkoutProgram) =
        programDao.deleteProgram(program)

    override suspend fun deleteProgramById(programId: Long) =
        programDao.deleteProgramById(programId)

    override suspend fun insertExercise(exercise: Exercise): Long =
        programDao.insertExercise(exercise)

    override suspend fun updateExercise(exercise: Exercise) =
        programDao.updateExercise(exercise)

    override suspend fun deleteExercise(exercise: Exercise) =
        programDao.deleteExercise(exercise)

    override suspend fun deleteExerciseById(exerciseId: Long) =
        programDao.deleteExerciseById(exerciseId)
}
