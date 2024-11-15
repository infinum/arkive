package com.infinum.arkive.plugin.services

import com.inifnum.arkive.metadata.model.ComponentsMetaData
import com.inifnum.arkive.metadata.toComponentsMetaData
import org.gradle.api.Project
import java.io.File

interface MetadataLoader {
    fun loadMetaData(): ComponentsMetaData
}

interface ProcessorMetadataLoader : MetadataLoader {
    fun getMetaDataFile(): File
    override fun loadMetaData(): ComponentsMetaData =
        getMetaDataFile().readText().toComponentsMetaData()
}

// TODO: Support for KAPT
private const val KSP_META_DATA_PATH =
    "generated/ksp/debug/resources/META-INF/arkive/components_meta_data.json"

class KSPMetaDataLoader(
    private val project: Project,
) : ProcessorMetadataLoader {

    override fun getMetaDataFile(): File =
        project.layout.buildDirectory.get().asFile.resolve(KSP_META_DATA_PATH)

}