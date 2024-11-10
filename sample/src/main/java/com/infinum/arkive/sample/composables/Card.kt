package com.infinum.arkive.sample.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.sample.theme.SampleApptheme

@Composable
fun Card(
    modifier: Modifier = Modifier,
    roundDirection: RoundDirection = RoundDirection.UP,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = remember(roundDirection) {
        when (roundDirection) {
            RoundDirection.UP -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            RoundDirection.DOWN -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
    ) {
        content()
    }
}

enum class RoundDirection {
    UP, DOWN
}

@Preview
@Composable
fun PreviewUpCard() {
    SampleApptheme {
        Card(roundDirection = RoundDirection.UP) {
            Column(Modifier.align(Alignment.Center)) {
                Text(text = "First")
                Text(text = "Second")
                Text(text = "third")
            }
        }
    }
}

@Preview
@Composable
fun PreviewDownCard() {
    SampleApptheme {
        Card(roundDirection = RoundDirection.DOWN) {
            Column(Modifier.align(Alignment.Center)) {
                Text(text = "First")
                Text(text = "Second")
                Text(text = "third")
            }
        }
    }
}
