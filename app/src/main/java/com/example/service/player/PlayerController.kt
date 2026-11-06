package com.example.service.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesDataStore
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.service.PlaybackService
import com.example.service.audioeffect.AudioEffectsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val mediaRepository: MediaRepository,
    private val audioEffectsManager: AudioEffectsManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val _isVideoPlayerActive = MutableStateFlow(false)
    val isVideoPlayerActive: StateFlow<Boolean> = _isVideoPlayerActive.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    init {
        coroutineScope.launch {
            userPreferencesDataStore.userPreferencesFlow.collect { prefs ->
                _userPreferences.value = prefs
                applyEndOfItemBehavior()
                if (!prefs.backgroundPlayVideo && _currentMedia.value?.mediaType == MediaType.VIDEO) {
                    if (!_isVideoPlayerActive.value && exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                }
                // Background video playback off ho to video ki notification hata dein;
                // on ho aur video chal rahi ho to notification dikha dein.
                if (_currentMedia.value?.mediaType == MediaType.VIDEO) {
                    if (!prefs.backgroundPlayVideo) {
                        PlaybackService.hideNotification(context)
                    } else if (exoPlayer.isPlaying) {
                        PlaybackService.start(context)
                    }
                }
            }
        }
    }

    private var exoPlayerCreated = false

    // Auto-play off: item ke end par pause hua. Dobara play dabane par wohi item shuru se chalna chahiye.
    private var pausedAtItemEnd = false
        set(value) {
            field = value
            _isAtItemEnd.value = value
        }
    private var pausedItemIndex = 0

    // Item khatam ho kar player ruka hua hai (auto-play off, ya aakhri item): UI play button dikhata hai
    private val _isAtItemEnd = MutableStateFlow(false)
    val isAtItemEnd: StateFlow<Boolean> = _isAtItemEnd.asStateFlow()

    /**
     * "Auto-play next" off ho to ExoPlayer har item ke end par pause ho jaye (khud next par na jaye).
     * Repeat-one mein item ko dobara chalna hi hai, isliye wahan pause nahi.
     */
    private fun shouldPauseAtEndOfItem(): Boolean =
        !_userPreferences.value.autoPlayNext && _repeatMode.value != Player.REPEAT_MODE_ONE

    private fun applyEndOfItemBehavior() {
        if (exoPlayerCreated) {
            exoPlayer.pauseAtEndOfMediaItems = shouldPauseAtEndOfItem()
        }
    }

    /**
     * Video chal rahi ho aur "Background video playback" off ho to koi media notification nahi dikhani.
     * Audio ke liye hamesha allowed hai.
     */
    fun isNotificationAllowed(): Boolean {
        val media = _currentMedia.value
        return !(media?.mediaType == MediaType.VIDEO && !_userPreferences.value.backgroundPlayVideo)
    }

    private fun startPlaybackService() {
        if (isNotificationAllowed()) {
            PlaybackService.start(context)
        } else {
            PlaybackService.hideNotification(context)
        }
    }

    fun setVideoPlayerActive(active: Boolean) {
        _isVideoPlayerActive.value = active
        if (!active && _currentMedia.value?.mediaType == MediaType.VIDEO && !_userPreferences.value.backgroundPlayVideo) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(playerListener)
                exoPlayerCreated = true
                pauseAtEndOfMediaItems = shouldPauseAtEndOfItem()
            }
    }

    private val _currentMedia = MutableStateFlow<MediaItemModel?>(null)
    val currentMedia: StateFlow<MediaItemModel?> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItemModel>>(emptyList())
    val queue: StateFlow<List<MediaItemModel>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // A-B Repeat State
    private val _pointA = MutableStateFlow<Long?>(null)
    val pointA: StateFlow<Long?> = _pointA.asStateFlow()

    private val _pointB = MutableStateFlow<Long?>(null)
    val pointB: StateFlow<Long?> = _pointB.asStateFlow()

    // Available audio and subtitle tracks
    private val _currentTracks = MutableStateFlow<Tracks?>(null)
    val currentTracks: StateFlow<Tracks?> = _currentTracks.asStateFlow()

    private var progressTrackerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                pausedAtItemEnd = true
                pausedItemIndex = exoPlayer.currentMediaItemIndex
            } else if (playWhenReady && pausedAtItemEnd) {
                pausedAtItemEnd = false
                // ExoPlayer agle item par ja raha hota hai; usi item ko shuru se chalayein
                exoPlayer.seekTo(pausedItemIndex, 0L)
                _currentPosition.value = 0L
            }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                if (_currentMedia.value?.mediaType == MediaType.VIDEO && !_userPreferences.value.backgroundPlayVideo && !_isVideoPlayerActive.value) {
                    exoPlayer.pause()
                    return
                }
                startPlaybackService()
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            _isAtItemEnd.value = pausedAtItemEnd || state == Player.STATE_ENDED
            _duration.value = exoPlayer.duration.coerceAtLeast(0L)
            if (state == Player.STATE_READY) {
                audioEffectsManager.attachAudioSession(exoPlayer.audioSessionId)
            }
            if (state == Player.STATE_ENDED) {
                coroutineScope.launch {
                    val media = _currentMedia.value
                    if (media != null) {
                        mediaRepository.saveResumePosition(media.id, media.uriString, 0L, media.durationMs)
                    }
                    if (_userPreferences.value.autoPlayNext) {
                        playNext()
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pausedAtItemEnd = false
            val idx = exoPlayer.currentMediaItemIndex
            _currentQueueIndex.value = idx
            if (idx in _queue.value.indices) {
                _currentMedia.value = _queue.value[idx]
            }
            _duration.value = exoPlayer.duration.coerceAtLeast(0L)
        }

        override fun onTracksChanged(tracks: Tracks) {
            _currentTracks.value = tracks
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = coroutineScope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                _currentPosition.value = pos
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _duration.value = dur

                // A-B repeat logic
                val pB = _pointB.value
                val pA = _pointA.value
                if (pA != null && pB != null && pB > pA && pos >= pB) {
                    exoPlayer.seekTo(pA)
                }

                // Periodically save history/resume position every 5s
                if (pos % 5000 < 500) {
                    val media = _currentMedia.value
                    if (media != null) {
                        mediaRepository.recordHistory(media, pos)
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
    }

    // Video ke liye play() tab hota hai jab pehla frame render ho jaye, taake audio video se pehle start na ho.
    private var pendingVideoStart = false
    private var pendingVideoStartJob: Job? = null

    // UI isay "playing" dikhata hai taake screen khulte hi pause icon ka flash na aaye
    private val _isVideoStartPending = MutableStateFlow(false)
    val isVideoStartPending: StateFlow<Boolean> = _isVideoStartPending.asStateFlow()

    private fun cancelPendingVideoStart() {
        pendingVideoStart = false
        _isVideoStartPending.value = false
        pendingVideoStartJob?.cancel()
        pendingVideoStartJob = null
    }

    /** VideoPlayerScreen se call hota hai jab video ka pehla frame screen par aa jaye. */
    fun onVideoFrameReady() {
        if (!pendingVideoStart) return
        cancelPendingVideoStart()
        startPlaybackService()
        exoPlayer.play()
    }

    fun playMediaList(
        mediaList: List<MediaItemModel>,
        startIndex: Int = 0,
        deferVideoStart: Boolean = true
    ) {
        if (mediaList.isEmpty()) return
        cancelPendingVideoStart()
        pausedAtItemEnd = false
        _queue.value = mediaList
        _currentQueueIndex.value = startIndex
        val current = mediaList.getOrNull(startIndex)
        _currentMedia.value = current

        _isVideoPlayerActive.value = (current?.mediaType == MediaType.VIDEO)

        val exoItems = mediaList.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(item.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .build()
                )
                .build()
        }

        coroutineScope.launch {
            // Cold start (jaise file manager se "Open with") par prefs abhi load na hui hon to
            // pehle unhein load kar lein, taake background-video / notification rule sahi lage.
            runCatching { userPreferencesDataStore.userPreferencesFlow.first() }
                .getOrNull()?.let { _userPreferences.value = it }
            applyEndOfItemBehavior()

            val startPos = if (_userPreferences.value.rememberResumePosition && current != null) {
                val resumePos = mediaRepository.getResumePosition(current.id)
                if (resumePos > 3000L && (current.durationMs == 0L || resumePos < (current.durationMs - 5000L))) {
                    resumePos
                } else {
                    C.TIME_UNSET
                }
            } else {
                C.TIME_UNSET
            }

            val deferStart = deferVideoStart && current?.mediaType == MediaType.VIDEO
            if (deferStart) {
                // Pehle se kuch chal raha ho to rok dein; naya video frame ready hone par start hoga
                exoPlayer.playWhenReady = false
            }
            exoPlayer.setMediaItems(exoItems, startIndex, startPos)
            // Naye media ki position/duration foran set karo. Warna seek bar purane media ki stale
            // position (ya duration 0) dikhata hai aur play shuru hone par exact resume position par
            // "kood" kar aata hai.
            _currentPosition.value = if (startPos != C.TIME_UNSET) startPos else 0L
            _duration.value = current?.durationMs?.coerceAtLeast(0L) ?: 0L
            exoPlayer.prepare()
            if (deferStart) {
                pendingVideoStart = true
                _isVideoStartPending.value = true
                // Safety: agar frame event na aaye (audio-only file, screen na khuli) to bhi play ho jaye
                pendingVideoStartJob = coroutineScope.launch {
                    delay(1500)
                    onVideoFrameReady()
                }
            } else {
                exoPlayer.play()
                startPlaybackService()
            }
        }
    }

    fun playMediaItem(media: MediaItemModel) {
        playMediaList(listOf(media), 0)
    }

    fun togglePlayPause() {
        if (pendingVideoStart) {
            // UI "playing" dikha raha tha, to tap ka matlab pause hai
            cancelPendingVideoStart()
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_currentMedia.value?.mediaType == MediaType.VIDEO && !_userPreferences.value.backgroundPlayVideo && !_isVideoPlayerActive.value) {
                return
            }
            startPlaybackService()
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                // Aakhri item khatam ho chuka: usi ko dobara shuru se chalayein
                exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, 0L)
            }
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        pausedAtItemEnd = false
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward(seconds: Int = 10) {
        val target = (exoPlayer.currentPosition + seconds * 1000L).coerceAtMost(exoPlayer.duration)
        seekTo(target)
    }

    fun seekBack(seconds: Int = 10) {
        val target = (exoPlayer.currentPosition - seconds * 1000L).coerceAtLeast(0L)
        seekTo(target)
    }

    fun playNext() {
        pausedAtItemEnd = false
        val curIndex = _currentQueueIndex.value
        val q = _queue.value
        if (curIndex + 1 < q.size) {
            exoPlayer.seekToDefaultPosition(curIndex + 1)
            exoPlayer.play()
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && q.isNotEmpty()) {
            exoPlayer.seekToDefaultPosition(0)
            exoPlayer.play()
        }
    }

    fun playPrevious() {
        pausedAtItemEnd = false
        val curIndex = _currentQueueIndex.value
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
        } else if (curIndex > 0) {
            exoPlayer.seekToDefaultPosition(curIndex - 1)
            exoPlayer.play()
        }
    }

    fun setSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playbackSpeed.value = speed
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = next
        _repeatMode.value = next
        applyEndOfItemBehavior()
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        exoPlayer.shuffleModeEnabled = next
        _isShuffle.value = next
    }

    fun addToQueue(media: MediaItemModel) {
        val currentQ = _queue.value.toMutableList()
        currentQ.add(media)
        _queue.value = currentQ
        exoPlayer.addMediaItem(
            MediaItem.Builder()
                .setMediaId(media.id)
                .setUri(media.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setArtist(media.artist)
                        .build()
                )
                .build()
        )
    }

    fun playNextInQueue(media: MediaItemModel) {
        val insertIndex = (_currentQueueIndex.value + 1).coerceAtMost(_queue.value.size)
        val currentQ = _queue.value.toMutableList()
        currentQ.add(insertIndex, media)
        _queue.value = currentQ
        exoPlayer.addMediaItem(
            insertIndex,
            MediaItem.Builder()
                .setMediaId(media.id)
                .setUri(media.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setArtist(media.artist)
                        .build()
                )
                .build()
        )
    }

    fun removeQueueItem(index: Int) {
        val currentQ = _queue.value.toMutableList()
        if (index in currentQ.indices && index != _currentQueueIndex.value) {
            currentQ.removeAt(index)
            _queue.value = currentQ
            exoPlayer.removeMediaItem(index)
        }
    }

    fun setPointA() {
        _pointA.value = exoPlayer.currentPosition
    }

    fun setPointB() {
        _pointB.value = exoPlayer.currentPosition
    }

    fun clearAB() {
        _pointA.value = null
        _pointB.value = null
    }

    fun selectTrack(trackGroup: androidx.media3.common.Tracks.Group, trackIndex: Int) {
        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(trackGroup.mediaTrackGroup, trackIndex)
            )
            .build()
        exoPlayer.trackSelectionParameters = params
    }

    fun clearSubtitleTrack() {
        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        exoPlayer.trackSelectionParameters = params
    }

    fun stopPlayback() {
        cancelPendingVideoStart()
        pausedAtItemEnd = false
        progressTrackerJob?.cancel()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _currentMedia.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        PlaybackService.stop(context)
    }

    fun release() {
        progressTrackerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}