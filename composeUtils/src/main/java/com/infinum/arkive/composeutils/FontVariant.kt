package com.infinum.arkive.composeutils

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density


@Composable
fun FontVariant(
    scale: Float,
    component: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = scale
        )
    ) {
        component()
    }
}

@Preview
@Composable
fun PreviewFontVariant() {
    FontVariant(1f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}

@Preview
@Composable
fun Preview2xFontVariant() {
    FontVariant(2f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}

@Preview
@Composable
fun Preview3xFontVariant() {
    FontVariant(3f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}
