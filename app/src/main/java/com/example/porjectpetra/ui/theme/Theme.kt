package com.example.porjectpetra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// PETRA est volontairement toujours sombre et n'utilise pas les couleurs dynamiques d'Android,
// pour garder la même identité sur tous les appareils.
private val PetraColorScheme = darkColorScheme(
    primary = PetraOliveLight,
    onPrimary = PetraBlack,
    primaryContainer = PetraOlive,
    onPrimaryContainer = PetraText,
    inversePrimary = PetraOlive,
    secondary = PetraKhaki,
    onSecondary = PetraBlack,
    secondaryContainer = PetraSurfaceAlt,
    onSecondaryContainer = PetraText,
    tertiary = PetraSuccess,
    onTertiary = PetraBlack,
    tertiaryContainer = PetraOlive,
    onTertiaryContainer = PetraText,
    background = PetraBlack,
    onBackground = PetraText,
    surface = PetraSurface,
    onSurface = PetraText,
    surfaceVariant = PetraSurfaceAlt,
    onSurfaceVariant = PetraTextMuted,
    surfaceTint = PetraOlive,
    surfaceDim = PetraBlack,
    surfaceBright = PetraSurfaceAlt,
    surfaceContainerLowest = PetraBlack,
    surfaceContainerLow = PetraSurface,
    surfaceContainer = PetraSurface,
    surfaceContainerHigh = PetraSurfaceAlt,
    surfaceContainerHighest = PetraSurfaceAlt,
    inverseSurface = PetraText,
    inverseOnSurface = PetraBlack,
    outline = PetraBorder,
    outlineVariant = PetraBorder,
    error = PetraError,
    onError = PetraBlack,
    errorContainer = PetraDestructive,
    onErrorContainer = PetraText,
    scrim = PetraBlack
)

private val PetraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

@Composable
fun PetraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PetraColorScheme,
        typography = Typography,
        shapes = PetraShapes,
        content = content
    )
}
