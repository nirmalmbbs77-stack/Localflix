package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.LocalVideo
import com.example.ui.components.HeroBanner
import com.example.ui.components.VideoRow
import com.example.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val allVideos by viewModel.allVideos.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
    val heroVideo = allVideos.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LocalFlix", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Optional to not pad top so banner goes under top bar.. actually let's just keep padding for now
        ) {
            item {
                HeroBanner(
                    video = heroVideo,
                    onPlayClick = { heroVideo?.let { onNavigateToPlayer(it.id) } },
                    onInfoClick = { heroVideo?.let { onNavigateToDetails(it.id) } }
                )
            }
            
            if (continueWatching.isNotEmpty()) {
                item {
                    VideoRow(
                        title = "Continue Watching",
                        videos = continueWatching,
                        onVideoClick = { onNavigateToDetails(it.id) }
                    )
                }
            }
            
            if (favorites.isNotEmpty()) {
                item {
                    VideoRow(
                        title = "My List",
                        videos = favorites,
                        onVideoClick = { onNavigateToDetails(it.id) }
                    )
                }
            }

            if (allVideos.isNotEmpty()) {
                item {
                    VideoRow(
                        title = "Recently Added",
                        videos = allVideos.take(15), // Show latest
                        onVideoClick = { onNavigateToDetails(it.id) }
                    )
                }
                
                // Example of creating pseudo-categories dynamically based on folders
                val groupedByFolder = allVideos.groupBy { it.folderPath }
                for ((folder, videos) in groupedByFolder) {
                    if (videos.size > 1) {
                        item {
                            VideoRow(
                                title = folder,
                                videos = videos,
                                onVideoClick = { onNavigateToDetails(it.id) }
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No videos found on device", color = Color.Gray)
                    }
                }
            }
        }
    }
}
