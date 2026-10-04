package com.infinum.arkive.composeutils.uitree

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UiTreeSessionTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun tree(id: String) = UiTree(id, 1f, 10f, 10f, UiTreeNode(bounds = UiBounds(0f, 0f, 10f, 10f)))

    @Test
    fun startDeletesALeftoverTree() {
        File(folder.root, "shot.json").writeText("stale")

        UiTreeSession("shot", folder.root, { tree("shot") }).start()

        assertFalse(File(folder.root, "shot.json").exists())
    }

    @Test
    fun walksOnlyWhenDirty() {
        var walks = 0
        val session = UiTreeSession("shot", folder.root, { walks++; tree("shot") })

        session.onDrawn()
        session.onDrawn()
        assertEquals(1, walks)

        session.markDirty()
        session.onDrawn()
        assertEquals(2, walks)
        assertTrue(File(folder.root, "shot.json").readText().contains("\"id\": \"shot\""))
    }

    @Test
    fun failingWalkIsLoggedOnceAndLeavesNoTree() {
        val logs = mutableListOf<String>()
        File(folder.root, "shot.json").writeText("from an earlier frame")
        val session = UiTreeSession("shot", folder.root, { error("boom") }, logs::add)

        session.onDrawn()
        session.markDirty()
        session.onDrawn()

        assertEquals(listOf("Arkive: ui tree skipped for shot: boom"), logs)
        assertFalse(File(folder.root, "shot.json").exists())
    }

    @Test
    fun createsTheDirectoryOnFirstWrite() {
        val directory = File(folder.root, "a/b")

        UiTreeSession("shot", directory, { tree("shot") }).onDrawn()

        assertTrue(File(directory, "shot.json").exists())
    }
}
