package com.infinum.arkive.composeutils.uitree

/**
 * Android Views hosted by AndroidView live in the View hierarchy, not the layout-node tree;
 * they are matched back by geometry — the AndroidView's layout node has exactly the
 * holder's bounds, so the deepest compose node containing the view is its host.
 */
internal object UiTreeInterop {
    private const val CONTAINMENT_SLACK = 0.5f

    fun attach(root: UiTreeNode, hosted: UiTreeNode): UiTreeNode =
        if (contains(root.bounds, hosted.bounds)) attachWithin(root, hosted) else root.copy(children = root.children + hosted)

    private fun attachWithin(node: UiTreeNode, hosted: UiTreeNode): UiTreeNode {
        val index = node.children.indexOfLast { it.view == null && contains(it.bounds, hosted.bounds) }
        return if (index < 0) {
            node.copy(children = node.children + hosted)
        } else {
            node.copy(children = node.children.toMutableList().also { it[index] = attachWithin(it[index], hosted) })
        }
    }

    private fun contains(outer: UiBounds, inner: UiBounds): Boolean =
        inner.x >= outer.x - CONTAINMENT_SLACK &&
            inner.y >= outer.y - CONTAINMENT_SLACK &&
            inner.x + inner.width <= outer.x + outer.width + CONTAINMENT_SLACK &&
            inner.y + inner.height <= outer.y + outer.height + CONTAINMENT_SLACK
}
