package com.mhoehn.freunde.ui.screens.meetingform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.util.formatDisplay
import com.mhoehn.freunde.util.toDate
import com.mhoehn.freunde.util.toLocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingFormScreen(
    personId: String,
    meetingId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: MeetingFormViewModel = viewModel(
        factory = remember(personId, meetingId) {
            viewModelFactory {
                initializer {
                    MeetingFormViewModel(
                        personId,
                        meetingId,
                        container.meetingRepository,
                        container.personRepository,
                        container.authRepository
                    )
                }
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (meetingId != null) "Treffen bearbeiten" else "Neues Treffen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved) }, enabled = !state.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.date.formatDisplay(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Datum") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
            }
            item {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = viewModel::onLocationChange,
                    label = { Text("Ort") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Stichpunkte / Notizen") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            }
            item {
                TextButton(onClick = { viewModel.save(onSaved) }, enabled = !state.isSaving) {
                    Text(if (state.isSaving) "Speichern..." else "Speichern")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.date.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date: Date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toDate()
                        viewModel.onDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
