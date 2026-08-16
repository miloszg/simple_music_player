package app.flow.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Song
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType

/** What the user picked in [SongActionSheet]. */
sealed interface SongAction {
    data object PlayNext : SongAction
    data object AddToQueue : SongAction
    data object ToggleFavourite : SongAction
    data object AddToPlaylist : SongAction
    data object GoToAlbum : SongAction
    data object GoToArtist : SongAction
    data object Info : SongAction
    data object Delete : SongAction
}

/**
 * Everything you can do to one song, in one place.
 *
 * Reached by long-pressing a row. A sheet rather than a dropdown because it can
 * show *which* song it is acting on — a menu reading "Add to playlist" with no
 * context is a coin flip when two rows share a title.
 *
 * Set in FLOW's own type and rules rather than Material defaults, so it reads
 * as part of the app instead of a system component that wandered in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheet(
    song: Song,
    isFavourite: Boolean,
    onAction: (SongAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Flow.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bar,
        contentColor = colors.fg,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverPlate(song.mediaStoreId, song.album, Modifier.size(PlateSize.search))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = FlowType.trackTitle, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        song.artist.ifBlank { song.album },
                        style = FlowType.rowSub,
                        color = colors.fg2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Divider()
            ActionRow("Play next") { onAction(SongAction.PlayNext) }
            ActionRow("Add to queue") { onAction(SongAction.AddToQueue) }
            ActionRow(
                label = if (isFavourite) "Remove from liked" else "Add to liked",
                tint = if (isFavourite) colors.cherryHi else null,
            ) { onAction(SongAction.ToggleFavourite) }
            ActionRow("Add to playlist…") { onAction(SongAction.AddToPlaylist) }

            Divider()
            ActionRow("Go to album") { onAction(SongAction.GoToAlbum) }
            ActionRow("Go to artist") { onAction(SongAction.GoToArtist) }
            ActionRow("Song info") { onAction(SongAction.Info) }
            ActionRow("Delete from device", tint = colors.cherryHi) { onAction(SongAction.Delete) }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(
    thickness = 1.dp,
    color = Flow.colors.line,
    modifier = Modifier.padding(vertical = 8.dp),
)

@Composable
private fun ActionRow(label: String, tint: Color? = null, onClick: () -> Unit) {
    val colors = Flow.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = FlowType.trackTitle, color = tint ?: colors.fg)
    }
}
