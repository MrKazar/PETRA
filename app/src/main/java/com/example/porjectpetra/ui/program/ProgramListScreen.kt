package com.example.porjectpetra.ui.program

import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.graphics.Color
import com.example.porjectpetra.data.model.SportVocabulary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.data.local.entity.Exercise
import com.example.porjectpetra.data.local.entity.ProgramWithExercises
import com.example.porjectpetra.data.local.entity.WorkoutProgram
import com.example.porjectpetra.ui.program.components.ProgramFormDialog
import com.example.porjectpetra.ui.theme.PetraTheme

val CATEGORY_FILTERS = listOf("Tout") + SportVocabulary.programCategories.filter { it != "Autre" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramListScreen(
    viewModel: ProgramViewModel,
    onNavigateToDetail: (programId: Long) -> Unit,
    onStartWorkout: (programId: Long?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: (() -> Unit)? = null,
    onOpenPlan: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val programsWithExercises by viewModel.programsWithExercises.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val filteredPrograms = remember(programsWithExercises, uiState.selectedCategoryFilter) {
        if (uiState.selectedCategoryFilter == "Tout") {
            programsWithExercises
        } else {
            programsWithExercises.filter { it.program.category.equals(uiState.selectedCategoryFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Sport · Programmes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    if (onOpenPlan != null) {
                        IconButton(onClick = onOpenPlan) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Planning sur 2 semaines")
                        }
                    }
                    if (onOpenHistory != null) {
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, contentDescription = "Historique des séances")
                        }
                    }
                    FilledTonalButton(
                        onClick = { onStartWorkout(null) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Séance Libre", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddProgramDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "Ajouter") },
                text = { Text("Nouveau Programme") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filters Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORY_FILTERS.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == category,
                        onClick = { viewModel.setCategoryFilter(category) },
                        label = { Text(category) }
                    )
                }
            }

            if (filteredPrograms.isEmpty()) {
                EmptyProgramsView(
                    onAddProgramClick = { viewModel.openAddProgramDialog() },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPrograms, key = { it.program.id }) { item ->
                        ProgramCard(
                            programWithExercises = item,
                            onClick = {
                                viewModel.selectProgram(item.program.id)
                                onNavigateToDetail(item.program.id)
                            },
                            onStartWorkout = { onStartWorkout(item.program.id) },
                            onEditClick = { viewModel.openEditProgramDialog(item.program) },
                            onDeleteClick = { viewModel.deleteProgram(item.program.id) }
                        )
                    }
                }
            }
        }

        if (uiState.showProgramDialog) {
            ProgramFormDialog(
                programToEdit = uiState.programToEdit,
                onDismiss = { viewModel.closeProgramDialog() },
                onConfirm = { name, description, category, targetFrequency ->
                    viewModel.saveProgram(name, description, category, targetFrequency)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramCard(
    programWithExercises: ProgramWithExercises,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val program = programWithExercises.program
    val exercises = programWithExercises.exercises

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = program.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.height(14.dp)
                        )
                        Text(
                            text = "${program.targetFrequencyPerWeek}x / sem.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (program.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercises summary chips
            if (exercises.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    exercises.take(4).forEach { ex ->
                        AssistChip(
                            onClick = onClick,
                            label = { Text(ex.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (exercises.size > 4) {
                        AssistChip(
                            onClick = onClick,
                            label = { Text("+${exercises.size - 4} autres", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Aucun exercice configuré. Cliquez pour ajouter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${exercises.size} exercice(s) • ${exercises.sumOf { it.targetSets }} séries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartWorkout,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Démarrer", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyProgramsView(
    onAddProgramClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier
                .height(72.dp)
                .width(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucun programme trouvé",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Créez votre premier programme d'entraînement personnalisé pour commencer votre suivi sportif.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        ExtendedFloatingActionButton(
            onClick = onAddProgramClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Créer un programme") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgramCardPreview() {
    PetraTheme {
        ProgramCard(
            programWithExercises = ProgramWithExercises(
                program = WorkoutProgram(
                    id = 1,
                    name = "Corps entier — Fondamentaux",
                    description = "Entraînement complet du corps visant la force et l'endurance.",
                    category = "Corps entier",
                    targetFrequencyPerWeek = 3
                ),
                exercises = listOf(
                    Exercise(programId = 1, name = "Squat", muscleGroup = "Jambes", targetSets = 4, targetReps = 10, targetWeightKg = 60.0),
                    Exercise(programId = 1, name = "Développé couché", muscleGroup = "Pectoraux", targetSets = 4, targetReps = 8, targetWeightKg = 50.0),
                    Exercise(programId = 1, name = "Rowing barre", muscleGroup = "Dos", targetSets = 3, targetReps = 10, targetWeightKg = 40.0)
                )
            ),
            onClick = {},
            onStartWorkout = {},
            onEditClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
