package com.example.data.repository

import com.example.data.local.VideoDao
import com.example.data.models.LocalVideo
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {
    val allVideos: Flow<List<LocalVideo>> = videoDao.getAllVideos()
    val continueWatching: Flow<List<LocalVideo>> = videoDao.getContinueWatching()
    val favorites: Flow<List<LocalVideo>> = videoDao.getFavorites()
    
    fun searchVideos(query: String): Flow<List<LocalVideo>> = videoDao.searchVideos(query)
    
    fun getVideoById(id: Long): Flow<LocalVideo?> = videoDao.getVideoById(id)
    
    suspend fun insertVideos(videos: List<LocalVideo>) = videoDao.insertVideos(videos)
    
    suspend fun updateVideo(video: LocalVideo) = videoDao.updateVideo(video)
    
    suspend fun updateProgress(id: Long, progress: Long) = videoDao.updateProgress(id, progress)
    
    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        videoDao.updateFavorite(id, !currentFavorite)
    }
}
