package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaId"]
)
data class PlaylistItemCrossRef(
    val playlistId: Long,
    val mediaId: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val mediaId: String,
    val title: String,
    val artist: String,
    val uriString: String,
    val mediaType: String,
    val playedAt: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Entity(tableName = "resume_positions")
data class ResumePositionEntity(
    @PrimaryKey
    val mediaId: String,
    val uriString: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)
