package com.example.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.feature.nowplaying.MiniPlayerBar
import com.example.service.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerController: PlayerController,
    onNavigateToVideoPlayer: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPlayingMedia by playerController.currentMedia.collectAsState()
    var searchVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    fun hasMediaAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshMedia()
    }

    val onRefreshWithPermissionCheck = {
        if (!hasMediaAccess()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.refreshMedia()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                MiniPlayerBar(
                    playerController = playerController,
                    onExpandClick = {
                        val current = currentPlayingMedia
                        if (current?.mediaType == MediaType.VIDEO) {
                            onNavigateToVideoPlayer()
                        } else {
                            onNavigateToNowPlaying()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
               // .statusBarsPadding()
        ) {
            // App Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_as_player_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "As Player",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { searchVisible = !searchVisible },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToPlaylists,
                        modifier = Modifier.testTag("nav_playlists_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = "Playlists",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar (Animated reveal)
            AnimatedVisibility(visible = searchVisible) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_input_field"),
                    placeholder = { Text("Search title, artist, album...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = uiState.currentTab.ordinal,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Sorting & Filter bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortOption.entries.forEach { sort ->
                    val isSelected = uiState.sortOption == sort
                    AssistChip(
                        onClick = { viewModel.setSortOption(sort) },
                        label = {
                            Text(
                                text = when (sort) {
                                    SortOption.TITLE -> "Name"
                                    SortOption.DATE -> "Date Added"
                                    SortOption.DURATION -> "Duration"
                                    SortOption.SIZE -> "Size"
                                }
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = if (uiState.isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRefreshWithPermissionCheck,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.currentTab) {
                    LibraryTab.VIDEOS -> {
                        VideoListTab(
                            videos = uiState.videos,
                            currentMediaId = currentPlayingMedia?.id,
                            onVideoClick = { video ->
                                viewModel.playMedia(video, uiState.videos)
                                onNavigateToVideoPlayer()
                            },
                            onDetailsClick = { viewModel.showMediaDetails(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(it) },
                            onRefresh = onRefreshWithPermissionCheck,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LibraryTab.MUSIC -> {
                        AudioListTab(
                            audios = uiState.audios,
                            currentMediaId = currentPlayingMedia?.id,
                            onAudioClick = { audio ->
                                viewModel.playMedia(audio, uiState.audios)
                                onNavigateToNowPlaying()
                            },
                            onDetailsClick = { viewModel.showMediaDetails(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(it) },
                            onRefresh = onRefreshWithPermissionCheck,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LibraryTab.FOLDERS -> {
                        FolderListTab(
                            folders = uiState.folders,
                            selectedFolder = uiState.selectedFolder,
                            currentMediaId = currentPlayingMedia?.id,
                            onSelectFolder = { viewModel.selectFolder(it) },
                            onMediaClick = { media, list ->
                                viewModel.playMedia(media, list)
                                if (media.mediaType == MediaType.VIDEO) onNavigateToVideoPlayer() else onNavigateToNowPlaying()
                            },
                            onDetailsClick = { viewModel.showMediaDetails(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(it) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LibraryTab.FAVORITES -> {
                        FavoritesListTab(
                            favorites = uiState.favorites,
                            currentMediaId = currentPlayingMedia?.id,
                            onMediaClick = { fav ->
                                viewModel.playMedia(fav, uiState.favorites)
                                if (fav.mediaType == MediaType.VIDEO) onNavigateToVideoPlayer() else onNavigateToNowPlaying()
                            },
                            onDetailsClick = { viewModel.showMediaDetails(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylistClick = { viewModel.showAddToPlaylistDialog(it) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LibraryTab.HISTORY -> {
                        HistoryTab(
                            history = uiState.history,
                            onClearHistory = { viewModel.clearHistory() },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Details Bottom Sheet
        uiState.selectedMediaForDetails?.let { media ->
            MediaDetailSheet(
                media = media,
                onDismiss = { viewModel.showMediaDetails(null) }
            )
        }

        // Add to Playlist Dialog
        uiState.selectedMediaForPlaylist?.let { media ->
            val playlists by viewModel.playlists.collectAsState()
            var showNewPlaylistInput by remember { mutableStateOf(false) }
            var newPlaylistName by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.showAddToPlaylistDialog(null) },
                title = { Text("Add to Playlist") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (playlists.isEmpty() && !showNewPlaylistInput) {
                            Text(
                                text = "No playlists found. Create one below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!showNewPlaylistInput) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.addMediaToPlaylist(playlist.id, media.id)
                                            },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistPlay,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showNewPlaylistInput) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("New Playlist Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!showNewPlaylistInput) {
                            TextButton(
                                onClick = { showNewPlaylistInput = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create New Playlist")
                            }
                        }
                    }
                },
                confirmButton = {
                    if (showNewPlaylistInput) {
                        Button(
                            onClick = {
                                viewModel.createPlaylistAndAddMedia(newPlaylistName, media.id)
                            },
                            enabled = newPlaylistName.isNotBlank()
                        ) {
                            Text("Create & Add")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showAddToPlaylistDialog(null) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
