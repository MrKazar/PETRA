package com.example.porjectpetra.ui.program

import androidx.compose.ui.graphics.Color
import com.example.porjectpetra.data.catalog.CatalogExercise
import com.example.porjectpetra.data.catalog.ExerciseCycle
import com.example.porjectpetra.ui.program.components.CatalogExerciseDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.ui.program.components.ExerciseFormDialog
import com.example.porjectpetra.ui.program.components.ProgramFormDialog
import com.example.porjectpetra.ui.theme.PetraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    programId: Long,
    viewModel: ProgramViewModel,
    onBackClick: () -> Unit,
    onStartWorkout: (programId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val programWithExercises by viewModel.selectedProgramWithExercises.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProgramMenu by remember { mutableStateOf(false) }

    LaunchedEffect(programId) {
        viewModel.selectProgram(programId)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val program = programWithExercises?.program
    val exercises = programWithExercises?.exercises ?: emptyList()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = program?.name ?: "Détail du Programme",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (program != null) {
                        Box {
                            IconButton(onClick = { showProgramMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = showProgramMenu,
                                onDismissRequest = { showProgramMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Modifier le programme") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showProgramMenu = false
                                        viewModel.openEditProgramDialog(program)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer le programme") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        showProgramMenu = false
                                        viewModel.deleteProgram(program.id)
                                        onBackClick()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddExerciseDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "Ajouter") },
                text = { Text("Ajouter Exercice") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (program == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chargement du programme...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Program Header Summary Card
                item {
                    ProgramHeaderCard(
                        program = program,
                        exerciseCount = exercises.size,
                        totalSets = exercises.sumOf { it.targetSets },
                        onStartWorkout = { onStartWorkout(program.id) }
                    )
                }

                // Exercises List Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exercices (${exercises.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${exercises.sumOf { it.targetSets }} séries au total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (exercises.isEmpty()) {
                    item {
                        EmptyExercisesView(
                            onAddExerciseClick = { viewModel.openAddExerciseDialog() }
                        )
                    }
                } else {
                    itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                        ExerciseCard(
                            orderNumber = index + 1,
                            exercise = exercise,
                            cycleSummary = viewModel.catalog.byId(exercise.catalogId)?.let { cycleSummary(it, exercise.targetWeightKg) },
                            onEditClick = { viewModel.openEditExerciseDialog(exercise) },
                            onDeleteClick = { viewModel.deleteExercise(exercise.id) }
                        )
                    }
                }
            }
        }

        if (uiState.showProgramDialog && program != null) {
            ProgramFormDialog(
                programToEdit = uiState.programToEdit ?: program,
                onDismiss = { viewModel.closeProgramDialog() },
                onConfirm = { name, description, category, targetFrequency ->
                    viewModel.saveProgram(name, description, category, targetFrequency)
                }
            )
        }

        val editing = uiState.exerciseToEdit
        if (uiState.showExerciseDialog && (editing == null || editing.catalogId != null)) {
            // Nouveaux exercices : toujours depuis le catalogue (cycle fixe).
            CatalogExerciseDialog(
                catalog = viewModel.catalog,
                initialCatalogId = editing?.catalogId,
                initialWorkLoadKg = editing?.targetWeightKg ?: 0.0,
                initialNotes = editing?.notes ?: "",
                confirmLabel = if (editing == null) "Ajouter" else "Enregistrer",
                onDismiss = { viewModel.closeExerciseDialog() },
                onConfirm = { catalogExercise, workLoad, notes ->
                    viewModel.saveCatalogExercise(catalogExercise, workLoad, notes)
                }
            )
        } else if (uiState.showExerciseDialog) {
            // Exercice libre créé avant le catalogue : modifiable avec l'ancien formulaire.
            ExerciseFormDialog(
                exerciseToEdit = uiState.exerciseToEdit,
                onDismiss = { viewModel.closeExerciseDialog() },
                onConfirm = { name, muscleGroup, targetSets, targetReps, targetWeightKg, targetDuration, restTime, notes ->
                    viewModel.saveExercise(
                        name, muscleGroup, targetSets, targetReps, targetWeightKg, targetDuration, restTime, notes
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramHeaderCard(
    program: WorkoutProgram,
    exerciseCount: Int,
    totalSets: Int,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = program.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(16.dp)
                    )
                    Text(
                        text = "${program.targetFrequencyPerWeek}x par semaine",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$exerciseCount exercices",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalSets séries totales",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Démarrer l'entraînement", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseCard(
    orderNumber: Int,
    exercise: Exercise,
    cycleSummary: String? = null,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$orderNumber",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = exercise.muscleGroup,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exercise Metrics Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cycleSummary != null) {
                    MetricChip(icon = Icons.Default.Repeat, text = cycleSummary)
                    if (exercise.targetWeightKg > 0) {
                        MetricChip(icon = Icons.Default.Scale, text = "Travail ${ExerciseCycle.formatKg(exercise.targetWeightKg)} kg")
                    }
                    return@FlowRow
                }

                MetricChip(
                    icon = Icons.Default.Repeat,
                    text = "${exercise.targetSets} séries x ${exercise.targetReps} rép."
                )

                if (exercise.targetWeightKg > 0) {
                    MetricChip(
                        icon = Icons.Default.Scale,
                        text = "${exercise.targetWeightKg} kg"
                    )
                }

                if (exercise.targetDurationSeconds > 0) {
                    MetricChip(
                        icon = Icons.Default.Timer,
                        text = "${exercise.targetDurationSeconds}s durée"
                    )
                }

                MetricChip(
                    icon = Icons.Default.Schedule,
                    text = "${exercise.restTimeSeconds}s repos"
                )
            }

            if (exercise.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Consignes: ${exercise.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyExercisesView(
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aucun exercice dans ce programme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ajoutez des exercices avec nombre de séries, répétitions, charge cible et temps de repos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExtendedFloatingActionButton(
                onClick = onAddExerciseClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ajouter un exercice") }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseCardPreview() {
    PetraTheme {
        ExerciseCard(
            orderNumber = 1,
            exercise = Exercise(
                id = 101,
                programId = 1,
                name = "Squat barre",
                muscleGroup = "Jambes",
                targetSets = 4,
                targetReps = 10,
                targetWeightKg = 80.0,
                restTimeSeconds = 90,
                notes = "Pousser bien sur les talons, dos droit."
            ),
            onEditClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Ex. « Chauffe 12 · Travail 2 × 6 · Hypertrophie 10 rép. » */
private fun cycleSummary(exercise: CatalogExercise, workLoadKg: Double): String {
    exercise.cardio?.let { c ->
        return "Chauffe ${c.warmupMinutes} min · Effort ${c.effortMinutes} min · Retour au calme ${c.cooldownMinutes} min"
    }
    val sets = ExerciseCycle.build(exercise, workLoadKg)
    val unit = if (exercise.work.isSeconds) "s" else "rép."
    val side = if (exercise.isUnilateral) " / côté" else ""
    return "Chauffe ${sets[0].reps} · Travail 2 × ${sets[1].reps} · Hypertrophie ${sets[3].reps} $unit$side"
}
