package app.flow.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.flow.music.ui.theme.Flow

/**
 * The FLOW mark — "E, bars becoming water".
 *
 * Three measured bars on the left dissolve into a single loose stroke on the
 * right. Structure at one end, current at the other; the transition is the
 * idea, not either end of it.
 *
 * Deliberately imperfect: the bars are unevenly spaced and unevenly tall, the
 * wave does not return to the height it started at, and nothing is centred or
 * mirrored. **There is no container** — an earlier version clipped white bars
 * inside a cherry disc, and the disc was doing the work the stroke should do.
 * The launcher tile is the only place cherry sits behind it, and that is the
 * tile, not the mark.
 *
 * Geometry is the design's 100-unit viewBox verbatim. Each bar is a round-capped
 * line rather than a rounded rectangle — with `rx` at half the width the two are
 * identical, and a stroked line is one instruction instead of eight.
 */
@Composable
fun FlowMark(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val ink = color ?: Flow.colors.cherry

    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / VIEWBOX
        // The mark is wider than it is tall and sits low-left in its box; centre
        // the drawn content rather than the viewBox so it optically balances
        // next to the wordmark.
        val ox = (this.size.width - CONTENT_W * u) / 2f - MIN_X * u
        val oy = (this.size.height - CONTENT_H * u) / 2f - MIN_Y * u

        fun x(v: Float) = ox + v * u
        fun y(v: Float) = oy + v * u

        BARS.forEach { (cx, span) ->
            val (top, bottom) = span
            drawLine(
                color = ink,
                start = Offset(x(cx), y(top)),
                end = Offset(x(cx), y(bottom)),
                strokeWidth = BAR_W * u,
                cap = StrokeCap.Round,
            )
        }

        // M56,52 C66,34 74,70 96,54
        drawPath(
            path = Path().apply {
                moveTo(x(56f), y(52f))
                cubicTo(x(66f), y(34f), x(74f), y(70f), x(96f), y(54f))
            },
            color = ink,
            style = Stroke(
                width = WAVE_W * u,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/**
 * Bar centres and their capped extents, derived from the design's rects: a
 * `rect(x, y, w, h, rx = w/2)` is the same shape as a line down its centre
 * from `y + rx` to `y + h - rx` stroked at `w` with a round cap.
 */
private val BARS = listOf(
    13f to (37f to 67f), // rect x=8  y=32 h=40
    29f to (25f to 79f), // rect x=24 y=20 h=64
    45f to (17f to 87f), // rect x=40 y=12 h=80
)

private const val BAR_W = 10f
private const val WAVE_W = 9f
private const val VIEWBOX = 100f

// Drawn bounds including stroke: bars span x 8..50, the wave reaches x 100.5.
private const val MIN_X = 8f
private const val MIN_Y = 12f
private const val CONTENT_W = 92.5f
private const val CONTENT_H = 80f
