package com.infinum.arkive.composeutils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection


@Composable
fun LayoutDirectionVariant(
    isLtr: Boolean,
    component: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isLtr) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        component()
    }
}

@Preview
@Composable
fun PreviewEnglishLtrLayoutDirectionVariant() {
    LayoutDirectionVariant(isLtr = true) {
        Button(onClick = {}) {
            Text(
                "Hello, World",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun PreviewEnglishRtlLayoutDirectionVariant() {
    LayoutDirectionVariant(isLtr = false) {
        Button(onClick = {}) {
            Text(
                "Hello, World",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun PreviewArabicLtrLayoutDirectionVariant() {
    LayoutDirectionVariant(isLtr = true) {
        Button(onClick = {}) {
            Text(
                "اهلا, بالعالم",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun PreviewArabicRtlLayoutDirectionVariant() {
    LayoutDirectionVariant(isLtr = false) {
        Button(onClick = {}) {
            Text(
                "اهلا, بالعالم",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
