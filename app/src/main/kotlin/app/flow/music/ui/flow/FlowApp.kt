package app.flow.music.ui.flow

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.flow.music.domain.model.Song
import app.flow.music.ui.library.LibraryViewModel
import app.flow.music.ui.player.PlayerViewModel
import app.flow.music.ui.player.rememberPlaybackPosition
import app.flow.music.ui.playlist.CreatePlaylistDialog
import app.flow.music.ui.playlist.DeletePlaylistDialog
import app.flow.music.ui.playlist.PickPlaylistSheet
import app.flow.music.ui.playlist.PlaylistDialog
import app.flow.music.ui.playlist.PlaylistViewModel
import app.flow.music.ui.playlist.RenamePlaylistDialog
import app.flow.music.ui.settings.SettingsViewModel
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.ThemeMode

/** Which full-screen surface is covering the tabs, if any. */
private sealed interface Overlay {
    data object None : Overlay
    data object Player : Overlay
    data object Settings : Overlay
    data class Detail(val content: FlowDetailContent) : Overlay
}

@Composable
fun FlowApp(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        libraryViewModel.start()
        playerViewModel.connect()
    }

    val colors = Flow.colors
    val home by libraryViewModel.flowHome.collectAsStateWithLifecycle()
    val library by libraryViewModel.flowLibrary.collectAsStateWithLifecycle()
    val favourites by libraryViewModel.favourites.collectAsStateWithLifecycle()
    val query by libraryViewModel.query.collectAsStateWithLifecycle()
    val results by libraryViewModel.searchResults.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val dialog by playlistViewModel.dialog.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.all.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(FlowTab.Home) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.Albums) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var queueOpen by remember { mutableStateOf(false) }
    var sleepOn by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableStateOf(0) }
    var actionSong by remember { mutableStateOf<Song?>(null) }

    val index = libraryViewModel.index

    fun openAlbumDetail(albumKey: Long) {
        val album = index.value.album(albumKey) ?: return
        overlay = Overlay.Detail(
            FlowDetailContent(
                title = album.name,
                artist = album.albumArtist,
                subtitle = listOfNotNull(album.year?.toString(), songCount(album.songCount))
                    .joinToString(" · "),
                artworkSongId = album.artworkSongId,
                tracks = index.value.songsOfAlbum(albumKey),
            ),
        )
    }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            FlowHeader(onOpenSettings = { overlay = Overlay.Settings })

            val listPadding = PaddingValues(bottom = 16.dp)

            Box(Modifier.weight(1f)) {
                when (tab) {
                    FlowTab.Home -> if (home.isEmpty) {
                        FlowEmpty(
                            title = "Nothing here yet",
                            body = "Flow plays what's saved on this phone. Copy some music across " +
                                "and rescan from Settings.",
                            actionLabel = "Rescan",
                            onAction = libraryViewModel::rescan,
                        )
                    } else {
                        FlowHome(
                            content = home,
                            contentPadding = listPadding,
                            onOpenAlbum = { openAlbumDetail(it.key) },
                            onPlayHero = {
                                home.hero?.let {
                                    libraryViewModel.playAll(index.value.songsOfAlbum(it.key), 0)
                                }
                                overlay = Overlay.Player
                            },
                            onOpenPlaylist = { },
                            onPlaySong = { song ->
                                libraryViewModel.playAll(listOf(song), 0)
                                overlay = Overlay.Player
                            },
                            onSongMenu = { actionSong = it },
                        )
                    }

                    FlowTab.Search -> FlowSearch(
                        query = query,
                        results = results,
                        browse = library.albums.map { it.name }.distinct().take(6),
                        contentPadding = listPadding,
                        onQueryChange = libraryViewModel::onQueryChange,
                        onBrowse = libraryViewModel::onQueryChange,
                        onOpenAlbum = { openAlbumDetail(it.key) },
                        onPlaySong = { song ->
                            libraryViewModel.playAll(listOf(song), 0)
                            overlay = Overlay.Player
                        },
                    )

                    FlowTab.Library -> FlowLibrary(
                        content = library,
                        filter = filter,
                        contentPadding = listPadding,
                        onFilter = { filter = it },
                        onOpenLiked = {
                            val liked = index.value.songs.filter { it.contentKey in favourites }
                            overlay = Overlay.Detail(
                                FlowDetailContent(
                                    title = "Liked songs",
                                    artist = "Everything you've liked",
                                    subtitle = songCount(liked.size),
                                    artworkSongId = null,
                                    tracks = liked,
                                    cherryPlate = true,
                                ),
                            )
                        },
                        onOpenAlbum = { openAlbumDetail(it.key) },
                        onOpenPlaylist = { playlist ->
                            overlay = Overlay.Detail(
                                FlowDetailContent(
                                    title = playlist.name,
                                    artist = "Made by you",
                                    subtitle = songCount(playlist.songCount),
                                    artworkSongId = playlist.artworkSongId,
                                    tracks = emptyList(),
                                ),
                            )
                        },
                        onOpenArtist = { artist ->
                            overlay = Overlay.Detail(
                                FlowDetailContent(
                                    title = artist.name,
                                    artist = "Artist",
                                    subtitle = songCount(artist.songCount),
                                    artworkSongId = artist.artworkSongId,
                                    tracks = index.value.songsOfArtist(artist.key),
                                ),
                            )
                        },
                        onPlaySong = { song ->
                            libraryViewModel.playAll(listOf(song), 0)
                            overlay = Overlay.Player
                        },
                    )
                }
            }

            FlowMiniPlayerBar(playerViewModel) { overlay = Overlay.Player }
            FlowTabBar(selected = tab, onSelect = { tab = it })
        }

        // Overlays. The design slides them up over everything including the
        // tabs, which is why they live outside the column rather than in it.
        SlideUp(visible = overlay is Overlay.Player) {
            val position by rememberPlaybackPosition(playback.isPlaying, playerViewModel::positionMs)
            BackHandler { overlay = Overlay.None }
            FlowNowPlaying(
                state = playback,
                positionMs = position,
                isFavourite = playback.current?.contentKey in favourites,
                queueOpen = queueOpen,
                sleepOn = sleepOn,
                speedLabel = SPEEDS[speedIndex],
                onClose = { overlay = Overlay.None; queueOpen = false },
                onToggleQueue = { queueOpen = !queueOpen },
                onSeekFraction = { playerViewModel.seekTo((it * playback.durationMs).toLong()) },
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onPrevious = playerViewModel::previous,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeat,
                onToggleFavourite = {
                    playback.current?.let { playerViewModel.toggleFavourite(it.contentKey) }
                },
                onToggleSleep = { sleepOn = !sleepOn },
                onCycleSpeed = { speedIndex = (speedIndex + 1) % SPEEDS.size },
                onOpenAlbum = {
                    playback.current?.let { openAlbumDetail(it.albumKey) }
                    overlay = Overlay.Detail((overlay as? Overlay.Detail)?.content ?: return@FlowNowPlaying)
                },
                onQueueItem = { playerViewModel.skipTo(it) },
            )
        }

        SlideUp(visible = overlay is Overlay.Settings) {
            BackHandler { overlay = Overlay.None }
            FlowSettings(
                dark = settings.themeMode != ThemeMode.Light,
                followSystem = settings.themeMode == ThemeMode.System,
                settings = emptyList(),
                onClose = { overlay = Overlay.None },
                onSetDark = { settingsViewModel.setThemeMode(ThemeMode.Dark) },
                onSetLight = { settingsViewModel.setThemeMode(ThemeMode.Light) },
                onRescan = { settingsViewModel.rescan() },
            )
        }

        val detail = overlay as? Overlay.Detail
        SlideUp(visible = detail != null) {
            detail?.let { d ->
                BackHandler { overlay = Overlay.None }
                FlowDetail(
                    content = d.content,
                    playingKey = playback.current?.contentKey,
                    isFavourite = d.content.tracks.firstOrNull()?.contentKey in favourites,
                    onClose = { overlay = Overlay.None },
                    onToggleFavourite = {
                        d.content.tracks.firstOrNull()?.let { libraryViewModel.toggleFavourite(it.contentKey) }
                    },
                    onPlay = {
                        libraryViewModel.playAll(d.content.tracks, 0)
                        overlay = Overlay.Player
                    },
                    onShuffle = {
                        libraryViewModel.shuffleAll(d.content.tracks)
                        overlay = Overlay.Player
                    },
                    onTrack = { i ->
                        libraryViewModel.playAll(d.content.tracks, i)
                        overlay = Overlay.Player
                    },
                )
            }
        }
    }

    actionSong?.let { song ->
        app.flow.music.ui.components.SongActionSheet(
            song = song,
            isFavourite = song.contentKey in favourites,
            onDismiss = { actionSong = null },
            onAction = { action ->
                when (action) {
                    app.flow.music.ui.components.SongAction.PlayNext ->
                        libraryViewModel.playNext(listOf(song))
                    app.flow.music.ui.components.SongAction.AddToQueue ->
                        libraryViewModel.addToQueue(listOf(song))
                    app.flow.music.ui.components.SongAction.ToggleFavourite ->
                        libraryViewModel.toggleFavourite(song.contentKey)
                    app.flow.music.ui.components.SongAction.AddToPlaylist ->
                        playlistViewModel.openPicker(listOf(song.contentKey))
                    app.flow.music.ui.components.SongAction.GoToAlbum ->
                        openAlbumDetail(song.albumKey)
                    else -> Unit
                }
                actionSong = null
            },
        )
    }

    when (val d = dialog) {
        is PlaylistDialog.Create -> CreatePlaylistDialog(
            songCount = d.songsToAdd.size,
            onConfirm = { playlistViewModel.create(it, d.songsToAdd) },
            onDismiss = playlistViewModel::dismissDialog,
        )
        is PlaylistDialog.Pick -> PickPlaylistSheet(
            playlists = playlists,
            songCount = d.songsToAdd.size,
            onPick = { playlistViewModel.addTo(it.id, d.songsToAdd) },
            onCreateNew = { playlistViewModel.openCreate(d.songsToAdd) },
            onDismiss = playlistViewModel::dismissDialog,
        )
        is PlaylistDialog.Rename -> RenamePlaylistDialog(
            playlist = d.playlist,
            onConfirm = { playlistViewModel.rename(d.playlist.id, it) },
            onDismiss = playlistViewModel::dismissDialog,
        )
        is PlaylistDialog.ConfirmDelete -> DeletePlaylistDialog(
            playlist = d.playlist,
            onConfirm = { playlistViewModel.delete(d.playlist.id) },
            onDismiss = playlistViewModel::dismissDialog,
        )
        PlaylistDialog.None -> Unit
    }
}

@Composable
private fun FlowMiniPlayerBar(viewModel: PlayerViewModel, onExpand: () -> Unit) {
    val playback by viewModel.state.collectAsStateWithLifecycle()
    val position by rememberPlaybackPosition(playback.isPlaying, viewModel::positionMs)
    FlowMiniPlayer(
        state = playback,
        progress = if (playback.durationMs > 0) position.toFloat() / playback.durationMs else 0f,
        onExpand = onExpand,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
    )
}

/** The design's sheet transition: 190ms, `cubic-bezier(.22,.7,.25,1)`. */
@Composable
private fun SlideUp(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(190)) { it },
        exit = slideOutVertically(tween(190)) { it },
    ) { content() }
}

private val SPEEDS = listOf("1.0×", "1.25×", "1.5×", "0.9×")
