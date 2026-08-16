package app.flow.music.ui.flow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Playlist
import app.flow.music.domain.model.Song
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.PlateSize
import app.flow.music.ui.formatDuration
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType

/** What the home screen shows. */
data class FlowHomeContent(
    val hero: Album? = null,
    val heroTrack: Song? = null,
    val recent: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val rotation: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = hero == null && recent.isEmpty() && rotation.isEmpty()
}

@Composable
fun FlowHome(
    content: FlowHomeContent,
    contentPadding: PaddingValues,
    onOpenAlbum: (Album) -> Unit,
    onPlayHero: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onPlaySong: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        content.hero?.let { hero ->
            item(key = "hero") {
                Column(Modifier.padding(start = Gutter, end = Gutter, bottom = 26.dp)) {
                    CoverPlate(
                        mediaStoreId = hero.artworkSongId,
                        title = hero.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(HERO_ASPECT)
                            .clickable { onOpenAlbum(hero) },
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Continue",
                                style = FlowType.continueLabel,
                                color = colors.cherryHi,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = content.heroTrack?.title ?: hero.name,
                                style = FlowType.continueTrack,
                                color = colors.fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.cherry)
                                .clickable(onClick = onPlayHero),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayTriangle(
                                size = 16.dp,
                                color = colors.onCherry,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (content.recent.isNotEmpty()) {
            item(key = "recent") {
                Column(Modifier.padding(bottom = 28.dp)) {
                    SectionTitle("Recently played", Modifier.padding(start = Gutter, bottom = 14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = Gutter),
                    ) {
                        items(content.recent, key = { it.key }) { album ->
                            Column(
                                Modifier
                                    .width(PlateSize.shelf)
                                    .clickable { onOpenAlbum(album) },
                            ) {
                                CoverPlate(
                                    mediaStoreId = album.artworkSongId,
                                    title = album.name,
                                    modifier = Modifier.size(PlateSize.shelf),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = album.albumArtist,
                                    style = FlowType.shelfCaption,
                                    color = colors.fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (content.playlists.isNotEmpty()) {
            item(key = "playlists") {
                Column(Modifier.padding(start = Gutter, end = Gutter, bottom = 28.dp)) {
                    SectionTitle("Your playlists", Modifier.padding(bottom = 6.dp))
                    Text(
                        "Made by you, kept on this phone",
                        style = FlowType.sectionSub,
                        color = colors.fg2,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                    content.playlists.forEach { playlist ->
                        PlaylistRow(playlist) { onOpenPlaylist(playlist) }
                    }
                }
            }
        }

        if (content.rotation.isNotEmpty()) {
            item(key = "rotation") {
                Column(Modifier.padding(horizontal = Gutter)) {
                    SectionTitle("Back in rotation", Modifier.padding(bottom = 14.dp))
                    content.rotation.forEach { song ->
                        RotationRow(song, onClick = { onPlaySong(song) }, onLongClick = { onSongMenu(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = FlowType.sectionTitle, color = Flow.colors.fg, modifier = modifier)
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    val colors = Flow.colors
    Column {
        FlowRule()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverPlate(
                mediaStoreId = playlist.artworkSongId,
                title = playlist.name,
                modifier = Modifier.size(PlateSize.playlist),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    style = FlowType.rowTitleLarge,
                    color = colors.fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    songCount(playlist.songCount),
                    style = FlowType.rowSub,
                    color = colors.fg2,
                )
            }
            Text("→", style = FlowType.chevron, color = colors.fg3)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RotationRow(song: Song, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = Flow.colors
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverPlate(
            mediaStoreId = song.mediaStoreId,
            title = song.album,
            modifier = Modifier.size(PlateSize.row),
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = FlowType.rowTitle,
                color = colors.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artist,
                style = FlowType.rowSub,
                color = colors.fg2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(formatDuration(song.durationMs), style = FlowType.rowSub, color = colors.fg3)
    }
}

internal fun songCount(n: Int) = if (n == 1) "1 song" else "$n songs"

/** The design's hero plate is `aspect-ratio:1.12`. */
private const val HERO_ASPECT = 1.12f
