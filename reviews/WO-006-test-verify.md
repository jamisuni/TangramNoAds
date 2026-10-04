# WO-006 #Layout, #Language, #Promise: Test & Verify

Station: Test & Verify (fresh context), 2026-10-04. Executed, not read. One Gradle build and one emulator at a time. No git commands. No code, test or assertion edited.

**Verdict: PASS. All 5 held-out IDs pass on API 37 and API 26 at the first run, all visible suites match the baseline on both channels, 0 corrections, 0 escaped product defects. Substance: 0 B / 1 S / 5 N.**

## 1. Input checks

- Held-out compiled: yes (TASK-T6b row: `BUILD SUCCESSFUL in 3s`, scratch removed).
- `python tools/compare_kits.py .swdev/heldout/WO-006 .`: AppLaunch, DeviceShell, DisplaySpec, GestureInsets, NavigationMode, PlayerControlWalk, Rotation, ScreenWalk, Seed: all 9 `equal`, exit 0. Re-run after the move-in (before the held run): still all equal (no non-equal line).
- The in-tree visible helpers were not touched by me, so the kit stayed equal.

## 2. Visible baseline (counts from the JUnit XML, forced re-run)

JVM: `assembleDebug cleanTest test --no-build-cache` was a no-op for the Android modules (UP-TO-DATE), so I forced them with `:<m>:testDebugUnitTest --rerun` (XML timestamps 11:28, today).

| Suite | Expected | Measured | Failures |
|---|---|---|---|
| kernel JVM | 141 | 141 | 0 |
| content JVM | 47 | 47 | 0 |
| store JVM | 68 | 68 | 0 |
| play JVM | 192 | 192 | 0 |
| browse JVM | 66 | 66 | 0 |
| app JVM (debug) | 24 | 24 | 0 |
| devtools JVM | 29 | 29 | 0 |
| app JVM (release) | +2 | 2, but **stale** (2026-10-03 XML) | 0 (not re-run, see N1) |

Device, visible (Medium_Phone_API_37.0 = emulator-5554, Phone_API_26 = emulator-5556):

| Suite | Expected | API 37 | API 26 |
|---|---|---|---|
| play | 115 | 115 | 115 |
| browse | 25 | 25 | 25 |
| app | 75 | 75 visible (86 with held, see below) | 75 (run before the held move-in) |
| devtools | 13 | 13 | 13 |

Failures, errors, skips: 0 on both channels. API 37 ran the four suites once with the held-out files already in the tree: app measured 86 = 75 visible + 11 held, 0 failures. `HarnessScaffoldingTest` (changed under DA-111) is inside that 75 and green on API 37 and API 26, so DA-111's app-launches-before-rotating change is re-proven on API 37. No difference from the orchestrator baseline.

## 3. Held-out first run

Moved (copied; originals stay in `.swdev/heldout/WO-006/`, the WO-004/005 precedent) to `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/`: `HeldGestureInsetAppTest`, `HeldRotationAppTest`, `HeldTouchTargetAppTest` (incl. `HeldTouchTargetNativeWindowAppTest`), `HeldLanguageSwedishAppTest`, `HeldInstalledPermissionsAppTest` and the 9 kit copies. No assertion edited. API 26 ran first (app suite 86/86 in 8 min 57 s), then `adb emu kill`, then API 37 booted as emulator-5554 (full four suites, 13 min 2 s).

| ID | Test (cases) | API 37 | API 26 |
|---|---|---|---|
| REQ-035.A2 | `HeldGestureInsetAppTest` (`PHONE_390x844`, `PHONE_360x780`) | **pass 2/2** (gesture mode forced, evidence) | pass 2/2, **rule-level only**: "tray above the system bar; A2 not applicable on this API" (DA-98); API 26 has no gesture navigation |
| REQ-036.A2 | `HeldRotationAppTest` (`TABLET_1280x800`) | pass 1/1 | pass 1/1 |
| REQ-037.A1 | `HeldTouchTargetAppTest` (390x844, 360x780 incl. fi and font 2.0, 1280x800, 800x1280, 600x960) | pass 5/5 | pass 5/5 |
| REQ-037.A1 (native window, no token) | `HeldTouchTargetNativeWindowAppTest` | pass 1/1 | pass 1/1 |
| REQ-047.A2 | `HeldLanguageSwedishAppTest` (sv-SE, 4 screens) | pass 1/1 | pass 1/1 |
| REQ-010.A2 | `HeldInstalledPermissionsAppTest` | pass 1/1 | pass 1/1 |

Totals: 11 held test cases per channel, 11/11 on both. Escaped held-out defects: **0**.

## 4. Corrections

None. No fixture or adapter defect surfaced, so the shared kit did not change; `compare_kits.py` was all `equal` before and after.

## 5. Escaped product defects

0.

## 6. Substance audit of the 13 IDs

Read: the 5 held classes, the kit (`ScreenWalk`, `PlayerControlWalk`, `NavigationMode`, `GestureInsets`, `Rotation`), and the visible `PhoneLayoutAppTest`, `TabletLayoutAppTest`, `PlayThroughAppTest`, `LanguageFinnishAppTest`, `PromiseWalkAppTest`, `AirplaneModeAppTest`, `AirplaneMode`, `PromiseWords`, `PromiseSourceScanTest` (head), `HarnessScaffoldingTest` (the 30 dp control).

| ID | Check | Real? |
|---|---|---|
| REQ-006.A1 | `PlayThroughAppTest`: solve by real touch, Retry, Restart, solve, Next, browse, grid to the last puzzle, on three windows; tablet flips the parallelogram | yes |
| REQ-035.A1 | 7 cells, rows 3 + 4 by piece set and TYPE-001 order, inside the area, below the top bar, no scroll action in the tree | yes |
| REQ-035.A2 | real window insets, gesture mode forced (loud if unreached), exclusion must be > 0 (loud), every cell bottom <= window - exclusion | yes on API 37; API 26 is rule-level and the failure/report wording says so |
| REQ-036.A1 | 7 cells, tops equal within 0.5 px, lefts strictly increasing in order, inside area | yes |
| REQ-036.A2 | real touch places 3 pieces, real display rotation, asserts 800x1280 dp, state in progress, layout changed (area size differs), each placed piece's colour at its centroid through the new `BoardTransform`, other 4 pieces still in the tray | yes, strong |
| REQ-037.A1 | merged-tree walk, tag-or-ancestor classification, unclassified interactive node fails, tray cells and flip badge added, `dev-*` skipped, node layout size (not the widened touch bounds); must-find lists per screen incl. `flip-badge` and one grid cell per puzzle; 30 dp negative control and 48 dp positive control in `HarnessScaffoldingTest` | yes, cannot be blind |
| REQ-047.A1 | every text and description is an own-language value, title or neutral token, none a leaked other-language value, recursive composites, "Kissa" literal, required buttons present | yes (settings screen carried, C1) |
| REQ-047.A2 | same walk under sv-SE, English titles in the top bar and every grid cell, English buttons required | yes (settings, free note, privacy carried, C2) |
| REQ-001.A1 / REQ-008.A1 | one walk (4 screens x en/fi x phone/tablet) against the PREFIX/WHOLE lists, currency regex, canary per screen (walk fails blind), no "locked" text, every grid cell clickable | yes; the two IDs share one identical `hitsIn` assertion (N2) |
| REQ-008.A2 | no extra compose root (dialog/popup) on any screen, rating words checked against the shared list and asserted present in it | yes |
| REQ-010.A1 | airplane flag set and asserted, then the player's flow (drag, prev/next, grid, recreate, Restart, solve); prior state restored | yes on device behaviour; the claim itself rests on the no-INTERNET argument (see airplane note) |
| REQ-010.A2 | `targetContext.packageName` (asserted not `.test`), `requestedPermissions` and `permissions` empty, no INTERNET, control: `android` package shows permissions | yes |

Word lists: `PromiseWords` marks every entry WHOLE or PREFIX. Short English words (`ad`, `tip`, `fee`, `rate`, `pay`, `stars`) are WHOLE, with inflected forms listed (CR-5 S3); Finnish stems are PREFIX with `maks`/`kaup` deliberately long (`maksu`, `kaupp`...), the device copy equals the JVM copy by `PromiseWordListEqualityTest` (green). `ALLOWED_WORDS` and `PROMISE_TEXT_KEYS` are empty, so nothing is silently exempt today.

**Airplane-mode honesty (API 26).** The claim is stated correctly. `AirplaneMode` on API 26 sets the settings flag and runs `svc data disable`; `svc wifi` is killed (rc 137) so Wi-Fi is not cut, and that is logged (`WO006Airplane`) and written in the test header, the helper and the design. On API 26 the settings flag alone does not drive the radios, so that run shows device behaviour only; the REQ-010 A1 evidence is the permission argument (REQ-010.A2 held test, V-01, V-08, source scan). Real airplane mode (`cmd connectivity airplane-mode`) is only exercised on API 37.

**Carried parts C1-C11:** all named in the design table, the tasks index and the test headers (C1 and C2 in the language tests, C3/C4 in `PromiseWalkAppTest`, C7 in `AirplaneModeAppTest`, C5/C6 in the touch-target KDoc, C8/C11 as WO-009 evidence). None dropped silently. They remain open for AUDIT6 and CLOSE6 to write into `build-map.md`.

**Scaffolding vs coverage:** token grep over the new tests finds REQ tokens only in `PhoneLayoutAppTest`, `TabletLayoutAppTest`, `PlayThroughAppTest`, `LanguageFinnishAppTest`, `PromiseWalkAppTest`, `AirplaneModeAppTest` (visible, 8 IDs) and the 5 held classes; `HarnessScaffoldingTest`, `GestureInsetProbeScaffoldingTest`, `LocaleSeamSmokeScaffoldingTest`, `TraySizeSweepTest`, `PromiseSourceScanTest` carry decision or guardrail markers only.

### Findings

- **B:** none.
- **S1 (tracking, not a defect):** REQ-035.A2 is satisfied on API 26 only at rule level. The passing API 26 run must not be counted as criterion evidence; API 37 is the evidence (as DA-98 says). Keep the "API 37 only" note in the audit and checkpoint.
- **N1:** the "+2 release" app JVM tests: `testReleaseUnitTest` is no longer a task (`:app:testReleaseUnitTest` not found); the 2 results on disk are from 2026-10-03. Either the baseline wording ("+2 release") is stale or the release unit-test variant was dropped; the orchestrator's 567 + 2 close line cannot be reproduced by task today.
- **N2:** REQ-001.A1 and REQ-008.A1 share one walk and the same `hitsIn(seen)` assertion; REQ-001.A1 adds only the "locked" check. Legitimate (REQ-008 A1 is a subset), but the two tokens are one assertion.
- **N3:** the touch-target walk allows 1 px of layout rounding (`slackDp = 1/density`). At the AVDs' native densities that lets a control of about 47.6 dp pass; the negative control uses 30 dp, so it does not test the edge. Tray cells use 0.01 dp (exact). Low risk, noted.
- **N4:** `HeldTouchTargetNativeWindowAppTest` has no token and no marker by design (counted here as a decision/matrix test, 11 held cases not 10 IDs-by-class).
- **N5:** `device_reset.py` and the helper restores held up: every `--check` after a step was clean, no leftover, no voided step.

## 7. `device_reset.py` lines (every one)

```
API 26 (emulator-5556), before visible suites:
PASS emulator-5556 (Phone_API_26, API 26): clean (native baseline)        reset, exit 0
PASS emulator-5556 (Phone_API_26, API 26): clean (native baseline)        --check, exit 0
API 26, after visible suites (--check):
PASS emulator-5556 (Phone_API_26, API 26): clean (native baseline)        exit 0
API 26, before held-out app run (reset):
PASS emulator-5556 (Phone_API_26, API 26): clean (native baseline)        exit 0
API 26, after held-out app run (reset+check):
PASS emulator-5556 (Phone_API_26, API 26): clean (native baseline)        exit 0
API 37 (emulator-5554), after boot, before suites (reset, then --check):
PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)   exit 0
PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)   exit 0
API 37, after the four suites + held-out (reset+check):
PASS emulator-5554 (Medium_Phone_API_37.0, API 37): clean (native baseline)   exit 0
```

No non-clean check: no step voided, no leftover to record.

## 8. Files moved or changed

- Copied in from `.swdev/heldout/WO-006/app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/` to the same path under `C:\GitHub\AI\TangramNoAds\`: `AppLaunch.kt`, `DeviceShell.kt`, `DisplaySpec.kt`, `GestureInsets.kt`, `HeldGestureInsetAppTest.kt`, `HeldInstalledPermissionsAppTest.kt`, `HeldLanguageSwedishAppTest.kt`, `HeldRotationAppTest.kt`, `HeldTouchTargetAppTest.kt`, `NavigationMode.kt`, `PlayerControlWalk.kt`, `Rotation.kt`, `ScreenWalk.kt`, `Seed.kt` (the directory already held the WO-004/005 held files).
- Created: this file. Nothing else changed.

## 9. Emulator state

API 26 stopped with `adb emu kill`. `Medium_Phone_API_37.0` is running as **emulator-5554**, booted, `--check` clean (native baseline).
