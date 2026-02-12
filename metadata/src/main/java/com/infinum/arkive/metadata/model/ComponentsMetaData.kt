package com.infinum.arkive.metadata.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Component(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("functionName") val functionName: String,
    @SerialName("packageName") val packageName: String,
    @SerialName("group") val group: String,
    @SerialName("tags") val tags: List<String>,
    @SerialName("extraMetadata") val extraMetadata: List<String>,
)

@Serializable
data class ComponentsMetaData(
    @SerialName("components") val components: List<Component>,
)
