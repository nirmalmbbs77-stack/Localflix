package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.MediaScanner
import com.example.data.repository.VideoRepository

interface AppContainer {
    val videoRepository: VideoRepository
    val mediaScanner: MediaScanner
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "local_flix_db")
            .build()
    }
    
    override val videoRepository: VideoRepository by lazy {
        VideoRepository(database.videoDao())
    }
    
    override val mediaScanner: MediaScanner by lazy {
        MediaScanner(context)
    }
}
