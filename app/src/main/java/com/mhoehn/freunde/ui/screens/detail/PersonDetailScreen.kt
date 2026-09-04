package com.mhoehn.freunde.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mhoehn.freunde.data.model.FixedInfo
import com.mhoehn.freunde.data.model.Meeting
import com.mhoehn.freunde.data.model.TempInfo
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.ui.components.PersonAvatar
import com.mhoehn.freunde.util.formatDisplay
import com.mhoehn.freunde.util.lastSeenLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: String,
    onBack: () -> Unit,
    onEditPerson: (String) -> Unit,
    onAddMeeting: (String) -> Unit,
    onEditMeeting: (String, String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: PersonDetailViewModel = viewModel(
        factory = remember(personId) {
            viewModelFactory {
                initializer {
                    PersonDetailViewModel(
                        personId,
                        container.personRepository,
                        container.meetingRepository,
                        container.authRepository
                    )
                }
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.personDeleted) {
        if (state.personDeleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditPerson(personId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Person bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Person löschen")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onAddMeeting(personId) }) {
                Text("Treffen erfassen")
            }
        }
    ) { padding ->
        val person = state.person
        if (state.isLoading || person == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PersonAvatar(name = person.name, photoUri = person.photoUri, size = 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(person.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            lastSeenLabel(person.lastMeetingDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        person.birthday?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Cake,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(it.formatDisplay(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item { FixedInfoCard(person.fixedInfo) }
            item {
                TempInfoCard(
                    tempInfo = person.tempInfo,
                    onSave = { viewModel.updateTempInfo(it) }
                )
            }

            item {
                Text("Treffen", style = MaterialTheme.typography.titleMedium)
            }

            if (state.meetings.isEmpty()) {
                item {
                    Text(
                        "Noch keine Treffen erfasst.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.meetings, key = { it.id }) { meeting ->
                    MeetingRow(
                        meeting = meeting,
                        onClick = { onEditMeeting(personId, meeting.id) },
                        onDelete = { viewModel.deleteMeeting(meeting.id) }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Person löschen?") },
            text = { Text("${state.person?.name} und alle erfassten Treffen werden dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePerson()
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun FixedInfoCard(fixedInfo: FixedInfo) {
    val hasContent = fixedInfo.partnerName.isNotBlank() || fixedInfo.children.isNotEmpty() || fixedInfo.otherInfo.isNotBlank()
    if (!hasContent) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Fixdaten", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (fixedInfo.partnerName.isNotBlank()) {
                Text("Partner/in: ${fixedInfo.partnerName}", style = MaterialTheme.typography.bodyMedium)
            }
            fixedInfo.children.forEach { child ->
                val ageInfo = child.birthYear?.let { " (geb. $it)" } ?: ""
                Text("Kind: ${child.name}$ageInfo", style = MaterialTheme.typography.bodyMedium)
            }
            if (fixedInfo.otherInfo.isNotBlank()) {
                Text(fixedInfo.otherInfo, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TempInfoCard(tempInfo: TempInfo, onSave: (TempInfo) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var job by remember(tempInfo) { mutableStateOf(tempInfo.currentJob) }
    var hobbies by remember(tempInfo) { mutableStateOf(tempInfo.hobbies) }
    var vacation by remember(tempInfo) { mutableStateOf(tempInfo.vacation) }
    var notes by remember(tempInfo) { mutableStateOf(tempInfo.notes) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aktueller Stand", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        if (isEditing) Icons.Filled.Close else Icons.Filled.Edit,
                        contentDescription = if (isEditing) "Bearbeitung schließen" else "Bearbeiten"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(job, { job = it }, label = { Text("Job-Situation") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(hobbies, { hobbies = it }, label = { Text("Freizeit / Hobbys") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(vacation, { vacation = it }, label = { Text("Urlaub") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Sonstiges") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        onSave(TempInfo(job.trim(), hobbies.trim(), vacation.trim(), notes.trim()))
                        isEditing = false
                    }) { Text("Speichern") }
                }
            } else {
                val rows = listOfNotNull(
                    tempInfo.currentJob.takeIf { it.isNotBlank() }?.let { "Job: $it" },
                    tempInfo.hobbies.takeIf { it.isNotBlank() }?.let { "Hobbys: $it" },
                    tempInfo.vacation.takeIf { it.isNotBlank() }?.let { "Urlaub: $it" },
                    tempInfo.notes.takeIf { it.isNotBlank() }
                )
                if (rows.isEmpty()) {
                    Text(
                        "Noch keine aktuellen Infos hinterlegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    rows.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun MeetingRow(meeting: Meeting, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meeting.date.formatDisplay(), style = MaterialTheme.typography.titleMedium)
                    if (meeting.location.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(meeting.location, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (meeting.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(meeting.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Treffen löschen")
            }
        }
    }
}
