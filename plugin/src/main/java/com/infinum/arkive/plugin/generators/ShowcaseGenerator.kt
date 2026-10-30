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
        val idsLongestFirst = metadata.components.map { it.id }.distinct().sortedByDescending { it.length }
        val resolved = snapshots.mapNotNull { resolve(it, idsLongestFirst) }
        // A component whose snapshot never materialized (e.g. its preview failed to render
        // and was skipped at test time) is dropped with a warning instead of failing the task.
        return metadata.components.mapNotNull { component ->
            val owned = resolved.filter { it.componentId == component.id }
            val base = owned.firstOrNull { it.variant == null }
            if (base == null) {
                onMissingSnapshot(component.id)
                null
            } else {
                ShowcaseItem(
                    component = component,
                    snapshotPath = base.snapshot,
                    variants = owned.mapNotNull { it.variant },
                )
            }
        }
    }

    // Snapshot filenames are <prefix>_<id>.png (base) and <prefix>_<id>_<category>_<value>.png
    // (variants; the processor keeps '_' out of categories and values). Ids are dash-joined,
    // but function and package names may contain '_' — so one component's id can extend
    // another's at a '_' (Card / Card_Header), and the shorter id's "_<id>_" marker then
    // matches the longer one's files. Each snapshot therefore goes to the LONGEST id it
    // matches as either a base (nothing after the id) or a variant (exactly category_value).
    private fun resolve(snapshot: String, idsLongestFirst: List<String>): ResolvedSnapshot? {
        val name = snapshot.substringBeforeLast('.')
        val resolved = idsLongestFirst.firstNotNullOfOrNull { id -> resolveAgainst(snapshot, name, id) }
        if (resolved == null && idsLongestFirst.any { "_${it}_" in name }) {
            onMalformedVariant(snapshot)
        }
        return resolved
    }

    private fun resolveAgainst(snapshot: String, name: String, id: String): ResolvedSnapshot? {
        val marker = "_${id}_"
        return when {
            name.endsWith("_$id") -> ResolvedSnapshot(id, snapshot, variant = null)
            marker in name -> name.substringAfter(marker)
                .split('_')
                .takeIf { blocks -> blocks.size == 2 && blocks.none(String::isEmpty) }
                ?.let { (category, value) ->
                    ResolvedSnapshot(
                        componentId = id,
                        snapshot = snapshot,
                        variant = ComponentVariant(category = category, variant = value, snapshotPath = snapshot),
                    )
                }
            else -> null
        }
    }

    private data class ResolvedSnapshot(
        val componentId: String,
        val snapshot: String,
        val variant: ComponentVariant?,
    )
}
