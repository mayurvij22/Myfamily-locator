package com.familytracker.data

import android.location.Location
import android.util.Log
import com.familytracker.location.Stay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class FamilyRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    val currentUid: String? get() = auth.currentUser?.uid

    private fun members(code: String): CollectionReference =
        db.collection("families").document(code).collection("members")

    /** Returns the anonymous uid, signing in if needed. The session survives app restarts. */
    suspend fun signIn(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid

    /** Creates my member doc. Waits for the server so the family listener is allowed to read. */
    suspend fun join(code: String, uid: String, name: String, sharing: Boolean) {
        val data = mapOf("uid" to uid, "name" to name, "sharing" to sharing)
        withTimeout(JOIN_TIMEOUT_MS) {
            members(code).document(uid).set(data, SetOptions.merge()).await()
        }
    }

    /** Fire-and-forget: Firestore queues the write offline and syncs when the network returns. */
    fun uploadLocation(code: String, uid: String, name: String, location: Location, stay: Stay?) {
        val data = mutableMapOf<String, Any>(
            "uid" to uid,
            "name" to name,
            "lat" to location.latitude,
            "lng" to location.longitude,
            "accuracy" to location.accuracy.toDouble(),
            "updatedAt" to System.currentTimeMillis(),
            "sharing" to true,
        )
        if (stay != null) {
            data["stayLat"] = stay.lat
            data["stayLng"] = stay.lng
            data["stayingSince"] = stay.since
        }
        members(code).document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Location upload failed", it) }
    }

    fun setSharing(code: String, uid: String, name: String, sharing: Boolean) {
        members(code).document(uid)
            .set(mapOf("uid" to uid, "name" to name, "sharing" to sharing), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Sharing update failed", it) }
    }

    /** Deletes my doc. If offline, the delete stays queued and syncs later. */
    suspend fun leave(code: String, uid: String) {
        withTimeoutOrNull(LEAVE_TIMEOUT_MS) {
            runCatching { members(code).document(uid).delete().await() }
                .onFailure { Log.w(TAG, "Leave failed", it) }
        }
    }

    /** Live list of all members. Fails with PERMISSION_DENIED if my own doc doesn't exist. */
    fun membersFlow(code: String): Flow<List<Member>> = callbackFlow {
        val registration = members(code).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull(Member::from).orEmpty())
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "FamilyRepository"
        const val JOIN_TIMEOUT_MS = 20_000L
        const val LEAVE_TIMEOUT_MS = 8_000L
    }
}
