package com.infinum.arkive.sample.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.R
import com.infinum.arkive.sample.theme.SampleApptheme

@Composable
fun IconWithText(
    text: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    onCLick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCLick) {
            Icon(painter = painterResource(id = icon), contentDescription = null)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Preview
@ArkiveComposable(name = "Icon with Text", group = "Text", tags = ["Text", "Icon"])
@Composable
fun PreviewIconWithText() {
    SampleApptheme {
        IconWithText(
            text = "Text",
            icon = R.drawable.ic_settings,
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}
