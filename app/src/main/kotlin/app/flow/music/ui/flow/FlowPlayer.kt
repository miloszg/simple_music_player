package app.flow.music.ui.flow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Song
import app.flow.music.playback.PlaybackState
import app.flow.music.playback.RepeatMode
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.PlateSize
import app.flow.music.ui.formatDuration
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType
import app.flow.music.ui.theme.PlateShape
import app.flow.music.ui.theme.QueueSheetShape
import app.flow.music.ui.theme.TimecodeTextStyle

/**
 * The docked bar above the tabs.
 *
 * A 2px progress strip, a 56dp plate, the title pair, then play and next. The
 * plate carries the album's first word in the serif when there's no artwork —
 * the design's `npAlbumShort`.
 */
@Composable
fun FlowMiniPlayer(
    state: PlaybackState,
    progress: Float,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors
    val song = state.current ?: return

    Column(modifier.fillMaxWidth().background(colors.bar)) {
        FlowRule()
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.line),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(colors.cherryHi),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverPlate(
                mediaStoreId = song.mediaStoreId,
                title = song.album.substringBefore(' ').ifBlank { song.album },
                shape = androidx.compose.ui.graphics.RectangleShape,
                modifier = Modifier
                    .size(PlateSize.mini)
                    .clickable(onClick = onExpand),
            )
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 14.dp),
            ) {
                Text(song.title, style = FlowType.miniTitle, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(song.artist, style = FlowType.miniSub, color = colors.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(46.dp, 56.dp).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isPlaying) {
                    PauseGlyph(12.dp, 14.dp, 4.dp, colors.fg)
                } else {
                    PlayTriangle(13.dp, colors.fg, Modifier.padding(start = 3.dp))
                }
            }
            Box(
                Modifier.size(46.dp, 56.dp).clickable(onClick = onNext).padding(end = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                SkipGlyph(forward = true, color = colors.fg2)
            }
        }
    }
}

@Composable
fun SkipGlyph(forward: Boolean, color: Color, big: Boolean = false) {
    val tri = if (big) 11.dp else 9.dp
    val barW = if (big) 2.5.dp else 2.dp
    val barH = if (big) 17.dp else 13.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (forward) {
            PlayTriangle(tri, color)
            Box(Modifier.size(barW, barH).background(color))
        } else {
            Box(Modifier.size(barW, barH).background(color))
            Box(Modifier.graphicsLayer { scaleX = -1f }) { PlayTriangle(tri, color) }
        }
    }
}

/**
 * The full player.
 *
 * Laid out exactly as the design: header row, square plate, serif title with
 * the heart beside it, seek, transport, the "Next" strip, then the three
 * pills. The queue slides up over the lower 62%.
 */
@Composable
fun FlowNowPlaying(
    state: PlaybackState,
    positionMs: Long,
    isFavourite: Boolean,
    queueOpen: Boolean,
    sleepOn: Boolean,
    speedLabel: String,
    onClose: () -> Unit,
    onToggleQueue: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavourite: () -> Unit,
    onToggleSleep: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenAlbum: () -> Unit,
    onQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors
    val song = state.current
    val duration = state.durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Box(modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header: collapse, source, queue toggle
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(14.dp, 2.dp).background(colors.fg))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (state.shuffle) "Shuffling · ${song?.album.orEmpty()}"
                    else "Playing from ${song?.album.orEmpty()}",
                    style = FlowType.playerSource,
                    color = colors.fg2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clickable(onClick = onToggleQueue),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(Modifier.width(14.dp), verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                        Box(Modifier.width(14.dp).height(1.8.dp).background(colors.cherryHi))
                        Box(Modifier.width(14.dp).height(1.8.dp).background(colors.fg2))
                        Box(Modifier.width(8.dp).height(1.8.dp).background(colors.fg2))
                    }
                }
            }

            Box(Modifier.padding(horizontal = 24.dp).padding(top = 10.dp)) {
                CoverPlate(
                    mediaStoreId = song?.mediaStoreId,
                    title = song?.album,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }

            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            song?.title.orEmpty(),
                            style = FlowType.playerTitle,
                            color = colors.fg,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            song?.artist.orEmpty(),
                            style = FlowType.rowTitle.copy(fontSize = FlowType.miniTitle.fontSize),
                            color = colors.fg2,
                            modifier = Modifier.clickable(onClick = onOpenAlbum),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier.size(38.dp).clickable(onClick = onToggleFavourite),
                        contentAlignment = Alignment.Center,
                    ) {
                        HeartIcon(
                            filled = isFavourite,
                            size = 20.dp,
                            color = if (isFavourite) colors.cherryHi else colors.fg2,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SeekBar(fraction, positionMs, state.durationMs, onSeekFraction)

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIcon(active = state.shuffle, onClick = onToggleShuffle) {
                        ShuffleGlyph(if (state.shuffle) colors.cherryHi else colors.fg2)
                    }
                    Box(
                        Modifier.size(50.dp).clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center,
                    ) { SkipGlyph(forward = false, color = colors.fg, big = true) }

                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(colors.cherry)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isPlaying) {
                            PauseGlyph(19.dp, 24.dp, 6.5.dp, Color.White)
                        } else {
                            PlayTriangle(21.dp, Color.White, Modifier.padding(start = 6.dp))
                        }
                    }

                    Box(
                        Modifier.size(50.dp).clickable(onClick = onNext),
                        contentAlignment = Alignment.Center,
                    ) { SkipGlyph(forward = true, color = colors.fg, big = true) }
                    RoundIcon(active = state.repeat != RepeatMode.Off, onClick = onCycleRepeat) {
                        RepeatGlyph(
                            if (state.repeat != RepeatMode.Off) colors.cherryHi else colors.fg2,
                            one = state.repeat == RepeatMode.One,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                FlowRule()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleQueue)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Next", style = FlowType.nextLabel, color = colors.fg2)
                    Text(
                        text = state.queue.getOrNull(state.queueIndex + 1)?.title ?: "End of queue",
                        style = FlowType.nextTitle,
                        color = colors.fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("↑", style = FlowType.chevron, color = colors.fg3)
                }

                Row(
                    Modifier.padding(top = 14.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pill(if (sleepOn) "Sleep 25:00" else "Sleep", sleepOn, Modifier.weight(1f), onToggleSleep)
                    Pill(speedLabel, speedLabel != "1.0×", Modifier.weight(1f), onCycleSpeed)
                    Pill("Queue", queueOpen, Modifier.weight(1f), onToggleQueue)
                }
            }
        }

        QueuePanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            open = queueOpen,
            songs = state.queue.drop(state.queueIndex + 1),
            onClose = onToggleQueue,
            onPick = { onQueueItem(state.queueIndex + 1 + it) },
        )
    }
}

@Composable
private fun SeekBar(fraction: Float, positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit) {
    val colors = Flow.colors
    val animated by animateFloatAsState(fraction, label = "seek")
    val density = LocalDensity.current

    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
                },
        ) {
            val trackWidth = maxWidth
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.line2),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .background(colors.cherryHi),
                )
            }
            Box(
                Modifier
                    .offset(x = trackWidth * animated - 5.5.dp, y = with(density) { -4.dp })
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(colors.cherryHi)
                    .align(Alignment.CenterStart),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), style = TimecodeTextStyle, color = colors.fg2)
            // The design shows time remaining, negative — not total duration.
            Text("-" + formatDuration((durationMs - positionMs).coerceAtLeast(0)), style = TimecodeTextStyle, color = colors.fg2)
        }
    }
}

@Composable
private fun RoundIcon(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Pill(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Flow.colors
    Box(
        modifier
            .clip(PlateShape)
            .background(if (on) colors.cherry else Color.Transparent)
            .border(1.dp, if (on) colors.cherryHi else colors.line2, PlateShape)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = FlowType.pill, color = if (on) colors.onCherry else colors.fg2)
    }
}

@Composable
private fun QueuePanel(
    open: Boolean,
    songs: List<Song>,
    onClose: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors
    if (!open) return

    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(QUEUE_HEIGHT)
            .clip(QueueSheetShape)
            .background(colors.bar)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.cherry))
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Up next", style = FlowType.sheetTitle, color = colors.fg, modifier = Modifier.weight(1f))
            Text(
                "Close",
                style = FlowType.closeLink,
                color = colors.cherryHi,
                modifier = Modifier.clickable(onClick = onClose),
            )
        }
        LazyColumn(Modifier.padding(horizontal = 20.dp)) {
            items(songs.size, key = { songs[it].contentKey }) { i ->
                val song = songs[i]
                Column {
                    FlowRule()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(i) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverPlate(song.mediaStoreId, song.album, Modifier.size(PlateSize.queue))
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = FlowType.miniTitle, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(song.artist, style = FlowType.miniSub, color = colors.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(Modifier.width(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) { Box(Modifier.fillMaxWidth().height(1.5.dp).background(colors.fg3)) }
                        }
                    }
                }
            }
        }
    }
}

private const val QUEUE_HEIGHT = 0.62f
