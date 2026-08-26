package com.infinum.arkive.plugin.generators

import java.io.File
import java.io.InputStream

interface ShowcaseWebGenerator {
    fun generateWeb(outputDir: File)
}

private const val INDEX_HTML_FILE = "index.html"
private const val APP_JS_FILE = "app.js"
private const val STYLES_FILE = "styles.css"

private const val RESOURCES_DIR = "web"

class ShowcaseWebGeneratorImpl : ShowcaseWebGenerator {
    // Best way to get class loader in a plugin
    private val classLoader = Thread.currentThread().contextClassLoader

    override fun generateWeb(outputDir: File) {
        outputDir.mkdirs()

        listOf(
            INDEX_HTML_FILE,
            APP_JS_FILE,
            STYLES_FILE,
        ).forEach {
            writeResource(it, outputDir)
        }
    }

    private fun getResourceStream(resourcePath: String): InputStream =
        // Classpath resource names always use '/', regardless of OS — File.separatorChar
        // here would break resource lookup on Windows.
        classLoader.getResourceAsStream("$RESOURCES_DIR/$resourcePath")
            ?: error("Can't locate resource: $resourcePath")

    private fun writeResource(resource: String, outputDir: File) {
        val targetFile = File(outputDir, resource)
        getResourceStream(resource).use {
            targetFile.writeBytes(it.readBytes())
        }
    }
}
