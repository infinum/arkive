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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.components.AchievementCard
import com.infinum.arkive.sample.components.Avatar
import com.infinum.arkive.sample.components.GoalCard
import com.infinum.arkive.sample.components.PeakBottomBar
import com.infinum.arkive.sample.components.SettingsToggleRow
import com.infinum.arkive.sample.components.StatusBadge
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Mist
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Slate

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(390.dp)
            .background(Mist),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(initials = "MH", size = 72.dp)
                Box(modifier = Modifier.height(12.dp))
                Text(text = "Maya Horvat", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(text = "Zagreb, Croatia", style = MaterialTheme.typography.bodyLarge, color = Slate)
                Box(modifier = Modifier.height(10.dp))
                StatusBadge(text = "TRAIL PIONEER")
            }
            Box(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(value = "128", label = "Hikes")
                ProfileStat(value = "1,842 km", label = "Distance")
                ProfileStat(value = "23", label = "Summits")
            }
            Box(modifier = Modifier.height(24.dp))
            Text(text = "Achievements", style = MaterialTheme.typography.titleLarge, color = Ink)
            Box(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementCard(title = "First Summit", date = "Mar 12")
                AchievementCard(title = "100 km Club", date = "Apr 3")
            }
            Box(modifier = Modifier.height(20.dp))
            GoalCard(
                title = "Monthly elevation goal",
                subtitle = "3,480 of 5,000 m climbed",
                progress = 0.7f,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(20.dp))
            Text(text = "Preferences", style = MaterialTheme.typography.titleLarge, color = Ink)
            Box(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleRow(icon = Icons.Filled.Notifications, label = "Trail alerts", checked = true)
                SettingsToggleRow(icon = Icons.Filled.LocationOn, label = "Share live location", checked = false)
            }
        }
        PeakBottomBar(selectedIndex = 3)
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate)
    }
}

@Preview
@ArkiveComposable(name = "Profile", group = "Screens", tags = ["screen"])
@Composable
internal fun PreviewProfileScreen() {
    PeakTheme {
        ProfileScreen()
    }
}
