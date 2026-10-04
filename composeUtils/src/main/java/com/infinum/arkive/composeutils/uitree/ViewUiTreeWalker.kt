package com.infinum.arkive.composeutils.uitree

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView

internal class ViewUiTreeWalker(
    private val shotId: String,
    private val host: ViewGroup,
) {
    fun walk(): UiTree {
        val density = host.resources.displayMetrics.density
        // The engine captures the host; its single child is the inflated layout's root.
        val content = host.takeIf { it.childCount == 1 }?.getChildAt(0) ?: host
        val x = if (content === host) 0f else content.left + content.translationX
        val y = if (content === host) 0f else content.top + content.translationY
        return UiTree(
            id = shotId,
            density = density,
            width = host.width / density,
            height = host.height / density,
            root = UiTreePruning.prune(ViewTreeBuilder.node(content, x, y, density)),
        )
    }
}

/** Builds a View subtree; [x]/[y] are the view's px offset from the snapshot origin. */
internal object ViewTreeBuilder {

    fun node(view: View, x: Float, y: Float, density: Float): UiTreeNode {
        val group = view as? ViewGroup
        val children = group?.let { parent ->
            (0 until parent.childCount)
                .map(parent::getChildAt)
                .filter { it.visibility == View.VISIBLE }
                .map { child ->
                    node(
                        view = child,
                        x = x + child.left - parent.scrollX + child.translationX,
                        y = y + child.top - parent.scrollY + child.translationY,
                        density = density,
                    )
                }
        }.orEmpty()
        val editable = view as? EditText
        return UiTreeNode(
            bounds = UiBounds(x / density, y / density, view.width / density, view.height / density),
            view = view.javaClass.simpleName,
            viewId = resourceEntryName(view),
            text = (view as? TextView)?.takeIf { editable == null }?.text?.toString()?.takeIf { it.isNotEmpty() },
            editableText = editable?.text?.toString(),
            contentDescription = view.contentDescription?.toString()?.takeIf { it.isNotEmpty() },
            children = children,
        )
    }

    private fun resourceEntryName(view: View): String? =
        view.id.takeIf { it != View.NO_ID }?.let { id ->
            runCatching { view.resources.getResourceEntryName(id) }.getOrNull()
        }
}
