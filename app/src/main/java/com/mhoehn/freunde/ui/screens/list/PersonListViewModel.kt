package com.mhoehn.freunde.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhoehn.freunde.data.model.Person
import com.mhoehn.freunde.data.repository.AuthRepository
import com.mhoehn.freunde.data.repository.PersonRepository
import com.mhoehn.freunde.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class PersonListUiState(
    val persons: List<Person> = emptyList(),
    val thresholdDays: Int = SettingsRepository.DEFAULT_THRESHOLD_DAYS,
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = true
)

class PersonListViewModel(
    private val personRepository: PersonRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val personsFlow = authRepository.authState.flatMapLatest { user ->
        val uid = user?.uid
        if (uid == null) flowOf(emptyList()) else personRepository.observePersons(uid)
    }

    val uiState: StateFlow<PersonListUiState> = combine(
        personsFlow,
        settingsRepository.thresholdDays,
        searchQuery
    ) { persons, threshold, query ->
        val filtered = if (query.isBlank()) {
            persons
        } else {
            persons.filter { it.name.contains(query, ignoreCase = true) }
        }
        // Personen ohne Treffen (kein lastMeetingDate) zuerst, danach längste Zeit ohne Kontakt oben.
        val sorted = filtered.sortedBy { it.lastMeetingDate?.time ?: Long.MIN_VALUE }
        PersonListUiState(
            persons = sorted,
            thresholdDays = threshold,
            isLoading = false,
            isSignedIn = authRepository.currentUser != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonListUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun signOut() {
        authRepository.signOut()
    }
}
