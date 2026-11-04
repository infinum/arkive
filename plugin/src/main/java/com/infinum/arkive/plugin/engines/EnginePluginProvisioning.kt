package com.infinum.arkive.plugin.engines

import java.io.File
import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.UnknownPluginException
import org.gradle.internal.classpath.CachedClasspathTransformer
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.internal.classpath.transforms.InstrumentingClassTransform
import org.gradle.internal.service.ServiceLookupException
import org.gradle.util.GradleVersion

/**
 * Applies the selected engine's Gradle plugin to a consumer module WITHOUT Arkive carrying
 * it on its own classpath. A plugin's runtime dependencies land on the consumer's build
 * classpath along with their plugin descriptors, so a consumer declaring its own
 * Roborazzi/Paparazzi either had its version silently replaced by Arkive's (same
 * classloader scope) or failed plugin resolution with "already on the classpath with an
 * unknown version" (Arkive in a parent scope, e.g. the root build).
 *
 * In order of preference:
 * 1. the module already applied the engine plugin → left alone;
 * 2. the plugin is on the module's build classpath (`apply false` in the root build, a
 *    convention plugin, a buildscript classpath entry) → applied by id — the consumer's
 *    version wins;
 * 3. neither → Arkive resolves the plugin at the version it is tested against and applies
 *    it from a child of the module's own classloader scope, so it shares the consumer's
 *    AGP, Kotlin and Gradle classes.
 *
 * Step 3 uses Gradle INTERNALS on purpose, for the two things Gradle only does for
 * classpaths it loads itself: a plain `URLClassLoader` fails task validation ("was loaded
 * with an unknown classloader" — Gradle can't fingerprint the engine's test-task action),
 * and uninstrumented jars break the configuration cache (lambdas it can't restore). So the
 * jars go through Gradle's build-logic instrumentation and load in a `ClassLoaderScope`.
 * Internal API can change in any Gradle release; a mismatch surfaces as a [LinkageError]
 * or a missing service, turned into an error telling the consumer to declare the plugin
 * themselves (step 2), which needs no internals.
 */
internal object EnginePluginProvisioning {

    // Floors for vulnerable transitives of the provisioned plugins: Paparazzi pins
    // com.android.tools:sdk-common 31.13.2, which drags these in. Keep in sync with the
    // securityFloors map in the repo's root build.gradle.kts.
    private val securityFloors = listOf(
        "org.bouncycastle:bcprov-jdk18on:1.86" to
            "CVE-2026-8763, CVE-2026-13506 (<1.85), CVE-2026-0636 (<1.84), CVE-2025-14813 (<=1.80.1)",
        "org.bouncycastle:bcpkix-jdk18on:1.86" to "CVE-2026-5588 (<1.84)",
        "org.bouncycastle:bcutil-jdk18on:1.86" to "Bouncy Castle modules must resolve as a set",
        "org.apache.commons:commons-compress:1.27.1" to "CVE-2024-25710, CVE-2024-26308 (<1.26.0)",
        "org.apache.httpcomponents:httpclient:4.5.14" to "CVE-2020-13956 (<4.5.13)",
        "com.google.guava:guava:33.7.2-jre" to
            "CompactHashMap/CompactHashSet/MapMakerInternalMap deserialization DoS (<=33.7.1)",
    )

    fun apply(project: Project, engine: SnapshotEngineAdapter) {
        when {
            project.pluginManager.hasPlugin(engine.pluginId) ->
                project.logger.info("Arkive: using the ${engine.pluginId} plugin this module already applies")

            applyFromBuildClasspath(project, engine.pluginId) ->
                project.logger.info("Arkive: applied ${engine.pluginId} from the build classpath")

            else -> {
                project.logger.info("Arkive: providing ${engine.pluginCoordinates} (not on the build classpath)")
                project.pluginManager.apply(provisionedPluginClass(project, engine))
            }
        }
    }

    private fun applyFromBuildClasspath(project: Project, pluginId: String): Boolean =
        try {
            project.pluginManager.apply(pluginId)
            true
        } catch (expected: UnknownPluginException) {
            false
        }

    private fun provisionedPluginClass(project: Project, engine: SnapshotEngineAdapter): Class<*> {
        val loader = scopedClassLoader(project, engine, resolvePluginClasspath(project, engine))
        val implementationClass = loader.getResource("META-INF/gradle-plugins/${engine.pluginId}.properties")
            ?.openStream()
            ?.use { stream -> Properties().apply { load(stream) } }
            ?.getProperty("implementation-class")
            ?: throw GradleException(
                "Arkive: ${engine.pluginCoordinates} has no plugin descriptor for ${engine.pluginId}.",
            )
        return loader.loadClass(implementationClass)
    }

    // A locked child of the module's script scope: Gradle builds (and caches) the loader
    // itself, so it can fingerprint the engine's task actions, and the parent gives the
    // engine the same AGP/KGP classes the module sees. The jars first get the same
    // instrumentation Gradle applies to every build-script classpath — among other things
    // it makes lambdas serializable. Raw jars leave the engine's task lambdas (Roborazzi's
    // onlyIf specs) as hidden classes, which the configuration cache stores by name and
    // then cannot load back.
    private fun scopedClassLoader(project: Project, engine: SnapshotEngineAdapter, classpath: Set<File>): ClassLoader =
        try {
            val projectInternal = project as ProjectInternal
            val instrumented = projectInternal.services.get(CachedClasspathTransformer::class.java)
                .transform(DefaultClassPath.of(classpath), InstrumentingClassTransform())
            projectInternal.classLoaderScope
                .createChild("arkive-${engine.engineName}", null)
                .local(instrumented)
                .lock()
                .localClassLoader
        } catch (e: LinkageError) {
            throw internalApiChanged(engine, e)
        } catch (e: ServiceLookupException) {
            throw internalApiChanged(engine, e)
        }

    private fun internalApiChanged(engine: SnapshotEngineAdapter, cause: Throwable) = GradleException(
        declareItYourself(engine, "this Gradle version changed the internal API Arkive loads it with."),
        cause,
    )

    private fun resolvePluginClasspath(project: Project, engine: SnapshotEngineAdapter): Set<File> {
        val configuration = project.configurations.detachedConfiguration(
            project.dependencies.create(engine.pluginCoordinates),
        )
        requestPluginRuntimeVariant(project, configuration)
        securityFloors.forEach { (notation, reason) ->
            configuration.dependencyConstraints.add(
                project.dependencies.constraints.create(notation) { it.because(reason) },
            )
        }
        return try {
            configuration.incoming.files.files
        } catch (e: GradleException) {
            throw GradleException(
                declareItYourself(engine, "resolving it from this project's repositories failed (they need mavenCentral())."),
                e,
            )
        }
    }

    // The attributes Gradle itself requests when resolving a plugin: the runtime jar
    // variant for this daemon's JVM and Gradle version (Paparazzi publishes Java 21 only).
    private fun requestPluginRuntimeVariant(project: Project, configuration: Configuration) {
        val objects = project.objects
        configuration.attributes { attributes ->
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
            attributes.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.JAR),
            )
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            attributes.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
            )
            attributes.attribute(
                TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                JavaVersion.current().majorVersion.toInt(),
            )
            attributes.attribute(
                GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                objects.named(GradlePluginApiVersion::class.java, GradleVersion.current().version),
            )
        }
    }

    private fun declareItYourself(engine: SnapshotEngineAdapter, reason: String) =
        """
        Arkive could not provide the ${engine.engineName} engine's Gradle plugin: $reason
        Declare the plugin yourself — Arkive then uses yours as-is:

            // root build.gradle.kts
            plugins {
                id("${engine.pluginId}") version "${engine.pluginVersion}" apply false
            }
        """.trimIndent()
}
