package com.checkarr.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: Int = 0,
    val title: String = "",
    val originalTitle: String = "",
    val sortTitle: String = "",
    val overview: String = "",
    val year: Int = 0,
    val imdbId: String? = null,
    val tmdbId: Int = 0,
    val runtime: Int = 0,
    val status: String = "",
    val monitored: Boolean = false,
    val hasFile: Boolean = false,
    val isAvailable: Boolean = false,
    val qualityProfileId: Int = 0,
    val sizeOnDisk: Long = 0,
    val studio: String? = null,
    val path: String = "",
    val rootFolderPath: String? = null,
    val inCinemas: String? = null,
    val digitalRelease: String? = null,
    val physicalRelease: String? = null,
    val genres: List<String> = emptyList(),
    val ratings: MovieRatings = MovieRatings(),
    val images: List<MediaImage> = emptyList(),
    val movieFile: MovieFile? = null,
    val certification: String? = null,
    val added: String = "",
    val tags: List<Int> = emptyList(),
    val popularity: Double = 0.0
) {
    val remotePoster: String? get() = images.firstOrNull { it.coverType == "poster" }?.remoteUrl
    val remoteFanart: String? get() = images.firstOrNull { it.coverType == "fanart" }?.remoteUrl
    val runtimeFormatted: String get() {
        val h = runtime / 60
        val m = runtime % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val fileSizeFormatted: String get() {
        val mb = sizeOnDisk / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }
}

@Serializable
data class MovieRatings(
    val imdb: Rating? = null,
    val tmdb: Rating? = null,
    val rottenTomatoes: Rating? = null,
    val trakt: Rating? = null
)

@Serializable
data class Rating(
    val votes: Int = 0,
    val value: Double = 0.0,
    val type: String = "user"
)

@Serializable
data class MediaImage(
    val coverType: String = "",
    val url: String = "",
    val remoteUrl: String? = null
)

@Serializable
data class MovieFile(
    val id: Int = 0,
    val relativePath: String = "",
    val path: String = "",
    val size: Long = 0,
    val dateAdded: String = "",
    val quality: QualityModel? = null,
    val mediaInfo: MediaInfo? = null
)

@Serializable
data class QualityModel(
    val quality: Quality = Quality(),
    val revision: Revision = Revision()
)

@Serializable
data class Quality(
    val id: Int = 0,
    val name: String = "",
    val resolution: Int = 0,
    val source: String = ""
)

@Serializable
data class Revision(
    val version: Int = 1,
    val real: Int = 0,
    val isRepack: Boolean = false
)

@Serializable
data class MediaInfo(
    val videoCodec: String? = null,
    val videoBitDepth: Int = 0,
    val videoColourPrimaries: String? = null,
    val videoTransferCharacteristics: String? = null,
    val audioCodec: String? = null,
    val audioChannels: Double = 0.0,
    val audioStreamCount: Int = 0,
    val videoFps: Double = 0.0,
    val resolution: String? = null,
    val runTime: String? = null,
    val scanType: String? = null,
    val subtitles: String? = null
)

@Serializable
data class QualityProfile(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class RootFolder(
    val id: Int = 0,
    val path: String = "",
    val freeSpace: Long = 0
)

@Serializable
data class Tag(
    val id: Int = 0,
    val label: String = ""
)
