package com.pronaycoding.blanko.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetJsonTest {
    @Test
    fun roundTripsVolumes() {
        val volumes = mapOf(0 to 1f, 3 to 0.5f, 12 to 0.25f)

        val decoded = PresetJson.jsonToMap(PresetJson.mapToJson(volumes))

        assertEquals(volumes, decoded)
    }

    @Test
    fun skipsSilentSounds() {
        val json = PresetJson.mapToJson(mapOf(1 to 0f, 2 to 0.75f))

        assertEquals(mapOf(2 to 0.75f), PresetJson.jsonToMap(json))
    }

    @Test
    fun emptyMapProducesAnEmptyObject() {
        assertEquals("{}", PresetJson.mapToJson(emptyMap()))
        assertEquals(emptyMap(), PresetJson.jsonToMap("{}"))
        assertEquals(emptyMap(), PresetJson.jsonToMap(""))
        assertEquals(emptyMap(), PresetJson.jsonToMap("   "))
    }

    /** Presets written by earlier versions of the app used `org.json`, which prints whole numbers without `.0`. */
    @Test
    fun readsPresetsStoredByOrgJson() {
        assertEquals(mapOf(2 to 1f, 5 to 0.5f), PresetJson.jsonToMap("""{"2":1,"5":0.5}"""))
        assertEquals(mapOf(7 to 0.30000001192092896.toFloat()), PresetJson.jsonToMap("""{"7":0.30000001192092896}"""))
        assertEquals(mapOf(1 to 1.0E-4f), PresetJson.jsonToMap("""{"1":1.0E-4}"""))
    }

    @Test
    fun toleratesWhitespaceAndIgnoresMalformedEntries() {
        val decoded = PresetJson.jsonToMap("""{ "1" : 0.5 , "x": 1, "3": "oops", "4": 0.2 }""")

        assertEquals(mapOf(1 to 0.5f, 4 to 0.2f), decoded)
    }

    @Test
    fun outputIsFlatJsonObjectWithQuotedKeys() {
        val json = PresetJson.mapToJson(mapOf(4 to 0.5f))

        assertTrue(json.startsWith("{\"4\":"), json)
        assertTrue(json.endsWith("}"), json)
    }
}
