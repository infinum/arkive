package com.infinum.arkive.plugin.generators

import com.infinum.arkive.metadata.model.ComponentVariant
import com.infinum.arkive.metadata.model.ComponentsMetaData
import com.infinum.arkive.metadata.model.ShowcaseItem

interface ShowcaseGenerator {
    fun generateShowcase(snapshots: List<String>, metadata: ComponentsMetaData): List<ShowcaseItem>
}

class ShowcaseGeneratorImpl(
    private val onMissingSnapshot: (String) -> Unit = {},
) : ShowcaseGenerator {
    override fun generateShowcase(
        snapshots: List<String>,
        metadata: ComponentsMetaData,
    ): List<ShowcaseItem> {
        // A component whose snapshot never materialized (e.g. its preview failed to render
        // and was skipped at test time) is dropped with a warning instead of failing the task.
        return metadata.components.mapNotNull { component ->
            val snapshotPath = snapshots.findSnapshot(component.id)
            if (snapshotPath == null) {
                onMissingSnapshot(component.id)
                null
            } else {
                ShowcaseItem(
                    component = component,
                    snapshotPath = snapshotPath,
                    variants = snapshots.findVariants(component.id),
                )
            }
        }
    }

    private fun List<String>.findSnapshot(id: String): String? {
        return find {
            it.endsWith("$id.png")
        }
    }

    private fun List<String>.findVariants(id: String): List<ComponentVariant> {
        return filter {
            it.contains("${id}_")
        }.map { snapshot ->
            val variantBlock =
                snapshot.substring(snapshot.indexOf(id) + id.length + 1, snapshot.indexOf(".png"))
                    .split("_")

            val category = variantBlock[0]
            val variant = variantBlock[1]
            ComponentVariant(
                category = category,
                variant = variant,
                snapshotPath = snapshot,
            )
        }
    }
}
