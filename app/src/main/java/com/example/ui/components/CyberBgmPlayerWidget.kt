package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.CyberBgmSynthesizer
import com.example.util.CyberBgmTrack
import com.example.util.CyberSoundFX

@Composable
fun CyberBgmPlayerWidget(
    modifier: Modifier = Modifier
) {
    val isPlaying by CyberBgmSynthesizer.isPlaying.collectAsState()
    val currentTrack by CyberBgmSynthesizer.currentTrack.collectAsState()
    val volume by CyberBgmSynthesizer.volume.collectAsState()

    var showExpandedControls by remember { mutableStateOf(false) }

    CyberCard(
        borderColor = if (isPlaying) NeonCyan else CyberBorder,
        backgroundColor = CyberSurface,
        cutCornerSize = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("cyber_bgm_player_widget")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play/Pause + Equalizer Animation Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            CyberSoundFX.playClickSound()
                            CyberBgmSynthesizer.togglePlayPause()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isPlaying) NeonCyan.copy(alpha = 0.2f) else CyberBorder.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .border(1.dp, if (isPlaying) NeonCyan else TextMuted, CircleShape)
                            .testTag("cyber_bgm_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause BGM" else "Play BGM",
                            tint = if (isPlaying) NeonCyan else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CYBERPUNK AUDIO SYNTH",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isPlaying) {
                                AnimatedCyberEqualizer(modifier = Modifier.height(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = currentTrack.title,
                            fontFamily = CyberMonospace,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                CyberSoundFX.playClickSound()
                                CyberBgmSynthesizer.nextTrack()
                            }
                        )
                    }
                }

                // Action Controls: Skip Track & Volume Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            CyberSoundFX.playClickSound()
                            CyberBgmSynthesizer.nextTrack()
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("cyber_bgm_next_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Cyber Track",
                            tint = NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            CyberSoundFX.playClickSound()
                            showExpandedControls = !showExpandedControls
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("cyber_bgm_controls_toggle")
                    ) {
                        Icon(
                            imageVector = if (volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Volume Controls",
                            tint = CyberAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Controls: Slider & Preset Selector
            if (showExpandedControls) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOL: ${(volume * 100).toInt()}%",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.width(55.dp)
                    )

                    Slider(
                        value = volume,
                        onValueChange = { CyberBgmSynthesizer.setVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CyberBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cyber_bgm_volume_slider")
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCyberEqualizer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_transition")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h1.dp)
                .background(NeonCyan)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h2.dp)
                .background(ElectricGreen)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h3.dp)
                .background(NeonMagenta)
        )
    }
}
