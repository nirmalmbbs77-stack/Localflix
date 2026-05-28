package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.LocalFlixApplication
import com.example.data.models.LocalVideo
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedPlayerViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    private val _currentVideo = MutableStateFlow<LocalVideo?>(null)
    val currentVideo: StateFlow<LocalVideo?> = _currentVideo.asStateFlow()

    fun setVideo(id: Long) {
        viewModelScope.launch {
            repository.getVideoById(id).collect { video ->
                _currentVideo.value = video
            }
        }
    }
    
    fun saveProgress(progressMs: Long) {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            repository.updateProgress(video.id, progressMs)
        }
    }

    fun toggleFavorite(video: LocalVideo) {
        viewModelScope.launch {
            repository.toggleFavorite(video.id, video.isFavorite)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as LocalFlixApplication)
                val repository = application.container.videoRepository
                SharedPlayerViewModel(repository)
            }
        }
    }
}
