package com.infinum.arkive.plugin.consumers

import org.gradle.api.Project

internal const val JUNIT_DEPENDENCY = "junit:junit:4.13.2"
internal const val JUNIT_VINTAGE_DEPENDENCY = "org.junit.vintage:junit-vintage-engine:5.9.1"

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
