package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExtractedEmail
import com.example.ui.theme.CatBusinessColor
import com.example.ui.theme.CatEduColor
import com.example.ui.theme.CatGmailColor
import com.example.ui.theme.CatGovColor
import com.example.ui.theme.CatOtherColor
import com.example.ui.theme.CatOutlookColor
import com.example.ui.theme.CatYahooColor
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.util.CyberSoundFX

@Composable
fun EmailListDrawerView(
    emails: List<ExtractedEmail>,
    selectedCategory: String,
    searchQuery: String,
    lastCheckpointFile: String?,
    isVerifyingMx: Boolean = false,
    mxVerifyProgress: String? = null,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onExportCsv: ((Intent) -> Unit) -> Unit,
    onExportJson: ((Intent) -> Unit) -> Unit = {},
    onExportTxt: ((Intent) -> Unit) -> Unit = {},
    onExportAnalyticsPdf: ((Intent) -> Unit) -> Unit = {},
    onExportAnalyticsHtml: ((Intent) -> Unit) -> Unit = {},
    onExportAnalyticsExcel: ((Intent) -> Unit) -> Unit = {},
    onCopyClipboard: (Context, List<ExtractedEmail>) -> Unit = { _, _ -> },
    onVerifyMx: () -> Unit = {},
    onImportCsvText: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onStartSmtpCampaign: (com.example.data.smtp.SmtpConfig) -> Unit = {},
    savedSmtpProfiles: List<com.example.data.model.SmtpProfile> = emptyList(),
    onSaveSmtpProfile: (com.example.data.model.SmtpProfile) -> Unit = {},
    onDeleteSmtpProfile: (com.example.data.model.SmtpProfile) -> Unit = {},
    smtpProgress: com.example.data.smtp.SmtpProgress = com.example.data.smtp.SmtpProgress(),
    onSyncWebhook: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onOpenSmtpWorkspace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = listOf("ALL", "GMAIL", "YAHOO", "BUSINESS", "EDU", "GOV", "OUTLOOK", "OTHER")
    var showImportDialog by remember { mutableStateOf(false) }
    var showWebhookDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    if (showBatteryDialog) {
        BatteryOptimizationDialog(
            onDismiss = { showBatteryDialog = false }
        )
    }

    if (showImportDialog) {
        ImportCsvDialog(
            onDismiss = { showImportDialog = false },
            onImport = { csvText ->
                onImportCsvText(csvText) { toastMsg ->
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                    showImportDialog = false
                }
            }
        )
    }

    if (showWebhookDialog) {
        CloudWebhookDialog(
            targetEmails = emails,
            onDismiss = { showWebhookDialog = false },
            onSyncWebhook = { url, cb -> onSyncWebhook(url, cb) }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Toolbar Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EXTRACTED MAILIST (${emails.size})",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                )
                if (mxVerifyProgress != null) {
                    Text(
                        text = mxVerifyProgress,
                        fontFamily = CyberMonospace,
                        fontSize = 9.sp,
                        color = CyberAmber,
                        maxLines = 1
                    )
                } else if (lastCheckpointFile != null) {
                    Text(
                        text = "SAVED: $lastCheckpointFile",
                        fontFamily = CyberMonospace,
                        fontSize = 9.sp,
                        color = ElectricGreen,
                        maxLines = 1
                    )
                }
            }
        }

        // Action Toolbars Row 1: Operations (Import, MX Verify, Copy All)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Surface(
                onClick = {
                    CyberSoundFX.playClickSound()
                    showImportDialog = true
                },
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Import", tint = CyberAmber, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("IMPORT CSV", fontFamily = CyberMonospace, fontSize = 9.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    CyberSoundFX.playScanPulseSound()
                    onVerifyMx()
                },
                shape = RoundedCornerShape(6.dp),
                color = if (isVerifyingMx) CyberAmber.copy(alpha = 0.3f) else CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                ) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "MX Verify", tint = ElectricGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isVerifyingMx) "VERIFYING..." else "MX VERIFY", fontFamily = CyberMonospace, fontSize = 9.sp, color = ElectricGreen, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    CyberSoundFX.playSuccessSound()
                    onCopyClipboard(context, emails)
                },
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                ) {
                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY ALL", fontFamily = CyberMonospace, fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action Toolbars Row 2: Exports & Reports (CSV, EXCEL, PDF)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Surface(
                onClick = {
                    CyberSoundFX.playSuccessSound()
                    onExportCsv { shareIntent -> context.startActivity(Intent.createChooser(shareIntent, "Share Mailist CSV")) }
                },
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "CSV", tint = ElectricGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("EXPORT CSV", fontFamily = CyberMonospace, fontSize = 9.sp, color = ElectricGreen, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    CyberSoundFX.playSuccessSound()
                    onExportAnalyticsExcel { shareIntent -> context.startActivity(Intent.createChooser(shareIntent, "Share Excel Analytics Report")) }
                },
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text("📈 EXCEL REPORT", fontFamily = CyberMonospace, fontSize = 9.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    CyberSoundFX.playSuccessSound()
                    onExportAnalyticsPdf { shareIntent -> context.startActivity(Intent.createChooser(shareIntent, "Share Analytics Report (PDF)")) }
                },
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text("📊 PDF REPORT", fontFamily = CyberMonospace, fontSize = 9.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Primary Feature Action Bar: SMTP CAMPAIGN, WEBHOOK SYNC & AUTO-START
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Surface(
                onClick = { onOpenSmtpWorkspace() },
                shape = RoundedCornerShape(8.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = if (smtpProgress.isSending) "⚡ CAMPAIGN (${smtpProgress.sentCount}/${smtpProgress.total})" else "📧 SMTP WORKSPACE",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NeonMagenta
                    )
                }
            }

            Surface(
                onClick = { showWebhookDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = "☁️ SYNC",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                }
            }

            Surface(
                onClick = { showBatteryDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = "⚡ AUTO-START",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ElectricGreen
                    )
                }
            }
        }

        // Visual Category Analytics Distribution Bar
        if (emails.isNotEmpty()) {
            CategoryAnalyticsBar(emails = emails)
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text("Search emails or domains...", fontFamily = CyberMonospace, fontSize = 12.sp, color = TextMuted)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory.equals(category, ignoreCase = true)
                val badgeColor = getCategoryColor(category)

                Surface(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) badgeColor.copy(alpha = 0.25f) else CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) badgeColor else badgeColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = category,
                        fontFamily = CyberMonospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isSelected) badgeColor else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Email Items List
        if (emails.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                CyberCard(
                    borderColor = NeonCyan.copy(alpha = 0.5f),
                    backgroundColor = CyberSurface,
                    cutCornerSize = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DATABASE MAILIST MASIH KOSONG",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Belum ada email terdeteksi atau filter pencarian tidak cocok.",
                            fontFamily = CyberMonospace,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CyberButton(
                                text = "📥 IMPOR CSV",
                                icon = Icons.Default.Download,
                                accentColor = CyberAmber,
                                modifier = Modifier.weight(1f),
                                onClick = { showImportDialog = true }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(emails, key = { it.id }) { item ->
                    EmailItemRow(item = item)
                }
            }
        }
    }
}

@Composable
fun EmailItemRow(item: ExtractedEmail) {
    val context = LocalContext.current
    val categoryColor = getCategoryColor(item.category)
    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(item.timestamp))

    CyberCard(
        borderColor = categoryColor.copy(alpha = 0.4f),
        backgroundColor = CyberSurface,
        cutCornerSize = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CyberSoundFX.playClickSound()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Copied Email", item.email)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: ${item.email}", Toast.LENGTH_SHORT).show()
            }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.email,
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Lead Quality Score Badge (0-100)
                    val score = item.calculateScore()
                    val scoreColor = when {
                        score >= 80 -> ElectricGreen
                        score >= 60 -> CyberAmber
                        else -> TextMuted
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = scoreColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor)
                    ) {
                        Text(
                            text = "SCORE: $score",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // MX Deliverability Badge
                    if (item.isMxVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (item.mxStatus == "VALID") ElectricGreen.copy(alpha = 0.2f) else NeonMagenta.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (item.mxStatus == "VALID") ElectricGreen else NeonMagenta)
                        ) {
                            Text(
                                text = if (item.mxStatus == "VALID") "MX VALID" else "NO MX",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = if (item.mxStatus == "VALID") ElectricGreen else NeonMagenta,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Category Tag Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor)
                    ) {
                        Text(
                            text = item.category,
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Multi-Data Extractor Section: Phone/WA & Social Handles
            if (item.phone.isNotBlank() || item.social.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (item.phone.isNotBlank()) {
                        Surface(
                            onClick = {
                                val cleanNum = item.phone.replace("[^0-9+]".toRegex(), "")
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$cleanNum"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Opening WA: $cleanNum", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = ElectricGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "📱 WA/TEL: ${item.phone}",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = ElectricGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.social.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🔗 ${item.social}",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.sourceUrl,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }

                Text(
                    text = formattedTime,
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ImportCsvDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📥 IMPORT & MERGE CSV/TXT LIST",
                fontFamily = CyberMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CyberAmber
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste your email list or CSV content below. The cleaning engine will auto-extract emails, remove duplicates, filter out fakes, and merge with existing database.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                        Text("Paste CSV content or email list here...", fontFamily = CyberMonospace, fontSize = 10.sp, color = TextMuted)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        focusedBorderColor = CyberAmber,
                        unfocusedBorderColor = CyberAmber.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            CyberButton(
                text = "IMPORT & MERGE",
                icon = Icons.Default.Download,
                accentColor = CyberAmber,
                onClick = {
                    if (rawText.isNotBlank()) {
                        onImport(rawText)
                    }
                }
            )
        },
        dismissButton = {
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface
            ) {
                Text(
                    text = "CANCEL",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun CategoryAnalyticsBar(emails: List<ExtractedEmail>) {
    if (emails.isEmpty()) return
    val total = emails.size.toFloat()
    val counts = emails.groupBy { it.category }.mapValues { it.value.size }

    CyberCard(
        borderColor = NeonCyan.copy(alpha = 0.3f),
        backgroundColor = CyberSurface,
        cutCornerSize = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CATEGORY DISTRIBUTION ANALYTICS",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = "TOTAL: ${emails.size}",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = ElectricGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Multi-segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF101420), RoundedCornerShape(3.dp))
            ) {
                val catOrder = listOf("GMAIL", "BUSINESS", "EDU", "GOV", "YAHOO", "OUTLOOK", "OTHER")
                catOrder.forEach { cat ->
                    val count = counts[cat] ?: 0
                    if (count > 0) {
                        val weight = count / total
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weight)
                                .background(getCategoryColor(cat))
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.uppercase(Locale.ROOT)) {
        "GMAIL" -> CatGmailColor
        "YAHOO" -> CatYahooColor
        "OUTLOOK" -> CatOutlookColor
        "BUSINESS" -> CatBusinessColor
        "EDU" -> CatEduColor
        "GOV" -> CatGovColor
        "OTHER" -> CatOtherColor
        else -> NeonCyan
    }
}
