package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("elevation") elevation: Double? = null,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timeformat") timeformat: String = "iso8601",
        @Query("past_days") pastDays: Int = 0,
        @Query("past_hours") pastHours: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("start_hour") startHour: String? = null,
        @Query("end_hour") endHour: String? = null,
        @Query("models") models: String? = null,
        @Query("cell_selection") cellSelection: String = "land",
        @Query("forecast_hours") forecastHours: Int? = null,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,snowfall,snow_depth,soil_temperature_0cm,cloud_cover_low,cloud_cover_mid,cloud_cover_high,weather_code,surface_pressure,pressure_msl,wind_speed_80m,wind_direction_80m,wind_speed_10m,wind_direction_10m,freezing_level_height,wind_gusts_10m,visibility,cloud_cover,is_day,dew_point_2m,cape,geopotential_height_500hPa,geopotential_height_700hPa,geopotential_height_850hPa",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,rain,showers,snowfall,snow_depth,soil_temperature_0cm,cloud_cover_low,cloud_cover_mid,cloud_cover_high,weather_code,surface_pressure,pressure_msl,wind_speed_80m,wind_direction_80m,wind_speed_10m,wind_direction_10m,freezing_level_height,wind_gusts_10m,visibility,cloud_cover,is_day,dew_point_2m,cape,evapotranspiration,et0_fao_evapotranspiration,shortwave_radiation,direct_radiation,diffuse_radiation,geopotential_height_500hPa,geopotential_height_700hPa,geopotential_height_850hPa",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,uv_index_max,uv_index_clear_sky_max,precipitation_probability_max,precipitation_probability_mean,precipitation_probability_min,precipitation_hours,wind_speed_10m_max,wind_direction_10m_dominant,wind_gusts_10m_max,precipitation_sum,rain_sum,showers_sum,snowfall_sum,sunshine_duration,daylight_duration,shortwave_radiation_sum,et0_fao_evapotranspiration",
        @Query("minutely_15") minutely15: String = "precipitation,snowfall,snowfall_height,freezing_level_height,lightning_potential,cape,wind_speed_10m,wind_gusts_10m,temperature_2m,apparent_temperature,visibility,weather_code",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("precipitation_unit") precipitationUnit: String = "mm",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}