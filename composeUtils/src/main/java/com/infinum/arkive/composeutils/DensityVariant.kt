package com.infinum.arkive.composeutils

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density


@Composable
fun DensityVariant(
    scale: Float,
    component: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = scale,
            fontScale = LocalDensity.current.fontScale
        )
    ) {
        component()
    }
}

@Preview
@Composable
fun PreviewDensityVariant() {
    DensityVariant(1f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}

@Preview
@Composable
fun Preview2xDensityVariant() {
    DensityVariant(2f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}

@Preview
@Composable
fun Preview3xDensityVariant() {
    DensityVariant(3f) {
        Button(onClick = {}) {
            Text("Hello, World")
        }
    }
}
