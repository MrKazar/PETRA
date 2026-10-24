package com.example.porjectpetra.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.porjectpetra.ui.components.PetraCard
import com.example.porjectpetra.ui.components.PetraPrimaryButton
import com.example.porjectpetra.ui.components.PetraProgressBar
import com.example.porjectpetra.ui.components.PetraSecondaryButton
import com.example.porjectpetra.ui.components.SectionHeader
import com.example.porjectpetra.ui.theme.PetraError
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraTextMuted
import com.example.porjectpetra.update.UpdateManager
import com.example.porjectpetra.update.UpdateState

/** Bandeau de l'accueil : n'apparaît que lorsqu'une mise à jour est disponible ou en cours. */
@Composable
fun UpdateBanner(manager: UpdateManager, modifier: Modifier = Modifier) {
    val state by manager.state.collectAsStateWithLifecycle()
    val visible = state is UpdateState.Available || state is UpdateState.Downloading ||
        state is UpdateState.Installing || state is UpdateState.NeedsInstallPermission ||
        (state as? UpdateState.Error)?.info != null
    if (visible) {
        PetraCard(modifier = modifier) {
            SectionHeader("Mise à jour")
            UpdateContent(manager, state, showDismiss = true)
        }
    }
}

/** Bloc « Application » du profil : version installée et vérification manuelle. */
@Composable
fun UpdateSection(manager: UpdateManager, modifier: Modifier = Modifier) {
    val state by manager.state.collectAsStateWithLifecycle()
    PetraCard(modifier = modifier) {
        SectionHeader("Application")
        Text("Version installée : ${manager.currentVersion}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        when (state) {
            UpdateState.Idle -> PetraSecondaryButton("Vérifier les mises à jour", onClick = { manager.check() }, modifier = Modifier.fillMaxWidth())
            UpdateState.Checking -> Text("Vérification…", style = MaterialTheme.typography.bodyMedium, color = PetraTextMuted)
            UpdateState.UpToDate -> {
                Text("PETRA est à jour.", style = MaterialTheme.typography.bodyMedium, color = PetraSuccess)
                Spacer(Modifier.height(6.dp))
                PetraSecondaryButton("Vérifier à nouveau", onClick = { manager.check() }, modifier = Modifier.fillMaxWidth())
            }
            else -> UpdateContent(manager, state, showDismiss = false)
        }
    }
}

@Composable
private fun UpdateContent(manager: UpdateManager, state: UpdateState, showDismiss: Boolean) {
    val context = LocalContext.current
    when (state) {
        is UpdateState.Available -> {
            Text("Nouvelle version ${state.info.versionName} disponible", style = MaterialTheme.typography.titleMedium)
            if (state.info.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(state.info.notes.lines().take(6).joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PetraPrimaryButton("Mettre à jour", onClick = { manager.downloadAndInstall(state.info) }, modifier = Modifier.weight(1f))
                if (showDismiss) PetraSecondaryButton("Plus tard", onClick = { manager.dismiss() })
            }
        }
        is UpdateState.Downloading -> {
            Text("Téléchargement de la version ${state.info.versionName}… ${(state.progress * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            PetraProgressBar(state.progress)
        }
        is UpdateState.Installing -> Text(
            "Installation de la version ${state.info.versionName}… Confirme sur l'écran d'Android si on te le demande.",
            style = MaterialTheme.typography.bodyMedium
        )
        is UpdateState.NeedsInstallPermission -> {
            Text(
                "Pour se mettre à jour, PETRA doit être autorisée à installer des applications. " +
                    "Active « Autoriser cette source », reviens, puis touche « Mettre à jour ».",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PetraSecondaryButton("Ouvrir le réglage", onClick = { context.startActivity(manager.installPermissionIntent()) })
                PetraPrimaryButton("Mettre à jour", onClick = { manager.downloadAndInstall(state.info) }, modifier = Modifier.weight(1f))
            }
        }
        is UpdateState.Error -> {
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = PetraError)
            Spacer(Modifier.height(8.dp))
            val info = state.info
            if (info != null) {
                PetraSecondaryButton("Réessayer", onClick = { manager.downloadAndInstall(info) }, modifier = Modifier.fillMaxWidth())
            } else {
                PetraSecondaryButton("Réessayer", onClick = { manager.check() }, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> Unit
    }
}
