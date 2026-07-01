package com.infinum.arkive.plugin.services

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.Project

interface SnapshotsGrabber {
    fun grabAndMoveSnapshots(outputDir: File): List<String>
}

private val SNAPSHOTS_PATH =
    "src${File.separator}test${File.separator}snapshots"

class SnapshotsGrabberImpl(
    private val project: Project,
) : SnapshotsGrabber {

    private val snapshotsDir: File
        get() = project.projectDir.resolve(SNAPSHOTS_PATH)

    override fun grabAndMoveSnapshots(outputDir: File): List<String> {
        val originalSnapshots = snapshotsDir
            .walkTopDown()
            .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            .toList()
        return moveSnapshots(outputDir, originalSnapshots)
    }

    private fun moveSnapshots(outputDir: File, snapshots: List<File>): List<String> {
        outputDir.mkdirs()

        project.logger.warn("Moving ${snapshots.size} snapshot(s) to ${outputDir.absolutePath}")

        return snapshots.map { snapshot ->
            val destinationFile = File(outputDir, snapshot.name)
            Files.move(snapshot.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            "${outputDir.name}${File.separatorChar}${snapshot.name}"
        }
    }
}
