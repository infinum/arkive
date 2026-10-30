package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.model.ShowcaseItem
import java.io.File

/**
 * Pairs the UI trees the generated test wrote (`<shotId>.json`, one per snapshot) with the
 * showcase and copies the paired ones next to the images. Trees are informational: they
 * never live in the golden directory and retention never applies to them.
 */
class UiTreeGrabber(private val uiTreeDir: File) {

    /** Lowercased shot id → tree file (Paparazzi lowercases snapshot names; trees don't). */
    fun available(): Map<String, File> =
        uiTreeDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.associateBy { it.nameWithoutExtension.lowercase() }
            .orEmpty()

    /** Lowercased shot id → `uitree/<file name>` — the paths written into the showcase JSON. */
    fun relativePaths(available: Map<String, File>): Map<String, String> =
        available.mapValues { (_, file) -> "$OUTPUT_DIR/${file.name}" }

    /** Replaces `<showcaseDir>/uitree/` with exactly the trees [items] reference; returns how many. */
    fun copyReferenced(items: List<ShowcaseItem>, available: Map<String, File>, showcaseDir: File): Int {
        val referenced = items
            .flatMap { item -> listOf(item.uiTreePath) + item.variants.map { it.uiTreePath } }
            .filterNotNull()
            .toSet()
        val outputDir = showcaseDir.resolve(OUTPUT_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        val byPath = available.values.associateBy { "$OUTPUT_DIR/${it.name}" }
        referenced.forEach { path ->
            byPath[path]?.copyTo(outputDir.resolve(path.substringAfter('/')), overwrite = true)
        }
        return referenced.size
    }

    companion object {
        const val OUTPUT_DIR = "uitree"
    }
}
