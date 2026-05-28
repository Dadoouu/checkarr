package com.checkarr.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyfinInstance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val url: String = "",
    val apiKey: String = "",
    val color: String = "blue"
) {
    val displayName: String get() = label.ifBlank {
        url.removePrefix("http://").removePrefix("https://").trimEnd('/')
    }
    val isValid: Boolean get() = url.isNotBlank() && apiKey.isNotBlank()
}

@Serializable
data class JellyfinSession(
    @SerialName("Id") val id: String = "",
    @SerialName("UserName") val userName: String = "",
    @SerialName("Client") val client: String = "",
    @SerialName("DeviceName") val deviceName: String = "",
    @SerialName("LastActivityDate") val lastActivityDate: String = "",
    @SerialName("NowPlayingItem") val nowPlayingItem: JellyfinNowPlaying? = null,
    @SerialName("PlayState") val playState: JellyfinPlayState? = null
)

@Serializable
data class JellyfinNowPlaying(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long = 0,
    @SerialName("ProductionYear") val productionYear: Int? = null
) {
    val displayTitle: String get() = when {
        seriesName != null && indexNumber != null && parentIndexNumber != null ->
            "$seriesName S${parentIndexNumber.toString().padStart(2,'0')}E${indexNumber.toString().padStart(2,'0')} · $name"
        seriesName != null -> "$seriesName · $name"
        else -> name
    }
    val runtimeMinutes: Int get() = (runTimeTicks / 600_000_000).toInt()
}

@Serializable
data class JellyfinPlayState(
    @SerialName("PositionTicks") val positionTicks: Long = 0,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("IsMuted") val isMuted: Boolean = false,
    @SerialName("VolumeLevel") val volumeLevel: Int = 100
) {
    val progressPercent: Float get() = 0f // computed against NowPlaying.runTimeTicks
}

@Serializable
data class JellyfinItem(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("DateCreated") val dateCreated: String = "",
    @SerialName("RunTimeTicks") val runTimeTicks: Long = 0,
    @SerialName("CommunityRating") val communityRating: Double? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Genres") val genres: List<String> = emptyList(),
    @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
    @SerialName("UserData") val userData: JellyfinUserData? = null
) {
    val displayTitle: String get() = when {
        seriesName != null && indexNumber != null && parentIndexNumber != null ->
            "$seriesName S${parentIndexNumber.toString().padStart(2,'0')}E${indexNumber.toString().padStart(2,'0')} · $name"
        seriesName != null -> "$seriesName · $name"
        else -> name
    }
    val runtimeMinutes: Int get() = (runTimeTicks / 600_000_000).toInt()
    fun posterUrl(baseUrl: String): String? =
        if (imageTags.containsKey("Primary")) "$baseUrl/Items/$id/Images/Primary?maxHeight=300" else null
}

@Serializable
data class JellyfinUserData(
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("Played") val played: Boolean = false,
    @SerialName("LastPlayedDate") val lastPlayedDate: String? = null
)

@Serializable
data class JellyfinItemsResponse(
    @SerialName("Items") val items: List<JellyfinItem> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)

@Serializable
data class JellyfinSystemInfo(
    @SerialName("ServerName") val serverName: String = "",
    @SerialName("Version") val version: String = "",
    @SerialName("OperatingSystem") val operatingSystem: String = "",
    @SerialName("Id") val id: String = ""
)

@Serializable
data class JellyfinDriveInfo(
    @SerialName("Name") val name: String = "",
    @SerialName("Path") val path: String = "",
    @SerialName("TotalSpace") val totalSpace: Long = 0,
    @SerialName("FreeSpace") val freeSpace: Long = 0
) {
    val usedSpace: Long get() = totalSpace - freeSpace
    val usedPercent: Float get() = if (totalSpace > 0) usedSpace.toFloat() / totalSpace else 0f
    fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) String.format("%.1f GB", gb) else String.format("%.0f MB", bytes / (1024.0 * 1024.0))
    }
}

@Serializable
data class JellyfinActivity(
    @SerialName("Id") val id: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("ShortOverview") val shortOverview: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("Date") val date: String = "",
    @SerialName("UserId") val userId: String? = null,
    @SerialName("UserName") val userName: String? = null,
    @SerialName("Severity") val severity: String = "Information"
)

@Serializable
data class JellyfinActivityResponse(
    @SerialName("Items") val items: List<JellyfinActivity> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)
