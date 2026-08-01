package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CyberHudMetricCard(
    title: String,
    value: String,
    unit: String = "",
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    CyberCard(
        borderColor = accentColor.copy(alpha = 0.6f),
        cutCornerSize = 8.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        slideInVertically { height -> height } togetherWith
                                slideOutVertically { height -> -height }
                    },
                    label = "hudValue"
                ) { targetVal ->
                    Text(
                        text = targetVal,
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                }

                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CyberHudGrid(
    totalEmails: Int,
    speedEmailPerMin: Int,
    scannedPages: Int,
    activeThreads: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CyberHudMetricCard(
                title = "Total Emails",
                value = totalEmails.toString(),
                unit = "FOUND",
                icon = Icons.Default.AlternateEmail,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )

            CyberHudMetricCard(
                title = "Scrape Speed",
                value = speedEmailPerMin.toString(),
                unit = "e/min",
                icon = Icons.Default.Speed,
                accentColor = ElectricGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CyberHudMetricCard(
                title = "Pages Scanned",
                value = scannedPages.toString(),
                unit = "URL",
                icon = Icons.Default.Public,
                accentColor = NeonMagenta,
                modifier = Modifier.weight(1f)
            )

            CyberHudMetricCard(
                title = "Worker Threads",
                value = activeThreads.toString(),
                unit = "ACTIVE",
                icon = Icons.Default.Memory,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
