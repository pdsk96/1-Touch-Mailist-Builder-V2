package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.CyberBgmPlayerWidget
import com.example.ui.components.CyberHudGrid
import com.example.ui.components.EmailListDrawerView
import com.example.ui.components.LiveTerminalLogView
import com.example.ui.components.OneTouchTriggerButton
import com.example.ui.components.ScrapedEmailDashboardScreen
import com.example.ui.components.SeedConfigView
import com.example.ui.components.SmtpWorkspaceView
import com.example.util.CyberBgmSynthesizer
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
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CrawlerViewModel

import com.example.ui.components.CyberHudNotificationBanner
import com.example.ui.components.CyberNotificationData
import com.example.ui.components.CyberNotificationType
import com.example.ui.components.CyberRadarView
import com.example.ui.components.CyberSplashScreen
import com.example.util.CyberSoundFX

@Composable
fun DashboardScreen(viewModel: CrawlerViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        CyberSplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredEmails by viewModel.filteredEmails.collectAsStateWithLifecycle()
    val allScrapedEmails by viewModel.allScrapedEmails.collectAsStateWithLifecycle()
    val smtpProgress by viewModel.smtpProgress.collectAsStateWithLifecycle()
    val smtpProfiles by viewModel.smtpProfiles.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var activeNotification by remember { mutableStateOf<CyberNotificationData?>(null) }

    // Sound effect on SMTP finished
    LaunchedEffect(smtpProgress.isSending) {
        if (!smtpProgress.isSending && smtpProgress.sentCount > 0) {
            CyberSoundFX.playSuccessSound()
            activeNotification = CyberNotificationData(
                title = "CAMPAIGN COMPLETE",
                message = "Sent ${smtpProgress.sentCount} emails (${smtpProgress.failCount} failed).",
                type = CyberNotificationType.SUCCESS
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CyberSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
            ) {
                CyberDrawerNavContent(
                    activeTab = uiState.activeTab,
                    emailCount = filteredEmails.size,
                    scrapedCount = allScrapedEmails.size,
                    onTabSelected = { tab ->
                        CyberSoundFX.playClickSound()
                        viewModel.setActiveTab(tab)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onCloseDrawer = {
                        CyberSoundFX.playClickSound()
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberDarkBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CyberTopBar(
                isRunning = uiState.stats.isRunning,
                lastCheckpointFile = uiState.lastCheckpointFile,
                onManualCheckpoint = {
                    CyberSoundFX.playSuccessSound()
                    viewModel.manualTriggerSaveCheckpoint()
                    activeNotification = CyberNotificationData(
                        title = "CHECKPOINT SAVED",
                        message = "Database snapshot saved successfully to local storage.",
                        type = CyberNotificationType.SUCCESS
                    )
                }
            )

            // Cyberpunk HUD Notification Overlay Banner
            CyberHudNotificationBanner(
                notification = activeNotification,
                onDismiss = { activeNotification = null }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                when (uiState.activeTab) {
                    0 -> {
                        // TAB 0: LIVE DASHBOARD - RADAR, CRAWLER & LIVE TERMINAL LOGS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CyberRadarView(
                                isRunning = uiState.stats.isRunning,
                                currentUrl = uiState.stats.currentUrl,
                                foundCount = uiState.stats.totalEmailsFound
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            CyberHudGrid(
                                totalEmails = uiState.stats.totalEmailsFound,
                                speedEmailPerMin = uiState.stats.speedEmailPerMin,
                                scannedPages = uiState.stats.scannedPagesCount,
                                activeThreads = uiState.stats.activeThreadsCount
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OneTouchTriggerButton(
                                isRunning = uiState.stats.isRunning,
                                onClick = {
                                    if (!uiState.stats.isRunning) {
                                        CyberSoundFX.playScanPulseSound()
                                        activeNotification = CyberNotificationData(
                                            title = "SCRAPER INITIALIZED",
                                            message = "Crawler threads spawned. Searching target leads...",
                                            type = CyberNotificationType.INFO
                                        )
                                    } else {
                                        CyberSoundFX.playAlertSound()
                                        activeNotification = CyberNotificationData(
                                            title = "SCRAPER HALTED",
                                            message = "Crawler engine paused by user signal.",
                                            type = CyberNotificationType.WARNING
                                        )
                                    }
                                    viewModel.toggleOneTouchCrawler()
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LiveTerminalLogView(
                                logs = uiState.logs,
                                currentCrawlingUrl = uiState.stats.currentUrl,
                                onClearLogs = {
                                    CyberSoundFX.playClickSound()
                                    viewModel.clearTerminalLogs()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }

                    1 -> {
                        // TAB 1: EXTRACTED MAILIST DATABASE
                        EmailListDrawerView(
                            emails = filteredEmails,
                            selectedCategory = uiState.selectedCategory,
                            searchQuery = uiState.searchQuery,
                            lastCheckpointFile = uiState.lastCheckpointFile,
                            isVerifyingMx = uiState.isVerifyingMx,
                            mxVerifyProgress = uiState.mxVerifyProgress,
                            smtpProgress = smtpProgress,
                            savedSmtpProfiles = smtpProfiles,
                            onSaveSmtpProfile = { viewModel.saveSmtpProfile(it) },
                            onDeleteSmtpProfile = { viewModel.deleteSmtpProfile(it) },
                            onExportAnalyticsPdf = { viewModel.exportAndShareAnalyticsPdf(it) },
                            onCategorySelected = { viewModel.setCategoryFilter(it) },
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onExportCsv = { viewModel.exportAndShareCsv(it) },
                            onExportJson = { viewModel.exportAndShareJson(it) },
                            onExportTxt = { viewModel.exportAndShareTxt(it) },
                            onExportAnalyticsHtml = { viewModel.exportAndShareAnalyticsHtml(it) },
                            onExportAnalyticsExcel = { viewModel.exportAndShareAnalyticsExcel(it) },
                            onCopyClipboard = { ctx, list -> viewModel.copyEmailsToClipboard(ctx, list) },
                            onVerifyMx = { viewModel.verifyMxDeliverability() },
                            onImportCsvText = { text, callback -> viewModel.importAndCleanCsv(text, callback) },
                            onStartSmtpCampaign = { cfg -> viewModel.startSmtpCampaign(cfg) },
                            onSyncWebhook = { url, cb -> viewModel.syncLeadsToWebhook(url, cb) },
                            onOpenSmtpWorkspace = { viewModel.setActiveTab(2) }
                        )
                    }

                    2 -> {
                        // TAB 2: DEDICATED SMTP CAMPAIGN WORKSPACE
                        SmtpWorkspaceView(
                            targetEmails = filteredEmails,
                            progress = smtpProgress,
                            savedProfiles = smtpProfiles,
                            onStartCampaign = { cfg -> viewModel.startSmtpCampaign(cfg) },
                            onSaveProfile = { viewModel.saveSmtpProfile(it) },
                            onDeleteProfile = { viewModel.deleteSmtpProfile(it) },
                            onEnrichProspects = { callback -> viewModel.enrichAllProspects(callback) },
                            onExportBackup = { passkey, callback -> viewModel.exportEncryptedBackup(passkey, callback) },
                            onRestoreBackup = { encryptedJson, passkey, callback -> viewModel.restoreEncryptedBackup(encryptedJson, passkey, callback) }
                        )
                    }

                    3 -> {
                        // TAB 3: SEED CONFIGURATION & TARGET FILTERS
                        SeedConfigView(
                            seedInputUrl = uiState.seedInputUrl,
                            customSeeds = uiState.customSeedsList,
                            targetRegion = uiState.targetRegion,
                            targetIndustry = uiState.targetIndustry,
                            speedMode = uiState.speedMode,
                            blacklistInput = uiState.blacklistInput,
                            blacklistKeywords = uiState.blacklistKeywords,
                            isWakeLockEnabled = uiState.isWakeLockEnabled,
                            isOcrEnabled = uiState.isOcrEnabled,
                            isServiceRunning = uiState.isServiceRunning,
                            dorkKeywordInput = uiState.dorkKeywordInput,
                            cronState = uiState.cronState,
                            onToggleWakeLock = { viewModel.toggleWakeLockSetting() },
                            onToggleOcr = { viewModel.toggleOcrSetting() },
                            onSeedInputChange = { viewModel.setSeedInputUrl(it) },
                            onAddSeed = { viewModel.addSeedUrl() },
                            onRemoveSeed = { viewModel.removeSeedUrl(it) },
                            onRegionSelected = { viewModel.setTargetRegion(it) },
                            onIndustrySelected = { viewModel.setTargetIndustry(it) },
                            onSpeedModeSelected = { viewModel.setSpeedMode(it) },
                            onBlacklistInputChange = { viewModel.setBlacklistInput(it) },
                            onAddBlacklist = { viewModel.addBlacklistKeyword() },
                            onRemoveBlacklist = { viewModel.removeBlacklistKeyword(it) },
                            onDorkKeywordChange = { viewModel.setDorkKeywordInput(it) },
                            onExecuteDork = { viewModel.executeDorkSearch() },
                            onSetCronScheduler = { enabled, hours -> viewModel.setCronScheduler(enabled, hours) },
                            onClearDatabase = { viewModel.clearDatabase() }
                        )
                    }

                    4 -> {
                        // TAB 4: ROOM DATABASE SCRAPED EMAIL DASHBOARD SCREEN
                        ScrapedEmailDashboardScreen(
                            scrapedEmails = allScrapedEmails,
                            onDeleteEmail = { viewModel.deleteScrapedEmail(it) },
                            onDeleteMultiple = { viewModel.deleteScrapedEmails(it) },
                            onClearAll = { viewModel.clearDatabase() },
                            onVerifyMx = { viewModel.verifyMxDeliverability() },
                            onSendToSmtp = { targetList ->
                                viewModel.setActiveTab(2) // Switch directly to SMTP Workspace tab
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberTopBar(
    isRunning: Boolean,
    lastCheckpointFile: String?,
    onManualCheckpoint: () -> Unit
) {
    val isBgmPlaying by CyberBgmSynthesizer.isPlaying.collectAsStateWithLifecycle()

    Surface(
        color = CyberDarkBackground,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyberAmber.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyberAmber),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "PDSK",
                                    fontFamily = CyberMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = CyberAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "1 TOUCH ",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "MAILIST",
                                fontFamily = CyberMonospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = NeonMagenta,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isRunning) ElectricGreen else CyberAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRunning) "CRAWLER RUNNING" else "TRIGGER READY",
                                fontFamily = CyberMonospace,
                                fontSize = 8.sp,
                                color = if (isRunning) ElectricGreen else NeonCyan.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Cyberpunk BGM Toggle Button
                    IconButton(
                        onClick = {
                            CyberSoundFX.playClickSound()
                            CyberBgmSynthesizer.togglePlayPause()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("cyber_topbar_bgm_button")
                    ) {
                        Icon(
                            imageVector = if (isBgmPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = "Toggle Cyberpunk BGM",
                            tint = if (isBgmPlaying) NeonCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onManualCheckpoint,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Checkpoint",
                            tint = ElectricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberDrawerNavContent(
    activeTab: Int,
    emailCount: Int,
    scrapedCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberSurface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -15f || dragAmount > 15f) {
                        onCloseDrawer()
                    }
                }
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyberAmber.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CyberAmber),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "PDSK",
                        fontFamily = CyberMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = CyberAmber,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "NAV MENU",
                    fontFamily = CyberMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NeonCyan
                )
            }

            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Hide Drawer",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = "👈 Usap untuk menyembunyikan",
            fontFamily = CyberMonospace,
            fontSize = 9.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        HorizontalDivider(
            color = NeonCyan.copy(alpha = 0.25f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Nav Item 0: DASHBOARD
        SideNavItem(
            icon = Icons.Default.Dashboard,
            label = "DASHBOARD",
            isSelected = activeTab == 0,
            isExpanded = true,
            onClick = { onTabSelected(0) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Nav Item 1: MAILIST
        SideNavItem(
            icon = Icons.Default.ListAlt,
            label = "MAILIST",
            badgeCount = emailCount,
            isSelected = activeTab == 1,
            isExpanded = true,
            onClick = { onTabSelected(1) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Nav Item 4: ROOM SCRAPED CONTACTS DB
        SideNavItem(
            icon = Icons.Default.Storage,
            label = "SCRAPED DB",
            badgeCount = scrapedCount,
            isSelected = activeTab == 4,
            isExpanded = true,
            onClick = { onTabSelected(4) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Nav Item 2: SMTP WORKSPACE
        SideNavItem(
            icon = Icons.Default.Email,
            label = "SMTP CAMPAIGN",
            isSelected = activeTab == 2,
            isExpanded = true,
            onClick = { onTabSelected(2) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Nav Item 3: TARGETS
        SideNavItem(
            icon = Icons.Default.Settings,
            label = "TARGETS",
            isSelected = activeTab == 3,
            isExpanded = true,
            onClick = { onTabSelected(3) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Elegant Cyberpunk Watermark - PDSK PROJECT 2026
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CyberSurface.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "PDSK Protection",
                        tint = NeonCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDSK PROJECT © 2026 // ALL RIGHTS RESERVED",
                        fontFamily = CyberMonospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary.copy(alpha = 0.9f),
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // Cyberpunk BGM Player Widget integrated at bottom of drawer
        CyberBgmPlayerWidget(
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SideNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int = 0,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, NeonCyan) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) NeonCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                if (badgeCount > 0 && !isExpanded) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricGreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                    ) {}
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    fontFamily = CyberMonospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (isSelected) NeonCyan else TextMuted,
                    modifier = Modifier.weight(1f)
                )

                if (badgeCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricGreen,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontFamily = CyberMonospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberBlack,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
