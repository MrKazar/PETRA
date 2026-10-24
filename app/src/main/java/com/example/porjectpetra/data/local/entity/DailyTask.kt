package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.Recurrence
import com.example.porjectpetra.data.model.TriggerType

/**
 * Tâche quotidienne. Elle appartient à une routine ([routineId]) ou est indépendante (null).
 * Une tâche de routine n'est due que les jours où sa routine ET la tâche sont actives.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val title: String,
    val description: String = "",
    val triggerType: String = TriggerType.ANYTIME.name,
    val scheduledMinutes: Int? = null,
    val triggerEvent: String = "",
    val estimatedMinutes: Int = 0,
    val activeDays: Int = DayMask.ALL_DAYS,
    val recurrence: String = Recurrence.WEEKLY.name,
    /** Date de la tâche si [recurrence] = ONCE. */
    val oneOffEpochDay: Long? = null,
    val orderIndex: Int = 0,
    /** Une tâche n'est jamais comptée avant le jour de sa création. */
    val createdEpochDay: Long = PetraDates.epochDay()
)
