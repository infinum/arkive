package com.inifnum.arkive.metadata.model

import kotlinx.serialization.Serializable

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
    val components: List<Component>,
)
