package app.flow.music.ui.flow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Shuffle and repeat, traced from the design's inline SVG paths.
 *
 * Drawn rather than pulled from Material Icons: the design's marks are 1.6px
 * strokes on an 18-unit grid with round caps, and the Material equivalents are
 * a different weight and a different corner treatment — close enough to look
 * like a mistake rather than a choice.
 */
@Composable
fun ShuffleGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 19.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 18f
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(build: Path.() -> Unit) = drawPath(Path().apply(build), color, style = stroke)

        // M1.5 4.5h3l8 9h3
        path {
            moveTo(1.5f * u, 4.5f * u); lineTo(4.5f * u, 4.5f * u)
            lineTo(12.5f * u, 13.5f * u); lineTo(15.5f * u, 13.5f * u)
        }
        // M13 11l2.5 2.5L13 16
        path { moveTo(13f * u, 11f * u); lineTo(15.5f * u, 13.5f * u); lineTo(13f * u, 16f * u) }
        // M1.5 13.5h3l2.5-3
        path { moveTo(1.5f * u, 13.5f * u); lineTo(4.5f * u, 13.5f * u); lineTo(7f * u, 10.5f * u) }
        // M10.5 6.5l2-2h3
        path { moveTo(10.5f * u, 6.5f * u); lineTo(12.5f * u, 4.5f * u); lineTo(15.5f * u, 4.5f * u) }
        // M13 2l2.5 2.5L13 7
        path { moveTo(13f * u, 2f * u); lineTo(15.5f * u, 4.5f * u); lineTo(13f * u, 7f * u) }
    }
}

@Composable
fun RepeatGlyph(color: Color, one: Boolean = false, size: androidx.compose.ui.unit.Dp = 19.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 18f
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(build: Path.() -> Unit) = drawPath(Path().apply(build), color, style = stroke)

        // M4.5 5h6.5a3 3 0 013 3v1
        path {
            moveTo(4.5f * u, 5f * u); lineTo(11f * u, 5f * u)
            cubicTo(12.66f * u, 5f * u, 14f * u, 6.34f * u, 14f * u, 8f * u)
            lineTo(14f * u, 9f * u)
        }
        // M13.5 13h-6.5a3 3 0 01-3-3v-1
        path {
            moveTo(13.5f * u, 13f * u); lineTo(7f * u, 13f * u)
            cubicTo(5.34f * u, 13f * u, 4f * u, 11.66f * u, 4f * u, 10f * u)
            lineTo(4f * u, 9f * u)
        }
        path { moveTo(6.5f * u, 2.5f * u); lineTo(4f * u, 5f * u); lineTo(6.5f * u, 7.5f * u) }
        path { moveTo(11.5f * u, 10.5f * u); lineTo(14f * u, 13f * u); lineTo(11.5f * u, 15.5f * u) }

        // Repeat-one gets a bar through the middle. The design only specifies a
        // two-state toggle, but Media3 exposes three and dropping one would
        // make the button lie about the player.
        if (one) {
            path { moveTo(9f * u, 7.2f * u); lineTo(9f * u, 10.8f * u) }
        }
    }
}
