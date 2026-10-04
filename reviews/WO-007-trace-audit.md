# WO-007 traceability audit

Auditor: Traceability Auditor, 2026-10-04. Read-only run. No Gradle, no emulator, no git. Format follows `reviews/WO-006-trace-audit.md`. The only thing written is this file.

**Verdict: WO-007 scope GREEN.** All 7 in-scope IDs have a token carrier under the code home `settings`, and the 4 held IDs also have one at app level. Every carried-in part is present with its original token. There is no orphan code and no drift. The contract baseline re-hashes 72 of 72, the v1 save format is unchanged, and the G-04 grep finds exactly the two DA-89 lines. 0 B / 1 S / 9 N. The S is a limit that travels with the verdict (audio and haptics are measured as requests, never heard), not a linkage gap.
Test & Verify is PASS (`reviews/WO-007-test-verify.md`), so there is no open T&V condition.

## Run

`trace_check.py --project .`: 51 REQs (47 locked, 4 withdrawn), 216 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, 72 contract files. **No drift section, no baseline-delta line.** Project-wide RED as expected.
- The fail list is 8 REQs: 001 (A2 only), 005, 029, 030, 031, 039 (A2), 042, 048.
- **None of the 7 WO-007 IDs is in the list.** REQ-009, 032, 033, 034 and 049 left the list of 13 seen at the WO-006 close.
- The 8 remaining REQs belong to WO-008 (005, 029, 030, 031) and WO-009 (001.A2 = C11, 039.A2, 042, 048). None is WO-007's.
- The one in-scope-adjacent REQ shown is REQ-001 A2. It was never in WO-007's scope: it is C11, carried to WO-009.
- One harmless `DeprecationWarning` from `trace_check.py:381` (the SWDev tool, read-only to me).

Re-run by me (pure Python, no Gradle):
- `.swdev/verifiers/test_verifiers.py`: **Ran 93, OK** (the orchestrator's count after CR-6 / 057b).
- V-05 PASS, V-06 PASS, V-07 PASS against the project.
- `tools/compare_kits.py . .`: 9 of 9 `equal`. `compare_kits.py .swdev/heldout/WO-007 .swdev/staged/WO-007`: 3 of 3 `equal`.
- The four held-out test files in `.swdev/heldout/WO-007/.../acceptance/held/` against the in-tree copies by `cmp`: all 4 identical (the corrected `HeldSoundOffAppTest`, DA-132, is in both).
- Control-byte scan (bytes under 0x20 other than tab and LF) over `tasks.md`, `AGENTS.md`, `decisions.md`, `STATUS.md`, `progress.md`, `build-map.md`, `design-inputs.md`, the WO-007 workorder, the design and the WO-007 reviews: **none**.

## (a) Per-ID chain

Task chain from the `tasks.md` WO-007 section ("ID → task → test index"). Code home for the table is `settings`. The wiring is in `app`, `play` and `browse`.
Spine: TASK-050 (build), 051/052/053 (settings), 054 (`play`), 055 (`browse`), 056 (`app`), 057 (V-08), T7a/T7b/T7c, MOVE-JVM7, MOVE-DEV7, 058, T&V7.

| ID | Task chain | Code (home + wiring) | Module-level token carrier (under `settings`) | App-level token carrier | Device channels |
|---|---|---|---|---|---|
| REQ-032.A1 | 053, 054, 056, T7c, T7b | `settings` `SettingsController`, `SettingsOverlay`; `play` `PlayArea(inputEnabled)`; `app` `TangramApp` | `settings/src/test/.../SettingsControllerTest` (4 tokens, 4 `@Test`): open and close make no store write and no callback | **H** `app/.../acceptance/held/HeldSettingsBoardAppTest` (1): 3 pieces by touch, Done / Back / recreate / second finger / real rotation; board, tray and counter unchanged | API 37 + API 26 (held first run pass) |
| REQ-032.A2 | 053, T7a | `settings` `SettingsOverlay` (tags) | `settings/src/androidTest/.../SettingsScreenTest` (3): interactive nodes = the tagged set, word-marked difficulty scan | V `app/.../acceptance/settings/SettingsScreenAppTest` (2), en + fi | API 37 + API 26 |
| REQ-033.A1 | 051, 052, 054, 056, 057, T7c, T7b | `settings` `Feedback` (the one gate), `SoundSynth`, `AudioTrackSoundOut`, `ViewHapticOut`; `play` `PlayEvent`; `app` `PlatformFeedbackLever`, `SessionHost.onEvent` | `settings/src/test/.../FeedbackGateTest` (4 claim tests: sound off = 0 and 0; sound on = 1 and a tick only on LOCK; flip between events; a long mixed run) | **H** `HeldSoundOffAppTest` (1) and `HeldPlatformFeedbackAppTest` (2: sound off, sound on) | API 37 + API 26 (after the DA-132 correction) |
| REQ-034.A1 | 053, 055, 056, T7c, T7b | `settings` `SettingsController.confirmReset`; `browse` `BrowseController.afterReset`; `app` `AppViewModel` (`onReset`) | `settings/src/test/.../ResetFlowTest` (4: every id reads New, settings kept, callback once after the store call, question gone) | **H** `HeldResetAppTest` (1): real store seeded, Erase, same puzzle New, grid New, fresh store re-read, close and relaunch | API 37 + API 26 |
| REQ-034.A2 | 053, T7c, T7b | same | `ResetFlowTest` (4: Keep, Back / close, fresh question needed, repeated ask) | **H** `HeldResetAppTest` (1): Keep and Back leave store, board and grid unchanged | API 37 + API 26 |
| REQ-009.A1 | 053, T7a | `settings` `SettingsOverlay` + 2 string keys | `SettingsScreenTest` (1): note equals the REQ literal after scroll | V `SettingsScreenAppTest` (2: in full en + fi; absent from every play screen and the grid) | API 37 + API 26 |
| REQ-049.A1 | 053, T7a | same | `SettingsScreenTest` (1): privacy equals the REQ literal, no click action, below the note | V `SettingsScreenAppTest` (1) | API 37 + API 26 |

**Result: 7 of 7 IDs have a token under `settings` and a device run on both channels.** Held 4 of 7 (032.A1, 033.A1, 034.A1, 034.A2) first-ran green at T&V.

Module-level vs app-level, read honestly:
- **REQ-032.A1.** The module test proves the controller does no write and calls nothing (the engine reading, design 5 item 4). "Board unchanged" has no module-level meaning beyond that. The claim rests on `HeldSettingsBoardAppTest`.
- **REQ-033.A1.** `FeedbackGateTest` proves the gate with recording fakes and a sound-on positive control. The held tests prove the wiring through the real app: the probe counts what reaches the two outs, and the lever's counters show each node asked and the lever answered. Whole-APK reach is guarded by V-08's feedback-caller check (25 call sites = 25 pinned rows) and `FeedbackPathScanTest`. See S1: real audio and haptics are not observable.
- **REQ-034.A1.** `ResetFlowTest` asserts against its own `RecordingStore` (CR-6 N6). Its real content is the single store call, the order with `onReset`, and "settings kept". The meaning of the token rests on `HeldResetAppTest` and the store tests. No REQ-034.A1 claim leans on the module test alone.
- **REQ-034.A2.** The module test counts store calls (zero). The held test proves board, grid and stored state unchanged.

## (b) Carried parts

**Carried IN (same token, in the extended WO-006 tests).** Each carries settings content through the kit's `WalkScreen.SETTINGS` and `SETTINGS_RESET_CONFIRM` and the `settings` R in `ScreenWalk.bundle`. T&V read them and they passed on both channels.

| # | Token | Carrier | Settings content present |
|---|---|---|---|
| C1 | REQ-047.A1 | `acceptance/language/LanguageFinnishAppTest` (2) | settings and reset-confirm in the walk; the free note and privacy in full (26 mentions) |
| C2 | REQ-047.A2 | `acceptance/language/LanguageEnglishFallbackAppTest` (2, new) and `acceptance/held/HeldLanguageSwedishAppTest` (1) | `sv-SE`, settings, free note, privacy |
| C3 | REQ-001.A1, REQ-008.A1, REQ-008.A2 | `acceptance/promise/PromiseWalkAppTest` (1 each) | the two settings screens; `PROMISE_TEXT_KEYS` = `settings_free_note`, `settings_privacy` in **both** `PromiseWords.kt` copies (equal, `PromiseWordListEqualityTest`) |
| C5 | REQ-037.A1 | `acceptance/held/HeldTouchTargetAppTest` (1) | `WalkScreen.entries` includes the two settings screens; `PlayerControlWalk` must-finds `settings-*` at 48 dp and `settings-button` on every play screen |
| C7 | REQ-010.A1 | `acceptance/promise/AirplaneModeAppTest` (2) | sound off and on, a lock with its tick request through `FeedbackProbe`, Reset then Keep, Reset then Erase |
| C10 | REQ-006.A1 | `acceptance/layout/PlayThroughAppTest` (2) | settings step; tablet rotation with the overlay open; phone `recreate()` |
| WO-001 / WO-003 sound | none by design (decision tests) | `PlayEventTableTest` (DA-115), `SoundSynthTest` (DA-114), `FeedbackGateTest` (the off-switch half is REQ-033.A1), `AudioOutSmokeScaffoldingTest` | return sound, lock tick, solve chime built and gated |

The `build-map.md` WO-006 row and the WO-007 row already name C1…C10 as carried (v1.5). What is missing is the "re-verified" status. See CLOSE7 item 1.

**Carried OUT to WO-008 (what CLOSE7 writes).** `tasks.md` lines 458-461 and the workorder "Carried OUT (expected)" agree:
- the timer row in settings (REQ-031);
- the play-time and best-time section (REQ-029, REQ-030);
- "puzzle time is paused while settings is open", through `SettingsController.isOpen` (DA-124; the REQ-032 rule).

They are re-verified in WO-008 under WO-008's own tokens (REQ-005, 029, 030, 031), never under REQ-032 or REQ-033. WO-007 left no placeholder row or slot (D3, DA-124). The existing "WO-008: in ← WO-006: C4, C6, C7 (play time, best times), C9" stays; add the WO-007 items beside it. Exact text in CLOSE7 item 1.

## (c) Orphan check

Method: files changed after the build-map freeze (by mtime, no git), excluding `build`, `.gradle`, `Study`, `Requirements`, the staged and held-out dirs, logs and docs. Main, debug and release non-test files, and build files, are exactly the set below. **No file outside this set was touched.** In particular none of these changed: `kernel/`, `contracts/`, `content/`, `store/` (any file), `IProgressStore.kt`, `IPuzzleLibrary.kt`, `SavedGame.kt`, `Puzzle.kt`, `app/src/main/AndroidManifest.xml`, `HeldSolveNowStoreTest.kt`, `SessionHostScaffoldingTest.kt`.

| File | Anchor (task, REQ / decision / guardrail) |
|---|---|
| `settings/build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts` (dep + `scannedFileGlobs` `*/src/debug/**`), `devtools/build.gradle.kts` (`a4FileSetS` + `settings/src/main/**`) | TASK-050 / 050b, DA-113, DA-116, DA-121, DA-126. Globs only. The one `devtools` line in `app` is the DA-89 line, now at `app/build.gradle.kts:48` |
| `settings/.../FeedbackEvent.kt`, `Feedback.kt` | TASK-051, REQ-033 A1, DA-115, DA-116, G-10 |
| `settings/.../SoundSynth.kt` | TASK-051, DA-114; REQ-033 / REQ-020 / REQ-023 rules |
| `settings/.../AudioTrackSoundOut.kt`, `ViewHapticOut.kt` | TASK-052 / 052b, DA-114, REQ-010 A2 (no permission) |
| `settings/.../SettingsController.kt`, `SettingsOverlay.kt`, `SettingsGear.kt`, `SettingsStyle.kt`, `res/values{,-fi}/strings.xml` | TASK-053 / 053b / 053c, REQ-032, 034, 009, 049, DA-117, 118, 119, 120, 121, 122, 124, 130, 131. `SettingsStyle` is used by the overlay and the gear (palette and the 48 dp constant), so it is not an extra layer |
| `play/.../PlaySession.kt` (`PlayEvent`, `onEvent`), `PlayArea.kt` (`inputEnabled`) | TASK-054, DA-115, DA-118, REQ-033 A1, REQ-032 A1. Anchors are in the file |
| `browse/.../BrowseController.kt` (`afterReset`), `BrowseTopBar.kt` (`trailing`) | TASK-055, DA-120, O-09, REQ-034 A1, REQ-032. `afterReset` is `showAt(indexState)`, no persist |
| `app/.../SessionHost.kt` (`onEvent`, `toFeedback`) | TASK-056, DA-115, DA-123. The internal constructor is unchanged (DA-123), and `HeldSolveNowStoreTest` is not touched |
| `app/.../AppViewModel.kt`, `MainActivity.kt`, `TangramApp.kt` | TASK-056 / 056b / 056c, DA-116, 118, 125, G-10. `Feedback(` is constructed once (CR-6 N2, rule 6) |
| `app/.../PlatformFeedbackLever.kt` | TASK-056, DA-116, DA-125, REQ-033 A1 |
| `app/src/debug/.../DebugAids.kt`, `FeedbackProbe.kt`; `app/src/release/.../DebugAids.kt` | TASK-056, DA-116, DA-123; the release twin only passes through, asserted by `ReleaseSeparationTest` |
| `.swdev/verifiers/dex_callers.py` (248 lines), `v08_promise_apk.py` changes, `test_verifiers.py` | TASK-057 / 057b, DA-123, 125, 127, G-01. The reviewer's script, copied in and credited, hardened fail-closed, with crafted-dex fixtures |
| Test side (`settings/src/{test,androidTest}`, `play`, `browse`, `app`, `devtools` tests; kit files; both `PromiseWords.kt` copies) | T7a / T7b / T7c, MOVE-JVM7, PROMISE-KEYS7, all named in the tasks |

D3 (unjustified abstractions): **none.** Every new type is in the design's frozen seam table and a task:
- `Feedback`, `SoundOut`, `HapticOut`, `FeedbackEvent`: one gate, two outs, five events (DA-116, G-06 keeps `play` and `settings` independent).
- `PlayEvent`: the independent `play` enum (G-06).
- `PlatformFeedbackLever`: the F1 blocker's one lever (DA-125).
- `FeedbackProbe`: debug-only test seam (DA-116).
- `trailing` slot and `afterReset`: O-09 and DA-120.
- `dex_callers.py`: the E3 mechanical check, not a manual duty.

No placeholder rows, no slot parameters for WO-008 (DA-124). `FeedbackEvent` and `PlayEvent` are two copies of one five-value enum by design, mapped by an exhaustive `when`.

## (d) Token audit

- **15 distinct IDs carry tokens** for this gate: the 7 in-scope (009.A1, 032.A1, 032.A2, 033.A1, 034.A1, 034.A2, 049.A1) and the carried 8 (047.A1, 047.A2, 001.A1, 008.A1, 008.A2, 037.A1, 010.A1, 006.A1).
- Every `REQ-NNN.An` in the settings claim classes is immediately above a real `@Test` whose name carries the ID and whose assertion is that criterion's meaning. I read the token lines against the `@Test` lines for all 8 settings classes. Counts: FeedbackGateTest 4 tokens / 4 tests, ResetFlowTest 8 / 8, SettingsControllerTest 4 / 4, SettingsScreenTest 7 comment lines / 4 tests, SettingsScreenAppTest 5 / 5, the four held classes 1+1+2+1+1 = 6 / 6.
- **Token isolation by class** (my grep, same regex as trace-check):
  - REQ-032.A1 only in `HeldSettingsBoardAppTest` and `SettingsControllerTest`.
  - REQ-033.A1 only in `HeldSoundOffAppTest`, `HeldPlatformFeedbackAppTest` and `FeedbackGateTest`.
  - REQ-034.A1/A2 only in `HeldResetAppTest` and `ResetFlowTest`.
  - REQ-032.A2, REQ-009.A1 and REQ-049.A1 only in `SettingsScreenTest` and `SettingsScreenAppTest`.
  - The held IDs' app-level tokens occur only in `Held*AppTest` classes (the held-kit rule, N5 of the plan).
- **No token in any scaffolding, decision or guardrail test.** Files that carry only a `decision DA-n` or `guardrail G-nn` marker and no `REQ-NNN.An`: `SoundSynthTest`, `SoundSynthScaffoldingTest`, `LockedTextsTest`, `PxDrawingScanTest`, `SettingsControllerScaffoldingTest`, `AudioOutSmokeScaffoldingTest`, `PlayEventTableTest`, `PlayEventScaffoldingTest`, `InputEnabledScaffoldingTest`, `AfterResetTest`, `AfterResetScaffoldingTest`, `FeedbackMappingTest`, `FeedbackPathScanTest`, `SettingsOverlayScaffoldingTest`, `SettingsWiringScaffoldingTest`, `ReleaseSeparationTest`, `PromiseSourceScanTest`, `PromiseWordListEqualityTest`. Helpers (`FeedbackFakes`, `RecordingStore`, `RepoFiles`) have no token and need no marker.
- No dotted token anywhere under `.swdev/staged/WO-007/` or `.swdev/heldout/WO-007/` counts as a home: neither is a test tree, and the held copies are identical to the tree.
- **Claim tokens to claims, one to one:** the 7 WO-007 IDs plus the 8 carried each map to a real claim. Two extra tokened cases are controls of a claim and are legitimate: `req032_A2_theDifficultyWordMatcherCanFail` (the matcher control for A2) and `req009_A1_theFreeNoteIsNotOnAnyPlayScreenOrTheGrid` (REQ-009's rule "only in settings"). See N5.
- **Decision and guardrail coverage, DA-113…132** (a `decision DA-n` tag, a guardrail tag, a verifier test, or a process ruling):

| DA | Covered by |
|---|---|
| 113 module shape | V-06 PASS (`settings: {kernel, contracts}`), `ReleaseSeparationTest.PRODUCT_MODULES` + `settings`; no in-code tag (N2) |
| 114 sound and haptics | `SoundSynthTest`, `SoundSynthScaffoldingTest`, `AudioOutSmokeScaffoldingTest` (smoke PASS both channels, an output present on both, head advanced) |
| 115 which actions sound | `PlayEventTableTest`, `PlayEventScaffoldingTest`, `FeedbackMappingTest` |
| 116 literal test | `FeedbackGateTest`, `FeedbackPathScanTest` (rules 1-6, Dialog/Popup deny, canaries), `FeedbackProbe`, V-08 |
| 117 persistence in v1 | `SettingsControllerTest` (read-modify-write, `timerShown` survives), `HeldResetAppTest` (relaunch), LOCK-V1 files unchanged |
| 118 overlay, input, focus | `SettingsOverlayScaffoldingTest`, `SettingsWiringScaffoldingTest`, `InputEnabledScaffoldingTest`, `HeldSettingsBoardAppTest` |
| 119, 131 gear 48 / 24 dp | `HeldTouchTargetAppTest` (48 dp touch area on every play screen), screenshot `reviews/screens/WO-007-release-api26.png`; the 24 dp glyph is visual only (N3) |
| 120 reset | `ResetFlowTest`, `AfterResetTest`, `AfterResetScaffoldingTest`, `HeldResetAppTest` |
| 121 texts | `LockedTextsTest`, `SettingsScreenTest`, `PROMISE_TEXT_KEYS` in both copies |
| 122 how-to | the promise and language walks scan it; owner list |
| 123 plumbing | `FeedbackMappingTest`, `FeedbackPathScanTest`, `ReleaseSeparationTest`, V-08 |
| 124 contents order | `SettingsOverlay` (anchor); WO-008 inserts |
| 125 platform lever | `HeldPlatformFeedbackAppTest`, `FeedbackPathScanTest` rule 3, V-08 caller check (25 = 25) |
| 126 builds | process ruling (no code) |
| 127 Vibrator capability query | `v08_promise_apk.py` entry + `test_v08_feedback_*` cases (2 hits on the query name) |
| 128 declarations are not calls | `FeedbackPathScanTest` controls (lines 135, 287-290); no DA tag in the file (N2) |
| 129 CR-6 triage | each fix is in a row (050b, 052b, 056b/c, 057b, test author); verified in code and 93 verifier tests |
| 130, 132 failure routing | corrected checks: kit scroll fix, Finnish text, `HeldSoundOffAppTest` fixture; both held copies identical |

  **No gap.**

## (e) Contract integrity

- trace-check **reports no drift and no baseline-delta line.**
- All 72 files in `.swdev/contract-baseline.json` (`frozen: 2026-10-03`, `delta_refrozen: build-map.md 2026-10-04T12:12:07`) re-hashed with Python SHA-256: **72 of 72 match, 0 mismatches, 0 missing.**
  - `build-map.md` `e02fac59…` is the WO-006 close hash (v1.5). It is unchanged since. The last `contract-delta` in `.swdev/log/guard-log.jsonl` is 2026-10-04T12:12:07 (CLOSE6). **There is no WO-007 contract-delta event.**
  - `architecture.md` `5ac89340…` unchanged (the design says no edit).
  - `IProgressStore.kt` `0353e36e…`, `SavedGame.kt` `cbc6e11e…`, `IPuzzleLibrary.kt` `4c4f4f3a…`, `Puzzle.kt` `5c955224…`, kernel model files: equal.
- **LOCK-V1:** `progress-v1.json` `ab1c9d1e…`, `progress-v1-fresh.json` `2be7af50…` and `FrozenV1FixtureTest.kt` `4807518b…` are equal to the baseline. No `store` file (main or test) changed after the freeze. **The v1 save format is unchanged.** `settings.soundOn` was already in v1 (DA-117): the fixture holds `"soundOn": false`, and the sound setting is a read-modify-write of the existing `IProgressStore.settings()`.
- `IPuzzleLibrary` and `IProgressStore` are **not changed** (hashes equal). `resetAllProgress()` is called through the existing store, once, by `confirmReset`.
- The WO-007 "Contract deltas" table is empty: consistent. **No re-freeze is needed by this WO's code.** The only pending delta is CLOSE7's own `build-map.md` Edit (a logged notify-tier edit, followed by a re-freeze).

## (f) G-04 grep

`grep -rniE "devtools|0417"` over S = `<m>/src/main/**` (kernel, contracts, content, store, play, browse, **settings**, app) + `app/src/release/**` + `app/build.gradle.kts` + `settings.gradle.kts`:
- `app/build.gradle.kts:48: debugImplementation(project(":devtools"))`
- `settings.gradle.kts:25: include(":devtools")`

**Exactly the two DA-89 lines.** `settings/src/main` and `app/src/release` have no hit. The new `app` glob (`*/src/debug/**`) names no module and no "devtools".
`FeedbackProbe`, `LocaleOverrideActivity` and `TestConfig` appear only under `app/src/debug` (`DebugAids.kt`, `FeedbackProbe.kt`, `LocaleOverrideActivity.kt`, `TestConfig.kt`, the debug manifest) and in the test trees: 21 `androidTest` files (the held and visible kits and tests), `FeedbackPathScanTest` and `devtools` `ReleaseSeparationTest` (both guards, as they must). Nothing in any `main`, `release`, other module main, `tools` or build file. The release `DebugAids` twin does not name `FeedbackProbe`.

## Findings

- **B (blocker): none.**
- **S1 (should): REQ-033.A1's evidence is request-level, not heard.** The platform's own `playSoundEffect` has no external observer, and real audio and haptics cannot be observed on an emulator (design 3.6, T&V). The tests measure what reaches our two outs (`FeedbackProbe`, with a sound-on positive control per kind), what the Compose nodes asked of the platform (the lever's counters and the flag), and, for the whole APK, V-08's pinned caller list (25 sites = 25 rows). The loudness, the tone character and whether the tick can be felt are **owner-phone evidence only**. Keep this line in the close note (build-map #Settings note, workorder log, checkpoint 7) so the green is not read as "sound proven silent". Not a linkage gap. Record also that the smoke's playback head advanced on both channels (API 37 +1088 frames per cue, API 26 +720), so no row-16 waiver was needed.
- **N1.** `HeldSoundOffAppTest` could not run on its first execution (fixture, DA-132): authored compile-only, it reimplemented a placement the visible kit already solves in `SolveByTouch`. Corrected in both the tree and the held dir (identical). Source of the pattern is now an AGENTS lesson (line 155). The held-out compile proof does not prove a held test runs.
- **N2.** DA-113 and DA-128 have no `decision DA-n` tag in any code or test file. DA-113 is held by V-06, `ReleaseSeparationTest` and the build file; DA-128 by `FeedbackPathScanTest`'s controls. Add the tags when those files are next touched.
- **N3.** DA-119 / DA-131 (⚙ 48 dp touch area, 24 dp glyph centred in it) have no in-code anchor in `SettingsGear.kt`, and the 24 dp glyph is pinned by a screenshot (`reviews/screens/WO-007-release-api26.png`), not by a test. The 48 dp touch area is pinned by the touch-target walk (C5). Low risk; one comment line in `SettingsGear.kt` would close it.
- **N4.** The keyboard limit (CR-6 N1b): with the base's `onEnter = { cancelFocusChange() }`, a hardware-Tab from no focus cannot enter settings (Shift+Tab can). `SettingsOverlayScaffoldingTest` records it. #Accessibility is an `idea`, so no locked REQ is breached. Put it on the owner list (checkpoint 7).
- **N5.** Two tokened cases are controls of their claim (`req032_A2_theDifficultyWordMatcherCanFail`, `req009_A1_theFreeNoteIsNotOnAnyPlayScreenOrTheGrid`). Legitimate, but they raise the token count above "one per claim". The `REQ-032.A1` and `REQ-034.A1` module classes also hold several tokened `@Test`s that are engine-level readings of one criterion.
- **N6.** `ResetFlowTest` asserts against its own `RecordingStore` (CR-6 N6). Do not lean on it alone for REQ-034.A1. The held test and the store tests carry the meaning.
- **N7.** `tasks.md` bookkeeping, the orchestrator's: the header still reads "WO-007 planned" and v1.11; the AUDIT7 row says "in progress"; TASK-056's row says "G-04 grep = 1 line" (the current figure over the full set S is 2 lines, with `settings.gradle.kts`); TASK-T7a/T7b rows mention "13 originals restored" (fine). The workorder ledger names `reviews/WO-007-code-review.md`, but the file is `reviews/WO-007-CR-6-code-review.md`.
- **N8.** `app/build/outputs/apk/*` were rebuilt in TASK-058 run 2, so the V-04 / V-08 lines in the workorder log are the close evidence (CR-6 N7 is resolved). They are the only home of the artifact half of REQ-010.A2 and the feedback-caller result. Keep them there.
- **N9.** `trace_check.py:381` prints a `DeprecationWarning` (`re.split` positional `maxsplit`). It is in the SWDev tool (read-only to me).
- Checked and clean: no `Dialog(` / `Popup(` in any shipping module's `src/main` (the one in `devtools` is DA-125's accepted debug-only residue); no manifest in `settings`; no new dependency (D5); the new Finnish keys (13) are on `reviews/WO-007-owner-finnish-list.md` with 12 F14 keys and 1 AI key.

## CLOSE7: exactly what is owed

Contract note: `build-map.md` is notify tier. Use the Edit tool (not a shell write), so the guard logs the contract-delta. Then re-freeze `.swdev/contract-baseline.json` for the new hash (record `delta_refrozen`). Scripts that write docs: no backslash in a Python literal.

1. **`build-map.md`**
   - **Header:** Status `Current (P3, WO-006 closed)` to `Current (P3, WO-007 closed)`; Version 1.5 to 1.6; the date.
   - **§1 `#Settings` row:** Status `Built (WO-007, 2026-10-04)`.
     - REQ-032, 033, 034, 009 and 049 verified on API 37 and API 26.
     - Sound is synthesised (`SoundSynth`, `AudioTrack`) and the tick is `View.performHapticFeedback`: no asset, no permission (DA-114).
     - `settings.soundOn` already in v1 (DA-117); no format change.
     - One gate `Feedback` (DA-116). `PlatformFeedbackLever` in `app` suppresses the Compose click sound and the long-press haptic always (DA-125).
     - Reset is in-screen two-step and leaves the current puzzle New (DA-120).
     - **Note: REQ-033 A1's evidence is request-level; real audio and haptics are owner-phone evidence (S1).**
     - Deps column: replace "IPuzzleLibrary: titles for the best-time list / PlayTime: play time" with the truth: built against `IProgressStore` only; the best-time list and the play-time rows come with WO-008.
   - **§1 `#Locking` row:** "sounds/haptic tick in WO-007" becomes built (WO-007).
   - **§1 `#Promise` row:** REQ-009 stays WO-007 → built. Add V-08's feedback-caller check (25 sites = 25 pinned rows; `dex_callers.py`; any new caller of a platform sound or haptic API fails). REQ-049 (#Release) is rendered by `settings`: built.
   - **§1 `#Language`, `#Layout` rows:** add "settings screens verified (C1, C2; C5, C10)".
   - **§1 `#PlayTime` row:** "Planned" stays; add "inserts its rows into `settings`' screen (timer row, play time, best times) and reads `SettingsController.isOpen` for the pause (DA-124)".
   - **§2 WO-007 row, "Carried in / out":**
     - Replace with "in ← WO-001: return sound, lock tick (+ the WO-003 solve chime), built; in ← WO-006: C1, C2, C3, C5, C7 (sound, haptic, reset), C10 **re-verified with the same tokens, API 37 + API 26**".
     - Add "out → WO-008: the timer row in settings (REQ-031), the play-time and best-time section (REQ-029/030), time paused while settings is open via `SettingsController.isOpen` (DA-124), each re-verified under WO-008's own REQ-005 / 029 / 030 / 031 tokens".
     - Append "**Built 2026-10-04**: 7 IDs, T&V PASS with 1 corrected check".
   - **§2 WO-008 row:** add "in ← WO-007: the timer row, play-time and best-time section and the pause signal (`isOpen`) in settings".
   - **§3 change log:** a 1.6 row, "build status after WO-007 (pipeline-written, notify tier)", approved AI (checkpoint 7).
2. **`design-inputs.md` §2:** a new numbered item after item 5 (the tablet gap): the **⚙ touch area is 48 dp** with the glyph drawn at about 24 dp centred in it (DA-119, DA-131), where `Spec/02` §4 gives 44 dp. DA-53 (WO-004) had ruled 48 dp; REQ-037 sets the 48 dp floor. Cite `HeldTouchTargetAppTest` (C5) and `reviews/screens/WO-007-release-api26.png`. Add a change-log line in §3 of that file.
3. **`AGENTS.md`**
   - Line 133 ("Two device channels at every WO close"): the device suites become `play`, `browse`, `settings`, `app`, `devtools`.
   - Line 132 ("Release safety"): add the V-08 line: "V-08's feedback-caller check fails on any new sound or haptic caller, a Compose BOM bump included. Re-judge DA-125's inventory before extending its allow-list (`FEEDBACK_ALLOW` in `v08_promise_apk.py`)." Note that `dex_callers.py` is the shared reader.
   - "Current phase": WO-007 closed, next WO-008 (resume from `STATUS.md`).
   - Already present: the `solveByTouch` lesson (line 155) and the corrected Compose-import lesson (line 130). Optional lessons line: a held test is compiled, not run, before T&V, so the fixture trap shows only at the first run (N1).
4. **`workorders/WO-007.md`**
   - Status `In progress` to closed.
   - Ledger rows still blank: Acceptance Test Author (T7a / T7b / T7c / T7pk), Slice Implementers (050, 051, 052, 053, 054, 055, 056, 057, 059 + 050b, 052b, 053b / c, 056b / c, 057b), Code Reviewer (CR-6, `reviews/WO-007-CR-6-code-review.md`), Test & Verify (`reviews/WO-007-test-verify.md`, PASS), Traceability Auditor (this file).
   - Scope "Carried OUT": replace "(expected)" with the three items of (b).
   - Contract deltas: none (state it); the `build-map.md` Edit and re-freeze are CLOSE7's.
   - Waivers: none (the audio smoke found an output on both channels, so no row-16 waiver).
   - DoD boxes: tick them. On the verifier line note V-01, V-04 + `--positive-control`, V-05, V-06, V-07 and V-08 + `--expect-debug` green, and every `device_reset.py --check` clean.
   - Metrics:
     - Tests: JVM 675; device per channel play 116, browse 25, settings 8, app 115 (+ held cases), devtools 13; verifiers 93.
     - Findings by station: design review 1/4/8, plan review 2/10/8, CR-6 0/3/9, T&V 0/1/5, this audit 0/1/9.
     - Corrections: DA-129, DA-130, DA-131, DA-132.
     - Held-out first run: 1 failure (fixture, route b), 0 product.
     - Device time about 18 min per channel.
   - Log: the close line and this audit's line.
5. **`tasks.md`:** header Status line (WO-007 closed) and version; AUDIT7 row done, citing this file; CLOSE7 row done; TASK-056's "G-04 grep = 1 line" corrected to the 2 DA-89 lines of the full set S; a 1.12 change-log row for the WO-007 close.
6. **`STATUS.md`, `progress.md`**, and **checkpoint 7** with the owner items:
   - (1) the tones and the tick: loudness, character, whether the tick can be felt (S1);
   - (2) after a reset: the current puzzle stays, now New (DA-120), or the first puzzle;
   - (3) the Finnish how-to and the reset question's wording (`reviews/WO-007-owner-finnish-list.md`: 12 F14 keys accepted at G1, 1 AI key);
   - (4) the ⚙ at 48 dp with the 24 dp glyph (DA-119 / 131);
   - (5) Android's own touch click sounds and the long-press vibration on › are now always off, whatever the switch says (DA-125);
   - plus N4: a hardware-keyboard Tab cannot enter settings from no focus (#Accessibility is an idea).
7. **Keep the evidence where trace-check cannot see it:** the V-01, V-04, V-08, API 26 release launch and `dumpsys package` lines are in the WO-007 log (the TASK-058 run-2 row). Keep them there.
8. **Re-freeze** `.swdev/contract-baseline.json` after item 1 (new `build-map.md` hash; record `delta_refrozen`).

**WO-007 scope GREEN.**
