package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTreeInteropTest {

    private val hosted = UiTreeNode(bounds = UiBounds(40f, 10f, 60f, 20f), view = "TextView", text = "From a View")

    @Test
    fun hostedViewHangsUnderTheDeepestContainingComposeNode() {
        val androidView = UiTreeNode(bounds = UiBounds(40f, 10f, 60f, 20f), composable = "AndroidView", source = "A.kt:9")
        val row = UiTreeNode(
            bounds = UiBounds(0f, 0f, 200f, 40f),
            composable = "Row",
            children = listOf(UiTreeNode(bounds = UiBounds(0f, 0f, 40f, 40f), composable = "Text"), androidView),
        )

        val attached = UiTreeInterop.attach(row, hosted)

        assertEquals(listOf(hosted), attached.children[1].children)
        assertEquals(emptyList<UiTreeNode>(), attached.children[0].children)
    }

    @Test
    fun viewOutsideEveryNodeHangsOffTheRoot() {
        val root = UiTreeNode(bounds = UiBounds(0f, 0f, 10f, 10f), composable = "Box")

        assertEquals(listOf(hosted), UiTreeInterop.attach(root, hosted).children)
    }
}
