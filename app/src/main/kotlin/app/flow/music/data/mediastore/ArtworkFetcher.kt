package app.flow.music.data.mediastore

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.CancellationSignal
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.pxOrElse

/**
 * Loads a song's embedded cover art.
 *
 * Album art on Android is not a file — it lives inside the audio files. The
 * modern way to get at it is [ContentResolver.loadThumbnail], which reads the
 * embedded picture and hands back an already-downscaled bitmap, so a grid of
 * 120dp album tiles never decodes a 3000x3000 JPEG.
 *
 * [MediaMetadataRetriever] is the fallback for the cases loadThumbnail gives up
 * on — some FLACs, and files whose picture frame the platform thumbnailer does
 * not recognise. It decodes the full-size image, so it is only ever a fallback.
 *
 * Coil owns caching on both sides of this; the fetcher only runs on a miss.
 */
class ArtworkFetcher private constructor(
    private val context: Context,
    private val request: ArtworkRequest,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val uri = MediaStoreScanner.contentUri(request.mediaStoreId)
        val target = options.size.toSizeOrDefault()

        val thumbnail = runCatching {
            context.contentResolver.loadThumbnail(uri, target, CancellationSignal())
        }.getOrNull()

        val bitmap = thumbnail ?: runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        }.getOrNull()

        // Null rather than a placeholder bitmap: the caller draws its own
        // fallback, which can then follow the theme.
        return bitmap?.let {
            ImageFetchResult(
                image = it.asImage(),
                isSampled = thumbnail != null,
                dataSource = DataSource.DISK,
            )
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<ArtworkRequest> {
        override fun create(data: ArtworkRequest, options: Options, imageLoader: ImageLoader) =
            ArtworkFetcher(context, data, options)
    }

    private companion object {
        /**
         * loadThumbnail rejects a zero or unbounded dimension, which is what
         * Coil reports for a not-yet-measured composable. 512px is a sensible
         * album-tile size and gets scaled once measurement lands.
         */
        const val FALLBACK_PX = 512

        fun coil3.size.Size.toSizeOrDefault(): Size = Size(
            width.pxOr(FALLBACK_PX),
            height.pxOr(FALLBACK_PX),
        )

        fun Dimension.pxOr(fallback: Int): Int = pxOrElse { fallback }.coerceAtLeast(1)
    }
}

/**
 * Coil model for "the cover art of this song".
 *
 * A data class rather than a bare Long so it has a distinct type for the
 * fetcher to key on, and so the memory cache key is unambiguous.
 */
data class ArtworkRequest(val mediaStoreId: Long)
