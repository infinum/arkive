package com.infinum.arkive.composeutils.uitree

/** Bounds in dp, relative to the snapshot's top-left corner. */
internal data class UiBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

/** Semantics state; every field is written only when meaningful (see SemanticsMapping). */
internal data class UiState(
    val enabled: Boolean? = null,
    val selected: Boolean? = null,
    val toggle: String? = null,
    val focused: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = enabled == null && selected == null && toggle == null && focused == null
}

/** Sizes in sp, color as `#RRGGBBAA`; [overflow] is true when the text was clipped or ellipsized. */
internal data class UiTextStyle(
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val lineHeight: Float? = null,
    val color: String? = null,
    val maxLines: Int? = null,
    val overflow: Boolean = false,
)

/**
 * One layout node (or View). [composable]/[callers]/[source] come from the slot tables,
 * [view]/[viewId] from the View hierarchy, the rest from semantics and the text layout.
 */
internal data class UiTreeNode(
    val bounds: UiBounds,
    val composable: String? = null,
    val callers: List<String> = emptyList(),
    val source: String? = null,
    val view: String? = null,
    val viewId: String? = null,
    val role: String? = null,
    val text: String? = null,
    val editableText: String? = null,
    val contentDescription: String? = null,
    val testTag: String? = null,
    val actions: List<String> = emptyList(),
    val state: UiState? = null,
    val textStyle: UiTextStyle? = null,
    val children: List<UiTreeNode> = emptyList(),
) {
    /** True when any semantics field is set — such nodes are never pruned. */
    val hasSemantics: Boolean
        get() = role != null || text != null || editableText != null || contentDescription != null ||
            testTag != null || actions.isNotEmpty() || state != null || textStyle != null
}

/** One snapshot's tree; [width]/[height] are the snapshot size in dp. */
internal data class UiTree(
    val id: String,
    val density: Float,
    val width: Float,
    val height: Float,
    val root: UiTreeNode,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
