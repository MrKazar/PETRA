package com.example.porjectpetra.data.model

import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog

/** Une tâche prévue pour un jour donné, avec son état ce jour-là. */
data class PlannedTask(
    val task: DailyTask,
    val routine: Routine?,
    val status: TaskStatus?,
    /** Vrai si la tâche a été reportée la veille et réapparaît aujourd'hui. */
    val carriedOver: Boolean
)

/**
 * Bilan d'une période. Les tâches sautées ou reportées ne sont pas comptées comme des échecs :
 * elles sont retirées du total attendu.
 */
data class CompletionSummary(
    val done: Int,
    val expected: Int,
    val skipped: Int,
    val postponed: Int
) {
    val rate: Float get() = if (expected == 0) 0f else done.toFloat() / expected
    val remaining: Int get() = (expected - done).coerceAtLeast(0)
}

/** Logique pure (sans Android) de planification des tâches : testable en tests unitaires. */
object DailyPlanner {

    fun isDue(task: DailyTask, routine: Routine?, epochDay: Long): Boolean {
        if (task.createdEpochDay > epochDay) return false
        if (task.routineId != null && routine == null) return false
        return when (Recurrence.from(task.recurrence)) {
            Recurrence.ONCE -> task.oneOffEpochDay == epochDay
            Recurrence.WEEKLY -> {
                val day = PetraDates.dayIndex(epochDay)
                DayMask.contains(task.activeDays, day) &&
                    (routine == null || DayMask.contains(routine.activeDays, day))
            }
        }
    }

    /**
     * Tâches du jour [epochDay], ordonnées pour l'affichage.
     * [logs] doit contenir au moins les journaux du jour et de la veille (pour les reports).
     */
    fun planDay(
        epochDay: Long,
        tasks: List<DailyTask>,
        routines: List<Routine>,
        logs: List<TaskLog>
    ): List<PlannedTask> {
        val routinesById = routines.associateBy { it.id }
        val todayLogs = logs.filter { it.epochDay == epochDay }.associateBy { it.taskId }
        val postponedYesterday = logs
            .filter { it.epochDay == epochDay - 1 && TaskStatus.from(it.status) == TaskStatus.POSTPONED }
            .map { it.taskId }
            .toSet()

        return tasks.mapNotNull { task ->
            val routine = task.routineId?.let { routinesById[it] }
            if (task.routineId != null && routine == null) return@mapNotNull null
            val due = isDue(task, routine, epochDay)
            val carried = task.id in postponedYesterday
            if (!due && !carried) return@mapNotNull null
            PlannedTask(
                task = task,
                routine = routine,
                status = todayLogs[task.id]?.let { TaskStatus.from(it.status) },
                carriedOver = carried && !due
            )
        }.sortedWith(
            compareBy<PlannedTask>(
                { sortMinutes(it) },
                { it.routine?.orderIndex ?: Int.MAX_VALUE },
                { it.routine?.id ?: Long.MAX_VALUE },
                { it.task.orderIndex },
                { it.task.id }
            )
        )
    }

    fun summarize(planned: List<PlannedTask>): CompletionSummary {
        val done = planned.count { it.status == TaskStatus.DONE }
        val skipped = planned.count { it.status == TaskStatus.SKIPPED }
        val postponed = planned.count { it.status == TaskStatus.POSTPONED }
        return CompletionSummary(
            done = done,
            expected = planned.size - skipped - postponed,
            skipped = skipped,
            postponed = postponed
        )
    }

    /** Bilan cumulé sur les jours [fromDay]..[toDay] inclus. */
    fun summarizeRange(
        fromDay: Long,
        toDay: Long,
        tasks: List<DailyTask>,
        routines: List<Routine>,
        logs: List<TaskLog>
    ): CompletionSummary {
        var done = 0
        var expected = 0
        var skipped = 0
        var postponed = 0
        for (day in fromDay..toDay) {
            val s = summarize(planDay(day, tasks, routines, logs))
            done += s.done
            expected += s.expected
            skipped += s.skipped
            postponed += s.postponed
        }
        return CompletionSummary(done, expected, skipped, postponed)
    }

    /** Première tâche encore à faire, dans l'ordre de la journée. */
    fun nextPending(planned: List<PlannedTask>): PlannedTask? = planned.firstOrNull { it.status == null }

    /** Position de la tâche dans la journée : son horaire, sinon celui de sa routine, sinon l'événement. */
    fun sortMinutes(item: PlannedTask): Int {
        val task = item.task
        if (TriggerType.from(task.triggerType) == TriggerType.SCHEDULED && task.scheduledMinutes != null) {
            return task.scheduledMinutes
        }
        val routine = item.routine
        if (routine != null) {
            return when (TriggerType.from(routine.triggerType)) {
                TriggerType.SCHEDULED -> routine.scheduledMinutes ?: 720
                TriggerType.EVENT -> DayEvents.approximateMinutes(routine.triggerEvent)
                TriggerType.ANYTIME -> 720
            }
        }
        return when (TriggerType.from(task.triggerType)) {
            TriggerType.EVENT -> DayEvents.approximateMinutes(task.triggerEvent)
            else -> 720
        }
    }

    /** Libellé court du déclencheur : « 21:30 », « Après le réveil », « Dans la journée ». */
    fun triggerLabel(triggerType: String, scheduledMinutes: Int?, triggerEvent: String): String =
        when (TriggerType.from(triggerType)) {
            TriggerType.SCHEDULED -> scheduledMinutes?.let { PetraDates.formatMinutes(it) } ?: "Heure à définir"
            TriggerType.EVENT -> triggerEvent.ifBlank { "Événement à définir" }
            TriggerType.ANYTIME -> TriggerType.ANYTIME.label
        }
}
