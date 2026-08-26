package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.metadata.model.ArkiveModule
import com.infinum.arkive.metadata.model.ShowcaseItem
import com.infinum.arkive.plugin.extensions.SnapshotRetention
import com.infinum.arkive.plugin.generators.ShowcaseGeneratorImpl
import com.infinum.arkive.plugin.services.GrabbedSnapshot
import com.infinum.arkive.plugin.services.KSPMetaDataLoader
import com.infinum.arkive.plugin.services.SnapshotsGrabberImpl
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.utils.capFirst
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
internal abstract class GenerateShowcaseTask : SourceTask() {
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
            .convention(ArkiveVersion.current)

    @get:Input
    var variant = ""

    @get:Input
    var designFileKey = ""

    @get:Input
    var snapshotRetention = SnapshotRetention.NONE.name

    init {
        project.gradle.projectsEvaluated {
            val variantText = variant

            if (variantText.isEmpty()) {
                dependsOn(RECORDING_TASK)
            } else {
                dependsOn("$RECORDING_TASK${variantText.capFirst}")
            }
        }
    }

    @TaskAction
    fun doOnRun() {
        val snapshotsGrabber = SnapshotsGrabberImpl(project)
        val metadataLoader = KSPMetaDataLoader(project)
        val generator = ShowcaseGeneratorImpl(
            onMissingSnapshot = { id ->
                logger.warn("Arkive: no snapshot recorded for component '$id' — excluded from the showcase")
            },
            onMalformedVariant = { snapshot ->
                logger.warn("Arkive: unrecognized variant snapshot name '$snapshot' — skipped")
            },
        )
        val writer = ShowcaseWriterImpl()

        val grabbed = snapshotsGrabber.grabSnapshots(
            outputDir = outputDirectory.get().dir(IMAGES_OUTPUT_PATH).asFile,
        )

        val vrr = variant
        logger.warn("Loading metadata for variant: $vrr")
        val metadata = metadataLoader.loadMetaData(vrr)

        val moduleItems = generator.generateShowcase(grabbed.map { it.relativePath }, metadata)
        applyRetention(grabbed, moduleItems)
        // logger.warn("Showcase: $moduleItems")
        writer.write(
            outputDir = outputDirectory.get().asFile,
            module = ArkiveModule(project.name, moduleItems, designFileKey.takeIf { it.isNotEmpty() }),
        )

        logger.warn("Showcase written to ${outputDirectory.get().asFile.resolve("arkive-showcase.json").absolutePath}")
    }

    /**
     * Decides which recorded snapshots survive in Paparazzi's golden directory after the
     * showcase has taken its copies. Base vs variant is determined from the generated
     * showcase items — not filename heuristics.
     */
    private fun applyRetention(grabbed: List<GrabbedSnapshot>, items: List<ShowcaseItem>) {
        val retention = SnapshotRetention.valueOf(snapshotRetention)
        val removed = when (retention) {
            SnapshotRetention.ALL -> emptyList()
            SnapshotRetention.NONE -> grabbed
            SnapshotRetention.BASE -> {
                val basePaths = items.map { it.snapshotPath }.toSet()
                grabbed.filter { it.relativePath !in basePaths }
            }
        }
        removed.forEach { it.sourceFile.delete() }
        if (removed.isNotEmpty()) {
            logger.warn(
                "Arkive: removed ${removed.size} consumed snapshot(s) from the golden directory " +
                    "(snapshotRetention = $retention)",
            )
        }
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
