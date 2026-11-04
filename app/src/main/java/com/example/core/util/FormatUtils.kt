package com.example.core.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for formatting durations, byte sizes, and media resolution.
 */
object FormatUtils {

    /**
     * Formats duration in milliseconds to "mm:ss" or "hh:mm:ss".
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats bytes to readable size string: "KB", "MB", "GB".
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kilobyte = 1024.0
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024

        return when {
            bytes >= gigabyte -> String.format(Locale.getDefault(), "%.1f GB", bytes / gigabyte)
            bytes >= megabyte -> String.format(Locale.getDefault(), "%.1f MB", bytes / megabyte)
            bytes >= kilobyte -> String.format(Locale.getDefault(), "%.1f KB", bytes / kilobyte)
            else -> "$bytes B"
        }
    }

    /**
     * Formats width and height to resolution badge like "4K", "1080p", "720p", "SD".
     */
    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return ""
        val minDim = minOf(width, height)
        val maxDim = maxOf(width, height)
        return when {
            maxDim >= 3800 || minDim >= 2100 -> "4K"
            maxDim >= 2500 || minDim >= 1400 -> "2K"
            maxDim >= 1900 || minDim >= 1000 -> "1080p"
            maxDim >= 1200 || minDim >= 700 -> "720p"
            maxDim >= 800 || minDim >= 450 -> "480p"
            else -> "SD"
        }
    }
}
