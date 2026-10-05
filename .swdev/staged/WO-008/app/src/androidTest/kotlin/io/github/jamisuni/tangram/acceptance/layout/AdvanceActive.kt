package io.github.jamisuni.tangram.acceptance.layout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.AppViewModel
import java.time.LocalDate

// Scaffolding for the WO-008 device tests (design 5.3 "Running" and 8, review N2 and E6; decision DA-142); no requirement token.
// `advanceActive` is the ONLY way a time-driven test moves time: `waitForIdle` first (so the keeper's running gate, which follows a state
// change by one frame, has emitted and the seconds are credited under the right flag), then, INSIDE ONE `onActivity` (the main thread), the
// manual clock moves and `accrue()` closes the interval, then `waitForIdle` (so the pill and the settings rows have recomposed). A test with a
// paused Compose clock (`mainClock.autoAdvance = false`) never emits the gate and must not assert time: switch it back on first.
internal fun <A : ComponentActivity> ActivityScenario<A>.advanceActive(
    rule: ComposeTestRule,
    clock: ManualTimeSource,
    ms: Long,
    newDate: LocalDate? = null,
) {
    rule.waitForIdle()
    onActivity { activity ->
        clock.advance(ms)
        if (newDate != null) clock.setDate(newDate)
        ViewModelProvider(activity)[AppViewModel::class.java].time.accrue()
    }
    rule.waitForIdle()
}
