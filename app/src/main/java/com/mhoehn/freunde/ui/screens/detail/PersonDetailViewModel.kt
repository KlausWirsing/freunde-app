package com.mhoehn.freunde.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhoehn.freunde.data.model.Meeting
import com.mhoehn.freunde.data.model.Person
import com.mhoehn.freunde.data.model.TempInfo
import com.mhoehn.freunde.data.repository.AuthRepository
import com.mhoehn.freunde.data.repository.MeetingRepository
import com.mhoehn.freunde.data.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: Person? = null,
    val meetings: List<Meeting> = emptyList(),
    val isLoading: Boolean = true,
    val personDeleted: Boolean = false
)

class PersonDetailViewModel(
    private val personId: String,
    private val personRepository: PersonRepository,
    private val meetingRepository: MeetingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val uid: String? get() = authRepository.currentUser?.uid

    private val personDeleted = MutableStateFlow(false)

    val uiState: StateFlow<PersonDetailUiState> = run {
        val currentUid = uid
        val flow: Flow<PersonDetailUiState> = if (currentUid == null) {
            flowOf(PersonDetailUiState(isLoading = false))
        } else {
            combine(
                personRepository.observePerson(currentUid, personId),
                meetingRepository.observeMeetings(currentUid, personId),
                personDeleted
            ) { person, meetings, deleted ->
                PersonDetailUiState(person = person, meetings = meetings, isLoading = false, personDeleted = deleted)
            }
        }
        flow
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonDetailUiState())

    fun deletePerson() {
        val currentUid = uid ?: return
        viewModelScope.launch {
            personRepository.deletePerson(currentUid, personId)
            personDeleted.value = true
        }
    }

    fun updateTempInfo(tempInfo: TempInfo) {
        val currentUid = uid ?: return
        val person = uiState.value.person ?: return
        viewModelScope.launch {
            personRepository.savePerson(currentUid, person.copy(tempInfo = tempInfo))
        }
    }

    fun deleteMeeting(meetingId: String) {
        val currentUid = uid ?: return
        viewModelScope.launch {
            meetingRepository.deleteMeeting(currentUid, personId, meetingId)
            val latest = meetingRepository.getLatestMeetingDate(currentUid, personId)
            personRepository.updateLastMeetingDate(currentUid, personId, latest)
        }
    }
}
