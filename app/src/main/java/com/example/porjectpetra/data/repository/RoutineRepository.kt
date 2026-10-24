package com.example.porjectpetra.data.repository

import com.example.porjectpetra.data.local.dao.RoutineDao
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.TaskStatus
import com.example.porjectpetra.data.model.TriggerType
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getAllRoutines(): Flow<List<Routine>>
    fun getRoutineById(routineId: Long): Flow<Routine?>
    fun getAllTasks(): Flow<List<DailyTask>>
    fun getTasksForRoutine(routineId: Long): Flow<List<DailyTask>>
    fun getLogsBetween(startDay: Long, endDay: Long): Flow<List<TaskLog>>

    /** Crée la routine si [Routine.id] = 0, sinon la met à jour. Retourne son id. */
    suspend fun saveRoutine(routine: Routine): Long
    suspend fun deleteRoutine(routineId: Long)

    /** Crée la tâche (placée en fin de liste) si [DailyTask.id] = 0, sinon la met à jour. */
    suspend fun saveTask(task: DailyTask): Long
    suspend fun deleteTask(taskId: Long)

    /** Déplace une tâche d'un cran dans sa liste (routine ou tâches indépendantes). */
    suspend fun moveTask(task: DailyTask, up: Boolean)

    /** Enregistre l'état d'une tâche pour un jour ; null = revenir à « à faire ». */
    suspend fun setTaskStatus(taskId: Long, epochDay: Long, status: TaskStatus?)
    suspend fun clearTaskHistory()

    /** Crée la routine du matin d'exemple si aucune routine n'existe. */
    suspend fun seedDefaultRoutinesIfEmpty()
}

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao
) : RoutineRepository {
    override fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()

    override fun getRoutineById(routineId: Long): Flow<Routine?> = routineDao.getRoutineById(routineId)

    override fun getAllTasks(): Flow<List<DailyTask>> = routineDao.getAllTasks()

    override fun getTasksForRoutine(routineId: Long): Flow<List<DailyTask>> =
        routineDao.getTasksForRoutine(routineId)

    override fun getLogsBetween(startDay: Long, endDay: Long): Flow<List<TaskLog>> =
        routineDao.getLogsBetween(startDay, endDay)

    override suspend fun saveRoutine(routine: Routine): Long =
        if (routine.id == 0L) {
            routineDao.insertRoutine(routine.copy(orderIndex = routineDao.getMaxRoutineOrder() + 1))
        } else {
            routineDao.updateRoutine(routine)
            routine.id
        }

    override suspend fun deleteRoutine(routineId: Long) = routineDao.deleteRoutineById(routineId)

    override suspend fun saveTask(task: DailyTask): Long =
        if (task.id == 0L) {
            val maxOrder = task.routineId?.let { routineDao.getMaxTaskOrderInRoutine(it) }
                ?: routineDao.getMaxStandaloneTaskOrder()
            routineDao.insertTask(task.copy(orderIndex = maxOrder + 1))
        } else {
            routineDao.updateTask(task)
            task.id
        }

    override suspend fun deleteTask(taskId: Long) = routineDao.deleteTaskById(taskId)

    override suspend fun moveTask(task: DailyTask, up: Boolean) {
        val siblings = task.routineId?.let { routineDao.getTasksForRoutineOnce(it) }
            ?: routineDao.getStandaloneTasksOnce()
        val index = siblings.indexOfFirst { it.id == task.id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in siblings.indices) return
        val reordered = siblings.toMutableList().apply { add(target, removeAt(index)) }
        // Renumérote toute la liste : corrige aussi d'éventuels doublons d'orderIndex.
        routineDao.updateTasks(reordered.mapIndexed { i, t -> t.copy(orderIndex = i) })
    }

    override suspend fun setTaskStatus(taskId: Long, epochDay: Long, status: TaskStatus?) {
        if (status == null) {
            routineDao.deleteLog(taskId, epochDay)
        } else {
            val existing = routineDao.getLog(taskId, epochDay)
            routineDao.upsertLog(
                TaskLog(id = existing?.id ?: 0, taskId = taskId, epochDay = epochDay, status = status.name)
            )
        }
    }

    override suspend fun clearTaskHistory() = routineDao.deleteAllLogs()

    override suspend fun seedDefaultRoutinesIfEmpty() {
        if (routineDao.getRoutineCount() > 0) return
        routineDao.insertRoutineWithTasks(
            Routine(
                name = "Routine du matin",
                description = "Bien démarrer la journée. Modifie les étapes et les durées selon ton rythme.",
                triggerType = TriggerType.EVENT.name,
                triggerEvent = "Après le réveil",
                activeDays = DayMask.ALL_DAYS
            ),
            listOf(
                DailyTask(title = "Se lever à l'heure prévue", estimatedMinutes = 1),
                DailyTask(title = "Faire son lit", description = "Dans les deux minutes qui suivent le lever.", estimatedMinutes = 2),
                DailyTask(title = "S'habiller et se préparer", estimatedMinutes = 15),
                DailyTask(title = "Ranger rapidement son espace", estimatedMinutes = 5),
                DailyTask(title = "Commencer la première tâche de la journée", estimatedMinutes = 0)
            )
        )
    }
}
