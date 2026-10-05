package io.github.jamisuni.tangram.kernel.time

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

// Implementer scaffolding (disposable); the acceptance tests are the Test Author's.
class ActiveSecondScaffoldingTest {
    private fun ms(prev: Long, now: Long, visible: Boolean = true, touch: Long? = null, down: Boolean = false) =
        ActiveSecond.activeMs(prev, now, visible, touch, down)

    @Test fun hiddenCreditsNothing() = assertEquals(0L, ms(0, 1000, visible = false, touch = 0, down = true))
    @Test fun touchingCreditsWholeInterval() = assertEquals(1000L, ms(0, 1000, down = true))
    @Test fun neverTouchedCreditsNothing() = assertEquals(0L, ms(0, 1000))
    @Test fun touchExactlyAtLimitCreditsThroughNow() = assertEquals(1000L, ms(299_000, 300_000, touch = 240_000))
    @Test fun straddlingWindowEndCreditsPartOnly() = assertEquals(500L, ms(59_500, 61_000, touch = 0))
    @Test fun beyondWindowCreditsNothing() = assertEquals(0L, ms(60_001, 70_000, touch = 0))
    @Test fun emptyOrReversedIntervalCreditsNothing() {
        assertEquals(0L, ms(1000, 1000, down = true))
        assertEquals(0L, ms(2000, 1000, down = true))
    }

    @Test fun puzzleCountsOnlyInProgressWithNoOverlay() {
        assertTrue(ActiveSecond.puzzleCounts(PuzzleState.IN_PROGRESS, settingsOpen = false, gridOpen = false))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.IN_PROGRESS, settingsOpen = true, gridOpen = false))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.IN_PROGRESS, settingsOpen = false, gridOpen = true))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.NEW, settingsOpen = false, gridOpen = false))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.SOLVED, settingsOpen = false, gridOpen = false))
    }
}
