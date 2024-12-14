package com.infinum.arkive.plugin.generators

import com.inifnum.arkive.metadata.model.ComponentVariant
import com.inifnum.arkive.metadata.model.ComponentsMetaData
import com.inifnum.arkive.metadata.model.ShowcaseItem
import org.gradle.internal.cc.base.logger

interface ShowcaseGenerator {
    fun generateShowcase(snapshots: List<String>, metadata: ComponentsMetaData): List<ShowcaseItem>
}

class ShowcaseGeneratorImpl : ShowcaseGenerator {
    override fun generateShowcase(
        snapshots: List<String>,
        metadata: ComponentsMetaData,
    ): List<ShowcaseItem> {
        val items = metadata.components.map { component ->
            ShowcaseItem(
                component = component,
                snapshotPath = snapshots.findSnapshot(component.id),
                variants = snapshots.findVariants(component.id)
            )
        }
        return items
    }

    private fun List<String>.findSnapshot(id: String): String {
        return find {
            it.endsWith("$id.png")
        } ?: error("Cant find component with id: $id")
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
                snapshotPath = snapshot
            )
        }
    }
}


fun main() {
    val path =
        "images/com.infinum.arkive_ArkiveSnapshotTestGenerator_testAllComposableFunctions_com_infinum_arkive_sample_composables_previewwideroundedbutton_density_3.0.png"

    val id = "com_infinum_arkive_sample_composables_previewwideroundedbutton"
    println(path)
}