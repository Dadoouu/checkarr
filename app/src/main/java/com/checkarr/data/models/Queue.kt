package com.checkarr.data.models

import kotlinx.serialization.Serializable

@Serializable
data class QueueItem(
    val id: Int = 0,
    val movieId: Int? = null,
    val seriesId: Int? = null,
    val episodeId: Int? = null,
    val title: String = "",
    val status: String = "",
    val trackedDownloadStatus: String? = null,
    val trackedDownloadState: String? = null,
    val statusMessages: List<StatusMessage> = emptyList(),
    val size: Double = 0.0,
    val sizeleft: Double = 0.0,
    val timeleft: String? = null,
    val estimatedCompletionTime: String? = null,
    val added: String? = null,
    val protocol: String = "torrent",
    val downloadClient: String? = null,
    val outputPath: String? = null,
    val quality: QualityModel? = null
) {
    val progress: Float get() {
        if (size <= 0) return 0f
        return ((size - sizeleft) / size).toFloat().coerceIn(0f, 1f)
    }
    val sizeFormatted: String get() {
        val mb = size / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }
    val statusLabel: String get() = when (status.lowercase()) {
        "downloading" -> "Downloading"
        "queued" -> "Queued"
        "paused" -> "Paused"
        "completed" -> "Completed"
        "failed" -> "Failed"
        "warning" -> "Warning"
        "delay" -> "Delayed"
        "downloadclientunavailable" -> "Client Unavailable"
        "fallback" -> "Fallback"
        else -> status
    }
}

@Serializable
data class StatusMessage(
    val title: String = "",
    val messages: List<String> = emptyList()
)

@Serializable
data class QueueResponse(
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalRecords: Int = 0,
    val records: List<QueueItem> = emptyList()
)

@Serializable
data class HistoryItem(
    val id: Int = 0,
    val movieId: Int? = null,
    val seriesId: Int? = null,
    val episodeId: Int? = null,
    val sourceTitle: String = "",
    val quality: QualityModel? = null,
    val date: String = "",
    val eventType: String = "",
    val data: Map<String, String?> = emptyMap(),
    val downloadId: String? = null
) {
    val eventTypeLabel: String get() = when (eventType.lowercase()) {
        "grabbed" -> "Grabbed"
        "downloadfolderimported" -> "Imported"
        "downloadfailed" -> "Download Failed"
        "downloadignored" -> "Download Ignored"
        "moviefileimported" -> "File Imported"
        "moviefiledeleted" -> "File Deleted"
        "moviefileadded" -> "File Added"
        "unknown" -> "Unknown"
        else -> eventType
    }
}

@Serializable
data class HistoryResponse(
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalRecords: Int = 0,
    val records: List<HistoryItem> = emptyList()
)

@Serializable
data class CalendarItem(
    val id: Int = 0,
    val title: String = "",
    val year: Int = 0,
    val inCinemas: String? = null,
    val physicalRelease: String? = null,
    val digitalRelease: String? = null,
    val images: List<MediaImage> = emptyList(),
    val hasFile: Boolean = false,
    val monitored: Boolean = false,
    val isAvailable: Boolean = false
)

@Serializable
data class SeriesCalendarItem(
    val id: Int = 0,
    val seriesId: Int = 0,
    val title: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val airDate: String? = null,
    val airDateUtc: String? = null,
    val hasFile: Boolean = false,
    val monitored: Boolean = false,
    val series: Series? = null
)

@Serializable
data class MovieRelease(
    val guid: String = "",
    val quality: QualityModel = QualityModel(),
    val title: String = "",
    val indexer: String = "",
    val size: Long = 0,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val protocol: String = "",
    val age: Int = 0,
    val ageHours: Double = 0.0,
    val ageMinutes: Double = 0.0,
    val rejected: Boolean = false,
    val rejections: List<Rejection> = emptyList(),
    val indexerFlags: Int = 0,
    val downloadAllowed: Boolean = true,
    val releaseType: String = ""
) {
    val sizeFormatted: String get() {
        val mb = size / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }
    val ageFormatted: String get() = when {
        ageMinutes < 60 -> "${ageMinutes.toInt()}m"
        ageHours < 24 -> "${ageHours.toInt()}h"
        else -> "${age}d"
    }
}

@Serializable
data class Rejection(
    val reason: String = "",
    val type: String = "permanent"
)

@Serializable
data class SystemStatus(
    val appName: String = "",
    val version: String = "",
    val buildTime: String? = null,
    val startupPath: String? = null,
    val appData: String? = null,
    val osName: String? = null,
    val osVersion: String? = null
)

@Serializable
data class DiskSpace(
    val path: String = "",
    val label: String = "",
    val freeSpace: Long = 0,
    val totalSpace: Long = 0
) {
    val usedPercent: Float get() {
        if (totalSpace <= 0) return 0f
        return ((totalSpace - freeSpace).toFloat() / totalSpace.toFloat()).coerceIn(0f, 1f)
    }
    val freeFormatted: String get() {
        val gb = freeSpace / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
    val totalFormatted: String get() {
        val gb = totalSpace / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
}
