package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExtractedEmail
import com.example.data.model.SmtpProfile
import com.example.data.smtp.SmtpConfig
import com.example.data.smtp.SmtpProgress
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DraftPreset(
    val id: String,
    val name: String,
    val category: String,
    val subject: String,
    val body: String,
    val isCustom: Boolean = false
)

@Composable
fun SmtpWorkspaceView(
    targetEmails: List<ExtractedEmail>,
    progress: SmtpProgress,
    savedProfiles: List<SmtpProfile> = emptyList(),
    onStartCampaign: (SmtpConfig) -> Unit,
    onSaveProfile: (SmtpProfile) -> Unit = {},
    onDeleteProfile: (SmtpProfile) -> Unit = {},
    onEnrichProspects: ((String) -> Unit) -> Unit = {},
    onExportBackup: (passkey: String, (String) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackup: (encryptedJson: String, passkey: String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var host by remember { mutableStateOf("smtp.gmail.com") }
    var portStr by remember { mutableStateOf("587") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("PDSK Sales Team") }
    var profileNameInput by remember { mutableStateOf("") }
    var showSaveProfileDialog by remember { mutableStateOf(false) }

    // Feature 1: Multi-Sender SMTP Rotation state
    var enableRotation by remember { mutableStateOf(false) }
    val selectedRotationProfiles = remember { mutableStateListOf<SmtpProfile>() }

    // Feature 2: Automated Follow-Up Sequences state
    var enableSequence by remember { mutableStateOf(false) }
    var step1Subject by remember { mutableStateOf("Quick question regarding {{domain}}") }
    var step2Subject by remember { mutableStateOf("Re: Quick question regarding {{domain}}") }
    var step3Subject by remember { mutableStateOf("Permission to close file for {{domain}}?") }

    // Feature 3: Unsubscribe / Opt-Out Footer Generator state
    var enableOptOutFooter by remember { mutableStateOf(true) }
    var optOutType by remember { mutableStateOf("REPLY_UNSUBSCRIBE") } // REPLY_UNSUBSCRIBE, LINK, CUSTOM
    var customOptOutText by remember { mutableStateOf("") }

    // Feature 4 & 5: Backup & Enrich Dialog states
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var backupEncryptedResult by remember { mutableStateOf<String?>(null) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var isEnriching by remember { mutableStateOf(false) }

    // Draft Templates
    val builtInPresets = remember {
        listOf(
            DraftPreset(
                id = "b2b_direct",
                name = "🚀 B2B Direct Outreach",
                category = "COLD OUTREACH",
                subject = "Quick question regarding {{domain}}",
                body = "Hi,\n\nI noticed your contact ({{email}}) at {{domain}}. We specialize in helping companies in the {{category}} sector automate outreach and grow revenues.\n\nWould you be open for a brief 5-minute chat this week?\n\nBest regards,\n{{sender_name}}"
            ),
            DraftPreset(
                id = "partnership",
                name = "🤝 Strategic Partnership",
                category = "PARTNERSHIP",
                subject = "Partnership proposal for {{domain}}",
                body = "Hello,\n\nI came across {{domain}} and was really impressed by your operations. I am reaching out to discuss potential synergy between our solutions.\n\nCould we schedule a short discovery call?\n\nBest regards,\n{{sender_name}}"
            ),
            DraftPreset(
                id = "saas_pitch",
                name = "💼 SaaS Demo Request",
                category = "SALES PITCH",
                subject = "Streamlining sales workflows at {{domain}}",
                body = "Hi,\n\nReaching out to {{email}}. We built a platform that cuts lead processing time by 80% for {{category}} teams.\n\nWould you be open to a quick 2-minute demo?\n\nBest,\n{{sender_name}}"
            ),
            DraftPreset(
                id = "followup_1",
                name = "🔄 Gentle Follow-Up #1",
                category = "FOLLOW-UP",
                subject = "Re: Quick question regarding {{domain}}",
                body = "Hi,\n\nFollowing up on my previous note to {{email}}. I know your schedule is busy, so I'll keep this brief.\n\nWould you have 3 minutes for a quick overview?\n\nBest,\n{{sender_name}}"
            ),
            DraftPreset(
                id = "audit_guide",
                name = "🎁 Value Audit & Insights",
                category = "LEAD MAGNET",
                subject = "Growth insights for {{domain}}",
                body = "Hi {{email}},\n\nWe recently analyzed leading platforms in {{category}} and compiled an actionable optimization breakdown for {{domain}}.\n\nReply to this message and I will send the guide over right away.\n\nCheers,\n{{sender_name}}"
            ),
            DraftPreset(
                id = "breakup_final",
                name = "🚪 Breakup Email (Final)",
                category = "CLOSING",
                subject = "Permission to close file for {{domain}}?",
                body = "Hi,\n\nI haven't heard back regarding my notes to {{email}}, so I assume this isn't a priority right now.\n\nIf anything changes in the future, please feel free to reach out anytime!\n\nBest regards,\n{{sender_name}}"
            )
        )
    }

    var customDraftPresets by remember { mutableStateOf<List<DraftPreset>>(emptyList()) }
    var selectedPresetId by remember { mutableStateOf("b2b_direct") }
    var customDraftTitleInput by remember { mutableStateOf("") }
    var showSaveDraftDialog by remember { mutableStateOf(false) }

    var subjectTemplate by remember { mutableStateOf(builtInPresets.first().subject) }
    var bodyTemplate by remember { mutableStateOf(builtInPresets.first().body) }

    var sendingDelaySeconds by remember { mutableFloatStateOf(2f) }

    // Live Preview State
    var isLivePreviewEnabled by remember { mutableStateOf(false) }
    var previewRecipientIndex by remember { mutableStateOf(0) }

    var isTestingHandshake by remember { mutableStateOf(false) }
    var handshakeResult by remember { mutableStateOf<String?>(null) }

    val defaultServerPresets = remember {
        listOf(
            SmtpProfile(profileName = "Gmail SMTP", host = "smtp.gmail.com", port = 587),
            SmtpProfile(profileName = "SendGrid", host = "smtp.sendgrid.net", port = 587),
            SmtpProfile(profileName = "Mailgun", host = "smtp.mailgun.org", port = 587),
            SmtpProfile(profileName = "Outlook / O365", host = "smtp.office365.com", port = 587),
            SmtpProfile(profileName = "Yahoo SMTP", host = "smtp.mail.yahoo.com", port = 587)
        )
    }

    // Helper preview recipient target
    val currentPreviewTarget: ExtractedEmail = remember(targetEmails, previewRecipientIndex) {
        if (targetEmails.isNotEmpty()) {
            targetEmails[previewRecipientIndex.coerceIn(0, targetEmails.lastIndex)]
        } else {
            ExtractedEmail(
                email = "prospect@acme-corp.com",
                domain = "acme-corp.com",
                category = "BUSINESS",
                sourceUrl = "https://acme-corp.com/contact",
                phone = "+1 (555) 234-5678"
            )
        }
    }

    // Dynamic Render Function
    fun renderTemplate(template: String, target: ExtractedEmail, sender: String): String {
        val currentDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        return template
            .replace("{{email}}", target.email)
            .replace("{{domain}}", target.domain)
            .replace("{{category}}", target.category)
            .replace("{{phone}}", target.phone.ifBlank { "N/A" })
            .replace("{{sender_name}}", sender)
            .replace("{{date}}", currentDateStr)
            .replace("{{company}}", target.domain.replace(".com", "").replace(".co", "").replace("-", " ").replaceFirstChar { it.uppercase() })
    }

    // Anti-spam warning detector
    val spamKeywords = listOf("FREE", "100%", "GUARANTEED", "BUY NOW", "CLICK HERE", "MAKE MONEY", "URGENT", "CASH")
    val detectedSpamWords = remember(subjectTemplate, bodyTemplate) {
        val text = "$subjectTemplate $bodyTemplate".uppercase()
        spamKeywords.filter { text.contains(it) }
    }

    // Word & Character count calculation
    val wordCount = remember(bodyTemplate) { bodyTemplate.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size }
    val charCount = remember(bodyTemplate) { bodyTemplate.length }
    val estReadTimeSeconds = remember(wordCount) { (wordCount / 3.0).toInt().coerceAtLeast(2) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp)
    ) {
        // Workspace Header Banner
        CyberCard(
            borderColor = NeonMagenta,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMTP Workspace",
                            tint = NeonMagenta,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMTP DIRECT CAMPAIGN WORKSPACE",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonMagenta
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricGreen.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen)
                    ) {
                        Text(
                            text = "${targetEmails.size} RECIPIENTS",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = ElectricGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Konfigurasi SMTP server, kelola preset draf email, dan luncurkan kampanye pemasaran langsung ke prospek.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Bar for Data Enrichment & Encrypted Backup
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = {
                            isEnriching = true
                            onEnrichProspects { msg ->
                                isEnriching = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = if (isEnriching) "⏳ ENRICHING..." else "✨ ENRICH PROSPECTS",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    Surface(
                        onClick = { showBackupDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        color = CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "🔐 ENCRYPTED BACKUP",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberAmber
                            )
                        }
                    }
                }
            }
        }

        // Encrypted Backup & Restore Dialog
        if (showBackupDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = {
                    Text("🔐 Backup & Restore Database (AES Encrypted)", fontFamily = CyberMonospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Masukkan Passkey untuk mengunci (Encrypt) atau membuka (Decrypt) data:", fontFamily = CyberMonospace, fontSize = 11.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it },
                            label = { Text("Encrypted Passkey / Master Password", fontFamily = CyberMonospace, fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = {
                                    if (backupPassword.isBlank()) {
                                        Toast.makeText(context, "Masukkan Passkey terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        return@Surface
                                    }
                                    onExportBackup(backupPassword) { encryptedResult ->
                                        backupEncryptedResult = encryptedResult
                                        Toast.makeText(context, "Encrypted Backup String Generated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = CyberAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔒 EXPORT BACKUP", fontFamily = CyberMonospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberAmber, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                        if (backupEncryptedResult != null) {
                            Text("Hasil Encrypted Payload (Base64 AES):", fontFamily = CyberMonospace, fontSize = 9.sp, color = ElectricGreen)
                            OutlinedTextField(
                                value = backupEncryptedResult!!,
                                onValueChange = {},
                                readOnly = true,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Restore Data dari Encrypted JSON Payload:", fontFamily = CyberMonospace, fontSize = 10.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            placeholder = { Text("Paste Encrypted Base64 JSON payload...", fontFamily = CyberMonospace, fontSize = 10.sp) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            onClick = {
                                if (backupPassword.isBlank() || restoreJsonInput.isBlank()) {
                                    Toast.makeText(context, "Isi Passkey dan Encrypted JSON payload!", Toast.LENGTH_SHORT).show()
                                    return@Surface
                                }
                                onRestoreBackup(restoreJsonInput, backupPassword) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) showBackupDialog = false
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔓 RESTORE DATABASE", fontFamily = CyberMonospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricGreen, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                },
                confirmButton = {
                    Surface(onClick = { showBackupDialog = false }, shape = RoundedCornerShape(4.dp), color = CyberSurface) {
                        Text("TUTUP", fontFamily = CyberMonospace, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campaign Execution Progress Box
        if (progress.isSending || progress.sentCount > 0 || progress.failCount > 0) {
            CyberCard(
                borderColor = if (progress.isSending) ElectricGreen else NeonCyan,
                backgroundColor = CyberSurface,
                cutCornerSize = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (progress.isSending) "⚡ CAMPAIGN IN PROGRESS..." else "✅ CAMPAIGN STATUS REPORT",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (progress.isSending) ElectricGreen else NeonCyan
                        )
                        Text(
                            text = "SENT: ${progress.sentCount} / ${progress.total} (FAIL: ${progress.failCount})",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progressFraction = if (progress.total > 0) progress.sentCount.toFloat() / progress.total.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = ElectricGreen,
                        trackColor = com.example.ui.theme.CyberDarkBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "LAST LOG: ${progress.lastLog}",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = CyberAmber
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // SECTION 1: COLD EMAIL PRESET LIBRARY & CUSTOM DRAFT MANAGER
        CyberCard(
            borderColor = CyberAmber,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Style, contentDescription = "Presets", tint = CyberAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📚 COLD EMAIL PRESET LIBRARY",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = CyberAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        onClick = { showSaveDraftDialog = !showSaveDraftDialog },
                        shape = RoundedCornerShape(6.dp),
                        color = CyberAmber.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Draft", tint = CyberAmber, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE DRAFT PRESET", fontFamily = CyberMonospace, fontSize = 9.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Pilih preset standar atau simpan draf kustom Anda untuk mempermudah kampanye berulang.",
                    fontFamily = CyberMonospace,
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Built-in & Custom Presets Chips Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Built-in templates
                    items(builtInPresets) { preset ->
                        val isSelected = selectedPresetId == preset.id
                        Surface(
                            onClick = {
                                selectedPresetId = preset.id
                                subjectTemplate = preset.subject
                                bodyTemplate = preset.body
                                Toast.makeText(context, "Loaded Preset: ${preset.name}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyberAmber else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = if (isSelected) 1f else 0.4f))
                        ) {
                            Text(
                                text = preset.name,
                                fontFamily = CyberMonospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberBlack else CyberAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Custom user saved drafts
                    items(customDraftPresets) { customPreset ->
                        val isSelected = selectedPresetId == customPreset.id
                        Surface(
                            onClick = {
                                selectedPresetId = customPreset.id
                                subjectTemplate = customPreset.subject
                                bodyTemplate = customPreset.body
                                Toast.makeText(context, "Loaded Custom Draft: ${customPreset.name}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ElectricGreen else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (isSelected) CyberBlack else ElectricGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customPreset.name,
                                    fontFamily = CyberMonospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberBlack else ElectricGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        customDraftPresets = customDraftPresets.filter { it.id != customPreset.id }
                                        Toast.makeText(context, "Deleted custom preset!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Save Custom Draft Dialog Row
                AnimatedVisibility(visible = showSaveDraftDialog) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = customDraftTitleInput,
                                onValueChange = { customDraftTitleInput = it },
                                placeholder = { Text("Nama Preset Draf (cth. Pitch Q3 Promo)", fontFamily = CyberMonospace, fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberAmber,
                                    unfocusedBorderColor = CyberAmber.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                onClick = {
                                    if (customDraftTitleInput.isNotBlank()) {
                                        val newPreset = DraftPreset(
                                            id = "custom_${System.currentTimeMillis()}",
                                            name = "⭐ ${customDraftTitleInput.trim()}",
                                            category = "CUSTOM",
                                            subject = subjectTemplate,
                                            body = bodyTemplate,
                                            isCustom = true
                                        )
                                        customDraftPresets = customDraftPresets + newPreset
                                        selectedPresetId = newPreset.id
                                        showSaveDraftDialog = false
                                        customDraftTitleInput = ""
                                        Toast.makeText(context, "Custom Draft Preset Saved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = CyberAmber
                            ) {
                                Text("SIMPAN", fontFamily = CyberMonospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlack, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 2: ADVANCED EMAIL DRAFT EDITOR & LIVE RECIPIENT SIMULATOR
        CyberCard(
            borderColor = NeonMagenta,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = "Draft Editor", tint = NeonMagenta, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📝 COLD EMAIL DRAFT EDITOR",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Live Recipient Preview Toggle
                    Surface(
                        onClick = { isLivePreviewEnabled = !isLivePreviewEnabled },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLivePreviewEnabled) ElectricGreen.copy(alpha = 0.2f) else CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLivePreviewEnabled) ElectricGreen else TextMuted)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Preview,
                                contentDescription = "Preview Mode",
                                tint = if (isLivePreviewEnabled) ElectricGreen else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLivePreviewEnabled) "👁️ PREVIEW ON" else "✏️ EDIT TEMPLATE",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLivePreviewEnabled) ElectricGreen else TextMuted
                            )
                        }
                    }
                }

                // Variable Tags Bar & Quick Insert Chips
                Text(
                    text = "Klik variabel di bawah untuk menyisipkan ke dalam teks draf:",
                    fontFamily = CyberMonospace,
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tags = listOf(
                        "{{email}}" to "Email Prospek",
                        "{{domain}}" to "Domain Perusahaan",
                        "{{category}}" to "Kategori/Sektor",
                        "{{phone}}" to "Nomor Telepon",
                        "{{sender_name}}" to "Nama Pengirim",
                        "{{company}}" to "Nama Brand",
                        "{{date}}" to "Tanggal Hari Ini"
                    )
                    items(tags) { (tag, label) ->
                        Surface(
                            onClick = {
                                bodyTemplate += " $tag"
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = NeonMagenta.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = tag,
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = NeonMagenta,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLivePreviewEnabled) {
                    // LIVE PREVIEW SIMULATOR VIEW MODE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.CyberDarkBackground, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, ElectricGreen, shape = RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👁️ LIVE RECIPIENT PREVIEW (${previewRecipientIndex + 1}/${targetEmails.size.coerceAtLeast(1)})",
                                fontFamily = CyberMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricGreen
                            )

                            if (targetEmails.size > 1) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = { previewRecipientIndex = (previewRecipientIndex - 1 + targetEmails.size) % targetEmails.size },
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyberSurface
                                    ) {
                                        Text("< PREV", fontFamily = CyberMonospace, fontSize = 9.sp, color = NeonCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        onClick = { previewRecipientIndex = (previewRecipientIndex + 1) % targetEmails.size },
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyberSurface
                                    ) {
                                        Text("NEXT >", fontFamily = CyberMonospace, fontSize = 9.sp, color = NeonCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val renderedSubject = renderTemplate(subjectTemplate, currentPreviewTarget, senderName)
                        val renderedBody = renderTemplate(bodyTemplate, currentPreviewTarget, senderName)

                        Text(
                            text = "TO: ${currentPreviewTarget.email}",
                            fontFamily = CyberMonospace,
                            fontSize = 10.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = "SUBJECT: $renderedSubject",
                            fontFamily = CyberMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(TextMuted.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = renderedBody,
                            fontFamily = CyberMonospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Rendered Text Button
                        Surface(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Rendered Email", "Subject: $renderedSubject\n\n$renderedBody")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied rendered email to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = ElectricGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ElectricGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY RENDERED DRAFT", fontFamily = CyberMonospace, fontSize = 9.sp, color = ElectricGreen)
                            }
                        }
                    }
                } else {
                    // EDIT TEMPLATE MODE
                    OutlinedTextField(
                        value = subjectTemplate,
                        onValueChange = { subjectTemplate = it },
                        label = { Text("Subject Line Template", fontFamily = CyberMonospace, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = NeonMagenta.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = bodyTemplate,
                        onValueChange = { bodyTemplate = it },
                        label = { Text("Email Body Template", fontFamily = CyberMonospace, fontSize = 10.sp) },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = NeonMagenta.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // DRAFT ANALYTICS & HEALTH PANEL
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurface, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$wordCount Words • $charCount Chars • ~$estReadTimeSeconds sec read",
                            fontFamily = CyberMonospace,
                            fontSize = 9.sp,
                            color = NeonCyan
                        )
                    }

                    if (detectedSpamWords.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Spam Warning", tint = CyberAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Spam words: ${detectedSpamWords.joinToString()}",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Health OK", tint = ElectricGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Deliverability Health: HIGH",
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = ElectricGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 3: SERVER CREDENTIALS & HANDSHAKE TEST
        CyberCard(
            borderColor = NeonCyan,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚙️ SMTP SERVER CREDENTIALS & PRESETS",
                    fontFamily = CyberMonospace,
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Feature 1 UI: Multi-Sender SMTP Rotation Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("🔄 Multi-Sender SMTP Rotation", fontFamily = CyberMonospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricGreen)
                        Text("Load balancing pengiriman lewat beberapa akun SMTP", fontFamily = CyberMonospace, fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = enableRotation,
                        onCheckedChange = { enableRotation = it }
                    )
                }

                if (enableRotation) {
                    Text("Pilih akun SMTP tersimpan untuk dirotasi:", fontFamily = CyberMonospace, fontSize = 10.sp, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        items(savedProfiles) { prof ->
                            val isSelected = selectedRotationProfiles.contains(prof)
                            Surface(
                                onClick = {
                                    if (isSelected) selectedRotationProfiles.remove(prof) else selectedRotationProfiles.add(prof)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricGreen.copy(alpha = 0.2f) else CyberSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ElectricGreen else TextMuted)
                            ) {
                                Text(
                                    text = "${if (isSelected) "✓ " else ""}${prof.profileName} (${prof.username})",
                                    fontFamily = CyberMonospace,
                                    fontSize = 9.sp,
                                    color = if (isSelected) ElectricGreen else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Server Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaultServerPresets) { p ->
                        Surface(
                            onClick = {
                                host = p.host
                                portStr = p.port.toString()
                                Toast.makeText(context, "Server preset loaded: ${p.profileName}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "⚡ ${p.profileName}",
                                fontFamily = CyberMonospace,
                                fontSize = 10.sp,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(savedProfiles) { sp ->
                        Surface(
                            onClick = {
                                host = sp.host
                                portStr = sp.port.toString()
                                username = sp.username
                                password = sp.password
                                senderName = sp.senderName
                                Toast.makeText(context, "Loaded server profile: ${sp.profileName}", Toast.LENGTH_SHORT).show()
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteProfile(sp) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("SMTP Host Server", fontFamily = CyberMonospace, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port", fontFamily = CyberMonospace, fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Sender Email / Username", fontFamily = CyberMonospace, fontSize = 10.sp) },
                    placeholder = { Text("sales@company.com", fontFamily = CyberMonospace, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("SMTP Password / App Password", fontFamily = CyberMonospace, fontSize = 10.sp) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Display Name", fontFamily = CyberMonospace, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Delay Throttling Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ANTI-SPAM SENDING DELAY:", fontFamily = CyberMonospace, fontSize = 10.sp, color = TextSecondary)
                    Text("${sendingDelaySeconds.toInt()}s per email", fontFamily = CyberMonospace, fontSize = 10.sp, color = ElectricGreen, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = sendingDelaySeconds,
                    onValueChange = { sendingDelaySeconds = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricGreen,
                        activeTrackColor = ElectricGreen,
                        inactiveTrackColor = CyberSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Test Connection Button
                Surface(
                    onClick = {
                        if (host.isBlank()) {
                            Toast.makeText(context, "Enter SMTP Host first", Toast.LENGTH_SHORT).show()
                            return@Surface
                        }
                        coroutineScope.launch {
                            isTestingHandshake = true
                            handshakeResult = "Pinging $host:${portStr.toIntOrNull() ?: 587}..."
                            val port = portStr.toIntOrNull() ?: 587
                            val success = withContext(Dispatchers.IO) {
                                try {
                                    Socket().use { socket ->
                                        socket.connect(InetSocketAddress(host, port), 5000)
                                        socket.isConnected
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            isTestingHandshake = false
                            handshakeResult = if (success) "✅ CONNECTION SUCCESSFUL! Host $host:$port is reachable." else "❌ CONNECTION FAILED! Host $host:$port could not be reached."
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = "Test Connection", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTestingHandshake) "TESTING HANDSHAKE..." else "🔌 TEST CONNECTION & HANDSHAKE",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }

                if (handshakeResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = handshakeResult!!,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = if (handshakeResult!!.contains("SUCCESSFUL")) ElectricGreen else NeonMagenta
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FEATURE 2 CARD: AUTOMATED FOLLOW-UP SEQUENCES (Pengiriman Berjenjang)
        CyberCard(
            borderColor = CyberAmber,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("⏳ Automated Follow-Up Sequence (Pengiriman Berjenjang)", fontFamily = CyberMonospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                        Text("Otomatisasi urutan email berjenjang (Pitch -> Follow Up -> Breakup)", fontFamily = CyberMonospace, fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = enableSequence,
                        onCheckedChange = { enableSequence = it }
                    )
                }

                if (enableSequence) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sequence Step 1: Initial Pitch (Day 0)", fontFamily = CyberMonospace, fontSize = 10.sp, color = ElectricGreen)
                    OutlinedTextField(
                        value = step1Subject,
                        onValueChange = { step1Subject = it },
                        label = { Text("Subject Step 1", fontFamily = CyberMonospace, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sequence Step 2: Gentle Follow-Up (+3 Days)", fontFamily = CyberMonospace, fontSize = 10.sp, color = CyberAmber)
                    OutlinedTextField(
                        value = step2Subject,
                        onValueChange = { step2Subject = it },
                        label = { Text("Subject Step 2", fontFamily = CyberMonospace, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sequence Step 3: Breakup Email (+7 Days)", fontFamily = CyberMonospace, fontSize = 10.sp, color = NeonMagenta)
                    OutlinedTextField(
                        value = step3Subject,
                        onValueChange = { step3Subject = it },
                        label = { Text("Subject Step 3", fontFamily = CyberMonospace, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FEATURE 3 CARD: UNSUBSCRIBE / OPT-OUT FOOTER GENERATOR
        CyberCard(
            borderColor = NeonCyan,
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("🔕 Unsubscribe / Opt-Out Footer Generator", fontFamily = CyberMonospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Text("Kepatuhan CAN-SPAM / GDPR dengan footer opt-out otomatis", fontFamily = CyberMonospace, fontSize = 9.sp, color = TextMuted)
                    }
                    androidx.compose.material3.Switch(
                        checked = enableOptOutFooter,
                        onCheckedChange = { enableOptOutFooter = it }
                    )
                }

                if (enableOptOutFooter) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "REPLY_UNSUBSCRIBE" to "Reply Opt-Out",
                            "LINK" to "Unsubscribe Link",
                            "CUSTOM" to "Custom Text"
                        ).forEach { (type, label) ->
                            val isSel = optOutType == type
                            Surface(
                                onClick = { optOutType = type },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) NeonCyan.copy(alpha = 0.2f) else CyberSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else TextMuted),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = CyberMonospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NeonCyan else TextMuted,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    if (optOutType == "CUSTOM") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customOptOutText,
                            onValueChange = { customOptOutText = it },
                            label = { Text("Custom Opt-Out Footer Text", fontFamily = CyberMonospace, fontSize = 10.sp) },
                            placeholder = { Text("Example: To opt out of future mailings, reply 'REMOVE'.", fontFamily = CyberMonospace, fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Preview Generated Footer:", fontFamily = CyberMonospace, fontSize = 9.sp, color = TextMuted)
                    val sampleConfig = SmtpConfig(enableOptOutFooter = true, optOutType = optOutType, customOptOutText = customOptOutText)
                    val dummyTarget = ExtractedEmail(email = "prospect@targetdomain.com", domain = "targetdomain.com", category = "BUSINESS")
                    Text(
                        text = sampleConfig.generateOptOutFooter(dummyTarget),
                        fontFamily = CyberMonospace,
                        fontSize = 9.sp,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: LAUNCH CAMPAIGN ACTION BUTTON
        Surface(
            onClick = {
                if (username.isBlank()) {
                    Toast.makeText(context, "Enter your Sender Username / Email first!", Toast.LENGTH_SHORT).show()
                    return@Surface
                }
                if (targetEmails.isEmpty()) {
                    Toast.makeText(context, "No target emails in database to send campaign to!", Toast.LENGTH_SHORT).show()
                    return@Surface
                }
                val portInt = portStr.toIntOrNull() ?: 587
                val sequenceStepsToRun = if (enableSequence) listOf(
                    com.example.data.smtp.SequenceStep(1, "Step 1: Initial Pitch", 0, step1Subject, bodyTemplate),
                    com.example.data.smtp.SequenceStep(2, "Step 2: Gentle Follow-Up", 3, step2Subject, "Hi {{email}},\n\nFollowing up on my previous email regarding {{domain}}...\n\nBest,\n{{sender_name}}"),
                    com.example.data.smtp.SequenceStep(3, "Step 3: Breakup Email", 7, step3Subject, "Hi {{email}},\n\nAssuming this isn't a priority right now...")
                ) else emptyList()

                val cfg = SmtpConfig(
                    host = host,
                    port = portInt,
                    username = username,
                    password = password,
                    senderName = senderName,
                    subjectTemplate = subjectTemplate,
                    bodyTemplate = bodyTemplate,
                    minDelaySeconds = sendingDelaySeconds.toInt(),
                    maxDelaySeconds = sendingDelaySeconds.toInt() + 3,
                    enableSenderRotation = enableRotation,
                    rotationProfiles = if (enableRotation && selectedRotationProfiles.isNotEmpty()) selectedRotationProfiles else emptyList(),
                    enableFollowUpSequence = enableSequence,
                    sequenceSteps = sequenceStepsToRun,
                    enableOptOutFooter = enableOptOutFooter,
                    optOutType = optOutType,
                    customOptOutText = customOptOutText
                )
                onStartCampaign(cfg)
                Toast.makeText(context, "🚀 Bulk SMTP Campaign Launched for ${targetEmails.size} targets!", Toast.LENGTH_LONG).show()
            },
            shape = RoundedCornerShape(8.dp),
            color = if (username.isBlank() || targetEmails.isEmpty()) TextMuted else ElectricGreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = CyberBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (progress.isSending) "CAMPAIGN RUNNING..." else "🚀 LAUNCH BULK SMTP CAMPAIGN",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyberBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
