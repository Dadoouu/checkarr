package com.checkarr.data.repository

import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

class RadarrRepository(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    suspend fun getMovies(instance: Instance): Result<List<Movie>> =
        client.get(instance, "movie", ListSerializer(Movie.serializer()))

    suspend fun getMovie(instance: Instance, id: Int): Result<Movie> =
        client.get(instance, "movie/$id", Movie.serializer())

    suspend fun lookupMovie(instance: Instance, query: String): Result<List<Movie>> =
        client.get(instance, "movie/lookup?term=${java.net.URLEncoder.encode(query, "UTF-8")}", ListSerializer(Movie.serializer()))

    suspend fun lookupMovieByTmdb(instance: Instance, tmdbId: Int): Result<List<Movie>> =
        client.get(instance, "movie/lookup/tmdb?tmdbId=$tmdbId", ListSerializer(Movie.serializer()))

    suspend fun addMovie(instance: Instance, movie: Movie, qualityProfileId: Int, rootFolderPath: String, monitored: Boolean): Result<Movie> {
        val body = buildJsonObject {
            put("title", movie.title)
            put("year", movie.year)
            put("tmdbId", movie.tmdbId)
            put("qualityProfileId", qualityProfileId)
            put("rootFolderPath", rootFolderPath)
            put("monitored", monitored)
            put("addOptions", buildJsonObject {
                put("searchForMovie", monitored)
            })
        }.toString()
        val result = client.post(instance, "movie", body)
        return result.mapCatching { json.decodeFromString(Movie.serializer(), it) }
    }

    suspend fun updateMovie(instance: Instance, movie: Movie): Result<Movie> {
        val body = json.encodeToString(Movie.serializer(), movie)
        val result = client.put(instance, "movie/${movie.id}", body)
        return result.mapCatching { json.decodeFromString(Movie.serializer(), it) }
    }

    suspend fun deleteMovie(instance: Instance, id: Int, deleteFiles: Boolean = false): Result<Unit> =
        client.delete(instance, "movie/$id?deleteFiles=$deleteFiles")

    suspend fun searchMovie(instance: Instance, movieId: Int): Result<Unit> {
        val body = buildJsonObject {
            put("name", "MoviesSearch")
            put("movieIds", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(movieId)) })
        }.toString()
        return client.post(instance, "command", body).map {}
    }

    suspend fun getCalendar(instance: Instance, start: String, end: String): Result<List<Movie>> =
        client.get(instance, "calendar?start=$start&end=$end&unmonitored=true", ListSerializer(Movie.serializer()))

    suspend fun getQueue(instance: Instance, page: Int = 1, pageSize: Int = 50): Result<QueueResponse> =
        client.get(instance, "queue?page=$page&pageSize=$pageSize&includeMovie=true", QueueResponse.serializer())

    suspend fun removeQueueItem(instance: Instance, id: Int, blacklist: Boolean = false): Result<Unit> =
        client.delete(instance, "queue/$id?blacklist=$blacklist")

    suspend fun getHistory(instance: Instance, page: Int = 1, pageSize: Int = 50): Result<HistoryResponse> =
        client.get(instance, "history?page=$page&pageSize=$pageSize&sortKey=date&sortDirection=descending", HistoryResponse.serializer())

    suspend fun getMovieHistory(instance: Instance, movieId: Int): Result<List<HistoryItem>> =
        client.get(instance, "history/movie?movieId=$movieId", ListSerializer(HistoryItem.serializer()))

    suspend fun getReleases(instance: Instance, movieId: Int): Result<List<MovieRelease>> =
        client.get(instance, "release?movieId=$movieId", ListSerializer(MovieRelease.serializer()))

    suspend fun downloadRelease(instance: Instance, guid: String, indexerId: Int): Result<Unit> {
        val body = buildJsonObject {
            put("guid", guid)
            put("indexerId", indexerId)
        }.toString()
        return client.post(instance, "release", body).map {}
    }

    suspend fun getQualityProfiles(instance: Instance): Result<List<QualityProfile>> =
        client.get(instance, "qualityprofile", ListSerializer(QualityProfile.serializer()))

    suspend fun getRootFolders(instance: Instance): Result<List<RootFolder>> =
        client.get(instance, "rootfolder", ListSerializer(RootFolder.serializer()))

    suspend fun getTags(instance: Instance): Result<List<Tag>> =
        client.get(instance, "tag", ListSerializer(Tag.serializer()))

    suspend fun getSystemStatus(instance: Instance): Result<SystemStatus> =
        client.get(instance, "system/status", SystemStatus.serializer())

    suspend fun getDiskSpace(instance: Instance): Result<List<DiskSpace>> =
        client.get(instance, "diskspace", ListSerializer(DiskSpace.serializer()))
}
