package com.checkarr.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Series(
    val id: Int = 0,
    val title: String = "",
    val sortTitle: String = "",
    val overview: String = "",
    val year: Int = 0,
    val tvdbId: Int = 0,
    val imdbId: String? = null,
    val status: String = "",
    val network: String? = null,
    val runtime: Int = 0,
    val monitored: Boolean = false,
    val seriesType: String = "standard",
    val qualityProfileId: Int = 0,
    val path: String = "",
    val rootFolderPath: String? = null,
    val ended: Boolean = false,
    val genres: List<String> = emptyList(),
    val images: List<MediaImage> = emptyList(),
    val ratings: MovieRatings = MovieRatings(),
    val seasons: List<Season> = emptyList(),
    val tags: List<Int> = emptyList(),
    val statistics: SeriesStatistics? = null,
    val added: String = "",
    val certification: String? = null,
    val cleanTitle: String? = null,
    val titleSlug: String? = null,
    val firstAired: String? = null,
    val languageProfileId: Int = 0
) {
    val remotePoster: String? get() = images.firstOrNull { it.coverType == "poster" }?.remoteUrl
    val remoteFanart: String? get() = images.firstOrNull { it.coverType == "fanart" }?.remoteUrl
    val statusLabel: String get() = when (status.lowercase()) {
        "continuing" -> "Continuing"
        "ended" -> "Ended"
        "upcoming" -> "Upcoming"
        "deleted" -> "Deleted"
        else -> status
    }
    val runtimeFormatted: String get() {
        val h = runtime / 60
        val m = runtime % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

@Serializable
data class Season(
    val seasonNumber: Int = 0,
    val monitored: Boolean = false,
    val statistics: SeasonStatistics? = null
)

@Serializable
data class SeasonStatistics(
    val episodeCount: Int = 0,
    val episodeFileCount: Int = 0,
    val totalEpisodeCount: Int = 0,
    val sizeOnDisk: Long = 0,
    val percentOfEpisodes: Double = 0.0
)

@Serializable
data class SeriesStatistics(
    val episodeCount: Int = 0,
    val episodeFileCount: Int = 0,
    val totalEpisodeCount: Int = 0,
    val sizeOnDisk: Long = 0,
    val percentOfEpisodes: Double = 0.0,
    val seasonCount: Int = 0
)

@Serializable
data class Episode(
    val id: Int = 0,
    val seriesId: Int = 0,
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val title: String = "",
    val overview: String? = null,
    val airDate: String? = null,
    val airDateUtc: String? = null,
    val monitored: Boolean = false,
    val hasFile: Boolean = false,
    val episodeFile: MovieFile? = null,
    val grabbed: Boolean = false,
    val images: List<MediaImage> = emptyList()
) {
    val episodeIdentifier: String get() = "S%02dE%02d".format(seasonNumber, episodeNumber)
}
