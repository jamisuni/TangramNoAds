# Review — plan · WO-001

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (Plan Reviewer; never saw the planner's or the design author's reasoning)
**Inputs:** `tasks.md` v0.3 (subject) · `workorders/WO-001.md` (Full) · `designs/WO-001-design.md` (as revised after design review 01; §§1-10, Test seams, Assumptions) · `build-map.md` v1.1 §2 · `architecture.md` v1.0 §2, §5 · `.swdev/guard.json` · `progress.md` 2026-10-02 entries (06:05 G3 proof, 06:59) · REQ-019, REQ-020, REQ-021, REQ-051 (locked) · SWDev `task-decomposition.md` v0.3, `definitions.md` v0.3, `gates-and-autonomy.md` v0.6 §1/§5/§7, `review-report.md`, `handoff-contract.md` v0.2.
Evidence also consulted (read-only, to check claims): `STATUS.md` "How to run things", headers of `tools/tests/test_prototype.py` and `tools/svg2png.py`, `tools/tangram_geom.py` consumers, `decisions.md` rows, `req_review_01.md` F23/F30/F31, `features.md` (#Locking subtree), the current build files and `Pieces.kt`/`Puzzle.kt`. From `reviews/WO-001-design-review.md` only the verdict and the spot-check line were read.

## Verdict

**recirculate -> planner.** The cut is sound and the coverage is complete both ways, but one G3 item is wrong: the WO declares that no browser channel is needed while its own scope (the `tangram_geom.py` reference fix) obliges the browser-based rule-10 runs, and Playwright is not installed on this machine. Around that, six Shoulds: the Acceptance Test Author arrangement is not in the plan, TASK-005 and TASK-003 are too large and mix seams while the riskiest piece sits fifth, and the WO DoD overclaims against its own scope.
**1 Blocker, 6 Shoulds, 4 Notes.**

## Coverage, both ways (step 3 re-derived)

| Acceptance ID | Lands on | Test home (design "Test seams") |
|---|---|---|
| REQ-019.A1, A2, A3, A4 | TASK-006 (also claimed by TASK-005, see F4) | `DropResolver.release` (A3 also `LockSearch.find`), `play/src/test` |
| REQ-020.A1 | TASK-006 | `release` |
| REQ-021.A1, A2 | TASK-006 | `preview` vs `release` |
| REQ-051.A1, A2 | TASK-006 | `release` (`Home.pulse`) |

Forward: 9 of 9 in-scope IDs land on at least one task (no hole). Carried IDs (REQ-020.A2, REQ-021/051 on screen, F5) are correctly absent and named as carried to WO-003 in `tasks.md`, WO and build-map.
Reverse: every task names a REQ, TYPE or guardrail (TASK-000: ADR-002/G-06; 001: TYPE-004/G-03; 002: TYPE-001/003/O-01; 003: G-03; 004: REQ-019/051; 005: REQ-019/TYPE-004; 006: all 9; 007: G-01/G-05/G-06). No task without a link. WO scope items checked against the rows: TYPE-001/003/004, arithmetic, silhouette corners, `poseOf` (TASK-002), golden exporter (TASK-003), `PieceShapes.kt` notify delta (TASK-002), manifest/V-01/V-05/V-06 (TASK-007), shared build setup (TASK-000): all land. One work item that the design requires lands on no task: F2.
`[P]`: TASK-007's files (`app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/`, `.swdev/verifiers/*`) are disjoint from every file of TASK-000..006 as listed in design §9/§10, and it depends only on TASK-000 (the sole build-file editor). The marking is correct (but see F9 for the working-tree caveat).
Trace-check rule 3: `#AnchorLock/#GoHome/#LandingPreview/#CornerTeach` sit under `#Locking` (features.md 42-54), code home `play` (build-map §1); `test_globs` `*/src/test/*` matches `play/src/test/kotlin/...`; `play` may see `kernel` and `contracts`. Satisfied if the Test Author's tokens (`REQ-019.A1` ...) sit in files under `play/src/test`, which the design's Test seams already fix.

## Checklist applied

- [x] **Directives** - D1 one vertical slice, but cut as kernel layers with all acceptance at the last task (F5); D2/D4 fine (`interrupt` seam dropped, no Compose); D5 deps are recorded (decisions.md rows for kotlinx-serialization-json and `startup-runtime`).
- [x] **Guardrails** - G-01/G-03/G-05/G-06/G-10 each have an owning task; no G-NN bent.
- [x] **Contract** - no `I*` change; one notify-tier delta (`PieceShapes.kt`, TASK-002) correctly marked; locked REQs untouched.
- [x] **Scope** - every task traces to the WO/design; nothing invented. The omission direction has one gap (F2).
- [x] **Traceability** - coverage proven both ways above; the acceptance-claim wording on TASK-005 is circular (F4).
- [x] **Hard-stops** - none touched; manifest edit only removes permissions and backup (design §9, governance row 10 unaffected).
- [x] **Evidence re-derived** - G3 toolchain proof re-checked against `progress.md` (06:05 entry: `assembleDebug testDebugUnitTest` BUILD SUCCESSFUL, 1 test; 06:59: `assembleDebug test` green with kernel and contracts) and the artifacts (`app/build/intermediates/merged_manifest/release` exists, so `:app:processReleaseMainManifest` has run). Playwright/Chromium re-checked on this machine: not available (F1).
- [x] **Conceptual 20 %** - spent on: the verification channel behind the reference fix (F1), the ATA/held-out mechanics at a single seam (F3), where the riskiest logic sits in the order (F5).

## Trajectory & quality

- **Verification actually run?** The Gradle/JUnit proof was executed and recorded. The Python/browser proof was not attempted, and the WO says it is not needed (F1).
- **Proportionate?** Yes: the task list tracks the design's suggested cut; nothing is gold-plated. The cost is serialization: 7 of 8 tasks form one chain, max fan-out 2.
- **Path sane?** One design recirculation, converged (forward on spot-check). Tasks are a first draft with one revision (v0.2 -> v0.3).

## Findings

### F1 - QUALITY (G3 verification channel) · Blocker · `workorders/WO-001.md` "Constraints & notes" (Verification channels); `tasks.md` TASK-003
- **Observation:** WO-001 states "No device, browser or human channel is needed for this WO's DoD", and lists AGENTS rule 10 as "(validator, prototype build)". But design §8 (and the design review's carry-over to the planner) require, after the `tools/tangram_geom.py` fix, `build_prototype.py`, `validate_puzzles.py`, **`tools/tests/test_prototype.py`** and the sheet re-render; AGENTS rule 10 says those "must all pass". `test_prototype.py` and `svg2png.py` both import Playwright and launch Chromium ("Needs Playwright with Chromium"). `STATUS.md` says Playwright is "not installed on the dev VM". Re-checked: `python -c "import playwright"` -> `ModuleNotFoundError` (Python 3.14.6, the only installed interpreter), no `%LOCALAPPDATA%\ms-playwright`. Chrome and Edge exist, but the scripts call `p.chromium.launch()`, so they would need Playwright's own Chromium. A DoD item (the reference fix is sound for the prototype and the shipped sheets) has no reachable channel - the Z02 pattern in `gates-and-autonomy.md` §1 ("a DoD item without a reachable channel is the toolchain fiction with a delay on it"). `make_ui_sheets.py`, `build_prototype.py` and `validate_puzzles.py` all import `outline_corners`/`build_order`, so the change is not hypothetical.
- **Proposed resolution:** Before P4, either (a) install it (`python -m pip install playwright`, `python -m playwright install chromium`), run `python tools/tests/test_prototype.py --fast` once as the channel proof, and record that in the WO "Verification channels" line and `progress.md`; or (b) record a §7 waiver in the WO Waivers table ("browser rule-10 runs deferred", scope TASK-003 + the prototype/sheets rebuild, exit = Playwright installed and the full run green; TASK-003 then caps at `implemented (unverified)` and the WO cannot close first). Correct the WO text: the rule-10 list is validator, prototype build, **prototype browser tests, sheet re-render**, and the channel is a browser.
- **Triage:** *(filled on recirculation)*

### F2 - SCOPE (omission) · Should · `tasks.md` TASK-003
- **Observation:** The blast radius of the reference fix is in design §8 ("Consequences for the orchestrator") and in the design review's carry-over, but no task, order line or DoD owns it: `python tools/build_prototype.py` (rewrites `Spec/prototype/tangram-prototype.html`, which gains the (2,2) anchor of `shapes-warmup-4`), `python tools/validate_puzzles.py Tangrams`, `python tools/tests/test_prototype.py`, `make_ui_sheets.py`/preview re-render, and the exporter re-run per rule 10. Those files lie outside TASK-003's listed files (`tools/tangram_geom.py`, `export_geometry_golden.py`, `golden/geometry.json`, `kernel/src/test`), so an implementer would not touch them and the hole only shows at WO close.
- **Proposed resolution:** Put the rule-10 chain into TASK-003's row as an explicit Done item (outputs expected: validator 0 errors, prototype rebuilt, browser tests green or waived per F1, sheets re-rendered, diff of `Spec/` limited to the intended anchor change), or add a named orchestrator step directly after TASK-003 in the Order line with the same checks. Say which files under `Spec/` the task is allowed to change.
- **Triage:** *(filled on recirculation)*

### F3 - TEST · Should · `tasks.md` (header, TASK-006, Order); WO station ledger
- **Observation:** The Acceptance Test Author (ATA) is mentioned in the `tasks.md` header and the WO ledger only. The plan has no slot, no start condition and no dependency for it, although TASK-006's Done is "passes the visible acceptance tests". Feasibility itself is good (9 IDs at one seam, `DropResolver`, signatures fixed in design §7 and the Test seams table; held-out ~3 of 9, risk-weighted, would be REQ-019.A2 (the overlap/outside oracle), REQ-019.A4 (non-stored arrangements) and REQ-021.A1 - the ATA decides). But the mechanics are unspecified and have three concrete traps:
  1. **Staging.** If the visible tests land in `play/src/test` while TASK-001..005 and TASK-007 still run, `gradlew test` (and the DoD "existing tests pass") fails to compile `:play` (no `DropResolver` yet), poisoning every earlier task's self-check. The held-out slice must stay out of every implementer-visible path and out of the `*/src/test/*` globs until Test & Verify, then be moved in so trace-check rules 1 and 3 see its tokens (until then trace-check will legitimately show those IDs uncovered).
  2. **ATA inputs.** The ATA works "from the REQs and frozen contracts alone", yet the readings that give A1/A3/A4 their meaning are not in the REQ files: F23 and F31 (`req_review_01.md`, accepted at G1 - note WO-001 cites F23 as "decisions.md" but it is not there), F2/F5 and DA-1..DA-7 (`decisions.md`). The ATA brief must name them and name the design's signature surface (§2-§4, §7, "Test seams") as the frozen interface; it must exclude `tools/prototype_template.html` and `tangram_geom.py` (WO: "never a source of acceptance tests"). Two Test-seams assertions are DA assumptions, not REQ text (REQ-051.A2 "MINI with overBoard=false -> null" is DA-4/F30; DA-1 tie-break): the ATA should cite DA-4 as its source or leave them to the visible kernel tests.
  3. **Recirculation target.** All 9 IDs only become testable at TASK-006, a ~50-line wrapper, so a visible or held-out failure is almost always an engine defect in TASK-004/005. The plan does not say that TASK-006's implementer must hand back `recirculate -> TASK-00x` rather than patch kernel code (no orchestration/seam-crossing inside tasks).
- **Proposed resolution:** Add to `tasks.md`: (a) an ATA line - dispatched in parallel from the start, reads the REQ/decision/design-signature list above, writes visible tests to a staging path (for example `workorders/WO-001-tests/visible/`, moved into `play/src/test` when TASK-006 is dispatched) and the held-out slice to a path implementers' briefs exclude (moved in at T&V); (b) TASK-006 "Depends on" gains "ATA visible tests delivered"; (c) one sentence on failure routing. Optionally let the ATA also write the A3-style checks at `LockSearch.find` so TASK-005 has independent visible tests (kernel tokens are allowed; rule 3 only needs one under `play`).
- **Triage:** *(filled on recirculation)*

### F4 - TRACE · Should · `tasks.md` TASK-005 (REQ link column), TASK-001..004
- **Observation:** TASK-005's REQ link reads "REQ-019 A1-A4 (engine)" and TASK-006 claims all 9 as well. Under the DoD, "the visible acceptance tests for every acceptance ID the task claims must pass before handoff" - but those tests live at `DropResolver`, which TASK-006 builds after TASK-005. As written TASK-005 cannot meet its own Done, or its implementer is invited to write the acceptance verdict himself (the kernel golden, margin and round-trip tests are implementer-authored). TASK-001..004 are correctly worded "enabler".
- **Proposed resolution:** Re-word TASK-005 to "enabler for REQ-019 A1-A4; acceptance verified at TASK-006", and state once in `tasks.md` that all kernel tests of TASK-001..005 (golden, clip, margin, round trip, `find` unit tests) are scaffolding: marked as such and carrying **no** `REQ-NNN.A<n>` tokens, so the only acceptance coverage is the ATA's.
- **Triage:** *(filled on recirculation)*

### F5 - QUALITY (task shape, hardest-first) · Should · `tasks.md` TASK-005, Order line
- **Observation:** TASK-005 holds two seams and six or more files: `ConvexClip` (float clip, flush/collinear contract, degenerate-case tests) and `LockSearch`/`Fit` (candidate keying, validity, score, window, DA-1 tie-break, own-corner exclusion, `lockDistance`) plus the margin test and the round trip over all 13 puzzle files. It is the biggest task and, per design "Risks", the highest-risk logic ("the validity predicate and anchor completeness at exact contact"; the design reviewer was told to read §5/§6 first). It sits fifth of seven in a strict chain, after the golden infrastructure. The Order line gives no justification for risk-last (`task-decomposition.md`: "hardest first ... or justified"). `ConvexClip` consumes only float polygons (it needs `Vec2` from TASK-002 and nothing else), so the chain order is not forced for it. All acceptance verdicts are likewise deferred to the last task (late-surprise exposure).
- **Proposed resolution:** Split TASK-005 into 005a `ConvexClip` + the degenerate-case tests (depends on TASK-002 only, scheduled immediately after it, `[P]` with 003/004 - own files only) and 005b `LockSearch`/`Fit` + margin test + round trip (depends on 004 and 005a). Or keep one task and add a one-line justification in the Order line that explains why risk-last is unavoidable. Either way, name which task owns the margin test, since it is the DA-3 safety net.
- **Triage:** *(filled on recirculation)*

### F6 - QUALITY (task shape) · Should · `tasks.md` TASK-003
- **Observation:** TASK-003 spans two languages and toolchains and two seams: Python (`tangram_geom.outline_corners` fix, `export_geometry_golden.py` with the sampling oracle, `tools/golden/geometry.json`) and Kotlin (golden loader, freshness, shapes, 80 transforms, per-file poses and areas). The golden JSON format (design §8) is a ready-made frozen interface between them. The Python half depends on nothing in the kernel (it needs no TASK-001/002), and it is the half with the outside blast radius (F2) and the "reference fix" decision; the Kotlin half is what needs TASK-002. Bundling forces the strict chain and one context to hold both.
- **Proposed resolution:** Split into 003a (Python: reference fix, exporter, oracle, golden, rule-10 chain; depends on nothing; `[P]` with 001/002 because it touches `tools/` only) and 003b (Kotlin golden tests; depends on 002 and 003a). That also surfaces the DA-7 risk early.
- **Triage:** *(filled on recirculation)*

### F7 - TRACE · Should · `workorders/WO-001.md` (Constraints, DoD)
- **Observation:** (1) "all 10 acceptance IDs are verifiable as JVM unit tests" - the scope, build-map and design say 9. (2) The DoD line "Tests cover every acceptance criterion of REQ-019/020/021/051" includes REQ-020.A2 and the on-screen parts of REQ-021/051, which the same WO carries to WO-003; closed literally it is unsatisfiable, closed loosely it is a silent redefinition at G4. The design already flags the "10" as a document to align.
- **Proposed resolution:** Make the DoD line "the 9 in-scope acceptance IDs (REQ-019 A1-A4, REQ-020 A1, REQ-021 A1-A2, REQ-051 A1-A2) at engine/state level; carried IDs listed", fix "10" to "9", and add DoD items for the non-JVM checks the WO creates: V-01/V-05/V-06 pass and `test_verifiers.py` green (verifiers "run at every WO close beside trace-check", architecture §2), exporter exits 0 with the golden fresh, rule-10 runs (F1/F2).
- **Triage:** *(filled on recirculation)*

### F8 - QUALITY · Note · `tasks.md` TASK-000
- **Observation:** TASK-000 is rightly first (it is the riskiest build-side item: AGP 9.4.1 library plugin, `subprojects { plugins.withId }` DSL, which the design says may need a fallback), but its row carries no done-check and its link names only ADR-002/G-06 while it also carries G-03 (golden inputs), G-02 (new catalog entries) and G-01 (`startup-runtime`). The new `play` module has no source until TASK-006, so `:play:test` would report NO-SOURCE, and the first real compile/test of an Android library module happens at the last task.
- **Proposed resolution:** Add the design §10 check as the Done ("`gradlew assembleDebug test :app:processReleaseMainManifest` green") and a trivial class plus one scaffolding test in `play`, run via `:play:testDebugUnitTest`, so the new module's toolchain is proven at the start (G3 intent: "trivial compile + test has actually run"). Add G-03/G-02/G-01 to the link column.

### F9 - QUALITY · Note · `tasks.md` TASK-007
- **Observation:** Correctly `[P]` (files disjoint, depends only on TASK-000). It bundles two unrelated slices (manifest hardening + V-01 needing Gradle; V-05/V-06 pure Python) and a self-test: roughly six files, acceptable but near the size limit. Its REQ link "(REQ-001/008/010 side)" can be read as claiming acceptance IDs that WO-006 owns. In the shared working tree, TASK-007's self-check must not run a whole `gradlew test` while 001-006 are mid-edit (transient kernel compile errors would fail it for reasons it does not own); a second Gradle invocation in the same tree also waits on the build lock.
- **Proposed resolution:** Reword the link to "G-01 (serves REQ-001/008/010; their acceptance is WO-006's), G-05, G-06"; limit TASK-007's self-check to `python -m unittest` and `:app:processReleaseMainManifest`; optionally split 007a/007b. The V-01 test fixtures are implementer-written: Code Review should confirm each verifier fails on its fixture.

### F10 - TRACE (records) · Note · `workorders/WO-001.md` header and ledger; `tasks.md` change log
- **Observation:** `tasks.md` v0.2 says "orchestrator as planner - logged inline exception", but there is no such row in `decisions.md` (the other inline exceptions have one) and the ledger's Work-Order Planner row is empty. The WO header reads "Status: Approved (G3 ...)" while "Plan approved: —" (pending this review; governance row 7 = ai). The design-reviewer ledger row still says "spot-check pending" although the spot-check verdict (forward) exists.
- **Proposed resolution:** Add the decisions.md row, fill the planner row and the spot-check status, and set the G3 status only after this review's findings are triaged.

### F11 - QUALITY · Note · `tasks.md` rows (brief inputs)
- **Observation:** Rows point to design sections (good), and the briefs do not yet exist, so nothing is restated wrongly. Small items for the brief step: TASK-003 writes "(anchors = outline corners)", a paraphrase of REQ-019 rule 1 - point at "REQ-019 Rules, first bullet" instead; TASK-004 omits the `isOutlineCornerMask` unit tests that design §5 requires; TASK-002's brief must say to create `PieceShapes.kt` with the Write/Edit tools (shell writes are invisible to the guard and become drift, per the lesson in `progress.md` 06:59) so the contract-delta is logged and the WO Contract-deltas row can be filled; implementer briefs must cite design sections by number and exclude the "Test seams" section (and the DI-1 prototype stays behaviour reference only).
- **Proposed resolution:** Carry these into the dispatch briefs.

## Mode and toolchain

- **Mode Full:** justified (novel exact-geometry logic, first WO, new module, new dependencies, > 300 lines); `gates-and-autonomy.md` §5 Light is excluded on several counts (new external dependencies, diff size). The WO text names two; fine.
- **Toolchain proof (G3):** present and re-derived for Gradle/AGP/Kotlin/JUnit (app, kernel, contracts); not shown for the browser channel (F1) or for the new `play` library module (F8).

## Handoff — Plan Reviewer · WO-001
- **Scope:** REQ-019 A1-A4, REQ-020 A1, REQ-021 A1-A2, REQ-051 A1-A2 · governed touched: `IPuzzleLibrary` (consumes) | no `I*` change; one notify-tier delta (`PieceShapes.kt`)
- **Inputs read:** tasks.md v0.3 · WO-001.md · designs/WO-001-design.md (revised after review 01) · build-map.md v1.1 · architecture.md v1.0 · .swdev/guard.json · progress.md (2026-10-02) · REQ-019/020/021/051 · SWDev task-decomposition v0.3, definitions v0.3, gates-and-autonomy v0.6, review-report, handoff-contract v0.2
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-001-plan-review.md`
- **Status:** recirculate -> planner (F1 Blocker: browser verification channel for the rule-10 runs is not reachable and the WO says none is needed; F2-F7 Shoulds)
- **Traceability delta:** none (review only); coverage verified 9/9 IDs on TASK-006, every task linked
- **Notes for next station:** after the fixes a short spot-check of tasks.md v0.4 (F1, F3, F4, F5, F6) is enough; the Acceptance Test Author brief needs the F3 input list.

## Spot-check — tasks.md v0.4 and WO-001.md after plan review 01 (2026-10-02)

**Inputs:** `tasks.md` v0.4, `workorders/WO-001.md` (status Proposed), `decisions.md` rows 44-45, the design-review spot-check (for the margin-test Note), the scratchpad Playwright venv. Re-derived, not taken on trust: Playwright and Chromium launch from `venv-pw` with `PLAYWRIGHT_BROWSERS_PATH` set to `pw-browsers` (and fail without it); the TASK-007 unittest command was run in a scratch tree.

- **F1 - fixed (proved, not waived).** The channel is reachable: Chromium 153 launched through the venv in this review. WO "Verification channels" now names the three channels and the proof. Residual Note N3.
- **F2 - fixed.** TASK-003a's done-check owns the whole rule-10 chain (validator, renderer + sheet look, prototype rebuild, `test_prototype.py`, `validate.py`), and the Order paragraph names the generated `Spec/prototype/` and `Tangrams/previews/` as the files it may change. (`make_ui_sheets.py` also imports `outline_corners` but draws only `nature-mountain`; the golden diff shows if its corners moved.)
- **F3 - fixed, by a different route than proposed (accepted).** TASK-T row exists (6 visible, 3 held-out, ~1 in 3), TASK-006 depends on it, visible tests are never compiled before TASK-006 and every earlier self-check is module-scoped (checked: no pre-TASK-006 done-check is a bare `gradlew test`), the held-out slice sits in `.swdev/heldout/WO-001` outside `test_globs`, and the failure-routing paragraph is in. Not verifiable from the plan: the ATA's source list (F23/F31/F30, decisions F2/F5, DA-1/DA-4); see N5.
- **F4 - fixed.** TASK-005b is labelled enabler with acceptance at TASK-006, and the header says implementer tests carry no `REQ-NNN.An` token.
- **F5 - fixed.** TASK-005a `ConvexClip` has its own file, depends only on TASK-002 (`Vec2`), runs early in parallel; TASK-005b owns the margin test and the round trip. See N2 for the shared test source set.
- **F6 - fixed.** TASK-003a is Python only, no dependency, `[P]` from the start; TASK-003b owns the Kotlin reader and depends on 002 + 003a. Dependencies and the Order line agree.
- **F7 - fixed.** DoD names the 9 IDs, golden, verifiers and rule 10; "10" is now 9. Stale: the "Guardrails in play" line still says rule 10 is "(validator, prototype build)" (N6).
- **F8 - fixed.** Per-task done-check column added. TASK-000 checks `:play:compileDebugKotlin` only (a source-less module), but the design-review spot-check already ran the `play` tests in a scratch copy, so the risk is covered.
- **F9 - fixed, one new defect.** Link reworded, self-check module-scoped. The new done-check `python -m unittest .swdev/verifiers/test_verifiers.py` does not run: "ValueError: Empty module name" (the leading dot of `.swdev`, Python 3.14). See N1.
- **F10 - fixed.** Status Proposed, ledger rows filled, decisions.md row 44 for the inline planner.
- **F11 - fixed.** `PieceShapes.kt` via Write (with a guard-log check), `isOutlineCornerMask` tests in TASK-004, no REQ-019 paraphrase left.

New Notes (none blocks; all are one-line fixes at brief time):
- **N1 (TASK-007 done-check).** Replace with `python -m unittest discover -s .swdev/verifiers -p test_verifiers.py` (verified to run) or `cd .swdev/verifiers && python -m unittest test_verifiers`.
- **N2 (TASK-005a ∥ TASK-003b).** No shared file, but both work in the `kernel` main/test source set, so one worker's half-written file fails the other's `:kernel:test` compile. Run them in separate worktrees (`isolation: worktree`) or serialize the pair.
- **N3 (browser recipe).** The WO and decisions row say "venv-pw", "pw-browsers" but not the exact invocation. Without `PLAYWRIGHT_BROWSERS_PATH=<scratchpad>/pw-browsers` Playwright cannot find Chromium. The TASK-003a brief must carry the venv interpreter path, that variable and `PYTHONIOENCODING=utf-8`; the channel is session-scoped, so the WO-close re-run needs it again.
- **N4 (margin test wording).** TASK-005b widens board (iii) per the design-review residual Note, but design §8 still says "exactly one solution piece". Add a one-line design addendum or cite the Note in the TASK-005b brief, so the implementer is not following two texts.
- **N5 (ATA/held-out hygiene).** TASK-T was dispatched before this spot-check (fine under row 7 = ai). Check its handoff for the source list; the held-out directory must not contain a `src/test` path segment (the glob `*/src/test/*` matches at any depth) and implementer briefs must list `.swdev/heldout` under "Do not read".
- **N6 (WO text).** Update the "Guardrails in play" sentence to the full rule-10 list.

**Verdict: forward** -> Orchestrator (dispatch). Plan review 01: F1-F11 all closed; 0 Blockers, 0 Shoulds open; 6 Notes open (N1-N6), of which N1 and N2 should be applied before TASK-007 and the kernel pair are dispatched.
