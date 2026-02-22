package com.fridgelist.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.model.*
import com.fridgelist.app.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val tiles: List<Tile> = emptyList(),
    val gridConfig: GridConfig = GridConfig(),
    val isEditMode: Boolean = false,
    val error: AppError? = null,
    val lastSyncTime: Long = 0L,
    val syncFailed: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tileRepository: TileRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var periodicSyncJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                tileRepository.tiles,
                appSettings.gridConfig,
                appSettings.lastSyncTime
            ) { tiles, config, lastSync ->
                Triple(tiles, config, lastSync)
            }.collect { (tiles, config, lastSync) ->
                _uiState.update { it.copy(tiles = tiles, gridConfig = config, lastSyncTime = lastSync) }
            }
        }
        startPeriodicSync()
        syncNow()
    }

    fun onTileTap(tile: Tile) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch {
            val result = tileRepository.toggleTile(tile)
            when (result) {
                is SyncResult.AuthRequired -> _uiState.update { it.copy(error = AppError.AuthRequired) }
                is SyncResult.Offline -> _uiState.update { it.copy(error = AppError.Offline) }
                is SyncResult.Failure -> _uiState.update {
                    it.copy(error = AppError.SyncFailed(result.message), syncFailed = true)
                }
                is SyncResult.Success -> _uiState.update { it.copy(error = null) }
            }
        }
    }

    fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }

    fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, syncFailed = false) }
    }

    fun syncNow() {
        viewModelScope.launch {
            when (val result = tileRepository.syncFromProvider()) {
                is SyncResult.Success -> _uiState.update {
                    it.copy(syncFailed = false, error = if (it.error is AppError.Offline) null else it.error)
                }
                is SyncResult.Failure -> _uiState.update { it.copy(syncFailed = true) }
                is SyncResult.AuthRequired -> _uiState.update { it.copy(error = AppError.AuthRequired) }
                is SyncResult.Offline -> _uiState.update { it.copy(syncFailed = true) }
            }
        }
    }

    private fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            while (true) {
                delay(5 * 1000L)
                //delay(10 * 60 * 1000L) // 10 minutes
                syncNow()
            }
        }
    }

    // Edit mode operations
    fun moveTile(tileId: Long, newRow: Int, newCol: Int) {
        viewModelScope.launch { tileRepository.moveTile(tileId, newRow, newCol) }
    }

    fun removeTile(tile: Tile) {
        viewModelScope.launch { tileRepository.removeTile(tile) }
    }

    fun addTile(row: Int, col: Int, iconName: String, taskName: String) {
        viewModelScope.launch { tileRepository.addTile(row, col, iconName, taskName) }
    }

    fun updateTile(tile: Tile) {
        viewModelScope.launch { tileRepository.updateTile(tile) }
    }
}
