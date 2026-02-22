package com.fridgelist.app.data.repository

import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.provider.TodoProvider
import com.fridgelist.app.provider.microsoft.MicrosoftTodoProvider
import com.fridgelist.app.provider.todoist.TodoistProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderFactory @Inject constructor(
    private val appSettings: AppSettings,
    private val todoistProvider: TodoistProvider,
    private val microsoftTodoProvider: MicrosoftTodoProvider,
) {
    suspend fun current(): TodoProvider? {
        return when (appSettings.providerType.first()) {
            ProviderType.TODOIST -> todoistProvider
            ProviderType.MICROSOFT_TODO -> microsoftTodoProvider
            ProviderType.GOOGLE_TASKS -> null    // TODO: implement
            ProviderType.TICKTICK -> null        // TODO: implement
            null -> null
        }
    }
}
