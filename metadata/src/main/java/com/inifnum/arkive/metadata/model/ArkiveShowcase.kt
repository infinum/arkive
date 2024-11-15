package com.inifnum.arkive.metadata.model

import kotlinx.serialization.Serializable

@Serializable
data class ArkiveShowcase(
    val component: Component,
    val snapshotPath: String,
)