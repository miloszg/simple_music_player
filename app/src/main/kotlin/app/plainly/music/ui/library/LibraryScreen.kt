package app.plainly.music.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.plainly.music.domain.model.Album
import app.plainly.music.domain.model.Artist
import app.plainly.music.domain.model.Folder
import app.plainly.music.domain.model.Song
import app.plainly.music.ui.LibraryTab
import app.plainly.music.ui.components.ArtworkCard
import app.plainly.music.ui.components.CollectionRow
import app.plainly.music.ui.components.EmptyState
import app.plainly.music.ui.components.SongRow
import app.plainly.music.ui.formatCollectionSummary

@Composable
fun LibraryTabContent(
    tab: LibraryTab,
    state: LibraryUiState,
    contentPadding: PaddingValues,
    onSongClick: (Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onFolderClick: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (tab) {
            LibraryTab.Songs -> SongList(state, contentPadding, onSongClick)
            LibraryTab.Albums -> AlbumGrid(state.albums, contentPadding, onAlbumClick)
            LibraryTab.Artists -> ArtistList(state.artists, contentPadding, onArtistClick)
            LibraryTab.Folders -> FolderList(state.folders, contentPadding, onFolderClick)
            LibraryTab.Playlists -> EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "No playlists yet",
                body = "Long-press any song to start one.",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SongList(
    state: LibraryUiState,
    contentPadding: PaddingValues,
    onSongClick: (Int) -> Unit,
) {
    if (state.songs.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.MusicNote,
            title = "No music found",
            body = "Nothing on this device matches your library filters. " +
                "Check the minimum track length and excluded folders in Settings.",
        )
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        itemsIndexed(state.songs) { position, song ->
            SongRow(
                song = song,
                isFavourite = song.contentKey in state.favourites,
                onClick = { onSongClick(position) },
            )
        }
    }
}

/**
 * `key` is the content key rather than the list position, so reordering after a
 * sort change animates instead of recycling every row into the wrong place.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    songs: List<Song>,
    row: @Composable (Int, Song) -> Unit,
) = items(count = songs.size, key = { songs[it].contentKey }) { index -> row(index, songs[index]) }

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    contentPadding: PaddingValues,
    onClick: (Album) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyState(Icons.Rounded.Album, "No albums", "Albums appear once there is music to group.")
        return
    }
    LazyVerticalGrid(
        // Adaptive rather than a fixed count so a tablet or an unfolded phone
        // gets more columns instead of enormous tiles.
        columns = GridCells.Adaptive(minSize = 148.dp),
        contentPadding = contentPadding,
    ) {
        items(albums, key = { it.key }) { album ->
            ArtworkCard(
                title = album.name,
                subtitle = album.albumArtist,
                artworkSongId = album.artworkSongId,
                onClick = { onClick(album) },
            )
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<Artist>,
    contentPadding: PaddingValues,
    onClick: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyState(Icons.Rounded.Person, "No artists", "Artists appear once there is music to group.")
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        items(artists, key = { it.key }) { artist ->
            CollectionRow(
                title = artist.name,
                subtitle = formatCollectionSummary(artist.songCount, artist.durationMs),
                artworkSongId = artist.artworkSongId,
                onClick = { onClick(artist) },
            )
        }
    }
}

@Composable
private fun FolderList(
    folders: List<Folder>,
    contentPadding: PaddingValues,
    onClick: (Folder) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyState(Icons.Rounded.Folder, "No folders", "Folders appear once there is music on the device.")
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        items(folders, key = { it.path }) { folder ->
            CollectionRow(
                title = folder.name,
                subtitle = folder.path,
                artworkSongId = folder.artworkSongId,
                onClick = { onClick(folder) },
            )
        }
    }
}
