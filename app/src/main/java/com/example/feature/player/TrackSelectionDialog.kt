package com.example.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Tracks
import com.example.service.player.PlayerController

@Composable
fun TrackSelectionDialog(
    playerController: PlayerController,
    tracks: Tracks?,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Audio, 1: Subtitles

    val audioGroups = remember(tracks) {
        tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }.orEmpty()
    }
    val textGroups = remember(tracks) {
        tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }.orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tracks & Subtitles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audio (${audioGroups.size})")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ClosedCaption, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subtitles (${textGroups.size})")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    if (selectedTab == 0) {
                        if (audioGroups.isEmpty()) {
                            item {
                                Text(
                                    text = "Default stereo track (Embedded)",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(audioGroups.size) { groupIdx ->
                                val group = audioGroups[groupIdx]
                                for (trackIdx in 0 until group.length) {
                                    val isSelected = group.isTrackSelected(trackIdx)
                                    val format = group.getTrackFormat(trackIdx)
                                    val label = format.label ?: format.language ?: "Track ${trackIdx + 1} (${format.sampleMimeType ?: "Audio"})"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                playerController.selectTrack(group, trackIdx)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { playerController.selectTrack(group, trackIdx) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    } else {
                        // Subtitle Tab with "Off" option
                        item {
                            val isAnySelected = textGroups.any { it.isSelected }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playerController.clearSubtitleTrack() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isAnySelected,
                                    onClick = { playerController.clearSubtitleTrack() }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Off (Disable Subtitles)", style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }

                        if (textGroups.isEmpty()) {
                            item {
                                Text(
                                    text = "No subtitle tracks found in this stream",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(textGroups.size) { groupIdx ->
                                val group = textGroups[groupIdx]
                                for (trackIdx in 0 until group.length) {
                                    val isSelected = group.isTrackSelected(trackIdx)
                                    val format = group.getTrackFormat(trackIdx)
                                    val label = format.label ?: format.language ?: "Subtitle ${trackIdx + 1}"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                playerController.selectTrack(group, trackIdx)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { playerController.selectTrack(group, trackIdx) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
