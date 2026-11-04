package com.infinum.arkive.metadata

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@PublishedApi
internal val json: Json = Json { encodeDefaults = true }

public inline fun <reified T> toJson(model: T): String = json.encodeToString(model)

public inline fun <reified T> fromJson(data: String): T = json.decodeFromString(data)
