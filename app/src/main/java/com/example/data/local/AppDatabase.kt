package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.models.LocalVideo

@Database(entities = [LocalVideo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
