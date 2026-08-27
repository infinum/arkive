@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Moss
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.Slate

@Composable
fun LeaderboardRow(
    rank: Int,
    name: String,
    points: String,
    initials: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Row(
        modifier = modifier
            .width(340.dp)
            .background(
                color = if (highlighted) Moss else Cloud,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.bodyMedium,
            color = if (rank <= 3) Pine else Slate,
            modifier = Modifier.width(28.dp),
        )
        Avatar(initials = initials, size = 38.dp)
        Box(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        Text(text = points, style = MaterialTheme.typography.bodyMedium, color = PineDark)
    }
}

@Composable
fun ActivityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(340.dp)
            .background(color = Cloud, shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = Moss, shape = RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PineDark, modifier = Modifier.size(22.dp))
        }
        Box(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Box(modifier = Modifier.height(3.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Slate)
        }
        Text(text = time, style = MaterialTheme.typography.labelSmall, color = Slate)
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(340.dp)
            .background(color = Cloud, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PineDark, modifier = Modifier.size(22.dp))
        Box(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Pine),
        )
    }
}

@Preview
@ArkiveComposable(name = "Leaderboard Row", group = "List Items", tags = ["gamification"])
@Composable
internal fun PreviewLeaderboardRow() {
    PeakTheme {
        LeaderboardRow(rank = 1, name = "Maya Horvat", points = "2,340 pts", initials = "MH", highlighted = true)
    }
}

@Preview
@ArkiveComposable(name = "Activity Row", group = "List Items", tags = ["history"])
@Composable
internal fun PreviewActivityRow() {
    PeakTheme {
        ActivityRow(
            icon = Icons.Filled.Place,
            title = "Morning trail run",
            subtitle = "6.4 km · 320 m elev.",
            time = "07:12",
        )
    }
}

@Preview
@ArkiveComposable(name = "Settings Toggle Row", group = "List Items", tags = ["settings"])
@Composable
internal fun PreviewSettingsToggleRow() {
    PeakTheme {
        SettingsToggleRow(icon = Icons.Filled.Notifications, label = "Trail alerts", checked = true)
    }
}
