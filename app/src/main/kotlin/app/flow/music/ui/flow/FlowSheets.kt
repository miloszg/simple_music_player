package app.flow.music.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.flow.music.domain.model.Song
import app.flow.music.ui.components.CoverPlate
import app.flow.music.ui.components.Plate
import app.flow.music.ui.formatDuration
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType
import app.flow.music.ui.theme.PlateShape
import app.flow.music.ui.theme.TrackNumberTextStyle

/** What a detail page is showing. */
data class FlowDetailContent(
    val title: String,
    val artist: String,
    val subtitle: String,
    val artworkSongId: Long?,
    val tracks: List<Song>,
    /** Liked songs is always cherry, whatever its name hashes to. */
    val cherryPlate: Boolean = false,
)

@Composable
fun FlowDetail(
    content: FlowDetailContent,
    playingKey: Long?,
    isFavourite: Boolean,
    onClose: () -> Unit,
    onToggleFavourite: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onTrack: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors

    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Text("←", style = FlowType.backArrow, color = colors.fg) }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(34.dp).clickable(onClick = onToggleFavourite),
                contentAlignment = Alignment.Center,
            ) {
                HeartIcon(
                    filled = isFavourite,
                    size = 18.dp,
                    color = if (isFavourite) colors.cherryHi else colors.fg2,
                )
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(horizontal = Gutter),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
        ) {
            item(key = "head") {
                Column {
                    if (content.cherryPlate) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(DETAIL_ASPECT)
                                .clip(PlateShape),
                        ) { Plate(title = content.title, toneOverride = CHERRY_TONE) }
                    } else {
                        CoverPlate(
                            mediaStoreId = content.artworkSongId,
                            title = content.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(DETAIL_ASPECT),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(content.subtitle, style = FlowType.detailSub, color = colors.fg2)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(PlateShape)
                                .background(colors.cherry)
                                .clickable(onClick = onPlay)
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Play", style = FlowType.detailAction, color = colors.onCherry) }
                        Box(
                            Modifier
                                .clip(PlateShape)
                                .border(1.dp, colors.fg3, PlateShape)
                                .clickable(onClick = onShuffle)
                                .padding(horizontal = 24.dp, vertical = 15.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Shuffle", style = FlowType.detailAction, color = colors.fg) }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            items(content.tracks.size, key = { content.tracks[it].contentKey }) { i ->
                val track = content.tracks[i]
                val playing = track.contentKey == playingKey
                Column {
                    FlowRule()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onTrack(i) }
                            .padding(horizontal = 2.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (playing) "▶" else "${i + 1}",
                            style = TrackNumberTextStyle,
                            color = if (playing) colors.cherryHi else colors.fg3,
                            modifier = Modifier.width(18.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style = FlowType.trackTitle,
                                color = if (playing) colors.cherryHi else colors.fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(track.artist, style = FlowType.miniSub, color = colors.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatDuration(track.durationMs), style = FlowType.rowSub, color = colors.fg2)
                    }
                }
            }
        }
    }
}

/** One switch row in Settings. */
data class FlowSetting(val label: String, val sub: String, val on: Boolean, val onToggle: () -> Unit)

@Composable
fun FlowSettings(
    dark: Boolean,
    followSystem: Boolean,
    settings: List<FlowSetting>,
    onClose: () -> Unit,
    onSetDark: () -> Unit,
    onSetLight: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors

    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(34.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Text("←", style = FlowType.backArrow, color = colors.fg) }
            Text("Settings", style = FlowType.sheetTitle, color = colors.fg)
        }

        LazyColumn(
            Modifier.weight(1f).padding(horizontal = Gutter),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            item(key = "manifesto") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(PlateShape)
                        .background(colors.cherry)
                        .padding(22.dp),
                ) {
                    Text(
                        "No ads. No account.\nNo tracking. Ever.",
                        style = FlowType.manifesto,
                        color = colors.onCherry,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Flow plays the music you already own and never sends a byte anywhere.",
                        style = FlowType.rowSub,
                        color = colors.onCherry.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            item(key = "appearance") {
                Column {
                    FlowRule()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Appearance", style = FlowType.settingLabel, color = colors.fg)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (followSystem) "Following the system" else "Set once, remembered on this device",
                                style = FlowType.rowSub,
                                color = colors.fg2,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ThemePill("Dark", dark, onSetDark)
                            ThemePill("Light", !dark, onSetLight)
                        }
                    }
                }
            }

            items(settings.size, key = { settings[it].label }) { i ->
                val setting = settings[i]
                Column {
                    FlowRule()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = setting.onToggle)
                            .padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(setting.label, style = FlowType.settingLabel, color = colors.fg)
                            Spacer(Modifier.height(3.dp))
                            Text(setting.sub, style = FlowType.rowSub, color = colors.fg2)
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(setting.on)
                    }
                }
            }

            item(key = "rescan") {
                Column {
                    FlowRule()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRescan)
                            .padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Rescan library", style = FlowType.settingLabel, color = colors.fg)
                            Spacer(Modifier.height(3.dp))
                            Text("Look for music added since last time", style = FlowType.rowSub, color = colors.fg2)
                        }
                        Text("→", style = FlowType.chevron, color = colors.fg3)
                    }
                    Spacer(Modifier.height(26.dp))
                    Text(
                        "Flow 1.0 — for people who still keep their music",
                        style = FlowType.miniSub,
                        color = colors.fg3,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePill(label: String, on: Boolean, onClick: () -> Unit) {
    val colors = Flow.colors
    Box(
        Modifier
            .clip(PlateShape)
            .background(if (on) colors.cherry else Color.Transparent)
            .border(1.dp, if (on) colors.cherryHi else colors.line2, PlateShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = FlowType.pill, color = if (on) colors.onCherry else colors.fg2)
    }
}

/** The design's 44x24 track with an 18px knob. */
@Composable
private fun Switch(on: Boolean) {
    val colors = Flow.colors
    Box(
        Modifier
            .size(44.dp, 24.dp)
            .clip(CircleShape)
            .background(if (on) colors.cherry else colors.line2)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (on) colors.onCherry else colors.fg2),
        )
    }
}

/** Empty state shared by Home and Library when there is no music at all. */
@Composable
fun FlowEmpty(title: String, body: String, actionLabel: String?, onAction: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = Flow.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = FlowType.emptyTitle, color = colors.fg, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(body, style = FlowType.rowSub, color = colors.fg2, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .clip(PlateShape)
                    .border(1.dp, colors.fg3, PlateShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 11.dp),
            ) { Text(actionLabel, style = FlowType.pill, color = colors.fg) }
        }
    }
}

private const val DETAIL_ASPECT = 1.12f

/** Index of cherry in the tone list. */
private const val CHERRY_TONE = 1
