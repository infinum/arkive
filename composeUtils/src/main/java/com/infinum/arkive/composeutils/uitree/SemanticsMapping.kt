package com.infinum.arkive.composeutils.uitree

/** Plain values read off a SemanticsNode — the Android-free input of [SemanticsMapping]. */
internal data class SemanticsSnapshot(
    val role: String? = null,
    val text: String? = null,
    val editableText: String? = null,
    val contentDescription: String? = null,
    val testTag: String? = null,
    val keyNames: List<String> = emptyList(),
    val disabled: Boolean = false,
    val selected: Boolean? = null,
    val toggle: String? = null,
    val focused: Boolean? = null,
)

/** Plain values read off a TextLayoutResult; sizes in sp, color as ARGB. */
internal data class TextStyleSnapshot(
    val fontSizeSp: Float?,
    val fontWeight: Int?,
    val lineHeightSp: Float?,
    val colorArgb: Int?,
    val maxLines: Int,
    val overflow: Boolean,
)

internal object SemanticsMapping {
    private const val DEFAULT_FONT_WEIGHT = 400
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val ALPHA_SHIFT = 24
    private const val BYTE_MASK = 0xFF

    // SemanticsActions key names → schema action names. Only user-facing actions are listed;
    // plumbing such as GetTextLayoutResult is deliberately absent.
    private val ACTION_NAMES = mapOf(
        "OnClick" to "click",
        "OnLongClick" to "longClick",
        "ScrollBy" to "scroll",
        "ScrollToIndex" to "scrollToIndex",
        "SetText" to "setText",
        "SetProgress" to "setProgress",
        "Expand" to "expand",
        "Collapse" to "collapse",
        "Dismiss" to "dismiss",
        "RequestFocus" to "requestFocus",
    )

    fun actions(keyNames: List<String>): List<String> = keyNames.mapNotNull(ACTION_NAMES::get).distinct()

    fun state(snapshot: SemanticsSnapshot, actions: List<String>): UiState? {
        val state = UiState(
            enabled = if (actions.isNotEmpty() || snapshot.disabled) !snapshot.disabled else null,
            selected = snapshot.selected,
            toggle = snapshot.toggle?.lowercase(),
            focused = snapshot.focused?.takeIf { it },
        )
        return state.takeUnless { it.isEmpty }
    }

    fun textStyle(snapshot: TextStyleSnapshot): UiTextStyle = UiTextStyle(
        fontSize = snapshot.fontSizeSp,
        fontWeight = snapshot.fontWeight ?: DEFAULT_FONT_WEIGHT,
        lineHeight = snapshot.lineHeightSp,
        color = snapshot.colorArgb?.let(::color),
        maxLines = snapshot.maxLines.takeIf { it != Int.MAX_VALUE },
        overflow = snapshot.overflow,
    )

    /** `#RRGGBBAA` from an ARGB int. */
    fun color(argb: Int): String = "#%02X%02X%02X%02X".format(
        argb shr RED_SHIFT and BYTE_MASK,
        argb shr GREEN_SHIFT and BYTE_MASK,
        argb and BYTE_MASK,
        argb ushr ALPHA_SHIFT and BYTE_MASK,
    )
}
