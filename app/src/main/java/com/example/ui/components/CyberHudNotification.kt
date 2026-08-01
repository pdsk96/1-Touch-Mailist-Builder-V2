package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CyberSoundFX
import kotlinx.coroutines.delay

enum class CyberNotificationType {
    INFO, SUCCESS, WARNING, ALERT
}

data class CyberNotificationData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val type: CyberNotificationType = CyberNotificationType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CyberHudNotificationBanner(
    notification: CyberNotificationData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(notification?.id) { mutableStateOf(notification != null) }

    LaunchedEffect(notification?.id) {
        if (notification != null) {
            visible = true
            when (notification.type) {
                CyberNotificationType.SUCCESS -> CyberSoundFX.playSuccessSound()
                CyberNotificationType.ALERT, CyberNotificationType.WARNING -> CyberSoundFX.playAlertSound()
                CyberNotificationType.INFO -> CyberSoundFX.playClickSound()
            }
            delay(4000)
            visible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        notification?.let { notif ->
            val (borderColor, icon, titleColor) = when (notif.type) {
                CyberNotificationType.SUCCESS -> Triple(ElectricGreen, Icons.Default.CheckCircle, ElectricGreen)
                CyberNotificationType.WARNING -> Triple(CyberAmber, Icons.Default.Warning, CyberAmber)
                CyberNotificationType.ALERT -> Triple(NeonMagenta, Icons.Default.Warning, NeonMagenta)
                CyberNotificationType.INFO -> Triple(NeonCyan, Icons.Default.Info, NeonCyan)
            }

            Surface(
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                color = CyberSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, borderColor),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    // Top glowing progress indicator line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(borderColor)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 8.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = borderColor.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "HUD ALERT",
                                            fontFamily = CyberMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = borderColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = notif.title.uppercase(),
                                        fontFamily = CyberMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = titleColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = notif.message,
                                    fontFamily = CyberMonospace,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                visible = false
                                onDismiss()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
