package com.infinum.arkive.sample.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.SampleApptheme

@Composable
fun RoundedButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@ArkiveComposable(name = "Rounded Button", group = "Button", tags = ["Rounded"])
@Composable
fun PreviewRoundedButton() {
    SampleApptheme {
        RoundedButton(
            text = "5 DAY FORECAST",
            modifier = Modifier.background(MaterialTheme.colorScheme.background)

        )
    }
}

@Preview
@ArkiveComposable(name = "Big Rounded Button", group = "Button", tags = ["Rounded"])
@Composable
fun PreviewWideRoundedButton() {
    SampleApptheme {
        RoundedButton(
            text = "5 DAY FORECAST",
            Modifier
                .width(200.dp)
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
