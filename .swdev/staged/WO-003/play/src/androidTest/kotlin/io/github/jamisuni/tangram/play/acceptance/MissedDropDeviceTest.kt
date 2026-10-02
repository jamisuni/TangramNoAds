package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** REQ-051 A2 on screen (touch-injected). */
class MissedDropDeviceTest {

    @get:Rule
    val rule = createComposeRule()

    // REQ-051.A2 - "In a warm-up, the same drop sends the piece home and nothing else is shown."
    // The accent colour of the corner marker is #3D8BFD (REQ-051 ASSUMPTION "a small circle in the accent colour", DA-20).
    // No such pixel may appear near any silhouette corner after the piece has gone home.
    @Test
    fun req051_A2_inAWarmupAMissedDropShowsNoCornerMarkers() {
        val p = puzzle("shapes-warmup-1")
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val l = layoutFor(p)
        val driver = TouchDriver(rule, session, l, p)
        val centre = driver.missDrop(PieceId.ST1)
        assertTrue("fixture: the drop was over the board", l.overBoard(centre))
        assertEquals("the piece went home", "Tray", session.where(PieceId.ST1))
        for (wait in listOf(100L, 300L, 700L)) {
            driver.advance(wait)
            val shot = rule.shot()
            for (c in silhouetteOf(p).outlineCorners) {
                val cd = l.toDp(v(c))
                var dy = -12.0
                while (dy <= 12.0) {
                    var dx = -12.0
                    while (dx <= 12.0) {
                        val px = shot.px(Vec2(cd.x + dx, cd.y + dy))
                        assertTrue("accent-coloured pixel near corner $c ${wait}ms after the miss", diff(px, rgb(0x3D8BFD)) > 30)
                        dx += 2.0
                    }
                    dy += 2.0
                }
            }
        }
    }
}
