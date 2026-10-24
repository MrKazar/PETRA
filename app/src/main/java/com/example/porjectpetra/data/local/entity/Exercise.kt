package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutProgram::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("programId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val muscleGroup: String = "Corps entier",
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double = 0.0,
    val targetDurationSeconds: Int = 0,
    val restTimeSeconds: Int = 60,
    val orderIndex: Int = 0,
    val notes: String = "",
    /**
     * Identifiant dans le catalogue (assets/exercices.json). Non nul : l'exercice suit le cycle fixe
     * chauffe / travail / hypertrophie du catalogue, et [targetWeightKg] est la charge de travail.
     */
    val catalogId: String? = null
)
