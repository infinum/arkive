package com.infinum.arkive.metadata

import com.infinum.arkive.metadata.model.ArkiveModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShowcaseJsonCompatibilityTest {

    @Test
    fun showcaseWrittenBeforeUiTreesStillDecodes() {
        val legacy = """
            {"name":"sample","items":[{"component":{"id":"c","name":"C","functionName":"f",
            "packageName":"p","fileName":"F.kt","group":"g","tags":[],"extraMetadata":[]},
            "snapshotPath":"images/c.png","variants":[{"category":"font","variant":"1.5",
            "snapshotPath":"images/c_font_1.5.png"}]}]}
        """.trimIndent()

        val module: ArkiveModule = fromJson(legacy)

        assertNull(module.items.single().uiTreePath)
        assertNull(module.items.single().variants.single().uiTreePath)
    }

    @Test
    fun uiTreePathsRoundTrip() {
        val module: ArkiveModule = fromJson(
            """
            {"name":"sample","items":[{"component":{"id":"c","name":"C","functionName":"f",
            "packageName":"p","fileName":"F.kt","group":"g","tags":[],"extraMetadata":[]},
            "snapshotPath":"images/c.png","uiTreePath":"uitree/c.json","variants":[]}]}
            """.trimIndent(),
        )

        assertEquals("uitree/c.json", fromJson<ArkiveModule>(toJson(module)).items.single().uiTreePath)
    }
}
