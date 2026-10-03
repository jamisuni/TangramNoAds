# WO-003 traceability audit

Auditor: Traceability Auditor, 2026-10-03. Read-only run. Raw trace-check output: scratchpad `trace_wo3.txt`.
Verdict: **WO-003 scope RED** on one mechanical finding (REQ-002.A2 has no covering test under `play/`). Everything else is green.

## Run
trace-check: 51 REQs (47 locked, 4 withdrawn: 004, 027, 028, 044), 63 test files scanned, req-lint OK (0 errors), views fresh, drift: none.
Project-wide result RED, as expected. 26 REQs fail as uncovered (all later WOs) and 1 fails rule 3 (REQ-002, in scope, below).

## (a) Coverage per ID (dotted tokens only; play = `play/src/...`, app = `app/src/androidTest/.../acceptance/`)
| ID | Test file(s) carrying the token | Home |
|---|---|---|
| REQ-002 A1 | play test `SolvingAcceptanceTest`; app androidTest `FirstPuzzleSolvedTest` | ok |
| REQ-002 A2 | app androidTest `NoModeScreenTest` ONLY | **GAP: no token under play/** (trace-check rule 3) |
| REQ-011 A1, A2 | play androidTest `BoardPixelsTest` | ok |
| REQ-012 A1 | play test `TrayAcceptanceTest`; play androidTest `TrayDeviceTest` | ok |
| REQ-012 A2 | play test `held/DropHeldTest` | ok |
| REQ-013 A1, A3 | play test `TrayAcceptanceTest` | ok |
| REQ-013 A2 | play test `TrayAcceptanceTest`; play androidTest `TrayDeviceTest` | ok |
| REQ-014 A1 | play test `held/GestureHeldTest` | ok |
| REQ-014 A2 | play test `DragFlipAcceptanceTest` | ok |
| REQ-015 A1, A2 | play test `held/GestureHeldTest`; play androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-016 A1 | play test `TrayAcceptanceTest` | ok |
| REQ-016 A2 | play test `held/GestureHeldTest` | ok |
| REQ-017 A1 | play test `held/GestureHeldTest`; androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-017 A2 | play test `held/GestureHeldTest` | ok |
| REQ-018 A1 | play test `DragFlipAcceptanceTest`; androidTest `TrayDeviceTest` | ok |
| REQ-018 A2 | play androidTest `FlipBadgeTest` | ok |
| REQ-018 A3 | play test `held/DropHeldTest` | ok |
| REQ-022 A1 | play test `SolvingAcceptanceTest`; kernel test `KernelAcceptanceTest` | ok |
| REQ-023 A1, A2 | play androidTest `SolvedPictureTest` | ok |
| REQ-043 A1, A2 | play androidTest `TrayDeviceTest` | ok |
Carried in:
| ID | Test file(s) | Home |
|---|---|---|
| REQ-020 A2 | play androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-021 A1 | play test `Req021A1...`, `held/DropHeldTest`; androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-021 A2 | play test `Req021LandingPreview...`, `held/DropHeldTest`; androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-051 A1 | play test `Req051CornerPulse...`, `held/DropHeldTest`; androidTest `held/GestureDeviceHeldTest` | ok |
| REQ-051 A2 | play tests `Req051CornerPulse...`, `DragFlipAcceptanceTest`; androidTest `MissedDropDeviceTest` | ok |
| REQ-039 A1 | content test `Req039A1PictureTest`; play androidTest `SolvedPictureTest` (on screen) | ok |
| REQ-045 A2 | content test `Req045A2MiniSolvedTest`; app androidTest `FirstPuzzleSolvedTest` (on screen) | ok |
Note on REQ-002: architecture.md section 5 rule 1 puts the covering test in the feature's module (`play`), and rule 3 ADDS an `app` test for cross-slice behaviour. So the app test alone does not satisfy rule 3. REQ-002.A1 is fine (has a play token); only A2 is missing. Fix is a play-side token on a test whose assertion is "no screen offers a toddler or picture-matching mode" (or a recorded decision that A2 is verified only at app level, which trace-check would still flag). I did not edit anything.
REQ-039 A2 is correctly carried to WO-009 (no token yet).

## (b) Whole-REQ status per trace-check
PASS: REQ-007, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 038, 040, 041, 043, 045, 051.
FAIL, real gap: **REQ-002** (rule 3: A2 covered only outside `play/`).
FAIL, carried to a later WO (build-map section 2): **REQ-039** (A2 only, WO-009), **REQ-042** (WO-009; not in the touched list, listed for completeness).

## (c) Exact counts
- Locked REQs: **47** (51 total less 4 withdrawn).
- Pass: **20**.
- Fail, later WOs: **26** (001, 003, 005, 006, 008, 009, 010, 024, 025, 026, 029, 030, 031, 032, 033, 034, 035, 036, 037, 039, 042, 046, 047, 048, 049, 050; each maps to WO-004..009 per build-map).
- Fail inside closed WO-001..003 scope: **1** (REQ-002 A2, rule 3, see above). Not 0.
- Check: 20 + 26 + 1 = 47.

## (d) Orphans and over-claims
- Tokens for REQs outside WO-001..003 in any test: **none**. Tokens present only for REQ-002, 007, 011-023, 038-041, 043, 045, 051 (REQ-040/041/045 A1 are WO-002 library-level, carried to WO-004 on screen).
- Tokens on scaffolding tests (outside `acceptance/`): **none** (dotted-token grep over every test tree).
- Non-test tokens (informational, not counted as coverage): `contracts/.../puzzle/IPuzzleLibrary.kt` (REQ-038.A1) and `progress/IProgressStore.kt` (REQ-045.A1) mention tokens in KDoc; both pre-date WO-003.
- Main code with no REQ/decision/ADR/TYPE/guardrail reference (sample of all `play/src/main`, `app/src/main`, `debug`, `release`): 3 of 25 files: `play/.../PlayArea.kt` (pointer-event composable), `play/.../draw/PlayCanvas.kt` (composition and semantics), `play/.../draw/DpScope.kt` (the dp-to-px helper). All three are UI plumbing for the traced play features, not orphan behaviour, and the design names them; recommend a one-line REQ/DA reference in each (note, not blocking). No untraced behaviour found.

## (e) Contract integrity
- trace-check drift/baseline: no drift reported. Only note: build-map.md notify change, delta recorded 2026-10-02T20:15:56, baseline re-frozen (that is WO-002's close, before WO-003 opened; WO-003 Contract-deltas table is empty, consistent).
- Locked paths (Requirements reqs/features/types, architecture.md, puzzle.schema.json, `contracts/.../puzzle/*`): unchanged (baseline matches, guard log has no deny or delta after 2026-10-02T20:15:56; no file modified after the WO-003 file under those paths).
- Notify paths: no change in WO-003. `build-map.md` last edited 2026-10-02 20:15 (WO-002). `adr/*`, `contracts/.../progress/*`, `kernel/.../kernel/model/*` unchanged (last delta: PieceShapes.kt, WO-001, 2026-10-02T07:28). CR-1's "kernel/model untouched" holds; `contracts/` did not change.
- Guard log after 20:15 on 2026-10-02 holds only stop/progress-reminder events. Baseline not re-frozen by me (file mtime 08:07 today is a trace-check side effect on the same baseline content; `delta_refrozen` stamps are unchanged).

## (f) Verifiers
- V-01 release manifest: **PASS** (run against the existing merged release manifest, built 2026-10-03 07:14; `AndroidManifest.xml` source unchanged since 2026-10-02 07:26; I did not rebuild).
- V-05 string parity: **PASS**.
- V-06 module deps: **PASS**.
- V-07 no junk paths: **PASS**.

## Other notes
- API 26-32 release launch: recorded waiver in WO-003 (DA-43), exit pending Jami's image; not a trace gap.
- `.swdev/heldout/` not read.

## Orchestrator follow-up (2026-10-03)

The one gap (REQ-002.A2 with no covering test under `play`) was closed. TASK-T3 added `play/src/androidTest/.../acceptance/NoModeAcceptanceTest.kt` (`req002_A2_thePlayAreaOffersNoToddlerOrPictureMatchingMode`, green on API 37). The three plumbing files now carry REQ references.

trace-check re-run results:
- 26 FAIL, which is exactly the later-WO set (001, 003, 005, 006, 008, 009, 010, 024, 025, 026, 029–037, 039 [A2 → WO-009], 042, 046–050);
- 21 of 47 locked REQs pass;
- 0 gaps in the WO-001…003 scope;
- no drift.

**WO-003 scope GREEN.**
