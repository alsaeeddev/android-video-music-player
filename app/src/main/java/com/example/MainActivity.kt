package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.navigation.Screen
import com.example.data.mediastore.ExternalMediaResolver
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import com.example.feature.library.LibraryScreen
import com.example.feature.library.LibraryViewModel
import com.example.feature.nowplaying.NowPlayingScreen
import com.example.feature.playlists.PlaylistDetailScreen
import com.example.feature.playlists.PlaylistsScreen
import com.example.feature.playlists.PlaylistsViewModel
import com.example.feature.player.VideoPlayerScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.ui.theme.AsPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // "Open with As Player" se video aaye to har request par barhta hai; AppNavigation isay dekh kar
    // video player screen kholti hai.
    private var videoLaunchRequest by mutableIntStateOf(0)

    // Activity recreate hone par bhi "Open with" ka queue resolve hota rahe
    private val openWithScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AsPlayerApplication
        val container = app.appContainer

        // Handle opening media file via Intent from external file manager or app
        // (savedInstanceState != null: activity recreate hui hai, intent dobara handle nahi karna)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent, container)
        }

        setContent {
            val userPrefs by container.userPreferencesDataStore.userPreferencesFlow.collectAsState(
                initial = com.example.data.local.UserPreferences()
            )

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (userPrefs.themeMode) {
                0 -> isSystemDark
                1 -> false
                2 -> true
                3 -> true
                else -> true
            }
            val isAmoled = userPrefs.themeMode == 3

            AsPlayerTheme(
                darkTheme = isDarkTheme,
                isAmoled = isAmoled
            ) {
                // Request media and notification permissions
                RequestMediaPermissions(onPermissionsGranted = {
                    lifecycleScope.launch {
                        container.mediaRepository.refreshMedia()
                    }
                })

                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    val currentMedia = container.playerController.currentMedia.value
                    val prefs = container.playerController.userPreferences.value
                    if (currentMedia != null) {
                        if (currentMedia.mediaType == MediaType.VIDEO && !prefs.backgroundPlayVideo) {
                            container.playerController.exoPlayer.pause()
                        } else if (currentMedia.mediaType == MediaType.AUDIO && !prefs.backgroundPlay) {
                            container.playerController.exoPlayer.pause()
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        container = container,
                        videoLaunchRequest = videoLaunchRequest
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as AsPlayerApplication
        handleIncomingIntent(intent, app.appContainer)
    }

    /**
     * Khuli hui file ke folder ki baaqi media ka queue bana kar chalata hai (Next / Previous ke liye).
     * Folder resolve na ho sake (permission nahi, ya file library mein nahi) to sirf wohi file chalti hai.
     */
    private fun playOpenedMedia(
        container: com.example.core.di.AppContainer,
        uri: Uri,
        media: MediaItemModel
    ) {
        val appContext = applicationContext
        openWithScope.launch {
            val resolved = ExternalMediaResolver(appContext).resolve(
                repository = container.mediaRepository,
                uri = uri,
                isVideo = media.mediaType == MediaType.VIDEO
            )
            if (resolved != null) {
                container.playerController.playMediaList(resolved.queue, resolved.index)
            } else {
                container.playerController.playMediaItem(media)
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?, container: com.example.core.di.AppContainer) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            val mimeType = intent.type ?: uri?.let { contentResolver.getType(it) } ?: ""
            if (uri != null) {
                val isVideo = mimeType.startsWith("video") || uri.toString().contains(".mp4") || uri.toString().contains(".mkv")
                val media = MediaItemModel(
                    id = uri.toString(),
                    title = uri.lastPathSegment ?: "External Media",
                    uriString = uri.toString(),
                    mimeType = mimeType,
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
                )
                playOpenedMedia(container, uri, media)
                if (isVideo) {
                    videoLaunchRequest++
                }
            }
        }
    }
}


@Composable
fun RequestMediaPermissions(onPermissionsGranted: () -> Unit) {
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

    val context = LocalContext.current

    fun hasMediaAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasMediaAccess()) onPermissionsGranted()
    }

    LaunchedEffect(Unit) {
        if (hasMediaAccess()) onPermissionsGranted()
        else launcher.launch(permissionsToRequest)
    }
}


@Composable
fun AppNavigation(
    container: com.example.core.di.AppContainer,
    videoLaunchRequest: Int = 0
) {
    val navController = rememberNavController()

    // Video player ki taraf/se jate hue koi fade animation nahi (white flash / blink se bachne ke liye).
    // Baaqi screens ke liye default fade (700ms) wahi rehta hai.
    val isVideo: (NavBackStackEntry) -> Boolean = { it.destination.route == Screen.VideoPlayer.route }

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        enterTransition = {
            if (isVideo(targetState)) EnterTransition.None else fadeIn(animationSpec = tween(700))
        },
        exitTransition = {
            if (isVideo(targetState)) ExitTransition.None else fadeOut(animationSpec = tween(700))
        },
        popEnterTransition = {
            if (isVideo(initialState)) EnterTransition.None else fadeIn(animationSpec = tween(700))
        },
        popExitTransition = {
            if (isVideo(initialState)) ExitTransition.None else fadeOut(animationSpec = tween(700))
        }
    ) {
        composable(Screen.Library.route) {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(
                    repository = container.mediaRepository,
                    playerController = container.playerController
                )
            )

            LibraryScreen(
                viewModel = libraryViewModel,
                playerController = container.playerController,
                onNavigateToVideoPlayer = { navController.navigate(Screen.VideoPlayer.route) },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.VideoPlayer.route) {
            VideoPlayerScreen(
                playerController = container.playerController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                playerController = container.playerController,
                audioEffectsManager = container.audioEffectsManager,
                sleepTimerManager = container.sleepTimerManager,
                mediaRepository = container.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Playlists.route) {
            val playlistsViewModel: PlaylistsViewModel = viewModel(
                factory = PlaylistsViewModel.Factory(
                    repository = container.mediaRepository,
                    playerController = container.playerController
                )
            )

            PlaylistsScreen(
                viewModel = playlistsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPlaylistClick = { id, name ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("playlistName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"

            PlaylistDetailScreen(
                playlistId = playlistId,
                playlistName = playlistName,
                mediaRepository = container.mediaRepository,
                playerController = container.playerController,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideoPlayer = { navController.navigate(Screen.VideoPlayer.route) },
                onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    userPreferencesDataStore = container.userPreferencesDataStore,
                    repository = container.mediaRepository
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // "Open with" se video aayi: video player screen kholo (agar pehle se khuli na ho)
    LaunchedEffect(videoLaunchRequest) {
        if (videoLaunchRequest > 0 &&
            navController.currentDestination?.route != Screen.VideoPlayer.route
        ) {
            navController.navigate(Screen.VideoPlayer.route)
        }
    }
}
