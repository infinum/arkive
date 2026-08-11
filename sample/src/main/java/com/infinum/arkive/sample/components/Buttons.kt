@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.PineLight

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(Pine, PineLight)),
                shape = CircleShape,
            )
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Cloud, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Cloud)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .border(width = 2.dp, color = Pine, shape = CircleShape)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = PineDark)
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "$text →", style = MaterialTheme.typography.bodyMedium, color = Pine)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PineDark, modifier = Modifier.size(24.dp))
    }
}

@Preview
@ArkiveComposable(name = "Primary Button", group = "Buttons", tags = ["cta"], designNodeId = "102:45")
@Composable
internal fun PreviewPrimaryButton() {
    PeakTheme {
        PrimaryButton(text = "Start hike", icon = Icons.Filled.PlayArrow)
    }
}

@Preview
@ArkiveComposable(name = "Secondary Button", group = "Buttons", tags = ["outlined"])
@Composable
internal fun PreviewSecondaryButton() {
    PeakTheme {
        SecondaryButton(text = "Save for later")
    }
}

@Preview
@ArkiveComposable(name = "Ghost Button", group = "Buttons", tags = ["text"])
@Composable
internal fun PreviewGhostButton() {
    PeakTheme {
        GhostButton(text = "See all trails")
    }
}

@Preview
@ArkiveComposable(name = "Circle Icon Button", group = "Buttons", tags = ["icon"])
@Composable
internal fun PreviewCircleIconButton() {
    PeakTheme {
        CircleIconButton(icon = Icons.Filled.PlayArrow)
    }
}
