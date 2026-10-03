package io.github.jamisuni.tangram.play.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.play.PlayLayout
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.play.R

/**
 * Required by design section 2 and 3 (REQ-011 A1, REQ-012 A1, REQ-043 A1, REQ-018 A2; DA-19 solved timeline).
 * TASK-018a: draws the whole play area and nothing else. No pointer input, no frame loop, no side effect in draw.
 * [layout] and [nowMs] (the frame clock, design E5) are parameters; PlayArea (TASK-018b) supplies them.
 *
 * Observation: the draw block reads `session.version` once per pass (and the derived semantics below read the
 * session through it too), so every intent and `onFrame` redraws. The semantics-only nodes carry the test tags:
 * `size-mark-<id>` per visible mark and `flip-badge` with the badge rect as bounds.
 */
@Composable
internal fun PlayCanvas(session: PlaySession, layout: PlayLayout, nowMs: Long, modifier: Modifier = Modifier) {
    val texts = SizeMarkTexts(
        stringResource(R.string.size_mark_large),
        stringResource(R.string.size_mark_medium),
        stringResource(R.string.size_mark_small),
    )
    val descriptions = SizeMarkTexts(
        stringResource(R.string.size_large_description),
        stringResource(R.string.size_medium_description),
        stringResource(R.string.size_small_description),
    )
    val badgeDescription = stringResource(R.string.flip_badge)
    val measurer = rememberTextMeasurer()
    val marks by remember(session, layout) { derivedStateOf { visibleSizeMarks(session) } }
    val badge by remember(session, layout) { derivedStateOf { layout.badgeRect(session) } }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawPlay(session, layout, nowMs, texts, measurer)
        }
        for ((piece, mark) in marks) {
            if (!layout.hasCell(piece)) continue
            val r = sizeMarkRect(layout.cell(piece))
            Box(
                Modifier
                    .offset(r.left.dp, r.top.dp)
                    .size(r.width.dp, r.height.dp)
                    .semantics {
                        testTag = "size-mark-${piece.name}"
                        text = AnnotatedString(texts.of(mark))
                        contentDescription = descriptions.of(mark)
                    },
            )
        }
        badge?.let { r ->
            Box(
                Modifier
                    .offset(r.left.dp, r.top.dp)
                    .size(r.width.dp, r.height.dp)
                    .semantics {
                        testTag = "flip-badge"
                        role = Role.Button
                        contentDescription = badgeDescription
                    },
            )
        }
    }
}
