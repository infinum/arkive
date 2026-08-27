@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One brewing recipe in the journal: method, grind size, and total brew time. */
@Composable
fun BrewMethodRow(
    method: String,
    grind: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrewColors.Foam)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method,
                color = BrewColors.Espresso,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = grind,
                color = BrewColors.Caramel,
                fontSize = 12.sp,
            )
        }
        Text(
            text = time,
            color = BrewColors.Leaf,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// A plain @Preview without @ArkiveComposable — collected by Arkive all the same.
@Preview
@Composable
internal fun BrewMethodRowPreview() {
    BrewMethodRow(
        method = "V60 pour-over",
        grind = "Medium-fine · 18 g",
        time = "2:45",
    )
}
