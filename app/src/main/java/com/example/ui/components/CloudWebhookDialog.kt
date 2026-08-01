package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExtractedEmail
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted

@Composable
fun CloudWebhookDialog(
    targetEmails: List<ExtractedEmail>,
    onDismiss: () -> Unit,
    onSyncWebhook: (String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var webhookUrl by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var syncResultMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CyberDarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Cloud Sync",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLOUD WEBHOOK AUTO-SYNC",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeonCyan
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Push ${targetEmails.size} extracted leads automatically to Zapier, Make.com, or custom CRM webhook endpoints.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Webhook URL", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    placeholder = { Text("https://hooks.zapier.com/hooks/catch/...", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (syncResultMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncResultMsg,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = CyberAmber
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = {
                        if (webhookUrl.isNotBlank()) {
                            isSyncing = true
                            onSyncWebhook(webhookUrl) { msg ->
                                isSyncing = false
                                syncResultMsg = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (webhookUrl.isBlank()) TextMuted else ElectricGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isSyncing) "SYNCING TO WEBHOOK..." else "⚡ SYNC ${targetEmails.size} LEADS TO CLOUD",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberBlack
                        )
                    }
                }
            }
        }
    }
}
