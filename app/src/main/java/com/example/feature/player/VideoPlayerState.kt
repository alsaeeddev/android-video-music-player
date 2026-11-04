package com.example.feature.player

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

data class VideoPlayerUiState(
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val showSpeedDialog: Boolean = false,
    val showTracksDialog: Boolean = false,
    val showABRepeatDialog: Boolean = false,
    val brightnessPercent: Int? = null,
    val volumePercent: Int? = null,
    val seekDeltaSeconds: Int? = null,
    val isLongPressSpeedActive: Boolean = false
)
