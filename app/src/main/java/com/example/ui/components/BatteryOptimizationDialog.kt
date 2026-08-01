package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.BatteryOptimizationHelper

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isExempt by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    Dialog(onDismissRequest = onDismiss) {
        CyberCard(
            borderColor = ElectricGreen,
            backgroundColor = CyberSurface,
            cutCornerSize = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ PERSISTENT & AUTO-START",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ElectricGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Aplikasi perlu izin berjalan tanpa henti di background (bahkan saat mode hemat baterai & HP di-restart).",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Indicator Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isExempt) ElectricGreen.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isExempt) ElectricGreen else CyberAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isExempt) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isExempt) ElectricGreen else CyberAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isExempt) "BATTERY SAVER EXEMPTION: ACTIVE" else "BATTERY SAVER EXEMPTION: NOT GRANTED",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isExempt) ElectricGreen else CyberAmber
                            )
                            Text(
                                text = if (isExempt) "Engine bebas berjalan 24/7 tanpa dihentikan sistem OS." else "Sistem Android dapat menghentikan engine saat hemat baterai aktif.",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action 1: Request Battery Optimization Exemption
                CyberButton(
                    text = "1. MINTA IZIN BEBAS HEMAT BATERAI",
                    icon = Icons.Default.FlashOn,
                    accentColor = ElectricGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                        isExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Open OEM Auto-Start Manager (Xiaomi, Oppo, Vivo, Samsung, Huawei)
                CyberButton(
                    text = "2. BUKA PENGATURAN AUTO-START (OEM)",
                    icon = Icons.Default.Settings,
                    accentColor = NeonCyan,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        BatteryOptimizationHelper.openAutoStartSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(6.dp),
                    color = CyberSurface,
                    border = BorderStroke(1.dp, TextMuted),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "TUTUP",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
