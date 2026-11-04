package com.example.feature.nowplaying

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.service.audioeffect.AudioEffectsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    audioEffectsManager: AudioEffectsManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val eqState by audioEffectsManager.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer & Effects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = { audioEffectsManager.setEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Row
            Text(
                text = "Sound Profiles",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eqState.presets.forEachIndexed { index, presetName ->
                    FilterChip(
                        selected = eqState.currentPreset == index,
                        onClick = { audioEffectsManager.applyPreset(index) },
                        label = { Text(presetName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Frequency Bands
            Text(
                text = "Frequency Sliders",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            eqState.bands.forEach { band ->
                val range = (band.maxLevelMilliBels - band.minLevelMilliBels).toFloat()
                val currentFloat = if (range > 0) {
                    ((band.currentLevelMilliBels - band.minLevelMilliBels) / range).coerceIn(0f, 1f)
                } else 0.5f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}kHz" else "${band.centerFreqHz}Hz",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(54.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = currentFloat,
                        onValueChange = { newVal ->
                            val level = (band.minLevelMilliBels + (newVal * range)).toInt().toShort()
                            audioEffectsManager.setBandLevel(band.index, level)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = eqState.isEnabled
                    )
                    Text(
                        text = "${band.currentLevelMilliBels / 100}dB",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(44.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bass Boost & Virtualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bass Boost",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = eqState.bassBoostStrength / 1000f,
                    onValueChange = { audioEffectsManager.setBassBoost((it * 1000).toInt()) },
                    modifier = Modifier.weight(1f),
                    enabled = eqState.isEnabled
                )
                Text(
                    text = "${(eqState.bassBoostStrength / 10)}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(36.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3D Surround",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = eqState.virtualizerStrength / 1000f,
                    onValueChange = { audioEffectsManager.setVirtualizer((it * 1000).toInt()) },
                    modifier = Modifier.weight(1f),
                    enabled = eqState.isEnabled
                )
                Text(
                    text = "${(eqState.virtualizerStrength / 10)}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
