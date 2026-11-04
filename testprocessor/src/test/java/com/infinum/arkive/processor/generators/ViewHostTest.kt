package com.infinum.arkive.processor.generators

import org.junit.Assert.assertTrue
import org.junit.Test

class ViewHostTest {

    // Whitespace-normalized: KotlinPoet wraps long lines at spaces.
    private fun String.normalized() = replace(Regex("\\s+"), " ")

    @Test
    fun paparazziInflatesViewsIntoTheUiTreeHost() {
        val code = PaparazziTestGenerator().generate().toString().normalized()

        assertTrue(code, code.contains("ArkiveUiTreeViews.host(paparazzi.context, name)"))
    }

    @Test
    fun roborazziInflatesViewsIntoTheUiTreeHost() {
        val code = RoborazziTestGenerator(deviceQualifiers = "").generate().toString().normalized()

        assertTrue(code, code.contains("ArkiveUiTreeViews.host(activity, shotName)"))
    }
}
