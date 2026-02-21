package com.fridgelist.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.Tile
import com.fridgelist.app.data.model.TileState
import com.fridgelist.app.ui.common.GroceryIcon

@Composable
fun TileGrid(
    tiles: List<Tile>,
    config: GridConfig,
    isEditMode: Boolean,
    onTileTap: (Tile) -> Unit,
    onTileLongPress: (Tile) -> Unit,
    onEmptySlotTap: (row: Int, col: Int) -> Unit,
    onEmptySlotLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileMap = remember(tiles) {
        tiles.associateBy { it.gridRow to it.gridCol }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tileSize = minOf(
            maxWidth / config.columns,
            maxHeight / config.rows
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            repeat(config.rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(config.columns) { col ->
                        val tile = tileMap[row to col]
                        if (tile != null) {
                            TileCell(
                                tile = tile,
                                size = tileSize,
                                isEditMode = isEditMode,
                                onTap = { onTileTap(tile) },
                                onLongPress = { onTileLongPress(tile) }
                            )
                        } else {
                            EmptyCell(
                                size = tileSize,
                                isEditMode = isEditMode,
                                onTap = { onEmptySlotTap(row, col) },
                                onLongPress = onEmptySlotLongPress
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileCell(
    tile: Tile,
    size: Dp,
    isEditMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val isNeeded = tile.state == TileState.NEEDED

    Box(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isEditMode) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                else Modifier
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        val grayscaleMatrix = remember {
            ColorMatrix().apply { setToSaturation(0f) }
        }

        GroceryIcon(
            iconName = tile.iconName,
            contentDescription = tile.taskName,
            modifier = Modifier
                .fillMaxSize(0.82f)
                .alpha(if (isNeeded) 1f else 0.35f),
            colorFilter = if (isNeeded) null else ColorFilter.colorMatrix(grayscaleMatrix)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptyCell(
    size: Dp,
    isEditMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isEditMode) Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                else Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
            )
    )
}
