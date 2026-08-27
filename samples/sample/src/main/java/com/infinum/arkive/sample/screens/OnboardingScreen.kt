@file:Suppress("MagicNumber", "LongMethod")

package com.infinum.arkive.sample.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.components.GhostButton
import com.infinum.arkive.sample.components.PrimaryButton
import com.infinum.arkive.sample.theme.Amber
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Line
import com.infinum.arkive.sample.theme.Mist
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.PineLight
import com.infinum.arkive.sample.theme.Slate

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(390.dp)
            .background(Mist)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(PineLight.copy(alpha = 0.25f), Mist)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            MountainIllustration(modifier = Modifier.size(190.dp))
        }
        Box(modifier = Modifier.height(36.dp))
        Text(
            text = "Every summit\nstarts small",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.height(14.dp))
        Text(
            text = "Track hikes, climb leaderboards and turn weekend walks into mountain stories.",
            style = MaterialTheme.typography.bodyLarge,
            color = Slate,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 8.dp)
                    .background(color = Pine, shape = CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = Line, shape = CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = Line, shape = CircleShape),
            )
        }
        Box(modifier = Modifier.height(28.dp))
        PrimaryButton(text = "Get started", modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.height(8.dp))
        GhostButton(text = "I already have an account")
        Box(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MountainIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val back = Path().apply {
            moveTo(0f, size.height * 0.85f)
            lineTo(size.width * 0.35f, size.height * 0.30f)
            lineTo(size.width * 0.62f, size.height * 0.85f)
            close()
        }
        val front = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.85f)
            lineTo(size.width * 0.68f, size.height * 0.42f)
            lineTo(size.width, size.height * 0.85f)
            close()
        }
        drawPath(path = back, brush = Brush.verticalGradient(listOf(PineLight, Pine)))
        drawPath(path = front, brush = Brush.verticalGradient(listOf(Pine, PineDark)))
        val snow = Path().apply {
            moveTo(size.width * 0.60f, size.height * 0.52f)
            lineTo(size.width * 0.68f, size.height * 0.42f)
            lineTo(size.width * 0.76f, size.height * 0.52f)
            lineTo(size.width * 0.71f, size.height * 0.50f)
            lineTo(size.width * 0.65f, size.height * 0.54f)
            close()
        }
        drawPath(path = snow, color = Cloud)
        drawCircle(
            color = Amber,
            radius = size.width * 0.09f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f),
        )
    }
}

@Preview
@ArkiveComposable(name = "Onboarding", group = "Screens", tags = ["screen"])
@Composable
internal fun PreviewOnboardingScreen() {
    PeakTheme {
        OnboardingScreen()
    }
}
