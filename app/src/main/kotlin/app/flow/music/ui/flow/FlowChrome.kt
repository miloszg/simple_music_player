package app.flow.music.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.flow.music.ui.components.FlowMark
import app.flow.music.ui.theme.Flow
import app.flow.music.ui.theme.FlowType
import app.flow.music.ui.theme.InstrumentSans
import app.flow.music.ui.theme.PlateShape

/** Bottom destinations. Three, as the design has them. */
enum class FlowTab(val label: String) { Home("Home"), Search("Search"), Library("Library") }

/** The design's page gutter. Everything aligns to it. */
val Gutter = 18.dp

/**
 * Header: mark, wordmark, and a hamburger that opens Settings.
 *
 * `padding: 2px 18px 14px` in the design. The status-bar inset is applied by
 * the caller — the canvas mock got it from its device frame.
 */
@Composable
fun FlowHeader(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = Gutter, top = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowMark(size = 20.dp)
        Spacer(Modifier.width(9.dp))
        Text("FLOW", style = FlowType.wordmark, color = Flow.colors.fg)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(32.dp)
                .clip(PlateShape)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.width(14.dp),
                verticalArrangement = Arrangement.spacedBy(3.5.dp),
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(Flow.colors.fg2),
                    )
                }
            }
        }
    }
}

/**
 * The bottom tab bar: a 7px dot over a label, cherry when active.
 *
 * No icons anywhere — three identical dots, and the type carries the state.
 * That restraint is most of why the bar reads as quiet rather than as chrome.
 */
@Composable
fun FlowTabBar(
    selected: FlowTab,
    onSelect: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Flow.colors
    Column(modifier.fillMaxWidth().background(colors.bar)) {
        FlowRule()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 4.dp),
        ) {
            FlowTab.entries.forEach { entry ->
                val active = entry == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            // The design has no ripple; a rectangular flash under
                            // a 7px dot reads as a rendering fault.
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(entry) }
                        .padding(top = 11.dp, bottom = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.cherryHi else colors.fg3),
                    )
                    Text(
                        text = entry.label,
                        style = TabLabel.copy(
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (active) colors.fg else colors.fg2,
                    )
                }
            }
        }
    }
}

private val TabLabel = TextStyle(
    fontFamily = InstrumentSans,
    fontSize = 11.sp,
    letterSpacing = 0.44.sp, // .04em
)

/** The hairline rule used between sections and rows throughout the design. */
@Composable
fun FlowRule(modifier: Modifier = Modifier, color: Color = Flow.colors.line) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** The solid cherry triangle used for every "play" affordance. */
@Composable
fun PlayTriangle(size: androidx.compose.ui.unit.Dp, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w, h / 2f)
            lineTo(0f, h)
            close()
        }
        drawPath(path, color)
    }
}

/** The two-bar pause glyph. */
@Composable
fun PauseGlyph(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    barWidth: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.size(width, height),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(2) {
            Box(Modifier.width(barWidth).height(height).background(color))
        }
    }
}
