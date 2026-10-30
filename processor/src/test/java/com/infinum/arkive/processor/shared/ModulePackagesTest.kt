package com.infinum.arkive.processor.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class ModulePackagesTest {

    @Test
    fun keepsEachRealPackageOnceSortedAndDropsArkivesGeneratedOne() {
        val packages = ModulePackages.from(sequenceOf("com.b", "", "com.infinum.arkive", "com.a", "com.a"))

        assertEquals(listOf("com.a", "com.b"), packages.toList())
    }
}
