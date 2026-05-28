package com.checkarr.data.repository

import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class JellyfinRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(instance: JellyfinInstance, path: String): Request.Builder {
        val baseUrl = instance.url.trimEnd('/')
        return Request.Builder()
            .url("$baseUrl$path")
            .addHeader("X-Emby-Token", instance.apiKey)
            .addHeader("X-MediaBrowser-Token", instance.apiKey)
    }

    private suspend fun <T> get(instance: JellyfinInstance, path: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val request = buildRequest(instance, path).get().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                Result.success(json.decodeFromString(deserializer, body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessions(instance: JellyfinInstance): Result<List<JellyfinSession>> =
        get(instance, "/Sessions", ListSerializer(JellyfinSession.serializer()))

    suspend fun getSystemInfo(instance: JellyfinInstance): Result<JellyfinSystemInfo> =
        get(instance, "/System/Info", JellyfinSystemInfo.serializer())

    suspend fun getDrives(instance: JellyfinInstance): Result<List<JellyfinDriveInfo>> =
        get(instance, "/System/Storage", ListSerializer(JellyfinDriveInfo.serializer()))

    suspend fun getRecentlyAdded(instance: JellyfinInstance, limit: Int = 20): Result<JellyfinItemsResponse> =
        get(instance, "/Items?SortBy=DateCreated&SortOrder=Descending&Recursive=true&IncludeItemTypes=Movie,Episode&Limit=$limit&Fields=Overview,Genres,ImageTags,UserData,RunTimeTicks", JellyfinItemsResponse.serializer())

    suspend fun getResumeItems(instance: JellyfinInstance, limit: Int = 10): Result<JellyfinItemsResponse> =
        get(instance, "/Items/Resume?MediaTypes=Video&Limit=$limit&Fields=Overview,ImageTags,UserData,RunTimeTicks,SeriesName", JellyfinItemsResponse.serializer())

    suspend fun getActivityLog(instance: JellyfinInstance, limit: Int = 30): Result<JellyfinActivityResponse> =
        get(instance, "/System/ActivityLog/Entries?Limit=$limit", JellyfinActivityResponse.serializer())
}
