package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PersianDateHelper {

    fun formatToPersianDigits(text: String): String {
        return text.replace('0', '۰')
            .replace('1', '۱')
            .replace('2', '۲')
            .replace('3', '۳')
            .replace('4', '۴')
            .replace('5', '۵')
            .replace('6', '۶')
            .replace('7', '۷')
            .replace('8', '۸')
            .replace('9', '۹')
    }

    fun formatToPersianDigits(number: Number): String {
        val str = if (number is Double || number is Float) {
            String.format(Locale.US, "%.1f", number.toDouble())
        } else {
            number.toString()
        }
        return formatToPersianDigits(str)
    }

    fun getPersianDayOfWeek(isoDateString: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = format.parse(isoDateString) ?: return ""
            getPersianDayOfWeek(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun getPersianDayOfWeek(date: Date): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.time = date
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنجشنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getJalaliDateString(isoDateString: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = format.parse(isoDateString) ?: return ""
            getJalaliDateString(date)
        } catch (e: Exception) {
            isoDateString
        }
    }

    fun getJalaliDateString(date: Date): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.time = date
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        val monthName = getJalaliMonthName(jalali.month)
        return formatToPersianDigits("${jalali.day} $monthName ${jalali.year}")
    }

    fun getJalaliNumericDateString(isoDateString: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = format.parse(isoDateString) ?: return ""
            getJalaliNumericDateString(date)
        } catch (e: Exception) {
            isoDateString
        }
    }

    fun getJalaliNumericDateString(date: Date): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.time = date
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        val monthStr = String.format(Locale.US, "%02d", jalali.month)
        val dayStr = String.format(Locale.US, "%02d", jalali.day)
        return formatToPersianDigits("${jalali.year}/$monthStr/$dayStr")
    }

    fun getPersianDateTimeString(date: Date): String {
        val jalali = getJalaliNumericDateString(date)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        timeFormat.timeZone = TimeZone.getTimeZone("Asia/Tehran")
        val timeStr = formatToPersianDigits(timeFormat.format(date))
        return "$jalali - $timeStr"
    }

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (isLeapYearGregorian(gy)) {
            gDaysInMonth[2] = 29
        }
        var gDayNo = 0
        for (i in 1 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        gDayNo += gd

        var march21DayNo = 80
        if (isLeapYearGregorian(gy)) {
            march21DayNo = 81
        }

        var jyResult = gy - 621
        var jmResult = 0
        var jdResult = 0

        if (gDayNo >= march21DayNo) {
            var offset = gDayNo - march21DayNo
            if (offset < 186) {
                jmResult = (offset / 31) + 1
                jdResult = (offset % 31) + 1
            } else {
                offset -= 186
                jmResult = (offset / 30) + 7
                jdResult = (offset % 30) + 1
            }
        } else {
            jyResult -= 1
            var offset = gDayNo + (if (isLeapYearGregorian(gy - 1)) 366 else 365) - march21DayNo
            if (offset < 186) {
                jmResult = (offset / 31) + 1
                jdResult = (offset % 31) + 1
            } else {
                offset -= 186
                jmResult = (offset / 30) + 7
                jdResult = (offset % 30) + 1
            }
        }

        return JalaliDate(jyResult, jmResult, jdResult)
    }

    private fun isLeapYearGregorian(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun getJalaliMonthName(month: Int): String {
        return when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
    }

    /**
     * Calculates astronomical sunrise and sunset for a given coordinate and altitude.
     * Returns Pair of "HH:mm" in English digits, which can be formatted to Persian.
     */
    fun calculateSunriseSunset(
        latitude: Double,
        longitude: Double,
        elevation: Double,
        date: Date
    ): Pair<String, String> {
        val sunTimes = AstronomicalCalculator.calculateSunriseSunsetUTC(latitude, longitude, elevation, date)
        if (sunTimes.isAlwaysAbove) {
            return Pair("00:00", "23:59")
        }
        if (sunTimes.isAlwaysBelow) {
            return Pair("--:--", "--:--")
        }
        val peakOffsetHours = AstronomicalCalculator.getStandardTimezoneOffset("", latitude, longitude)
        val localRise = sunTimes.sunriseUTC?.let { (it + peakOffsetHours) % 24.0 }
        val localSet = sunTimes.sunsetUTC?.let { (it + peakOffsetHours) % 24.0 }
        
        val riseStr = AstronomicalCalculator.formatFractionalHour(localRise)
        val setStr = AstronomicalCalculator.formatFractionalHour(localSet)
        return Pair(riseStr, setStr)
    }

    fun normalizePersianDigits(text: String): String {
        var result = text
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            result = result.replace(persianDigits[i], (i + '0'.code).toChar())
            result = result.replace(arabicDigits[i], (i + '0'.code).toChar())
        }
        result = result.replace('٫', '.') // Normalize Arabic/Persian decimal separator
        return result.trim()
    }

    fun formatIsoDateTimeToPersian(isoDateTime: String): String {
        if (isoDateTime.isBlank()) return ""
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (format in formats) {
            try {
                val date = format.parse(isoDateTime)
                if (date != null) {
                    return getPersianDateTimeString(date)
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return formatToPersianDigits(isoDateTime) // fallback
    }
}
