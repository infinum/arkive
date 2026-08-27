@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Amber
import com.infinum.arkive.sample.theme.AmberDeep
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Coral
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Moss
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.PineLight
import com.infinum.arkive.sample.theme.Slate

@Composable
fun StatCard(
    label: String,
    value: String,
    delta: String,
    modifier: Modifier = Modifier,
    positive: Boolean = true,
) {
    Column(
        modifier = modifier
            .background(color = Cloud, shape = RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate)
        Box(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Box(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (positive) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (positive) Pine else Coral,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                color = if (positive) Pine else Coral,
            )
        }
    }
}

@Composable
fun TrailCard(
    name: String,
    distance: String,
    elevation: String,
    difficulty: Difficulty,
    rating: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Cloud),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.linearGradient(listOf(PineDark, Pine, PineLight))),
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = Cloud.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(color = Cloud, shape = CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = AmberDeep,
                    modifier = Modifier.size(12.dp),
                )
                Box(modifier = Modifier.width(3.dp))
                Text(text = rating, style = MaterialTheme.typography.labelSmall, color = Ink)
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Box(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DifficultyTag(difficulty = difficulty)
                Box(modifier = Modifier.width(10.dp))
                Text(
                    text = "$distance · $elevation elev.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate,
                )
            }
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    subtitle: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = Cloud, shape = RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Pine,
            )
        }
        Box(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Slate)
        Box(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(color = Moss, shape = CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(listOf(Pine, PineLight)),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
fun AchievementCard(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .background(color = Cloud, shape = RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color = Amber.copy(alpha = 0.2f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = AmberDeep,
                modifier = Modifier.size(26.dp),
            )
        }
        Box(modifier = Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = Ink)
        Box(modifier = Modifier.height(4.dp))
        Text(text = date, style = MaterialTheme.typography.labelSmall, color = Slate)
    }
}

@Preview
@ArkiveComposable(name = "Stat Card", group = "Cards", tags = ["stats"])
@Composable
internal fun PreviewStatCard() {
    PeakTheme {
        StatCard(label = "DISTANCE THIS WEEK", value = "42.3 km", delta = "12% vs last week")
    }
}

@Preview
@ArkiveComposable(name = "Trail Card", group = "Cards", tags = ["trail"])
@Composable
internal fun PreviewTrailCard() {
    PeakTheme {
        TrailCard(
            name = "Velebit Ridge Loop",
            distance = "14.2 km",
            elevation = "870 m",
            difficulty = Difficulty.MODERATE,
            rating = "4.8",
        )
    }
}

@Preview
@ArkiveComposable(name = "Goal Card", group = "Cards", tags = ["progress"])
@Composable
internal fun PreviewGoalCard() {
    PeakTheme {
        GoalCard(title = "Monthly elevation goal", subtitle = "3,480 of 5,000 m climbed", progress = 0.7f)
    }
}

@Preview
@ArkiveComposable(name = "Achievement Card", group = "Cards", tags = ["gamification"])
@Composable
internal fun PreviewAchievementCard() {
    PeakTheme {
        AchievementCard(title = "First Summit", date = "Mar 12")
    }
}
