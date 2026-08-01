package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExtractedEmail
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    @Query("SELECT * FROM extracted_emails ORDER BY timestamp DESC")
    fun getAllEmailsFlow(): Flow<List<ExtractedEmail>>

    @Query("SELECT * FROM extracted_emails WHERE category = :category ORDER BY timestamp DESC")
    fun getEmailsByCategoryFlow(category: String): Flow<List<ExtractedEmail>>

    @Query("SELECT * FROM extracted_emails ORDER BY timestamp DESC")
    suspend fun getAllEmailsSync(): List<ExtractedEmail>

    @Query("SELECT COUNT(*) FROM extracted_emails")
    fun getEmailCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmail(email: ExtractedEmail): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmails(emails: List<ExtractedEmail>): List<Long>

    @Query("DELETE FROM extracted_emails")
    suspend fun deleteAll()

    @Query("UPDATE extracted_emails SET isMxVerified = :isVerified, mxStatus = :status WHERE id = :id")
    suspend fun updateMxStatus(id: Long, isVerified: Boolean, status: String)

    @Query("SELECT * FROM extracted_emails WHERE isMxVerified = 1 ORDER BY timestamp DESC")
    fun getMxVerifiedEmailsFlow(): Flow<List<ExtractedEmail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateEmail(email: ExtractedEmail)
}
