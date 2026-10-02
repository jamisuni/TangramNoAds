package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-014): not an acceptance test.
class SolvedPiecesScaffoldingTest {
    @Test fun parsesGoodPaths() {
        val s = PathData.parse("M3.3,3.35 Q3.7,3.55 4,3.35")!!
        assertEquals(PathSeg.MoveTo(3.3, 3.35), s[0])
        assertEquals(PathSeg.QuadTo(3.7, 3.55, 4.0, 3.35), s[1])
        assertEquals(
            listOf(PathSeg.MoveTo(0.0, -1.5), PathSeg.LineTo(.5, 2.0), PathSeg.Close),
            PathData.parse("M0 -1.5L.5,2 Z"),
        )
        assertEquals(PathSeg.CubicTo(1.0, 1.0, 2.0, 2.0, 3.0, 3.0), PathData.parse("M0,0 C1,1 2,2 3,3")!![1])
    }

    @Test fun minusIsAnImplicitSeparator() {
        assertEquals(
            listOf(PathSeg.MoveTo(0.0, 0.0), PathSeg.LineTo(1.0, -2.0), PathSeg.Close),
            PathData.parse("M0 0L1-2Z"),
        )
        // decision: "M0-0" is M 0,-0 (a minus starts a number); "1--2" is rejected (a bare minus is no number)
        assertEquals(PathSeg.MoveTo(0.0, -0.0), PathData.parse("M0-0")!![0])
        assertEquals(PathSeg.MoveTo(1.0, -2.0), PathData.parse("M1-2")!![0])
        assertNull(PathData.parse("M1--2"))
    }

    @Test fun rejectsBadPaths() {
        listOf(
            "", " ", "L1,1", "m1,1", "M1,1 l2,2", "M1,1 2,2", "M1", "M1,2,3", "M1,2 L3", "M1e2,3", "M+1,2",
            "M1,,2", "M,1 2", "M1.,2", "M1,2 Z3", "M1", "M1--2", "M0 0L1--2Z", "M1,2-3", "M1,2 Q1,2,3", "M1,2 X", "M1,2,", "M NaN,1",
            "M1,2 L3,4,", "M1,2 Z,",
        ).forEach { assertNull("path <$it>", PathData.parse(it)) }
    }

    @Test fun everyShippedPathParses() {
        var count = 0
        for (p in PuzzleLibrary.packaged().puzzles) for (sh in p.picture.shapes) {
            if (sh is PictureShape.Path) {
                count++
                assertNotNull("${p.id.value}: ${sh.data}", PathData.parse(sh.data))
            }
        }
        assertTrue(count > 0)
    }

    @Test fun pictureAlpha() {
        assertEquals(0.0, SolvedTimeline.pictureAlpha(0), 0.0)
        assertEquals(0.0, SolvedTimeline.pictureAlpha(599), 0.0)
        assertEquals(0.0, SolvedTimeline.pictureAlpha(600), 0.0)
        assertEquals(0.5, SolvedTimeline.pictureAlpha(1000), 1e-12)
        assertEquals(1.0, SolvedTimeline.pictureAlpha(1400), 0.0)
        assertEquals(1.0, SolvedTimeline.pictureAlpha(5000), 0.0)
        assertFalse(SolvedTimeline.piecesHidden(1399))
        assertTrue(SolvedTimeline.piecesHidden(1400))
    }

    @Test fun reducedMotionGatesPopAndConfetti() {
        assertEquals(1.0, SolvedTimeline.popScale(150, true), 0.0)
        assertEquals(1.06, SolvedTimeline.popScale(150, false), 1e-12)
        assertEquals(1.0, SolvedTimeline.popScale(300, false), 0.0)
        assertTrue(SolvedTimeline.confetti(100, true).isEmpty())
        assertEquals(PlayTiming.CONFETTI_COUNT, SolvedTimeline.confetti(100, false).size)
        assertTrue(SolvedTimeline.confetti(2399, false).isNotEmpty())
        assertTrue(SolvedTimeline.confetti(2400, false).isEmpty())
        assertEquals(1.0, SolvedTimeline.pictureAlpha(1400), 0.0)
    }

    @Test fun confettiIsDeterministic() {
        assertEquals(Confetti.at(1234), Confetti.at(1234))
        assertTrue(Confetti.at(-1).isEmpty())
    }

    @Test fun timingConstants() {
        assertEquals(180L, PlayTiming.GLIDE_MS)
        assertEquals(120L, PlayTiming.GROW_MS)
        assertEquals(600L, PlayTiming.PULSE_MS)
        assertEquals(2400L, PlayTiming.CONFETTI_MS)
        assertEquals(400L, PlayTiming.SHAKE_MS)
    }
}
