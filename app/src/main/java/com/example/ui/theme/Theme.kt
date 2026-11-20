package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TurboExtendedColors(
    val elevatedCard: Color = FernSurfaceContainerLowest,
    val recessedSurface: Color = FernSurfaceContainerLow,
    val containerFill: Color = FernSurfaceContainer,
    val success: Color = FernSuccess,
    val onSuccess: Color = FernOnSuccess,
    val warning: Color = FernWarning,
    val onWarning: Color = FernOnWarning,
    val warningContainer: Color = FernWarningContainer,
    val onWarningContainer: Color = FernOnWarningContainer,
    val error: Color = FernError,
    val onError: Color = FernOnError,
    val errorContainer: Color = FernErrorContainer,
    val onErrorContainer: Color = FernOnErrorContainer,
    val hairlineBorder: Color = FernOutlineVariant
)

val LocalTurboColors = staticCompositionLocalOf { TurboExtendedColors() }

private val MidnightFernColorScheme = darkColorScheme(
    primary = FernPrimary,
    onPrimary = FernOnPrimary,
    primaryContainer = FernPrimaryContainer,
    onPrimaryContainer = FernOnPrimaryContainer,
    secondary = FernSecondary,
    onSecondary = FernOnSecondary,
    secondaryContainer = FernSecondaryContainer,
    onSecondaryContainer = FernOnSecondaryContainer,
    background = FernSurface,
    onBackground = FernOnSurface,
    surface = FernSurface,
    onSurface = FernOnSurface,
    surfaceVariant = FernSurfaceContainerLow,
    onSurfaceVariant = FernOnSurfaceVariant,
    surfaceContainerLowest = FernSurfaceContainerLowest,
    surfaceContainerLow = FernSurfaceContainerLow,
    surfaceContainer = FernSurfaceContainer,
    surfaceContainerHigh = FernSurfaceContainerHigh,
    outline = FernOutline,
    outlineVariant = FernOutlineVariant,
    inverseSurface = FernSurface,
    inverseOnSurface = FernOnSurface,
    inversePrimary = FernSecondary,
    error = FernError,
    onError = FernOnError,
    errorContainer = FernErrorContainer,
    onErrorContainer = FernOnErrorContainer,
    scrim = FernScrim
)

val TurboShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TurboModeTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTurboColors provides TurboExtendedColors()) {
        MaterialTheme(
            colorScheme = MidnightFernColorScheme,
            typography = TurboTypography,
            shapes = TurboShapes,
            content = content
        )
    }
}
