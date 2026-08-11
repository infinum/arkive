@file:Suppress("MagicNumber", "LongMethod")

package com.infinum.arkive.sample.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.components.AvatarStack
import com.infinum.arkive.sample.components.Difficulty
import com.infinum.arkive.sample.components.DifficultyTag
import com.infinum.arkive.sample.components.ElevationChart
import com.infinum.arkive.sample.components.PrimaryButton
import com.infinum.arkive.sample.components.RatingStars
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Mist
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.PineLight
import com.infinum.arkive.sample.theme.Slate

@Composable
fun TrailDetailScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(390.dp)
            .background(Mist),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.linearGradient(listOf(PineDark, Pine, PineLight))),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                DifficultyTag(difficulty = Difficulty.MODERATE)
                Box(modifier = Modifier.height(10.dp))
                Text(
                    text = "Velebit Ridge Loop",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Cloud,
                )
                Text(
                    text = "Northern Velebit National Park",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cloud.copy(alpha = 0.85f),
                )
            }
        }
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TrailStat(value = "14.2 km", label = "Distance")
                TrailStat(value = "870 m", label = "Elevation")
                TrailStat(value = "5 h 30 m", label = "Est. time")
            }
            Box(modifier = Modifier.height(20.dp))
            Text(text = "Elevation profile", style = MaterialTheme.typography.titleLarge, color = Ink)
            Box(modifier = Modifier.height(12.dp))
            ElevationChart(
                points = listOf(0.1f, 0.25f, 0.2f, 0.45f, 0.6f, 0.5f, 0.8f, 0.95f, 0.7f, 0.75f),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(rating = 4)
                AvatarStack(members = listOf("MH", "JK", "AP"), extraCount = 21)
            }
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = "A panoramic ridge walk over limestone karst with views across " +
                    "the Adriatic. Exposed in parts — bring sun protection and 2 L of water.",
                style = MaterialTheme.typography.bodyLarge,
                color = Slate,
            )
            Box(modifier = Modifier.height(24.dp))
            PrimaryButton(
                text = "Start hike",
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TrailStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate)
    }
}

@Preview
@ArkiveComposable(name = "Trail Detail", group = "Screens", tags = ["screen"])
@Composable
internal fun PreviewTrailDetailScreen() {
    PeakTheme {
        TrailDetailScreen()
    }
}
