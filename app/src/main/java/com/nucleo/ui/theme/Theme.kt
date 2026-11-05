package com.nucleo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nucleo.game.Settings

private val NucleoScheme = darkColorScheme(
    primary = NucleoColors.Forge,
    onPrimary = NucleoColors.OnAccent,
    secondary = NucleoColors.Spectrum,
    onSecondary = NucleoColors.OnAccent,
    tertiary = NucleoColors.Reaction,
    background = NucleoColors.Bg,
    onBackground = NucleoColors.TextPrimary,
    surface = NucleoColors.Surface,
    onSurface = NucleoColors.TextPrimary,
    surfaceVariant = NucleoColors.Surface2,
    onSurfaceVariant = NucleoColors.TextSecondary,
    surfaceContainer = NucleoColors.Surface,
    surfaceContainerLow = NucleoColors.Surface,
    surfaceContainerHigh = NucleoColors.Surface2,
    outline = NucleoColors.Track,
    error = NucleoColors.Warning,
)

/** Wraps the app in Nucleo's colors, type, text-size setting and accessibility options. */
@Composable
fun NucleoTheme(settings: Settings = Settings(), content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        // In-game text size multiplies the system font scale.
        LocalDensity provides Density(density.density, density.fontScale * settings.textScale),
        LocalClueColors provides if (settings.colorBlind) ClueColors.ColorBlind else ClueColors.Default,
        LocalReduceMotion provides settings.reduceMotion,
    ) {
        MaterialTheme(colorScheme = NucleoScheme, typography = NucleoTypography, content = content)
    }
}
