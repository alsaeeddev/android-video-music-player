package com.example.service

import com.example.service.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerManager(private val playerController: PlayerController) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    fun startTimerMinutes(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        val totalSeconds = minutes * 60
        _remainingSeconds.value = totalSeconds
        _isTimerActive.value = true

        timerJob = scope.launch {
            var left = totalSeconds
            while (isActive && left > 0) {
                delay(1000L)
                left--
                _remainingSeconds.value = left
            }
            if (isActive) {
                playerController.exoPlayer.pause()
                _isTimerActive.value = false
                _remainingSeconds.value = 0
            }
        }
    }

    fun startTimerEndOfTrack() {
        cancelTimer()
        _isTimerActive.value = true
        _remainingSeconds.value = -1 // Sentinal for 'end of track'

        timerJob = scope.launch {
            val startMediaId = playerController.currentMedia.value?.id
            while (isActive) {
                delay(500L)
                val currentId = playerController.currentMedia.value?.id
                if (currentId != startMediaId) {
                    playerController.exoPlayer.pause()
                    _isTimerActive.value = false
                    _remainingSeconds.value = 0
                    break
                }
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerActive.value = false
        _remainingSeconds.value = 0
    }
}
