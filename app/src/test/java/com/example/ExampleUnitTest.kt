package com.example

import com.example.ui.util.AstronomicalCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPersianDateConversion() {
        // July 20, 2026 should be 29 Tir 1405
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.set(2026, Calendar.JULY, 20, 12, 0, 0)
        val date = cal.time
        
        val jalaliStr = com.example.ui.util.PersianDateHelper.getJalaliDateString(date)
        // Since we format it with Persian digits, it should contain "۲۹ تیر ۱۴۰۵"
        assertTrue("Expected 29 Tir 1405 but got: $jalaliStr", jalaliStr.contains("۲۹") && jalaliStr.contains("تیر") && jalaliStr.contains("۱۴۰۵"))
    }

    @Test
    fun testSunriseSunsetDamavand() {
        // Damavand, Iran
        val latitude = 35.9551
        val longitude = 52.1097
        val elevation = 5610.0

        // June 11, 2026
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.JUNE, 11, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val date = cal.time

        val sunTimes = AstronomicalCalculator.calculateSunriseSunsetUTC(latitude, longitude, elevation, date)
        assertNotNull(sunTimes.sunriseUTC)
        assertNotNull(sunTimes.sunsetUTC)

        // Sunrise is early in June (UT morning)
        assertTrue(sunTimes.sunriseUTC!! in 0.0..12.0)
        // Sunset is late in June (UT afternoon/evening)
        assertTrue(sunTimes.sunsetUTC!! in 12.0..24.0)

        // Sunrise should be earlier at high altitude due to elevation dip
        val seaLevelSunTimes = AstronomicalCalculator.calculateSunriseSunsetUTC(latitude, longitude, 0.0, date)
        // Sunrise at 5610m should be earlier than at 0m
        assertTrue(sunTimes.sunriseUTC!! < seaLevelSunTimes.sunriseUTC!!)
        // Sunset at 5610m should be later than at 0m
        assertTrue(sunTimes.sunsetUTC!! > seaLevelSunTimes.sunsetUTC!!)
    }

    @Test
    fun testMoonriseSunsetDamavand() {
        val latitude = 35.9551
        val longitude = 52.1097
        val elevation = 5610.0

        // July 1, 2026
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.JULY, 1, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val date = cal.time

        val moonTimes = AstronomicalCalculator.calculateMoonriseSunsetUTC(latitude, longitude, elevation, date)
        
        // Assert that rise and set are correctly found (or non-null)
        if (!moonTimes.isAlwaysAbove && !moonTimes.isAlwaysBelow) {
            if (moonTimes.moonriseUTC != null) {
                assertTrue(moonTimes.moonriseUTC in 0.0..24.0)
            }
            if (moonTimes.moonsetUTC != null) {
                assertTrue(moonTimes.moonsetUTC in 0.0..24.0)
            }
        }
    }
}
