package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ExtractedEmail
import com.example.data.model.ScrapedEmail
import com.example.data.model.SmtpProfile

@Database(
    entities = [ExtractedEmail::class, SmtpProfile::class, ScrapedEmail::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emailDao(): EmailDao
    abstract fun smtpProfileDao(): SmtpProfileDao
    abstract fun scrapedEmailDao(): ScrapedEmailDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mailist_builder.db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
