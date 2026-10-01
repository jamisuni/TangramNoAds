# Build map — TangramNoAds

**Status:** Draft (P2 not started — technology parked by the owner)  ·  **Version:** 0.1  ·  **Last updated:** 2026-10-01
**Approved by:** —  ·  **Approved on:** —  *(signed with `architecture.md` at G2)*
**Feature tree:** `Requirements/features.md` (SwReqCollector collection v1.1, locked 2026-10-01, SRC-015)

> Code homes and cross-feature dependencies for the locked feature tree.
> Features not listed inherit their nearest listed ancestor's code home;
> `—` = system-level (no slice); `TBD` = decided at P2 once the stack is
> chosen. Mapping rules: `C:\GitHub\AI\SWDev\framework\traceability\traceability-rules.md` §5.
> Build status is pipeline-written; this file is `contract_paths.notify`.

---

## 1. Code homes

Subtree rows only — 53 tags, 37 build units (leaves with live REQs),
grouped the way the tree already groups them. Paths are placeholders until
the technology topic opens (`STATUS.md` next step 5).

| #Tag | Code home | Build status | Cross-feature deps (via I*) | Notes |
|---|---|---|---|---|
| (kernel) | TBD | Planned | — | TYPE-001 piece set, TYPE-003 turn step, TYPE-004 lock parameters + scoring, TYPE-005 play-time, TYPE-006 puzzle-state transitions, TYPE-007 layout classes — implemented once (D1) |
| #Promise | — | Planned | — | REQ-001, REQ-008/009/010: no ads, no money, no network, no permissions — verified at app/build level, not a slice |
| #Solving | TBD (play slice) | Planned | #Content (puzzle library), #Browsing (puzzle state) | the 14 play leaves inherit; REQ-002 (BIZ) verifies at slice level |
| #Locking | TBD (play slice / locking) | Planned | — | REQ-019/020/021/051 + TYPE-004 — the engine; candidate first WO |
| #Turning | TBD (play slice / turning) | Planned | — | REQ-016/017/018 + TYPE-003; #TurnButtons is withdrawn-only (no unit) |
| #Browsing | TBD (browse slice) | Planned | #Content, #Solving (saved pieces) | REQ-003, 024–026, 050, 034 (reset lives here with the state) |
| #PlayTime | TBD (time slice) | Planned | #Browsing (per-puzzle state) | REQ-005, 029–031 + TYPE-005 |
| #Settings | TBD (settings slice) | Planned | #PlayTime, #Browsing | REQ-032/033/034/047 — language (REQ-047) is cross-cutting in effect; keep it here, consumed everywhere via the kernel |
| #Layout | TBD (layout / shell) | Planned | — | REQ-006, 013, 035–037 + TYPE-007; the app shell that hosts the slices |
| #Content | TBD (content pipeline) | Planned | — | REQ-007, 038–042, 045: `Tangrams/*.json` + `puzzle.schema.json` + validator — governed-interface candidate `IPuzzleLibrary` (locked tier) |
| #Release | — | Planned | — | REQ-048 (BIZ), REQ-049 (privacy text in Settings) — store obligations, verified at release level |
| #DevTools | TBD (dev / test aid, excluded from release builds) | Planned | #Solving | REQ-046 — passcode reveal; never in a release build (AGENTS.md rule 11) |
| #Difficulty | — | — | — | withdrawn subtree (REQ-004/027/028, TYPE-002) — not a build unit |
| #PieceSets | — | — | — | `idea` — not a build unit |
| #Accessibility | — | — | — | `idea` — not a build unit |

## 2. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-10-01 | initial subtree map from the locked tree; code homes TBD | project adopted under SWDev v0.15, intake path C | — |
