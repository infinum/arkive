package com.infinum.arkive.plugin.tasks

import com.infinum.arkive.metadata.model.ArkiveModule
import com.infinum.arkive.metadata.model.ShowcaseItem
import com.infinum.arkive.plugin.extensions.SnapshotRetention
import com.infinum.arkive.plugin.generators.ShowcaseGeneratorImpl
import com.infinum.arkive.plugin.services.GrabbedSnapshot
import com.infinum.arkive.plugin.services.KSPMetaDataLoader
import com.infinum.arkive.plugin.services.SnapshotsGrabberImpl
import com.infinum.arkive.plugin.services.UiTreeGrabber
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.writers.ShowcaseWriterImpl
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

// Everything the action needs is captured as task properties at registration — the
// action never touches `project`, so the task serializes under the configuration cache.
//
// Never cached and never up-to-date (the latter wired at registration, ArkivePlugin
// .addTaskWithVariant): the action has a side effect on its own inputs — snapshotRetention
// deletes the goldens it consumed — that a skipped run silently drops. Re-recorded PNGs
// are byte-identical whenever the rendering didn't change, so a skip was common: NONE then
// left every consumed golden in src/. Its output isn't a pure function of its inputs
// either (the keep-previous branch below returns the PREVIOUS output). The work is file
// copies and one JSON file, so always running costs about what a cache restore would.
@DisableCachingByDefault(
    because = "Consumes its own inputs (snapshotRetention deletes goldens) and may keep its previous output",
)
internal abstract class GenerateShowcaseTask : SourceTask() {

    /** `<build>/generated/arkive/showcase`; set at registration. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** The module directory (golden dir parent); set at registration. */
    @get:Internal
    abstract val moduleDirectory: DirectoryProperty

    /** The module build directory (KSP output parent); set at registration. */
    @get:Internal
    abstract val buildDir: DirectoryProperty

    // Required to invalidate the task on version updates.
    @Suppress("unused")
    @get:Input
    val pluginVersion: String = ArkiveVersion.current

    @get:Input
    var variant = ""

    /** Path-derived module name (bare project names collide in nested layouts). */
    @get:Input
    var moduleName = ""

    /** KSP-generated resources dir, relative to the build dir; set from the ConsumerAdapter. */
    @get:Input
    var kspResourcesPath = ""

    /** Paparazzi's golden dir, relative to the module dir; set from the ConsumerAdapter. */
    @get:Input
    var snapshotsPath = ""

    @get:Input
    var designFileKey = ""

    @get:Input
    var snapshotRetention = SnapshotRetention.NONE.name

    /** The test JVM's UI-tree dir for this variant, relative to the build dir; from UiTreeWiring. */
    @get:Input
    var uiTreePath = ""

    /**
     * With `uiTree` off, trees an earlier enabled run left on disk are ignored — the showcase
     * then carries no uiTreePath at all.
     */
    @get:Input
    var uiTreeEnabled = true

    // NOTE: the engine's record-task dependency is wired at registration (ArkivePlugin.
    // addTaskWithVariant). Never register listeners from this class's init — tasks can
    // be realized lazily from guarded contexts (e.g. findByName inside a projectsEvaluated
    // callback when several modules apply the plugin), where that is illegal.

    @TaskAction
    fun doOnRun() {
        val snapshotsGrabber = SnapshotsGrabberImpl(
            snapshotsDir = moduleDirectory.get().asFile.resolve(snapshotsPath),
            logger = logger,
        )
        val metadataLoader = KSPMetaDataLoader(buildDir.get().asFile, kspResourcesPath)
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

        // Under snapshotRetention NONE this task CONSUMES the goldens it grabs — so on a
        // re-run where the recording task was up-to-date (nothing new recorded), the
        // golden directory is legitimately empty. Regenerating from zero snapshots would
        // overwrite a perfectly good showcase with an empty one; keep the previous output.
        // This is meant for UP-TO-DATE only. A record run served FROM-CACHE without its PNGs
        // would keep a showcase built from other code, so a run that records outside the
        // test task's outputs never comes from the cache (ArkivePlugin
        // .keepRecordRunsOutOfBuildCache); Paparazzi's cache restores its PNGs.
        val previousOutput = outputDirectory.get().asFile.resolve(ARKIVE_SHOWCASE_FILE_NAME)
        if (grabbed.isEmpty() && previousOutput.exists()) {
            logger.warn(
                "Arkive: no new snapshots to consume — keeping the previously generated " +
                    "showcase at ${previousOutput.absolutePath}",
            )
            return
        }

        logger.warn("Loading metadata for variant: $variant")
        val metadata = metadataLoader.loadMetaData()

        val uiTreeGrabber = UiTreeGrabber(buildDir.get().asFile.resolve(uiTreePath))
        val uiTrees = if (uiTreeEnabled) uiTreeGrabber.available() else emptyMap()
        val moduleItems = generator.generateShowcase(
            grabbed.map { it.relativePath },
            metadata,
            uiTreeGrabber.relativePaths(uiTrees),
        )

        // Per-component resilience must not compound into a silent total failure: if the
        // processor collected components but not a single snapshot matched, recording
        // broke wholesale (theme, engine/AGP mismatch, OOM in the test JVM) and an empty
        // showcase would sail through CI and deploy. Fail loudly instead.
        if (metadata.components.isNotEmpty() && moduleItems.isEmpty()) {
            throw GradleException(
                "Arkive: recording produced no usable snapshot for any of the " +
                    "${metadata.components.size} collected component(s) — the showcase " +
                    "would be empty. Recording likely failed wholesale; re-run the test " +
                    "task with --info for the per-component crash messages.",
            )
        }

        val pairedTrees = uiTreeGrabber.copyReferenced(moduleItems, uiTrees, outputDirectory.get().asFile)
        logger.info("Arkive: paired $pairedTrees ui tree(s) with the showcase")
        applyRetention(grabbed, moduleItems)
        writer.write(
            outputDir = outputDirectory.get().asFile,
            module = ArkiveModule(moduleName, moduleItems, designFileKey.takeIf { it.isNotEmpty() }),
        )

        logger.warn("Showcase written to ${previousOutput.absolutePath}")
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

    // The task never skips (see the class comment), so the fingerprint feeds no up-to-date
    // check or cache key; the declared source still drives task dependencies and NO-SOURCE.
    // RELATIVE keeps that fingerprint free of machine-specific absolute paths.
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree =
        super.getSource()

    companion object {
        const val GROUP = "arkive"
        const val NAME = "generateShowcase"
        const val DESCRIPTION = "Generates arkive showcase json file"
        val FD_GENERATED = "generated${File.separatorChar}arkive${File.separatorChar}showcase"
        const val IMAGES_OUTPUT_PATH = "images"
        const val ARKIVE_SHOWCASE_FILE_NAME = "arkive-showcase.json"
    }
}
