package com.fridgelist.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fridgelist.app.data.model.Tile

@Composable
fun TileEditorDialog(
    tile: Tile,
    onDismiss: () -> Unit,
    onChangeIcon: () -> Unit,
    onSaveName: (String) -> Unit,
    onRemove: () -> Unit
) {
    var taskName by remember { mutableStateOf(tile.taskName) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove tile?") },
            text = { Text("\"${tile.taskName}\" will be removed from the grid. The task in your list will not be deleted.") },
            confirmButton = {
                TextButton(onClick = { onRemove(); showRemoveConfirm = false }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit tile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onChangeIcon,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change icon")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveName(taskName.trim()) },
                enabled = taskName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showRemoveConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
