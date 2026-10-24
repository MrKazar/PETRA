package com.example.porjectpetra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    // Routines
    @Query("SELECT * FROM routines ORDER BY orderIndex ASC, id ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutineById(routineId: Long): Flow<Routine?>

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun getRoutineCount(): Int

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM routines")
    suspend fun getMaxRoutineOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    // Tâches
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllTasks(): Flow<List<DailyTask>>

    @Query("SELECT * FROM tasks WHERE routineId = :routineId ORDER BY orderIndex ASC, id ASC")
    fun getTasksForRoutine(routineId: Long): Flow<List<DailyTask>>

    @Query("SELECT * FROM tasks WHERE routineId = :routineId ORDER BY orderIndex ASC, id ASC")
    suspend fun getTasksForRoutineOnce(routineId: Long): List<DailyTask>

    @Query("SELECT * FROM tasks WHERE routineId IS NULL ORDER BY orderIndex ASC, id ASC")
    suspend fun getStandaloneTasksOnce(): List<DailyTask>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM tasks WHERE routineId = :routineId")
    suspend fun getMaxTaskOrderInRoutine(routineId: Long): Int

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM tasks WHERE routineId IS NULL")
    suspend fun getMaxStandaloneTaskOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: DailyTask): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTasks(tasks: List<DailyTask>)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Update
    suspend fun updateTasks(tasks: List<DailyTask>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    // Journal de réalisation
    @Query("SELECT * FROM task_logs WHERE epochDay BETWEEN :startDay AND :endDay")
    fun getLogsBetween(startDay: Long, endDay: Long): Flow<List<TaskLog>>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND epochDay = :epochDay LIMIT 1")
    suspend fun getLog(taskId: Long, epochDay: Long): TaskLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: TaskLog)

    @Query("DELETE FROM task_logs WHERE taskId = :taskId AND epochDay = :epochDay")
    suspend fun deleteLog(taskId: Long, epochDay: Long)

    @Query("DELETE FROM task_logs")
    suspend fun deleteAllLogs()

    @Transaction
    suspend fun insertRoutineWithTasks(routine: Routine, tasks: List<DailyTask>): Long {
        val routineId = insertRoutine(routine)
        insertTasks(tasks.mapIndexed { index, task -> task.copy(routineId = routineId, orderIndex = index) })
        return routineId
    }
}
