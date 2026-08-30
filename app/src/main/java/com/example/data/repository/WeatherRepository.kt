package com.example.data.repository

import android.util.Log
import com.example.data.remote.OpenMeteoApiException
import com.example.data.remote.OpenMeteoApiService
import com.example.data.remote.OpenMeteoErrorDto
import com.example.data.remote.RetrofitHelper
import com.example.data.remote.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class WeatherRepository(
    private val apiService: OpenMeteoApiService,
    private val fallbackApiService: OpenMeteoApiService = RetrofitHelper.directApiService
) {
    private val errorMoshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    suspend fun fetchWeatherForecast(
        lat: Double,
        lng: Double,
        elevation: Double? = null,
        disableDownscaling: Boolean = false,
        forecastDays: Int = 3,
        timezone: String = "auto",
        windSpeedUnit: String = "kmh",
        precipitationUnit: String = "mm",
        temperatureUnit: String = "celsius",
        timeformat: String = "iso8601",
        models: String? = null,
        cellSelection: String = "land",
        forecastHours: Int? = null,
        pastDays: Int = 0,
        pastHours: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        startHour: String? = null,
        endHour: String? = null
    ): WeatherResponse = withContext(Dispatchers.IO) {
        require(lat in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
        require(lng in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
        // طبق مستندات Open-Meteo، بازه مجاز forecast_days بین ۰ تا ۱۶ است (۰ برای دریافت فقط داده‌های گذشته).
        require(forecastDays in 0..16) { "forecast_days must be between 0 and 16" }
        require(elevation == null || elevation.isNaN() || elevation in -500.0..9000.0) {
            "Elevation must be between -500 and 9000 meters or NaN to disable downscaling"
        }

        // Open-Meteo برای غیرفعال‌سازی کاهش مقیاس ارتفاعی (downscaling) دقیقاً رشته "nan" را می‌پذیرد.
        // Double.NaN به شکل "NaN" (با حروف بزرگ) سریالایز می‌شود که API آن را به عنوان مقدار معتبر تشخیص نمی‌دهد؛
        // بنابراین هر بار که downscaling غیرفعال است یا مقدار NaN ارسال می‌شود، مقدار "nan" به‌صورت رشته ارسال می‌گردد.
        val elevationParam: String? = when {
            disableDownscaling || elevation?.isNaN() == true -> "nan"
            elevation != null -> elevation.toString()
            else -> null
        }

        val response = try {
            executeForecast(
                service = apiService,
                lat = lat,
                lng = lng,
                elevationParam = elevationParam,
                temperatureUnit = temperatureUnit,
                timeformat = timeformat,
                pastDays = pastDays,
                pastHours = pastHours,
                startDate = startDate,
                endDate = endDate,
                startHour = startHour,
                endHour = endHour,
                models = models,
                cellSelection = cellSelection,
                forecastHours = forecastHours,
                windSpeedUnit = windSpeedUnit,
                precipitationUnit = precipitationUnit,
                forecastDays = forecastDays,
                timezone = timezone
            )
        } catch (primaryErr: Throwable) {
            Log.w("WeatherRepository", "Primary proxy request failed (${primaryErr.message}). Activating seamless direct Open-Meteo failover...")
            try {
                executeForecast(
                    service = fallbackApiService,
                    lat = lat,
                    lng = lng,
                    elevationParam = elevationParam,
                    temperatureUnit = temperatureUnit,
                    timeformat = timeformat,
                    pastDays = pastDays,
                    pastHours = pastHours,
                    startDate = startDate,
                    endDate = endDate,
                    startHour = startHour,
                    endHour = endHour,
                    models = models,
                    cellSelection = cellSelection,
                    forecastHours = forecastHours,
                    windSpeedUnit = windSpeedUnit,
                    precipitationUnit = precipitationUnit,
                    forecastDays = forecastDays,
                    timezone = timezone
                )
            } catch (fallbackErr: Throwable) {
                Log.e("WeatherRepository", "Both primary proxy and direct Open-Meteo requests failed: ${fallbackErr.message}")
                throw fallbackErr
            }
        }
        Log.i("WeatherRepository", "Weather response received successfully. Units: [snowfall=${response.hourlyUnits?.snowfall}, snow_depth=${response.hourlyUnits?.snowDepth}, precipitation=${response.hourlyUnits?.precipitation}]")
        Log.d("WeatherRepository", "API generation time: ${response.generationtimeMs} ms")
        response
    }

    /**
     * Performs a single forecast request and maps the outcome to either a
     * [WeatherResponse] (HTTP 2xx with a body) or a typed [OpenMeteoApiException]
     * (non-2xx, empty body, or an Open-Meteo validation error body).
     *
     * Open-Meteo returns `HTTP 400` with a JSON error object of the shape
     * `{"error": true, "reason": "..."}` for invalid parameters. We parse that
     * reason and surface it through [OpenMeteoApiException] so callers can
     * distinguish an API validation failure from a transport or parsing failure.
     */
    private suspend fun executeForecast(
        service: OpenMeteoApiService,
        lat: Double,
        lng: Double,
        elevationParam: String?,
        temperatureUnit: String,
        timeformat: String,
        pastDays: Int,
        pastHours: Int?,
        startDate: String?,
        endDate: String?,
        startHour: String?,
        endHour: String?,
        models: String?,
        cellSelection: String,
        forecastHours: Int?,
        windSpeedUnit: String,
        precipitationUnit: String,
        forecastDays: Int,
        timezone: String
    ): WeatherResponse {
        val response: Response<WeatherResponse> = service.getForecast(
            latitude = lat.toString(),
            longitude = lng.toString(),
            elevation = elevationParam,
            temperatureUnit = temperatureUnit,
            timeformat = timeformat,
            pastDays = pastDays,
            pastHours = pastHours,
            startDate = startDate,
            endDate = endDate,
            startHour = startHour,
            endHour = endHour,
            models = models,
            cellSelection = cellSelection,
            forecastHours = forecastHours,
            windSpeedUnit = windSpeedUnit,
            precipitationUnit = precipitationUnit,
            forecastDays = forecastDays,
            timezone = timezone
        )

        if (!response.isSuccessful) {
            val apiReason = parseErrorReason(response)
            throw OpenMeteoApiException(httpCode = response.code(), apiReason = apiReason)
        }

        val body = response.body()
            ?: throw OpenMeteoApiException(
                httpCode = response.code(),
                apiReason = "Open-Meteo returned an empty response body"
            )

        return body
    }

    /**
     * Parses the Open-Meteo error JSON body (`{"error":true,"reason":"..."}`).
     * Returns null when the body is absent, unreadable, or not parseable so the
     * caller can still throw a typed exception without a reason string.
     */
    private fun parseErrorReason(response: Response<WeatherResponse>): String? {
        return try {
            val errorBody = response.errorBody()?.string() ?: return null
            if (errorBody.isBlank()) return null
            val adapter = errorMoshi.adapter(OpenMeteoErrorDto::class.java)
            adapter.fromJson(errorBody)?.reason
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Failed to parse Open-Meteo error body (HTTP ${response.code()}): ${e.message}")
            null
        }
    }
}
