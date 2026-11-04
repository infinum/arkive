package com.infinum.arkive.composeutils.uitree

import kotlin.math.absoluteValue

/**
 * The consumer module's own packages, collected by Arkive's KSP processor. A composable call
 * whose call site lives in one of them is app code; everything else — androidx, other
 * libraries, other modules, Arkive's generated wrappers — is library code. Compose source
 * information identifies a call site's package only by hash, so membership is by hash.
 */
internal class AppPackages(packageNames: Collection<String>) {

    private val hashes: Set<Int> = packageNames.filter { it.isNotEmpty() }.mapTo(HashSet(), ::packageHash)

    operator fun contains(hash: Int): Boolean = hash in hashes

    companion object {
        private const val HASH_MULTIPLIER = 31

        /** Exactly the Compose compiler's package hash (the `#xxxx` suffix of source information). */
        fun packageHash(packageName: String): Int =
            packageName.fold(0) { hash, char -> hash * HASH_MULTIPLIER + char.code }.absoluteValue
    }
}
