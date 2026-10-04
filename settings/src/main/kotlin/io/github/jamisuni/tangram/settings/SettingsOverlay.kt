package io.github.jamisuni.tangram.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The settings screen (REQ-032), an in-app overlay: no Dialog, Popup or AlertDialog (DA-118, DA-125). Content order
 * (REQ-032): sound, reset, how to play, the free note (REQ-009, green box), the privacy text (REQ-049, plain text below
 * it, no click action). [WO-008 inserts timer and play time rows without reordering, DA-124.] No difficulty control.
 * The caller places it over the whole screen and gives it the safe-drawing padding it needs; this swallows touches.
 */
@Composable
fun SettingsOverlay(controller: SettingsController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .testTag("settings-overlay")
            .background(SettingsStyle.PAPER)
            .pointerInput(Unit) { detectTapGestures { } } // swallow touches so nothing below the overlay reacts
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(R.string.settings_title),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = TextStyle(color = SettingsStyle.INK, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
                Box(
                    modifier = Modifier
                        .testTag("settings-close")
                        .sizeIn(minWidth = SettingsStyle.MIN_TOUCH_DP.dp, minHeight = SettingsStyle.MIN_TOUCH_DP.dp)
                        .clickable(role = Role.Button, onClick = controller::close)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(R.string.settings_done),
                        style = TextStyle(color = SettingsStyle.ACCENT, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .testTag("settings-scroll")
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SoundRow(controller)
                ResetSection(controller)
                BasicText(
                    text = stringResource(R.string.settings_how_to),
                    modifier = Modifier.testTag("settings-how-to").fillMaxWidth(),
                    style = TextStyle(color = SettingsStyle.MUTED, fontSize = 14.sp, lineHeight = 21.sp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SettingsStyle.FREE_BG)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicText(
                        text = stringResource(R.string.settings_free_note),
                        modifier = Modifier.testTag("settings-free-note").fillMaxWidth(),
                        style = TextStyle(color = SettingsStyle.FREE_INK, fontSize = 15.sp, lineHeight = 22.sp),
                    )
                }
                BasicText(
                    text = stringResource(R.string.settings_privacy),
                    modifier = Modifier.testTag("settings-privacy").fillMaxWidth(),
                    style = TextStyle(color = SettingsStyle.MUTED, fontSize = 14.sp, lineHeight = 21.sp),
                )
            }
        }
    }
}

@Composable
private fun SoundRow(controller: SettingsController) {
    val on = controller.soundOn
    val state = stringResource(if (on) R.string.settings_sound_on_description else R.string.settings_sound_off_description)
    Row(
        modifier = Modifier
            .testTag("settings-sound")
            .fillMaxWidth()
            .heightIn(min = SettingsStyle.MIN_TOUCH_DP.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = controller::setSound)
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(R.string.settings_sound),
            modifier = Modifier.weight(1f),
            style = TextStyle(color = SettingsStyle.INK, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (on) SettingsStyle.ACCENT else SettingsStyle.CELL)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            BasicText(
                text = state,
                style = TextStyle(
                    color = if (on) Color.White else SettingsStyle.INK,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun ResetSection(controller: SettingsController) {
    if (!controller.confirmingReset) {
        SettingsButton(
            text = stringResource(R.string.settings_reset),
            tag = "settings-reset",
            color = SettingsStyle.DANGER,
            onClick = controller::requestReset,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                text = stringResource(R.string.settings_reset_question),
                modifier = Modifier.testTag("settings-reset-question").fillMaxWidth(),
                style = TextStyle(color = SettingsStyle.INK, fontSize = 16.sp, lineHeight = 23.sp),
            )
            // Keep first, Erase second: the safe choice is the first one reached (design section 5).
            SettingsButton(
                text = stringResource(R.string.settings_reset_cancel),
                tag = "settings-reset-cancel",
                color = SettingsStyle.INK,
                onClick = controller::cancelReset,
            )
            SettingsButton(
                text = stringResource(R.string.settings_reset_confirm),
                tag = "settings-reset-confirm",
                color = SettingsStyle.DANGER,
                onClick = controller::confirmReset,
            )
        }
    }
}

@Composable
private fun SettingsButton(text: String, tag: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .heightIn(min = SettingsStyle.MIN_TOUCH_DP.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SettingsStyle.CELL)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}
