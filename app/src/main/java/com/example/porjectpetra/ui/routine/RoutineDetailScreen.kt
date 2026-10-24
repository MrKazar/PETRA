package com.example.porjectpetra.ui.routine

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.data.model.DailyPlanner
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraProgressBar
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.routine.components.RoutineEditorDialog
import com.example.porjectpetra.ui.routine.components.TaskEditorDialog
import com.example.porjectpetra.ui.theme.PetraBlack
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraOliveLight
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraSurfaceAlt
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var routineEditorOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var taskEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTask = editingTaskId?.let { id -> state.tasks.firstOrNull { it.id == id } }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    val routine = state.routine

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text((routine?.name ?: "Routine").uppercase(), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { routineEditorOpen = true }, enabled = routine != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier la routine")
                    }
                    IconButton(onClick = { confirmDelete = true }, enabled = routine != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer la routine")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PetraBlack)
            )
        }
    ) { inner ->
        if (routine == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    listOf(
                        DailyPlanner.triggerLabel(routine.triggerType, routine.scheduledMinutes, routine.triggerEvent),
                        DayMask.describe(routine.activeDays)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = PetraTextMuted
                )
                if (routine.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(routine.description, style = MaterialTheme.typography.bodyMedium, color = PetraTextMuted)
                }
            }

            item {
                PetraCard {
                    val s = state.todaySummary
                    SectionHeader(
                        title = "Aujourd'hui",
                        trailing = {
                            if (state.todayTasks.isNotEmpty()) {
                                Text(
                                    "${s.done}/${s.expected}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (s.expected > 0 && s.remaining == 0) PetraSuccess else PetraTextMuted
                                )
                            }
                        }
                    )
                    if (state.todayTasks.isEmpty()) {
                        Text(
                            if (state.tasks.isEmpty()) "Ajoute une première étape ci-dessous."
                            else "Cette routine n'est pas prévue aujourd'hui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PetraTextMuted
                        )
                    } else {
                        PetraProgressBar(s.rate)
                        Spacer(Modifier.height(4.dp))
                        state.todayTasks.forEach { item ->
                            PlannedTaskRow(
                                item = item,
                                onSetStatus = viewModel::setTaskStatus,
                                onEdit = { editingTaskId = it.task.id; taskEditorOpen = true }
                            )
                        }
                        if (state.todayTasks.any { it.status != null }) {
                            TextButton(onClick = viewModel::resetToday) { Text("TOUT REMETTRE À FAIRE") }
                        }
                    }
                }
            }

            item {
                PetraCard {
                    SectionHeader(
                        title = "Étapes",
                        trailing = {
                            TextButton(onClick = { editingTaskId = null; taskEditorOpen = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("AJOUTER")
                            }
                        }
                    )
                    if (state.tasks.isEmpty()) {
                        Text("Aucune étape.", style = MaterialTheme.typography.bodyMedium, color = PetraTextMuted)
                    }
                    state.tasks.forEachIndexed { index, task ->
                        if (index > 0) HorizontalDivider(color = PetraBorder)
                        TaskDefinitionRow(
                            task = task,
                            today = state.today,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.tasks.lastIndex,
                            onClick = { editingTaskId = task.id; taskEditorOpen = true },
                            onMove = { up -> viewModel.moveTask(task, up) },
                            showTrigger = false
                        )
                    }
                }
            }

            item { HistoryCard(state.history) }
        }
    }

    if (routineEditorOpen && routine != null) {
        RoutineEditorDialog(
            initial = routine,
            onDismiss = { routineEditorOpen = false },
            onSave = {
                viewModel.saveRoutine(it)
                routineEditorOpen = false
            }
        )
    }

    if (taskEditorOpen && routine != null) {
        TaskEditorDialog(
            initial = editingTask,
            routineId = routine.id,
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer la routine ?") },
            text = { Text("La routine, ses étapes et leur historique seront supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteRoutine()
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}

/** Historique sur 14 jours : une barre par jour, hauteur = part des étapes réalisées. */
@Composable
private fun HistoryCard(history: List<RoutineDayHistory>) {
    PetraCard {
        SectionHeader("Historique — 14 jours")
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            history.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PetraSurfaceAlt),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val rate = day.summary?.rate ?: 0f
                        if (rate > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(rate)
                                    .background(if (rate >= 1f) PetraSuccess else PetraOliveLight)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        DayMask.shortLabels[PetraDates.dayIndex(day.epochDay)],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.summary == null) PetraBorder else PetraTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val tracked = history.mapNotNull { it.summary }
        val done = tracked.sumOf { it.done }
        val expected = tracked.sumOf { it.expected }
        Text(
            if (expected == 0) "Pas encore d'historique."
            else "$done étapes réalisées sur $expected prévues (${done * 100 / expected} %).",
            style = MaterialTheme.typography.bodySmall,
            color = PetraText
        )
        Text(
            "La régularité sur plusieurs semaines compte plus qu'un jour parfait.",
            style = MaterialTheme.typography.bodySmall,
            color = PetraTextMuted
        )
    }
}
