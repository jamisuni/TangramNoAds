# Review — design · WO-006

**Date:** 2026-10-04  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:**
- **The design and its frame:**
  - `designs/WO-006-design.md` (Draft rev 0, 2026-10-04);
  - `workorders/WO-006.md`;
  - AGENTS.md (rules 1, 9, 11, 13; seams, test-adapter, token, staffing, release-safety and two-channel duties);
  - `build-map.md` v1.4 §1–2;
  - `architecture.md` v1.0 (G-01…G-10, the verifier table, the `app` row, O-05/O-09, §5 including test placement and verification);
  - `governance.md` v0.2 (rows 4, 10, 12–14, 16);
  - `design-inputs.md` (DI-1/DI-2, §2);
  - `req_review_01.md` (CA-1…CA-6);
  - `.swdev/guard.json`.
- **Requirements (read in full):** REQ-001, 006, 008, 009, 010, 013, 035, 036, 037 and 047; TYPE-007; `features.md` (#Accessibility = colour-blind support, `idea`).
- **Decisions:** F3, F4, F5, F6, F8, F9, F14, F17, F18; DA-26, 40, 42, 43, 49, 53, 60, 62, 70, 71, 74, 75, 76, 80, 82, 88, 89, 92, 93, 95.
- **Spec and prototype:** `Spec/02-ui-layout.md`; `tools/prototype_template.html` (the tray formula, lines 318–336; the en/fi free note, lines 180/189).
- **Code:**
  - `app`: `MainActivity`, `AppViewModel`, `TangramApp`, `TrayRows`, the manifest, `build.gradle.kts`; androidTest `ResetStoreRule`, `AppTouchKit` (`TouchRig`), `FirstPuzzleSolvedTest`.
  - `play`: `PlayLayout`, `PlaySession` (the clock reseed), `PlaceSecondarySweepTest`.
  - `browse`: `BrowseTopBar`, `AllPuzzlesOverlay`, `SolvedBar`.
  - `kernel`: `LayoutRules`, `TrayRules`, the TYPE-007 case in `KernelAcceptanceTest`.
  - `contracts`: `Puzzle.kt` (`PuzzleTitle.inLanguage`).
  - Every module's `strings.xml`, en and fi.
  - `.swdev/verifiers/v04_release_apk.py` (dex reader, canaries).
- **Build outputs (read only):** the dex type tables of `app/build/outputs/apk/{release/app-release-unsigned,debug/app-debug}.apk`; the merged manifests (debug, release, debugAndroidTest); the release merged resource folders.
- **Style reference:** `reviews/WO-005-design-review.md`.
- **Not read:** `.swdev/heldout/`. No Gradle, no emulator and no git were run.

## Verdict

**recirculate → design-author (rev 1, then a spot-check).**

The shape is right and mostly well argued:
- most of the WO is evidence, not code;
- the display override on the two existing AVDs is the simplest honest channel;
- the 600 dp tablet tray breach is real (re-derived: 47.78 dp);
- rotation needs no code, and v1 is untouched;
- the locale seam subclasses the real `MainActivity`.

Rev 1 is still needed before planning, for three reasons:
- **The harness, its gate and its restore are not crash-safe or complete** (F1, F2), and several harness helpers first run at T&V (F3).
- **Two owner-facing readings rest on wrong facts.**
  - DA-100 dismisses split screen, which F3 names (F4).
  - DA-101's motivating defect does not occur for its own example *Swedish, Finnish*, and probably not at all (F5).
- **The promise word list and V-08 fail in opposite ways** (F6, F8).
  - The word list collides with the locked REQ-009 note the next WO must show, and it is blind to Finnish inflection.
  - V-08's network-type half is red on today's APK, and any allowance would blind it.

Each fix is an edit to the design, but F1–F3, F7 and F8 change frozen seams or verifier scope, and F4–F5 change rows that go to Jami.

**0 Blockers, 8 Shoulds, 9 Nits.**

## Checklist applied

- [x] **Directives.**
  - D1: the shell, layout and locale work stays in `app`; the one layout constant stays in `play`; the strings stay in their modules.
  - D2: the display override beats a hand-made tablet AVD, and the subclass seam beats per-app locales. Two items are not the simplest working option:
    - V-08 (a) as written cannot work (F8);
    - L-2 may fix nothing (F5).
  - D3: every abstraction names its REQ or guardrail:
    - `DisplayRule` / `ScreenWalk` / `PlayerControlWalk`: REQ-035/036/037/047;
    - `ui_language`: REQ-047;
    - `LocaleOverrideActivity`: REQ-047 tests;
    - V-08: G-01.
  - D4: the font-scale-2.0 "no clipping" assertion imports a requirement no REQ states (F7).
  - D5: no new library. `UiAutomation` is framework API, and `ActivityScenario` is already on the androidTest classpath.
- [x] **Guardrails.**
  - G-01/G-02: the source scan, V-08 and the installed-package test all apply.
    - V-08's net-type list: F8.
    - The word list: F6.
  - G-04: by construction nothing debug-only reaches release, but only a review checks it (N2).
  - G-05: `ui_language` is in both languages, and V-05 keeps the pair.
  - G-06: untouched.
  - G-09: untouched, LOCK-V1 holds.
  - G-10: every test adapter fails with `error()`, except the `TestWatcher` swallow (F2).
- [x] **Contract.**
  - No `I*` change. The locked `Puzzle.kt` is not edited, and the v1 save format is untouched (re-checked: rotation keeps everything in `AppViewModel`, the manifest has no `configChanges`).
  - A conflict between two locked REQs (REQ-013's row cap vs REQ-037's floor in short windows) is presented as "not a supported size" (F4).
  - DA-101 states a behaviour the build will not have (F5).
  - The REQ-009 note text contradicts the C3 carry plan (F6).
- [x] **Scope.** Everything traces to the 13 IDs. Two exceptions:
  - the font-scale-2.0 clipping assertion (F7);
  - the C9 carry under a token whose meaning it is not (N7).

  The carried parts C1–C11 are otherwise right; nothing in scope is wrongly carried.
- [x] **Traceability.**
  - Rule 3 holds: #Layout and #Language are coded in `app` with covering tests in `app/src/androidTest`. #Promise is `—` (system level, rule 3 skipped), and `app` placement is fine.
  - Every seam row names a delivering task. The font-scale mechanism has none (F7).
  - Token claims: see N5 and N7.
- [x] **Hard-stops.**
  - No schema or migration change, no security or PII.
  - No money matter is decided: DA-105/106 only define stricter checks (N6).
- [x] **Evidence re-derived.** See below. Five claims are wrong or unverified:
  - "Swedish, Finnish shows Finnish buttons over an English title" (F5);
  - `resources.displayMetrics` as the dp check (F2);
  - "the override cannot give a taskbar" (F2);
  - "side margin: equal effect" (N4);
  - the Socket/WebView deny entries (F8).
- [x] **The conceptual 20 %.** The effort went to:
  - the harness on both API levels;
  - the REQ-013/REQ-037 arithmetic, ported and swept;
  - the platform's locale resolution against the real merged resources;
  - the word list against REQ-009 and the real Finnish strings;
  - V-08 against the real dex type tables;
  - the held-out slice versus where the risk first shows.

### Re-derived

- **Tray arithmetic** (`trayScaleFor` ported to Python with exact diameters: LT 4√10/3, MT 4√5/3, ST 2√10/3, SQ 2, PG √10; script `tray_sweep.py` in the session scratchpad):
  - **The breach.** The tablet 600 × 960 square cell is **47.78 dp**. Every tablet 600–602 dp wide breaches; 603 gives 48.07. Real 7-inch tablets are 600 dp wide, so this is no edge case.
  - **After the fix.** Gap 12 gives 49.32 at 600 dp and 68.56 at 800 × 1280. 1280 × 800 is height-limited and unchanged (67.02; the design's 66.98 comes from the rounded 4.22 and does not matter).
  - **The design's sweep after the fix.** The minimum over 360–1400 × {550, 640, 780, 844, 960, 1200} is **48.05 dp, at H = 550, in both classes**. The exact height bound is **549.3 dp**.
  - **Below the bound.** For H < 525 the 84 dp cap applies, and the square is 46.15 dp.
  - **The prototype.** It uses the same (n + 1) gaps (`prototype_template.html:325`), so the port is faithful. Spec/02 §3.4 names the gaps 8/14.
- **Locale resolution.**
  - **The real APK.** The release merge has **85 `values-*` folders** (androidx/Compose translations), `values-sv` among them: `app/build/intermediates/incremental/release/mergeReleaseResources/merged.dir/values-sv/values-sv.xml` holds 31 androidx strings.
  - **Who reads the locale.** Only `BrowseTopBar.kt:54` and `AllPuzzlesOverlay.kt:57` read `locales[0]`.
  - **`grid_cell_description`** is `%1$d. %2$s · %3$s` in both languages.
- **Release dex** (`v08_probe.py`, V-04's string reader plus type_ids and class_defs):
  - **Framework types already referenced.** The release APK references `Ljava/net/Socket;`, `Ljava/net/InetAddress;`, `Ljava/net/URL;` and `Landroid/webkit/WebView;`; `Ljava/net/HttpURLConnection;` and `Ljavax/net/ssl/HttpsURLConnection;` are absent. It defines `androidx/core/net/DatagramSocketWrapper`, `TrafficStatsCompat` and similar classes.
  - **SDK packages.** No class is defined or referenced under `com/google/android/gms`, `com/google/firebase`, `com/android/billingclient`, `com/google/android/play`, `okhttp3`, `retrofit2` or `io/ktor`.
- **Manifests.**
  - The debug and release merged manifests have no `uses-permission`. `android:permission="android.permission.DUMP"` is an attribute (the profile-install receiver), as the design says.
  - The debugAndroidTest merged manifest has `REORDER_TASKS`.
- **REQ-009 versus the word list.**
  - REQ-009 (locked) mandates "Enjoy, it's absolutely free. No ads, no purchases, no network."
  - Under F14 the Finnish text is the prototype's verbatim: "Ei mainoksia, ei ostoksia, ei verkkoa."
- **Rotation.**
  - `controller.gridOpen` lives in `BrowseController`, which is in `AppViewModel`, so the F3 "grid survives" case should pass as is.
  - `PlaySession` keeps the last frame time, and "a rebuilt composition seeds its clock from it (CR-F1)", so glides and the solve timeline continue through a recreation.
  - `MainActivity.onCreate` reads `Resources.getSystem().configuration.smallestScreenWidthDp`.
- **Insets.** `TangramApp` pads with `WindowInsets.safeDrawing`, which is system bars, IME and cutout, **not** the gesture insets. So the REQ-035 A2 claim rests on `PlayLayout`'s 10 dp phone bottom margin (N5).
- **A4 scan surface.** DA-89's `ReleaseSeparationTest` scans `app/build.gradle.kts`, which L-0 edits (N2).

## Trajectory & quality

- **Verification actually run?** Partly.
  - The code reading behind "test only" is accurate: insets, ViewModel, no `configChanges`, `TrayRows`.
  - The tablet breach was computed by hand and is correct.
  - The locale defect, the dp check and V-08's deny entries were never run against the artifacts that exist in `app/build/`. Each would have shown its problem.
- **Proportionate?** Mostly. Four small code changes and an evidence harness fit the WO. The five promise layers are generous but defensible for the project's founding promise. The font-scale-2.0 clipping leg and V-08's aapt2 half add cost without a REQ need.
- **Path sane?** First pass. The author named the right highest risk and asked to be attacked there. The gaps are concrete (restore, metric, gate coverage), not conceptual.

## Findings

| ID | Sev | Section | Finding | Rule / REQ | Required change |
|---|---|---|---|---|---|
| F1 | **S** | §1 `DisplayRule.finished`, the hygiene line, Risks bullet 1, DA-96 | **Restoring the device is not crash-safe, and it is incomplete.** `TestWatcher.finished` runs after a failed assertion, but not after an instrumentation crash, a Gradle timeout or a killed process. `wm size`/`wm density` are **persisted** (Settings on API 26, display settings on API 29+) and survive an emulator restart. The hygiene line runs only *after* a suite, and it lists only size, density and the API 30+ airplane mode. It misses: <br>• `font_scale` (§4 uses 2.0); <br>• the rotation settings: `setRotation(ROTATION_UNFREEZE)` turns auto-rotate **on** whatever it was, and the prior `user_rotation` is lost; <br>• the gesture-navigation overlay (§2, API 29+); <br>• API 26 airplane mode (the flag plus wifi/data). <br>Nothing detects leaked state. `play`, `browse`, `devtools` and the older `app` suites "keep their displays" only if the display is native when they start; otherwise they run on a 1920 × 1200 tablet and fail confusingly, or pass on the wrong size. | AGENTS two-channel duty; REQ-035/036/037 evidence integrity | (a) **Reset before every device suite as well as after**, in the D-1 recipe and in the AGENTS line. Cover every global this WO touches: size, density, `font_scale`, `accelerometer_rotation` + `user_rotation`, the nav-mode overlay, airplane mode + the API 26 radios. <br>(b) `DisplayRule` first asserts the native size and density (`error("leaked display state from an earlier run")`), then freezes rotation 0 before applying. On finish it restores the **saved prior** rotation settings, never `UNFREEZE`. <br>(c) Put the same native-display assertion where the older suites start (the `app` rules, or a `RunListener` per module), so no suite runs on a leaked display. <br>(d) Font scale: see F7. |
| F2 | **S** | §1 "reads the dp … (`resources.displayMetrics`)", the seam row `DisplayRule … : TestWatcher()`, the G-DISPLAY gate and fallback, Risks (1)–(4) | **The dp check measures the wrong thing, and the rule cannot stop a bad test.** <br>• `resources.displayMetrics` is the app-usable area. On API 26 it excludes the navigation bar (844 → about 796 dp; 1280 × 800 landscape → about 752 dp), so a correct override `error()`s. <br>• `wm` reaches the test process asynchronously, and `executeShellCommand` returns before the command finishes unless its output is read to EOF. Meanwhile `MainActivity` reads `Resources.getSystem()` sw at once. <br>• A `TestWatcher` **swallows** an exception thrown in `starting` and still runs the test body; it fails only at the end. So a dp mismatch does not stop the test. <br>• The fallback is not plannable. A runner-set display means one Gradle run per spec per channel. The parameterised classes would need a spec filter that neither runs nor silently skips the other specs, and neither the argument nor the cost is named. <br>• "The override cannot give a real tablet's taskbar" is unverified. On API 37 a sw ≥ 600 configuration may well bring Launcher3's taskbar. | AGENTS "test adapters fail loudly"; G-10 spirit | • Verify against the **real** display (`Display.getRealMetrics` on API 26–29, `WindowManager.maximumWindowMetrics` on 30+) plus `densityDpi`. Poll, with a timeout, until `Resources.getSystem().configuration` shows the expected sw and density. Drain every shell output. <br>• Make `DisplayRule` a `TestRule`/`ExternalResource` whose `before` throws, with the restore in `finally`. <br>• Add to G-DISPLAY: (4) the instrumentation survives a mid-process density change; (5) the restore is observed. <br>• Write the fallback concretely: the instrumentation-argument name, an annotation filter, the per-spec Gradle calls, and the time cost. <br>• Replace the taskbar sentence with "record the system UI the first API 37 tablet run shows". |
| F3 | **S** | Test seams (T-1a / T-1b), the acceptance table's H rows, L-3, G-DISPLAY | **The riskiest harness pieces first run at T&V.** `NavigationMode.ensureGesture` (`cmd overlay`), `Rotation.freeze90`, `PlayerControlWalk` and the font-scale switch are used only by held-out tests. A harness fault then shows up as a held-out failure (the WO-005 T&V HOLD, DA-95, is the precedent). <br>L-3 is "created only if a walk finds a breach", but the touch-target walk is held-out. So no REQ-037 breach other than the sweep's can trigger L-3 before T&V. <br>The `sv-SE,fi-FI` decision test, which decides L-2 (F5), has no stated slice. | AGENTS "compile staged and held-out tests early"; held-out practice | • T-1a adds **one visible scaffolding test per helper** (no token; `// decision DA-96/98`): set, verify and restore for nav mode, rotation, font scale and airplane mode. <br>• G-DISPLAY becomes a device-harness gate over all of them, on both channels. <br>• Add a visible `PlayerControlWalk` run on one display (e.g. `TABLET_600x960`, en, `// decision DA-99`) as the visible signal for L-1/L-3. The full matrix stays held-out. <br>• Put the mixed-list decision test in the visible slice. |
| F4 | **S** | §4 "The supported window", DA-100, CA-7(b), Risks bullet 3 | **A conflict between two locked REQs is filed as "not a supported size".** Below **549.3 dp** of window height, locked REQ-013's own rule (a row is at most max(84 dp, 16 % of H), padding 12) forces the square's cell to 2.00 × 72 / 4.216 + 12 = **46.2 dp**. No build value can satisfy both REQ-013 and REQ-037 there. <br>DA-100's basis, "no REQ asks for such windows (F9 names the reference sizes)", is wrong. Owner-accepted **F3 names split screen** as a window change the game re-lays for, and F9 bounds only the width (360 dp), not the height. <br>Also, "tablet width ≥ 600" in the bound is a tautology, and the sweep's 550 row passes by 0.05 dp. | REQ-013 Rules (locked), REQ-037 A1, decisions F3/F9, governance rows 12/13/14 | • Restate DA-100 as **a REQ-013 ↔ REQ-037 conflict**, with the interim rule: REQ-013's cap holds (today's behaviour), stated for split screen. <br>• Make CA-7(b) a **two-option CHG for Jami**: (i) the floor wins, so rows may exceed the cap below 549 dp; or (ii) REQ-037 names a supported window. <br>• Surface it at the checkpoint (owner item 1), not as a silent row-12 call. <br>• State the bound as 549.3 dp and drop the tautology. |
| F5 | **S** | In-one-paragraph change (2), §5 "Who chooses the language", DA-101, L-2, the Alternatives "titles" row | **The motivating defect does not occur for the design's own example, and probably not at all.** The merged APK carries androidx/Compose translations in 85 `values-*` folders, **`values-sv` among them**. <br>• For *Swedish, Finnish*, Android resolves the resources to `sv`. Our modules have no `sv`, so every string falls back to English. `locales[0]` is `sv`, so the title is English too. That is one language, today and after L-2. <br>• DA-101's "a list such as *Swedish, Finnish* shows all Finnish" is therefore false, and it goes to Jami and into CA-8. <br>• Where the resolver does pick a later list entry, `ResourcesImpl.updateConfiguration` moves the best match to the **front** of the configuration's `LocaleList`. Compose's `LocalConfiguration` is a copy of `resources.configuration`, so `locales[0]` very likely already *is* the resolved language. | REQ-047 Statement, decisions F6, D2/D3 (a change answers a real need) | (a) **Run the mixed-list decision test on today's code first**, as L-1 waits for the sweep. If it passes, drop L-2, or keep `ui_language` explicitly as a one-line belt with no defect claim. <br>(b) Rewrite DA-101 with the real behaviour and **name the lever that decides it**. Without `androidResources.localeFilters` (en, fi), any first language a library ships (about 80 of them) gives English. With it, *Swedish, Finnish* gives Finnish. Choose deliberately, and put the choice to Jami in CA-8(a): REQ-047's "device language" vs F6's "the language Android reports for the app". <br>(c) Use a test list whose outcome tells the two alternatives apart. |
| F6 | **S** | §6 item 4 (the word list), layer 1 (d), C3, DA-106 | **The forbidden-word list fails in both directions.** <br>(1) **It collides with a locked text.** REQ-009's note says "No ads, no purchases, no network", and whole-word `ads` matches it. So C3 ("the free note must pass the word list") cannot hold. Layer 1 (d), which carries `REQ-008.A1`, turns red the day WO-007 adds the note, and with Notes rule 5 ("never loosen a walk") WO-007 inherits a contradiction. <br>(2) **Finnish inflection blinds it.** The Finnish note "Ei *mainoksia*, ei *ostoksia*" passes `mainos*` and `osta*`. `hinta*` misses *hinnat/hinnan*; `tilaa*` misses *tilaus* and false-hits *tilaa* ("room"); `arvostele*` misses *arvostelu*. <br>(3) **The core roots are missing:** en `pay*`, `paid`, `money`, `shop`, `coin*`; fi `maks*`, `raha*`, `kaup*`. Paying is REQ-001's core ("ask … for money in any form"). <br>(4) **A title false positive is waiting.** `tähti*` would hit a future *Tähti* (star) puzzle, a common tangram shape. | REQ-009 (locked) text, REQ-001 Statement and Rules, REQ-008 A1 | • Exempt the REQ-mandated promise texts **by key**, in one named list that cites REQ-009 for the note and REQ-049 for the privacy text (or match *asks*, not nouns). Rewrite C3 to match. <br>• Use inflection-safe stems: `mainok`/`mainos`, `ostok`/`osta`, `hinn`/`hint`, `maks`, `tilau`, `arvostel`, `arvio`, `lahjoit`, `raha`, `kaup`. Add the en pay/money roots. <br>• Run the list over every `Tangrams/*.json` title in the JVM layer, with a named title allowlist. |
| F7 | **S** | §5 "No clipped control text", §4 font scales, Test seams | **"No clipped text at font scale 2.0" has no REQ behind it.** REQ-047 A1 is about language, and #Accessibility is an `idea` (colour-blind support). <br>• At 2.0 the Finnish solved bar does not fit 360 dp: *Uudestaan* and *Seuraava* at 34 sp are about 212 + 192 dp, against about 312 dp of row. L-3, under "the test is never loosened", then becomes a solved-bar redesign for a font size no REQ names. <br>• The width-only check also misses the top bar's vertical clip (a fixed 66 dp, Spec/02). <br>• No seam sets the font scale. The global `font_scale` is one more leakable setting (F1), and no task delivers it. | D4 / no silent scope, REQ-047 A1, AGENTS seams lesson | • Keep the no-clip assertion at font scale **1.0** only (state it in DA-103). <br>• Keep 2.0 only for REQ-037's "a dp size does not shrink" check. <br>• Add the font-scale mechanism as a frozen seam with a delivering task. Prefer a `fontScale` beside `languageTags` in the debug activity's configuration wrap, so no global setting changes. |
| F8 | **S** | §6 item 2 (V-08 (a)), DA-104, seam row P-1 | **V-08's dex half is red today, and an allowance would blind it.** <br>• Today's release dex already references `Ljava/net/Socket;`, `Ljava/net/InetAddress;`, `Ljava/net/URL;` and `Landroid/webkit/WebView;`, as type_ids from androidx.core and others. So "none of … `Socket`, `WebView`" fails on day one. <br>• An `ALLOWED_HITS` keyed by type then blinds the check for good, because a string/type table cannot say *which class* references a type. <br>• The SDK-prefix half (classes defined under gms, firebase, billing, play, okhttp, retrofit, ktor) has 0 hits today. It is the only check that sees transitive SDKs. <br>• V-08 does not build its input, so it can pass a stale APK. | G-01 ("no dependency does networking, ads, billing…"), D2; the WO-005 V-04 freshness lesson (F2 there) | • Restrict (a) to class **definitions** under the SDK deny-list, and drop the framework-type references. The missing `INTERNET` permission is what makes them harmless, and layers 2(b) and 3 prove that. <br>• Build the release APK as V-04 does, or print and check its sha256 and mtime against V-04's run. Align the self-test. <br>• (b) `aapt2` mostly repeats V-01 on the linked manifest: keep it only as a cheap belt. |
| N1 | N | DA-96 | Architecture §5 promises "the API 37 image … with phone 390×844 and tablet 1280×800 profiles", and WO-006's constraints expected a hand-made tablet AVD. The override replaces that channel, which is a sound choice. | architecture §5, governance row 13 | Cite the §5 line in DA-96 as an architecture reading (ai+inform → checkpoint). |
| N2 | N | §5 locale seam, DA-102, L-0, C-1 | **Release safety holds by construction:** <br>• `app/src/debug` and its manifest never reach release; <br>• `open` changes only an access flag; <br>• the release-side changes are `open`, `ui_language` and the gap, each justified. <br>But only a review checks it. And L-0 edits `app/build.gradle.kts`, which DA-89's `ReleaseSeparationTest` scans for "devtools", exempting one line. | G-04 spirit, ADR-006, DA-89 | • Add `Lio/github/jamisuni/tangram/LocaleOverrideActivity;` and `…/TestLocale;` to V-08's release deny-list, and to a debug positive control as must-find. <br>• L-0 brief: declare the scan inputs with **globs**, never module names. <br>• V-04 needs no change. |
| N3 | N | §5 ScreenWalk A1, seam rows `ScreenWalk` / `PlayerControlWalk` | "A fi value after the format arguments are filled" accepts an *English* title inside the Finnish grid description (`%1$d. %2$s · %3$s` in both languages) unless the `%s` captures are checked recursively. That is exactly the title-language class this WO touches. The grid is lazy, so "one cell per library puzzle" needs a scroll to each index. | REQ-047 A1 | State both rules in the seams. |
| N4 | N | Alternatives "Tablet tray fix" (a), DA-100 basis | "Side margin 16 → 12: equal effect" is wrong. In `trayScaleFor` the gap counts (n + 1) = 8 times and the margin twice: gap 14 → 12 frees 16 dp (49.32), while margin 16 → 12 frees 8 dp (48.55). The slack comes from the prototype's (n + 1)-gap formula with centred cells. The fix departs from Spec/02 §3.4 and DI-1. | DI-1/DI-2, design-inputs §2, AGENTS rule 9 | Correct the row. Add the departure to `design-inputs.md` §2 at the close; CA-7(c) names the prototype template for the capture side. |
| N5 | N | §2, acceptance row REQ-035 A2, DA-98 | Checking the 3-button bar below API 29 is a fair reading; it needs no CHG beyond CA-7(a). But that branch asserts the inset *Rule*, not A2. "Code: none" rests on `PlayLayout`'s 10 dp phone bottom margin covering the difference between the mandatory-gesture inset and the navigation-bar inset (`safeDrawing` pads only the latter). | AGENTS "tokens are coverage claims" | Label the API < 29 branch "rule-level; A2 not applicable" in its message. Name the API 37 run as A2's evidence. Log both insets in the visible kit (F3). |
| N6 | N | §6 item 5, DA-105/106 | Airplane mode on API 26 is honest only as "flag set, radios off". The no-`INTERNET` argument carries the claim (good). `svc … enable` on restore changes an AVD whose radios were off. | REQ-010 A1, governance row 12 "never on a row-10 matter" | • Restore the saved prior states. Optionally toggle only on API 37, and let API 26 rest on the argument plus the installed-permission test. <br>• Add one line each to DA-105/106: they define stricter checks and add nothing, so they are not a row-10 matter. |
| N7 | N | Carried parts C2 and C9, the REQ-008 acceptance rows, the REQ-010 A2 seam | • C9 puts a puzzle-*time* assertion under `REQ-036.A2` ("keeps every placed piece"). <br>• C2's "visible twin" names no file. <br>• `PromiseSourceScanTest` carries `REQ-008.A1/A2`, but it asserts the REQ-008 rule ("no component"), not "appears". <br>• `HeldInstalledPermissionsAppTest` must query the *target* package: the test APK's manifest has `REORDER_TASKS`. | AGENTS "tokens are coverage claims" | • C9 → the REQ-029/031 tokens, or `// decision F3`. <br>• Name C2's file. <br>• Tag the source scan `// guardrail G-01`, or state the engine-level reading in the row. <br>• Write `targetContext.packageName` in the seam. |
| N8 | N | Suggested cut, the WO's DoD | • L-0 (haiku) holds `LocaleOverrideActivity`. Its behaviour only a device run shows; its done-check is assemble-only; and its fallback (`applyOverrideConfiguration`) depends on that run. <br>• No device-time budget is given. The held-out touch walk alone is about 72 launches per channel (4 screens × 6 displays × 2 languages, plus the phone font cases), each with two configuration changes. <br>• V-08 is missing from the WO's DoD verifier line. | AGENTS staffing lesson | • Split **L-0a** (mechanical: `open`, the build inputs; haiku) from **L-0b** (the locale seam; sonnet; done-check = a device smoke on both channels). <br>• Give D-1 a time budget, and trim language × font to the smallest phone. <br>• Add V-08 to the DoD line, and "V-08 live" to build-map #Promise at the close (notify tier, as V-04 in #DevTools). |
| N9 | N | §3 rotation, §7 REQ-006 | The rotation argument is complete for the REQ (re-derived above). The REQ-006 flow solves a 3-piece mini and never flips, yet the badge's place depends on the layout (DA-29). | REQ-036 A2, REQ-006 A1 | • Cite CR-F1 (the clock reseed) in §3. <br>• Optional: a rotate-during-the-solve-timeline case (`// decision F3`), and one parallelogram flip on a tablet in `PlayThroughAppTest`. |

### Answers to the brief's questions not covered above

- **Q1, the dp evidence.** The override gives honest dp evidence: real window, real insets, real rotation, and the F3 rule fed by a recomputed sw. The 2× width clamp is handled by the spec choice plus a loud check.
- **Q1, smallest width and F3.** `smallestScreenWidthDp` follows the override on both API levels:
  - on API 26, sw is the minimum of the per-rotation app widths, and the nav bar sits at the bottom for sw ≥ 600, so the 600 × 960 spec gives exactly 600;
  - it follows only after the asynchronous update (F2).
- **Q2, release.**
  - Nothing debug-only can reach the release APK (N2).
  - The release-side changes are `open`, `ui_language` and the tablet gap, all justified. `ui_language` may be unneeded (F5).
  - V-04 needs nothing. `ReleaseSeparationTest` needs nothing, but the L-0 brief must keep "devtools" out of `app/build.gradle.kts`.
- **Q4, V-08's status.** V-08 is not a contract change. It enforces an existing G-01 sentence, as V-07 sits outside the §2 table. It is worth having for the transitive-SDK half only (F8).
- **Q5, REQ-035 A2 on API 26.** Fair; see N5.
- **Q7, the held-out choice.** The five held-out IDs (REQ-035 A2, 036 A2, 037 A1, 047 A2, 010 A2) are a sensible spread. The visible slice still guides L-1, through the JVM sweep. It does not guide the harness (F3) or L-2 (F5).

## What is good and must be kept

- **Evidence first, verified against the code.** `safeDrawing` keeps the play area above the bars, the session is in `AppViewModel`, there are no `configChanges`, and `TrayRows` are already right. So REQ-006/035/036 are tests, not features.
- **The 600 dp tablet finding.** It is real and correctly computed. The pattern "the sweep decides L-1, else the fix is withdrawn" is the right one; reuse it for L-2 (F5).
- **The display override over a hand-made tablet AVD.** It uses exact-dp specs chosen around the 2× clamp, a loud mismatch check, a gate and a stated limit. It is the right D2 choice.
- **Rotation without code.** No `SavedStateHandle`, no `configChanges`, no v1 change, and the test places more than one piece.
- **The locale seam.** A debug-only subclass of the real `MainActivity` wraps only the base context, and per-app locales, the system locale and AppCompat are each rejected for a stated reason.
- **The player-control walk.** It classifies by tag or nearest tagged ancestor, **fails on an unclassified interactive node**, has must-find canaries, checks both axes separately and skips `dev-*`.
- **DA-98's exclusion.** It is conservative (the maximum of stable, system-gesture and mandatory-gesture insets), it forces gesture mode on API 29+, and it never silently skips.
- **The promise scan reads declared Gradle inputs** (the DA-88 lesson applied). `PackageInfo` on the installed app is the literal REQ-010 A2 evidence, and the `dumpsys` line covers the installed release copy.
- **Carried parts C1–C11** are named, each with its token and its target WO. The Notes' "never loosen a walk; a breach is a code change or a CA" is right; F6 only removes a contradiction it would otherwise meet.

## Owner items (Jami)

Nothing here needs Jami before rev 1. For the WO-006 checkpoint:
1. **Short windows (F4, CA-7(b)).** In Android split screen, below about 549 dp of height, REQ-013's row cap and REQ-037's 48 dp floor cannot both hold. Should tray rows grow past the cap, or does REQ-037 name a supported window?
2. **Devices with several languages (F5, CA-8(a)).** Today a *Swedish, Finnish* device gets English everywhere, because the app's libraries ship Swedish. A one-line build setting would make it Finnish. Which does he want?
3. **Tablet tray spacing (DA-100).** The gap between tray cells goes from 14 to 12 dp on tablets, so the miniatures on portrait tablets are about 2 % larger.
4. **Tablets are checked by a display override on the phone emulators, not by a tablet AVD** (DA-96, architecture §5). His own tablet playtest remains the check of the real system UI.
5. **The Finnish forbidden-word list and the unmarked Finnish strings** go on his review list (X-1, already planned).

## Handoff — Design Reviewer · WO-006

- **Scope:** REQ-006 A1, REQ-035 A1–A2, REQ-036 A1–A2, REQ-037 A1, REQ-047 A1–A2, REQ-001 A1, REQ-008 A1–A2, REQ-010 A1–A2 (#Layout, #Language, #Promise). Governed: `IPuzzleLibrary` consumed (locked, `Puzzle.kt` not edited), `IProgressStore` untouched, v1 untouched (re-verified).
- **Inputs read:** see the header (design rev 0; the working tree after the WO-005 close; the real APKs and merged resources in `app/build/`).
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-006-design-review.md`
- **Status:** recirculate → design-author (0 Blockers / 8 Shoulds / 9 Nits). One line each:
  - **F1:** device-global restore is not crash-safe (`wm` overrides persist) and misses font scale, rotation settings, nav mode and API 26 airplane mode. It needs a reset before each suite and a leaked-state check.
  - **F2:** the dp check reads `displayMetrics`, which excludes the nav bar on API 26. `wm` is asynchronous, `TestWatcher` swallows a failing `starting`, and the fallback is not plannable.
  - **F3:** nav mode, rotation, the control walk and font scale first run in held-out tests at T&V. They need visible scaffolding and a wider device gate.
  - **F4:** DA-100 calls a REQ-013 ↔ REQ-037 conflict "not a supported size" and dismisses split screen, which F3 names. It must be a two-option CHG for Jami.
  - **F5:** the APK ships `values-sv`, so *Swedish, Finnish* is all English, today and after L-2. DA-101's claim is false, and L-2 may fix nothing. Test first, and name the `localeFilters` lever.
  - **F6:** whole-word `ads` hits REQ-009's mandated note, so C3 is impossible; Finnish inflection and the missing pay/maksu roots blind the list.
  - **F7:** the font-scale-2.0 no-clipping assertion has no REQ and would force a solved-bar redesign; the font-scale mechanism also has no seam.
  - **F8:** V-08's java.net/WebView entries already hit today's release dex, and allowing them blinds the check. Keep only the SDK class definitions, and guard against a stale APK.
- **Traceability delta:** none (review only). Changes the rev 1 will bring:
  - F1, F2 and F7 change the `DisplayRule` and `TestLocale` seams (rule type, rotation save and restore, `fontScale`);
  - F3 adds visible scaffolding tests to T-1a;
  - F5 may remove L-2 and its seam row;
  - F8 changes V-08's CLI and self-test scope;
  - N8 splits L-0.
- **Notes for next station:**
  - Fix F5 and F4 first: they change what goes to Jami (DA-100, DA-101, CA-7, CA-8). Then F1–F3 (the harness), then F6–F8.
  - A rev 1 re-review can be a spot-check of:
    - §1, §4, §5 and §6;
    - the seam and acceptance tables;
    - DA-96, 98, 100, 101, 103, 104, 106;
    - CA-7/CA-8;
    - the cut.
  - The reviewer's scripts are in `C:\Users\Jami\AppData\Local\Temp\claude\C--GitHub-AI-TangramNoAds\9c42d4d4-131b-4b08-a0c2-faa3e9f21c46\scratchpad\`:
    - `tray_sweep.py` ports `trayScaleFor` with exact diameters;
    - `v08_probe.py` reads the type_ids and class_defs of either APK through V-04's string reader.

## Spot-check of rev 1

**Date:** 2026-10-04
**Read:** `designs/WO-006-design.md` rev 1 in full, with weight on:
- the rev 1 paragraph;
- §1, §2, §4, §5 and §6;
- the Alternatives rows;
- the seam table and value shapes;
- the acceptance table and the scaffolding list;
- the Risks;
- DA-96…DA-108;
- CA-7/CA-8 and the owner items;
- the cut.

The orchestrator's rulings were taken as given:
- **F4:** the interim is "REQ-013's cap holds", with a two-option CHG and no code.
- **F5:** test first; `localeFilters` is an owner option; L-2 is a contingency.
- **F7:** as required.

No Gradle, emulator or git was run, and `.swdev/heldout/` was not read.

**Re-derived for this check:**
- **The rev 1 word stems** (prefix at a word start, as §6 item 4 defines them; `stems_probe.py` in the session scratchpad) were run over every module's `strings.xml`, every `Tangrams/*.json` title, and the prototype's en/fi how-to, free-note and privacy texts. Results:
  - the free note is hit in both languages, as intended: `ad` and `purchas` in English, `mainok` and `ostok` in Finnish (rev 0 missed the Finnish note);
  - **one day-one hit:** devtools `devtools_hint_unlocked`, "**Unlock**ed until the app is restarted…", via `unlock` (E3);
  - nothing else: no title, and nothing in the how-to or privacy texts that WO-007 will add.
- **`tests_regex`:** `androidxTestRunner = "1.7.0"` is in `gradle/libs.versions.toml`.
  - AndroidJUnitRunner's `tests_regex` matches `Class#method`. A JUnit `Parameterized` case is named `method[<name>]`, so `.*\[PHONE_390x844\]` selects it **only if** the parameters are declared `name = "{0}"`. The default `[{index}]` would select nothing (E2).
- **The packaged locale folders** (85 `values-*`) contain no `se` (Northern Sami), `smn` or `fo`. Such a language is therefore the only kind of first entry for which Android resolves *past* it to `fi` (E1).

### The author's two variations

- **F1(c): `device_reset.py --check` before each suite, not a `RunListener` per module. Accepted, with one condition.**
  - The `play`, `browse` and `devtools` suites each run in their own Gradle call, so a check before every call protects them fully.
  - The **older `app` tests are the exception.** They share one instrumentation run with the `DisplayRule` tests. If a rule's `after()` fails to restore without crashing the process (for example, an observation timeout), the rest of that run can execute on a leaked display, and nothing between the tests notices.
  - The run is red in that case anyway, but its per-test results must not be trusted.
  - Fix: E5 (the after-check voids the suite) closes it at no cost.
- **F2 fallback: `tests_regex` plus `[SPEC]` case names. Accepted as the fallback mechanism, but incomplete.** The fallback is only used if gate item (2) fails, so this is not a Blocker, but three gaps would make it skip tests silently:
  - (i) the naming convention must be `@Parameters(name = "{0}")` with the enum constant;
  - (ii) every display must be its own parameter case. No test method may loop over displays inside itself, so the held-out touch matrix and `PlayThroughAppTest` must be parameterised by spec;
  - (iii) the tests *without* a spec (the older `app` tests, the airplane and promise walks on the native display) need a sixth call that excludes the spec cases. `tests_regex` cannot exclude, but a marker annotation on the `DisplayRule` classes plus `notAnnotation` can.

  The claim "silently skips none" needs a proof: the executed counts of the six calls add up to the suite's count (E2).

### The other checks asked for

- **No new Blocker.** Every F and N item is answered (table below). The new material adds four small risks, none of them a Blocker: E1, E3, E4 and E6.
- **H-1 `device_reset.py`.**
  - **Crash-safe by construction.** It is host-side `adb`, run before *and* after every device step, so it does not depend on the instrumentation surviving. A leak from an earlier session or an emulator restart is caught by the before-check.
  - **It covers API 26:**
    - `wm size` / `wm density` reset, and verified through the absence of the `Override …` lines both API levels print;
    - `font_scale`, rotation, and the airplane flag plus `svc wifi/data`;
    - the nav-mode overlay correctly limited to API 29+.
  - **Two loose ends.** The nav-mode baseline is "recorded" at runtime, so it could record a leaked state. And the airplane reset is not written per API level (E4).
- **V-08 builds its own release APK.**
  - **Cost.** It deletes the output and re-runs `:app:assembleRelease` right after V-04, so only packaging re-runs: well under a minute.
  - **One build at a time.** V-01 → V-04 → V-08 run sequentially, then debug runs for `--positive-control` and `--expect-debug`. The self-test uses crafted fixtures and builds nothing.
  - **The stale-APK guard works, with one gap.** With `--apk`, "older than the newest tracked source file" needs the file set named; there is no git on the build side (E6).
- **The Model column follows the AGENTS.md staffing rule.**
  - **L-0a, haiku.** Mechanical: `open` plus a Gradle input block copied from the DA-88 pattern. The orchestrator reads the diff, and the re-run proof at the JVM move-in exercises its one behaviour.
  - **Sonnet tasks:** L-0b (device-only behaviour, with a device smoke on both channels), H-1 (device I/O), P-1 (binary parsing plus a build), and L-1 and L-3 (UI).
  - **X-1, haiku.**
  - If the L-2 contingency is revived, it is mechanical (haiku, diff read).

### Rev 0 findings

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (restore not crash-safe or complete) — S | **fixed**, residue | `device_reset.py` runs before and after every step and covers every global. `DisplayRule` asserts a native start, saves and restores prior rotation, and never uses `UNFREEZE`. Font scale is per launch, so nothing global changes. DA-108 records it. Residue: E4 and E5 |
| F2 (dp metric, async, `TestWatcher`, fallback) — S | **fixed**, residue | It is an `ExternalResource` that throws, drains shell output, polls `Resources.getSystem()` sw and density, then the real window (`getRealMetrics` / `maximumWindowMetrics`). The restore is observed. The taskbar is recorded, not assumed. Residue: the fallback gaps (E2) |
| F3 (harness first run at T&V) — S | **fixed**, residue | `HarnessScaffoldingTest` has one set-verify-restore case per helper, including the mid-process density change. The gate runs on both channels. The visible one-display walk and the visible `LanguageListDecisionAppTest` are in. Residue: the walk inside the gate is red until L-1 (E6) |
| F4 (REQ-013 ↔ REQ-037) — S | **fixed** | DA-100(ii) states the conflict at 549.3 dp, with split screen under F3 and the interim rule. CA-7(b) has two options, and it is owner item 1. The sweep moves to 560, and a non-asserting 500–549 printout is added |
| F5 (locale premise) — S | **fixed**, residue | `values-sv` is acknowledged, L-2 is dropped, DA-101 is rewritten, and the `localeFilters` owner option is in CA-8(a). Residue: the decision lists cannot show the one case where a mismatch could exist (E1) |
| F6 (word list) — S | **fixed**, residue | REQ-009/REQ-049 texts are exempt by key (`PROMISE_TEXT_KEYS`, empty until WO-007). The Finnish stems are inflection-safe, the pay/money roots are added, and titles are scanned against an allowlist. `tähti` and `tilaa` are dropped. Residue: E3 |
| F7 (font scale 2.0) — S | **fixed** | The no-clip check is at 1.0 only, with width and height including the top bar. 2.0 is used only for REQ-037, on one phone. `TestConfig.fontScale` is in the debug wrap with a delivering task (L-0b) |
| F8 (V-08 dex half) — S | **fixed** | Only SDK class definitions are checked, with no `ALLOWED_HITS` (0 hits today). Framework-type references are dropped. V-08 builds its own input, and `--apk` checks freshness. Release must not define the debug-only classes, and debug must define them (`--expect-debug`). Residue: E6 (the definition of a stale APK) |
| N1 (architecture §5) | **fixed** | DA-96 cites the §5 line, row 13 `ai+inform`, owner item 4 |
| N2 (release safety mechanised; globs) | **fixed** | V-08 denies and expects the debug-only classes. The L-0a seam uses globs, with no module names and no "devtools" |
| N3 (format arguments; lazy grid) | **fixed** | The `%2$s` / `%3$s` captures are checked against the expected language, and `gridDescriptions` scrolls to every index |
| N4 (margin row; DI departure) | **fixed** | The row is corrected (48.55 vs 49.32). `design-inputs.md` §2 gets the departure at the close, and CA-7(c) covers it |
| N5 (A2 on API 26; insets) | **fixed** | The rule-level report line is in. API 37 is named as A2's evidence. Both insets are logged, and `safeGestures` is the L-3 contingency |
| N6 (airplane restore; row 10) | **fixed** | Prior states are saved and restored. DA-105/106 say they are not a row-10 matter |
| N7 (tokens; target package) | **fixed** | C9 moves off `REQ-036.A2`, and C2's file is named. `PromiseSourceScanTest` is tagged `// guardrail G-01` with no token. The test queries `targetContext.packageName` |
| N8 (staffing; time; DoD) | **fixed** | L-0 is split into L-0a/L-0b. The time budget is about 25 minutes per channel, and the touch matrix is trimmed to about 36 launches. V-08 is in the DoD line and in build-map at the close |
| N9 (rotation; flip) | **fixed** | CR-F1 is cited. A rotate-during-the-solve-timeline decision case is added, and one parallelogram flip on a tablet |

### E-items (line edits; the orchestrator may apply them without another re-review)

- **E1 — §5, `LanguageListDecisionAppTest`, DA-101: add the one list that can discriminate.**
  - **Why the current lists cannot.** `sv-SE,fi-FI` resolves to `sv`, and `fi-FI,sv-SE` to `fi`. In both, `locales[0]` already *is* the resolved language, so neither can ever show "titles in another language than the buttons". The L-2 contingency is unreachable as written.
  - **Add `se-NO,fi-FI`.** Northern Sami is absent from all 85 packaged folders. It is also a real Finnish-user setting.
  - **Expected result.** The whole screen is Finnish. The title is Finnish only if the platform moves the resolved locale to `locales[0]`.
  - **This is the case that decides L-2.**
- **E2 — §1 fallback, the seam conventions: make "skips none" true.**
  - (i) Every display-dependent class is `@RunWith(Parameterized::class)` with `@Parameters(name = "{0}")` over `DisplaySpec`, one spec per case. There are no loops over displays inside a method; this covers the held-out touch matrix and `PlayThroughAppTest`.
  - (ii) The `DisplayRule` classes carry a marker annotation. The fallback's sixth call runs everything else on the native display with `notAnnotation=<marker>`.
  - (iii) The fallback proves completeness: the executed test counts of the six calls, taken from the JUnit XML, add up to the normal run's count on that channel.
- **E3 — §6 item 4: define match semantics and pre-empt the day-one hit.**
  - **Hard rule.** Mark each entry as whole word or word-start prefix: `ad`, `ads`, `tip`, `tips`, `fee`, `rate`, `stars`, `pay` as whole words, the rest as prefixes.
  - **What prefixes catch otherwise.** Without the rule, `ad` matches *add/adjust/address*, `fee` *feel/feed*, `cost` *costume*, `coin` *coincide*, `pay` *payload*, `maks` *maksimi* and `kaup` *kaupunki*. Each would become an allowlist entry later.
  - **Day-one hit.** Layer 1 (d) scans every `strings.xml`, `devtools` included, and today's `devtools_hint_unlocked` ("Unlocked until the app is restarted…") hits `unlock`. Either skip `devtools_` keys in the JVM scan, consistent with the walk's `dev-*` skip and REQ-037's DevTools exemption, or seed the allowlist with that exact string and its reason.
- **E4 — §1 / seam H-1: two loose ends in `device_reset.py`.**
  - **The nav-mode baseline is a per-AVD constant in the script**, never "recorded" from a device that may already have leaked:
    - for `Medium_Phone_API_37.0`, the mode observed on the clean AVD when H-1 is written;
    - for `Phone_API_26`, none.

    The "set by hand once" route in the Risks then updates that constant.
  - **The airplane reset is written per API level:**
    - API ≥ 30: `cmd connectivity airplane-mode disable`;
    - API 26: `settings put global airplane_mode_on 0` plus `svc wifi enable` / `svc data enable`.

    `--check` reads the flag on both levels, and the radio states on API 26.
  - (Minor) The self-test belongs beside the script's precedent, `tools/tests/` (as `test_check_apk_puzzles.py`), not in `.swdev/verifiers/test_verifiers.py`.
- **E5 — D-1 recipe and DA-108: the after-check is a hard gate.**
  - A non-clean `device_reset.py --check` after a device step **voids that step's results**. The step is re-run after a reset, and the leftover is recorded in the workorder.
  - This is the guarantee the F1(c) variation needs for the older `app` tests, which share the instrumentation run with the `DisplayRule` tests.
- **E6 — two small definitions.**
  - **(a) The walk in the gate.** `HarnessScaffoldingTest`'s visible `PlayerControlWalk` on `TABLET_600x960` is **red until L-1 lands** (47.78 dp). Inside the G-DISPLAY gate, that is a product breach blocking a harness gate. Either run the gate after L-1, or move the walk case to its own visible class (`// decision DA-99/DA-100`) outside the gate, stating "red until L-1".
  - **(b) A stale APK.** For V-08 `--apk`, "the newest tracked source file" is defined as the newest file in the set `PromiseSourceScanTest` declares as inputs, plus `Tangrams/*.json`. No git is involved.

**Spot-check verdict: forward**, conditional on E1–E6 being applied before the Planner briefs the Acceptance Test Author, H-1 and P-1.
- **E1 matters most.** Without it the mixed-list test cannot decide L-2, which is the question F5 left open.
- **E5 and E2 are needed next.** They are what make the F1(c) and F2 variations as safe as the review's original asks.
- **E3, E4 and E6 are small definitions that prevent a day-one false red.**

No new Blocker or Should-level design question remains. All 8 Shoulds and 9 Nits of rev 0 are resolved, with the residues listed above.
