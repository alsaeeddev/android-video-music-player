package com.example.core.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object VideoPlayer : Screen("video_player")
    data object NowPlaying : Screen("now_playing")
    data object Playlists : Screen("playlists")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: Long, playlistName: String) = "playlist_detail/$playlistId/$playlistName"
    }
    data object Settings : Screen("settings")
}
