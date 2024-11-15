package com.infinum.arkive.plugin.generators

import java.io.File
import java.io.InputStream

interface ShowcaseWebGenerator {
    fun generateWeb(outputDir: File)
}

private const val INDEX_FILE = "index.html"
private const val STYLES_FILE = "styles.css"
private const val JS_FILE = "script.js"
private const val RESOURCES_DIR = "web"

class ShowcaseWebGeneratorImpl : ShowcaseWebGenerator {
    // Best way to get class loader in a plugin
    private val classLoader = Thread.currentThread().contextClassLoader

    override fun generateWeb(outputDir: File) {
        outputDir.mkdirs()

        writeResource(INDEX_FILE, outputDir)
        writeResource(STYLES_FILE, outputDir)
        writeResource(JS_FILE, outputDir)
    }

    private fun getResourceStream(resourcePath: String): InputStream =
        classLoader.getResourceAsStream("$RESOURCES_DIR${File.separatorChar}$resourcePath")
            ?: error("Can't locate resource: $resourcePath")

    private fun writeResource(resource: String, outputDir: File) {
        val targetFile = File(outputDir, resource)
        getResourceStream(resource).use {
            targetFile.writeBytes(it.readBytes())
        }
    }
}
