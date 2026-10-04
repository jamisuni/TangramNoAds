# WO-007 #Settings: Test & Verify

Station: Test & Verify (fresh context), 2026-10-04. Executed, not read. One Gradle build and one emulator at a time. No git. No product code edited. One held-out test-body fixture step corrected (section 4).

**Verdict: PASS (with 1 corrected check).** All 4 held-out IDs (REQ-032.A1, REQ-033.A1, REQ-034.A1, REQ-034.A2) pass on API 26 and API 37 after the correction. The first run on API 26 had 1 held-out failure (`HeldSoundOffAppTest`): a fixture defect in the test, not a product defect. All visible suites match the baseline on JVM and on both channels. 0 escaped product defects. Substance: 0 B / 1 S / 5 N.

## 1. Input checks

- Held-out compiled: yes (TASK-T7b row: overlay compile `BUILD SUCCESSFUL`, scratch removed).
- `compare_kits.py .swdev/heldout/WO-007 .swdev/staged/WO-007`: PlayerControlWalk, ScreenWalk, Seed all `equal`, exit 0.
- `compare_kits.py . .`: AppLaunch, DeviceShell, DisplaySpec, GestureInsets, NavigationMode, PlayerControlWalk, Rotation, ScreenWalk, Seed all `equal`, exit 0. Re-run after the move-in and after the correction: still all equal (0 non-equal lines each time).

## 2. Visible baseline

JVM (`assembleDebug test`, then each module test task with `--rerun` after it; `BUILD SUCCESSFUL in 32s`; XML timestamps 20:36 today; counts from the JUnit XML):

| Module | Expected | Measured | Fail |
|---|---|---|---|
| kernel | 141 | 141 | 0 |
| content | 47 | 47 | 0 |
| store | 68 | 68 | 0 |
| play | 217 | 217 | 0 |
| browse | 74 | 74 | 0 |
| settings | 47 | 47 | 0 |
| app (debug) | 50 | 50 | 0 |
| devtools | 31 | 31 | 0 |
| **Total** | **675** | **675** | 0 |

(`app/build/test-results/testReleaseUnitTest` still holds 2 stale 2026-10-03 results; that task no longer exists, see N1. They are not in the 675.)

Device, visible. The API 26 run was before the held move-in; the API 37 app run was after it, so it also contains the 7 WO-007 held cases:

| Suite | Expected | API 26 (Phone_API_26) | API 37 (Medium_Phone_API_37.0) |
|---|---|---|---|
| play | 116 | 116 | 116 |
| browse | 25 | 25 | 25 |
| settings | 8 | 8 | 8 |
| app | 115 | 115 | 122 = 115 + 7 held (2 + 2 + 2 + 1) |
| devtools | 13 | 13 | 13 |

Failures, errors, skips: 0 in every suite on both channels. No difference from the baseline.

## 3. Held-out first run

Copied into `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/`: `HeldSettingsBoardAppTest`, `HeldSoundOffAppTest`, `HeldPlatformFeedbackAppTest`, `HeldResetAppTest`. The 9 kit copies were already in the tree and equal. API 26 ran first with the instrumentation filter `package=io.github.jamisuni.tangram.acceptance.held` (all held classes, 33 cases). Then `adb emu kill`, API 37 booted, all five suites.

| ID | Test (cases) | API 26 first run | API 26 after correction | API 37 (after correction) |
|---|---|---|---|---|
| REQ-032.A1 | `HeldSettingsBoardAppTest` (PHONE_390x844, TABLET_1280x800 with real rotation) | pass 2/2 | pass 2/2 | pass 2/2 |
| REQ-033.A1 | `HeldSoundOffAppTest` (1) | **FAIL 0/1 (fixture, see 4)** | pass 1/1 | pass 1/1 |
| REQ-033.A1 | `HeldPlatformFeedbackAppTest` (sound off, sound on) | pass 2/2 | pass 2/2 | pass 2/2 |
| REQ-034.A1 | `HeldResetAppTest.req034_A1...` | pass 1/1 | pass 1/1 | pass 1/1 |
| REQ-034.A2 | `HeldResetAppTest.req034_A2...` | pass 1/1 | pass 1/1 | pass 1/1 |

The API 26 first run had 33 cases and 1 failure; the other 32 (the earlier WO-004/005/006 held tests included) passed. After the correction: 33/33. Escaped product defects: **0**. For the WO metrics: **1 held-out failure at first run, class fixture (route b), 0 product**.

## 4. Corrections (corrected check, route (b))

**What failed.** `HeldSoundOffAppTest.req033_A1_withSoundOffNoActionReachesTheSoundOrHapticOutput`, API 26, sound-ON phase (the positive control), at `playEveryKindOfAction` (line 99), in `TouchRig.trayCentre`: `IllegalStateException: the ST2 miniature is not in the tray (0 matching pixels)`, after 2.9 s. No REQ assertion had been reached.

**Evidence it is a fixture defect.** The test places all pieces with `placePieces(puzzle, puzzle.solution.size)` on `shapes-mini-1` (SQ, ST1, ST2). `TouchRig.tryPlace` judges a drop by the piece colour at its solution centroid, but the last locked piece solves the puzzle and the solved picture replaces the piece colours, so `tryPlace(ST2)` reports false although ST2 locked. The retry loop then finds no ST2 in the tray and errors. This is the trap documented in `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/layout/SolveByTouch.kt` (header: WO-006 MOVE-DEV6, "the next attempt found no tray"); that helper places all but the last piece and drops the last unjudged. The held test had only been compiled, never run, so it could not have met this. The failure is before any gate, probe or product call that REQ-033 judges.

**Correction (fixture step only).** In `HeldSoundOffAppTest.playEveryKindOfAction` the line `TouchRig(compose).placePieces(puzzle, puzzle.solution.size)` became: a `TouchRig`, `placePieces(puzzle, size - 1)`, then `tryPlace` on the one piece not placed (result ignored, as `solveByTouch` does), with a "CORRECTED CHECK" comment. The assertion that follows (`puzzle-state` equals `state_solved`) is unchanged and still proves the puzzle was solved. No assertion, tolerance, sample, token, probe expectation or timing changed. The 9 kit files were not touched, so `compare_kits` stays all equal. The same edit was applied byte-identically to the original `.swdev/heldout/WO-007/app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/HeldSoundOffAppTest.kt` (`cmp` identical), so a later run of the held kit uses the corrected body. The SHA-256 before the edit was `38e8ffbb0c4b75d3...` (both copies). Re-run after the correction: API 26 33/33, API 37 all suites green.

Recommended for the orchestrator: file this as a corrected check; record the lesson (a held test that solves a puzzle by touch must use the `solveByTouch` pattern, and compile-only authoring cannot see this).

## 5. Escaped product defects

0.

## 6. Substance audit

Read: the 4 held classes in full; visible `FeedbackGateTest`, `ResetFlowTest`; the settings parts of the carried tests (`LanguageFinnishAppTest`, `LanguageEnglishFallbackAppTest`, `HeldLanguageSwedishAppTest`, `PromiseWalkAppTest`, `HeldTouchTargetAppTest`, `AirplaneModeAppTest`, `PlayThroughAppTest`); `HeldTouchKit`, `SolveByTouch`.

| ID | Check | Real? |
|---|---|---|
| REQ-032.A1 | places 3 pieces by real touch, then opens and closes by Done, by Back, by activity recreate with the overlay open, with a second finger down on a tray piece while the gear is tapped (and moved under the sheet), and by a real 90 degree rotation on the tablet. After each: `puzzle-state`, counter, each placed piece's colour at its solution centroid, every other piece in the tray. Reset excluded (F25), stated | yes, strong; the fixture asserts the 3 pieces are on the board first |
| REQ-033.A1 (`HeldSoundOffAppTest`) | the debug probe counts what reaches `SoundOut.play` and `HapticOut.tick`. Sound ON: each of the five event kinds counted at least once and at least one tick (**positive control**). Sound OFF through the real settings screen (switch asserted off, stored `soundOn == false` asserted), probe reset, the same actions (asserted to have happened: tray drop, solved state): every count 0 and 0 ticks. Sound back ON: requests resume | yes, with the sound-on control |
| REQ-033.A1 (`HeldPlatformFeedbackAppTest`) | with sound OFF and with sound ON: a tap on each of the control kinds raises the lever's `interceptedClicks` by exactly 1 (positive control: the node asked and the lever answered), a long press on next raises `interceptedHaptics`, `FeedbackProbe` stays 0, `isInteractionSoundEffectsEnabled` is false | yes; the counters move per tap, so a zero cannot come from blindness |
| REQ-034.A1 | seeds solved + in-progress + times + sound off through the real store; Reset then Erase through the real UI: the same puzzle stays and reads New, all pieces in the tray, every grid cell New, a fresh `AppStore.open()` shows NEW for all puzzles, `PlayTime.NONE`, settings and last-shown kept; then the activity is closed and relaunched: still New, sound still off | yes, strong; the fixture asserts the pre-state is not New |
| REQ-034.A2 | Reset then Keep, and Reset then Back: counter, placed pieces, grid, stored state, pieces and best time, play time, settings all unchanged; the cancelled question is gone on reopen | yes |
| REQ-032.A2, REQ-009.A1, REQ-049.A1 | visible, tokens only in `SettingsScreenAppTest` and `SettingsScreenTest`; ran green on both channels | not re-read in depth |

**Tautology check.** `FeedbackGateTest` and `ResetFlowTest` use recording fakes (the fake store's `resetAllProgress()` really clears) with a non-New seed asserted first, plus positive controls (sound-on counts; a stale Erase after cancel does nothing). No tautology found.

**Honest limit on audio.** Stated correctly in the held headers and design 3.6: the tests measure requests at our output boundary (`FeedbackProbe`) and the platform defaults at the root lever (counters and the flag). Real audio and vibration cannot be observed on an emulator (the platform's `playSoundEffect` has no external observer), so the actual sound is owner-phone evidence. No test claims to hear sound.

**Carried parts, same tokens, present.** C1 `REQ-047.A1` (`LanguageFinnishAppTest`: settings and reset-confirm in the walk, free note and privacy in full); C2 `REQ-047.A2` (`LanguageEnglishFallbackAppTest`, `HeldLanguageSwedishAppTest`); C3 `REQ-001.A1`, `REQ-008.A1`, `REQ-008.A2` (`PromiseWalkAppTest`, settings and reset-confirm screens); C5 `REQ-037.A1` (`HeldTouchTargetAppTest`, settings screens and gear); C7 `REQ-010.A1` (`AirplaneModeAppTest`: sound off and on through real settings, lock tick request via the probe, reset with confirm); C10 `REQ-006.A1` (`PlayThroughAppTest`: settings step, tablet rotates with the overlay open). All passed on both channels.

**Token isolation by class (grep).** REQ-032.A1 only in `HeldSettingsBoardAppTest` and `SettingsControllerTest`; REQ-033.A1 only in `HeldSoundOffAppTest`, `HeldPlatformFeedbackAppTest`, `FeedbackGateTest`; REQ-034.A1 and A2 only in `HeldResetAppTest` and `ResetFlowTest`. REQ-032.A2, 009.A1, 049.A1 only in the two settings classes. Scaffolding tests carry decision or guardrail markers.

### Findings

- **B:** none. The one held failure was a fixture defect, corrected (section 4).
- **S1:** `HeldSoundOffAppTest` could not run on its first execution: authored compile-only, it re-implemented a placement that the visible kit already solved in `SolveByTouch` (which is not among the 9 kit files). Corrected by route (b). Process lesson: the held-out compile proof does not prove a held test runs. Not a product risk.
- **N1:** the "+2 release" app JVM tests: `testReleaseUnitTest` is not a task (only stale 2026-10-03 XML); the 675 excludes it (as WO-006 N1).
- **N2:** `HeldTouchTargetAppTest` (WO-006 held, in the `held` package) now also carries the WO-007 C5 walk and its header calls it "a visible test"; counted visible from now.
- **N3:** `HeldSoundOffAppTest` runs on the 3-piece `shapes-mini-1`; adequate for the five event kinds (pick up, turn, lock, return, solve are each counted).
- **N4:** the held sound test asserts "at least one" per kind and ticks while sound is on, not exact counts; right strength for a control (exact counts are in `FeedbackGateTest`).
- **N5:** `device_reset.py` held up: every `--check` was clean, no step voided.

## 7. `device_reset.py` lines (every one)

```
API 26 (emulator-5554 = Phone_API_26)
before visible suites, reset:   PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
before visible suites, --check: PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
after visible suites, --check:  PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
after visible suites, reset:    PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
after held run 1 (failing), reset: PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
after held run 2, reset:        PASS emulator-5554 (Phone_API_26, API 26): clean (native baseline)
API 37 (emulator-5554 = Medium_Phone_API_37.0)
after boot, before suites, reset:   PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)
after boot, before suites, --check: PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)
after the five suites, reset:       PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)
after the five suites, --check:     PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)
```

No non-clean check: no step voided. Note: no separate "before held run 1" reset was run beyond the reset that closed the visible suites (it ran immediately before the held move-in); the state was clean. I also ran a `--check` after held run 2 through the reset line above (reset runs a check).

## 8. Files moved or changed

- Copied into `C:/GitHub/AI/TangramNoAds/app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/`: `HeldSettingsBoardAppTest.kt`, `HeldSoundOffAppTest.kt`, `HeldPlatformFeedbackAppTest.kt`, `HeldResetAppTest.kt` (originals kept in `C:/GitHub/AI/TangramNoAds/.swdev/heldout/WO-007/...`).
- Corrected (one fixture step, identical in both copies): `HeldSoundOffAppTest.kt` in the tree and in `.swdev/heldout/WO-007/...`.
- Created: this file. Nothing else changed.

## 9. Emulator state

API 26 stopped with `adb emu kill`. `Medium_Phone_API_37.0` is running as **emulator-5554** (`adb devices`: device), booted, `device_reset.py --check` clean (native baseline).
