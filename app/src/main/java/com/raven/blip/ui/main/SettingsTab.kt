package com.raven.blip.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.blip.domain.model.OverlayCorner
import com.raven.blip.ui.overlay.OverlayService
import kotlin.math.roundToInt

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    isRunning: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState())
            ) {
                // Service Toggle
                Text("Service Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (isRunning) "Blip is active" else "Blip is inactive",
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (!isRunning) {
                    Button(
                        onClick = onStartService,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Start Blip", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopService,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Blip", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Corner Placement
                Text("Corner Placement", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Where should Blip anchor on your screen?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.corner == OverlayCorner.TOP_START,
                        onClick = { viewModel.updateCorner(OverlayCorner.TOP_START) },
                        label = { Text("Top Left") }
                    )
                    FilterChip(
                        selected = settings.corner == OverlayCorner.TOP_END,
                        onClick = { viewModel.updateCorner(OverlayCorner.TOP_END) },
                        label = { Text("Top Right") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.corner == OverlayCorner.BOTTOM_START,
                        onClick = { viewModel.updateCorner(OverlayCorner.BOTTOM_START) },
                        label = { Text("Bottom Left") }
                    )
                    FilterChip(
                        selected = settings.corner == OverlayCorner.BOTTOM_END,
                        onClick = { viewModel.updateCorner(OverlayCorner.BOTTOM_END) },
                        label = { Text("Bottom Right") }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bubble Interval
                Text("Bubble Interval", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                val currentMinutes = (settings.bubbleIntervalMs / 60000f)
                Text(
                    text = "${currentMinutes.roundToInt()} minutes", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Slider(
                    value = currentMinutes,
                    onValueChange = { mins -> viewModel.updateBubbleInterval((mins * 60000f).toLong()) },
                    valueRange = 1f..60f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Quiet Hours
                Text("Quiet Hours", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Mute thought bubbles during these hours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val startHour = settings.quietHoursStart?.toFloat() ?: 22f
                val endHour = settings.quietHoursEnd?.toFloat() ?: 7f
                val isQuietHoursEnabled = settings.quietHoursStart != null

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Enable Quiet Hours", color = MaterialTheme.colorScheme.onSurface)
                    androidx.compose.material3.Switch(
                        checked = isQuietHoursEnabled,
                        onCheckedChange = { enabled -> 
                            if (enabled) viewModel.updateQuietHours(22, 7)
                            else viewModel.updateQuietHours(null, null)
                        }
                    )
                }

                if (isQuietHoursEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start time: ${startHour.roundToInt()}:00", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = startHour,
                        onValueChange = { h -> viewModel.updateQuietHours(h.roundToInt(), settings.quietHoursEnd) },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Text(
                        text = "End time: ${endHour.roundToInt()}:00", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = endHour,
                        onValueChange = { h -> viewModel.updateQuietHours(settings.quietHoursStart, h.roundToInt()) },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                val context = LocalContext.current
                Text("Testing", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, OverlayService::class.java).apply {
                            action = OverlayService.ACTION_SHOW_BUBBLE
                        }
                        context.startService(intent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Force Bubble", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.injectTestTasks() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Inject Test Tasks", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
