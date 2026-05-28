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
        type = if (type == ServiceType.RADARR) InstanceType.RADARR else InstanceType.SONARR,
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
    val supportsSearch: Boolean = false
)

// Seerr models
@Serializable
data class SeerrRequest(
    val id: Int = 0,
    val status: Int = 1,
    val createdAt: String = "",
    val requestedBy: SeerrUser? = null
) {
    val statusLabel: String get() = when (status) {
        1 -> "En attente"; 2 -> "Approuvée"; 3 -> "Refusée"; else -> "Inconnu"
    }
}

@Serializable
data class SeerrUser(val id: Int = 0, val displayName: String = "", val email: String = "")

@Serializable
data class SeerrRequestsResponse(
    val pageInfo: SeerrPageInfo = SeerrPageInfo(),
    val results: List<SeerrRequest> = emptyList()
)

@Serializable
data class SeerrPageInfo(val pages: Int = 0, val pageSize: Int = 10, val results: Int = 0, val page: Int = 1)

// Maintainerr models
@Serializable
data class MaintainerrCollection(
    val id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val isActive: Boolean = false,
    val deleteAfterDays: Int? = null,
    val type: Int = 1
)
