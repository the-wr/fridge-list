package com.fridgelist.app.provider.todoist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class TodoistProject(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TodoistTask(
    @Json(name = "id") val id: String,
    @Json(name = "content") val content: String,
    @Json(name = "is_completed") val isCompleted: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateTaskRequest(
    @Json(name = "content") val content: String,
    @Json(name = "project_id") val projectId: String
)

interface TodoistApi {

    @GET("projects")
    suspend fun getProjects(
        @Header("Authorization") token: String
    ): Response<List<TodoistProject>>

    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Query("project_id") projectId: String
    ): Response<List<TodoistTask>>

    @POST("tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Header("X-Request-Id") requestId: String,
        @Body request: CreateTaskRequest
    ): Response<TodoistTask>

    @POST("tasks/{id}/close")
    suspend fun closeTask(
        @Header("Authorization") token: String,
        @Path("id") taskId: String
    ): Response<Unit>

    @POST("tasks/{id}/reopen")
    suspend fun reopenTask(
        @Header("Authorization") token: String,
        @Path("id") taskId: String
    ): Response<Unit>
}
