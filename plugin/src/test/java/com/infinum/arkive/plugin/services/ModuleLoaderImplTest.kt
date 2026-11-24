package com.infinum.arkive.plugin.services

import com.infinum.arkive.metadata.model.ArkiveModule
import com.infinum.arkive.metadata.toJson
import java.io.File
import org.gradle.api.logging.Logging
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleLoaderImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger = Logging.getLogger(ModuleLoaderImplTest::class.java)

    @Test
    fun aggregateHoldsExactlyTheModuleShowcase() {
        val moduleDir = folder.newFolder("module", "showcase")
        File(moduleDir, "arkive-showcase.json").writeText(toJson(ArkiveModule("app", emptyList())))
        File(moduleDir, "images").mkdirs()
        File(moduleDir, "images/card.png").writeText("new")
        val output = folder.newFolder("aggregate")
        File(output, "app/images").mkdirs()
        File(output, "app/images/stale.png").writeText("stale")
        File(output, "app/uitree").mkdirs()
        File(output, "app/uitree/stale.json").writeText("{}")

        val modules = ModuleLoaderImpl(mapOf("app" to moduleDir), logger).loadModules(output)

        assertEquals(listOf("app"), modules.map { it.name })
        assertEquals(listOf("arkive-showcase.json", "images"), File(output, "app").list()!!.sorted())
        assertEquals(listOf("card.png"), File(output, "app/images").list()!!.sorted())
    }
}
