# WO-009 #Release: Test & Verify

Station: Test & Verify T&V9 (fresh context), 2026-10-05. Executed, not read. One Gradle build and one emulator at a time. No git. No product code and no test edited. No flag set and no `mark` run on the real tree (0 of 25 puzzle files `true`, no `release/puzzle-review.json`, checked after the runs).

**Verdict: PASS (with 0 B / 1 S / 3 N).** The held `REQ-048.A3` method passed on first run on both channels (API 26 and API 37), and so did the unchanged `REQ-010.A2` beside it. 0 escaped defects. Both staged carriers are red on their assertion lines for the real state and green on positive scratch inputs. Visible JVM baseline 835/835. The held file stays in place (HELD-IN9 follows).

## 1. Inputs

- Carriers, sha256 checked against the brief and the scratchpad `wo9_carriers_sha.txt`: `ReleaseLibraryReviewedTest` 31c3bef5eef859def4356b64b7c625fa5df419527bf1817101b83b24c3bd27bc (equal); `ReleaseEvidenceTest` c8b33dc04f96af343a105dff08165ce4870b7e9f6fbe43126696622a4ca7cff1 (equal). See N1 for the tasks.md prefix.
- Held file `.swdev/heldout/WO-009/.../held/HeldInstalledPermissionsAppTest.kt` sha256 d09828800eba0a54b6037aa207ef6f13fed178cc1af84dfb347cbaadce32bc76 (equal to the brief). The held file was read in full before the run.
- **E1 backup:** the in-tree file (sha256 bf975022c3d64a417a8ba0d2b6d7ba04c99a0e43ce5e607238b39a122678caa7, the brief's value) copied to `scratchpad/tv9bak/HeldInstalledPermissionsAppTest.kt.inTreeBackup`, SHA re-checked equal after the copy. The held file was then copied over the in-tree path by path; the in-tree SHA is now d0982880... (equal to the held file), and was re-checked after both channels.

## 2. Carrier controls (scratch repo copy under `scratchpad/tv9/`, excluding build, .gradle, .git; removed with the guarded form afterwards)

**`ReleaseLibraryReviewedTest` (`:content:test --rerun --tests *ReleaseLibraryReviewedTest`)**

| Input | Result |
|---|---|
| Real state (0 of 25 `true`) | RED, 2 of 2, on the assertion lines: `ReleaseLibraryReviewedTest.kt:40` (`req042_A1`: "only 0 of 25 puzzles have reviewedByHuman: true (REQ-042 needs at least 20)") and `:48` (`req039_A2`: "25 of 25 puzzles still have reviewedByHuman: false: [...]"). Not a canary `error`. |
| Positive: all 25 flags `true`, ledger entries made by `puzzle_review.mark_files(..., _cli=True)` on the copy only (25 marked; ROOT asserted to be the scratch copy before the call) | GREEN, tests=2 skipped=0 failures=0 |

**`ReleaseEvidenceTest` (`:app:testDebugUnitTest --rerun --tests *ReleaseEvidenceTest`)**

| Input | Result |
|---|---|
| Real checklist (all rows `waiting-for-owner`) | RED, 3 of 3, all at `ReleaseEvidenceTest.kt:76` (`assertEquals ... "done"`): "the row REQ-048.A1 / REQ-048.A2 / REQ-001.A2 is not done ... expected done but was waiting-for-owner" |
| Positive: scratch checklist, the three rows `done`, owner `jami`, evidence `release/evidence/live.txt` with `live-title: Tangram, absolutely free` (REQ-048's current A2 title), `privacy-policy-link: https://example.org/privacy`, `labels: none` | GREEN, tests=3 skipped=0 failures=0 |
| Negative variants on the same scratch | `labels:` empty: canary `IllegalStateException` at :95 on A1 and 001.A2 (the CR-8 N2 vacuous pass is closed). `labels: Contains ads`: assertion failure :101. Link `TBD-OWNER` and title `Wrong`: assertion :126 and ComparisonFailure :137. Evidence file missing: assertion :81. |

Both carriers are fail-loud on a missing or empty input. A copy of the positive checklist is kept at `scratchpad/tv9/positive-checklist.md.keep`.

## 3. Held-out first run (E1 overlay, `HeldInstalledPermissionsAppTest`, 2 methods)

Command: `cmd.exe //c ".\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.jamisuni.tangram.acceptance.held.HeldInstalledPermissionsAppTest --console=plain"` with `ANDROID_SERIAL`. `device_reset.py --serial S` run before and after each channel.

| Channel | Serial / AVD (confirmed) | reset before / after | `req010_A2_...` | `req048_A3_...` |
|---|---|---|---|---|
| API 26 | emulator-5554, Phone_API_26 (sdk 26) | clean / clean | PASS | PASS |
| API 37 | emulator-5554, Medium_Phone_API_37.0 (sdk 37) | clean / clean | PASS | PASS |

JUnit XML both channels: tests=2 failures=0 errors=0 skipped=0. Emulators stopped with `emu kill` after each. **Held file kept in place** (PASS on both channels); no restore was needed.

## 4. Visible baseline

JVM, each module's unit-test task with `--rerun` (counts from the JUnit XML): kernel 168, content 52, store 68, play 234, browse 74, settings 61, time 65, app 82, devtools 31 = **835, 0 failed, 0 skipped**. Also: `test_verifiers.py` 114 OK, `test_v09_acceptance.py` 34 OK, `tools/tests/test_puzzle_review.py` 25 OK. Device visible suites were not re-run here (MOVE-DEV9 / TASK-099 own them; the brief asked for the JVM baseline only).

## 5. Substance findings

- **Token greps** (all in-tree test sources and verifiers, build dirs excluded): `REQ-042.A1`, `REQ-039.A2`, `REQ-048.A1`, `REQ-048.A2`, `REQ-001.A2`: none in tree. `REQ-042.A2` and `REQ-042.A3`: only `content/.../release/ReleaseLibraryTest.kt`. `REQ-048.A3`: only the held file (once, the method comment). `REQ-010.A2`: still there (held file; the `PromiseSourceScanTest` mention is written without the dot). No overclaim.
- **`REQ-048.A3` method:** asserts the target package (not the `.test` package, and different from the test package) requests no permissions and defines none, with the control that the `android` package shows permissions, so an empty answer is real. This is what the token claims. The store-bound limit is stated in the class KDoc (debug build under test, not the Play file; the store-bound proof is V-01/04/08/09 on the universal APK, the API 26 `dumpsys` line and the manual store row).
- **Waiver exit conditions (DA-158, design "Waiver and close")** are real and observable: A1/039.A2 need `mark` for every shipped puzzle (carrier reads the flags; V-09 adds the ledger and STALE checks); 048.A1 / 001.A2 need a `done` row with evidence and an explicit `labels:` statement; 048.A2 compares the live title with REQ-048's A2 text read from the file, so it stays red until CA-12 re-locks it (not a vacuous pass).
- **TASK-099 step 3 and the `mark` controls:** recorded in `tasks.md` (positive, STALE by byte edit, unledgered, step 2 again, hashes unchanged). Spot-check green: `test_v09_acceptance.py` 34 OK; `test_puzzle_review.py` 25 OK. My own scratch control (`mark_files` on a copy, 25 of 25, then V-09-style carrier green) agrees.
- **N1 (nit)** `tasks.md` TASK-T9c row still shows the earlier ReleaseEvidenceTest prefix `7fc80cc5` and "red at :73"; the file now carries `c8b33dc0...` (red at :76, after the CR-8 N2 fail-loud fix), which the later rows and the scratchpad record. Re-record the full sha256 of both carriers at CLOSE9 (CR-8 N4 asked for it).
- **S1 (should)** `ReleaseLibraryReviewedTest` reads only the `reviewedByHuman` flag, not the ledger. A flag set by hand without `mark` would make it green. The ledger and STALE protection live only in V-09 (strict, on the store-bound APK). Acceptable under G-08's "speed bump" limit, but RELEASE-DAY must run V-09 strict before moving the carrier in, and the move-in step should say so. Route: CLOSE9 / RELEASE-DAY checklist wording, no code change.
- **N2 (nit)** the carrier's "library holds one puzzle per file" canary compares the packaged library with `Tangrams/`; both come from the same files, so it can only catch a parser rejection or a stale package, which is its stated purpose.
- **N3 (nit)** `./gradlew :<module>:test --rerun` is the lifecycle task and re-runs nothing for Android modules (it showed `UP-TO-DATE`); the counts above come from `:<module>:testDebugUnitTest --rerun`. The commands in `tasks.md` should name `testDebugUnitTest` for play, browse, settings, time, app, devtools.

## 6. Routes

- **B:** none. **S1:** CLOSE9 / RELEASE-DAY wording. **N1:** CLOSE9 (record full SHAs). **N3:** note for the next WO's command lines.
- HELD-IN9 next: `cmp` of the in-tree file with `.swdev/heldout/WO-009/...` (equal now), the backup with SHA bf975022 is in the scratchpad, `:app:compileDebugAndroidTestKotlin --rerun`, and the token grep (`REQ-048.A3` once, `REQ-010.A2` present).
- Real-tree state after this station: no flag `true`, no ledger, no real checklist change, only the in-tree held file replaced (as E1 provides).
