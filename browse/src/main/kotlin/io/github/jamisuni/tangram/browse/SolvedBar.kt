package io.github.jamisuni.tangram.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * REQ-026: what the tray region shows while the puzzle is solved: Retry, the best time, Next (the primary action).
 * `best-time` holds the value only: a dash, or `m:ss` (hours are WO-008's, DA-52). Each button is at least 48 dp high.
 */
@Composable
fun SolvedBar(bestSeconds: Long?, onRetry: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val value = if (bestSeconds == null) {
        stringResource(R.string.best_time_none)
    } else {
        stringResource(R.string.time_minutes_seconds, (bestSeconds / 60).toInt(), (bestSeconds % 60).toInt())
    }
    Row(
        modifier = modifier.fillMaxWidth().testTag("solved-bar").padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BarButton(stringResource(R.string.retry), "retry-button", filled = false, onClick = onRetry)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = stringResource(R.string.best_time),
                maxLines = 1,
                style = TextStyle(color = BrowseStyle.INK.copy(alpha = 0.7f), fontSize = 12.sp),
            )
            BasicText(
                text = value,
                modifier = Modifier.testTag("best-time"),
                maxLines = 1,
                style = TextStyle(color = BrowseStyle.INK, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
        }
        BarButton(stringResource(R.string.next), "solved-next-button", filled = true, onClick = onNext)
    }
}

@Composable
private fun BarButton(text: String, tag: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .sizeIn(minWidth = 96.dp, minHeight = BrowseStyle.MIN_TOUCH_DP.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (filled) BrowseStyle.GREEN else BrowseStyle.CELL)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = if (filled) Color.White else BrowseStyle.INK,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * REQ-025 rule 2: the Restart pill. Drawn 44 dp high inside a touch area of at least 48 x 48 dp (REQ-037, DA-53).
 */
@Composable
fun RestartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag("restart-button")
            .sizeIn(minWidth = BrowseStyle.MIN_TOUCH_DP.dp, minHeight = BrowseStyle.MIN_TOUCH_DP.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BrowseStyle.CELL)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(R.string.restart),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = BrowseStyle.INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
