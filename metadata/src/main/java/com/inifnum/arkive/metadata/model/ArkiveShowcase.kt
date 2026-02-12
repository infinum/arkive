package com.inifnum.arkive.metadata.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowcaseItem(
    @SerialName("component") val component: Component,
    @SerialName("snapshotPath") val snapshotPath: String,
)

@Serializable
data class ArkiveShowcase(
    @SerialName("items") val items: List<ShowcaseItem>,
)
