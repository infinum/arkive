package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SemanticsMappingTest {

    @Test
    fun knownActionsAreRenamedAndPlumbingDropped() {
        val actions = SemanticsMapping.actions(listOf("OnClick", "GetTextLayoutResult", "Text", "OnLongClick", "ScrollBy", "OnClick"))

        assertEquals(listOf("click", "longClick", "scroll"), actions)
    }

    @Test
    fun enabledIsOnlyWrittenForActionableOrDisabledNodes() {
        assertNull(SemanticsMapping.state(SemanticsSnapshot(), emptyList()))
        assertEquals(UiState(enabled = true), SemanticsMapping.state(SemanticsSnapshot(), listOf("click")))
        assertEquals(UiState(enabled = false), SemanticsMapping.state(SemanticsSnapshot(disabled = true), emptyList()))
    }

    @Test
    fun toggleIsLowercasedAndFocusedOnlyWhenTrue() {
        val state = SemanticsMapping.state(SemanticsSnapshot(toggle = "Indeterminate", focused = false, selected = true), emptyList())

        assertEquals(UiState(selected = true, toggle = "indeterminate"), state)
    }

    @Test
    fun textStyleDefaultsAndUnlimitedLines() {
        val style = SemanticsMapping.textStyle(
            TextStyleSnapshot(fontSizeSp = 16f, fontWeight = null, lineHeightSp = null, colorArgb = null, maxLines = Int.MAX_VALUE, overflow = true),
        )

        assertEquals(UiTextStyle(fontSize = 16f, fontWeight = 400, overflow = true), style)
    }

    @Test
    fun colorIsRgbaHex() {
        assertEquals("#112233FF", SemanticsMapping.color(0xFF112233.toInt()))
        assertEquals("#FFFFFF80", SemanticsMapping.color(0x80FFFFFF.toInt()))
    }
}
