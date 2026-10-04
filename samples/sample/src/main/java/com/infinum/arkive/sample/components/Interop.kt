@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import android.widget.TextView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.PeakTheme

@Composable
fun LegacyBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Legacy:", style = MaterialTheme.typography.bodyMedium)
        AndroidView(
            factory = { context -> TextView(context).apply { text = label } },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview
@ArkiveComposable(name = "Legacy Badge", group = "Interop", tags = ["android-view"])
@Composable
internal fun PreviewLegacyBadge() {
    PeakTheme {
        LegacyBadge(label = "From a TextView")
    }
}
