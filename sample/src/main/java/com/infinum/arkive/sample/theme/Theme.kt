package com.infinum.arkive.sample.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun PeakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = shapes,
        content = content,
    )
}
