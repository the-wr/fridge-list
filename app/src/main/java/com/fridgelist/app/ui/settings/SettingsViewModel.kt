package com.fridgelist.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val gridColumns: Int = 8,
    val gridRows: Int = 11,
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

    private var originalColumns = 8
    private var originalRows = 11

    init {
        viewModelScope.launch {
            combine(
                appSettings.gridConfig,
                appSettings.providerListName
            ) { config, listName ->
                config to listName
            }.collect { (config, listName) ->
                originalColumns = config.columns
                originalRows = config.rows
                _uiState.update {
                    it.copy(
                        gridColumns = config.columns,
                        gridRows = config.rows,
                        providerName = listName
                    )
                }
            }
        }
    }

    fun setColumns(cols: Int) {
        _uiState.update {
            it.copy(
                gridColumns = cols,
                pendingResize = cols != originalColumns || it.gridRows != originalRows
            )
        }
    }

    fun setRows(rows: Int) {
        _uiState.update {
            it.copy(
                gridRows = rows,
                pendingResize = it.gridColumns != originalColumns || rows != originalRows
            )
        }
    }

    fun applyGridResize() {
        viewModelScope.launch {
            val state = _uiState.value
            tileRepository.resizeGrid(GridConfig(state.gridColumns, state.gridRows))
            originalColumns = state.gridColumns
            originalRows = state.gridRows
            _uiState.update { it.copy(pendingResize = false) }
        }
    }

    fun reconnectProvider() {
        // TODO: launch OAuth flow
    }
}
