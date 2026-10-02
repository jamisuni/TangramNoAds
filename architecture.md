# Architecture — TangramNoAds

**Status:** Locked (G2)  ·  **Version:** 1.0  ·  **Last updated:** 2026-10-02
**Approved by:** AI (orchestrator) under governance.md row 4 = `ai+inform`, after fresh-eyes review `reviews/P2-architecture-review-01.md` (verdict after fixes: **forward**); surfaced to Jami at checkpoint 1  ·  **Approved on:** 2026-10-02

> Together with the locked collection — `Requirements/` v1.1 including
> `req_types.md` v0.2, accepted at G1 by Jami on 2026-10-02 — this document is
> **the Contract**. The binding readings of ambiguous REQs that the owner
> accepted at G1 are the ASSUMPTION rows in `decisions.md` (req_review_01
> F2–F20). Standing directives: `C:\GitHub\AI\SWDev\framework\guardrails\directives.md`.
> Code homes per `#Tag`: `build-map.md`. Design inputs: `design-inputs.md`.
> Review: `reviews/P2-architecture-review-01.md`.

---

## 1. Standing directives — acknowledgment

Directives D1–D6 apply in full. Deviations: none.

| Directive | Deviation | Reason | Approved by / on |
|---|---|---|---|
| none | — | — | — |

How D1 lands here: one Gradle module per code home, plus two support modules
(`contracts`, `store`) (ADR-002). The TYPE value rules are implemented once in
`kernel`; slices meet only through `kernel`, the governed interfaces in
`contracts`, and the composition `app` does (O-09). Gradle module
dependencies make the isolation mechanical (G-06).

## 2. Project guardrails

> Few, concrete, checkable. Sources: AGENTS.md rules 1, 6, 7, 11, 12, 13,
> governance.md §2 and the G1 decisions.

- **G-01 — Free and device-only** *(AGENTS rule 1; REQ-001, 008, 010, 048; decisions F7/F8)*.
  The **release** merged manifest has no `<permission>`, `<uses-permission>`
  or `<uses-permission-sdk-23>` element (library-injected ones, such as
  androidx.core's `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, are removed with
  `tools:node="remove"`). It also has `android:allowBackup="false"` and
  data-extraction rules that exclude everything from cloud backup and device
  transfer. The androidx.startup `EmojiCompatInitializer` is removed (its
  default can make the device download a font). No dependency does
  networking, ads, billing, analytics or crash reporting. No code calls
  `ContextCompat.registerReceiver` (with the injected permission removed, it
  throws on API 26–32). Verifier **V-01**, blocking from WO-001.
- **G-02 — Dependency allowlist** *(D5, rule 1)*. Only the libraries in §5;
  each addition is a `decisions.md` row (governance row 15) with its D5
  justification. Never one that adds network, ads, analytics or a permission.
- **G-03 — Exact geometry** *(rule 6; TYPE-003, TYPE-004; ADR-003)*.
  Puzzle coordinates, anchors and placed-piece positions are exact
  a + b·√2 values (`kernel`); a locked position is anchor − corner offset,
  exactly. Doubles only for drawing, the finger, the distance-to-R comparison
  and the inside/overlap decisions within TYPE-004's 1e-6 tolerance.
  `tools/tangram_geom.py` is the reference, checked through **golden data**:
  `tools/export_geometry_golden.py` writes `tools/golden/geometry.json` (local
  piece shapes and transforms; per puzzle file its SHA-256, solution
  polygons, outline corners, area and validator verdict). Kernel and content
  tests compare against it, and a golden whose hashes no longer match the
  puzzle files fails the tests. The exporter is re-run with AGENTS.md rule 10
  whenever `Tangrams/` or `tools/` change.
- **G-04 — DevTools never ship** *(rule 11; REQ-046; decisions F18; ADR-006)*.
  The DEV button, the passcode, the solution overlay and any code that reads
  stored solutions in order to place pieces live only in the `devtools`
  module, linked with `debugImplementation`. Release code may contain only
  **neutral hooks**: "place these piece poses and finish the puzzle", and a
  `byAid` flag on the solve event so that an aid solve sets no best time
  (REQ-030, O-06). The passcode is a string constant inside `devtools`.
  Verifier **V-04** builds the release APK, unzips it, and scans its dex
  files and resources for the `io.github.jamisuni.tangram.devtools` package
  and the passcode literal.
- **G-05 — Two languages, always** *(rule 13; REQ-047; decisions F6/F14)*.
  Every user-visible text is a string resource present in both `values/` (en)
  and `values-fi/` (fi) of its module; puzzle titles come from `title[lang]`
  with English fallback; no hard-coded UI text in Kotlin. Verifier **V-05**:
  en/fi key sets are equal in every module.
- **G-06 — Slices meet only through kernel, contracts and app** *(D1)*.
  In **main scope**, `play`, `browse`, `time`, `settings`, `devtools`,
  `content` and `store` depend on `kernel` and `contracts` only (plus
  allowlisted libraries). Only `app` depends on the other modules and wires
  implementations to interfaces. In **test scope**, a module may also depend
  on `content`, to test against the real puzzles (§5). Verifier **V-06** checks
  the main-scope project dependencies in every `build.gradle.kts`.
- **G-07 — No levels, no turn buttons** *(rule 12)*. Withdrawn REQ-004, 027,
  028, 044 and TYPE-002 stay withdrawn: the puzzle rating is the only
  difficulty and a tap turns a piece. Review check.
- **G-08 — Tangrams/ is the only puzzle source** *(rule 7; REQ-007, 038, 039)*.
  The build packages `Tangrams/*.json` (never the schema, previews or a
  hand-edited copy). After touching `Tangrams/`, run the validator, the
  renderer and the golden exporter (G-03), and look at the preview sheet.
  Agents never set `reviewedByHuman`.
- **G-09 — Saved state is versioned** *(hard-stop 2; REQ-025; governance row 11; ADR-005)*.
  One document with a `version`, stored with explicit, stable serial names
  that are independent of Kotlin identifiers. Every shape change bumps the
  version, migrates every older version and ships a migration test. From the
  first WO that writes the store (WO-004), a frozen v1 document fixture must
  always load identically. A document that is unreadable, or newer than the
  app knows, is moved aside, never overwritten, before the game starts fresh.
  Stored data never crashes the app (decisions F4).
- **G-10 — Error model** *(project-wide)*. Expected outcomes are values, not
  exceptions: "no valid lock" is `null` or a sealed result, an unknown puzzle
  is `null`, and an unreadable save is a New puzzle. Exceptions are for
  programmer errors only (`require` / `check`). No crash path from touch
  input or stored data.

### Verifiers *(`.swdev/verifiers/`, stdlib Python, each with a self-test showing it can fail; run at every WO close beside trace-check)*

| ID | Checks | Builds what it needs | Created by |
|---|---|---|---|
| V-01 | release merged manifest: zero `<permission>` / `<uses-permission>` / `<uses-permission-sdk-23>`, `allowBackup="false"`, extraction rules present, no `EmojiCompatInitializer` | runs `:app:processReleaseMainManifest` itself | WO-001 (blocking from then) |
| V-04 | release APK: no `…tangram.devtools` classes, no passcode literal (dex and resources scanned after unzipping) | runs `:app:assembleRelease` itself | WO-005 |
| V-05 | en/fi string-resource key parity per module | — | WO-001 |
| V-06 | main-scope module dependencies follow G-06 | — | WO-001 |

## 3. Component map

> Sized to the 47 locked REQs. A module is created by the first WO that needs
> it (D4). Package root: `io.github.jamisuni.tangram.<module>`.

| Module | Kind | Owns | Serves |
|---|---|---|---|
| `kernel` | Kotlin/JVM | value types in `kernel.model` (exact numbers, piece ids, turns, puzzle state, the local piece-shape table); TYPE-001 piece set; TYPE-003 turns and mirror; exact polygons (ADR-003); outline corners; each solution piece's pose (turn, mirror, `at`); **TYPE-004 lock search**; the solved check (REQ-022); TYPE-005 active-second rule; TYPE-006 transitions; TYPE-007 layout class | the TYPEs; REQ-022; used by every module |
| `contracts` | Kotlin/JVM, support module (no `#Tag`) | the governed interfaces and their contract types (§4); a separate module so the guard can protect exactly these files | registry |
| `content` | Kotlin/JVM | `IPuzzleLibrary`: packages `Tangrams/*.json` at build time, parses `tangram-puzzle/1`, orders the list | #Content: REQ-007, 038–042, 045 |
| `store` | Kotlin/JVM, support module (no `#Tag`) | `IProgressStore`: the saved-state document (ADR-005); a module so O-03 is mechanical and the store is JVM-testable | REQ-010 (on device), REQ-025, REQ-034 persistence |
| `play` | Android lib | board, tray, size marks, drag, tap/twist turn, flip, drop resolution (lock or home), landing preview, corner pulse, solved picture and silhouette drawing (also lent to other slices, O-09), game events | #Solving: REQ-002, 011–023, 043, 051 |
| `browse` | Android lib | top bar (‹ › counter, title, rating dots, state), long press, overview grid, resume / Restart, solved view with Retry and Next | #Browsing: REQ-003, 024–026, 050 |
| `time` | Android lib | active-second ticker, today / all-time time, puzzle time, best time, on-board timer | #PlayTime: REQ-005, 029–031 |
| `settings` | Android lib | settings overlay (timer, sound, play and best times, reset with confirmation, how to play, free note, privacy text); sound and haptic output | #Settings: REQ-032–034; REQ-009, REQ-049 |
| `devtools` | Android lib, debug only | DEV button, passcode, solution overlay, solve-now via the neutral hook (G-04) | #DevTools: REQ-046 |
| `app` | Android app | the shell: scaffold and screen composition (O-09), edge-to-edge and insets, device class and orientation policy (decisions F3), back button, layout class, locale, wiring of slices to interfaces, manifest | #Layout: REQ-006, 035–037; #Language: REQ-047; #Promise / #Release checks: REQ-001, 008, 010, 048 |

### 3b. Component ownership rules

- **O-01** — Only `kernel` decides where a piece locks (TYPE-004). The drop
  (REQ-019/020), the landing preview (REQ-021), tap-turn on the board
  (REQ-016) and mirror on the board (REQ-018) all call the same kernel search,
  with no second implementation. `kernel` also derives each solution piece's
  pose (turn, mirror, `at`) from its polygon, once, for every consumer
  (devtools solve-now, tests). It also decides "solved" (REQ-022: every
  puzzle piece locked).
- **O-02** — Only `content` reads puzzle files; everything else sees puzzles
  through `IPuzzleLibrary`.
- **O-03** — Only `store` touches persistent storage; everything else goes
  through `IProgressStore`.
- **O-04** — Only `time` counts time (TYPE-005, REQ-029/030); it is told by
  `app` whether the game is visible and when the last touch was, and which
  puzzle is shown and in progress.
- **O-05** — Only `app` knows the Activity and the window (insets, orientation,
  back, device class); it computes the layout class (TYPE-007, decisions F3)
  and passes it down.
- **O-06** — `play` reports game events (pick-up, turn, lock, return, solve,
  with `byAid` on solve) through plain callbacks; `settings` owns sound and
  haptic output and the switch (REQ-033); `time` decides best times from the
  solve event (REQ-030); `app` connects them.
- **O-07** — The puzzle-state transitions (TYPE-006) are implemented once in
  `kernel`; modules request a transition, they never set a state directly.
- **O-08 — Store writes** *(review 01 F1)*. Every `IProgressStore` save is a
  read-modify-write of the current stored value inside one main-thread event
  (`store.progress(id).copy(…)`). No slice keeps a stored record across
  events. `play` and `browse` write `state` and `pieces`; `time` writes
  `puzzleSeconds`, `bestSeconds` and the play time; Restart and Retry write
  `PuzzleProgress.restarted()`, which keeps the best time (REQ-030). After
  `resetAllProgress()` (REQ-034), `app` reloads `play`, `time` and `browse`
  from the store, so nothing erased comes back. Time is saved on every
  puzzle event, every 10 s while counting, and when the app goes to the
  background (ADR-005).
- **O-09 — Screen composition** *(review 01 F2)*. `app` owns the scaffold.
  Slices expose composables with slot and callback parameters (AI-owned
  seams), and `app` passes cross-slice drawing into them. `play` lends its
  silhouette and picture drawing to `browse` (the solved view of REQ-026 and
  the grid thumbnails of REQ-050). `play`'s board takes overlay slots for
  `time`'s timer (REQ-031) and `devtools`' overlay (REQ-046). `browse`'s top
  bar takes a slot for `settings`' gear (REQ-032). `play` exposes "a piece is
  being dragged", which `app` uses to make every other control ignore
  touches (decisions F5). Drawing code is lent, never duplicated.

## 4. Governed Interface Registry  *(normative)*

> Marker convention (Kotlin): a governed interface has the `I` prefix, lives
> in the `contracts` module (package `io.github.jamisuni.tangram.contracts`)
> and starts with the governed banner. AI-owned interfaces are ordinary
> Kotlin interfaces outside `contracts`, without the prefix. Tiers are
> mirrored in `.swdev/guard.json`.

| Interface | Tier | Purpose (one line) | Between | Serves REQs | Defined in | Sign-off |
|---|---|---|---|---|---|---|
| `IPuzzleLibrary` | **locked** | the shipped puzzles in list order, with exact solution geometry, titles in both languages, kind, rating, picture and review flag; its file format is `Tangrams/puzzle.schema.json` | `content` → `play`, `browse`, `settings`, `devtools`, `app` | REQ-007, 011, 012, 019, 023, 038, 039, 040, 041, 042, 045, 046, 047 | `contracts/src/main/kotlin/io/github/jamisuni/tangram/contracts/puzzle/` (`IPuzzleLibrary.kt`, `Puzzle.kt`) | AI, 2026-10-02 (governance row 6), after review 01 spot-check "forward"; checkpoint 1 |
| `IProgressStore` | notify | the saved game: per-puzzle state, pieces and times, play time, settings, last shown puzzle, reset | `store` → `play`, `browse`, `time`, `settings`, `app` | REQ-003, 010, 012, 025, 026, 029, 030, 031, 033, 034, 046 | `contracts/src/main/kotlin/io/github/jamisuni/tangram/contracts/progress/` (`IProgressStore.kt`, `SavedGame.kt`) | AI, 2026-10-02 (governance row 6), after review 01 spot-check "forward"; checkpoint 1 |

Both contracts are built on the kernel value types in package
`io.github.jamisuni.tangram.kernel.model` (`Rational`, `Q2`, `ExactPoint`,
`PieceId`, `PieceShape`, `Turn`, `PuzzleState`, and from WO-001 the local
piece-shape table that saved board positions depend on). Those files are
`contract_paths.notify`, so every change is a logged contract-delta. The
kernel's arithmetic and algorithms live outside `kernel.model` and are
AI-owned.

## 5. Technology & data constraints

- **Stack (ADR-001):** Kotlin + Jetpack Compose, native Android; prototype
  logic ported, never a WebView.
- **Toolchain:** Gradle wrapper 9.8.0 (checksums pinned), AGP 9.4.1 (built-in
  Kotlin), Kotlin 2.4.20, Compose BOM 2026.09.00; compileSdk / targetSdk 37,
  **minSdk 26** (decisions F9); bytecode 17; runs on Android Studio's JBR
  (JDK 25). Versions live in `gradle/libs.versions.toml`.
- **Dependency allowlist (G-02):** AndroidX core, activity, lifecycle; Compose
  (ui, foundation; material3 only if a WO justifies it);
  kotlinx-serialization-json (ADR-004); kotlinx-coroutines. Tests only:
  JUnit 4, kotlin-test, Compose UI test, AndroidX test (+ Robolectric if a WO
  justifies it). Anything else: a `decisions.md` row first.
- **Saved state (ADR-005):** one versioned JSON document, app-private,
  written atomically before each save returns; no cloud backup, no device
  transfer (decisions F7).
- **Build types (decisions F18, ADR-006):** debug = test build with DevTools;
  release = no DevTools.
- **Test placement** *(trace-check rule 3; review 01 F3)*:
  1. Each acceptance ID's covering test lives in its feature's module
     (build-map.md) and drives that module's code, with fakes of the
     contracts for the rest.
  2. A module may `testImplementation(project(":content"))` to run against
     the real puzzles (G-06 covers main scope only).
  3. Behaviour that spans slices also gets an `app/src/androidTest` test
     carrying the same token.
  4. Where a JVM module cannot see the UI, the test uses the engine-level
     reading recorded in the WO, e.g. REQ-045.A2 → "locking the three stored
     pieces through the kernel solves the puzzle, and it has a picture".
     The on-screen part is carried to the WO that has the screen.
- **Verification:**
  - `gradlew assembleDebug test` runs all unit tests, JVM and Android.
  - The verifiers in §2 build the release variant themselves.
  - `gradlew connectedDebugAndroidTest` runs the UI tests on emulators: the
    API 37 image is installed, with phone 390×844 and tablet 1280×800 profiles.
  - **An API 26–32 image is also needed**, to launch the release build at the
    minimum version (G-01's removed permission, decisions F9). It is a
    verification-channel item from the first WO with instrumented tests
    (WO-003); it needs the SDK Manager because no command-line SDK tools are
    installed.
  - `trace_check.py`.
  - The golden exporter and the prototype checks of AGENTS.md rule 10 run
    whenever `Tangrams/` or `tools/` change.
  - Feel and look: the owner plays the debug APK (governance row 9).

## 6. Architecture Decision Records

| ADR | Title | Status | Links |
|---|---|---|---|
| ADR-001 | Kotlin + Jetpack Compose, native; port the prototype's logic | Accepted (Jami) | `adr/ADR-001-kotlin-compose-native.md` |
| ADR-002 | One Gradle module per code home (+ `contracts`, `store`) | Accepted | `adr/ADR-002-module-per-code-home.md` |
| ADR-003 | Exact ℚ(√2) geometry in the kernel | Accepted | `adr/ADR-003-exact-geometry.md` |
| ADR-004 | kotlinx.serialization for puzzles and saved state | Accepted | `adr/ADR-004-kotlinx-serialization.md` |
| ADR-005 | The saved state is one versioned document | Accepted | `adr/ADR-005-saved-state-document.md` |
| ADR-006 | DevTools live in a debug-only module | Accepted | `adr/ADR-006-devtools-debug-only.md` |

## 7. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-10-01 | placeholder | scaffolded under SWDev v0.15 | — |
| 0.9 | 2026-10-02 | P2 authored: guardrails G-01…G-10, verifiers, component map, ownership rules, registry (IPuzzleLibrary locked, IProgressStore notify), ADR-001…006 | G1 accepted 2026-10-02; stack decided 2026-10-01 | — |
| 0.10 | 2026-10-02 | P2 review 01 applied: G-01/V-01 (`<permission>`, release build, EmojiCompat, blocking from WO-001, API 26 channel), G-03 golden route, G-04 neutral hooks + V-04 dex scan, G-06 main vs test scope, G-09 fixture/move-aside/serial names, O-01 poses + solved check, O-08 store writes, O-09 screen composition, §5 test placement, registry Serves/Between aligned, `kernel.model` under notify | `reviews/P2-architecture-review-01.md` (recirculate, 0 B / 8 S / 6 N) | — |
| 1.0 | 2026-10-02 | G2: locked; registry signed; guard.json contract paths + re-freeze | review 01 spot-check: forward (F4 residue fixed in build-map v1.1) | AI under governance row 4 (ai+inform → checkpoint 1) |
