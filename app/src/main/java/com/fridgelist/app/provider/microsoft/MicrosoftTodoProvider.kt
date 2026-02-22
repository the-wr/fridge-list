package com.fridgelist.app.provider.microsoft

import com.fridgelist.app.BuildConfig
import com.fridgelist.app.data.datastore.AppSettings
import com.fridgelist.app.data.datastore.TokenStore
import com.fridgelist.app.data.model.ProviderType
import com.fridgelist.app.data.model.SyncResult
import com.fridgelist.app.provider.TodoProvider
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val MS_TASK_COMPLETED = "completed"
private const val MS_TASK_NOT_STARTED = "notStarted"
private const val MS_TOKEN_ENDPOINT =
    "https://login.microsoftonline.com/common/oauth2/v2.0/token"
private const val MS_SCOPES = "Tasks.ReadWrite offline_access"

/** Moshi model for the OAuth token refresh response. */
@JsonClass(generateAdapter = true)
private data class MsTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String?,
)

@Singleton
class MicrosoftTodoProvider @Inject constructor(
    private val api: MicrosoftTodoApi,
    private val tokenStore: TokenStore,
    private val appSettings: AppSettings,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) : TodoProvider {

    private fun authHeader(): String {
        val token = tokenStore.getAccessToken(ProviderType.MICROSOFT_TODO) ?: ""
        return "Bearer $token"
    }

    override suspend fun getLists(): Result<List<TodoProvider.ProviderList>> = runCatching {
        val all = mutableListOf<MsGraphList>()
        var response = api.getLists(authHeader())

        if (response.code() == 401) throw UnauthorizedException()
        if (!response.isSuccessful) throw Exception("Failed to fetch lists: ${response.code()}")

        all += response.body()!!.value

        var nextLink = response.body()!!.nextLink
        while (nextLink != null) {
            response = api.getListsNextPage(nextLink, authHeader())
            if (!response.isSuccessful) break
            all += response.body()!!.value
            nextLink = response.body()!!.nextLink
        }

        all.map { TodoProvider.ProviderList(it.id, it.displayName) }
    }

    override suspend fun getTasks(listId: String): Result<List<TodoProvider.ProviderTask>> =
        runCatching {
            val all = mutableListOf<MsGraphTask>()
            var response = api.getTasks(authHeader(), listId, top = 100)

            if (response.code() == 401) throw UnauthorizedException()
            if (!response.isSuccessful) throw Exception("Failed to fetch tasks: ${response.code()}")

            all += response.body()!!.value

            var nextLink = response.body()!!.nextLink
            while (nextLink != null) {
                response = api.getTasksNextPage(nextLink, authHeader())
                if (!response.isSuccessful) break
                all += response.body()!!.value
                nextLink = response.body()!!.nextLink
            }

            all.map {
                TodoProvider.ProviderTask(
                    id = it.id,
                    name = it.title,
                    isComplete = it.status == MS_TASK_COMPLETED,
                )
            }
        }

    override suspend fun createTask(listId: String, name: String): Result<String> = runCatching {
        val response = api.createTask(
            authHeader(),
            listId,
            MsGraphCreateTaskRequest(name),
        )
        if (response.isSuccessful) {
            response.body()!!.id
        } else {
            throw Exception("Failed to create task: ${response.code()}")
        }
    }

    override suspend fun completeTask(taskId: String): SyncResult =
        updateTaskStatus(taskId, MS_TASK_COMPLETED)

    override suspend fun reopenTask(taskId: String): SyncResult =
        updateTaskStatus(taskId, MS_TASK_NOT_STARTED)

    private suspend fun updateTaskStatus(taskId: String, status: String): SyncResult {
        val listId = appSettings.providerListId.first() ?: return SyncResult.Failure("No list configured")
        return try {
            val response = api.updateTask(
                authHeader(),
                listId,
                taskId,
                MsGraphUpdateTaskRequest(status),
            )
            when {
                response.isSuccessful -> SyncResult.Success
                response.code() == 401 -> SyncResult.AuthRequired
                else -> SyncResult.Failure("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Offline
        }
    }

    /**
     * Exchanges the stored refresh token for a new access token via the Microsoft identity
     * platform. Microsoft access tokens expire after ~1 hour, so this is called automatically
     * when the provider receives a 401.
     */
    override suspend fun refreshToken(): Boolean {
        val refreshToken = tokenStore.getRefreshToken(ProviderType.MICROSOFT_TODO) ?: return false
        val clientId = BuildConfig.MICROSOFT_CLIENT_ID.takeIf { it.isNotEmpty() } ?: return false

        return try {
            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("scope", MS_SCOPES)
                .build()

            val httpRequest = Request.Builder()
                .url(MS_TOKEN_ENDPOINT)
                .post(requestBody)
                .build()

            val httpResponse = okHttpClient.newCall(httpRequest).execute()
            if (!httpResponse.isSuccessful) return false

            val body = httpResponse.body?.string() ?: return false
            val adapter = moshi.adapter(MsTokenResponse::class.java)
            val tokenResponse = adapter.fromJson(body) ?: return false

            tokenStore.saveTokens(
                ProviderType.MICROSOFT_TODO,
                tokenResponse.accessToken,
                tokenResponse.refreshToken,
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

private class UnauthorizedException : Exception("Unauthorized")
