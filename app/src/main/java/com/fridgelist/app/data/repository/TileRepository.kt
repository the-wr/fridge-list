package com.fridgelist.app.data.repository

import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.db.TileDao
import com.fridgelist.app.data.db.TileEntity
import com.fridgelist.app.data.model.*
import com.fridgelist.app.provider.TodoProvider
import com.fridgelist.app.provider.UnauthorizedException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val tileDao: TileDao,
    private val appSettings: AppSettings,
    private val providerFactory: ProviderFactory
) {
    /** Live stream of the active (on-grid) tiles. */
    val tiles: Flow<List<Tile>> = tileDao.observeActiveTiles().map { entities ->
        entities.map { it.toTile() }
    }

    /**
     * Toggle a tile's state. Applies optimistic UI update immediately,
     * then syncs to the provider. Returns the sync result.
     */
    suspend fun toggleTile(tile: Tile): SyncResult {
        val newState = if (tile.state == TileState.NEEDED) TileState.NOT_NEEDED else TileState.NEEDED
        // Optimistic update
        tileDao.updateState(tile.id, newState.name)

        val provider = providerFactory.current() ?: return SyncResult.Failure("No provider configured")

        // Resolve task ID, creating the task in the provider if it doesn't exist yet.
        // Track whether we just created it — a freshly created task is already open,
        // so we must not call reopenTask on it.
        val justCreated: Boolean
        val taskId: String
        if (tile.taskId != null) {
            taskId = tile.taskId
            justCreated = false
        } else {
            val listId = appSettings.providerListId.first() ?: return SyncResult.Failure("No list configured")
            var createResult = provider.createTask(listId, tile.taskName)
            if (createResult.exceptionOrNull() is UnauthorizedException && provider.refreshToken()) {
                createResult = provider.createTask(listId, tile.taskName)
            }
            taskId = createResult.getOrNull()?.also { newId ->
                tileDao.updateTaskId(tile.id, newId)
            } ?: return if (createResult.exceptionOrNull() is UnauthorizedException) SyncResult.AuthRequired
                        else SyncResult.Failure(createResult.exceptionOrNull()?.message ?: "Create task failed")
            justCreated = true
        }

        // A just-created task is already open (NEEDED); only sync state if it differs.
        if (justCreated && newState == TileState.NEEDED) return SyncResult.Success

        val syncResult = withTokenRefresh(provider) {
            if (newState == TileState.NEEDED) provider.reopenTask(taskId)
            else provider.completeTask(taskId)
        }

        if (syncResult !is SyncResult.Success) {
            // Revert optimistic update on failure
            tileDao.updateState(tile.id, tile.state.name)
        }
        return syncResult
    }

    /**
     * Sync all tile states from the provider.
     */
    suspend fun syncFromProvider(): SyncResult {
        val provider = providerFactory.current() ?: return SyncResult.Failure("No provider configured")
        val listId = appSettings.providerListId.first() ?: return SyncResult.Failure("No list configured")

        var tasksResult = provider.getTasks(listId)
        if (tasksResult.exceptionOrNull() is UnauthorizedException && provider.refreshToken()) {
            tasksResult = provider.getTasks(listId)
        }
        if (tasksResult.isFailure) {
            val ex = tasksResult.exceptionOrNull()
            return if (ex is UnauthorizedException) SyncResult.AuthRequired
                   else SyncResult.Failure(ex?.message ?: "Sync failed")
        }

        val tasks = tasksResult.getOrThrow()
        val taskById = tasks.associateBy { it.id }
        val taskByName = tasks.associateBy { it.name.lowercase() }

        val allTiles = tileDao.observeActiveTiles().first()
        for (entity in allTiles) {
            val task = entity.taskId?.let { taskById[it] }
                ?: taskByName[entity.taskName.lowercase()]

            val newState = when {
                // Task returned by provider — use its completion status
                task != null -> if (task.isComplete) TileState.NOT_NEEDED else TileState.NEEDED
                // Tile has a known task ID but task is absent from active list — completed externally
                entity.taskId != null -> TileState.NOT_NEEDED
                // No task ID and no name match — task not created in provider yet, leave unchanged
                else -> TileState.valueOf(entity.state)
            }

            if (entity.taskId == null && task != null) {
                tileDao.updateTaskId(entity.id, task.id)
            }
            if (TileState.valueOf(entity.state) != newState) {
                tileDao.updateState(entity.id, newState.name)
            }
        }

        appSettings.setLastSyncTime(System.currentTimeMillis())
        return SyncResult.Success
    }

    /**
     * Populate the grid with the default tile set.
     */
    suspend fun populateDefaultGrid() {
        tileDao.deleteAll()
        val entities = DefaultGrid.tiles.map { dt ->
            TileEntity(
                gridRow = dt.row,
                gridCol = dt.col,
                iconName = dt.iconName,
                taskName = dt.taskName,
                taskId = null,
                state = TileState.NOT_NEEDED.name
            )
        }
        tileDao.insertAll(entities)
        appSettings.setGridConfig(GridConfig(DefaultGrid.columns, DefaultGrid.rows))
    }

    /**
     * Populate grid by importing tasks from the connected provider list.
     * Tasks whose name matches a known icon are placed; others are ignored.
     */
    suspend fun populateFromProvider(): SyncResult {
        val provider = providerFactory.current() ?: return SyncResult.Failure("No provider")
        val listId = appSettings.providerListId.first() ?: return SyncResult.Failure("No list")
        val tasksResult = provider.getTasks(listId)
        if (tasksResult.isFailure) return SyncResult.Failure(tasksResult.exceptionOrNull()?.message ?: "Failed")

        val tasks = tasksResult.getOrThrow()
        tileDao.deleteAll()

        val catalog = IconCatalog.all.associateBy { it.displayName.lowercase() }
        var row = 0; var col = 0
        val config = appSettings.gridConfig.first()

        for (task in tasks) {
            val icon = catalog[task.name.lowercase()] ?: continue
            tileDao.insert(
                TileEntity(
                    gridRow = row,
                    gridCol = col,
                    iconName = icon.name,
                    taskName = task.name,
                    taskId = task.id,
                    state = if (task.isComplete) TileState.NOT_NEEDED.name else TileState.NEEDED.name
                )
            )
            col++
            if (col >= config.columns) { col = 0; row++ }
            if (row >= config.rows) break
        }
        return SyncResult.Success
    }

    suspend fun addTile(row: Int, col: Int, iconName: String, taskName: String): Long {
        return tileDao.insert(
            TileEntity(
                gridRow = row,
                gridCol = col,
                iconName = iconName,
                taskName = taskName,
                taskId = null,
                state = TileState.NOT_NEEDED.name
            )
        )
    }

    suspend fun removeTile(tile: Tile) {
        val entity = tileDao.getById(tile.id) ?: return
        tileDao.delete(entity)
    }

    suspend fun updateTile(tile: Tile) {
        tileDao.update(TileEntity.fromTile(tile))
        if (tile.taskId == null) {
            linkToProviderTaskIfExists(tile)
        }
    }

    private suspend fun linkToProviderTaskIfExists(tile: Tile) {
        val provider = providerFactory.current() ?: return
        val listId = appSettings.providerListId.first() ?: return
        val existingTask = provider.getTasks(listId).getOrNull()
            ?.firstOrNull { it.name.equals(tile.taskName, ignoreCase = true) }
            ?: return
        tileDao.updateTaskId(tile.id, existingTask.id)
        val newState = if (existingTask.isComplete) TileState.NOT_NEEDED else TileState.NEEDED
        tileDao.updateState(tile.id, newState.name)
    }

    suspend fun moveTile(id: Long, newRow: Int, newCol: Int) {
        // If there's a tile at the target, swap
        val existing = tileDao.getAtPosition(newRow, newCol)
        val moving = tileDao.getById(id) ?: return
        if (existing != null && existing.id != id) {
            tileDao.moveTile(existing.id, moving.gridRow, moving.gridCol)
        }
        tileDao.moveTile(id, newRow, newCol)
    }

    /**
     * Resize grid. Tiles that fall outside the new dimensions go to the off-grid store.
     */
    /**
     * Calls [block], and if it returns [SyncResult.AuthRequired], attempts one token
     * refresh before retrying. Returns [SyncResult.AuthRequired] if the retry also fails.
     */
    private suspend fun withTokenRefresh(
        provider: TodoProvider,
        block: suspend () -> SyncResult
    ): SyncResult {
        val result = block()
        if (result !is SyncResult.AuthRequired) return result
        return if (provider.refreshToken()) block() else SyncResult.AuthRequired
    }

    suspend fun resizeGrid(newConfig: GridConfig) {
        val allTiles = tileDao.observeActiveTiles().first()
        for (entity in allTiles) {
            if (entity.gridRow >= newConfig.rows || entity.gridCol >= newConfig.columns) {
                tileDao.moveToOffGrid(entity.id)
            }
        }
        // Restore any previously off-grid tiles that now fit
        val offGrid = tileDao.getOffGridTiles()
        for (entity in offGrid) {
            if (entity.gridRow < newConfig.rows && entity.gridCol < newConfig.columns) {
                val occupant = tileDao.getAtPosition(entity.gridRow, entity.gridCol)
                if (occupant == null) {
                    tileDao.moveTile(entity.id, entity.gridRow, entity.gridCol)
                }
            }
        }
        appSettings.setGridConfig(newConfig)
    }
}
