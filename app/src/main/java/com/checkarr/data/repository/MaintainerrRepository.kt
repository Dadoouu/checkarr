package com.checkarr.data.repository

import com.checkarr.data.models.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MaintainerrRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun <T> get(url: String, path: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val request = Request.Builder().url("${url.trimEnd('/')}/api/$path").get().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty"))
                Result.success(json.decodeFromString(deserializer, body))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getCollections(url: String): Result<List<MaintainerrCollection>> =
        get(url, "rules/collections", ListSerializer(MaintainerrCollection.serializer()))
}
