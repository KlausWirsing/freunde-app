package com.mhoehn.freunde.di

import android.content.Context
import com.mhoehn.freunde.data.repository.AuthRepository
import com.mhoehn.freunde.data.repository.MeetingRepository
import com.mhoehn.freunde.data.repository.PersonRepository
import com.mhoehn.freunde.data.repository.SettingsRepository

/**
 * Einfacher, manueller DI-Container statt eines Frameworks (Hilt etc.),
 * um das Setup minimal zu halten. Alle Repositories sind zustandslose Singletons.
 */
class AppContainer(context: Context) {
    val authRepository: AuthRepository by lazy { AuthRepository(context.applicationContext) }
    val personRepository: PersonRepository by lazy { PersonRepository() }
    val meetingRepository: MeetingRepository by lazy { MeetingRepository() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context.applicationContext) }
}
