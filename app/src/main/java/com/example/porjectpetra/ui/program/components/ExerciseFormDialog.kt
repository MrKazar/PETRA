package com.example.porjectpetra.ui.program.components

import com.example.porjectpetra.data.model.SportVocabulary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.data.local.entity.Exercise

val MUSCLE_GROUPS = SportVocabulary.muscleGroups

val REST_TIME_OPTIONS = listOf(30, 45, 60, 90, 120, 180)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseFormDialog(
    exerciseToEdit: Exercise?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        muscleGroup: String,
        targetSets: Int,
        targetReps: Int,
        targetWeightKg: Double,
        targetDurationSeconds: Int,
        restTimeSeconds: Int,
        notes: String
    ) -> Unit
) {
    var name by remember(exerciseToEdit) { mutableStateOf(exerciseToEdit?.name ?: "") }
    var selectedMuscleGroup by remember(exerciseToEdit) {
        mutableStateOf(exerciseToEdit?.muscleGroup ?: MUSCLE_GROUPS.first())
    }
    var targetSets by remember(exerciseToEdit) { mutableIntStateOf(exerciseToEdit?.targetSets ?: 3) }
    var targetRepsText by remember(exerciseToEdit) {
        mutableStateOf(exerciseToEdit?.targetReps?.toString() ?: "10")
    }
    var targetWeightText by remember(exerciseToEdit) {
        mutableStateOf(if (exerciseToEdit != null && exerciseToEdit.targetWeightKg > 0) exerciseToEdit.targetWeightKg.toString() else "")
    }
    var targetDurationText by remember(exerciseToEdit) {
        mutableStateOf(if (exerciseToEdit != null && exerciseToEdit.targetDurationSeconds > 0) exerciseToEdit.targetDurationSeconds.toString() else "0")
    }
    var selectedRestTime by remember(exerciseToEdit) {
        mutableIntStateOf(exerciseToEdit?.restTimeSeconds ?: 60)
    }
    var notes by remember(exerciseToEdit) { mutableStateOf(exerciseToEdit?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (exerciseToEdit != null) "Modifier l'exercice" else "Ajouter un exercice",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'exercice *") },
                    placeholder = { Text("ex: Développé couché") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Groupe musculaire",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MUSCLE_GROUPS.forEach { muscle ->
                            FilterChip(
                                selected = selectedMuscleGroup == muscle,
                                onClick = { selectedMuscleGroup = muscle },
                                label = { Text(muscle) }
                            )
                        }
                    }
                }

                // Target Sets Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nombre de séries cible",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (targetSets > 1) targetSets-- },
                            enabled = targetSets > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuer")
                        }
                        Text(
                            text = "$targetSets",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { targetSets++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Augmenter")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetRepsText,
                        onValueChange = { targetRepsText = it.filter { char -> char.isDigit() } },
                        label = { Text("Répétitions") },
                        placeholder = { Text("10") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Poids (kg)") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = targetDurationText,
                    onValueChange = { targetDurationText = it.filter { char -> char.isDigit() } },
                    label = { Text("Durée cible (secondes)") },
                    placeholder = { Text("0 pour réps, ou ex: 60 (Gainage)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Temps de repos entre séries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        REST_TIME_OPTIONS.forEach { seconds ->
                            FilterChip(
                                selected = selectedRestTime == seconds,
                                onClick = { selectedRestTime = seconds },
                                label = { Text("${seconds}s") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Consignes") },
                    placeholder = { Text("Ex: tempo 3-1-1, accentuer la contraction...") },
                    minLines = 2,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val reps = targetRepsText.toIntOrNull() ?: 10
                        val weight = targetWeightText.toDoubleOrNull() ?: 0.0
                        val duration = targetDurationText.toIntOrNull() ?: 0
                        onConfirm(
                            name.trim(),
                            selectedMuscleGroup,
                            targetSets,
                            reps,
                            weight,
                            duration,
                            selectedRestTime,
                            notes.trim()
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (exerciseToEdit != null) "Enregistrer" else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
