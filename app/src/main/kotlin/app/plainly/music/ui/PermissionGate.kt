package app.plainly.music.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.plainly.music.data.AudioPermission
import app.plainly.music.ui.components.EmptyState

/**
 * Shows [content] once the app can read the music library, and an explanation
 * plus a way forward until then.
 *
 * Re-checks on every resume rather than only on the permission callback,
 * because the recovery path for a permanently-denied permission goes through
 * system Settings — there is no result to receive, the user just comes back.
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        asked = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = AudioPermission.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) {
        content()
        return
    }

    EmptyState(
        icon = Icons.Rounded.LibraryMusic,
        title = "Plainly Music needs to read your music",
        body = if (asked) {
            "Permission was denied. You can grant it from the app's settings — " +
                "it is the only permission this app asks for, and nothing leaves your device."
        } else {
            "It reads the audio files already on this device. There is no network " +
                "permission, so nothing can be uploaded anywhere."
        },
        // Once the system stops showing its dialog, sending the user to the
        // system settings page is the only route left. Asking again would do
        // nothing and look broken.
        actionLabel = if (asked) "Open app settings" else "Grant access",
        onAction = {
            if (asked) context.openAppSettings() else launcher.launch(AudioPermission.name)
        },
    )
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
