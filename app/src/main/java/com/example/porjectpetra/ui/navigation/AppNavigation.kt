package com.example.porjectpetra.ui.navigation

import androidx.compose.ui.graphics.Color
import com.example.porjectpetra.ui.components.PetraBackground
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.porjectpetra.FitnessApplication
import com.example.porjectpetra.ui.components.PetraEmblem
import com.example.porjectpetra.ui.home.HomeScreen
import com.example.porjectpetra.ui.home.HomeViewModel
import com.example.porjectpetra.ui.profile.ProfileScreen
import com.example.porjectpetra.ui.profile.ProfileViewModel
import com.example.porjectpetra.ui.program.ProgramDetailScreen
import com.example.porjectpetra.ui.program.ProgramListScreen
import com.example.porjectpetra.ui.program.ProgramViewModel
import com.example.porjectpetra.ui.routine.RoutineDetailScreen
import com.example.porjectpetra.ui.routine.RoutineDetailViewModel
import com.example.porjectpetra.ui.routine.RoutinesScreen
import com.example.porjectpetra.ui.routine.RoutinesViewModel
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraSurface
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted
import com.example.porjectpetra.ui.workout.ActiveWorkoutScreen
import com.example.porjectpetra.ui.workout.ActiveWorkoutViewModel
import com.example.porjectpetra.ui.workout.WorkoutHistoryScreen
import com.example.porjectpetra.ui.workout.WorkoutHistoryViewModel
import kotlinx.serialization.Serializable

// Destinations (Navigation 3 : la pile de navigation est un simple état observable)
@Serializable data object HomeKey : NavKey
@Serializable data object RoutinesKey : NavKey
@Serializable data class RoutineDetailKey(val routineId: Long) : NavKey
@Serializable data object SportKey : NavKey
@Serializable data class ProgramDetailKey(val programId: Long) : NavKey
@Serializable data object WorkoutHistoryKey : NavKey
@Serializable data class ActiveWorkoutKey(val programId: Long? = null) : NavKey
@Serializable data object ProfileKey : NavKey

enum class PetraTab(val label: String, val icon: ImageVector, val root: NavKey) {
    HOME("Accueil", Icons.Outlined.Home, HomeKey),
    ROUTINES("Routines", Icons.Outlined.Checklist, RoutinesKey),
    SPORT("Sport", Icons.Outlined.FitnessCenter, SportKey),
    PROFILE("Profil", Icons.Outlined.Person, ProfileKey)
}

private fun tabOf(key: NavKey?): PetraTab = when (key) {
    is RoutinesKey, is RoutineDetailKey -> PetraTab.ROUTINES
    is SportKey, is ProgramDetailKey, is WorkoutHistoryKey, is ActiveWorkoutKey -> PetraTab.SPORT
    is ProfileKey -> PetraTab.PROFILE
    else -> PetraTab.HOME
}

/** Ouvre un onglet sur sa racine. L'accueil reste toujours en bas de pile : « retour » y ramène. */
private fun NavBackStack<NavKey>.openTab(tab: PetraTab, vararg then: NavKey) {
    clear()
    add(HomeKey)
    if (tab != PetraTab.HOME) add(tab.root)
    then.forEach { add(it) }
}

private fun NavBackStack<NavKey>.popIfTop(key: NavKey) {
    if (lastOrNull() == key) removeAt(lastIndex)
}

@Composable
fun AppNavigation(
    app: FitnessApplication,
    programViewModel: ProgramViewModel,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(HomeKey)
    val current = backStack.lastOrNull()
    val currentTab = tabOf(current)
    // La séance en cours occupe tout l'écran : pas de barre de navigation pour éviter les sorties accidentelles.
    val showNavigation = current !is ActiveWorkoutKey
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        NavDisplay(
            backStack = backStack,
            modifier = contentModifier,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<HomeKey> {
                    val vm: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(app))
                    HomeScreen(
                        viewModel = vm,
                        onOpenRoutine = { id -> backStack.openTab(PetraTab.ROUTINES, RoutineDetailKey(id)) },
                        onOpenRoutines = { backStack.openTab(PetraTab.ROUTINES) },
                        onStartWorkout = { programId -> backStack.openTab(PetraTab.SPORT, ActiveWorkoutKey(programId)) },
                        onOpenSport = { backStack.openTab(PetraTab.SPORT) }
                    )
                }
                entry<RoutinesKey> {
                    val vm: RoutinesViewModel = viewModel(factory = RoutinesViewModel.provideFactory(app))
                    RoutinesScreen(
                        viewModel = vm,
                        onOpenRoutine = { id -> backStack.add(RoutineDetailKey(id)) }
                    )
                }
                entry<RoutineDetailKey> { key ->
                    val vm: RoutineDetailViewModel = viewModel(
                        factory = RoutineDetailViewModel.provideFactory(app, key.routineId)
                    )
                    RoutineDetailScreen(
                        viewModel = vm,
                        onBack = { backStack.popIfTop(key) }
                    )
                }
                entry<SportKey> {
                    ProgramListScreen(
                        viewModel = programViewModel,
                        onNavigateToDetail = { id -> backStack.add(ProgramDetailKey(id)) },
                        onStartWorkout = { programId -> backStack.add(ActiveWorkoutKey(programId)) },
                        onOpenHistory = { backStack.add(WorkoutHistoryKey) }
                    )
                }
                entry<ProgramDetailKey> { key ->
                    ProgramDetailScreen(
                        programId = key.programId,
                        viewModel = programViewModel,
                        onBackClick = { backStack.popIfTop(key) },
                        onStartWorkout = { programId -> backStack.add(ActiveWorkoutKey(programId)) }
                    )
                }
                entry<WorkoutHistoryKey> {
                    val vm: WorkoutHistoryViewModel = viewModel(factory = WorkoutHistoryViewModel.provideFactory(app))
                    WorkoutHistoryScreen(viewModel = vm, onBack = { backStack.popIfTop(WorkoutHistoryKey) })
                }
                entry<ActiveWorkoutKey> { key ->
                    val vm: ActiveWorkoutViewModel = viewModel(
                        factory = ActiveWorkoutViewModel.provideFactory(
                            programRepository = app.programRepository,
                            workoutSessionRepository = app.workoutSessionRepository,
                            catalog = app.exerciseCatalog,
                            programId = key.programId
                        )
                    )
                    // Le geste « retour » demande confirmation au lieu de perdre la séance en cours.
                    BackHandler { vm.openCancelDialog() }
                    ActiveWorkoutScreen(
                        viewModel = vm,
                        onNavigateBack = { backStack.popIfTop(key) },
                        catalog = app.exerciseCatalog
                    )
                }
                entry<ProfileKey> {
                    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.provideFactory(app))
                    ProfileScreen(viewModel = vm)
                }
            }
        )
    }

    if (wide) {
        // Tablettes, pliables ouverts, paysage : rail latéral.
        PetraBackground(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (showNavigation) {
                PetraNavigationRail(currentTab) { backStack.openTab(it) }
                VerticalDivider(color = PetraBorder)
            }
            content(Modifier.weight(1f).fillMaxHeight())
        }
        }
    } else {
        PetraBackground(modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showNavigation) PetraBottomBar(currentTab) { backStack.openTab(it) }
            }
        ) { innerPadding ->
            content(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding))
        }
        }
    }
}

@Composable
private fun PetraBottomBar(selected: PetraTab, onSelect: (PetraTab) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = PetraBorder)
        NavigationBar(containerColor = PetraSurface) {
            PetraTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PetraText,
                        selectedTextColor = PetraText,
                        indicatorColor = PetraOlive,
                        unselectedIconColor = PetraTextMuted,
                        unselectedTextColor = PetraTextMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun PetraNavigationRail(selected: PetraTab, onSelect: (PetraTab) -> Unit) {
    NavigationRail(
        containerColor = PetraSurface,
        header = { PetraEmblem(modifier = Modifier.padding(vertical = 12.dp)) }
    ) {
        PetraTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = PetraText,
                    selectedTextColor = PetraText,
                    indicatorColor = PetraOlive,
                    unselectedIconColor = PetraTextMuted,
                    unselectedTextColor = PetraTextMuted
                )
            )
        }
    }
}
