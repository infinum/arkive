package com.infinum.arkive.plugin.consumers

import com.android.build.api.variant.AndroidComponentsExtension
import com.infinum.arkive.plugin.utils.capFirst
import org.gradle.api.Project

/**
 * Everything about a consumer module that depends on which Android plugin flavor the
 * module uses. Exactly one adapter is selected per module (see [select]); all
 * flavor-specific knowledge — configuration names, generated-code paths, task names —
 * lives behind this interface, so the rest of the plugin never branches on project type.
 */
internal interface ConsumerAdapter {

    /** Log-friendly name of the consumer flavor. */
    val flavor: String

    /** Default `arkive.multiModuleVariant` when the consumer configures none. */
    val defaultMultiModuleVariant: String

    /** The `implementation` configuration of the unit-test source set (engine test deps go here). */
    val testImplementationConfigurationName: String

    /** Invokes [action] for every variant Arkive should showcase, as variants become known. */
    fun onVariants(action: (String) -> Unit)

    /** Wires the runtime, processor, and test dependencies into the right configurations. */
    fun wireDependencies()

    /** KSP-generated resources directory for [variant], relative to the module build dir. */
    fun kspResourcesPath(variant: String): String

    /** Paparazzi's golden directory, relative to the module directory. */
    fun snapshotsPath(): String

    /** The unit-test task the engine's verify runs through for [variant]. */
    fun unitTestTaskName(variant: String): String

    /**
     * Suffix of the Roborazzi plugin's per-variant tasks. On `android {}` flavors they
     * follow the build variant (`recordRoborazziUatDebug`); on the AGP KMP library plugin
     * Roborazzi names them after the test task instead (`recordRoborazziAndroidHostTest`).
     */
    fun roborazziTaskSuffix(variant: String): String = variant.capFirst

    companion object {
        private const val ANDROID_APPLICATION_PLUGIN_ID = "com.android.application"
        private const val ANDROID_LIBRARY_PLUGIN_ID = "com.android.library"
        private const val ANDROID_KMP_LIBRARY_PLUGIN_ID = "com.android.kotlin.multiplatform.library"
        private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"

        /**
         * Picks the adapter matching the consumer's plugin combination, deferring until
         * the plugins are applied (the order of the consumer's plugins block is
         * arbitrary). [onSelected] runs at most once.
         *
         * `com.android.application`/`com.android.library` alone don't identify the
         * flavor — the same android plugin appears in plain Android modules (AGP 9's
         * built-in Kotlin, `kotlin-android`, or java-only) and in legacy KMP modules (with
         * `kotlin.multiplatform` + `androidTarget()`), and those need different wiring.
         * So the android plugin decides in AGP's `finalizeDsl`, once the whole plugins
         * block is applied: `kotlin.multiplatform` → legacy KMP, anything else → plain.
         * One decision point keeps every consumer on the path the samples exercise.
         * Never `afterEvaluate`: AGP locks `onVariants` in its own afterEvaluate,
         * registered before any `withPlugin(android)` hook runs. Whatever runs from here
         * (including an engine plugin applied via the `arkive.engine` property) can no
         * longer register a `finalizeDsl` of its own.
         */
        fun select(project: Project, onSelected: (ConsumerAdapter) -> Unit) {
            val chooser = AdapterChooser(onSelected)

            project.pluginManager.withPlugin(ANDROID_KMP_LIBRARY_PLUGIN_ID) {
                chooser.choose(KmpConsumerAdapter(project))
            }
            project.pluginManager.withPlugin(ANDROID_APPLICATION_PLUGIN_ID) {
                chooseByKotlinFlavor(project, chooser, AndroidConsumerAdapter.Kind.APPLICATION)
            }
            project.pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN_ID) {
                chooseByKotlinFlavor(project, chooser, AndroidConsumerAdapter.Kind.LIBRARY)
            }
        }

        private fun chooseByKotlinFlavor(
            project: Project,
            chooser: AdapterChooser,
            kind: AndroidConsumerAdapter.Kind,
        ) {
            project.extensions.getByType(AndroidComponentsExtension::class.java).finalizeDsl {
                val adapter = if (project.pluginManager.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
                    LegacyKmpConsumerAdapter(project, kind)
                } else {
                    AndroidConsumerAdapter(project, kind)
                }
                chooser.choose(adapter)
            }
        }
    }

    private class AdapterChooser(
        private val onSelected: (ConsumerAdapter) -> Unit,
    ) {
        private var selected = false

        fun choose(adapter: ConsumerAdapter) {
            if (!selected) {
                selected = true
                onSelected(adapter)
            }
        }
    }
}
