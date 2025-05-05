package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.infinum.arkive.metadata.model.Component
import com.infinum.arkive.metadata.model.ComponentsMetaData
import com.infinum.arkive.metadata.toJson
import com.infinum.arkive.processor.models.UiComponentHolder
import java.io.File

class UiComponentMetaDataSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<UiComponentHolder>,
) : Spec {
    override fun write() {
        val writer = codeGenerator.createNewFileByPath(
            dependencies = Dependencies(false),
            path = META_DATA_RESOURCES_PATH,
            extensionName = META_DATA_FILE_EXTENSION,
        ).bufferedWriter()

        val components = holders.map { holder ->
            Component(
                id = holder.functionId,
                name = holder.name,
                functionName = holder.functionName,
                packageName = holder.packageName,
                group = holder.group,
                tags = holder.tags,
                extraMetadata = holder.extraMetadata,
            )
        }

        val metaData = ComponentsMetaData(components)

        writer.use {
            it.write(toJson(metaData))
        }
    }

    companion object {
        private val META_DATA_RESOURCES_PATH = "arkive${File.separator}components_meta_data"
        private const val META_DATA_FILE_EXTENSION = "json"
    }
}
