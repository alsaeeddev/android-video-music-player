package com.example.data.model

import android.net.Uri

enum class MediaType {
    VIDEO,
    AUDIO
}

/**
 * Clean data model representing a media file (Video or Audio) in As Player.
 */
data class MediaItemModel(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val folderName: String = "Internal Storage",
    val width: Int = 0,
    val height: Int = 0,
    val mediaType: MediaType = MediaType.VIDEO,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val isSample: Boolean = false,
    val filePath: String = ""
) {
    val uri: Uri get() = Uri.parse(uriString)

    val formattedDuration: String
        get() = com.example.core.util.FormatUtils.formatDuration(durationMs)

    val formattedSize: String
        get() = com.example.core.util.FormatUtils.formatFileSize(sizeBytes)

    val resolutionBadge: String
        get() = com.example.core.util.FormatUtils.formatResolution(width, height)
}
