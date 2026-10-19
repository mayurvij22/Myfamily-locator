package com.familytracker

import com.familytracker.location.Stay
import com.familytracker.location.StayTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class StayTrackerTest {

    // android.location.Location isn't available in JVM tests; haversine is close enough.
    private val haversine: (Double, Double, Double, Double) -> Float = { lat1, lng1, lat2, lng2 ->
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
    }

    private fun update(prev: Stay?, lat: Double, lng: Double, acc: Float, now: Long) =
        StayTracker.update(prev, lat, lng, acc, now, haversine)

    @Test
    fun firstAccurateFix_startsStay() {
        assertEquals(Stay(18.5, 73.8, 1000), update(null, 18.5, 73.8, 20f, 1000))
    }

    @Test
    fun inaccurateFix_isIgnored() {
        assertNull(update(null, 18.5, 73.8, 500f, 1000))
        val stay = Stay(18.5, 73.8, 1000)
        assertSame(stay, update(stay, 18.6, 73.9, 201f, 2000))
    }

    @Test
    fun smallMovement_keepsAnchorAndStartTime() {
        val stay = Stay(18.5, 73.8, 1000)
        // ~100 m north
        assertSame(stay, update(stay, 18.5009, 73.8, 30f, 5000))
    }

    @Test
    fun movingMoreThan150m_resetsAnchor() {
        val stay = Stay(18.5, 73.8, 1000)
        // ~200 m north
        assertEquals(Stay(18.5018, 73.8, 5000), update(stay, 18.5018, 73.8, 30f, 5000))
    }
}
