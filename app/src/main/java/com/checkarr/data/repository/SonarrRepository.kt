package com.checkarr.data.repository

import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

class SonarrRepository(private val client: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    suspend fun getSeries(instance: Instance): Result<List<Series>> =
        client.get(instance, "series", ListSerializer(Series.serializer()))

    suspend fun getSeriesById(instance: Instance, id: Int): Result<Series> =
        client.get(instance, "series/$id", Series.serializer())

    suspend fun lookupSeries(instance: Instance, query: String): Result<List<Series>> =
        client.get(instance, "series/lookup?term=${java.net.URLEncoder.encode(query, "UTF-8")}", ListSerializer(Series.serializer()))

    suspend fun addSeries(instance: Instance, series: Series, qualityProfileId: Int, rootFolderPath: String, monitored: Boolean): Result<Series> {
        val body = buildJsonObject {
            put("title", series.title)
            put("year", series.year)
            put("tvdbId", series.tvdbId)
            put("qualityProfileId", qualityProfileId)
            put("rootFolderPath", rootFolderPath)
            put("monitored", monitored)
            put("seasonFolder", true)
            put("addOptions", buildJsonObject {
                put("searchForMissingEpisodes", monitored)
                put("monitor", if (monitored) "all" else "none")
            })
        }.toString()
        val result = client.post(instance, "series", body)
        return result.mapCatching { json.decodeFromString(Series.serializer(), it) }
    }

    suspend fun updateSeries(instance: Instance, series: Series): Result<Series> {
        val body = json.encodeToString(Series.serializer(), series)
        val result = client.put(instance, "series/${series.id}", body)
        return result.mapCatching { json.decodeFromString(Series.serializer(), it) }
    }

    suspend fun deleteSeries(instance: Instance, id: Int, deleteFiles: Boolean = false): Result<Unit> =
        client.delete(instance, "series/$id?deleteFiles=$deleteFiles")

    suspend fun searchSeries(instance: Instance, seriesId: Int): Result<Unit> {
        val body = buildJsonObject {
            put("name", "SeriesSearch")
            put("seriesId", seriesId)
        }.toString()
        return client.post(instance, "command", body).map {}
    }

    suspend fun getEpisodes(instance: Instance, seriesId: Int, seasonNumber: Int? = null): Result<List<Episode>> {
        val path = if (seasonNumber != null) {
            "episode?seriesId=$seriesId&seasonNumber=$seasonNumber"
        } else {
            "episode?seriesId=$seriesId"
        }
        return client.get(instance, path, ListSerializer(Episode.serializer()))
    }

    suspend fun updateEpisode(instance: Instance, episode: Episode): Result<Episode> {
        val body = json.encodeToString(Episode.serializer(), episode)
        val result = client.put(instance, "episode/${episode.id}", body)
        return result.mapCatching { json.decodeFromString(Episode.serializer(), it) }
    }

    suspend fun searchEpisodes(instance: Instance, episodeIds: List<Int>): Result<Unit> {
        val idsArray = kotlinx.serialization.json.buildJsonArray {
            episodeIds.forEach { add(JsonPrimitive(it)) }
        }
        val body = buildJsonObject {
            put("name", "EpisodeSearch")
            put("episodeIds", idsArray)
        }.toString()
        return client.post(instance, "command", body).map {}
    }

    suspend fun getCalendar(instance: Instance, start: String, end: String): Result<List<SeriesCalendarItem>> =
        client.get(instance, "calendar?start=$start&end=$end&unmonitored=true&includeSeries=true", ListSerializer(SeriesCalendarItem.serializer()))

    suspend fun getQueue(instance: Instance, page: Int = 1, pageSize: Int = 50): Result<QueueResponse> =
        client.get(instance, "queue?page=$page&pageSize=$pageSize&includeSeries=true&includeEpisode=true", QueueResponse.serializer())

    suspend fun removeQueueItem(instance: Instance, id: Int, blacklist: Boolean = false): Result<Unit> =
        client.delete(instance, "queue/$id?blacklist=$blacklist")

    suspend fun getHistory(instance: Instance, page: Int = 1, pageSize: Int = 50): Result<HistoryResponse> =
        client.get(instance, "history?page=$page&pageSize=$pageSize&sortKey=date&sortDirection=descending", HistoryResponse.serializer())

    suspend fun getReleases(instance: Instance, episodeId: Int): Result<List<MovieRelease>> =
        client.get(instance, "release?episodeId=$episodeId", ListSerializer(MovieRelease.serializer()))

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
