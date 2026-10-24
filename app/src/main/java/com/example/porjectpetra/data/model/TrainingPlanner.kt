package com.example.porjectpetra.data.model

import com.example.porjectpetra.data.local.entity.PlanOverride
import com.example.porjectpetra.data.local.entity.PlanSlot

/** Une séance prévue un jour donné, issue du cycle A/B ou ajoutée pour ce jour seulement. */
data class PlannedWorkout(
    val epochDay: Long,
    val programId: Long,
    val timeMinutes: Int?,
    /** Séance du cycle (récurrente) ; null pour une séance ajoutée ce jour-là. */
    val slotId: Long?,
    /** Exception qui a ajouté cette séance ; null pour une séance du cycle. */
    val overrideId: Long?,
    /** Séance du cycle annulée pour ce jour seulement (affichée barrée, non comptée). */
    val isCancelled: Boolean = false
) {
    val isRecurring: Boolean get() = slotId != null
}

/** Logique pure du planning sur deux semaines (testable sans Android). */
object TrainingPlanner {
    const val WEEK_A = 0
    const val WEEK_B = 1

    fun weekLabel(weekIndex: Int): String = if (weekIndex == WEEK_A) "A" else "B"

    /** Semaine A ou B du jour [epochDay], [anchorMonday] étant un lundi de semaine A. */
    fun weekIndex(epochDay: Long, anchorMonday: Long): Int =
        Math.floorMod(Math.floorDiv(PetraDates.startOfWeek(epochDay) - anchorMonday, 7L), 2L).toInt()

    /** Séances prévues le jour [epochDay], annulées comprises (marquées), triées par heure. */
    fun planDay(
        epochDay: Long,
        anchorMonday: Long,
        slots: List<PlanSlot>,
        overrides: List<PlanOverride>
    ): List<PlannedWorkout> {
        val week = weekIndex(epochDay, anchorMonday)
        val day = PetraDates.dayIndex(epochDay)
        val dayOverrides = overrides.filter { it.epochDay == epochDay }
        val cancelledSlots = dayOverrides.mapNotNull { o -> o.slotId.takeIf { o.programId == null } }.toSet()

        val fromCycle = slots.filter { it.weekIndex == week && it.dayIndex == day }.map {
            PlannedWorkout(epochDay, it.programId, it.timeMinutes, slotId = it.id, overrideId = null, isCancelled = it.id in cancelledSlots)
        }
        val extras = dayOverrides.filter { it.programId != null }.map {
            PlannedWorkout(epochDay, it.programId!!, it.timeMinutes, slotId = null, overrideId = it.id)
        }
        return (fromCycle + extras).sortedWith(compareBy({ it.timeMinutes ?: Int.MAX_VALUE }, { it.programId }))
    }

    /** Séances réellement prévues (hors annulations) sur [fromDay]..[toDay]. */
    fun planRange(
        fromDay: Long,
        toDay: Long,
        anchorMonday: Long,
        slots: List<PlanSlot>,
        overrides: List<PlanOverride>
    ): List<PlannedWorkout> =
        (fromDay..toDay).flatMap { planDay(it, anchorMonday, slots, overrides) }.filterNot { it.isCancelled }

    /**
     * Séances prévues déjà faites : une séance terminée ce jour-là avec le même programme.
     * [completedByDay] : jour → programmes des séances terminées ce jour-là.
     */
    fun isDone(planned: PlannedWorkout, completedByDay: Map<Long, List<Long>>): Boolean =
        completedByDay[planned.epochDay].orEmpty().contains(planned.programId)
}
