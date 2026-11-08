package com.khaory.taller.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** Utilidades de ubicación y geocodificación (antes estaban dentro de DemoDashboardScreen). */
internal fun isRecentLocation(location: Location): Boolean {
    val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
    return ageMillis in 0..300_000
}

internal fun cachedStreetAddress(context: Context, location: Location): String? {
    val saved = context.getSharedPreferences("delivery_address_cache", Context.MODE_PRIVATE)
    val savedAt = saved.getLong("savedAt", 0L)
    val ageMillis = System.currentTimeMillis() - savedAt
    if (ageMillis !in 0..3_600_000) return null
    val meters = FloatArray(1)
    Location.distanceBetween(location.latitude, location.longitude,
        saved.getFloat("latitude", 0f).toDouble(), saved.getFloat("longitude", 0f).toDouble(), meters)
    return saved.getString("address", null)?.takeIf { meters[0] <= 30f && it.isNotBlank() }
}

internal fun rememberStreetAddress(context: Context, location: Location, address: String) {
    context.getSharedPreferences("delivery_address_cache", Context.MODE_PRIVATE).edit()
        .putFloat("latitude", location.latitude.toFloat())
        .putFloat("longitude", location.longitude.toFloat())
        .putString("address", address)
        .putLong("savedAt", System.currentTimeMillis())
        .apply()
}

internal suspend fun addressForLocation(context: Context, location: Location): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.forLanguageTag("es-PE"))
    val matches: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(location.latitude, location.longitude, 5,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                })
        }
    } else {
        @Suppress("DEPRECATION")
        withContext(Dispatchers.IO) { geocoder.getFromLocation(location.latitude, location.longitude, 5).orEmpty() }
    }
    return matches.firstOrNull { !it.thoroughfare.isNullOrBlank() }
        ?.getAddressLine(0)?.takeIf { it.isNotBlank() }
}
