package com.checkarr.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object Calendar : Screen("calendar")
    object Activity : Screen("activity")
    object Settings : Screen("settings")
    object Setup : Screen("setup")
    object Appearance : Screen("appearance")

    object MovieDetail : Screen("movie/{movieId}") {
        fun createRoute(movieId: Int) = "movie/$movieId"
    }
    object MovieSearch : Screen("movie/search")
    object MovieReleases : Screen("movie/{movieId}/releases") {
        fun createRoute(movieId: Int) = "movie/$movieId/releases"
    }

    object SeriesDetail : Screen("series/{seriesId}") {
        fun createRoute(seriesId: Int) = "series/$seriesId"
    }
    object SeriesSearch : Screen("series/search")
    object SeasonDetail : Screen("series/{seriesId}/season/{seasonNumber}") {
        fun createRoute(seriesId: Int, seasonNumber: Int) = "series/$seriesId/season/$seasonNumber"
    }
    object EpisodeReleases : Screen("episode/{episodeId}/releases") {
        fun createRoute(episodeId: Int) = "episode/$episodeId/releases"
    }

    object Jellyfin : Screen("jellyfin")
    object QBittorrent : Screen("qbittorrent")
    object Prowlarr : Screen("prowlarr")
    object Seerr : Screen("seerr")
    object Maintainerr : Screen("maintainerr")
}
