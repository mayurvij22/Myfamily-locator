package com.familytracker.location

import android.location.Location

/** Where this phone has been "staying", and since when (epoch ms). */
data class Stay(val lat: Double, val lng: Double, val since: Long)

object StayTracker {
    const val MAX_ACCURACY_M = 200f
    const val RADIUS_M = 150f

    /**
     * Returns the stay after a new fix. Fixes worse than [MAX_ACCURACY_M] are ignored.
     * Moving more than [RADIUS_M] from the anchor starts a new stay at the new fix.
     */
    fun update(
        previous: Stay?,
        lat: Double,
        lng: Double,
        accuracy: Float,
        now: Long,
        distanceMeters: (Double, Double, Double, Double) -> Float = ::androidDistance,
    ): Stay? {
        if (accuracy > MAX_ACCURACY_M) return previous
        if (previous == null) return Stay(lat, lng, now)
        val moved = distanceMeters(previous.lat, previous.lng, lat, lng)
        return if (moved > RADIUS_M) Stay(lat, lng, now) else previous
    }

    fun androidDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val out = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, out)
        return out[0]
    }
}
