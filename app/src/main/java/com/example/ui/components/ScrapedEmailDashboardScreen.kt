package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScrapedEmail
import com.example.ui.theme.*
import com.example.util.CyberSoundFX
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BulkExporter {
    fun exportToCsv(context: Context, emails: List<ScrapedEmail>): File? {
        val sb = StringBuilder()
        sb.append("ID,Email,Domain,Category,SourceURL,Phone,Social,MxVerified,MxStatus,LeadScore,IndustryTag,Timestamp\n")
        emails.forEach { item ->
            val escapedSource = "\"${item.sourceUrl.replace("\"", "\"\"")}\""
            sb.append("${item.id},\"${item.email}\",\"${item.domain}\",\"${item.category}\",$escapedSource,\"${item.phone}\",\"${item.social}\",${item.isMxVerified},\"${item.mxStatus}\",${item.leadScore},\"${item.industryTag}\",${item.timestamp}\n")
        }
        val csvStr = sb.toString()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Exported CSV", csvStr)
        clipboard.setPrimaryClip(clip)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, "scraped_emails_${System.currentTimeMillis()}.csv")
        return try {
            file.writeText(csvStr)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportToJson(context: Context, emails: List<ScrapedEmail>): File? {
        val jsonArray = JSONArray()
        emails.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("email", item.email)
            obj.put("domain", item.domain)
            obj.put("category", item.category)
            obj.put("sourceUrl", item.sourceUrl)
            obj.put("phone", item.phone)
            obj.put("social", item.social)
            obj.put("isMxVerified", item.isMxVerified)
            obj.put("mxStatus", item.mxStatus)
            obj.put("leadScore", item.leadScore)
            obj.put("industryTag", item.industryTag)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        val jsonStr = jsonArray.toString(2)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Exported JSON", jsonStr)
        clipboard.setPrimaryClip(clip)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, "scraped_emails_${System.currentTimeMillis()}.json")
        return try {
            file.writeText(jsonStr)
            file
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapedEmailDashboardScreen(
    scrapedEmails: List<ScrapedEmail>,
    modifier: Modifier = Modifier,
    onDeleteEmail: (ScrapedEmail) -> Unit = {},
    onDeleteMultiple: (List<ScrapedEmail>) -> Unit = {},
    onClearAll: () -> Unit = {},
    onVerifyMx: () -> Unit = {},
    onSendToSmtp: (List<ScrapedEmail>) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    val categories = remember { listOf("ALL", "GMAIL", "YAHOO", "OUTLOOK", "BUSINESS", "EDU", "GOV", "OTHER") }

    // Filter emails based on category and search query
    val filteredEmails = remember(scrapedEmails, searchQuery, selectedCategory) {
        scrapedEmails.filter { item ->
            val matchesCategory = if (selectedCategory == "ALL") true else item.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = if (searchQuery.isBlank()) true else {
                item.email.contains(searchQuery, ignoreCase = true) ||
                item.domain.contains(searchQuery, ignoreCase = true) ||
                item.sourceUrl.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesQuery
        }
    }

    val selectedItems = remember(filteredEmails, selectedIds) {
        filteredEmails.filter { selectedIds.contains(it.id) }
    }

    val isAllSelected = remember(filteredEmails, selectedIds) {
        filteredEmails.isNotEmpty() && filteredEmails.all { selectedIds.contains(it.id) }
    }

    val totalCount = scrapedEmails.size
    val mxVerifiedCount = remember(scrapedEmails) { scrapedEmails.count { it.isMxVerified && it.mxStatus == "VALID" } }
    val avgScore = remember(scrapedEmails) {
        if (scrapedEmails.isEmpty()) 0 else scrapedEmails.map { it.calculateScore() }.average().toInt()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBackground)
            .padding(12.dp)
            .testTag("scraped_email_dashboard_screen")
    ) {
        // HUD Stats Summary Banner
        CyberCard(
            borderColor = NeonCyan.copy(alpha = 0.6f),
            backgroundColor = CyberSurface,
            cutCornerSize = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Database",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROOM DATABASE SCRAPED CONTACTS",
                            fontFamily = CyberMonospace,
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "TOTAL: $totalCount",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "MX VALID: $mxVerifiedCount",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = ElectricGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "AVG SCORE: $avgScore/100",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = CyberAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (totalCount > 0) {
                    IconButton(
                        onClick = {
                            CyberSoundFX.playClickSound()
                            onClearAll()
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.testTag("clear_all_scraped_emails_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = NeonMagenta
                        )
                    }
                }
            }
        }

        // Search & Filter Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search email, domain, or URL...", color = TextMuted, fontSize = 12.sp, fontFamily = CyberMonospace) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scraped_email_search_input")
            )
        }

        // Category Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                val chipColor = when (cat) {
                    "GMAIL" -> Color(0xFFEA4335)
                    "YAHOO" -> Color(0xFF6001D2)
                    "OUTLOOK" -> Color(0xFF0078D4)
                    "BUSINESS" -> ElectricGreen
                    "EDU" -> NeonCyan
                    "GOV" -> CyberAmber
                    else -> NeonMagenta
                }

                Surface(
                    onClick = {
                        CyberSoundFX.playClickSound()
                        selectedCategory = cat
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.25f) else CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) chipColor else CyberBorder
                    ),
                    modifier = Modifier.testTag("category_chip_$cat")
                ) {
                    Text(
                        text = cat,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) chipColor else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bulk Operations & Multi-select Toolbar
        if (filteredEmails.isNotEmpty()) {
            CyberCard(
                borderColor = if (selectedIds.isNotEmpty()) NeonCyan else CyberBorder,
                backgroundColor = CyberSurface,
                cutCornerSize = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multi-select status & Select All checkbox
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { checked ->
                                    CyberSoundFX.playClickSound()
                                    selectedIds = if (checked) {
                                        filteredEmails.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonCyan,
                                    uncheckedColor = TextMuted
                                ),
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("select_all_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedIds.isEmpty()) "ALL (${filteredEmails.size})" else "SELECTED (${selectedIds.size}/${filteredEmails.size})",
                                fontFamily = CyberMonospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedIds.isNotEmpty()) NeonCyan else TextSecondary
                            )
                        }

                        // Bulk Actions Buttons Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Direct integration: Send to SMTP Campaign Button
                            Button(
                                onClick = {
                                    CyberSoundFX.playSuccessSound()
                                    val targetList = if (selectedItems.isNotEmpty()) selectedItems else filteredEmails
                                    onSendToSmtp(targetList)
                                    Toast.makeText(
                                        context,
                                        "Targeted ${targetList.size} contacts for SMTP Campaign!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("send_to_smtp_campaign_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send to SMTP",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SMTP CAMPAIGN",
                                    fontFamily = CyberMonospace,
                                    fontSize = 10.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // CSV Export
                            IconButton(
                                onClick = {
                                    CyberSoundFX.playSuccessSound()
                                    val targetList = if (selectedItems.isNotEmpty()) selectedItems else filteredEmails
                                    val file = BulkExporter.exportToCsv(context, targetList)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported ${targetList.size} CSV! Copied to clipboard", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Exported ${targetList.size} CSV to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("bulk_export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Export CSV",
                                    tint = ElectricGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // JSON Export
                            IconButton(
                                onClick = {
                                    CyberSoundFX.playSuccessSound()
                                    val targetList = if (selectedItems.isNotEmpty()) selectedItems else filteredEmails
                                    val file = BulkExporter.exportToJson(context, targetList)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported ${targetList.size} JSON! Copied to clipboard", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Exported ${targetList.size} JSON to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("bulk_export_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Export JSON",
                                    tint = CyberAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Bulk Delete Selected
                            if (selectedIds.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        CyberSoundFX.playClickSound()
                                        onDeleteMultiple(selectedItems)
                                        selectedIds = emptySet()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("bulk_delete_selected_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = NeonMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of Scraped Emails using LazyColumn
        if (filteredEmails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CyberCard(
                    borderColor = CyberBorder,
                    backgroundColor = CyberSurface,
                    cutCornerSize = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailUnread,
                            contentDescription = "No Emails",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO SCRAPED CONTACTS FOUND",
                            fontFamily = CyberMonospace,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try clearing your search query filter." else "Launch the crawler or dork engine to capture contacts into Room DB.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("scraped_emails_lazy_column")
            ) {
                items(
                    items = filteredEmails,
                    key = { item -> item.id }
                ) { item ->
                    val isChecked = selectedIds.contains(item.id)
                    ScrapedEmailItemCard(
                        item = item,
                        isSelected = isChecked,
                        onToggleSelect = {
                            selectedIds = if (isChecked) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        },
                        onCopy = { emailStr ->
                            CyberSoundFX.playSuccessSound()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied Email", emailStr)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied: $emailStr", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            CyberSoundFX.playClickSound()
                            onDeleteEmail(item)
                            selectedIds = selectedIds - item.id
                        },
                        onSendSingleToSmtp = {
                            CyberSoundFX.playSuccessSound()
                            onSendToSmtp(listOf(item))
                            Toast.makeText(context, "Targeted ${item.email} for SMTP Campaign!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScrapedEmailItemCard(
    item: ScrapedEmail,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit,
    onSendSingleToSmtp: () -> Unit
) {
    val categoryColor = when (item.category.uppercase()) {
        "GMAIL" -> Color(0xFFEA4335)
        "YAHOO" -> Color(0xFF6001D2)
        "OUTLOOK" -> Color(0xFF0078D4)
        "BUSINESS" -> ElectricGreen
        "EDU" -> NeonCyan
        "GOV" -> CyberAmber
        else -> TextSecondary
    }

    val score = item.calculateScore()
    val scoreColor = when {
        score >= 80 -> ElectricGreen
        score >= 50 -> CyberAmber
        else -> NeonMagenta
    }

    CyberCard(
        borderColor = if (isSelected) NeonCyan else categoryColor.copy(alpha = 0.4f),
        backgroundColor = if (isSelected) CyberSurface.copy(alpha = 0.95f) else CyberSurface,
        cutCornerSize = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("scraped_email_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Checkbox + Email + Score Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonCyan,
                            uncheckedColor = TextMuted
                        ),
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("item_checkbox_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (item.isMxVerified && item.mxStatus == "VALID") ElectricGreen else CyberAmber,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.email,
                        fontFamily = CyberMonospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Score Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = scoreColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "SCORE: $score",
                        fontFamily = CyberMonospace,
                        fontSize = 9.sp,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Row: Category, Domain, Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontFamily = CyberMonospace,
                            fontSize = 9.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = item.domain,
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Direct send button to SMTP Campaign
                    IconButton(
                        onClick = onSendSingleToSmtp,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("send_single_to_smtp_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send to Campaign",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = { onCopy(item.email) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Email",
                            tint = ElectricGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Email",
                            tint = NeonMagenta,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (item.sourceUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SRC: ${item.sourceUrl}",
                    fontFamily = CyberMonospace,
                    fontSize = 9.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
