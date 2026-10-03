# Review — design · WO-005

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:** `designs/WO-005-design.md` (Draft, 2026-10-03) · `workorders/WO-005.md` · AGENTS.md (rule 11, the seams/test-adapter and staffing lessons) · REQ-046 (in full), REQ-030, REQ-047; touched REQ-022, REQ-023, REQ-037 (collection locked) · `architecture.md` v1.0 (G-04, G-05, G-06, G-10, the V-04 row, the `devtools` row, O-01, O-06, O-07, O-09, §5 build types and test placement) · ADR-006 · `decisions.md` F5, F9, F18, DA-23, 27, 40, 46, 48, 52, 56, 65, 66, 70, 71, LOCK-V1 · `governance.md` rows 12–13 · `build-map.md` §1–2 · `.swdev/guard.json` · `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `kernel/` and `content/build.gradle.kts` (test-input precedent) · `app/src/main/**` (`MainActivity`, `AppViewModel`, `TangramApp` incl. `unplacedUnless`, `SessionHost`, `TrayRows`) · `play/.../{PlayArea,PlaySession,PlayLayout,DropResolver}.kt`, `play/.../draw/PlayDrawing.kt` · `kernel/.../{state/PuzzleStates,lock/SolvedCheck,lock/LockSearch.isValidPlacement,layout/TrayRules}.kt` · `browse/.../{BrowseController,SolvedBar(RestartButton),BrowseTopBar}.kt` and browse strings en/fi · `store/.../JsonProgressStore.kt` (writer line 189, `decodeEntry` line 352), `contracts/.../progress/SavedGame.kt`, `store/src/test/resources/fixtures/progress-v1*.json` (read only) · `.swdev/verifiers/{v01,v05,v06,v07,test_verifiers}.py` · `tools/check_apk_puzzles.py` · `tools/prototype_template.html` (DEV aid: `.devbtn`, `#devDlg`, `drawHint`, `devSolveNow`, `solve`) · `play/build/test-results/.../TEST-...CornerControlSweepScaffoldingTest.xml` (the recorded sweep) · the real `app-release-unsigned.apk` and `app-debug.apk` in `app/build/outputs/apk/` (built 2026-10-03 11:19 / 11:23, before WO-005) · style reference `reviews/WO-004-design-review.md`. `.swdev/heldout/` not read.

## Verdict

**recirculate → design-author.** The release-safety shape is right and well argued:
- the `DebugAids` source-set pair with one `debugImplementation` line;
- a validated `solveByAid` instead of a bare "mark solved";
- no v1 change (re-checked: writer, reader, KDoc and fixtures all already carry a Solved entry with a null best).

But the DEV pill's placement (DA-75) fails its own pinned gate on data already in the tree. The pair-width slot makes `animals-cat` reserve a strip on the 360 × 640 area, which is the real play area of the smallest reference phone. The pre-declared `Column` fallback fails on four puzzles there. By the design's own rule that is "stop and escalate" in the middle of D-1. V-04 is sound in principle, but its proof of "can see" covers only half of its markers, and only on the debug artifact.
**1 Blocker, 5 Shoulds, 7 Nits.**

## Checklist applied

- [x] **Directives.**
  - D1: `devtools` is the #DevTools slice; `play` gets neutral hooks; `app` joins them.
  - D2: proportionate, except the placement. The shared-slot design was picked over a second placement "for D2", but it cannot pass its own parity gate (F1), so the simpler-looking option is not the simpler working one.
  - D3: every abstraction names its REQ or rule:
    - `BoardSpace` and `boardOverlay`: O-09, REQ-046 rule 3;
    - `solveByAid`: G-04, REQ-046 A3;
    - `onSolved(byAid)`: O-06, REQ-046 A3, the WO scope line;
    - `DebugAids`: ADR-006;
    - `DevToolsState`, `DevSolution`, `DevPasscode`: REQ-046 rules 1–3, A1–A3.
  - D4: `onSolved(false)` is built without a production consumer. That is accepted, because the WO scope asks for the event-level check (DA-73 amends DA-27 openly).
  - D5: no new library. `Dialog`, `BasicTextField`, `KeyboardType.NumberPassword` and `PasswordVisualTransformation` arrive transitively through foundation (ui, ui-text).
- [x] **Guardrails.**
  - G-04: the leak paths are well covered (§7). V-04's proof of visibility: F2, F3. The guardrail's text vs DA-78: N1. A missing leak-path row: N7.
  - G-05: en+fi for every key; the Finnish rows are marked AI (wording: N6).
  - G-06: `devtools` main = `kernel` + `contracts`, and V-06 already lists it. `app` → `devtools` is debug-only, enforced by the separation test, not by V-06 (V-06 treats `debugImplementation` as main scope and allows `app` everything).
  - G-10: an unplaceable stored solution becomes a notice, not a crash (DA-80).
- [x] **Contract.**
  - No `I*` signature or KDoc changes.
  - `contracts/`, `kernel/…/model/`, `store/` and the locked fixtures are untouched.
  - Locked REQ-023's solve rules are bent for the aid solve without a recorded reading (F5).
  - Locked REQ-046's bottom-left ASSUMPTION is departed from without the capture-side CHG proposal that governance row 12 asks for (F1, owner item 1).
- [x] **Scope.** Everything traces to REQ-046 or the WO's V-04/fold line. DA-80's extra notice is reasoned (the REQ's purpose is to prove solvability); it is not invented scope.
- [x] **Traceability.**
  - Each of A1–A4 has a `devtools` covering test (rule 3).
  - A4's token test can replay a cached pass (F4).
  - A3's `devtools` token rests on an engine-level reading that is not stated (N3).
- [x] **Hard-stops.** v1 untouched; no security/PII; no locked path edited.
- [x] **Evidence re-derived.** See below. Two claims are wrong: "board rect identical" (F1) and "resource names are intact" (N2).
- [x] **The conceptual 20 %.** Spent on DA-75 (ported the layout and ran it), V-04 against the two real APKs (parsed their dex string tables and scanned every entry), the `solveByAid` → `persist` → v1 chain, and the REQ-023/REQ-046 readings.

**Re-derived:**
- **v1 untouched.**
  - `JsonProgressStore` writes `"bestSeconds"` as an explicit `JsonNull` (line 189), and `decodeEntry` reads a null best with no state coupling (line 352).
  - `SavedGame.kt` KDoc: "`null` … solved only by the DEV aid, REQ-046".
  - `ScaffoldJsonProgressStoreTest` line 119 reads a Solved entry with a null best.
  - `persist` = `canonical(session.toProgress(base))`, and `canonical` empties a Solved entry's pieces. So an aid solve stores `{"state":"solved","pieces":{},"puzzleSeconds":<kept>,"bestSeconds":<kept>}`, exactly as §5 says.
- **Release is not minified.** `app/build.gradle.kts` has no `buildTypes`, and the release dex still names `Lio/github/jamisuni/tangram/MainActivity;` etc.
- **The real release APK resources.**
  - Its resource *paths* are already shortened by AGP: `res/Xk.xml` in release, `res/drawable/ic_call_decline.xml` in debug.
  - Its arsc key pool is intact UTF-8: `best_time_none` and `restart` are present.
- **Dex shape.**
  - Release: 2 dex files, debug: **13** (`classes.dex` … `classes13.dex`).
  - All are `dex\n038\0` with header size 0x70.
  - `string_ids_size`/`_off` at 0x38/0x3C: the design's offsets are right.
- **Marker false positives today.**
  - Neither APK has the exact dex string `0417`, nor any dex string containing `0417` or `devtools`.
  - No non-dex entry of either APK holds `devtools` (ASCII or UTF-16LE).
  - `0417` does occur as raw bytes in two androidx vector drawables (`ic_call_decline*.xml`; `res/Xk.xml` and `res/x4.xml` in release). This **confirms** DA-78's choice not to search binary resources for it.
- **Precedent for test inputs.** `kernel/build.gradle.kts` and `content/build.gradle.kts` declare the files their tests read as task inputs, "without them org.gradle.caching=true could replay a cached pass" (WO-001 §10), and `gradle.properties` has `org.gradle.caching=true` (F4).
- **Prototype.**
  - The DEV pill sits at the board's bottom-left (`top: b.y + b.h - 38`), with Restart at the top-left.
  - `devSolveNow` places the pieces and then runs the full `solve()` (pop, confetti, fade). A `cheated` flag only suppresses the best time.

## Trajectory & quality

- **Verification actually run?** Partly.
  - The v1 claims were checked against code and fixtures, and they hold.
  - The parity claim was not run. The existing sweep's printout already shows a 200 × 48 control reserving a strip on 360 × 640 for `shapes-warmup-1` and `animals-cat`. The design's pair is 184 wide, and Finnish makes it wider (F1).
- **Proportionate?** Yes in structure: a small hook surface, no reflection, no new library, one verifier. The validated `solveByAid` costs a little code and buys a real G-04 property.
- **Path sane?** First pass. The author flagged the right two risks. The second one is where the design breaks.

## Findings

### F1 — DIRECTIVE · Blocker · design §3b, DA-75, the "Mitigation (pinned)" and "Pre-declared fallback" bullets, the D-1 sweep row / REQ-046 ASSUMPTION, DA-71, governance row 12
- **Observation:**
  - **The pair fails the pinned parity sweep on the existing size set.** I ported `PlayLayout.compute`/`computeWithCorner`/`touchesSilhouette` to Python. Puzzles from `Tangrams/*.json`, diameters from `TrayRules`. The port reproduces the recorded `CornerControlSweepScaffoldingTest` output row for row: 48 → `TOP_RIGHT [warmup-1]`, 120 → `BOTTOM_RIGHT [warmup-1]`, 200 → `STRIP [warmup-1, animals-cat]` on 360 × 640. Then:

    | slot (w × h, dp) | 360 × 640 area | 390 × 700 area |
    |---|---|---|
    | Restart alone, 120 × 48 (design's figure) | TL ×12, BR `warmup-1` | TL ×12, BR `warmup-1` |
    | pair, 184 × 48 (design's figure) | **STRIP `animals-cat`**, BR `warmup-1`, TL ×11 | **STRIP `animals-cat`** |
    | pair, English estimate 146 × 48 | **STRIP `animals-cat`** | TL ×12, BR `warmup-1` |
    | fallback `Column`, 120 × 92 | **STRIP `mini-2`, `square`, `warmup-1`, `warmup-2`**; BR `warmup-3` | **STRIP `mini-2`, `square`** |

  - For `animals-cat` on 360 × 640 the debug board starts 48 dp lower, and `dpPerUnit` drops from **54.88 to 48.48** (−12 %). So in debug the cat puzzle has a smaller board, and a different lock distance in units (`LockSearch.lockDistance(dpPerUnit)`), than the release build the children get.
  - Both the chosen design and its fallback fail. The design's own rule then says "stop and escalate; do not accept a debug-only layout", which would happen inside D-1.
  - **360 × 640 is the real case, not an edge.** On a 360 × 780 dp phone (decisions F9's smallest reference) the play area is 780 − status bar − navigation bar − the 66 dp top bar ≈ 640–660 dp.
  - **The sizes in the sweep are guesses.** `RestartButton` is 15 sp SemiBold text plus 2 × 16 dp padding: about 82 dp for "Restart" and about 118 dp for "Aloita alusta" (estimates from Roboto advance widths), not 120. The DEV width "56" is not derived. The pair is therefore about 146 dp in English and about 182 dp in Finnish, the players' language. Release corner choice already differs by language (`warmup-1`: TR at 82, BR at 118).
  - **The first frame lays out at the 120 × 48 default** (`PlayArea.DEFAULT_CORNER_W_DP`) until the slot is measured. So in debug the cat board visibly re-lays out one frame after every visit. That relayout also cancels any live gesture (the `SideEffect` layout-change path).
  - **Release layout is never exercised on a device.** Every device test runs on debug, and the API 26 release launch is waived (DA-43). With a shared slot, the device suites see a layout that does not ship.
  - **The ASSUMPTION is set aside on the wrong premise.** "Bottom-left collides with the silhouette on some puzzles" is the reason given for leaving REQ-046's bottom-left ASSUMPTION. Yet with the board laid out for Restart alone, a 56 × 48 pill clears the silhouette (4 dp) and the Restart rect at **bottom-left on 10–13 of 13 puzzles** on every size I ran (360 × 600 … 1280 × 700, 800 × 1100/1180). Some other corner is free on every puzzle except `shapes-square` on 360 × 600/620, and there Restart is already in a strip that has room beside it.
  - **A CHG proposal is missing.** Governance row 12 says that departing from a locked REQ's line means "decide, write the ASSUMPTION, file a capture-side DEF/CHG proposal". DA-75 files none.
- **Proposed resolution** (the author's choice; the constraint is what matters): **the DEV pill must not take part in the board's layout decision**, so debug and release boards are identical by construction and no parity sweep is needed. One shape that the numbers above support:
  - `PlayArea` keeps `computeWithCorner` for the primary `cornerControl` (Restart) only.
  - A second, layout-neutral slot (for example `auxCornerControl`) is placed *after* the layout is fixed:
    - bottom-left first (REQ-046 ASSUMPTION), then BR, TR, TL;
    - clear of the silhouette (`touchesSilhouette`, 4 dp) and of the primary control's rect;
    - if no corner is free, beside Restart in its strip when one was reserved;
    - as a last resort, at bottom-left over the board (an exempt testing aid, REQ-037 rule 3), never reserving space.
  - Pin it with a JVM sweep, `// decision DA-75`:
    - the board rect **with** the aux slot equals the board rect **without** it, for every puzzle × size;
    - report the corner chosen;
    - use the real size set: add the realistic phone areas (360 × 600–660, 390 × 700) and tablet landscape (1280 × 700).
  - Rewrite DA-75 and §3b.
  - File the capture-side CHG proposal for the ASSUMPTION ("bottom-left, or the next free corner when the silhouette occupies it"). It is much narrower than today's deviation.
  - Seam impact: `DebugAids.CornerButton` stays. `TangramApp` passes it to the new slot instead of the `Row`, and `PlayArea` gains one more trailing nullable slot parameter.

### F2 — TEST · Should · design §6 self-test table (last row) and the "orchestrator also runs the positive control" paragraph, Risks (1)–(3), DA-79 / G-04, A4
- **Observation:** V-04 is the only artifact-level proof of A4, and its positive control proves less than V-04 claims.
  - **Half the markers are unproven on a real artifact.** The row requires "a `devtools` finding and the `0417` finding". Nothing requires the `devtools_` key or the `Wrong passcode.` text to be found in the real `resources.arsc`. Risks (3) asks for exactly that ("confirm on the real debug APK that the marker is found"), but the table does not make it a pass condition. A wrong entry filter or encoding in the resource scan would pass every crafted fixture and every release build.
  - **The control runs on a different pipeline from release.** In the real release APK AGP already rewrites resources (`res/Xk.xml`), while debug keeps `res/drawable/…`. A release-only resource step (for example, collapsed key names) would blind the `devtools_` marker in release while the debug control still passes. Today the release key pool is intact (`best_time_none` present), but nothing pins that.
  - **Sequencing.** D-5 is declared independent of D-1…D-4. But `app/build/outputs/apk/debug/app-debug.apk` exists today without devtools. So the "skipTest when absent" row will *run* during D-5 and fail, and V-04's self-test cannot be green until D-4 lands. A stale pre-devtools APK at T&V would fail too. That is safe, but it is noise, and it invites someone to "fix" the test.
- **Proposed resolution:**
  - (a) The positive control requires one finding of **each** marker kind: dex descriptor (slash form), dex `0417`, arsc `devtools_` key, arsc `Wrong passcode.`.
  - (b) Add a **release-side canary**. On the scanned release APK, V-04 must also *find* known main-scope markers through the same code paths, or fail closed with "scanner blind":
    - a dex descriptor such as `Lio/github/jamisuni/tangram/MainActivity;`;
    - a known `app`/`browse` key in the arsc key pool (e.g. `best_time_none`);
    - a known English string (e.g. `Restart`).

    That proves visibility on the artifact that ships. It catches R8 renaming, key collapsing and packaging changes the day they happen, not at WO-009.
  - (c) Take the positive control out of the always-run self-test. Make it a V-04 mode (e.g. `--positive-control`) that runs `:app:assembleDebug` itself, after deleting the old debug APK, exactly as the release path does. Run it at T&V and at close, and name it on the close evidence line. The self-test keeps crafted fixtures only, so D-5 is truly independent.

### F3 — GUARDRAIL · Should · design §6 "Dex scan" and "Resource scan", the fold paragraph / G-04, V-04 row
- **Observation:**
  - **The resource scan uses an include-list** (`resources.arsc`, `AndroidManifest.xml`, `res/**`, `assets/**`). The real APKs also carry:
    - `META-INF/**` (57 entries in each);
    - `kotlin/**` (8);
    - root-level Java resources (5 in release, 16 in debug);
    - `lib/**` (4).

    A leaked library's Java resources land at the root or in `META-INF`, unscanned.

    Scanning **every** non-dex entry of today's release APK for the package forms and `devtools` (ASCII and UTF-16LE) finds nothing. A deny-list would have no false positives today.
  - **The dex version is unspecified.** "A dex with a bad magic is a finding" does not say which versions are good. Both APKs are `038`. A strict `035` check would fail closed on every build. A loose `dex\n` check would parse an unknown future layout as one table: a multi-section container file has a header per section, so the later sections would never be read, and that is a silent pass.
  - **Error mapping.** The folded `check_apk_puzzles.check()` raises `zipfile.BadZipFile` / `OSError`. The design gives V-04 exit codes, but not the mapping for exceptions out of the imported function.
- **Proposed resolution:**
  - Scan **all** zip entries except `classes*.dex` (parsed) and binary image/audio extensions (`.png`, `.webp`, `.jpg`, `.ogg`, `.so` if wanted) for the package forms, `devtools_` and `Wrong passcode.`. Keep `0417` dex-only, with the `ic_call_decline` evidence in DA-78.
  - Accept magic `dex\n03[5-9]\0` only. Any other version is "unparseable dex": a finding plus the raw-byte search.
  - Map every exception from the zip, the dex parser or the folded check to exit 2 (cannot read) or a finding. Never let a traceback produce an undefined exit code.
  - Add a self-test case for each of the three.

### F4 — TRACE · Should · acceptance table row REQ-046 A4 (`ReleaseSeparationTest`), DA-81, D-0 / AGENTS "test tokens are coverage claims", WO-001 §10 precedent
- **Observation:**
  - The A4 token test in `devtools/src/test` reads files outside its module:
    - every non-devtools `src/main` tree;
    - `app/src/release`;
    - the manifests and `res/`;
    - `app/build.gradle.kts`.
  - None of these are inputs of `:devtools:testDebugUnitTest`. With `org.gradle.caching=true` (gradle.properties) and the normal up-to-date checks, a later edit to exactly those files does not re-run the test. Examples: a D-4 rework, a code-review fix, a stray `0417` in `play`. The earlier green is replayed, from the cache even on a clean checkout.
  - So the test that carries REQ-046.A4 can report a pass for code it never read.
  - The project already learned this: `kernel/build.gradle.kts` and `content/build.gradle.kts` declare the Tangrams/golden files as test inputs for this very reason.
- **Proposed resolution:**
  - In `devtools/build.gradle.kts` (D-0), declare the scanned trees and build files as inputs of the unit-test task: `tasks.withType<Test>().configureEach { inputs.files(fileTree(rootDir) { include(...) }).withPathSensitivity(PathSensitivity.RELATIVE) }`, with the same include set the test scans, plus a `systemProperty` for the root as `kernel` does. Alternatively, mark the task never up to date and not cacheable.
  - Add a D-0 done-check: edit a scanned file, and the task re-runs.

### F5 — CONTRACT · Should · design §2b step 3, DA-83, the "What the aid-solve looks like" Alternatives row / REQ-023 (locked) Rules 1–2, governance row 12
- **Observation:**
  - REQ-023's Statement is "**WHEN a puzzle is solved**, the game SHALL replace the coloured pieces with the puzzle's stylised picture". Its Rules add the 600 → 1400 ms fade "after the last piece locks" and "a short celebration (a 4-note chime and confetti lasting 2.4 s) plays with it".
  - An aid solve *is* a solve; the design itself fires `onSolved`. DA-83 nevertheless shows the settled picture at once, with no pop or confetti. Its Basis cites REQ-046 A3, DA-66 and DA-52, and **not REQ-023**.
  - The prototype (DI-1) runs the full `solve()` after `devSolveNow`.
  - DA-66 does not carry over: it covers a *restore*, which is not a solve event.
  - The owner-list item "(b) settled picture without the celebration" does not tell Jami that it departs from a locked REQ's rules.
  - The departure is defensible:
    - the timing rules are anchored to "the last piece locks", which an aid solve never has;
    - it is a debug-only aid;
    - it is fast for a 13-puzzle walk.

    But it is a reading of a locked REQ, and it is unrecorded.
- **Proposed resolution:**
  - Either follow REQ-023: set `SolvedAt` from the next frame's clock, e.g. a pending `t0` resolved in `onFrame`, with `animating` true while pending. That is small and keeps one solve look.
  - Or keep DA-83 and record the reading in it, citing REQ-023: "REQ-023's fade and celebration are timed from the last piece locking; an aid solve locks no piece, so it shows the settled picture." File it under governance row 12 with a capture-side note, and name REQ-023 on the owner list.
  - Either way, add REQ-023 to the design's Inputs.

### F6 — CONTRACT · Should · design §3 rule 1, §3b `Row`, seam row `DebugAids.CornerButton` / decisions F5 (binding), O-09, WO-004 design review F8
- **Observation:**
  - F5: "while a piece is dragged, other controls ignore touches". O-09: `app` uses play's "a piece is being dragged" to make every other control ignore touches.
  - Restart obeys this through `BrowseController.restart()`'s `isDragging` guard.
  - The DEV pill is a sibling above the gesture box, like Restart. A second finger on it during a drag opens a `Dialog`, and the window change then cancels the drag. Not harmful, but against F5.
  - Nothing in `DebugAids.CornerButton(puzzle, solveNow, modifier)` can express the guard, and that seam is frozen now. This is the WO-004 `openGrid` gap (F8) again.
- **Proposed resolution:**
  - Add `blocked: () -> Boolean` (or `enabled: Boolean`) to `DebugAids.CornerButton` and `DevCornerButton`. `app` passes `{ session.isDragging }`, and a click while blocked is a no-op.
  - Add a `devtools` device or JVM row tagged `// decision F5`.

### N1 — GUARDRAIL · Nit · DA-78, §6 resource scan / G-04 text (locked `architecture.md`)
- G-04 says V-04 "scans its dex files **and resources** for … the passcode literal". DA-78 narrows that to dex-only.
- The narrowing is right, and the evidence is stronger than the design's prose: androidx's `ic_call_decline*.xml` drawables contain the bytes `0417` in **both** real APKs today. A literal reading would fail every release build.
- Log it as a G-04 reading under governance row 13 (ai+inform, checkpoint surface), with that evidence.

### N2 — QUALITY · Nit · design §7 "R8 / minification" row, Risks "R8"
- "Class and resource names are intact" is half right:
  - class names and the arsc key pool are intact;
  - release resource **paths** are already shortened by AGP without minify (`res/Xk.xml`).
- Correct the sentence. It is the reason the F2(b) canary matters: release already has a resource step debug does not.

### N3 — TRACE · Nit · acceptance table row REQ-046 A3 (`devtools` column)
- The `devtools` A3 tests assert that the stored-solution poses solve through the kernel, and that "Solve now" hands them to `solveNow` once. They do not assert "shows the solved picture" or "leaves the best time empty". That is legitimate as architecture §5 item 4's engine-level reading (like the REQ-045.A2 example), but the row does not say so.
- Write "engine-level reading (architecture §5 item 4): the poses `devtools` hands over solve the puzzle through the kernel; `devtools` touches no store or time", so the token audit does not flag it.

### N4 — QUALITY · Nit · design §2b "On `true` it" steps 2–3, Test seams value shapes
- Step 2 (the TYPE-006 transitions) is overwritten by step 3: `settle()` assigns `state = SOLVED` directly, as `restore` does for DA-66.
- State it as one step: "settles through `settle()`, which sets SOLVED as the DA-66 restore does; New → In progress → Solved is the TYPE-006 path this represents".
- Add the details an implementer can miss:
  - `invalidate()`;
  - clearing any `glides`/`pulse`/`shake` left from the live board.
- `SolvedCheck.isSolved` adds nothing once "exactly the puzzle's pieces, each once" holds; harmless, but say it is a belt.

### N5 — TEST · Nit · scaffolding list, `app` "pairwise disjoint for all 13 puzzles on the phone sizes"
- A device test through `MainActivity` runs at one emulator size, so "the phone sizes" cannot be swept there.
- After F1, this becomes the JVM sweep. Alternatively, host `TangramApp` in fixed-size boxes and name the sizes.

### N6 — QUALITY · Nit · design §4 Finnish (owner list)
- `devtools_hint_unlocked`: "Ratkaisun peitto" reads as "the solution's blanket". Suggest "Auki, kunnes sovellus käynnistetään uudelleen. Ratkaisu pysyy näkyvissä, kun selaat tehtäviä."
- `devtools_no_best_time`: "ei aseta parasta aikaa" is a calque of "set". Suggest "Näin ratkaistusta tehtävästä ei tallennu parasta aikaa."
- `devtools_solve_failed`: suggest "Pelimoottori ei saanut tämän tehtävän tallennettua ratkaisua paikoilleen."

### N7 — GUARDRAIL · Nit · design §7 leak table, the D-1 + D-4 review checkpoint
- One leak path has no row: devtools logic, such as reading `puzzle.solution` into poses, **re-implemented under another package** in `play`/`app` release code. Neither V-04 nor the separation test can see it, because no `devtools` word or `0417` would appear. Only review catches it, and G-04 names it explicitly.
- Add the row, and put "no release code derives placements from `puzzle.solution` except `devtools` (via `PieceGeometry.poseOf`)" in the brief for the mandatory D-1 + D-4 code-review checkpoint.

## Owner items (Jami)

Nothing here needs Jami before the redesign; F1–F6 can be decided under governance rows 12–13. For the WO-005 close (checkpoint surface):
1. **Where the DEV pill sits (DA-75, after F1).** REQ-046's ASSUMPTION says bottom-left. After F1 it can be bottom-left on most puzzles, and the next free corner where the picture fills that corner. That narrower deviation goes to him as a capture-side CHG proposal (governance row 12).
2. **The aid solve's look (DA-83 / F5).** A settled picture at once, with no chime or confetti, reads locked REQ-023 as applying to player solves only. Name REQ-023 when asking.
3. **One extra string (DA-80)**, "could not place the stored solution", and the AI-written Finnish rows (with N6's wording).
4. **G-04 reading (N1).** V-04 does not search resources for the passcode digits, because androidx's own drawables contain them.

## Handoff — Design Reviewer · WO-005
- **Scope:** REQ-046 A1–A4 (#DevTools, `devtools`) + V-04 with `check_apk_puzzles` folded in · governed: `IPuzzleLibrary` (consumed, locked), `IProgressStore` (existing save path; v1 untouched, re-verified)
- **Inputs read:** see the header (design Draft 2026-10-03; working tree after the WO-004 close; real APKs built 2026-10-03 before WO-005)
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-005-design-review.md`
- **Status:** recirculate → design-author (1 Blocker / 5 Shoulds / 7 Nits). One line each:
  - **F1 (Blocker):** the DEV + Restart `Row` fails its own board-parity gate. `animals-cat` reserves a strip on 360 × 640 (debug board −12 %), and the `Column` fallback fails on four puzzles. The pill must not take part in the layout decision.
  - F2: V-04's positive control proves only the dex markers, only on debug, and collides with D-5's independence. Needs all marker kinds, a release-side canary and a self-building mode.
  - F3: V-04's resource include-list misses `META-INF`/root/`kotlin`/`lib`; dex versions unspecified; exception mapping missing.
  - F4: the A4 token test reads undeclared files, so Gradle caching can replay a stale pass.
  - F5: DA-83 departs from locked REQ-023's solve rules without a recorded reading.
  - F6: the DEV pill ignores the F5 drag guard, and its frozen seam cannot express one.
- **Traceability delta:** none (review only). F1 changes a frozen `play` seam (one more `PlayArea` slot) and the D-1 sweep. F2 and F3 change V-04's CLI and self-test table. F4 adds build-file lines to D-0. F6 adds a parameter to the `DebugAids`/`DevCornerButton` seams.
- **Notes for next station:**
  - Fix F1 first; §3b, DA-75, the seam table, the scaffolding list and the D-1 cut change. The parity sweep becomes "board rect with the aux slot == board rect without it".
  - The reviewer's layout port is in the session scratchpad (`corner_port.py`, `dev_second_corner.py`). It reproduces the recorded sweep exactly; the author may re-run it on any candidate size.
  - Re-review can be a spot-check of §3b, §6, the seam/acceptance tables, the Alternatives rows and DA-75/78/79/81/83.

## Spot-check (re-review)

**Date:** 2026-10-03 · **Read:** `designs/WO-005-design.md` rev 1, every part marked "(rev 1)":
- the rev 1 paragraph;
- §0 and §1 (the `blocked` parameter);
- §2a (`secondaryCornerControl`) and §2b (`solveByAid` with the pending solve);
- §3 rules 1 and 6, §3b (placement, sweep, ASSUMPTION), §4 Finnish;
- §6 in full, §7, §8, §9;
- the Alternatives rows;
- the seam table, the test-input paragraph, the value shapes and the acceptance table;
- the scaffolding list, Risks, DA-73…DA-88 and the cut.

The orchestrator's rulings (F1 shape, F5 "follow REQ-023", no CHG for DA-75) were taken as given.

**Re-derived:**
- **`placeSecondary` on the layout port.** I implemented §3b's rule as written on `corner_port.py`:
  - 4 dp inset;
  - order BL, BR, TR, TL;
  - 4 dp clearance from the silhouette and from the Restart rect (measured size, Restart drawn or not);
  - inside the board;
  - then beside Restart in a strip, then compact 32 × 32.

  Run over 13 puzzles × the 9 sizes × Restart 82/118/170/230 dp: **468 cases, 0 null, and the compact step is never used.** BL 412, BR 39, TR 9, beside Restart in the strip 8. This matches §3b's table. The layout is computed from `cornerControl` alone, so board parity holds by construction.
- **Pending solve vs the code.**
  - `HitTest` returns null on SOLVED (HitTest.kt:26). `beginDrag`, `tapTray`, `flipTray` and `boardPiece` all refuse on SOLVED.
  - `PlayDrawing` keys the tray on `state` and the timeline on `session.solved` (lines 114–117), and nothing dereferences `solved` with `!!`.
  - With `solved == null`, `popScale` is 1, `pictureAlpha` is 0 and no confetti is drawn. That is exactly what `SolvedAt(t0)` draws at `sinceSolve = 0` (SolvedTimeline.kt:7–32).
- **Canary markers on today's real release APK.** All four are present, so there is no false "blind" today:
  - 165 dex strings under `Lio/github/jamisuni/tangram/play/`;
  - the exact `Lio/github/jamisuni/tangram/MainActivity;`;
  - `best_time_none` in the key pool;
  - `Restart` once in the value pool. It is our own string, between "…nge start" and "Retry".
- **A4 scan surface on disk.**
  - `app/build/intermediates/incremental/debug/mergeDebugResources/merged.dir/values/values.xml` already holds merged *library* strings (browse's `best_time_none`). After D-3 it will hold `devtools_*` and "Wrong passcode.".
  - Five merged `AndroidManifest.xml` files sit under `app/build/intermediates/`.
  - `Spec/prototype/tangram-prototype.html` and `tools/prototype_template.html` contain `0417`. That bears on E2.

**The three questions asked:**
- **(a) `solvePending`: no disagreeing frame and no touch window.**
  - **What the pending frame shows.** Between `solveByAid` and the next `onFrame` the session is `state = SOLVED`, pieces at their poses, `solved = null`. Everything keyed on `state` already shows the solved screen:
    - the tray, marks and badge are gone (DA-52/69);
    - `trayCellsPx` is empty;
    - the overlay slot, the DEV pill and Restart are hidden;
    - the solved bar is shown.

    Everything keyed on `solved` draws the first frame of a player solve. That is what a player solve shows too: `release()` sets SOLVED and `t0` in the same frame-loop call.
  - **Touches.** None can land: `HitTest` and every session intent refuse on SOLVED, and at the moment of the call the `Dialog` window holds focus.
  - **Retry and Next are live.** The solved bar's Retry/Next work during the pending frame and the 1.4 s timeline, as after a player solve. Both rebuild the session from the store, so a pending flag is simply dropped.
  - **One implementation trap.** `onFrame` returns early when `drag == null` (PlaySession.kt:220). The pending flag must be resolved, and `invalidate()` called, **before** that early return. The `SolveByAidTest` row (`solvePending` until `onFrame(1000)`, then `t0 == 1000`, no drag) catches the wrong order, so no edit is needed.
  - **Backgrounding.** If the app is backgrounded inside that frame, the timeline starts on return. Harmless.
- **(b) The fixed 56 × 40 dp pill, not font-scaled: acceptable.**
  - REQ-037 rule 3 exempts the #DevTools controls by name.
  - The WO-006 carry already excludes `dev-*` from the REQ-037 A1 walk.
  - The control is debug-only, and its accessible name comes from `devtools_button_description` ("Developer tools" / "Kehittäjän työkalut").
  - Fixing only the three-letter symbol "DEV" costs no readable content. The dialog's texts still scale.
  - The fixed footprint is what makes the placement sweep a proof.
  - Give the node `Role.Button` next to the description (trivial).
- **(c) Is the release canary robust? Yes in the safe direction, with two noise sources.**
  - A canary can only turn a run red ("scanner blind"); it can never make a leak pass. All four markers exist today.
  - `MainActivity` survives R8: it is manifest-referenced and kept by AAPT2's rules.
  - **First noise source:** the `play` package marker will disappear the day R8 is switched on. That is the correct alarm (the package marker is blind then), but §7's R8 row still says the other markers "would still catch it", as if V-04 would run on degraded. It will in fact fail until WO-009 adapts it (E4).
  - **Second noise source:** `best_time_none` and `Restart` are product strings. WO-008 takes over best-time display and may rename or move that key, and a wording CHG may change "Restart". Either would give a false "blind" that reads like a scanner fault (E4).

**Rev 0 findings:**

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (shared slot breaks parity) — Blocker | **fixed** | `secondaryCornerControl` + pure `placeSecondary`; the layout comes from the primary slot alone. Re-derived: 468/468 placed, BL in 412. Parity is pinned twice (JVM value equality, device `BoardTransform` equality). Residue: the compact step 3 (E3); DA-75's "no CHG" (open item O1) |
| F2 (positive control too narrow) | **fixed**, residue | `--positive-control` builds a fresh debug APK and needs all four kinds; release canary; self-test on crafted fixtures only, so D-5 is independent. Residue: E1 (negative fixtures can fail for the wrong reason now that a canary exists) |
| F3 (scan surface, dex versions, exceptions) | **fixed** | Deny-list over every non-dex entry except images and `tangrams/`; magic `dex\n03[5-9]\0` and header 0x70, anything else fails closed (`040` case); every exception mapped; one self-test case each |
| F4 (A4 test cache replay) | **fixed**, residue | Declared inputs, `repo.root`, and a D-0 "edit a scanned file → re-runs and fails" done-check. Residue: E2 (the scan set as worded reaches into `build/`) |
| F5 (REQ-023 reading) | **fixed** | DA-83 replaced. The aid solve runs the REQ-023 timeline from the next frame's clock, and only the best time is withheld. (a) above |
| F6 (drag guard) | **fixed** | `blocked: () -> Boolean` on both seams, `{ session.isDragging }`, `// decision F5` rows in `devtools` and `app` |
| N1 (G-04 reading) | **fixed** | DA-85, governance row 13, owner list |
| N2 (§7 "names intact") | **fixed**, residue | Corrected. The R8 sentence now contradicts the canary (E4) |
| N3 (A3 engine-level reading) | **fixed** | Stated in the A3 row |
| N4 (`solveByAid` steps) | **fixed** | State through the TYPE-006 transitions (no direct set any more), left-over animations cleared, `invalidate()`, `SolvedCheck` named a belt |
| N5 (app sweep not runnable) | **fixed** | The sweep is JVM; the app device test checks the emulator size only |
| N6 (Finnish) | **fixed** | The three rows reworded as suggested |
| N7 (re-implemented logic) | **fixed** | §7 row and the D-1 + D-4 checkpoint brief |

**Line edits.** The orchestrator may apply these without re-review, as in the WO-004 spot-check.
- **E1 — §6 self-test table: each negative case must fail for its own reason.** With the canary in `scan_apk`, a minimal fixture that lacks the canary markers exits 1 as "scanner blind" whatever else it contains. So the rows "devtools descriptor → 1", "`0417` → 1", "`Wrong passcode.` → 1", "deny-list entries → 1" and "bad dex → 1" would pass even if their detection were broken. The same goes for the 0-expected puzzle-match fixture, which would wrongly exit 1.
  - Write: *every fixture is the clean, canary-complete release-like base plus exactly one change.*
  - *Each negative case asserts its expected `V-04 FAIL <kind>` line, and that no `scanner blind` line is printed.*
  - *The canary row asserts `scanner blind` and nothing else.*

  This is the one edit with a safety consequence: without it, the self-test cannot show that each marker detector can fail.
- **E2 — the A4 test's scan set and its declared inputs (§Test inputs paragraph, A4 row).** "Every `AndroidManifest.xml` and `res/` outside `devtools`", if implemented as a recursive glob, reaches `app/build/intermediates/…/merged.dir/values/values.xml`. After D-3 that file holds `devtools_*` and "Wrong passcode.", so the test false-fails, and the five merged manifests there would be scanned too.
  - Name the set exactly:
    - `<m>/src/main/**` for `m` in `app, play, browse, kernel, contracts, content, store` (Kotlin, `AndroidManifest.xml`, `res/**`);
    - `app/src/release/**`;
    - `app/build.gradle.kts`, `settings.gradle.kts`.
  - Exclude `build/`, `.gradle/` and `.swdev/`.
  - Use the same set for the declared inputs.
  - `Spec/` and `tools/` (the prototype holds `0417`) stay outside it.
- **E3 — drop §3b step 3 (the compact 32 × 32 last resort)** and fix the `placeSecondary` value-shape line to match.
  - It is used in 0 of 468 cases. The never-null sweep assertion already hands any future case to the author.
  - It contradicts the frozen `DevCornerButton` seam ("touch box exactly 56 × 40 dp").
  - Nothing tells the slot content to draw compact: under 32 × 32 constraints a fixed-size pill would simply be squeezed (D4).
  - If the author keeps it, the seam must state how the content learns its size.
- **E4 — the canary's noise (§6 item 5, §7 R8 row, the WO-009 carry).**
  - §7: "with the canary, switching R8 on makes V-04 fail `scanner blind` (the `play` package is renamed) until WO-009 adapts it (mapping-based de-obfuscation, or a keep rule for one marker class)". Add the same to the WO-009 carry.
  - Prefer `app_name` (in `app/src/main/res`, manifest-referenced, so it survives shrinking) over `best_time_none` for the key canary.
  - Have the `scanner blind` line name the canary list in `v04_release_apk.py` as the place to update after a legitimate rename.

**Open item (orchestrator, not a design edit):**
- **O1 — DA-75 "no CHG" vs governance row 12.** Row 12 reads "decide, write the ASSUMPTION, **file a capture-side DEF/CHG proposal**". The ruling skips the last step. The departure is now small (bottom-left in 412 of 468 cases, elsewhere only where geometry forces it), so that is defensible. But the decisions row should say why row 12's proposal step does not apply. Rev 1's handoff also removes the pill's corner from Jami's list ("closed by the F1 ruling"). Keep one informational line on the checkpoint surface: "DEV pill: bottom-left unless the picture or Restart is there, then the next free corner". Then the owner sees every departure from a locked REQ line.

**Spot-check verdict: forward**, conditional on E1–E4 being applied before the Planner briefs the Acceptance Test Author and D-5. E1 matters most: it decides whether V-04's self-test proves anything. E2 would otherwise appear as a false failure right after D-3. E3 and E4 are D4 and wording. No new Blocker or Should-level design question remains. F1–F6 and N1–N7 are resolved. The answers to (a)–(c) need no change beyond E4 and the `Role.Button` remark in (b).
