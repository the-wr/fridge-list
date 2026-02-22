package com.fridgelist.app.provider.todoist

import com.fridgelist.app.data.datastore.TokenStore
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.data.model.SyncResult
import com.fridgelist.app.provider.TodoProvider
import java.util.UUID
import javax.inject.Inject

class TodoistProvider @Inject constructor(
    private val api: TodoistApi,
    private val tokenStore: TokenStore
) : TodoProvider {

    private fun authHeader(): String {
        val token = tokenStore.getAccessToken(ProviderType.TODOIST) ?: ""
        return "Bearer $token"
    }

    override suspend fun getLists(): Result<List<TodoProvider.ProviderList>> = runCatching {
        val all = mutableListOf<TodoistProject>()
        var cursor: String? = null
        do {
            val response = api.getProjects(authHeader(), cursor)
            if (!response.isSuccessful) throw Exception("Failed to fetch projects: ${response.code()}")
            val body = response.body()!!
            all += body.results
            cursor = body.nextCursor
        } while (cursor != null)
        all.map { TodoProvider.ProviderList(it.id, it.name) }
    }

    override suspend fun getTasks(listId: String): Result<List<TodoProvider.ProviderTask>> =
        runCatching {
            val all = mutableListOf<TodoistTask>()
            var cursor: String? = null
            do {
                val response = api.getTasks(authHeader(), listId, cursor)
                if (!response.isSuccessful) throw Exception("Failed to fetch tasks: ${response.code()}")
                val body = response.body()!!
                all += body.results
                cursor = body.nextCursor
            } while (cursor != null)
            all.map { TodoProvider.ProviderTask(it.id, it.content, it.isCompleted) }
        }

    override suspend fun createTask(listId: String, name: String): Result<String> = runCatching {
        val response = api.createTask(
            authHeader(),
            UUID.randomUUID().toString(),
            CreateTaskRequest(name, listId)
        )
        if (response.isSuccessful) {
            response.body()!!.id
        } else {
            throw Exception("Failed to create task: ${response.code()}")
        }
    }

    override suspend fun completeTask(taskId: String): SyncResult {
        return try {
            val response = api.closeTask(authHeader(), taskId)
            when {
                response.isSuccessful -> SyncResult.Success
                response.code() == 401 -> SyncResult.AuthRequired
                else -> SyncResult.Failure("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Offline
        }
    }

    override suspend fun reopenTask(taskId: String): SyncResult {
        return try {
            val response = api.reopenTask(authHeader(), taskId)
            when {
                response.isSuccessful -> SyncResult.Success
                response.code() == 401 -> SyncResult.AuthRequired
                else -> SyncResult.Failure("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Offline
        }
    }

    override suspend fun refreshToken(): Boolean {
        // Todoist uses long-lived API tokens; no refresh needed.
        // For OAuth flows this would call the token endpoint.
        return tokenStore.getAccessToken(ProviderType.TODOIST) != null
    }
}
