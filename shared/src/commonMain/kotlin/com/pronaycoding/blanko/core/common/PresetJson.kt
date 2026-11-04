package com.pronaycoding.blanko.core.common

/**
 * Minimal JSON codec for preset volume maps (`{"<index>": <volume>, ...}`).
 *
 * The format is the same flat object that was previously written with `org.json`, so presets that
 * are already stored in the database keep loading. It is hand-rolled because `org.json` is not
 * available on desktop and the schema is trivial.
 */
internal object PresetJson {
    fun mapToJson(map: Map<Int, Float>): String =
        map
            .filterValues { it > 0f }
            .entries
            .joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
                "\"$key\":${value.toDouble()}"
            }

    fun jsonToMap(json: String): Map<Int, Float> {
        val body = json.trim().removePrefix("{").removeSuffix("}").trim()
        if (body.isEmpty()) return emptyMap()
        val out = mutableMapOf<Int, Float>()
        body.split(',').forEach { entry ->
            val separator = entry.indexOf(':')
            if (separator < 0) return@forEach
            val key = entry.substring(0, separator).trim().trim('"').toIntOrNull()
            val value = entry.substring(separator + 1).trim().toFloatOrNull()
            if (key != null && value != null) out[key] = value
        }
        return out
    }
}
