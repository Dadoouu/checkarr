package com.checkarr.data.network

import com.checkarr.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(instance: Instance, path: String): Request.Builder {
        val baseUrl = instance.url.trimEnd('/')
        return Request.Builder()
            .url("$baseUrl/api/v3/$path")
            .addHeader("X-Api-Key", instance.apiKey)
    }

    suspend fun <T> get(instance: Instance, path: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val request = buildRequest(instance, path).get().build()
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(ApiException(response.code, response.message))
                val body = response.body?.string() ?: return@withContext Result.failure(ApiException(0, "Réponse vide"))
                Result.success(json.decodeFromString(deserializer, body))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun post(instance: Instance, path: String, body: String): Result<String> {
        return try {
            val rb = body.toRequestBody("application/json".toMediaType())
            val request = buildRequest(instance, path).post(rb).build()
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(ApiException(response.code, response.message))
                Result.success(response.body?.string() ?: "")
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun put(instance: Instance, path: String, body: String): Result<String> {
        return try {
            val rb = body.toRequestBody("application/json".toMediaType())
            val request = buildRequest(instance, path).put(rb).build()
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(ApiException(response.code, response.message))
                Result.success(response.body?.string() ?: "")
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun delete(instance: Instance, path: String): Result<Unit> {
        return try {
            val request = buildRequest(instance, path).delete().build()
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(ApiException(response.code, response.message))
                Result.success(Unit)
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

class ApiException(val code: Int, override val message: String) : Exception(message)
