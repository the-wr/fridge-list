package com.fridgelist.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.datastore.TokenStore
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep {
    CHOOSE_PROVIDER,
    AUTHENTICATE,
    SELECT_LIST,
    SET_GRID,
    INITIAL_POPULATION
}

data class SetupUiState(
    val step: SetupStep = SetupStep.CHOOSE_PROVIDER,
    val selectedProvider: ProviderType? = null,
    val availableLists: List<Pair<String, String>> = emptyList(), // id to name
    val selectedListId: String? = null,
    val selectedListName: String? = null,
    val gridColumns: Int = 8,
    val gridRows: Int = 11,
    val isLandscape: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val tokenStore: TokenStore,
    private val tileRepository: TileRepository
) : ViewModel() {

    val isSetupComplete: Flow<Boolean> = appSettings.isSetupComplete

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun selectProvider(provider: ProviderType) {
        _uiState.update { it.copy(selectedProvider = provider, step = SetupStep.AUTHENTICATE) }
    }

    fun onAuthComplete(accessToken: String, refreshToken: String?) {
        val provider = _uiState.value.selectedProvider ?: return
        tokenStore.saveTokens(provider, accessToken, refreshToken)
        _uiState.update { it.copy(step = SetupStep.SELECT_LIST, isLoading = true) }
        loadLists()
    }

    private fun loadLists() {
        // In a real implementation, fetch lists from the provider here
        // For now, move to next step with placeholder
        _uiState.update {
            it.copy(
                isLoading = false,
                availableLists = listOf("default" to "Shopping List"),
                step = SetupStep.SELECT_LIST
            )
        }
    }

    fun selectList(id: String, name: String) {
        _uiState.update {
            it.copy(
                selectedListId = id,
                selectedListName = name,
                step = SetupStep.SET_GRID
            )
        }
    }

    fun setGridDimensions(columns: Int, rows: Int, landscape: Boolean) {
        _uiState.update {
            it.copy(gridColumns = columns, gridRows = rows, isLandscape = landscape)
        }
    }

    fun proceedToPopulation() {
        _uiState.update { it.copy(step = SetupStep.INITIAL_POPULATION) }
    }

    fun populateEmpty(onComplete: () -> Unit) {
        finishSetup(onComplete)
    }

    fun populateDefault(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tileRepository.populateDefaultGrid()
            finishSetup(onComplete)
        }
    }

    fun populateFromList(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tileRepository.populateFromProvider()
            finishSetup(onComplete)
        }
    }

    private fun finishSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val provider = state.selectedProvider ?: return@launch
            val listId = state.selectedListId ?: return@launch
            val listName = state.selectedListName ?: return@launch

            appSettings.setProvider(provider, listId, listName)
            appSettings.setGridConfig(GridConfig(state.gridColumns, state.gridRows))
            appSettings.setOrientation(state.isLandscape)
            appSettings.setSetupComplete(true)

            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }
}
