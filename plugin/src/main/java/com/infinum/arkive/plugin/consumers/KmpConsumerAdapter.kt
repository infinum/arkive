package com.infinum.arkive.plugin.consumers

import com.infinum.arkive.plugin.utils.ArkiveVersion
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * Adapter for KMP modules using the `com.android.kotlin.multiplatform.library` plugin:
 * a single android compilation (no build types/flavors — the one Paparazzi variant is
 * "androidMain"), target-prefixed KSP configurations, and unit tests living in the
 * `androidHostTest` source set.
 *
 * The legacy KMP setup (`com.android.library` + `androidTarget()`) is NOT handled here —
 * it matches [AndroidConsumerAdapter], where Arkive cannot work (see the warning in
 * [ConsumerAdapter.select]).
 */
internal class KmpConsumerAdapter(
    private val project: Project,
) : ConsumerAdapter {

    override val flavor = "kotlin multiplatform"

    // The single android variant; keeps root aggregation (generateWebShowcase) working
    // without the consumer having to configure multiModuleVariant at all.
    override val defaultMultiModuleVariant = ANDROID_VARIANT

    override val testImplementationConfigurationName = "androidHostTestImplementation"

    init {
        enableAndroidResources()
        requireKspPlugin()
    }

    override fun onVariants(action: (String) -> Unit) {
        action(ANDROID_VARIANT)
    }

    override fun wireDependencies() {
        val arkiveVersion = ArkiveVersion.current
        with(project) {
            // annotations is multiplatform — commonMain when the consumer's Kotlin can
            // read our klibs, androidMain otherwise (see wireAnnotationsWhereConsumable).
            wireAnnotationsWhereConsumable("com.infinum.arkive:annotations:$arkiveVersion")
            // Everything else only ever runs in the android compilation.
            addDependencyWhenConfigurationExists(
                "androidMainImplementation",
                "com.infinum.arkive:composeUtils:$arkiveVersion",
            )
            addDependencyWhenConfigurationExists(
                "kspAndroid",
                "com.infinum.arkive:processor:$arkiveVersion",
            )
            addDependencyWhenConfigurationExists(
                "kspAndroidHostTest",
                "com.infinum.arkive:testprocessor:$arkiveVersion",
            )
            // Test-runtime deps aren't resolution-timing-sensitive the way KSP configs
            // are; deferring to afterEvaluate lets the consumer's own junit win.
            afterEvaluate {
                if (configurations.findByName(testImplementationConfigurationName) != null) {
                    addDependencyIfMissing(testImplementationConfigurationName, JUNIT_DEPENDENCY)
                }
                if (configurations.findByName("androidHostTestRuntimeOnly") != null) {
                    addDependencyIfMissing("androidHostTestRuntimeOnly", JUNIT_VINTAGE_DEPENDENCY)
                }
            }
        }
    }

    override fun kspResourcesPath(variant: String): String =
        "generated${File.separator}ksp${File.separator}android" +
            "${File.separator}$ANDROID_VARIANT${File.separator}resources"

    override fun snapshotsPath(): String =
        "src${File.separator}androidHostTest${File.separator}snapshots"

    override fun unitTestTaskName(variant: String): String = "testAndroidHostTest"

    // Roborazzi names its tasks after the host-test task here, not the variant.
    override fun roborazziTaskSuffix(variant: String): String = "AndroidHostTest"

    /**
     * Paparazzi needs the consumer's `R` class, and the KMP library plugin does not
     * generate one unless android resources are enabled. Without it every snapshot dies
     * on `NoClassDefFoundError: <namespace>.R` — an error the resilient recording
     * swallows, leaving a silently empty showcase. Reflection keeps this tolerant of
     * AGP DSL drift; on failure the consumer gets told what to set by hand.
     */
    private fun enableAndroidResources() {
        runCatching {
            val kotlinExtension = project.extensions.getByName("kotlin") as ExtensionAware
            val androidLibrary = kotlinExtension.extensions.getByName("androidLibrary")
            val androidResources = androidLibrary.javaClass
                .getMethod("getAndroidResources")
                .invoke(androidLibrary)
            val enabled = androidResources.javaClass
                .getMethod("getEnable")
                .invoke(androidResources) as? Boolean

            if (enabled != true) {
                androidResources.javaClass
                    .getMethod("setEnable", Boolean::class.javaPrimitiveType)
                    .invoke(androidResources, true)
                project.logger.info(
                    "Arkive: enabled androidResources on the KMP android library — " +
                        "snapshot rendering needs the module's R class.",
                )
            }
        }.onFailure { failure ->
            project.logger.warn(
                "Arkive: could not enable androidResources on the KMP android library " +
                    "(${failure.message}). Snapshot rendering needs the module's R class — " +
                    "set `kotlin { androidLibrary { androidResources { enable = true } } }` manually.",
            )
        }
    }

    /**
     * On plain Android modules a missing KSP plugin fails fast (the `kspDebug`
     * configuration doesn't exist). The deferred KMP wiring would silently skip
     * instead — so check explicitly.
     */
    private fun requireKspPlugin() {
        project.afterEvaluate {
            if (!project.pluginManager.hasPlugin(KSP_PLUGIN_ID)) {
                throw GradleException(
                    "Arkive requires the KSP plugin on KMP modules — apply " +
                        "'$KSP_PLUGIN_ID' next to the arkive plugin.",
                )
            }
        }
    }

    companion object {
        private const val ANDROID_VARIANT = "androidMain"
        private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"
    }
}
