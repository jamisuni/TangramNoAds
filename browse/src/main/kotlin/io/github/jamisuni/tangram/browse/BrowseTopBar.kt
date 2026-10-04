package io.github.jamisuni.tangram.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/** REQ-050 rule 1: a long press on next is at least this long (DA-54). */
internal const val LONG_PRESS_MS = 500L

/**
 * The top bar in the WO-003 title slot (66 dp phone, 74 dp tablet): previous, title + rating dots + state, the counter, next.
 * Every control has a touch area of at least 48 dp (REQ-037, DA-53). Reads `controller.index` and the host state
 * through the controller, so it recomposes when either changes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseTopBar(
    controller: BrowseController,
    layoutClass: LayoutClass,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val height = if (layoutClass == LayoutClass.TABLET) 74.dp else 66.dp
    val puzzle = controller.current
    val total = controller.puzzles.size
    val number = controller.index + 1
    val language = LocalConfiguration.current.locales[0].language
    val stateText = when (controller.shownState) {
        PuzzleState.NEW -> stringResource(R.string.state_new)
        PuzzleState.IN_PROGRESS -> stringResource(R.string.state_in_progress)
        PuzzleState.SOLVED -> stringResource(R.string.state_solved)
    }

    Row(
        modifier = modifier.fillMaxWidth().height(height).testTag("top-bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(
            glyph = "‹",
            description = stringResource(R.string.prev_puzzle),
            tag = "prev-button",
            onClick = controller::previous,
            onLongClick = null,
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = puzzle.title.inLanguage(language),
                modifier = Modifier.testTag("puzzle-title"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = BrowseStyle.INK, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingDots(puzzle.rating)
                BasicText(
                    text = stateText,
                    modifier = Modifier.testTag("puzzle-state"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = BrowseStyle.INK, fontSize = 13.sp),
                )
            }
        }
        val counterDescription = stringResource(R.string.puzzle_counter_description, number, total)
        Box(
            modifier = Modifier
                .testTag("puzzle-counter")
                .sizeIn(minWidth = BrowseStyle.MIN_TOUCH_DP.dp, minHeight = BrowseStyle.MIN_TOUCH_DP.dp)
                .height(52.dp)
                .combinedClickable(role = Role.Button, onClick = controller::openGrid)
                .semantics { contentDescription = counterDescription }
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(R.string.puzzle_counter, number, total),
                maxLines = 1,
                style = TextStyle(color = BrowseStyle.INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        ArrowButton(
            glyph = "›",
            description = stringResource(R.string.next_puzzle),
            tag = "next-button",
            onClick = controller::next,
            onLongClick = controller::nextUnsolved,
        )
        trailing()
    }
}

@Composable
private fun RatingDots(rating: Int) {
    val description = stringResource(R.string.rating_description, rating)
    Row(
        modifier = Modifier.testTag("rating-dots").semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 1..5) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (i <= rating) BrowseStyle.INK else BrowseStyle.INK.copy(alpha = 0.18f), CircleShape),
            )
        }
    }
}

/**
 * A 52 dp square control (above the 48 dp floor). With [onLongClick] it is combinedClickable under a view configuration
 * whose long-press timeout is max(500 ms, the platform's): one fire at the timeout, and the release then does nothing (DA-54).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArrowButton(
    glyph: String,
    description: String,
    tag: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val platform = LocalViewConfiguration.current
    val config = remember(platform) {
        object : ViewConfiguration by platform {
            override val longPressTimeoutMillis: Long = maxOf(LONG_PRESS_MS, platform.longPressTimeoutMillis)
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides config) {
        Box(
            modifier = Modifier
                .testTag(tag)
                .size(52.dp)
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = glyph, style = TextStyle(color = BrowseStyle.INK, fontSize = 30.sp, fontWeight = FontWeight.Bold))
        }
    }
}
