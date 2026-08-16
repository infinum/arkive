@file:Suppress("MagicNumber")

package com.infinum.arkive.samplecmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinum.arkive.annotations.ArkiveComposable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

/** A single flavour note picked during cupping. */
@Composable
fun TastingNoteChip(
    note: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = note,
        color = BrewColors.Espresso,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BrewColors.Leaf.copy(alpha = 0.25f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

internal class TastingNoteProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("Dark chocolate", "Blood orange", "Jasmine")
}

// Deliberately uses the (deprecated) jetbrains preview annotations: Arkive must keep
// collecting parameterized previews written against the pre-1.11 CMP API.
@Suppress("DEPRECATION")
@ArkiveComposable
@Preview
@Composable
internal fun TastingNoteChipPreview(
    @PreviewParameter(TastingNoteProvider::class) note: String,
) {
    TastingNoteChip(note = note)
}
