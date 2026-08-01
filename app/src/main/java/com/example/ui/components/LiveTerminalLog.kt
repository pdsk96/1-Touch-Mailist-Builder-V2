package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crawler.LogEntry
import com.example.data.crawler.LogType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun LiveTerminalLogView(
    logs: List<LogEntry>,
    currentCrawlingUrl: String,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto scroll to latest log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    CyberCard(
        borderColor = NeonCyan.copy(alpha = 0.4f),
        backgroundColor = CyberDarkBackground,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Terminal Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10131E))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cyber Dot Indicators
                    Box(modifier = Modifier.size(8.dp).background(NeonMagenta, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(CyberAmber, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(ElectricGreen, CircleShape))

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE STREAM TERMINAL // FEED",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Current Active Crawl Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090B12))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[TARGET]: ",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = CyberAmber
                )
                Text(
                    text = currentCrawlingUrl,
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Logs Feed List
            if (logs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "> STANDBY MODE. TAP '1 TOUCH START' TO LAUNCH CRAWLER <",
                        fontFamily = CyberMonospace,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        TerminalLogRow(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLogRow(log: LogEntry) {
    val typeColor = when (log.type) {
        LogType.MATCH -> ElectricGreen
        LogType.SCAN -> NeonCyan
        LogType.WARN -> NeonMagenta
        LogType.SUCCESS -> CyberAmber
        LogType.FILTER -> NeonMagenta.copy(alpha = 0.8f)
        LogType.INFO -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "${log.timestamp} ",
            fontFamily = CyberMonospace,
            fontSize = 11.sp,
            color = TextMuted
        )
        Text(
            text = log.message,
            fontFamily = CyberMonospace,
            fontSize = 11.sp,
            color = typeColor,
            lineHeight = 14.sp
        )
    }
}
