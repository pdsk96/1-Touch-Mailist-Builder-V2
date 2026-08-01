package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crawler.CrawlerStats
import com.example.data.crawler.LogEntry
import com.example.data.crawler.WebCrawlerEngine
import com.example.data.local.AppDatabase
import com.example.data.model.ExtractedEmail
import com.example.data.repository.EmailRepository
import com.example.service.CrawlerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: CrawlerStats = CrawlerStats(),
    val logs: List<LogEntry> = emptyList(),
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val activeTab: Int = 0, // 0: Live Dashboard, 1: Extracted Mailist, 2: Target Config
    val lastCheckpointFile: String? = null,
    val seedInputUrl: String = "",
    val customSeedsList: List<String> = emptyList(),
    val targetRegion: String = "ALL",
    val targetIndustry: String = "ALL",
    val speedMode: String = "BALANCED", // ULTRA, BALANCED, STEALTH
    val blacklistInput: String = "",
    val blacklistKeywords: List<String> = emptyList(),
    val isWakeLockEnabled: Boolean = true,
    val isOcrEnabled: Boolean = true,
    val isServiceRunning: Boolean = false,
    val dorkKeywordInput: String = "",
    val isVerifyingMx: Boolean = false,
    val mxVerifyProgress: String? = null,
    val cronState: com.example.data.scheduler.CronScheduleState = com.example.data.scheduler.CronScheduleState()
)

class CrawlerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = EmailRepository(db.emailDao(), application)

    private val _logsState = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _selectedCategoryState = MutableStateFlow("ALL")
    private val _searchQueryState = MutableStateFlow("")
    private val _activeTabState = MutableStateFlow(0)
    private val _seedInputState = MutableStateFlow("")
    private val _customSeedsState = MutableStateFlow<List<String>>(emptyList())
    private val _targetRegionState = MutableStateFlow("ALL")
    private val _targetIndustryState = MutableStateFlow("ALL")
    private val _speedModeState = MutableStateFlow("BALANCED")
    private val _blacklistInputState = MutableStateFlow("")
    private val _blacklistKeywordsState = MutableStateFlow<List<String>>(emptyList())
    private val _isWakeLockEnabledState = MutableStateFlow(true)
    private val _isOcrEnabledState = MutableStateFlow(true)

    private val _dorkKeywordInputState = MutableStateFlow("")
    private val _isVerifyingMxState = MutableStateFlow(false)
    private val _mxVerifyProgressState = MutableStateFlow<String?>(null)

    private val cronSchedulerManager = com.example.data.scheduler.CronSchedulerManager(application)
    val cronState = cronSchedulerManager.scheduleState

    private lateinit var crawlerEngine: WebCrawlerEngine

    val crawlerStats: StateFlow<CrawlerStats>
    val lastCheckpointFile: StateFlow<String?> = repository.lastSavedCheckpoint

    // Reactive email list from Room filtered by category & search query
    val filteredEmails: StateFlow<List<ExtractedEmail>>

    val allScrapedEmails: StateFlow<List<com.example.data.model.ScrapedEmail>> = repository.allScrapedEmailsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<DashboardUiState>

    init {
        crawlerEngine = WebCrawlerEngine(
            onEmailFound = { email ->
                viewModelScope.launch {
                    repository.saveExtractedEmail(email)
                }
            },
            onLog = { logEntry ->
                _logsState.update { current ->
                    // Keep last 100 logs for memory efficiency & lag-free UI
                    val updated = (listOf(logEntry) + current)
                    if (updated.size > 100) updated.take(100) else updated
                }
            }
        )

        crawlerStats = crawlerEngine.stats

        // Preload existing emails from Room DB for cross-session deduplication
        viewModelScope.launch {
            repository.allEmailsFlow.collect { emails ->
                val emailStrings = emails.map { it.email }
                crawlerEngine.preloadExistingEmails(emailStrings)
            }
        }

        filteredEmails = combine(
            _selectedCategoryState,
            _searchQueryState
        ) { category, query ->
            Pair(category, query)
        }.combine(repository.allEmailsFlow) { (category, query), emails ->
            emails.filter { item ->
                val categoryMatch = if (category == "ALL") true else item.category.equals(category, ignoreCase = true)
                val queryMatch = if (query.isBlank()) true else {
                    item.email.contains(query, ignoreCase = true) ||
                    item.domain.contains(query, ignoreCase = true) ||
                    item.sourceUrl.contains(query, ignoreCase = true)
                }
                categoryMatch && queryMatch
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        uiState = combine(
            crawlerStats,
            _logsState,
            _selectedCategoryState,
            _searchQueryState,
            _activeTabState
        ) { stats, logs, category, query, tab ->
            Tuple5(stats, logs, category, query, tab)
        }.combine(
            combine(
                _seedInputState,
                _customSeedsState,
                _targetRegionState,
                _targetIndustryState,
                _isWakeLockEnabledState
            ) { seed, custom, region, industry, wakeLock ->
                Tuple5(seed, custom, region, industry, wakeLock)
            }
        ) { t1, t2 ->
            DashboardUiState(
                stats = t1.v1,
                logs = t1.v2,
                selectedCategory = t1.v3,
                searchQuery = t1.v4,
                activeTab = t1.v5,
                lastCheckpointFile = repository.lastSavedCheckpoint.value,
                seedInputUrl = t2.v1,
                customSeedsList = t2.v2,
                targetRegion = t2.v3,
                targetIndustry = t2.v4,
                speedMode = _speedModeState.value,
                blacklistInput = _blacklistInputState.value,
                blacklistKeywords = _blacklistKeywordsState.value,
                isWakeLockEnabled = t2.v5,
                isOcrEnabled = _isOcrEnabledState.value,
                isServiceRunning = CrawlerService.isServiceRunning.value,
                dorkKeywordInput = _dorkKeywordInputState.value,
                isVerifyingMx = _isVerifyingMxState.value,
                mxVerifyProgress = _mxVerifyProgressState.value,
                cronState = cronSchedulerManager.scheduleState.value
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )
    }

    private data class Tuple5<A, B, C, D, E>(
        val v1: A, val v2: B, val v3: C, val v4: D, val v5: E
    )

    fun setDorkKeywordInput(text: String) {
        _dorkKeywordInputState.value = text
    }

    fun executeDorkSearch() {
        val kw = _dorkKeywordInputState.value.trim()
        if (kw.isNotEmpty()) {
            val count = crawlerEngine.discoverLeadsFromDork(kw)
            _dorkKeywordInputState.value = ""
            if (!crawlerStats.value.isRunning) {
                toggleOneTouchCrawler()
            }
        }
    }

    fun verifyMxDeliverability() {
        if (_isVerifyingMxState.value) return
        viewModelScope.launch {
            _isVerifyingMxState.value = true
            _mxVerifyProgressState.value = "Starting MX Live Verifier..."
            repository.verifyAllMxRecords { current, total ->
                _mxVerifyProgressState.value = "Verifying MX: $current/$total domains"
            }
            _isVerifyingMxState.value = false
            _mxVerifyProgressState.value = "MX Verification Complete!"
            kotlinx.coroutines.delay(3000)
            _mxVerifyProgressState.value = null
        }
    }

    fun importAndCleanCsv(csvContent: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val (imported, dropped) = repository.importAndCleanCsvText(csvContent)
            val msg = "Import Completed! $imported valid emails merged, $dropped duplicates/fakes dropped."
            onResult(msg)
        }
    }

    fun setCronScheduler(enabled: Boolean, intervalHours: Int) {
        cronSchedulerManager.setCronSchedule(enabled, intervalHours)
    }

    // Feature: Built-in SMTP Campaign Sender
    val smtpProgress = com.example.data.smtp.SmtpCampaignSender.progress
    val smtpProfiles = repository.smtpProfilesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveSmtpProfile(profile: com.example.data.model.SmtpProfile) {
        viewModelScope.launch {
            repository.saveSmtpProfile(profile)
        }
    }

    fun deleteSmtpProfile(profile: com.example.data.model.SmtpProfile) {
        viewModelScope.launch {
            repository.deleteSmtpProfile(profile)
        }
    }

    fun deleteScrapedEmail(scrapedEmail: com.example.data.model.ScrapedEmail) {
        viewModelScope.launch {
            db.scrapedEmailDao().deleteScrapedEmail(scrapedEmail)
        }
    }

    fun deleteScrapedEmails(scrapedEmails: List<com.example.data.model.ScrapedEmail>) {
        viewModelScope.launch {
            val ids = scrapedEmails.map { it.id }
            db.scrapedEmailDao().deleteScrapedEmailsByIds(ids)
        }
    }

    fun startSmtpCampaign(config: com.example.data.smtp.SmtpConfig) {
        viewModelScope.launch {
            com.example.data.smtp.SmtpCampaignSender.sendCampaign(filteredEmails.value, config)
        }
    }

    fun exportEncryptedBackup(passkey: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val backupData = repository.getEncryptedBackupPackage()
            val encryptedJson = com.example.util.EncryptedBackupManager.exportEncryptedBackup(backupData, passkey)
            onResult(encryptedJson)
        }
    }

    fun restoreEncryptedBackup(encryptedJson: String, passkey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val pkg = com.example.util.EncryptedBackupManager.restoreEncryptedBackup(encryptedJson, passkey)
                repository.restoreBackupPackage(pkg)
                onResult(true, "Restored ${pkg.emails.size} emails and ${pkg.smtpProfiles.size} SMTP profiles!")
            } catch (e: Exception) {
                onResult(false, "Decryption/Restore failed: ${e.localizedMessage ?: "Invalid password or corrupted file."}")
            }
        }
    }

    fun enrichAllProspects(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val count = repository.enrichAllProspects()
            onResult("Enriched $count lead records with industry, phone, and social data!")
        }
    }

    fun exportAndShareAnalyticsHtml(onShareIntentReady: (Intent) -> Unit) {
        repository.exportAndShareAnalyticsHtml(onShareIntentReady)
    }

    fun exportAndShareAnalyticsPdf(onShareIntentReady: (Intent) -> Unit) {
        repository.exportAndShareAnalyticsPdf(onShareIntentReady)
    }

    fun exportAndShareAnalyticsExcel(onShareIntentReady: (Intent) -> Unit) {
        repository.exportAndShareAnalyticsExcel(onShareIntentReady)
    }

    // Feature 3: Proxy Rotator Config
    fun setProxyRotatorConfig(proxyStr: String) {
        crawlerEngine.setProxyRotatorConfig(proxyStr)
    }

    // Feature 5: Google Maps & Business Directory Scraper Engine
    fun discoverGoogleMapsAndDirectoryLeads(businessType: String, location: String) {
        val count = crawlerEngine.discoverGoogleMapsAndDirectoryLeads(businessType, location)
        if (count > 0 && !crawlerStats.value.isRunning) {
            toggleOneTouchCrawler()
        }
    }

    // Feature 6: Cloud Webhook Auto-Sync
    fun syncLeadsToWebhook(webhookUrl: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = com.example.data.sync.CloudWebhookSyncManager.syncLeadsToWebhook(
                webhookUrl, filteredEmails.value
            )
            onResult(message)
        }
    }

    // Feature 7: Mailbox Ping Handshake
    fun pingMailboxHandshake(email: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val (exists, status) = com.example.data.verifier.DnsMxVerifier.pingMailboxHandshake(email)
            onResult("Mailbox Ping [$email]: ${if (exists) "VALID ($status)" else "INVALID ($status)"}")
        }
    }

    fun setSpeedMode(mode: String) {
        _speedModeState.value = mode
        crawlerEngine.setSpeedMode(mode)
    }

    fun setBlacklistInput(text: String) {
        _blacklistInputState.value = text
    }

    fun addBlacklistKeyword() {
        val kw = _blacklistInputState.value.trim()
        if (kw.isNotEmpty()) {
            _blacklistKeywordsState.update { current -> current + kw }
            crawlerEngine.addBlacklistKeyword(kw)
            _blacklistInputState.value = ""
        }
    }

    fun removeBlacklistKeyword(kw: String) {
        _blacklistKeywordsState.update { current -> current.filter { it != kw } }
        crawlerEngine.removeBlacklistKeyword(kw)
    }

    /**
     * ONE-TOUCH ACTION TRIGGER
     * Toggles automatic crawler ON / OFF with active target filters & wake lock
     */
    fun toggleOneTouchCrawler() {
        if (crawlerStats.value.isRunning) {
            crawlerEngine.stopCrawler()
            try {
                CrawlerService.stopService(getApplication())
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            if (_isWakeLockEnabledState.value) {
                try {
                    CrawlerService.startService(getApplication())
                } catch (e: Exception) {
                    // Ignore
                }
            }
            crawlerEngine.startCrawler(
                customSeeds = _customSeedsState.value,
                targetRegion = _targetRegionState.value,
                targetIndustry = _targetIndustryState.value
            )
        }
    }

    fun toggleWakeLockSetting() {
        val newValue = !_isWakeLockEnabledState.value
        _isWakeLockEnabledState.value = newValue
        if (crawlerStats.value.isRunning) {
            if (newValue) {
                try {
                    CrawlerService.startService(getApplication())
                } catch (e: Exception) {}
            } else {
                try {
                    CrawlerService.stopService(getApplication())
                } catch (e: Exception) {}
            }
        }
    }

    fun toggleOcrSetting() {
        val newValue = !_isOcrEnabledState.value
        _isOcrEnabledState.value = newValue
        crawlerEngine.isOcrEnabled = newValue
    }

    fun setTargetRegion(region: String) {
        _targetRegionState.value = region
    }

    fun setTargetIndustry(industry: String) {
        _targetIndustryState.value = industry
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryState.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQueryState.value = query
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTabState.value = tabIndex
    }

    fun setSeedInputUrl(url: String) {
        _seedInputState.value = url
    }

    fun addSeedUrl() {
        val url = _seedInputState.value.trim()
        if (url.isNotEmpty()) {
            _customSeedsState.update { current -> current + url }
            if (crawlerStats.value.isRunning) {
                crawlerEngine.addSeedUrl(url)
            }
            _seedInputState.value = ""
        }
    }

    fun removeSeedUrl(url: String) {
        _customSeedsState.update { current -> current.filter { it != url } }
    }

    fun clearTerminalLogs() {
        _logsState.value = emptyList()
    }

    fun manualTriggerSaveCheckpoint() {
        repository.triggerCheckpointSave()
    }

    fun exportAndShareCsv(onIntentReady: (Intent) -> Unit) {
        repository.exportAndShareCsv(onIntentReady)
    }

    fun exportAndShareJson(onIntentReady: (Intent) -> Unit) {
        repository.exportAndShareJson(onIntentReady)
    }

    fun exportAndShareTxt(onIntentReady: (Intent) -> Unit) {
        repository.exportAndShareTxt(onIntentReady)
    }

    fun copyEmailsToClipboard(context: android.content.Context, emails: List<ExtractedEmail>) {
        if (emails.isEmpty()) return
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = emails.joinToString("\n") { it.email }
        val clip = android.content.ClipData.newPlainText("Extracted Mailist", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "${emails.size} emails copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun clearDatabase() {
        viewModelScope.launch {
            if (crawlerStats.value.isRunning) {
                crawlerEngine.stopCrawler()
                try {
                    CrawlerService.stopService(getApplication())
                } catch (e: Exception) {}
            }
            repository.clearAllData()
            _logsState.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        crawlerEngine.stopCrawler()
        try {
            CrawlerService.stopService(getApplication())
        } catch (e: Exception) {}
    }
}
