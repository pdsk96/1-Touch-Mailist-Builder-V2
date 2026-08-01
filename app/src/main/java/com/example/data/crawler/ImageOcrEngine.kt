package com.example.data.crawler

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.model.ExtractedEmail
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.regex.Pattern
import kotlin.coroutines.resume

/**
 * Intelligent Image & Document OCR Engine powered by ML Kit Text Recognition.
 * Downloads embedded web images, decodes Base64 images, scans document/canvas media,
 * and extracts text to mine hidden email addresses inside graphic banners, business cards,
 * flyers, and scanned documents.
 */
object ImageOcrEngine {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Regex to match image URLs in web pages (img src, css background-image, og:image, etc.)
    private val imageUrlPattern: Pattern = Pattern.compile(
        "(?:src|background(?:-image)?|data-src|data-lazy|og:image)\\s*=\\s*[\"']?([^\"'\\s>]+\\.(?:png|jpg|jpeg|webp|bmp|gif))[\"']?",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for Base64 image URIs
    private val base64ImagePattern: Pattern = Pattern.compile(
        "data:image/(?:png|jpg|jpeg|webp|bmp);base64,([a-zA-Z0-9+/=]{100,})",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Finds and processes image URLs & Base64 image payloads in HTML content using OCR.
     */
    suspend fun processWebImagesWithOcr(
        html: String,
        baseUrl: String,
        httpClient: OkHttpClient,
        customBlacklistKeywords: Set<String> = emptySet()
    ): List<ExtractedEmail> {
        val extractedEmails = mutableListOf<ExtractedEmail>()
        val processedBitmaps = mutableListOf<Bitmap>()

        try {
            // 1. Extract Base64 Images & perform OCR
            val b64Matcher = base64ImagePattern.matcher(html)
            var b64Count = 0
            while (b64Matcher.find() && b64Count < 5) { // Limit to 5 embedded images per page for speed
                b64Count++
                val b64Data = b64Matcher.group(1) ?: continue
                val bitmap = decodeBase64ToBitmap(b64Data)
                if (bitmap != null) {
                    processedBitmaps.add(bitmap)
                }
            }

            // 2. Extract Image URLs & Download for OCR
            val imageUrls = extractImageUrls(html, baseUrl)
            val topImageUrls = imageUrls.take(5) // Top 5 relevant images per page

            for (imgUrl in topImageUrls) {
                val bitmap = downloadImageAsBitmap(imgUrl, httpClient)
                if (bitmap != null) {
                    processedBitmaps.add(bitmap)
                }
            }

            // 3. Perform OCR on all collected bitmaps
            for (bitmap in processedBitmaps) {
                val textFromOcr = recognizeTextFromBitmap(bitmap)
                if (textFromOcr.isNotBlank()) {
                    val emailsFromOcr = EmailParsingEngine.extractValidEmails(
                        rawContent = textFromOcr,
                        sourceUrl = "$baseUrl [OCR]",
                        customBlacklistKeywords = customBlacklistKeywords
                    )
                    extractedEmails.addAll(emailsFromOcr)
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Handle OCR process gracefully
        }

        return extractedEmails.distinctBy { it.email }
    }

    /**
     * Performs OCR directly on a provided Bitmap object.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume("")
            }
        }
    }

    /**
     * Decodes Base64 string to Android Bitmap.
     */
    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads image from URL and converts to Bitmap.
     */
    private fun downloadImageAsBitmap(url: String, httpClient: OkHttpClient): Bitmap? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val inputStream: InputStream = response.body?.byteStream() ?: return null
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts relative/absolute image URLs from raw HTML.
     */
    private fun extractImageUrls(html: String, baseUrl: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = imageUrlPattern.matcher(html)
        while (matcher.find()) {
            val rawPath = matcher.group(1) ?: continue
            if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
                urls.add(rawPath)
            } else if (rawPath.startsWith("/")) {
                val baseDomain = getBaseDomainUrl(baseUrl)
                urls.add("$baseDomain$rawPath")
            } else {
                urls.add("$baseUrl/$rawPath")
            }
        }
        return urls.distinct()
    }

    private fun getBaseDomainUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            url
        }
    }
}
