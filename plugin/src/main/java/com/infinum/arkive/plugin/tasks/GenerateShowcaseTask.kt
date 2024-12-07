package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.plugin.generators.ShowcaseGeneratorImpl
import com.infinum.arkive.plugin.services.KSPMetaDataLoader
import com.infinum.arkive.plugin.services.SnapshotsGrabberImpl
import com.infinum.arkive.plugin.writers.ShowcaseWriterImpl
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
internal open class GenerateShowcaseTask : SourceTask() {

    // Property<File>
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

    init {
        dependsOn(RECORDING_TASK)
    }

    @TaskAction
    fun doOnRun() {
        val snapshotsGrabber = SnapshotsGrabberImpl(project)
        val metadataLoader = KSPMetaDataLoader(project)
        val generator = ShowcaseGeneratorImpl()
        val writer = ShowcaseWriterImpl()

        val snapshots =
            snapshotsGrabber.grabAndMoveSnapshots(
                outputDirectory.get().dir(IMAGES_OUTPUT_PATH).asFile,
            )
        val metadata = metadataLoader.loadMetaData()

        val arkiveShowcase = generator.generateShowcase(snapshots, metadata)
        logger.warn("Showcase generated: $arkiveShowcase")
        writer.write(
            outputDir = outputDirectory.get().asFile,
            showcase = arkiveShowcase,
        )

        logger.warn("Showcase written")
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    override fun getSource(): FileTree =
        super.getSource()

    companion object {
        const val GROUP = "arkive"
        const val NAME = "generateShowcase"
        const val DESCRIPTION = "Generates arkive showcase json file"
        val FD_GENERATED = "generated${File.separatorChar}arkive${File.separatorChar}showcase"
        const val RECORDING_TASK = "recordPaparazzi"
        const val IMAGES_OUTPUT_PATH = "images"
    }
}
