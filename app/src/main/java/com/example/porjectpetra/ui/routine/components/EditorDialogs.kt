package com.example.porjectpetra.ui.routine.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.data.local.entity.DailyTask
import com.example.porjectpetra.data.local.entity.Routine
import com.example.porjectpetra.data.model.DayEvents
import com.example.porjectpetra.data.model.DayMask
import com.example.porjectpetra.data.model.Recurrence
import com.example.porjectpetra.data.model.TriggerType
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

private const val DEFAULT_MINUTES = 7 * 60

/** Création / modification d'une tâche. [routineId] non nul = tâche d'une routine (horaire hérité). */
@Composable
fun TaskEditorDialog(
    initial: DailyTask?,
    routineId: Long?,
    today: Long,
    onDismiss: () -> Unit,
    onSave: (DailyTask) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val inRoutine = routineId != null
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var estimated by rememberSaveable { mutableStateOf(initial?.estimatedMinutes?.takeIf { it > 0 }?.toString() ?: "") }
    var triggerType by rememberSaveable { mutableStateOf(TriggerType.from(initial?.triggerType ?: TriggerType.ANYTIME.name)) }
    var minutes by rememberSaveable { mutableIntStateOf(initial?.scheduledMinutes ?: DEFAULT_MINUTES) }
    var event by rememberSaveable { mutableStateOf(initial?.triggerEvent ?: "") }
    var recurrence by rememberSaveable { mutableStateOf(Recurrence.from(initial?.recurrence ?: Recurrence.WEEKLY.name)) }
    var days by rememberSaveable { mutableIntStateOf(initial?.activeDays ?: DayMask.ALL_DAYS) }
    var oneOffDay by rememberSaveable { mutableStateOf(initial?.oneOffEpochDay ?: today) }
    var titleError by rememberSaveable { mutableStateOf(false) }

    val daysError = recurrence == Recurrence.WEEKLY && days == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle tâche" else "Modifier la tâche") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Nom de la tâche") },
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Donne un nom à la tâche") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (facultatif)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estimated,
                    onValueChange = { v -> estimated = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Durée estimée (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (inRoutine) {
                    Text(
                        "Cette tâche suit l'horaire ou le déclencheur de sa routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                } else {
                    ScheduleFields(
                        triggerType = triggerType,
                        minutes = minutes,
                        event = event,
                        onTriggerTypeChange = { triggerType = it },
                        onMinutesChange = { minutes = it },
                        onEventChange = { event = it }
                    )
                }

                FieldLabel("Récurrence")
                ChipRow {
                    Recurrence.entries.forEach { r ->
                        PetraChip(label = r.label, selected = recurrence == r) { recurrence = r }
                    }
                }
                if (recurrence == Recurrence.WEEKLY) {
                    DaysSelector(days = days, onChange = { days = it })
                    if (daysError) {
                        Text("Choisis au moins un jour", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    ChipRow {
                        PetraChip("Aujourd'hui", oneOffDay == today) { oneOffDay = today }
                        PetraChip("Demain", oneOffDay == today + 1) { oneOffDay = today + 1 }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    titleError = true
                    return@TextButton
                }
                if (daysError) return@TextButton
                val type = if (inRoutine) TriggerType.ANYTIME else triggerType
                onSave(
                    (initial ?: DailyTask(title = "", routineId = routineId, createdEpochDay = today)).copy(
                        title = title.trim(),
                        description = description.trim(),
                        estimatedMinutes = estimated.toIntOrNull() ?: 0,
                        triggerType = type.name,
                        scheduledMinutes = if (type == TriggerType.SCHEDULED) minutes else null,
                        triggerEvent = if (type == TriggerType.EVENT) event.trim() else "",
                        recurrence = recurrence.name,
                        activeDays = days,
                        oneOffEpochDay = if (recurrence == Recurrence.ONCE) oneOffDay else null,
                        // Une tâche ponctuelle prévue aujourd'hui doit compter dès aujourd'hui.
                        createdEpochDay = minOf(initial?.createdEpochDay ?: today, today)
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

/** Création / modification d'une routine. */
@Composable
fun RoutineEditorDialog(
    initial: Routine?,
    onDismiss: () -> Unit,
    onSave: (Routine) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var triggerType by rememberSaveable { mutableStateOf(TriggerType.from(initial?.triggerType ?: TriggerType.EVENT.name)) }
    var minutes by rememberSaveable { mutableIntStateOf(initial?.scheduledMinutes ?: DEFAULT_MINUTES) }
    var event by rememberSaveable { mutableStateOf(initial?.triggerEvent ?: "") }
    var days by rememberSaveable { mutableIntStateOf(initial?.activeDays ?: DayMask.ALL_DAYS) }
    var nameError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle routine" else "Modifier la routine") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Nom de la routine") },
                    placeholder = { Text("Ex. Routine du soir") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Donne un nom à la routine") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (facultatif)") },
                    modifier = Modifier.fillMaxWidth()
                )
                ScheduleFields(
                    triggerType = triggerType,
                    minutes = minutes,
                    event = event,
                    onTriggerTypeChange = { triggerType = it },
                    onMinutesChange = { minutes = it },
                    onEventChange = { event = it }
                )
                FieldLabel("Jours actifs")
                DaysSelector(days = days, onChange = { days = it })
                if (days == 0) {
                    Text("Choisis au moins un jour", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@TextButton
                }
                if (days == 0) return@TextButton
                onSave(
                    (initial ?: Routine(name = "")).copy(
                        name = name.trim(),
                        description = description.trim(),
                        triggerType = triggerType.name,
                        scheduledMinutes = if (triggerType == TriggerType.SCHEDULED) minutes else null,
                        triggerEvent = if (triggerType == TriggerType.EVENT) event.trim() else "",
                        activeDays = days
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/** Choix du placement dans la journée : heure fixe, événement déclencheur ou libre. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFields(
    triggerType: TriggerType,
    minutes: Int,
    event: String,
    onTriggerTypeChange: (TriggerType) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onEventChange: (String) -> Unit
) {
    FieldLabel("Quand ?")
    ChipRow {
        TriggerType.entries.forEach { t ->
            PetraChip(label = t.label, selected = triggerType == t) { onTriggerTypeChange(t) }
        }
    }
    when (triggerType) {
        TriggerType.SCHEDULED -> {
            val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
            LaunchedEffect(state.hour, state.minute) { onMinutesChange(state.hour * 60 + state.minute) }
            TimeInput(state = state)
        }
        TriggerType.EVENT -> {
            OutlinedTextField(
                value = event,
                onValueChange = onEventChange,
                label = { Text("Événement déclencheur") },
                placeholder = { Text("Ex. Après le réveil") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ChipRow {
                DayEvents.suggestions.forEach { s ->
                    PetraChip(label = s, selected = event.equals(s, ignoreCase = true)) { onEventChange(s) }
                }
            }
        }
        TriggerType.ANYTIME -> Text(
            "À faire quand tu veux dans la journée.",
            style = MaterialTheme.typography.bodySmall,
            color = PetraTextMuted
        )
    }
}

@Composable
fun DaysSelector(days: Int, onChange: (Int) -> Unit) {
    ChipRow {
        DayMask.shortLabels.forEachIndexed { index, label ->
            PetraChip(label = label, selected = DayMask.contains(days, index)) { onChange(DayMask.toggle(days, index)) }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = PetraTextMuted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) { content() }
}

@Composable
private fun PetraChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PetraOlive,
            selectedLabelColor = PetraText
        )
    )
}
