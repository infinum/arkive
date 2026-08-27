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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.components.ActivityRow
import com.infinum.arkive.sample.components.Avatar
import com.infinum.arkive.sample.components.PeakBottomBar
import com.infinum.arkive.sample.components.ProgressRing
import com.infinum.arkive.sample.components.StatCard
import com.infinum.arkive.sample.components.StreakBadge
import com.infinum.arkive.sample.components.WeeklyBarChart
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Mist
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Slate

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(390.dp)
            .background(Mist),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Good morning,", style = MaterialTheme.typography.bodyLarge, color = Slate)
                    Text(text = "Maya", style = MaterialTheme.typography.headlineMedium, color = Ink)
                }
                Avatar(initials = "MH")
            }
            Box(modifier = Modifier.height(8.dp))
            StreakBadge(days = 12)
            Box(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRing(progress = 0.72f, value = "8,412", caption = "of 10,000 steps")
            }
            Box(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "DISTANCE",
                    value = "42.3 km",
                    delta = "12%",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "ELEVATION",
                    value = "1,240 m",
                    delta = "8%",
                    modifier = Modifier.weight(1f),
                )
            }
            Box(modifier = Modifier.height(20.dp))
            Text(text = "This week", style = MaterialTheme.typography.titleLarge, color = Ink)
            Box(modifier = Modifier.height(12.dp))
            WeeklyBarChart(
                values = listOf(0.4f, 0.7f, 0.3f, 0.85f, 0.55f, 1f, 0.62f),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(20.dp))
            Text(text = "Recent activity", style = MaterialTheme.typography.titleLarge, color = Ink)
            Box(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityRow(
                    icon = Icons.Filled.Place,
                    title = "Morning trail run",
                    subtitle = "6.4 km · 320 m elev.",
                    time = "07:12",
                )
                ActivityRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Sljeme summit hike",
                    subtitle = "11.8 km · 780 m elev.",
                    time = "Sun",
                )
            }
        }
        PeakBottomBar(selectedIndex = 0)
    }
}

@Preview
@ArkiveComposable(name = "Home", group = "Screens", tags = ["screen"])
@Composable
internal fun PreviewHomeScreen() {
    PeakTheme {
        HomeScreen()
    }
}
