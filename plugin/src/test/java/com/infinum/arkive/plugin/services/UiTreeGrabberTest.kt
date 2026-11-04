package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.model.Component
import com.infinum.arkive.metadata.model.ShowcaseItem
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UiTreeGrabberTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun treesAreKeyedByLowercasedShotId() {
        val source = folder.newFolder("trees")
        File(source, "com-app-card_layoutDirection_LTR.json").writeText("{}")
        File(source, "notes.txt").writeText("not a tree")

        val grabber = UiTreeGrabber(source)
        val available = grabber.available()

        assertEquals(setOf("com-app-card_layoutdirection_ltr"), available.keys)
        assertEquals(
            mapOf("com-app-card_layoutdirection_ltr" to "uitree/com-app-card_layoutDirection_LTR.json"),
            grabber.relativePaths(available),
        )
    }

    @Test
    fun missingDirectoryMeansNoTrees() {
        assertTrue(UiTreeGrabber(File(folder.root, "absent")).available().isEmpty())
    }

    @Test
    fun onlyReferencedTreesAreCopiedAndStaleOnesRemoved() {
        val source = folder.newFolder("trees")
        val showcase = folder.newFolder("showcase")
        File(source, "com-app-card.json").writeText("{\"id\": \"com-app-card\"}")
        File(source, "com-app-orphan.json").writeText("{}")
        File(showcase, "uitree").mkdirs()
        File(showcase, "uitree/stale.json").writeText("{}")
        val grabber = UiTreeGrabber(source)
        val item = ShowcaseItem(
            component = Component("com-app-card", "Card", "PreviewCard", "com.app", "Card.kt", "", emptyList(), emptyList()),
            snapshotPath = "images/card.png",
            variants = emptyList(),
            uiTreePath = "uitree/com-app-card.json",
        )

        val copied = grabber.copyReferenced(listOf(item), grabber.available(), showcase)

        assertEquals(1, copied)
        assertEquals(listOf("com-app-card.json"), File(showcase, "uitree").list()!!.sorted())
    }
}
