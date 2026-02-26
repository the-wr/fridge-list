package com.fridgelist.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fridgelist.app.data.model.GridConfig
import com.fridgelist.app.data.model.ProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val ORIENTATION_LANDSCAPE = booleanPreferencesKey("orientation_landscape")
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val PROVIDER_LIST_ID = stringPreferencesKey("provider_list_id")
        val PROVIDER_LIST_NAME = stringPreferencesKey("provider_list_name")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val EDIT_MODE_HINT_SEEN = booleanPreferencesKey("edit_mode_hint_seen")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SETUP_COMPLETE] ?: false }

    val gridConfig: Flow<GridConfig> = context.dataStore.data
        .map {
            GridConfig(
                columns = it[Keys.GRID_COLUMNS] ?: 8,
                rows = it[Keys.GRID_ROWS] ?: 11
            )
        }

    val isLandscape: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.ORIENTATION_LANDSCAPE] ?: true }

    val providerType: Flow<ProviderType?> = context.dataStore.data
        .map {
            it[Keys.PROVIDER_TYPE]?.let { name -> ProviderType.valueOf(name) }
        }

    val providerListId: Flow<String?> = context.dataStore.data
        .map { it[Keys.PROVIDER_LIST_ID] }

    val providerListName: Flow<String?> = context.dataStore.data
        .map { it[Keys.PROVIDER_LIST_NAME] }

    val lastSyncTime: Flow<Long> = context.dataStore.data
        .map { it[Keys.LAST_SYNC_TIME] ?: 0L }

    val editModeHintSeen: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.EDIT_MODE_HINT_SEEN] ?: false }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_COMPLETE] = complete }
    }

    suspend fun setGridConfig(config: GridConfig) {
        context.dataStore.edit {
            it[Keys.GRID_COLUMNS] = config.columns
            it[Keys.GRID_ROWS] = config.rows
        }
    }

    suspend fun setOrientation(landscape: Boolean) {
        context.dataStore.edit { it[Keys.ORIENTATION_LANDSCAPE] = landscape }
    }

    suspend fun setProvider(type: ProviderType, listId: String, listName: String) {
        context.dataStore.edit {
            it[Keys.PROVIDER_TYPE] = type.name
            it[Keys.PROVIDER_LIST_ID] = listId
            it[Keys.PROVIDER_LIST_NAME] = listName
        }
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIME] = time }
    }

    suspend fun setEditModeHintSeen() {
        context.dataStore.edit { it[Keys.EDIT_MODE_HINT_SEEN] = true }
    }
}
