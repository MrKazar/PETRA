package com.example.porjectpetra.data.repository

import com.example.porjectpetra.data.local.dao.WorkoutSessionDao
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.data.local.entity.SessionWithSets
import com.example.porjectpetra.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutSessionRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    fun getAllSessionsWithSets(): Flow<List<SessionWithSets>>
    fun getSessionWithSetsById(sessionId: Long): Flow<SessionWithSets?>
    fun getActiveSession(): Flow<WorkoutSession?>

    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)
    suspend fun deleteSessionById(sessionId: Long)
    suspend fun deleteAllSessions()

    suspend fun insertLoggedSet(loggedSet: LoggedSet): Long
    suspend fun insertLoggedSets(loggedSets: List<LoggedSet>)
    suspend fun updateLoggedSet(loggedSet: LoggedSet)
    suspend fun deleteLoggedSet(loggedSet: LoggedSet)
    suspend fun deleteLoggedSetsForSession(sessionId: Long)

    suspend fun saveCompletedWorkoutSession(session: WorkoutSession, loggedSets: List<LoggedSet>): Long
}

class WorkoutSessionRepositoryImpl(
    private val workoutSessionDao: WorkoutSessionDao
) : WorkoutSessionRepository {
    override fun getAllSessions(): Flow<List<WorkoutSession>> =
        workoutSessionDao.getAllSessions()

    override fun getAllSessionsWithSets(): Flow<List<SessionWithSets>> =
        workoutSessionDao.getAllSessionsWithSets()

    override fun getSessionWithSetsById(sessionId: Long): Flow<SessionWithSets?> =
        workoutSessionDao.getSessionWithSetsById(sessionId)

    override fun getActiveSession(): Flow<WorkoutSession?> =
        workoutSessionDao.getActiveSession()

    override suspend fun insertSession(session: WorkoutSession): Long =
        workoutSessionDao.insertSession(session)

    override suspend fun updateSession(session: WorkoutSession) =
        workoutSessionDao.updateSession(session)

    override suspend fun deleteSession(session: WorkoutSession) =
        workoutSessionDao.deleteSession(session)

    override suspend fun deleteSessionById(sessionId: Long) =
        workoutSessionDao.deleteSessionById(sessionId)

    override suspend fun deleteAllSessions() =
        workoutSessionDao.deleteAllSessions()

    override suspend fun insertLoggedSet(loggedSet: LoggedSet): Long =
        workoutSessionDao.insertLoggedSet(loggedSet)

    override suspend fun insertLoggedSets(loggedSets: List<LoggedSet>) =
        workoutSessionDao.insertLoggedSets(loggedSets)

    override suspend fun updateLoggedSet(loggedSet: LoggedSet) =
        workoutSessionDao.updateLoggedSet(loggedSet)

    override suspend fun deleteLoggedSet(loggedSet: LoggedSet) =
        workoutSessionDao.deleteLoggedSet(loggedSet)

    override suspend fun deleteLoggedSetsForSession(sessionId: Long) =
        workoutSessionDao.deleteLoggedSetsForSession(sessionId)

    override suspend fun saveCompletedWorkoutSession(
        session: WorkoutSession,
        loggedSets: List<LoggedSet>
    ): Long {
        val sessionId = workoutSessionDao.insertSession(session)
        val setsWithSessionId = loggedSets.map { it.copy(sessionId = sessionId) }
        workoutSessionDao.insertLoggedSets(setsWithSessionId)
        return sessionId
    }
}
