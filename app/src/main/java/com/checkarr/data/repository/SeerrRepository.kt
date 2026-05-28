package com.checkarr.data.repository

import com.checkarr.data.models.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SeerrRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(url: String, apiKey: String, path: String): Request =
        Request.Builder()
            .url("${url.trimEnd('/')}/api/v1/$path")
            .addHeader("X-Api-Key", apiKey)
            .get()
            .build()

    private suspend fun <T> get(url: String, apiKey: String, path: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val request = buildRequest(url, apiKey, path)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty"))
                Result.success(json.decodeFromString(deserializer, body))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRequests(url: String, apiKey: String, filter: String = "all"): Result<SeerrRequestsResponse> =
        get(url, apiKey, "request?filter=$filter&take=30&sort=added&order=desc", SeerrRequestsResponse.serializer())

    suspend fun getPendingRequests(url: String, apiKey: String): Result<SeerrRequestsResponse> =
        get(url, apiKey, "request?filter=pending&take=20", SeerrRequestsResponse.serializer())

}
