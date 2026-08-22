package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object WeatherCodeHelper {

    fun getDescription(code: Int, isDay: Int? = 1): String {
        val isNight = isDay == 0
        return when (code) {
            0 -> if (isNight) "آسمان کاملاً صاف و ستاره‌باران" else "آسمان کاملاً صاف و آفتابی"
            1 -> if (isNight) "عمدتاً صاف (ابر تکه‌ای پراکنده)" else "عمدتاً صاف تا کمی ابری"
            2 -> if (isNight) "نیمه ابری با کاهش دید شبانه" else "نیمه ابری (رشد ابرهای محلی)"
            3 -> "تمام ابری (اورکست پوششی کامل)"
            45 -> "مه‌گرفتگی کوهستان (کاهش عمق دید)"
            48 -> "مه یخ‌زن (خطر تشکیل لایه ریم و یخ)"
            51 -> "بارش ریزش نم‌نم (دریزل سبک)"
            53 -> "بارش دریزل متوسط (تداوم رطوبت)"
            55 -> "دریزل متراکم و اشباع رطوبت سطحی"
            56 -> "دریزل یخ‌زن سبک (تشکیل بلور یخ)"
            57 -> "دریزل یخ‌زن متراکم و خطر لغزش"
            61 -> "بارش ملایم باران کوهستانی"
            63 -> "بارش مداوم و متوسط باران"
            65 -> "بارش شدید باران و خطر سیلاب"
            66 -> "باران یخ‌زن ملایم (یخ‌بندان سطحی)"
            67 -> "باران یخ‌زن شدید (خطر حاد لغزندگی)"
            71 -> "بارش برف سبک (دانه‌های پراکنده)"
            73 -> "بارش برف متوسط و کوبنده"
            75 -> "بارش برف سنگین و خطر ریزش بهمن"
            77 -> "دانه‌های ریز برف (Snow grains)"
            80 -> "رگبار پراکنده و موقتی باران"
            81 -> "رگبار شدید باران کوهستانی"
            82 -> "رگبار سیل‌آسای باران (سیلاب آنی)"
            85 -> "رگبار ناگهانی و سبک برف"
            86 -> "رگبار شدید و کولاک موقتی برف"
            // کدهای ۸۷ و ۸۹ در جدول استاندارد WMO و Open-Meteo وجود ندارند (تگرگ منحصراً در کدهای ۹۶ و ۹۹ پشتیبانی می‌شود)
            // 87 -> "بارش تگرگ سبک (Slight hail)"
            // 89 -> "بارش تگرگ شدید (Heavy hail)"
            95 -> "طوفان تندری (صاعقه و رعدوبرق)"
            96 -> "طوفان تندری همراه با ریزش تگرگ سبک"
            99 -> "طوفان تندری حاد و بارش تگرگ سنگین"
            else -> "شرایط جوی متغیر و نامشخص"
        }
    }

    fun getIcon(code: Int, isDay: Int? = 1): ImageVector {
        val isNight = isDay == 0
        return when (code) {
            0 -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
            1, 2 -> if (isNight) Icons.Default.CloudQueue else Icons.Default.CloudQueue
            3 -> Icons.Default.Cloud
            45, 48 -> Icons.Default.FilterHdr
            51, 53, 55 -> Icons.Default.WaterDrop // Drizzle
            56, 57 -> Icons.Default.AcUnit // Freezing drizzle
            61, 63 -> Icons.Default.WaterDrop
            65 -> Icons.Default.Thunderstorm // Heavy rain
            66, 67 -> Icons.Default.AcUnit // Freezing rain
            71, 73 -> Icons.Default.AcUnit // Snow
            75 -> Icons.Default.SevereCold // Heavy snow
            77 -> Icons.Default.AcUnit // Snow grains
            80, 81, 82 -> Icons.Default.WaterDrop // Showers
            85, 86 -> Icons.Default.SevereCold // Snow showers
            // 87, 89 -> Icons.Default.Storm // Hail (Non-standard WMO codes)
            95 -> Icons.Default.Bolt // Thunderstorm
            96, 99 -> Icons.Default.Storm // Thunderstorm with hail
            else -> Icons.Default.Air
        }
    }
}

