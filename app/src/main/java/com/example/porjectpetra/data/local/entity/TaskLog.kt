package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * État d'une tâche pour un jour donné (fait / sauté / reporté).
 * Absence de ligne = tâche encore à faire (ou non faite ce jour-là).
 */
@Entity(
    tableName = "task_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId", "epochDay"], unique = true), Index("epochDay")]
)
data class TaskLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val epochDay: Long,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
)
