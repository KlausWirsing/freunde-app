package com.mhoehn.freunde.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mhoehn.freunde.data.model.Meeting
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class MeetingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun meetingsCollection(uid: String, personId: String) =
        firestore.collection("users").document(uid)
            .collection("persons").document(personId)
            .collection("meetings")

    fun observeMeetings(uid: String, personId: String): Flow<List<Meeting>> = callbackFlow {
        val registration = meetingsCollection(uid, personId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Meeting::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    suspend fun getMeeting(uid: String, personId: String, meetingId: String): Meeting? =
        meetingsCollection(uid, personId).document(meetingId).get().await().toObject(Meeting::class.java)

    suspend fun saveMeeting(uid: String, personId: String, meeting: Meeting): String {
        val collection = meetingsCollection(uid, personId)
        return if (meeting.id.isBlank()) {
            collection.add(meeting).await().id
        } else {
            collection.document(meeting.id).set(meeting).await()
            meeting.id
        }
    }

    suspend fun deleteMeeting(uid: String, personId: String, meetingId: String) {
        meetingsCollection(uid, personId).document(meetingId).delete().await()
    }

    suspend fun getLatestMeetingDate(uid: String, personId: String): Date? {
        val snapshot = meetingsCollection(uid, personId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(Meeting::class.java)?.date
    }
}
