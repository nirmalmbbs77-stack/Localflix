package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.LocalVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<LocalVideo>>

    @Query("SELECT * FROM videos WHERE progress > 0 AND progress < duration ORDER BY dateAdded DESC")
    fun getContinueWatching(): Flow<List<LocalVideo>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<LocalVideo>>
    
    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%'")
    fun searchVideos(query: String): Flow<List<LocalVideo>>
    
    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun getVideoById(id: Long): Flow<LocalVideo?>

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Don't override progress and favorites
    suspend fun insertVideos(videos: List<LocalVideo>)

    @Update
    suspend fun updateVideo(video: LocalVideo)
    
    @Query("UPDATE videos SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Long)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
