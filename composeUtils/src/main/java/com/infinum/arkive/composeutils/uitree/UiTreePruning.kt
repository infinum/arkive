package com.infinum.arkive.composeutils.uitree

import kotlin.math.abs

/**
 * Drops pass-through layout nodes: no semantics, no app-code attribution, not a View, and
 * exactly one child with identical bounds (`Surface → Box → Layout` chains). Applied
 * bottom-up, so whole chains collapse into their content.
 */
internal object UiTreePruning {
    private const val BOUNDS_EPSILON = 0.01f

    fun prune(node: UiTreeNode): UiTreeNode {
        val pruned = node.copy(children = node.children.map(::prune))
        val only = pruned.children.singleOrNull()
        return if (only != null && isPassThrough(pruned) && sameBounds(pruned.bounds, only.bounds)) only else pruned
    }

    private fun isPassThrough(node: UiTreeNode): Boolean =
        !node.hasSemantics && node.source == null && node.view == null

    private fun sameBounds(first: UiBounds, second: UiBounds): Boolean =
        abs(first.x - second.x) < BOUNDS_EPSILON &&
            abs(first.y - second.y) < BOUNDS_EPSILON &&
            abs(first.width - second.width) < BOUNDS_EPSILON &&
            abs(first.height - second.height) < BOUNDS_EPSILON
}
