package com.infinum.arkive.plugin.writers

import com.infinum.arkive.metadata.model.ArkiveModule
import com.infinum.arkive.metadata.toJson
import java.io.File

interface ShowcaseWriter {
    fun write(outputDir: File, module: ArkiveModule)
}

private const val FILE_NAME = "arkive-showcase.json"

class ShowcaseWriterImpl : ShowcaseWriter {
    override fun write(outputDir: File, module: ArkiveModule) {
        outputDir.resolve(FILE_NAME).writeText(toJson(module))
    }
}
