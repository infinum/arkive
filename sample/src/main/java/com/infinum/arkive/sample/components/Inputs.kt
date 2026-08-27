@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.AmberDeep
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Line
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Slate

@Composable
fun SearchField(
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(340.dp)
            .height(50.dp)
            .background(color = Cloud, shape = CircleShape)
            .border(width = 1.dp, color = Line, shape = CircleShape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.width(10.dp))
        Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = Slate)
    }
}

@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    max: Int = 5,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < rating) AmberDeep else Line,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview
@ArkiveComposable(name = "Search Field", group = "Inputs", tags = ["search"])
@Composable
internal fun PreviewSearchField() {
    PeakTheme {
        SearchField(placeholder = "Search trails, peaks, parks…")
    }
}

@Preview
@ArkiveComposable(name = "Rating Stars", group = "Inputs", tags = ["rating"])
@Composable
internal fun PreviewRatingStars() {
    PeakTheme {
        RatingStars(rating = 4)
    }
}
