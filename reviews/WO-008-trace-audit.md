# WO-008 traceability audit

Auditor: Traceability Auditor AUDIT8 (fresh context), 2026-10-05. Read-only run: no Gradle, no emulator, no git, no product code or test edited. Format follows `reviews/WO-007-trace-audit.md`. The only thing written is this file.

**Verdict: WO-008 scope GREEN.** All 9 in-scope IDs and TYPE-005 are linked REQ to task to code to token-carrying test. Every carried-in part is present with its original token. No orphan code and no unjustified abstraction. The contract baseline re-hashes 72 of 72, the v1 save format is untouched, and the G-04 grep finds exactly the two DA-89 lines. Test & Verify is PASS, so no T&V condition is open. Findings: **0 B / 2 S / 6 N**. Both S are bookkeeping gaps (stale rows in `tasks.md`, no evidence lines in the workorder yet). Neither is a linkage gap, and both are CLOSE8 work.

## Run

`trace_check.py --project .`: 51 REQs (47 locked, 4 withdrawn), 265 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, 72 contract files. No drift section and no baseline-delta line.
- The fail list is exactly four REQs: **REQ-001 A2, REQ-039 A2, REQ-042 A1-A3, REQ-048 A1-A3**. All four are WO-009 scope (REQ-001 A2 is C11).
- **None of the 9 WO-008 IDs is in the list.** REQ-005, 029, 030 and 031 left the WO-007 close list of 8.
- One harmless `DeprecationWarning` from `trace_check.py:381` (the SWDev tool, read-only to me).

Re-run by me, pure Python against the tree and the existing APKs:

| Check | Result |
|---|---|
| `.swdev/verifiers/test_verifiers.py` | Ran 93, OK |
| V-05 / V-06 / V-07 | PASS / PASS / PASS |
| V-01 on the release merged manifest | PASS |
| V-04 on `app-release-unsigned.apk` | PASS (canary found) |
| V-04 `--positive-control` on `app-debug.apk` | PASS (all 4 marker kinds) |
| V-08 on the release APK | PASS (release) |
| V-08 `--apk <debug> --expect-debug` | PASS (debug) |
| `tools/compare_kits.py . .` | all equal (`ScreenWalk`, `Seed`, `SolveByTouch` and the rest) |
| Control-byte scan (bytes under 0x20 other than tab, LF, CR) over 12 docs | none |

Freshness of the APKs: both were built on 2026-10-05 at 07:17-07:18. **No `src/main` file and no `.kts` file in `app`, `time`, `kernel`, `play`, `settings` or `browse` is newer than the release APK.** The V-08 tool printed each APK's sha256 and mtime. Release: `247517f3...b44e110`, debug: `bf6f17aa...84ab4`. The CR-7-FIX 068b edit therefore predates these builds.

## (a) Per-ID chain

Code home `time`, with the kernel rule in `kernel/.../kernel/time` and wiring in `play`, `settings`, `browse` and `app`. Task numbers are from the `tasks.md` WO-008 index. Token counts are exact dotted matches over all source sets. `M` is module level, `A` is app level, `H` is held. Held files are in tree and byte-identical to the originals.

| ID | Tasks | Code | Covering tests (token count) | Does the test exercise the code? |
|---|---|---|---|---|
| REQ-005.A1 (solve time is kept) | 063, 065, 068 | `PlayTimeKeeper.solved`, `mergeInto`; `PlaySession.solvedListener`; `SessionHost.build` | M `time/SolveTimeTest` (4); H `HeldBestTimeAppTest` (2) | Yes. The module test drives the real keeper through a manual `TimeSource`. The held test solves by touch and re-reads a fresh store. First run green on 2 channels |
| REQ-005.A2 (total available at any time) | 063, 066, 068 | `PlayTimeKeeper.totalSeconds`; `AppViewModel` readout; `SettingsOverlay` | M `time/TotalAvailableTest` (5); A `PlayTimeSettingsAppTest` (1) | Yes. Fresh install, through play, after a relaunch from the stored file |
| REQ-029.A1 (untouched 5 min adds at most 60 s) | 061, 063, 068 | `kernel ActiveSecond.activeMs` and `IDLE_LIMIT_MS`; keeper `account`; `TangramApp` touch observer (TYPE-005) | M `time/IdleCutoffTest` (6); H `HeldIdleCutoffAppTest` (2) | Yes. Real touch on a settings screen and on a play screen. The held test asserts the exact window of 60 s, not just "at most" |
| REQ-029.A2 (settings shows today and all-time) | 061, 062, 066, 068 | `PlayTimeReadout`; `SettingsOverlay` rows; `DurationFormat` | M `time/PlayTimeReadoutTest` (7, DA-151); M `settings/SettingsPlayTimeTest` (3); A `PlayTimeSettingsAppTest` (1) | Yes. Fresh install, m:ss below an hour, h min from an hour, the readout follows the keeper |
| REQ-030.A1 (browsing away adds nothing) | 063, 068 | keeper `setPuzzleRunning`; `ActiveSecond.puzzleCounts`; `TangramApp` snapshot flow | M `time/BrowseAwayTest` (3); H `HeldPuzzleTimeAppTest` (1) | Yes. Away three ways (settings, next/prev, grid); the settings total proves the minutes did pass |
| REQ-030.A2 (slower re-solve keeps the best) | 063, 067, 068 | `mergeInto` (best from the stored base); `SolvedBar` | M `time/BestTimeTest` (6); H `HeldBestTimeAppTest` (1) | Yes. 30 s, slower 50 s keeps 30, faster 20 updates, equal 20 keeps |
| REQ-030.A3 (turning a tray piece on a New puzzle starts no time) | 061, 063, 068 | `puzzleCounts` (state must be IN_PROGRESS) | M `time/NewPuzzleStartsNoTimeTest` (4); H `HeldPuzzleTimeAppTest` (1) | Yes. T&V proved the tap really turns (TURN = 3) |
| REQ-031.A1 (fresh install shows no timer) | 064, 066, 068 | `PuzzleTimer(shown)`; `SettingsController.timerShown` (default off from the v1 `timerShown`) | M `time/PuzzleTimerTest` (1); A `TimerAppTest` (1) | Yes. `TimerAppTest` has a positive control (`DA-140`, no token) that turns the switch on and off |
| REQ-031.A2 (timer shown and counts active seconds) | 064, 065, 068 | `PuzzleTimer`; `PlayArea(timer)` + `timerRect`; `time.puzzleSeconds` | M `PuzzleTimerTest` (1); H `HeldTimerActiveAppTest` (1); A `layout/TimerRotationAppTest` (1, the carried C9 exception, DA-143) | The held test carries the meaning (follows touched time, stops at the window, resumes). The module test proves render and inertness only (S, planned split, see N1) |
| TYPE-005 (the active second) | 061, 063 | `ActiveSecond` (60 s inclusive window, clamped, never negative); touch observer | Carried by the REQ-029.A1 and REQ-030.A3 tests. TYPE-005 is a type, so it has no token of its own. `ActiveSecondTest` 11 and `DurationFormatTest` 3 (kernel, decision tests, no REQ token) | Yes |

**Result: 9 of 9 IDs plus TYPE-005 have a token carrier under `time`.** Held 6 of the 9 (005.A1, 029.A1, 030.A1, A2, A3, 031.A2) first-ran green on API 26 and API 37 (42 of 42 held, 9 WO-008 cases).

## (b) Carried parts

| Part | Token | Carrier (tokens in it) | Present? |
|---|---|---|---|
| ← WO-005 aid regression | REQ-046.A3 | `HeldBestTimeAppTest` (2) beside the existing WO-005 classes | Yes. Aid solve with a seeded best of 600 above the counted seconds; the never-solved puzzle keeps the dash and a null best. Frozen-clock note: the test does not need the clock (N2), but a `min(best, counted)` fault would fail it |
| C4 | REQ-001.A1, REQ-008.A1 | `PromiseWalkAppTest` (1 each) | Yes. New `timerScreens`: a play screen with the pill and the settings screen with the timer on, a best of 59:59 and a best of 1 h 5 min. It fails loudly ("walk blind") if the tags are absent. Fixture asserts `2 * (entries + 2)` screens |
| C6 | REQ-037.A1 | `HeldTouchTargetAppTest` (1) | Yes. `settings-timer` at 48 dp in `PlayerControlWalk` (both copies); the pill is inert and is not a control |
| C7 | REQ-010.A1 | `AirplaneModeAppTest` (3) | Yes. Airplane flag asserted, timer on, a manual clock, pill reads 0:07, settings shows today and total, the solved bar shows the best |
| C9 | REQ-031.A2 | `TimerRotationAppTest` (1) | Yes. The one carried exception in the isolation table; "kept across rotation" is `decision F3`, no token |
| WO-007 carries: timer row, play-time section, REQ-032's order | REQ-032.A2 (existing) | `SettingsScreenTest` (4), `SettingsScreenAppTest` (3, both exact sets gain `settings-timer`), `PlayerControlWalk` x2; order in `SettingsPlayTimeTest.theRowsAreInRequirementOrder` (DA-141) | Yes. Landed together at LAND-A (B1) |
| WO-007 carry: time paused while settings is open | none by design | `SettingsPauseTest` (DA-138, no token); code: `TangramApp` `snapshotFlow { puzzleCounts(host.state, settings.isOpen, controller.gridOpen) }` | Yes. Settings 61 of 61. Held backing: `HeldIdleCutoffAppTest` runs on the real settings screen |
| REQ-034 casualty | REQ-034.A1, A2 | corrected `HeldResetAppTest` (2 + 2); `ResetFlowTest` (5 + 4, unchanged) | Yes. Frozen clock at the 3 launch sites (`frozen = ManualTimeSource`), a loud DA-40 pill-versus-probe geometry guard. T&V: no assertion changed. The reset-time half is the new `HeldResetTimeAppTest` (DA-138, no token) |

## (c) Token audit

**Isolation table.** I grepped every `REQ-005/029/030/031.An` dotted token over all source sets (main, test, androidTest, debug, release). They sit only where the isolation table says. No file outside the table carries one. Counts are in (a). The `.swdev/staged/WO-008` copies repeat the same tokens by design, are outside the trace scope, and were deleted from nowhere.
- REQ-005.A1: `SolveTimeTest`, `HeldBestTimeAppTest`.
- REQ-005.A2: `TotalAvailableTest`, `PlayTimeSettingsAppTest`.
- REQ-029.A1: `IdleCutoffTest`, `HeldIdleCutoffAppTest`.
- REQ-029.A2: `SettingsPlayTimeTest`, `PlayTimeReadoutTest`, `PlayTimeSettingsAppTest`.
- REQ-030.A1/A2/A3: `BrowseAwayTest`, `BestTimeTest`, `NewPuzzleStartsNoTimeTest`, `HeldPuzzleTimeAppTest`, `HeldBestTimeAppTest`.
- REQ-031.A1: `PuzzleTimerTest`, `TimerAppTest`.
- REQ-031.A2: `PuzzleTimerTest`, `HeldTimerActiveAppTest`, `TimerRotationAppTest`.
- Prose mentions use "REQ-029 A1" (no dot) and are not tokens.

**Per method in the four mixed classes (read in tree).** The rule holds in all four.

| Class | Tokened methods | Decision or scaffolding methods (no token) |
|---|---|---|
| `PuzzleTimerTest` | `aFreshInstallShowsNoTimer` (REQ-031.A1); `withTheSettingOnThePillShowsTheGivenSecondsAndIsInert` (REQ-031.A2) | `thePillUsesTheOneTimeFormatInEnglish`, `...InFinnish` (`decision DA-135`) |
| `SettingsPlayTimeTest` | the 3 A2 methods (below an hour; from one hour up; fresh install) | `theSectionHasItsEnglishLabels...`, `theSectionHasItsFinnishLabels` (`DA-141`), `theRowsAreInRequirementOrder` (`DA-141`) |
| `PlayTimeSettingsAppTest` | REQ-005.A2 total; REQ-029.A2 today and total | `todayRestartsAtMidnightAndTheTotalIsKept` (`DA-139`), `theBestTimeListHas...EmptiesOnReset` (`DA-141`) |
| `TimerAppTest` | `aFreshInstallShowsNoTimerOnAnyPlayScreen` (REQ-031.A1) | `thePillAppearsWithTheSwitchOnAndGoesWithItOff` (`DA-140`, positive control) |

All the other new classes (`OwnershipOrderTest`, `FlushRulesTest`, `TimeStoreRoundTripTest`, `SettingsPauseTest`, `TimerLayoutMeasuredAppTest`, `TimerTemplateScanTest`, `PxDrawingScanTest`, `TimeStringsEqualTest`, `ActiveSecondTest`, `DurationFormatTest`, every `...ScaffoldingTest`, `HeldResetTimeAppTest`) carry decision markers and no dotted REQ token. `TYPE-005` occurs only in code comments, never in a test token position.

**Held classes.** `cmp` of the five in `.swdev/heldout/WO-008/` against the in-tree `app/src/androidTest/.../acceptance/held/`: **5 of 5 identical** (`HeldBestTimeAppTest`, `HeldIdleCutoffAppTest`, `HeldPuzzleTimeAppTest`, `HeldResetTimeAppTest`, `HeldTimerActiveAppTest`). The held kit has no files of its own, so `compare_kits` held-versus-tree has nothing to compare, as recorded at HELD-COMPILE8.

## (d) Orphans and D3

Files changed after 2026-10-04 12:00 by mtime (no git), outside `build`, `Study`, the staged and held dirs, logs and docs:

| Area | Files | Anchor |
|---|---|---|
| Build | `settings.gradle.kts` (`include(":time")`), `time/build.gradle.kts`, `app/build.gradle.kts` (dep + `scannedFileGlobs`), `devtools/build.gradle.kts` (`a4FileSetS`) | TASK-060, DA-126, DA-146. Globs and one `implementation(project(":time"))` only. No new library (D5: none) |
| `kernel/.../kernel/time` | `ActiveSecond.kt` (26 lines), `DurationFormat.kt` (17 lines, with `DurationParts`) | TASK-061, TYPE-005, REQ-029/030, DA-135 |
| `time/src/main` | `PlayTimeKeeper.kt` (228), `TimeSource.kt` (19), `PuzzleTimer.kt` (45), 2 string files | TASK-062/063/064; REQ-005/029/030/031 |
| `play/src/main` | `PlaySession.kt` (`solvedListener`), `PlayArea.kt` (`timer` slot, one `SubcomposeLayout`), `TimerPlacement.kt` (50) | TASK-065, REQ-031, DA-147 |
| `settings/src/main` | `SettingsController.kt` (`readout`, `puzzles`, `timerShown`, `revision`), `SettingsOverlay.kt`, 2 string files | TASK-066, REQ-029.A2, REQ-031, DA-124/141 |
| `browse/src/main` | `SolvedBar.kt`, 2 string files | TASK-067, REQ-030, REQ-029 format rule |
| `app/src/main`, `debug`, `release` | `CountingTicker.kt` (59), `SessionHost`, `AppViewModel`, `MainActivity`, `TangramApp`, `DebugAids` x2, debug `TestConfig` | TASK-068/068b, TYPE-005, DA-137/138, DA-72 (the split) |

Everything outside this set is untouched: `contracts/`, `store/` (every file, the frozen fixtures too), `content/`, `Tangrams/`, `Requirements/` have **no file newer than the reference**. Kernel's only changes are the two new `kernel/time` files and their tests.

D3 check on the abstractions:
- `TimeSource` has a real second implementer (`ManualTimeSource` and `GateManualTimeSource` in tests): the injectable clock the WO required, so no wall clock in tests.
- `PlayTimeReadout` (a settings interface plus `NONE`) exists because `settings` may not depend on `time` (V-06); `app` supplies it.
- `CountingTicker` is the 1 Hz ticker that posts only while counting (design rev 0, DA-133). It is not a speculative layer.
- `DurationFormat` is the single place for the one format and replaces three would-be copies (`browse`, `settings`, `time`).
- `TimerPlacement.timerRect` is a pure function with a 2340-case sweep. DA-147 explains the template width.
- No abstraction with one caller and no future-only hook found.

## (e) Contract integrity

| Check | Result |
|---|---|
| Baseline re-hash vs `.swdev/contract-baseline.json` | **72 of 72 equal, 0 missing**. WO-007 was 72 of 72 |
| `SavedGame.kt`, `IProgressStore.kt`, `IPuzzleLibrary.kt`, `Puzzle.kt` | In the baseline, equal. mtimes 2026-10-02 |
| Store encode/decode (`JsonProgressStore.kt`) and the frozen fixtures (`progress-v1*.json`, `FrozenV1FixtureTest`) | Untouched (mtimes 2026-10-03). The time model fits `puzzleSeconds`, `bestSeconds`, `PlayTime(day, todaySeconds, totalSeconds)` and `timerShown`. Governance row 11 is logged as **analysed, not triggered** (decisions row 185, DA-137). No migration, so no migration test is owed |
| `IProgressStore` | notify only; no signature change; no contract delta row in the workorder, which is correct |
| G-04 grep over set S (7 product modules, `settings`, `time` `src/main`, `app/src/release`, `app/build.gradle.kts`, `settings.gradle.kts`) for `devtools` and `0417` | **exactly 2 lines**: `app/build.gradle.kts:49` (`debugImplementation(project(":devtools"))`) and `settings.gradle.kts:25` (`include(":devtools")`). The `app` line moved from 48 to 49 because TASK-060 added the `time` dependency; same line, same content |
| V-06 allow-list | `time` is `{kernel, contracts}` (`v06_module_deps.py` line 20), and `time/build.gradle.kts` main scope is exactly `kernel` + `contracts` (`content` is test and androidTest scope only). V-06 PASS |

## (f) Definition of Done evidence lines

| Evidence | Where it is recorded | Re-checked by me |
|---|---|---|
| V-01 | `tasks.md` TASK-069 row ("V-01 PASS") | PASS |
| V-04 and `--positive-control` | TASK-069 ("V-04 PASS, canary found"; "positive control PASS, 4 marker kinds") | both PASS |
| V-05, V-06, V-07 | MOVE-JVM8 and TASK-069 rows | PASS x3 |
| V-08 release and `--expect-debug`, caller check | PRE-V08-8 and TASK-069 ("the feedback-caller check included, `time` adds no caller") | PASS (release), PASS (debug) |
| Device, API 37 and API 26 | MOVE-DEV8: gate 10, `play` 116, `browse` 31, `settings` 14, `time` 8, `app` 137, `devtools` 13, all green on both; resets clean. T&V8 repeats the baselines and the 42/42 held run | consistent with `reviews/WO-008-test-verify.md` |
| Release launch | TASK-069 (3): API 26, `MainActivity` resumed, 0 FATAL, no permission entries, timer off on a fresh install, `reviews/screens/WO-008-release-api26.png` (file present) | file exists; permissions confirmed by V-08 PASS |
| JVM | final 816 of 816 (kernel 168, content 47, store 68, play 234, browse 74, settings 61, time 65, app 68, devtools 31) | equals T&V8's measured table |

The evidence lines are in `tasks.md`. They are **not yet in `workorders/WO-008.md`** (S2).

## Findings

**B (blocking): none.**

**S (should fix, bookkeeping, no product or test impact):**
- **S1: stale and blank task rows in `tasks.md`.** TASK-T8a's Status cell is **empty**, though its (i) landed at LAND-A and its (ii) at LAND-068, with the kit and the corrected `HeldResetAppTest`. TASK-T8c's Status still reads "in progress ... (iii) and (iv) next", though both landed (LAND-A and LAND-068). AUDIT8 and CLOSE8 are blank by design. CLOSE8 should set T8a and T8c to done with the landing evidence.
- **S2: `workorders/WO-008.md` carries no DoD evidence lines yet.** The DoD boxes are unticked, the Station ledger rows for Acceptance Test Author, Slice Implementers and Code Reviewer are blank (CR-7 exists: `reviews/WO-008-CR-7-code-review.md`, 0 B / 0 S / 9 N), the Log stops at MOVE-JVM8 (no CR-7, CR-7-FIX, MOVE-DEV8, TASK-069, T&V8 entries), and the Metrics table is not filled in. The DoD lines for V-01 / V-04 / V-05 / V-06 / V-07 / V-08, both device channels and the release launch must be copied there from the `tasks.md` rows at close. `STATUS.md` still reads "WO-008 in P4 design review, paused 2026-10-04 night" and `build-map.md` still says #PlayTime is Planned. Both are CLOSE8's list. Not a linkage gap.

**N (notes):**
- **N1 (T&V S1, carried):** `PuzzleTimerTest`'s REQ-031.A2 method proves render and inertness; "counts active seconds only" is proved by the held `HeldTimerActiveAppTest` and by `TimerRotationAppTest` for rotation. The coverage exists in the intended split; if the held class were ever dropped, A2's visible depth would be thin.
- **N2 (T&V N1):** `HeldBestTimeAppTest.theAidSetsNoBestTimeOnRealBestTimes` does not depend on the clock (it passes with a frozen clock). It is still discriminating (a `min(best, counted)` fault fails it). Cosmetic.
- **N3 (T&V N2):** the A3 test asserts New and 0:00 but not that a turn happened. A `FeedbackProbe` TURN count would make it self-proving. The T&V probe showed TURN = 3.
- **N4:** `HeldResetTimeAppTest` cannot hold a flush due at the instant of Erase. That case lives in `OwnershipOrderTest` (1)/(8) and `TimeStoreRoundTripTest`, which carry decision markers and no token, as designed.
- **N5:** `.swdev/staged/WO-008/` still holds copies of the token-carrying tests. trace-check ignores `.swdev`, so no double count. Remove or leave at close per the WO-007 practice.
- **N6:** the DA-89 `devtools` line in `app/build.gradle.kts` moved to line 49. Any doc that quotes "line 48" (the WO-007 audit does) is historical only.

## Handoff

```
trace-audit: WO-008 -> GREEN for WO scope
trace-check: RED project-wide, expected (WO-009 only: REQ-001 A2, REQ-039 A2, REQ-042 A1-A3, REQ-048 A1-A3)
IDs: 9 of 9 + TYPE-005 linked and tokened; carried parts 9 of 9 present
tokens: isolation table holds; 4 mixed classes per method OK; 5 held byte-identical
orphans: 0; D3: 0 unjustified; contract: 72/72, v1 format untouched, G-04 = 2 DA-89 lines, V-06 time -> kernel, contracts
verifiers (re-run): V-01, V-04 (+PC), V-05, V-06, V-07, V-08 (+expect-debug) all PASS; self-tests 93 OK
findings: 0 B / 2 S / 6 N  (S1 stale T8a/T8c rows in tasks.md; S2 no DoD evidence lines or log in WO-008.md yet; both CLOSE8)
```
