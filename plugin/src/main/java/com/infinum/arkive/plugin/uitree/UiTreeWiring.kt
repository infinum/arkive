package com.infinum.arkive.plugin.uitree

import com.infinum.arkive.plugin.consumers.ConsumerAdapter
import com.infinum.arkive.plugin.consumers.addDependencyWhenConfigurationExists
import com.infinum.arkive.plugin.utils.UiTreeArgumentProvider
import java.io.File
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

internal object UiTreeWiring {

    /** Read by composeUtils' UiTreeFiles — keep the names in sync. */
    const val DIRECTORY_PROPERTY = "arkive.uitree.dir"

    // ui-tooling-data is compileOnly in composeUtils; the walker needs it at test runtime only.
    // Pinned to the version composeUtils compiles against (its Compose BOM). androidx aligns
    // the artifacts of a group, so a consumer on newer Compose resolves it at their version.
    private const val UI_TOOLING_DATA_DEPENDENCY = "androidx.compose.ui:ui-tooling-data:1.8.3"

    /** Build-dir-relative directory the test JVM writes [variant]'s trees into. */
    fun relativeDirectory(variant: String): String =
        "arkive${File.separator}uitree${File.separator}${variant.ifEmpty { "default" }}"

    fun wireDependencies(project: Project, adapter: ConsumerAdapter) {
        project.addDependencyWhenConfigurationExists(
            adapter.testImplementationConfigurationName,
            UI_TOOLING_DATA_DEPENDENCY,
        )
    }

    /**
     * Forwards the tree directory to [variant]'s unit-test task and declares it as an output
     * of that task — outside the golden directory, so engine record/verify and
     * snapshotRetention never see it, and a cached test run restores the trees with it.
     */
    fun forwardToTests(project: Project, adapter: ConsumerAdapter, variant: String, enabled: Provider<Boolean>) {
        val directory = project.layout.buildDirectory.dir(relativeDirectory(variant))
        val testTaskName = adapter.unitTestTaskName(variant)
        project.tasks.withType(Test::class.java).named { it == testTaskName }.configureEach { test ->
            test.jvmArgumentProviders.add(UiTreeArgumentProvider(enabled, directory.get().asFile.absolutePath))
            test.outputs.dir(directory).withPropertyName("arkiveUiTree").optional(true)
        }
    }
}
