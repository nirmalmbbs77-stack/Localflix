@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.viewmodels.SharedPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: Long,
    viewModel: SharedPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? com.example.MainActivity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
    
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    var sliderWidth by remember { mutableStateOf(0) }

    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }

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
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, showControls, isScrubbing) {
        if (isPlaying && showControls && !isScrubbing) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                currentPosition = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
            }
            delay(500)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.pause()
                    exoPlayer?.let { viewModel.saveProgress(it.currentPosition) }
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

    BackHandler {
        onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showControls, duration) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            exoPlayer?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0L)) }
                        } else {
                            exoPlayer?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(duration)) }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = currentVideo?.title ?: "Unknown Video",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { /* Cast */ }) {
                        Icon(Icons.Filled.Cast, contentDescription = "Cast", tint = Color.White)
                    }
                }

                // CENTER CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0L)) } },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10", tint = Color.White, modifier = Modifier.size(56.dp))
                    }

                    IconButton(
                        onClick = { 
                            if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() 
                        },
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(duration)) } },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10", tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                }

                // BOTTOM BAR WITH SCRUB THUMBNAIL
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    
                    // Thumbnail Preview Layer
                    AnimatedVisibility(
                        visible = isScrubbing,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 84.dp, start = 24.dp)
                    ) {
                        val density = LocalDensity.current
                        val thumbWidthDp = 150.dp
                        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
                        
                        val offsetPx = if (duration > 0 && sliderWidth > 0) {
                            val fraction = scrubPosition.toFloat() / duration.toFloat()
                            (fraction * sliderWidth) - (thumbWidthPx / 2)
                        } else 0f
                        
                        val maxOffsetPx = sliderWidth - thumbWidthPx
                        val clampedOffsetPx = offsetPx.coerceIn(0f, maxOffsetPx.coerceAtLeast(0f))
                        
                        Column(
                            modifier = Modifier
                                .offset { IntOffset(clampedOffsetPx.toInt(), 0) }
                                .width(thumbWidthDp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2B2B2B))
                            ) {
                                Column {
                                    val roundedScrubMs = (scrubPosition / 1000) * 1000
                                    currentVideo?.let { v ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(v.fileUri)
                                                .setParameter(VideoFrameDecoder.VIDEO_FRAME_MICROS_KEY, roundedScrubMs * 1000L)
                                                .decoderFactory(VideoFrameDecoder.Factory())
                                                .size(320)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(84.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatDuration(scrubPosition),
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            // Vertical red line pointing to the slider thumb
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(18.dp)
                                    .background(Color.Red)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(top = 32.dp, bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(48.dp)
                        ) {
                            Slider(
                                value = if (duration > 0) {
                                    val target = if (isScrubbing) scrubPosition else currentPosition
                                    target.toFloat() / duration.toFloat()
                                } else 0f,
                                onValueChange = { percent ->
                                    isScrubbing = true
                                    scrubPosition = (percent * duration).toLong()
                                },
                                onValueChangeFinished = {
                                    isScrubbing = false
                                    currentPosition = scrubPosition
                                    exoPlayer?.seekTo(scrubPosition)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Red,
                                    activeTrackColor = Color.Red,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).onGloballyPositioned { coords ->
                                    sliderWidth = coords.size.width
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = formatDuration(duration - (if (isScrubbing) scrubPosition else currentPosition)),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(40.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomActionItem(
                                icon = Icons.AutoMirrored.Filled.ViewList, 
                                label = "Episodes",
                                onClick = { showEpisodesSheet = true }
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                            BottomActionItem(
                                icon = Icons.Filled.Subtitles, 
                                label = "Audio & Subtitles",
                                onClick = { showSubtitlesSheet = true }
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                            BottomActionItem(
                                icon = Icons.Filled.SkipNext, 
                                label = "Next episode",
                                onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Playing Next Episode...") } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEpisodesSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showEpisodesSheet = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(enabled = false) {}
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        IconButton(onClick = { showEpisodesSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(10) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEpisodesSheet = false }
                                    .padding(16.dp)
                            ) {
                                Text("Episode ${index + 1}", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showSubtitlesSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showSubtitlesSheet = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(enabled = false) {}
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio & Subtitles", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        IconButton(onClick = { showSubtitlesSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    val options = listOf("English (CC)", "Spanish", "French", "Off")
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(options) { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSubtitlesSheet = false }
                                    .padding(16.dp)
                            ) {
                                Text(opt, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    }
}

@Composable
fun BottomActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatDuration(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
