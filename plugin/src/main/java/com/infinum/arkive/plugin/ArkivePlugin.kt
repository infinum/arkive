package com.infinum.arkive.plugin

import com.infinum.arkive.plugin.consumers.ConsumerAdapter
import com.infinum.arkive.plugin.engines.PaparazziEngine
import com.infinum.arkive.plugin.engines.SnapshotEngineAdapter
import com.infinum.arkive.plugin.extensions.ArkiveExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_PREVIEW_PARAMETERS
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_VARIANTS
import com.infinum.arkive.plugin.extensions.SnapshotRetention
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import com.infinum.arkive.plugin.utils.RetentionArgumentProvider
import com.infinum.arkive.plugin.utils.capFirst
import com.infinum.arkive.plugin.utils.showcaseModuleName
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

class ArkivePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project == project.rootProject) {
            // Root application hosts the aggregate task. It must be registered eagerly:
            // with org.gradle.configureondemand, task-name resolution happens right after
            // the root project configures — before any projectsEvaluated callback fires —
            // so callback-time registration is invisible to it. Children are forced from
            // afterEvaluate (not mid-script, which would reorder root-side configuration),
            // and the module task dependencies resolve lazily once they exist.
            project.afterEvaluate {
                project.evaluationDependsOnChildren()
            }
            project.tasks.register(
                GenerateWebShowcaseTask.NAME,
                GenerateWebShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateWebShowcaseTask.GROUP
                task.description = GenerateWebShowcaseTask.DESCRIPTION
                task.outputDirectory.set(project.layout.buildDirectory.dir(GenerateWebShowcaseTask.FD_GENERATED))
                task.projectName = project.name
                task.moduleShowcaseDirs.set(moduleShowcaseDirMap(project))
                task.setSource(moduleShowcaseOutputDirs(project))
                task.dependsOn(project.provider { moduleShowcaseTaskPaths(project) })
            }
            return
        }

        with(project) {
            // Selection sources, by priority: the arkive.engine Gradle property (per-run
            // and org-wide control), then the script's engine(...) DSL call, then the
            // Roborazzi default. The DSL call executes during the script body — still
            // inside AGP's variant window, so the chosen engine's plugin can be applied
            // on the spot. Everything engine-dependent besides plugin application is
            // deferred until the selection is final (afterEvaluate / variant callbacks).
            val selection = EngineSelection(this)
            addExtension(this, selection)
            // All flavor-specific work (dependency wiring, variant discovery, paths)
            // happens behind the adapter selected for this module's Android plugin.
            ConsumerAdapter.select(this) { adapter ->
                configureConsumer(this, adapter, selection)
            }
            addRootTasks(rootProject)
        }
    }

    private fun configureConsumer(
        project: Project,
        adapter: ConsumerAdapter,
        selection: EngineSelection,
    ) {
        project.logger.info("Arkive: configuring ${adapter.flavor} consumer")

        project.extensions.findByType(ArkiveExtension::class.java)
            ?.multiModuleVariant
            ?.convention(adapter.defaultMultiModuleVariant)

        selection.applyCurrentEngine()
        adapter.wireDependencies()
        forwardSnapshotsDirToTests(project, adapter)
        project.afterEvaluate {
            val engine = selection.engine
            project.logger.info("Arkive: ${adapter.flavor} consumer renders with ${engine.engineName}")
            engine.wireTestDependencies(project, adapter.testImplementationConfigurationName)
            engine.configureTestTasks(project, adapter.snapshotsPath())
        }
        adapter.onVariants { variant ->
            addTaskWithVariant(project, adapter, selection, variant)
        }
    }

    // The generated Roborazzi test records to (and verifies against) explicit file paths
    // inside the adapter's golden directory — the same directory Paparazzi uses — so the
    // grabber/retention/verify pipeline stays engine-agnostic. Paparazzi ignores this.
    private fun forwardSnapshotsDirToTests(project: Project, adapter: ConsumerAdapter) {
        val snapshotsDir = project.projectDir.resolve(adapter.snapshotsPath()).absolutePath
        project.tasks.withType(Test::class.java).configureEach { test ->
            test.jvmArgumentProviders.add(
                CommandLineArgumentProvider {
                    listOf("-Darkive.snapshots.dir=$snapshotsDir")
                },
            )
        }
    }

    private fun forwardRetentionToTests(project: Project, extension: ArkiveExtension) {
        // The generated snapshot test reads retention at runtime to decide which verify
        // guards apply (see ArkiveTestProcessor). The Provider defers reading the value
        // until the test's inputs are resolved — after the consumer's script ran — and the
        // named provider class keeps the Test task configuration-cache safe and makes the
        // forwarded value a tracked input (see RetentionArgumentProvider).
        val retention = extension.snapshotRetention.map { it.name }
        project.tasks.withType(Test::class.java).configureEach { test ->
            test.jvmArgumentProviders.add(RetentionArgumentProvider(retention))
        }
    }

    private fun addExtension(project: Project, selection: EngineSelection) {
        val extension = project.extensions.create(
            "arkive",
            ArkiveExtension::class.java,
        )
        // Wired here, right after creation, so the provider can capture the extension's
        // property (never the Project) regardless of the consumer's plugin order.
        forwardRetentionToTests(project, extension)
        // The extension exists before the script body runs, so the engine(...) DSL call
        // reaches the selection the moment the script makes it.
        extension.onEngineSelected = selection::selectFromDsl

        project.afterEvaluate {
            // Outside the try/catch below on purpose: reading the engine is what enforces
            // the mandatory selection, and its error must fail the build, not become a
            // "failed to pass KSP arg" warning.
            val engineName = selection.engine.engineName

            val enablePreviewParameters = extension.enablePreviewParameters.get()
            val enableVariants = extension.enableVariants.get()

            // Reflective on purpose: a typed dependency on the KSP Gradle plugin would
            // pin one KSP version onto every consumer, while `arg(String, String)` has
            // been stable across the KSP versions consumers actually use.
            project.extensions.findByName("ksp")?.let { kspExt ->
                val kspArgs = listOf(
                    ENABLE_PREVIEW_PARAMETERS to enablePreviewParameters.toString(),
                    ENABLE_VARIANTS to enableVariants.toString(),
                    // The test processor generates the test class matching the engine.
                    SnapshotEngineAdapter.ENGINE_KSP_ARG to engineName,
                    // Baked into the generated test's @Config so Robolectric keeps one
                    // cached environment per module; changing device re-records.
                    ArkiveExtension.DEVICE to extension.roborazziOptions.device.get(),
                )
                try {
                    val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                    kspArgs.forEach { (key, value) -> argMethod.invoke(kspExt, key, value) }
                } catch (e: Exception) {
                    project.logger.warn(
                        "Arkive: failed to pass KSP args ${kspArgs.map { it.first }}: ${e.message}",
                    )
                }
            }
        }
    }

    private fun addTaskWithVariant(
        project: Project,
        adapter: ConsumerAdapter,
        selection: EngineSelection,
        variant: String,
    ) {
        with(project) {
            tasks.register(
                showcaseTaskName(variant),
                GenerateShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateShowcaseTask.GROUP
                task.description = GenerateShowcaseTask.DESCRIPTION
                task.variant = variant
                task.kspResourcesPath = adapter.kspResourcesPath(variant)
                task.snapshotsPath = adapter.snapshotsPath()
                task.outputDirectory.set(layout.buildDirectory.dir(GenerateShowcaseTask.FD_GENERATED))
                task.moduleDirectory.set(layout.projectDirectory)
                task.buildDir.set(layout.buildDirectory)
                // Path-derived name: bare project names collide in nested layouts
                // (":epos:ui" vs ":cfs:ui") and it doubles as the aggregate's module dir.
                task.moduleName = project.showcaseModuleName
                val extension = project.extensions.findByType(ArkiveExtension::class.java)
                task.designFileKey = extension?.designFileKey?.get().orEmpty()
                task.snapshotRetention = snapshotRetentionOf(project).name
                // Task configuration runs at realization, after the script body — the
                // selection is final by then.
                task.dependsOn(selection.engine.recordTaskName(adapter, variant))
                // Only what the task actually reads. Declaring a broader tree (e.g. the
                // project dir) makes every unrelated task's output an undeclared input,
                // which strict Gradle validation rejects when they share an invocation.
                task.setSource(
                    files(
                        layout.projectDirectory.dir(adapter.snapshotsPath()),
                        layout.buildDirectory.dir(adapter.kspResourcesPath(variant)),
                    ),
                )
            }
        }
        if (hasGeneratedTestClass(adapter, variant)) {
            addVerifyTaskWithVariant(project, adapter, selection, variant)
        }
    }

    // The generated test class only exists where the test-source KSP ran — debug build
    // types on android modules, the single host-test compilation on KMP — so a verify
    // task for any other variant could only fail with an opaque "No tests found".
    private fun hasGeneratedTestClass(adapter: ConsumerAdapter, variant: String): Boolean {
        if (variant.isEmpty() || variant == "debug" || variant.endsWith("Debug")) {
            return true
        }
        return variant == adapter.defaultMultiModuleVariant
    }

    private fun showcaseTaskName(variant: String) = "${GenerateShowcaseTask.NAME}${variant.capFirst}"

    private fun snapshotRetentionOf(project: Project): SnapshotRetention =
        project.extensions.findByType(ArkiveExtension::class.java)
            ?.snapshotRetention?.get() ?: SnapshotRetention.NONE

    /**
     * `verifyShowcase<Variant>` is the public verify entry point: it runs the engine's
     * verify scoped to Arkive's generated test class only, so the consumer's own snapshot
     * tests and goldens are never pulled into an Arkive verification (mirroring the
     * boundary SnapshotsGrabber keeps on the golden directory).
     */
    private fun addVerifyTaskWithVariant(
        project: Project,
        adapter: ConsumerAdapter,
        selection: EngineSelection,
        variant: String,
    ) {
        val verifyTaskName = "$VERIFY_SHOWCASE_TASK${variant.capFirst}"
        project.tasks.register(verifyTaskName) { task ->
            task.group = GenerateShowcaseTask.GROUP
            task.description = "Verifies Arkive snapshots against the retained goldens"
            task.dependsOn(selection.engine.verifyTaskName(adapter, variant))
        }

        project.gradle.taskGraph.whenReady { graph ->
            val verifyTask = project.tasks.findByName(verifyTaskName)
            if (verifyTask != null && graph.hasTask(verifyTask)) {
                if (snapshotRetentionOf(project) == SnapshotRetention.NONE) {
                    throw GradleException(
                        "Arkive: $verifyTaskName has nothing to verify — snapshotRetention is NONE, " +
                            "so no goldens are retained. Set arkive.snapshotRetention to BASE or ALL " +
                            "and record goldens with ${showcaseTaskName(variant)} first.",
                    )
                }
                failOnConflictingTasks(
                    project,
                    graph,
                    verifyTaskName,
                    variant,
                    selection.engine.recordTaskName(adapter, variant),
                )
                val testTask = project.tasks.findByName(adapter.unitTestTaskName(variant)) as? Test
                testTask?.filter?.includeTestsMatching(GENERATED_TEST_CLASS)
            }
        }
    }

    /**
     * The verify filter narrows the consumer's SHARED unit-test task to Arkive's generated
     * class for the whole invocation, and Paparazzi resolves record-vs-verify from
     * properties on that same task. Any co-scheduled task that drives those tests (check,
     * build) would silently run none of the consumer's own tests, and a co-scheduled
     * record (generateShowcase / recordPaparazzi) fights verify over the mode — fail fast
     * instead of silently misbehaving.
     */
    private fun failOnConflictingTasks(
        project: Project,
        graph: TaskExecutionGraph,
        verifyTaskName: String,
        variant: String,
        recordTaskName: String,
    ) {
        // The variant's own unit-test task can't be listed here — it is always in the
        // graph as verifyPaparazzi's dependency, so a direct request for it is
        // indistinguishable. The aggregates below catch the common combinations.
        val conflicting = listOfNotNull(
            project.tasks.findByName("check"),
            project.tasks.findByName("build"),
            project.tasks.findByName("test"),
            project.tasks.findByName(showcaseTaskName(variant)),
            project.tasks.findByName(recordTaskName),
        ).filter { graph.hasTask(it) }
        if (conflicting.isNotEmpty()) {
            throw GradleException(
                "Arkive: $verifyTaskName cannot share an invocation with " +
                    conflicting.joinToString { it.name } +
                    " — the verify filter restricts the shared unit-test task to Arkive's " +
                    "generated class (their tests would silently not run), and record and " +
                    "verify flip the same Paparazzi mode. Run $verifyTaskName in its own " +
                    "Gradle invocation.",
            )
        }
    }

    private fun addRootTasks(rootProject: Project) {
        if (rootProject.tasks.findByName(GenerateWebShowcaseTask.NAME) != null) {
            return
        }
        rootProject.gradle.projectsEvaluated {
            // Root and module applications may both schedule this; only register once.
            if (rootProject.tasks.findByName(GenerateWebShowcaseTask.NAME) == null) {
                rootProject.tasks.register(
                    GenerateWebShowcaseTask.NAME,
                    GenerateWebShowcaseTask::class.java,
                ) { task ->
                    task.group = GenerateWebShowcaseTask.GROUP
                    task.description = GenerateWebShowcaseTask.DESCRIPTION
                    task.outputDirectory.set(
                        rootProject.layout.buildDirectory.dir(GenerateWebShowcaseTask.FD_GENERATED),
                    )
                    task.projectName = rootProject.name
                    task.moduleShowcaseDirs.set(moduleShowcaseDirMap(rootProject))
                    task.dependsOn(moduleShowcaseTaskPaths(rootProject))
                    task.setSource(moduleShowcaseOutputDirs(rootProject))
                }
            }
        }
    }

    // The task only reads each arkive module's showcase output (see ModuleLoaderImpl).
    // The source must stay exactly those directories: anything broader (like the root
    // projectDir) turns every other task's outputs — jacoco reports, lint results — into
    // undeclared inputs, which strict Gradle validation rejects. Lazy because at
    // root-apply time the modules haven't been configured yet.
    // Module name -> generated showcase dir, resolved lazily (modules aren't configured
    // yet when the root task registers). The task holds plain names/files, never Projects.
    private fun moduleShowcaseDirMap(rootProject: Project) =
        rootProject.provider {
            rootProject.subprojects
                .filter { it.pluginManager.hasPlugin(PLUGIN_ID) }
                .associate { module ->
                    module.showcaseModuleName to
                        module.layout.buildDirectory.dir(GenerateWebShowcaseTask.FD_GENERATED).get().asFile
                }
        }

    private fun moduleShowcaseOutputDirs(rootProject: Project) =
        rootProject.provider {
            rootProject.subprojects
                .filter { it.pluginManager.hasPlugin(PLUGIN_ID) }
                .map { it.layout.buildDirectory.dir(GenerateWebShowcaseTask.FD_GENERATED) }
        }

    private fun moduleShowcaseTaskPaths(rootProject: Project): List<String> {
        return rootProject.subprojects
            .filter { it.pluginManager.hasPlugin(PLUGIN_ID) }
            .mapNotNull { module ->
                val variant = module.extensions.findByType(ArkiveExtension::class.java)
                    ?.multiModuleVariant?.get().orEmpty()
                module.tasks.findByName(showcaseTaskName(variant))?.path
            }
    }

    companion object {
        private const val PLUGIN_ID = "com.infinum.arkive"
        private const val VERIFY_SHOWCASE_TASK = "verifyShowcase"
        private const val GENERATED_TEST_CLASS = "com.infinum.arkive.ArkiveSnapshotTestGenerator"
    }
}

/**
 * The module's engine choice. **There is no default engine** — every module must select
 * one, via the `arkive.engine` Gradle property (per-run/org-wide control, outranks the
 * DSL) or the script's `engine(...)` DSL call. Plugin application happens the moment a
 * selection is made — property at configure time, DSL during the script body, both while
 * AGP's variant window is still open. Reading [engine] before any selection fails the
 * build with instructions.
 */
private class EngineSelection(private val project: Project) {

    private val fromProperty = SnapshotEngineAdapter.fromGradleProperty(project)
    private var applied: SnapshotEngineAdapter? = null
    private var dslSelection: SnapshotEngineAdapter? = null

    private val resolved: SnapshotEngineAdapter?
        get() = fromProperty ?: dslSelection

    /** The selected engine; throws with selection instructions when the module chose none. */
    val engine: SnapshotEngineAdapter
        get() = resolved ?: throw GradleException(
            """
            Arkive: no snapshot engine selected for ${project.path} — choose one in the arkive block:

                arkive {
                    engine(Roborazzi)   // JDK 17+; renders Compose Multiplatform resources;
                                        // real framework rendering via Robolectric
                    // or
                    engine(Paparazzi)   // requires a JDK 21+ Gradle daemon; pixel-identical
                                        // to Android Studio previews; CMP resources DON'T render
                }

            (or force one for the whole build with the Gradle property arkive.engine=roborazzi|paparazzi)
            """.trimIndent(),
        )

    /** Applies the property-selected engine eagerly; a DSL selection applies itself when made. */
    fun applyCurrentEngine() {
        applyEngine(resolved ?: return)
    }

    fun selectFromDsl(engineName: String) {
        val requested = SnapshotEngineAdapter.fromName(engineName)
        dslSelection?.let { previous ->
            if (previous != requested) {
                throw GradleException(
                    "Arkive: engine(...) called twice with different engines — " +
                        "a module renders with exactly one engine.",
                )
            }
            return
        }
        dslSelection = requested

        if (fromProperty != null) {
            if (fromProperty != requested) {
                project.logger.warn(
                    "Arkive: the ${SnapshotEngineAdapter.ENGINE_PROPERTY} Gradle property " +
                        "(${fromProperty.engineName}) overrides this script's " +
                        "engine(${requested.engineName}) selection.",
                )
            }
            return
        }
        // Script body — AGP's variant window is still open, safe to apply the plugin now.
        applyEngine(requested)
    }

    private fun applyEngine(engine: SnapshotEngineAdapter) {
        if (applied == engine) {
            return
        }
        requireCompatibleJdk(engine)
        engine.apply(project)
        applied = engine
    }

    private fun requireCompatibleJdk(engine: SnapshotEngineAdapter) {
        if (engine == PaparazziEngine && !JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
            throw GradleException(
                "Arkive: the Paparazzi engine requires a JDK 21+ Gradle daemon " +
                    "(current: JDK ${JavaVersion.current()}) — Paparazzi 2.0.0-alpha03+ ships " +
                    "Java 21 bytecode. Use the Roborazzi engine or raise the Gradle JDK.",
            )
        }
    }
}
