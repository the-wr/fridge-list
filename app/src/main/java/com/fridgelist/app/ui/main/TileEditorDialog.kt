package com.fridgelist.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fridgelist.app.data.model.IconCatalog
import com.fridgelist.app.data.model.IconCategory
import com.fridgelist.app.data.model.Tile
import com.fridgelist.app.ui.common.GroceryIcon

/**
 * Unified dialog for adding a new tile (existingTile == null) or editing one
 * (existingTile != null). Both flows live in a single screen:
 *   - top: icon preview + name field
 *   - middle: search bar + category chips + icon grid
 *   - bottom: action buttons
 */
@Composable
fun TileEditorDialog(
    existingTile: Tile?,
    onDismiss: () -> Unit,
    onSave: (iconName: String, taskName: String) -> Unit,
    onRemove: () -> Unit
) {
    val initialCategory = remember(existingTile) {
        existingTile?.iconName?.let { name -> IconCatalog.all.find { it.name == name }?.category }
    }

    var selectedIconName by remember { mutableStateOf(existingTile?.iconName) }
    var taskName by remember { mutableStateOf(existingTile?.taskName ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var confirmRemove by remember { mutableStateOf(false) }

    val filteredIcons = remember(searchQuery, selectedCategory) {
        when {
            searchQuery.isNotBlank() -> IconCatalog.findByName(searchQuery)
            selectedCategory != null -> IconCatalog.byCategory[selectedCategory] ?: emptyList()
            else -> IconCatalog.all
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // ── Header: icon preview + name field ─────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedIconName != null) {
                            GroceryIcon(
                                iconName = selectedIconName!!,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // ── Search bar ────────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search icons…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // ── Category chips (hidden while searching) ───────────────
                if (searchQuery.isEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All") }
                            )
                        }
                        items(IconCategory.values().toList()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat.displayName) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Icon grid ─────────────────────────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredIcons) { icon ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (selectedIconName == icon.name)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent
                                )
                                .clickable {
                                    selectedIconName = icon.name
                                    taskName = icon.displayName
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GroceryIcon(
                                iconName = icon.name,
                                contentDescription = icon.displayName,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                // ── Buttons ───────────────────────────────────────────────
                if (confirmRemove) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Remove \"${existingTile?.taskName}\"?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onRemove(); confirmRemove = false }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { confirmRemove = false }) {
                            Text("Cancel")
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (existingTile != null) {
                            TextButton(onClick = { confirmRemove = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = { onSave(selectedIconName!!, taskName.trim()) },
                            enabled = selectedIconName != null && taskName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
