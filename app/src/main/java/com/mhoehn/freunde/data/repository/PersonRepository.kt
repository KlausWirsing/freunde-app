package com.mhoehn.freunde.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mhoehn.freunde.data.model.Person
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class PersonRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun personsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("persons")

    /** Sortierung nach letztem Treffen erfolgt clientseitig, damit Personen ohne Treffen sauber einsortiert werden. */
    fun observePersons(uid: String): Flow<List<Person>> = callbackFlow {
        val registration = personsCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Person::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    fun observePerson(uid: String, personId: String): Flow<Person?> = callbackFlow {
        val registration = personsCollection(uid).document(personId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Person::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun getPerson(uid: String, personId: String): Person? =
        personsCollection(uid).document(personId).get().await().toObject(Person::class.java)

    suspend fun savePerson(uid: String, person: Person): String {
        val collection = personsCollection(uid)
        return if (person.id.isBlank()) {
            collection.add(person).await().id
        } else {
            collection.document(person.id).set(person).await()
            person.id
        }
    }

    suspend fun deletePerson(uid: String, personId: String) {
        val personDoc = personsCollection(uid).document(personId)
        val meetings = personDoc.collection("meetings").get().await()
        val batch = firestore.batch()
        meetings.documents.forEach { batch.delete(it.reference) }
        batch.delete(personDoc)
        batch.commit().await()
    }

    suspend fun updateLastMeetingDate(uid: String, personId: String, date: Date?) {
        personsCollection(uid).document(personId)
            .update(
                mapOf(
                    "lastMeetingDate" to date,
                    "longTimeNoSeeNotified" to false
                )
            ).await()
    }
}
