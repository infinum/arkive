package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTreePruningTest {

    private val full = UiBounds(0f, 0f, 100f, 40f)
    private val inner = UiBounds(8f, 8f, 84f, 24f)

    @Test
    fun passThroughChainCollapsesIntoItsChild() {
        val text = UiTreeNode(bounds = full, composable = "Text", source = "A.kt:3", text = "Hi")
        val chain = UiTreeNode(
            bounds = full,
            composable = "Surface",
            children = listOf(UiTreeNode(bounds = full, composable = "Box", children = listOf(text))),
        )

        assertEquals(text, UiTreePruning.prune(chain))
    }

    @Test
    fun appAttributedNodeIsKept() {
        val node = UiTreeNode(
            bounds = full,
            composable = "PrimaryButton",
            source = "A.kt:1",
            children = listOf(UiTreeNode(bounds = full, composable = "Layout")),
        )

        assertEquals(node, UiTreePruning.prune(node))
    }

    @Test
    fun nodeWithSemanticsIsKept() {
        val node = UiTreeNode(bounds = full, testTag = "pay", children = listOf(UiTreeNode(bounds = full)))

        assertEquals(node, UiTreePruning.prune(node))
    }

    @Test
    fun differentBoundsOrSeveralChildrenAreKept() {
        val padded = UiTreeNode(bounds = full, children = listOf(UiTreeNode(bounds = inner)))
        val two = UiTreeNode(bounds = full, children = listOf(UiTreeNode(bounds = full), UiTreeNode(bounds = full)))

        assertEquals(padded, UiTreePruning.prune(padded))
        assertEquals(two, UiTreePruning.prune(two))
    }

    @Test
    fun viewNodesAreNeverPruned() {
        val frame = UiTreeNode(bounds = full, view = "FrameLayout", children = listOf(UiTreeNode(bounds = full, view = "TextView")))

        assertEquals(frame, UiTreePruning.prune(frame))
    }
}
