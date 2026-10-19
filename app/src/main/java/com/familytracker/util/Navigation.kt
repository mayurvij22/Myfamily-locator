package com.familytracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

/**
 * Hands turn-by-turn directions to Google Maps (free, no Directions API needed).
 * Falls back to any map app, then to Google Maps in the browser.
 */
object Navigation {

    fun navigateTo(context: Context, lat: Double, lng: Double, label: String) {
        val dest = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        val attempts = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$dest&mode=d"))
                .setPackage("com.google.android.apps.maps"),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$dest?q=$dest(${Uri.encode(label)})")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$dest&travelmode=driving")),
        )
        for (intent in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next option.
            }
        }
    }
}
