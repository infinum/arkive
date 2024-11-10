package com.infinum.arkive.sample.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.R
import com.infinum.arkive.sample.theme.SampleApptheme

@Composable
fun CircularButton(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.background,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, color = borderColor, shape = CircleShape)
            .clickable {
                onClick()
            }
            .size(32.dp),

        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(id = id), contentDescription = null)
    }
}

@Preview
@ArkiveComposable
@Composable
fun PreviewCircularButton() {
    SampleApptheme {
        CircularButton(id = R.drawable.ic_settings)
    }
}

@Preview
@ArkiveComposable
@Composable
fun PreviewCircularButtonWithBlackBorder() {
    SampleApptheme {
        CircularButton(
            id = R.drawable.ic_settings,
            borderColor = MaterialTheme.colorScheme.secondary,
        )
    }
}
