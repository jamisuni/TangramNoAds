package io.github.jamisuni.tangram.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/**
 * REQ-050 / DA-55: the all-puzzles grid, a full-size overlay (the caller places it over the whole screen, top bar
 * included). One cell per puzzle in library order; [thumbnail] draws it (`solved = true` is the picture, false the flat
 * silhouette), so `browse` does not depend on `play`. A cell opens its puzzle and closes the grid; "Done" closes it.
 */
@Composable
fun AllPuzzlesOverlay(
    controller: BrowseController,
    thumbnail: @Composable (Puzzle, Boolean, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalConfiguration.current.locales[0].language
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = controller.index)
    Column(
        modifier = modifier
            .testTag("all-puzzles")
            .background(BrowseStyle.PAPER)
            .pointerInput(Unit) { detectTapGestures { } } // swallow touches so nothing below the overlay reacts
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(R.string.all_puzzles),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                style = TextStyle(color = BrowseStyle.INK, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
            Box(
                modifier = Modifier
                    .testTag("grid-close")
                    .sizeIn(minWidth = BrowseStyle.MIN_TOUCH_DP.dp, minHeight = BrowseStyle.MIN_TOUCH_DP.dp)
                    .clickable(role = Role.Button, onClick = controller::closeGrid)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.done),
                    style = TextStyle(color = BrowseStyle.ACCENT, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(84.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(controller.puzzles, key = { _, p -> p.id.value }) { i, puzzle ->
                val state = controller.stateOf(i)
                val shown = i == controller.index
                val description = stringResource(
                    R.string.grid_cell_description,
                    i + 1,
                    puzzle.title.inLanguage(language),
                    stateWord(state),
                )
                // The cell is not a merging node: its dot has to stay findable. The click layer is a sibling on top.
                Box(
                    modifier = Modifier
                        .testTag("grid-cell-${puzzle.id.value}")
                        .sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrowseStyle.CELL)
                        .border(if (shown) 3.dp else 0.dp, if (shown) BrowseStyle.ACCENT else BrowseStyle.CELL, RoundedCornerShape(12.dp)),
                ) {
                    thumbnail(puzzle, state == PuzzleState.SOLVED, Modifier.fillMaxSize().padding(8.dp))
                    BasicText(
                        text = (i + 1).toString(),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 4.dp),
                        style = TextStyle(color = BrowseStyle.INK, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                    if (state == PuzzleState.IN_PROGRESS) {
                        Box(
                            Modifier
                                .testTag("grid-dot-${puzzle.id.value}")
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(10.dp)
                                .background(BrowseStyle.DOT, CircleShape),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(role = Role.Button) {
                                controller.open(i)
                                controller.closeGrid()
                            }
                            .semantics { contentDescription = description },
                    )
                }
            }
        }
    }
}

@Composable
private fun stateWord(state: PuzzleState): String = when (state) {
    PuzzleState.NEW -> stringResource(R.string.state_new)
    PuzzleState.IN_PROGRESS -> stringResource(R.string.state_in_progress)
    PuzzleState.SOLVED -> stringResource(R.string.state_solved)
}
