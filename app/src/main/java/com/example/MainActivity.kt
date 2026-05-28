package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.DetailsRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.PlayerRoute
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodels.MainViewModel
import com.example.viewmodels.SharedPlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val sharedPlayerViewModel: SharedPlayerViewModel = viewModel(factory = SharedPlayerViewModel.Factory)
                
                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                
                val storagePermissionState = rememberPermissionState(permissionToRequest)
                
                LaunchedEffect(storagePermissionState.status) {
                    if (storagePermissionState.status.isGranted) {
                        mainViewModel.scanVideos()
                    } else {
                        storagePermissionState.launchPermissionRequest()
                    }
                }

                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (storagePermissionState.status.isGranted) {
                        NavHost(navController = navController, startDestination = HomeRoute) {
                            composable<HomeRoute> {
                                com.example.ui.screens.HomeScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) },
                                    onNavigateToPlayer = { id -> navController.navigate(PlayerRoute(id)) },
                                    onNavigateToSearch = { navController.navigate(com.example.ui.navigation.SearchRoute) }
                                )
                            }
                            composable<DetailsRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<DetailsRoute>()
                                com.example.ui.screens.DetailsScreen(
                                    videoId = route.videoId,
                                    viewModel = sharedPlayerViewModel,
                                    onNavigateUp = { navController.navigateUp() },
                                    onPlayClick = { id -> navController.navigate(PlayerRoute(id)) }
                                )
                            }
                            composable<PlayerRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<PlayerRoute>()
                                com.example.ui.screens.PlayerScreen(
                                    videoId = route.videoId,
                                    viewModel = sharedPlayerViewModel,
                                    onBack = { navController.navigateUp() }
                                )
                            }
                            composable<com.example.ui.navigation.SearchRoute> {
                                com.example.ui.screens.SearchScreen(
                                    viewModel = mainViewModel,
                                    onNavigateUp = { navController.navigateUp() },
                                    onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Storage permission is required to find local videos.", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
    
    fun enterImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun exitImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

