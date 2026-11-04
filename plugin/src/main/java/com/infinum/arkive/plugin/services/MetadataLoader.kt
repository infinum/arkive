package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.fromJson
import com.infinum.arkive.metadata.model.ComponentsMetaData
import java.io.File

internal interface MetadataLoader {
    fun loadMetaData(): ComponentsMetaData
}

internal interface ProcessorMetadataLoader : MetadataLoader {
    fun getMetaDataFile(): File
    override fun loadMetaData(): ComponentsMetaData =
        fromJson(getMetaDataFile().readText())
}

// TODO: Support for KAPT
private val ARKIVE_METADATA_FILE =
    "arkive${File.separator}components_meta_data.json"

/**
 * Reads the metadata JSON the processor emits as a KSP resource. Where KSP puts its
 * output differs per project flavor, so the resources dir (relative to the build dir)
 * comes from the ConsumerAdapter through the task.
 */
internal class KSPMetaDataLoader(
    private val buildDir: File,
    private val kspResourcesPath: String,
) : ProcessorMetadataLoader {

    override fun getMetaDataFile(): File {
        return buildDir
            .resolve(kspResourcesPath)
            .resolve(ARKIVE_METADATA_FILE)
    }
}
