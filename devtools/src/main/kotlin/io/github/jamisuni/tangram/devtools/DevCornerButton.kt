package io.github.jamisuni.tangram.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece

/**
 * REQ-046 rule 1: the small dashed DEV pill and the dialog it opens. The touch box is exactly 56 x 40 dp and the
 * label ignores the system font scale (DA-75). A click while [blocked] is true does nothing (DA-87). Hiding the
 * pill on a solved puzzle is the caller's job, through [modifier] (design 3, rule 6).
 *
 * [solveNow] is given the stored solution's poses ([DevSolution.poses]); if it returns false, or the poses are
 * null, the dialog stays open with the "could not place" notice (DA-80).
 */
@Composable
fun DevCornerButton(
    state: DevToolsState,
    puzzle: Puzzle,
    solveNow: (List<PlacedPiece>) -> Boolean,
    blocked: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val on = state.overlayOn
    val description = stringResource(R.string.devtools_button_description)
    val dens = LocalDensity.current
    val labelSize = with(dens) { 11.dp.toSp() } // dp-equivalent: the font scale is not applied
    Box(
        modifier = modifier
            .testTag("dev-button")
            .size(DevStyle.PILL_WIDTH_DP.dp, DevStyle.PILL_HEIGHT_DP.dp)
            .alpha(if (on) 1f else 0.75f)
            .clickable(role = Role.Button) { if (!blocked()) state.open() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val lineColour = if (on) DevStyle.ACCENT else DevStyle.MUTED
        Box(
            modifier = Modifier
                .size(DevStyle.PILL_WIDTH_DP.dp, 32.dp)
                .drawBehind {
                    val w = 1.5f * density
                    val stroke = if (on) {
                        Stroke(width = w)
                    } else {
                        Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density)))
                    }
                    drawRoundRect(
                        color = lineColour,
                        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(16f * density),
                        style = stroke,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(R.string.devtools_button),
                maxLines = 1,
                style = TextStyle(color = lineColour, fontSize = labelSize, fontWeight = FontWeight.Bold),
            )
        }
    }
    if (state.dialogOpen) {
        DevDialog(state, puzzle, solveNow)
    }
}

@Composable
private fun DevDialog(state: DevToolsState, puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean) {
    Dialog(onDismissRequest = state::close) {
        Column(
            modifier = Modifier
                .testTag("dev-dialog")
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DevStyle.PAPER)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(
                text = stringResource(R.string.devtools_title),
                style = TextStyle(color = DevStyle.INK, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
            if (state.unlocked) {
                UnlockedView(state, puzzle, solveNow)
            } else {
                LockedView(state)
            }
        }
    }
}

@Composable
private fun NoticeLine(state: DevToolsState) {
    val text = when (state.notice) {
        DevNotice.NONE -> return
        DevNotice.WRONG_PASSCODE -> stringResource(R.string.devtools_wrong_passcode)
        DevNotice.SOLVE_FAILED -> stringResource(R.string.devtools_solve_failed)
    }
    BasicText(
        text = text,
        modifier = Modifier.testTag("dev-notice"),
        style = TextStyle(color = DevStyle.NOTICE, fontSize = 14.sp),
    )
}

@Composable
private fun LockedView(state: DevToolsState) {
    var code by remember { mutableStateOf("") }
    BasicText(
        text = stringResource(R.string.devtools_hint_locked),
        style = TextStyle(color = DevStyle.INK, fontSize = 15.sp),
    )
    BasicText(
        text = stringResource(R.string.devtools_passcode_label),
        style = TextStyle(color = DevStyle.MUTED, fontSize = 12.sp),
    )
    // No hint text in the field: a hint would leak the code into the UI. Longer input is refused, not cut.
    BasicTextField(
        value = code,
        onValueChange = { if (it.length <= 4) code = it },
        modifier = Modifier
            .testTag("dev-passcode")
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DevStyle.CELL)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = DevStyle.INK, fontSize = 18.sp),
        cursorBrush = SolidColor(DevStyle.INK),
    )
    NoticeLine(state)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    ) {
        DialogButton(stringResource(R.string.devtools_cancel), "dev-cancel", filled = false, onClick = state::close)
        DialogButton(stringResource(R.string.devtools_ok), "dev-ok", filled = true) {
            if (!state.submit(code)) code = ""
        }
    }
}

@Composable
private fun UnlockedView(state: DevToolsState, puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean) {
    BasicText(
        text = stringResource(R.string.devtools_hint_unlocked),
        style = TextStyle(color = DevStyle.INK, fontSize = 15.sp),
    )
    val showText = stringResource(
        if (state.overlayOn) R.string.devtools_hide_solution else R.string.devtools_show_solution,
    )
    DialogButton(showText, "dev-show-solution", filled = false, wide = true) {
        state.toggleOverlay()
        state.close()
    }
    DialogButton(stringResource(R.string.devtools_solve_now), "dev-solve-now", filled = false, wide = true) {
        val poses = DevSolution.poses(puzzle)
        if (poses != null && solveNow(poses)) state.close() else state.solveFailed()
    }
    BasicText(
        text = stringResource(R.string.devtools_no_best_time),
        style = TextStyle(color = DevStyle.MUTED, fontSize = 12.sp),
    )
    NoticeLine(state)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        DialogButton(stringResource(R.string.devtools_done), "dev-done", filled = true, onClick = state::close)
    }
}

@Composable
private fun DialogButton(
    text: String,
    tag: String,
    filled: Boolean,
    wide: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .then(if (wide) Modifier.fillMaxWidth() else Modifier.sizeIn(minWidth = 88.dp))
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) DevStyle.ACCENT else DevStyle.CELL)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = if (filled) Color.White else DevStyle.INK,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
