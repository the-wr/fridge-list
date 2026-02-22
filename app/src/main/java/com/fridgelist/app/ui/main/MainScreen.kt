package com.fridgelist.app.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridgelist.app.data.model.AppError
import com.fridgelist.app.data.model.Tile

@Composable
fun MainScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var editTile by remember { mutableStateOf<Tile?>(null) }
    var addTilePosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main grid
        TileGrid(
            tiles = uiState.tiles,
            config = uiState.gridConfig,
            isEditMode = uiState.isEditMode,
            onTileTap = { tile ->
                if (uiState.isEditMode) {
                    editTile = tile
                } else {
                    viewModel.onTileTap(tile)
                }
            },
            onTileLongPress = { _ ->
                if (!uiState.isEditMode) viewModel.enterEditMode()
                viewModel.onEditModeActivity()
            },
            onEmptySlotTap = { row, col ->
                if (uiState.isEditMode) {
                    addTilePosition = row to col
                }
            },
            onEmptySlotLongPress = {
                if (!uiState.isEditMode) viewModel.enterEditMode()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Edit mode top bar
        AnimatedVisibility(
            visible = uiState.isEditMode,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateToSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
                Button(
                    onClick = { viewModel.exitEditMode() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Done")
                }
            }
        }

        // Error / status banners
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            when (val error = uiState.error) {
                is AppError.Offline -> ErrorBanner(
                    message = "No internet connection",
                    icon = { Icon(Icons.Filled.WifiOff, null) }
                )
                is AppError.SyncFailed -> ErrorBanner(
                    message = "Sync failed: ${error.message} — tap to retry",
                    onClick = { viewModel.syncNow(); viewModel.dismissError() }
                )
                is AppError.AuthRequired -> AuthRequiredOverlay(
                    onReconnect = { /* Launch OAuth flow */ }
                )
                null -> {}
            }
        }

        // Sync failed subtle indicator (bottom right)
        if (uiState.syncFailed && uiState.error == null) {
            SyncIndicator(
                lastSyncTime = uiState.lastSyncTime,
                onSyncTap = { viewModel.syncNow() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }

    // Edit tile dialog
    editTile?.let { tile ->
        TileEditorDialog(
            tile = tile,
            onDismiss = { editTile = null },
            onChangeIcon = {
                editTile = null
                addTilePosition = tile.gridRow to tile.gridCol
                viewModel.removeTile(tile)
            },
            onSaveName = { newName ->
                viewModel.updateTile(tile.copy(taskName = newName))
                editTile = null
            },
            onRemove = {
                viewModel.removeTile(tile)
                editTile = null
            }
        )
    }

    // Add tile / icon picker
    addTilePosition?.let { (row, col) ->
        IconPickerDialog(
            onDismiss = { addTilePosition = null },
            onIconSelected = { icon, taskName ->
                viewModel.addTile(row, col, icon.name, taskName)
                addTilePosition = null
            }
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.invoke()
            if (icon != null) Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthRequiredOverlay(onReconnect: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Connection lost",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your account needs to reconnect to sync your shopping list.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onReconnect) {
                Text("Reconnect")
            }
        }
    }
}

@Composable
private fun SyncIndicator(
    lastSyncTime: Long,
    onSyncTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onSyncTap, modifier = modifier) {
        Icon(
            Icons.Filled.Sync,
            contentDescription = "Sync failed — tap to retry",
            tint = Color.White.copy(alpha = 0.5f)
        )
    }
}
