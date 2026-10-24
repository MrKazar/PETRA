package com.example.porjectpetra.ui.components

import com.example.porjectpetra.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.porjectpetra.ui.theme.PetraBlack
import com.example.porjectpetra.ui.theme.PetraBorder
import com.example.porjectpetra.ui.theme.PetraFrenchBlue
import com.example.porjectpetra.ui.theme.PetraFrenchRed
import com.example.porjectpetra.ui.theme.PetraFrenchWhite
import com.example.porjectpetra.ui.theme.PetraKhaki
import com.example.porjectpetra.ui.theme.PetraOlive
import com.example.porjectpetra.ui.theme.PetraOliveLight
import com.example.porjectpetra.ui.theme.PetraSuccess
import com.example.porjectpetra.ui.theme.PetraSurface
import com.example.porjectpetra.ui.theme.PetraSurfaceAlt
import com.example.porjectpetra.ui.theme.PetraText
import com.example.porjectpetra.ui.theme.PetraTextMuted

/** Devise PETRA. */
const val PETRA_MOTTO = "Plus fort aujourd'hui, plus utile demain."

/** Emblème PETRA officiel (massif et casque), fond transparent : res/drawable-nodpi/petra_emblem.png. */
@Composable
fun PetraEmblem(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Image(
        painter = painterResource(R.drawable.petra_emblem),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

/** Logo complet : emblème, « PETRA », liseré tricolore et devise. */
@Composable
fun PetraLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.petra_logo_full),
        contentDescription = "PETRA — Discipline, progrès, objectif",
        modifier = modifier
    )
}

/** Fond de l'application : texture topographique discrète. */
@Composable
fun PetraBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(PetraBlack)) {
        Image(
            painter = painterResource(R.drawable.petra_topo_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}

/** Version horizontale du logo : emblème + « PETRA » + liseré tricolore. */
@Composable
fun PetraWordmark(modifier: Modifier = Modifier, emblemSize: Dp = 40.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PetraEmblem(size = emblemSize)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("PETRA", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp), color = PetraText)
            TricolorStripe(width = 48.dp)
        }
    }
}

/** Fin liseré tricolore, accent discret. */
@Composable
fun TricolorStripe(modifier: Modifier = Modifier, width: Dp = 36.dp, height: Dp = 3.dp) {
    Row(modifier = modifier.width(width).height(height).clip(RoundedCornerShape(2.dp))) {
        Box(Modifier.weight(1f).height(height).background(PetraFrenchBlue))
        Box(Modifier.weight(1f).height(height).background(PetraFrenchWhite))
        Box(Modifier.weight(1f).height(height).background(PetraFrenchRed))
    }
}

/** En-tête d'écran PETRA : emblème + titre en capitales + sous-titre optionnel. */
@Composable
fun PetraScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetraEmblem(size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = PetraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            TricolorStripe()
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
            }
        }
        actions()
    }
}

/** Titre de section en capitales (ex. « ROUTINE DU MATIN »). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = PetraKhaki,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

/** Carte PETRA standard : surface sombre, liseré discret. */
@Composable
fun PetraCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = PetraSurface, contentColor = PetraText)
    val border = BorderStroke(1.dp, PetraBorder)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Barre de progression PETRA (vert olive clair sur fond secondaire). */
@Composable
fun PetraProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = if (progress >= 1f) PetraSuccess else PetraOliveLight,
        trackColor = PetraSurfaceAlt,
        drawStopIndicator = {}
    )
}

/** Bouton d'action principale (vert olive). */
@Composable
fun PetraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PetraOlive, contentColor = PetraText),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

enum class TaskVisualState { PENDING, DONE, SKIPPED, POSTPONED }

/**
 * Ligne de tâche cochable en un seul appui. Le menu « ⋮ » donne accès à Sauter / Reporter / Modifier.
 * Un appui long ouvre aussi ce menu.
 */
@Composable
fun TaskCheckRow(
    title: String,
    state: TaskVisualState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    menuItems: List<Pair<String, () -> Unit>> = emptyList()
) {
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val done = state == TaskVisualState.DONE
    val inactive = state == TaskVisualState.SKIPPED || state == TaskVisualState.POSTPONED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                role = Role.Checkbox,
                onClickLabel = if (done) "Marquer à faire" else "Marquer terminée",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                onLongClick = if (menuItems.isNotEmpty()) ({ menuOpen = true }) else null
            )
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckMark(state = state)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = when {
                    done -> PetraSuccess
                    inactive -> PetraTextMuted
                    else -> PetraText
                },
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val stateLabel = when (state) {
                TaskVisualState.SKIPPED -> "Sautée aujourd'hui"
                TaskVisualState.POSTPONED -> "Reportée à demain"
                else -> null
            }
            val secondary = listOfNotNull(stateLabel, supportingText).joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(secondary, style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
            }
        }
        if (menuItems.isNotEmpty()) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options de la tâche", tint = PetraTextMuted)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    menuItems.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                menuOpen = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckMark(state: TaskVisualState) {
    val done = state == TaskVisualState.DONE
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (done) PetraSuccess else PetraSurfaceAlt),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PetraSurface, modifier = Modifier.size(18.dp))
        } else {
            Canvas(Modifier.size(28.dp)) {
                drawCircle(
                    color = if (state == TaskVisualState.PENDING) PetraOliveLight else PetraBorder,
                    radius = size.minDimension / 2 - 1.5.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

/** Indicateur chiffré compact (ex. « 82 % — Tâches réalisées »). */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PetraSurfaceAlt)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = PetraText)
        Text(label, style = MaterialTheme.typography.bodySmall, color = PetraTextMuted)
    }
}

/** Mention honnête pour une fonctionnalité pas encore disponible. */
@Composable
fun ComingSoonNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "En préparation — $text",
        style = MaterialTheme.typography.bodySmall,
        color = PetraTextMuted,
        modifier = modifier
    )
}
