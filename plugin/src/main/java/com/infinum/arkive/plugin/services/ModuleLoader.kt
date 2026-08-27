package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.fromJson
import com.infinum.arkive.metadata.model.ArkiveModule
import java.io.File
import org.gradle.api.logging.Logger

private const val ARKIVE_SHOWCASE_FILE = "arkive-showcase.json"

interface ModuleLoader {
    fun loadModules(outputDir: File): List<ArkiveModule>
}

/**
 * Copies each module's generated showcase into the aggregate output and reads its JSON.
 * [moduleShowcaseDirs] (module name → module showcase dir) is resolved at configuration
 * time by the plugin — the loader itself never touches the Gradle model, so the task
 * stays configuration-cache safe.
 */
class ModuleLoaderImpl(
    private val moduleShowcaseDirs: Map<String, File>,
    private val logger: Logger,
) : ModuleLoader {
    override fun loadModules(outputDir: File): List<ArkiveModule> {
        return moduleShowcaseDirs.mapNotNull { (moduleName, moduleDir) ->
            if (moduleDir.isDirectory) {
                // Must match the name stamped into the module's JSON (the web app
                // resolves images as <moduleName>/images/...) and stay unique per module.
                val destinationDir = File(outputDir, moduleName)
                moduleDir.copyRecursively(destinationDir, overwrite = true)
                fromJson(File(destinationDir, ARKIVE_SHOWCASE_FILE).readText())
            } else {
                logger.warn("Arkive: module '$moduleName' has no generated showcase at ${moduleDir.path} — skipped")
                null
            }
        }
    }
}
