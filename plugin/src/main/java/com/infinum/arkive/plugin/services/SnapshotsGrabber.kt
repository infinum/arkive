package com.infinum.arkive.plugin.services

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.Project

/** A snapshot copied into the showcase output, with its origin in the golden directory. */
data class GrabbedSnapshot(
    val sourceFile: File,
    val relativePath: String,
)

interface SnapshotsGrabber {
    fun grabSnapshots(outputDir: File): List<GrabbedSnapshot>
}

private val SNAPSHOTS_PATH =
    "src${File.separator}test${File.separator}snapshots"

// Only snapshots recorded by the generated Arkive test class are grabbed (and later
// subject to retention cleanup) — a consumer's own Paparazzi goldens are never touched.
private const val ARKIVE_SNAPSHOT_PREFIX = "com.infinum.arkive_"

class SnapshotsGrabberImpl(
    private val project: Project,
) : SnapshotsGrabber {

    private val snapshotsDir: File
        get() = project.projectDir.resolve(SNAPSHOTS_PATH)

    override fun grabSnapshots(outputDir: File): List<GrabbedSnapshot> {
        val originalSnapshots = snapshotsDir
            .walkTopDown()
            .filter {
                it.isFile &&
                    it.extension.equals("png", ignoreCase = true) &&
                    it.name.startsWith(ARKIVE_SNAPSHOT_PREFIX)
            }
            .toList()

        outputDir.mkdirs()

        project.logger.warn("Copying ${originalSnapshots.size} snapshot(s) to ${outputDir.absolutePath}")

        // The walk is recursive but the copy is flat (the web template resolves images by
        // basename) — two same-named files in different subdirectories would silently
        // clobber each other, so collisions are surfaced loudly.
        val seenNames = mutableSetOf<String>()
        return originalSnapshots.map { snapshot ->
            if (!seenNames.add(snapshot.name)) {
                project.logger.warn(
                    "Arkive: duplicate snapshot filename '${snapshot.name}' — " +
                        "'${snapshot.absolutePath}' overwrites a previously grabbed copy",
                )
            }
            val destinationFile = File(outputDir, snapshot.name)
            Files.copy(snapshot.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            GrabbedSnapshot(
                sourceFile = snapshot,
                relativePath = "${outputDir.name}${File.separatorChar}${snapshot.name}",
            )
        }
    }
}
