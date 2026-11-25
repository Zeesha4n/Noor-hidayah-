package com.noorehidayah.quran.utils

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthName: String,
    val year: Int
)

data class IslamicEvent(
    val hijriMonth: Int,
    val hijriDay: Int,
    val name: String,
    val description: String
)

/**
 * Pure-Kotlin offline Hijri (Islamic) calendar converter & key Islamic dates.
 */
object HijriCalendarHelper {

    private val monthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val importantEvents = listOf(
        IslamicEvent(1, 1, "Islamic New Year", "1 Muharram — Start of Hijri Year"),
        IslamicEvent(1, 10, "Day of Ashura", "10 Muharram — Recommended fast"),
        IslamicEvent(3, 12, "Rabi' al-Awwal 12", "Birth month of Prophet Muhammad (PBUH)"),
        IslamicEvent(7, 27, "Al-Isra wal-Mi'raj", "27 Rajab — Night Journey"),
        IslamicEvent(8, 15, "Shab-e-Barat", "15 Sha'ban — Night of Forgiveness"),
        IslamicEvent(9, 1, "1st Ramadan", "Start of Fasting Month (Sehri & Iftar)"),
        IslamicEvent(9, 27, "Laylatul Qadr (Odd Nights)", "Last 10 nights of Ramadan"),
        IslamicEvent(10, 1, "Eid-ul-Fitr", "1 Shawwal — Festival of Breaking Fast"),
        IslamicEvent(12, 9, "Day of Arafah", "9 Dhu al-Hijjah — Recommended fast"),
        IslamicEvent(12, 10, "Eid-ul-Adha", "10 Dhu al-Hijjah — Festival of Sacrifice")
    )

    fun todayHijri(calendar: Calendar = Calendar.getInstance()): HijriDate {
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Day Number (Gregorian)
        val a = (14 - gMonth) / 12
        val y = gYear + 4800 - a
        val m = gMonth + 12 * a - 3
        val jdn = gDay + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        // Tabular Islamic Calendar conversion
        val l = jdn - 1948440 + 10632
        val n = floor((l - 1) / 10631.0).toInt()
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316.0).toInt()) * (floor((50 * l2) / 17719.0).toInt()) +
            (floor(l2 / 5670.0).toInt()) * (floor((43 * l2) / 15238.0).toInt())
        val l3 = l2 - (floor((30 - j) / 15.0).toInt()) * (floor((17719 * j) / 50.0).toInt()) -
            (floor(j / 16.0).toInt()) * (floor((15238 * j) / 43.0).toInt()) + 29
        val hMonth = floor((24 * l3) / 709.0).toInt().coerceIn(1, 12)
        val hDay = l3 - floor((709 * hMonth) / 24.0).toInt()
        val hYear = 30 * n + j - 30

        return HijriDate(
            day = hDay.coerceIn(1, 30),
            month = hMonth,
            monthName = monthNames[hMonth - 1],
            year = hYear
        )
    }
}
