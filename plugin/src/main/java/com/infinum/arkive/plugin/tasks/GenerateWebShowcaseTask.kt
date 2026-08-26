package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.metadata.model.ArkiveShowcase
import com.infinum.arkive.plugin.generators.ShowcaseWebGeneratorImpl
import com.infinum.arkive.plugin.services.ModuleLoaderImpl
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.writers.ShowcaseMultiModuleWriterImpl
import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

// Everything the action needs is captured as task properties at registration — the
// action never touches `project`, so the task serializes under the configuration cache.
@CacheableTask
internal abstract class GenerateWebShowcaseTask : SourceTask() {

    /** `<root build>/generated/arkive/showcase`; set at registration. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    // Required to invalidate the task on version updates.
    @Suppress("unused")
    @get:Input
    val pluginVersion: String = ArkiveVersion.current

    @get:Input
    var projectName = ""

    /** Module name → module showcase dir; resolved lazily from the arkive subprojects. */
    @get:Internal
    abstract val moduleShowcaseDirs: MapProperty<String, File>

    @TaskAction
    fun doOnRun() {
        val moduleLoader = ModuleLoaderImpl(moduleShowcaseDirs.get(), logger)
        val modules = moduleLoader.loadModules(outputDirectory.get().asFile)
        val showcase = ArkiveShowcase(
            projectName,
            modules,
        )

        val multiModuleWriter = ShowcaseMultiModuleWriterImpl()
        multiModuleWriter.write(outputDirectory.get().asFile, showcase)

        val webGenerator = ShowcaseWebGeneratorImpl()
        webGenerator.generateWeb(outputDirectory.get().asFile)
    }

    // RELATIVE: cache keys must not embed machine-specific absolute paths, or the build
    // cache can never hit across machines/checkouts.
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree =
        super.getSource()

    companion object {
        const val GROUP = "arkive"
        const val NAME = "generateWebShowcase"
        const val DESCRIPTION = "Generates arkive web showcase "
        val FD_GENERATED = "generated${File.separatorChar}arkive${File.separatorChar}showcase"
    }
}
