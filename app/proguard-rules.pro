# ProGuard Rules for 1 Touch Mailist Builder

# Keep Room database and DAO implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
}

# Keep Data Models
-keep class com.example.data.model.** { *; }

# OkHttp ProGuard Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines Rules
-keepclassmembers class kotlinx.coroutines.** { *; }

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
