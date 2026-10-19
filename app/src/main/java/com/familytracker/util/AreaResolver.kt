package com.familytracker.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** Turns coordinates into "Koregaon Park, Pune"; falls back to "18.5362, 73.8930". */
object AreaResolver {

    private val cache = ConcurrentHashMap<String, String>()

    /** ~100 m grid, so nearby fixes share one lookup. */
    fun key(lat: Double, lng: Double): String = String.format(Locale.US, "%.3f,%.3f", lat, lng)

    fun cached(lat: Double, lng: Double): String? = cache[key(lat, lng)]

    suspend fun resolve(context: Context, lat: Double, lng: Double): String {
        cache[key(lat, lng)]?.let { return it }
        val address = lookup(context.applicationContext, lat, lng)
        val area = listOfNotNull(address?.subLocality, address?.locality)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        // Don't cache the fallback, so a later lookup can still succeed once online.
        if (area.isEmpty()) return Format.latLng(lat, lng)
        cache[key(lat, lng)] = area
        return area
    }

    private suspend fun lookup(context: Context, lat: Double, lng: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
            }
        }
    }
}
