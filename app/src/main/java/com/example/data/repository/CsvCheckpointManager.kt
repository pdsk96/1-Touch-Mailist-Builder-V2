package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ExtractedEmail
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvCheckpointManager(private val context: Context) {

    private val exportDir: File
        get() {
            val dir = File(context.filesDir, "exports")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Creates a checkpoint CSV file with dynamic name:
     * [Total_Email]_[YYYYMMDD_HHmmss].csv
     */
    fun saveCheckpointCsv(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${total}_$timestamp.csv"
        val file = File(exportDir, fileName)

        FileWriter(file, false).use { writer ->
            // CSV Header
            writer.append("ID,Email,Domain,Category,Source_URL,Timestamp\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (item in emails) {
                val timeStr = dateFormat.format(Date(item.timestamp))
                val cleanUrl = item.sourceUrl.replace(",", "%2C")
                writer.append("${item.id},${item.email},${item.domain},${item.category},$cleanUrl,$timeStr\n")
            }
        }
        return file
    }

    /**
     * Exports email list as JSON file
     */
    fun saveCheckpointJson(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${total}_$timestamp.json"
        val file = File(exportDir, fileName)

        FileWriter(file, false).use { writer ->
            writer.append("[\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (i in emails.indices) {
                val item = emails[i]
                val timeStr = dateFormat.format(Date(item.timestamp))
                val comma = if (i < emails.size - 1) "," else ""
                val escapedUrl = item.sourceUrl.replace("\"", "\\\"")
                writer.append("  {\"id\":${item.id},\"email\":\"${item.email}\",\"domain\":\"${item.domain}\",\"category\":\"${item.category}\",\"sourceUrl\":\"$escapedUrl\",\"timestamp\":\"$timeStr\"}$comma\n")
            }
            writer.append("]")
        }
        return file
    }

    /**
     * Exports plain TXT list of email addresses (1 per line for bulk email tools)
     */
    fun saveCheckpointTxt(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${total}_$timestamp.txt"
        val file = File(exportDir, fileName)

        FileWriter(file, false).use { writer ->
            for (item in emails) {
                writer.append("${item.email}\n")
            }
        }
        return file
    }

    /**
     * Get share intent for exported file using FileProvider
     */
    fun getShareIntent(file: File): Intent {
        val mimeType = when {
            file.name.endsWith(".json") -> "application/json"
            file.name.endsWith(".txt") -> "text/plain"
            file.name.endsWith(".html") -> "text/html"
            file.name.endsWith(".pdf") -> "application/pdf"
            else -> "text/csv"
        }
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "1 Touch Mailist Report - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Analytics Report for ${file.name} containing ${file.name.substringBefore("_")} extracted leads.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Exports full Analytics Report in PDF format using Android PdfDocument
     */
    fun saveAnalyticsReportPdf(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "AnalyticsReport_${total}_$timestamp.pdf"
        val file = File(exportDir, fileName)

        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        paint.isAntiAlias = true

        // Header Title
        paint.color = android.graphics.Color.parseColor("#00F2FE")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("PDSK LEAD INTELLIGENCE ANALYTICS REPORT", 40f, 50f, paint)

        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Generated Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}", 40f, 68f, paint)

        // Divider Line
        paint.color = android.graphics.Color.parseColor("#334155")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 80f, 555f, 80f, paint)

        // Summary Metric Cards Box
        paint.color = android.graphics.Color.parseColor("#1E293B")
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(40f, 95f, 555f, 165f, 8f, 8f, paint)

        val mxVerifiedCount = emails.count { it.isMxVerified || it.mxStatus == "VALID" }
        val avgQualityScore = if (emails.isNotEmpty()) emails.map { it.calculateScore() }.average().toInt() else 0
        val categories = emails.groupBy { it.category }

        paint.color = android.graphics.Color.parseColor("#38EF7D")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("$total", 60f, 125f, paint)
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Total Leads", 60f, 142f, paint)

        paint.color = android.graphics.Color.parseColor("#38EF7D")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("$avgQualityScore%", 180f, 125f, paint)
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Avg Score", 180f, 142f, paint)

        paint.color = android.graphics.Color.parseColor("#38EF7D")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("$mxVerifiedCount", 300f, 125f, paint)
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("MX Valid", 300f, 142f, paint)

        paint.color = android.graphics.Color.parseColor("#38EF7D")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("${categories.size}", 420f, 125f, paint)
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Categories", 420f, 142f, paint)

        // Table Header
        var yPos = 195f
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawRect(40f, yPos - 15f, 555f, yPos + 10f, paint)

        paint.color = android.graphics.Color.parseColor("#00F2FE")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("#", 45f, yPos, paint)
        canvas.drawText("Email Address", 75f, yPos, paint)
        canvas.drawText("Domain", 260f, yPos, paint)
        canvas.drawText("Category", 380f, yPos, paint)
        canvas.drawText("Score", 470f, yPos, paint)
        canvas.drawText("MX", 515f, yPos, paint)

        yPos += 20f
        paint.isFakeBoldText = false

        // Table Rows (up to 25 leads per PDF page)
        emails.take(25).forEachIndexed { index, email ->
            paint.color = if (index % 2 == 0) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.parseColor("#1E293B")
            canvas.drawRect(40f, yPos - 12f, 555f, yPos + 8f, paint)

            paint.color = android.graphics.Color.parseColor("#E2E8F0")
            paint.textSize = 9f
            canvas.drawText("${index + 1}", 45f, yPos, paint)
            canvas.drawText(email.email.take(28), 75f, yPos, paint)
            canvas.drawText(email.domain.take(18), 260f, yPos, paint)
            canvas.drawText(email.category.take(14), 380f, yPos, paint)

            val score = email.calculateScore()
            paint.color = if (score >= 70) android.graphics.Color.parseColor("#38EF7D") else android.graphics.Color.parseColor("#FFB000")
            canvas.drawText("$score%", 470f, yPos, paint)

            paint.color = if (email.mxStatus == "VALID" || email.isMxVerified) android.graphics.Color.parseColor("#38EF7D") else android.graphics.Color.parseColor("#94A3B8")
            canvas.drawText(email.mxStatus.take(6), 515f, yPos, paint)

            yPos += 20f
        }

        // Footer
        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 8f
        canvas.drawText("PDSK Lead Intelligence Extractor - Confidential & Automated Report", 40f, 820f, paint)

        pdfDocument.finishPage(page)
        try {
            pdfDocument.writeTo(file.outputStream())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file
    }

    /**
     * Exports full Analytics Report in HTML/PDF printable visual format
     */
    fun saveAnalyticsReportHtml(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "AnalyticsReport_${total}_$timestamp.html"
        val file = File(exportDir, fileName)

        val categories = emails.groupBy { it.category }
        val mxVerifiedCount = emails.count { it.isMxVerified || it.mxStatus == "VALID" }
        val avgQualityScore = if (emails.isNotEmpty()) emails.map { it.calculateScore() }.average().toInt() else 0

        FileWriter(file, false).use { writer ->
            writer.append("""
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="utf-8">
                <title>Lead Intelligence & Analytics Report</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0c0f1d; color: #e2e8f0; margin: 0; padding: 20px; }
                    .card { background: #171c2f; border: 1px solid #00f2fe; border-radius: 10px; padding: 20px; margin-bottom: 20px; }
                    h1 { color: #00f2fe; margin-top: 0; font-size: 22px; }
                    .grid { display: flex; gap: 15px; margin-bottom: 20px; }
                    .metric { background: #0f1424; border: 1px solid #38ef7d; border-radius: 8px; padding: 15px; flex: 1; text-align: center; }
                    .metric-val { font-size: 24px; font-weight: bold; color: #38ef7d; }
                    .metric-label { font-size: 11px; color: #94a3b8; text-transform: uppercase; margin-top: 5px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 12px; }
                    th, td { padding: 10px; text-align: left; border-bottom: 1px solid #232a42; }
                    th { background: #1e2640; color: #00f2fe; }
                    tr:nth-child(even) { background: #13182b; }
                    .badge { padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: bold; }
                    .badge-high { background: #15803d; color: #ffffff; }
                    .badge-mid { background: #b45309; color: #ffffff; }
                    .badge-low { background: #b91c1c; color: #ffffff; }
                </style>
                </head>
                <body>
                <div class="card">
                    <h1>📊 PDSK LEAD INTELLIGENCE ANALYTICS REPORT</h1>
                    <p style="color: #94a3b8; font-size: 12px;">Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}</p>
                    
                    <div class="grid">
                        <div class="metric">
                            <div class="metric-val">$total</div>
                            <div class="metric-label">Total Leads Extracted</div>
                        </div>
                        <div class="metric">
                            <div class="metric-val">$avgQualityScore%</div>
                            <div class="metric-label">Avg Quality Score</div>
                        </div>
                        <div class="metric">
                            <div class="metric-val">$mxVerifiedCount</div>
                            <div class="metric-label">MX Verified Valid</div>
                        </div>
                        <div class="metric">
                            <div class="metric-val">${categories.size}</div>
                            <div class="metric-label">Domain Categories</div>
                        </div>
                    </div>

                    <h3 style="color: #ff007f;">CATEGORY DISTRIBUTION</h3>
                    <ul>
            """.trimIndent())

            categories.forEach { (cat, list) ->
                val pct = if (total > 0) (list.size * 100) / total else 0
                writer.append("<li><strong>$cat:</strong> ${list.size} leads ($pct%)</li>\n")
            }

            writer.append("""
                    </ul>

                    <h3 style="color: #00f2fe;">EXTRACTED LEAD TARGET LIST</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Email Address</th>
                                <th>Domain</th>
                                <th>Category</th>
                                <th>Quality Score</th>
                                <th>MX Status</th>
                                <th>Phone</th>
                            </tr>
                        </thead>
                        <tbody>
            """.trimIndent())

            emails.forEachIndexed { idx, item ->
                val score = item.calculateScore()
                val badgeClass = when {
                    score >= 70 -> "badge-high"
                    score >= 40 -> "badge-mid"
                    else -> "badge-low"
                }
                writer.append("""
                    <tr>
                        <td>${idx + 1}</td>
                        <td><strong>${item.email}</strong></td>
                        <td>${item.domain}</td>
                        <td>${item.category}</td>
                        <td><span class="badge $badgeClass">$score%</span></td>
                        <td>${item.mxStatus}</td>
                        <td>${if (item.phone.isBlank()) "-" else item.phone}</td>
                    </tr>
                """.trimIndent())
            }

            writer.append("""
                        </tbody>
                    </table>
                </div>
                </body>
                </html>
            """.trimIndent())
        }
        return file
    }

    /**
     * Exports Excel-compatible Report CSV with quality metrics summary sheet block
     */
    fun saveAnalyticsReportExcelCsv(emails: List<ExtractedEmail>): File {
        val total = emails.size
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "ExcelAnalytics_${total}_$timestamp.csv"
        val file = File(exportDir, fileName)

        val mxVerifiedCount = emails.count { it.isMxVerified || it.mxStatus == "VALID" }
        val avgQualityScore = if (emails.isNotEmpty()) emails.map { it.calculateScore() }.average().toInt() else 0

        FileWriter(file, false).use { writer ->
            writer.append("PDSK LEAD INTELLIGENCE REPORT\n")
            writer.append("Generated Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            writer.append("Total Extracted Leads,$total\n")
            writer.append("MX Deliverable Verified,$mxVerifiedCount\n")
            writer.append("Average Quality Score,$avgQualityScore%\n\n")

            // Table Header
            writer.append("ID,Email Address,Domain,Category,Quality Score,MX Status,Phone,Social,Source URL\n")
            for (item in emails) {
                val score = item.calculateScore()
                val cleanUrl = item.sourceUrl.replace(",", "%2C")
                val cleanPhone = item.phone.replace(",", " ")
                writer.append("${item.id},${item.email},${item.domain},${item.category},$score%,${item.mxStatus},$cleanPhone,${item.social},$cleanUrl\n")
            }
        }
        return file
    }

    /**
     * List all previously saved checkpoints
     */
    fun listSavedCheckpoints(): List<File> {
        val files = exportDir.listFiles()
        return files?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

