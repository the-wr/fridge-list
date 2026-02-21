package com.fridgelist.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fridgelist.app.data.model.IconCatalog
import com.fridgelist.app.data.model.IconCategory
import com.fridgelist.app.data.model.IconItem
import com.fridgelist.app.ui.common.GroceryIcon

private enum class PickerMode { SEARCH, BROWSE_CATEGORIES, BROWSE_ITEMS }

@Composable
fun IconPickerDialog(
    onDismiss: () -> Unit,
    onIconSelected: (IconItem, taskName: String) -> Unit
) {
    var mode by remember { mutableStateOf(PickerMode.BROWSE_CATEGORIES) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<IconCategory?>(null) }
    var selectedIcon by remember { mutableStateOf<IconItem?>(null) }
    var taskName by remember { mutableStateOf("") }

    // Name confirmation step
    selectedIcon?.let { icon ->
        AlertDialog(
            onDismissRequest = { selectedIcon = null },
            title = { Text("Name this item") },
            text = {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onIconSelected(icon, taskName.trim()) },
                    enabled = taskName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedIcon = null }) { Text("Back") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mode == PickerMode.BROWSE_ITEMS) {
                    IconButton(onClick = {
                        mode = PickerMode.BROWSE_CATEGORIES
                        selectedCategory = null
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = when (mode) {
                        PickerMode.SEARCH -> "Search icons"
                        PickerMode.BROWSE_CATEGORIES -> "Choose category"
                        PickerMode.BROWSE_ITEMS -> selectedCategory?.displayName ?: "Items"
                    },
                    modifier = Modifier.weight(1f)
                )
                if (mode != PickerMode.SEARCH) {
                    IconButton(onClick = { mode = PickerMode.SEARCH }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }
        },
        text = {
            when (mode) {
                PickerMode.SEARCH -> SearchPane(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onIconPick = { icon ->
                        selectedIcon = icon
                        taskName = icon.displayName
                    }
                )
                PickerMode.BROWSE_CATEGORIES -> CategoryList(
                    onCategorySelected = { cat ->
                        selectedCategory = cat
                        mode = PickerMode.BROWSE_ITEMS
                    }
                )
                PickerMode.BROWSE_ITEMS -> IconGrid(
                    icons = IconCatalog.byCategory[selectedCategory] ?: emptyList(),
                    onIconPick = { icon ->
                        selectedIcon = icon
                        taskName = icon.displayName
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SearchPane(
    query: String,
    onQueryChange: (String) -> Unit,
    onIconPick: (IconItem) -> Unit
) {
    val results = remember(query) { IconCatalog.findByName(query) }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        IconGrid(icons = results, onIconPick = onIconPick)
    }
}

@Composable
private fun CategoryList(onCategorySelected: (IconCategory) -> Unit) {
    LazyColumn {
        items(IconCategory.values().toList()) { category ->
            ListItem(
                headlineContent = { Text(category.displayName) },
                modifier = Modifier.clickable { onCategorySelected(category) }
            )
            Divider()
        }
    }
}

@Composable
private fun IconGrid(icons: List<IconItem>, onIconPick: (IconItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 64.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(icons) { icon ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onIconPick(icon) }
                    .padding(4.dp)
            ) {
                GroceryIcon(
                    iconName = icon.name,
                    contentDescription = icon.displayName,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
