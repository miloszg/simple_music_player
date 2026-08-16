package app.plainly.music.data.mediastore

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce

/**
 * Signals when the device's audio collection changes.
 *
 * Copying files onto a phone fires the observer once per file, so an
 * un-debounced version would kick off a hundred syncs while a folder is still
 * being written. Two seconds of quiet is long enough for a transfer to settle
 * and short enough that a single new download shows up before the user has
 * finished switching back to the app.
 */
@Singleton
class MediaStoreWatcher @Inject constructor(
    private val contentResolver: ContentResolver,
) {
    private val signals = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            signals.tryEmit(Unit)
        }
    }

    private var registered = false

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val changes: Flow<Unit> = signals.asSharedFlow().debounce(DEBOUNCE_MS)

    @Synchronized
    fun start() {
        if (registered) return
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            /* notifyForDescendants = */ true,
            observer,
        )
        registered = true
    }

    @Synchronized
    fun stop() {
        if (!registered) return
        contentResolver.unregisterContentObserver(observer)
        registered = false
    }

    private companion object {
        const val DEBOUNCE_MS = 2_000L
    }
}
