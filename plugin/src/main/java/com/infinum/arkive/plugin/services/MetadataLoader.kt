package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.fromJson
import com.infinum.arkive.metadata.model.ComponentsMetaData
import java.io.File
import org.gradle.api.Project

interface MetadataLoader {
    fun loadMetaData(): ComponentsMetaData
}

interface ProcessorMetadataLoader : MetadataLoader {
    fun getMetaDataFile(): File
    override fun loadMetaData(): ComponentsMetaData =
        fromJson(getMetaDataFile().readText())
}

// TODO: Support for KAPT
@SuppressWarnings("MaximumLineLength")
private val KSP_META_DATA_PATH =
    "generated${File.separator}ksp${File.separator}debug${File.separator}resources${File.separator}arkive${File.separator}components_meta_data.json"

class KSPMetaDataLoader(
    private val project: Project,
) : ProcessorMetadataLoader {

    override fun getMetaDataFile(): File =
        project.layout.buildDirectory.get().asFile.resolve(KSP_META_DATA_PATH)
}
