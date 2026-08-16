package app.plainly.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.plainly.music.playback.PlaybackState
import app.plainly.music.playback.RepeatMode
import app.plainly.music.ui.components.Artwork
import app.plainly.music.ui.formatDuration
import app.plainly.music.ui.theme.TimecodeTextStyle

@Composable
fun NowPlayingSheet(
    state: PlaybackState,
    positionMs: Long,
    isFavourite: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.current ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        SpinningArtwork(
            mediaStoreId = song.mediaStoreId,
            isPlaying = state.isPlaying,
            // Reset the angle when the track changes, so a new song starts from
            // the top rather than wherever the last one left off.
            resetKey = song.contentKey,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(24.dp))

        SeekBar(
            positionMs = positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffle) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous track",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next track",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (state.repeat == RepeatMode.One) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = when (state.repeat) {
                        RepeatMode.Off -> "Repeat off"
                        RepeatMode.All -> "Repeat queue"
                        RepeatMode.One -> "Repeat track"
                    },
                    tint = if (state.repeat == RepeatMode.Off) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }

        IconButton(onClick = onToggleFavourite) {
            Icon(
                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                tint = if (isFavourite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The record.
 *
 * Driven by a hand-advanced [Animatable] rather than `rememberInfiniteTransition`
 * for two reasons: pausing has to leave the disc at its current angle rather
 * than snapping back to zero, and the frame loop must stop dead when playback
 * stops so a paused app in the background is not animating anything.
 */
@Composable
private fun SpinningArtwork(
    mediaStoreId: Long?,
    isPlaying: Boolean,
    resetKey: Long,
    modifier: Modifier = Modifier,
) {
    val angle = remember { Animatable(0f) }

    LaunchedEffect(resetKey) { angle.snapTo(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val elapsedSec = (now - last) / 1_000_000_000f
            last = now
            angle.snapTo((angle.value + elapsedSec * DEGREES_PER_SECOND) % 360f)
        }
    }

    Artwork(
        mediaStoreId = mediaStoreId,
        shape = CircleShape,
        modifier = modifier.graphicsLayer { rotationZ = angle.value },
    )
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    // While the thumb is held, the slider shows the finger's position rather
    // than the player's — otherwise the next poll yanks it back mid-drag.
    var scrubbing by remember { mutableFloatStateOf(NOT_SCRUBBING) }
    val fraction = when {
        scrubbing != NOT_SCRUBBING -> scrubbing
        durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayedMs = if (scrubbing != NOT_SCRUBBING) (scrubbing * durationMs).toLong() else positionMs

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                onSeek((scrubbing * durationMs).toLong())
                scrubbing = NOT_SCRUBBING
            },
            enabled = durationMs > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(displayedMs), style = TimecodeTextStyle)
            Text(formatDuration(durationMs), style = TimecodeTextStyle)
        }
    }
}

/** Sentinel: a real fraction is always in 0..1, so -1 cannot collide. */
private const val NOT_SCRUBBING = -1f

/** One rotation every 12 seconds — slow enough to read the label, fast enough to notice. */
private const val DEGREES_PER_SECOND = 30f
