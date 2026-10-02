# Review — plan · WO-003

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` v0.8 (WO-003 section; WO-001/WO-002 sections for conventions) · `workorders/WO-003.md` · `designs/WO-003-design.md` (Revised; "Test seams", "Suggested cut"; seam lines E1–E7 not reviewed) · `reviews/WO-003-design-review.md` (verdicts) · `build-map.md` v1.1 §2 · `architecture.md` v1.0 §2/§5 · `AGENTS.md` · `decisions.md` DA-23…30 · `.swdev/guard.json` · SWDev task-decomposition.md, gates-and-autonomy.md §1/§7, handoff-contract.md, review-report.md. REQ files: only the A-ID counts were checked (17 files, matching the WO scope).

## Verdict

**recirculate → planner (orchestrator).** The shape is sound: serial order in `play`, interface-first kernel step, mandatory CR-1 before any Compose task, staging outside the scanned trees. But several things the plan relies on are not written down in the task rows. The first Compose/instrumented run comes only at TASK-019. The two riskiest tasks (015, 018) are oversized. Some carried-in items land on no task row. The G3 API 26 waiver is unrecorded. No coverage hole is fatal; all fixes are edits to `tasks.md` and the WO file.
**0 Blockers, 9 Shoulds, 5 Notes.**

## Checklist applied

- [x] **Directives** — D1: serial slices behind one `play` surface. D3: every task names REQs or an enabler guardrail, except DA-23 (F9). D5: dependencies are all in TASK-011 and match design §9.
- [x] **Guardrails** — G-01/05/06 each have a verifier or done-check. G-04: the `puzzle` extra is debug-only with a release stub; see F9 for its check.
- [x] **Contract** — no `I*` touched; the only notify-tier item is the build-map edit in F6.
- [x] **Scope** — nothing unrequested except DA-23 (decided, logged, see F9).
- [x] **Traceability (both ways)** — every in-scope REQ and carried-in REQ lands on a task and on TASK-T3 at REQ level. Hole at A-ID level and for three carried items (F1, F2).
- [x] **Evidence re-derived** — checked that `.swdev` is in trace-check's `SKIP_DIRS` (so `.swdev/staged/**` and `.swdev/heldout/**` are never scanned), that `.swdev/heldout/WO-003/` does not exist yet and `.swdev/staged/` does not exist yet (T3 creates both), and that `.swdev/verifiers/v01_release_manifest.py` needs `--manifest` / `--res-dir`.
- [x] **The conceptual 20 %** — spent on where unverified assumptions pile up: the first instrumented run, the pointer adapter, and who moves and runs staged tests.

## Trajectory & quality

- **Verification actually run?** The G3 instrumented channel is proven in a scratch copy (API 37, 1 trivial test). It is not proven inside the real project's build wiring (F7), and the API 26 channel is neither proven nor waived (F8).
- **Proportionate?** Mostly. TASK-015 and TASK-018 each bundle several seams (F5).
- **Path sane?** Yes. The plan applies the WO-001/002 lessons: serialization for the shared source set, staging, and one build step.

## Findings

### F1 — TRACE · Should · tasks.md WO-003 rows (Serves column), "Coverage, both ways"
- **Observation:** "Serves" names REQs, not acceptance IDs, and the coverage paragraph asserts "every ID lands on a task" by pointing at the design's Test seams table. The WO-002 plan (v0.7) added A-ID Serves cells for exactly this reason. Example: TASK-018 serves "REQ-014, 018, 021, 023, 039, 043, 051" with no A-IDs, so REQ-018 A2 (badge exists only with PG) and REQ-023 A2 (no piece boundary) are not visibly assigned.
- **Proposed resolution:** Replace Serves with A-ID cells (REQ-nnn.A1, A2…) per task, plus a one-line index (ID → code task → T3 file). Then re-check both directions from that.

### F2 — TRACE · Should · tasks.md / WO-003.md carried-in list
- **Observation:** Four carried-in items are named by no task row, so none is owned:
  - the on-device library check (DA-22) and the `play/src/androidTest` kernel smoke;
  - the "release with the pose of the last displayed preview" calling rule (it sits in TASK-015's work but not in its row);
  - the 180 ms glides (build-map WO-001 row: "out → WO-003"; design §4/§0; DA-19). Not in `WO-003.md` Carried-IN either.
  - Also: "F3" is used for both the WO-001 code review F3 (TASK-012) and decisions F3 (TASK-019). The WO file is clearer than the task rows.
- **Proposed resolution:** Name each item in the owning row: library check and kernel smoke → TASK-019 (and 018 for the smoke, see F3). Calling rule and glides → TASK-015 (state) and TASK-018 (draw). Add the 180 ms glides to WO-003.md Carried IN. Spell the two Fs as "code-review F3" and "decisions F3".

### F3 — QUALITY · Should · TASK-018 / TASK-019 (where androidTest tests run)
- **Observation:** TASK-019 moves **all** staged androidTest tests (including `play/src/androidTest`: pixel seams, picture clip, size marks, flip badge, the touch-injected adapter tests F1/F2 of the design review, and the kernel smoke) only after the shell exists. TASK-018's done-check (`:play:assembleDebug :play:testDebugUnitTest`) runs **no** instrumented test. So the drawing and the pointer adapter, the design's second and highest risks, are first exercised one task later, in a different implementer's context. A failure at 019 recirculates across two tasks and the CR-1 boundary.
- **Proposed resolution:** Split the move. The `play/src/androidTest` tests go in after TASK-018 and run with `:play:connectedDebugAndroidTest` (the emulator is up, started by the orchestrator, see F8). Only the `app/src/androidTest` tests (library check, end-to-end solve, REQ-002 A2, REQ-045 A2) wait for TASK-019. These `play` tests need `PlayArea` and a `PlaySession` only, not the shell.

### F4 — TEST · Should · tasks.md "Order" / staged tests (acceptance coverage arrives late and routing is missing)
- **Observation:** Staged JVM visible tests move in only after TASK-017, so TASK-012…016 hand off with scaffolding tests alone. The decomposition rule says a task's visible acceptance tests must pass before its handoff. IDs whose code lands earlier (REQ-013 and F8 `overBoard` in 013, REQ-022 and the kernel truth tables in 012, timeline and `PathData` in 014) are not checked until 017, when a failure is expensive to route. The WO-001 plan had a "Failure routing at TASK-006" paragraph; WO-003 has none (for 017, CR-1, 019). Also, the staged files compile against frozen seam signatures the author cannot compile: one signature mismatch breaks the whole `play` test source set at 017.
- **Proposed resolution:** (a) Move each staged test in at the task that creates its API (T3's handoff already lists IDs per file), not all at 017. (b) Add a "Failure routing" paragraph: failing visible test → read against REQ and seam; cause in an earlier task → `recirculate →` that task; the test itself wrong → implementer contests once (orchestration.md §5), orchestrator rules by the REQ. (c) State that implementers build the frozen seam signatures verbatim, and may read `.swdev/staged/WO-003/**` (visible tests, not `heldout`). A deviation is a recirculate to the planner, not a test edit.

### F5 — QUALITY · Should · TASK-015 and TASK-018 (task shape)
- **Observation:** TASK-015 is `PlaySession` + `DragMotion` + `DropResolver.refit` carrying nine REQs and F5: pieces and `where` states, drag lifecycle, frame sync with the "displayed preview" rule, release/lock, interruption, tap/flip through `refit`, solved/state transitions, and all animation facts (glide, shake, pulse, solved). That is the design's highest-risk seam in one implementer pass. TASK-018 is all drawing (silhouette union, pieces, preview, pulse, picture, confetti), `PlayArea`, tray composables, size marks and badge, the frame loop and the pointer adapter. Two contexts, and the adapter is the Blocker-class area of the design review.
- **Proposed resolution:** Split 015 into 015a (pieces/state, beginDrag/dragTo/onFrame/release/interruptDrag, `refit`; the highest-risk part, done first) and 015b (tap/flip intents, glide/shake/pulse/solved facts, `DragMotion`). Split 018 into 018a (drawing, tray, marks, badge, `PlayArea` without gestures; pixel tests) and 018b (frame loop plus pointer adapter; touch-injected tests). Keep each serial in `play`; keep CR-1 before 018a.

### F6 — TRACE · Should · WO-003.md / build-map §2 / tasks.md Carried OUT
- **Observation:** `tasks.md` carries "→ WO-005 (V-04 fold)" (design §6.3: fold `check_apk_puzzles.py` into V-04). `WO-003.md` Carried OUT and the build-map WO-005 row do not mention it, so the carry has no owner at WO-005's start. WO-003.md also lists the REQ-033 haptic carry (→ WO-007) while tasks.md lists only "sounds, haptic tick, REQ-033" in the shorter form; check the build-map WO-007 row says the same.
- **Proposed resolution:** Add the V-04 fold to the WO-003 Carried OUT and to the build-map WO-005 row (notify tier, via Edit/Write). Make the three lists agree.

### F7 — TEST · Should · TASK-011 done-check (channel not exercised at the start)
- **Observation:** The design (§10, risk 3) says to prove the instrumented channel on the real project before the Compose tests are written: first `androidTest`, `ui-test-manifest` in a library module, runner wiring, time. The proof was in a scratch copy with a trivial test. TASK-011 is where the real wiring (runner, `ui-test-manifest` in `play`, `app` → `play`/`content`) is built, but its done-check compiles only main and unit tests (`assembleDebug :kernel:test :content:test`) and never the androidTest source sets. A wiring error would surface at TASK-018/019.
- **Proposed resolution:** Add to TASK-011's done-check: `:play:assembleDebugAndroidTest :app:assembleDebugAndroidTest`, plus one trivial instrumented test in each of `play` and `app` run once on the API 37 AVD (the orchestrator starts and stops the emulator). Delete the trivial tests when the real ones arrive.

### F8 — QUALITY · Should · "Order", TASK-018/019/020 (emulator ownership, API 26 waiver timing)
- **Observation:** (a) Done-checks need an emulator "up", but the plan never says who starts it, when, or what happens when the implementer cannot run the check. The brief's own rule is that the emulator start/stop belongs to the orchestrator; as written, TASK-018/019 can only reach `done` with the emulator already running. (b) G3 (gates-and-autonomy §1) requires every verification channel to be proven **or** covered by a recorded waiver with an exit condition **before P4**. The API 26 image is not installed (WO-003.md: "not installed yet") and the WO Waivers table is empty. The design defers the waiver decision to "when the first Compose task starts"; the plan has no step for it, so the first look at the image is TASK-020, last.
- **Proposed resolution:** (a) Add an orchestrator precondition to 018a/018b/019: AVD started before dispatch, stopped after; if it will not start the task hands back `implemented (unverified)` rather than `done`. (b) Before TASK-011 is dispatched, record the API 26 waiver in WO-003.md Waivers (what: API 26 release launch + kernel smoke on API 26; scope: TASK-020 and the DoD line; exit: Jami installs the image, then the deferred checks run first), or have Jami install it. Add a step "check image present" at the start of 018a. Keep the waiver in the metrics and the G4 surface.

### F9 — SCOPE · Should · TASK-019 (DA-23 debug-only `puzzle` extra)
- **Observation:** The extra has no REQ; the Serves cell for TASK-019 does not name it, and "silent scope" is exactly what the tasks.md header forbids. It is logged (decisions DA-23) and accepted by the design review, so it is not a defect in itself. Missing: how the plan proves the release stub cannot read an extra (`app/src/release` returns `null`, no `getStringExtra` in the release dex), and that the extra is not used by any test.
- **Proposed resolution:** Put "DA-23 verification aid (not a REQ)" in the Serves cell. Add to the done-check one cheap proof for the release side (a unit test of `PuzzleExtra` in the release source set, or grep of the release sources for `getStringExtra`; V-01 re-run). State that no acceptance test depends on the extra.

### F10 — QUALITY · Note · TASK-020, TASK-009-style command spelling
- **Observation:** TASK-020's rule-10 chain is written as "AGENTS.md rule-10 chain ALL PASS". On this machine `test_prototype.py` needs `PLAYWRIGHT_BROWSERS_PATH`, the scratchpad venv's `python.exe`, `PYTHONIOENCODING=utf-8` and `--fast` (WO-002 TASK-009 spells it out); an implementer will not know it. Same for V-01 (`--manifest`, `--res-dir`) and V-05/V-06 in TASK-019 ("PASS" without commands). The WO-003 header note also drops the WO-002 line about `JAVA_HOME`/`ANDROID_HOME` being set.
- **Proposed resolution:** Copy the exact commands from the WO-002 TASK-009 row and the TASK-007 row, or point to a single "Commands" block under the table. Repeat the env-variable note.

### F11 — QUALITY · Note · dependencies and `[P]`
- **Observation:** TASK-014 "depends on 013" and TASK-016 "depends on 015" but neither uses the other's code; the edge is source-set serialization only (`PathData`/timeline/confetti are independent of `PlayLayout`; strings are resource-only). The plan says so in "Order" but the Depends-on column reads as a logical dependency. No `[P]` is claimed anywhere, which is safe. Possible parallel work (TASK-020's Python script after 011, TASK-016 with no code) is left unused on purpose.
- **Proposed resolution:** Label serial-only edges "(serial: shared source set)" in Depends-on, as WO-001 did. Optionally mark TASK-016 and the TASK-020 script `[P]` after TASK-011 (own files only: `play/src/main/res/`, `tools/`).

### F12 — TEST · Note · held-out slice and scaffolding tokens
- **Observation:** Held-out isolation is sound: `.swdev/heldout/WO-003/` and `.swdev/staged/` are both under `.swdev`, which trace-check skips, and the orchestrator checks isolation and tokens (as WO-001/002). Two small gaps: the T3 row asks for "~1 in 3 IDs, risk-weighted" but does not say the slice should include at least one of F5/REQ-017, REQ-021 and the pixel/adapter set; implementer rows 013/014/015/017 say "scaffolding tests" without the no-token rule that TASK-010 spelled out.
- **Proposed resolution:** Name the risk-weighted candidates in T3's row (drag session, frame-synced preview, cancel, picture seam). Add "scaffolding, no `REQ-NNN.An` token" to the implementer rows.

### F13 — QUALITY · Note · CR-1 and TASK-017 wording
- **Observation:** TASK-017's row says the orchestrator moves the tests "then they must pass unedited", which reads as the implementer's own done-check, but the implementer cannot move files in `.swdev/staged`. CR-1 has no recirculation target ("verdict forward" only).
- **Proposed resolution:** Make the orchestrator move the matching staged tests **before** dispatch (see F4a) so the implementer's done-check includes them. For CR-1: findings route `recirculate →` the owning task (012…017), re-review only the changed files.

### F14 — QUALITY · Note · TASK-019 done-check completeness
- **Observation:** The check runs `connectedDebugAndroidTest` and "V-01, V-05, V-06 PASS" but names no step that produces the debug APK path Jami plays (the WO's headline DoD) or the adb lines of DA-23 for the checkpoint.
- **Proposed resolution:** Add "`app/build/outputs/apk/debug/*.apk` exists" to the done-check; the orchestrator surfaces the install and `am start --es puzzle <id>` lines at close.

## Handoff — Plan Reviewer · WO-003
- **Scope:** REQ-002, 011, 012, 013, 014, 015, 016, 017, 018, 020, 021, 022, 023, 039, 043, 045, 051 + F3 (both), F5/DA-5, F8 · governed touched: none (consumes IPuzzleLibrary)
- **Inputs read:** tasks.md v0.8 · WO-003.md · designs/WO-003-design.md (revised) · reviews/WO-003-design-review.md · build-map.md v1.1 §2 · architecture.md v1.0 §2/§5 · AGENTS.md · decisions.md DA-23…30 · `.swdev/guard.json` · SWDev task-decomposition, gates-and-autonomy §1/§7, handoff-contract, review-report
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-003-plan-review.md`
- **Status:** recirculate → planner (A-ID coverage and named carried items, split of TASK-015/018, androidTest run at 018, real-project channel in TASK-011, API 26 waiver before dispatch, failure routing; 0 B / 9 S / 5 N)
- **Traceability delta:** none (review only; the planner's edits will change task-to-ID links)
- **Notes for next station:** fixes are edits to tasks.md, WO-003.md (Carried IN/OUT, Waivers) and the build-map WO-005 row. A spot-check of F1–F9 is enough. After forward, the Acceptance Test Author needs the corrected design seam lines (E1–E7).

## Spot-check (tasks.md v0.9, WO-003.md, build-map.md)

**Verdict: forward.** All nine Shoulds are closed or accepted; no new findings blocking.

- F1 closed: A-ID Serves cells and an ID → task → T3 index are present; both directions re-checked, no gap.
- F2 closed: library check → 019, kernel smoke → 018b, calling rule → 017, 180 ms glides → 015b (and in WO-003.md); the two F3s are disambiguated.
- F3 closed: `play/src/androidTest` runs at 018b on the emulator; `app` tests after 019.
- F4 accepted in part: one move-in at 017 is justified (staged files mix APIs); the verbatim-seam rule and the failure-routing paragraph compensate. Residual: 013/014/015 have scaffolding only until 017; CR-1 and 017 catch it.
- F5 closed: 015a/015b and 018a/018b, each serial, with CR-1 before 018a.
- F6 closed: the V-04 fold is in WO-003.md Carried OUT, the build-map WO-005 row and tasks.md.
- F7 closed: TASK-011 compiles both androidTest variants and runs the trivial instrumented test on API 37.
- F8 closed: emulator precondition paragraph; the API 26 waiver is recorded in WO-003.md (exit: Jami installs the image).
- F9 closed: DA-23 labelled a verification aid, with a `testReleaseUnitTest` proof of the release stub.
- F10–F14 closed: commands spelled out, serial edges labelled, CR-1 routing, orchestrator moves tests, APK existence checked.
- Note (non-blocking): the T3 row says "running", so the TASK-T3 brief must use the seam lines as corrected by E1–E7.
