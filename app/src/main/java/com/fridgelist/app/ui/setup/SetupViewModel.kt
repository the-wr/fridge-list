package com.fridgelist.app.ui.setup

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgelist.app.auth.OAuthConfig
import com.fridgelist.app.auth.OAuthConfigs
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.datastore.TokenStore
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.data.repository.ProviderFactory
import com.fridgelist.app.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Log
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.NoClientAuthentication
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "FridgeList.OAuth"

data class ProviderListInfo(val id: String, val name: String, val totalTasks: Int)

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
    val availableLists: List<ProviderListInfo> = emptyList(),
    val selectedListId: String? = null,
    val selectedListName: String? = null,
    val gridColumns: Int = 8,
    val gridRows: Int = 11,
    val isLandscape: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthLoading: Boolean = false,
    val error: String? = null,
    val authError: String? = null,
    val hasExistingGrid: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appSettings: AppSettings,
    private val tokenStore: TokenStore,
    private val tileRepository: TileRepository,
    private val providerFactory: ProviderFactory,
) : ViewModel() {

    val isSetupComplete: Flow<Boolean> = appSettings.isSetupComplete

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private var authService: AuthorizationService? = null

    override fun onCleared() {
        super.onCleared()
        authService?.dispose()
        authService = null
    }

    fun selectProvider(provider: ProviderType) {
        _uiState.update { it.copy(selectedProvider = provider, step = SetupStep.AUTHENTICATE, authError = null) }
    }

    fun goBack() {
        _uiState.update { state ->
            when (state.step) {
                SetupStep.AUTHENTICATE -> state.copy(step = SetupStep.CHOOSE_PROVIDER)
                SetupStep.SELECT_LIST -> state.copy(step = SetupStep.AUTHENTICATE)
                SetupStep.SET_GRID -> state.copy(step = SetupStep.SELECT_LIST)
                SetupStep.INITIAL_POPULATION -> state.copy(step = SetupStep.SET_GRID)
                else -> state
            }
        }
    }

    /**
     * Builds the AppAuth intent that launches the provider's OAuth sign-in page in a
     * Chrome Custom Tab. The caller is responsible for launching this intent via
     * [ActivityResultLauncher] and passing the result to [handleOAuthResult].
     *
     * Returns null if the intent could not be built (error is surfaced via [uiState]).
     */
    fun buildAuthIntent(provider: ProviderType): Intent? {
        return try {
            val config = OAuthConfigs.forProvider(provider)

            if (config.clientId.isEmpty()) {
                _uiState.update {
                    it.copy(authError = "No OAuth client ID configured for ${provider.name}. " +
                        "Register an OAuth app with the provider and add the credentials to BuildConfig.")
                }
                return null
            }
            val requestBuilder = AuthorizationRequest.Builder(
                config.serviceConfig,
                config.clientId,
                ResponseTypeValues.CODE,
                config.redirectUri,
            ).setScope(config.scopes)

            // Providers that use a client_secret don't need PKCE; disable it so the
            // token endpoint doesn't reject the request.
            if (config.clientSecret != null) {
                requestBuilder.setCodeVerifier(null)
            }

            val service = getOrCreateAuthService()
            service.getAuthorizationRequestIntent(requestBuilder.build())
        } catch (e: Exception) {
            _uiState.update { it.copy(authError = "Failed to start sign-in: ${e.message}") }
            null
        }
    }

    /**
     * Called with the [Intent] returned from the OAuth activity result.
     * Extracts the authorization response and exchanges the code for tokens.
     */
    fun handleOAuthResult(data: Intent?) {
        Log.d(TAG, "handleOAuthResult: data=${data?.extras?.keySet()}")
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        val ex = data?.let { AuthorizationException.fromIntent(it) }
        Log.d(TAG, "response=$response  exception=$ex")

        when {
            response != null -> {
                Log.d(TAG, "Auth code received, starting token exchange")
                _uiState.update { it.copy(isAuthLoading = true, authError = null) }
                viewModelScope.launch { exchangeTokens(response) }
            }
            ex != null -> {
                val message = ex.errorDescription ?: ex.error ?: "Authorization failed"
                Log.e(TAG, "Authorization error: $message", ex)
                _uiState.update { it.copy(authError = message) }
            }
            else -> {
                Log.w(TAG, "handleOAuthResult: no response and no exception (cancelled or null intent)")
                // User cancelled (back button / closed tab)
                _uiState.update { it.copy(authError = "Sign-in was cancelled") }
            }
        }
    }

    private suspend fun exchangeTokens(response: AuthorizationResponse) {
        val provider = _uiState.value.selectedProvider ?: return
        val config = OAuthConfigs.forProvider(provider)
        Log.d(TAG, "exchangeTokens: provider=$provider, tokenEndpoint=${config.serviceConfig.tokenEndpoint}")

        try {
            val (accessToken, refreshToken) = suspendCancellableCoroutine { cont ->
                val service = getOrCreateAuthService()
                val clientAuth = config.clientSecret
                    ?.let { ClientSecretPost(it) }
                    ?: NoClientAuthentication.INSTANCE

                Log.d(TAG, "Calling performTokenRequest")
                service.performTokenRequest(
                    response.createTokenExchangeRequest(),
                    clientAuth,
                ) { tokenResponse, exception ->
                    Log.d(TAG, "Token response: accessToken=${tokenResponse?.accessToken?.take(8)}, exception=$exception")
                    when {
                        tokenResponse?.accessToken != null ->
                            cont.resume(Pair(tokenResponse.accessToken!!, tokenResponse.refreshToken))
                        exception != null ->
                            cont.resumeWithException(
                                Exception(exception.errorDescription ?: exception.error ?: "Token exchange failed")
                            )
                        else ->
                            cont.resumeWithException(Exception("Token exchange returned no token"))
                    }
                }
            }

            Log.d(TAG, "Token exchange succeeded, saving tokens")
            tokenStore.saveTokens(provider, accessToken, refreshToken)
            _uiState.update { it.copy(isAuthLoading = false, step = SetupStep.SELECT_LIST, isLoading = true) }
            loadLists()
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed", e)
            _uiState.update { it.copy(isAuthLoading = false, authError = "Sign-in failed: ${e.message}") }
        }
    }

    private fun getOrCreateAuthService(): AuthorizationService {
        return authService ?: AuthorizationService(appContext).also { authService = it }
    }

    private fun loadLists() {
        val provider = providerFactory.forType(_uiState.value.selectedProvider)
        if (provider == null) {
            _uiState.update { it.copy(isLoading = false, error = "Provider not supported yet") }
            return
        }
        viewModelScope.launch {
            provider.getLists()
                .onSuccess { lists ->
                    val listsWithCounts = coroutineScope {
                        lists.map { l ->
                            async {
                                val count = provider.getTasks(l.id).getOrElse { emptyList() }.size
                                ProviderListInfo(l.id, l.name, count)
                            }
                        }.awaitAll()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availableLists = listsWithCounts,
                            step = SetupStep.SELECT_LIST,
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load lists", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load lists: ${e.message}") }
                }
        }
    }

    fun createAndSelectList(name: String) {
        val provider = providerFactory.forType(_uiState.value.selectedProvider)
        if (provider == null) {
            _uiState.update { it.copy(error = "Provider not supported yet") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            provider.createList(name)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false) }
                    selectList(list.id, list.name)
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to create list", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create list: ${e.message}") }
                }
        }
    }

    fun selectList(id: String, name: String) {
        _uiState.update {
            it.copy(
                selectedListId = id,
                selectedListName = name,
                step = SetupStep.SET_GRID,
            )
        }
    }

    fun setGridDimensions(columns: Int, rows: Int, landscape: Boolean) {
        _uiState.update {
            it.copy(gridColumns = columns, gridRows = rows, isLandscape = landscape)
        }
    }

    fun proceedToPopulation() {
        viewModelScope.launch {
            val hasGrid = tileRepository.tiles.first().isNotEmpty()
            _uiState.update { it.copy(step = SetupStep.INITIAL_POPULATION, hasExistingGrid = hasGrid) }
        }
    }

    fun populateKeepCurrent(onComplete: () -> Unit) {
        finishSetup(onComplete)
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
