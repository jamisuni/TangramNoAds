package io.github.jamisuni.tangram

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.acceptance.touch
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

// SCAFFOLDING (disposable): decision DA-118, the settings wiring in TangramApp. It carries no REQ token.
class SettingsWiringScaffoldingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzle = PuzzleLibrary.packaged().puzzles.first()

    private fun openAndClose() {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
        compose.touch("settings-close")
        compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
    }

    @Test
    fun gearOpensAndDoneClosesOnNew() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("settings-button").assertExists()
            openAndClose()
        }
    }

    @Test
    fun gearOpensAndDoneClosesInProgress() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            TouchRig(compose).placePieces(puzzle, 1)
            compose.mainClock.autoAdvance = true
            openAndClose()
        }
    }

    @Test
    fun gearOpensAndDoneClosesSolved() {
        AppStore.open().apply {
            saveProgress(puzzle.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
            saveLastShownPuzzle(puzzle.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("solved-bar").assertExists()
            compose.onNodeWithTag("settings-button").assertExists()
            openAndClose()
        }
    }

    @Test
    fun gearExistsWithTheGridOpen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.touch("puzzle-counter")
            compose.onNodeWithTag("grid-close").assertExists()
            compose.onNodeWithTag("settings-button").assertExists()
        }
    }
}
