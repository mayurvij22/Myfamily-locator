package com.familytracker

import com.familytracker.util.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun distance_usesMetersBelowOneKm() {
        assertEquals("0 m", Format.distance(0f))
        assertEquals("850 m", Format.distance(850.2f))
        assertEquals("999 m", Format.distance(999.4f))
    }

    @Test
    fun distance_usesKmWithOneDecimalFromOneKm() {
        assertEquals("1.0 km", Format.distance(1000f))
        assertEquals("12.4 km", Format.distance(12_400f))
    }

    @Test
    fun duration() {
        assertEquals("0 min", Format.duration(30_000))
        assertEquals("45 min", Format.duration(45 * 60_000L))
        assertEquals("2 h", Format.duration(120 * 60_000L))
        assertEquals("3 h 10 min", Format.duration(190 * 60_000L))
    }

    @Test
    fun ago() {
        assertEquals("Just now", Format.ago(20_000))
        assertEquals("2 min ago", Format.ago(2 * 60_000L))
        assertEquals("3 h ago", Format.ago(3 * 3_600_000L))
    }

    @Test
    fun initials() {
        assertEquals("AS", Format.initials("Asha Sharma"))
        assertEquals("MO", Format.initials("mom"))
        assertEquals("?", Format.initials("  "))
    }
}
