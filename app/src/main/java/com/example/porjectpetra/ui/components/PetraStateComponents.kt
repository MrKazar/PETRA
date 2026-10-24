package com.example.porjectpetra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraDestructive
import com.example.porjectpetra.ui.theme.PetraError
import com.example.porjectpetra.ui.theme.PetraKhaki
import com.example.porjectpetra.ui.theme.PetraOliveLight
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraSurfaceAlt
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

/** Bouton secondaire : contour clair sur fond sombre. */
@Composable
fun PetraSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) PetraText else PetraBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PetraText)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Bouton destructif : rouge sombre, pour supprimer ou abandonner. */
@Composable
fun PetraDestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PetraDestructive, contentColor = PetraText)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Cercle de progression (ex. « 68 % »). */
@Composable
fun PetraProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 96.dp, label: String? = null) {
    val p = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(PetraSurfaceAlt, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (p > 0f) {
                drawArc(
                    if (p >= 1f) PetraSuccess else PetraOliveLight, -90f, 360f * p, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(label ?: "${(p * 100).toInt()} %", style = MaterialTheme.typography.titleMedium, color = PetraText)
    }
}

/** États d'écran de la charte : chargement, aucune donnée, erreur. */
enum class PetraStateKind { LOADING, EMPTY, ERROR }

@Composable
fun PetraStateMessage(
    kind: PetraStateKind,
    modifier: Modifier = Modifier,
    title: String = when (kind) {
        PetraStateKind.LOADING -> "Chargement"
        PetraStateKind.EMPTY -> "Aucune donnée"
        PetraStateKind.ERROR -> "Erreur"
    },
    message: String = when (kind) {
        PetraStateKind.LOADING -> ""
        PetraStateKind.EMPTY -> "Commence par ajouter ta première donnée."
        PetraStateKind.ERROR -> "Une erreur est survenue. Réessaye plus tard."
    },
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (kind) {
            PetraStateKind.LOADING -> CircularProgressIndicator(color = PetraOliveLight, trackColor = PetraSurfaceAlt, modifier = Modifier.size(36.dp))
            PetraStateKind.EMPTY -> PetraEmblem(size = 44.dp)
            PetraStateKind.ERROR -> Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = PetraError, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = PetraText, textAlign = TextAlign.Center)
        if (message.isNotEmpty()) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = PetraTextMuted, textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}

/**
 * Carte d'accès rapide de la charte : icône linéaire, titre, sous-titre, chevron,
 * et bouton principal optionnel (ex. « Séance haut du corps · Musculation · 45 min · Démarrer »).
 */
@Composable
fun PetraActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    PetraCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PetraKhaki, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = PetraText)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PetraTextMuted)
        }
        if (buttonText != null && onButtonClick != null) {
            Spacer(Modifier.height(12.dp))
            PetraPrimaryButton(text = buttonText, onClick = onButtonClick, modifier = Modifier.fillMaxWidth())
        }
    }
}
