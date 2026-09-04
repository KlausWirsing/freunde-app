package com.mhoehn.freunde.ui.screens.meetingform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhoehn.freunde.data.model.Meeting
import com.mhoehn.freunde.data.repository.AuthRepository
import com.mhoehn.freunde.data.repository.MeetingRepository
import com.mhoehn.freunde.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class MeetingFormState(
    val id: String = "",
    val date: Date = Date(),
    val location: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class MeetingFormViewModel(
    private val personId: String,
    private val meetingId: String?,
    private val meetingRepository: MeetingRepository,
    private val personRepository: PersonRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MeetingFormState(isLoading = meetingId != null))
    val state: StateFlow<MeetingFormState> = _state.asStateFlow()

    init {
        if (meetingId != null) {
            viewModelScope.launch {
                val uid = authRepository.currentUser?.uid
                val meeting = uid?.let { meetingRepository.getMeeting(it, personId, meetingId) }
                if (meeting != null) {
                    _state.update {
                        it.copy(
                            id = meeting.id,
                            date = meeting.date,
                            location = meeting.location,
                            notes = meeting.notes,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onDateChange(date: Date) = _state.update { it.copy(date = date) }
    fun onLocationChange(value: String) = _state.update { it.copy(location = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun save(onSaved: () -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val meeting = Meeting(
                id = current.id,
                date = current.date,
                location = current.location.trim(),
                notes = current.notes.trim()
            )
            meetingRepository.saveMeeting(uid, personId, meeting)
            val latest = meetingRepository.getLatestMeetingDate(uid, personId)
            personRepository.updateLastMeetingDate(uid, personId, latest)
            _state.update { it.copy(isSaving = false, saved = true) }
            onSaved()
        }
    }
}
