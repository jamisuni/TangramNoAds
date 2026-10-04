package io.github.jamisuni.tangram.promise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// guardrail G-01: SCAFFOLDING / GUARDRAIL TEST (WO-006 T6c). It asserts the RULE "no ad, billing, review, analytics or network
// component, no store link, no such text exists in our sources" (G-01: "No dependency does networking, ads, billing, analytics or
// crash reporting"; REQ-008 and REQ-001 as read by DA-106). It carries no requirement token on purpose: it proves "no such component
// in our code or declared dependencies, no such text exists", not that something appears on a screen (design WO-006 section 6 item 1).
// Inputs: exactly the glob set app/build.gradle.kts declares as `scannedFileGlobs` (RepoScan), from `repo.root`; a missing property
// or an empty glob fails loudly. Not seen here: transitive dependencies (V-08 sees them in the release APK).
class PromiseSourceScanTest {
    private val root: File = RepoScan.repoRoot()
    private val scanned = RepoScan.scannedFiles(root)

    // ---- the deny lists -------------------------------------------------------------------------------------------------------

    /** Ad, billing, in-app review, analytics, crash and network SDK package roots (design section 6 items 1(a) and 2(a)). */
    private val sdkDeny = listOf(
        "com.google.android.gms", "com.google.firebase", "com.android.billingclient", "com.google.android.play", "okhttp3",
        "retrofit2", "io.ktor", "com.applovin", "com.unity3d.ads", "com.ironsource", "com.facebook", "com.adjust",
        "com.appsflyer", "io.sentry",
    )

    /** Framework network and web types. Denied in OUR sources only (in the APK they come in through androidx, V-08 leaves them). */
    private val frameworkDeny = listOf(
        "java.net.", "javax.net.", "android.webkit", "HttpURLConnection", "WebView", "ReviewManager",
        // WO-007 (design WO-007 section 3.2 rule 1, plan T7c): the tick is View.performHapticFeedback, never Vibrator, which needs the
        // VIBRATE permission (REQ-033 rule "without a permission", REQ-010 A2 as read by V-08).
        "Vibrator", "VibratorManager", "VIBRATE",
    )

    /** Permission names that must not appear in a manifest (the same WO-007 addition: VIBRATE is a permission, so the manifests are read too). */
    private val permissionDeny = listOf("VIBRATE")

    private val urlLiteral = Regex("(?i)(https?://|market://|play\\.google\\.com)")

    // ---- G-02 allowlist = architecture section 5 -----------------------------------------------------------------------------
    // "AndroidX core, activity, lifecycle; Compose (ui, foundation; material3 ...); kotlinx-serialization-json; kotlinx-coroutines.
    //  Tests only: JUnit 4, kotlin-test, Compose UI test, AndroidX test." Anything else needs a decisions.md row first.

    private fun allowedCoordinate(group: String, name: String): Boolean = when {
        group == "androidx.ads" -> false // the one AndroidX group that is an ad identifier
        group.startsWith("androidx.") -> true // AndroidX incl. Compose, activity, startup and the AndroidX test libraries
        group == "org.jetbrains.kotlinx" && (name == "kotlinx-serialization-json" || name.startsWith("kotlinx-coroutines")) -> true
        group == "org.jetbrains.kotlin" && name.startsWith("kotlin-test") -> true
        group == "junit" && name == "junit" -> true
        else -> false
    }

    // ---- file text helpers ----------------------------------------------------------------------------------------------------

    private fun text(rel: String) = File(root, rel).readText()

    /** Kotlin / Java source with comments blanked (newlines kept so line numbers stay true); string literals are kept. */
    internal fun stripComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        var blockDepth = 0
        var inLine = false
        var inString = false
        var inRaw = false
        var inChar = false
        while (i < src.length) {
            val c = src[i]
            val n = if (i + 1 < src.length) src[i + 1] else '\u0000'
            when {
                inLine -> { if (c == '\n') { inLine = false; out.append(c) } else out.append(' '); i++ }
                blockDepth > 0 -> when {
                    c == '/' && n == '*' -> { blockDepth++; out.append("  "); i += 2 }
                    c == '*' && n == '/' -> { blockDepth--; out.append("  "); i += 2 }
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
                c == '/' && n == '*' -> { blockDepth = 1; out.append("  "); i += 2 }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    private fun lineOf(s: String, idx: Int) = s.substring(0, idx).count { it == '\n' } + 1

    private val codeFiles get() = scanned.files.filter { it.endsWith(".kt") || it.endsWith(".java") }

    // ---- (a) no SDK import or identifier, no framework network type in our code ----------------------------------------------

    @Test fun noAdBillingReviewAnalyticsOrNetworkSdkInOurSources() {
        val hits = ArrayList<String>()
        for (rel in codeFiles) {
            val code = stripComments(text(rel))
            for (t in sdkDeny + frameworkDeny) {
                var from = 0
                while (true) {
                    val idx = code.indexOf(t, from)
                    if (idx < 0) break
                    hits += "$rel:${lineOf(code, idx)} uses `$t`"
                    from = idx + t.length
                }
            }
        }
        assertTrue("forbidden SDK or network identifiers in our sources: $hits", hits.isEmpty())
    }

    @Test fun noSdkPackageInBuildFilesCatalogOrManifests() {
        val hits = ArrayList<String>()
        val files = scanned.files.filter { it.endsWith(".kts") || it.endsWith(".toml") || it.endsWith("AndroidManifest.xml") }
        for (rel in files) {
            val body = text(rel)
            for (t in sdkDeny) if (body.contains(t)) hits += "$rel mentions `$t`"
            if (rel.endsWith("AndroidManifest.xml")) for (t in permissionDeny) if (body.contains(t)) hits += "$rel mentions `$t`"
        }
        assertTrue("SDK package names in build files, the version catalog or manifests: $hits", hits.isEmpty())
    }

    // ---- (b) no store or web link in any Kotlin source ------------------------------------------------------------------------

    @Test fun noHttpHttpsMarketOrPlayStoreLiteralInKotlinSources() {
        val hits = ArrayList<String>()
        for (rel in codeFiles) {
            val code = stripComments(text(rel))
            for (m in urlLiteral.findAll(code)) hits += "$rel:${lineOf(code, m.range.first)} `${m.value}`"
        }
        assertTrue("web or store links in Kotlin sources: $hits", hits.isEmpty())
    }

    // ---- (c) every library coordinate is on the G-02 allowlist ---------------------------------------------------------------

    private fun catalogCoordinates(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val body = text("gradle/libs.versions.toml")
        val libs = body.substringAfter("[libraries]", "").substringBefore("\n[")
        if (libs.isBlank()) error("gradle/libs.versions.toml has no [libraries] table")
        for (line in libs.lines()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val g = Regex("""group\s*=\s*"([^"]+)"""").find(l)?.groupValues?.get(1)
            val n = Regex("""name\s*=\s*"([^"]+)"""").find(l)?.groupValues?.get(1)
            val mod = Regex("""module\s*=\s*"([^":]+):([^"]+)"""").find(l)
            when {
                g != null && n != null -> out += g to n
                mod != null -> out += mod.groupValues[1] to mod.groupValues[2]
                else -> error("gradle/libs.versions.toml: cannot read the coordinate in `$l`")
            }
        }
        return out
    }

    private fun buildFileCoordinates(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val dep = Regex("""\b(?:implementation|api|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly|androidTestImplementation|debugImplementation|releaseImplementation|classpath|kapt|ksp|annotationProcessor)\s*\(\s*"([^":"]+):([^":"]+)""")
        for (rel in scanned.files.filter { it.endsWith("build.gradle.kts") || it == "settings.gradle.kts" }) {
            for (m in dep.findAll(stripComments(text(rel)))) out += m.groupValues[1] to m.groupValues[2]
        }
        return out
    }

    @Test fun everyLibraryCoordinateIsOnTheG02Allowlist() {
        val catalog = catalogCoordinates()
        assertTrue("the version catalog lists no library: the check would be blind", catalog.isNotEmpty())
        val bad = (catalog + buildFileCoordinates()).filterNot { (g, n) -> allowedCoordinate(g, n) }
        assertTrue("library coordinates outside the architecture section 5 allowlist (G-02): $bad", bad.isEmpty())
    }

    // ---- (d) no forbidden word in any string resource or puzzle title ---------------------------------------------------------

    private fun skippedDevKey(e: StringEntry) = e.key.startsWith("devtools_")

    @Test fun noStringResourceInAnyLanguageMatchesTheWordLists() {
        val strings = RepoScan.allStrings(root)
        assertTrue("no string resource found: the scan would be blind", strings.size > 20)
        val hits = ArrayList<String>()
        for (e in strings) {
            if (skippedDevKey(e)) continue // testing-aid texts, never in release (V-04 / V-08 prove it): DA-106, E3
            if (e.key.substringBefore('[') in PromiseWords.PROMISE_TEXT_KEYS) continue // REQ-009 / REQ-049 prescribed text, exempt by key
            for (h in PromiseWords.hits(e.value)) hits += "${e.file} ${e.key}=\"${e.value}\": $h"
        }
        assertTrue("promise words in string resources: $hits", hits.isEmpty())
    }

    @Test fun noPuzzleTitleInEitherLanguageMatchesTheWordLists() {
        val titles = RepoScan.puzzleTitles(root)
        val files = titles.map { it.file }.distinct()
        assertTrue("no puzzle file found: the title scan would be blind", files.isNotEmpty())
        // AGENTS rule 7 / 13: every puzzle has both titles
        for (f in files) {
            val langs = titles.filter { it.file == f }.map { it.lang }.toSet()
            assertTrue("$f needs both an en and a fi title, has $langs", "en" in langs && "fi" in langs)
        }
        val hits = ArrayList<String>()
        for (t in titles) for (h in PromiseWords.hits(t.text)) hits += "${t.file} title.${t.lang}=\"${t.text}\": $h"
        assertTrue("promise words in puzzle titles: $hits", hits.isEmpty())
    }

    // ---- the devtools_ skip is bounded --------------------------------------------------------------------------------------

    @Test fun everySkippedDevtoolsKeyLivesUnderTheDevtoolsResources() {
        val skipped = RepoScan.allStrings(root).filter { skippedDevKey(it) }
        assertTrue("no devtools_ key found: the skip would be dead", skipped.isNotEmpty())
        val outside = skipped.filter { !it.file.startsWith("devtools/src/main/res/") }
        assertTrue("devtools_ keys outside devtools/src/main/res (they would escape the scan): ${outside.map { it.file + " " + it.key }}", outside.isEmpty())
    }

    @Test fun noPromiseTextKeyIsADevtoolsKey() {
        assertTrue(PromiseWords.PROMISE_TEXT_KEYS.none { it.startsWith("devtools_") })
    }

    // ---- the scan must see: its inputs, and its own matcher --------------------------------------------------------------------

    @Test fun theScanSeesTheFilesItClaimsToScan() {
        assertTrue("fewer Kotlin files than expected: ${codeFiles.size}", codeFiles.size > 20)
        assertTrue(scanned.files.contains("gradle/libs.versions.toml"))
        assertTrue(scanned.files.contains("app/build.gradle.kts"))
        assertTrue(scanned.files.contains("settings.gradle.kts"))
        assertTrue(scanned.files.any { it.matches(Regex("app/src/main/kotlin/.*MainActivity\\.kt")) })
    }

    @Test fun theScannerFlagsWhatItMustFlagAndNothingElse() {
        // positive controls: each fixture contains exactly one violation of its kind
        assertTrue(stripComments("import com.google.android.gms.ads.AdView").contains("com.google.android.gms"))
        assertTrue(stripComments("val u = \"https://example.org\"").contains("https://"))
        assertTrue(stripComments("val v = getSystemService(Vibrator::class.java)").contains("Vibrator"))
        assertTrue(stripComments("val m = context.getSystemService(VibratorManager::class.java)").contains("VibratorManager"))
        assertTrue(stripComments("const val P = \"android.permission.VIBRATE\"").contains("VIBRATE"))
        assertFalse("a comment is not code", stripComments("// Vibrator is not used\nval x = 1").contains("Vibrator"))
        assertFalse("a comment is not code", stripComments("// com.google.firebase is not used\nval x = 1").contains("firebase"))
        assertFalse("a block comment is not code", stripComments("/* WebView /* nested */ still comment */ val x = 1").contains("WebView"))
        assertTrue("a string literal stays visible", stripComments("val s = \"market://details\" // c").contains("market://"))
        assertEquals("line numbers survive", 3, lineOf(stripComments("/* a\nb */\nval Q = 1"), 12))
        assertFalse(allowedCoordinate("com.google.firebase", "firebase-analytics"))
        assertFalse(allowedCoordinate("com.squareup.okhttp3", "okhttp"))
        assertFalse(allowedCoordinate("androidx.ads", "ads-identifier"))
        assertTrue(allowedCoordinate("androidx.activity", "activity-compose"))
        assertTrue(allowedCoordinate("org.jetbrains.kotlinx", "kotlinx-serialization-json"))
        assertTrue(allowedCoordinate("junit", "junit"))
    }

    @Test fun theWordListMatchesWholeWordsAndPrefixesAsMarked() {
        // must hit (one violation each)
        for (s in listOf(
            "Remove ads", "Ad", "No tip needed? Leave a tip", "Rate this app", "5 stars", "Pay now", "Service fee", "Buy it", "Purchases",
            "Price list", "Donate", "Subscribe", "Go premium", "Upgrade", "Unlock all", "Review us", "Start a free trial", "In-App offer",
            "Already paid", "Payment required", "Payments", "Paying now", "Leave a rating", "Ratings", "Rated 5", "No fees", "Tipping jar",
            "Pay 5 USD", "EUR 3", "Start a free trial", "Start a free   trial", "Money back", "Cash", "Shop", "Cost", "Coins", "Only 0,99 EUR", "just €1", "\$5",
            "Poista mainokset", "Ei mainoksia", "Ostoksia", "Osta lisää", "Hinnat", "Tilaus", "Arvostelu", "Lahjoitus",
            "Maksu", "Maksullinen", "Maksetaan", "Kauppa", "Kaupan", "Kaupassa", "Rahaa", "Euroa", "Arvioi sovellus",
        )) assertTrue("must hit: \"$s\"", PromiseWords.hits(s).isNotEmpty())
        // must NOT hit: the false positives the marking exists to avoid, and the app's own words
        for (s in listOf(
            "add", "Adjust", "Address", "feel", "Feed", "payload", "Paycheck", "Feedback", "Moderate", "Operating", "Separated", "Paymaster",
            "usdx",
            "maksimi", "Maksimi 10", "kaupunki", "Kaupungissa", "tilaa", "Tilaa on", "tähti", "Restart", "Aloita alusta", "Retry",
            "Uudestaan", "Seuraava", "best time", "paras aika", "%1\$d / %2\$d", "%1\$d:%2\$02d", "%1\$d. %2\$s · %3\$s", "Kissa",
            "Cat", "Difficulty 3 of 5", "Vaikeus %1\$d viidestä",
        )) {
            assertTrue("must NOT hit: \"$s\" -> ${PromiseWords.hits(s)}", PromiseWords.hits(s).isEmpty())
        }
        // "tipsy" is not the whole word tips: the WHOLE marking holds
        assertTrue(PromiseWords.hits("tipsy").isEmpty())
    }

    @Test fun everyListEntryIsMarkedAndTheDocumentedFalsePositiveStemsAreAbsent() {
        val all = PromiseWords.EN + PromiseWords.FI
        for (w in listOf("ad", "ads", "tip", "tips", "fee", "rate", "stars", "pay")) {
            assertEquals("$w must be a whole word", PromiseWord(w, Match.WHOLE), PromiseWords.EN.single { it.text == w })
        }
        // stems the design forbids as prefixes (they would hit maksimi, kaupunki, tilaa, tähti)
        for (bad in listOf("maks", "kaup", "tilaa", "tähti")) assertTrue("$bad must not be a stem", all.none { it.text == bad })
        assertTrue("free is said by REQ-009 and must not be listed", all.none { it.text == "free" })
        assertEquals(all.size, all.map { it.text to it.match }.toSet().size) // no duplicate entry
    }
}
