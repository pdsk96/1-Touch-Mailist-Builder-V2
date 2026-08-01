package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import com.example.data.model.ExtractedEmail
import com.example.data.model.SmtpProfile
import com.example.data.smtp.SmtpConfig
import com.example.data.smtp.SmtpProgress
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
import com.example.util.CyberSoundFX

@Composable
fun SmtpCampaignDialog(
    targetEmails: List<ExtractedEmail>,
    progress: SmtpProgress,
    savedProfiles: List<SmtpProfile> = emptyList(),
    onDismiss: () -> Unit,
    onStartCampaign: (SmtpConfig) -> Unit,
    onSaveProfile: (SmtpProfile) -> Unit = {},
    onDeleteProfile: (SmtpProfile) -> Unit = {},
    onGenerateAiTemplate: (productName: String, valueProp: String, apiKey: String, onResult: (String, String) -> Unit) -> Unit
) {
    var host by remember { mutableStateOf("smtp.gmail.com") }
    var portStr by remember { mutableStateOf("587") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("PDSK Sales Team") }
    var profileNameInput by remember { mutableStateOf("") }
    var showSaveProfileDialog by remember { mutableStateOf(false) }

    var subjectTemplate by remember { mutableStateOf("Quick proposal regarding {{domain}}") }
    var bodyTemplate by remember { mutableStateOf("Hi,\n\nI noticed your contact ({{email}}) at {{domain}}. We offer high quality digital solutions.\n\nWould you be open for a quick chat?\n\nBest regards,\nPDSK Team") }

    var showAiGenerator by remember { mutableStateOf(false) }
    var aiProductName by remember { mutableStateOf("PDSK B2B Platform") }
    var aiValueProp by remember { mutableStateOf("automating email lead extraction and sales outreach") }
    var aiApiKey by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val defaultPresets = listOf(
        SmtpProfile(profileName = "Gmail SMTP", host = "smtp.gmail.com", port = 587),
        SmtpProfile(profileName = "SendGrid", host = "smtp.sendgrid.net", port = 587),
        SmtpProfile(profileName = "Mailgun", host = "smtp.mailgun.org", port = 587),
        SmtpProfile(profileName = "Outlook / O365", host = "smtp.office365.com", port = 587)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CyberDarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMTP",
                            tint = NeonMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DIRECT SMTP CAMPAIGN",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonMagenta
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Text(
                    text = "Send bulk cold emails directly to ${targetEmails.size} extracted targets with smart anti-spam delays.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Monitor Box if sending
                if (progress.isSending || progress.sentCount > 0 || progress.failCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (progress.isSending) "⚡ CAMPAIGN IN PROGRESS..." else "STATUS REPORT",
                                    fontFamily = CyberMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ElectricGreen
                                )
                                Text(
                                    text = "SENT: ${progress.sentCount} / ${progress.total} (FAIL: ${progress.failCount})",
                                    fontFamily = CyberMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val progressFraction = if (progress.total > 0) progress.sentCount.toFloat() / progress.total.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                color = ElectricGreen,
                                trackColor = CyberDarkBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = progress.lastLog,
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = CyberAmber
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // AI Draft Generator Toggle Card
                Surface(
                    onClick = { showAiGenerator = !showAiGenerator },
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = CyberAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GEMINI AI EMAIL DRAFT GENERATOR", fontFamily = CyberMonospace, fontSize = 11.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                        }
                        Text(if (showAiGenerator) "[-]" else "[+]", fontFamily = CyberMonospace, fontSize = 11.sp, color = CyberAmber)
                    }
                }

                AnimatedVisibility(visible = showAiGenerator) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSurface.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        OutlinedTextField(
                            value = aiProductName,
                            onValueChange = { aiProductName = it },
                            label = { Text("Product / Service Name", fontFamily = CyberMonospace, fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = aiValueProp,
                            onValueChange = { aiValueProp = it },
                            label = { Text("Value Proposition", fontFamily = CyberMonospace, fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = aiApiKey,
                            onValueChange = { aiApiKey = it },
                            label = { Text("Gemini API Key (Optional)", fontFamily = CyberMonospace, fontSize = 9.sp) },
                            placeholder = { Text("Leave blank for template draft", fontFamily = CyberMonospace, fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            onClick = {
                                if (targetEmails.isNotEmpty()) {
                                    isGeneratingAi = true
                                    onGenerateAiTemplate(aiProductName, aiValueProp, aiApiKey) { subj, body ->
                                        subjectTemplate = subj
                                        bodyTemplate = body
                                        isGeneratingAi = false
                                        showAiGenerator = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = CyberAmber,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = if (isGeneratingAi) "GENERATING WITH GEMINI..." else "✨ GENERATE DRAFT FOR CAMPAIGN",
                                    fontFamily = CyberMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = CyberBlack
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SAVED SMTP PROFILES & PRESETS SECTION
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💾 SAVED SMTP PROFILES & PRESETS", fontFamily = CyberMonospace, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Surface(
                        onClick = { showSaveProfileDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE PRESET", fontFamily = CyberMonospace, fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preset Chips Row (Built-in + Saved in Database)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Default Presets
                    items(defaultPresets) { p ->
                        Surface(
                            onClick = {
                                host = p.host
                                portStr = p.port.toString()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⚡ ${p.profileName}",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Saved DB Profiles
                    items(savedProfiles) { sp ->
                        Surface(
                            onClick = {
                                host = sp.host
                                portStr = sp.port.toString()
                                username = sp.username
                                password = sp.password
                                senderName = sp.senderName
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sp.profileName,
                                    fontFamily = CyberMonospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteProfile(sp) },
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                if (showSaveProfileDialog) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = profileNameInput,
                            onValueChange = { profileNameInput = it },
                            placeholder = { Text("Profile Name (e.g. My Gmail Sales)", fontFamily = CyberMonospace, fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = {
                                if (profileNameInput.isNotBlank()) {
                                    onSaveProfile(
                                        SmtpProfile(
                                            profileName = profileNameInput,
                                            host = host,
                                            port = portStr.toIntOrNull() ?: 587,
                                            username = username,
                                            password = password,
                                            senderName = senderName
                                        )
                                    )
                                    showSaveProfileDialog = false
                                    profileNameInput = ""
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricGreen
                        ) {
                            Text("SAVE", fontFamily = CyberMonospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlack, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SMTP Credentials Form
                Text("SMTP CREDENTIALS", fontFamily = CyberMonospace, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("SMTP Host", fontFamily = CyberMonospace, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port", fontFamily = CyberMonospace, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Sender Email / Username", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    placeholder = { Text("sales@yourcompany.com", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("SMTP Password / App Password", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Display Name", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Template Configuration
                Text("EMAIL TEMPLATE (Variables: {{email}}, {{domain}}, {{phone}})", fontFamily = CyberMonospace, fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = subjectTemplate,
                    onValueChange = { subjectTemplate = it },
                    label = { Text("Subject Template", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = bodyTemplate,
                    onValueChange = { bodyTemplate = it },
                    label = { Text("Email Body Template", fontFamily = CyberMonospace, fontSize = 9.sp) },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Launch Button
                Surface(
                    onClick = {
                        CyberSoundFX.playScanPulseSound()
                        val portInt = portStr.toIntOrNull() ?: 587
                        val cfg = SmtpConfig(
                            host = host,
                            port = portInt,
                            username = username,
                            password = password,
                            senderName = senderName,
                            subjectTemplate = subjectTemplate,
                            bodyTemplate = bodyTemplate
                        )
                        onStartCampaign(cfg)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (username.isBlank()) TextMuted else ElectricGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = CyberBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (progress.isSending) "CAMPAIGN RUNNING..." else "🚀 LAUNCH BULK SMTP CAMPAIGN",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberBlack
                        )
                    }
                }
            }
        }
    }
}
