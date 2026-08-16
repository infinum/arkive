package com.infinum.arkive.plugin.consumers

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

    /** Invokes [action] for every variant Arkive should showcase, as variants become known. */
    fun onVariants(action: (String) -> Unit)

    /** Wires the runtime, processor, and test dependencies into the right configurations. */
    fun wireDependencies()

    /** KSP-generated resources directory for [variant], relative to the module build dir. */
    fun kspResourcesPath(variant: String): String

    /** Paparazzi's golden directory, relative to the module directory. */
    fun snapshotsPath(): String

    /** The unit-test task Paparazzi's verify runs through for [variant]. */
    fun unitTestTaskName(variant: String): String

    companion object {
        private const val ANDROID_APPLICATION_PLUGIN_ID = "com.android.application"
        private const val ANDROID_LIBRARY_PLUGIN_ID = "com.android.library"
        private const val ANDROID_KMP_LIBRARY_PLUGIN_ID = "com.android.kotlin.multiplatform.library"
        private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"

        /**
         * Picks the adapter matching the consumer's Android plugin, deferring until that
         * plugin is applied (the order of the consumer's plugins block is arbitrary).
         * [onSelected] runs at most once.
         */
        fun select(project: Project, onSelected: (ConsumerAdapter) -> Unit) {
            val chooser = AdapterChooser(onSelected)

            project.pluginManager.withPlugin(ANDROID_KMP_LIBRARY_PLUGIN_ID) {
                chooser.choose(KmpConsumerAdapter(project))
            }
            project.pluginManager.withPlugin(ANDROID_APPLICATION_PLUGIN_ID) {
                chooser.choose(AndroidConsumerAdapter(project, AndroidConsumerAdapter.Kind.APPLICATION))
            }
            project.pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN_ID) {
                chooser.choose(AndroidConsumerAdapter(project, AndroidConsumerAdapter.Kind.LIBRARY))
            }

            project.afterEvaluate {
                warnOnLegacyKmp(project)
            }
        }

        /**
         * The legacy KMP setup (`kotlin.multiplatform` + `com.android.library` +
         * `androidTarget()`) selects the plain android adapter, whose `ksp<Variant>`
         * configurations exist there but feed no compilation — Arkive silently no-ops.
         */
        private fun warnOnLegacyKmp(project: Project) {
            val isLegacyKmp = project.pluginManager.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID) &&
                project.pluginManager.hasPlugin(ANDROID_LIBRARY_PLUGIN_ID)
            if (isLegacyKmp) {
                project.logger.warn(
                    "Arkive: legacy KMP setup detected (kotlin.multiplatform + com.android.library). " +
                        "Arkive's processors cannot reach the android compilation there — migrate the " +
                        "module to the com.android.kotlin.multiplatform.library plugin.",
                )
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
