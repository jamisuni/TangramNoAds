package io.github.jamisuni.tangram.kernel.layout

/** TYPE-007: which layout the game uses. */
enum class LayoutClass { PHONE, TABLET }

/** TYPE-007 and decisions F3 (device class). */
object LayoutRules {
    const val TABLET_MIN_DP = 600

    /** Window width below 600 dp is PHONE, 600 dp and wider TABLET. A NaN width is PHONE. */
    fun classFor(windowWidthDp: Double): LayoutClass =
        if (windowWidthDp >= TABLET_MIN_DP) LayoutClass.TABLET else LayoutClass.PHONE

    fun classFor(windowWidthDp: Int): LayoutClass = classFor(windowWidthDp.toDouble())

    /** F3: a device whose smallest screen width is below 600 dp is portrait-locked. */
    fun lockPortrait(deviceSmallestWidthDp: Double): Boolean = deviceSmallestWidthDp < TABLET_MIN_DP

    fun lockPortrait(deviceSmallestWidthDp: Int): Boolean = lockPortrait(deviceSmallestWidthDp.toDouble())
}
