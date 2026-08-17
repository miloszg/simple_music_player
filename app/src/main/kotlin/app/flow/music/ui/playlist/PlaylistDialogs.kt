package app.flow.music.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Playlist
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.PlateSize
import app.flow.music.ui.formatCollectionSummary
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType

/**
 * Name a new playlist.
 *
 * Opens with the keyboard already up and the field focused: this dialog exists
 * to capture one short string, and making the user tap into it first is a
 * wasted interaction.
 */
@Composable
fun CreatePlaylistDialog(
    songCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (trimmed.isNotEmpty()) onConfirm(trimmed) },
                    ),
                    modifier = Modifier.focusRequester(focus),
                )
                if (songCount > 0) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        text = if (songCount == 1) "1 song will be added" else "$songCount songs will be added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            // Disabled rather than hidden, and disabled on blank rather than
            // empty, so a name of spaces can't create an invisible playlist.
            TextButton(onClick = { onConfirm(trimmed) }, enabled = trimmed.isNotEmpty()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun RenamePlaylistDialog(
    playlist: Playlist,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(playlist.id) { mutableStateOf(playlist.name) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (trimmed.isNotEmpty()) onConfirm(trimmed) },
                ),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = trimmed.isNotEmpty()) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DeletePlaylistDialog(
    playlist: Playlist,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"${playlist.name}\"?") },
        // Says what is *not* destroyed. Deleting a playlist sounds like it might
        // take the music with it, and that fear stops people tidying up.
        text = { Text("The playlist is removed. The songs stay on your device.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Choose an existing playlist, or start a new one, for some songs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickPlaylistSheet(
    playlists: List<Playlist>,
    songCount: Int,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = if (songCount == 1) "Add song to…" else "Add $songCount songs to…",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 12.dp,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateNew)
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PlateSize.search),
                )
                Spacer(Modifier.width(13.dp))
                Text(
                    "New playlist…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(playlist) }
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverPlate(
                            playlist.artworkSongId,
                            playlist.name,
                            Modifier.size(PlateSize.search),
                        )
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = FlowType.trackTitle, color = Flow.colors.fg)
                            Text(
                                formatCollectionSummary(playlist.songCount, playlist.durationMs),
                                style = FlowType.rowSub,
                                color = Flow.colors.fg2,
                            )
                        }
                    }
                }
            }
        }
    }
}
