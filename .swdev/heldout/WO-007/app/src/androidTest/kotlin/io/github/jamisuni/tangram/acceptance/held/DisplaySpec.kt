package io.github.jamisuni.tangram.acceptance.held

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import kotlin.math.abs

// Scaffolding (decision DA-96 / DA-108, design WO-006 section 1): the five exact-dp display specs and the rule that applies one.
// No requirement token. The values are the design's table and equal DISPLAY_SPECS of tools/device_reset.py (px = dp x density / 160).

enum class DisplaySpec(val widthDp: Int, val heightDp: Int, val densityDpi: Int, val widthPx: Int, val heightPx: Int) {
    PHONE_390x844(390, 844, 400, 975, 2110),
    PHONE_360x780(360, 780, 400, 900, 1950),
    TABLET_1280x800(1280, 800, 240, 1920, 1200),
    TABLET_800x1280(800, 1280, 240, 1200, 1920),
    TABLET_600x960(600, 960, 240, 900, 1440),
    ;

    /** What `Configuration.smallestScreenWidthDp` shows once the spec is applied at rotation 0. */
    val smallestWidthDp: Int get() = minOf(widthDp, heightDp)
}

/**
 * The REAL display size in px (never `resources.displayMetrics`, which is the app-usable area and excludes the navigation bar on
 * API 26): `Display.getRealMetrics` on API 26-29, `WindowManager.maximumWindowMetrics` on API 30+ (design section 1, step 4).
 */
internal object RealDisplay {
    fun sizePx(): Pair<Int, Int> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    }

    /** The `wm size` / `wm density` override: the text after "Override size:" or "Override density:", or null (native). */
    fun override(word: String): String? =
        DeviceShell.run("wm $word").lines().map { it.trim() }.firstOrNull { it.startsWith("Override $word:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()
}

/**
 * Applies a [DisplaySpec] for one test and puts the display back (decision DA-96). An `ExternalResource`, NOT a `TestWatcher`: a
 * `TestWatcher` swallows an exception thrown in `starting` and still runs the test (F2). `before` throws, so a setup failure fails
 * the test; `after` restores in `finally`.
 *
 * Order in [engage]: (1) the display must be native (`error("leaked display state ...")`), (2) save the prior rotation settings and
 * freeze rotation at 0, (3) `wm size` and `wm density`, output drained, (4) poll up to 10 s until `Resources.getSystem()` shows the
 * spec's smallest width and density, then until the REAL window equals the spec in px (tolerance 2 px, well under 0.5 dp).
 * With the instrumentation argument `displayPreset=true` (fallback, design section 1) the rule only CHECKS that the display already is
 * the spec and never calls `wm`; [ignorePreset] is for the harness's own scaffolding test, which must change displays itself.
 */
class DisplayRule(val spec: DisplaySpec, private val ignorePreset: Boolean = false) : ExternalResource() {
    private val rotation = Rotation()
    private var nativePx: Pair<Int, Int>? = null
    private var engaged = false

    private val preset: Boolean
        get() = !ignorePreset && InstrumentationRegistry.getArguments().getString("displayPreset") == "true"

    override fun before() = engage()

    override fun after() = release()

    fun engage() {
        if (engaged) error("DisplayRule($spec) engaged twice")
        if (!preset) {
            val size = RealDisplay.override("size")
            val dens = RealDisplay.override("density")
            if (size != null || dens != null) {
                error("leaked display state from an earlier run: wm size override=$size, density override=$dens (run tools/device_reset.py)")
            }
            nativePx = RealDisplay.sizePx()
        }
        engaged = true
        // ExternalResource never calls `after` when `before` throws: undo a half-applied override here (the failure still propagates).
        try {
            rotation.freeze0()
            if (!preset) {
                DeviceShell.run("wm size ${spec.widthPx}x${spec.heightPx}")
                DeviceShell.run("wm density ${spec.densityDpi}")
            }
            DeviceShell.waitUntil("Resources.getSystem() to show smallestScreenWidthDp ${spec.smallestWidthDp} and densityDpi ${spec.densityDpi}", 10_000) {
                val c = Resources.getSystem().configuration
                abs(c.smallestScreenWidthDp - spec.smallestWidthDp) <= 1 && c.densityDpi == spec.densityDpi
            }
            DeviceShell.waitUntil("the real window to be ${spec.widthPx}x${spec.heightPx}", 10_000) {
                val (w, h) = RealDisplay.sizePx()
                abs(w - spec.widthPx) <= 2 && abs(h - spec.heightPx) <= 2
            }
            val (w, _) = RealDisplay.sizePx()
            val dpW = w * 160.0 / spec.densityDpi
            if (abs(dpW - spec.widthDp) > 0.5) error("override not applied: wanted ${spec.widthDp} dp wide, got $dpW")
            if (!preset && Build.VERSION.SDK_INT < 30) DeviceShell.settleHome() // let the launcher settle after the display change (API 26 evidence)

        } catch (t: Throwable) {
            try { release() } catch (_: Throwable) { }
            throw t
        }
    }

    fun release() {
        if (!engaged) return
        engaged = false
        // each step runs even if the one before it throws (CR-5 N12)
        try {
            if (!preset) DeviceShell.run("wm size reset")
        } finally {
            try {
                if (!preset) DeviceShell.run("wm density reset")
            } finally {
                rotation.restore()
            }
        }
        if (!preset) {
            val native = nativePx ?: error("native display size was never recorded")
            DeviceShell.waitUntil("the native display ${native.first}x${native.second} to be observed again", 10_000) {
                val (w, h) = RealDisplay.sizePx()
                minOf(w, h) == minOf(native.first, native.second) && maxOf(w, h) == maxOf(native.first, native.second) &&
                    RealDisplay.override("size") == null && RealDisplay.override("density") == null
            }
            if (Build.VERSION.SDK_INT < 30) DeviceShell.settleHome()
        }
    }
}
