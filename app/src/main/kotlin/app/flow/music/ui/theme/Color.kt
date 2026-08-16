package app.flow.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * FLOW.
 *
 * Values come straight from the design's `oklch()` declarations, converted to
 * sRGB once here rather than approximated by eye:
 *
 *   oklch(0.575 0.225 18) -> #DE1043   cherry, dark
 *   oklch(0.665 0.235 20) -> #FF3854   cherry hi, dark
 *   oklch(0.525 0.225 18) -> #CC0035   cherry, light
 *   oklch(0.475 0.235 20) -> #BD001B   cherry hi, light
 *
 * One accent, two stops. Cherry means "playing" or "primary action" — nothing
 * decorative is ever cherry. Everything else is black, white, and hairlines at
 * 8-10% white.
 */

// ---- Raw brand values ------------------------------------------------------

@Immutable
data class FlowPalette(
    val bg: Color,
    val bar: Color,
    val fg: Color,
    /** Secondary text. 48% on dark, 52% on light. */
    val fg2: Color,
    /** Tertiary — chevrons, track numbers, disabled. */
    val fg3: Color,
    /** Hairline rules. */
    val line: Color,
    /** Slightly stronger rule, for outlined pills and the seek track. */
    val line2: Color,
    val cherry: Color,
    val cherryHi: Color,
    val onCherry: Color,
    /** Flat cover-plate grounds, indexed by an album's tone. */
    val tones: List<Color>,
)

val FlowDarkPalette = FlowPalette(
    bg = Color(0xFF070707),
    bar = Color(0xFF0D0D0D),
    fg = Color(0xFFFFFFFF),
    fg2 = Color(0x7AFFFFFF), // .48
    fg3 = Color(0x4DFFFFFF), // .30
    line = Color(0x17FFFFFF), // .09
    line2 = Color(0x29FFFFFF), // .16
    cherry = Color(0xFFDE1043),
    cherryHi = Color(0xFFFF3854),
    onCherry = Color(0xFFFFFFFF),
    tones = DarkTones,
)

val FlowLightPalette = FlowPalette(
    bg = Color(0xFFFBFAF9),
    bar = Color(0xFFFFFFFF),
    fg = Color(0xFF0B0B0B),
    fg2 = Color(0x85000000), // .52
    fg3 = Color(0x57000000), // .34
    line = Color(0x17000000), // .09
    line2 = Color(0x26000000), // .15
    cherry = Color(0xFFCC0035),
    cherryHi = Color(0xFFBD001B),
    onCherry = Color(0xFFFFFFFF),
    tones = LightTones,
)

/**
 * The six plate grounds, converted from the design's `TONES` array.
 *
 * Index 1 is cherry itself — the design deliberately lets one album in six sit
 * on the accent, which is what stops a shelf of plates reading as sludge.
 */
private val DarkTones: List<Color>
    get() = listOf(
        Color(0xFF372B26), // oklch(0.30 0.02 40)
        Color(0xFFDE1043), // cherry
        Color(0xFF202429), // oklch(0.26 0.01 250)
        Color(0xFF3D3826), // oklch(0.34 0.03 95)
        Color(0xFF1E1919), // oklch(0.22 0.008 20)
        Color(0xFF603D3A), // oklch(0.40 0.05 25)
    )

/**
 * Light-theme plates.
 *
 * The design only specifies one tone set. Reusing the dark grounds verbatim on
 * a near-white page gives six dark rectangles that fight the layout, so these
 * are the same hues held at the same chroma and lifted in lightness — the
 * plates stay recognisably the same six colours, and white plate text still
 * clears 4.5:1 on every one.
 */
private val LightTones: List<Color>
    get() = listOf(
        Color(0xFF6B5449), // hue 40
        Color(0xFFCC0035), // cherry, light
        Color(0xFF3F4854), // hue 250
        Color(0xFF6E6547), // hue 95
        Color(0xFF3B3231), // hue 20
        Color(0xFF8C5A55), // hue 25
    )

// ---- Material bridge -------------------------------------------------------

/**
 * Material's scheme, filled from the palette.
 *
 * The app draws almost everything from [FlowPalette] directly — the design is
 * not a Material design and forcing it through tonal roles would lose it. This
 * exists so the Material components we do still use (sheets, sliders, ripples)
 * land on the right colours instead of defaulting to purple.
 */
internal fun FlowPalette.toColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = cherryHi,
        onPrimary = onCherry,
        primaryContainer = cherry,
        onPrimaryContainer = onCherry,
        background = bg,
        onBackground = fg,
        surface = bg,
        onSurface = fg,
        surfaceVariant = bar,
        onSurfaceVariant = fg2,
        surfaceContainerLowest = bg,
        surfaceContainerLow = bar,
        surfaceContainer = bar,
        surfaceContainerHigh = Color(0xFF141414),
        surfaceContainerHighest = Color(0xFF1A1A1A),
        outline = fg3,
        outlineVariant = line,
        error = cherryHi,
        onError = onCherry,
        scrim = Color(0xCC000000),
    )
} else {
    lightColorScheme(
        primary = cherry,
        onPrimary = onCherry,
        primaryContainer = cherry,
        onPrimaryContainer = onCherry,
        background = bg,
        onBackground = fg,
        surface = bg,
        onSurface = fg,
        surfaceVariant = bar,
        onSurfaceVariant = fg2,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = bar,
        surfaceContainer = bar,
        surfaceContainerHigh = Color(0xFFF3F1EF),
        surfaceContainerHighest = Color(0xFFEDEBE8),
        outline = fg3,
        outlineVariant = line,
        error = cherry,
        onError = onCherry,
        scrim = Color(0x99000000),
    )
}
