package com.example.porjectpetra.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class LoggedSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long?,
    val exerciseName: String,
    val setNumber: Int,
    val repsCompleted: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
    val calories: Int = 0,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    /** Phase du cycle (WARMUP, WORK, HYPERTROPHY) ; vide pour un exercice hors catalogue. */
    @ColumnInfo(defaultValue = "")
    val phase: String = ""
)
