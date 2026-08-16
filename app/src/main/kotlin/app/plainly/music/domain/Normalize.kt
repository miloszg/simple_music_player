package app.plainly.music.domain

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * Folds a title/artist/album into the form used for searching and sorting.
 *
 * Lowercased and stripped of diacritics, so "Bjork" finds "Björk" and
 * "motorhead" finds "Motörhead" — the common case of typing on a phone keyboard
 * without reaching for accented characters.
 */
fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .trim()

private val LEADING_ARTICLE = Regex("^(the|a|an)\\s+")

/**
 * Sort key that files "The Beatles" under B.
 *
 * English articles only. Doing this properly per-language needs a collation
 * table nobody maintains, and getting it wrong in the user's language is worse
 * than not doing it at all — so this stays deliberately narrow.
 */
fun String.foldForSort(): String =
    foldForSearch().replace(LEADING_ARTICLE, "")

/**
 * The letter this title files under in the fast scroller. Anything that isn't
 * a Latin letter buckets together under "#".
 */
fun String.indexBucket(): String {
    val first = foldForSort().firstOrNull() ?: return "#"
    return if (first in 'a'..'z') first.uppercase() else "#"
}
