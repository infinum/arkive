package com.infinum.arkive.composeutils.uitree

import android.content.Context
import android.graphics.Canvas
import android.widget.FrameLayout
import java.io.File

internal class UiTreeViewHost(
    context: Context,
    private val shotId: String,
    directory: File,
) : FrameLayout(context) {

    private val session = UiTreeSession(shotId, directory, ::walkTree)

    init {
        session.start()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // View hierarchies rarely redraw; walking on every draw keeps the tree exact
        // without a change signal.
        session.markDirty()
        session.onDrawn()
    }

    private fun walkTree(): UiTree = ViewUiTreeWalker(shotId, this).walk()
}
