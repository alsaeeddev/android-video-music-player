package com.example.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.util.FormatUtils
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesDataStore
import com.example.data.model.MediaType
import com.example.service.player.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerController: PlayerController,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val currentMedia by playerController.currentMedia.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val position by playerController.currentPosition.collectAsState()
    val duration by playerController.duration.collectAsState()
    val speed by playerController.playbackSpeed.collectAsState()
    val tracks by playerController.currentTracks.collectAsState()
    val startPending by playerController.isVideoStartPending.collectAsState()
    val atItemEnd by playerController.isAtItemEnd.collectAsState()
    val pointA by playerController.pointA.collectAsState()
    val pointB by playerController.pointB.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var isLandscape by remember { mutableStateOf(false) }

    // Dialog toggles
    var showTrackDialog by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    var showABDialog by remember { mutableStateOf(false) }

    // Gesture feedback states
    var brightnessFeedback by remember { mutableStateOf<Int?>(null) }
    var volumeFeedback by remember { mutableStateOf<Int?>(null) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }

    // User dragging slider state
    var isSeeking by remember { mutableStateOf(false) }
    var seekSliderPos by remember { mutableFloatStateOf(0f) }

    // Back press par jab player pause hota hai to UI (play/pause icon) ko freeze rakhte hain,
    // taake exit ke dauran "play" icon ka flicker nazar na aaye.
    var leavingPlaying by remember { mutableStateOf<Boolean?>(null) }

    // Play/Pause icon ExoPlayer ke isPlaying par nahi, "play ka intent" (playWhenReady) par chalta hai.
    // isPlaying buffering / seek (khaas kar resume position par) ke dauran chand ms ke liye false ho jata
    // hai, jis se icon Pause -> Play -> Pause blink karta tha.
    var playWhenReadyState by remember { mutableStateOf(playerController.exoPlayer.playWhenReady) }
    var playbackStateValue by remember { mutableIntStateOf(playerController.exoPlayer.playbackState) }
    val intendedPlaying = startPending || isPlaying || (
            playWhenReadyState &&
                    playbackStateValue != Player.STATE_ENDED &&
                    playbackStateValue != Player.STATE_IDLE
            )
    val uiIsPlaying = leavingPlaying ?: intendedPlaying
    val navigateBackOnce: () -> Unit = {
        if (leavingPlaying == null) {
            leavingPlaying = isPlaying
            onNavigateBack()
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // PiP state: jab PiP mode mein ho (ya PiP mein jane ki request ho chuki ho) to saara controls UI
    // hide rehta hai, warna full-size controls chhoti mini window mein scale ho kar bohat bade nazar aate hain.
    var isInPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    var pipRequested by remember { mutableStateOf(false) }
    val hideControlsUi = isInPipMode || pipRequested

    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
            if (!info.isInPictureInPictureMode) {
                pipRequested = false
                controlsVisible = false
            }
        }
        componentActivity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { componentActivity?.removeOnPictureInPictureModeChangedListener(listener) }
    }

    val prefs by remember {
        UserPreferencesDataStore(context).userPreferencesFlow
    }.collectAsState(initial = UserPreferences())

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!prefs.backgroundPlayVideo && currentMedia?.mediaType == MediaType.VIDEO) {
            playerController.exoPlayer.pause()
        }
    }

    // playWhenReady / playback state track karo (icon ke liye)
    DisposableEffect(Unit) {
        val exo = playerController.exoPlayer
        playWhenReadyState = exo.playWhenReady
        playbackStateValue = exo.playbackState
        val stateListener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playWhenReadyState = playWhenReady
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackStateValue = playbackState
            }
        }
        exo.addListener(stateListener)
        onDispose { exo.removeListener(stateListener) }
    }

    // Keep screen on while video player is open
    DisposableEffect(Unit) {
        playerController.setVideoPlayerActive(true)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Pehla frame render hote hi playback start (audio aur video sync mein)
        val firstFrameListener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                playerController.onVideoFrameReady()
            }
        }
        playerController.exoPlayer.addListener(firstFrameListener)
        onDispose {
            playerController.exoPlayer.removeListener(firstFrameListener)
            playerController.setVideoPlayerActive(false)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Video khatam ho kar ruk jaye (auto-play off) to controls aur play button dikha dein
    LaunchedEffect(atItemEnd) {
        if (atItemEnd) controlsVisible = true
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying, isSeeking) {
        if (controlsVisible && isPlaying && !isSeeking) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Dismiss gesture overlays after inactivity
    LaunchedEffect(brightnessFeedback, volumeFeedback, seekFeedback) {
        if (brightnessFeedback != null || volumeFeedback != null || seekFeedback != null) {
            delay(1200)
            brightnessFeedback = null
            volumeFeedback = null
            seekFeedback = null
        }
    }

    BackHandler {
        if (isLocked) {
            isLocked = false
        } else {
            navigateBackOnce()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // Pehle frame se pehle black dikhe (white flash na aaye)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    player = playerController.exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playerController.exoPlayer
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Input Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible }
                        )
                    } else {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var startX = 0f

                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.4f) {
                                    playerController.seekBack(10)
                                    seekFeedback = "-10s"
                                } else if (offset.x > screenWidth * 0.6f) {
                                    playerController.seekForward(10)
                                    seekFeedback = "+10s"
                                } else {
                                    playerController.togglePlayPause()
                                }
                            },
                            onLongPress = {
                                playerController.setSpeed(2.0f)
                                seekFeedback = "2.0x Fast Forward"
                            }
                        )
                    }
                }
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // start drag
                            },
                            onDragEnd = {
                                // drag end
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val screenWidth = size.width
                                val isLeftHalf = change.position.x < (screenWidth / 2f)

                                if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                                    // Vertical swipe: Brightness on left, Volume on right
                                    val delta = -dragAmount.y / 600f
                                    if (isLeftHalf) {
                                        activity?.let { act ->
                                            val lp = act.window.attributes
                                            val current = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                            val newBrightness = (current + delta).coerceIn(0.01f, 1.0f)
                                            lp.screenBrightness = newBrightness
                                            act.window.attributes = lp
                                            brightnessFeedback = (newBrightness * 100).toInt()
                                        }
                                    } else {
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val changeStep = if (delta > 0) 1 else -1
                                        val newVol = (curVol + changeStep).coerceIn(0, maxVol)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        volumeFeedback = ((newVol.toFloat() / maxVol) * 100).toInt()
                                    }
                                } else {
                                    // Horizontal swipe: seek
                                    val seekDeltaSec = (dragAmount.x / 10f).toInt()
                                    if (seekDeltaSec != 0) {
                                        playerController.seekForward(seekDeltaSec)
                                        seekFeedback = if (seekDeltaSec > 0) "+${seekDeltaSec}s" else "${seekDeltaSec}s"
                                    }
                                }
                            }
                        )
                    }
                }
        )

        // Gesture Feedback Overlays (Center)
        brightnessFeedback?.takeIf { !hideControlsUi }?.let { pct ->
            GestureIndicatorCard(
                icon = Icons.Default.BrightnessMedium,
                label = "Brightness",
                percent = pct,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        volumeFeedback?.takeIf { !hideControlsUi }?.let { pct ->
            GestureIndicatorCard(
                icon = Icons.Default.VolumeUp,
                label = "Volume",
                percent = pct,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        seekFeedback?.takeIf { !hideControlsUi }?.let { label ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // Lock button (always visible when locked or when controls are visible)
        if ((controlsVisible || isLocked) && !hideControlsUi) {
            IconButton(
                onClick = { isLocked = !isLocked },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("lock_controls_button")
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock controls",
                    tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        // Full Controls Overlay (Top Bar, Center Play/Pause, Bottom Seek & Bar)
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !hideControlsUi,
            enter = fadeIn(),
            // PiP mein jate waqt fade-out animation nahi, foran hide (warna kuch ms bade controls nazar aate hain)
            exit = if (hideControlsUi) ExitTransition.None else fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateBackOnce) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = currentMedia?.title ?: "Video Player",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Track / Subtitle selector
                    IconButton(onClick = { showTrackDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Tracks",
                            tint = Color.White
                        )
                    }

                    // Aspect ratio selector
                    Box {
                        IconButton(onClick = { showAspectRatioMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showAspectRatioMenu,
                            onDismissRequest = { showAspectRatioMenu = false }
                        ) {
                            AspectRatioMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        aspectRatioMode = mode
                                        showAspectRatioMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Speed menu
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s}x") },
                                    onClick = {
                                        playerController.setSpeed(s)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // PiP Action
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(
                            onClick = {
                                // Pehle UI foran hide karo, phir PiP mein jao
                                pipRequested = true
                                controlsVisible = false
                                showSpeedMenu = false
                                showAspectRatioMenu = false
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                val entered = try {
                                    activity?.enterPictureInPictureMode(params) == true
                                } catch (e: IllegalStateException) {
                                    false
                                }
                                if (!entered) pipRequested = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "PiP",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playerController.seekBack(10) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Seek -10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerController.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (uiIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiIsPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerController.seekForward(10) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Seek +10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Bar Controls (Seek slider, duration, A-B repeat, orientation)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Slider Row
                    val currentProgress = if (isSeeking) seekSliderPos else {
                        if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    }

                    Slider(
                        value = currentProgress,
                        onValueChange = {
                            isSeeking = true
                            seekSliderPos = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            val targetMs = (seekSliderPos * duration).toLong()
                            playerController.seekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${FormatUtils.formatDuration(if (isSeeking) (seekSliderPos * duration).toLong() else position)} / ${FormatUtils.formatDuration(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // A-B Repeat button
                            IconButton(
                                onClick = { showABDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "A-B Repeat",
                                    tint = if (pointA != null) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Rotate / Fullscreen orientation toggle
                            IconButton(
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtitles and Tracks Dialog
        if (showTrackDialog) {
            TrackSelectionDialog(
                playerController = playerController,
                tracks = tracks,
                onDismiss = { showTrackDialog = false }
            )
        }

        // A-B Repeat Dialog
        if (showABDialog) {
            AlertDialog(
                onDismissRequest = { showABDialog = false },
                title = { Text("A-B Repeat Interval") },
                text = {
                    Column {
                        Text(
                            text = "Loop a specific section of the video between points A and B.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { playerController.setPointA() }) {
                                Text("Set A (${pointA?.let { FormatUtils.formatDuration(it) } ?: "None"})")
                            }
                            TextButton(onClick = { playerController.setPointB() }) {
                                Text("Set B (${pointB?.let { FormatUtils.formatDuration(it) } ?: "None"})")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showABDialog = false }) {
                        Text("Done")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            playerController.clearAB()
                            showABDialog = false
                        }
                    ) {
                        Text("Clear Loop")
                    }
                }
            )
        }
    }
}

@Composable
private fun GestureIndicatorCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$label: $percent%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}