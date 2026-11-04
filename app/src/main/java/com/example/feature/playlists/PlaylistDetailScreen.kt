package com.example.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.feature.library.EmptyMediaView
import com.example.feature.library.MediaDetailSheet
import com.example.feature.library.MediaItemCard
import com.example.service.player.PlayerController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    mediaRepository: MediaRepository,
    playerController: PlayerController,
    onNavigateBack: () -> Unit,
    onNavigateToVideoPlayer: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsFlow: Flow<List<MediaItemModel>> = remember(playlistId) {
        mediaRepository.getPlaylistItems(playlistId)
    }
    val playlistItems: List<MediaItemModel> by itemsFlow.collectAsState(initial = emptyList())
    val currentMedia by playerController.currentMedia.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedMediaForDetails by remember { mutableStateOf<MediaItemModel?>(null) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
               // .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playlistItems.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (playlistItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            playerController.playMediaList(playlistItems, 0)
                            if (playlistItems.first().mediaType == MediaType.VIDEO) {
                                onNavigateToVideoPlayer()
                            } else {
                                onNavigateToNowPlaying()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All")
                    }
                }
            }

            if (playlistItems.isEmpty()) {
                EmptyMediaView(
                    title = "Playlist is Empty",
                    message = "Add songs or videos to this playlist from the library tabs.",
                    icon = Icons.Default.MusicNote,
                    onAction = null,
                    actionLabel = ""
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlistItems, key = { it.id }) { item ->
                        MediaItemCard(
                            media = item,
                            isCurrentPlaying = item.id == currentMedia?.id,
                            onClick = {
                                val idx = playlistItems.indexOf(item).coerceAtLeast(0)
                                playerController.playMediaList(playlistItems, idx)
                                if (item.mediaType == MediaType.VIDEO) {
                                    onNavigateToVideoPlayer()
                                } else {
                                    onNavigateToNowPlaying()
                                }
                            },
                            onDetailsClick = { selectedMediaForDetails = item },
                            onToggleFavorite = {
                                scope.launch { mediaRepository.toggleFavorite(item.id) }
                            },
                            onAddToPlaylistClick = {},
                            secondaryActionLabel = "Remove from list",
                            onSecondaryActionClick = {
                                scope.launch {
                                    mediaRepository.removeMediaFromPlaylist(playlistId, item.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        selectedMediaForDetails?.let { media ->
            MediaDetailSheet(
                media = media,
                onDismiss = { selectedMediaForDetails = null }
            )
        }
    }
}
