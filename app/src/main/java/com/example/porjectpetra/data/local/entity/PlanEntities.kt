package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Séance du cycle d'entraînement sur deux semaines : programme prévu tel jour de la semaine A ou B.
 * Le cycle se répète indéfiniment (A, B, A, B…).
 */
@Entity(
    tableName = "plan_slots",
    foreignKeys = [
        ForeignKey(entity = WorkoutProgram::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("programId")]
)
data class PlanSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 0 = semaine A, 1 = semaine B. */
    val weekIndex: Int,
    /** 0 = lundi … 6 = dimanche. */
    val dayIndex: Int,
    val programId: Long,
    /** Heure prévue (minutes depuis minuit), facultative. */
    val timeMinutes: Int? = null
)

/**
 * Exception datée au cycle : séance ajoutée pour ce jour seulement ([programId] non nul, [slotId] nul),
 * ou séance du cycle annulée pour ce jour seulement ([slotId] non nul, [programId] nul).
 */
@Entity(
    tableName = "plan_overrides",
    foreignKeys = [
        ForeignKey(entity = WorkoutProgram::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlanSlot::class, parentColumns = ["id"], childColumns = ["slotId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("epochDay"), Index("programId"), Index("slotId")]
)
data class PlanOverride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epochDay: Long,
    val programId: Long? = null,
    val slotId: Long? = null,
    val timeMinutes: Int? = null
)
