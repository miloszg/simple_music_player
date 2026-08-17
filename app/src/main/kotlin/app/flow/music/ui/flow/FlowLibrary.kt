package app.flow.music.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Artist
import app.flow.music.domain.model.Playlist
import app.flow.music.domain.model.Song
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.PlateSize
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType
import app.flow.music.ui.theme.PlateShape

enum class LibraryFilter(val label: String) {
    Albums("Albums"), Playlists("Playlists"), Artists("Artists"), Songs("Songs")
}

data class FlowLibraryContent(
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val likedCount: Int = 0,
)

@Composable
fun FlowLibrary(
    content: FlowLibraryContent,
    filter: LibraryFilter,
    contentPadding: PaddingValues,
    onFilter: (LibraryFilter) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            Column {
                Text(
                    "Library",
                    style = FlowType.screenTitle,
                    color = Flow.colors.fg,
                    modifier = Modifier.padding(start = Gutter, bottom = 16.dp),
                )
                FilterTabs(filter, onFilter)
                Spacer(Modifier.height(18.dp))
                LikedSongsCard(content.likedCount, onOpenLiked)
                Spacer(Modifier.height(20.dp))
            }
        }

        when (filter) {
            LibraryFilter.Albums -> items(content.albums, key = { it.key }) { album ->
                AlbumTile(album, isLeftColumn(content.albums.indexOf(album))) { onOpenAlbum(album) }
            }

            LibraryFilter.Playlists -> row(content.playlists.size) { i ->
                val p = content.playlists[i]
                LibraryRow(p.artworkSongId, p.name, p.name, songCount(p.songCount)) { onOpenPlaylist(p) }
            }

            LibraryFilter.Artists -> row(content.artists.size) { i ->
                val a = content.artists[i]
                LibraryRow(a.artworkSongId, a.name, a.name, "Artist", circular = true) { onOpenArtist(a) }
            }

            LibraryFilter.Songs -> row(content.songs.size) { i ->
                val s = content.songs[i]
                LibraryRow(s.mediaStoreId, s.album, s.title, s.artist) { onPlaySong(s) }
            }
        }
    }
}

/** Full-width rows inside the two-column grid. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.row(
    count: Int,
    item: @Composable (Int) -> Unit,
) = items(count, span = { GridItemSpan(maxLineSpan) }) { i -> item(i) }

private fun isLeftColumn(index: Int) = index % 2 == 0

@Composable
private fun FilterTabs(selected: LibraryFilter, onSelect: (LibraryFilter) -> Unit) {
    val colors = Flow.colors
    Box {
        // The rule runs the full width and the active tab's 2px cherry underline
        // sits on top of it, overlapping by a pixel — the design's `margin-bottom:-1`.
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .height(1.dp)
                .background(colors.line),
        )
        Row(
            Modifier.padding(start = Gutter, end = Gutter),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            LibraryFilter.entries.forEach { entry ->
                val active = entry == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // IntrinsicSize.Max makes the column exactly as wide as its
                    // label, so the underline below can fillMaxWidth and match
                    // the text. Without it the first tab's fillMaxWidth claims
                    // the whole row and the rest collapse to one letter wide.
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(entry) },
                ) {
                    Text(
                        entry.label,
                        style = FlowType.filterTab.copy(
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (active) colors.fg else colors.fg2,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (active) colors.cherryHi else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedSongsCard(count: Int, onClick: () -> Unit) {
    val colors = Flow.colors
    Row(
        Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .clip(PlateShape)
            .background(colors.cherry)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, colors.onCherry.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HeartIcon(filled = true, size = 17.dp, color = colors.onCherry)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Liked songs", style = FlowType.likedTitle, color = colors.onCherry)
            Spacer(Modifier.height(5.dp))
            Text(songCount(count), style = FlowType.miniSub, color = colors.onCherry.copy(alpha = 0.8f))
        }
        PlayTriangle(size = 15.dp, color = colors.onCherry)
    }
}

@Composable
private fun AlbumTile(album: Album, leftColumn: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(start = if (leftColumn) Gutter else 0.dp, end = if (leftColumn) 0.dp else Gutter)
            .clickable(onClick = onClick),
    ) {
        CoverPlate(
            mediaStoreId = album.artworkSongId,
            title = album.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            album.albumArtist,
            style = FlowType.albumCaption,
            color = Flow.colors.fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryRow(
    artworkSongId: Long?,
    plateTitle: String?,
    title: String,
    sub: String,
    circular: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = Flow.colors
    Column(Modifier.padding(horizontal = Gutter)) {
        FlowRule()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverPlate(
                mediaStoreId = artworkSongId,
                title = plateTitle,
                shape = if (circular) CircleShape else PlateShape,
                modifier = Modifier.size(PlateSize.rowLarge),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = FlowType.trackTitle, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(sub, style = FlowType.rowSub, color = colors.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("→", style = FlowType.chevron, color = colors.fg3)
        }
    }
}
