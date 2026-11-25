package com.noorehidayah.quran.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.noorehidayah.quran.data.model.CityLocation
import java.util.Locale
import java.util.TimeZone

/**
 * Automatic offline-capable GPS/Network location detector using standard Android LocationManager.
 * Zero Google Play Services dependency.
 */
object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    fun detectLastKnownCity(context: Context): CityLocation? {
        if (!hasLocationPermission(context)) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var best: Location? = null
            for (p in providers) {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p)
                if (loc != null && (best == null || loc.accuracy < best.accuracy)) {
                    best = loc
                }
            }
            val found = best ?: return null
            val tzHours = TimeZone.getDefault().rawOffset / (1000.0 * 3600.0)
            val cityName = resolveCityName(context, found.latitude, found.longitude)
                ?: "Auto Location (%.2f, %.2f)".format(found.latitude, found.longitude)
            CityLocation(
                name = cityName,
                latitude = found.latitude,
                longitude = found.longitude,
                timezoneOffsetHours = tzHours
            )
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(context: Context, lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
        } catch (_: Exception) {
            null
        }
    }
}
