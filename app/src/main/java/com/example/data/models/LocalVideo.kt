package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "videos")
data class LocalVideo(
    @PrimaryKey
    val id: Long, // MediaStore ID
    val title: String,
    val duration: Long,
    val resolution: String,
    val folderPath: String,
    val fileUri: String,
    val size: Long,
    val dateAdded: Long,
    val progress: Long = 0L, // How much user has watched in ms
    val isFavorite: Boolean = false,
    val thumbnailUri: String = "" // Added thumbnail uri if needed
)
