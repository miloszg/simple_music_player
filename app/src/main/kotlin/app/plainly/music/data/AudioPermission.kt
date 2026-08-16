package app.plainly.music.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The one permission this app needs to do its job.
 *
 * Android 13 split the blanket storage read into per-media-type permissions.
 * Unlike images and video, audio has no "selected items only" variant — it is
 * all or nothing — so there is no partial-access state to handle.
 */
object AudioPermission {

    val name: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
}
