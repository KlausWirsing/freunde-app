package com.mhoehn.freunde.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val BIRTHDAY_WORK_NAME = "birthday_check_work"
    private const val LONG_TIME_WORK_NAME = "long_time_no_see_work"

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val birthdayRequest = PeriodicWorkRequestBuilder<BirthdayCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayMinutes(hour = 9), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            BIRTHDAY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            birthdayRequest
        )

        val longTimeRequest = PeriodicWorkRequestBuilder<LongTimeNoSeeWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayMinutes(hour = 10), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            LONG_TIME_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            longTimeRequest
        )
    }

    private fun computeInitialDelayMinutes(hour: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMinutes()
    }
}
