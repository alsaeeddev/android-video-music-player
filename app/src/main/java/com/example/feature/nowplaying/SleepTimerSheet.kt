package com.example.feature.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.service.SleepTimerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerManager: SleepTimerManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTimerActive by sleepTimerManager.isTimerActive.collectAsState()
    val remainingSeconds by sleepTimerManager.remainingSeconds.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isTimerActive) {
                Spacer(modifier = Modifier.height(12.dp))
                val timeLabel = if (remainingSeconds < 0) {
                    "Music will stop at the end of this track"
                } else {
                    "Music stops in ${remainingSeconds / 60}m ${remainingSeconds % 60}s"
                }
                Text(
                    text = timeLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val options = listOf(
                "15 Minutes" to 15,
                "30 Minutes" to 30,
                "45 Minutes" to 45,
                "60 Minutes" to 60
            )

            options.forEach { (label, minutes) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sleepTimerManager.startTimerMinutes(minutes)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isTimerActive && remainingSeconds == minutes * 60,
                        onClick = {
                            sleepTimerManager.startTimerMinutes(minutes)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        sleepTimerManager.startTimerEndOfTrack()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isTimerActive && remainingSeconds == -1,
                    onClick = {
                        sleepTimerManager.startTimerEndOfTrack()
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "End of this track", style = MaterialTheme.typography.bodyLarge)
            }

            if (isTimerActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        sleepTimerManager.cancelTimer()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.TimerOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn Off Sleep Timer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
