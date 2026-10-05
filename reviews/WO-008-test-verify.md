# WO-008 #PlayTime: Test & Verify

Station: Test & Verify T&V8 (fresh context), 2026-10-05. Executed, not read. One Gradle build and one emulator at a time. No git. No product code and no test edited. Scratch files (a positive-control copy set and one probe) were created in the tree, run, and removed.

**Verdict: PASS.** All 9 held-out cases of WO-008 (REQ-005.A1, REQ-029.A1, REQ-030.A1/A2/A3, REQ-031.A2, carried REQ-046.A3, and the DA-138 reset case) passed on first run on both channels. 0 escaped defects. Visible baseline equals the expected numbers on JVM (816) and on both channels. Substance: 0 B / 1 S / 5 N.

## 1. Inputs

- Held-out compiled at HELD-COMPILE8: the overlay compiled and ran (so it compiled in tree).
- `python tools/compare_kits.py . .` exit 0, all 12 kit files `equal` (AdvanceActive, AppLaunch, DeviceShell, DisplaySpec, GestureInsets, ManualTimeSource, NavigationMode, PlayerControlWalk, Rotation, ScreenWalk, Seed, SolveByTouch). Re-run after every overlay removal: still 0.
- Held classes were read in full before the run.

## 2. Visible baselines

JVM (`assembleDebug test`, then each module's unit-test task with `--rerun` placed after the task name; counts from the JUnit XML):

| Module | Expected | Measured | Fail |
|---|---|---|---|
| kernel | 168 | 168 | 0 |
| content | 47 | 47 | 0 |
| store | 68 | 68 | 0 |
| play | 234 | 234 | 0 |
| browse | 74 | 74 | 0 |
| settings | 61 | 61 | 0 |
| time | 65 | 65 | 0 |
| app (debug) | 68 | 68 | 0 |
| devtools | 31 | 31 | 0 |
| **Total** | **816** | **816** | 0 |

(The app results folder also holds 2 stale `testReleaseUnitTest` XML files, as in WO-006/007 N1; they are not counted.)

Device, visible (every suite run in full, `connectedDebugAndroidTest` per module; all green, 0 failures, 0 errors; counts = testcase elements):

| Suite | Expected | API 26 | API 37 |
|---|---|---|---|
| app (includes the gate `HarnessScaffoldingTest` 10 and the in-tree held WO-005..007 classes) | 137 | 137 (14 min 57 s) | 137 (10 min 51 s) |
| time | 8 | 8 | 8 |
| play | 116 | 116 | 116 |
| browse | 31 | 31 | 31 |
| settings | 14 | 14 | 14 |
| devtools | 13 | 13 | 13 |

## 3. Held-out first run

Order: API 26 first (it was already running as emulator-5554; the brief allowed this), then `adb emu kill`, then API 37. Serial and AVD confirmed with `adb devices` and `adb emu avd name` each time. The five WO-008 held files were copied into the in-tree `held/` dir, the whole held package was run (`-Pandroid.testInstrumentationRunnerArguments.package=...acceptance.held`: 42 cases = 33 earlier held + 9 WO-008), and the overlay was removed.

| ID | Test | API 26 | API 37 |
|---|---|---|---|
| REQ-029.A1 | `HeldIdleCutoffAppTest` (2: settings screen, play screen) | pass 2/2 | pass 2/2 |
| REQ-030.A1 | `HeldPuzzleTimeAppTest.browsingAway...` | pass | pass |
| REQ-030.A3 | `HeldPuzzleTimeAppTest.turningATrayPiece...` | pass | pass |
| REQ-030.A2 | `HeldBestTimeAppTest.solvingAgainSlower...` | pass | pass |
| REQ-005.A1 | `HeldBestTimeAppTest.afterSolving...` | pass | pass |
| REQ-046.A3 (carried) | `HeldBestTimeAppTest.theAidSetsNoBest...` | pass | pass |
| REQ-031.A2 | `HeldTimerActiveAppTest` | pass | pass |
| decision DA-138 | `HeldResetTimeAppTest` | pass | pass |
| earlier held (WO-001..007) | 33 cases | pass 33/33 | pass 33/33 |

Whole held package: 42/42 on both channels. **Held-out first-run failures: 0. Escaped defects: 0.**

One infrastructure event, not a result: the first API 26 attempt failed at install (`INSTALL_FAILED_ALREADY_EXISTS`, the tool's own installer, nothing ran, the package was not on the device). Re-run unchanged, passed. Voids nothing: no test executed in the first attempt.

Overlay removal shown: after the API 26 step and again after the API 37 step, `ls` of the held dir finds 0 of the five WO-008 files and 0 scratch files; `sha256sum *.kt` of the in-tree held dir equals the hash list taken before the first overlay (identical); `compare_kits` exit 0. The `.swdev/heldout/WO-008/` originals were not touched.

## 4. Substance audit

### 4.1 The positive control: a clock that never advances

Read first, then run on API 26. Scratch copies of each held class (class renamed `Frozen...`) with a member `advanceActive` that only calls `waitForIdle` (shadows the kit's top-level function). Result on API 26 with the frozen clock:

| Class | Frozen result | Where it fails |
|---|---|---|
| `HeldIdleCutoffAppTest` (2 tests) | both fail | "10 touched seconds are counted" expected 10 but 0; "and exactly the window" expected 60 but 0 |
| `HeldPuzzleTimeAppTest` (2 tests) | both fail | fixture "5 active seconds" expected 0:05 got 0:00; "the first drag started the time" 0:05 vs 0:00 |
| `HeldBestTimeAppTest` | 2 of 3 fail | solve time bar 0:30 vs 0:00 (both solve tests). **`theAidSetsNoBestTimeOnRealBestTimes` passes** (see N1) |
| `HeldTimerActiveAppTest` | fails | "the timer follows the touched clock" 0:05 vs 0:00 |
| `HeldResetTimeAppTest` | fails | "15 active seconds are on the seeded play time" expected 140 but 125 |

10 of 11 methods fail under a frozen clock, each at a fixture assertion before the REQ assertion, so a failure in the real run could not be a blind pass. The A3 test passes its first half (timer 0:00 after 30 s) under a frozen clock but fails at the end, so as a whole it discriminates.

### 4.2 The author's two notes

- **`HeldResetTimeAppTest` first-due-flush at `advanceActive(15 s)`.** Judged sound. The test does not assume it silently: it asserts, from a fresh `AppStore.open()`, that the stored play time is 140 / 4015 (125 + 15, 4000 + 15) before the reset. If the first flush did not fall due in 15 s, the fixture fails loudly (as the frozen run shows: 125 vs 140). It passed on both channels, so the assumption holds with the shipped 10 s flush. The test is correctly labelled an end-to-end regression, not the discriminating guard (a flush due at the very moment of Erase is `OwnershipOrderTest` (1)/(8), run green on JVM and read in this audit; `TimeStoreRoundTripTest` wires the real `onReset` with a flush due). Limit stated, not a gap.
- **`HeldPuzzleTimeAppTest` A3 tray tap could miss the piece.** Checked by execution. A scratch copy of the A3 test (API 37) reset `FeedbackProbe`, did the same three taps and asserted three `TURN` requests: `TURN=3, PICK_UP=0, LOCK=0`. The tap lands on the cell (the rect from `BoardTransform.trayCellsPx` is in `play-area` coordinates, the node it taps), registers as a turn and not as a drag, and does not leave the tray. So the turn half is genuinely exercised. The test itself does not assert that a turn happened (see N2); the evidence is this probe.

### 4.3 Each held class against its REQ

| Class | Token claimed | Real? |
|---|---|---|
| `HeldIdleCutoffAppTest` | REQ-029.A1 "untouched 5 minutes adds at most 60 s" | yes. Real touch on the real settings screen, five minutes through `advanceActive`; asserts the total moved by exactly the 60 s window (not just at most, so a keeper that never counts also fails); proves the clock is wired first (10 s reads 0:10); the next touch counts again. Second test: the same on a play screen. Strong |
| `HeldPuzzleTimeAppTest` A1 | REQ-030.A1 "browsing away 2 minutes adds nothing" | yes. 2 minutes of touched time away three ways (settings, next/prev puzzle, grid), the timer still 0:05 each time and the settings total shows the 2 minutes did pass (so "nothing added" is not "clock stopped"); resumes on return |
| `HeldPuzzleTimeAppTest` A3 | REQ-030.A3 "turning a tray piece on a New puzzle starts no time" | yes (turn confirmed by the probe in 4.2): three turns, 30 s, still New and 0:00; then the first drag makes it In progress and the next 5 s read 0:05 |
| `HeldBestTimeAppTest` | REQ-005.A1, REQ-030.A2, carried REQ-046.A3 | yes. Solves by real touch in the SolveByTouch pattern with exact solve times: 30 s, slower 50 s keeps best 30 (stored `puzzleSeconds` 50, N7d), faster 20 updates, equal 20 keeps; fresh `JsonProgressStore` reads and a relaunch. Aid: seeded earlier best 600 above the counted seconds; a never-solved second puzzle shows the dash, null best, and no best-time row |
| `HeldTimerActiveAppTest` | REQ-031.A2 "the timer is shown and counts active seconds only" | yes. Switched on by the real settings screen; follows touched time (0:05, 0:15), reads 1:15 at the end of the window, does not move after 2 and 5 more idle minutes, counts again on the next touch (1:18) |
| `HeldResetTimeAppTest` | decision DA-138, no token | yes, carries no REQ token (header says why). Reset with time on both sides; stored file and relaunch read only post-reset seconds; settings kept |

### 4.4 Visible tests at sampling depth

- `OwnershipOrderTest` (9 cases), `FlushRulesTest`: read. Simulate the real callers over a recording fake store; each case makes a flush due at the moment under test; asserts "no write between `resetAllProgress()` and the end of `onReset`" in both call orders and with and without a touch on Erase, restart/open/solve/stale-best cases, 11 accounting inputs. No tautology. They carry `// decision DA-138` / `DA-137`; no dotted REQ token.
- `TimeStoreRoundTripTest`: read in full. Real `JsonProgressStore` on a temp folder, real `BrowseController` and `SettingsController`, `onReset` wired as `AppViewModel` does, a recorder of every write; reopen with a new store and keeper; Restart, Retry and reset legs. Real. Decision marker, no token.
- `PlayTimeReadoutTest`: read in full. Nonzero or distinguishing values (midnight, reload, 59:59 to 1 h 0 min); every method carries the dotted REQ-029.A2. Real at module level (keeper values and the format, the screen is in the other two classes).
- `TimerLayoutMeasuredAppTest`: header and first test read. Real device rects of the pill, Restart and the DEV pill in three windows (fi 2.0 font, en phone, tablet); the density is read from the composition (DA-40 lesson applied); the pill never overlaps and keeps the board's top-right edge, or steps 4 dp below an obstacle; decision DA-140, no token. Ran green on both channels.
- **Token isolation grep** (exact dotted match over all source sets in tree): REQ-005.A1 only `SolveTimeTest`; A2 `TotalAvailableTest`, `PlayTimeSettingsAppTest`; REQ-029.A1 `IdleCutoffTest`; A2 `PlayTimeReadoutTest`, `PlayTimeSettingsAppTest`, `SettingsPlayTimeTest`; REQ-030.A1/A2/A3 `BrowseAwayTest`, `BestTimeTest`, `NewPuzzleStartsNoTimeTest`; REQ-031.A1 `PuzzleTimerTest`, `TimerAppTest`; A2 `PuzzleTimerTest`, `TimerRotationAppTest` (the one carried exception, DA-143). Matches the isolation table; the held-only ones are absent from the tree. Prose mentions in `FlushRulesTest`/`OwnershipOrderTest` are written "REQ-029 A1" / "REQ-030 A3" without the dot.
- **Per-method token rule in the four mixed classes:** `PuzzleTimerTest` (A1, A2 methods carry the token; the two format methods carry `decision DA-135` and no token), `SettingsPlayTimeTest` (three A2 methods carry the token; the label and order methods carry `decision DA-141`), `PlayTimeSettingsAppTest` (A2/005.A2 methods tokened; midnight and best-list methods `DA-139`/`DA-141`, no token), `TimerAppTest` (A1 method tokened; the positive control `DA-140`, no token). Rule holds in all four.

### Findings

- **B:** none.
- **S1:** `PuzzleTimerTest`'s REQ-031.A2 method ("the pill shows the given seconds and is inert") is module-level and proves rendering, not "counts active seconds only"; the real A2 meaning is only in the held `HeldTimerActiveAppTest` (and `TimerRotationAppTest` for rotation). That is the intended split in the isolation table and coverage exists, but if the held test is ever dropped or folded into the visible suite, A2's visible coverage is thin. Not a defect today.
- **N1:** `HeldBestTimeAppTest.theAidSetsNoBestTimeOnRealBestTimes` does not depend on the clock (it passes with a frozen clock). Its `advanceActive(20 s)` is not asserted. Still discriminating: the seeded 10 s counted is below the best 600, so a `min(best, counted)` fault would show 0:10 and fail. Cosmetic.
- **N2:** the A3 test asserts New and 0:00 after the taps but not that a turn really happened; a missed tap would pass the first half. The probe in 4.2 shows it does turn (3 of 3). A corrected check could assert `FeedbackProbe` TURN count (sound is on by default) for robustness. Optional.
- **N3:** `HeldResetTimeAppTest` holds the reset case end to end but cannot hold a flush due at the instant of Erase; stated in its header and covered by the JVM gate and `TimeStoreRoundTripTest`.
- **N4:** `compose.touch("puzzle-state")` / a click on plain text is used as a "real contact" for engagement; fine, and consistent across held classes.
- **N5:** `--rerun` must follow each task name (`:browse:test --rerun` on the aggregate task reran nothing); the first JVM pass here showed browse, time and devtools up to date, so they were re-run explicitly. Process note for the baseline recipe.

## 5. Escaped defects and routes

- Held-out first-run failures: **0**. For the WO metrics: 0 escaped defects at first run, 0 fixture corrections, 0 product faults.
- Routes: none. No corrected check, no product fault.
- Optional, non-blocking, to the Test Author: S1 and N2 as a later corrected-check improvement.

## 6. `device_reset.py` lines

```
API 26 (emulator-5554 = Phone_API_26)
before held run (reset):               PASS ... clean (native baseline)
after held run, reset:                 PASS ... clean
after scratch frozen runs, reset:      PASS ... clean
before visible suites, --check:        PASS ... clean
after visible suites, reset:           PASS ... clean
API 37 (emulator-5554 = Medium_Phone_API_37.0)
after boot, before held, reset:        PASS ... clean
after held run, reset (before probe):  PASS ... clean (reset before probe also)
after probe, reset:                    PASS ... clean
before visible suites, --check:        PASS ... clean
after visible suites, reset:           PASS ... clean
```

Every reset and check was clean; no step voided. Emulator stopped at the end (no instance left running).
