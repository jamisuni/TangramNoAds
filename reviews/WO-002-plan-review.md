# Review — plan · WO-002

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` v0.5 header / change log 0.6 (WO-002 section; WO-001 section for conventions) · `workorders/WO-002.md` · `designs/WO-002-design.md` (as it stands, spot-check pending) · `build-map.md` §2 (v1.1) · `architecture.md` §2, §5 · `.swdev/guard.json` · `Requirements/reqs/REQ-007, 038, 039, 040, 041, 045` · SWDev `task-decomposition.md`, `definitions.md`, `gates-and-autonomy.md` §1/§5, `review-report.md`, `handoff-contract.md`. Spot-checked against the tree: `tools/golden/geometry.json` keys, `tools/export_geometry_golden.py`, `.swdev/verifiers/v06_module_deps.py`, `settings.gradle.kts`, `kernel/build.gradle.kts`, `Tangrams/` (13 puzzle files + schema), session scratchpad (`venv-pw`, `pw-browsers` present).

## Verdict

**recirculate → planner** — the split is sound (packaging risk first, one Kotlin task, Python in parallel, independent test author) but the dependency graph has a real hole (TASK-010 cannot go green without TASK-009's output) and one golden-key contract is stated two ways between the design and the tasks that consume it.
2 Blockers, 6 Shoulds, 5 Notes.

## Checklist applied

- [x] **Directives** — D1 slice (one module, one library seam) ok; D3 each task names guardrail/REQ ok; D4 `app` deliberately not wired (design A9) ok.
- [x] **Guardrails** — G-02/03/06/08/10 named in WO-002.md; V-06 already allows `content` -> kernel + contracts only. No violation.
- [x] **Contract** — only `IPuzzleLibrary` implemented (locked tier); `Tangrams/puzzle.schema.json` consumed; no task edits a locked/notify path. The WO-002.md "Contract deltas" table is empty although the build-map §2 edit (design-review N11) was a notify delta (see N3).
- [x] **Scope** — nothing in tasks.md beyond the design's "Suggested cut"; TASK-009 is the design's step 2a, in WO scope ("Also in this WO").
- [x] **Traceability** — coverage both ways checked below; trace-check rule 3 holds (see Coverage).
- [x] **Hard-stops** — none touched.
- [x] **Evidence re-derived** — golden structure, V-06 usage, file count, scratchpad venv checked directly. No code exists yet.
- [x] **Conceptual 20 %** — dependency/ordering, golden-key agreement, concurrency of Gradle runs, and the done-check commands received the effort.

## Coverage, both ways

| ID | Task (code) | Test (TASK-T2) | Note |
|---|---|---|---|
| REQ-007 A1, A2 | TASK-010 | T2 | ok |
| REQ-038 A1 | TASK-010 + **TASK-009** (verdicts) | T2 | depends on 009 output, see F1 |
| REQ-038 A2 | TASK-010 | T2 | needs golden `buildOrder` (already present) |
| REQ-039 A1 | TASK-010 | T2 | library level; carry listed |
| REQ-040 A1, A2 | TASK-010 | T2 | A1 fresh-install part carried to WO-004 |
| REQ-041 A1 | TASK-010 | T2 | idem |
| REQ-041 A2 | TASK-010 + **TASK-009** (exposure) | T2 | depends on 009 output |
| REQ-045 A1, A2 | TASK-010 | T2 | A1 on screen -> WO-004, A2 on screen -> WO-003 |

All 11 in-scope acceptance IDs land on TASK-T2 and a code task; every task names its REQs or guardrails (008 enabler, 009 G-03 + REQ-038 A1/041 A2, 010 REQs, T2 IDs). The coverage is true but the rows only cite REQ numbers, not A-IDs (F6). Trace-check rule 3: visible tests under `content/src/test/…/content/acceptance/` match `guard.json` `test_globs` (`*/src/test/*`) and the build-map module for #Content; `tools/tests/test_golden_verdict.py` is correctly outside the scan and must carry no `REQ-NNN.An` token.

Carried items: tasks.md, WO-002.md and build-map §2 (WO-002, WO-003, WO-004, WO-009 rows) agree — WO-003: REQ-039 A1 / REQ-045 A2 on screen, on-device library check, release-APK `tangrams/` check, path data `d`, and from WO-001 the ConvexClip non-finite fix (F3) and `overBoard` (F8); WO-004: fresh-install parts of REQ-040 A1, REQ-041 A1, REQ-045 A1; WO-009: REQ-039 A2, REQ-042. (Wording differs, "fail-open" vs "fail-closed" for the same F3 item; meaning is the same.)

## Trajectory & quality

- **Verification actually run?** Plan-level: the channels exist (JVM via WO-001, `python` 3.14 on PATH, Playwright `venv-pw` + `pw-browsers` still in the session scratchpad). The rule-10 command lines are not written into the tasks (F2).
- **Proportionate?** Yes: four rows, one Kotlin task of ~300 lines after the design review's merge (F3 there). Mode Full is justified (first implementation behind a locked interface, new module, Python change re-triggering rule 10).
- **Path sane?** Mirrors WO-001 conventions that worked (build setup first, module-scoped self-checks, failure routing).

## Findings

### F1 — TRACE · Blocker · tasks.md TASK-010 "Depends on" (and the Order paragraph)
- **Observation:** TASK-010 depends only on TASK-008 (+ T2 for the final check). But its done-check `.\gradlew.bat assembleDebug test` runs T2's visible tests, which read the golden's new keys `validator`, `difficulty` (REQ-038 A1, REQ-040/007 A1, REQ-041 A2 rows of the design's Test seams table). Those keys exist only after TASK-009 regenerates `tools/golden/geometry.json` (checked: today a puzzle entry has only `area, buildOrder, kind, outlineCorners, sha256, solution`). The Order paragraph says "TASK-009 in parallel from the start" and never joins it to TASK-010, so the stated done-check cannot be met by TASK-010 alone and a green-but-skipped or red-for-the-wrong-reason result is possible. Same gap for T2's own compile-and-run.
- **Proposed resolution:** TASK-010 "Depends on": TASK-008; **TASK-009 (golden regenerated)** and TASK-T2 visible tests for the final check (writing code may start earlier). State in the Order paragraph that TASK-009 is a prerequisite of the TASK-010 done-check, and that the golden's key shape (F2) is frozen by the planner before T2 and 009 start.
- **Triage:** *(filled on recirculation)*

### F2 — TEST · Blocker · TASK-T2 / TASK-009 shared golden contract (design Test seams, REQ-038 A1 row vs §4)
- **Observation:** The design contradicts itself on the golden's shape. §4 says the verdict is `validator{pass, errors, exposure}` and "no per-rule list is written (it would be a constant, D3)". The Test seams row for REQ-038 A1 tells the Test Author to assert `rules` containing V1–V6, V8–V10 (V12 iff warm-up). TASK-T2 is told to derive tests from that table, TASK-009 is told to add "validator verdicts + difficulty"; neither row pins the keys. As written, T2 would assert a key TASK-009 never writes (a guaranteed visible-test failure that then looks like an implementation bug), or the author invents the shape (Z02 over-specification class).
- **Proposed resolution:** Settle it once, in the design (spot-check in parallel) and mirror it in tasks.md: either drop `rules` from the REQ-038 A1 row (keep `pass`, `errors == []`, the negative control) or add the list to §4 and TASK-009. The TASK-009 and TASK-T2 rows should point to the design § holding the final key list (point, do not paraphrase) so both stations read the same artifact.
- **Triage:** *(filled on recirculation)*

### F3 — TRACE · Should · `[P]` on TASK-009 vs TASK-008 (shared Gradle state)
- **Observation:** The file lists are disjoint (009: `tools/**`, generated `Spec/prototype/`, `Tangrams/previews/`; 008: `settings.gradle.kts`, `content/build.gradle.kts`), but both done-checks run Gradle in the same root: 009 runs `:kernel:test`, 008 runs `assembleDebug :kernel:test`. They write the same `kernel/build/` outputs and share the configuration step. While 008 is mid-edit, `include(":content")` without a finished `content/` directory/build file can fail configuration for every Gradle call in the tree (Gradle 9 rejects a missing project directory), which would break 009's check through no fault of its own. This is the WO-001 spot-check N2 class (parallel tasks that touch no common file but share one build). Also 009's rule-10 chain rewrites `Spec/prototype/` + `Tangrams/previews/` while 008's `inputs.dir(Tangrams)` and `:content:processResources` read `Tangrams/` (previews are excluded by `include("*.json")`, but the golden/prototype are regenerated under it).
- **Proposed resolution:** Keep the editing parallel but write the rule into the Order paragraph: "Gradle invocations are serialized by the orchestrator; TASK-009's `:kernel:test` runs only when no other task is mid-edit on `settings.gradle.kts`/`content/`" — or simply run TASK-009's Gradle step after TASK-008 is done. Re-justify `[P]` in the row with the real file list incl. generated outputs.
- **Triage:** *(filled on recirculation)*

### F4 — TEST · Should · TASK-009 done-check (executable as written)
- **Observation:** "exporter + its test green … rule-10 chain ALL PASS" is prose. WO-001 TASK-007 wrote its commands out (`python -m unittest discover -s .swdev/verifiers -p test_verifiers.py`). Here the unittest command for `tools/tests/test_golden_verdict.py` is not given (tools/tests holds only `test_prototype.py`, which is a browser test; `discover -p` must be narrowed so it is not run without the venv, and the test needs `tools/` on `sys.path` to import the exporter). The rule-10 chain needs the Playwright interpreter and `PLAYWRIGHT_BROWSERS_PATH`/venv from the session scratchpad (present: `…\scratchpad\venv-pw`, `pw-browsers`), but the row only says "ALL PASS"; AGENTS.md rule 10 lists plain `python tools/tests/test_prototype.py`, which will not find Playwright without the venv. SwReqCollector `validate.py` is at `C:/GitHub/AI/SwReqCollector/tools` (guard.json) and is not named in the row.
- **Proposed resolution:** Write the commands in the row: `python -m unittest discover -s tools/tests -p test_golden_verdict.py`; the exporter run + a second run with no diff of `tools/golden/geometry.json`; the rule-10 chain with the venv interpreter (named relative to the scratchpad); the SwReqCollector `validate.py Requirements`. State that the exporter test must not carry a `REQ-NNN.An` token.
- **Triage:** *(filled on recirculation)*

### F5 — QUALITY · Should · TASK-008 done-check misses the design's highest risk
- **Observation:** The design names the packaging chain as the highest risk (silent short/empty library) and asks to "run the build twice, then edit a puzzle, then once more" (`Sync` + `doLast` index vs up-to-date checks, cache). TASK-008's done-check only runs `processResources` once and eyeballs "lists the 13 puzzle files, no schema" (13 is correct: 14 `*.json` minus the schema); the run-twice probe sits only in the design's Step 3 (Test & Verify), too late for a task meant to de-risk first. "Edit a puzzle" also collides with WO-002's out-of-scope ("any edit to the puzzle files", AGENTS.md rule 7).
- **Proposed resolution:** Make the check mechanical in the row: `processResources`, count lines of the generated `tangrams/index.txt` against `ls Tangrams/*.json` minus schema (13) and assert `puzzle.schema.json` absent; run again (index still present, task UP-TO-DATE); delete `build/generated/puzzles` and rerun (index regenerated). Replace "edit a puzzle" with a probe that does not modify `Tangrams/` (e.g. a temporary extra file in a scratch copy via a `-P` root override, or the delete-and-rerun above), or state the probe is reverted byte-for-byte and the owner accepts it.
- **Triage:** *(filled on recirculation)*

### F6 — TRACE · Should · tasks.md "Serves" cells (acceptance IDs per task)
- **Observation:** TASK-010 serves "REQ-007, REQ-038, … (library level)" and the coverage paragraph says "every in-scope acceptance ID lands on TASK-010 and TASK-T2". The WO-001 section had IDs per task. The 11 IDs with their carry notes (e.g. REQ-040 A1 / 041 A1 / 045 A1 library-level only; REQ-039 A1 library-level; REQ-038 A1 and REQ-041 A2 also through TASK-009) are the actual traceability anchor and are only implicit.
- **Proposed resolution:** Put the A-IDs in the TASK-010 and TASK-009 "Serves" cells (the table in the Coverage section above is usable as is).
- **Triage:** *(filled on recirculation)*

### F7 — TEST · Should · owner of the parser hardening tests
- **Observation:** The design lists a long block of "further tests" (token exactness, 19-digit rejection, `"reviewedByHuman": "true"` fails closed, swapped-vertex polygon, malformed JSON leaves other files, `check`/`checkNotNull` cases, parsed polygons equal golden). They protect G-03/G-08/G-10, not an acceptance ID ("carry the REQ they protect, no new IDs"). The plan assigns them to nobody: T2 is told to derive tests from the Test seams table (acceptance rows), and tasks.md says implementer tests are scaffolding with no token. Unowned, the highest-value negative tests (G-08 fail-closed flag, cyclic-order check) either vanish or are written by the implementer against its own code.
- **Proposed resolution:** Say which station writes them: either T2 (preferred for the G-08/G-10 negatives, since they distinguish states the implementation could get wrong) with the list pointed at by design §, or the implementer as marked scaffolding (no `An` token) with the list as a TASK-010 done-check item. Also state they must not claim an acceptance ID.
- **Triage:** *(filled on recirculation)*

### F8 — TEST · Should · TASK-T2 held-out arrangement is unspecified
- **Observation:** The row says "held-out slice in `.swdev/heldout/WO-002/`" but gives no size or weighting. The method says ~1 in 3 IDs, risk-weighted (WO-001 recorded 3 of 9). With 11 IDs the plan should say which kind of IDs are to be held out; the held-out files also have to be self-contained (WO-001's `HeldOutOracle.kt` pattern) because the visible helpers (`GoldenFile`, `lockInOrder`) live in the scanned tree and the implementer must not learn the held-out assertions through them. The held-out files must name the same internal symbols (`PuzzleLibrary(files)`, `PuzzleFile`, `PackagedPuzzles.files(loader)`), which the pinned seams table supplies.
- **Proposed resolution:** Add to the TASK-T2 row: hold out 3–4 IDs weighted to the riskiest (suggestion: REQ-038 A2, REQ-040 A2, REQ-041 A2, REQ-045 A2 — the ones that need the lock search or synthetic inputs), own copy of any helper, isolation check by the orchestrator as in WO-001, runs first at Test & Verify.
- **Triage:** *(filled on recirculation)*

### F9 — SCOPE · Note · tasks.md / WO-002.md headers and ledger
- **Observation:** tasks.md header says Version 0.5 while the change log's last row is 0.6 (and 0.5 sits above 0.4). WO-002.md header still says "In progress (P3/P4 design)" and "Plan approved: —"; the Metrics table has a log row pasted in it (2026-10-02 P4 design review). The design is still "spot-check pending", and TASK-T2 lists "design spot-check forward" as its dependency: it must not be dispatched before that is recorded.
- **Proposed resolution:** Bump the header, correct the WO status line when the plan is confirmed (G3), move the metrics row to the Log.

### F10 — CONTRACT · Note · WO-002.md "Contract deltas" table
- **Observation:** Empty, while the design review N11 edited `build-map.md` §2 (notify tier) to carry REQ-040 A1 / REQ-041 A1 / REQ-045 A1 on screen to WO-004. The guard log is the source of truth, but the WO table should list it.
- **Proposed resolution:** Add the row (build-map.md §2, WO-004 carry, delta logged by the guard hook).

### F11 — TEST · Note · shell syntax in done-checks
- **Observation:** Done-checks are written `.\gradlew.bat …` (PowerShell form), the same as WO-001, which worked because the primary shell is PowerShell. Under the Bash tool they need `cmd.exe //c ".\\gradlew.bat …"`. `python .swdev/verifiers/v06_module_deps.py .` is valid in both (argv[1] = project root; the `content` row of ALLOWED_DEPS exists).
- **Proposed resolution:** One line in the Order paragraph stating the invocation form for implementer briefs.

### F12 — QUALITY · Note · TASK-008 empty source folder
- **Observation:** The design's step 1 asks for "an empty source folder so `:content:test` runs"; the row does not say it, and TASK-T2 writes into `content/src/test/…` in parallel. A placeholder file there would be a (trivial) shared path.
- **Proposed resolution:** If a placeholder is wanted, put it under `content/src/main/kotlin/…` (owned by TASK-010 afterwards) or omit it: TASK-008's check does not run `:content:test`.

### F13 — TRACE · Note · `content`-level acceptance vs later on-screen tokens
- **Observation:** The library-level tests will carry `REQ-040.A1`, `REQ-041.A1`, `REQ-045.A1/A2`, `REQ-039.A1` and trace-check will show them green although the real assertion lands in WO-003/004 (same as WO-001's REQ-020 A2). The carried lines in tasks.md/WO-002.md are the only protection against reading that green as complete.
- **Proposed resolution:** Keep the "carried" lines in the WO-002 close summary and in WO-003/004 scope; no plan change needed.

## Handoff — Plan Reviewer · WO-002
- **Scope:** REQ-007, REQ-038, REQ-039, REQ-040, REQ-041, REQ-045 · governed touched: `IPuzzleLibrary` (implements; unchanged)
- **Inputs read:** tasks.md v0.5 (rows 0.6) · WO-002.md · designs/WO-002-design.md (Draft, revised after review) · build-map.md v1.1 · architecture.md v1.0 §2, §5 · .swdev/guard.json · REQ-007/038/039/040/041/045 · SWDev task-decomposition.md v0.3, definitions.md, gates-and-autonomy.md, review-report.md, handoff-contract.md v0.2
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-002-plan-review.md`
- **Status:** recirculate → planner (TASK-010 omits its dependency on TASK-009; golden `validator` key shape disagrees between design §4 and the Test seams row, with TASK-009/T2 not pinning it). 2 Blockers, 6 Shoulds, 5 Notes. After F1/F2 and the Shoulds, a spot-check is enough; no re-review in full needed.
- **Traceability delta:** none (review only)
- **Notes for next station:** F2 needs a one-line decision in the design (parallel spot-check) before TASK-009 and TASK-T2 are dispatched; the golden key list is the interface between them.

## Spot-check (tasks.md v0.7, WO-002.md updated)

- F1 resolved: TASK-010 depends on TASK-008, TASK-009 and T2 visible tests; Order is 008 -> 009 -> 010.
- F2 resolved: design REQ-038 A1 row no longer mentions `rules`; tasks point at design §4 for the key list.
- F3 resolved: strict serial 008 -> 009 -> 010; `[P]` removed from 009.
- F4 resolved: unittest, double-run golden sha256, Gradle, rule-10 chain (venv-pw path), validate.py all written as commands.
- F5 resolved: 13 index lines, no schema, UP-TO-DATE second run, delete-and-rerun; Tangrams/ untouched.
- F6 resolved: Serves cells carry A-IDs.
- F7 resolved: parser-hardening scaffolding tests owned by TASK-010, no An token, outside acceptance/.
- F8 resolved: 3-4 risk-weighted held-out IDs, self-contained, isolation check (IDs not named in the row; acceptable, T2 chooses and the orchestrator records them).
- F9-F13 resolved (F10 delta row present; F11 syntax note present; F12 TASK-008 creates nothing under content/src/test; F13 caveat in Coverage).

**Final verdict: forward.** 0 Blockers, 0 Shoulds open.

## Handoff — Plan Reviewer (spot-check) · WO-002
- **Status:** forward
- **Result:** this file
- **Traceability delta:** none
