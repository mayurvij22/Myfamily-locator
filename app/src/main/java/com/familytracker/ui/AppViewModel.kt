package com.familytracker.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familytracker.data.FamilyRepository
import com.familytracker.data.Member
import com.familytracker.data.Prefs
import com.familytracker.location.LocationService
import com.familytracker.util.AreaResolver
import com.familytracker.util.Permissions
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Session(val name: String, val code: String)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val repo = FamilyRepository()

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _welcomeSeen = MutableStateFlow(prefs.welcomeSeen)
    val welcomeSeen: StateFlow<Boolean> = _welcomeSeen.asStateFlow()

    private val _sharing = MutableStateFlow(prefs.sharing)
    val sharing: StateFlow<Boolean> = _sharing.asStateFlow()

    private val _joining = MutableStateFlow(false)
    val joining: StateFlow<Boolean> = _joining.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    /** Bumped on ON_RESUME so permission-dependent flows re-check. */
    private val permissionTick = MutableStateFlow(0)

    val myUid: String? get() = repo.currentUid

    /** All members including me; null until the first snapshot arrives. */
    val members: StateFlow<List<Member>?> = _session
        .flatMapLatest { s ->
            if (s == null) flowOf(null)
            else repo.membersFlow(s.code)
                .retryWhen { cause, _ ->
                    Log.w(TAG, "Member listener failed, retrying", cause)
                    delay(10_000)
                    true
                }
                .onEach(::resolveAreas)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Live position of this phone while the map is on screen (independent of the 5-min uploads). */
    val myLocation: StateFlow<Location?> = permissionTick
        .map { Permissions.hasLocation(getApplication()) }
        .flatMapLatest { granted -> if (granted) liveLocation() else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Area names keyed by [AreaResolver.key]. */
    private val _areas = MutableStateFlow<Map<String, String>>(emptyMap())
    val areas: StateFlow<Map<String, String>> = _areas.asStateFlow()
    private val pendingAreas = mutableSetOf<String>()

    private fun loadSession(): Session? {
        val name = prefs.name ?: return null
        val code = prefs.code ?: return null
        return Session(name, code)
    }

    val savedName: String get() = prefs.name.orEmpty()

    fun onResume() = permissionTick.update { it + 1 }

    fun markWelcomeSeen() {
        prefs.welcomeSeen = true
        _welcomeSeen.value = true
    }

    fun join(name: String, code: String) {
        if (_joining.value) return
        _joining.value = true
        _joinError.value = null
        viewModelScope.launch {
            try {
                val uid = repo.signIn()
                repo.join(code, uid, name, sharing = true)
                prefs.name = name
                prefs.code = code
                prefs.sharing = true
                _sharing.value = true
                _session.value = Session(name, code)
            } catch (e: Exception) {
                Log.w(TAG, "Join failed", e)
                _joinError.value = "Couldn't join. Check your internet connection and Firebase setup."
            } finally {
                _joining.value = false
            }
        }
    }

    fun setSharing(enabled: Boolean) {
        val s = _session.value ?: return
        prefs.sharing = enabled
        _sharing.value = enabled
        repo.currentUid?.let { repo.setSharing(s.code, it, s.name, enabled) }
        LocationService.start(getApplication()) // Running service re-reads prefs.
    }

    fun leave() {
        val s = _session.value ?: return
        val uid = repo.currentUid
        LocationService.stop(getApplication())
        // Detach the family listener before deleting my doc, which revokes read access.
        _session.value = null
        prefs.clearFamily()
        _sharing.value = true
        viewModelScope.launch {
            if (uid != null) repo.leave(s.code, uid)
        }
    }

    private fun resolveAreas(members: List<Member>) {
        for (m in members) {
            val lat = m.lat ?: continue
            val lng = m.lng ?: continue
            val key = AreaResolver.key(lat, lng)
            if (key in _areas.value || !pendingAreas.add(key)) continue
            viewModelScope.launch {
                val area = AreaResolver.resolve(getApplication(), lat, lng)
                _areas.update { it + (key to area) }
                pendingAreas.remove(key)
            }
        }
    }

    @SuppressLint("MissingPermission") // Only collected when Permissions.hasLocation is true.
    private fun liveLocation(): Flow<Location?> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        var current: Location? = null
        // Keep the best recent fix: a rough cell-tower fix must not replace a fresh GPS one.
        fun offer(location: Location) {
            val prev = current
            val prevAgeMs = prev?.let { (location.elapsedRealtimeNanos - it.elapsedRealtimeNanos) / 1_000_000 }
            if (prev == null || location.accuracy <= prev.accuracy + 25f || (prevAgeMs ?: 0) > 60_000) {
                current = location
                trySend(location)
            }
        }
        // The cached fix can be hours old or from a cell tower; use it only if it's fresh and precise.
        client.lastLocation.addOnSuccessListener { loc ->
            val ageMs = loc?.let { (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000 }
            if (loc != null && ageMs != null && ageMs < 2 * 60_000 && loc.accuracy <= 100f) offer(loc)
        }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { it?.let(::offer) }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
            .setMinUpdateIntervalMillis(3_000)
            .setWaitForAccurateLocation(true)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::offer)
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
