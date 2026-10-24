package com.example.porjectpetra.data.model

import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.local.entity.TaskLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DailyPlannerTest {

    // 2026-10-19 est un lundi : epochDay = 20745
    private val monday = 20745L
    private val saturday = monday + 5

    private fun task(
        id: Long,
        routineId: Long? = null,
        days: Int = DayMask.ALL_DAYS,
        recurrence: Recurrence = Recurrence.WEEKLY,
        oneOff: Long? = null,
        created: Long = monday - 30,
        order: Int = 0,
        trigger: TriggerType = TriggerType.ANYTIME,
        minutes: Int? = null
    ) = DailyTask(
        id = id, routineId = routineId, title = "T$id", activeDays = days, recurrence = recurrence.name,
        oneOffEpochDay = oneOff, createdEpochDay = created, orderIndex = order,
        triggerType = trigger.name, scheduledMinutes = minutes
    )

    private fun log(taskId: Long, day: Long, status: TaskStatus) =
        TaskLog(taskId = taskId, epochDay = day, status = status.name)

    @Test
    fun dayIndex_mondayIsZero_sundayIsSix() {
        assertEquals(0, PetraDates.dayIndex(monday))
        assertEquals(6, PetraDates.dayIndex(monday + 6))
        assertEquals(0, PetraDates.dayIndex(monday + 7))
        assertEquals(monday, PetraDates.startOfWeek(saturday))
    }

    @Test
    fun epochDay_usesLocalTimeZone_aroundMidnight() {
        val paris = TimeZone.getTimeZone("Europe/Paris")
        // 2026-10-19 23:30 UTC = 2026-10-20 01:30 à Paris (UTC+2 en octobre)
        val millis = monday * PetraDates.DAY_MS + (23 * 60 + 30) * 60_000L
        assertEquals(monday, PetraDates.epochDay(millis, TimeZone.getTimeZone("UTC")))
        assertEquals(monday + 1, PetraDates.epochDay(millis, paris))
    }

    @Test
    fun weeklyTask_dueOnlyOnActiveDays() {
        val weekdays = task(1, days = DayMask.WEEKDAYS)
        assertTrue(DailyPlanner.isDue(weekdays, null, monday))
        assertFalse(DailyPlanner.isDue(weekdays, null, saturday))
    }

    @Test
    fun routineTask_requiresRoutineAndTaskActive() {
        val weekendRoutine = Routine(id = 10, name = "Week-end", activeDays = 0b1100000)
        val t = task(1, routineId = 10)
        assertFalse(DailyPlanner.isDue(t, weekendRoutine, monday))
        assertTrue(DailyPlanner.isDue(t, weekendRoutine, saturday))
        // Routine supprimée : la tâche n'est plus due
        assertFalse(DailyPlanner.isDue(t, null, saturday))
    }

    @Test
    fun oneOffTask_dueOnlyOnItsDate() {
        val t = task(1, recurrence = Recurrence.ONCE, oneOff = monday + 1)
        assertFalse(DailyPlanner.isDue(t, null, monday))
        assertTrue(DailyPlanner.isDue(t, null, monday + 1))
        assertFalse(DailyPlanner.isDue(t, null, monday + 2))
    }

    @Test
    fun task_notDueBeforeCreation() {
        val t = task(1, created = monday + 2)
        assertFalse(DailyPlanner.isDue(t, null, monday + 1))
        assertTrue(DailyPlanner.isDue(t, null, monday + 2))
    }

    @Test
    fun postponedTask_reappearsNextDay_evenIfNotScheduled() {
        val onlyMonday = task(1, days = 0b0000001)
        val planTuesday = DailyPlanner.planDay(
            monday + 1, listOf(onlyMonday), emptyList(), listOf(log(1, monday, TaskStatus.POSTPONED))
        )
        assertEquals(1, planTuesday.size)
        assertTrue(planTuesday.single().carriedOver)
        assertNull(planTuesday.single().status)

        // Sans report, elle n'apparaît pas le mardi
        assertTrue(DailyPlanner.planDay(monday + 1, listOf(onlyMonday), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun summary_excludesSkippedAndPostponedFromExpected() {
        val tasks = (1L..4L).map { task(it) }
        val logs = listOf(
            log(1, monday, TaskStatus.DONE),
            log(2, monday, TaskStatus.SKIPPED),
            log(3, monday, TaskStatus.POSTPONED)
        )
        val summary = DailyPlanner.summarize(DailyPlanner.planDay(monday, tasks, emptyList(), logs))
        assertEquals(1, summary.done)
        assertEquals(2, summary.expected)
        assertEquals(1, summary.skipped)
        assertEquals(1, summary.postponed)
        assertEquals(0.5f, summary.rate)
    }

    @Test
    fun logsFromOtherDays_doNotLeakIntoToday() {
        val tasks = listOf(task(1))
        val plan = DailyPlanner.planDay(monday + 1, tasks, emptyList(), listOf(log(1, monday, TaskStatus.DONE)))
        assertNull(plan.single().status)
    }

    @Test
    fun planDay_ordersByTimeThenRoutineOrder() {
        val morning = Routine(id = 1, name = "Matin", triggerType = TriggerType.EVENT.name, triggerEvent = "Après le réveil")
        val evening = Routine(id = 2, name = "Soir", triggerType = TriggerType.SCHEDULED.name, scheduledMinutes = 21 * 60)
        val tasks = listOf(
            task(1, routineId = 2, order = 0),
            task(2, routineId = 1, order = 1),
            task(3, routineId = 1, order = 0),
            task(4, trigger = TriggerType.SCHEDULED, minutes = 12 * 60)
        )
        val ids = DailyPlanner.planDay(monday, tasks, listOf(morning, evening), emptyList()).map { it.task.id }
        assertEquals(listOf(3L, 2L, 4L, 1L), ids)
    }

    @Test
    fun nextPending_skipsDoneSkippedAndPostponed() {
        val tasks = (1L..3L).map { task(it, order = it.toInt()) }
        val logs = listOf(log(1, monday, TaskStatus.DONE), log(2, monday, TaskStatus.SKIPPED))
        val plan = DailyPlanner.planDay(monday, tasks, emptyList(), logs)
        assertEquals(3L, DailyPlanner.nextPending(plan)?.task?.id)
    }

    @Test
    fun summarizeRange_addsUpEachDay() {
        val weekdays = task(1, days = DayMask.WEEKDAYS)
        val logs = listOf(log(1, monday, TaskStatus.DONE), log(1, monday + 1, TaskStatus.DONE))
        // Lundi → samedi : 5 jours ouvrés prévus, 2 faits
        val summary = DailyPlanner.summarizeRange(monday, saturday, listOf(weekdays), emptyList(), logs)
        assertEquals(2, summary.done)
        assertEquals(5, summary.expected)
    }

    @Test
    fun dayMask_toggleAndDescribe() {
        assertEquals("Tous les jours", DayMask.describe(DayMask.ALL_DAYS))
        assertEquals("En semaine", DayMask.describe(DayMask.toggle(DayMask.toggle(DayMask.ALL_DAYS, 5), 6)))
        assertEquals("Le week-end", DayMask.describe(0b1100000))
    }
}
