# WO-005 #DevTools: Test & Verify

Station: Test & Verify (fresh context), 2026-10-04. Executed, not read. Gradle one build at a time. No git commands.

**Verdict: HOLD, one held-out failure, classified fixture (threshold), not a product defect. The product behaviour is verified correct. A corrected check by the test author is needed. Escaped product defects: 0.**

## 1. Visible baseline (counts from the XML results)

The JVM suite was re-run with `cleanTest test --no-build-cache` (first run was a cache hit, so it was forced). The device suites ran on API 37 (`Medium_Phone_API_37.0`).

| Suite | Expected | Measured | Failures |
|---|---|---|---|
| kernel JVM | 141 | 141 | 0 |
| content JVM | 47 | 47 | 0 |
| store JVM | 68 | 68 | 0 |
| play JVM | 188 | 188 | 0 |
| browse JVM | 66 | 66 | 0 |
| app JVM | 4 (+2 release) | 6 | 0 |
| devtools JVM | 24 | 24 | 0 |
| play device (API 37) | 115 | 115 | 0 |
| browse device | 25 | 25 | 0 |
| app device | 30 | 30 | 0 |
| devtools device | 10 | 10 | 0 |

No difference from the orchestrator baseline.

## 2. Held-out first run

Files were copied from `.swdev/heldout/WO-005/` to the mirrored paths. The originals stay in `.swdev/heldout/` (the WO-004 precedent keeps them). The test packages are `held` sub-packages.

| Class | Tests | Tokens |
|---|---|---|
| `devtools/src/test/.../devtools/held/HeldDevToolsStateTest` | 4 | A1 |
| `devtools/src/test/.../devtools/held/HeldDevSolvePosesTest` | 1 | A3 |
| `app/src/test/.../tangram/held/HeldSolveNowStoreTest` | 4 | A3, all 13 puzzles |
| `devtools/src/androidTest/.../devtools/held/HeldDevDialogDeviceTest` | 3 | A1, A3 (dialog half) |
| `app/src/androidTest/.../acceptance/held/HeldPasscodeAppTest` | 4 | A1 |
| `app/src/androidTest/.../acceptance/held/HeldSolveNowAppTest` | 4 | A3 |
| `HeldAidAppKit.kt` | adapter | none |

Totals: JVM 9 (app 4, devtools 5). Device 11 (app 8, devtools 3). Device totals after the move-in: app 30 to 38, devtools 10 to 13.

| Criterion | JVM | API 37 device | API 26 device |
|---|---|---|---|
| REQ-046.A1 (wrong code refused, 0417 unlocks, stays unlocked until restart) | pass 4/4 | pass (HeldPasscodeAppTest 4/4, HeldDevDialogDeviceTest A1 1/1) | pass |
| REQ-046.A3, stored half (Solved, no best time, earlier best kept, other puzzles untouched) | pass (HeldSolveNowStoreTest 4/4 over all 13 puzzles, HeldDevSolvePosesTest 1/1) | pass (3 stored tests, HeldDevDialog A3 2/2) | pass |
| REQ-046.A3, picture half `HeldSolveNowAppTest.req046_A3_solveNowShowsTheSolvedPictureThroughTheNormalTimeline` | n/a | **FAIL** | pass |

- API 37 totals: app 38 tests, 1 failure. devtools 13, 0 failures. play 115, browse 25, 0 failures.
- API 26 totals: play 115, browse 25, app 38, devtools 13, all 0 failures. API 26 ran after the API 37 emulator was stopped with `adb emu kill`, and the API 37 result files were not reused.

### The failure

- Test: `HeldSolveNowAppTest.kt:75`.
- Expected: the early count of the picture's base colour is `<= 3`.
- Got: `4`. The message was "shortly after the solve the picture is already there (no timeline?): 4".
- Classification: **fixture/threshold defect, not a product defect.** Evidence from a temporary diagnostic test (written, run on API 37, then deleted):
  - Base colour `e9c46a`. Count over time after the solve at t = 230, 430, 630, 830, 1030, 1230, 1430 ms: 4, 1, 2, 2, 72, 10007, 21790.
  - The four early hits are isolated pixels with values such as `ffe7bf69`, `ffe4c764` and `ffe6bf70`. They sit at piece edges. They are anti-aliased blends of the yellow, orange and pink piece colours that fall within the 10-per-channel tolerance of the base colour.
  - The picture really does appear only after the REQ-023 delay (0 at ~830 ms, 72 at ~1030 ms, full at ~1230 ms). The normal timeline runs.
  - The same test passes on API 26, so the count is platform-dependent noise at the threshold, not a behaviour difference.
  - The no-picture baseline `flat <= 3` passed. The noise level straddles the threshold.
- Why not corrected here: the only fix is the `<= 3` tolerance (or the colour tolerance of the `baseCount` sampler). The corrected-check rule forbids tolerance changes. Route back to the test author. Suggested change: compare the early count with a small fraction of the late count (for example early `<= 2%` of late, or `<= 20`), since late is about 21,000 and early is 4.
- The play-module `AidTimelineDeviceTest` (visible) separately pins the aid-solve timeline and passes on both channels.

## 3. Corrections

One corrected check, adapter only:

- `app/src/test/kotlin/io/github/jamisuni/tangram/held/HeldSolveNowStoreTest.kt` line 70.
  - The call `session.onFrame(1000)` did not compile: `PlaySession.onFrame` is `internal` in `play`, and the test lives in `app`.
  - The call was removed and a comment line put in its place. No assertion, tolerance, sample or `REQ-` token changed.
  - It is safe to drop. `solveByAid` sets Solved and fires `onSolved(true)` and `onChanged` at the call, and the test saves through `controller.persist()`. The frame only starts the visual timeline.
  - After the change the test compiled and passed 4/4.
  - The staged original in `.swdev/heldout/WO-005/` is unchanged, so the test author should fold the correction in.

## 4. Escaped defects

- Product defects escaped to Test & Verify: 0.
- Held-out failures: 1 (fixture threshold, API 37 only, class (b), awaiting a test-author correction).
- Adapter compile defects: 1 (corrected, above).

## 5. Substance audit, REQ-046 A1 to A4

Read: `DevSolutionTest`, `ReleaseSeparationTest`, `DevSolutionOverlayTest`, `ShowSolutionAppTest` (assertions), all six held classes, V-04 evidence in the WO log.

| ID | Coverage | Real check? |
|---|---|---|
| A1 | held: state (exact `0417` only, no trim, other inputs refused, per-state unlock), dialog device test (wrong code shows the notice and tools stay hidden, 0417 shows the tools), full-app test (unlock survives browsing, locked again after restart) | yes, strong. The English text check is guarded by locale, but the string-resource check always runs |
| A2 | `DevSolutionTest` (one shape per piece, polygon equals the stored polygon, 13 puzzles), `DevSolutionOverlayTest` (exact node set, none when off, bounds against an independent mapping), `ShowSolutionAppTest` (nodes, bounds, pixels in the piece colour) | yes, strong |
| A3 | held: poses for every puzzle, the real chain (`solveByAid` then `BrowseController` then `JsonProgressStore`) read back through a new store for all 13 puzzles, an earlier best kept (600 and 41), other puzzles untouched; app test of the picture through the timeline | yes. "No best time" is checked at the stored-document level (`bestSeconds` null), which the WO says is the event-level reading because real best times arrive in WO-008. The seed fixtures (77 s in progress) make an ordinary solve distinguishable, so the assertion cannot pass by accident |
| A4 | source half `ReleaseSeparationTest` (scan of the file set S, blindness guards, 6 control tests that prove the scanner can fail); artifact half V-04 (`v04_release_apk.py`, release PASS with canary, positive control 4 markers) | yes |

Findings:

- **B:** none.
- **S1:** the picture-half threshold in `HeldSolveNowAppTest` is too tight (see section 2). The assertion is meaningful but flaky on API 37. Needs a corrected check.
- **N1:** A4 has a source test token only. The artifact proof (V-04) carries no `REQ-` token by design (DA-81), so trace-check sees A4 through `ReleaseSeparationTest` alone. The proof is real, but trace-check cannot confirm the artifact half.
- **N2:** the A4 source scan proves absence of names and the passcode. It cannot prove that the release app shows no DEV button. V-04 on the built APK covers that.
- **N3:** A3 "shows the solved picture" depends on pixel counting at the board (`baseCount` with a colour tolerance), the weakest coverage in the set. Backed by the visible `AidTimelineDeviceTest` for the timeline.
- **N4:** no tautologies or unfailable assertions found. Scaffolding tests carry no `REQ-` tokens (`DevSolutionScaffoldingTest`, `DevUiScaffoldingTest`, `CrispEdgesScaffoldingTest`).

## 6. Files moved or changed

Copied in from `.swdev/heldout/WO-005/` (originals left in place):

- `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/HeldAidAppKit.kt`
- `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/HeldPasscodeAppTest.kt`
- `app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/held/HeldSolveNowAppTest.kt`
- `app/src/test/kotlin/io/github/jamisuni/tangram/held/HeldSolveNowStoreTest.kt` (corrected, see section 3)
- `devtools/src/androidTest/kotlin/io/github/jamisuni/tangram/devtools/held/HeldDevDialogDeviceTest.kt`
- `devtools/src/test/kotlin/io/github/jamisuni/tangram/devtools/held/HeldDevSolvePosesTest.kt`
- `devtools/src/test/kotlin/io/github/jamisuni/tangram/devtools/held/HeldDevToolsStateTest.kt`

Also created: this file. A temporary diagnostic test (`ZzDiagTest.kt`) was created and deleted.

## 7. Emulators

API 37 was stopped with `adb emu kill` per the brief. API 26 was booted, used, then stopped with `adb emu kill`. `adb devices` shows no device. No emulator is running.

## Re-run 2026-10-04 (after DA-95, TASK-T5c, TASK-037b)

Changes under test: `HeldSolveNowAppTest` early bound `earlyBase * 50 <= lateBase` and `flat <= 20` (token and other assertions unchanged; in-tree copy is byte-identical to `.swdev/heldout/WO-005/`, checked with `cmp`); `HeldSolveNowStoreTest` carries my `onFrame` removal; TASK-037b nits in `CrispEdgesScaffoldingTest` and `play` drawing. I edited no assertion.

Run: JVM `cleanTest test --no-build-cache`, then the four device suites on API 37, then API 37 stopped, `Phone_API_26` booted (`boot_completed` = 1), the same four suites, API 26 stopped.

| Channel | play | browse | app | devtools | Failures |
|---|---|---|---|---|---|
| JVM (kernel 141, content 47, store 68, play 188, browse 66, app 10 = 6 + 4 held, devtools 29 = 24 + 5 held) | 188 | 66 | 10 | 29 | 0 |
| API 37 device (expected 115 / 25 / 38 / 13) | 115 | 25 | 38 | 13 | 0 |
| API 26 device (expected 115 / 25 / 38 / 13) | 115 | 25 | 38 | 13 | 0 |

Held-out per ID:

| ID | JVM | API 37 | API 26 |
|---|---|---|---|
| REQ-046.A1 | pass | pass | pass |
| REQ-046.A3 (stored half and picture half) | pass | pass | pass |

The corrected check passes on both channels. The `earlyBase` / `lateBase` numbers come from a temporary diagnostic that repeats the test's flow (written, run, deleted; passing tests print nothing):
- API 37: flat 0, earlyBase 4, lateBase 21943 (4 * 50 = 200 <= 21943, margin about 110x).
- API 26: flat 0, earlyBase 1, lateBase 21981.

Escaped product defects: 0. Held-out failures after correction: 0.

**Final verdict: PASS.** All visible and held-out tests are green on JVM, API 37 and API 26. No emulator is running (`adb devices` is empty); the API 37 one that was up at the start was stopped as the brief required.
