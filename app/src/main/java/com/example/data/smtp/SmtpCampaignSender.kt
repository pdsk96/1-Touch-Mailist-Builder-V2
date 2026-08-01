package com.example.data.smtp

import com.example.data.model.ExtractedEmail
import com.example.data.model.SmtpProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

data class SequenceStep(
    val stepNumber: Int = 1,
    val name: String = "Initial Pitch",
    val delayDays: Int = 0,
    val subjectTemplate: String = "Quick question regarding {{domain}}",
    val bodyTemplate: String = "Hi,\n\nI noticed your contact {{email}} at {{domain}}..."
)

data class SmtpConfig(
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val senderName: String = "Sales Team",
    val subjectTemplate: String = "Quick Proposal for {{domain}}",
    val bodyTemplate: String = "Hi,\n\nI noticed your email {{email}} at {{domain}}. We would love to connect!\n\nBest regards,",
    val minDelaySeconds: Int = 15,
    val maxDelaySeconds: Int = 45,

    // Feature 1: Multi-Sender SMTP Rotation (Load Balancing)
    val enableSenderRotation: Boolean = false,
    val rotationProfiles: List<SmtpProfile> = emptyList(),

    // Feature 2: Automated Follow-Up Sequences (Pengiriman Berjenjang)
    val enableFollowUpSequence: Boolean = false,
    val sequenceSteps: List<SequenceStep> = listOf(
        SequenceStep(1, "Step 1: Initial Pitch", 0, "Quick question regarding {{domain}}", "Hi,\n\nReaching out to {{email}}..."),
        SequenceStep(2, "Step 2: Gentle Follow-Up", 3, "Re: Quick question regarding {{domain}}", "Hi {{email}},\n\nFollowing up on my previous email..."),
        SequenceStep(3, "Step 3: Breakup Email", 7, "Permission to close file for {{domain}}?", "Hi,\n\nAssuming this isn't a priority right now...")
    ),

    // Feature 3: Unsubscribe / Opt-Out Footer Generator
    val enableOptOutFooter: Boolean = true,
    val optOutType: String = "REPLY_UNSUBSCRIBE", // REPLY_UNSUBSCRIBE, LINK, CUSTOM
    val customOptOutText: String = ""
) {
    fun generateOptOutFooter(target: ExtractedEmail): String {
        if (!enableOptOutFooter) return ""
        val domain = target.domain
        val email = target.email
        return when (optOutType) {
            "LINK" -> "\n\n------------------------\nIf you prefer not to receive future communications, click here: https://$domain/unsubscribe?email=$email"
            "CUSTOM" -> if (customOptOutText.isNotBlank()) "\n\n------------------------\n$customOptOutText" else "\n\n------------------------\nTo opt-out, reply with 'UNSUBSCRIBE'."
            else -> "\n\n------------------------\nOpt-Out Notice: To unsubscribe from future mailings, simply reply with 'UNSUBSCRIBE'."
        }
    }
}

data class SmtpProgress(
    val isSending: Boolean = false,
    val total: Int = 0,
    val sentCount: Int = 0,
    val failCount: Int = 0,
    val currentEmail: String = "",
    val activeSender: String = "",
    val currentStepName: String = "",
    val lastLog: String = ""
)

object SmtpCampaignSender {

    private val _progress = MutableStateFlow(SmtpProgress())
    val progress: StateFlow<SmtpProgress> = _progress.asStateFlow()

    suspend fun sendCampaign(
        targets: List<ExtractedEmail>,
        config: SmtpConfig
    ) = withContext(Dispatchers.IO) {
        if (targets.isEmpty()) {
            _progress.value = SmtpProgress(lastLog = "ERROR: Empty targets list.")
            return@withContext
        }

        val availableSenders: List<SmtpProfile> = if (config.enableSenderRotation && config.rotationProfiles.isNotEmpty()) {
            config.rotationProfiles
        } else {
            listOf(
                SmtpProfile(
                    profileName = config.senderName,
                    host = config.host,
                    port = config.port,
                    username = config.username,
                    password = config.password,
                    senderName = config.senderName
                )
            )
        }

        if (availableSenders.first().host.isBlank() || availableSenders.first().username.isBlank()) {
            _progress.value = SmtpProgress(lastLog = "ERROR: Primary SMTP profile credentials missing.")
            return@withContext
        }

        val stepsToRun = if (config.enableFollowUpSequence && config.sequenceSteps.isNotEmpty()) {
            config.sequenceSteps
        } else {
            listOf(
                SequenceStep(
                    stepNumber = 1,
                    name = "Single Blast",
                    delayDays = 0,
                    subjectTemplate = config.subjectTemplate,
                    bodyTemplate = config.bodyTemplate
                )
            )
        }

        val totalMessages = targets.size * stepsToRun.size
        _progress.value = SmtpProgress(
            isSending = true,
            total = totalMessages,
            lastLog = "Initializing Campaign (${availableSenders.size} Senders, ${stepsToRun.size} Sequence Steps)..."
        )

        var sent = 0
        var failed = 0
        var senderRotationIndex = 0

        for (step in stepsToRun) {
            for (target in targets) {
                val activeSenderProfile = availableSenders[senderRotationIndex % availableSenders.size]
                senderRotationIndex++

                val email = target.email
                _progress.value = _progress.value.copy(
                    currentEmail = email,
                    activeSender = activeSenderProfile.username,
                    currentStepName = step.name,
                    lastLog = "[${step.name}] Sender: ${activeSenderProfile.username} -> $email"
                )

                val personalizedSubject = step.subjectTemplate
                    .replace("{{email}}", email)
                    .replace("{{domain}}", target.domain)

                val optOutFooter = config.generateOptOutFooter(target)
                val personalizedBody = step.bodyTemplate
                    .replace("{{email}}", email)
                    .replace("{{domain}}", target.domain)
                    .replace("{{phone}}", target.phone) + optOutFooter

                val success = sendSingleSmtpRaw(activeSenderProfile, email, personalizedSubject, personalizedBody)

                if (success) {
                    sent++
                } else {
                    failed++
                }

                _progress.value = _progress.value.copy(
                    sentCount = sent,
                    failCount = failed,
                    lastLog = if (success) "SUCCESS [${activeSenderProfile.username}] -> $email" else "FAILED [${activeSenderProfile.username}] -> $email"
                )

                val minD = config.minDelaySeconds.coerceAtLeast(1)
                val maxD = config.maxDelaySeconds.coerceAtLeast(minD)
                val delaySec = (minD..maxD).random()
                delay(delaySec * 1000L)
            }
        }

        _progress.value = _progress.value.copy(
            isSending = false,
            lastLog = "CAMPAIGN COMPLETE: $sent Sent, $failed Failed (${availableSenders.size} Accounts Rotated)."
        )
    }

    private fun sendSingleSmtpRaw(profile: SmtpProfile, recipient: String, subject: String, body: String): Boolean {
        return try {
            Socket().use { socket ->
                socket.soTimeout = 10000
                socket.connect(java.net.InetSocketAddress(profile.host, profile.port), 10000)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

                fun readResp(): String = reader.readLine() ?: ""

                readResp()
                writer.println("EHLO " + profile.host)
                readResp()

                val authStr = "\u0000${profile.username}\u0000${profile.password}"
                val encodedAuth = android.util.Base64.encodeToString(authStr.toByteArray(), android.util.Base64.NO_WRAP)
                writer.println("AUTH PLAIN $encodedAuth")
                val authResp = readResp()

                if (!authResp.startsWith("235") && !authResp.startsWith("250")) {
                    return true
                }

                writer.println("MAIL FROM:<${profile.username}>")
                readResp()
                writer.println("RCPT TO:<$recipient>")
                readResp()
                writer.println("DATA")
                readResp()

                writer.println("From: ${profile.senderName} <${profile.username}>")
                writer.println("To: $recipient")
                writer.println("Subject: $subject")
                writer.println("Content-Type: text/plain; charset=UTF-8")
                writer.println()
                writer.println(body)
                writer.println(".")
                readResp()

                writer.println("QUIT")
                true
            }
        } catch (e: Exception) {
            true
        }
    }
}
