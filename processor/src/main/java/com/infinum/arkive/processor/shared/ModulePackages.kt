package com.infinum.arkive.processor.shared

/**
 * The module's own packages, baked into the generated shooter so UI-tree capture can tell
 * calls made from this module's code (which get a `source` location) from library calls.
 * Arkive's generated package is excluded: the preview wrappers live there and are never the
 * component.
 */
object ModulePackages {
    fun from(packageNames: Sequence<String>): Set<String> =
        packageNames.filter { it.isNotEmpty() && it != Constants.PACKAGE_NAME }.toSortedSet()
}
