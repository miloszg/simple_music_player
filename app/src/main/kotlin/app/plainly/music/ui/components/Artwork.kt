package app.plainly.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.plainly.music.artworkOf
import coil3.compose.AsyncImage

/**
 * A song's cover art, with a themed fallback for the many files that have none.
 *
 * The fallback is not a separate branch — it is the background this draws on
 * top of. A missing, failed or still-loading image simply leaves it showing,
 * which avoids the flash of a placeholder swapping for the real thing and keeps
 * untagged libraries looking deliberate rather than broken.
 */
@Composable
fun Artwork(
    mediaStoreId: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(FALLBACK_GLYPH_FRACTION),
        )
        AsyncImage(
            model = artworkOf(mediaStoreId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Square artwork at the standard list-row size. */
@Composable
fun ListArtwork(mediaStoreId: Long?, modifier: Modifier = Modifier) {
    Artwork(mediaStoreId, modifier.size(48.dp))
}

private const val FALLBACK_GLYPH_FRACTION = 0.45f
