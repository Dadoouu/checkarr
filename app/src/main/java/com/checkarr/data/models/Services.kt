package com.checkarr.data.models

import kotlinx.serialization.Serializable

enum class ServiceType(
    val label: String,
    val defaultPort: Int,
    val defaultSubdomain: String,
    val color: Long
) {
    RADARR("Radarr", 7878, "radarr", 0xFFFFAC33),
    SONARR("Sonarr", 8989, "sonarr", 0xFF00D4FF),
    JELLYFIN("Jellyfin", 8096, "jellyfin", 0xFF00A4DC),
    PROWLARR("Prowlarr", 9696, "prowlarr", 0xFFFF6B35),
    QBITTORRENT("qBittorrent", 8080, "qbittorrent", 0xFF6BBF4E),
    SEERR("Seerr", 5055, "seerr", 0xFF7C3AED),
    MAINTAINERR("Maintainerr", 6246, "maintainerr", 0xFFEC4899)
}

@Serializable
data class AppConfig(
    val setupMode: String = "manual",
    val baseIp: String = "",
    val baseDomain: String = "",
    val serviceConfigs: Map<String, ServiceConfig> = emptyMap()
)

@Serializable
data class ServiceConfig(
    val enabled: Boolean = false,
    val url: String = "",
    val apiKey: String = "",
    val username: String = "admin",
    val password: String = ""
) {
    fun toInstance(type: ServiceType) = Instance(
        type = when (type) {
            ServiceType.RADARR -> InstanceType.RADARR
            ServiceType.PROWLARR -> InstanceType.PROWLARR
            else -> InstanceType.SONARR
        },
        label = type.label,
        url = url,
        apiKey = apiKey
    )

    fun toJellyfinInstance() = JellyfinInstance(
        label = "Jellyfin",
        url = url,
        apiKey = apiKey
    )
}

// qBittorrent models
@Serializable
data class QBitTorrent(
    val hash: String = "",
    val name: String = "",
    val size: Long = 0,
    val progress: Float = 0f,
    val dlspeed: Long = 0,
    val upspeed: Long = 0,
    val num_seeds: Int = 0,
    val num_leechs: Int = 0,
    val state: String = "",
    val eta: Long = 0,
    val save_path: String = "",
    val added_on: Long = 0,
    val downloaded: Long = 0,
    val uploaded: Long = 0,
    val category: String = "",
    val ratio: Float = 0f
) {
    val sizeFormatted: String get() {
        val mb = size / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }
    val dlSpeedFormatted: String get() {
        val kb = dlspeed / 1024.0
        return if (kb >= 1024) String.format("%.1f MB/s", kb / 1024) else String.format("%.0f KB/s", kb)
    }
    val upSpeedFormatted: String get() {
        val kb = upspeed / 1024.0
        return if (kb >= 1024) String.format("%.1f MB/s", kb / 1024) else String.format("%.0f KB/s", kb)
    }
    val stateLabel: String get() = when (state) {
        "downloading" -> "Téléchargement"
        "uploading" -> "Upload"
        "stalledDL" -> "En attente"
        "stalledUP" -> "Upload en attente"
        "pausedDL", "pausedUP" -> "En pause"
        "queuedDL", "queuedUP" -> "File d'attente"
        "checkingDL", "checkingUP" -> "Vérification"
        "error" -> "Erreur"
        "missingFiles" -> "Fichiers manquants"
        "moving" -> "Déplacement"
        "forcedDL" -> "Forcé"
        else -> state
    }
    val stateColor: Long get() = when (state) {
        "downloading", "forcedDL" -> 0xFF3B82F6
        "uploading" -> 0xFF22C55E
        "error", "missingFiles" -> 0xFFEF4444
        "pausedDL", "pausedUP" -> 0xFF6B7280
        else -> 0xFF6B7280
    }
}

@Serializable
data class QBitMainData(
    val server_state: QBitServerState = QBitServerState()
)

@Serializable
data class QBitServerState(
    val dl_info_speed: Long = 0,
    val dl_info_data: Long = 0,
    val up_info_speed: Long = 0,
    val up_info_data: Long = 0,
    val free_space_on_disk: Long = 0,
    val connection_status: String = ""
) {
    val dlSpeedFormatted: String get() {
        val kb = dl_info_speed / 1024.0
        return if (kb >= 1024) String.format("%.1f MB/s", kb / 1024) else String.format("%.0f KB/s", kb)
    }
    val upSpeedFormatted: String get() {
        val kb = up_info_speed / 1024.0
        return if (kb >= 1024) String.format("%.1f MB/s", kb / 1024) else String.format("%.0f KB/s", kb)
    }
}

// Prowlarr models
@Serializable
data class ProwlarrIndexer(
    val id: Int = 0,
    val name: String = "",
    val enable: Boolean = false,
    val protocol: String = "",
    val privacy: String = "",
    val supportsRss: Boolean = false,
    val supportsSearch: Boolean = false,
    val vipExpiration: String? = null,
    val indexerUrls: List<String> = emptyList(),
    val language: String? = null
) {
    val protocolIcon: String get() = if (protocol == "usenet") "U" else "T"
}

@Serializable
data class ProwlarrIndexerStats(
    val indexers: List<ProwlarrIndexerStat> = emptyList()
)

@Serializable
data class ProwlarrIndexerStat(
    val indexerId: Int = 0,
    val indexerName: String = "",
    val averageResponseTime: Int = 0,
    val numberOfQueries: Int = 0,
    val numberOfGrabs: Int = 0,
    val numberOfRssQueries: Int = 0,
    val numberOfAuthQueries: Int = 0,
    val numberOfFailedQueries: Int = 0,
    val numberOfFailedGrabs: Int = 0,
    val numberOfFailedRssQueries: Int = 0,
    val numberOfFailedAuthQueries: Int = 0
)

@Serializable
data class ProwlarrSearchResult(
    val guid: String = "",
    val indexerId: Int = 0,
    val indexer: String = "",
    val title: String = "",
    val size: Long = 0,
    val publishDate: String = "",
    val downloadUrl: String? = null,
    val infoUrl: String? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val protocol: String = "",
    val categories: List<ProwlarrCategory> = emptyList(),
    val age: Int = 0,
    val ageHours: Double = 0.0
) {
    val sizeFormatted: String get() {
        val mb = size / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }
    val ageFormatted: String get() = when {
        ageHours < 1 -> "${(ageHours * 60).toInt()}m"
        ageHours < 24 -> "${ageHours.toInt()}h"
        else -> "${age}d"
    }
}

@Serializable
data class ProwlarrCategory(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class ProwlarrHistoryResponse(
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalRecords: Int = 0,
    val records: List<ProwlarrHistoryItem> = emptyList()
)

@Serializable
data class ProwlarrHistoryItem(
    val id: Int = 0,
    val indexer: String = "",
    val successful: Boolean = false,
    val date: String = "",
    val eventType: String = "",
    val data: Map<String, String?> = emptyMap()
)

// Seerr models
@Serializable
data class SeerrRequest(
    val id: Int = 0,
    val status: Int = 1,
    val createdAt: String = "",
    val updatedAt: String? = null,
    val type: String = "movie",
    val requestedBy: SeerrUser? = null,
    val modifiedBy: SeerrUser? = null,
    val media: SeerrMedia? = null,
    val seasons: List<SeerrSeason> = emptyList(),
    val is4k: Boolean = false
) {
    val statusLabel: String get() = when (status) {
        1 -> "Pending"; 2 -> "Approved"; 3 -> "Declined"; 4 -> "Available"; 5 -> "Processing"; else -> "Unknown"
    }
    val statusColor: Long get() = when (status) {
        1 -> 0xFFF59E0B; 2 -> 0xFF22C55E; 3 -> 0xFFEF4444; 4 -> 0xFF3B82F6; else -> 0xFF6B7280
    }
}

@Serializable
data class SeerrUser(
    val id: Int = 0,
    val displayName: String = "",
    val email: String = "",
    val avatar: String? = null,
    val requestCount: Int? = null,
    val permissions: Int? = null,
    val userType: Int? = null
)

@Serializable
data class SeerrMedia(
    val id: Int = 0,
    val mediaType: String = "movie",
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val status: Int = 1,
    val status4k: Int = 1,
    val externalServiceId: Int? = null,
    val externalServiceId4k: Int? = null,
    val serviceId: Int? = null,
    val serviceId4k: Int? = null
)

@Serializable
data class SeerrSeason(
    val id: Int = 0,
    val seasonNumber: Int = 0,
    val status: Int = 1
)

@Serializable
data class SeerrRequestsResponse(
    val pageInfo: SeerrPageInfo = SeerrPageInfo(),
    val results: List<SeerrRequest> = emptyList()
)

@Serializable
data class SeerrPageInfo(val pages: Int = 0, val pageSize: Int = 10, val results: Int = 0, val page: Int = 1)

@Serializable
data class SeerrSearchResult(
    val id: Int = 0,
    val mediaType: String = "movie",
    val title: String? = null,
    val name: String? = null,
    val originalTitle: String? = null,
    val originalName: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double = 0.0,
    val mediaInfo: SeerrMedia? = null
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayDate: String get() = (releaseDate ?: firstAirDate)?.take(4) ?: ""
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

@Serializable
data class SeerrSearchResponse(
    val page: Int = 1,
    val totalPages: Int = 0,
    val totalResults: Int = 0,
    val results: List<SeerrSearchResult> = emptyList()
)

@Serializable
data class SeerrUserList(
    val pageInfo: SeerrPageInfo = SeerrPageInfo(),
    val results: List<SeerrUser> = emptyList()
)

// Maintainerr models
@Serializable
data class MaintainerrRuleGroup(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val libraryId: String = "",
    val isActive: Boolean = false,
    val dataType: String = "movie",
    val collection: MaintainerrCollection? = null,
    val rules: List<MaintainerrRule> = emptyList()
)

@Serializable
data class MaintainerrCollection(
    val id: Int = 0,
    val title: String? = null,
    val description: String? = null,
    val isActive: Boolean = false,
    val deleteAfterDays: Int? = null,
    val type: String = "movie",
    val arrAction: Int? = null,
    val visibleOnHome: Boolean? = null,
    val manualCollection: Boolean? = null
) {
    val typeLabel: String get() = when (type) { "movie" -> "Movies"; "show" -> "Shows"; else -> type }
    val arrActionLabel: String get() = when (arrAction) {
        0 -> "Do Nothing"; 1 -> "Delete"; 2 -> "Unmonitor & Delete"; 3 -> "Unmonitor"; 4 -> "Unmonitor & Keep"; else -> "Delete"
    }
}

@Serializable
data class MaintainerrRule(
    val id: Int = 0,
    val ruleGroupId: Int = 0,
    val section: Int = 0,
    val isActive: Boolean = false,
    val ruleJson: String = ""
)

@Serializable
data class MaintainerrMedia(
    val id: Int = 0,
    val plexId: Int = 0,
    val collectionId: Int = 0,
    val addDate: String? = null,
    val image_path: String? = null,
    val isManual: Boolean = false
)

@Serializable
data class MaintainerrStorageMetrics(
    val collectionCount: Int = 0,
    val mediaCount: Int = 0,
    val collectionSummary: MaintainerrCollectionSummary? = null,
    val cleanupTotals: MaintainerrCleanupTotals? = null
)

@Serializable
data class MaintainerrCollectionSummary(
    val movieSizeBytes: Long = 0,
    val showSizeBytes: Long = 0,
    val seasonSizeBytes: Long = 0,
    val episodeSizeBytes: Long = 0,
    val reclaimableMovieCount: Int = 0,
    val reclaimableShowCount: Int = 0
) {
    val totalBytes: Long get() = movieSizeBytes + showSizeBytes + seasonSizeBytes + episodeSizeBytes
    val totalFormatted: String get() {
        val gb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) String.format("%.1f GB", gb) else String.format("%.0f MB", totalBytes / (1024.0 * 1024.0))
    }
}

@Serializable
data class MaintainerrCleanupTotals(
    val itemsHandled: Int = 0,
    val moviesHandled: Int = 0,
    val showsHandled: Int = 0
)