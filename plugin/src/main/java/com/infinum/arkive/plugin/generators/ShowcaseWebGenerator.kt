package com.infinum.arkive.plugin.generators

import java.io.File
import java.io.InputStream

interface ShowcaseWebGenerator {
    fun generateWeb(outputDir: File)
}

private const val INDEX_HTML_FILE = "index.html"
private const val INDEX_JS_FILE = "index.js"
private const val MODULE_HTML_FILE = "module.html"
private const val MODULE_JS_FILE = "module.js"
private const val STYLES_FILE = "styles.css"

private const val RESOURCES_DIR = "web"

class ShowcaseWebGeneratorImpl : ShowcaseWebGenerator {
    // Best way to get class loader in a plugin
    private val classLoader = Thread.currentThread().contextClassLoader

    override fun generateWeb(outputDir: File) {
        outputDir.mkdirs()

        listOf(
            INDEX_HTML_FILE,
            INDEX_JS_FILE,
            MODULE_HTML_FILE,
            MODULE_JS_FILE,
            STYLES_FILE,
        ).forEach {
            writeResource(it, outputDir)
        }
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
