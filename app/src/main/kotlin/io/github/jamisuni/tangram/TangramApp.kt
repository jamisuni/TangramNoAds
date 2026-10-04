package io.github.jamisuni.tangram

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import io.github.jamisuni.tangram.settings.SettingsController
import io.github.jamisuni.tangram.settings.SettingsGear
import io.github.jamisuni.tangram.settings.SettingsOverlay
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.browse.AllPuzzlesOverlay
import io.github.jamisuni.tangram.browse.BROWSE_PAPER
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.browse.BrowseTopBar
import io.github.jamisuni.tangram.browse.RestartButton
import io.github.jamisuni.tangram.browse.SolvedBar
import io.github.jamisuni.tangram.kernel.layout.LayoutRules
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlayArea
import io.github.jamisuni.tangram.play.PuzzleThumbnail

/**
 * The screen (WO-004 design section 7). Window metrics include the system bars (DA-30), so a phone gets the same
 * tray on every API level. Back closes the grid while it is open (REQ-050); otherwise it leaves the app.
 */
@Composable
fun TangramApp(controller: BrowseController, host: SessionHost, aids: DebugAids, settings: SettingsController) {
    PlatformFeedbackLever.Provide { TangramContent(controller, host, aids, settings) }
}

@Composable
private fun TangramContent(controller: BrowseController, host: SessionHost, aids: DebugAids, settings: SettingsController) {
    val density = LocalDensity.current
    val sizePx = LocalWindowInfo.current.containerSize
    val widthDp = sizePx.width / density.density
    val heightDp = sizePx.height / density.density
    val layoutClass = LayoutRules.classFor(widthDp.toDouble())
    val rows = remember(layoutClass) { TrayRows.forClass(layoutClass) }

    val focus = LocalFocusManager.current
    LaunchedEffect(settings.isOpen) { if (settings.isOpen) focus.clearFocus() } // a base control may hold focus

    Box(Modifier.fillMaxSize().background(BROWSE_PAPER)) {
        val baseModifier = if (settings.isOpen) {
            Modifier.clearAndSetSemantics { }
                .focusProperties { onEnter = { cancelFocusChange() } }
                .focusGroup()
        } else {
            Modifier
        }
        Column(baseModifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            BrowseTopBar(controller, layoutClass, trailing = { SettingsGear(settings) })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val session = host.session
                if (session != null) {
                    key(session) {
                        PlayArea(
                            session = session,
                            layoutClass = layoutClass,
                            trayRows = rows,
                            screenHeight = heightDp.dp,
                            modifier = Modifier.fillMaxSize(),
                            inputEnabled = !settings.isOpen,
                            solvedBar = { SolvedBar(controller.shownBestSeconds, controller::restart, controller::next) },
                            // F1 / DA-71: always composed, so the slot is measured once and the board never relayouts
                            cornerControl = {
                                val visible = controller.shownState == PuzzleState.IN_PROGRESS
                                RestartButton(onClick = controller::restart, modifier = Modifier.unplacedUnless(visible))
                            },
                            // DA-82 / DA-75: no pill on a solved puzzle (the release twin renders nothing at all)
                            secondaryCornerControl = {
                                if (session.state != PuzzleState.SOLVED) {
                                    aids.CornerButton(
                                        puzzle = session.puzzle,
                                        solveNow = { poses -> session.solveByAid(poses) },
                                        blocked = { session.isDragging },
                                    )
                                }
                            },
                            boardOverlay = { space -> aids.BoardOverlay(session.puzzle, space) }, // not composed while solved (play)
                        )
                    }
                }
            }
        }
        if (controller.gridOpen) {
            AllPuzzlesOverlay(
                controller = controller,
                thumbnail = { p, solved, m -> PuzzleThumbnail(p, solved, m) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (settings.isOpen) {
            SettingsOverlay(settings, Modifier.fillMaxSize())
        }
    }
    BackHandler(enabled = controller.gridOpen) { controller.closeGrid() }
    BackHandler(enabled = settings.isOpen) { settings.close() } // registered last, so it wins
}

/**
 * Measured but, unless [visible], not placed and stripped of semantics: it draws nothing, takes no touch and cannot be found
 * (REQ-025 rule 2: Restart is shown only while the puzzle is in progress), while keeping its size for the layout.
 */
private fun Modifier.unplacedUnless(visible: Boolean): Modifier =
    (if (visible) this else this.clearAndSetSemantics { }) // an unplaced node still has semantics: clear them (tag, click)
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { if (visible) placeable.place(0, 0) }
        }
