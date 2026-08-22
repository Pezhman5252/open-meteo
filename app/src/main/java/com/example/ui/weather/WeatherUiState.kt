package com.example.ui.weather

import com.example.data.local.MountainEntity
import com.example.data.remote.WeatherResponse

enum class WeatherErrorType {
    NO_INTERNET,   // عدم دسترسی به شبکه
    TIMEOUT,       // زمان اتصال به پایان رسید
    SERVER_ERROR,  // اختلال سرور ابری
    UNKNOWN        // خطای نامشخص
}

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val mountain: MountainEntity,
        val weather: WeatherResponse,
        val isOffline: Boolean = false,
        val offlineTime: String? = null
    ) : WeatherUiState
    data class Error(
        val message: String,
        val errorType: WeatherErrorType = WeatherErrorType.UNKNOWN
    ) : WeatherUiState
}
