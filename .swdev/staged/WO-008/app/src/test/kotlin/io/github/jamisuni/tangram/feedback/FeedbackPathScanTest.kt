package io.github.jamisuni.tangram.feedback

import io.github.jamisuni.tangram.promise.RepoScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// decision DA-116 / DA-123 (design WO-007 section 3.2, review F2, plan N8 / spot-check E1, E5): it pins THE GATE, not G-01. The one gate
// `Feedback.on` must be the only path in our code to a sound or a haptic, so this scan works on CALL PATHS, not on names alone. No
// acceptance token (the on-screen REQ-033 A1 evidence is the held device tests).
//
// Module `app`; trees `*/src/main/**`, `*/src/release/**`, `*/src/debug/**` (never build/, .gradle/, .swdev/, Study/, Requirements/;
// test trees are not scanned). Inputs: the globs that app/build.gradle.kts declares as `scannedFileGlobs` (DA-88 pattern: globs only, no
// module names, no word "devtools", DA-89), read through RepoScan, so the test and the build's declared inputs cannot drift apart;
// `repo.root` comes from the build. Matching is on identifier tokens, comments skipped. The four rules:
//   1. FORBIDDEN EVERYWHERE: Vibrator, VibratorManager, VibrationEffect, VIBRATE, MediaPlayer, ToneGenerator, Ringtone, RingtoneManager,
//      MediaActionSound, playSoundEffect, SoundEffectConstants (consistent with PromiseSourceScanTest, which gains Vibrator, VibratorManager, VIBRATE).
//   2. ALLOWED ONLY in settings/src/main/**/AudioTrackSoundOut.kt and ViewHapticOut.kt: AudioTrack, AudioAttributes, AudioFormat,
//      performHapticFeedback, HapticFeedbackConstants.
//   3. ALLOWED ONLY in app/src/main/**/PlatformFeedbackLever.kt: LocalSoundEffect, LocalHapticFeedback, SoundEffect, HapticFeedback (whole
//      identifiers), isInteractionSoundEffectsEnabled, isInteractionSoundEffectOnClickEnabled, hapticFeedbackEnabled, isSoundEffectsEnabled,
//      isHapticFeedbackEnabled.
//   4. CALL RULE: the calls `.play(` and `.tick(` and the references `::play` and `::tick` (so `haptics::tick` passed as a callback cannot route
//      around the gate) appear only in settings/.../Feedback.kt, app/src/debug/.../FeedbackProbe.kt (it delegates to the real out) and the two out files.
// Plus the lever's precondition (spot-check E1, plan N8): no `Dialog(` / `Popup(` (and the other window-creating composables) in the
// src/main of the release-shipping PRODUCT modules (kernel, contracts, content, store, play, browse, settings, app). NEVER devtools: its
// debug-only passcode Dialog is DA-125's accepted residue (V-04 proves devtools never ships).
// The scan FAILS BLIND: it asserts its canaries, else error().

internal object FeedbackScan {
    val FORBIDDEN_EVERYWHERE = listOf(
        "Vibrator", "VibratorManager", "VibrationEffect", "VIBRATE", "MediaPlayer", "ToneGenerator", "Ringtone", "RingtoneManager",
        "MediaActionSound", "playSoundEffect", "SoundEffectConstants",
    )
    val OUT_ONLY = listOf("AudioTrack", "AudioAttributes", "AudioFormat", "performHapticFeedback", "HapticFeedbackConstants")
    val LEVER_ONLY = listOf(
        "LocalSoundEffect", "LocalHapticFeedback", "SoundEffect", "HapticFeedback", "isInteractionSoundEffectsEnabled",
        "isInteractionSoundEffectOnClickEnabled", "hapticFeedbackEnabled", "isSoundEffectsEnabled", "isHapticFeedbackEnabled",
    )
    val WINDOW_COMPOSABLES = listOf("Dialog", "Popup", "AlertDialog", "ModalBottomSheet", "DropdownMenu")
    // WO-008 (design 8 inventory): `time` ships in release, so rule 5 and the canaries cover it; the keeper file must be seen (canary below).
    val PRODUCT_MODULES = listOf("kernel", "contracts", "content", "store", "play", "browse", "settings", "time", "app")

    private val outFile = Regex("^settings/src/main/(?:.+/)?(?:AudioTrackSoundOut|ViewHapticOut)\\.kt$")
    private val leverFile = Regex("^app/src/main/(?:.+/)?PlatformFeedbackLever\\.kt$")
    private val gateFile = Regex("^settings/src/main/(?:.+/)?Feedback\\.kt$")
    private val probeFile = Regex("^app/src/debug/(?:.+/)?FeedbackProbe\\.kt$")
    private val scannedTree = Regex("^[^/]+/src/(?:main|release|debug)/.+\\.(?:kt|java)$")
    private val productMain = Regex("^(?:" + PRODUCT_MODULES.joinToString("|") + ")/src/main/.+\\.(?:kt|java)$")

    private val callRef = Regex("\\.\\s*(?:play|tick)\\s*\\(|::\\s*(?:play|tick)(?![A-Za-z0-9_])")

    // CR-6 N2: a BARE call inside a scope function (`with(sound) { play(e) }`, `sound.run { play(e) }`) names no receiver, so the dot rule cannot
    // see it. Declarations (`override fun play(`) are not calls and are blanked first.
    private val playTickDeclaration = Regex("fun\\s+(?:play|tick)\\s*\\(")
    private val bareCall = Regex("(?<![A-Za-z0-9_.:])(?:play|tick)\\s*\\(")

    // CR-6 N2: the gate is built once, in AppViewModel (a second `Feedback(` could be given another switch); its declaration is no construction.
    private val feedbackDeclaration = Regex("class\\s+Feedback\\s*\\(")
    private val feedbackConstruction = Regex("(?<![A-Za-z0-9_.])Feedback\\s*\\(")
    private val modelFile = Regex("^app/src/main/(?:.+/)?AppViewModel\\.kt$")

    fun isScanned(path: String) = scannedTree.matches(path)
    fun isOutFile(path: String) = outFile.matches(path)
    fun isLeverFile(path: String) = leverFile.matches(path)
    fun isGateFile(path: String) = gateFile.matches(path)
    fun isProbeFile(path: String) = probeFile.matches(path)

    fun token(name: String): Regex = Regex("(?<![A-Za-z0-9_])" + Regex.escape(name) + "(?![A-Za-z0-9_])")

    class Violation(val file: String, val rule: Int, val what: String) {
        override fun toString() = "$file [rule $rule] $what"
    }

    /** Kotlin source with comments blanked (newlines kept); string literals are kept, so a name in a string is a hit. */
    fun stripComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        var depth = 0
        var inLine = false
        var inString = false
        var inRaw = false
        var inChar = false
        while (i < src.length) {
            val c = src[i]
            val n = if (i + 1 < src.length) src[i + 1] else '\u0000'
            when {
                inLine -> { if (c == '\n') { inLine = false; out.append(c) } else out.append(' '); i++ }
                depth > 0 -> when {
                    c == '/' && n == '*' -> { depth++; out.append("  "); i += 2 }
                    c == '*' && n == '/' -> { depth--; out.append("  "); i += 2 }
                    else -> { out.append(if (c == '\n') '\n' else ' '); i++ }
                }
                inRaw -> { if (src.startsWith("\"\"\"", i)) { inRaw = false; out.append("\"\"\""); i += 3 } else { out.append(c); i++ } }
                inString -> when {
                    c == '\\' -> { out.append(c); if (i + 1 < src.length) out.append(src[i + 1]); i += 2 }
                    c == '"' -> { inString = false; out.append(c); i++ }
                    else -> { out.append(c); i++ }
                }
                inChar -> when {
                    c == '\\' -> { out.append(c); if (i + 1 < src.length) out.append(src[i + 1]); i += 2 }
                    c == '\'' -> { inChar = false; out.append(c); i++ }
                    else -> { out.append(c); i++ }
                }
                src.startsWith("\"\"\"", i) -> { inRaw = true; out.append("\"\"\""); i += 3 }
                c == '"' -> { inString = true; out.append(c); i++ }
                c == '\'' -> { inChar = true; out.append(c); i++ }
                c == '/' && n == '/' -> { inLine = true; out.append("  "); i += 2 }
                c == '/' && n == '*' -> { depth = 1; out.append("  "); i += 2 }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    /**
     * CR-6 N8 (decision DA-125): `PlatformFeedbackLever.installProcessFlags()` comes BEFORE `setContent` in the source (comments skipped), both present.
     * Compose reads the process flag when a window's compose context is built, so a later install would silence nothing.
     */
    fun installsBeforeSetContent(src: String): Boolean {
        val code = stripComments(src)
        val install = Regex("PlatformFeedbackLever\\s*\\.\\s*installProcessFlags\\s*\\(\\s*\\)").find(code) ?: return false
        val set = Regex("(?<![A-Za-z0-9_])setContent\\s*[({]").find(code) ?: return false
        return install.range.first < set.range.first
    }

    /** All four rules and the window deny over [files] (relative path with forward slashes -> source). Files outside the scanned trees are ignored. */
    fun scan(files: Map<String, String>): List<Violation> {
        val out = ArrayList<Violation>()
        for ((path, src) in files) {
            if (!isScanned(path)) continue
            val code = stripComments(src)
            for (n in FORBIDDEN_EVERYWHERE) if (token(n).containsMatchIn(code)) out += Violation(path, 1, n)
            // The lever's silent HapticFeedback must DECLARE `override fun performHapticFeedback(...)` (it implements the Compose interface, rule 3's
            // `HapticFeedback`); a declaration is not a call, so in the lever file only that declaration is allowed (found at the first compile-check:
            // the design's rule 2 and rule 3 cannot both hold otherwise). A call or reference there is still a hit.
            val rule2Code = if (isLeverFile(path)) code.replace(Regex("fun\\s+performHapticFeedback\\b"), "fun _") else code
            if (!isOutFile(path)) for (n in OUT_ONLY) if (token(n).containsMatchIn(rule2Code)) out += Violation(path, 2, n)
            if (!isLeverFile(path)) for (n in LEVER_ONLY) if (token(n).containsMatchIn(code)) out += Violation(path, 3, n)
            if (!(isGateFile(path) || isProbeFile(path) || isOutFile(path))) {
                for (m in callRef.findAll(code)) out += Violation(path, 4, m.value.replace(Regex("\\s+"), ""))
            }
            if (!(isGateFile(path) || isProbeFile(path) || isOutFile(path))) {
                for (m in bareCall.findAll(playTickDeclaration.replace(code, "fun _("))) out += Violation(path, 4, "bare " + m.value.replace(Regex("\\s+"), ""))
            }
            if (!isGateFile(path) && !modelFile.matches(path)) {
                for (m in feedbackConstruction.findAll(feedbackDeclaration.replace(code, "class F_("))) out += Violation(path, 6, "Feedback(")
            }
            if (productMain.matches(path)) {
                for (n in WINDOW_COMPOSABLES) {
                    if (Regex("(?<![A-Za-z0-9_])" + n + "\\s*\\(").containsMatchIn(code)) out += Violation(path, 5, "$n(")
                }
            }
        }
        return out
    }
}

class FeedbackPathScanTest {
    private val root: File = RepoScan.repoRoot()
    private val scanned = RepoScan.scannedFiles(root)
    private val files: Map<String, String> by lazy {
        scanned.files.filter { FeedbackScan.isScanned(it) }.associateWith { File(root, it).readText() }
    }
    private val violations by lazy { FeedbackScan.scan(files) }

    private fun rule(n: Int) = violations.filter { it.rule == n }

    // ---- the four rules and the window deny, on the real tree ----------------------------------------------------------------------

    // rule 1: forbidden everywhere (every scanned tree, the outs and the lever included).
    @Test
    fun decisionDA116_noVibrationOrOtherSoundApiAnywhere() {
        assertTrue("rule 1 (forbidden everywhere): ${rule(1)}", rule(1).isEmpty())
    }

    // rule 2: the audio and haptic output APIs live in the two out files only.
    @Test
    fun decisionDA116_audioTrackAndPerformHapticFeedbackLiveOnlyInTheTwoOutFiles() {
        assertTrue("rule 2 (allowed only in AudioTrackSoundOut.kt / ViewHapticOut.kt): ${rule(2)}", rule(2).isEmpty())
    }

    // rule 3: the platform-default lever names live in PlatformFeedbackLever.kt only.
    @Test
    fun decisionDA116_thePlatformLeverNamesLiveOnlyInTheLeverFile() {
        assertTrue("rule 3 (allowed only in PlatformFeedbackLever.kt): ${rule(3)}", rule(3).isEmpty())
    }

    // rule 4: the gate is the only caller of the outs: `.play(` / `.tick(` / `::play` / `::tick` only in the four named files.
    @Test
    fun decisionDA116_onlyTheGateTheProbeAndTheOutsCallOrReferencePlayAndTick() {
        assertTrue("rule 4 (play / tick calls or references outside Feedback.kt, FeedbackProbe.kt and the two outs): ${rule(4)}", rule(4).isEmpty())
    }

    // the lever's precondition (spot-check E1, plan N8): no window-creating composable in what ships (devtools excluded on purpose).
    @Test
    fun decisionDA116_noDialogOrPopupInTheReleaseShippingProductModules() {
        assertTrue("Dialog / Popup in the release-shipping product modules' src/main (lever precondition, DA-125): ${rule(5)}", rule(5).isEmpty())
    }

    // CR-6 N2: the gate is constructed once (in AppViewModel), never a second time with another switch.
    @Test
    fun decisionDA116_theGateIsConstructedOnlyInAppViewModel() {
        assertTrue("a second `Feedback(` construction (rule 6): ${rule(6)}", rule(6).isEmpty())
        val holders = files.filter { (path, src) -> path.matches(Regex("^app/src/main/(?:.+/)?AppViewModel\\.kt$")) && Regex("(?<![A-Za-z0-9_.])Feedback\\s*\\(").containsMatchIn(FeedbackScan.stripComments(src)) }
        assertEquals("AppViewModel must hold the one construction (else the check is blind)", 1, holders.size)
    }

    // CR-6 N8 / decision DA-125: the process flag is installed before setContent in MainActivity (source order).
    @Test
    fun decisionDA125_mainActivityInstallsTheProcessFlagsBeforeSetContent() {
        val hits = files.filter { it.key.matches(Regex("^app/src/main/(?:.+/)?MainActivity\\.kt$")) }
        assertEquals("expected exactly one MainActivity.kt under app/src/main: ${hits.keys}", 1, hits.size)
        assertTrue("MainActivity must call PlatformFeedbackLever.installProcessFlags() before setContent", FeedbackScan.installsBeforeSetContent(hits.values.single()))
    }

    @Test
    fun decisionDA125_theOrderCheckCanFail() {
        assertTrue(FeedbackScan.installsBeforeSetContent("override fun onCreate(b: Bundle?) {\n PlatformFeedbackLever.installProcessFlags()\n super.onCreate(b)\n setContent { X() }\n}"))
        assertFalse("reversed", FeedbackScan.installsBeforeSetContent("override fun onCreate(b: Bundle?) {\n setContent { X() }\n PlatformFeedbackLever.installProcessFlags()\n}"))
        assertFalse("no install", FeedbackScan.installsBeforeSetContent("override fun onCreate(b: Bundle?) {\n setContent { X() }\n}"))
        assertFalse("no setContent", FeedbackScan.installsBeforeSetContent("PlatformFeedbackLever.installProcessFlags()"))
        assertFalse("a comment is not the call", FeedbackScan.installsBeforeSetContent("// PlatformFeedbackLever.installProcessFlags()\nsetContent { X() }\nPlatformFeedbackLever.installProcessFlags()"))
    }

    // ---- fail blind -----------------------------------------------------------------------------------------------------------------

    // the build declares the three trees as test inputs (a changed file in app/src/debug must re-run this test, DA-88), by globs only.
    @Test
    fun decisionDA116_theBuildDeclaresTheThreeTreesAsInputsByGlobsOnly() {
        val globs = RepoScan.declaredGlobs(root)
        for (g in listOf("*/src/main/**", "*/src/release/**", "*/src/debug/**")) assertTrue("scannedFileGlobs lacks \"$g\": $globs", g in globs)
        assertTrue("a declared glob names devtools (DA-89 / G-04): $globs", globs.none { it.contains("devtools", ignoreCase = true) })
    }

    // canaries: the scan must see the files it protects, else it proved nothing.
    @Test
    fun decisionDA116_theScanSeesTheFilesItProtects() {
        for (m in FeedbackScan.PRODUCT_MODULES) {
            assertTrue("no scanned source under $m/src/main: the scan is blind to a product module", files.keys.any { it.startsWith("$m/src/main/") })
        }
        // WO-008: the `time` module is scanned for real, not just present: its keeper source is among the scanned files, and `time` holds no window composable
        // (the lever's precondition, rule 5) - the second half is rule 5 itself running over `time/src/main`.
        val keeper = files.keys.filter { it.matches(Regex("^time/src/main/(?:.+/)?PlayTimeKeeper\\.kt$")) }
        assertEquals("expected exactly one time/src/main PlayTimeKeeper.kt among the scanned files: the `time` scan is blind without it: $keeper", 1, keeper.size)
        assertTrue("no scanned source under app/src/debug", files.keys.any { it.startsWith("app/src/debug/") })
        assertTrue("no scanned source under app/src/release", files.keys.any { it.startsWith("app/src/release/") })

        fun code(match: (String) -> Boolean, what: String): String {
            val hits = files.keys.filter(match)
            if (hits.size != 1) error("expected exactly one $what among the scanned files, found $hits")
            return FeedbackScan.stripComments(files.getValue(hits.single()))
        }
        val gate = code(FeedbackScan::isGateFile, "settings Feedback.kt")
        assertTrue("Feedback.kt holds no `.play(`: the call rule is blind", Regex("\\.\\s*play\\s*\\(").containsMatchIn(gate))
        assertTrue("Feedback.kt holds no `.tick(`: the call rule is blind", Regex("\\.\\s*tick\\s*\\(").containsMatchIn(gate))
        val lever = code(FeedbackScan::isLeverFile, "app PlatformFeedbackLever.kt")
        assertTrue("PlatformFeedbackLever.kt names no LocalSoundEffect: rule 3 is blind", FeedbackScan.token("LocalSoundEffect").containsMatchIn(lever))
        val probe = code(FeedbackScan::isProbeFile, "app debug FeedbackProbe.kt")
        assertTrue("FeedbackProbe.kt calls no `.play(`: it must delegate to the real out", Regex("\\.\\s*play\\s*\\(").containsMatchIn(probe))
        assertTrue("FeedbackProbe.kt calls no `.tick(`: it must delegate to the real out", Regex("\\.\\s*tick\\s*\\(").containsMatchIn(probe))
        val sound = code({ it.matches(Regex("^settings/src/main/(?:.+/)?AudioTrackSoundOut\\.kt$")) }, "AudioTrackSoundOut.kt")
        assertTrue("AudioTrackSoundOut.kt names no AudioTrack: rule 2 is blind", FeedbackScan.token("AudioTrack").containsMatchIn(sound))
        val haptic = code({ it.matches(Regex("^settings/src/main/(?:.+/)?ViewHapticOut\\.kt$")) }, "ViewHapticOut.kt")
        assertTrue("ViewHapticOut.kt names no performHapticFeedback: rule 2 is blind", FeedbackScan.token("performHapticFeedback").containsMatchIn(haptic))
    }

    // ---- controls: every rule can fail, and nothing else does ---------------------------------------------------------------------

    private fun hits(path: String, src: String, rule: Int? = null): List<FeedbackScan.Violation> =
        FeedbackScan.scan(mapOf(path to src)).filter { rule == null || it.rule == rule }

    @Test
    fun decisionDA116_rule1CanFail() {
        assertFalse(hits("play/src/main/kotlin/X.kt", "val v = ctx.getSystemService(Vibrator::class.java)", 1).isEmpty())
        assertFalse(hits("browse/src/debug/kotlin/X.kt", "val e = VibrationEffect.createOneShot(1, 1)", 1).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "const val P = \"android.permission.VIBRATE\"", 1).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "view.playSoundEffect(0)", 1).isEmpty())
        assertFalse(hits("app/src/release/kotlin/X.kt", "SoundEffectConstants.CLICK", 1).isEmpty())
        assertFalse("forbidden even in the out files", hits("settings/src/main/kotlin/AudioTrackSoundOut.kt", "ToneGenerator(1, 1)", 1).isEmpty())
        assertTrue("a comment is not code", hits("play/src/main/kotlin/X.kt", "// Vibrator is not used here\nval x = 1", 1).isEmpty())
        assertTrue("another identifier", hits("play/src/main/kotlin/X.kt", "val RingtoneManagerX = 1", 1).isEmpty())
        assertTrue("outside the trees nothing is scanned", hits("app/src/test/kotlin/X.kt", "Vibrator", null).isEmpty())
    }

    @Test
    fun decisionDA116_rule2CanFail() {
        assertFalse(hits("app/src/main/kotlin/X.kt", "val t = AudioTrack.Builder()", 2).isEmpty())
        assertFalse(hits("settings/src/main/kotlin/Other.kt", "val a = AudioAttributes.Builder()", 2).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)", 2).isEmpty())
        assertTrue(hits("settings/src/main/kotlin/io/x/AudioTrackSoundOut.kt", "val t = AudioTrack.Builder(); val f = AudioFormat.Builder(); val a = AudioAttributes.Builder()", 2).isEmpty())
        assertTrue(hits("settings/src/main/kotlin/io/x/ViewHapticOut.kt", "view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)", 2).isEmpty())
        assertTrue("the lever may declare the override", hits("app/src/main/kotlin/io/x/PlatformFeedbackLever.kt", "override fun performHapticFeedback(t: HapticFeedbackType) {}", 2).isEmpty())
        assertFalse("but not call it", hits("app/src/main/kotlin/io/x/PlatformFeedbackLever.kt", "view.performHapticFeedback(1)", 2).isEmpty())
        assertFalse("and no other file may declare it", hits("app/src/main/kotlin/io/x/Other.kt", "override fun performHapticFeedback(t: HapticFeedbackType) {}", 2).isEmpty())
        assertTrue("AudioTrackSoundOut is another identifier than AudioTrack", hits("app/src/main/kotlin/X.kt", "val o = AudioTrackSoundOut()", 2).isEmpty())
    }

    @Test
    fun decisionDA116_rule3CanFail() {
        assertFalse(hits("app/src/main/kotlin/io/x/MainActivity.kt", "CompositionLocalProvider(LocalSoundEffect provides s) {}", 3).isEmpty())
        assertFalse(hits("browse/src/main/kotlin/X.kt", "val h = LocalHapticFeedback.current", 3).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "object S : SoundEffect { }", 3).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "val h: HapticFeedback = x", 3).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "view.isHapticFeedbackEnabled = false", 3).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "AndroidComposeUiFlags.isInteractionSoundEffectsEnabled = false", 3).isEmpty())
        assertFalse(hits("settings/src/main/kotlin/io/x/PlatformFeedbackLever.kt", "LocalSoundEffect", 3).isEmpty()) // a lever file in the wrong module
        assertTrue(hits("app/src/main/kotlin/io/x/PlatformFeedbackLever.kt", "LocalSoundEffect LocalHapticFeedback SoundEffect HapticFeedback isInteractionSoundEffectsEnabled", 3).isEmpty())
        assertTrue("SoundOut is not SoundEffect", hits("app/src/main/kotlin/X.kt", "val s: SoundOut = x", 3).isEmpty())
        assertTrue("HapticFeedbackConstants is rule 2's name, not the whole identifier HapticFeedback", hits("settings/src/main/kotlin/io/x/ViewHapticOut.kt", "HapticFeedbackConstants.CLOCK_TICK", 3).isEmpty())
    }

    @Test
    fun decisionDA116_rule4CanFail() {
        assertFalse(hits("app/src/main/kotlin/AppViewModel.kt", "model.haptics.tick()", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/AppViewModel.kt", "sound.play(e)", 4).isEmpty())
        assertFalse("a function reference routes around the gate too", hits("app/src/main/kotlin/AppViewModel.kt", "val cb = haptics::tick", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/AppViewModel.kt", "val cb = sound::play", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/AppViewModel.kt", "val cb = haptics :: tick", 4).isEmpty())
        assertFalse(hits("play/src/main/kotlin/X.kt", "out . play (e)", 4).isEmpty())
        assertFalse("the debug tree is scanned: a second caller there", hits("app/src/debug/kotlin/io/x/Other.kt", "real.play(e)", 4).isEmpty())
        assertTrue(hits("settings/src/main/kotlin/io/x/Feedback.kt", "sound.play(e); haptic.tick()", 4).isEmpty())
        assertTrue(hits("app/src/debug/kotlin/io/x/FeedbackProbe.kt", "real.play(e); real.tick()", 4).isEmpty())
        assertTrue(hits("settings/src/main/kotlin/io/x/AudioTrackSoundOut.kt", "track.play()", 4).isEmpty())
        assertTrue(hits("settings/src/main/kotlin/io/x/ViewHapticOut.kt", "val x = this::tick", 4).isEmpty())
        assertTrue("the gate call is fine", hits("app/src/main/kotlin/X.kt", "feedback.on(FeedbackEvent.LOCK)", 4).isEmpty())
        assertTrue("a declaration is not a call", hits("app/src/main/kotlin/X.kt", "override fun tick() {}\noverride fun play(e: FeedbackEvent) {}", 4).isEmpty())
        assertTrue("a comment is not a call", hits("app/src/main/kotlin/X.kt", "// sound.play(e) and haptics::tick\nval x = 1", 4).isEmpty())
        assertTrue("other names", hits("app/src/main/kotlin/X.kt", "player.playing(); x.ticker(); a::ticks", 4).isEmpty())
    }

    // CR-6 N2: bare calls inside scope functions, and a second construction of the gate.
    @Test
    fun decisionDA116_bareScopeFunctionCallsAndASecondGateConstructionAreHits() {
        assertFalse(hits("app/src/main/kotlin/X.kt", "with(sound) { play(e) }", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "sound.run { play(e) }", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "haptics.apply { tick() }", 4).isEmpty())
        assertFalse(hits("app/src/main/kotlin/X.kt", "with (h) {\n  tick ()\n}", 4).isEmpty())
        assertTrue("allowed in the gate", hits("settings/src/main/kotlin/io/x/Feedback.kt", "with(sound) { play(e) }", 4).isEmpty())
        assertTrue("allowed in an out file", hits("settings/src/main/kotlin/io/x/ViewHapticOut.kt", "run { tick() }", 4).isEmpty())
        assertTrue("a declaration is not a call", hits("app/src/main/kotlin/X.kt", "override fun play(e: FeedbackEvent) {}\noverride fun tick() {}", 4).isEmpty())
        assertTrue("another identifier", hits("app/src/main/kotlin/X.kt", "display(x); sticky(y); xtick(); playing()", 4).isEmpty())

        assertFalse(hits("app/src/main/kotlin/io/x/MainActivity.kt", "val f = Feedback({ true }, s, h)", 6).isEmpty())
        assertFalse(hits("browse/src/main/kotlin/X.kt", "val f = Feedback(soundOn = { true }, sound = s, haptic = h)", 6).isEmpty())
        assertTrue("the one construction", hits("app/src/main/kotlin/io/x/AppViewModel.kt", "val feedback = Feedback(\n soundOn = { true }, sound = s, haptic = h)", 6).isEmpty())
        assertTrue("the declaration", hits("settings/src/main/kotlin/io/x/Feedback.kt", "class Feedback(private val soundOn: () -> Boolean)", 6).isEmpty())
        assertTrue("another class", hits("app/src/main/kotlin/X.kt", "val p = FeedbackProbe(); val e = FeedbackEvent.LOCK; x.Feedback(1)", 6).isEmpty())
    }

    @Test
    fun decisionDA116_theWindowDenyCanFailAndExcludesDevtools() {
        assertFalse(hits("app/src/main/kotlin/X.kt", "Dialog(onDismissRequest = {}) { }", 5).isEmpty())
        assertFalse(hits("settings/src/main/kotlin/X.kt", "Popup(alignment = a) { }", 5).isEmpty())
        assertFalse(hits("browse/src/main/kotlin/X.kt", "AlertDialog(onDismissRequest = {})", 5).isEmpty())
        assertFalse(hits("play/src/main/kotlin/X.kt", "ModalBottomSheet(onDismissRequest = {}) { }", 5).isEmpty())
        assertFalse(hits("kernel/src/main/kotlin/X.kt", "DropdownMenu(expanded = e) { }", 5).isEmpty())
        for (m in FeedbackScan.PRODUCT_MODULES) assertFalse("$m", hits("$m/src/main/kotlin/X.kt", "Dialog(x)", 5).isEmpty())
        assertTrue("devtools is the accepted residue (DA-125)", hits("devtools/src/main/kotlin/DevCornerButton.kt", "Dialog(onDismissRequest = {}) { }", 5).isEmpty())
        assertTrue("the debug tree of a product module is not release code", hits("app/src/debug/kotlin/X.kt", "Dialog(x)", 5).isEmpty())
        assertTrue("MyDialog is another identifier", hits("app/src/main/kotlin/X.kt", "MyDialog(x); showDialog(); val dialog = 1", 5).isEmpty())
        assertTrue("a comment is not code", hits("app/src/main/kotlin/X.kt", "// Dialog(x)\nval x = 1", 5).isEmpty())
    }

    @Test
    fun decisionDA116_aNameInAStringLiteralIsStillAHitButAnInnerCommentMarkerDoesNotHideCode() {
        assertFalse(hits("app/src/main/kotlin/X.kt", "val s = \"Vibrator\"", 1).isEmpty())
        assertFalse("a // inside a string is not a comment", hits("app/src/main/kotlin/X.kt", "val u = \"a//b\"; val v: Vibrator? = null", 1).isEmpty())
        assertEquals(1, hits("app/src/main/kotlin/X.kt", "val a = 1 /* x */ ; val v: Vibrator? = null", 1).size)
    }
}
