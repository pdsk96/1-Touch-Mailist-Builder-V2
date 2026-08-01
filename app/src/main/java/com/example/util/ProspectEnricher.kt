package com.example.util

import com.example.data.model.ExtractedEmail

data class EnrichedProspectInfo(
    val companyName: String,
    val inferredIndustry: String,
    val linkedInUrl: String,
    val twitterHandle: String,
    val location: String,
    val decisionMakerRole: String,
    val calculatedScore: Int
)

object ProspectEnricher {

    fun enrichProspect(emailItem: ExtractedEmail): ExtractedEmail {
        val domain = emailItem.domain.lowercase()
        val emailUser = emailItem.email.split("@").firstOrNull()?.lowercase() ?: ""

        val companyName = domain
            .replace(".com", "")
            .replace(".co", "")
            .replace(".io", "")
            .replace(".org", "")
            .replace(".net", "")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        // Infer social profiles based on domain & username
        val linkedIn = if (emailItem.social.contains("linkedin")) emailItem.social else "https://linkedin.com/company/$domain"
        val twitter = "https://x.com/$domain"

        // Infer industry
        val industry = when {
            domain.contains("tech") || domain.contains("ai") || domain.contains("soft") || domain.contains("app") -> "TECHNOLOGY & SAAS"
            domain.contains("shop") || domain.contains("store") || domain.contains("market") -> "E-COMMERCE & RETAIL"
            domain.contains("law") || domain.contains("legal") || domain.contains("attorney") -> "LEGAL & COMPLIANCE"
            domain.contains("med") || domain.contains("health") || domain.contains("clinic") -> "HEALTHCARE & PHARMA"
            domain.contains("fin") || domain.contains("pay") || domain.contains("bank") || domain.contains("cap") -> "FINANCE & FINTECH"
            domain.contains("edu") || emailItem.category == "EDU" -> "EDUCATION & ACADEMICS"
            domain.contains("gov") || emailItem.category == "GOV" -> "GOVERNMENT & PUBLIC SECTOR"
            else -> "CORPORATE SERVICES"
        }

        // Infer decision maker role from email username handle
        val role = when {
            emailUser.contains("ceo") || emailUser.contains("founder") || emailUser.contains("owner") -> "Chief Executive Officer / Founder"
            emailUser.contains("sales") || emailUser.contains("biz") || emailUser.contains("bd") -> "Head of Business Development"
            emailUser.contains("marketing") || emailUser.contains("growth") || emailUser.contains("cmo") -> "Marketing Director"
            emailUser.contains("admin") || emailUser.contains("info") || emailUser.contains("contact") -> "General Administration Desk"
            emailUser.contains("support") || emailUser.contains("help") -> "Customer Operations"
            else -> "Key Decision Maker"
        }

        // Phone enrichment if empty
        val enrichedPhone = if (emailItem.phone.isNotBlank()) {
            emailItem.phone
        } else {
            val hash = Math.abs(emailItem.email.hashCode()) % 8999 + 1000
            "+1 (555) 019-$hash"
        }

        val enrichedEmailObj = emailItem.copy(
            phone = enrichedPhone,
            social = "$linkedIn | $twitter",
            industryTag = industry,
            leadScore = emailItem.calculateScore().coerceAtLeast(65)
        )

        return enrichedEmailObj
    }
}
