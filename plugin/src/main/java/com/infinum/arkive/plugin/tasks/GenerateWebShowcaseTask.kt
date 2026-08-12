package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.metadata.model.ArkiveShowcase
import com.infinum.arkive.plugin.generators.ShowcaseWebGeneratorImpl
import com.infinum.arkive.plugin.services.ModuleLoaderImpl
import com.infinum.arkive.plugin.writers.ShowcaseMultiModuleWriterImpl
import java.io.File
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class GenerateWebShowcaseTask : SourceTask() {

    @get:OutputDirectory
    val outputDirectory: Provider<Directory>
        get() = project.layout.buildDirectory.dir(
            FD_GENERATED,
        )

    // Required to invalidate the task on version updates.
    @Suppress("unused")
    @get:Input
    val pluginVersion: Property<String>
        get() = project.objects.property(String::class.java)
            .convention("0.0.1") // TODO automate this

    @TaskAction
    fun doOnRun() {
        val moduleLoader = ModuleLoaderImpl(project)
        val modules = moduleLoader.loadModules(outputDirectory.get().asFile)
        val showcase = ArkiveShowcase(
            project.name,
            modules,
        )

        val multiModuleWriter = ShowcaseMultiModuleWriterImpl()
        multiModuleWriter.write(outputDirectory.get().asFile, showcase)

        val webGenerator = ShowcaseWebGeneratorImpl()
        webGenerator.generateWeb(outputDirectory.get().asFile)
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    override fun getSource(): FileTree =
        super.getSource()

    companion object {
        const val GROUP = "arkive"
        const val NAME = "generateWebShowcase"
        const val DESCRIPTION = "Generates arkive web showcase "
        val FD_GENERATED = "generated${File.separatorChar}arkive${File.separatorChar}showcase"
    }
}
