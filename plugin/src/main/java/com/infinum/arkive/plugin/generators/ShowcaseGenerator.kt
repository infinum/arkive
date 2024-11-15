package com.infinum.arkive.plugin.generators

import com.infinum.arkive.plugin.services.MetadataLoader
import com.infinum.arkive.plugin.services.SnapshotsLoader
import com.inifnum.arkive.metadata.model.ArkiveShowcase
import com.inifnum.arkive.metadata.model.ShowcaseItem

interface ShowcaseGenerator {
    fun generateShowcase(): ArkiveShowcase
}

class ShowcaseGeneratorImpl(
    private val snapshotsLoader: SnapshotsLoader,
    private val metadataLoader: MetadataLoader,
) : ShowcaseGenerator {
    override fun generateShowcase(): ArkiveShowcase {
        val snapshots = snapshotsLoader.loadSnapshots()
        val metadata = metadataLoader.loadMetaData()

        val items = metadata.components.map { component ->
            ShowcaseItem(
                component = component,
                snapshotPath = snapshots.findSnapshot(component.id)
            )
        }
        return ArkiveShowcase(items)
    }

    private fun List<String>.findSnapshot(id: String): String {
        return find {
            it.endsWith("$id.png")
        } ?: throw IllegalStateException("Cant find component with id: $id")
    }

}