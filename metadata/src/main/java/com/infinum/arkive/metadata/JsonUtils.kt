package com.infinum.arkive.metadata

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }

inline fun <reified T> toJson(model: T): String = json.encodeToString(model)

inline fun <reified T> fromJson(data: String): T = json.decodeFromString(data)
