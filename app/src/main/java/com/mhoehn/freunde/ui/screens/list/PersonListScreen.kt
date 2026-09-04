package com.mhoehn.freunde.ui.screens.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhoehn.freunde.data.model.Person
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.ui.components.PersonAvatar
import com.mhoehn.freunde.util.daysSince
import com.mhoehn.freunde.util.lastSeenLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onPersonClick: (String) -> Unit,
    onAddPerson: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: PersonListViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer {
                    PersonListViewModel(container.personRepository, container.authRepository, container.settingsRepository)
                }
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freunde") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerson) {
                Icon(Icons.Filled.Add, contentDescription = "Person hinzufügen")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Suche nach Namen") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.persons.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (query.isBlank()) {
                                "Noch keine Freunde erfasst. Tippe auf + zum Hinzufügen."
                            } else {
                                "Keine Treffer für \"$query\""
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.persons, key = { it.id }) { person ->
                            PersonListRow(
                                person = person,
                                thresholdDays = state.thresholdDays,
                                onClick = { onPersonClick(person.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonListRow(person: Person, thresholdDays: Int, onClick: () -> Unit) {
    val days = person.lastMeetingDate?.let { daysSince(it) }
    val isLongTimeNoSee = days == null || days >= thresholdDays

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { PersonAvatar(name = person.name, photoUri = person.photoUri) },
        headlineContent = { Text(person.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(lastSeenLabel(person.lastMeetingDate)) },
        trailingContent = {
            if (isLongTimeNoSee) {
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("!")
                }
            }
        }
    )
}
