package com.infinum.arkive.plugin.services

import com.inifnum.arkive.metadata.fromJson
import com.inifnum.arkive.metadata.model.ComponentsMetaData
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import java.io.File

interface MetadataLoader {
    fun loadMetaData(variant: String): ComponentsMetaData
}

interface ProcessorMetadataLoader : MetadataLoader {
    fun getMetaDataFile(variant: String): File
    override fun loadMetaData(variant: String): ComponentsMetaData =
        fromJson(getMetaDataFile(variant).readText())
}

// TODO: Support for KAPT
@SuppressWarnings("MaximumLineLength")
private val KSP_META_DATA_PATH =
    "generated${File.separator}ksp%s${File.separator}resources${File.separator}arkive${File.separator}components_meta_data.json"

class KSPMetaDataLoader(
    private val project: Project,
) : ProcessorMetadataLoader {

    override fun getMetaDataFile(variant: String): File {
        logger.warn("variant: $variant")
        val variantSegment = "${File.separator}$variant".takeIf { variant.isNotEmpty() }
        logger.warn("variantSegment: $variantSegment")

        return project.layout.buildDirectory.get().asFile.resolve(
            KSP_META_DATA_PATH.format(
                variantSegment.orEmpty()
            )
        )
    }
}
