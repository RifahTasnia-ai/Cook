package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TurboExtendedColors(
    val elevatedCard: Color = SageElevatedCard,
    val recessedSurface: Color = SageRecessedInset,
    val containerFill: Color = SageRecessedInset,
    val hairlineBorder: Color = SageMistBorder,
    val success: Color = SageDeepAccent,
    val onSuccess: Color = SageElevatedCard,
    val activeBadgeFill: Color = SageActiveBadgeFill,
    val activeBadgeText: Color = SageActiveBadgeText,
    val warning: Color = SageLazinessBar,
    val onWarning: Color = SageElevatedCard,
    val warningContainer: Color = SageLazinessFill,
    val onWarningContainer: Color = SageLazinessText,
    val error: Color = SageLazinessBar,
    val onError: Color = SageElevatedCard,
    val errorContainer: Color = SageLazinessFill,
    val onErrorContainer: Color = SageLazinessText,
    val criticalBadgeFill: Color = SageCriticalFill,
    val criticalBadgeText: Color = SageCriticalText,
    val darkContrastCanvas: Color = SagePineInk,
    val darkContrastTrack: Color = SageDarkTrack,
    val darkContrastText: Color = SageElevatedCard,
    val darkContrastMuted: Color = SageMistBorder,
    val modalBackdrop: Color = SagePineInk.copy(alpha = 0.45f),
    val heatmapLevel0: Color = HeatmapLevel0Missed,
    val heatmapLevel1: Color = HeatmapLevel1HeavyLazy,
    val heatmapLevel2: Color = HeatmapLevel2NearPass,
    val heatmapLevel3: Color = HeatmapLevel3ActivePass,
    val heatmapLevel4: Color = HeatmapLevel4StrongActive,
    val heatmapLevel5: Color = HeatmapLevel5PeakTurbo
) {
    fun heatmapColorForScore(score: Double): Color = when {
        score <= 0.0 -> heatmapLevel0
        score < 30.0 -> heatmapLevel1
        score < 50.0 -> heatmapLevel2
        score < 70.0 -> heatmapLevel3
        score < 90.0 -> heatmapLevel4
        else -> heatmapLevel5
    }
}

val LocalTurboColors = staticCompositionLocalOf { TurboExtendedColors() }

private val BotanicalSageColorScheme = lightColorScheme(
    primary = SagePineInk,
    onPrimary = SageElevatedCard,
    primaryContainer = SageMintAccent,
    onPrimaryContainer = SagePineInk,
    secondary = SageDeepAccent,
    onSecondary = SageElevatedCard,
    secondaryContainer = SageRecessedInset,
    onSecondaryContainer = SagePineInk,
    tertiary = SageMintAccent,
    onTertiary = SagePineInk,
    background = SageDewCanvas,
    onBackground = SagePineInk,
    surface = SageDewCanvas,
    onSurface = SagePineInk,
    surfaceVariant = SageRecessedInset,
    onSurfaceVariant = SageSecondaryInk,
    surfaceContainerLowest = SageElevatedCard,
    surfaceContainerLow = SageRecessedInset,
    surfaceContainer = SageRecessedInset,
    surfaceContainerHigh = SageMistBorder,
    outline = SageSecondaryInk,
    outlineVariant = SageMistBorder,
    inverseSurface = SagePineInk,
    inverseOnSurface = SageElevatedCard,
    inversePrimary = SageMintAccent,
    error = SageLazinessBar,
    onError = SageElevatedCard,
    errorContainer = SageLazinessFill,
    onErrorContainer = SageLazinessText,
    scrim = SagePineInk
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
            colorScheme = BotanicalSageColorScheme,
            typography = TurboTypography,
            shapes = TurboShapes,
            content = content
        )
    }
}
