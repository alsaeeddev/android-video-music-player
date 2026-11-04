package com.example.core.di

import android.content.Context
import com.example.data.local.AsPlayerDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.data.mediastore.MediaStoreDataSource
import com.example.data.repository.MediaRepository
import com.example.data.repository.MediaRepositoryImpl
import com.example.service.SleepTimerManager
import com.example.service.audioeffect.AudioEffectsManager
import com.example.service.player.PlayerController

interface AppContainer {
    val database: AsPlayerDatabase
    val userPreferencesDataStore: UserPreferencesDataStore
    val mediaStoreDataSource: MediaStoreDataSource
    val mediaRepository: MediaRepository
    val audioEffectsManager: AudioEffectsManager
    val playerController: PlayerController
    val sleepTimerManager: SleepTimerManager
}

class AppContainerImpl(private val context: Context) : AppContainer {
    override val database: AsPlayerDatabase by lazy {
        AsPlayerDatabase.getDatabase(context)
    }

    override val userPreferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val mediaStoreDataSource: MediaStoreDataSource by lazy {
        MediaStoreDataSource(context)
    }

    override val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            mediaStoreDataSource = mediaStoreDataSource,
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao(),
            resumePositionDao = database.resumePositionDao(),
            favoriteDao = database.favoriteDao(),
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    override val audioEffectsManager: AudioEffectsManager by lazy {
        AudioEffectsManager()
    }

    override val playerController: PlayerController by lazy {
        PlayerController(context, userPreferencesDataStore, mediaRepository, audioEffectsManager)
    }

    override val sleepTimerManager: SleepTimerManager by lazy {
        SleepTimerManager(playerController)
    }
}
