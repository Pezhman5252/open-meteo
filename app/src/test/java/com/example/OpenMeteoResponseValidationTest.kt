package com.example

import com.example.data.remote.DailyData
import com.example.data.remote.HourlyData
import com.example.data.remote.Minutely15Data
import com.example.data.remote.OpenMeteoApiException
import com.example.data.remote.WeatherResponse
import com.example.data.repository.WeatherRepository
import com.example.ui.util.MountaineeringHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Open-Meteo skill §30/§34 tests: parallel-array alignment validation, unknown
 * WMO code fallback, missing optional sections, and malformed JSON handling.
 */
class OpenMeteoResponseValidationTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val responseAdapter = moshi.adapter(WeatherResponse::class.java)

    // =========================================================================
    // §30 — parallel array alignment (via reflection on the private validator
    // to keep the test independent of Retrofit wiring)
    // =========================================================================

    private fun invokeValidator(response: WeatherResponse) {
        val method = WeatherRepository::class.java.getDeclaredMethod(
            "validateResponseArrayAlignment",
            WeatherResponse::class.java
        )
        method.isAccessible = true
        try {
            // Retrofit dynamic proxy instance — never used for network calls;
            // the validator under test is pure and needs only a receiver.
            // Response<T> return types are handled by Retrofit's built-in converters,
            // so no explicit converter factory is required to build the proxy.
            val api = retrofit2.Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .build()
                .create(com.example.data.remote.OpenMeteoApiService::class.java)
            // Pass fallbackApiService explicitly: the constructor's default touches
            // RetrofitHelper, whose static init needs the Android framework and
            // fails in a plain JVM test environment.
            method.invoke(WeatherRepository(api, api), response)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Unwrap and rethrow the real exception so assertThrows works
            throw e.cause ?: e
        }
    }

    @Test
    fun `aligned hourly arrays pass validation`() {
        val response = WeatherResponse(
            hourly = HourlyData(
                time = listOf("2026-01-01T00:00", "2026-01-01T01:00", "2026-01-01T02:00"),
                temperature2m = listOf(10.0, 11.0, 12.0),
                windSpeed10m = listOf(5.0, 6.0, 7.0)
            )
        )
        invokeValidator(response) // no exception
    }

    @Test
    fun `misaligned hourly array throws with section and variable name`() {
        val response = WeatherResponse(
            hourly = HourlyData(
                time = listOf("2026-01-01T00:00", "2026-01-01T01:00", "2026-01-01T02:00"),
                temperature2m = listOf(10.0, 11.0)
            )
        )
        val ex = assertThrows(OpenMeteoApiException::class.java) {
            invokeValidator(response)
        }
        assertEquals(422, ex.httpCode)
        assertTrue("reason must name the variable", ex.apiReason?.contains("temperature_2m") == true)
        assertTrue("reason must name the section", ex.apiReason?.contains("hourly") == true)
    }

    @Test
    fun `misaligned daily array throws`() {
        val response = WeatherResponse(
            daily = DailyData(
                time = listOf("2026-01-01", "2026-01-02"),
                temperature2mMax = listOf(10.0)
            )
        )
        val ex = assertThrows(OpenMeteoApiException::class.java) {
            invokeValidator(response)
        }
        assertEquals(422, ex.httpCode)
        assertTrue(ex.apiReason?.contains("temperature_2m_max") == true)
    }

    @Test
    fun `misaligned minutely_15 array throws`() {
        val response = WeatherResponse(
            minutely15 = Minutely15Data(
                time = listOf("2026-01-01T00:00", "2026-01-01T00:15"),
                cape = listOf(100.0)
            )
        )
        val ex = assertThrows(OpenMeteoApiException::class.java) {
            invokeValidator(response)
        }
        assertEquals(422, ex.httpCode)
    }

    @Test
    fun `null or empty value arrays are skipped without false positives`() {
        val response = WeatherResponse(
            hourly = HourlyData(
                time = listOf("2026-01-01T00:00"),
                temperature2m = emptyList(),
                windSpeed10m = null
            )
        )
        invokeValidator(response) // no exception
    }

    @Test
    fun `empty time array skips validation entirely`() {
        val response = WeatherResponse(
            hourly = HourlyData(time = emptyList(), temperature2m = listOf(1.0))
        )
        invokeValidator(response) // no exception
    }

    @Test
    fun `all sections null is valid`() {
        invokeValidator(WeatherResponse()) // no exception
    }

    // =========================================================================
    // §21/§34 — WMO unknown code fallback + §34 malformed JSON / missing sections
    // =========================================================================

    @Test
    fun `unknown WMO code maps to unknown description without crashing`() {
        // Codes not in the documented WMO table (skill §21)
        assertEquals(
            "وضعیت نامشخص جوی",
            MountaineeringHelper.getWeatherCodeDescriptionPersian(999)
        )
        assertEquals(
            "وضعیت نامشخص جوی",
            MountaineeringHelper.getWeatherCodeDescriptionPersian(-5)
        )
    }

    @Test
    fun `response with missing current and minutely_15 sections parses cleanly`() {
        val json = """
            {
              "latitude": 35.9, "longitude": 52.1, "elevation": 5610,
              "timezone": "Asia/Tehran",
              "hourly": {
                "time": ["2026-01-01T00:00"],
                "temperature_2m": [8.5]
              }
            }
        """.trimIndent()
        val dto = responseAdapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(5610.0, dto?.elevation!!, 0.01)
        assertEquals(1, dto?.hourly?.time?.size)
        assertEquals(null, dto?.current)
        assertEquals(null, dto?.minutely15)
    }

    @Test
    fun `malformed JSON throws parse error instead of silent data loss`() {
        val json = """{"latitude": 35.9, "hourly": {"time": ["2026-01-01T00:00"], "temperature_2m": [8.5"""
        try {
            responseAdapter.fromJson(json)
            fail("Expected a JSON parse exception for truncated input")
        } catch (expected: Exception) {
            // Moshi surfaces JsonDataException/JsonEncodingException; any parse
            // failure is acceptable per skill §34 — it must NOT be swallowed.
            assertNotNull(expected)
        }
    }

    @Test
    fun `mismatched arrays in raw JSON are rejected by the validator`() {
        val json = """
            {
              "latitude": 35.9, "longitude": 52.1,
              "hourly": {
                "time": ["2026-01-01T00:00", "2026-01-01T01:00"],
                "temperature_2m": [8.5]
              }
            }
        """.trimIndent()
        val dto = responseAdapter.fromJson(json)!!
        val ex = assertThrows(OpenMeteoApiException::class.java) {
            invokeValidator(dto)
        }
        assertEquals(422, ex.httpCode)
    }

    // =========================================================================
    // §22 rule 7 / §12 — request parameter validation (date/hour formats, hours > 0)
    // =========================================================================

    private fun apiForRequestTests() = retrofit2.Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .build()
        .create(com.example.data.remote.OpenMeteoApiService::class.java)

    /** Endpoint that refuses connections instantly and deterministically (port 1 on loopback). */
    private fun apiFailingFast() = retrofit2.Retrofit.Builder()
        .baseUrl("http://127.0.0.1:1/")
        .build()
        .create(com.example.data.remote.OpenMeteoApiService::class.java)

    private suspend fun rejectsWith(messagePart: String, block: suspend (WeatherRepository) -> Unit) {
        try {
            block(WeatherRepository(apiForRequestTests(), apiForRequestTests()))
            fail("Expected request to be rejected ($messagePart)")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "expected message containing '$messagePart' but was: ${e.message}",
                (e.message ?: "").contains(messagePart)
            )
        }
    }

    @Test
    fun `past_hours zero or negative is rejected`() = kotlinx.coroutines.runBlocking {
        // require() checks run before any network I/O, so no request is attempted
        rejectsWith("past_hours") { it.fetchWeatherForecast(lat = 35.9, lng = 52.1, pastHours = 0) }
        rejectsWith("past_hours") { it.fetchWeatherForecast(lat = 35.9, lng = 52.1, pastHours = -24) }
    }

    @Test
    fun `forecast_hours zero or negative is rejected`() = kotlinx.coroutines.runBlocking {
        rejectsWith("forecast_hours") {
            it.fetchWeatherForecast(lat = 35.9, lng = 52.1, forecastHours = 0)
        }
    }

    @Test
    fun `start_date with invalid format is rejected`() = kotlinx.coroutines.runBlocking {
        rejectsWith("start_date") {
            it.fetchWeatherForecast(lat = 35.9, lng = 52.1, startDate = "01-02-2026")
        }
    }

    @Test
    fun `end_date with invalid format is rejected`() = kotlinx.coroutines.runBlocking {
        rejectsWith("end_date") {
            it.fetchWeatherForecast(lat = 35.9, lng = 52.1, endDate = "2026/01/05")
        }
    }

    @Test
    fun `start_hour without time part is rejected`() = kotlinx.coroutines.runBlocking {
        rejectsWith("start_hour") {
            it.fetchWeatherForecast(lat = 35.9, lng = 52.1, startHour = "2026-01-01")
        }
    }

    @Test
    fun `end_hour missing minutes is rejected`() = kotlinx.coroutines.runBlocking {
        rejectsWith("end_hour") {
            it.fetchWeatherForecast(lat = 35.9, lng = 52.1, endHour = "2026-01-01T14")
        }
    }

    @Test
    fun `valid documented formats pass request validation and reach transport`() = kotlinx.coroutines.runBlocking {
        // pastHours=24 mirrors the production call in WeatherViewModel. If the
        // format checks wrongly rejected, we would see IllegalArgumentException;
        // reaching the loopback transport error proves validation passed.
        try {
            WeatherRepository(apiFailingFast(), apiFailingFast()).fetchWeatherForecast(
                lat = 35.9,
                lng = 52.1,
                forecastDays = 3,
                pastHours = 24,
                startHour = "2026-01-01T00:00",
                endHour = "2026-01-01T23:00"
            )
            fail("expected a transport error from the refusing loopback endpoint")
        } catch (e: IllegalArgumentException) {
            fail("request validation rejected a valid request: ${e.message}")
        } catch (expected: Exception) {
            // Expected: validation passed; transport to 127.0.0.1:1 was refused.
        }
    }
}
