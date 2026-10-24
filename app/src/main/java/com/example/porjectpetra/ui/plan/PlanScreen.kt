package com.example.porjectpetra.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.PetraDates
import com.example.porjectpetra.data.model.TrainingPlanner
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraStateKind
import com.example.porjectpetra.ui.components.PetraStateMessage
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.theme.PetraBlack
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraKhaki
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraSurfaceAlt
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onStartWorkout: (programId: Long) -> Unit,
    onCreateProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addingForDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSwap by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("PLANNING — 2 SEMAINES", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = { confirmSwap = true }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Inverser les semaines A et B")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PetraBlack)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PetraCard {
                    Text(
                        "Cette semaine : ${state.doneThisWeek} séance${if (state.doneThisWeek > 1) "s" else ""} faite${if (state.doneThisWeek > 1) "s" else ""} sur ${state.plannedThisWeek} prévue${if (state.plannedThisWeek > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Le planning alterne semaine A et semaine B, puis recommence. Ajoute une séance avec « + » : " +
                            "chaque semaine A (ou B), ou ce jour seulement. Touche une séance pour la déplacer ou l'annuler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                }
            }

            if (!state.isLoading && state.programs.isEmpty()) {
                item {
                    PetraCard {
                        PetraStateMessage(
                            kind = PetraStateKind.EMPTY,
                            title = "Aucun programme",
                            message = "Crée d'abord un programme (musculation ou cardio) pour le placer dans le planning.",
                            action = { TextButton(onClick = onCreateProgram) { Text("CRÉER UN PROGRAMME") } }
                        )
                    }
                }
            }

            state.days.chunked(7).forEachIndexed { weekPos, week ->
                item(key = "week_$weekPos") {
                    val weekIndex = week.first().weekIndex
                    SectionHeader(
                        title = (if (weekPos == 0) "Cette semaine" else "Semaine prochaine") +
                            " · Semaine ${TrainingPlanner.weekLabel(weekIndex)}"
                    )
                }
                items(week, key = { "day_${it.epochDay}" }) { day ->
                    DayCard(
                        day = day,
                        canAdd = state.programs.isNotEmpty(),
                        onAdd = { addingForDay = day.epochDay },
                        onItemClick = { selectedItemKey = itemKey(it) }
                    )
                }
            }
        }
    }

    addingForDay?.let { epochDay ->
        val day = state.days.firstOrNull { it.epochDay == epochDay }
        if (day != null) {
            AddSessionDialog(
                day = day,
                programs = state.programs.map { it.id to it.name },
                onDismiss = { addingForDay = null },
                onConfirm = { programId, time, recurring ->
                    viewModel.addSession(day, programId, time, recurring)
                    addingForDay = null
                }
            )
        }
    }

    selectedItemKey?.let { key ->
        val found = state.days.firstNotNullOfOrNull { d -> d.items.firstOrNull { itemKey(it) == key }?.let { d to it } }
        if (found == null) {
            selectedItemKey = null
        } else {
            val (day, item) = found
            ItemActionsDialog(
                day = day,
                item = item,
                onDismiss = { selectedItemKey = null },
                onStart = { selectedItemKey = null; onStartWorkout(item.planned.programId) },
                onCancelDay = { viewModel.setCancelledForDay(item, true); selectedItemKey = null },
                onRestoreDay = { viewModel.setCancelledForDay(item, false); selectedItemKey = null },
                onRemoveCycle = { viewModel.removeFromCycle(item); selectedItemKey = null },
                onRemoveDayOnly = { viewModel.removeDayOnly(item); selectedItemKey = null }
            )
        }
    }

    if (confirmSwap) {
        AlertDialog(
            onDismissRequest = { confirmSwap = false },
            title = { Text("Inverser les semaines A et B ?") },
            text = { Text("Cette semaine deviendra l'autre semaine du cycle. Les séances du cycle changent de semaine ; les séances ajoutées pour un jour précis restent à leur date.") },
            confirmButton = { TextButton(onClick = { viewModel.swapWeeks(); confirmSwap = false }) { Text("Inverser") } },
            dismissButton = { TextButton(onClick = { confirmSwap = false }) { Text("Annuler") } }
        )
    }
}

private fun itemKey(item: PlanItemUi): String =
    "${item.planned.epochDay}_${item.planned.slotId ?: "x"}_${item.planned.overrideId ?: "x"}"

private fun dayTitle(epochDay: Long): String = PetraDates.formatLongDate(epochDay).replaceFirstChar { it.uppercase() }

@Composable
private fun DayCard(day: PlanDayUi, canAdd: Boolean, onAdd: () -> Unit, onItemClick: (PlanItemUi) -> Unit) {
    PetraCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dayTitle(day.epochDay),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (day.isPast) PetraTextMuted else PetraText
                    )
                    if (day.isToday) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AUJOURD'HUI",
                            style = MaterialTheme.typography.labelSmall,
                            color = PetraText,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(PetraOlive).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (day.items.isEmpty()) {
                    Text("Repos", style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
                }
            }
            if (canAdd) {
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Ajouter une séance le ${dayTitle(day.epochDay)}", tint = PetraKhaki) }
            }
        }
        day.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onItemClick(item) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        item.isDone -> Icons.Default.Check
                        item.planned.isRecurring -> Icons.Default.Repeat
                        else -> Icons.Default.Event
                    },
                    contentDescription = when {
                        item.isDone -> "Faite"
                        item.planned.isRecurring -> "Chaque semaine ${TrainingPlanner.weekLabel(day.weekIndex)}"
                        else -> "Ce jour seulement"
                    },
                    tint = if (item.isDone) PetraSuccess else PetraTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.programName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            item.planned.isCancelled -> PetraTextMuted
                            item.isDone -> PetraSuccess
                            else -> PetraText
                        },
                        textDecoration = if (item.planned.isCancelled) TextDecoration.LineThrough else null
                    )
                    Text(
                        listOfNotNull(
                            item.category,
                            item.planned.timeMinutes?.let { PetraDates.formatMinutes(it) },
                            if (item.planned.isCancelled) "annulée ce jour" else null
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddSessionDialog(
    day: PlanDayUi,
    programs: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onConfirm: (programId: Long, timeMinutes: Int?, recurring: Boolean) -> Unit
) {
    var programId by rememberSaveable { mutableStateOf(programs.firstOrNull()?.first) }
    var recurring by rememberSaveable { mutableStateOf(true) }
    var withTime by rememberSaveable { mutableStateOf(false) }
    val timeState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = true)
    val weekday = DayMask.longLabels[PetraDates.dayIndex(day.epochDay)]
    val week = TrainingPlanner.weekLabel(day.weekIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Séance — ${dayTitle(day.epochDay)}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PROGRAMME", style = MaterialTheme.typography.labelMedium, color = PetraTextMuted)
                programs.forEach { (id, name) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { programId = id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = programId == id, onClick = { programId = id })
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                HorizontalDivider(color = PetraBorder)
                Text("RÉPÉTITION", style = MaterialTheme.typography.labelMedium, color = PetraTextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(true to "Chaque $weekday · semaine $week", false to "Ce jour seulement").forEach { (value, label) ->
                        FilterChip(
                            selected = recurring == value,
                            onClick = { recurring = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PetraOlive, selectedLabelColor = PetraText)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Heure précise", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = withTime,
                        onCheckedChange = { withTime = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PetraOlive, uncheckedTrackColor = PetraSurfaceAlt)
                    )
                }
                if (withTime) TimeInput(state = timeState)
            }
        },
        confirmButton = {
            TextButton(
                enabled = programId != null,
                onClick = {
                    programId?.let { onConfirm(it, if (withTime) timeState.hour * 60 + timeState.minute else null, recurring) }
                }
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ItemActionsDialog(
    day: PlanDayUi,
    item: PlanItemUi,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onCancelDay: () -> Unit,
    onRestoreDay: () -> Unit,
    onRemoveCycle: () -> Unit,
    onRemoveDayOnly: () -> Unit
) {
    val week = TrainingPlanner.weekLabel(day.weekIndex)
    val weekday = DayMask.longLabels[PetraDates.dayIndex(day.epochDay)]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.programName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (item.planned.isRecurring) "Prévue chaque $weekday de semaine $week." else "Prévue le ${PetraDates.formatLongDate(day.epochDay)} seulement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PetraTextMuted
                )
                Spacer(Modifier.height(8.dp))
                if (day.isToday && !item.isDone && !item.planned.isCancelled) {
                    TextButton(onClick = onStart) { Text("DÉMARRER LA SÉANCE") }
                }
                if (item.planned.isRecurring) {
                    if (item.planned.isCancelled) {
                        TextButton(onClick = onRestoreDay) { Text("RÉTABLIR CE JOUR") }
                    } else {
                        TextButton(onClick = onCancelDay) { Text("ANNULER CE JOUR SEULEMENT") }
                    }
                    TextButton(onClick = onRemoveCycle) {
                        Text("RETIRER DU CYCLE (CHAQUE SEMAINE $week)", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = onRemoveDayOnly) { Text("RETIRER", color = MaterialTheme.colorScheme.error) }
                }
                Text(
                    "Pour déplacer une séance : annule-la ce jour-là, puis ajoute-la sur un autre jour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PetraTextMuted
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
