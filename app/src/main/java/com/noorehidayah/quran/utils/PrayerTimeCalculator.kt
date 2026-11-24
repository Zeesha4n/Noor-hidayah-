package com.noorehidayah.quran.utils

import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.CityLocation
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class DailyPrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

/**
 * Pure-Kotlin 100% offline astronomical Prayer Times calculator.
 * No internet or GPS permission required—works immediately with built-in cities.
 */
object PrayerTimeCalculator {

    val builtInCities = listOf(
        CityLocation("New Delhi / NCR", 28.6139, 77.2090, 5.5),
        CityLocation("Greater Noida", 28.4744, 77.5040, 5.5),
        CityLocation("Mumbai", 19.0760, 72.8777, 5.5),
        CityLocation("Hyderabad", 17.3850, 78.4867, 5.5),
        CityLocation("Lucknow", 26.8467, 80.9462, 5.5),
        CityLocation("Bengaluru", 12.9716, 77.5946, 5.5),
        CityLocation("Kolkata", 22.5726, 88.3639, 5.5),
        CityLocation("Srinagar", 34.0837, 74.7973, 5.5),
        CityLocation("Patna", 25.5941, 85.1376, 5.5),
        CityLocation("Jaipur", 26.9124, 75.7873, 5.5),
        CityLocation("Bhopal", 23.2599, 77.4126, 5.5),
        CityLocation("Makkah", 21.4225, 39.8262, 3.0),
        CityLocation("Madinah", 24.4672, 39.6112, 3.0),
        CityLocation("Dubai", 25.2048, 55.2708, 4.0),
        CityLocation("London", 51.5074, -0.1278, 0.0)
    )

    fun calculate(
        city: CityLocation,
        method: PrayerCalcMethod,
        asrMethod: AsrMethod,
        calendar: Calendar = Calendar.getInstance()
    ): DailyPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (decl, eqTime) = sunPosition(jd + 0.5)

        val dhuhr = fixHour(12.0 + city.timezoneOffsetHours - city.longitude / 15.0 - eqTime)
        val sunrise = dhuhr - hourAngle(0.833, city.latitude, decl)
        val maghrib = dhuhr + hourAngle(0.833, city.latitude, decl)
        val fajr = dhuhr - hourAngle(method.fajrAngle, city.latitude, decl)
        val isha = dhuhr + hourAngle(method.ishaAngle, city.latitude, decl)
        val asr = dhuhr + asrTimeOffset(asrMethod.shadowFactor, city.latitude, decl)

        return DailyPrayerTimes(
            fajr = formatTime(fajr),
            sunrise = formatTime(sunrise),
            dhuhr = formatTime(dhuhr),
            asr = formatTime(asr),
            maghrib = formatTime(maghrib),
            isha = formatTime(isha)
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = radToDeg(atan2(cosDeg(e) * sinDeg(l), cosDeg(l))) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = radToDeg(asin(sinDeg(e) * sinDeg(l)))
        return decl to eqTime
    }

    private fun hourAngle(angle: Double, lat: Double, decl: Double): Double {
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return radToDeg(acos(ratio)) / 15.0
    }

    private fun asrTimeOffset(factor: Double, lat: Double, decl: Double): Double {
        val angle = -radToDeg(atan(1.0 / (factor + tanDeg(abs(lat - decl)))))
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return radToDeg(acos(ratio)) / 15.0
    }

    private fun formatTime(hoursInput: Double): String {
        val totalMinutes = ((fixHour(hoursInput) * 60.0) + 0.5).toInt()
        val h24 = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        val amPm = if (h24 >= 12) "PM" else "AM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return "%d:%02d %s".format(h12, m, amPm)
    }

    private fun degToRad(d: Double) = d * PI / 180.0
    private fun radToDeg(r: Double) = r * 180.0 / PI
    private fun sinDeg(d: Double) = sin(degToRad(d))
    private fun cosDeg(d: Double) = cos(degToRad(d))
    private fun tanDeg(d: Double) = tan(degToRad(d))
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double): Double = h - 24.0 * floor(h / 24.0)
}
