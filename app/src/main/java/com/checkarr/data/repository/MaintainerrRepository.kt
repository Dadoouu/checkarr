package com.checkarr.data.repository

import com.checkarr.data.models.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MaintainerrRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(url: String, path: String) = "${url.trimEnd('/')}/api/$path"

    private suspend fun <T> get(url: String, apiKey: String, path: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val fullUrl = baseUrl(url, path)
            android.util.Log.d("MaintainerrRepo", "GET $fullUrl")
            android.util.Log.d("MaintainerrRepo", "Header X-Api-Key: ${apiKey.take(8)}...")
            val request = Request.Builder().url(fullUrl)
                .addHeader("X-Api-Key", apiKey)
                .get().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                android.util.Log.d("MaintainerrRepo", "Response ${response.code} <- $fullUrl")
                if (!response.isSuccessful) {
                    android.util.Log.e("MaintainerrRepo", "Error body: $body")
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(json.decodeFromString(deserializer, body ?: return@withContext Result.failure(Exception("Empty"))))
            }
        } catch (e: Exception) {
            android.util.Log.e("MaintainerrRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun post(url: String, apiKey: String, path: String, body: String = "{}"): Result<String> {
        return try {
            val fullUrl = baseUrl(url, path)
            android.util.Log.d("MaintainerrRepo", "POST $fullUrl")
            android.util.Log.d("MaintainerrRepo", "Body: $body")
            val requestBody = body.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(fullUrl)
                .addHeader("X-Api-Key", apiKey)
                .post(requestBody).build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                android.util.Log.d("MaintainerrRepo", "Response ${response.code} <- $fullUrl")
                if (!response.isSuccessful) {
                    android.util.Log.e("MaintainerrRepo", "Error body: $responseBody")
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
                Result.success(responseBody ?: "")
            }
        } catch (e: Exception) {
            android.util.Log.e("MaintainerrRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun delete(url: String, apiKey: String, path: String): Result<Unit> {
        return try {
            val request = Request.Builder().url(baseUrl(url, path))
                .addHeader("X-Api-Key", apiKey)
                .delete().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                Result.success(Unit)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getCollections(url: String, apiKey: String): Result<List<MaintainerrRuleGroup>> =
        get(url, apiKey, "rules", ListSerializer(MaintainerrRuleGroup.serializer()))

    suspend fun getCollectionMedia(url: String, apiKey: String, collectionId: Int): Result<List<MaintainerrMedia>> =
        get(url, apiKey, "rules/collection/$collectionId/media", ListSerializer(MaintainerrMedia.serializer()))

    suspend fun handleCollections(url: String, apiKey: String): Result<Unit> =
        post(url, apiKey, "rules/handle").map {}

    suspend fun handleCollection(url: String, apiKey: String, collectionId: Int): Result<Unit> =
        post(url, apiKey, "rules/collection/$collectionId/handle").map {}

    suspend fun removeMediaFromCollection(url: String, apiKey: String, collectionId: Int, mediaId: Int): Result<Unit> =
        delete(url, apiKey, "rules/collection/$collectionId/media/$mediaId")

    suspend fun getStorageMetrics(url: String, apiKey: String): Result<MaintainerrStorageMetrics> =
        get(url, apiKey, "storage-metrics", MaintainerrStorageMetrics.serializer())

    suspend fun runRules(url: String, apiKey: String): Result<Unit> =
        post(url, apiKey, "rules/execute").map {}

    suspend fun addMediaToCollection(url: String, collectionId: Int, plexId: Int): Result<Unit> =
        post(url, "rules/collection/$collectionId/media", """{"plexId":$plexId}""").map {}
}
