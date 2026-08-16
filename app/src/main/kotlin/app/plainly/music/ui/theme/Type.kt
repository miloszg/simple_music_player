package app.plainly.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The stock Material 3 type scale, with the list-facing styles tightened.
 *
 * We ship no custom font on purpose: the system font is what the rest of the
 * phone uses, it costs nothing in APK size, and it means the app inherits the
 * user's font-size and boldness accessibility settings for free.
 */
private val Default = Typography()

internal val PlainlyTypography = Default.copy(
    titleMedium = Default.titleMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    // Song and album titles in dense lists: slightly tighter than stock so two
    // lines of metadata sit comfortably inside a 64dp row.
    bodyLarge = Default.bodyLarge.copy(
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = Default.bodyMedium.copy(
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = Default.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
)

/** Monospaced digits for timers, so the seek position doesn't jitter as it ticks. */
internal val TimecodeTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.sp,
)
