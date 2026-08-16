package app.flow.music.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.SearchResults
import app.flow.music.domain.model.Album
import app.flow.music.domain.model.Song
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.PlateSize
import app.flow.music.ui.components.Plate
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType
import app.flow.music.ui.theme.PlateShape

@Composable
fun FlowSearch(
    query: String,
    results: SearchResults,
    browse: List<String>,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onBrowse: (String) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasQuery = query.isNotBlank()
    val hasHits = !results.isEmpty

    Column(modifier.fillMaxSize()) {
        SearchField(query, onQueryChange)

        when {
            !hasQuery -> BrowseGrid(browse, contentPadding, onBrowse)
            hasHits -> ResultList(results, contentPadding, onOpenAlbum, onPlaySong)
            else -> EmptyResults(onBrowseInstead = { onQueryChange("") })
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = Flow.colors
    Column(Modifier.padding(start = Gutter, end = Gutter, bottom = 22.dp)) {
        Row(
            Modifier.padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagnifierIcon()
            Spacer(Modifier.width(11.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Songs, albums, artists",
                        style = FlowType.searchField,
                        color = colors.fg2,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(FlowType.searchField).copy(color = colors.fg),
                    cursorBrush = SolidColor(colors.cherryHi),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Text(
                    "✕",
                    style = FlowType.rowSub,
                    color = colors.fg2,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(start = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // A 1.5px cherry underline is the entire field treatment — no box, no
        // fill. It is also the only cherry on the screen until something plays.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(colors.cherry),
        )
    }
}

@Composable
private fun MagnifierIcon() {
    val colors = Flow.colors
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val r = size.minDimension * 0.31f
        val c = Offset(size.width * 0.42f, size.height * 0.42f)
        val w = 1.8.dp.toPx()
        drawCircle(color = colors.cherryHi, radius = r, center = c, style = Stroke(width = w))
        drawLine(
            color = colors.cherryHi,
            start = Offset(c.x + r * 0.72f, c.y + r * 0.72f),
            end = Offset(size.width * 0.94f, size.height * 0.94f),
            strokeWidth = w,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun BrowseGrid(
    browse: List<String>,
    contentPadding: PaddingValues,
    onBrowse: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            SectionTitle("Browse", Modifier.padding(start = Gutter, bottom = 14.dp))
        }
        // Keyed by position, not by name: two different albums can legitimately
        // share a title ("Greatest Hits"), and a duplicate key crashes the grid.
        itemsIndexed(browse) { i, name ->
            Box(
                Modifier
                    .padding(start = if (i % 2 == 0) Gutter else 0.dp)
                    .aspectRatio(BROWSE_ASPECT)
                    .clip(PlateShape)
                    .clickable { onBrowse(name) },
            ) {
                Plate(title = name)
            }
        }
    }
}

@Composable
private fun ResultList(
    results: SearchResults,
    contentPadding: PaddingValues,
    onOpenAlbum: (Album) -> Unit,
    onPlaySong: (Song) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(results.albums.size, key = { "al${results.albums[it].key}" }) { i ->
            val album = results.albums[i]
            ResultRow(
                artworkSongId = album.artworkSongId,
                plateTitle = album.name,
                title = album.name,
                sub = album.albumArtist,
            ) { onOpenAlbum(album) }
        }
        items(results.songs.size, key = { "sg${results.songs[it].contentKey}" }) { i ->
            val song = results.songs[i]
            ResultRow(
                artworkSongId = song.mediaStoreId,
                plateTitle = song.album,
                title = song.title,
                sub = song.artist,
            ) { onPlaySong(song) }
        }
    }
}

@Composable
private fun ResultRow(
    artworkSongId: Long?,
    plateTitle: String?,
    title: String,
    sub: String,
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
            CoverPlate(artworkSongId, plateTitle, Modifier.size(PlateSize.search))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = FlowType.rowTitle, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(sub, style = FlowType.rowSub, color = colors.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("→", style = FlowType.chevron, color = colors.fg3)
        }
    }
}

@Composable
private fun EmptyResults(onBrowseInstead: () -> Unit) {
    val colors = Flow.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing here", style = FlowType.emptyTitle, color = colors.fg)
        Spacer(Modifier.height(9.dp))
        Text(
            "Flow plays what's saved on this phone. Try another name.",
            style = FlowType.rowSub,
            color = colors.fg2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .clip(PlateShape)
                .border(1.dp, colors.fg3, PlateShape)
                .clickable(onClick = onBrowseInstead)
                .padding(horizontal = 22.dp, vertical = 11.dp),
        ) {
            Text("Browse instead", style = FlowType.pill.copy(fontSize = FlowType.rowSub.fontSize), color = colors.fg)
        }
    }
}

/** `aspect-ratio:1.7` on the browse cards. */
private const val BROWSE_ASPECT = 1.7f
