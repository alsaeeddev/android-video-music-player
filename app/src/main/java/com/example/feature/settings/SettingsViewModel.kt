package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesDataStore
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val repository: MediaRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesDataStore.userPreferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences()
    )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            userPreferencesDataStore.setThemeMode(mode)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setAutoPlayNext(enabled)
        }
    }

    fun setBackgroundPlay(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setBackgroundPlay(enabled)
        }
    }

    fun setBackgroundPlayVideo(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setBackgroundPlayVideo(enabled)
        }
    }

    fun setRememberResumePosition(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setRememberResumePosition(enabled)
        }
    }

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setGesturesEnabled(enabled)
        }
    }

    fun setShowSampleMedia(show: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setShowSampleMedia(show)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    class Factory(
        private val userPreferencesDataStore: UserPreferencesDataStore,
        private val repository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userPreferencesDataStore, repository) as T
        }
    }
}
