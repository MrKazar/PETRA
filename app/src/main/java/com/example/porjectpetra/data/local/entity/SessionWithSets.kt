package com.example.porjectpetra.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithSets(
    @Embedded val session: WorkoutSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val loggedSets: List<LoggedSet>
)
