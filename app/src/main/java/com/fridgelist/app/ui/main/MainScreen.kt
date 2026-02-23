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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridgelist.app.data.model.AppError
import com.fridgelist.app.data.model.Tile
import com.fridgelist.app.ui.settings.SettingsDialog

@Composable
fun MainScreen(
    onNavigateToSetup: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // null = closed; Triple(row, col, tile) — tile==null means add mode
    var tileEditTarget by remember { mutableStateOf<Triple<Int, Int, Tile?>?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main grid
        TileGrid(
            tiles = uiState.tiles,
            config = uiState.gridConfig,
            isEditMode = uiState.isEditMode,
            onTileTap = { tile ->
                if (uiState.isEditMode) {
                    tileEditTarget = Triple(tile.gridRow, tile.gridCol, tile)
                } else {
                    viewModel.onTileTap(tile)
                }
            },
            onTileLongPress = { _ ->
                if (!uiState.isEditMode) viewModel.enterEditMode()
            },
            onEmptySlotTap = { row, col ->
                if (uiState.isEditMode) {
                    tileEditTarget = Triple(row, col, null)
                }
            },
            onEmptySlotLongPress = {
                if (!uiState.isEditMode) viewModel.enterEditMode()
            },
            onMoveTile = { id, row, col -> viewModel.moveTile(id, row, col) },
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
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showSettings = true },
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
                    message = "No internet connection — tap to dismiss",
                    icon = { Icon(Icons.Filled.WifiOff, null) },
                    onClick = { viewModel.dismissError() }
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

    // Unified tile add / edit dialog
    tileEditTarget?.let { (row, col, tile) ->
        TileEditorDialog(
            existingTile = tile,
            onDismiss = { tileEditTarget = null },
            onSave = { iconName, taskName ->
                if (tile != null) {
                    val taskId = if (taskName == tile.taskName) tile.taskId else null
                    viewModel.updateTile(tile.copy(iconName = iconName, taskName = taskName, taskId = taskId))
                } else {
                    viewModel.addTile(row, col, iconName, taskName)
                }
                tileEditTarget = null
            },
            onRemove = {
                viewModel.removeTile(tile!!)
                tileEditTarget = null
            }
        )
    }

    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onNavigateToSetup = onNavigateToSetup
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
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Connection lost",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your account needs to reconnect to sync your shopping list.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
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
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
