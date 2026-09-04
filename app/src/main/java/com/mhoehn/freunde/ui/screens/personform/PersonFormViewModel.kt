package com.mhoehn.freunde.ui.screens.personform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhoehn.freunde.data.model.Child
import com.mhoehn.freunde.data.model.FixedInfo
import com.mhoehn.freunde.data.model.Person
import com.mhoehn.freunde.data.model.TempInfo
import com.mhoehn.freunde.data.repository.AuthRepository
import com.mhoehn.freunde.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class PersonFormState(
    val id: String = "",
    val name: String = "",
    val photoUri: String? = null,
    val partnerName: String = "",
    val children: List<Child> = emptyList(),
    val otherFixedInfo: String = "",
    val currentJob: String = "",
    val hobbies: String = "",
    val vacation: String = "",
    val tempNotes: String = "",
    val birthday: Date? = null,
    val lastMeetingDate: Date? = null,
    val longTimeNoSeeNotified: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val nameError: String? = null
) {
    val isEdit: Boolean get() = id.isNotBlank()
}

class PersonFormViewModel(
    private val personId: String?,
    private val personRepository: PersonRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PersonFormState(isLoading = personId != null))
    val state: StateFlow<PersonFormState> = _state.asStateFlow()

    init {
        if (personId != null) {
            viewModelScope.launch {
                val uid = authRepository.currentUser?.uid
                val person = uid?.let { personRepository.getPerson(it, personId) }
                if (person != null) {
                    _state.update {
                        it.copy(
                            id = person.id,
                            name = person.name,
                            photoUri = person.photoUri,
                            partnerName = person.fixedInfo.partnerName,
                            children = person.fixedInfo.children,
                            otherFixedInfo = person.fixedInfo.otherInfo,
                            currentJob = person.tempInfo.currentJob,
                            hobbies = person.tempInfo.hobbies,
                            vacation = person.tempInfo.vacation,
                            tempNotes = person.tempInfo.notes,
                            birthday = person.birthday,
                            lastMeetingDate = person.lastMeetingDate,
                            longTimeNoSeeNotified = person.longTimeNoSeeNotified,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onPhotoUriChange(value: String?) = _state.update { it.copy(photoUri = value) }
    fun onPartnerNameChange(value: String) = _state.update { it.copy(partnerName = value) }
    fun onOtherFixedInfoChange(value: String) = _state.update { it.copy(otherFixedInfo = value) }
    fun onCurrentJobChange(value: String) = _state.update { it.copy(currentJob = value) }
    fun onHobbiesChange(value: String) = _state.update { it.copy(hobbies = value) }
    fun onVacationChange(value: String) = _state.update { it.copy(vacation = value) }
    fun onTempNotesChange(value: String) = _state.update { it.copy(tempNotes = value) }
    fun onBirthdayChange(value: Date?) = _state.update { it.copy(birthday = value) }

    fun addChild() = _state.update { it.copy(children = it.children + Child()) }

    fun updateChild(index: Int, child: Child) = _state.update { state ->
        state.copy(children = state.children.toMutableList().also { it[index] = child })
    }

    fun removeChild(index: Int) = _state.update { state ->
        state.copy(children = state.children.toMutableList().also { it.removeAt(index) })
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = "Bitte einen Namen eingeben") }
            return
        }
        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val person = Person(
                id = current.id,
                name = current.name.trim(),
                photoUri = current.photoUri,
                fixedInfo = FixedInfo(
                    partnerName = current.partnerName.trim(),
                    children = current.children.filter { it.name.isNotBlank() },
                    otherInfo = current.otherFixedInfo.trim()
                ),
                tempInfo = TempInfo(
                    currentJob = current.currentJob.trim(),
                    hobbies = current.hobbies.trim(),
                    vacation = current.vacation.trim(),
                    notes = current.tempNotes.trim()
                ),
                birthday = current.birthday,
                lastMeetingDate = current.lastMeetingDate,
                longTimeNoSeeNotified = current.longTimeNoSeeNotified
            )
            personRepository.savePerson(uid, person)
            _state.update { it.copy(isSaving = false, saved = true) }
            onSaved()
        }
    }
}
