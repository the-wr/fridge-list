package com.fridgelist.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridgelist.app.data.model.ProviderType

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.startSession() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall)

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Columns: ${uiState.gridColumns}", modifier = Modifier.width(120.dp))
                    Slider(
                        value = uiState.gridColumns.toFloat(),
                        onValueChange = { viewModel.setColumns(it.toInt()) },
                        valueRange = 3f..15f,
                        steps = 11,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rows: ${uiState.gridRows}", modifier = Modifier.width(120.dp))
                    Slider(
                        value = uiState.gridRows.toFloat(),
                        onValueChange = { viewModel.setRows(it.toInt()) },
                        valueRange = 3f..15f,
                        steps = 11,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { viewModel.resetGrid() },
                    enabled = uiState.pendingResize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Provider section
                Text("List provider", style = MaterialTheme.typography.titleMedium)

                val providerLabel = buildProviderLabel(uiState.providerType, uiState.providerName)
                Text(
                    text = providerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Synchronization from your provider happens automatically every 10 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToSetup()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change provider")
                }

                Text(
                    text = "Your grid layout will be preserved when changing provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                // Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private fun buildProviderLabel(providerType: ProviderType?, listName: String?): String {
    val typeName = when (providerType) {
        ProviderType.TODOIST -> "Todoist"
        ProviderType.MICROSOFT_TODO -> "Microsoft To Do"
        ProviderType.GOOGLE_TASKS -> "Google Tasks"
        ProviderType.TICKTICK -> "TickTick"
        null -> null
    }
    return when {
        typeName != null && listName != null -> "$typeName \u2192 $listName"
        typeName != null -> typeName
        else -> "None"
    }
}
