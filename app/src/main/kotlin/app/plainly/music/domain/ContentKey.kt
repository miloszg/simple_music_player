package app.plainly.music.domain

import java.text.Normalizer
import java.util.Locale

/**
 * A song's identity, as far as this app is concerned.
 *
 * Deliberately *not* `MediaStore._ID`. That id is reassigned whenever the media
 * scanner rebuilds its database — a full rescan, an SD card remount, a
 * delete-and-restore — and anything keyed on it (playlists, favourites, play
 * counts) silently points at the wrong song, or at nothing, afterwards.
 *
 * Instead we derive a key from where the file lives. Same path, same key,
 * forever; and a file that is removed and put back gets its playlist entries
 * and play count back with it.
 *
 * The trade-off is the mirror image: moving or renaming a file looks like a
 * delete plus an insert. That is the right way round — losing a favourite when
 * you deliberately reorganise your library is expected, losing it because the
 * OS reindexed overnight is not.
 */
object ContentKey {

    /**
     * @param relativePath MediaStore's `RELATIVE_PATH`, e.g. `"Music/Rock/"`.
     * @param displayName MediaStore's `DISPLAY_NAME`, e.g. `"03 Sabotage.mp3"`.
     */
    fun of(relativePath: String?, displayName: String): Long =
        fnv1a64(canonicalPath(relativePath, displayName))

    /**
     * The exact string that gets hashed. Exposed so tests can assert on the
     * normalisation rules rather than on opaque hash values.
     */
    fun canonicalPath(relativePath: String?, displayName: String): String {
        val dir = (relativePath ?: "")
            .replace('\\', '/')
            .trim('/')
        val name = displayName.trim()
        val joined = if (dir.isEmpty()) name else "$dir/$name"
        // NFC first: the same filename can arrive decomposed from one volume and
        // composed from another (notably external SD cards formatted on macOS),
        // and those must not hash differently.
        return Normalizer.normalize(joined, Normalizer.Form.NFC).lowercase(Locale.ROOT)
    }

    /**
     * FNV-1a. Chosen over a cryptographic digest because it is a few
     * instructions per byte and we hash every row on every sync; collision risk
     * across even a 100k-track library is around 3e-10.
     */
    private fun fnv1a64(input: String): Long {
        var hash = -0x340d631b7bdddcdbL // 14695981039346656037 unsigned
        for (byte in input.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toLong() and 0xFF)
            hash *= 0x100000001B3L
        }
        // 0 is reserved as "unset" in the database, so nudge the one input that
        // would produce it.
        return if (hash == 0L) 1L else hash
    }
}
