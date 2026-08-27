package com.infinum.arkive.plugin.consumers

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.utils.capFirst
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Adapter for legacy KMP modules: `org.jetbrains.kotlin.multiplatform` +
 * `com.android.library`/`com.android.application` with `androidTarget()`. This is where
 * most KMP projects live today (AGP 9 gates it behind `android.newDsl=false`; AGP 10
 * removes it — [KmpConsumerAdapter] covers the replacement plugin).
 *
 * Android build variants exist here exactly as on a plain Android module, but everything
 * KMP is target-prefixed: KSP configurations are `kspAndroid<Variant>`, KSP output gains
 * an extra `android/` segment with an `android`-prefixed compilation name, and unit
 * tests live in the `androidUnitTest` source set.
 */
internal class LegacyKmpConsumerAdapter(
    private val project: Project,
    private val kind: AndroidConsumerAdapter.Kind,
) : ConsumerAdapter {

    override val flavor = "kotlin multiplatform (legacy androidTarget, ${kind.name.lowercase()})"

    // Legacy KMP library modules practically always have plain debug/release build
    // types, so defaulting root aggregation to debug keeps them zero-config; flavored
    // modules override via the extension, same as plain Android.
    override val defaultMultiModuleVariant = "debug"

    override val testImplementationConfigurationName = "androidUnitTestImplementation"

    init {
        requireKspPlugin()
    }

    override fun onVariants(action: (String) -> Unit) {
        when (kind) {
            AndroidConsumerAdapter.Kind.APPLICATION ->
                project.extensions.getByType(AndroidComponentsExtension::class.java)
                    .onVariants { variant -> action(variant.name) }

            AndroidConsumerAdapter.Kind.LIBRARY ->
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
                    .onVariants { variant -> action(variant.name) }
        }
    }

    override fun wireDependencies() {
        val arkiveVersion = ArkiveVersion.current
        with(project) {
            // annotations is multiplatform — commonMain when the consumer's Kotlin can
            // read our klibs, androidMain otherwise (see wireAnnotationsWhereConsumable).
            wireAnnotationsWhereConsumable("com.infinum.arkive:annotations:$arkiveVersion")
            // Everything else only ever runs in the android debug compilation. The KSP
            // configurations are created once the android target exists, so wiring is
            // deferred to configuration creation (same reasoning as KmpConsumerAdapter).
            addDependencyWhenConfigurationExists(
                "androidMainImplementation",
                "com.infinum.arkive:composeUtils:$arkiveVersion",
            )
            addDependencyWhenConfigurationExists(
                "kspAndroidDebug",
                "com.infinum.arkive:processor:$arkiveVersion",
            )
            addDependencyWhenConfigurationExists(
                "kspAndroidTestDebug",
                "com.infinum.arkive:testprocessor:$arkiveVersion",
            )
            afterEvaluate {
                if (configurations.findByName(testImplementationConfigurationName) != null) {
                    addDependencyIfMissing(testImplementationConfigurationName, JUNIT_DEPENDENCY)
                }
                if (configurations.findByName("androidUnitTestRuntimeOnly") != null) {
                    addDependencyIfMissing("androidUnitTestRuntimeOnly", JUNIT_VINTAGE_DEPENDENCY)
                }
            }
        }
    }

    override fun kspResourcesPath(variant: String): String =
        "generated${File.separator}ksp${File.separator}android" +
            "${File.separator}android${variant.capFirst}${File.separator}resources"

    override fun snapshotsPath(): String =
        "src${File.separator}androidUnitTest${File.separator}snapshots"

    override fun unitTestTaskName(variant: String): String =
        if (variant.isEmpty()) "test" else "test${variant.capFirst}UnitTest"

    /** Deferred wiring silently skips when KSP is absent — check explicitly. */
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
        private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"
    }
}
