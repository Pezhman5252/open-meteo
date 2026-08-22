package com.example

import com.example.data.remote.CurrentWeather
import com.example.data.remote.DailyData
import com.example.data.remote.HourlyData
import com.example.data.remote.WeatherUnits
import com.example.ui.util.MountaineeringHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MountaineeringSafetyLogicTest {

    @Test
    fun testNormalizeSnowDepthCm_unitConversions() {
        // Meters to CM
        assertEquals(50.0, MountaineeringHelper.normalizeSnowDepthCm(0.5, "m"), 0.01)
        assertEquals(120.0, MountaineeringHelper.normalizeSnowDepthCm(1.2, "meters"), 0.01)

        // CM as is
        assertEquals(45.0, MountaineeringHelper.normalizeSnowDepthCm(45.0, "cm"), 0.01)

        // Inches to CM
        assertEquals(25.4, MountaineeringHelper.normalizeSnowDepthCm(10.0, "inch"), 0.01)

        // Default fallback when unit is null (defaults to meters in Open-Meteo)
        assertEquals(30.0, MountaineeringHelper.normalizeSnowDepthCm(0.3, null), 0.01)
        assertEquals(80.0, MountaineeringHelper.normalizeSnowDepthCm(0.8, null), 0.01)
    }

    @Test
    fun testNormalizeSnowfallCm_unitConversions() {
        assertEquals(2.5, MountaineeringHelper.normalizeSnowfallCm(25.0, "mm"), 0.01)
        assertEquals(15.0, MountaineeringHelper.normalizeSnowfallCm(15.0, "cm"), 0.01)
        assertEquals(100.0, MountaineeringHelper.normalizeSnowfallCm(1.0, "m"), 0.01)
    }

    @Test
    fun testCalculateBarometricPressure_ICAO_ISA() {
        // Sea level ISA pressure
        val seaLevelP = MountaineeringHelper.calculateBarometricPressure(
            basePressure = 1013.25,
            baseTemp = 15.0,
            baseAltitude = 0.0,
            targetAltitude = 0.0
        )
        assertEquals(1013.25, seaLevelP, 0.1)

        // High altitude pressure drop at 4000m (Damavand / High Peak level)
        val pressure4000m = MountaineeringHelper.calculateBarometricPressure(
            basePressure = 1013.25,
            baseTemp = 15.0,
            baseAltitude = 0.0,
            targetAltitude = 4000.0
        )
        // Expected pressure at 4000m is ~615 hPa
        assertTrue("Pressure at 4000m should be between 580 and 640 hPa", pressure4000m in 580.0..640.0)

        // QNH Calibration test (e.g. low pressure system 995 hPa QNH)
        val lowPressureP = MountaineeringHelper.calculateBarometricPressure(
            basePressure = 995.0,
            baseTemp = 15.0,
            baseAltitude = 0.0,
            targetAltitude = 0.0,
            qnh = 995.0
        )
        assertEquals(995.0, lowPressureP, 0.1)
    }

    @Test
    fun testCalculateLightningRisk_dryLightningFallback() {
        // High CAPE + high cloud cover + summit above freezing level even with 0 rain and null lightningPotential
        val dryLightningRisk = MountaineeringHelper.calculateLightningRisk(
            cape = 1200.0,
            precipitation = 0.0,
            cloudCover = 85.0,
            freezingLevel = 2500.0,
            summitElevation = 3800.0,
            weatherCode = 2,
            lightningPotential = null
        )
        assertTrue("Dry lightning scenario should produce significant risk (>= 40%)", dryLightningRisk >= 40)
    }

    @Test
    fun testCalculateAvalancheRisk_boundaryScenarios() {
        // Dry slope without snow -> 0 risk
        val dryRisk = MountaineeringHelper.calculateAvalancheRisk(
            newSnow24h = 0.0,
            windSpeed = 10.0,
            tempChange24h = 0.0,
            slopeAngle = 35.0,
            aspect = "N",
            tempNow = 12.0,
            snowDepth = 0.0
        )
        assertEquals(0, dryRisk)

        // Critical avalanche scenario: 40cm new snow, 50km/h wind, 35 degree slope, temperature spike
        val criticalRisk = MountaineeringHelper.calculateAvalancheRisk(
            newSnow24h = 40.0,
            windSpeed = 50.0,
            tempChange24h = 8.0,
            slopeAngle = 35.0,
            aspect = "S",
            tempNow = 2.0,
            snowDepth = 60.0
        )
        assertTrue("Critical avalanche risk should be >= 60%", criticalRisk >= 60)
    }

    @Test
    fun testCalculateAvalancheRisk_soilTemperatureInsulationFallback() {
        // Test glide avalanche risk when soilTemp is null but snow cover is insulating ground
        val insulatedRisk = MountaineeringHelper.calculateAvalancheRisk(
            newSnow24h = 10.0,
            windSpeed = 20.0,
            tempChange24h = 2.0,
            slopeAngle = 32.0,
            aspect = "SE",
            tempNow = 1.5,
            snowDepth = 25.0,
            soilTemp = null // Null soil temp -> triggers insulating fallback (soil ~0.8C > 0.5C)
        )
        assertTrue("Insulated soil temp fallback should contribute to avalanche risk calculation", insulatedRisk > 15)
    }

    @Test
    fun testCalculateLightningRisk_CAPE_and_WMO() {
        // Zero risk when calm atmospheric conditions
        val calmRisk = MountaineeringHelper.calculateLightningRisk(
            cape = 10.0,
            precipitation = 0.0,
            cloudCover = 10.0,
            freezingLevel = 3500.0,
            summitElevation = 2000.0,
            weatherCode = 0
        )
        assertEquals(0, calmRisk)

        // Severe thunderstorm (WMO 95)
        val stormRisk = MountaineeringHelper.calculateLightningRisk(
            cape = 1800.0,
            precipitation = 8.0,
            cloudCover = 95.0,
            freezingLevel = 2500.0,
            summitElevation = 3000.0,
            weatherCode = 95
        )
        assertTrue("Severe thunderstorm lightning risk should be >= 70%", stormRisk >= 70)
    }

    @Test
    fun testCalculateWhiteoutRisk_fog_and_high_altitude() {
        // High risk in dense fog (WMO 45) with low visibility
        val fogRisk = MountaineeringHelper.calculateWhiteoutRisk(
            visibility = 100.0,
            windSpeed = 35.0,
            snowfall = 1.0,
            cloudCoverLow = 90.0,
            weatherCode = 45,
            altitude = 3800
        )
        assertTrue("Dense fog and blowing snow whiteout risk should be >= 75%", fogRisk >= 75)
    }

    @Test
    fun testCalculateWhiteoutRisk_midCloud_high_altitude_immersion() {
        // High mountain summit (4200m) immersed in dense mid-level cloud deck (cloudCoverMid = 90%)
        // even when low-level cloud cover is 10% in lower valleys
        val summitRisk = MountaineeringHelper.calculateWhiteoutRisk(
            visibility = 200.0,
            windSpeed = 30.0,
            snowfall = 0.0,
            cloudCoverLow = 10.0,
            cloudCoverTotal = 90.0,
            altitude = 4200,
            cloudCoverMid = 90.0
        )
        assertTrue("High altitude summit immersed in mid clouds should have high whiteout/fog risk >= 60%", summitRisk >= 60)
    }

    @Test
    fun testCalculateWindRisk_high_altitude_thresholds() {
        // Wind speed 40km/h at 3800m altitude (high altitude sensitivity)
        val highAltWindRisk = MountaineeringHelper.calculateWindRisk(
            windSpeed = 40.0,
            windGusts = 60.0,
            altitude = 3800
        )
        assertTrue("High altitude wind risk at 3800m should be >= 70%", highAltWindRisk >= 70)
    }

    @Test
    fun testEvaluateSafety_compositeReport() {
        val current = CurrentWeather(
            time = "2026-08-13T12:00",
            temperature2m = -5.0,
            relativeHumidity2m = 85.0,
            windSpeed10m = 35.0,
            windSpeed80m = 50.0,
            snowfall = 1.5,
            snowDepth = 0.4, // 0.4 meters = 40 cm
            weatherCode = 73,
            cloudCover = 90.0,
            surfacePressure = 650.0,
            cape = 250.0
        )

        val hourly = HourlyData(
            time = listOf("2026-08-13T12:00"),
            temperature2m = listOf(-5.0),
            snowfall = listOf(1.5),
            snowDepth = listOf(0.4)
        )

        val units = WeatherUnits(
            snowDepth = "m",
            snowfall = "cm"
        )

        val report = MountaineeringHelper.evaluateSafety(
            current = current,
            hourly = hourly,
            altitudeOverride = 3500,
            units = units
        )

        assertTrue("Composite risk score should reflect winter mountain conditions", report.riskScore > 30)
        assertTrue("Hazards should be generated for winter conditions", report.environmentalHazards.isNotEmpty())
    }

    @Test
    fun testCalculateWindChill_lowSpeedThreshold() {
        // At wind speeds below 4.8 km/h (1.3 m/s), wind chill equals ambient temperature
        val ambientTemp = -10.0
        val chillLowWind = MountaineeringHelper.calculateWindChill(temp = ambientTemp, windSpeed = 3.0)
        assertEquals("Wind chill below 4.8 km/h must equal ambient temperature", ambientTemp, chillLowWind, 0.01)

        val chillModerateWind = MountaineeringHelper.calculateWindChill(temp = ambientTemp, windSpeed = 25.0)
        assertTrue("Wind chill at 25 km/h must be colder than ambient", chillModerateWind < ambientTemp)
    }

    @Test
    fun testMultiHazardSynergy_windAndWhiteoutEscalation() {
        // High wind + high whiteout risk simultaneously should produce an amplified composite risk
        val current = CurrentWeather(
            time = "2026-08-13T12:00",
            temperature2m = -4.0,
            relativeHumidity2m = 95.0,
            windSpeed10m = 32.0,
            windSpeed80m = 40.0,
            visibility = 250.0,
            weatherCode = 45, // Fog/Whiteout
            cloudCover = 100.0,
            cloudCoverLow = 95.0,
            surfacePressure = 680.0
        )

        val report = MountaineeringHelper.evaluateSafety(
            current = current,
            hourly = null,
            altitudeOverride = 3200
        )

        assertTrue("Multi-hazard synergy must result in high risk score (>= 65)", report.riskScore >= 65)
        assertTrue("Hazard list must include wind+whiteout synergy warning", report.environmentalHazards.any { it.contains("ترکیب پرخطر باد و وایت‌اوت") })
    }

    @Test
    fun testRapidPressureDrop3h_triggersHazardAlert() {
        val current = CurrentWeather(
            time = "2026-08-13T15:00",
            temperature2m = 2.0,
            surfacePressure = 700.0,
            windSpeed10m = 20.0
        )

        val hourly = HourlyData(
            time = listOf("2026-08-13T12:00", "2026-08-13T13:00", "2026-08-13T14:00", "2026-08-13T15:00"),
            temperature2m = listOf(3.0, 2.8, 2.5, 2.0),
            surfacePressure = listOf(706.0, 704.0, 702.0, 700.0) // 6 hPa drop in 3 hours
        )

        val report = MountaineeringHelper.evaluateSafety(
            current = current,
            hourly = hourly,
            altitudeOverride = 3000,
            hourIndexOverride = 3
        )

        assertTrue("Rapid pressure drop must trigger severe barometric drop hazard warning", report.environmentalHazards.any { it.contains("افت شدید فشار بارومتریک") })
    }

    @Test
    fun testExtremeCAPE_handlesGracefully() {
        val extremeCape = 7500.0
        val result = MountaineeringHelper.calculateLightningRisk(
            cape = extremeCape,
            precipitation = 10.0,
            cloudCover = 90.0,
            freezingLevel = 3000.0,
            summitElevation = 4000.0
        )
        assertTrue(result in 0..100)
        assertTrue(result >= 80)
    }

    @Test
    fun testExtremeWindAndSnowSynergy() {
        val wind = 70.0  // km/h
        val snow = 3.0   // cm/h
        val whiteoutRisk = MountaineeringHelper.calculateWhiteoutRisk(
            visibility = 100.0,
            windSpeed = wind,
            snowfall = snow,
            cloudCoverLow = 85.0,
            cloudCoverTotal = 95.0,
            altitude = 3500
        )
        assertTrue(whiteoutRisk >= 70)
    }

    @Test
    fun testFreezingLevelNegativeValue() {
        val current = CurrentWeather(
            time = "2026-08-15T10:00",
            temperature2m = -20.0,
            freezingLevelHeight = -500.0,
            windSpeed10m = 30.0,
            surfacePressure = 650.0
        )
        val result = MountaineeringHelper.evaluateSafety(
            current = current,
            hourly = null,
            altitudeOverride = 3000
        )
        assertNotNull(result)
        assertTrue(result.riskScore in 0..100)
    }

    @Test
    fun testCalculateUvRisk_WHO_piecewise_standards() {
        // Low: 0 to <3 (0-20%)
        assertEquals(0, MountaineeringHelper.calculateUvRisk(0.0))
        assertEquals(10, MountaineeringHelper.calculateUvRisk(1.5))
        assertEquals(20, MountaineeringHelper.calculateUvRisk(3.0))

        // Moderate: 3 to <6 (20-40%)
        assertEquals(30, MountaineeringHelper.calculateUvRisk(4.5))
        assertEquals(40, MountaineeringHelper.calculateUvRisk(6.0))

        // High: 6 to <8 (40-60%)
        assertEquals(50, MountaineeringHelper.calculateUvRisk(7.0))
        assertEquals(60, MountaineeringHelper.calculateUvRisk(8.0))

        // Very High: 8 to <11 (60-80%)
        assertEquals(70, MountaineeringHelper.calculateUvRisk(9.5))
        assertEquals(80, MountaineeringHelper.calculateUvRisk(11.0))

        // Extreme: 11+ (80-100%)
        assertEquals(90, MountaineeringHelper.calculateUvRisk(13.0))
        assertEquals(100, MountaineeringHelper.calculateUvRisk(15.0))
        assertEquals(100, MountaineeringHelper.calculateUvRisk(25.0))

        // Out of bounds / negative
        assertEquals(0, MountaineeringHelper.calculateUvRisk(-5.0))
    }

    @Test
    fun testCalculateResolvedUvIndex_standardsAndMultiTierFallbacks() {
        // 1. Night returns 0.0
        val nightCurrent = CurrentWeather(
            time = "2026-08-15T22:00",
            isDay = 0
        )
        val nightUv = MountaineeringHelper.calculateResolvedUvIndex(
            current = nightCurrent,
            hourly = null,
            altitude = 3000
        )
        assertEquals(0.0, nightUv, 0.01)

        // 2. Hourly shortwave radiation with elevation and snow factors
        val dayCurrent = CurrentWeather(
            time = "2026-08-15T12:00",
            isDay = 1
        )
        val hourlyWithSwRad = HourlyData(
            time = listOf("2026-08-15T05:00", "2026-08-15T06:00", "2026-08-15T12:00"),
            temperature2m = listOf(10.0, 12.0, 15.0),
            isDay = listOf(0, 1, 1),
            shortwaveRadiation = listOf(0.0, 50.0, 780.0),
            cloudCover = listOf(0.0, 0.0, 0.0)
        )
        // 05:00 Night/Pre-dawn => strictly 0.0
        val uvHour5 = MountaineeringHelper.calculateResolvedUvIndex(
            current = dayCurrent,
            hourly = hourlyWithSwRad,
            altitude = 5670,
            hourlyIndex = 0
        )
        assertEquals(0.0, uvHour5, 0.01)

        // 06:00 Dawn (50 W/m² => 0.5 base) at 5670m (diffElev = 0 because altitude=mountainAltitude defaults) => ~0.5
        val uvHour6 = MountaineeringHelper.calculateResolvedUvIndex(
            current = dayCurrent,
            hourly = hourlyWithSwRad,
            altitude = 5670,
            hourlyIndex = 1
        )
        assertTrue(uvHour6 in 0.4..0.6)

        // 12:00 Noon Peak (780 W/m² => 7.8 base) at 5670m (diffElev = 0) with snow (1.4x) => 7.8 * 1.4 ≈ 10.9
        val uvHour12WithSnow = MountaineeringHelper.calculateResolvedUvIndex(
            current = dayCurrent,
            hourly = hourlyWithSwRad,
            altitude = 5670,
            snowCover = true,
            hourlyIndex = 2
        )
        assertTrue(uvHour12WithSnow in 10.0..12.0)

        // 3. Fallback to daily max with diurnal curve
        val dailyWithUv = DailyData(
            time = listOf("2026-08-15"),
            temperature2mMax = listOf(20.0),
            temperature2mMin = listOf(10.0),
            uvIndexMax = listOf(8.0)
        )
        
        // 12:30 Local time (solar noon peak) -> should yield peak UV (1.0 factor)
        val currentWithOffset = CurrentWeather(
            time = "2026-08-15T12:30",
            isDay = 1
        )
        val uvWithOffset = MountaineeringHelper.calculateResolvedUvIndex(
            current = currentWithOffset,
            hourly = null,
            altitude = 3000,
            daily = dailyWithUv
        )
        // At 3000m: Elev factor = 1.3. Max UV = 8.0. Base = 8.0 * 1.0 (since local time is 12:00) => 8.0.
        // Result = 8.0 * 1.3 = 10.4
        assertEquals(10.4, uvWithOffset, 0.5)
        
        val resolvedFromDaily = MountaineeringHelper.calculateResolvedUvIndex(
            current = dayCurrent,
            hourly = null,
            daily = dailyWithUv,
            altitude = 0,
            snowCover = false
        )
        assertTrue(resolvedFromDaily > 0.0)
    }

    @Test
    fun testUvIndexWithOffsetHours_ProvesTimezoneAutoIsLocalTime() {
        val dailyWithUv = DailyData(
            time = listOf("2026-08-15"),
            temperature2mMax = listOf(20.0),
            temperature2mMin = listOf(10.0),
            uvIndexMax = listOf(8.0)
        )
        
        // As Open-Meteo with timezone=auto returns LOCAL TIME, the time string is already local.
        // For example, 12:30 Local (solar noon)
        val currentLocalNoon = CurrentWeather(
            time = "2026-08-15T12:30",
            isDay = 1
        )
        val uv = MountaineeringHelper.calculateResolvedUvIndex(
            current = currentLocalNoon,
            hourly = null,
            altitude = 3000,
            daily = dailyWithUv,
            offsetHours = 3.5 // Provided, but should not double-offset the already local time
        )
        
        // At 3000m: Elev factor = 1.0 + (0.10 * 3000/1000) = 1.3
        // Max UV = 8.0. Base = 8.0 * 1.0 (since local time is 12:30 peak) => 8.0.
        // Expected ≈ 8.0 * 1.3 = 10.4
        assertEquals(10.4, uv, 0.5)
    }
}
