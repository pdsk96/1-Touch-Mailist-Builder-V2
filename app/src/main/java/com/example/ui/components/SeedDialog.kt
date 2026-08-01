package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberMonospace
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CyberSoundFX

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeedConfigView(
    seedInputUrl: String,
    customSeeds: List<String>,
    targetRegion: String,
    targetIndustry: String,
    speedMode: String = "BALANCED",
    blacklistInput: String = "",
    blacklistKeywords: List<String> = emptyList(),
    isWakeLockEnabled: Boolean = true,
    isOcrEnabled: Boolean = true,
    isServiceRunning: Boolean = false,
    dorkKeywordInput: String = "",
    cronState: com.example.data.scheduler.CronScheduleState = com.example.data.scheduler.CronScheduleState(),
    onToggleWakeLock: () -> Unit = {},
    onToggleOcr: () -> Unit = {},
    onSeedInputChange: (String) -> Unit,
    onAddSeed: () -> Unit,
    onRemoveSeed: (String) -> Unit,
    onRegionSelected: (String) -> Unit,
    onIndustrySelected: (String) -> Unit,
    onSpeedModeSelected: (String) -> Unit = {},
    onBlacklistInputChange: (String) -> Unit = {},
    onAddBlacklist: () -> Unit = {},
    onRemoveBlacklist: (String) -> Unit = {},
    onDorkKeywordChange: (String) -> Unit = {},
    onExecuteDork: () -> Unit = {},
    onSetCronScheduler: (Boolean, Int) -> Unit = { _, _ -> },
    onClearDatabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val regions = listOf(
        "ALL",
        "INDONESIA (.id)",
        "USA / GLOBAL",
        "EUROPE",
        "JAPAN (.jp)",
        "SINGAPORE (.sg)"
    )

    val industries = listOf(
        "ALL",
        "GAME ONLINE",
        "E-COMMERCE",
        "GOVERNMENT",
        "EDUCATION",
        "FINTECH & BIZ"
    )

    val speedModes = listOf(
        "ULTRA" to "100ms (High Speed)",
        "BALANCED" to "400ms (Optimal)",
        "STEALTH" to "1000ms (Anti-Block)"
    )

    val presets = listOf(
        "https://news.ycombinator.com",
        "https://github.com/trending",
        "https://slashdot.org",
        "https://dev.to",
        "https://medium.com",
        "https://producthunt.com",
        "https://indiehackers.com",
        "https://techcrunch.com"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "TARGET SEED & FILTER CONFIG",
            fontFamily = CyberMonospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = NeonCyan,
            letterSpacing = 0.5.sp
        )

        Text(
            text = "Set region, industry niche, and background wake lock mode. Engines filter out fake emails automatically.",
            fontFamily = CyberMonospace,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // 0. GOOGLE / BING SEARCH DORK ENGINE (AUTOMATIC LEAD DISCOVERY)
        CyberCard(
            borderColor = NeonCyan,
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🌐 DORK SEARCH ENGINE (AUTO LEAD DISCOVERY)",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }

                Text(
                    text = "Type business niche keywords or choose a preset from the Dork Library below. Engine fetches search results and injects crawler seeds automatically.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // Dork Preset Library Chips
                Text(
                    text = "🔍 BUILT-IN DORK PRESET LIBRARY",
                    fontFamily = CyberMonospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val dorkPresets = listOf(
                    "💼 B2B CEO" to "\"CEO\" \"contact\"",
                    "🛍️ Shopify" to "\"contact us\" \"myshopify.com\"",
                    "🏠 Real Estate" to "\"real estate agency\" \"email\"",
                    "💻 SaaS Founders" to "\"SaaS founder\" \"contact\"",
                    "📣 Marketing" to "\"digital agency\" \"contact us\"",
                    "🎮 Game Studios" to "\"game publisher\" \"contact\"",
                    "🇮🇩 Indo PT" to "\"kontak kami\" \"pt\""
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(dorkPresets.size) { idx ->
                        val (label, query) = dorkPresets[idx]
                        Surface(
                            onClick = {
                                CyberSoundFX.playClickSound()
                                onDorkKeywordChange(query)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CyberSurface,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = label,
                                fontFamily = CyberMonospace,
                                fontSize = 9.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dorkKeywordInput,
                        onValueChange = onDorkKeywordChange,
                        placeholder = {
                            Text("e.g. \"contact us\" \"e-commerce\"", fontFamily = CyberMonospace, fontSize = 11.sp, color = TextMuted)
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
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    CyberButton(
                        text = "DISCOVER",
                        icon = Icons.Default.Add,
                        accentColor = NeonCyan,
                        onClick = {
                            CyberSoundFX.playScanPulseSound()
                            onExecuteDork()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 0B. CRON TASK AUTO-SCHEDULER CARD
        CyberCard(
            borderColor = CyberAmber,
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⏱️ CRON AUTO-SCHEDULER",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberAmber
                        )
                    }

                    Switch(
                        checked = cronState.isEnabled,
                        onCheckedChange = { onSetCronScheduler(it, cronState.intervalHours) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberAmber,
                            checkedTrackColor = CyberAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurface
                        )
                    )
                }

                Text(
                    text = if (cronState.isEnabled)
                        "SCHEDULED: Runs background scraping job every ${cronState.intervalHours} hour(s). Next run: ${cronState.nextRunFormatted}"
                    else "DISABLED: Enable to schedule automatic background scraping periodically.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                if (cronState.isEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 6, 12, 24).forEach { hours ->
                            val isSelected = cronState.intervalHours == hours
                            Surface(
                                onClick = { onSetCronScheduler(true, hours) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberAmber.copy(alpha = 0.25f) else CyberSurface,
                                border = BorderStroke(1.dp, if (isSelected) CyberAmber else CyberAmber.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${hours}h",
                                    fontFamily = CyberMonospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = if (isSelected) CyberAmber else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 0. WAKE LOCK & BACKGROUND EXECUTION CARD
        CyberCard(
            borderColor = if (isWakeLockEnabled) ElectricGreen.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.4f),
            backgroundColor = CyberSurface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isWakeLockEnabled) ElectricGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BACKGROUND WAKE LOCK MODE",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isWakeLockEnabled) ElectricGreen else TextMuted
                        )
                    }
                    Text(
                        text = if (isWakeLockEnabled)
                            "ACTIVE ${if (isServiceRunning) "[SERVICE RUNNING]" else "[STANDBY]"}: Scraper keeps running continuously when app is minimized or screen turns off."
                        else "DISABLED: Scraper pauses when app enters background.",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Switch(
                    checked = isWakeLockEnabled,
                    onCheckedChange = { onToggleWakeLock() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ElectricGreen,
                        checkedTrackColor = ElectricGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 0C. OCR IMAGE & MEDIA TEXT RECOGNITION SCANNER CARD
        CyberCard(
            borderColor = if (isOcrEnabled) NeonMagenta.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.4f),
            backgroundColor = CyberSurface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (isOcrEnabled) NeonMagenta else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📷 OCR IMAGE & MEDIA SCANNER",
                            fontFamily = CyberMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isOcrEnabled) NeonMagenta else TextMuted
                        )
                    }
                    Text(
                        text = if (isOcrEnabled)
                            "ACTIVE: ML Kit Text Recognition scans image banners, flyers, Base64 payloads & graphics for embedded emails."
                        else "DISABLED: Scraper extracts emails from text, HTML & code streams only (faster).",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Switch(
                    checked = isOcrEnabled,
                    onCheckedChange = {
                        CyberSoundFX.playClickSound()
                        onToggleOcr()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonMagenta,
                        checkedTrackColor = NeonMagenta.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. SMART TARGET REGION FILTER
        CyberCard(
            borderColor = NeonCyan.copy(alpha = 0.5f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TARGET COUNTRY / REGION",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    regions.forEach { region ->
                        val isSelected = targetRegion.equals(region, ignoreCase = true)
                        Surface(
                            onClick = { onRegionSelected(region) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else CyberSurface,
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = region,
                                fontFamily = CyberMonospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. SMART INDUSTRY / NICHE FILTER
        CyberCard(
            borderColor = ElectricGreen.copy(alpha = 0.5f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TARGET INDUSTRY / NICHE",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ElectricGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    industries.forEach { ind ->
                        val isSelected = targetIndustry.equals(ind, ignoreCase = true)
                        Surface(
                            onClick = { onIndustrySelected(ind) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ElectricGreen.copy(alpha = 0.25f) else CyberSurface,
                            border = BorderStroke(1.dp, if (isSelected) ElectricGreen else ElectricGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = ind,
                                fontFamily = CyberMonospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = if (isSelected) ElectricGreen else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2B. CRAWLER SPEED / DELAY MODE SELECTOR
        CyberCard(
            borderColor = NeonMagenta.copy(alpha = 0.5f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SCRAPER SPEED / DELAY MODE",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeonMagenta
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    speedModes.forEach { (mode, desc) ->
                        val isSelected = speedMode.equals(mode, ignoreCase = true)
                        Surface(
                            onClick = { onSpeedModeSelected(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonMagenta.copy(alpha = 0.25f) else CyberSurface,
                            border = BorderStroke(1.dp, if (isSelected) NeonMagenta else NeonMagenta.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = mode,
                                    fontFamily = CyberMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) NeonMagenta else TextSecondary
                                )
                                Text(
                                    text = desc,
                                    fontFamily = CyberMonospace,
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2C. CUSTOM EXCLUSION / BLACKLIST MANAGER
        CyberCard(
            borderColor = CyberAmber.copy(alpha = 0.5f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CUSTOM DOMAIN / KEYWORD BLACKLIST",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CyberAmber
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = blacklistInput,
                        onValueChange = onBlacklistInputChange,
                        placeholder = {
                            Text("e.g. no-reply, spam.com", fontFamily = CyberMonospace, fontSize = 11.sp, color = TextMuted)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface,
                            focusedBorderColor = CyberAmber,
                            unfocusedBorderColor = CyberAmber.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    CyberButton(
                        text = "BLOCK",
                        icon = Icons.Default.Add,
                        accentColor = CyberAmber,
                        onClick = onAddBlacklist
                    )
                }

                if (blacklistKeywords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        blacklistKeywords.forEach { kw ->
                            Surface(
                                onClick = { onRemoveBlacklist(kw) },
                                shape = RoundedCornerShape(12.dp),
                                color = CyberAmber.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyberAmber)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "X $kw",
                                        fontFamily = CyberMonospace,
                                        fontSize = 10.sp,
                                        color = CyberAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. ENGINE RULES STATUS BADGES
        CyberCard(
            borderColor = NeonCyan.copy(alpha = 0.3f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTOMATIC QUALITY CONTROL RULES",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fake Email & Temp Domain Blocker Active (e.g. mailinator, tempmail, example@)",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strict Cross-Session Email & Domain Deduplication (Room DB Unique Index)",
                        fontFamily = CyberMonospace,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seed URL Input Box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = seedInputUrl,
                onValueChange = onSeedInputChange,
                placeholder = {
                    Text("e.g. https://domain.com/contact", fontFamily = CyberMonospace, fontSize = 12.sp, color = TextMuted)
                },
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan)
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
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            CyberButton(
                text = "INJECT",
                icon = Icons.Default.Add,
                accentColor = ElectricGreen,
                onClick = onAddSeed
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Quick Injectors
        Text(
            text = "RECOMMENDED SEED PRESETS:",
            fontFamily = CyberMonospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { seed ->
                Surface(
                    onClick = {
                        onSeedInputChange(seed)
                        onAddSeed()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = CyberSurface,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = seed.removePrefix("https://").removePrefix("www."),
                            fontFamily = CyberMonospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Seeds List
        Text(
            text = "ACTIVE INJECTED SEEDS (${customSeeds.size}):",
            fontFamily = CyberMonospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (customSeeds.isEmpty()) {
            CyberCard(
                borderColor = NeonCyan.copy(alpha = 0.2f),
                backgroundColor = CyberSurface
            ) {
                Text(
                    text = "No custom seeds injected. Engine will use selected target region/industry seeds.",
                    fontFamily = CyberMonospace,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                customSeeds.forEach { seed ->
                    CyberCard(
                        borderColor = ElectricGreen.copy(alpha = 0.4f),
                        backgroundColor = CyberSurface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = seed,
                                fontFamily = CyberMonospace,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onRemoveSeed(seed) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Seed",
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Danger Zone: Reset Database
        CyberCard(
            borderColor = NeonMagenta.copy(alpha = 0.6f),
            backgroundColor = CyberSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SYSTEM PURGE ZONE",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeonMagenta
                )
                Text(
                    text = "Clear all extracted emails and reset local database records.",
                    fontFamily = CyberMonospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                CyberButton(
                    text = "PURGE DATABASE",
                    icon = Icons.Default.Delete,
                    accentColor = NeonMagenta,
                    onClick = onClearDatabase
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
