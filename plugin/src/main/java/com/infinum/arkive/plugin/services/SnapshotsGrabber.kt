package com.infinum.arkive.plugin.services

import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface SnapshotsGrabber {
    fun grabAndMoveSnapshots(outputDir: File): List<String>
}

private val SCREEN_SHOTS_PATH =
    "src${File.separator}test${File.separator}snapshots${File.separator}images"

class SnapshotsGrabberImpl(
    private val project: Project,
) : SnapshotsGrabber {

    private val screenshotDir: File
        get() = project.projectDir.resolve(SCREEN_SHOTS_PATH)

    override fun grabAndMoveSnapshots(outputDir: File): List<String> {
        val originalSnapshots = screenshotDir.listFiles().orEmpty().map { it.path }
        return moveSnapshots(outputDir, originalSnapshots)
    }

    private fun moveSnapshots(outputDir: File, snapshots: List<String>): List<String> {
        outputDir.mkdirs()
        val snapshotsFiles = snapshots.map {
            File(it)
        }

        logger.warn("Moving snapshots to ${outputDir.absolutePath}")

        return snapshotsFiles.map {
            val destinationFile = File(outputDir, it.name)
            Files.move(it.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            "${outputDir.name}${File.separatorChar}${it.name}"
        }
    }
}
