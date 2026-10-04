# WO-005 traceability audit

Auditor: Traceability Auditor, 2026-10-04. Read-only run. No Gradle, no emulator, no git. Format follows `reviews/WO-004-trace-audit.md`.

**Verdict: WO-005 scope GREEN on linkage** (REQ-046 A1-A4 each has a token carrier under its code home; no orphan; no drift; contract and G-04 clean).
One condition outside my gate: Test & Verify's status is HOLD (TASK-T5c corrected the A3 picture-half threshold; the device re-run is not yet recorded). "Green" in the sense of a green chain needs that re-run to pass on API 37. Not a linkage gap.

## Run
trace-check: 51 REQs (47 locked, 4 withdrawn), 144 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, **no drift section, no baseline line**. Project-wide RED as expected.
Fail list (20 REQs): 001, 005, 006, 008, 009, 010, 029-037 (9), 039 (A2), 042, 047, 048, 049.
**REQ-046 is not in the fail list.** All four criteria are found covered. In WO-004 it was in the list, so it is now covered by the T&V move-in of the held-out tests. REQ-047 is later WO-006.
Verifier suite `test_verifiers.py`: Ran 52, OK (re-run by me, pure Python).

## (a) REQ-046 per criterion
Task chain from the tasks.md index: A1 -> 032, 033, 034. A2 -> 031a, 032, 033, 034. A3 -> 031b, 032, 033, 034. A4 -> 030 (inputs), 034 (pair), 035 (V-04).

| ID | Code (home) | Token-carrying tests | Home ok | Note |
|---|---|---|---|---|
| A1 wrong code refused, 0417 unlocks, stays unlocked | `devtools` `DevPasscode`, `DevToolsState`, `DevCornerButton`; `app` `DebugAids` pair | held: `devtools/src/test/.../held/HeldDevToolsStateTest` (4 claims); `devtools/src/androidTest/.../held/HeldDevDialogDeviceTest`; `app/src/androidTest/.../acceptance/held/HeldPasscodeAppTest` (4 claims) | yes | held-out, now in tree (T&V). JVM 4/4, API 37 and 26 green per T&V |
| A2 one shape per piece on the silhouette | `devtools` `DevSolution`, `SolutionShape`, `DevSolutionOverlay`; `play` `boardOverlay` slot, `BoardSpace`; `app` wiring | data: `devtools/src/test/.../DevSolutionTest`. Draw (device): `devtools/src/androidTest/.../DevSolutionOverlayTest`; `app/src/androidTest/.../acceptance/ShowSolutionAppTest` | yes | both halves present: data (13 puzzles, polygon equals stored) and device draw (node set, bounds, pixel colour) |
| A3 solve-now shows picture, no best time | `devtools` `DevSolution.poses`; `play` `PlaySession.solveByAid` (+ `onFrame` order); `app` wiring | held: `devtools` `HeldDevSolvePosesTest`, `HeldDevDialogDeviceTest` (dialog half); `app/src/test/.../held/HeldSolveNowStoreTest` (13 puzzles, event and stored level); `app` device `HeldSolveNowAppTest` (picture half + 3 stored claims) | yes | held-out, now in tree. Reading "no best time" is at event (`solveByAid`) and stored-document (`bestSeconds` null, earlier best kept) level. Best times arrive in WO-008 (carried OUT, build-map row) |
| A4 release build has no DEV button | `app/src/release/DebugAids.kt` (no-op), `devtools` not in `release`; build lines `include(":devtools")`, `debugImplementation(project(":devtools"))` | source half: `devtools/src/test/.../ReleaseSeparationTest` (token at line 129) over file set S with DA-89 exemptions, blindness guards and 6 control tests. Artifact half: V-04 `.swdev/verifiers/v04_release_apk.py` (no token by design, DA-81) | yes | evidence lines in `workorders/WO-005.md` log, 2026-10-04: `V-04 PASS: app\build\outputs\apk\release\app-release-unsigned.apk` (canaries found, exit 0) and `V-04 POSITIVE-CONTROL PASS: all 4 marker kinds found` (exit 0). Artifact half is outside trace-check by design; recorded reason = DA-81 |

CR-3 F7 (A2 and A4 in halves) is closed here: the A2 data half and device half, and the A4 source half and artifact half, are all present and named above.
IDs with no token under a valid home: **none** (4 of 4). The token-carrying files are in `devtools` and `app` (the plan names both).

## (b) Orphan check (main code in WO-005)
Every new or changed main file traces to a REQ, decision or guardrail:

| File | Anchor |
|---|---|
| `devtools/.../DevCornerButton`, `DevSolutionOverlay` | REQ-046, DA-75/80/82/87 |
| `DevPasscode` | DA-78. `DevToolsState` DA-76. `DevSolution` DA-6, G-10. `DevStyle` G-06 |
| `SolutionShape` | "design WO-005, frozen seam D-2" (header text, no DA id) |
| `DevNotice` | none in file (a 3-value enum), used by state and button |
| `devtools/src/main/res/values*/strings.xml` | V-05 (15 keys en + fi), DA-79/90 |
| `app/src/debug/.../DebugAids`, `app/src/release/.../DebugAids` | REQ-046, DA-72, DA-76 |
| `AppViewModel` (`aids`), `TangramApp`, `MainActivity` | REQ-046, DA-71/75/76/82 |
| `play/.../BoardSpace` | DA-74. `PlaceSecondary` DA-75. `PlayArea` DA-71/74/75 |
| `PlaySession.solveByAid` | DA-73, DA-83, REQ-023 |
| `play/.../draw/PlayDrawing`, `PictureDrawing`, `VisualTokens`, `DpScope`, `PuzzleThumbnail` | DA-92 (REQ-011/012/023/050 at minSdk 26; waiver DA-43 exit, DA-93) |
| `browse/.../BrowseStyle.kt` | mtime 2026-10-03 20:50, the MOVE-JVM A4 cache proof (comment edit, reverted per the log). G-04 grep clean. I could not confirm byte equality without git (see N3) |

D3 (unjustified abstractions): none. The seams are all in a design cut and a task: `DebugAids` pair (ADR-006/DA-72), `boardOverlay` and `secondaryCornerControl` slots (DA-74/75), `solveByAid` (DA-73), `PlaceSecondary` (pure, swept by 468 cases).
Nit: `DpScope.kt` now holds only a comment (no code). It is a tombstone for the removed `inDp` and nothing references it.

## (c) Token audit
- Every dotted `REQ-046.A` token in the in-tree test files is real coverage claims, and every carrying file is under a valid home. A2 tokens: `DevSolutionTest`, `DevSolutionOverlayTest`, `ShowSolutionAppTest`. A4: `ReleaseSeparationTest`. A1/A3: the held files listed above.
- Scaffolding tests carry no `REQ-` token: `DevSolutionScaffoldingTest`, `DevUiScaffoldingTest`, `CrispEdgesScaffoldingTest` and the `Scaffold…` trap tests use `// decision DA-n` / `// guardrail`. Confirmed by a grep of `REQ-046` in `*.kt`.
- Rule-prose mentions ("REQ-046 rule 3/5" with no dotted ID) in `DevSolutionTest`, `DevSolutionOverlayTest`, `ShowSolutionAppTest` are labelled "prose, not an acceptance claim". Not tokens.
- Non-test mentions (informational): KDoc in `contracts` `IProgressStore`, `SavedGame`, `IPuzzleLibrary`, `Puzzle`; `AppViewModel`; V-04 docstring; `tools/tests/test_prototype.py`. None is a token.
- Duplicate copies of the same tokens under `.swdev/staged/WO-005/` and `.swdev/heldout/WO-005/` are not test homes and carry no extra claim.
- Held-out copies: all 7 files in `.swdev/heldout/WO-005/` are byte-identical to the in-tree copies (cmp), including the T5c fold-in of the `onFrame` correction.

Decision-test coverage DA-72...95 (file with a `decision DA-n` tag or V-04 test): 72, 73, 74, 75, 76, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 92 each have at least one. DA-90 is covered by the V-04 synthetic tests (`test_v04_devtools_values_threshold`, `…shared_value_exclusion_synthetic_root`, `…missing_devtools_strings_is_exit_2_no_fallback`; no DA tag in the test names). DA-77 (aid solve stores what any solve stores) is covered through the A3 held store tests. DA-84 is a dependency rule (V-06). DA-91, 93, 94, 95 are process and triage rulings (no code). No gap.

## (d) Contract integrity
- trace-check drift/baseline: **none reported**.
- All 72 files in `.swdev/contract-baseline.json` (`frozen: 2026-10-03`) re-hashed with Python SHA-256: **72 of 72 match, 0 mismatches, 0 missing**. This covers every path in `.swdev/guard.json` locked and notify tiers.
- LOCK-V1 pins, current = baseline:
  - `store/src/test/resources/fixtures/progress-v1.json` ab1c9d1e53ad861e...b63c
  - `store/src/test/resources/fixtures/progress-v1-fresh.json` 2be7af50d2ae54bb...e109
  - `store/src/test/kotlin/.../store/FrozenV1FixtureTest.kt` 4807518bbfc3dd47...2b88
- Also matching: `architecture.md` 5ac89340...0731; `build-map.md` c5830c25...336 (the baseline records `delta_refrozen: build-map.md 2026-10-03T11:41:10`, unchanged since); `IProgressStore.kt` 0353e36e...742c; `IPuzzleLibrary.kt` 4c4f4f3a...5ab2.
- The v1 save format is **unchanged** (fixtures and frozen test match; no store main file in the baseline differs). DA-77 and the held A3 store tests use the existing fields. `IPuzzleLibrary` and `IProgressStore` are **not changed** by WO-005 (hashes equal baseline).
- WO-005 "Contract deltas" table is empty, which is consistent. No re-freeze needed or done by me.
- The working tree shows `.swdev/contract-baseline.json` as modified, but its content is the WO-004 re-freeze, and the files match it. I did not check whether the baseline file itself changed after its freeze (no git).

## (e) G-04 grep
`grep -rniE "devtools|0417"` over S = `<m>/src/main/**` for kernel, contracts, content, store, play, browse, app, plus `app/src/release/**`, `app/build.gradle.kts`, `settings.gradle.kts`:
- `app/build.gradle.kts:47: debugImplementation(project(":devtools"))`
- `settings.gradle.kts:25: include(":devtools")`

**Exactly the two DA-89 lines, nothing else.** `app/src/release` has no hit. `app/src/debug` is not in S (the debug twin is allowed).

## (f) build-map vs what was built
`build-map.md` still reads as planned. The WO-005 close must write:
1. Section 1 `#DevTools` row: Status `Planned` -> `Built (WO-005, 2026-10-0x)`: `devtools` Android library (debug only): DEV pill, passcode 0417, solution overlay, solve-now through the `DebugAids` pair neutral hook (DA-72); `play` second corner slot (`secondaryCornerControl`) and `boardOverlay`, `PlaySession.solveByAid`.
2. V-04 live: the release-APK dex and resource scan with positive control, folding in `tools/check_apk_puzzles.py`. Either a build-map line or `#DevTools`, whichever the close chooses. Also the CR-3 F3 line: run `--positive-control` after any toolchain, AGP or dex-affecting change (DA-91).
3. WO-005 row: A3 is checked at event level (`byAid`) and the carried-OUT item to WO-008 (aid-solve regression on real best times) stays. The "in <- WO-003 fold" is done.
4. The second device channel: `Phone_API_26` AVD (Android 8.0), DA-43 waiver exited (DA-93), device suites run on both API 37 and API 26. That includes the DA-92 finding (draw every path in px, minSdk 26).
5. build-map is notify tier: edit it with Edit/Write so the guard logs the contract-delta. Then re-freeze the baseline for its new hash.
Other close duties: ledger rows (Acceptance Test Author, Slice Implementers, T&V, this audit) are blank in `workorders/WO-005.md` and "Status" is still "In progress"; `tasks.md` header still says "WO-005 planned" (Status `Current (WO-001...WO-004 closed; WO-005 planned)`).

## Findings
- **B (blocker): none.**
- **S (should): S1.** The WO is not closed on T&V: `reviews/WO-005-test-verify.md` is HOLD. The corrected A3 picture-half check (TASK-T5c, DA-95) must pass on API 37 in the re-run before close. Linkage is complete, but the chain is not green until this lands. Note T&V's report predates T5c, and the "in-tree = held-out" cmp holds, so the re-run will be on the corrected file.
- **N1.** A4's artifact half has no token by design (DA-81). trace-check cannot see it. The recorded evidence is the two V-04 lines in the WO log. At close, keep them there, since the WO log is the only home.
- **N2.** `DpScope.kt` is a comment-only file after DA-92 (D3 tidy). Delete it when `play` is next touched, or keep it as the "why no scale" note. It carries no behaviour.
- **N3.** `BrowseStyle.kt` was touched in the MOVE-JVM A4 cache proof (20:50, reverted per the log). I could not diff it (no git). The G-04 grep is clean and the file is 711 bytes. The orchestrator may confirm with `git diff` before commit.
- **N4.** `DevNotice.kt` has no anchor comment (the other devtools main files carry a DA/REQ). Cosmetic.
- **N5.** Count drift already corrected by TASK-035c: DA-90 says 25 values denied, the stderr note says 24 denied plus 1 excluded. DA-90 and TASK-035b text still say 25 (the correction is noted in the TASK-035c row). Not an audit gap.
- **N6.** The WO-005 file still says Status "In progress", the Station ledger rows for Acceptance Test Author, Slice Implementer(s) and T&V are empty, and the DoD boxes are unticked. Close bookkeeping, orchestrator's.

**WO-005 scope GREEN (linkage), conditional on the T&V re-run (S1).**
