package com.example.data.repository

import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryDao
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemCrossRef
import com.example.data.local.ResumePositionDao
import com.example.data.local.ResumePositionEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.mediastore.MediaStoreDataSource
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.data.model.SampleMediaProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface MediaRepository {
    val videosFlow: Flow<List<MediaItemModel>>
    val audiosFlow: Flow<List<MediaItemModel>>
    val playlistsFlow: Flow<List<PlaylistEntity>>
    val favoritesFlow: Flow<List<MediaItemModel>>
    val historyFlow: Flow<List<PlaybackHistoryEntity>>

    fun getFolders(mediaType: MediaType): Flow<Map<String, List<MediaItemModel>>>
    fun getPlaylistItems(playlistId: Long): Flow<List<MediaItemModel>>
    fun isFavorite(mediaId: String): Flow<Boolean>

    suspend fun refreshMedia()
    suspend fun enableSampleMedia()
    suspend fun getMediaById(id: String): MediaItemModel?
    suspend fun getResumePosition(mediaId: String): Long
    suspend fun saveResumePosition(mediaId: String, uriString: String, positionMs: Long, durationMs: Long)
    suspend fun recordHistory(media: MediaItemModel, positionMs: Long)
    suspend fun toggleFavorite(mediaId: String)
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addMediaToPlaylist(playlistId: Long, mediaId: String)
    suspend fun removeMediaFromPlaylist(playlistId: Long, mediaId: String)
    suspend fun clearHistory()
}

class MediaRepositoryImpl(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    private val resumePositionDao: ResumePositionDao,
    private val favoriteDao: FavoriteDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MediaRepository {

    private val localVideos = MutableStateFlow<List<MediaItemModel>>(emptyList())
    private val localAudios = MutableStateFlow<List<MediaItemModel>>(emptyList())

    init {
        coroutineScope.launch {
            refreshMedia()
        }
    }

    override suspend fun refreshMedia() = withContext(Dispatchers.IO) {
        val fetchedVideos = mediaStoreDataSource.getVideos()
        val fetchedAudios = mediaStoreDataSource.getAudios()
        localVideos.value = fetchedVideos
        localAudios.value = fetchedAudios
    }

    override suspend fun enableSampleMedia() = withContext(Dispatchers.IO) {
        userPreferencesDataStore.setShowSampleMedia(true)
    }

    private val baseVideosFlow: Flow<List<MediaItemModel>> = localVideos

    private val baseAudiosFlow: Flow<List<MediaItemModel>> = localAudios

/*    private val baseVideosFlow: Flow<List<MediaItemModel>> = combine(
        localVideos,
        userPreferencesDataStore.userPreferencesFlow
    ) { deviceVideos, prefs ->
        if (prefs.showSampleMedia) {
            deviceVideos + SampleMediaProvider.sampleVideos
        } else {
            deviceVideos
        }
    }

    private val baseAudiosFlow: Flow<List<MediaItemModel>> = combine(
        localAudios,
        userPreferencesDataStore.userPreferencesFlow
    ) { deviceAudios, prefs ->
        if (prefs.showSampleMedia) {
            deviceAudios + SampleMediaProvider.sampleAudios
        } else {
            deviceAudios
        }
    }*/

    override val videosFlow: Flow<List<MediaItemModel>> = combine(
        baseVideosFlow,
        favoriteDao.getFavorites()
    ) { videos, favEntities ->
        val favIds = favEntities.map { it.mediaId }.toSet()
        videos.map { item ->
            item.copy(isFavorite = favIds.contains(item.id))
        }
    }

    override val audiosFlow: Flow<List<MediaItemModel>> = combine(
        baseAudiosFlow,
        favoriteDao.getFavorites()
    ) { audios, favEntities ->
        val favIds = favEntities.map { it.mediaId }.toSet()
        audios.map { item ->
            item.copy(isFavorite = favIds.contains(item.id))
        }
    }

    override val playlistsFlow: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    override val favoritesFlow: Flow<List<MediaItemModel>> = combine(
        videosFlow,
        audiosFlow,
        favoriteDao.getFavorites()
    ) { videos, audios, favEntities ->
        val favIds = favEntities.map { it.mediaId }.toSet()
        val allMedia = videos + audios
        allMedia.filter { favIds.contains(it.id) }.map { it.copy(isFavorite = true) }
    }

    override val historyFlow: Flow<List<PlaybackHistoryEntity>> = historyDao.getHistory()

    override fun getFolders(mediaType: MediaType): Flow<Map<String, List<MediaItemModel>>> {
        val sourceFlow = if (mediaType == MediaType.VIDEO) videosFlow else audiosFlow
        return sourceFlow.map { list ->
            list.groupBy { it.folderName }
        }
    }

    override fun getPlaylistItems(playlistId: Long): Flow<List<MediaItemModel>> {
        return combine(
            videosFlow,
            audiosFlow,
            playlistDao.getItemsForPlaylist(playlistId)
        ) { videos, audios, crossRefs ->
            val allMediaMap = (videos + audios).associateBy { it.id }
            crossRefs.mapNotNull { allMediaMap[it.mediaId] }
        }
    }

    override fun isFavorite(mediaId: String): Flow<Boolean> = favoriteDao.isFavorite(mediaId)

    override suspend fun getMediaById(id: String): MediaItemModel? {
        val allMedia = (videosFlow.first() + audiosFlow.first()).associateBy { it.id }
        return allMedia[id]
    }

    override suspend fun getResumePosition(mediaId: String): Long {
        return resumePositionDao.getResumePosition(mediaId)?.lastPositionMs ?: 0L
    }

    override suspend fun saveResumePosition(
        mediaId: String,
        uriString: String,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        resumePositionDao.saveResumePosition(
            ResumePositionEntity(
                mediaId = mediaId,
                uriString = uriString,
                lastPositionMs = positionMs,
                durationMs = durationMs
            )
        )
    }

    override suspend fun recordHistory(media: MediaItemModel, positionMs: Long) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(
            PlaybackHistoryEntity(
                mediaId = media.id,
                title = media.title,
                artist = media.artist,
                uriString = media.uriString,
                mediaType = media.mediaType.name,
                lastPositionMs = positionMs,
                durationMs = media.durationMs
            )
        )
        saveResumePosition(media.id, media.uriString, positionMs, media.durationMs)
    }

    override suspend fun toggleFavorite(mediaId: String) = withContext(Dispatchers.IO) {
        val currentFavs = favoriteDao.getFavorites().first()
        val exists = currentFavs.any { it.mediaId == mediaId }
        if (exists) {
            favoriteDao.removeFavorite(mediaId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
        playlistDao.deletePlaylistItems(playlistId)
    }

    override suspend fun addMediaToPlaylist(playlistId: Long, mediaId: String) = withContext(Dispatchers.IO) {
        val existingItems = playlistDao.getItemsForPlaylist(playlistId).first()
        val nextOrder = existingItems.size
        playlistDao.insertPlaylistItem(
            PlaylistItemCrossRef(
                playlistId = playlistId,
                mediaId = mediaId,
                orderIndex = nextOrder
            )
        )
    }

    override suspend fun removeMediaFromPlaylist(playlistId: Long, mediaId: String) = withContext(Dispatchers.IO) {
        playlistDao.removePlaylistItem(playlistId, mediaId)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }
}
