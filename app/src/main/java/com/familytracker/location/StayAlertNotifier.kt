package com.familytracker.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.R
import com.familytracker.data.Member
import com.familytracker.data.Prefs
import com.familytracker.ui.MainActivity
import com.familytracker.util.AreaResolver
import com.familytracker.util.Format
import com.familytracker.util.Permissions

/** Runs on every phone: alerts once per stay when another member has been in one place ≥ 3 h. */
class StayAlertNotifier(private val context: Context, private val prefs: Prefs) {

    suspend fun check(members: List<Member>, myUid: String?, now: Long = System.currentTimeMillis()) {
        for (member in members) {
            if (member.uid == myUid) continue
            val stayed = member.stayDuration(now) ?: continue
            if (stayed < Member.ALERT_STAY_MS) continue

            val key = "${member.uid}:${member.stayingSince}"
            if (prefs.isAlerted(key)) continue
            // Mark before the (suspending) geocoder call so a concurrent check can't double-alert.
            prefs.markAlerted(key)

            val lat = member.stayLat ?: member.lat ?: continue
            val lng = member.stayLng ?: member.lng ?: continue
            val area = AreaResolver.resolve(context, lat, lng)
            notify(member, area, Format.duration(stayed), key.hashCode())
        }
    }

    @SuppressLint("MissingPermission") // Checked via Permissions.hasNotifications.
    private fun notify(member: Member, area: String, duration: String, id: Int) {
        if (!Permissions.hasNotifications(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = context.getString(R.string.alert_text, member.name, area, duration)
        val notification = NotificationCompat.Builder(context, FamilyTrackerApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_location)
            .setContentTitle(context.getString(R.string.alert_title, member.name, area))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
