package com.infinum.arkive.processor.generators

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotConfigTest {

    // Whitespace-normalized: KotlinPoet wraps long lines at spaces.
    private fun String.normalized() = replace(Regex("\\s+"), " ")

    private val roborazzi = RoborazziTestGenerator(deviceQualifiers = "").generate().toString().normalized()

    @Test
    fun roborazziAppliesTheShotConfigBeforeTheActivityLaunches() {
        assertTrue(
            roborazzi,
            roborazzi.contains("shotConfig.densityDpi?.let { org.robolectric.RuntimeEnvironment.setQualifiers(\"+\" + it + \"dpi\") }"),
        )
        assertTrue(roborazzi, roborazzi.contains("shotConfig.fontScale?.let { org.robolectric.RuntimeEnvironment.setFontScale(it) }"))
        // base.evaluate() is the compose rule, which launches the activity.
        assertTrue(roborazzi, roborazzi.indexOf("setFontScale") < roborazzi.indexOf("base.evaluate()"))
    }

    @Test
    fun roborazziCarriesTheShotConfigAsATestParameter() {
        assertTrue(roborazzi, roborazzi.contains("entries.add(arrayOf(name, \"variant\", config, function))"))
        assertTrue(roborazzi, roborazzi.contains("ArkiveShotConfig.Default, function))"))
    }

    private val paparazzi = PaparazziTestGenerator().generate().toString().normalized()

    @Test
    fun paparazziSwitchesTheDeviceOnlyWhenTheShotNeedsAnother() {
        assertTrue(
            paparazzi,
            paparazzi.contains(
                "val device = deviceFor(config) if (device != currentDevice) { " +
                    "paparazzi.unsafeUpdateConfig(deviceConfig = device) currentDevice = device } " +
                    "paparazzi.snapshot(name = name) { function() }",
            ),
        )
        assertFalse(paparazzi, paparazzi.contains("DensityVariant"))
        assertFalse(paparazzi, paparazzi.contains("FontVariant"))
    }

    @Test
    fun paparazziScalesVariantDevicesFromTheSessionsBaseDevice() {
        assertTrue(paparazzi, paparazzi.contains("Paparazzi(deviceConfig = baseDevice,"))
        // Pixels grow with the density, so the screen keeps its dp size.
        assertTrue(paparazzi, paparazzi.contains("screenWidth = (baseDevice.screenWidth * scale)"))
        assertTrue(paparazzi, paparazzi.contains("screenHeight = (baseDevice.screenHeight * scale)"))
        assertTrue(paparazzi, paparazzi.contains("density = com.android.resources.Density.create(dpi)"))
        assertTrue(paparazzi, paparazzi.contains("fontScale = config.fontScale ?: baseDevice.fontScale"))
    }

    @Test
    fun paparazziSavesImagesAtTheirRenderedSize() {
        // Without it, Paparazzi shrinks every image to a 1000 px long side, so large
        // density variants would all be saved at the same size.
        assertTrue(paparazzi, paparazzi.contains("useDeviceResolution = true"))
    }
}
