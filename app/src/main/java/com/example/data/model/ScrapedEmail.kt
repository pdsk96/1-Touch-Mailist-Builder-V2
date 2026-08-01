package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "scraped_emails",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["domain"]),
        Index(value = ["category"])
    ]
)
data class ScrapedEmail(
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

    fun toExtractedEmail(): ExtractedEmail {
        return ExtractedEmail(
            id = id,
            email = email,
            domain = domain,
            category = category,
            sourceUrl = sourceUrl,
            phone = phone,
            social = social,
            isMxVerified = isMxVerified,
            mxStatus = mxStatus,
            leadScore = leadScore,
            industryTag = industryTag,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromExtractedEmail(extracted: ExtractedEmail): ScrapedEmail {
            return ScrapedEmail(
                id = extracted.id,
                email = extracted.email,
                domain = extracted.domain,
                category = extracted.category,
                sourceUrl = extracted.sourceUrl,
                phone = extracted.phone,
                social = extracted.social,
                isMxVerified = extracted.isMxVerified,
                mxStatus = extracted.mxStatus,
                leadScore = extracted.leadScore,
                industryTag = extracted.industryTag,
                timestamp = extracted.timestamp
            )
        }

        fun categorise(email: String): String {
            return ExtractedEmail.categorise(email)
        }

        fun extractDomain(email: String): String {
            return ExtractedEmail.extractDomain(email)
        }
    }
}
