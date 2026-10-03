package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.kernel.geometry.Vec2

/**
 * decision DA-74: the public piece-units to dp mapping handed to the `boardOverlay` slot. Equals [PlayLayout.toDp]
 * for the layout it was made from (dp relative to the origin of the play area).
 */
class BoardSpace(val dpPerUnit: Double, val originXDp: Double, val originYDp: Double) {
    fun toDp(point: Vec2): Vec2 = Vec2(originXDp + point.x * dpPerUnit, originYDp + point.y * dpPerUnit)
}
