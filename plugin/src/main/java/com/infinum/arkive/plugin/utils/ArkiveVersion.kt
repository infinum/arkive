package com.infinum.arkive.plugin.utils

import java.util.Properties

/**
 * The plugin's own version, the Kotlin version it was built with, and the engine
 * versions it provisions, stamped into `arkive.properties` by `processResources`.
 * Version source of truth is `VERSION_NAME` in `gradle.properties`; the Kotlin version is
 * the repo's Kotlin Gradle plugin version; the engine versions come from the repo's
 * version catalog.
 */
internal object ArkiveVersion {
    private val properties: Properties by lazy {
        val stream = ArkiveVersion::class.java.classLoader.getResourceAsStream("arkive.properties")
            ?: error("arkive.properties is missing from the plugin jar")
        stream.use { Properties().apply { load(it) } }
    }

    val current: String by lazy { stamped("version") }

    /**
     * The Kotlin version the published artifacts were compiled with. Native/js klibs
     * are not forward-compatible: a consumer's non-JVM compilations can only read the
     * multiplatform annotations when the consumer's Kotlin is at least this version.
     */
    val builtWithKotlin: String by lazy { stamped("kotlinVersion") }

    /** Paparazzi version Arkive provisions when the consumer has no Paparazzi plugin. */
    val paparazzi: String by lazy { stamped("paparazziVersion") }

    /** Roborazzi version Arkive provisions (plugin and test runtime). */
    val roborazzi: String by lazy { stamped("roborazziVersion") }

    private fun stamped(key: String): String =
        properties.getProperty(key)
            ?.takeIf { it.isNotBlank() && !it.startsWith("\${") }
            ?: error("arkive.properties was not expanded with $key")
}
