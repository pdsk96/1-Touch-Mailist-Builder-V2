package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmtpProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface SmtpProfileDao {
    @Query("SELECT * FROM smtp_profiles ORDER BY id DESC")
    fun getAllProfiles(): Flow<List<SmtpProfile>>

    @Query("SELECT * FROM smtp_profiles ORDER BY id DESC")
    suspend fun getAllProfilesSync(): List<SmtpProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SmtpProfile): Long

    @Delete
    suspend fun deleteProfile(profile: SmtpProfile)

    @Query("DELETE FROM smtp_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)
}
