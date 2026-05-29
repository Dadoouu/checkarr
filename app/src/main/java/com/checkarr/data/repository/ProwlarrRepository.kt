package com.checkarr.data.repository

import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import kotlinx.serialization.builtins.ListSerializer

class ProwlarrRepository(private val client: ApiClient) {

    suspend fun getIndexers(instance: Instance): Result<List<ProwlarrIndexer>> =
        client.get(instance, "indexer", ListSerializer(ProwlarrIndexer.serializer()))

    suspend fun getIndexer(instance: Instance, id: Int): Result<ProwlarrIndexer> =
        client.get(instance, "indexer/$id", ProwlarrIndexer.serializer())

    suspend fun deleteIndexer(instance: Instance, id: Int): Result<Unit> =
        client.delete(instance, "indexer/$id")

    suspend fun getIndexerStats(instance: Instance): Result<ProwlarrIndexerStats> =
        client.get(instance, "indexerstats", ProwlarrIndexerStats.serializer())

    suspend fun search(instance: Instance, query: String, categories: List<Int> = emptyList()): Result<List<ProwlarrSearchResult>> {
        val catParam = if (categories.isNotEmpty()) "&categories=${categories.joinToString(",")}" else ""
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return client.get(instance, "search?query=$encodedQuery$catParam", ListSerializer(ProwlarrSearchResult.serializer()))
    }

    suspend fun getHistory(instance: Instance, page: Int = 1, pageSize: Int = 50): Result<ProwlarrHistoryResponse> =
        client.get(instance, "history?page=$page&pageSize=$pageSize&sortKey=date&sortDirection=descending", ProwlarrHistoryResponse.serializer())

    suspend fun getSystemStatus(instance: Instance): Result<SystemStatus> =
        client.get(instance, "system/status", SystemStatus.serializer())

    suspend fun testAllIndexers(instance: Instance): Result<Unit> =
        client.post(instance, "indexer/testall", "{}").map {}

    suspend fun getCategories(instance: Instance): Result<List<ProwlarrCategory>> =
        client.get(instance, "indexer/categories", ListSerializer(ProwlarrCategory.serializer()))
}
