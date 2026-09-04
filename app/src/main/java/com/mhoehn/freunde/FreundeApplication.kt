package com.mhoehn.freunde

import android.app.Application
import com.mhoehn.freunde.di.AppContainer
import com.mhoehn.freunde.notification.NotificationHelper
import com.mhoehn.freunde.notification.ReminderScheduler

class FreundeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Firestore Offline-Persistenz ist auf Android standardmäßig aktiviert - keine weitere Konfiguration nötig.
        NotificationHelper.createChannels(this)
        ReminderScheduler.scheduleAll(this)
    }
}
