package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "extracted_emails",
    indices = [Index(value = ["email"], unique = true)]
)
data class ExtractedEmail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val domain: String,
    val category: String, // GMAIL, YAHOO, OUTLOOK, BUSINESS, EDU, GOV, OTHER
    val sourceUrl: String = "",
    val phone: String = "",
    val social: String = "",
    val isMxVerified: Boolean = false,
    val mxStatus: String = "PENDING", // VALID, NO_MX, PENDING
    val leadScore: Int = 50,
    val industryTag: String = "GENERAL",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun calculateScore(): Int {
        var score = 30
        if (isMxVerified && mxStatus == "VALID") score += 30
        if (phone.isNotBlank()) score += 20
        if (social.isNotBlank()) score += 10
        if (category == "BUSINESS" || category == "GOV" || category == "EDU") score += 10
        return score.coerceAtMost(100)
    }
    companion object {
        fun categorise(email: String): String {
            val lower = email.lowercase(Locale.ROOT)
            val parts = lower.split("@")
            if (parts.size < 2) return "OTHER"
            val domain = parts[1]

            return when {
                domain.contains("gmail") -> "GMAIL"
                domain.contains("yahoo") || domain.contains("ymail") -> "YAHOO"
                domain.contains("outlook") || domain.contains("hotmail") || domain.contains("live") || domain.contains("msn") -> "OUTLOOK"
                domain.endsWith(".edu") || domain.contains(".edu.") -> "EDU"
                domain.endsWith(".gov") || domain.contains(".gov.") -> "GOV"
                domain.endsWith(".com") || domain.endsWith(".io") || domain.endsWith(".co") || domain.endsWith(".org") || domain.endsWith(".net") || domain.endsWith(".tech") || domain.endsWith(".ai") -> "BUSINESS"
                else -> "OTHER"
            }
        }

        fun extractDomain(email: String): String {
            val parts = email.split("@")
            return if (parts.size >= 2) parts[1].lowercase(Locale.ROOT) else "unknown"
        }
    }
}
