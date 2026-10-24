package com.example.porjectpetra.ui.workout

import androidx.compose.ui.graphics.Color
import com.example.porjectpetra.data.catalog.EmptyExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCatalog
import com.example.porjectpetra.data.catalog.ExerciseCycle
import com.example.porjectpetra.ui.program.components.CatalogExerciseDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.ui.theme.PetraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    catalog: ExerciseCatalog = EmptyExerciseCatalog
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSessionSaved, state.isSessionCancelled) {
        if (state.isSessionSaved || state.isSessionCancelled) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.workoutTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(14.dp)
                            )
                            Text(
                                text = state.formattedElapsedTime,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.openCancelDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "Annuler la séance")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.openFinishSummaryDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Terminer", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Rest Timer Active Banner
            AnimatedVisibility(
                visible = state.isRestTimerActive,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                RestTimerBannerCard(
                    exerciseName = state.restTimerExerciseName,
                    formattedRestTime = state.formattedRestTime,
                    remainingSeconds = state.restTimerRemainingSeconds,
                    totalSeconds = state.restTimerTotalSeconds,
                    isPaused = state.isRestTimerPaused,
                    onPauseToggle = {
                        if (state.isRestTimerPaused) viewModel.resumeRestTimer() else viewModel.pauseRestTimer()
                    },
                    onAdd30s = { viewModel.addRestTime(30) },
                    onSkip = { viewModel.skipRestTimer() }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Metrics overview header card
                item {
                    SessionOverviewHeader(
                        elapsedTime = state.formattedElapsedTime,
                        completedSets = state.completedSetsCount,
                        totalSets = state.totalSetsCount,
                        volumeLiftedKg = state.totalVolumeLiftedKg
                    )
                }

                if (state.exercises.isEmpty()) {
                    item {
                        EmptyExercisesWorkoutView(
                            onAddExerciseClick = { viewModel.openAddExerciseDialog() }
                        )
                    }
                } else {
                    itemsIndexed(state.exercises, key = { index, item -> item.exerciseId ?: index }) { exIdx, exercise ->
                        ExerciseWorkoutCard(
                            exerciseIndex = exIdx,
                            exercise = exercise,
                            onWeightChange = { sIdx, weight -> viewModel.updateSetWeight(exIdx, sIdx, weight) },
                            onDurationChange = { sIdx, mins -> viewModel.updateSetDuration(exIdx, sIdx, mins) },
                            onDistanceChange = { sIdx, km -> viewModel.updateSetDistance(exIdx, sIdx, km) },
                            onCaloriesChange = { sIdx, cal -> viewModel.updateSetCalories(exIdx, sIdx, cal) },
                            onToggleComplete = { sIdx -> viewModel.toggleSetCompleted(exIdx, sIdx) },
                            onDeleteExercise = { viewModel.removeExercise(exIdx) },
                            onWorkLoadChange = { load -> viewModel.updateWorkLoad(exIdx, load) }
                        )
                    }
                }

                // Add Exercise Button
                item {
                    OutlinedButton(
                        onClick = { viewModel.openAddExerciseDialog() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter un exercice")
                    }
                }

                // Session Notes
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.updateSessionNotes(it) },
                        label = { Text("Remarques sur la séance") },
                        placeholder = { Text("Sensations, fatigue, ajustements...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        }

        // Dialogs
        if (state.showFinishSummaryDialog) {
            FinishWorkoutSummaryDialog(
                elapsedTime = state.formattedElapsedTime,
                completedSets = state.completedSetsCount,
                totalSets = state.totalSetsCount,
                volumeLiftedKg = state.totalVolumeLiftedKg,
                notes = state.notes,
                onConfirmSave = { viewModel.confirmFinishAndSaveWorkout() },
                onDismiss = { viewModel.dismissFinishSummaryDialog() }
            )
        }

        if (state.showCancelDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCancelDialog() },
                title = { Text("Annuler l'entraînement ?") },
                text = { Text("Les données de cette séance ne seront pas enregistrées.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmCancelWorkout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Abandonner")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCancelDialog() }) {
                        Text("Continuer l'entraînement")
                    }
                }
            )
        }

        if (state.showAddExerciseDialog) {
            CatalogExerciseDialog(
                catalog = catalog,
                showNotes = false,
                onDismiss = { viewModel.closeAddExerciseDialog() },
                onConfirm = { catalogExercise, workLoad, _ -> viewModel.addCatalogExercise(catalogExercise, workLoad) }
            )
        }
    }
}

@Composable
fun RestTimerBannerCard(
    exerciseName: String,
    formattedRestTime: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onAdd30s: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Repos • $exerciseName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = formattedRestTime,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onAdd30s,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("+30s", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onPauseToggle) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Reprendre" else "Pause"
                    )
                }

                TextButton(onClick = onSkip) {
                    Text("Passer", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SessionOverviewHeader(
    elapsedTime: String,
    completedSets: Int,
    totalSets: Int,
    volumeLiftedKg: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DURÉE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SÉRIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "$completedSets / $totalSets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VOLUME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "${volumeLiftedKg.toInt()} kg",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExerciseWorkoutCard(
    exerciseIndex: Int,
    exercise: ActiveExerciseUiState,
    onWeightChange: (setIndex: Int, String) -> Unit,
    onDurationChange: (setIndex: Int, String) -> Unit,
    onDistanceChange: (setIndex: Int, String) -> Unit,
    onCaloriesChange: (setIndex: Int, String) -> Unit,
    onToggleComplete: (setIndex: Int) -> Unit,
    onDeleteExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onWorkLoadChange: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: Exercise Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = exercise.muscleGroup,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    if (exercise.restTimeSeconds > 0) {
                        Text(
                            text = "Repos cible : ${exercise.restTimeSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDeleteExercise) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer l'exercice",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exercice du catalogue : cycle fixe chauffe / travail / hypertrophie.
            if (exercise.isCycle) {
                CycleSetsContent(exercise = exercise, onWorkLoadChange = onWorkLoadChange, onToggleComplete = onToggleComplete)
                return@Column
            }

            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Série",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Cible",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = if (exercise.isCardio) "Min" else "Poids (kg)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (exercise.isCardio) "Km" else "Rép.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Fait",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sets List
            exercise.sets.forEachIndexed { sIdx, setItem ->
                val rowContainerColor = if (setItem.isCompleted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(rowContainerColor)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set Number
                    Text(
                        text = "${setItem.setNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )

                    // Target Description
                    Text(
                        text = if (exercise.isCardio) {
                            "${setItem.targetDurationSeconds / 60}m"
                        } else {
                            if (setItem.targetWeightKg > 0) {
                                "${setItem.targetReps} × ${setItem.targetWeightKg.toInt()} kg"
                            } else {
                                "${setItem.targetReps} rép."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    // Field 1: Weight or Duration
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(horizontal = 2.dp)
                    ) {
                        OutlinedTextField(
                            value = if (exercise.isCardio) setItem.actualDurationMins else setItem.actualWeightKg,
                            onValueChange = { valStr ->
                                if (exercise.isCardio) onDurationChange(sIdx, valStr) else onWeightChange(sIdx, valStr)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                        )
                    }

                    // Champ 2 : distance (cardio) ou répétitions fixées par le programme (non modifiables)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (exercise.isCardio) {
                            OutlinedTextField(
                                value = setItem.actualDistanceKm,
                                onValueChange = { valStr -> onDistanceChange(sIdx, valStr) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                            )
                        } else {
                            Text(
                                text = "${setItem.targetReps}",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Complete Toggle Checkbox
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = { onToggleComplete(sIdx) },
                            colors = if (setItem.isCompleted) {
                                IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (setItem.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Valider la série"
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun FinishWorkoutSummaryDialog(
    elapsedTime: String,
    completedSets: Int,
    totalSets: Int,
    volumeLiftedKg: Double,
    notes: String,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Séance terminée ! 🎉",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Félicitations pour vos efforts. Voici le récapitulatif :",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Durée totale :", style = MaterialTheme.typography.bodyMedium)
                            Text(elapsedTime, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Séries complétées :", style = MaterialTheme.typography.bodyMedium)
                            Text("$completedSets / $totalSets", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume soulevé :", style = MaterialTheme.typography.bodyMedium)
                            Text("${volumeLiftedKg.toInt()} kg", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                if (notes.isNotBlank()) {
                    Text(
                        text = "Notes : $notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmSave) {
                Text("Enregistrer la séance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continuer l'entraînement")
            }
        }
    )
}

/** Séries du cycle d'un exercice du catalogue : répétitions, charges et repos fixes. */
@Composable
private fun CycleSetsContent(
    exercise: ActiveExerciseUiState,
    onWorkLoadChange: (String) -> Unit,
    onToggleComplete: (setIndex: Int) -> Unit
) {
    if (exercise.usesLoad) {
        OutlinedTextField(
            value = exercise.workLoadKg,
            onValueChange = onWorkLoadChange,
            label = { Text("Charge de travail (kg)") },
            supportingText = { Text("Chauffe 50 % · Hypertrophie 80 % de cette charge") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text(
            text = "Poids du corps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Série" to 1.5f, "Rép." to 1.2f, "Charge" to 1f, "Repos" to 1f).forEach { (title, w) ->
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(w)
            )
        }
        Text(
            text = "Fait",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    val load = exercise.workLoadKg.toDoubleOrNull() ?: 0.0
    exercise.sets.forEachIndexed { sIdx, setItem ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (setItem.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = setItem.phaseLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = ExerciseCycle.formatReps(setItem.targetReps, setItem.isSeconds, exercise.isUnilateral),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = when {
                    !exercise.usesLoad -> "PdC"
                    load <= 0 -> "— kg"
                    else -> "${ExerciseCycle.formatKg(setItem.targetWeightKg)} kg"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ExerciseCycle.formatRest(setItem.restSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                FilledTonalIconButton(
                    onClick = { onToggleComplete(sIdx) },
                    colors = if (setItem.isCompleted) {
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (setItem.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Valider la série ${setItem.phaseLabel}"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
fun EmptyExercisesWorkoutView(
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp)
            )
            Text(
                text = "Aucun exercice dans cette séance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Ajoutez des exercices à la volée pour commencer votre entraînement.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddExerciseClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un exercice")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestTimerBannerPreview() {
    PetraTheme {
        RestTimerBannerCard(
            exerciseName = "Squat barre",
            formattedRestTime = "01:15",
            remainingSeconds = 75,
            totalSeconds = 90,
            isPaused = false,
            onPauseToggle = {},
            onAdd30s = {},
            onSkip = {}
        )
    }
}
