package com.infinum.arkive.plugin.utils

import org.gradle.api.Project

/**
 * Unique, path-derived module name used consistently for the module's showcase JSON and
 * the aggregate's per-module directory. Gradle project *names* collide in nested layouts
 * (":common:ui" and ":epos:ui" are both named "ui"), which made modules overwrite each
 * other in the aggregated showcase. Single-segment projects keep their plain name.
 */
val Project.showcaseModuleName: String
    get() = path.removePrefix(":").replace(':', '-').ifEmpty { name }
