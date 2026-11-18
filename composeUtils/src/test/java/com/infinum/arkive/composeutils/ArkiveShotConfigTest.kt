package com.infinum.arkive.composeutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArkiveShotConfigTest {

    @Test
    fun densityMapsToTheDpiOfItsBucket() {
        assertEquals(120, ArkiveShotConfig(density = 0.75f).densityDpi)
        assertEquals(160, ArkiveShotConfig(density = 1f).densityDpi)
        assertEquals(240, ArkiveShotConfig(density = 1.5f).densityDpi)
        assertEquals(640, ArkiveShotConfig(density = 4f).densityDpi)
    }

    @Test
    fun noDensityMeansTheDevicesOwn() {
        assertNull(ArkiveShotConfig.Default.densityDpi)
        assertNull(ArkiveShotConfig(fontScale = 2f).densityDpi)
    }
}
