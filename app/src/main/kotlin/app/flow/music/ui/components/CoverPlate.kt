package app.flow.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.flow.music.artworkOf
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.InstrumentSerif
import app.flow.music.ui.theme.PlateShape
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

/**
 * Cover art, or a plate.
 *
 * From the brand sheet: *"Real embedded artwork always wins. When a file has
 * none, Flow sets the album name in the serif on a flat plate. Four treatments
 * rotate by tone — plain, ruled, italic, ruled italic — so a shelf of untagged
 * albums still looks composed. No gradients, no mesh, no grey question mark.
 * Untagged files get a dashed plate and an italic 'Untitled'."*
 *
 * The tone is derived from the album's own name rather than stored, so a given
 * album lands on the same ground and the same treatment every time — which is
 * what lets a plate function as a cover at all.
 */
@Composable
fun CoverPlate(
    mediaStoreId: Long?,
    title: String?,
    modifier: Modifier = Modifier,
    shape: Shape = PlateShape,
    /** Overrides the derived tone. Liked songs is always cherry. */
    toneOverride: Int? = null,
) {
    val painter = rememberAsyncImagePainter(model = artworkOf(mediaStoreId))
    val state by painter.state.collectAsState()

    Box(modifier.clip(shape)) {
        if (state is AsyncImagePainter.State.Success) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Plate(title, toneOverride)
        }
    }
}

/** A plate on its own, with no artwork attempted. */
@Composable
fun Plate(
    title: String?,
    toneOverride: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors
    val named = !title.isNullOrBlank()
    val tone = toneOverride ?: toneOf(title)
    val ruled = tone % 4 == 1 || tone % 4 == 3
    val italic = tone % 4 == 2 || tone % 4 == 3 || !named

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(if (named) colors.tones[tone] else colors.bar)
            // The dashed edge is the tell for "no album tag at all", as distinct
            // from "tagged, but the file carries no picture".
            .then(if (named) Modifier else Modifier.dashedBorder(colors.fg3)),
    ) {
        // Plate text scales with the plate. The design sets 44sp on the player,
        // 40 on a detail header, 34 on the hero, 19 in the library grid and 17
        // on a shelf tile — every one of those is ~12.5% of the plate's width,
        // so one ratio reproduces them all instead of five magic numbers.
        val fontSize = (maxWidth.value * TITLE_RATIO).coerceIn(10f, 46f)
        val pad = (maxWidth.value * PAD_RATIO).coerceIn(7f, 22f)

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (ruled && named) {
                Box(
                    Modifier
                        .padding(bottom = (fontSize * 0.34f).dp)
                        .width((fontSize * 0.85f).dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.55f)),
                )
            }
            Text(
                text = if (named) title else "Untitled",
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.06f).sp,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = if (italic) 0.sp else (-fontSize * 0.012f).sp,
                ),
                color = if (named) colors.onCherry else colors.fg2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.dashedBorder(color: Color) = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
    )
}

/**
 * Stable tone for a name.
 *
 * `hashCode` can return [Int.MIN_VALUE], whose absolute value is still
 * negative — widen to Long before taking the magnitude, or one title in four
 * billion crashes the shelf it appears on.
 */
internal fun toneOf(title: String?, toneCount: Int = 6): Int {
    if (title.isNullOrBlank()) return 0
    val h = title.lowercase().hashCode().toLong()
    return ((if (h < 0) -h else h) % toneCount).toInt()
}

private const val TITLE_RATIO = 0.125f
private const val PAD_RATIO = 0.09f

/** Plate sizes from the design, so call sites read the way it does. */
object PlateSize {
    val row: Dp = 46.dp
    val rowLarge: Dp = 54.dp
    val search: Dp = 48.dp
    val playlist: Dp = 52.dp
    val mini: Dp = 56.dp
    val queue: Dp = 38.dp
    val shelf: Dp = 134.dp
}
