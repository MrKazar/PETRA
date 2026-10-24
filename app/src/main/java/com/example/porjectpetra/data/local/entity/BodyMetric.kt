package com.example.porjectpetra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double = 0.0,
    val bodyFatPercentage: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val armsCm: Double? = null,
    val thighsCm: Double? = null,
    val notes: String = ""
)
