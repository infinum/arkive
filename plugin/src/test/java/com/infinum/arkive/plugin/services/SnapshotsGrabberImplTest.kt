package com.infinum.arkive.plugin.services

import java.io.File
import org.gradle.api.logging.Logging
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SnapshotsGrabberImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger = Logging.getLogger(SnapshotsGrabberImplTest::class.java)

    @Test
    fun freshSnapshotsReplaceStaleImages() {
        val goldens = folder.newFolder("snapshots")
        File(goldens, "images").mkdirs()
        File(goldens, "images/com.infinum.arkive_Test_com-app-card.png").writeText("new")
        File(goldens, "images/consumer_OwnTest_card.png").writeText("consumer")
        val images = folder.newFolder("showcase", "images")
        File(images, "com.infinum.arkive_Test_testMethod_com-app-card.png").writeText("other engine")
        File(images, "com.infinum.arkive_Test_com-app-removed.png").writeText("removed component")

        val grabbed = SnapshotsGrabberImpl(goldens, logger).grabSnapshots(images)

        assertEquals(listOf("images/com.infinum.arkive_Test_com-app-card.png"), grabbed.map { it.relativePath })
        assertEquals(listOf("com.infinum.arkive_Test_com-app-card.png"), images.list()!!.sorted())
        assertEquals("new", File(images, "com.infinum.arkive_Test_com-app-card.png").readText())
    }

    @Test
    fun previousImagesSurviveWhenNothingWasRecorded() {
        // Under snapshotRetention NONE an up-to-date record task leaves no goldens; the
        // task then keeps the previous showcase, which still needs its images.
        val goldens = folder.newFolder("snapshots")
        File(goldens, "consumer_OwnTest_card.png").writeText("consumer")
        val images = folder.newFolder("showcase", "images")
        File(images, "com.infinum.arkive_Test_com-app-card.png").writeText("previous")

        val grabbed = SnapshotsGrabberImpl(goldens, logger).grabSnapshots(images)

        assertTrue(grabbed.isEmpty())
        assertEquals(listOf("com.infinum.arkive_Test_com-app-card.png"), images.list()!!.sorted())
    }
}
