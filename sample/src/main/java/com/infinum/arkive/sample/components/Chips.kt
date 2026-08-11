@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Amber
import com.infinum.arkive.sample.theme.AmberDeep
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Coral
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Line
import com.infinum.arkive.sample.theme.Moss
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.Slate

enum class Difficulty(val label: String, val color: Color) {
    EASY("Easy", Pine),
    MODERATE("Moderate", AmberDeep),
    HARD("Hard", Coral),
}

@Composable
fun PeakFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) PineDark else Cloud
    val textColor = if (selected) Cloud else Slate
    Box(
        modifier = modifier
            .background(color = background, shape = CircleShape)
            .border(width = 1.dp, color = if (selected) PineDark else Line, shape = CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
fun DifficultyTag(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = difficulty.color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color = difficulty.color, shape = CircleShape),
        )
        Box(modifier = Modifier.size(6.dp))
        Text(text = difficulty.label, style = MaterialTheme.typography.labelSmall, color = difficulty.color)
    }
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color = Moss, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = PineDark)
    }
}

@Composable
fun StreakBadge(
    days: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = Amber.copy(alpha = 0.18f), shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = AmberDeep,
            modifier = Modifier.size(14.dp),
        )
        Box(modifier = Modifier.size(5.dp))
        Text(text = "$days day streak", style = MaterialTheme.typography.labelSmall, color = Ink)
    }
}

class SelectedProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}

class DifficultyProvider : PreviewParameterProvider<Difficulty> {
    override val values = Difficulty.entries.asSequence()
}

@Preview
@ArkiveComposable(name = "Filter Chip", group = "Chips", tags = ["parameterized", "filter"])
@Composable
internal fun PreviewPeakFilterChip(@PreviewParameter(SelectedProvider::class) selected: Boolean) {
    PeakTheme {
        PeakFilterChip(label = "Forest trails", selected = selected)
    }
}

@Preview
@ArkiveComposable(name = "Difficulty Tag", group = "Chips", tags = ["parameterized", "label"])
@Composable
internal fun PreviewDifficultyTag(@PreviewParameter(DifficultyProvider::class) difficulty: Difficulty) {
    PeakTheme {
        DifficultyTag(difficulty = difficulty)
    }
}

@Preview
@ArkiveComposable(name = "Status Badge", group = "Chips", tags = ["label"])
@Composable
internal fun PreviewStatusBadge() {
    PeakTheme {
        StatusBadge(text = "COMPLETED")
    }
}

@Preview
@ArkiveComposable(name = "Streak Badge", group = "Chips", tags = ["gamification"])
@Composable
internal fun PreviewStreakBadge() {
    PeakTheme {
        StreakBadge(days = 12)
    }
}
