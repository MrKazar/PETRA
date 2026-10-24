package com.example.porjectpetra.ui.routine

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.Recurrence
import com.example.porjectpetra.ui.components.ComingSoonNote
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraProgressBar
import com.example.porjectpetra.ui.components.PetraScreenHeader
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.routine.components.RoutineEditorDialog
import com.example.porjectpetra.ui.routine.components.TaskEditorDialog
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel,
    onOpenRoutine: (routineId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var routineEditorOpen by rememberSaveable { mutableStateOf(false) }
    var taskEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTask = editingTaskId?.let { id -> state.standaloneTasks.firstOrNull { it.id == id } }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { routineEditorOpen = true },
                containerColor = PetraOlive,
                contentColor = PetraText,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ROUTINE", style = MaterialTheme.typography.labelLarge) }
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
                    title = "Routines",
                    subtitle = "Des étapes simples, dans l'ordre, chaque jour.",
                    contentPadding = PaddingValues(vertical = 12.dp)
                )
            }

            if (!state.isLoading && state.routines.isEmpty()) {
                item {
                    PetraCard {
                        Text("Aucune routine pour l'instant.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Crée par exemple une routine du matin ou du soir avec le bouton « Routine ».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PetraTextMuted
                        )
                    }
                }
            }

            items(state.routines, key = { "routine_${it.routine.id}" }) { summary ->
                val routine = summary.routine
                PetraCard(onClick = { onOpenRoutine(routine.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(
                                    DailyPlanner.triggerLabel(routine.triggerType, routine.scheduledMinutes, routine.triggerEvent),
                                    DayMask.describe(routine.activeDays)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = PetraTextMuted
                            )
                            Text(
                                "${summary.taskCount} tâche${if (summary.taskCount > 1) "s" else ""}" +
                                    if (summary.totalMinutes > 0) " · env. ${summary.totalMinutes} min" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = PetraTextMuted
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PetraTextMuted)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (summary.dueToday) {
                        PetraProgressBar(summary.today.rate)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Aujourd'hui : ${summary.today.done}/${summary.today.expected}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (summary.today.expected > 0 && summary.today.remaining == 0) PetraSuccess else PetraTextMuted
                        )
                    } else {
                        Text("Pas prévue aujourd'hui", style = MaterialTheme.typography.labelMedium, color = PetraTextMuted)
                    }
                }
            }

            item {
                PetraCard {
                    SectionHeader(
                        title = "Tâches indépendantes",
                        trailing = {
                            TextButton(onClick = { editingTaskId = null; taskEditorOpen = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("AJOUTER")
                            }
                        }
                    )
                    if (state.standaloneTasks.isEmpty()) {
                        Text(
                            "Les tâches hors routine (ex. préparer ses affaires pour demain) apparaissent ici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PetraTextMuted
                        )
                    }
                    state.standaloneTasks.forEachIndexed { index, task ->
                        if (index > 0) HorizontalDivider(color = PetraBorder)
                        TaskDefinitionRow(
                            task = task,
                            today = state.today,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.standaloneTasks.lastIndex,
                            onClick = { editingTaskId = task.id; taskEditorOpen = true },
                            onMove = { up -> viewModel.moveTask(task, up) }
                        )
                    }
                }
            }

            item {
                ComingSoonNote("rappels et notifications des routines.", modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    if (routineEditorOpen) {
        RoutineEditorDialog(
            initial = null,
            onDismiss = { routineEditorOpen = false },
            onSave = { routine ->
                routineEditorOpen = false
                viewModel.saveRoutine(routine) { id -> onOpenRoutine(id) }
            }
        )
    }

    if (taskEditorOpen) {
        TaskEditorDialog(
            initial = editingTask,
            routineId = null,
            today = state.today,
            onDismiss = { taskEditorOpen = false },
            onSave = {
                viewModel.saveTask(it)
                taskEditorOpen = false
            },
            onDelete = editingTask?.let { task ->
                {
                    viewModel.deleteTask(task.id)
                    taskEditorOpen = false
                }
            }
        )
    }
}

/** Ligne de définition d'une tâche (pas son état du jour) : nom, quand, récurrence, réordonnancement. */
@Composable
fun TaskDefinitionRow(
    task: DailyTask,
    today: Long,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    showTrigger: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = "Modifier la tâche", onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            val recurrence = when (Recurrence.from(task.recurrence)) {
                Recurrence.WEEKLY -> DayMask.describe(task.activeDays)
                Recurrence.ONCE -> when (task.oneOffEpochDay) {
                    today -> "Aujourd'hui seulement"
                    today + 1 -> "Demain seulement"
                    null -> "Une fois"
                    else -> "Le " + PetraDates.formatLongDate(task.oneOffEpochDay)
                }
            }
            Text(
                listOfNotNull(
                    if (showTrigger) DailyPlanner.triggerLabel(task.triggerType, task.scheduledMinutes, task.triggerEvent) else null,
                    recurrence,
                    task.estimatedMinutes.takeIf { it > 0 }?.let { "$it min" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = PetraTextMuted
            )
        }
        IconButton(onClick = { onMove(true) }, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Monter")
        }
        IconButton(onClick = { onMove(false) }, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descendre")
        }
    }
}
