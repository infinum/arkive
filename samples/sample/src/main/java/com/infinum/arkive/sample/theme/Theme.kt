package com.infinum.arkive.sample.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class PeakBrand { Forest, Sunset }

private const val DEEP_SHADE = 0.35f
private const val SOFT_SHADE = 0.2f

// Darker and lighter shades of the brand colour for gradients. Derived from primary so they
// follow the brand (Forest/Sunset) and light/dark mode.
val ColorScheme.primaryDeep: Color
    get() = lerp(primary, Color.Black, DEEP_SHADE)

val ColorScheme.primarySoft: Color
    get() = lerp(primary, Color.White, SOFT_SHADE)

// The brand the enclosing PeakTheme applied: a nested PeakTheme() keeps it instead of
// resetting to Forest, the way darkTheme follows isSystemInDarkTheme().
val LocalPeakBrand = staticCompositionLocalOf { PeakBrand.Forest }

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Cloud,
    primaryContainer = Moss,
    onPrimaryContainer = PineDark,
    secondary = Amber,
    onSecondary = Ink,
    tertiary = Sky,
    onTertiary = Cloud,
    background = Mist,
    onBackground = Ink,
    surface = Cloud,
    onSurface = Ink,
    surfaceVariant = Moss,
    onSurfaceVariant = Slate,
    outline = Line,
    error = Coral,
    onError = Cloud,
)

private val DarkColors = darkColorScheme(
    primary = PineLight,
    onPrimary = Cloud,
    primaryContainer = PineDark,
    onPrimaryContainer = Moss,
    secondary = Amber,
    onSecondary = Ink,
    tertiary = Sky,
    onTertiary = Cloud,
    background = Night,
    onBackground = Fog,
    surface = NightSurface,
    onSurface = Fog,
    surfaceVariant = NightVariant,
    onSurfaceVariant = FogDim,
    outline = NightLine,
    error = Coral,
    onError = Cloud,
)

@Composable
fun PeakTheme(
    brand: PeakBrand = LocalPeakBrand.current,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalPeakBrand provides brand) {
        MaterialTheme(
            colorScheme = when (brand) {
                PeakBrand.Forest -> colors
                PeakBrand.Sunset -> colors.toSunset(darkTheme)
            },
            typography = Typography,
            shapes = shapes,
            content = content,
        )
    }
}

private fun ColorScheme.toSunset(darkTheme: Boolean): ColorScheme = if (darkTheme) {
    copy(primary = EmberLight, primaryContainer = EmberDark, onPrimaryContainer = Peach)
} else {
    copy(primary = Ember, primaryContainer = Peach, onPrimaryContainer = EmberDark)
}
