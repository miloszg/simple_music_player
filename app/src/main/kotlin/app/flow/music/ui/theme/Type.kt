package app.flow.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.flow.music.R

/**
 * Two families, as the brand sheet specifies.
 *
 * **Instrument Serif** — titles, album names, section heads, cover plates.
 * **Instrument Sans** — rows, buttons, everything else, at 400/500/600.
 *
 * Both are SIL Open Font License, so they are fine to redistribute inside a
 * GPL app and fine for F-Droid. Licence text ships alongside them in
 * `res/font/LICENSE-OFL.txt`.
 *
 * Instrument Sans is a variable font, so all four weights come out of one
 * 190 KB file rather than four static cuts.
 */

private fun sansWeight(weight: Int) = Font(
    R.font.instrument_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val InstrumentSans = FontFamily(
    sansWeight(400),
    sansWeight(500),
    sansWeight(600),
    sansWeight(700),
)

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * Serif display styles. Sizes are the design's, verbatim.
 *
 * These are not part of the Material scale — the design uses the serif at
 * specific sizes per surface (30 on the player, 32 on Library, 24 on section
 * heads, 22 on sheet titles), so they are named for where they appear.
 */
private fun sans(size: Double, weight: FontWeight, lineHeight: Double = size * 1.35) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

object FlowType {
    /** Now Playing track title. */
    val playerTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.45).sp,
    )

    /** "Library" — the one screen title set at display size. */
    val screenTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.48).sp,
    )

    /** "Recently played", "Your playlists", "Back in rotation", "Browse". */
    val sectionTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 24.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.24).sp,
    )

    /** "Settings", "Up next". */
    val sheetTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 22.sp,
        lineHeight = 24.sp,
    )

    /** The settings manifesto card. */
    val manifesto = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 27.sp,
        lineHeight = 30.sp,
    )

    /** Empty-state headline — italic, per the design. */
    val emptyTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        lineHeight = 24.sp,
    )

    /** The FLOW wordmark: 700, .24em tracking. */
    val wordmark = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 3.36.sp, // .24em at 14sp
    )

    // ---- Sans, at the design's exact sizes -------------------------------
    // These are spelled out rather than mapped onto Material roles because the
    // design uses half-point sizes (12.5, 13.5, 14.5, 15.5) that a five-step
    // scale cannot express, and rounding them visibly loosens the rows.

    /** "Continue" — the only cherry label on Home. */
    val continueLabel = sans(12.5, FontWeight.SemiBold)

    /** The track name under "Continue". */
    val continueTrack = sans(16.0, FontWeight.SemiBold)

    /** Artist caption under a shelf tile. */
    val shelfCaption = sans(13.0, FontWeight.SemiBold)

    /** "Made by you, kept on this phone". */
    val sectionSub = sans(12.5, FontWeight.Normal)

    /** Song rows in a list. */
    val rowTitle = sans(14.5, FontWeight.SemiBold)

    /** Playlist and library rows, which sit a half-point larger. */
    val rowTitleLarge = sans(15.5, FontWeight.SemiBold)

    /** Secondary line of any row. */
    val rowSub = sans(12.5, FontWeight.Normal)

    /** The "→" affordance at the end of a navigable row. */
    val chevron = sans(17.0, FontWeight.Normal)

    /** Mini-player title. */
    val miniTitle = sans(14.0, FontWeight.SemiBold)

    /** Mini-player artist. */
    val miniSub = sans(12.0, FontWeight.Normal)

    /** Library filter tabs, detail track titles, settings labels. */
    val filterTab = sans(13.5, FontWeight.Normal)
    val trackTitle = sans(15.0, FontWeight.SemiBold)
    val settingLabel = sans(15.0, FontWeight.SemiBold)

    /** Pills: Sleep / Speed / Queue / Shuffle / Dark / Light. */
    val pill = sans(12.5, FontWeight.SemiBold)

    /** The big Play button on a detail page. */
    val detailAction = sans(14.5, FontWeight.SemiBold)

    /** Search field text. */
    val searchField = sans(15.0, FontWeight.Medium)

    /** "Playing from …" on the player. */
    val playerSource = sans(12.5, FontWeight.Medium)

    /** The "←" on a sheet header. */
    val backArrow = sans(19.0, FontWeight.Normal)

    /** "2011 · 6 songs" under a detail plate. */
    val detailSub = sans(14.0, FontWeight.Normal)

    /** The heart on the player. */
    val heart = sans(18.0, FontWeight.Normal)

    /** "Next" on the up-next strip. */
    val nextLabel = sans(13.0, FontWeight.SemiBold)
    val nextTitle = sans(13.5, FontWeight.Medium)

    /** "Close" on the queue panel. */
    val closeLink = sans(13.0, FontWeight.SemiBold)

    /** Artist caption under a library grid tile. */
    val albumCaption = sans(13.5, FontWeight.SemiBold)

    /** "Liked songs" — serif, on the cherry banner. */
    val likedTitle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 20.sp,
        lineHeight = 20.sp,
    )
}

/**
 * The sans scale. Material's roles are kept so stock components inherit
 * something sensible, but the sizes are the design's: 12-15px, secondary at
 * 45-52% opacity.
 */
internal val FlowTypography = Typography(
    headlineLarge = sans(28.0, FontWeight.SemiBold),
    headlineMedium = sans(24.0, FontWeight.SemiBold),
    headlineSmall = sans(20.0, FontWeight.SemiBold),
    titleLarge = sans(17.0, FontWeight.SemiBold),
    titleMedium = sans(15.5, FontWeight.SemiBold),
    titleSmall = sans(14.0, FontWeight.SemiBold),
    // The workhorse: 14.5-15px semibold row titles over 12.5px secondary.
    bodyLarge = sans(15.0, FontWeight.SemiBold, 20.0),
    bodyMedium = sans(14.5, FontWeight.SemiBold, 19.0),
    bodySmall = sans(12.5, FontWeight.Normal, 17.0),
    labelLarge = sans(14.5, FontWeight.SemiBold),
    labelMedium = sans(13.0, FontWeight.SemiBold),
    labelSmall = sans(12.0, FontWeight.Medium),
)

/** Track and total time on the player. */
val TimecodeTextStyle = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
)

/** Track numbers in a detail listing. */
val TrackNumberTextStyle = TextStyle(
    fontFamily = InstrumentSans,
    fontSize = 12.5.sp,
)
