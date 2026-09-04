package com.mhoehn.freunde.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhoehn.freunde.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val thresholdDays: StateFlow<Int> = settingsRepository.thresholdDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_THRESHOLD_DAYS)

    fun setThresholdDays(days: Int) {
        viewModelScope.launch { settingsRepository.setThresholdDays(days) }
    }
}
