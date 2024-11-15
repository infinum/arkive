package com.infinum.arkive.sample.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.SampleApptheme

@Composable
fun LabeledText(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
        Box(Modifier.height(5.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Preview
@ArkiveComposable(name = "Labeled Text", group = "Text", tags = ["Text","Label"])
@Composable
fun PreviewLabeledText() {
    SampleApptheme {
        LabeledText(
            label = "Temperatures",
            text = "11/0°C",
            modifier = Modifier.background(Color.White),
        )
    }
}
