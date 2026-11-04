package com.infinum.arkive.composeutils.uitree

import java.io.File

internal object UiTreeFiles {
    /** Set on the test JVM by the Arkive Gradle plugin (UiTreeWiring) — keep the names in sync. */
    const val DIRECTORY_PROPERTY = "arkive.uitree.dir"

    fun directory(): File? = System.getProperty(DIRECTORY_PROPERTY)?.takeIf { it.isNotBlank() }?.let(::File)

    /** `<shotId>.json` — the plugin's UiTreeGrabber pairs trees to snapshots by this name. */
    fun file(directory: File, shotId: String): File = File(directory, "$shotId.json")
}
