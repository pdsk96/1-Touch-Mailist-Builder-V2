package com.example.data.sync

import com.example.data.model.ExtractedEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object CloudWebhookSyncManager {

    suspend fun syncLeadsToWebhook(webhookUrl: String, emails: List<ExtractedEmail>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            return@withContext Pair(false, "Invalid Webhook URL")
        }

        val leadsArray = JSONArray()
        for (item in emails) {
            val obj = JSONObject()
            obj.put("email", item.email)
            obj.put("domain", item.domain)
            obj.put("category", item.category)
            obj.put("phone", item.phone)
            obj.put("social", item.social)
            obj.put("leadScore", item.calculateScore())
            obj.put("mxStatus", item.mxStatus)
            obj.put("sourceUrl", item.sourceUrl)
            leadsArray.put(obj)
        }

        val payloadObj = JSONObject()
        payloadObj.put("event", "LEADS_AUTO_SYNC")
        payloadObj.put("count", emails.size)
        payloadObj.put("timestamp", System.currentTimeMillis())
        payloadObj.put("leads", leadsArray)

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(webhookUrl)
                .post(payloadObj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    return@withContext Pair(true, "Synced ${emails.size} leads successfully!")
                } else {
                    return@withContext Pair(false, "HTTP ${resp.code}: ${resp.message}")
                }
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Sync Error: ${e.message}")
        }
    }
}
