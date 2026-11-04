package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.example.AsPlayerApplication
import com.example.MainActivity
import com.example.R
import com.example.service.player.PlayerController

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var playerController: PlayerController? = null
    private var notificationManager: NotificationManager? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateNotification(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isPlaying = playerController?.exoPlayer?.let { player ->
                player.playWhenReady && player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE
            } ?: false
            updateNotification(isPlaying)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val isPlaying = playerController?.exoPlayer?.let { player ->
                player.playWhenReady && player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE
            } ?: false
            updateNotification(isPlaying)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = application as? AsPlayerApplication ?: return
        val controller = app.appContainer.playerController
        playerController = controller
        val player = controller.exoPlayer

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
        mediaSession = session
        addSession(session)

        player.addListener(playerListener)
    }

    /**
     * startForegroundService() ke baad Android 8+ par startForeground() lazmi call hona chahiye,
     * chahe service foran band hi kyun na honi ho. Is liye sab se pehle yeh call hota hai;
     * baad mein asli notification update ya hide ho jati hai.
     */
    private fun promoteToForeground() {
        val title = playerController?.currentMedia?.value?.title ?: "As Player"
        try {
            val openIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_play)
                .setContentTitle(title)
                .setContentIntent(openIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .build()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Last resort: kuch nahi kar sakte
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            promoteToForeground()
        }
        val player = playerController?.exoPlayer
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                playerController?.togglePlayPause()
            }
            ACTION_PLAY -> {
                player?.play()
            }
            ACTION_PAUSE -> {
                player?.pause()
            }
            ACTION_NEXT -> {
                playerController?.playNext()
            }
            ACTION_PREV -> {
                playerController?.playPrevious()
            }
            ACTION_STOP -> {
                player?.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager?.cancel(NOTIFICATION_ID)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Video + "Background video playback" off: notification nahi dikhani
        if (playerController?.isNotificationAllowed() == false) {
            hideNotificationAndStop()
            return START_NOT_STICKY
        }

        val isPlaying = player?.let {
            it.playWhenReady && it.playbackState != Player.STATE_ENDED && it.playbackState != Player.STATE_IDLE
        } ?: false
        updateNotification(isPlaying)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun hideNotificationAndStop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    // Media3 ki default notification bhi isi rule ke tabe rahe
    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (playerController?.isNotificationAllowed() == false) {
            hideNotificationAndStop()
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerController?.exoPlayer
        val isPlaying = player?.let {
            it.playWhenReady && it.playbackState != Player.STATE_ENDED && it.playbackState != Player.STATE_IDLE
        } ?: false

        if (!isPlaying) {
            super.onTaskRemoved(rootIntent)
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager?.cancel(NOTIFICATION_ID)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows playback notification and media controls in system bar"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateNotification(isPlaying: Boolean) {
        val session = mediaSession ?: return
        val controller = playerController ?: return
        if (!controller.isNotificationAllowed()) {
            hideNotificationAndStop()
            return
        }
        val currentMedia = controller.currentMedia.value
        val title = currentMedia?.title ?: controller.exoPlayer.currentMediaItem?.mediaMetadata?.title?.toString() ?: "As Player"
        val artist = currentMedia?.artist ?: controller.exoPlayer.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Music Playing"

        val openActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action PendingIntents
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(session)
            .setShowActionsInCompactView(0, 1, 2)

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(resources, R.drawable.ic_as_player_logo)
        } catch (e: Exception) {
            null
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openActivityPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_notification_prev, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(R.drawable.ic_notification_next, "Next", nextPendingIntent)
            .addAction(R.drawable.ic_notification_close, "Close", stopPendingIntent)

        if (largeIconBitmap != null) {
            notificationBuilder.setLargeIcon(largeIconBitmap)
        }

        val notification = notificationBuilder.build()

        try {
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        playerController?.exoPlayer?.removeListener(playerListener)
        mediaSession?.run {
            removeSession(this)
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "as_player_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.alsaeeddev.asplayer.PLAY"
        const val ACTION_PAUSE = "com.alsaeeddev.asplayer.PAUSE"
        const val ACTION_PLAY_PAUSE = "com.alsaeeddev.asplayer.PLAY_PAUSE"
        const val ACTION_PREV = "com.alsaeeddev.asplayer.PREV"
        const val ACTION_NEXT = "com.alsaeeddev.asplayer.NEXT"
        const val ACTION_STOP = "com.alsaeeddev.asplayer.STOP"
        const val ACTION_START = "com.alsaeeddev.asplayer.START"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Handle background service start restrictions safely
            }
        }

        /** Chalti hui service (agar hai) band karta hai taake notification hat jaye; playback ko pause nahi karta. */
        fun hideNotification(context: Context) {
            try {
                context.stopService(Intent(context, PlaybackService::class.java))
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                // ignore
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
