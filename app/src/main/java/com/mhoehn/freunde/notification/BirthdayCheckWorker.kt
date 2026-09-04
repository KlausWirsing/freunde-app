package com.mhoehn.freunde.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mhoehn.freunde.data.model.Person
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

class BirthdayCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("persons")
                .get()
                .await()

            val persons = snapshot.toObjects(Person::class.java)
            val today = LocalDate.now()

            persons.forEach { person ->
                val birthday = person.birthday ?: return@forEach
                val birthdayLocalDate = birthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                if (birthdayLocalDate.month == today.month && birthdayLocalDate.dayOfMonth == today.dayOfMonth) {
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        channelId = NotificationHelper.CHANNEL_BIRTHDAYS,
                        notificationId = ("birthday_" + person.id).hashCode(),
                        title = "${person.name} hat heute Geburtstag 🎉",
                        text = "Denk dran, ${person.name} zu gratulieren!",
                        personId = person.id
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
