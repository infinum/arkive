package com.infinum.arkive.plugin.generators

import com.infinum.arkive.metadata.model.ComponentVariant
import com.infinum.arkive.metadata.model.ComponentsMetaData
import com.infinum.arkive.metadata.model.ShowcaseItem

interface ShowcaseGenerator {
    fun generateShowcase(snapshots: List<String>, metadata: ComponentsMetaData): List<ShowcaseItem>
}

class ShowcaseGeneratorImpl(
    private val onMissingSnapshot: (String) -> Unit = {},
    private val onMalformedVariant: (String) -> Unit = {},
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

    // Snapshot filenames are <testclass>_<id>.png (base) and <testclass>_<id>_<category>_<value>.png
    // (variants). Component ids are dash-joined and never contain '_', so "_<id>.png" and
    // "_<id>_" match exactly one component — they cannot match inside a longer id, and one
    // component's base can never be claimed as another's variant.

    private fun List<String>.findSnapshot(id: String): String? {
        return find {
            it.endsWith("_$id.png")
        }
    }

    private fun List<String>.findVariants(id: String): List<ComponentVariant> {
        val marker = "_${id}_"
        return filter {
            it.contains(marker)
        }.mapNotNull { snapshot ->
            val variantBlock = snapshot
                .substring(snapshot.indexOf(marker) + marker.length)
                .substringBeforeLast('.')
                .split("_")

            if (variantBlock.size < 2) {
                onMalformedVariant(snapshot)
                null
            } else {
                ComponentVariant(
                    category = variantBlock.first(),
                    // A category may itself contain '_' (e.g. a preview-parameter name);
                    // everything after the first block is the variant value.
                    variant = variantBlock.drop(1).joinToString("_"),
                    snapshotPath = snapshot,
                )
            }
        }
    }
}
