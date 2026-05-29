package com.checkarr.data.repository

import com.checkarr.data.models.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
            val fullUrl = "${url.trimEnd('/')}/api/v1/$path"
            android.util.Log.d("SeerrRepo", "GET $fullUrl")
            android.util.Log.d("SeerrRepo", "Header X-Api-Key: ${apiKey.take(8)}...")
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("X-Api-Key", apiKey)
                .get().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                android.util.Log.d("SeerrRepo", "Response ${response.code} <- $fullUrl")
                if (!response.isSuccessful) {
                    android.util.Log.e("SeerrRepo", "Error body: $body")
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(json.decodeFromString(deserializer, body ?: return@withContext Result.failure(Exception("Empty"))))
            }
        } catch (e: Exception) {
            android.util.Log.e("SeerrRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun post(url: String, apiKey: String, path: String, body: String = "{}"): Result<String> {
        return try {
            val fullUrl = "${url.trimEnd('/')}/api/v1/$path"
            android.util.Log.d("SeerrRepo", "POST $fullUrl")
            android.util.Log.d("SeerrRepo", "Body: $body")
            val requestBody = body.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("X-Api-Key", apiKey)
                .post(requestBody).build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                android.util.Log.d("SeerrRepo", "Response ${response.code} <- $fullUrl")
                if (!response.isSuccessful) {
                    android.util.Log.e("SeerrRepo", "Error body: $responseBody")
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
                Result.success(responseBody ?: "")
            }
        } catch (e: Exception) {
            android.util.Log.e("SeerrRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun delete(url: String, apiKey: String, path: String): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url("${url.trimEnd('/')}/api/v1/$path")
                .addHeader("X-Api-Key", apiKey)
                .delete()
                .build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                Result.success(Unit)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRequests(url: String, apiKey: String, filter: String = "all", page: Int = 1): Result<SeerrRequestsResponse> =
        get(url, apiKey, "request?filter=$filter&take=30&skip=${(page-1)*30}&sort=added", SeerrRequestsResponse.serializer())
        
    suspend fun approveRequest(url: String, apiKey: String, requestId: Int): Result<SeerrRequest> =
        post(url, apiKey, "request/$requestId/approve").mapCatching { json.decodeFromString(SeerrRequest.serializer(), it) }

    suspend fun declineRequest(url: String, apiKey: String, requestId: Int): Result<SeerrRequest> =
        post(url, apiKey, "request/$requestId/decline").mapCatching { json.decodeFromString(SeerrRequest.serializer(), it) }

    suspend fun deleteRequest(url: String, apiKey: String, requestId: Int): Result<Unit> =
        delete(url, apiKey, "request/$requestId")

    suspend fun retryRequest(url: String, apiKey: String, requestId: Int): Result<SeerrRequest> =
        post(url, apiKey, "request/$requestId/retry").mapCatching { json.decodeFromString(SeerrRequest.serializer(), it) }

    suspend fun search(url: String, apiKey: String, query: String, page: Int = 1): Result<SeerrSearchResponse> =
        get(url, apiKey, "search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page", SeerrSearchResponse.serializer())

    suspend fun getUsers(url: String, apiKey: String): Result<SeerrUserList> =
        get(url, apiKey, "user?take=50&sort=displayName", SeerrUserList.serializer())

    suspend fun getMovieDetails(url: String, apiKey: String, tmdbId: Int): Result<SeerrSearchResult> =
        get(url, apiKey, "movie/$tmdbId", SeerrSearchResult.serializer())

    suspend fun getTvDetails(url: String, apiKey: String, tvdbId: Int): Result<SeerrSearchResult> =
        get(url, apiKey, "tv/$tvdbId", SeerrSearchResult.serializer())

    suspend fun createMovieRequest(url: String, apiKey: String, tmdbId: Int, is4k: Boolean = false): Result<SeerrRequest> =
        post(url, apiKey, "request", """{"mediaType":"movie","mediaId":$tmdbId,"is4k":$is4k}""")
            .mapCatching { json.decodeFromString(SeerrRequest.serializer(), it) }

    suspend fun createTvRequest(url: String, apiKey: String, tvdbId: Int, seasons: List<Int>, is4k: Boolean = false): Result<SeerrRequest> {
        val seasonsJson = seasons.joinToString(",") { """{"seasonNumber":$it}""" }
        return post(url, apiKey, "request", """{"mediaType":"tv","mediaId":$tvdbId,"seasons":[$seasonsJson],"is4k":$is4k}""")
            .mapCatching { json.decodeFromString(SeerrRequest.serializer(), it) }
    }
}
