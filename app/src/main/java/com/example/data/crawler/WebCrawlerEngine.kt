package com.example.data.crawler

import com.example.data.model.ExtractedEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val type: LogType,
    val message: String
)

enum class LogType {
    INFO, MATCH, SCAN, WARN, SUCCESS, FILTER
}

data class CrawlerStats(
    val isRunning: Boolean = false,
    val totalEmailsFound: Int = 0,
    val currentUrl: String = "STANDBY",
    val scannedPagesCount: Int = 0,
    val activeThreadsCount: Int = 0,
    val speedEmailPerMin: Int = 0,
    val queueSize: Int = 0,
    val targetRegion: String = "ALL",
    val targetIndustry: String = "ALL"
)

class WebCrawlerEngine(
    private val onEmailFound: (ExtractedEmail) -> Unit,
    private val onLog: (LogEntry) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var crawlerJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val urlQueue = Channel<String>(Channel.UNLIMITED)
    private val visitedUrls = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val visitedDomains = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val knownEmails = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val _stats = MutableStateFlow(CrawlerStats())
    val stats: StateFlow<CrawlerStats> = _stats.asStateFlow()

    private var totalEmailsCount = 0
    private var scannedPagesCount = 0
    private var activeThreads = 0

    // Rate calculation tracking
    private var lastEmailCountForRate = 0
    private var rateCalculatorJob: Job? = null

    // Target Filtering Settings & Speed Control
    private var currentTargetRegion = "ALL"
    private var currentTargetIndustry = "ALL"
    private var crawlDelayMs: Long = 400L // 100ms (ULTRA), 400ms (BALANCED), 1000ms (STEALTH)
    private val customBlacklistKeywords = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private var activeProxyStr: String = ""
    var isOcrEnabled: Boolean = true

    private val userAgentPool = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
    )

    fun setProxyRotatorConfig(proxyStr: String) {
        activeProxyStr = proxyStr.trim()
        if (activeProxyStr.isNotEmpty()) {
            log(LogType.INFO, "[PROXY ROTATOR ACTIVE] Proxy Gateway set to: $activeProxyStr")
        }
    }

    fun setSpeedMode(mode: String) {
        crawlDelayMs = when (mode.uppercase(Locale.ROOT)) {
            "ULTRA" -> 100L
            "STEALTH" -> 1000L
            else -> 400L // BALANCED
        }
        log(LogType.INFO, "[SPEED MODE UPDATED] Delay set to ${crawlDelayMs}ms ($mode)")
    }

    fun addBlacklistKeyword(keyword: String) {
        val clean = keyword.lowercase(Locale.ROOT).trim()
        if (clean.isNotEmpty() && customBlacklistKeywords.add(clean)) {
            log(LogType.INFO, "[BLACKLIST KEYWORD ADDED] -> $clean")
        }
    }

    fun removeBlacklistKeyword(keyword: String) {
        customBlacklistKeywords.remove(keyword.lowercase(Locale.ROOT).trim())
    }

    // Regex Patterns
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    )
    private val urlPattern = Pattern.compile(
        "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
        Pattern.CASE_INSENSITIVE
    )
    private val phonePattern = Pattern.compile(
        "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}"
    )
    private val socialPattern = Pattern.compile(
        "https?://(?:www\\.)?(?:instagram\\.com|facebook\\.com|linkedin\\.com|twitter\\.com|x\\.com|t\\.me|wa\\.me)/[a-zA-Z0-9_.-]+",
        Pattern.CASE_INSENSITIVE
    )

    // Disposable & Temp Email Blacklist
    private val disposableDomains = setOf(
        "mailinator.com", "tempmail.com", "10minutemail.com", "guerrillamail.com",
        "trashmail.com", "yopmail.com", "sharklasers.com", "dispostable.com",
        "getnada.com", "maildrop.cc", "temp-mail.org", "fakeinbox.com",
        "crazymailing.com", "mohmal.com", "nada.ltd", "mailnesia.com",
        "anonymouse.org", "throwawaymail.com", "getairmail.com", "tempail.com",
        "guerrillamailblock.com", "pokemail.net", "spam4.me", "bccto.me"
    )

    // Pre-seeded high-yield public web directories to jumpstart automatic extraction
    private val defaultSeeds = listOf(
        "https://news.ycombinator.com",
        "https://github.com/trending",
        "https://slashdot.org",
        "https://dev.to",
        "https://medium.com",
        "https://producthunt.com",
        "https://indiehackers.com",
        "https://sourceforge.net",
        "https://www.wikipedia.org",
        "https://techcrunch.com",
        "https://www.reuters.com",
        "https://www.bloomberg.com"
    )

    // Specialized Seeds for Indonesian & Asian Regional Targeting
    private val indonesiaSeeds = listOf(
        "https://www.kompas.com",
        "https://www.detik.com",
        "https://www.liputan6.com",
        "https://www.kaskus.co.id",
        "https://www.tribunnews.com",
        "https://www.indonesia.go.id"
    )

    // Specialized Seeds for Gaming / iGaming Targets
    private val gamingSeeds = listOf(
        "https://www.ign.com",
        "https://www.gamespot.com",
        "https://www.polygon.com",
        "https://www.eurogamer.net",
        "https://www.pocketgamer.com"
    )

    // Specialized Seeds for E-Commerce Targets
    private val ecommerceSeeds = listOf(
        "https://www.shopify.com",
        "https://www.bigcommerce.com",
        "https://www.etsy.com",
        "https://www.producthunt.com/topics/e-commerce"
    )

    /**
     * Preload existing emails from Room DB into in-memory deduplication set
     * to guarantee NO duplicate emails ever get saved across app restarts.
     */
    fun preloadExistingEmails(emails: List<String>) {
        var preloadedCount = 0
        for (e in emails) {
            val clean = e.lowercase(Locale.ROOT).trim()
            if (knownEmails.add(clean)) {
                preloadedCount++
            }
        }
        totalEmailsCount = knownEmails.size
        updateStats()
        if (preloadedCount > 0) {
            log(LogType.INFO, "[DEDUPLICATION READY] Preloaded $preloadedCount existing emails into filter memory.")
        }
    }

    fun startCrawler(
        customSeeds: List<String> = emptyList(),
        targetRegion: String = "ALL",
        targetIndustry: String = "ALL"
    ) {
        if (_stats.value.isRunning) return

        currentTargetRegion = targetRegion
        currentTargetIndustry = targetIndustry

        _stats.update {
            it.copy(
                isRunning = true,
                targetRegion = targetRegion,
                targetIndustry = targetIndustry
            )
        }

        log(LogType.SUCCESS, "=== 1 TOUCH MAILIST ENGINE INITIALIZED ===")
        log(LogType.INFO, "[TARGET CONFIG] Region: $targetRegion | Industry: $targetIndustry")

        // Select optimal seed pool based on user's target filters
        val chosenSeeds = mutableListOf<String>()
        if (customSeeds.isNotEmpty()) {
            chosenSeeds.addAll(customSeeds)
        }
        
        when {
            targetRegion.contains("INDONESIA") -> chosenSeeds.addAll(indonesiaSeeds)
            targetIndustry.contains("GAME") -> chosenSeeds.addAll(gamingSeeds)
            targetIndustry.contains("E-COMMERCE") -> chosenSeeds.addAll(ecommerceSeeds)
            else -> chosenSeeds.addAll(defaultSeeds)
        }

        for (seed in chosenSeeds.distinct()) {
            val cleanSeed = formatUrl(seed)
            if (visitedUrls.add(cleanSeed)) {
                urlQueue.trySend(cleanSeed)
                log(LogType.INFO, "[SEED ADDED] -> $cleanSeed")
            }
        }

        // Start Rate Tracking timer
        startRateCalculator()

        // Launch concurrent worker coroutines
        crawlerJob = scope.launch {
            val workerCount = 4
            activeThreads = workerCount
            updateStats()

            val workers = List(workerCount) { workerId ->
                launch {
                    runWorker(workerId)
                }
            }
        }
    }

    private suspend fun runWorker(workerId: Int) {
        for (url in urlQueue) {
            if (!_stats.value.isRunning) break

            _stats.update { it.copy(currentUrl = url, queueSize = urlQueue.hashCode()) }
            log(LogType.SCAN, "[WORKER #$workerId SCANNING] $url")

            try {
                crawlPage(url)
            } catch (e: Exception) {
                log(LogType.WARN, "[HTTP ERR #$workerId] ${e.localizedMessage ?: "Failed"} -> $url")
            }

            // Polite scraping interval delay based on speed mode (ULTRA: 100ms, BALANCED: 400ms, STEALTH: 1000ms)
            delay(crawlDelayMs)
        }
    }

    private suspend fun crawlPage(targetUrl: String) {
        val randomUa = userAgentPool.random()
        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", randomUa)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                val html = response.body?.string() ?: return

                scannedPagesCount++
                updateStats()

                // 1. Extract Emails via EmailParsingEngine with Obfuscation Decoding, Validation, and Target Filtering
                val (extractedPhone, extractedSocial) = extractPhoneAndSocial(html)
                val parsedEmails = EmailParsingEngine.extractValidEmails(
                    rawContent = html,
                    sourceUrl = targetUrl,
                    extractedPhone = extractedPhone,
                    extractedSocial = extractedSocial,
                    customBlacklistKeywords = customBlacklistKeywords
                )

                // 1b. Optical Character Recognition (OCR) Pass for Image Banners, Canvas, & Media Graphics
                val ocrEmails = if (isOcrEnabled) {
                    val list = ImageOcrEngine.processWebImagesWithOcr(
                        html = html,
                        baseUrl = targetUrl,
                        httpClient = client,
                        customBlacklistKeywords = customBlacklistKeywords
                    )
                    if (list.isNotEmpty()) {
                        log(LogType.INFO, "[OCR SCAN] Extracted ${list.size} email(s) from image/media elements on $targetUrl")
                    }
                    list
                } else {
                    emptyList()
                }

                val combinedEmails = (parsedEmails + ocrEmails).distinctBy { it.email }

                var pageFoundCount = 0
                for (emailObj in combinedEmails) {
                    val foundEmail = emailObj.email

                    // Step A: Smart Target Criteria Filtering (Region & Industry match)
                    if (!matchesTargetCriteria(foundEmail, targetUrl)) {
                        continue
                    }

                    // Step B: Strict Deduplication Check
                    if (knownEmails.add(foundEmail)) {
                        totalEmailsCount++
                        pageFoundCount++

                        onEmailFound(emailObj)
                        log(LogType.MATCH, "[+] VALID EMAIL MATCH [${emailObj.category}]: $foundEmail ${if(extractedPhone.isNotEmpty()) "WA: $extractedPhone" else ""}")
                        updateStats()
                    }
                }

                if (pageFoundCount > 0) {
                    log(LogType.SUCCESS, "[EXTRACTED $pageFoundCount NEW VALID EMAILS] from $targetUrl")
                }

                // 2. Extract Links for Recursive Self-Expanding Crawling with Domain Deduplication
                val urlMatcher = urlPattern.matcher(html)
                var addedLinks = 0
                // Sliding visited set pruning if memory/queue size grows beyond 10,000
                if (visitedUrls.size > 10000) {
                    visitedUrls.clear()
                    log(LogType.INFO, "[BUFFER AUTO-PRUNED] Reset visited URLs cache to maintain peak performance.")
                }

                while (urlMatcher.find() && addedLinks < 15) {
                    val nextUrl = urlMatcher.group()
                    if (isValidWebUrl(nextUrl)) {
                        val cleanUrl = formatUrl(nextUrl)
                        val domain = extractRootDomain(cleanUrl)

                        // Smart URL Relevance Prioritization
                        val isRelevant = isUrlRelevantForTarget(cleanUrl, domain)
                        
                        if (isRelevant) {
                            if (visitedUrls.add(cleanUrl)) {
                                urlQueue.trySend(cleanUrl)
                                addedLinks++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log(LogType.WARN, "[CRAWL ERR] ${e.localizedMessage ?: "Failed"} -> $targetUrl")
        }
    }

    fun stopCrawler() {
        _stats.update { it.copy(isRunning = false, currentUrl = "PAUSED / STANDBY") }
        crawlerJob?.cancel()
        crawlerJob = null
        rateCalculatorJob?.cancel()
        rateCalculatorJob = null
        log(LogType.WARN, "=== CRAWLER ENGINE PAUSED ===")
    }

    fun extractPhoneAndSocial(html: String): Pair<String, String> {
        val phones = mutableSetOf<String>()
        val phoneMatcher = phonePattern.matcher(html)
        while (phoneMatcher.find() && phones.size < 3) {
            val p = phoneMatcher.group().trim()
            if (p.length in 8..25 && !p.contains("2000") && !p.contains("1999")) {
                phones.add(p)
            }
        }

        val socials = mutableSetOf<String>()
        val socialMatcher = socialPattern.matcher(html)
        while (socialMatcher.find() && socials.size < 3) {
            val s = socialMatcher.group().trim()
            if (!s.contains("sharer") && !s.contains("intent") && !s.contains("dialog")) {
                socials.add(s)
            }
        }

        return Pair(phones.joinToString(", "), socials.joinToString(", "))
    }

    /**
     * Google / Bing / DuckDuckGo Search Dork Engine (Automatic Lead Discovery)
     */
    fun discoverLeadsFromDork(keyword: String): Int {
        val cleanKw = keyword.trim()
        if (cleanKw.isEmpty()) return 0

        val encodedQuery = try {
            java.net.URLEncoder.encode(cleanKw, "UTF-8")
        } catch (e: Exception) {
            cleanKw.replace(" ", "+")
        }

        val dorkUrls = listOf(
            "https://html.duckduckgo.com/html/?q=$encodedQuery",
            "https://www.bing.com/search?q=$encodedQuery",
            "https://search.yahoo.com/search?p=$encodedQuery"
        )

        var addedCount = 0
        for (url in dorkUrls) {
            if (visitedUrls.add(url)) {
                urlQueue.trySend(url)
                addedCount++
            }
        }
        log(LogType.SUCCESS, "[DORK ENGINE DISCOVERY] Added ${dorkUrls.size} search dork seeds for keyword: '$cleanKw'")
        return addedCount
    }

    /**
     * Feature 5: Google Maps & Business Directory Scraping Engine
     */
    fun discoverGoogleMapsAndDirectoryLeads(businessType: String, location: String): Int {
        val query = "$businessType $location contact email phone".trim()
        if (query.isEmpty()) return 0

        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query.replace(" ", "+")
        }

        val mapsDirectorySeeds = listOf(
            "https://www.google.com/maps/search/$encoded",
            "https://html.duckduckgo.com/html/?q=$encoded",
            "https://www.yellowpages.com/search?search_terms=$encoded",
            "https://www.yelp.com/search?find_desc=$encoded"
        )

        var count = 0
        for (u in mapsDirectorySeeds) {
            if (visitedUrls.add(u)) {
                urlQueue.trySend(u)
                count++
            }
        }
        log(LogType.SUCCESS, "[GMAPS & DIRECTORY SCRAPER] Injected $count business directory seeds for '$query'")
        return count
    }

    fun addSeedUrl(url: String) {
        val formatted = formatUrl(url)
        if (isValidWebUrl(formatted) && visitedUrls.add(formatted)) {
            urlQueue.trySend(formatted)
            log(LogType.INFO, "[USER SEED INJECTED] -> $formatted")
        }
    }

    private fun startRateCalculator() {
        rateCalculatorJob = scope.launch {
            while (_stats.value.isRunning) {
                delay(5000)
                val diff = totalEmailsCount - lastEmailCountForRate
                lastEmailCountForRate = totalEmailsCount
                val speed = diff * 12
                _stats.update { it.copy(speedEmailPerMin = speed) }
            }
        }
    }

    private fun updateStats() {
        _stats.update {
            it.copy(
                totalEmailsFound = totalEmailsCount,
                scannedPagesCount = scannedPagesCount,
                activeThreadsCount = if (it.isRunning) activeThreads else 0
            )
        }
    }

    private fun log(type: LogType, message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date())
        onLog(LogEntry(timestamp = timeStr, type = type, message = message))
    }

    /**
     * EMAIL VALIDATION ENGINE (Filters out fake, temp, syntax error, or leak emails)
     */
    private fun isValidEmail(email: String): Boolean {
        if (email.length < 6 || email.length > 80) return false

        // Check 1: Must contain exactly one '@'
        val parts = email.split("@")
        if (parts.size != 2) return false
        val username = parts[0]
        val domain = parts[1]

        if (username.length < 2 || domain.length < 4) return false
        if (!domain.contains(".")) return false

        // Check 2: Disposable / Temp Mail Domain Blacklist & Custom Blacklist
        if (disposableDomains.contains(domain)) return false
        for (kw in customBlacklistKeywords) {
            if (email.contains(kw) || domain.contains(kw)) return false
        }

        // Check 3: Placeholder / Fake Prefixes & Patterns
        val lowerUsername = username.lowercase(Locale.ROOT)
        val fakeUsernames = listOf(
            "example", "test", "demo", "sample", "user", "admin",
            "john.doe", "johndoe", "name", "yourname", "username",
            "email", "xxx", "yyy", "abc", "xyz", "asdf", "qwer",
            "12345", "00000", "no-reply", "noreply", "donotreply",
            "info@example", "support@example"
        )
        if (fakeUsernames.contains(lowerUsername)) return false

        // Check 4: Numeric-only usernames (usually timestamps, user IDs, or tracking hashes)
        if (lowerUsername.all { it.isDigit() }) return false

        // Check 5: Invalid file extensions or web framework code leaks in extracted text
        val invalidTokens = listOf(
            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".css", ".js",
            "@2x", "w3.org", "schema.org", "bootstrap", "font-", "rating",
            "node_modules", "jquery", "react", "sentry", "cloudflare",
            "googleapis", "gstatic", "s3.amazonaws.com", "localhost"
        )
        for (token in invalidTokens) {
            if (email.contains(token)) return false
        }

        // Check 6: Valid TLD suffix
        val tld = domain.substringAfterLast(".").lowercase(Locale.ROOT)
        if (tld.length < 2 || tld.length > 12 || !tld.all { it.isLetter() }) return false

        return true
    }

    /**
     * SMART TARGET FILTERING ENGINE
     * Validates if email matches target country region & industry niche
     */
    private fun matchesTargetCriteria(email: String, sourceUrl: String): Boolean {
        val lowerEmail = email.lowercase(Locale.ROOT)
        val lowerUrl = sourceUrl.lowercase(Locale.ROOT)

        // 1. Region Filter Verification
        if (currentTargetRegion != "ALL") {
            val regionMatches = when {
                currentTargetRegion.contains("INDONESIA") -> {
                    lowerEmail.endsWith(".id") || lowerUrl.contains(".id") || lowerEmail.contains("indonesia")
                }
                currentTargetRegion.contains("USA") -> {
                    lowerEmail.endsWith(".com") || lowerEmail.endsWith(".us") || lowerEmail.endsWith(".org") || lowerUrl.contains(".us")
                }
                currentTargetRegion.contains("EUROPE") -> {
                    lowerEmail.endsWith(".uk") || lowerEmail.endsWith(".de") || lowerEmail.endsWith(".fr") || lowerEmail.endsWith(".eu") || lowerUrl.contains(".uk") || lowerUrl.contains(".de")
                }
                currentTargetRegion.contains("JAPAN") -> {
                    lowerEmail.endsWith(".jp") || lowerUrl.contains(".jp")
                }
                currentTargetRegion.contains("SINGAPORE") -> {
                    lowerEmail.endsWith(".sg") || lowerUrl.contains(".sg")
                }
                else -> true
            }
            if (!regionMatches) return false
        }

        // 2. Industry Filter Verification
        if (currentTargetIndustry != "ALL") {
            val industryMatches = when {
                currentTargetIndustry.contains("GAME") -> {
                    val keywords = listOf("game", "gaming", "slot", "casino", "poker", "bet", "esports", "play", "arcade", "steam", "roblox", "twitch", "discord")
                    keywords.any { lowerEmail.contains(it) || lowerUrl.contains(it) }
                }
                currentTargetIndustry.contains("E-COMMERCE") -> {
                    val keywords = listOf("shop", "store", "cart", "buy", "commerce", "mall", "tokopedia", "shopee", "lazada", "amazon", "ebay", "shopify", "retail")
                    keywords.any { lowerEmail.contains(it) || lowerUrl.contains(it) }
                }
                currentTargetIndustry.contains("GOVERNMENT") -> {
                    val keywords = listOf(".gov", ".go.id", "government", "kemen", "ministry", "parlemen", "state")
                    keywords.any { lowerEmail.contains(it) || lowerUrl.contains(it) }
                }
                currentTargetIndustry.contains("EDUCATION") -> {
                    val keywords = listOf(".edu", ".ac.id", "university", "kampus", "school", "academy", "college")
                    keywords.any { lowerEmail.contains(it) || lowerUrl.contains(it) }
                }
                currentTargetIndustry.contains("FINTECH") -> {
                    val keywords = listOf("bank", "pay", "fintech", "invest", "capital", "finance", "corp", "inc", "holding", "crypto")
                    keywords.any { lowerEmail.contains(it) || lowerUrl.contains(it) }
                }
                else -> true
            }
            if (!industryMatches) return false
        }

        return true
    }

    private fun isUrlRelevantForTarget(url: String, domain: String): Boolean {
        val lowerUrl = url.lowercase(Locale.ROOT)

        // Always allow search engines & search dorks to crawl results
        val isSearchEngine = lowerUrl.contains("duckduckgo.com") || lowerUrl.contains("bing.com") || lowerUrl.contains("google.com") || lowerUrl.contains("yahoo.com")
        if (isSearchEngine) return true

        if (currentTargetRegion.contains("INDONESIA")) {
            val isIndoDomain = lowerUrl.contains(".id") || lowerUrl.contains("indonesia") || lowerUrl.contains("kompas") || lowerUrl.contains("detik") || lowerUrl.contains("liputan6") || lowerUrl.contains("tribun") || lowerUrl.contains("kaskus")
            if (!isIndoDomain) {
                return false
            }
        }

        if (currentTargetIndustry.contains("GAME")) {
            val keywords = listOf("game", "gaming", "esports", "play", "steam", "twitch", "discord", "news", "forum")
            if (!keywords.any { lowerUrl.contains(it) }) return false
        }

        return true
    }

    private fun isValidWebUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val lower = url.lowercase(Locale.ROOT)
        // Only skip heavy video streams and binary compressed archives
        val skipExtensions = listOf(".mp4", ".mp3", ".avi", ".mov", ".zip", ".tar", ".gz", ".7z", ".rar", ".exe", ".apk")
        for (ext in skipExtensions) {
            if (lower.endsWith(ext)) return false
        }
        return true
    }

    private fun extractRootDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun formatUrl(url: String): String {
        var formatted = url.trim()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "https://$formatted"
        }
        return formatted
    }
}
