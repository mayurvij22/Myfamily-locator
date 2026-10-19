package com.familytracker.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.R
import com.familytracker.data.FamilyRepository
import com.familytracker.data.Member
import com.familytracker.data.Prefs
import com.familytracker.ui.MainActivity
import com.familytracker.util.Permissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that (1) uploads this phone's location every 5 minutes while sharing is on,
 * and (2) watches every family member's doc to raise the 3-hour stay alert locally.
 */
class LocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo = FamilyRepository()
    private lateinit var prefs: Prefs
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var notifier: StayAlertNotifier

    private var uid: String? = null
    private var code: String? = null
    private var updatesActive = false
    private var watchJob: Job? = null
    private var latestMembers: List<Member> = emptyList()

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
        .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
        .setWaitForAccurateLocation(true)
        .build()

    /** elapsedRealtime of the last precise fix; rough fixes shortly after it are dropped. */
    private var lastPreciseFixAt = 0L

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        fused = LocationServices.getFusedLocationProviderClient(this)
        notifier = StayAlertNotifier(this, prefs)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always enter the foreground first: startForegroundService() requires it within seconds.
        if (!goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val familyCode = prefs.code
        if (familyCode == null || !Permissions.allGranted(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (familyCode != code) {
            code = familyCode
            startWatching(familyCode)
        }
        applySharing()
        return START_STICKY
    }

    private fun goForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        true
    } catch (e: Exception) {
        // SecurityException without location permission, or a background-start restriction.
        Log.w(TAG, "Could not start foreground", e)
        false
    }

    /** Listens to all family docs and re-checks stays every 5 minutes. */
    private fun startWatching(familyCode: String) {
        watchJob?.cancel()
        watchJob = scope.launch {
            uid = runCatching { repo.signIn() }.getOrNull()
            launch {
                repo.membersFlow(familyCode)
                    .retryWhen { cause, _ ->
                        Log.w(TAG, "Member listener failed, retrying", cause)
                        delay(LISTENER_RETRY_MS)
                        true
                    }
                    .collect { members ->
                        latestMembers = members
                        notifier.check(members, uid)
                    }
            }
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                notifier.check(latestMembers, uid)
            }
        }
    }

    @SuppressLint("MissingPermission") // Checked via Permissions.hasLocation.
    private fun applySharing() {
        val shouldTrack = prefs.sharing && Permissions.hasLocation(this)
        if (shouldTrack && !updatesActive) {
            fused.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { it?.let(::onLocation) }
            updatesActive = true
        } else if (!shouldTrack && updatesActive) {
            fused.removeLocationUpdates(callback)
            updatesActive = false
        }
        updateNotification()
    }

    private fun onLocation(location: Location) {
        val familyCode = code ?: return
        if (!prefs.sharing) return
        // Indoors the phone often falls back to a cell tower (1–3 km off). Keep the last
        // precise position on the map instead, unless it's getting old.
        val nowElapsed = SystemClock.elapsedRealtime()
        if (location.accuracy <= PRECISE_ACCURACY_M) {
            lastPreciseFixAt = nowElapsed
        } else if (lastPreciseFixAt > 0 && nowElapsed - lastPreciseFixAt < KEEP_PRECISE_FIX_MS) {
            Log.d(TAG, "Skipping rough fix (±${location.accuracy.toInt()} m)")
            return
        }
        val stay = StayTracker.update(
            previous = prefs.stay,
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy,
            now = System.currentTimeMillis(),
        )
        prefs.stay = stay
        scope.launch {
            val myUid = uid ?: runCatching { repo.signIn() }.getOrNull()?.also { uid = it } ?: return@launch
            repo.uploadLocation(familyCode, myUid, prefs.name.orEmpty(), location, stay)
            // Location callbacks wake the phone even in Doze, so piggyback a stay check.
            notifier.check(latestMembers, myUid)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = getString(if (prefs.sharing) R.string.notif_sharing else R.string.notif_paused)
        return NotificationCompat.Builder(this, FamilyTrackerApp.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_location)
            .setContentTitle(text)
            .setContentText(prefs.code?.let { getString(R.string.notif_family, it) })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .build()
    }

    @SuppressLint("MissingPermission") // Foreground-service notifications are exempt.
    private fun updateNotification() {
        if (Permissions.hasNotifications(this)) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onDestroy() {
        if (updatesActive) fused.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L
        private const val MIN_UPDATE_INTERVAL_MS = 2 * 60 * 1000L
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L
        private const val LISTENER_RETRY_MS = 30_000L
        private const val PRECISE_ACCURACY_M = 100f
        private const val KEEP_PRECISE_FIX_MS = 15 * 60 * 1000L

        /** Starts the service, or tells a running one to re-read prefs (e.g. sharing toggled). */
        fun start(context: Context) {
            if (Prefs(context).code == null || !Permissions.allGranted(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationService::class.java))
        }
    }
}
