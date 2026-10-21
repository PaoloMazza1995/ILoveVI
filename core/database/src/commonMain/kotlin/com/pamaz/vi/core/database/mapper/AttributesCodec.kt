package com.pamaz.vi.core.database.mapper

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

/** Encodes a free-form attributes map as the JSON text stored in the `attributes` column. */
internal fun encodeAttributes(attributes: Map<String, String>): String =
    json.encodeToString(stringMapSerializer, attributes)

/** Reverses [encodeAttributes]. */
internal fun decodeAttributes(raw: String): Map<String, String> =
    json.decodeFromString(stringMapSerializer, raw)
