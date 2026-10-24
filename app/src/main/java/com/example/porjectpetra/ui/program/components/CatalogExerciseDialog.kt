package com.example.porjectpetra.ui.program.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.data.catalog.CatalogExercise
import com.example.porjectpetra.data.catalog.ExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCycle
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraKhaki
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted
import java.text.Normalizer

private const val ALL_GROUPS = "Tous"

/**
 * Choix d'un exercice dans le catalogue, puis saisie de la charge de travail.
 * Séries, répétitions et repos ne sont pas modifiables : ils viennent du catalogue.
 */
@Composable
fun CatalogExerciseDialog(
    catalog: ExerciseCatalog,
    onDismiss: () -> Unit,
    onConfirm: (exercise: CatalogExercise, workLoadKg: Double, notes: String) -> Unit,
    initialCatalogId: String? = null,
    initialWorkLoadKg: Double = 0.0,
    initialNotes: String = "",
    showNotes: Boolean = true,
    confirmLabel: String = "Ajouter"
) {
    var selectedId by rememberSaveable { mutableStateOf(initialCatalogId) }
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf(ALL_GROUPS) }
    var workLoad by rememberSaveable { mutableStateOf(if (initialWorkLoadKg > 0) ExerciseCycle.formatKg(initialWorkLoadKg) else "") }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }
    val selected = catalog.byId(selectedId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selected == null) "Choisir un exercice" else selected.name) },
        text = {
            if (selected == null) {
                ExercisePicker(
                    catalog = catalog,
                    query = query,
                    onQueryChange = { query = it },
                    group = group,
                    onGroupChange = { group = it },
                    onPick = { selectedId = it.id }
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        listOf(selected.muscleGroup, selected.equipment, if (selected.isUnilateral) "par côté" else null)
                            .filterNotNull().joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                    if (selected.usesLoad) {
                        OutlinedTextField(
                            value = workLoad,
                            onValueChange = { v -> workLoad = v.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6) },
                            label = { Text("Charge de travail (kg)") },
                            supportingText = { Text("Les charges de chauffe et d'hypertrophie en sont déduites.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Exercice au poids du corps : pas de charge à saisir.", style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
                    }
                    CyclePreview(selected, workLoad.toDoubleOrNull() ?: 0.0)
                    if (showNotes) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes & consignes (facultatif)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (initialCatalogId == null) {
                        TextButton(onClick = { selectedId = null }) { Text("CHANGER D'EXERCICE") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onConfirm(it, workLoad.toDoubleOrNull() ?: 0.0, notes.trim()) } }
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ExercisePicker(
    catalog: ExerciseCatalog,
    query: String,
    onQueryChange: (String) -> Unit,
    group: String,
    onGroupChange: (String) -> Unit,
    onPick: (CatalogExercise) -> Unit
) {
    val groups = remember(catalog) { listOf(ALL_GROUPS) + catalog.muscleGroups }
    val normalizedQuery = normalize(query)
    val results = catalog.exercises.filter {
        (group == ALL_GROUPS || it.muscleGroup == group) &&
            (normalizedQuery.isEmpty() || normalize(it.name).contains(normalizedQuery))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Rechercher (ex. développé, curl…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(groups) { g ->
                FilterChip(
                    selected = g == group,
                    onClick = { onGroupChange(g) },
                    label = { Text(g) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PetraOlive, selectedLabelColor = PetraText)
                )
            }
        }
        if (results.isEmpty()) {
            Text("Aucun exercice trouvé.", style = MaterialTheme.typography.bodyMedium, color = PetraTextMuted)
        }
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(results, key = { it.id }) { ex ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(ex) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${ex.muscleGroup} · ${ex.equipment}", style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
                }
                HorizontalDivider(color = PetraBorder)
            }
        }
    }
}

/** Aperçu du cycle fixe : 1 chauffe, 2 travail, 1 hypertrophie, avec répétitions, charges et repos. */
@Composable
fun CyclePreview(exercise: CatalogExercise, workLoadKg: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("CYCLE DE L'EXERCICE", style = MaterialTheme.typography.labelMedium, color = PetraKhaki)
        ExerciseCycle.build(exercise, workLoadKg).forEach { set ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(set.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.3f))
                Text(
                    ExerciseCycle.formatReps(set.reps, set.isSeconds, exercise.isUnilateral),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        !exercise.usesLoad -> "PdC"
                        workLoadKg <= 0 -> "— kg"
                        else -> "${ExerciseCycle.formatKg(set.weightKg)} kg"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    ExerciseCycle.formatRest(set.restSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = PetraTextMuted,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "Chauffe à 50 % et hypertrophie à 80 % de la charge de travail, arrondies à 0,5 kg.",
            style = MaterialTheme.typography.bodySmall,
            color = PetraTextMuted
        )
    }
}

private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
