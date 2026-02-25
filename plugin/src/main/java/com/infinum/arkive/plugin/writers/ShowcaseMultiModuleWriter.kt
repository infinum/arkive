package com.infinum.arkive.plugin.writers

import com.infinum.arkive.metadata.model.ArkiveShowcase
import com.infinum.arkive.metadata.toJson
import java.io.File

interface ShowcaseMultiModuleWriter {
    fun write(outputDir: File, showcase: ArkiveShowcase)
}

private const val FILE_NAME = "arkive-showcase.json"

class ShowcaseMultiModuleWriterImpl : ShowcaseMultiModuleWriter {
    override fun write(outputDir: File, showcase: ArkiveShowcase) {
        outputDir.resolve(FILE_NAME).writeText(toJson(showcase))
    }
}
