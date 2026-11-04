package com.infinum.arkive.plugin.generators

import com.infinum.arkive.metadata.model.ComponentVariant
import com.infinum.arkive.metadata.model.ComponentsMetaData
import com.infinum.arkive.metadata.model.ShowcaseItem

internal interface ShowcaseGenerator {
    /**
     * [uiTrees] maps a lowercased shot id (`<id>` or `<id>_<category>_<value>`) to the tree's
     * module-relative path. Lowercased because Paparazzi lowercases snapshot filenames while
     * the tree keeps the runner's original name.
     */
    fun generateShowcase(
        snapshots: List<String>,
        metadata: ComponentsMetaData,
        uiTrees: Map<String, String> = emptyMap(),
    ): List<ShowcaseItem>
}

internal class ShowcaseGeneratorImpl(
    private val onMissingSnapshot: (String) -> Unit = {},
    private val onMalformedVariant: (String) -> Unit = {},
) : ShowcaseGenerator {
    override fun generateShowcase(
        snapshots: List<String>,
        metadata: ComponentsMetaData,
        uiTrees: Map<String, String>,
    ): List<ShowcaseItem> {
        val idsLongestFirst = metadata.components.map { it.id }.distinct().sortedByDescending { it.length }
        val resolved = snapshots.mapNotNull { resolve(it, idsLongestFirst, uiTrees) }
        // A component whose snapshot never materialized (e.g. its preview failed to render
        // and was skipped at test time) is dropped with a warning instead of failing the task.
        // Its tree, if any, is therefore never referenced.
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
                    uiTreePath = uiTrees[component.id.lowercase()],
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
    private fun resolve(snapshot: String, idsLongestFirst: List<String>, uiTrees: Map<String, String>): ResolvedSnapshot? {
        val name = snapshot.substringBeforeLast('.')
        val resolved = idsLongestFirst.firstNotNullOfOrNull { id -> resolveAgainst(snapshot, name, id, uiTrees) }
        if (resolved == null && idsLongestFirst.any { "_${it}_" in name }) {
            onMalformedVariant(snapshot)
        }
        return resolved
    }

    private fun resolveAgainst(snapshot: String, name: String, id: String, uiTrees: Map<String, String>): ResolvedSnapshot? {
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
                        variant = ComponentVariant(
                            category = category,
                            variant = value,
                            snapshotPath = snapshot,
                            uiTreePath = uiTrees["${id}_${category}_$value".lowercase()],
                        ),
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
