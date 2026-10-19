package com.familytracker.ui.home

import android.location.Location
import com.familytracker.data.Member
import com.familytracker.location.StayTracker
import com.familytracker.util.AreaResolver
import com.familytracker.util.Format
import com.google.android.gms.maps.model.LatLng

/** Everything a row, chip, marker or detail card needs about one member, already formatted. */
data class MemberUi(
    val member: Member,
    val isMe: Boolean,
    val latLng: LatLng?,
    val area: String?,
    val distance: String?,
    /** "Updated 2 min ago" / "Paused" / "Waiting…" */
    val status: String,
    /** "2 min ago" / "Paused" / "Waiting…" */
    val lastUpdate: String,
    val stayMs: Long?,
) {
    val uid: String get() = member.uid
    val name: String get() = member.name
    val longStay: Boolean get() = stayMs != null && stayMs >= Member.ALERT_STAY_MS
    val dimmed: Boolean get() = !member.sharing || !member.hasLocation
    val stayText: String? get() = stayMs?.takeIf { it >= MIN_STAY_TO_SHOW_MS }?.let(Format::duration)

    companion object {
        private const val MIN_STAY_TO_SHOW_MS = 5 * 60 * 1000L
    }
}

fun buildMemberUi(
    members: List<Member>,
    myUid: String?,
    myLocation: LatLng?,
    areas: Map<String, String>,
    now: Long,
): List<MemberUi> = members.map { m ->
    val latLng = if (m.lat != null && m.lng != null) LatLng(m.lat, m.lng) else null
    val distance = if (latLng != null && myLocation != null && m.uid != myUid) {
        Format.distance(StayTracker.androidDistance(myLocation.latitude, myLocation.longitude, latLng.latitude, latLng.longitude))
    } else null
    val area = latLng?.let { areas[AreaResolver.key(it.latitude, it.longitude)] }
    val ago = Format.ago(now - m.updatedAt)
    val (status, lastUpdate) = when {
        !m.sharing -> "Paused" to "Paused"
        !m.hasLocation -> "Waiting…" to "Waiting…"
        else -> "Updated ${ago.replaceFirstChar { it.lowercase() }}" to ago
    }
    MemberUi(
        member = m,
        isMe = m.uid == myUid,
        latLng = latLng,
        area = area,
        distance = distance,
        status = status,
        lastUpdate = lastUpdate,
        stayMs = m.stayDuration(now),
    )
}.sortedBy { it.name.lowercase() }

fun Location.toLatLng() = LatLng(latitude, longitude)
