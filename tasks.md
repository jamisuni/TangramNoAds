# Tasks — TangramNoAds

**Status:** Current (P3, WO-001 plan approved)  ·  **Version:** 0.5  ·  **Last updated:** 2026-10-02

> Each task links back to the requirement(s) it implements via REQ IDs, so the
> traceability chain `REQ → task → code → test` stays intact. Do not invent
> tasks for work no requirement asked for (no silent scope). Decomposition
> method: `C:\GitHub\AI\SWDev\framework\methodology\task-decomposition.md`.
> Acceptance tests are written by the Acceptance Test Author (never the
> implementer); implementers' own tests are scaffolding and carry **no**
> `REQ-NNN.An` token.

---

## WO-001 — #Locking  (REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 at engine/state level · design `designs/WO-001-design.md`)

| Task ID | Description (design §) | Serves | Depends on | Done-check (self-check run by the implementer) | Status |
|---|---|---|---|---|---|
| TASK-T | **Acceptance Test Author** (independent, fresh context): visible tests for 6 IDs in `play/src/test/kotlin/…/play/acceptance/`; held-out tests for 3 IDs in `.swdev/heldout/WO-001/` (outside every scanned tree until Test & Verify) | REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 | frozen design seams (design review spot-check: forward) | handoff lists IDs per file; never compiled before TASK-006 | done 2026-10-02 (visible 31 tests: REQ-019 A1, A3 · REQ-020 A1 · REQ-021 A2 · REQ-051 A1, A2; held-out 15 tests: REQ-019 A2, A4 · REQ-021 A1 — isolation checked by orchestrator) |
| TASK-000 | Build setup — the only task that edits build files: shared Android/JVM settings once in the root build, catalog entries (android-library plugin, kotlinx-serialization-json, androidx-startup-runtime), `include(":play")` + `play/build.gradle.kts`, kernel test config (golden inputs, `tangram.root`), `startup-runtime` in `app` (§10 step 0) | enabler (ADR-002, G-06, G-01) | — | `.\gradlew.bat assembleDebug :kernel:test :contracts:test :app:testDebugUnitTest :play:compileDebugKotlin` green (the new `play` module compiles; its tests run from TASK-006 on) | done 2026-10-02 (done-check re-run by orchestrator: green) |
| TASK-003a [P] | **Python:** reference fix in `tools/tangram_geom.outline_corners` (sector rule, DA-7) + `tools/export_geometry_golden.py` (sampling oracle, `shapes-warmup-4` (2,2) assertion) → `tools/golden/geometry.json` (§5, §8). Then the AGENTS.md rule-10 chain: `validate_puzzles.py Tangrams`, `render_puzzle.py` + `svg2png.py` (look at the sheet for shapes-warmup-4), `build_prototype.py`, `tests/test_prototype.py` (all pass), SwReqCollector `validate.py Requirements` (0 errors) | G-03; REQ-019 rule 1 (design §5) | — (Python only; no build file) | every rule-10 command green, the exporter's own assertions pass, golden regenerated deterministically (second run = no diff) | done 2026-10-02 (only shapes-warmup-4 gained (2,2); prototype tests ALL PASS; validator + golden determinism re-run by orchestrator) — follow-up: generators write LF |
| TASK-001 | Kernel exact arithmetic: operator / `signum` / `compareTo` extensions on `Rational`, `Q2`, `ExactPoint` in `kernel.geometry` + unit tests (§2) | enabler: TYPE-004 "exact", G-03 | TASK-000 | `:kernel:test` green | done 2026-10-02 (17 tests; re-run by orchestrator: green) |
| TASK-002 | `kernel.model/PieceShapes.kt` — **create it with the Write tool** (notify-tier contract-delta, the guard must log it) — + `PieceGeometry` (`area`, `offsets`, `corners`, `poseOf`), `PlacedPiece`, `Vec2` + unit tests (§3) | enabler: TYPE-001, TYPE-003, O-01 | TASK-001 | `:kernel:test` green; guard log shows the delta | done 2026-10-02 (45 kernel tests; delta logged; re-run by orchestrator: green) |
| TASK-005a | `ConvexClip` in its own file `kernel/…/geometry/ConvexClip.kt`: the pinned side test (`CLIP_SIDE_EPS`, inclusive), interpolation only on a strict sign change, clamp, area 0 below 3 vertices + the degenerate-case clip tests (§6). Hardest-first: the riskiest primitive, built early | enabler: TYPE-004 validity | TASK-002 (`Vec2`) | `:kernel:test` green | done 2026-10-02 (18 clip tests, 63 kernel tests; re-run by orchestrator; adds a min-area cap beyond §6 — flagged for code review) |
| TASK-003b | Kotlin golden tests: the golden reader (test helper, owned here), freshness, shapes, the 80 transforms, per-file poses and areas (§8) | G-03 | TASK-002, TASK-003a | `:kernel:test` green | done 2026-10-02 (10 golden tests, Kotlin = Python everywhere; 73 kernel tests; re-run by orchestrator) |
| TASK-004 | `Silhouette` with exact outline corners by the sector rule (incl. the mask predicate `isOutlineCornerMask` and its tests) + corner goldens on every puzzle file + `shapes-warmup-4` (2,2) pin test (§5) | enabler: anchor set, pulse corners | TASK-003b | `:kernel:test` green | done 2026-10-02 (18 tests; exact corners = golden on 13 files, (2,2) pinned; 91 kernel tests; re-run by orchestrator) |
| TASK-005b | `LockSearch` / `Fit` (`lockDistance`, `find`: candidates by exact `at`, validity via `ConvexClip`, score, window stop, tie-break DA-1, own corners excluded) + kernel tests: margin test (board (iii) widened to one-piece boards at every valid non-solution position — design review spot-check Note), round trip on every puzzle file (§6, §8) | enabler: TYPE-004 (REQ-019's engine; its acceptance tests run at TASK-006) | TASK-004, TASK-005a | `:kernel:test` green | done 2026-10-02 (116 kernel tests; round trip on 13 files; margin: smallest nonzero area 1.2627e-3, nothing in (1e-9, 1e-4); re-run by orchestrator) |
| TASK-006 | `DropResolver` in `play` (`preview`, `release`), `DragPose`, `DropOutcome`, `CornerPulse` (§7); make TASK-T's visible acceptance tests pass without editing them | REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 | TASK-005b, TASK-T (visible tests) | `.\gradlew.bat assembleDebug test` green (all modules) | todo |
| TASK-007 [P] | G-01 manifest hardening (`tools:node="remove"` stanzas, startup provider stanza, `allowBackup="false"`, `data_extraction_rules.xml` with all nine domains) + verifiers V-01 (`--manifest`, `--res-dir`), V-05, V-06 + `test_verifiers.py` self-test (§9, §10). Touches no build file | G-01 (the REQ-010 side of #Promise), G-05, G-06 | TASK-000 | `python -m unittest discover -s .swdev/verifiers -p test_verifiers.py` green; V-01, V-05, V-06 green on the real tree; `.\gradlew.bat :app:assembleDebug :app:lintRelease` green | done 2026-10-02 (haiku audition PASSED; 13 self-tests, V-01/05/06 PASS on the real tree, forced lintRelease green — re-run by orchestrator) |

**Order.** 000 → 001 → 002 → 005a → 003b → 004 → 005b → 006 (the kernel tasks
are serialized: they share no file but compile one source set, so a
half-written file of one would break the other's `:kernel:test` — plan review
spot-check N2; 005a still comes early, right after the shapes). In parallel:
003a from the start (Python files only: `tools/tangram_geom.py`,
`tools/export_geometry_golden.py`, `tools/golden/`, generated `Spec/prototype/`
and `Tangrams/previews/`), 007 after 000 (own files only: the app manifest,
`app/src/main/res/xml/`, `.swdev/verifiers/`), TASK-T already running.
No two parallel tasks share a file.

**Self-checks are module-scoped until TASK-006.** TASK-T's visible tests sit in
`play/src/test` and do not compile until `DropResolver` exists, so tasks before
TASK-006 run the module checks named in their row, never a bare `gradlew test`.

**Failure routing at TASK-006.** A failing visible acceptance test is first
read against the REQ and the design seam. If the cause is in the kernel (most
likely), TASK-006 hands back `recirculate → TASK-005b/004/…` with the failing
test and the evidence; the orchestrator resumes that task's implementer. If the
test itself looks wrong, the implementer contests it once with evidence
(orchestration.md §5); the orchestrator rules against the REQ, never by taste.

**Coverage, both ways.** Every in-scope acceptance ID lands on TASK-006 (code
home `play`) and TASK-T; TASK-000…005b and 007 are enablers or guardrail
tasks, each naming its TYPE / guardrail. Carried to WO-003: REQ-020 A2, REQ-021
and REQ-051 on screen, decisions F5/DA-5 (interrupted drag).

## Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | 2026-10-01 | placeholder | — |
| 0.2 | 2026-10-02 | WO-001 decomposed into TASK-001…007 | P3 for WO-001 (design's suggested cut, orchestrator as planner — logged inline exception) |
| 0.3 | 2026-10-02 | TASK-000 build setup first; TASK-003 adds the reference fix (DA-7); TASK-006 without `interrupt`; TASK-007 parallel after TASK-000 | design review 01 (F1, F4, F5) |
| 0.5 | 2026-10-02 | kernel tasks serialized (005a before 003b); verifier self-test command fixed | plan review spot-check N1, N2 — verdict forward |
| 0.4 | 2026-10-02 | TASK-T row; done-checks per task; module-scoped self-checks; failure routing; TASK-003 split (003a Python ∥, 003b Kotlin); TASK-005 split (005a ConvexClip early ∥, 005b search); rule-10 chain owned by 003a; enabler labels; PieceShapes.kt via Write | plan review 01 (F2–F6, F8, F11); Playwright channel proven (prototype tests ALL PASS) |
