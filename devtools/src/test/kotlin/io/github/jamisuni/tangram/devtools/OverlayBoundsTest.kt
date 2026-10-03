package io.github.jamisuni.tangram.devtools

import io.github.jamisuni.tangram.kernel.geometry.Vec2
import org.junit.Assert.assertEquals
import org.junit.Test

// SCAFFOLDING (TASK-033): the pure part of the overlay, the bounding box behind each dev-solution-<id> node.
class OverlayBoundsTest {
    @Test
    fun boundingBoxOfATriangle() {
        val b = boundsOf(listOf(Vec2(10.0, 20.0), Vec2(50.0, 20.0), Vec2(10.0, 70.0)))
        assertEquals(BoundsDp(10.0, 20.0, 40.0, 50.0), b)
    }
}
