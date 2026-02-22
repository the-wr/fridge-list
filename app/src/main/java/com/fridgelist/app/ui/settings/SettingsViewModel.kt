package com.fridgelist.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val gridColumns: Int = 8,
    val gridRows: Int = 11,
    val providerType: ProviderType? = null,
    val providerName: String? = null,
    val pendingResize: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val tileRepository: TileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Values at dialog-open time; Reset reverts to these
    private var sessionOriginalColumns = 8
    private var sessionOriginalRows = 11

    private var resizeJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                appSettings.gridConfig.distinctUntilChanged(),
                appSettings.providerType.distinctUntilChanged(),
                appSettings.providerListName.distinctUntilChanged()
            ) { config, providerType, listName ->
                Triple(config, providerType, listName)
            }.collect { (config, providerType, listName) ->
                _uiState.update {
                    it.copy(
                        gridColumns = config.columns,
                        gridRows = config.rows,
                        providerType = providerType,
                        providerName = listName,
                        pendingResize = config.columns != sessionOriginalColumns || config.rows != sessionOriginalRows
                    )
                }
            }
        }
    }

    /** Called when the dialog opens — snapshots current saved state as the reset target. */
    fun startSession() {
        val current = _uiState.value
        sessionOriginalColumns = current.gridColumns
        sessionOriginalRows = current.gridRows
        _uiState.update { it.copy(pendingResize = false) }
    }

    fun setColumns(cols: Int) {
        _uiState.update {
            it.copy(
                gridColumns = cols,
                pendingResize = cols != sessionOriginalColumns || it.gridRows != sessionOriginalRows
            )
        }
        scheduleResize()
    }

    fun setRows(rows: Int) {
        _uiState.update {
            it.copy(
                gridRows = rows,
                pendingResize = it.gridColumns != sessionOriginalColumns || rows != sessionOriginalRows
            )
        }
        scheduleResize()
    }

    private fun scheduleResize() {
        resizeJob?.cancel()
        resizeJob = viewModelScope.launch {
            delay(300L)
            val state = _uiState.value
            tileRepository.resizeGrid(GridConfig(state.gridColumns, state.gridRows))
        }
    }

    fun resetGrid() {
        resizeJob?.cancel()
        _uiState.update {
            it.copy(
                gridColumns = sessionOriginalColumns,
                gridRows = sessionOriginalRows,
                pendingResize = false
            )
        }
        viewModelScope.launch {
            tileRepository.resizeGrid(GridConfig(sessionOriginalColumns, sessionOriginalRows))
        }
    }

    fun reconnectProvider() {
        // TODO: launch OAuth flow
    }
}
