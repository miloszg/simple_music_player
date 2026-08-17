package app.flow.music.ui.flow

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The heart, as a vector rather than the "♥" character the design uses.
 *
 * U+2665 has an emoji presentation, and Android's emoji font wins over the text
 * font — so the glyph renders as a fixed red emoji and ignores the colour it is
 * given. That broke the unfavourited state entirely: it drew a red heart where
 * a grey outline belonged. The vector honours the tint and matches the design's
 * weight closely enough that the substitution is invisible.
 */
@Composable
fun HeartIcon(filled: Boolean, size: Dp, color: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = if (filled) "Remove from liked" else "Add to liked",
        tint = color,
        modifier = modifier.size(size),
    )
}
