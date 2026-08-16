package com.infinum.arkive.plugin.consumers

import com.infinum.arkive.plugin.utils.ArkiveVersion
import org.gradle.api.Project

internal const val JUNIT_DEPENDENCY = "junit:junit:4.13.2"
internal const val JUNIT_VINTAGE_DEPENDENCY = "org.junit.vintage:junit-vintage-engine:5.9.1"

/**
 * Wires the multiplatform annotations into commonMain when the consumer's Kotlin can
 * actually read them, androidMain otherwise. Native/js klibs are not forward-compatible:
 * merely having the annotations on the commonMain classpath breaks every non-JVM
 * compilation of a consumer whose Kotlin is older than the Kotlin the annotations were
 * built with — even if the annotations are never used. The JVM artifact carries a
 * Kotlin 2.0 language floor, so androidMain is always safe. Plain `@Preview`s in
 * commonMain need no Arkive dependency and are collected either way.
 *
 * Deferred to afterEvaluate so the consumer's Kotlin version is reliably readable.
 */
internal fun Project.wireAnnotationsWhereConsumable(notation: String) {
    afterEvaluate {
        val consumerKotlin = consumerKotlinVersion()
        val requiredKotlin = ArkiveVersion.builtWithKotlin
        val configurationName =
            if (consumerKotlin != null && versionAtLeast(consumerKotlin, requiredKotlin)) {
                "commonMainImplementation"
            } else {
                logger.warn(
                    "Arkive: this project's Kotlin ($consumerKotlin) is older than the Kotlin " +
                        "Arkive's annotations were built with ($requiredKotlin), and klibs are not " +
                        "forward-compatible — @ArkiveComposable is available in androidMain only. " +
                        "Previews in commonMain are still collected via plain @Preview.",
                )
                "androidMainImplementation"
            }
        if (configurations.findByName(configurationName) != null) {
            addDependencyIfMissing(configurationName, notation)
        }
    }
}

/** The consumer's Kotlin Gradle plugin version, read reflectively off its classloader. */
private fun Project.consumerKotlinVersion(): String? = runCatching {
    val kotlinExtension = extensions.findByName("kotlin") ?: return null
    val wrapper = kotlinExtension.javaClass.classLoader
        .loadClass("org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt")
    wrapper.getMethod("getKotlinPluginVersion", Project::class.java)
        .invoke(null, this) as? String
}.getOrNull()

private fun versionAtLeast(actual: String, required: String): Boolean {
    fun parse(version: String) = version.substringBefore("-")
        .split(".")
        .mapNotNull { it.toIntOrNull() }
    val actualParts = parse(actual)
    val requiredParts = parse(required)
    repeat(maxOf(actualParts.size, requiredParts.size)) { index ->
        val diff = (actualParts.getOrElse(index) { 0 }) - (requiredParts.getOrElse(index) { 0 })
        if (diff != 0) {
            return diff > 0
        }
    }
    return true
}

/**
 * Adds [notation] to the (already existing) configuration unless the consumer declared
 * the same group:name themselves.
 */
internal fun Project.addDependencyIfMissing(configurationName: String, notation: String) {
    val configuration = configurations.getByName(configurationName)
    val group = notation.substringBefore(":")
    val name = notation.substringAfter(":").substringBefore(":")

    val dependencyExists = configuration.dependencies.any { dependency ->
        dependency.group == group && dependency.name == name
    }

    if (!dependencyExists) {
        dependencies.add(configurationName, notation)
    }
}

/**
 * Adds [notation] to [configurationName] the moment that configuration is created — KMP
 * creates its configurations while the consumer's `kotlin { }` block evaluates, which is
 * after this plugin applies, so eager wiring cannot see them. Adding at creation (rather
 * than via `withDependencies`) matters: KSP resolves the derived
 * `*ProcessorClasspath` configurations, whose extendsFrom edge does not fire the
 * parent's `withDependencies` hooks.
 */
internal fun Project.addDependencyWhenConfigurationExists(configurationName: String, notation: String) {
    configurations.matching { it.name == configurationName }.all { configuration ->
        configuration.dependencies.add(dependencies.create(notation))
    }
}
