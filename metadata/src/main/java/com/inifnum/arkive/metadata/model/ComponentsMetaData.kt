package com.inifnum.arkive.metadata.model

import com.inifnum.arkive.metadata.toJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Component(
    val id: String,
    val name: String,
    val functionName: String,
    val packageName: String,
    val group: String,
    val tags: List<String>,
    val extraMetadata: List<String>,
)

@Serializable
data class ComponentsMetaData(
    val components: List<Component>
)

fun main() {
    val json = Json { encodeDefaults = true }
    val componentList = ComponentsMetaData(
        listOf(
            Component(
                id = "id",
                name = "name",
                functionName = "functionName",
                packageName = "packageName",
                group = "group",
                tags = listOf("tag"),
                extraMetadata = listOf("extra")
            )
        )
    )
    val jsonReport = componentList.toJson()
    println(jsonReport)
}