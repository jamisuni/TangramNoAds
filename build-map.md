# Build map — TangramNoAds

**Status:** Current (P2)  ·  **Version:** 1.1  ·  **Last updated:** 2026-10-02
**Approved by:** AI under governance.md row 4 (ai+inform), after fresh-eyes review `reviews/P2-architecture-review-01.md` (spot-check verdict: forward)  ·  **Approved on:** 2026-10-02  *(signed with `architecture.md` v1.0 at G2; surfaced to Jami at checkpoint 1)*
**Feature tree:** `Requirements/features.md` (SwReqCollector collection v1.1, locked 2026-10-01, SRC-015; accepted for this build at G1 2026-10-02)

> Code homes and cross-feature dependencies for the locked feature tree.
> Features not listed inherit their nearest listed ancestor's code home;
> `—` = system-level (no slice; checked at app / release level). A code home
> is a Gradle module at the project root (ADR-002); its tests live in
> `<module>/src/test` and `<module>/src/androidTest`. Mapping rules:
> `C:\GitHub\AI\SWDev\framework\traceability\traceability-rules.md` §5.
> Build status is pipeline-written; this file is `contract_paths.notify`.

---

## 1. Code homes

| #Tag | Code home | Build status | Cross-feature deps (via I*) | Notes |
|---|---|---|---|---|
| (kernel) | `kernel` | Partly built (WO-001: exact geometry, TYPE-001/003 shapes, outline corners, TYPE-004 lock search; TYPE-005/006/007 later) | — | TYPE-001 piece set, TYPE-003 turns, exact geometry (ADR-003), TYPE-004 lock search, TYPE-005 active second, TYPE-006 transitions, TYPE-007 layout class — implemented once (D1, O-01, O-07) |
| #Promise | — | Partly built (WO-001: release manifest without permissions or backup, V-01 blocking) | — | REQ-001, 008, 009, 010: verified at app / release level (G-01, V-01); REQ-009's note is rendered by `settings`. WO-006 (REQ-009 in WO-007) |
| #Solving | `play` | Planned | #Content (IPuzzleLibrary), #Browsing (IProgressStore: saved pieces) | the play leaves inherit; REQ-002 verifies at slice level. WO-003 |
| #Locking | `play` | Built at engine/state level (WO-001, 2026-10-02); on-screen parts + REQ-020 A2 in WO-003 | — | REQ-019/020/021/051; the search itself is kernel (TYPE-004, O-01). **WO-001** |
| #Turning | `play` | Planned | — | REQ-016/017/018 + TYPE-003; #TurnButtons is withdrawn-only. WO-003 |
| #Browsing | `browse` | Planned | #Content (IPuzzleLibrary), #Solving (IProgressStore) | REQ-003, 024–026, 050; brings `store` (IProgressStore) with it. WO-004 |
| #PlayTime | `time` | Planned | #Browsing (IProgressStore: puzzle and play times) | REQ-005, 029–031 + TYPE-005. WO-008 |
| #Settings | `settings` | Planned | #Content (IPuzzleLibrary: titles for the best-time list), #PlayTime (IProgressStore: play time), #Browsing (IProgressStore: reset) | REQ-032, 033, 034; also renders REQ-009 (#NoMoney) and REQ-049 (#Release). WO-007 |
| #Language | `app` | Planned | — | REQ-047: the locale is app-wide; strings live in every module (G-05). WO-006 |
| #Layout | `app` | Planned | — | REQ-006, 035–037 + TYPE-007 (decisions F3); the shell that hosts the slices. WO-006 |
| #Content | `content` | Planned | — | REQ-007, 038–042, 045: `Tangrams/*.json` + `puzzle.schema.json` (locked) behind `IPuzzleLibrary` (locked). WO-002 |
| #Release | — | Planned | — | REQ-048 (store obligations) and the release checklist for the release-time criteria (decisions F17); REQ-049 is rendered by `settings`. WO-009 |
| #DevTools | `devtools` | Planned | #Content (IPuzzleLibrary), #Solving (solve-now hook wired by `app`) | REQ-046; debug builds only (ADR-006, G-04). WO-005 |
| #Difficulty | — | — | — | withdrawn subtree (REQ-004/027/028, TYPE-002) — not a build unit |
| #PieceSets | — | — | — | `idea` — not a build unit |
| #Accessibility | — | — | — | `idea` — not a build unit |

## 2. Work-order sequence (governance row 8, `ai`)

All REQs are `priority: none`, so the order follows the dependency graph and
the owner's way of reviewing (playing a build, governance row 9): the first
playable build should come as early as possible. Each WO lists the
acceptance IDs it covers; an ID whose on-screen part needs a later WO's code
is **carried** there and re-verified with the same token (architecture.md §5,
test placement). Every one of the 47 locked REQs is in exactly one row.

| WO | Scope (features → acceptance IDs) | Carried in / out | Why here |
|---|---|---|---|
| WO-001 | #Locking: REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2 and REQ-051 A1–A2 at engine / state level; kernel geometry (+ solution poses, golden data, G-03); manifest hardening + verifiers V-01, V-05, V-06 | out → WO-003: REQ-020 A2, REQ-021 and REQ-051 on screen; 180 ms animations; sounds → WO-007 | owner's kickoff choice; the engine everything rests on |
| WO-002 | #Content: REQ-007 A1–A2, REQ-038 A1–A2, REQ-039 A1 (library level), REQ-040 A1–A2, REQ-041 A1–A2, REQ-045 A1–A2 (library / engine level: the first puzzle is a 3-piece mini; locking its three stored pieces through the kernel solves it and it has a picture); the golden data for all puzzle files | out → WO-003: REQ-039 A1 and REQ-045 A2 on screen; → WO-004: REQ-045 A1 on screen (fresh install opens on it); → WO-009: REQ-039 A2, REQ-042 (release checklist) | a playable build needs the real puzzles behind `IPuzzleLibrary` |
| WO-003 | rest of #Solving: REQ-002 A1–A2, REQ-011 A1–A2, REQ-012 A1–A2, REQ-013 A1–A3, REQ-014 A1–A2, REQ-015 A1–A2, REQ-016 A1–A2, REQ-017 A1–A2, REQ-018 A1–A3, REQ-022 A1, REQ-023 A1–A2, REQ-043 A1–A2; **minimal shell** in `app` (edge-to-edge, portrait policy, TYPE-007 layout class, tray rows of REQ-035/036 — their acceptance stays WO-006) | in ← WO-001: REQ-020 A2, REQ-021, REQ-051 on screen; ← WO-002: REQ-039 A1 and REQ-045 A2 on screen. Needs the API 26–32 emulator (architecture.md §5) | **first playable APK** |
| WO-004 | #Browsing + `store`: REQ-003 A1–A2, REQ-024 A1–A3, REQ-025 A1–A3, REQ-026 A1–A2, REQ-050 A1–A3 | in ← WO-002: REQ-045 A1 on screen (fresh install opens on a mini puzzle) | many puzzles, saved progress; frozen v1 save fixture (G-09) |
| WO-005 | #DevTools: REQ-046 A1–A4 (A3's "no best time" at event level: `byAid`) + V-04 | out → WO-008: aid-solve regression on real best times | lets the owner check every puzzle on a device (REQ-039/042 review) |
| WO-006 | #Layout, #Language, #Promise: REQ-006 A1, REQ-035 A1–A2, REQ-036 A1–A2, REQ-037 A1, REQ-047 A1–A2, REQ-001 A1, REQ-008 A1–A2, REQ-010 A1–A2 | out → WO-009: REQ-001 A2 (store labels) | phone and tablet layouts, both languages, the free promise on every screen |
| WO-007 | #Settings: REQ-032 A1–A2, REQ-033 A1, REQ-034 A1–A2; REQ-009 A1, REQ-049 A1 | in ← WO-001: the soft return sound and lock tick (REQ-020/033) | settings screen, sound, reset |
| WO-008 | #PlayTime: REQ-005 A1–A2, REQ-029 A1–A2, REQ-030 A1–A3, REQ-031 A1–A2 | in ← WO-005: aid-solve regression (REQ-030 rule, REQ-046 A3) | active time, best times, timer |
| WO-009 | #Release: REQ-048 A1–A3; release checklist with manual evidence (decisions F17): REQ-001 A2, REQ-039 A2, REQ-042 A1–A3, REQ-048 A1–A2 | in ← WO-002, WO-006 | store obligations |

## 3. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-10-01 | initial subtree map from the locked tree; code homes TBD | project adopted under SWDev v0.15, intake path C | — |
| 1.0 | 2026-10-02 | code homes = Gradle modules (ADR-002); #Language row (code home `app`); #Locking/#Turning inherit `play`; REQ-013 dropped from the #Layout note (its feature is #Tray, under #Solving); WO sequence (#Content moved before the rest of #Solving, #DevTools before #Layout, #Promise placed with #Layout) | P2; governance rows 4 and 8 | AI (checkpoint 1) |
| 1.1 | 2026-10-02 | §2 per-WO acceptance-ID scope with carried IDs; minimal shell + TYPE-007 + tray rows moved into WO-003; REQ-045 A1/A2 in WO-002 at library/engine level, on-screen parts carried to WO-003/004; #Settings deps (IPuzzleLibrary), #PlayTime deps named | P2 review 01 F4, N11 + spot-check | AI (G2, 2026-10-02) |
