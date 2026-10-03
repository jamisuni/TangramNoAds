# WO-004 traceability audit

Auditor: Traceability Auditor, 2026-10-03. Read-only run. Raw trace-check output: scratchpad `trace_wo4b.txt`.
Verdict: **WO-004 scope GREEN**. No gap inside WO-001...004; no drift.

## Run
trace-check: 51 REQs (47 locked, 4 withdrawn), 114 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, no drift section.
Project-wide RED as expected: 21 locked REQs fail as uncovered, all belonging to later WOs.
Fail list: 001, 005, 006, 008, 009, 010, 029-037, 039 (A2 only), 042, 046-049.

## (a) Coverage per ID (dotted tokens; all under a valid home)
| ID | Test file(s) carrying the token | Home |
|---|---|---|
| REQ-003 A1 | browse test `NavigationTest`; app androidTest `BrowsingAppTest` | ok |
| REQ-003 A2 | browse test `LeaveReturnTest`; app `BrowsingAppTest` | ok |
| REQ-024 A1 | browse test `NavigationTest`; browse androidTest `BrowseDeviceTest` | ok |
| REQ-024 A2 | browse androidTest `BrowseDeviceTest`; app `BrowsingAppTest` | ok |
| REQ-024 A3 | browse test `NavigationTest`; browse androidTest `BrowseDeviceTest`; app `BrowsingAppTest` | ok |
| REQ-025 A1 | browse test `held/HeldResumeTest`; app `held/HeldResumeRestartAppTest` | ok |
| REQ-025 A2 | browse test `held/HeldResumeTest`; app `held/HeldResumeRestartAppTest` | ok |
| REQ-025 A3 | browse test `held/HeldSanitizeTest`; app `held/HeldResumeRestartAppTest` | ok |
| REQ-026 A1 | browse test `held/HeldSolvedAndFindTest`; app `held/HeldSolvedFindAppTest` | ok |
| REQ-026 A2 | browse test `LeaveReturnTest`; app `BrowsingAppTest` | ok |
| REQ-050 A1 | browse test `held/HeldSolvedAndFindTest`; browse androidTest `held/HeldLongPressDeviceTest`; app `held/HeldSolvedFindAppTest` | ok |
| REQ-050 A2 | browse androidTest `BrowseDeviceTest`; app `BrowsingAppTest` | ok |
| REQ-050 A3 | browse test `held/HeldSolvedAndFindTest`; app `held/HeldSolvedFindAppTest` | ok |

Carried in (code home `content`, plus app on screen):
| ID | Test file(s) | Home |
|---|---|---|
| REQ-040 A1 | content test `acceptance/Req038Req040Req041Req045VisibleTest`; app `FreshInstallScreenTest` | ok |
| REQ-041 A1 | content `Req038Req040Req041Req045VisibleTest`; app `FreshInstallScreenTest` | ok |
| REQ-045 A1 | content `Req038Req040Req041Req045VisibleTest`; app `FreshInstallScreenTest`; browse test `NavigationTest` | ok |

IDs with no token under a valid home: **none** (13 of 13 in scope, 3 of 3 carried). `store` tests carry no tokens (no REQ is coded in `store`; the format is covered by G-09 and its frozen test), which is correct under rule 3.

## (b) Whole-REQ status (REQs touched by WO-001...004)
PASS (26): REQ-002, 003, 007, 011, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025, 026, 038, 040, 041, 043, 045, 050, 051.
FAIL, later WO per build-map section 2: REQ-039 (A2 only, WO-009), REQ-042 (WO-009).
FAIL, real gap: none.

## (c) Exact counts
- Locked REQs: **47** (51 total less 4 withdrawn: 004, 027, 028, 044).
- Pass: **26**.
- Fail, later WOs: **21** (001, 005, 006, 008, 009, 010, 029-037 [9], 039, 042, 046, 047, 048, 049).
- Fail inside WO-001...004 scope: **0**.
- Check: 26 + 21 + 0 = 47.

## (d) Orphans and over-claims
- Tokens for REQs outside WO-001...004: **none**. Dotted tokens exist only for REQ-002, 003, 007, 011-026, 038-041, 043, 045, 050, 051.
- Tokens on scaffolding: **none**. Files with a dotted token are all under `acceptance/`, `held/`, or TASK-T4's named independent files (`browse` `NavigationTest`, `LeaveReturnTest`, `BrowseDeviceTest`; `app` `acceptance/*`). The scaffolding files (`BrowseControllerScaffoldingTest`, `RestartButtonScaffoldingTest`, `ShortWindowScaffoldingTest`, `BrowseChannelProofTest`, `store` scaffolding) carry no dotted token. The two REQ-037 mentions in the scaffolding tests are `// guardrail:` comments, not tokens.
- Non-test tokens (informational): KDoc in `contracts/.../IProgressStore.kt` and `IPuzzleLibrary.kt`, pre-existing. None in `store`/`browse`/`app` main.
- Main code with no REQ/DA/ADR/TYPE/guardrail reference: **none**. All 13 files in `store/src/main`, `browse/src/main` and `app/src/main` name at least one (for example `TrayRows` REQ-035/036/045, `AllPuzzlesOverlay` REQ-050/DA-55, `ProgressRestore` DA-50/G-10, `PuzzleHost` WO-004 design section 5 / G-06). Note: `TrayRows.kt` cites REQ-035/036, which are later-WO REQs. This is a reference in a comment, not a token, and not an over-claim.

## (e) Contract integrity
- trace-check drift/baseline: **none** reported.
- Locked paths: current SHA-256 equals `.swdev/contract-baseline.json` for `progress-v1.json` (ab1c9d1e...b63c), `progress-v1-fresh.json` (2be7af50...e109) and `FrozenV1FixtureTest.kt` (4807518b...2b88). The two fixture pins match design DA-67 / T4a. The baseline holds 72 files, `frozen: 2026-10-03`. Also matching: `architecture.md`. The guard log has 29 events; the only deny is 2026-10-01 (pre-build). No deny or delta touches a locked path in WO-004.
- Fixtures mtime 09:19; the frozen test's mtime 09:48 (CR-2 fixes) precedes the LOCK-V1 baseline freeze (09:50). Consistent with the WO log.
- Notify paths: `contracts/.../progress/*` (IProgressStore.kt, SavedGame.kt) unchanged since 2026-10-02 06:33 and hashes match baseline, so no `IProgressStore` delta, as claimed. `kernel/.../model/*` unchanged (last change PieceShapes.kt, 2026-10-02 07:28), hashes match. `build-map.md` last edit 2026-10-03 08:10 (the WO-003 close / WO-004 opening row edits, all before the design); no WO-004 close edit yet, hash matches baseline. WO-004 Contract-deltas table is empty, consistent.
- Not re-frozen by me.

## (f) Verifiers (run by me, read-only)
- V-01 release manifest: **PASS** (against the existing merged release manifest, built 2026-10-03 09:15; source manifest unchanged since 2026-10-02 07:26; not rebuilt).
- V-05 string parity: **PASS**.
- V-06 module deps: **PASS**.
- V-07 no junk paths: **PASS**.

## Other notes
- Waiver (API 26-32 release launch, DA-43) is recorded in WO-004, exit pending; not a trace gap.
- Held-out `held/` copies are ordinary suite files; `.swdev/heldout/` not read.
- Station-ledger row for this audit and the build-map update are the orchestrator's at close.

**WO-004 scope GREEN.**
