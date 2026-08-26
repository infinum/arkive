package com.infinum.arkive.plugin.consumers

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.utils.capFirst
import java.io.File
import org.gradle.api.Project

/**
 * Adapter for plain Android modules (`com.android.application` / `com.android.library`):
 * build-type/flavor variants, `ksp<Variant>` configurations, `src/test` unit tests.
 */
internal class AndroidConsumerAdapter(
    private val project: Project,
    private val kind: Kind,
) : ConsumerAdapter {

    internal enum class Kind { APPLICATION, LIBRARY }

    override val flavor = "android ${kind.name.lowercase()}"

    // "debug" exists on every unflavored module, so the root aggregate works with no
    // configuration there; flavored modules have no bare "debug" variant and must set
    // multiModuleVariant explicitly (the setup skill enforces this).
    override val defaultMultiModuleVariant = "debug"

    override val testImplementationConfigurationName = "testImplementation"

    override fun onVariants(action: (String) -> Unit) {
        when (kind) {
            Kind.APPLICATION ->
                project.extensions.getByType(AndroidComponentsExtension::class.java)
                    .onVariants { variant -> action(variant.name) }

            Kind.LIBRARY ->
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
                    .onVariants { variant -> action(variant.name) }
        }
    }

    override fun wireDependencies() {
        val arkiveVersion = ArkiveVersion.current
        with(project.dependencies) {
            add("implementation", "com.infinum.arkive:annotations:$arkiveVersion")
            add("implementation", "com.infinum.arkive:composeUtils:$arkiveVersion")
            add("kspDebug", "com.infinum.arkive:processor:$arkiveVersion")
            add("kspTestDebug", "com.infinum.arkive:testprocessor:$arkiveVersion")
        }
        project.addDependencyIfMissing("testImplementation", JUNIT_DEPENDENCY)
        project.addDependencyIfMissing("testRuntimeOnly", JUNIT_VINTAGE_DEPENDENCY)
    }

    override fun kspResourcesPath(variant: String): String {
        val variantSegment = "${File.separator}$variant".takeIf { variant.isNotEmpty() }.orEmpty()
        return "generated${File.separator}ksp$variantSegment${File.separator}resources"
    }

    override fun snapshotsPath(): String =
        "src${File.separator}test${File.separator}snapshots"

    override fun unitTestTaskName(variant: String): String =
        if (variant.isEmpty()) "test" else "test${variant.capFirst}UnitTest"
}
