package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smtp_profiles")
data class SmtpProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileName: String,
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val senderName: String = "Sales Team",
    val isDefault: Boolean = false
)
