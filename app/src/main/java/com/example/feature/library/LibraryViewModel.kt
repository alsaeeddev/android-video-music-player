package com.example.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.service.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    VIDEOS("Videos"),
    MUSIC("Music"),
    FOLDERS("Folders"),
    FAVORITES("Favorites"),
    HISTORY("History")
}

enum class SortOption {
    TITLE,
    DATE,
    DURATION,
    SIZE
}

data class LibraryUiState(
    val currentTab: LibraryTab = LibraryTab.VIDEOS,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE,
    val isSortAscending: Boolean = false,
    val videos: List<MediaItemModel> = emptyList(),
    val audios: List<MediaItemModel> = emptyList(),
    val favorites: List<MediaItemModel> = emptyList(),
    val history: List<PlaybackHistoryEntity> = emptyList(),
    val folders: Map<String, List<MediaItemModel>> = emptyMap(),
    val selectedFolder: String? = null,
    val selectedMediaForDetails: MediaItemModel? = null,
    val selectedMediaForPlaylist: MediaItemModel? = null,
    val isRefreshing: Boolean = false
)

class LibraryViewModel(
    private val repository: MediaRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _currentTab = MutableStateFlow(LibraryTab.VIDEOS)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.DATE)
    private val _isSortAscending = MutableStateFlow(false)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _selectedMediaForDetails = MutableStateFlow<MediaItemModel?>(null)
    private val _selectedMediaForPlaylist = MutableStateFlow<MediaItemModel?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val uiState: StateFlow<LibraryUiState> = combine(
        _currentTab,
        _searchQuery,
        _sortOption,
        _isSortAscending,
        _selectedFolder,
        _selectedMediaForDetails,
        _selectedMediaForPlaylist,
        _isRefreshing,
        repository.videosFlow,
        repository.audiosFlow,
        repository.favoritesFlow,
        repository.historyFlow
    ) { params ->
        val tab = params[0] as LibraryTab
        val query = params[1] as String
        val sort = params[2] as SortOption
        val asc = params[3] as Boolean
        val folder = params[4] as String?
        val mediaDetails = params[5] as MediaItemModel?
        val mediaPlaylist = params[6] as MediaItemModel?
        val refreshing = params[7] as Boolean
        val rawVideos = params[8] as List<MediaItemModel>
        val rawAudios = params[9] as List<MediaItemModel>
        val rawFavs = params[10] as List<MediaItemModel>
        val rawHistory = params[11] as List<PlaybackHistoryEntity>

        fun sortAndFilter(list: List<MediaItemModel>): List<MediaItemModel> {
            val filtered = if (query.isBlank()) list else {
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
            }
            return when (sort) {
                SortOption.TITLE -> if (asc) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
                SortOption.DATE -> if (asc) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
                SortOption.DURATION -> if (asc) filtered.sortedBy { it.durationMs } else filtered.sortedByDescending { it.durationMs }
                SortOption.SIZE -> if (asc) filtered.sortedBy { it.sizeBytes } else filtered.sortedByDescending { it.sizeBytes }
            }
        }

        val sortedVideos = sortAndFilter(rawVideos)
        val sortedAudios = sortAndFilter(rawAudios)
        val sortedFavs = sortAndFilter(rawFavs)

        val folderSource = if (tab == LibraryTab.VIDEOS) sortedVideos else sortedAudios
        val foldersMap = folderSource.groupBy { it.folderName }

        LibraryUiState(
            currentTab = tab,
            searchQuery = query,
            sortOption = sort,
            isSortAscending = asc,
            videos = sortedVideos,
            audios = sortedAudios,
            favorites = sortedFavs,
            history = rawHistory,
            folders = foldersMap,
            selectedFolder = folder,
            selectedMediaForDetails = mediaDetails,
            selectedMediaForPlaylist = mediaPlaylist,
            isRefreshing = refreshing
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LibraryUiState()
    )

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
        _selectedFolder.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        if (_sortOption.value == sort) {
            _isSortAscending.value = !_isSortAscending.value
        } else {
            _sortOption.value = sort
            _isSortAscending.value = false
        }
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun showMediaDetails(media: MediaItemModel?) {
        _selectedMediaForDetails.value = media
    }

    fun showAddToPlaylistDialog(media: MediaItemModel?) {
        _selectedMediaForPlaylist.value = media
    }

    fun addMediaToPlaylist(playlistId: Long, mediaId: String) {
        viewModelScope.launch {
            repository.addMediaToPlaylist(playlistId, mediaId)
            _selectedMediaForPlaylist.value = null
        }
    }

    fun createPlaylistAndAddMedia(name: String, mediaId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addMediaToPlaylist(playlistId, mediaId)
            _selectedMediaForPlaylist.value = null
        }
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshMedia()
            _isRefreshing.value = false
        }
    }

    fun playMedia(media: MediaItemModel, playlist: List<MediaItemModel>) {
        val index = playlist.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
        playerController.playMediaList(playlist, index)
    }

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    class Factory(
        private val repository: MediaRepository,
        private val playerController: PlayerController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, playerController) as T
        }
    }
}
