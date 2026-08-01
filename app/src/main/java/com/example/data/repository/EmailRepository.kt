package com.example.data.repository

import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.EmailDao
import com.example.data.model.ExtractedEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

import com.example.data.model.ScrapedEmail

class EmailRepository(
    private val emailDao: EmailDao,
    private val context: Context,
    private val smtpProfileDao: com.example.data.local.SmtpProfileDao = AppDatabase.getInstance(context).smtpProfileDao(),
    private val scrapedEmailDao: com.example.data.local.ScrapedEmailDao = AppDatabase.getInstance(context).scrapedEmailDao()
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val checkpointManager = CsvCheckpointManager(context)

    val allEmailsFlow: Flow<List<ExtractedEmail>> = emailDao.getAllEmailsFlow()
    val totalCountFlow: Flow<Int> = emailDao.getEmailCountFlow()

    val allScrapedEmailsFlow: Flow<List<ScrapedEmail>> = scrapedEmailDao.getAllScrapedEmailsFlow()
    val scrapedEmailCountFlow: Flow<Int> = scrapedEmailDao.getScrapedEmailCountFlow()

    val smtpProfilesFlow: Flow<List<com.example.data.model.SmtpProfile>> = smtpProfileDao.getAllProfiles()

    suspend fun saveSmtpProfile(profile: com.example.data.model.SmtpProfile): Long {
        return smtpProfileDao.insertProfile(profile)
    }

    suspend fun deleteSmtpProfile(profile: com.example.data.model.SmtpProfile) {
        smtpProfileDao.deleteProfile(profile)
    }

    private val _lastSavedCheckpoint = MutableStateFlow<String?>(null)
    val lastSavedCheckpoint: StateFlow<String?> = _lastSavedCheckpoint.asStateFlow()

    private var itemsSinceLastSave = 0

    suspend fun saveExtractedEmail(email: ExtractedEmail) {
        val rowId = emailDao.insertEmail(email)
        scrapedEmailDao.insertScrapedEmail(ScrapedEmail.fromExtractedEmail(email))
        if (rowId != -1L) {
            itemsSinceLastSave++
            // Auto-save CSV Checkpoint every 25 new emails added
            if (itemsSinceLastSave >= 25) {
                itemsSinceLastSave = 0
                triggerCheckpointSave()
            }
        }
    }

    suspend fun saveScrapedEmail(email: ScrapedEmail): Long {
        val id = scrapedEmailDao.insertScrapedEmail(email)
        emailDao.insertEmail(email.toExtractedEmail())
        return id
    }

    fun getEmailsByCategoryFlow(category: String): Flow<List<ExtractedEmail>> {
        return if (category == "ALL") {
            emailDao.getAllEmailsFlow()
        } else {
            emailDao.getEmailsByCategoryFlow(category)
        }
    }

    fun triggerCheckpointSave() {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            if (emails.isNotEmpty()) {
                val savedFile = checkpointManager.saveCheckpointCsv(emails)
                _lastSavedCheckpoint.value = savedFile.name
            }
        }
    }

    fun exportAndShareCsv(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveCheckpointCsv(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    fun exportAndShareJson(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveCheckpointJson(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    fun exportAndShareTxt(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveCheckpointTxt(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    fun exportAndShareAnalyticsHtml(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveAnalyticsReportHtml(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    fun exportAndShareAnalyticsPdf(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveAnalyticsReportPdf(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    fun exportAndShareAnalyticsExcel(onShareIntentReady: (Intent) -> Unit) {
        scope.launch {
            val emails = emailDao.getAllEmailsSync()
            val file = checkpointManager.saveAnalyticsReportExcelCsv(emails)
            _lastSavedCheckpoint.value = file.name
            val shareIntent = checkpointManager.getShareIntent(file)
            onShareIntentReady(shareIntent)
        }
    }

    suspend fun verifyAllMxRecords(onProgress: (Int, Int) -> Unit) {
        val emails = emailDao.getAllEmailsSync()
        var current = 0
        for (item in emails) {
            current++
            onProgress(current, emails.size)
            if (item.mxStatus == "PENDING" || !item.isMxVerified) {
                val (isVerified, status) = com.example.data.verifier.DnsMxVerifier.verifyDomainMx(item.domain)
                emailDao.updateMxStatus(item.id, isVerified, status)
            }
        }
    }

    suspend fun importAndCleanCsvText(csvContent: String): Pair<Int, Int> {
        val lines = csvContent.lines()
        val emailRegex = java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val matcher = emailRegex.matcher(csvContent)
        
        val foundEmails = mutableSetOf<String>()
        while (matcher.find()) {
            val email = matcher.group().lowercase().trim()
            if (email.length in 6..80 && email.contains(".") && !email.contains("example.com")) {
                foundEmails.add(email)
            }
        }

        var importedCount = 0
        var droppedCount = 0

        val existing = emailDao.getAllEmailsSync().map { it.email.lowercase() }.toSet()

        for (e in foundEmails) {
            if (existing.contains(e)) {
                droppedCount++
                continue
            }
            val domain = ExtractedEmail.extractDomain(e)
            val category = ExtractedEmail.categorise(e)
            val obj = ExtractedEmail(
                email = e,
                domain = domain,
                category = category,
                sourceUrl = "CSV_IMPORT",
                isMxVerified = false,
                mxStatus = "PENDING"
            )
            val id = emailDao.insertEmail(obj)
            if (id != -1L) {
                importedCount++
            } else {
                droppedCount++
            }
        }

        if (importedCount > 0) {
            triggerCheckpointSave()
        }

        return Pair(importedCount, droppedCount)
    }

    suspend fun clearAllData() {
        emailDao.deleteAll()
        scrapedEmailDao.deleteAllScrapedEmails()
        _lastSavedCheckpoint.value = null
        itemsSinceLastSave = 0
    }

    suspend fun getEncryptedBackupPackage(): com.example.util.BackupPackage {
        val emails = emailDao.getAllEmailsSync()
        val profiles = smtpProfileDao.getAllProfilesSync()
        return com.example.util.BackupPackage(emails, profiles)
    }

    suspend fun restoreBackupPackage(pkg: com.example.util.BackupPackage) {
        if (pkg.emails.isNotEmpty()) {
            emailDao.insertEmails(pkg.emails)
        }
        if (pkg.smtpProfiles.isNotEmpty()) {
            pkg.smtpProfiles.forEach { smtpProfileDao.insertProfile(it) }
        }
        triggerCheckpointSave()
    }

    suspend fun enrichAllProspects(): Int {
        val emails = emailDao.getAllEmailsSync()
        var count = 0
        for (email in emails) {
            val enriched = com.example.util.ProspectEnricher.enrichProspect(email)
            emailDao.updateEmail(enriched)
            count++
        }
        return count
    }
}
