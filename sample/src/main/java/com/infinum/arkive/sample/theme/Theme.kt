package com.infinum.arkive.sample.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// TODO: when we have dark theme update it here
private val DarkColorPalette = darkColorScheme(
    primary = PrimaryBlue,
    background = NeutralWhite,
    secondary = NeutralBlack,
    secondaryContainer = NeutralGray,
    onPrimary = NeutralGreen,
)

private val LightColorPalette = darkColorScheme(
    primary = PrimaryBlue,
    background = NeutralWhite,
    secondary = NeutralBlack,
    secondaryContainer = NeutralGray,
    onPrimary = NeutralGreen,
)

@Composable
fun SampleApptheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
