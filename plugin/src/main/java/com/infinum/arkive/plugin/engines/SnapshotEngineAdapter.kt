package com.infinum.arkive.plugin.engines

import com.infinum.arkive.plugin.consumers.ConsumerAdapter
import com.infinum.arkive.plugin.consumers.addDependencyWhenConfigurationExists
import com.infinum.arkive.plugin.utils.capFirst
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Everything about a snapshot rendering engine that the rest of the plugin needs:
 * which Gradle plugin to apply, and the record/verify task names Arkive's tasks hook
 * into. The engine also names itself into a KSP arg so the test processor generates
 * the matching test class.
 *
 * Selection happens eagerly at plugin apply time from the `arkive.engine` Gradle
 * property (module or root `gradle.properties`, or `-Parkive.engine=`) — a property,
 * NOT the `arkive {}` DSL, for two reasons:
 * - The engine's Gradle plugin must be applied while AGP's variant API is still open;
 *   extension values are only readable at afterEvaluate, which is too late.
 * - Only the chosen engine's classes ever load. Paparazzi ships Java 21 bytecode, so a
 *   consumer on a JDK 17 Gradle daemon can use Arkive as long as Paparazzi's jar stays
 *   an unloaded classpath entry — property-based selection guarantees that.
 */
internal interface SnapshotEngineAdapter {

    /** Property value, KSP arg value, and log-friendly name of the engine. */
    val engineName: String

    /** Id of the engine's Gradle plugin, applied by id so it stays off compile classpaths. */
    val pluginId: String

    /** The engine's snapshot-recording task for [variant] on [adapter]'s flavor. */
    fun recordTaskName(adapter: ConsumerAdapter, variant: String): String

    /** The engine's golden-verification task for [variant] on [adapter]'s flavor. */
    fun verifyTaskName(adapter: ConsumerAdapter, variant: String): String

    fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin(pluginId)) {
            project.pluginManager.apply(pluginId)
        }
    }

    /**
     * Injects whatever the engine needs on the unit-test classpath.
     * [testImplementationConfigurationName] comes from the ConsumerAdapter, since the
     * test source set differs per consumer flavor.
     */
    fun wireTestDependencies(project: Project, testImplementationConfigurationName: String) {
        // Paparazzi's plugin injects its own runtime; nothing to do by default.
    }

    /**
     * Engine-specific test-task configuration. [snapshotsDirPath] is the adapter's golden
     * directory relative to the module dir. Both engines default the test heap: Gradle's
     * 512m worker default dies once real apps render hundreds of snapshots in one JVM
     * (proven for both layoutlib and Robolectric on EdgePOS). Consumers who set their
     * own maxHeapSize keep it.
     */
    fun configureTestTasks(project: Project, snapshotsDirPath: String) {
        project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach { test ->
            if (test.maxHeapSize == null) {
                test.maxHeapSize = DEFAULT_TEST_HEAP
            }
        }
    }

    companion object {
        const val ENGINE_PROPERTY = "arkive.engine"
        const val ENGINE_KSP_ARG = "arkive.engine"
        const val DEFAULT_TEST_HEAP = "2g"

        /**
         * The engine forced via the `arkive.engine` Gradle property, or null when unset.
         * The property outranks the DSL: it lets a consumer flip engines per-run
         * (`-Parkive.engine=`) or pin one org-wide from a root gradle.properties.
         */
        fun fromGradleProperty(project: Project): SnapshotEngineAdapter? {
            val requested = (project.findProperty(ENGINE_PROPERTY) as? String)?.trim()?.lowercase()
            return when (requested) {
                null, "" -> null
                else -> fromName(requested)
            }
        }

        fun fromName(name: String): SnapshotEngineAdapter = when (name) {
            RoborazziEngine.NAME -> RoborazziEngine
            PaparazziEngine.NAME -> PaparazziEngine
            else -> throw GradleException(
                "Arkive: unknown engine '$name' — " +
                    "supported engines: ${RoborazziEngine.NAME}, ${PaparazziEngine.NAME}.",
            )
        }
    }
}

/**
 * Paparazzi/layoutlib: renders exactly what Android Studio previews render. Requires the
 * consumer's Gradle JDK to be 21+ (Paparazzi 2.0.0-alpha03+ is Java 21 bytecode).
 */
internal object PaparazziEngine : SnapshotEngineAdapter {
    const val NAME = "paparazzi"

    override val engineName = NAME
    override val pluginId = "app.cash.paparazzi"

    override fun recordTaskName(adapter: ConsumerAdapter, variant: String) =
        "recordPaparazzi${variant.capFirst}"

    override fun verifyTaskName(adapter: ConsumerAdapter, variant: String) =
        "verifyPaparazzi${variant.capFirst}"
}

/**
 * Roborazzi/Robolectric: renders with the real framework code on the JVM.
 * Works on a JDK 17 Gradle daemon, and — unlike layoutlib — provides a real Android
 * application context, so Compose Multiplatform resources load. The generated test pins
 * `@Config(sdk = 35)` (Robolectric's android-all jars for SDK 36 require a Java 21
 * test JVM — 35 keeps the whole stack 17-compatible).
 */
internal object RoborazziEngine : SnapshotEngineAdapter {
    const val NAME = "roborazzi"

    private const val ROBORAZZI_VERSION = "1.72.0"
    private const val ROBOLECTRIC_DEPENDENCY = "org.robolectric:robolectric:4.15.1"

    // Compile floor for the compose test host; consumers on newer Compose win by
    // ordinary Gradle conflict resolution. roborazzi-compose does not carry it
    // transitively (it is compileOnly upstream).
    private const val UI_TEST_JUNIT4_DEPENDENCY = "androidx.compose.ui:ui-test-junit4:1.8.2"

    override val engineName = NAME
    override val pluginId = "io.github.takahirom.roborazzi"

    override fun recordTaskName(adapter: ConsumerAdapter, variant: String) =
        "recordRoborazzi${adapter.roborazziTaskSuffix(variant)}"

    override fun verifyTaskName(adapter: ConsumerAdapter, variant: String) =
        "verifyRoborazzi${adapter.roborazziTaskSuffix(variant)}"

    override fun apply(project: Project) {
        super.apply(project)
        enableUnitTestAndroidResources(project)
    }

    override fun wireTestDependencies(project: Project, testImplementationConfigurationName: String) {
        listOf(
            "io.github.takahirom.roborazzi:roborazzi:$ROBORAZZI_VERSION",
            "io.github.takahirom.roborazzi:roborazzi-compose:$ROBORAZZI_VERSION",
            ROBOLECTRIC_DEPENDENCY,
            UI_TEST_JUNIT4_DEPENDENCY,
        ).forEach { notation ->
            project.addDependencyWhenConfigurationExists(testImplementationConfigurationName, notation)
        }
    }

    override fun configureTestTasks(project: Project, snapshotsDirPath: String) {
        super.configureTestTasks(project, snapshotsDirPath)
        // The generated test records/verifies via explicit file paths in the golden
        // directory, which Roborazzi's own up-to-date tracking knows nothing about.
        // Without declaring it, editing (or corrupting) a golden leaves the test task
        // UP-TO-DATE — and a verify run silently "passes" without comparing anything.
        val goldens = project.projectDir.resolve(snapshotsDirPath)
        project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach { test ->
            test.inputs.files(project.fileTree(goldens))
                .withPropertyName("arkiveGoldens")
                .optional(true)
        }
    }

    /**
     * Robolectric needs android resources (and CMP's composeResources assets) on the
     * unit-test classpath. On the `android {}` DSL flavors Arkive can flip the flag
     * itself; the AGP KMP library plugin has no such extension here — its consumers set
     * `withHostTestBuilder {}.configure { isIncludeAndroidResources = true }`, which the
     * README already requires. Reflection keeps this tolerant of AGP DSL drift.
     */
    private fun enableUnitTestAndroidResources(project: Project) {
        runCatching {
            val android = project.extensions.findByName("android") ?: return
            val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
            val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
            unitTests.javaClass
                .getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType)
                .invoke(unitTests, true)
        }.onFailure { failure ->
            project.logger.warn(
                "Arkive: could not enable includeAndroidResources for unit tests " +
                    "(${failure.message}). Roborazzi rendering needs it — set " +
                    "`android { testOptions { unitTests { isIncludeAndroidResources = true } } }` manually.",
            )
        }
    }
}
