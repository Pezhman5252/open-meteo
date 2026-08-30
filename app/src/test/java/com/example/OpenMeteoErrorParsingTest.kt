package com.example

import com.example.data.remote.OpenMeteoApiException
import com.example.data.remote.OpenMeteoErrorDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the Open-Meteo HTTP 400 error contract (skill section 24):
 * `{"error": true, "reason": "..."}` must parse cleanly, missing/blank reasons
 * must degrade to null instead of crashing, and [OpenMeteoApiException] must
 * carry the HTTP code plus the API reason through to the UI layer.
 */
class OpenMeteoErrorParsingTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(OpenMeteoErrorDto::class.java)

    @Test
    fun parseError_documentedShape_extractsReason() {
        val json = """{"error":true,"reason":"Cannot initialize WeatherVariable from invalid String value tempeture_2m for key hourly"}"""
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(true, dto?.error)
        assertEquals(
            "Cannot initialize WeatherVariable from invalid String value tempeture_2m for key hourly",
            dto?.reason
        )
    }

    @Test
    fun parseError_missingReason_isNull() {
        val json = """{"error":true}"""
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(true, dto?.error)
        assertNull(dto?.reason)
    }

    @Test
    fun parseError_unknownFields_areIgnored() {
        // Extra server fields must not break parsing.
        val json = """{"error":true,"reason":"bad param","extra":"ignored"}"""
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("bad param", dto?.reason)
    }

    @Test
    fun exception_carriesCodeAndReason() {
        val ex = OpenMeteoApiException(httpCode = 400, apiReason = "bad variable")
        assertEquals(400, ex.httpCode)
        assertEquals("bad variable", ex.apiReason)
        assertEquals("Open-Meteo API error (HTTP 400): bad variable", ex.message)
    }

    @Test
    fun exception_nullReason_buildsMessageWithoutReason() {
        val ex = OpenMeteoApiException(httpCode = 500, apiReason = null)
        assertEquals(500, ex.httpCode)
        assertNull(ex.apiReason)
        assertEquals("Open-Meteo API error (HTTP 500)", ex.message)
    }

    @Test
    fun exception_blankReason_buildsMessageWithoutReason() {
        val ex = OpenMeteoApiException(httpCode = 400, apiReason = "   ")
        assertEquals("Open-Meteo API error (HTTP 400)", ex.message)
    }
}
