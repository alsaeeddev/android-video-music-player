package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "as_player_preferences")

data class UserPreferences(
    val themeMode: Int = 0, // 0: SYSTEM, 1: LIGHT, 2: DARK, 3: AMOLED
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val backgroundPlay: Boolean = true,
    val backgroundPlayVideo: Boolean = true,
    val rememberResumePosition: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val gesturesEnabled: Boolean = true,
    val showSampleMedia: Boolean = false,
    val equalizerPresetIndex: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val BACKGROUND_PLAY_VIDEO = booleanPreferencesKey("background_play_video")
        val REMEMBER_RESUME_POSITION = booleanPreferencesKey("remember_resume_position")
        val DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("double_tap_seek_seconds")
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val SHOW_SAMPLE_MEDIA = booleanPreferencesKey("show_sample_media")
        val EQ_PRESET_INDEX = intPreferencesKey("eq_preset_index")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.userDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[PreferencesKeys.THEME_MODE] ?: 0,
            defaultPlaybackSpeed = prefs[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            autoPlayNext = prefs[PreferencesKeys.AUTO_PLAY_NEXT] ?: true,
            backgroundPlay = prefs[PreferencesKeys.BACKGROUND_PLAY] ?: true,
            backgroundPlayVideo = prefs[PreferencesKeys.BACKGROUND_PLAY_VIDEO] ?: true,
            rememberResumePosition = prefs[PreferencesKeys.REMEMBER_RESUME_POSITION] ?: true,
            doubleTapSeekSeconds = prefs[PreferencesKeys.DOUBLE_TAP_SEEK_SECONDS] ?: 10,
            gesturesEnabled = prefs[PreferencesKeys.GESTURES_ENABLED] ?: true,
            showSampleMedia = prefs[PreferencesKeys.SHOW_SAMPLE_MEDIA] ?: false,
            equalizerPresetIndex = prefs[PreferencesKeys.EQ_PRESET_INDEX] ?: 0,
            bassBoostStrength = prefs[PreferencesKeys.BASS_BOOST] ?: 0,
            virtualizerStrength = prefs[PreferencesKeys.VIRTUALIZER] ?: 0
        )
    }

    suspend fun setThemeMode(mode: Int) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setBackgroundPlay(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.BACKGROUND_PLAY] = enabled
        }
    }

    suspend fun setBackgroundPlayVideo(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.BACKGROUND_PLAY_VIDEO] = enabled
        }
    }

    suspend fun setRememberResumePosition(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.REMEMBER_RESUME_POSITION] = enabled
        }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.GESTURES_ENABLED] = enabled
        }
    }

    suspend fun setShowSampleMedia(show: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_SAMPLE_MEDIA] = show
        }
    }

    suspend fun setEqualizerPreset(presetIndex: Int) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_PRESET_INDEX] = presetIndex
        }
    }

    suspend fun setBassBoost(strength: Int) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.BASS_BOOST] = strength
        }
    }

    suspend fun setVirtualizer(strength: Int) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.VIRTUALIZER] = strength
        }
    }
}
