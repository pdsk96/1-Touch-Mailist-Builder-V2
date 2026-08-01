package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonCyan.copy(alpha = 0.5f),
    backgroundColor: Color = CyberSurface,
    borderWidth: Dp = 1.dp,
    cutCornerSize: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cutPx = cutCornerSize.toPx()
                val w = size.width
                val h = size.height

                // Custom cut-corner polygon path for Cyberpunk look
                val path = Path().apply {
                    moveTo(cutPx, 0f)
                    lineTo(w - cutPx, 0f)
                    lineTo(w, cutPx)
                    lineTo(w, h - cutPx)
                    lineTo(w - cutPx, h)
                    lineTo(cutPx, h)
                    lineTo(0f, h - cutPx)
                    lineTo(0f, cutPx)
                    close()
                }

                // Draw background
                drawPath(path = path, color = backgroundColor)

                // Draw neon stroke
                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(width = borderWidth.toPx())
                )

                // Top-Left Cyber Corner Accent
                drawLine(
                    color = borderColor,
                    start = Offset(0f, cutPx + 8f),
                    end = Offset(0f, cutPx),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = borderColor,
                    start = Offset(0f, cutPx),
                    end = Offset(cutPx, 0f),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .padding(12.dp),
        content = content
    )
}
