package com.example.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType

@Composable
fun VideoListTab(
    videos: List<MediaItemModel>,
    currentMediaId: String?,
    onVideoClick: (MediaItemModel) -> Unit,
    onDetailsClick: (MediaItemModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylistClick: (MediaItemModel) -> Unit,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        EmptyMediaView(
            title = "No Videos Found",
            message = "Tap below to scan device storage for video files.",
            icon = Icons.Default.Movie,
            onAction = onRefresh,
            actionLabel = "Refresh"
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                MediaItemCard(
                    media = video,
                    isCurrentPlaying = video.id == currentMediaId,
                    onClick = { onVideoClick(video) },
                    onDetailsClick = { onDetailsClick(video) },
                    onToggleFavorite = { onToggleFavorite(video.id) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                )
            }
        }
    }
}

@Composable
fun AudioListTab(
    audios: List<MediaItemModel>,
    currentMediaId: String?,
    onAudioClick: (MediaItemModel) -> Unit,
    onDetailsClick: (MediaItemModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylistClick: (MediaItemModel) -> Unit,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (audios.isEmpty()) {
        EmptyMediaView(
            title = "No Music Found",
            message = "Tap below to scan device storage for music and audio files.",
            icon = Icons.Default.MusicNote,
            onAction = onRefresh,
            actionLabel = "Refresh"
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(audios, key = { it.id }) { audio ->
                MediaItemCard(
                    media = audio,
                    isCurrentPlaying = audio.id == currentMediaId,
                    onClick = { onAudioClick(audio) },
                    onDetailsClick = { onDetailsClick(audio) },
                    onToggleFavorite = { onToggleFavorite(audio.id) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(audio) }
                )
            }
        }
    }
}

@Composable
fun FavoritesListTab(
    favorites: List<MediaItemModel>,
    currentMediaId: String?,
    onMediaClick: (MediaItemModel) -> Unit,
    onDetailsClick: (MediaItemModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylistClick: (MediaItemModel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (favorites.isEmpty()) {
        EmptyMediaView(
            title = "No Favorites Yet",
            message = "Tap the heart icon on Now Playing or choose 'Add to Favorites' from the menu to save tracks here.",
            icon = Icons.Default.Favorite,
            onAction = null,
            actionLabel = ""
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites, key = { it.id }) { item ->
                MediaItemCard(
                    media = item,
                    isCurrentPlaying = item.id == currentMediaId,
                    onClick = { onMediaClick(item) },
                    onDetailsClick = { onDetailsClick(item) },
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(item) }
                )
            }
        }
    }
}

@Composable
fun FolderListTab(
    folders: Map<String, List<MediaItemModel>>,
    selectedFolder: String?,
    currentMediaId: String?,
    onSelectFolder: (String?) -> Unit,
    onMediaClick: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onDetailsClick: (MediaItemModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylistClick: (MediaItemModel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (selectedFolder != null) {
        val folderItems = folders[selectedFolder].orEmpty()
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectFolder(null) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Folders")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedFolder,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${folderItems.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folderItems, key = { it.id }) { item ->
                    MediaItemCard(
                        media = item,
                        isCurrentPlaying = item.id == currentMediaId,
                        onClick = { onMediaClick(item, folderItems) },
                        onDetailsClick = { onDetailsClick(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(item) }
                    )
                }
            }
        }
    } else {
        if (folders.isEmpty()) {
            EmptyMediaView(
                title = "No Folders Available",
                message = "Media folders will appear here once files are detected.",
                icon = Icons.Default.Folder,
                onAction = null,
                actionLabel = ""
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders.keys.toList(), key = { it }) { folderName ->
                    val count = folders[folderName]?.size ?: 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectFolder(folderName) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$count files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    history: List<PlaybackHistoryEntity>,
    onClearHistory: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        EmptyMediaView(
            title = "No Watch History",
            message = "Media you play will be recorded here so you can easily resume anytime.",
            icon = Icons.Default.Movie,
            onAction = null,
            actionLabel = ""
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Played",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.mediaId }) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.mediaType == "VIDEO") Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.mediaType == "VIDEO") "Resume at ${com.example.core.util.FormatUtils.formatDuration(item.lastPositionMs)}" else item.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMediaView(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: (() -> Unit)?,
    actionLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}
