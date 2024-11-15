package com.infinum.arkive.plugin.services

import org.gradle.api.Project
import java.io.File

interface SnapshotsLoader {
    fun loadSnapshots(): List<String>
}


private const val SCREEN_SHOTS_PATH = "src/test/snapshots/images"

class SnapshotsLoaderImpl(
    private val project: Project
) : SnapshotsLoader {

    override fun loadSnapshots(): List<String> {
        return screenshotDir.listFiles().orEmpty().map { it.path }
    }

    private val screenshotDir: File
        get() = project.projectDir.resolve(SCREEN_SHOTS_PATH)

}