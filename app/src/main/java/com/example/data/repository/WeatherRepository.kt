package com.example.data.repository

import android.util.Log
import com.example.data.remote.OpenMeteoApiService
import com.example.data.remote.RetrofitHelper
import com.example.data.remote.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val apiService: OpenMeteoApiService,
    private val fallbackApiService: OpenMeteoApiService = RetrofitHelper.directApiService
) {
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
        // بنابراین این مقدار همیشه به‌صورت رشته به سرور ارسال می‌شود.
        val elevationParam: String? = when {
            disableDownscaling -> "nan"
            elevation != null -> elevation.toString()
            else -> null
        }

        val response = try {
            apiService.getForecast(
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
        } catch (primaryErr: Throwable) {
            Log.w("WeatherRepository", "Primary proxy request failed (${primaryErr.message}). Activating seamless direct Open-Meteo failover...")
            try {
                fallbackApiService.getForecast(
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
            } catch (fallbackErr: Throwable) {
                Log.e("WeatherRepository", "Both primary proxy and direct Open-Meteo requests failed: ${fallbackErr.message}")
                throw fallbackErr
            }
        }
        Log.i("WeatherRepository", "Weather response received successfully. Units: [snowfall=${response.hourlyUnits?.snowfall}, snow_depth=${response.hourlyUnits?.snowDepth}, precipitation=${response.hourlyUnits?.precipitation}]")
        Log.d("WeatherRepository", "API generation time: ${response.generationtimeMs} ms")
        response
    }
}

