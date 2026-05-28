package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.viewmodels.SharedPlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoId: Long,
    viewModel: SharedPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = LocalContext.current as? com.example.MainActivity

    DisposableEffect(Unit) {
        activity?.enterImmersiveMode()
        onDispose {
            activity?.exitImmersiveMode()
        }
    }

    LaunchedEffect(videoId) {
        viewModel.setVideo(videoId)
    }

    val currentVideo by viewModel.currentVideo.collectAsState()

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(currentVideo) {
        currentVideo?.let { video ->
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(video.fileUri))
                    setMediaItem(mediaItem)
                    prepare()
                    
                    if (video.progress > 0 && video.progress < video.duration - 5000) {
                        seekTo(video.progress)
                    }
                    
                    playWhenReady = true
                }
            } else {
                // If it already exists, just handle updating.
            }
        }
    }
    
    // Save progress periodically
    LaunchedEffect(exoPlayer) {
        while(true) {
            delay(5000)
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    viewModel.saveProgress(player.currentPosition)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.pause()
                    exoPlayer?.let { player ->
                        viewModel.saveProgress(player.currentPosition)
                    }
                }
                Lifecycle.Event.ON_RESUME -> exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.let {
                viewModel.saveProgress(it.currentPosition)
                it.release()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
