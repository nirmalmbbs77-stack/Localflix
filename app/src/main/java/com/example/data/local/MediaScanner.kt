package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.data.models.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {
    suspend fun scanLocalVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<LocalVideo>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val query: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)

            while (cursor.moveToNext()) {
                val id = if (idColumn >= 0) cursor.getLong(idColumn) else 0L
                val title = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "Unknown" else "Unknown"
                val duration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
                val path = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                val dateAdded = if (dateColumn >= 0) cursor.getLong(dateColumn) else 0L
                val resolution = if (resolutionColumn >= 0) cursor.getString(resolutionColumn) ?: "Unknown" else "Unknown"

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                // Keep only valid videos > 1 second
                if (duration > 1000) {
                    val folderName = path.substringBeforeLast('/').substringAfterLast('/')
                    
                    videoList.add(
                        LocalVideo(
                            id = id,
                            title = title.substringBeforeLast('.'),
                            duration = duration,
                            resolution = resolution,
                            folderPath = folderName,
                            fileUri = contentUri.toString(),
                            size = size,
                            dateAdded = dateAdded,
                            thumbnailUri = contentUri.toString() // Coil can load video thumbnails using URI
                        )
                    )
                }
            }
        }
        return@withContext videoList
    }
}
