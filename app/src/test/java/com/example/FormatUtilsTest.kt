package com.example

import com.example.core.util.FormatUtils
import com.example.data.model.MediaItemModel
import com.example.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun formatDuration_shortMinutes_returnsCorrectMmSs() {
        // 65 seconds = 01:05
        val formatted = FormatUtils.formatDuration(65_000L)
        assertEquals("01:05", formatted)
    }

    @Test
    fun formatDuration_hours_returnsCorrectHhMmSs() {
        // 1 hour, 2 minutes, 3 seconds = 01:02:03
        val ms = (3600 + 120 + 3) * 1000L
        val formatted = FormatUtils.formatDuration(ms)
        assertEquals("01:02:03", formatted)
    }

    @Test
    fun formatDuration_zeroOrNegative_returnsZero() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:00", FormatUtils.formatDuration(-500L))
    }

    @Test
    fun formatFileSize_megabytes_returnsFormattedString() {
        val bytes = 15_728_640L // 15 MB
        val formatted = FormatUtils.formatFileSize(bytes)
        assertEquals("15.0 MB", formatted)
    }

    @Test
    fun formatResolution_standardResolutions() {
        assertEquals("4K", FormatUtils.formatResolution(3840, 2160))
        assertEquals("1080p", FormatUtils.formatResolution(1920, 1080))
        assertEquals("720p", FormatUtils.formatResolution(1280, 720))
        assertEquals("SD", FormatUtils.formatResolution(640, 360))
    }

    @Test
    fun mediaItemModel_propertiesAndBadges() {
        val model = MediaItemModel(
            id = "test_1",
            title = "Test Video",
            durationMs = 125000L,
            uriString = "content://media/external/video/media/1",
            width = 1920,
            height = 1080,
            mediaType = MediaType.VIDEO
        )

        assertEquals("02:05", model.formattedDuration)
        assertEquals("1080p", model.resolutionBadge)
    }
}
