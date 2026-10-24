package com.example.porjectpetra.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.FitnessCenter
import com.example.porjectpetra.ui.components.PetraActionCard
import androidx.compose.foundation.layout.width
import com.example.porjectpetra.ui.components.PetraStateMessage
import com.example.porjectpetra.ui.components.PetraStateKind
import com.example.porjectpetra.ui.components.PetraSecondaryButton
import com.example.porjectpetra.ui.components.PetraProgressRing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.model.DailyPlanner
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraPrimaryButton
import com.example.porjectpetra.ui.components.PetraProgressBar
import com.example.porjectpetra.ui.components.PetraScreenHeader
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.components.StatTile
import com.example.porjectpetra.ui.routine.PlannedTaskRow
import com.example.porjectpetra.ui.routine.components.TaskEditorDialog
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenRoutine: (routineId: Long) -> Unit,
    onOpenRoutines: () -> Unit,
    onStartWorkout: (programId: Long?) -> Unit,
    onOpenSport: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlan: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTask: DailyTask? = editingTaskId?.let { id ->
        state.sections.flatMap { it.tasks }.firstOrNull { it.task.id == id }?.task
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingTaskId = null; editorOpen = true },
                containerColor = PetraOlive,
                contentColor = PetraText,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("TÂCHE", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PetraScreenHeader(
                    title = "PETRA — Aujourd'hui",
                    subtitle = PetraDates.formatLongDate(state.today).replaceFirstChar { it.uppercase() },
                    contentPadding = PaddingValues(vertical = 12.dp)
                )
                Text(
                    text = greeting(state.displayName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PetraTextMuted
                )
            }

            item { DailyProgressCard(state) }

            if (state.isLoading) {
                item { PetraStateMessage(kind = PetraStateKind.LOADING) }
            }

            if (!state.isLoading && state.sections.isEmpty()) {
                item {
                    PetraCard {
                        PetraStateMessage(
                            kind = PetraStateKind.EMPTY,
                            title = "Aucune tâche prévue aujourd'hui",
                            message = "Ajoute une tâche avec le bouton « Tâche » ou prépare une routine.",
                            action = { PetraSecondaryButton("Voir les routines", onClick = onOpenRoutines) }
                        )
                    }
                }
            }

            items(state.sections, key = { "section_${it.routine?.id ?: "standalone"}" }) { section ->
                PetraCard {
                    val routine = section.routine
                    SectionHeader(
                        title = routine?.name ?: "Tâches du jour",
                        trailing = {
                            Text(
                                "${section.summary.done}/${section.summary.expected}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (section.summary.expected > 0 && section.summary.remaining == 0) PetraSuccess else PetraTextMuted
                            )
                        }
                    )
                    if (routine != null) {
                        Text(
                            DailyPlanner.triggerLabel(routine.triggerType, routine.scheduledMinutes, routine.triggerEvent),
                            style = MaterialTheme.typography.bodySmall,
                            color = PetraTextMuted
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    section.tasks.forEach { item ->
                        PlannedTaskRow(
                            item = item,
                            onSetStatus = viewModel::setTaskStatus,
                            showTrigger = true,
                            onEdit = if (item.task.routineId == null) {
                                { editingTaskId = it.task.id; editorOpen = true }
                            } else {
                                { onOpenRoutine(it.task.routineId!!) }
                            }
                        )
                    }
                    if (routine != null) {
                        TextButton(onClick = { onOpenRoutine(routine.id) }) {
                            Text("GÉRER LA ROUTINE")
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            item { WorkoutCard(state, onStartWorkout = onStartWorkout, onOpenSport = onOpenSport, onOpenPlan = onOpenPlan) }
            item { WeekCard(state) }
        }
    }

    if (editorOpen) {
        TaskEditorDialog(
            initial = editingTask,
            routineId = null,
            today = state.today,
            onDismiss = { editorOpen = false },
            onSave = {
                viewModel.saveTask(it)
                editorOpen = false
            },
            onDelete = editingTask?.let { task ->
                {
                    viewModel.deleteTask(task.id)
                    editorOpen = false
                }
            }
        )
    }
}

private fun greeting(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val hello = if (hour >= 18) "Bonsoir" else "Bonjour"
    return if (name.isBlank()) "$hello. Une action après l'autre." else "$hello $name. Une action après l'autre."
}

@Composable
private fun DailyProgressCard(state: HomeUiState) {
    val s = state.daySummary
    PetraCard {
        SectionHeader("Progression quotidienne")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (s.expected == 0 && s.skipped + s.postponed == 0) "Aucune tâche aujourd'hui"
                    else "${s.done} ${if (s.done > 1) "tâches terminées" else "tâche terminée"} sur ${s.expected}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (s.expected > 0 && s.remaining == 0) PetraSuccess else PetraText
                )
                Spacer(Modifier.height(8.dp))
                PetraProgressBar(progress = s.rate)
            }
            if (s.expected > 0) {
                Spacer(Modifier.width(16.dp))
                PetraProgressRing(progress = s.rate, size = 76.dp)
            }
        }
        val extra = listOfNotNull(
            s.skipped.takeIf { it > 0 }?.let { "$it sautée${if (it > 1) "s" else ""}" },
            s.postponed.takeIf { it > 0 }?.let { "$it reportée${if (it > 1) "s" else ""}" }
        )
        if (extra.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
        }
        val next = state.nextTask
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                next != null -> "Prochaine : ${next.task.title}" + (next.routine?.let { " (${it.name})" } ?: "")
                s.expected > 0 -> "Tout est fait pour aujourd'hui. Bon travail."
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = PetraTextMuted
        )
    }
}

@Composable
private fun WorkoutCard(state: HomeUiState, onStartWorkout: (Long?) -> Unit, onOpenSport: () -> Unit, onOpenPlan: () -> Unit) {
    SectionHeader("Objectif sportif", trailing = { TextButton(onClick = onOpenPlan) { Text("PLANNING") } })
    val todo = state.todayPlanned.firstOrNull { !it.isDone }
    val next = state.nextPlanned
    val suggestion = state.suggestedWorkout
    if (todo != null) {
        // Séance prévue aujourd'hui par le planning
        PetraActionCard(
            icon = Icons.Outlined.FitnessCenter,
            title = todo.name,
            subtitle = listOfNotNull("Prévue aujourd'hui", todo.timeMinutes?.let { PetraDates.formatMinutes(it) }, todo.category)
                .joinToString(" · "),
            onClick = onOpenPlan,
            buttonText = "Démarrer",
            onButtonClick = { onStartWorkout(todo.programId) }
        )
    } else if (state.todayPlanned.isNotEmpty() || next != null) {
        PetraActionCard(
            icon = Icons.Outlined.FitnessCenter,
            title = if (state.todayPlanned.isNotEmpty()) "Séance du jour faite" else "Repos aujourd'hui",
            subtitle = next?.let {
                "Prochaine : ${PetraDates.formatLongDate(it.epochDay)} · ${it.name}" + (it.timeMinutes?.let { t -> " · ${PetraDates.formatMinutes(t)}" } ?: "")
            } ?: "Rien d'autre de prévu sur les 2 semaines",
            onClick = onOpenPlan
        )
    } else if (suggestion != null) {
        PetraActionCard(
            icon = Icons.Outlined.FitnessCenter,
            title = suggestion.name,
            subtitle = listOf(
                suggestion.category,
                "${suggestion.exerciseCount} exercice${if (suggestion.exerciseCount > 1) "s" else ""}",
                when (val d = suggestion.daysSinceLast) {
                    null -> "jamais réalisé"
                    0L -> "fait aujourd'hui"
                    1L -> "fait hier"
                    else -> "il y a $d jours"
                }
            ).joinToString(" · "),
            onClick = onOpenSport,
            buttonText = "Démarrer",
            onButtonClick = { onStartWorkout(suggestion.programId) }
        )
    } else {
        PetraCard {
            PetraStateMessage(
                kind = PetraStateKind.EMPTY,
                title = if (state.hasPrograms) "Programme sans exercice" else "Aucun programme",
                message = if (state.hasPrograms) "Ajoute des exercices à un programme pour démarrer une séance."
                else "Crée ton premier programme d'entraînement.",
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PetraSecondaryButton("Ouvrir Sport", onClick = onOpenSport)
                        PetraSecondaryButton("Séance libre", onClick = { onStartWorkout(null) })
                    }
                }
            )
        }
    }
}

@Composable
private fun WeekCard(state: HomeUiState) {
    PetraCard {
        SectionHeader("Bilan de la semaine")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val w = state.weekSummary
            StatTile(
                value = if (w.expected == 0) "—" else "${(w.rate * 100).toInt()} %",
                label = "Tâches réalisées",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${state.weekSessions} / ${state.plannedThisWeek ?: state.weeklySessionGoal}",
                label = if (state.plannedThisWeek != null) "Séances (planning)" else "Séances effectuées",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Depuis lundi. Les tâches sautées ou reportées ne comptent pas comme des échecs.",
            style = MaterialTheme.typography.bodySmall,
            color = PetraTextMuted
        )
    }
}
