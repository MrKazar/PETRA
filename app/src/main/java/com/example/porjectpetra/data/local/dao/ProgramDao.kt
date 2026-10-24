package com.example.porjectpetra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM workout_programs ORDER BY updatedAt DESC")
    fun getAllPrograms(): Flow<List<WorkoutProgram>>

    @Transaction
    @Query("SELECT * FROM workout_programs ORDER BY updatedAt DESC")
    fun getAllProgramsWithExercises(): Flow<List<ProgramWithExercises>>

    @Query("SELECT * FROM workout_programs WHERE id = :programId")
    fun getProgramById(programId: Long): Flow<WorkoutProgram?>

    @Transaction
    @Query("SELECT * FROM workout_programs WHERE id = :programId")
    fun getProgramWithExercisesById(programId: Long): Flow<ProgramWithExercises?>

    @Query("SELECT COUNT(*) FROM workout_programs")
    suspend fun getProgramCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: WorkoutProgram): Long

    @Update
    suspend fun updateProgram(program: WorkoutProgram)

    @Delete
    suspend fun deleteProgram(program: WorkoutProgram)

    @Query("DELETE FROM workout_programs WHERE id = :programId")
    suspend fun deleteProgramById(programId: Long)

    // Exercises
    @Query("SELECT * FROM exercises WHERE programId = :programId ORDER BY orderIndex ASC")
    fun getExercisesForProgram(programId: Long): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)
}
