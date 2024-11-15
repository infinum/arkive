package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.plugin.tasks.shared.BaseSourceTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
internal open class GenerateShowcaseTask : BaseSourceTask() {

    companion object {
        const val GROUP = "arkive"
        const val NAME = "generateShowcase"
        const val DESCRIPTION = "Generates arkive showcase json file"
        const val FD_GENERATED = "generated"
    }


    // Required to invalidate the task on version updates.
    @Suppress("unused", "ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR")
    @get:Input
    private val pluginVersion = "0.0.1" // TODO automate this

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    override fun getSource(): FileTree =
        super.getSource()

    @get:OutputDirectory
    var outputDirectory: File = project.layout.buildDirectory.get().file(
        "$FD_GENERATED${File.separatorChar}}"
    ).asFile

    @TaskAction
    fun doOnRun() {

    }

}
