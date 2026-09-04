package com.mhoehn.freunde.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "freunde_settings")

class SettingsRepository(private val context: Context) {
    private val thresholdKey = intPreferencesKey("long_time_no_see_threshold_days")

    val thresholdDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[thresholdKey] ?: DEFAULT_THRESHOLD_DAYS
    }

    suspend fun setThresholdDays(days: Int) {
        context.dataStore.edit { it[thresholdKey] = days }
    }

    companion object {
        const val DEFAULT_THRESHOLD_DAYS = 60
    }
}
