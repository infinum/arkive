@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.infinum.arkive.annotations.ArkiveComposable

/** A coffee bean entry: origin, roast level, and the taster's rating. */
@Composable
fun BeanCard(
    name: String,
    origin: String,
    roast: String,
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrewColors.Crema)
            .padding(16.dp),
    ) {
        Text(
            text = name,
            color = BrewColors.Espresso,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = origin,
            color = BrewColors.Caramel,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoastBadge(roast = roast)
            Spacer(modifier = Modifier.weight(1f))
            RatingDots(rating = rating)
        }
    }
}

@Composable
private fun RoastBadge(roast: String) {
    Text(
        text = roast,
        color = BrewColors.Foam,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrewColors.Espresso)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun RatingDots(rating: Int, total: Int = 5) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            val color = if (index < rating) BrewColors.Berry else BrewColors.Caramel.copy(alpha = 0.3f)
            Spacer(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@ArkiveComposable
@Preview
@Composable
internal fun BeanCardPreview() {
    BeanCard(
        name = "La Esperanza",
        origin = "Huila, Colombia · washed",
        roast = "Medium",
        rating = 4,
    )
}
