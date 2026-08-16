@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** SCA-style cupping score, badge form. 80+ is specialty grade. */
@Composable
fun CuppingScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val background = if (score >= 80) BrewColors.Leaf else BrewColors.Caramel
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(background),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                color = BrewColors.Foam,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "SCA",
                color = BrewColors.Foam.copy(alpha = 0.8f),
                fontSize = 10.sp,
            )
        }
    }
}

@Preview
@Composable
internal fun CuppingScoreBadgePreview() {
    CuppingScoreBadge(score = 86)
}
