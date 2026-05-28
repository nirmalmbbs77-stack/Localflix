package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.LocalFlixApplication
import com.example.data.local.MediaScanner
import com.example.data.models.LocalVideo
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: VideoRepository,
    private val mediaScanner: MediaScanner
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    val allVideos = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val continueWatching = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val favorites = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded = repository.allVideos // videos are already ordered by date added DESC
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movies = repository.allVideos // Very basic grouping for demo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // A fast way to trigger scanning
    fun scanVideos() {
        if (_isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch {
            try {
                val videos = mediaScanner.scanLocalVideos()
                repository.insertVideos(videos)
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(id, current) }
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as LocalFlixApplication)
                val appContainer = application.container
                MainViewModel(
                    repository = appContainer.videoRepository,
                    mediaScanner = appContainer.mediaScanner
                )
            }
        }
    }
}
