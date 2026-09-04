package com.mhoehn.freunde.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mhoehn.freunde.data.model.Person
import com.mhoehn.freunde.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class LongTimeNoSeeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val thresholdDays = SettingsRepository(applicationContext).thresholdDays.first()
        val personsCollection = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("persons")

        return try {
            val snapshot = personsCollection.get().await()
            val persons = snapshot.toObjects(Person::class.java)
            val now = System.currentTimeMillis()

            persons.forEach { person ->
                if (person.longTimeNoSeeNotified) return@forEach
                // Ohne bisheriges Treffen gibt es keinen sinnvollen Vergleichszeitpunkt - nicht benachrichtigen.
                val lastMeeting = person.lastMeetingDate ?: return@forEach
                val daysSince = TimeUnit.MILLISECONDS.toDays(now - lastMeeting.time)

                if (daysSince >= thresholdDays) {
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        channelId = NotificationHelper.CHANNEL_LONG_TIME,
                        notificationId = ("longtime_" + person.id).hashCode(),
                        title = "Lange nicht mehr getroffen: ${person.name}",
                        text = "Letztes Treffen vor $daysSince Tagen. Vielleicht mal wieder melden?",
                        personId = person.id
                    )
                    personsCollection.document(person.id)
                        .update("longTimeNoSeeNotified", true)
                        .await()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
