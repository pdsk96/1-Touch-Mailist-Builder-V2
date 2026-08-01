package com.example.data.crawler

import com.example.data.model.ExtractedEmail
import java.net.URLDecoder
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust Email Parsing Engine designed to extract, decode, validate, and categorize
 * valid email addresses from raw web content strings (HTML, JSON, plain text, obfuscated formats).
 */
object EmailParsingEngine {

    // Standard Email Regex Pattern
    private val standardEmailPattern: Pattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    )

    // Mailto URI Pattern (e.g., href="mailto:contact@domain.com?subject=...")
    private val mailtoPattern: Pattern = Pattern.compile(
        "mailto:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
        Pattern.CASE_INSENSITIVE
    )

    // Obfuscated Email Patterns (e.g., info [at] company [dot] com, user(at)domain.com)
    private val obfuscatedAtPattern: Pattern = Pattern.compile(
        "\\b([a-zA-Z0-9._%+-]+)\\s*(?:\\[at\\]|\\(at\\)|\\{at\\}|\\s+at\\s+)\\s*([a-zA-Z0-9.-]+)\\s*(?:\\[dot\\]|\\(dot\\)|\\{dot\\}|\\s+dot\\s+)\\s*([a-zA-Z]{2,})\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Image & Media Tag Attributes Pattern (e.g. alt="...", title="...", data-email="...", src="...")
    private val mediaAttributePattern: Pattern = Pattern.compile(
        "(?:alt|title|aria-label|data-email|data-contact|src|content)\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    )

    // Base64 Data URI Pattern (e.g., data:image/png;base64,... or data:text/plain;base64,...)
    private val base64DataUriPattern: Pattern = Pattern.compile(
        "data:[^;]+;base64,([a-zA-Z0-9+/=]{16,})",
        Pattern.CASE_INSENSITIVE
    )

    // JSON Key-Value Email Pattern (e.g. "email": "user@domain.com")
    private val jsonEmailPattern: Pattern = Pattern.compile(
        "\"(?:email|mail|contact|author_email|user_email|owner_email)\"\\s*:\\s*\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    )

    // Disposable & Temp Email Domain Blacklist
    private val disposableDomains: Set<String> = setOf(
        "mailinator.com", "tempmail.com", "10minutemail.com", "guerrillamail.com",
        "trashmail.com", "yopmail.com", "sharklasers.com", "dispostable.com",
        "getnada.com", "maildrop.cc", "temp-mail.org", "fakeinbox.com",
        "crazymailing.com", "mohmal.com", "nada.ltd", "mailnesia.com",
        "anonymouse.org", "throwawaymail.com", "getairmail.com", "tempail.com",
        "guerrillamailblock.com", "pokemail.net", "spam4.me", "bccto.me",
        "temp-mail.io", "emailondeck.com", "throwaway.email", "burnermail.io"
    )

    // Common fake/placeholder usernames
    private val fakeUsernames: Set<String> = setOf(
        "example", "test", "demo", "sample", "user", "admin",
        "john.doe", "johndoe", "name", "yourname", "username",
        "email", "xxx", "yyy", "abc", "xyz", "asdf", "qwer",
        "12345", "00000", "no-reply", "noreply", "donotreply",
        "info@example", "support@example", "user@example", "admin@example"
    )

    // Invalid tokens indicating code leaks or static asset URLs rather than real email addresses
    private val invalidTokens: List<String> = listOf(
        ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".css", ".js",
        "@2x", "w3.org", "schema.org", "bootstrap", "font-", "rating",
        "node_modules", "jquery", "react", "sentry", "cloudflare",
        "googleapis", "gstatic", "s3.amazonaws.com", "localhost", "example.com"
    )

    /**
     * Parse raw web content (HTML or plain text) and return a list of valid ExtractedEmail objects.
     */
    fun extractValidEmails(
        rawContent: String,
        sourceUrl: String = "",
        extractedPhone: String = "",
        extractedSocial: String = "",
        customBlacklistKeywords: Set<String> = emptySet()
    ): List<ExtractedEmail> {
        if (rawContent.isBlank()) return emptyList()

        val candidateEmails = mutableSetOf<String>()

        // 1. Direct Regex Match
        val directMatcher = standardEmailPattern.matcher(rawContent)
        while (directMatcher.find()) {
            candidateEmails.add(directMatcher.group().lowercase(Locale.ROOT).trim())
        }

        // 2. Mailto Link Extraction
        val mailtoMatcher = mailtoPattern.matcher(rawContent)
        while (mailtoMatcher.find()) {
            val mail = mailtoMatcher.group(1)?.lowercase(Locale.ROOT)?.trim()
            if (!mail.isNullOrEmpty()) {
                candidateEmails.add(mail)
            }
        }

        // 3. Obfuscated Match (e.g. user [at] domain [dot] com)
        val obfMatcher = obfuscatedAtPattern.matcher(rawContent)
        while (obfMatcher.find()) {
            val user = obfMatcher.group(1)
            val domain = obfMatcher.group(2)
            val tld = obfMatcher.group(3)
            if (!user.isNullOrEmpty() && !domain.isNullOrEmpty() && !tld.isNullOrEmpty()) {
                val reconstructed = "$user@$domain.$tld".lowercase(Locale.ROOT).trim()
                candidateEmails.add(reconstructed)
            }
        }

        // 4. JSON Payload Extraction (e.g., "email": "contact@domain.com")
        val jsonMatcher = jsonEmailPattern.matcher(rawContent)
        while (jsonMatcher.find()) {
            val emailVal = jsonMatcher.group(1)?.lowercase(Locale.ROOT)?.trim()
            if (!emailVal.isNullOrEmpty()) {
                candidateEmails.add(emailVal)
            }
        }

        // 5. Image & Media Attributes Extraction (alt, title, aria-label, src, filename)
        val mediaMatcher = mediaAttributePattern.matcher(rawContent)
        while (mediaMatcher.find()) {
            val attrContent = mediaMatcher.group(1) ?: continue
            val innerMatcher = standardEmailPattern.matcher(attrContent)
            while (innerMatcher.find()) {
                candidateEmails.add(innerMatcher.group().lowercase(Locale.ROOT).trim())
            }
        }

        // 6. Base64 Payload Decoding Pass (e.g. data:image/png;base64,... or encoded scripts)
        try {
            val b64Matcher = base64DataUriPattern.matcher(rawContent)
            var b64Count = 0
            while (b64Matcher.find() && b64Count < 10) {
                b64Count++
                val b64Str = b64Matcher.group(1) ?: continue
                val decodedBytes = android.util.Base64.decode(b64Str, android.util.Base64.DEFAULT)
                val decodedText = String(decodedBytes, Charsets.UTF_8)
                val innerMatcher = standardEmailPattern.matcher(decodedText)
                while (innerMatcher.find()) {
                    candidateEmails.add(innerMatcher.group().lowercase(Locale.ROOT).trim())
                }
            }
        } catch (e: Exception) {
            // Ignore Base64 decode errors
        }

        // 7. PDF Document Stream & Link Pattern Parsing
        if (rawContent.contains("%PDF-") || rawContent.contains("/PDF") || rawContent.contains("/TJ") || rawContent.contains("/URI")) {
            extractFromPdfStream(rawContent, candidateEmails)
        }

        // 8. Office XML / Word / Excel Tag Extraction
        if (rawContent.contains("<w:t") || rawContent.contains("xmlns:w") || rawContent.contains("<cell")) {
            extractFromOfficeXml(rawContent, candidateEmails)
        }

        // 9. Database Dump / SQL Insert & CSV Extraction Pass
        if (rawContent.contains("INSERT INTO") || rawContent.contains("VALUES (") || rawContent.contains("COPY ")) {
            extractFromDatabaseDump(rawContent, candidateEmails)
        }

        // 10. HTML Entity & URL Encoded String Decoding Pass
        val decodedContent = decodeWebEntities(rawContent)
        if (decodedContent != rawContent) {
            val decodedMatcher = standardEmailPattern.matcher(decodedContent)
            while (decodedMatcher.find()) {
                candidateEmails.add(decodedMatcher.group().lowercase(Locale.ROOT).trim())
            }
        }

        // Filter & Transform to ExtractedEmail objects
        val results = mutableListOf<ExtractedEmail>()
        for (email in candidateEmails) {
            if (isValidEmail(email, customBlacklistKeywords)) {
                val category = ExtractedEmail.categorise(email)
                val domain = ExtractedEmail.extractDomain(email)
                results.add(
                    ExtractedEmail(
                        email = email,
                        domain = domain,
                        category = category,
                        sourceUrl = sourceUrl,
                        phone = extractedPhone,
                        social = extractedSocial,
                        isMxVerified = false,
                        mxStatus = "PENDING"
                    )
                )
            }
        }

        return results
    }

    /**
     * Extracts emails from PDF stream buffers, PDF annotation links (/URI), and text block elements.
     */
    private fun extractFromPdfStream(pdfContent: String, outputSet: MutableSet<String>) {
        try {
            // PDF URI annotation links: /URI (mailto:user@domain.com) or /URI (https://...)
            val pdfUriPattern = Pattern.compile("/URI\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE)
            val uriMatcher = pdfUriPattern.matcher(pdfContent)
            while (uriMatcher.find()) {
                val uriStr = uriMatcher.group(1) ?: continue
                val emailMatcher = standardEmailPattern.matcher(uriStr)
                while (emailMatcher.find()) {
                    outputSet.add(emailMatcher.group().lowercase(Locale.ROOT).trim())
                }
            }

            // PDF text elements: (Text String) Tj or [(T) (e) (x) (t)] TJ
            val pdfTextPattern = Pattern.compile("\\(([^)]+)\\)\\s*Tj", Pattern.CASE_INSENSITIVE)
            val textMatcher = pdfTextPattern.matcher(pdfContent)
            while (textMatcher.find()) {
                val textChunk = textMatcher.group(1) ?: continue
                val emailMatcher = standardEmailPattern.matcher(textChunk)
                while (emailMatcher.find()) {
                    outputSet.add(emailMatcher.group().lowercase(Locale.ROOT).trim())
                }
            }
        } catch (e: Exception) {
            // Ignore PDF stream parse errors
        }
    }

    /**
     * Extracts emails from Word / Excel / PowerPoint XML schemas (<w:t>, <cell>, etc.).
     */
    private fun extractFromOfficeXml(xmlContent: String, outputSet: MutableSet<String>) {
        try {
            val xmlTextPattern = Pattern.compile("<(?:w:t|text|cell|d:prop|string)[^>]*>([^<]+)</", Pattern.CASE_INSENSITIVE)
            val matcher = xmlTextPattern.matcher(xmlContent)
            while (matcher.find()) {
                val valStr = matcher.group(1) ?: continue
                val emailMatcher = standardEmailPattern.matcher(valStr)
                while (emailMatcher.find()) {
                    outputSet.add(emailMatcher.group().lowercase(Locale.ROOT).trim())
                }
            }
        } catch (e: Exception) {
            // Ignore XML parse errors
        }
    }

    /**
     * Extracts emails from SQL dumps, INSERT INTO queries, and CSV data rows.
     */
    private fun extractFromDatabaseDump(sqlContent: String, outputSet: MutableSet<String>) {
        try {
            // SQL VALUES ('email@domain.com', ...)
            val sqlValuesPattern = Pattern.compile("VALUES\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE)
            val matcher = sqlValuesPattern.matcher(sqlContent)
            while (matcher.find()) {
                val valuesChunk = matcher.group(1) ?: continue
                val emailMatcher = standardEmailPattern.matcher(valuesChunk)
                while (emailMatcher.find()) {
                    outputSet.add(emailMatcher.group().lowercase(Locale.ROOT).trim())
                }
            }
        } catch (e: Exception) {
            // Ignore SQL parse errors
        }
    }

    /**
     * Decodes HTML entities (e.g. &#64;, &#46;) and URL percent encodings (%40)
     */
    private fun decodeWebEntities(raw: String): String {
        return try {
            var s = raw
            // Decode common entity encodings
            s = s.replace("&#64;", "@")
                .replace("&#x40;", "@")
                .replace("&#46;", ".")
                .replace("&#x2e;", ".")
                .replace("&nbsp;", " ")

            if (s.contains("%40")) {
                s = URLDecoder.decode(s, "UTF-8")
            }
            s
        } catch (e: Exception) {
            raw
        }
    }

    /**
     * Comprehensive Email Syntax, Domain, and Quality Validation
     */
    fun isValidEmail(email: String, customBlacklistKeywords: Set<String> = emptySet()): Boolean {
        val clean = email.trim()
        if (clean.length < 6 || clean.length > 80) return false

        val parts = clean.split("@")
        if (parts.size != 2) return false
        val username = parts[0].lowercase(Locale.ROOT)
        val domain = parts[1].lowercase(Locale.ROOT)

        if (username.length < 2 || domain.length < 4) return false
        if (!domain.contains(".")) return false

        // Disposable Domain Check
        if (disposableDomains.contains(domain)) return false

        // Custom Blacklist Keyword Check
        for (kw in customBlacklistKeywords) {
            val cleanKw = kw.lowercase(Locale.ROOT).trim()
            if (cleanKw.isNotEmpty() && (clean.contains(cleanKw) || domain.contains(cleanKw))) {
                return false
            }
        }

        // Placeholder / Fake Username Check
        if (fakeUsernames.contains(username)) return false

        // Numeric-only Username Check
        if (username.all { it.isDigit() }) return false

        // Code leak / Static Asset leak check
        for (token in invalidTokens) {
            if (clean.contains(token)) return false
        }

        // Valid TLD Check
        val tld = domain.substringAfterLast(".").lowercase(Locale.ROOT)
        if (tld.length < 2 || tld.length > 12 || !tld.all { it.isLetter() }) return false

        return true
    }
}
