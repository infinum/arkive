package com.infinum.arkive.processor.generators

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

    @Test
    fun paparazziKeepsComposeOverridesForNow() {
        val code = PaparazziTestGenerator().generate().toString().normalized()

        assertTrue(code, code.contains("DensityVariant(density) { function() }"))
        assertTrue(code, code.contains("FontVariant(fontScale) { function() }"))
    }
}
