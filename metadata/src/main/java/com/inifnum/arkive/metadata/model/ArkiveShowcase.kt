package com.inifnum.arkive.metadata.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowcaseItem(
    @SerialName("component") val component: Component,
    @SerialName("snapshotPath") val snapshotPath: String,
)

@Serializable
data class ArkiveModule(
    @SerialName("name") val name: String,
    @SerialName("items") val items: List<ShowcaseItem>,
)

@Serializable
data class ArkiveShowcase(
    @SerialName("projectName") val projectName: String,
    @SerialName("modules") val modules: List<ArkiveModule>
)