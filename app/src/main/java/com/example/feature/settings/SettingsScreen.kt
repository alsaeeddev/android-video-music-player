package com.example.feature.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
               // .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Appearance Section
            SectionHeader(title = "Appearance", icon = Icons.Default.Palette)

            SettingItem(
                title = "Theme",
                subtitle = when (prefs.themeMode) {
                    0 -> "Follow System"
                    1 -> "Light Theme"
                    2 -> "Dark Theme"
                    3 -> "AMOLED Pitch Black"
                    else -> "Follow System"
                },
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Playback Section
            SectionHeader(title = "Playback Defaults", icon = Icons.Default.PlayCircle)

            SettingSwitchItem(
                title = "Remember Playback Position",
                subtitle = "Resume videos and tracks from where you left off",
                checked = prefs.rememberResumePosition,
                onCheckedChange = { viewModel.setRememberResumePosition(it) }
            )

            SettingSwitchItem(
                title = "Auto-play Next",
                subtitle = "Automatically start the next track in queue or playlist",
                checked = prefs.autoPlayNext,
                onCheckedChange = { viewModel.setAutoPlayNext(it) }
            )

            SettingSwitchItem(
                title = "Background Playback",
                subtitle = "Continue playing audio when app is minimized or screen off",
                checked = prefs.backgroundPlay,
                onCheckedChange = { viewModel.setBackgroundPlay(it) }
            )

            SettingSwitchItem(
                title = "Background Video Playback",
                subtitle = "Continue playing videos when video player is closed or not visible",
                checked = prefs.backgroundPlayVideo,
                onCheckedChange = { viewModel.setBackgroundPlayVideo(it) }
            )

       /*     SettingSwitchItem(
                title = "Show Sample HD Media",
                subtitle = "Display bundled 4K/1080p open-source movie & audio demos",
                checked = prefs.showSampleMedia,
                onCheckedChange = { viewModel.setShowSampleMedia(it) }
            )*/

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Controls & Gestures Section
           // SectionHeader(title = "Controls & Gestures", icon = Icons.Default.TouchApp)

/*            SettingSwitchItem(
                title = "Player Gestures",
                subtitle = "Swipe vertically for brightness & volume, double-tap to seek 10s",
                checked = prefs.gesturesEnabled,
                onCheckedChange = { viewModel.setGesturesEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))*/

            // Storage & History Section
            SectionHeader(title = "Data & History", icon = Icons.Default.DeleteSweep)

            SettingItem(
                title = "Clear Watch History",
                subtitle = "Delete all recent playback records from database",
                onClick = {
                    viewModel.clearHistory()
                    Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // About Section
            SectionHeader(title = "About", icon = Icons.Default.Info)

            SettingItem(
                title = "As Player",
                subtitle = "Version 1.0.0 (Media3 Architecture & Jetpack Compose)",
                onClick = {}
            )

            SettingItem(
                title = "Codec Support",
                subtitle = "MP4, MKV, WebM, AVI, MOV, 3GP, FLAC, MP3, AAC, OGG, HLS, DASH",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            val themes = listOf(
                "Follow System" to 0,
                "Light Theme" to 1,
                "Dark Theme" to 2,
                "AMOLED Pitch Black" to 3
            )
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        themes.forEach { (name, mode) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = prefs.themeMode == mode,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
