@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinum.arkive.annotations.ArkiveComposable

/**
 * Android-only component (imagine it talks to a bluetooth grinder) — proves previews in
 * the android source set are collected next to the common ones.
 */
@Composable
fun GrinderStatusCard(
    deviceName: String,
    burrTemperature: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrewColors.Espresso)
            .padding(16.dp),
    ) {
        Text(
            text = deviceName,
            color = BrewColors.Foam,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Burrs at $burrTemperature",
            color = BrewColors.Caramel,
            fontSize = 12.sp,
        )
    }
}

@ArkiveComposable
@Preview
@Composable
internal fun GrinderStatusCardPreview() {
    GrinderStatusCard(
        deviceName = "Niche Zero · paired",
        burrTemperature = "31 °C",
    )
}
