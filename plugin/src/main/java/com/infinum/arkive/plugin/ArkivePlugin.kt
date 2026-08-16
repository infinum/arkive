package com.infinum.arkive.plugin

import com.infinum.arkive.plugin.consumers.ConsumerAdapter
import com.infinum.arkive.plugin.extensions.ArkiveExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_PREVIEW_PARAMETERS
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_VARIANTS
import com.infinum.arkive.plugin.extensions.SnapshotRetention
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask.Companion.RECORDING_TASK
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import com.infinum.arkive.plugin.utils.RetentionArgumentProvider
import com.infinum.arkive.plugin.utils.capFirst
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.testing.Test

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
                task.setSource(project.projectDir)
                task.dependsOn(project.provider { moduleShowcaseTaskPaths(project) })
            }
            return
        }

        with(project) {
            addExtension(this)
            // All flavor-specific work (dependency wiring, variant discovery, paths)
            // happens behind the adapter selected for this module's Android plugin.
            ConsumerAdapter.select(this) { adapter ->
                configureConsumer(this, adapter)
            }
            addRootTasks(rootProject)
        }
    }

    private fun configureConsumer(project: Project, adapter: ConsumerAdapter) {
        project.logger.info("Arkive: configuring ${adapter.flavor} consumer")

        project.extensions.findByType(ArkiveExtension::class.java)
            ?.multiModuleVariant
            ?.convention(adapter.defaultMultiModuleVariant)

        applyPaparazzi(project)
        adapter.wireDependencies()
        adapter.onVariants { variant ->
            addTaskWithVariant(project, adapter, variant)
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

    private fun addExtension(project: Project) {
        val extension = project.extensions.create(
            "arkive",
            ArkiveExtension::class.java,
        )
        // Wired here, right after creation, so the provider can capture the extension's
        // property (never the Project) regardless of the consumer's plugin order.
        forwardRetentionToTests(project, extension)

        project.afterEvaluate {
            val enablePreviewParameters = extension.enablePreviewParameters.get()
            val enableVariants = extension.enableVariants.get()

            project.extensions.findByName("ksp")?.let { kspExt ->
                try {
                    val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                    argMethod.invoke(kspExt, ENABLE_PREVIEW_PARAMETERS, enablePreviewParameters.toString())
                } catch (e: Exception) {
                    project.logger.warn("Failed to pass $ENABLE_PREVIEW_PARAMETERS to KSP: ${e.message}")
                }

                try {
                    val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                    argMethod.invoke(kspExt, ENABLE_VARIANTS, enableVariants.toString())
                } catch (e: Exception) {
                    project.logger.warn("Failed to pass enableVariants to KSP: ${e.message}")
                }
            }
        }
    }

    private fun applyPaparazzi(project: Project) {
        if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
            // Apply by id (not class reference) so Paparazzi need not be on the compile
            // classpath — it can stay an `implementation` dep, hidden from consumers.
            project.pluginManager.apply("app.cash.paparazzi")
        }
    }

    private fun addTaskWithVariant(project: Project, adapter: ConsumerAdapter, variant: String) {
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
                val extension = project.extensions.findByType(ArkiveExtension::class.java)
                task.designFileKey = extension?.designFileKey?.get().orEmpty()
                task.snapshotRetention = snapshotRetentionOf(project).name
                if (variant.isEmpty()) {
                    task.dependsOn(RECORDING_TASK)
                } else {
                    task.dependsOn("$RECORDING_TASK${variant.capFirst}")
                }
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
        // The generated test class only exists where the test-source KSP ran — debug
        // build types on android modules, the single host-test compilation on KMP — so a
        // verify for any other variant could only fail with an opaque "No tests found".
        if (variant.isEmpty() ||
            variant == "debug" ||
            variant.endsWith("Debug") ||
            variant == adapter.defaultMultiModuleVariant
        ) {
            addVerifyTaskWithVariant(project, adapter, variant)
        }
    }

    private fun showcaseTaskName(variant: String) = "${GenerateShowcaseTask.NAME}${variant.capFirst}"

    private fun snapshotRetentionOf(project: Project): SnapshotRetention =
        project.extensions.findByType(ArkiveExtension::class.java)
            ?.snapshotRetention?.get() ?: SnapshotRetention.NONE

    /**
     * `verifyShowcase<Variant>` is the public verify entry point: it runs Paparazzi's
     * verify scoped to Arkive's generated test class only, so the consumer's own Paparazzi
     * tests and goldens are never pulled into an Arkive verification (mirroring the
     * boundary SnapshotsGrabber keeps on the golden directory).
     */
    private fun addVerifyTaskWithVariant(project: Project, adapter: ConsumerAdapter, variant: String) {
        val verifyTaskName = "$VERIFY_SHOWCASE_TASK${variant.capFirst}"
        project.tasks.register(verifyTaskName) { task ->
            task.group = GenerateShowcaseTask.GROUP
            task.description = "Verifies Arkive snapshots against the retained goldens"
            if (variant.isEmpty()) {
                task.dependsOn(VERIFYING_TASK)
            } else {
                task.dependsOn("$VERIFYING_TASK${variant.capFirst}")
            }
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
                failOnConflictingTasks(project, graph, verifyTaskName, variant)
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
    ) {
        // The variant's own unit-test task can't be listed here — it is always in the
        // graph as verifyPaparazzi's dependency, so a direct request for it is
        // indistinguishable. The aggregates below catch the common combinations.
        val conflicting = listOfNotNull(
            project.tasks.findByName("check"),
            project.tasks.findByName("build"),
            project.tasks.findByName("test"),
            project.tasks.findByName(showcaseTaskName(variant)),
            project.tasks.findByName("$RECORDING_TASK${variant.capFirst}"),
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
                    task.dependsOn(moduleShowcaseTaskPaths(rootProject))
                    task.setSource(rootProject.projectDir)
                }
            }
        }
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
        private const val VERIFYING_TASK = "verifyPaparazzi"
        private const val GENERATED_TEST_CLASS = "com.infinum.arkive.ArkiveSnapshotTestGenerator"
    }
}
