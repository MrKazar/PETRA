package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.TriggerType

/** Routine réutilisable : une liste ordonnée de tâches (voir [DailyTask.routineId]). */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val triggerType: String = TriggerType.EVENT.name,
    /** Heure de la routine en minutes depuis minuit, si [triggerType] = SCHEDULED. */
    val scheduledMinutes: Int? = null,
    /** Événement déclencheur, si [triggerType] = EVENT (ex. « Après le réveil »). */
    val triggerEvent: String = "",
    val activeDays: Int = DayMask.ALL_DAYS,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
