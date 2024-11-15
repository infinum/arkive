package com.inifnum.arkive.metadata.model

import kotlinx.serialization.Serializable

@Serializable
data class ShowcaseItem(
    val component: Component,
    val snapshotPath: String,
)

@Serializable
data class ArkiveShowcase(
    val items: List<ShowcaseItem>,
)