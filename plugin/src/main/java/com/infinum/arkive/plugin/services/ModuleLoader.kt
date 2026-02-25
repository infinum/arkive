package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.fromJson
import com.infinum.arkive.metadata.model.ArkiveModule
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import java.io.File


private val MODULE_ARKIVE_DIR = "generated${File.separatorChar}arkive${File.separatorChar}showcase"
private const val ARKIVE_SHOWCASE_FILE = "arkive-showcase.json"

interface ModuleLoader {
    fun loadModules(outputDir: File): List<ArkiveModule>
}

class ModuleLoaderImpl(
    private val project: Project,
) : ModuleLoader {
    override fun loadModules(outputDir: File): List<ArkiveModule> {
        return project.subprojects
            .filter {
                it.pluginManager.hasPlugin("com.infinum.arkive")
            }
            .map { module ->
                logger.warn("Loading module")
                val moduleDir = module.layout.buildDirectory.file(MODULE_ARKIVE_DIR).get().asFile
                logger.warn("Module output path: ${moduleDir.path}")
                val destinationDir = File(outputDir, module.name)
                project.copy { spec ->
                    spec.from(moduleDir)
                    spec.into(destinationDir)
                }

                fromJson(File(destinationDir, ARKIVE_SHOWCASE_FILE).readText())
            }
    }

}