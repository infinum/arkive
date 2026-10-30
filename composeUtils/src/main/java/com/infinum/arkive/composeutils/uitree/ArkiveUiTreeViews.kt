package com.infinum.arkive.composeutils.uitree

import android.content.Context
import android.widget.FrameLayout

/**
 * Container the generated test inflates each `@ArkiveView` layout into. Without the
 * `arkive.uitree.dir` system property it is a plain FrameLayout — the container the tests
 * always used — so recorded pixels never change; with it, the container walks the View
 * hierarchy after drawing.
 */
object ArkiveUiTreeViews {

    @JvmStatic
    fun host(context: Context, shotId: String): FrameLayout {
        val directory = UiTreeFiles.directory()
        return if (directory == null) FrameLayout(context) else UiTreeViewHost(context, shotId, directory)
    }
}
