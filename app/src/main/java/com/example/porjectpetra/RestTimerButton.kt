package com.example.porjectpetra.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Gros bouton qui lance le minuteur flottant.
 *
 * Repos simple :   RestTimerButton(steps = singleRest(exercise.restTimeSeconds), startRestNow = true)
 * Séance complète: RestTimerButton(steps = listeDesSeries, startRestNow = false)
 */
@Composable
fun RestTimerButton(
    steps: List<RestStep>,
    startRestNow: Boolean,
    modifier: Modifier = Modifier,
    text: String = if (startRestNow) "Lancer le repos" else "Lancer le minuteur de séance"
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* le minuteur fonctionne même si refusé */ }

    Button(
        onClick = {
            if (!RestTimerController.canDrawOverlays(context)) {
                showPermissionDialog = true
            } else {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                RestTimerController.start(context, steps, startRestNow)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(Icons.Default.Timer, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Afficher par-dessus les autres apps") },
            text = {
                Text(
                    "Pour que le minuteur reste visible au-dessus de YouTube ou de toute autre app, " +
                        "active « Afficher par-dessus les autres applications » pour cette app, " +
                        "puis reviens appuyer sur le bouton."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    context.startActivity(RestTimerController.overlaySettingsIntent(context))
                }) { Text("Ouvrir les réglages") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Annuler") }
            }
        )
    }
}
