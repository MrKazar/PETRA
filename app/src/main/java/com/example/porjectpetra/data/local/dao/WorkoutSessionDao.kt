package com.example.porjectpetra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.local.entity.SessionWithSets
import com.example.porjectpetra.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessionsWithSets(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionWithSetsById(sessionId: Long): Flow<SessionWithSets?>

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    // Logged Sets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSet(loggedSet: LoggedSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSets(loggedSets: List<LoggedSet>)

    @Update
    suspend fun updateLoggedSet(loggedSet: LoggedSet)

    @Delete
    suspend fun deleteLoggedSet(loggedSet: LoggedSet)

    @Query("DELETE FROM logged_sets WHERE sessionId = :sessionId")
    suspend fun deleteLoggedSetsForSession(sessionId: Long)
}
