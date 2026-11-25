package com.noorehidayah.quran.utils

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 100% Offline Qibla bearing & distance calculator using spherical trigonometry.
 */
object QiblaCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun qiblaBearingDegrees(lat: Double, lon: Double): Float {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LON - lon)

        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }

    fun distanceToMakkahKm(lat: Double, lon: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - lat)
        val dLon = Math.toRadians(KAABA_LON - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt()
    }

    fun cardinalDirection(deg: Float): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
        return dirs[idx]
    }
}
