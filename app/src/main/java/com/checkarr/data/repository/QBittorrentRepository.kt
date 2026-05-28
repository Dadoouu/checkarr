package com.checkarr.data.repository

import com.checkarr.data.models.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class QBittorrentRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            private val store = mutableMapOf<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { store[url.host] = cookies }
            override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host] ?: emptyList()
        })
        .build()

    private fun base(url: String) = url.trimEnd('/')

    suspend fun login(url: String, username: String, password: String): Result<Unit> {
        return try {
            val body = "username=${java.net.URLEncoder.encode(username, "UTF-8")}&password=${java.net.URLEncoder.encode(password, "UTF-8")}".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder().url("${base(url)}/api/v2/auth/login").post(body).build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val text = response.body?.string() ?: ""
                if (text == "Ok." || response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Login échoué: $text"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun <T> get(url: String, path: String, ds: kotlinx.serialization.DeserializationStrategy<T>): Result<T> {
        return try {
            val request = Request.Builder().url("${base(url)}/api/v2/$path").get().build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Vide"))
                Result.success(json.decodeFromString(ds, body))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTorrents(url: String): Result<List<QBitTorrent>> =
        get(url, "torrents/info", ListSerializer(QBitTorrent.serializer()))

    suspend fun getServerState(url: String): Result<QBitMainData> =
        get(url, "sync/maindata", QBitMainData.serializer())

    private suspend fun postForm(url: String, path: String, body: String): Result<Unit> {
        return try {
            val rb = body.toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder().url("${base(url)}/api/v2/$path").post(rb).build()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute(); Result.success(Unit)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun pauseTorrent(url: String, hash: String) = postForm(url, "torrents/stop", "hashes=$hash")
    suspend fun resumeTorrent(url: String, hash: String) = postForm(url, "torrents/start", "hashes=$hash")
    suspend fun deleteTorrent(url: String, hash: String, deleteFiles: Boolean) =
        postForm(url, "torrents/delete", "hashes=$hash&deleteFiles=$deleteFiles")
}
