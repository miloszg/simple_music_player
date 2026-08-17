package app.flow.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Almost everything in FLOW is a 4dp rectangle.
 *
 * Cover plates, cards, pills, the Liked-songs banner — all `border-radius:4px`
 * in the design. The only round things are the play buttons and the mark. That
 * uniformity is a large part of why the layout reads as composed rather than
 * assembled.
 */
internal val FlowShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

/** Cover plates and artwork. */
val PlateShape = RoundedCornerShape(4.dp)

/** The queue panel, which slides up over the player. */
val QueueSheetShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
