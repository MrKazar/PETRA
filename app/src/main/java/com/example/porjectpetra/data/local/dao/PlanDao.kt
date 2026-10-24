package com.example.porjectpetra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.porjectpetra.data.local.entity.PlanOverride
import com.example.porjectpetra.data.local.entity.PlanSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_slots ORDER BY weekIndex, dayIndex, timeMinutes, id")
    fun getSlots(): Flow<List<PlanSlot>>

    @Insert
    suspend fun insertSlot(slot: PlanSlot): Long

    @Query("DELETE FROM plan_slots WHERE id = :slotId")
    suspend fun deleteSlot(slotId: Long)

    @Query("SELECT * FROM plan_overrides WHERE epochDay BETWEEN :startDay AND :endDay")
    fun getOverridesBetween(startDay: Long, endDay: Long): Flow<List<PlanOverride>>

    @Insert
    suspend fun insertOverride(override: PlanOverride): Long

    @Query("DELETE FROM plan_overrides WHERE id = :overrideId")
    suspend fun deleteOverride(overrideId: Long)

    @Query("DELETE FROM plan_overrides WHERE slotId = :slotId AND epochDay = :epochDay")
    suspend fun deleteSlotCancellation(slotId: Long, epochDay: Long)
}
