package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPackagesTest {

    @Test
    fun hashMatchesTheComposeCompilerAlgorithm() {
        // fold(0) { h, c -> h * 31 + c.code }.absoluteValue
        assertEquals(97, AppPackages.packageHash("a"))
        assertEquals(3105, AppPackages.packageHash("ab"))
    }

    @Test
    fun containsOnlyTheGivenPackages() {
        val packages = AppPackages(listOf("com.app", ""))

        assertTrue(AppPackages.packageHash("com.app") in packages)
        assertFalse(AppPackages.packageHash("com.app.other") in packages)
        assertFalse(0 in packages)
    }
}
