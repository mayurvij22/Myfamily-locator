package com.familytracker.data

import com.google.firebase.firestore.DocumentSnapshot

/** One document at families/{code}/members/{uid}. */
data class Member(
    val uid: String,
    val name: String,
    val lat: Double?,
    val lng: Double?,
    val accuracy: Float?,
    val updatedAt: Long,
    val stayLat: Double?,
    val stayLng: Double?,
    val stayingSince: Long,
    val sharing: Boolean,
) {
    val hasLocation: Boolean get() = lat != null && lng != null && updatedAt > 0

    /** Whether the last upload is recent enough to trust (the phone is still reporting). */
    fun isFresh(now: Long): Boolean = hasLocation && now - updatedAt <= FRESH_MS

    /** How long they've been at the same place, or null if unknown / not trustworthy. */
    fun stayDuration(now: Long): Long? =
        if (sharing && stayingSince > 0 && isFresh(now)) (now - stayingSince).coerceAtLeast(0) else null

    companion object {
        const val FRESH_MS = 30 * 60 * 1000L
        const val ALERT_STAY_MS = 3 * 60 * 60 * 1000L

        fun from(doc: DocumentSnapshot): Member? {
            val uid = doc.getString("uid") ?: doc.id
            return Member(
                uid = uid,
                name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Member",
                lat = doc.getDouble("lat"),
                lng = doc.getDouble("lng"),
                accuracy = doc.getDouble("accuracy")?.toFloat(),
                updatedAt = doc.getLong("updatedAt") ?: 0L,
                stayLat = doc.getDouble("stayLat"),
                stayLng = doc.getDouble("stayLng"),
                stayingSince = doc.getLong("stayingSince") ?: 0L,
                sharing = doc.getBoolean("sharing") ?: true,
            )
        }
    }
}
