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
        val code = RoborazziTestGenerator(deviceQualifiers = "", testApplication = "").generate().toString().normalized()

        assertTrue(code, code.contains("ArkiveUiTreeViews.host(activity, shotName)"))
    }

    @Test
    fun roborazziRunsUnderTheConfiguredTestApplication() {
        val code = RoborazziTestGenerator(deviceQualifiers = "", testApplication = "com.example.TestApp")
            .generate().toString().normalized()

        assertTrue(code, code.contains("application = com.example.TestApp::class"))
    }
}
