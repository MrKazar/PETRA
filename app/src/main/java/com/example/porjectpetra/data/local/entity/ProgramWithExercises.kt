package com.example.porjectpetra.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ProgramWithExercises(
    @Embedded val program: WorkoutProgram,
    @Relation(
        parentColumn = "id",
        entityColumn = "programId"
    )
    val exercises: List<Exercise>
)
