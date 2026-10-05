package io.github.jamisuni.tangram.time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jamisuni.tangram.kernel.time.DurationFormat

/**
 * The on-board timer pill (REQ-031, design 7). Inert: no click action, no pointer input, no role.
 * The text is 14 sp with tabular figures and the pill pads 10 dp on each side: the board layout
 * reserves exactly that for its template (DA-147), so keep the numbers in step with it.
 */
@Composable
fun PuzzleTimer(shown: Boolean, seconds: Long, modifier: Modifier = Modifier) {
    if (!shown) return
    val parts = DurationFormat.parts(seconds)
    val text = if (parts.withHours) {
        stringResource(R.string.time_hours_minutes, parts.hours, parts.minutes)
    } else {
        stringResource(R.string.time_minutes_seconds, parts.minutes, parts.seconds)
    }
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .testTag("puzzle-timer")
            .background(PILL_PAPER, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(text, style = TextStyle(color = PILL_INK, fontSize = 14.sp, fontFeatureSettings = "tnum"))
    }
}

private val PILL_INK = Color(0xFF2B2D42)
private val PILL_PAPER = Color(0xFFFBF7EE).copy(alpha = 0.85f)
