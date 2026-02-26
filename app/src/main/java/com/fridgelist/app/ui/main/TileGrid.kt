package com.fridgelist.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.Tile
import com.fridgelist.app.data.model.TileState
import com.fridgelist.app.ui.common.GroceryIcon
import kotlin.math.roundToInt

@Composable
fun TileGrid(
    tiles: List<Tile>,
    config: GridConfig,
    isEditMode: Boolean,
    onTileTap: (Tile) -> Unit,
    onTileLongPress: (Tile) -> Unit,
    onEmptySlotTap: (row: Int, col: Int) -> Unit,
    onEmptySlotLongPress: () -> Unit,
    onMoveTile: (tileId: Long, newRow: Int, newCol: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingTileId by remember { mutableStateOf<Long?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Optimistic pending move: applied immediately on drop so the tile renders at its
    // new position in the same frame the ghost disappears, eliminating the one-frame
    // flash where the source tile would reappear at the old position before the ViewModel
    // recomposition arrives. Cleared as soon as `tiles` reflects the committed change.
    var pendingMove by remember { mutableStateOf<Triple<Long, Int, Int>?>(null) }
    LaunchedEffect(tiles) { pendingMove = null }

    val effectiveTiles = remember(tiles, pendingMove) {
        val pm = pendingMove ?: return@remember tiles
        val (movedId, newRow, newCol) = pm
        val movedTile = tiles.find { it.id == movedId } ?: return@remember tiles
        val oldRow = movedTile.gridRow
        val oldCol = movedTile.gridCol
        tiles.map { t ->
            when {
                t.id == movedId -> t.copy(gridRow = newRow, gridCol = newCol)
                t.gridRow == newRow && t.gridCol == newCol -> t.copy(gridRow = oldRow, gridCol = oldCol)
                else -> t
            }
        }
    }

    val tileMap = remember(effectiveTiles) {
        effectiveTiles.associateBy { it.gridRow to it.gridCol }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tileSize: Dp = minOf(maxWidth / config.columns, maxHeight / config.rows)
        val density = LocalDensity.current
        val tileSizePx = with(density) { tileSize.toPx() }
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val gridWidthPx = config.columns * tileSizePx
        val gridHeightPx = config.rows * tileSizePx
        val gridOffsetX = (containerWidthPx - gridWidthPx) / 2f
        val gridOffsetY = (containerHeightPx - gridHeightPx) / 2f

        fun offsetToCell(offset: Offset): Pair<Int, Int>? {
            if (offset.x < gridOffsetX || offset.y < gridOffsetY ||
                offset.x >= gridOffsetX + gridWidthPx ||
                offset.y >= gridOffsetY + gridHeightPx
            ) return null
            val col = ((offset.x - gridOffsetX) / tileSizePx).toInt().coerceIn(0, config.columns - 1)
            val row = ((offset.y - gridOffsetY) / tileSizePx).toInt().coerceIn(0, config.rows - 1)
            return row to col
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Grid
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
                            val isHovered = hoveredCell == row to col
                            // Top-left of this cell in BoxWithConstraints coordinates
                            val tileOriginX = gridOffsetX + col * tileSizePx
                            val tileOriginY = gridOffsetY + row * tileSizePx
                            if (tile != null) {
                                TileCell(
                                    tile = tile,
                                    size = tileSize,
                                    isEditMode = isEditMode,
                                    isDragging = draggingTileId == tile.id,
                                    isDropTarget = isEditMode && isHovered && draggingTileId != tile.id,
                                    onTap = { onTileTap(tile) },
                                    onLongPress = { onTileLongPress(tile) },
                                    onDragStarted = { localOffset ->
                                        draggingTileId = tile.id
                                        dragPosition = Offset(
                                            tileOriginX + localOffset.x,
                                            tileOriginY + localOffset.y
                                        )
                                        hoveredCell = row to col
                                    },
                                    onDragDelta = { delta ->
                                        dragPosition += delta
                                        offsetToCell(dragPosition)?.let { hoveredCell = it }
                                    },
                                    onDragEnded = {
                                        val tileId = draggingTileId
                                        val cell = hoveredCell
                                        // Apply optimistic move and clear drag state atomically
                                        // so all three land in the same recomposition frame.
                                        if (tileId != null && cell != null &&
                                            (cell.first != tile.gridRow || cell.second != tile.gridCol)
                                        ) {
                                            pendingMove = Triple(tileId, cell.first, cell.second)
                                            onMoveTile(tileId, cell.first, cell.second)
                                        }
                                        draggingTileId = null
                                        hoveredCell = null
                                    },
                                    onDragCancelled = {
                                        draggingTileId = null
                                        hoveredCell = null
                                    }
                                )
                            } else {
                                EmptyCell(
                                    size = tileSize,
                                    isEditMode = isEditMode,
                                    isDropTarget = isEditMode && isHovered && draggingTileId != null,
                                    onTap = { onEmptySlotTap(row, col) },
                                    onLongPress = onEmptySlotLongPress
                                )
                            }
                        }
                    }
                }
            }

            // Floating drag ghost
            val dTile = draggingTileId?.let { id -> effectiveTiles.find { it.id == id } }
            if (dTile != null) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragPosition.x - tileSizePx / 2f).roundToInt(),
                                (dragPosition.y - tileSizePx / 2f).roundToInt()
                            )
                        }
                        .size(tileSize)
                        .graphicsLayer {
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                        .shadow(16.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    GroceryIcon(
                        iconName = dTile.iconName,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
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
    isDragging: Boolean,
    isDropTarget: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStarted: (localOffset: Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnded: () -> Unit,
    onDragCancelled: () -> Unit
) {
    val isNeeded = tile.state == TileState.NEEDED

    // rememberUpdatedState lets the long-lived pointerInput coroutine always call the
    // latest lambdas from the most recent recomposition, avoiding stale captures.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDragStarted by rememberUpdatedState(onDragStarted)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnded by rememberUpdatedState(onDragEnded)
    val currentOnDragCancelled by rememberUpdatedState(onDragCancelled)

    Box(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isDropTarget)
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                else
                    Modifier
            )
            .then(
                if (isEditMode) {
                    // In edit mode: a single pointerInput handles both tap and immediate drag.
                    // Ghost appears once the finger moves past touchSlop; short press = tap.
                    // Keyed on Unit so tile swaps (which change the tile at this slot but not
                    // the composable itself) never restart the coroutine mid-session.
                    Modifier.pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragStarted = false
                            var dragEnded = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes
                                        .firstOrNull { it.id == down.id } ?: break

                                    if (!change.pressed) {
                                        // Finger lifted — decide tap vs drag-end
                                        change.consume()
                                        if (dragStarted) {
                                            dragEnded = true
                                            currentOnDragEnded()
                                        } else {
                                            currentOnTap()
                                        }
                                        break
                                    }

                                    if (!dragStarted) {
                                        // Cross touchSlop → start drag, ghost appears
                                        if ((change.position - down.position).getDistance() > slop) {
                                            dragStarted = true
                                            currentOnDragStarted(change.position)
                                            change.consume()
                                        }
                                    } else {
                                        val delta = change.position - change.previousPosition
                                        change.consume()
                                        currentOnDragDelta(delta)
                                    }
                                }
                            } finally {
                                // If drag started but never ended normally (e.g. cancelled by
                                // the system), notify the caller to clean up ghost state.
                                if (dragStarted && !dragEnded) currentOnDragCancelled()
                            }
                        }
                    }
                } else {
                    // In normal mode: standard tap + long-press-to-enter-edit-mode.
                    Modifier.combinedClickable(
                        onClick = { currentOnTap() },
                        onLongClick = { currentOnLongPress() }
                    )
                }
            )
            .alpha(if (isDragging) 0.25f else 1f),
        contentAlignment = Alignment.Center
    ) {
        val grayscaleMatrix = remember {
            ColorMatrix().apply { setToSaturation(0f) }
        }

        GroceryIcon(
            iconName = tile.iconName,
            contentDescription = tile.taskName,
            modifier = Modifier
                .fillMaxSize()
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
    isDropTarget: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                when {
                    isDropTarget -> Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                    isEditMode -> Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                    else -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
                }
            )
    )
}
