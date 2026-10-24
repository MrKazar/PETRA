package com.example.porjectpetra.data.repository

import com.example.porjectpetra.data.local.dao.PlanDao
import com.example.porjectpetra.data.local.entity.PlanOverride
import com.example.porjectpetra.data.local.entity.PlanSlot
import com.example.porjectpetra.data.model.PlannedWorkout
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getSlots(): Flow<List<PlanSlot>>
    fun getOverridesBetween(startDay: Long, endDay: Long): Flow<List<PlanOverride>>

    /** Ajoute une séance au cycle : chaque semaine A (ou B), tel jour. */
    suspend fun addToCycle(weekIndex: Int, dayIndex: Int, programId: Long, timeMinutes: Int?)
    suspend fun removeFromCycle(slotId: Long)

    /** Ajoute une séance pour ce jour seulement. */
    suspend fun addForDay(epochDay: Long, programId: Long, timeMinutes: Int?)

    /** Annule (ou rétablit) une séance du cycle pour ce jour seulement. */
    suspend fun setCancelledForDay(planned: PlannedWorkout, cancelled: Boolean)

    /** Retire une séance ajoutée pour un jour. */
    suspend fun removeDayOnly(overrideId: Long)
}

class PlanRepositoryImpl(private val dao: PlanDao) : PlanRepository {
    override fun getSlots(): Flow<List<PlanSlot>> = dao.getSlots()

    override fun getOverridesBetween(startDay: Long, endDay: Long): Flow<List<PlanOverride>> =
        dao.getOverridesBetween(startDay, endDay)

    override suspend fun addToCycle(weekIndex: Int, dayIndex: Int, programId: Long, timeMinutes: Int?) {
        dao.insertSlot(PlanSlot(weekIndex = weekIndex, dayIndex = dayIndex, programId = programId, timeMinutes = timeMinutes))
    }

    override suspend fun removeFromCycle(slotId: Long) = dao.deleteSlot(slotId)

    override suspend fun addForDay(epochDay: Long, programId: Long, timeMinutes: Int?) {
        dao.insertOverride(PlanOverride(epochDay = epochDay, programId = programId, timeMinutes = timeMinutes))
    }

    override suspend fun setCancelledForDay(planned: PlannedWorkout, cancelled: Boolean) {
        val slotId = planned.slotId ?: return
        dao.deleteSlotCancellation(slotId, planned.epochDay)
        if (cancelled) dao.insertOverride(PlanOverride(epochDay = planned.epochDay, slotId = slotId))
    }

    override suspend fun removeDayOnly(overrideId: Long) = dao.deleteOverride(overrideId)
}
