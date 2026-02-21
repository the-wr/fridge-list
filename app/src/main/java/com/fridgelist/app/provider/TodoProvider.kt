package com.fridgelist.app.provider

import com.fridgelist.app.data.model.SyncResult

/**
 * Abstraction layer for todo list providers.
 * Each provider implements this interface independently.
 */
interface TodoProvider {

    data class ProviderList(val id: String, val name: String)
    data class ProviderTask(val id: String, val name: String, val isComplete: Boolean)

    /**
     * Fetch all available lists/projects from the provider.
     */
    suspend fun getLists(): Result<List<ProviderList>>

    /**
     * Fetch all tasks in the configured list.
     * Returns map of taskId -> isComplete.
     */
    suspend fun getTasks(listId: String): Result<List<ProviderTask>>

    /**
     * Create a new task with the given name. Returns the new task ID.
     */
    suspend fun createTask(listId: String, name: String): Result<String>

    /**
     * Mark a task as complete (not needed).
     */
    suspend fun completeTask(taskId: String): SyncResult

    /**
     * Mark a task as incomplete/open (needed).
     */
    suspend fun reopenTask(taskId: String): SyncResult

    /**
     * Attempt to refresh the access token. Returns true if successful.
     */
    suspend fun refreshToken(): Boolean
}
