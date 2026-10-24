package com.example.porjectpetra.ui.workout

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.data.local.entity.LoggedSet
import com.example.porjectpetra.ui.components.ComingSoonNote
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.StatTile
import com.example.porjectpetra.ui.theme.PetraBlack
import com.example.porjectpetra.ui.theme.PetraKhaki
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    viewModel: WorkoutHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val dateFormat = SimpleDateFormat("EEEE d MMMM · HH:mm", Locale.FRENCH)

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("HISTORIQUE DES SÉANCES", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PetraBlack)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.sessions.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatTile("${state.sessions.size}", "Séances enregistrées", Modifier.weight(1f))
                        StatTile(
                            "${state.sessions.sumOf { it.volumeKg }.toInt()} kg",
                            "Volume total",
                            Modifier.weight(1f)
                        )
                    }
                }
            }
            if (!state.isLoading && state.sessions.isEmpty()) {
                item {
                    PetraCard {
                        Text("Aucune séance enregistrée.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Termine une séance et enregistre-la : elle apparaîtra ici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PetraTextMuted
                        )
                    }
                }
            }
            items(state.sessions, key = { it.id }) { session ->
                val expanded = expandedId == session.id
                PetraCard(onClick = { expandedId = if (expanded) null else session.id }) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        dateFormat.format(Date(session.startTime)).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${session.durationMinutes} min · ${session.completedSets}/${session.totalSets} séries" +
                            if (session.volumeKg > 0) " · ${session.volumeKg.toInt()} kg soulevés" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (session.totalSets > 0 && session.completedSets == session.totalSets) PetraSuccess else PetraKhaki
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column(Modifier.padding(top = 8.dp)) {
                            session.exercises.forEach { ex ->
                                Text(ex.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                                ex.sets.forEach { set ->
                                    Text(
                                        describeSet(set),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (set.isCompleted) MaterialTheme.colorScheme.onSurface else PetraTextMuted
                                    )
                                }
                            }
                            if (session.notes.isNotBlank()) {
                                Text("Remarques : ${session.notes}", style = MaterialTheme.typography.bodySmall, color = PetraTextMuted, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            item { ComingSoonNote("graphiques de progression et records personnels.") }
        }
    }
}

private fun describeSet(set: LoggedSet): String {
    val parts = buildList {
        if (set.repsCompleted > 0) add("${set.repsCompleted} rép.")
        if (set.weightKg > 0) add("${formatNumber(set.weightKg)} kg")
        if (set.durationSeconds > 0) add("${set.durationSeconds / 60} min ${set.durationSeconds % 60} s")
        if (set.distanceKm > 0) add("${formatNumber(set.distanceKm)} km")
    }
    val status = if (set.isCompleted) "✓" else "—"
    return "$status Série ${set.setNumber} : ${parts.joinToString(" · ").ifEmpty { "non renseignée" }}"
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.FRENCH, "%.1f", value)
