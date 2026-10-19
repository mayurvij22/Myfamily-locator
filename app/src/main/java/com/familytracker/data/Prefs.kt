package com.familytracker.data

import android.content.Context
import androidx.core.content.edit
import com.familytracker.location.Stay

/** Local state shared by the UI, the service and the boot receiver. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("family_tracker", Context.MODE_PRIVATE)

    var name: String?
        get() = sp.getString(KEY_NAME, null)
        set(value) = sp.edit { putString(KEY_NAME, value) }

    var code: String?
        get() = sp.getString(KEY_CODE, null)
        set(value) = sp.edit { putString(KEY_CODE, value) }

    var sharing: Boolean
        get() = sp.getBoolean(KEY_SHARING, true)
        set(value) = sp.edit { putBoolean(KEY_SHARING, value) }

    var welcomeSeen: Boolean
        get() = sp.getBoolean(KEY_WELCOME_SEEN, false)
        set(value) = sp.edit { putBoolean(KEY_WELCOME_SEEN, value) }

    var stay: Stay?
        get() {
            if (!sp.contains(KEY_STAY_LAT)) return null
            return Stay(
                lat = Double.fromBits(sp.getLong(KEY_STAY_LAT, 0)),
                lng = Double.fromBits(sp.getLong(KEY_STAY_LNG, 0)),
                since = sp.getLong(KEY_STAY_SINCE, 0),
            )
        }
        set(value) = sp.edit {
            if (value == null) {
                remove(KEY_STAY_LAT); remove(KEY_STAY_LNG); remove(KEY_STAY_SINCE)
            } else {
                putLong(KEY_STAY_LAT, value.lat.toRawBits())
                putLong(KEY_STAY_LNG, value.lng.toRawBits())
                putLong(KEY_STAY_SINCE, value.since)
            }
        }

    /** "uid:stayingSince" keys we've already alerted for, so each stay alerts once. */
    fun isAlerted(key: String): Boolean = key in alertedKeys()

    fun markAlerted(key: String) {
        // Keep the newest entries only; a stay key is useless once that stay has ended.
        val keys = (alertedKeys() + key).toList().takeLast(MAX_ALERT_KEYS)
        sp.edit { putString(KEY_ALERTED, keys.joinToString("\n")) }
    }

    private fun alertedKeys(): Set<String> =
        sp.getString(KEY_ALERTED, null)?.split("\n")?.filter { it.isNotEmpty() }?.toCollection(LinkedHashSet())
            ?: emptySet()

    fun clearFamily() = sp.edit {
        remove(KEY_CODE)
        remove(KEY_STAY_LAT); remove(KEY_STAY_LNG); remove(KEY_STAY_SINCE)
        remove(KEY_ALERTED)
        putBoolean(KEY_SHARING, true)
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_CODE = "code"
        const val KEY_SHARING = "sharing"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_STAY_LAT = "stay_lat"
        const val KEY_STAY_LNG = "stay_lng"
        const val KEY_STAY_SINCE = "stay_since"
        const val KEY_ALERTED = "alerted_keys"
        const val MAX_ALERT_KEYS = 100
    }
}
