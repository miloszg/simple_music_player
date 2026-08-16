package app.plainly.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.plainly.music.playback.PlaybackState
import app.plainly.music.ui.library.LibraryTabContent
import app.plainly.music.ui.library.LibraryViewModel
import app.plainly.music.ui.player.MiniPlayer
import app.plainly.music.ui.player.NowPlayingSheet
import app.plainly.music.ui.player.PlayerViewModel
import app.plainly.music.ui.player.rememberPlaybackPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainlyAppScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        libraryViewModel.start()
        playerViewModel.connect()
    }

    val library by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()
    val favourites by playerViewModel.favourites.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(LibraryTab.Songs) }
    var showPlayer by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.label) },
                actions = {
                    IconButton(onClick = { /* search lands with the search screen */ }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* settings lands with the settings screen */ }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                MiniPlayerBar(playback, playerViewModel) { showPlayer = true }
                NavigationBar {
                    LibraryTab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(entry.label) },
                        )
                    }
                }
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize()) {
            LibraryTabContent(
                tab = tab,
                state = library,
                // The scaffold's bottom inset already accounts for the nav bar
                // and mini player; passing it as list padding rather than a
                // Modifier keeps the last row scrollable clear of them instead
                // of being clipped behind.
                contentPadding = insets.plus(bottom = 8.dp),
                onSongClick = { index -> libraryViewModel.playAll(library.songs, index) },
                onAlbumClick = { album ->
                    libraryViewModel.playAll(libraryViewModel.index.value.songsOfAlbum(album.key), 0)
                },
                onArtistClick = { artist ->
                    libraryViewModel.playAll(libraryViewModel.index.value.songsOfArtist(artist.key), 0)
                },
                onFolderClick = { folder ->
                    libraryViewModel.playAll(libraryViewModel.index.value.songsOfFolder(folder.path), 0)
                },
            )
        }
    }

    if (showPlayer) {
        BackHandler { showPlayer = false }
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            sheetState = sheetState,
            dragHandle = {
                IconButton(onClick = { showPlayer = false }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse player")
                }
            },
        ) {
            val position by rememberPlaybackPosition(playback.isPlaying, playerViewModel::positionMs)
            NowPlayingSheet(
                state = playback,
                positionMs = position,
                isFavourite = playback.current?.contentKey in favourites,
                onSeek = playerViewModel::seekTo,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onPrevious = playerViewModel::previous,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeat,
                onToggleFavourite = {
                    playback.current?.let { playerViewModel.toggleFavourite(it.contentKey) }
                },
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    playback: PlaybackState,
    viewModel: PlayerViewModel,
    onExpand: () -> Unit,
) {
    val position by rememberPlaybackPosition(playback.isPlaying, viewModel::positionMs)
    MiniPlayer(
        state = playback,
        progress = if (playback.durationMs > 0) {
            (position.toFloat() / playback.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        },
        onExpand = onExpand,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
    )
}

/** Adds to a [PaddingValues] without discarding the insets already in it. */
@Composable
private fun PaddingValues.plus(bottom: androidx.compose.ui.unit.Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
        bottom = calculateBottomPadding() + bottom,
    )
}
