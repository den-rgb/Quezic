package com.quezic.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quezic.ui.components.AddToPlaylistDialog
import com.quezic.ui.components.MiniPlayer
import com.quezic.ui.navigation.Screen
import com.quezic.ui.navigation.bottomNavItems
import com.quezic.ui.screens.home.HomeScreen
import com.quezic.ui.screens.spotify.ImportSpotifyScreen
import com.quezic.ui.screens.library.LibraryScreen
import com.quezic.ui.screens.player.PlayerScreen
import com.quezic.ui.screens.playlist.PlaylistScreen
import com.quezic.ui.screens.search.SearchScreen
import com.quezic.ui.theme.Gray1
import com.quezic.ui.theme.Gray6
import com.quezic.ui.theme.AccentGreen
import com.quezic.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuezicApp(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val playlistDialogState by playerViewModel.playlistDialogState.collectAsStateWithLifecycle()
    val showMiniPlayer = playerState.currentSong != null && 
        currentDestination?.route != Screen.Player.route

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Add to Playlist Dialog
    if (playlistDialogState.showDialog) {
        AddToPlaylistDialog(
            playlists = playlistDialogState.playlists,
            songInPlaylists = playlistDialogState.songInPlaylists,
            onDismiss = { playerViewModel.hideAddToPlaylistDialog() },
            onAddToPlaylist = { playlistId -> playerViewModel.addSongToPlaylist(playlistId) },
            onRemoveFromPlaylist = { playlistId -> playerViewModel.removeSongFromPlaylist(playlistId) },
            onCreateNewPlaylist = { 
                playerViewModel.hideAddToPlaylistDialog()
                // Could navigate to create playlist here
            }
        )
    }

    // Show error snackbar
    LaunchedEffect(playerState.error) {
        playerState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = com.quezic.ui.theme.Gray5,
                    contentColor = Color.White
                )
            }
        },
        containerColor = Color.Black,
        bottomBar = {
            Column(
                modifier = Modifier.background(Gray6)
            ) {
                // Mini Player
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.playNext() },
                        onClick = { navController.navigate(Screen.Player.route) },
                        onOpenInYouTube = { playerViewModel.openInYouTubeApp() }
                    )
                }
                
                // iOS-style Tab Bar
                NavigationBar(
                    containerColor = Gray6,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == item.screen.route 
                        } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(26.dp)
                                )
                            },
                            label = { 
                                Text(
                                    item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGreen,
                                selectedTextColor = AccentGreen,
                                unselectedIconColor = Gray1,
                                unselectedTextColor = Gray1,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToPlaylist = { playlistId ->
                        navController.navigate(Screen.Playlist.createRoute(playlistId))
                    },
                    onPlaySong = { song -> playerViewModel.playSong(song) }
                )
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    onPlaySong = { song -> playerViewModel.playSong(song) },
                    onAddToQueue = { song -> playerViewModel.addToQueue(song) }
                )
            }
            
            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToPlaylist = { playlistId ->
                        navController.navigate(Screen.Playlist.createRoute(playlistId))
                    },
                    onNavigateToArtist = { artistName ->
                        navController.navigate(Screen.Artist.createRoute(artistName))
                    },
                    onNavigateToAlbum = { albumName ->
                        navController.navigate(Screen.Album.createRoute(albumName))
                    },
                    onNavigateToImportSpotify = {
                        navController.navigate(Screen.ImportSpotify.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onPlaySong = { song -> playerViewModel.playSong(song) },
                    onAddToQueue = { song -> playerViewModel.addToQueue(song) }
                )
            }
            
            composable(Screen.Player.route) {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onShowAddToPlaylist = { song -> playerViewModel.showAddToPlaylistDialog(song) }
                )
            }
            
            composable(
                route = Screen.Playlist.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                PlaylistScreen(
                    playlistId = playlistId,
                    onNavigateBack = { navController.popBackStack() },
                    onPlaySong = { song -> playerViewModel.playSong(song) },
                    onAddToQueue = { song -> playerViewModel.addToQueue(song) },
                    onPlayAll = { songs -> playerViewModel.playQueue(songs) }
                )
            }
            
            composable(Screen.ImportSpotify.route) {
                ImportSpotifyScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlaylist = { playlistId ->
                        navController.navigate(Screen.Playlist.createRoute(playlistId)) {
                            popUpTo(Screen.Library.route)
                        }
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                com.quezic.ui.screens.settings.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
