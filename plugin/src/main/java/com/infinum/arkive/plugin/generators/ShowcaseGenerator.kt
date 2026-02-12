package com.infinum.arkive.plugin.generators

import com.inifnum.arkive.metadata.model.ArkiveShowcase
import com.inifnum.arkive.metadata.model.ComponentsMetaData
import com.inifnum.arkive.metadata.model.ShowcaseItem

interface ShowcaseGenerator {
    fun generateShowcase(snapshots: List<String>, metadata: ComponentsMetaData): ArkiveShowcase
}

class ShowcaseGeneratorImpl : ShowcaseGenerator {
    override fun generateShowcase(
        snapshots: List<String>,
        metadata: ComponentsMetaData,
    ): ArkiveShowcase {
        val items = metadata.components.map { component ->
            ShowcaseItem(
                component = component,
                snapshotPath = snapshots.findSnapshot(component.id),
            )
        }
        return ArkiveShowcase(items)
    }

    private fun List<String>.findSnapshot(id: String): String {
        return find {
            it.endsWith("$id.png")
        } ?: error("Cant find component with id: $id")
    }
}
