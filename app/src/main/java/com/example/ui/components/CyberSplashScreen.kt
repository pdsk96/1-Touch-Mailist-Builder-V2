package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.TextSecondary
import com.example.util.CyberSoundFX
import kotlinx.coroutines.delay

@Composable
fun CyberSplashScreen(
    onSplashFinished: () -> Unit
) {
    var bootProgress by remember { mutableFloatStateOf(0f) }
    var bootStepIndex by remember { mutableIntStateOf(0) }

    val bootLogs = remember {
        listOf(
            "[SYS_INIT] Loading Cyber Neural Engine...",
            "[NET_TUNNEL] Setting up stealth proxy matrix...",
            "[OCR_CORE] ML Kit Optical Scanner initialized...",
            "[SECURITY] Decrypting database keys...",
            "[STORAGE] Mounting local Room DB cache...",
            "[SMTP_RELAY] Pre-warming mail distribution engine...",
            "[OS_READY] CYBER CRAWLER OS booted successfully."
        )
    }

    // Pulse animation for neon HUD ring
    val infiniteTransition = rememberInfiniteTransition(label = "CyberSplashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotationAngle"
    )

    val glitchAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitchAlpha"
    )

    // Automatic boot progress timer
    LaunchedEffect(Unit) {
        CyberSoundFX.playSuccessSound()
        for (i in 1..100) {
            delay(25)
            bootProgress = i / 100f
            if (i == 15) bootStepIndex = 1
            if (i == 35) bootStepIndex = 2
            if (i == 55) bootStepIndex = 3
            if (i == 75) bootStepIndex = 4
            if (i == 90) bootStepIndex = 5
            if (i == 100) bootStepIndex = 6
        }
        delay(400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .testTag("cyber_splash_screen")
            .clickable {
                CyberSoundFX.playClickSound()
                onSplashFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Background Cyber Grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            val gridColor = NeonCyan.copy(alpha = 0.08f)

            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // HUD Scanner Ring & Logo Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                ) {
                    val strokeWidth = 3.dp.toPx()
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, NeonMagenta, ElectricGreen, NeonCyan)
                        ),
                        radius = size.width / 2 - strokeWidth,
                        style = Stroke(width = strokeWidth)
                    )
                }

                // Inner pulsing core badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.25f),
                                    NeonMagenta.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(1.dp, NeonCyan, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Option 3 Title & Subtext
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(glitchAlpha)
            ) {
                Text(
                    text = "CYBER CRAWLER OS",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = NeonCyan,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // SUBTEXT requested by user: "by pdsk project"
                Text(
                    text = "by pdsk project",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NeonMagenta,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "v3.5.0-NEON // AUTOMATED WEB DATA ENGINE",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Progress Bar Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = ElectricGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYSTEM BOOT SEQUENCE",
                                fontFamily = CyberMonospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricGreen
                            )
                        }

                        Text(
                            text = "${(bootProgress * 100).toInt()}%",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Track Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberBlack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(bootProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(NeonCyan, NeonMagenta, ElectricGreen)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Log output stream
                    Text(
                        text = bootLogs.getOrElse(bootStepIndex) { bootLogs.last() },
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Touch to Skip Prompt
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .background(CyberSurface.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "TAP TO INITIALIZE NOW ➔",
                    fontFamily = CyberMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }
    }
}
