package com.example.porjectpetra.ui.profile

import androidx.compose.ui.graphics.Color
import com.example.porjectpetra.ui.components.PetraLogo
import com.example.porjectpetra.ui.components.PetraWordmark
import com.example.porjectpetra.ui.components.PetraDestructiveButton
import com.example.porjectpetra.ui.components.PETRA_MOTTO
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.ui.components.ComingSoonNote
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraScreenHeader
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.theme.PetraTextMuted

private enum class ClearTarget { TASKS, WORKOUTS }

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by rememberSaveable { mutableStateOf<ClearTarget?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PetraScreenHeader(
                title = "Profil",
                subtitle = "Discipline · Progrès · Objectif",
                contentPadding = PaddingValues(vertical = 12.dp)
            )
            PetraCard {
                PetraLogo(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    PETRA_MOTTO.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PetraTextMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            val current = settings
            if (current != null) {
                PetraCard {
                    SectionHeader("Identité")
                    NameField(initial = current.displayName, onCommit = viewModel::setDisplayName)
                }

                PetraCard {
                    SectionHeader("Objectifs")
                    Text("Séances de sport par semaine", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { viewModel.setWeeklySessionGoal(current.weeklySessionGoal - 1) },
                            enabled = current.weeklySessionGoal > 1
                        ) { Icon(Icons.Default.Remove, contentDescription = "Moins") }
                        Text(
                            "${current.weeklySessionGoal}",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { viewModel.setWeeklySessionGoal(current.weeklySessionGoal + 1) },
                            enabled = current.weeklySessionGoal < 14
                        ) { Icon(Icons.Default.Add, contentDescription = "Plus") }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Utilisé pour le bilan de la semaine sur l'accueil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PetraTextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    ComingSoonNote("objectifs personnels détaillés (poids, performances).")
                }
            }

            PetraCard {
                SectionHeader("Rappels")
                Text(
                    "Les rappels et notifications ne sont pas encore disponibles dans cette version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PetraTextMuted
                )
                Spacer(Modifier.height(4.dp))
                ComingSoonNote("rappels configurables et désactivables par routine ou par tâche.")
            }

            PetraCard {
                SectionHeader("Données")
                Text(
                    "Toutes tes données sont enregistrées sur ce téléphone uniquement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PetraTextMuted
                )
                Spacer(Modifier.height(8.dp))
                PetraDestructiveButton("Effacer l'historique des tâches", onClick = { confirmClear = ClearTarget.TASKS }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                PetraDestructiveButton("Effacer l'historique des séances", onClick = { confirmClear = ClearTarget.WORKOUTS }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    confirmClear?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmClear = null },
            title = {
                Text(if (target == ClearTarget.TASKS) "Effacer l'historique des tâches ?" else "Effacer l'historique des séances ?")
            },
            text = {
                Text(
                    if (target == ClearTarget.TASKS) "Les tâches cochées, sautées ou reportées seront oubliées. Tes routines et tâches restent en place."
                    else "Toutes les séances enregistrées seront supprimées. Tes programmes restent en place."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == ClearTarget.TASKS) viewModel.clearTaskHistory() else viewModel.clearWorkoutHistory()
                    confirmClear = null
                }) { Text("Effacer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = null }) { Text("Annuler") } }
        )
    }
}

/** Champ prénom : enregistré à la validation du clavier ou à la perte du focus. */
@Composable
private fun NameField(initial: String, onCommit: (String) -> Unit) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { value = it.take(40) },
        label = { Text("Prénom (affiché sur l'accueil)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onCommit(value)
            focusManager.clearFocus()
        }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused && value != initial) onCommit(value) }
    )
}
