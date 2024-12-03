package com.inifnum.arkive.metadata

import com.inifnum.arkive.metadata.model.ComponentsMetaData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }

fun ComponentsMetaData.toJson(): String = json.encodeToString(this)

fun String.ComponentsMetaData(): ComponentsMetaData = json.decodeFromString(this)
