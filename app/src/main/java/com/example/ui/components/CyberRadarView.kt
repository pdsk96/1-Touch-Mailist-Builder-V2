package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberRadarView(
    isRunning: Boolean,
    currentUrl: String,
    foundCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    CyberCard(
        borderColor = if (isRunning) ElectricGreen else NeonCyan.copy(alpha = 0.5f),
        cutCornerSize = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            // Animated Radar Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .background(CyberBlack, CircleShape)
                    .border(1.dp, if (isRunning) ElectricGreen else NeonCyan.copy(alpha = 0.4f), CircleShape)
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // Concentric Radar Rings
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.2f),
                        radius = radius * 0.33f,
                        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f),
                        radius = radius * 0.66f,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.35f),
                        radius = radius,
                        style = Stroke(width = 1.5f)
                    )

                    // Crosshair Grid Lines
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.2f),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.2f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 1f
                    )

                    // Rotating Scanner Sweep Line
                    if (isRunning) {
                        rotate(angle, pivot = center) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(ElectricGreen.copy(alpha = 0.9f), Color.Transparent),
                                    start = center,
                                    end = Offset(center.x + radius, center.y)
                                ),
                                start = center,
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 3f
                            )
                        }

                        // Target Blips
                        val blipAngle1 = Math.toRadians(45.0)
                        val blipAngle2 = Math.toRadians(210.0)
                        val blipAngle3 = Math.toRadians(310.0)

                        drawCircle(
                            color = NeonMagenta,
                            radius = 4f * pulseScale,
                            center = Offset(
                                (center.x + radius * 0.5f * cos(blipAngle1)).toFloat(),
                                (center.y + radius * 0.5f * sin(blipAngle1)).toFloat()
                            )
                        )
                        drawCircle(
                            color = ElectricGreen,
                            radius = 5f * pulseScale,
                            center = Offset(
                                (center.x + radius * 0.7f * cos(blipAngle2)).toFloat(),
                                (center.y + radius * 0.7f * sin(blipAngle2)).toFloat()
                            )
                        )
                        drawCircle(
                            color = CyberAmber,
                            radius = 3.5f * pulseScale,
                            center = Offset(
                                (center.x + radius * 0.4f * cos(blipAngle3)).toFloat(),
                                (center.y + radius * 0.4f * sin(blipAngle3)).toFloat()
                            )
                        )
                    }
                }

                Text(
                    text = if (isRunning) "SCANNING" else "STANDBY",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = if (isRunning) ElectricGreen else TextMuted
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radar Status Summary Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isRunning) ElectricGreen.copy(alpha = 0.2f) else CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) ElectricGreen else TextMuted),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = if (isRunning) "ACTIVE HUD RADAR" else "IDLE HUD RADAR",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = if (isRunning) ElectricGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${foundCount} TARGETS DETECTED",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TARGET URL:",
                    fontFamily = CyberMonospace,
                    fontSize = 8.sp,
                    color = TextMuted
                )
                Text(
                    text = if (currentUrl.isNotBlank()) currentUrl else "Waiting for trigger signal...",
                    fontFamily = CyberMonospace,
                    fontSize = 9.sp,
                    color = TextPrimary,
                    maxLines = 2
                )
            }
        }
    }
}
