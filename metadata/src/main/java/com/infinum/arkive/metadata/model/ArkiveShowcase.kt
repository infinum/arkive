package com.infinum.arkive.metadata.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ComponentVariant(
    @SerialName("category") val category: String,
    @SerialName("variant") val variant: String,
    @SerialName("snapshotPath") val snapshotPath: String,
)

@Serializable
public data class ShowcaseItem(
    @SerialName("component") val component: Component,
    @SerialName("snapshotPath") val snapshotPath: String,
    @SerialName("variants") val variants: List<ComponentVariant>,
)

@Serializable
public data class ArkiveModule(
    @SerialName("name") val name: String,
    @SerialName("items") val items: List<ShowcaseItem>,
    @SerialName("designFileKey") val designFileKey: String? = null,
)

@Serializable
public data class ArkiveShowcase(
    @SerialName("projectName") val projectName: String,
    @SerialName("modules") val modules: List<ArkiveModule>,
)
