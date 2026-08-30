package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for the Open-Meteo HTTP 400 error JSON body.
 *
 * Documented shape:
 * ```json
 * { "error": true, "reason": "Cannot initialize WeatherVariable from invalid String value tempeture_2m for key hourly" }
 * ```
 */
@JsonClass(generateAdapter = true)
data class OpenMeteoErrorDto(
    @param:Json(name = "error") val error: Boolean = true,
    @param:Json(name = "reason") val reason: String? = null
)

/**
 * Typed exception representing an unsuccessful Open-Meteo API response.
 *
 * Carries the HTTP status code and, when the server returned a parseable
 * error JSON body, the API [reason] string. This lets the UI distinguish an
 * Open-Meteo API validation failure (HTTP 400) from a generic transport or
 * parsing failure, per the Open-Meteo error-handling contract.
 */
class OpenMeteoApiException(
    val httpCode: Int,
    val apiReason: String?,
    cause: Throwable? = null
) : Exception(
    buildString {
        append("Open-Meteo API error (HTTP $httpCode)")
        if (!apiReason.isNullOrBlank()) {
            append(": ").append(apiReason)
        }
    },
    cause
)
