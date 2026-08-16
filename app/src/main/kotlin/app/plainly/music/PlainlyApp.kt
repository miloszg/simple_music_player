package app.plainly.music

import android.app.Application
import app.plainly.music.data.mediastore.ArtworkFetcher
import app.plainly.music.data.mediastore.ArtworkRequest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlainlyApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(ArtworkFetcher.Factory(this@PlainlyApp)) }
            .memoryCache {
                MemoryCache.Builder()
                    // Album art is the only thing this app decodes, and a grid
                    // of tiles is worth keeping resident while the user scrolls.
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            // No disk cache: the source images already live on disk, inside the
            // audio files. Caching them again would duplicate the user's library
            // for no gain.
            .crossfade(CROSSFADE_MS)
            .build()

    private companion object {
        /** Long enough to not flicker, short enough to not feel laggy in a list. */
        const val CROSSFADE_MS = 150
    }
}

/** Coil model for a song's cover art, or null to draw the fallback. */
fun artworkOf(mediaStoreId: Long?): ArtworkRequest? = mediaStoreId?.let(::ArtworkRequest)
