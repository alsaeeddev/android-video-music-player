package com.example.service.audioeffect

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliBels: Short,
    val maxLevelMilliBels: Short,
    val currentLevelMilliBels: Short
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val presets: List<String> = listOf("Normal", "Rock", "Pop", "Jazz", "Classical", "Bass Boost"),
    val currentPreset: Int = 0,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0 - 1000
    val virtualizerStrength: Int = 0 // 0 - 1000
)

class AudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }

            readCurrentState()
        } catch (e: Exception) {
            // Audio effect initialization failed on this device/session; fallback gracefully
            setupDefaultBands()
        }
    }

    private fun setupDefaultBands() {
        val defaultBands = listOf(
            EqualizerBand(0, 60, -1000, 1000, 0),
            EqualizerBand(1, 230, -1000, 1000, 0),
            EqualizerBand(2, 910, -1000, 1000, 0),
            EqualizerBand(3, 3600, -1000, 1000, 0),
            EqualizerBand(4, 14000, -1000, 1000, 0)
        )
        _state.value = _state.value.copy(bands = defaultBands)
    }

    private fun readCurrentState() {
        val eq = equalizer ?: run {
            setupDefaultBands()
            return
        }

        try {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val bandsList = mutableListOf<EqualizerBand>()

            for (i in 0 until numBands) {
                val bandShort = i.toShort()
                val centerFreq = eq.getCenterFreq(bandShort) / 1000
                val curLevel = eq.getBandLevel(bandShort)
                bandsList.add(
                    EqualizerBand(
                        index = bandShort,
                        centerFreqHz = centerFreq,
                        minLevelMilliBels = minLevel,
                        maxLevelMilliBels = maxLevel,
                        currentLevelMilliBels = curLevel
                    )
                )
            }

            val presetCount = eq.numberOfPresets.toInt()
            val presetNames = mutableListOf<String>()
            for (p in 0 until presetCount) {
                presetNames.add(eq.getPresetName(p.toShort()))
            }
            val presets = if (presetNames.isNotEmpty()) presetNames else _state.value.presets

            _state.value = _state.value.copy(
                isEnabled = eq.enabled,
                bands = bandsList,
                presets = presets,
                currentPreset = eq.currentPreset.toInt().coerceAtLeast(0)
            )
        } catch (e: Exception) {
            setupDefaultBands()
        }
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, level)
            val updatedBands = _state.value.bands.map {
                if (it.index == bandIndex) it.copy(currentLevelMilliBels = level) else it
            }
            _state.value = _state.value.copy(bands = updatedBands)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun applyPreset(presetIndex: Int) {
        try {
            val eq = equalizer
            if (eq != null && presetIndex in 0 until eq.numberOfPresets.toInt()) {
                eq.usePreset(presetIndex.toShort())
                readCurrentState()
            }
            _state.value = _state.value.copy(currentPreset = presetIndex)
        } catch (e: Exception) {
            _state.value = _state.value.copy(currentPreset = presetIndex)
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
            _state.value = _state.value.copy(bassBoostStrength = strength)
        } catch (e: Exception) {
            _state.value = _state.value.copy(bassBoostStrength = strength)
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
            _state.value = _state.value.copy(virtualizerStrength = strength)
        } catch (e: Exception) {
            _state.value = _state.value.copy(virtualizerStrength = strength)
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isEnabled = enabled)
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
