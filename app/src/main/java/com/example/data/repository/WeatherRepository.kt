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
        // Open-Meteo skill §22 rule 6: past_days valid range is 0..92 per the documented API limits
        require(pastDays in 0..92) { "past_days must be between 0 and 92" }
        // Open-Meteo skill §12: forecast_hours / past_hours are integer values greater than zero
        require(forecastHours == null || forecastHours > 0) { "forecast_hours must be greater than 0" }
        require(pastHours == null || pastHours > 0) { "past_hours must be greater than 0" }
        // Open-Meteo skill §22 rule 7 / §12: explicit date/hour ranges must use the
        // documented ISO-8601 formats (date: yyyy-mm-dd, hour: yyyy-mm-ddThh:mm)
        val isoDate = Regex("""^\d{4}-\d{2}-\d{2}$""")
        val isoHour = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$""")
        require(startDate == null || isoDate.matches(startDate)) { "start_date must use yyyy-mm-dd format" }
        require(endDate == null || isoDate.matches(endDate)) { "end_date must use yyyy-mm-dd format" }
        require(startHour == null || isoHour.matches(startHour)) { "start_hour must use yyyy-mm-ddThh:mm format" }
        require(endHour == null || isoHour.matches(endHour)) { "end_hour must use yyyy-mm-ddThh:mm format" }
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

        // Open-Meteo skill §30: validate parallel-array alignment before returning
        // the response. All time and value arrays must have identical lengths; a
        // mismatch means the response is corrupt/invalid and must be rejected
        // (not silently truncated). Violation is surfaced as an API exception so
        // callers can fall back to cache / show an error instead of corrupt data.
        validateResponseArrayAlignment(body)

        return body
    }

    /**
     * Validates that every present, non-empty parallel value array is aligned
     * with its section's `time` array (Open-Meteo skill §30 / §17). Arrays that
     * are absent or empty for a given model combo are skipped; only a genuine
     * length mismatch is treated as an invalid response.
     *
     * @throws OpenMeteoApiException with httpCode 422 when any array is misaligned
     */
    private fun validateResponseArrayAlignment(response: WeatherResponse) {
        fun checkSection(
            section: String,
            time: List<String>?,
            vararg fields: Pair<String, List<*>?>
        ) {
            if (time == null || time.isEmpty()) return
            for ((name, values) in fields) {
                if (values == null || values.isEmpty()) continue
                if (values.size != time.size) {
                    throw OpenMeteoApiException(
                        httpCode = 422,
                        apiReason = "پاسخ نامعتبر: آرایه‌ی '$name' در بخش $section دارای " +
                            "${values.size} عنصر است در حالی که 'time' دارای ${time.size} عنصر است. " +
                            "داده‌ها هم‌تراز نیستند و پاسخ باطل تلقی شد."
                    )
                }
            }
        }

        checkSection(
            "hourly",
            response.hourly?.time,
            "temperature_2m" to response.hourly?.temperature2m,
            "relative_humidity_2m" to response.hourly?.relativeHumidity2m,
            "apparent_temperature" to response.hourly?.apparentTemperature,
            "precipitation" to response.hourly?.precipitation,
            "rain" to response.hourly?.rain,
            "showers" to response.hourly?.showers,
            "snowfall" to response.hourly?.snowfall,
            "weather_code" to response.hourly?.weatherCode,
            "wind_speed_10m" to response.hourly?.windSpeed10m,
            "wind_direction_10m" to response.hourly?.windDirection10m,
            "wind_speed_80m" to response.hourly?.windSpeed80m,
            "wind_direction_80m" to response.hourly?.windDirection80m,
            "surface_pressure" to response.hourly?.surfacePressure,
            "pressure_msl" to response.hourly?.pressureMsl,
            "freezing_level_height" to response.hourly?.freezingLevelHeight,
            "wind_gusts_10m" to response.hourly?.windGusts10m,
            "visibility" to response.hourly?.visibility,
            "cloud_cover" to response.hourly?.cloudCover,
            "is_day" to response.hourly?.isDay,
            "dew_point_2m" to response.hourly?.dewPoint2m,
            "cape" to response.hourly?.cape,
            "shortwave_radiation" to response.hourly?.shortwaveRadiation,
            "evapotranspiration" to response.hourly?.evapotranspiration,
            "et0_fao_evapotranspiration" to response.hourly?.et0FaoEvapotranspiration,
            "direct_radiation" to response.hourly?.directRadiation,
            "diffuse_radiation" to response.hourly?.diffuseRadiation,
            "geopotential_height_500hPa" to response.hourly?.geopotentialHeight500hPa,
            "geopotential_height_700hPa" to response.hourly?.geopotentialHeight700hPa,
            "geopotential_height_850hPa" to response.hourly?.geopotentialHeight850hPa,
            "snow_depth" to response.hourly?.snowDepth,
            "soil_temperature_0cm" to response.hourly?.soilTemperature0cm,
            "cloud_cover_low" to response.hourly?.cloudCoverLow,
            "cloud_cover_mid" to response.hourly?.cloudCoverMid,
            "cloud_cover_high" to response.hourly?.cloudCoverHigh
        )

        checkSection(
            "daily",
            response.daily?.time,
            "weather_code" to response.daily?.weatherCode,
            "temperature_2m_max" to response.daily?.temperature2mMax,
            "temperature_2m_min" to response.daily?.temperature2mMin,
            "sunrise" to response.daily?.sunrise,
            "sunset" to response.daily?.sunset,
            "uv_index_max" to response.daily?.uvIndexMax,
            "uv_index_clear_sky_max" to response.daily?.uvIndexClearSkyMax,
            "precipitation_probability_max" to response.daily?.precipitationProbabilityMax,
            "precipitation_probability_mean" to response.daily?.precipitationProbabilityMean,
            "precipitation_probability_min" to response.daily?.precipitationProbabilityMin,
            "precipitation_hours" to response.daily?.precipitationHours,
            "wind_speed_10m_max" to response.daily?.windSpeed10mMax,
            "wind_direction_10m_dominant" to response.daily?.windDirection10mDominant,
            "wind_gusts_10m_max" to response.daily?.windGusts10mMax,
            "precipitation_sum" to response.daily?.precipitationSum,
            "rain_sum" to response.daily?.rainSum,
            "showers_sum" to response.daily?.showersSum,
            "snowfall_sum" to response.daily?.snowfallSum,
            "sunshine_duration" to response.daily?.sunshineDuration,
            "daylight_duration" to response.daily?.daylightDuration,
            "shortwave_radiation_sum" to response.daily?.shortwaveRadiationSum,
            "et0_fao_evapotranspiration" to response.daily?.et0FaoEvapotranspiration
        )

        checkSection(
            "minutely_15",
            response.minutely15?.time,
            "precipitation" to response.minutely15?.precipitation,
            "snowfall" to response.minutely15?.snowfall,
            "snowfall_height" to response.minutely15?.snowfallHeight,
            "freezing_level_height" to response.minutely15?.freezingLevelHeight,
            "lightning_potential" to response.minutely15?.lightningPotential,
            "cape" to response.minutely15?.cape,
            "wind_speed_10m" to response.minutely15?.windSpeed10m,
            "wind_gusts_10m" to response.minutely15?.windGusts10m,
            "temperature_2m" to response.minutely15?.temperature2m,
            "apparent_temperature" to response.minutely15?.apparentTemperature,
            "visibility" to response.minutely15?.visibility,
            "weather_code" to response.minutely15?.weatherCode
        )
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
