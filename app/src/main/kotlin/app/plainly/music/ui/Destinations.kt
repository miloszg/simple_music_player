package app.plainly.music.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/** Top-level tabs. Order is the order they appear in the navigation bar. */
enum class LibraryTab(val label: String, val icon: ImageVector) {
    Songs("Songs", Icons.Rounded.MusicNote),
    Albums("Albums", Icons.Rounded.Album),
    Artists("Artists", Icons.Rounded.Person),
    Folders("Folders", Icons.Rounded.Folder),
    Playlists("Playlists", Icons.Rounded.QueueMusic),
}

/**
 * Navigation routes. Serializable objects rather than strings so the arguments
 * are type-checked at the call site and there is no route grammar to get wrong.
 */
@Serializable object HomeRoute

@Serializable object SearchRoute

@Serializable object SettingsRoute

@Serializable data class AlbumRoute(val albumKey: Long)

@Serializable data class ArtistRoute(val artistKey: Long)

@Serializable data class FolderRoute(val path: String)

@Serializable data class PlaylistRoute(val playlistId: Long)
