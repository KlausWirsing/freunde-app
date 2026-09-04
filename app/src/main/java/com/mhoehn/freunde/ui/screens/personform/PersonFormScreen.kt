package com.mhoehn.freunde.ui.screens.personform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.mhoehn.freunde.data.model.Child
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.ui.components.PersonAvatar
import com.mhoehn.freunde.util.formatDisplay
import com.mhoehn.freunde.util.toDate
import com.mhoehn.freunde.util.toLocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonFormScreen(
    personId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: PersonFormViewModel = viewModel(
        factory = remember(personId) {
            viewModelFactory {
                initializer { PersonFormViewModel(personId, container.personRepository, container.authRepository) }
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBirthdayPicker by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoUriChange(uri.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personId != null) "Person bearbeiten" else "Person anlegen") },
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
                Box(
                    modifier = Modifier.clickable {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    PersonAvatar(name = state.name, photoUri = state.photoUri, size = 80.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = "Foto ändern",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.birthday?.formatDisplay() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Geburtstag") },
                    modifier = Modifier.fillMaxWidth().clickable { showBirthdayPicker = true },
                    trailingIcon = {
                        if (state.birthday != null) {
                            IconButton(onClick = { viewModel.onBirthdayChange(null) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Geburtstag entfernen")
                            }
                        }
                    }
                )
            }

            item { SectionTitle("Fixdaten") }
            item {
                OutlinedTextField(
                    value = state.partnerName,
                    onValueChange = viewModel::onPartnerNameChange,
                    label = { Text("Partner/in") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { ChildrenEditor(children = state.children, viewModel = viewModel) }

            item {
                OutlinedTextField(
                    value = state.otherFixedInfo,
                    onValueChange = viewModel::onOtherFixedInfoChange,
                    label = { Text("Sonstige feste Infos (Wohnort, Beruf, ...)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item { SectionTitle("Aktueller Stand") }
            item {
                OutlinedTextField(
                    value = state.currentJob,
                    onValueChange = viewModel::onCurrentJobChange,
                    label = { Text("Job-Situation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.hobbies,
                    onValueChange = viewModel::onHobbiesChange,
                    label = { Text("Freizeit / Hobbys") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.vacation,
                    onValueChange = viewModel::onVacationChange,
                    label = { Text("Urlaub geplant / gerade im Urlaub") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.tempNotes,
                    onValueChange = viewModel::onTempNotesChange,
                    label = { Text("Sonstiges") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                TextButton(onClick = { viewModel.save(onSaved) }, enabled = !state.isSaving) {
                    Text(if (state.isSaving) "Speichern..." else "Speichern")
                }
            }
        }
    }

    if (showBirthdayPicker) {
        val initialMillis = state.birthday?.let {
            it.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date: Date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toDate()
                        viewModel.onBirthdayChange(date)
                    }
                    showBirthdayPicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ChildrenEditor(children: List<Child>, viewModel: PersonFormViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Kinder", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            children.forEachIndexed { index, child ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = child.name,
                        onValueChange = { viewModel.updateChild(index, child.copy(name = it)) },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = child.birthYear?.toString() ?: "",
                        onValueChange = { value ->
                            val year = value.toIntOrNull()
                            viewModel.updateChild(index, child.copy(birthYear = year))
                        },
                        label = { Text("Jahrgang") },
                        modifier = Modifier.width(110.dp)
                    )
                    IconButton(onClick = { viewModel.removeChild(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Kind entfernen")
                    }
                }
            }
            TextButton(onClick = { viewModel.addChild() }) {
                Text("+ Kind hinzufügen")
            }
        }
    }
}
