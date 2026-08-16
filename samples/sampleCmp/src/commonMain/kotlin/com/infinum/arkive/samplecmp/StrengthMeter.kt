@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinum.arkive.annotations.ArkiveComposable

/** Strength of a brew on a 1–5 scale, drawn as filled beans. */
@Composable
fun StrengthMeter(
    strength: Int,
    modifier: Modifier = Modifier,
    total: Int = 5,
) {
    Column(modifier = modifier) {
        Text(
            text = "Strength",
            color = BrewColors.Caramel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                val color = if (index < strength) BrewColors.Espresso else BrewColors.Crema
                Spacer(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@ArkiveComposable
@Preview
@Composable
internal fun StrengthMeterPreview() {
    StrengthMeter(strength = 3)
}
