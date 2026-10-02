# Review — design · P2 architecture package (G2, governance row 4 = ai+inform)

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (Design Reviewer)
**Inputs:** `architecture.md` v0.9 (draft) · `adr/ADR-001…006` (2026-10-02) · `build-map.md` v1.0 · `design-inputs.md` v0.2 · `decisions.md` (rows 2026-10-02) · `governance.md` v0.2 · `.swdev/guard.json` + `contract-baseline.json` (frozen 2026-10-02) · code: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `kernel/**`, `contracts/**`, `app/**` · Contract: `Requirements/` v1.1 (`requirements.md`, `features.md`, `req_types.md` v0.2, all 47 live `reqs/REQ-*.md`, `views/digest.md`), `Tangrams/puzzle.schema.json` (`tangram-puzzle/1`) + the 13 puzzle files (solution coordinates only) · `req_review_01.md` "Triage outcome" section only · framework: principles v0.5, directives v0.3, gates-and-autonomy v0.6 §1–2/§8, guardrail-authoring v0.1, interface-definition v0.1, traceability-rules v0.3 §5, review-report template, handoff-contract v0.2, `trace_check.py` v0.4 (rule-3 and glob code read).

## Verdict

**recirculate → P2 architecture author (orchestrator)**. Nothing here is a Contract or hard-stop matter for the owner, and nothing in the locked tier is wrong enough to block. But eight Should findings land in text that is about to become **locked**: G-01, G-03, G-04 and G-06/V-06, §3b, §5, and the build-map sequence. Three of them (F3, F4, F6) would otherwise surface as hard-stops or untestable acceptance in WO-001…005. All eight are edits to the documents, not a redesign. After that a spot-check of F1–F8 is enough; the Notes need no re-review.
**0 Blockers, 8 Shoulds, 6 Notes.**

## Checklist applied

- [x] **Directives.** D1: the module-per-home isolation is sound, but the cross-slice seams are missing (F2) and so is test placement (F3). D2: ADR-002 is defensible (N9). D3: both contracts were checked member by member and nothing in them is unrequested (N10). D4: modules are created lazily, the profile abstraction is not built (F19) ✓. D5: every dependency is justified (ADR-004, decisions G3 row) ✓. D6 applied as the testability of acceptance (F3).
- [x] **Guardrails.** The existing code violates only G-01: the skeleton's merged release manifest has an injected permission (F5). That is expected because V-01 is not wired yet. G-03 and G-04 as written are not checkable or contradict themselves (F8, F6).
- [x] **Contract.** No locked REQ or TYPE was touched: the trace-check baseline is clean for `Requirements/`. The governed marker convention (prefix `I`, `contracts` module, banner) fits principle 13. The kernel types that carry contract meaning are unguarded (F7). The banners and the registry "Serves" column disagree (N10).
- [x] **Scope.** Every component and contract member traces to a live REQ or to a G1 reading in `decisions.md`. Nothing unrequested was found.
- [x] **Traceability.** Every one of the 47 live REQs maps to a module (component map) and every feature resolves to a code home or a deliberate `—` (trace-check: no mapping failures). One cross-feature dep is missing (N11). Rule-3 feasibility for cross-slice acceptance is open (F3). Test tokens: none yet (expected).
- [x] **Hard-stops.** No security or PII is involved: no network, backup is off (G-01). Migration is declared (G-09, governance row 11). Hidden migration traps: F7, N14.
- [x] **Evidence re-derived.** `gradlew assembleDebug test` → BUILD SUCCESSFUL (46 tasks). A forced `--rerun-tasks :kernel:compileKotlin :contracts:compileKotlin :app:processReleaseMainManifest` → BUILD SUCCESSFUL, so the contracts compile standalone. `trace_check.py --project .` → RED only for uncovered acceptance of the 47 locked REQs. validate.py reports 0 errors, views are fresh, the build-map delta was re-frozen and the governance profile is in force. The merged release manifest was read (F5).
- [x] **The conceptual 20 %.** Most of the effort went here: who writes which stored field (F1), how slices meet on one screen (F2), whether each acceptance ID can be tested where rule 3 demands (F3), whether each WO's REQs need a later WO's code (F4), and what the release build may contain (F6).

## Trajectory & quality

- **Verification actually run?** Yes. The G3 dry run claimed in ADR-001 and decisions.md reproduces here. The verifiers do not exist yet, which is declared.
- **Proportionate?** Yes, overall. There are two governed interfaces, ten guardrails each traced to a rule, and modules created on first need. The cost of ten modules is real but bought deliberately (N9).
- **Path sane?** Not applicable: P2 has no WO log. decisions.md records the one inline exception (the skeleton) honestly.

## Findings

### F1 — CONTRACT · Should · `contracts/.../progress/SavedGame.kt` (`PuzzleProgress`), `IProgressStore.kt` (`saveProgress`, `resetAllProgress`), `architecture.md` §3b, `adr/ADR-005`
- **Observation:** The data shape carries every REQ-025/029/030/031/033/034 need and the readings F2, F4 and F10. The write model does not hold together, for three reasons:
  - (a) `PuzzleProgress` is one record with two owners. `play` owns `state` and `pieces`; `time` owns `puzzleSeconds` and `bestSeconds` (O-04). `saveProgress` replaces the whole record, so whichever slice writes from a stale copy erases the other's fields.
  - (b) Restart and Retry live in `browse`. `PuzzleProgress.NEW` (with `bestSeconds = null`) is the obvious value to write, and writing it erases the best time, against REQ-030 ("Restart and Retry reset the running time, not the best time").
  - (c) `resetAllProgress()` (REQ-034, called from `settings`) has no change signal. The next save from `time` (its `PlayTime` accumulator) or `play` (its board copy) quietly brings the erased data back.
  - The save cadence for times is also unstated. Saving every active second means two synchronous whole-document writes per second on the main thread, including during drags (ADR-005).
- **Proposed resolution:** Pick one of two options and write it down now. This is the notify tier, but fixing it before WO-004 is cheaper.
  - (i) Add an ownership rule O-08: every store write is a read-modify-write of the current store value inside one main-thread event; no slice keeps a store record across events; after `resetAllProgress` the `app` re-initialises `play`, `time` and `browse` from the store.
  - (ii) Shape the contract by owner: `saveBoard(puzzle, state, pieces)` for play/browse, `savePuzzleTime(puzzle, seconds, best)` for time, or one `update(puzzle) { … }`. Add a `PuzzleProgress.restarted()` that keeps `bestSeconds`, for Restart/Retry.
  - Also state the time-save cadence in ADR-005, either per event + `onStop` or per second, with the cost accepted.
  - Name two tests: "reset while a puzzle is in progress, then lock a piece and run 2 active seconds: nothing erased comes back"; and "Restart/Retry keep the best time".

### F2 — DIRECTIVE (D1) · Should · `architecture.md` §3 component map / §3b ownership rules
- **Observation:** G-06 forbids slice-to-slice dependencies, but several locked behaviours put the output of two slices on one surface, and no rule says how they meet:
  - REQ-026's solved view: the picture is `play`'s (#SolvedPicture), while the Retry/best/Next row is `browse`'s and replaces `play`'s tray.
  - REQ-050's grid (`browse`) draws silhouettes and solved pictures, and both renderers are `play`'s.
  - REQ-031's timer (`time`) sits in the board's top-right corner (`play`).
  - REQ-046's overlay (`devtools`) is drawn in board coordinates (`play`).
  - REQ-032's gear (`settings`) sits in `browse`'s top bar.
  - Decisions F5 says that while a piece is dragged every other control ignores touches, which spans all slices.

  Without a rule, WO-003/004 will either duplicate the picture renderer (SVG-path parsing plus clipping, so REQ-023.A1 and REQ-039.A1 would need testing twice) or reach across slices.
- **Proposed resolution:** Add O-08 (screen composition). `app` owns the scaffold. Slices expose composables with slot and callback parameters (AI-owned seams), and `app` passes cross-slice drawing into them. For example, `browse`'s grid takes a `thumbnail` composable wired to `play`'s silhouette/picture drawing; `play`'s board takes overlay slots for the timer and DEV overlays; `play` exposes "dragging", which `app` uses to disable the other controls. If duplicating the renderer is preferred instead, record it as the D1 choice and say which tests are doubled.

### F3 — TEST · Should · `architecture.md` §5 Verification, G-06 / V-06, `adr/ADR-002`
- **Observation:** Rule 3 puts each covering test in the REQ's feature module, and G-06 limits what that module can see. As written, several acceptance IDs therefore have no honest home:
  - REQ-002.A1 (`play`, "every puzzle can be completed") needs the real library, but O-02 forbids `play` from reading files and G-06 forbids depending on `content`.
  - REQ-045.A1/A2 (`content`, a JVM module) speak of the tray and the solved picture. Those live in Android modules that a JVM module cannot depend on, even for tests.
  - REQ-025.A3 (`browse`) needs the lock-validity check from the kernel and the board.
  - REQ-032.A1 (`settings`) says "nothing changes on the board".
  - REQ-046.A3 (`devtools`) needs the solved picture and an empty best time.

  Nothing says whether `testImplementation(project(…))` counts under V-06, or how cross-slice criteria are verified.
- **Proposed resolution:** Add a test-placement rule to §5 with four points:
  1. Each acceptance ID's covering test lives in its feature's module and drives that module's code, with contract fakes for the rest.
  2. Android slices may `testImplementation(project(":content"))` to run on the real puzzles. V-06 checks main-scope dependencies only; say so.
  3. Behaviour that spans slices also gets an `app/src/androidTest` test carrying the same token.
  4. Engine-level readings are stated where a JVM module cannot see the UI. For example, REQ-045.A2 under `content` reads "locking the three stored pieces through the kernel solves the puzzle, and the puzzle has a picture". That puts the trivial solved check (REQ-022) in `kernel`, next to O-01.

### F4 — QUALITY · Should · `build-map.md` §2 (WO sequence)
- **Observation:** There are dependency holes against the re-ordered sequence:
  - (a) WO-002 (#Content) takes REQ-045, whose `depends: [TYPE-001, REQ-012]` names #Tray (WO-003). Its A1 needs the tray and the fresh-install opening (WO-004, `lastShownPuzzle`); its A2 needs the solved picture (WO-003). Decisions F20 says `depends:` edges are honoured in WO scoping.
  - (b) WO-001 (#Locking) takes UI-level REQ-021 (dashed outline) and REQ-051 (pulse, reduced motion), plus REQ-020's 180 ms return and soft sound. It has no #Board or #Drag (WO-003) and no sound (WO-007).
  - (c) WO-003's REQ-013 ("every tray row … fits", A1 on 360×780 dp) needs REQ-035/036's tray rows and the TYPE-007 layout class through O-05, which are WO-006. The "first playable APK" needs the `app` shell (edge-to-edge, insets, portrait policy) anyway.
  - (d) REQ-046.A3 ("leaves the best time empty") passes vacuously at WO-005 because #PlayTime only arrives in WO-008.
- **Proposed resolution:** Give §2 a per-WO acceptance-ID scope, with carried-over IDs written down:
  - WO-001 = REQ-019 (all), REQ-020.A1, and REQ-021.A1–A2 and REQ-051.A1–A2 at engine/event level; the visible parts are verified in WO-003.
  - Move the TYPE-007 layout class, the tray-row rules and a minimal shell into WO-003, or put #Layout before it.
  - Move REQ-045.A1/A2 to WO-003/004.
  - Add a regression to WO-008 for the aid-solve rule (REQ-030 and REQ-046.A3).

### F5 — GUARDRAIL · Should · `architecture.md` G-01 / V-01, §5 Verification
- **Observation:** G-01 is feasible but under-specified. Re-derived today: the merged release manifest (`:app:processReleaseMainManifest`) contains `<permission …DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION protectionLevel="signature">` and the matching `<uses-permission>`, both injected by androidx.core. It has no `allowBackup` and it includes `androidx.emoji2…EmojiCompatInitializer`. Four problems follow:
  - (1) V-01 counts only `<uses-permission>`. In Android terms it is the `<permission>` element that *declares* a permission, which is REQ-010.A2's word.
  - (2) Removing the injected entry is safe only if nothing calls `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` on API 26–32. There, androidx.core checks that the app holds this permission and throws if it does not. §5 lists only an API 37 emulator image, where that path is never taken.
  - (3) The DoD command `gradlew assembleDebug test` never merges the release manifest or builds the release APK, so as written V-01 and V-04 have nothing to inspect.
  - (4) "Reported, not blocking until WO-006" leaves a violation that already exists unenforced for five WOs. The owner's debug installs from WO-003 also back up to the cloud, against decisions F7.
- **Proposed resolution:**
  - V-01 checks for zero `<permission>`, `<uses-permission>` and `<uses-permission-sdk-23>`.
  - Remove both injected nodes, and set `allowBackup="false"` plus extraction rules, in the main manifest in WO-001. V-01 is blocking from WO-001.
  - The verifiers run `:app:processReleaseMainManifest` and `assembleRelease` themselves.
  - Add an API 26 (≤ 32) emulator image to the verification channel and launch the release build on it. This also backs decisions F9 (minSdk 26).
  - Optional: remove `EmojiCompatInitializer`. Its default configuration can make Play services download a font, so removing it makes "no dependency does networking" literal.

### F6 — GUARDRAIL · Should · `architecture.md` G-04 vs `adr/ADR-006`, REQ-030, REQ-046
- **Observation:** G-04 allows "no passcode, reveal or solve-now code anywhere else" than `devtools`. Solve-now still has to set `play`'s board to the solution and mark the puzzle solved. REQ-030 ("a puzzle solved by the aid sets no best time") requires `time`, which is release code, to know that a solve came from the aid. ADR-006 itself relies on a "solve-now callback". WO-005 will therefore hit a guardrail conflict (hard-stop 4). Separately, V-04 ("no `0417`") is ambiguous: dex files are compressed inside the APK, so a raw grep finds nothing, and an int `417` escapes a string search.
- **Proposed resolution:** Reword G-04. Release code may contain neutral hooks: "place these piece poses and finish", and a `byAid` flag on the solve event (O-06). It may not contain the DEV button, the passcode, the overlay, or any code that reads stored solutions to place pieces. Specify that V-04 unzips the APK and scans the dex and resources for the `…tangram.devtools` package and the passcode literal. Keep the passcode as a string constant inside `devtools`.

### F7 — CONTRACT · Should · `architecture.md` §4 (closing note), `kernel/.../Exact.kt`, `kernel/.../Pieces.kt`, `.swdev/guard.json`, `SavedGame.kt` (`PieceSave.OnBoard` doc)
- **Observation:** The locked `IPuzzleLibrary` and the notify `IProgressStore` are built on kernel types (`ExactPoint`, `Q2`, `Rational`, `PieceId`, `Turn`, `PuzzleState`). The saved board positions also depend on the local-shape and origin convention, which `OnBoard` cites from `Spec/03` §3, a design input. §4 says that changing their meaning is a Contract change, but no file involved is in `contract_paths`, so neither the guard nor trace-check can see such a change. G-09's migration trap thus sits in AI-owned files: a different origin or turn direction in the kernel silently re-interprets every saved board.
- **Proposed resolution:**
  - Put only the declarations of the contract-referenced kernel types, plus the local-shape table, in dedicated kernel file(s) under `contract_paths.notify`, so deltas are logged at little cost.
  - Put the arithmetic in other files, so that WO-001 work does not trip deltas.
  - Point `OnBoard`'s doc at the kernel table instead of `Spec/03`.
  - Pin the table with a test against `tools/tangram_geom.py` (see F8).
  - From the first WO that writes the store, keep a frozen v1 saved-document fixture that must always load identically. This is G-09's migration-test baseline.

### F8 — GUARDRAIL · Should · `architecture.md` G-03; REQ-038.A1 test route
- **Observation:** G-03 says "the kernel must agree with `tools/tangram_geom.py` on every puzzle file" but names no mechanism, and JVM tests cannot import Python, so the guardrail cannot be checked as written. REQ-038.A1 ("every puzzle file passes the validator"; decisions F15 makes the G1 validator the reference) also needs a covering test under `content/`, and nothing says whether that test ports V1–V12 or runs the Python validator.
- **Proposed resolution:** Name the route. Either a `tools/` script writes per-puzzle golden data (outline corners, areas, V-rule verdicts) to a committed JSON, regenerated by the AGENTS rule-10 run, and a content/kernel test compares against it; or a content test runs `python tools/validate_puzzles.py`, and Python becomes a recorded build prerequisite.

### N9 — DIRECTIVE (D2) · Note · `adr/ADR-002`
- **Observation:** The decision is sound. It makes D1 isolation mechanical (the import-lint hook is still only planned), it satisfies rule 3 naturally, and ADR-006 needs a debug-only module anyway. The answer to "gold-plating?" is no, given the lazy creation. Three details are off:
  - (a) The decision says "one module per code home", but `contracts` and `store` are modules that are not code homes. Neither appears in build-map, and neither is justified in the ADR.
  - (b) "A package directory inside one `app` module can never contain its tests" is overstated. Per-slice source roots (`play/main` and `play/test` in one module) would pass rule 3 with one build file. The real decider is compile-time isolation, and that alternative is missing from the table.
  - (c) The per-module boilerplate (bytecode 17, compileSdk/minSdk) is repeated in every build file.
- **Proposed resolution:** Add the missing alternative and the reason it lost. Justify `contracts` (a guarded path) and `store` (JVM-testable, makes O-03 mechanical), or fold `store` into `app`. Put the shared module configuration in one root convention block.

### N10 — CONTRACT · Note · `contracts/.../puzzle/IPuzzleLibrary.kt`, `Puzzle.kt`, registry row
- **Observation:** The shape is complete for every consumer the REQs imply:
  - `play`: polygons give the silhouette, the anchors and the tray ids.
  - `browse`: order, titles, rating, and the index as counter.
  - `devtools`: polygons and ids.
  - content checks: `kind`, `category`, `reviewedByHuman`, polygons.

  Nothing in it is unrequested, and I found nothing that forces an early locked-tier change. Four details:
  - (a) All 13 shipped files store polygons only. Solve-now (REQ-046), the build-order test (REQ-038.A2 / F15) and REQ-002.A1's play test all need the pose (turn, mirror, `at`), which today must be derived by each consumer.
  - (b) The doc of `puzzle(id)` cites decisions F4 but no REQ.
  - (c) The registry "Serves" column omits REQ-022/023, which the `Puzzle.kt` banner lists, and REQ-012/046, which the `SavedGame.kt` banner lists.
  - (d) "Every packaged file passes the build's checks" names no check.
- **Proposed resolution:**
  - Derive the pose once in `kernel` (e.g. `poseOf(SolutionPiece)`) and record it in O-01, so that no consumer ever "needs" a locked change for it.
  - Cite REQ-025 on `puzzle(id)`.
  - Align the registry with the banners; the registry is normative.
  - Name the check: the content unit test over `Tangrams/*.json` in `gradlew test`.
  - These are wording fixes. Make them before the freeze, because afterwards even a doc change is locked tier.

### N11 — TRACE · Note · `build-map.md` §1 (#Settings row), registry "Between"
- **Observation:** `settings` lists best times per puzzle by title (REQ-030 rule, REQ-032 contents), so it needs `IPuzzleLibrary`. The #Settings deps list only #PlayTime and #Browsing, and #Settings → #PlayTime names no `I*` (rule 4). The registry's `IPuzzleLibrary` "Between" column omits `settings`.
- **Proposed resolution:** Add `#Content (IPuzzleLibrary)` to the #Settings deps. Rewrite #PlayTime as "(IProgressStore: play time)" or drop it. Add `settings` to "Between".

### N12 — QUALITY · Note · registry Sign-off cells, ADR "Status: Accepted", `build-map.md` header, `.swdev/guard.json`
- **Observation:** The sign-offs ("AI, 2026-10-02"; build-map "signed with architecture.md at G2") were written before this review, while `architecture.md` still says "Approved by: —". In guard.json, `_at_G2_add_to_locked` says `Contracts/**` (capital C); the real paths are lower-case.
- **Proposed resolution:** Set the sign-offs when G2 actually passes, quoting this review's verdict. When guard.json is updated, use the real lower-case paths.

### N13 — SCOPE · Note · `kernel/**` written ahead of WO-001
- **Observation:** This is acceptable, not scope creep. The interface-definition DoD needs contracts that compile standalone, and `Exact.kt`/`Pieces.kt` hold only the types the contracts reference (plus `Rational.compareTo`/`toDouble`). The code was written inline by the orchestrator and has no checker yet. ADR-003 says overflow is "asserted", but `Rational.of` does not guard `Long.MIN_VALUE`, where `abs` overflows.
- **Proposed resolution:** Give it WO-001's code review as the decisions.md row promises, and add the guard there.

### N14 — CONTRACT · Note · `adr/ADR-005` (G-09)
- **Observation:** A hidden migration trap: "An unreadable document starts fresh", and the next save then overwrites it. That includes a document whose `version` is newer than the app knows, for example when the owner sideloads an older debug APK over a newer one. Progress is wiped silently. Separately, if store DTOs serialise kernel enum names (`IN_PROGRESS`, `LT1`), a kernel rename becomes a format change.
- **Proposed resolution:** Move an unreadable or newer document aside (e.g. `progress.json.bad`) before starting fresh, and never overwrite a newer version. Store DTOs use explicit stable serial names, independent of kernel identifiers.

## Handoff — Design Reviewer · P2 architecture (G2)
- **Scope:** all 47 live REQs (architecture-level) · governed touched: `IPuzzleLibrary` (locked), `IProgressStore` (notify): reviewed, not changed
- **Inputs read:** `architecture.md` v0.9 · ADR-001…006 · `build-map.md` v1.0 · `design-inputs.md` v0.2 · `decisions.md` (2026-10-02 rows) · `governance.md` v0.2 · guard.json / baseline 2026-10-02 · `Requirements/` v1.1 (47 REQ files, features.md, req_types.md v0.2, digest) · `puzzle.schema.json` tangram-puzzle/1 · kernel/contracts/app sources · framework docs as listed in Inputs
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\P2-architecture-review-01.md`
- **Status:** recirculate → P2 architecture author (apply the F1–F8 resolutions in `architecture.md` §2/§3b/§5, `build-map.md` §2, the ADR-002/005/006 wording and guard.json; spot-check after that, no full re-review)
- **Traceability delta:** none (review only; no REQ↔code↔test links changed)
- **Notes for next station:** Nothing needs the owner. No locked REQ/TYPE is affected and nothing touches hard-stop 1. Before G2 is signed, fix the wording of the locked-tier files (`architecture.md`, `IPuzzleLibrary.kt`, `Puzzle.kt`: F5, F6, F8, N10); afterwards each edit needs the unlocked hatch. Evidence: the build is green and trace-check is RED only for uncovered acceptance.

## Spot-check — review 01 fixes (2026-10-02, same reviewer, fresh read)

**Read fresh:** `architecture.md` v0.10, `adr/ADR-002`, `adr/ADR-005`, `adr/ADR-006`, `build-map.md` v1.1, `contracts/**` (4 files), `kernel/.../kernel/model/**` (2 files).
**Re-derived:** `gradlew assembleDebug test` → BUILD SUCCESSFUL. No stale imports of the old kernel package. `trace_check.py` → RED, from uncovered acceptance (expected) **plus one new failure: `build-map.md` changed vs baseline (notify tier) with no contract-delta event recorded**.

| Finding | Result | Evidence / residue |
|---|---|---|
| F1 store writes | fixed | O-08 sets read-modify-write per event, per-field owners and reload after reset. `PuzzleProgress.restarted()` keeps `bestSeconds`. ADR-005 gives the cadence (events, every 10 s, background). `IProgressStore` doc cites O-08. |
| F2 screen composition | fixed | O-09: `app` scaffold, slots, play lends its drawing to browse, timer/DEV overlay slots, gear slot, drag lock-out. |
| F3 test placement | fixed | §5 rules 1–4. G-06/V-06 limited to main scope, `testImplementation(":content")` allowed. Solved check moved to `kernel` (O-01). |
| F4 WO sequence | **partly** | Per-WO acceptance IDs, carried IDs, minimal shell + TYPE-007 + tray rows in WO-003, and the aid-solve regression in WO-008 are all in place. **REQ-045 is in no WO's scope column** (only "carried"), so §2's claim "every one of the 47 locked REQs is in exactly one row" is false. Rule 3 still needs ≥1 REQ-045.A1/A2 covering test under `content/`. Fix: add "REQ-045 A1–A2 at library/engine level" to WO-002's scope (the §5 rule-4 example already says so) and keep the on-screen parts carried to WO-003/004. |
| F5 G-01 / V-01 | fixed | `<permission>`, `uses-permission-sdk-23`, allowBackup, extraction rules and EmojiCompat are all covered. V-01 builds the release manifest itself and blocks from WO-001. API 26–32 image from WO-003. Minor: do the WO-001 hardening in the *main* manifest so debug installs don't back up either. |
| F6 G-04 / V-04 | fixed | Neutral hooks plus `byAid` (G-04, O-06, ADR-006). V-04 unzips the APK and scans dex and resources. Passcode is a string constant in `devtools`. |
| F7 kernel types | fixed (guard pending) | `kernel.model` package. §4 says notify. OnBoard points at the kernel shape table with a golden test. Frozen v1 fixture (G-09). Takes effect when guard.json lists `kernel/.../kernel/model/*` as notify. |
| F8 G-03 route | fixed | Golden exporter + `tools/golden/geometry.json` with per-file SHA-256 and validator verdict (this also covers REQ-038.A1). Python is needed only to regenerate the golden data. |
| N9 ADR-002 | fixed | Support modules justified, per-slice-source-roots alternative recorded, shared config named. |
| N10 IPuzzleLibrary docs | fixed | Poses derived in `kernel` (O-01). `puzzle(id)` cites REQ-025. Registry "Serves" equals both banners. The check is named. |
| N11 build-map deps | fixed | #Settings → #Content (IPuzzleLibrary). Deps name their `I*`. `settings` added to "Between". |
| N12 sign-offs / paths | pending by design | Registry shows "— (at G2)". Still to do at G2: the `build-map.md` header ("Approved on … signed with architecture.md at G2") and the lower-case paths in guard.json. |
| N13 kernel early | fixed | `Rational.of` guards `Long.MIN_VALUE`. WO-001 code review still owed (decisions row). |
| N14 G-09 traps | fixed | Move-aside for unreadable or newer documents, and stable serial names (G-09, ADR-005). |

**Spot-check verdict: forward.** No Blocker or Should remains open. Two one-line conditions must be met **before the G2 re-freeze**:
1. F4 residue: add REQ-045 A1–A2 (library/engine level, `content`) to WO-002's scope in `build-map.md` §2.
2. Record the `build-map.md` v1.0 → v1.1 contract-delta, which trace-check currently fails as drift. Either log it, or name it explicitly in the G2 re-freeze entry.

After that comes the planned guard.json update (N12 paths, `kernel/model/*` notify, test_globs) and `trace_check.py --freeze`.
