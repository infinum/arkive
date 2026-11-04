package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposableAttributionTest {

    private val app = AppPackages(listOf("com.app"))
    private val appHash = AppPackages.packageHash("com.app")
    private val libHash = AppPackages.packageHash("androidx.compose.foundation.layout")

    @Test
    fun innermostAppCallNamesTheNodeAndOuterAppCallsBecomeCallers() {
        val tree = lib(
            "PreviewPrimaryButton",
            app("PeakTheme", 106, lib("MaterialTheme", app("PrimaryButton", 107, app("Row", 38, lib("Layout", node("row")))))),
        )

        val result = ComposableAttribution.attribute(listOf(tree), app)

        assertEquals(Attribution("Row", "Buttons.kt:38", listOf("PeakTheme", "PrimaryButton")), result["row"])
    }

    @Test
    fun libraryOnlyChainKeepsTheInnermostNameWithoutSource() {
        val tree = lib("BasicText", lib("Layout", node("text")))

        assertEquals(Attribution("Layout", null, emptyList()), ComposableAttribution.attribute(listOf(tree), app)["text"])
    }

    @Test
    fun chainResetsBelowAnEmittedNode() {
        val tree = app("Row", 1, node("row", app("Text", 2, lib("BasicText", node("text")))))

        val result = ComposableAttribution.attribute(listOf(tree), app)

        assertEquals(Attribution("Row", "Buttons.kt:1", emptyList()), result["row"])
        assertEquals(Attribution("Text", "Buttons.kt:2", emptyList()), result["text"])
    }

    @Test
    fun siblingsGetTheirOwnChainsInTreeOrder() {
        val tree = node("row", lambda(app("Icon", 4, node("icon")), app("Text", 5, node("text"))))

        val result = ComposableAttribution.attribute(listOf(tree), app)

        assertEquals(listOf("row", "icon", "text"), result.keys.toList())
        assertEquals("Icon", result["icon"]?.composable)
        assertEquals("Text", result["text"]?.composable)
    }

    @Test
    fun appCallWithoutSourceFileHasNoSource() {
        val tree = FakeGroup("Card", CallLocation(null, 3, appHash), children = listOf(node("card")))

        assertEquals(Attribution("Card", null, emptyList()), ComposableAttribution.attribute(listOf(tree), app)["card"])
    }

    @Test
    fun namedGroupWithoutLocationIsNotAppCode() {
        val tree = FakeGroup("Remembered", location = null, children = listOf(node("n")))

        assertEquals(Attribution("Remembered", null, emptyList()), ComposableAttribution.attribute(listOf(tree), app)["n"])
    }

    @Test
    fun syntheticGroupsAreIgnored() {
        val inApp = app(
            "PrimaryButton",
            1,
            FakeGroup("<get-typography>", CallLocation("Theme.kt", 5, appHash), children = listOf(lib("BasicText", node("text")))),
        )
        val inLib = lib("BasicText", FakeGroup("<get-x>", CallLocation("Lib.kt", 7, libHash), children = listOf(node("n"))))

        assertEquals(
            Attribution("PrimaryButton", "Buttons.kt:1", emptyList()),
            ComposableAttribution.attribute(listOf(inApp), app)["text"],
        )
        assertEquals(Attribution("BasicText", null, emptyList()), ComposableAttribution.attribute(listOf(inLib), app)["n"])
    }

    private fun app(name: String, line: Int, vararg children: SlotGroup): SlotGroup =
        FakeGroup(name, CallLocation("Buttons.kt", line, appHash), children = children.toList())

    private fun lib(name: String, vararg children: SlotGroup): SlotGroup =
        FakeGroup(name, CallLocation("Lib.kt", 7, libHash), children = children.toList())

    private fun node(id: String, vararg children: SlotGroup): SlotGroup =
        FakeGroup(null, null, node = id, children = children.toList())

    private fun lambda(vararg children: SlotGroup): SlotGroup =
        FakeGroup(null, null, children = children.toList())

    private data class FakeGroup(
        override val name: String?,
        override val location: CallLocation?,
        override val node: Any? = null,
        override val children: List<SlotGroup> = emptyList(),
    ) : SlotGroup
}
