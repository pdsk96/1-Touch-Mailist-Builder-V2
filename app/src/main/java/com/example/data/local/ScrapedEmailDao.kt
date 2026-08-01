package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScrapedEmail
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapedEmailDao {

    @Query("SELECT * FROM scraped_emails ORDER BY timestamp DESC")
    fun getAllScrapedEmailsFlow(): Flow<List<ScrapedEmail>>

    @Query("SELECT * FROM scraped_emails WHERE category = :category ORDER BY timestamp DESC")
    fun getScrapedEmailsByCategoryFlow(category: String): Flow<List<ScrapedEmail>>

    @Query("SELECT * FROM scraped_emails WHERE domain = :domain ORDER BY timestamp DESC")
    fun getScrapedEmailsByDomainFlow(domain: String): Flow<List<ScrapedEmail>>

    @Query("SELECT * FROM scraped_emails WHERE id = :id")
    suspend fun getScrapedEmailById(id: Long): ScrapedEmail?

    @Query("SELECT * FROM scraped_emails ORDER BY timestamp DESC")
    suspend fun getAllScrapedEmailsSync(): List<ScrapedEmail>

    @Query("SELECT COUNT(*) FROM scraped_emails")
    fun getScrapedEmailCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScrapedEmail(email: ScrapedEmail): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScrapedEmails(emails: List<ScrapedEmail>): List<Long>

    @Update
    suspend fun updateScrapedEmail(email: ScrapedEmail)

    @Delete
    suspend fun deleteScrapedEmail(email: ScrapedEmail)

    @Query("DELETE FROM scraped_emails WHERE id = :id")
    suspend fun deleteScrapedEmailById(id: Long)

    @Query("DELETE FROM scraped_emails WHERE id IN (:ids)")
    suspend fun deleteScrapedEmailsByIds(ids: List<Long>)

    @Query("DELETE FROM scraped_emails")
    suspend fun deleteAllScrapedEmails()

    @Query("UPDATE scraped_emails SET isMxVerified = :isVerified, mxStatus = :status WHERE id = :id")
    suspend fun updateMxStatus(id: Long, isVerified: Boolean, status: String)

    @Query("SELECT * FROM scraped_emails WHERE isMxVerified = 1 ORDER BY timestamp DESC")
    fun getMxVerifiedEmailsFlow(): Flow<List<ScrapedEmail>>
}
