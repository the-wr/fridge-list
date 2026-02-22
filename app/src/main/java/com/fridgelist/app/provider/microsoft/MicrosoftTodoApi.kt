package com.fridgelist.app.provider.microsoft

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class MsGraphList(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
)

@JsonClass(generateAdapter = true)
data class MsGraphListsResponse(
    @Json(name = "value") val value: List<MsGraphList>,
    @Json(name = "@odata.nextLink") val nextLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class MsGraphTask(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "status") val status: String, // "notStarted" | "completed"
)

@JsonClass(generateAdapter = true)
data class MsGraphTasksResponse(
    @Json(name = "value") val value: List<MsGraphTask>,
    @Json(name = "@odata.nextLink") val nextLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class MsGraphCreateTaskRequest(
    @Json(name = "title") val title: String,
)

@JsonClass(generateAdapter = true)
data class MsGraphUpdateTaskRequest(
    @Json(name = "status") val status: String,
)

interface MicrosoftTodoApi {

    @GET("me/todo/lists")
    suspend fun getLists(
        @Header("Authorization") token: String,
    ): Response<MsGraphListsResponse>

    /** Follows an @odata.nextLink from a previous getLists response. */
    @GET
    suspend fun getListsNextPage(
        @Url nextLink: String,
        @Header("Authorization") token: String,
    ): Response<MsGraphListsResponse>

    @GET("me/todo/lists/{listId}/tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Path("listId") listId: String,
        @Query("\$top") top: Int,
    ): Response<MsGraphTasksResponse>

    /** Follows an @odata.nextLink from a previous getTasks response. */
    @GET
    suspend fun getTasksNextPage(
        @Url nextLink: String,
        @Header("Authorization") token: String,
    ): Response<MsGraphTasksResponse>

    @POST("me/todo/lists/{listId}/tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Path("listId") listId: String,
        @Body request: MsGraphCreateTaskRequest,
    ): Response<MsGraphTask>

    @PATCH("me/todo/lists/{listId}/tasks/{taskId}")
    suspend fun updateTask(
        @Header("Authorization") token: String,
        @Path("listId") listId: String,
        @Path("taskId") taskId: String,
        @Body request: MsGraphUpdateTaskRequest,
    ): Response<MsGraphTask>
}
