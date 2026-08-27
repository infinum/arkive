package com.infinum.arkive.plugin.utils

import java.util.Properties

/**
 * The plugin's own version, stamped into `arkive.properties` by `processResources`.
 * Single source of truth is `releaseConfig["version"]` in `config.gradle.kts`.
 */
internal object ArkiveVersion {
    val current: String by lazy {
        val stream = ArkiveVersion::class.java.classLoader.getResourceAsStream("arkive.properties")
            ?: error("arkive.properties is missing from the plugin jar")
        stream.use { Properties().apply { load(it) }.getProperty("version") }
            ?.takeIf { it.isNotBlank() && !it.startsWith("\${") }
            ?: error("arkive.properties was not expanded with the plugin version")
    }
}
