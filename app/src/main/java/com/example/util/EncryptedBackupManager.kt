package com.example.util

import android.util.Base64
import com.example.data.model.ExtractedEmail
import com.example.data.model.SmtpProfile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupPackage(
    val emails: List<ExtractedEmail>,
    val smtpProfiles: List<SmtpProfile>,
    val timestamp: Long = System.currentTimeMillis()
)

object EncryptedBackupManager {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_GEN_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 10000

    fun exportEncryptedBackup(backupData: BackupPackage, passkey: String): String {
        val jsonRoot = JSONObject().apply {
            put("timestamp", backupData.timestamp)
            
            // Emails Array
            val emailsArr = JSONArray()
            backupData.emails.forEach { email ->
                emailsArr.put(JSONObject().apply {
                    put("id", email.id)
                    put("email", email.email)
                    put("domain", email.domain)
                    put("category", email.category)
                    put("sourceUrl", email.sourceUrl)
                    put("phone", email.phone)
                    put("social", email.social)
                    put("isMxVerified", email.isMxVerified)
                    put("mxStatus", email.mxStatus)
                    put("leadScore", email.leadScore)
                    put("industryTag", email.industryTag)
                    put("timestamp", email.timestamp)
                })
            }
            put("emails", emailsArr)

            // SMTP Profiles Array
            val profilesArr = JSONArray()
            backupData.smtpProfiles.forEach { prof ->
                profilesArr.put(JSONObject().apply {
                    put("id", prof.id)
                    put("profileName", prof.profileName)
                    put("host", prof.host)
                    put("port", prof.port)
                    put("username", prof.username)
                    put("password", prof.password)
                    put("senderName", prof.senderName)
                    put("isDefault", prof.isDefault)
                })
            }
            put("smtpProfiles", profilesArr)
        }

        val plainJson = jsonRoot.toString()

        // Encryption logic
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val key = deriveKey(passkey, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        // Encrypted Package JSON output with salt and IV
        val encryptedEnvelope = JSONObject().apply {
            put("version", 1)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("payload", Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
        }

        return encryptedEnvelope.toString(2)
    }

    fun restoreEncryptedBackup(encryptedJsonString: String, passkey: String): BackupPackage {
        val envelope = JSONObject(encryptedJsonString)
        val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val payloadBytes = Base64.decode(envelope.getString("payload"), Base64.NO_WRAP)

        val key = deriveKey(passkey, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val decryptedBytes = cipher.doFinal(payloadBytes)
        val decryptedJsonString = String(decryptedBytes, Charsets.UTF_8)

        val jsonRoot = JSONObject(decryptedJsonString)
        val timestamp = jsonRoot.optLong("timestamp", System.currentTimeMillis())

        val emailsList = mutableListOf<ExtractedEmail>()
        val emailsArr = jsonRoot.optJSONArray("emails") ?: JSONArray()
        for (i in 0 until emailsArr.length()) {
            val item = emailsArr.getJSONObject(i)
            emailsList.add(
                ExtractedEmail(
                    id = 0, // Auto-generate new primary key on restore
                    email = item.getString("email"),
                    domain = item.optString("domain", ExtractedEmail.extractDomain(item.getString("email"))),
                    category = item.optString("category", ExtractedEmail.categorise(item.getString("email"))),
                    sourceUrl = item.optString("sourceUrl", "RESTORED_BACKUP"),
                    phone = item.optString("phone", ""),
                    social = item.optString("social", ""),
                    isMxVerified = item.optBoolean("isMxVerified", false),
                    mxStatus = item.optString("mxStatus", "PENDING"),
                    leadScore = item.optInt("leadScore", 50),
                    industryTag = item.optString("industryTag", "GENERAL"),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        val profilesList = mutableListOf<SmtpProfile>()
        val profilesArr = jsonRoot.optJSONArray("smtpProfiles") ?: JSONArray()
        for (i in 0 until profilesArr.length()) {
            val item = profilesArr.getJSONObject(i)
            profilesList.add(
                SmtpProfile(
                    id = 0,
                    profileName = item.optString("profileName", "Restored Profile"),
                    host = item.optString("host", "smtp.gmail.com"),
                    port = item.optInt("port", 587),
                    username = item.optString("username", ""),
                    password = item.optString("password", ""),
                    senderName = item.optString("senderName", "Sales"),
                    isDefault = item.optBoolean("isDefault", false)
                )
            )
        }

        return BackupPackage(
            emails = emailsList,
            smtpProfiles = profilesList,
            timestamp = timestamp
        )
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_GEN_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
