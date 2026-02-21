package com.fridgelist.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Grid", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Columns: ${uiState.gridColumns}", modifier = Modifier.width(140.dp))
                Slider(
                    value = uiState.gridColumns.toFloat(),
                    onValueChange = { viewModel.setColumns(it.toInt()) },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rows: ${uiState.gridRows}", modifier = Modifier.width(140.dp))
                Slider(
                    value = uiState.gridRows.toFloat(),
                    onValueChange = { viewModel.setRows(it.toInt()) },
                    valueRange = 3f..15f,
                    steps = 11,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.pendingResize) {
                Button(
                    onClick = { viewModel.applyGridResize() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply grid changes")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Provider", style = MaterialTheme.typography.titleMedium)
            Text(
                "Connected: ${uiState.providerName ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = { viewModel.reconnectProvider() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reconnect provider")
            }
        }
    }
}
