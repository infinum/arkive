package com.infinum.arkive.plugin.services

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.Project

interface SnapshotsGrabber {
    fun grabSnapshots(outputDir: File, keepOriginals: Boolean): List<String>
}

private val SNAPSHOTS_PATH =
    "src${File.separator}test${File.separator}snapshots"

class SnapshotsGrabberImpl(
    private val project: Project,
) : SnapshotsGrabber {

    private val snapshotsDir: File
        get() = project.projectDir.resolve(SNAPSHOTS_PATH)

    override fun grabSnapshots(outputDir: File, keepOriginals: Boolean): List<String> {
        val originalSnapshots = snapshotsDir
            .walkTopDown()
            .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            .toList()

        outputDir.mkdirs()

        val verb = if (keepOriginals) "Copying" else "Moving"
        project.logger.warn("$verb ${originalSnapshots.size} snapshot(s) to ${outputDir.absolutePath}")

        return originalSnapshots.map { snapshot ->
            val destinationFile = File(outputDir, snapshot.name)
            if (keepOriginals) {
                Files.copy(snapshot.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                Files.move(snapshot.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            "${outputDir.name}${File.separatorChar}${snapshot.name}"
        }
    }
}
