# Progress Log — TangramNoAds (build side, SWDev)

Append-only. One entry per interaction:

- the human's input, **VERBATIM**
- a **~10-line** summary of what the AI did

This log is the ground truth the SWDev framework reads to improve itself
(see `C:\GitHub\AI\SWDev\framework\feedback\`). Capture-side history lives
in `Requirements/` (sources, evidence, change log) and `STATUS.md`.

---

## 2026-10-01 18:52 — adopted under SWDev v0.15 (intake path C)

**Human (verbatim):**

> k.. I wanna be able to run full planned requirement, feature etc from project under SwDev.. plz do steps as you propose what you can do automatic.. we need to figure here project mode where AI is more control on these smaller projects if I label something to AI-mastered project. For questions you need answer to tune frameworks, plz give me more human readable description from problem, proposal and question

**What was done (summary):**

- Owner decision recorded: the whole locked collection (BIZ/FUN/UI) is the
  spec for the build — SWDev S-01 (`SWDev/swreq-intake-review-2026-10.md`).
- SWDev v0.15 landed in the framework repo (trace-check v0.4 reads the
  collection natively; `build-map.md` + `design-inputs.md` templates;
  intake path C in `initnewproject.md` §2b; principle 2 reworded).
- This project scaffolded beside `Requirements/` (nothing written into it):
  `architecture.md` (placeholder), `build-map.md` (subtree rows, code homes
  TBD until technology is chosen), `design-inputs.md` (DI-1…DI-7 with the
  Contract-delta), `tasks.md`, `proposals.md`, this file,
  `.swdev/guard.json` (locked REQ files denied to the build side),
  `.claude/settings.json` (SWDev hooks), `AGENTS.md` § "Build under SWDev".
- trace-check on this project: 51 REQs parsed (47 locked, BIZ 7 / FUN 29 /
  UI 11), 53 features (37 build units), req-lint 0 errors, views fresh —
  RED only for uncovered acceptance (no product code yet) and the missing
  contract baseline (frozen by Jami at G1 acceptance).
- Phase: P1 — acceptance of the locked collection. Pending: fresh-eyes
  review, "Accept collection v1.1 for TangramNoAds (G1)?", `--freeze`.
- Open for the owner: the AI-mastered build mode
  (`SWDev/decisions-needed-2026-10.md` Q1–Q6) and the technology topic.

## 2026-10-01 20:12 — governance in force, stack decided

**Human (verbatim):**

> A. Kotlin + Jetpack Compose (native) is ok if can build it my computer. Anyway questions OK but need to jump Claude Code windows terminal soon for builds. And we do not build today as running out of tokens / time.. but get everything here ready with you

**What was done (summary):**

- `governance.md` → v0.2 **in force** (ai-mastered), set by Jami: floors
  F1–F4 confirmed, data migration dialable, prototype lane yes, checkpoint
  per WO close, fresh-eyes review to run (answers to
  `SWDev/decisions-needed-2026-10.md`).
- Row 5 technology = **owner-decided: Kotlin + Jetpack Compose, native**,
  conditional on the G3 toolchain proof passing on Jami's computer; if it
  cannot, stop and ask — never switch stacks silently. To be recorded as
  ADR-001 in `architecture.md` at P2.
- Decisions mirrored in SWDev (`gates-and-autonomy.md` §8, `TODO.md` T-05)
  and SwReqCollector (`req-format.md` §6, `capture-loop.md` §12.4, plan,
  CHANGELOG — the prototype lane).
- `KICKOFF.md` written: toolchain checklist + the copy-paste prompt for
  the first Claude Code terminal session (P1 acceptance → P2 → WO-001
  #Locking). No build today.
- trace-check: governance profile IN FORCE recognised; RED only for
  uncovered acceptance + missing baseline (freeze at G1).

## 2026-10-02 05:40 — first Claude Code terminal session: P1 acceptance started

**Human (verbatim):**

> You are the SWDev build agent for TangramNoAds. Read AGENTS.md (all of it, incl. "Build under SWDev"), governance.md (IN FORCE, ai-mastered: `ai` rows you decide and log to decisions.md, `human` rows and floors stop for me), then KICKOFF.md and follow its §1 steps in order: P1 acceptance (trace-check, fresh-eyes review via the requirements-reviewer subagent, ask me "Accept collection v1.1 for TangramNoAds (G1)?", then --freeze), P2 architecture (stack is DECIDED: Kotlin + Jetpack Compose native, port the prototype's logic, ADR-001), then WO-001 = #Locking with the G3 toolchain proof on this computer first (gradlew assembleDebug + testDebugUnitTest). Requirements/ is read-only for you. Frameworks at C:\GitHub\AI\SWDev and C:\GitHub\AI\SwReqCollector are read-only. Log every decision to decisions.md, every turn to progress.md, never commit. Start with step 1 now.

**What was done (summary):**

- Read AGENTS.md, STATUS.md, governance.md (v0.2 in force), KICKOFF.md, build-map.md,
  design-inputs.md and the SWDev session-start files. Drift check: SWDev README is
  v0.15 = AGENTS.md's recorded version, no drift.
- KICKOFF step 1: trace-check parses the collection — 51 REQs (47 locked, 4 withdrawn;
  BIZ 7 / FUN 29 / UI 11), 53 features (37 build units), req-lint 0 errors / 5 warnings
  (the withdrawn/idea leaves), views fresh, governance IN FORCE. RED only for uncovered
  acceptance + missing baseline, as expected. `swreq_tools` path correct on this machine.
- Step 2: requirements-reviewer subagent (fresh context, read-only on Requirements/,
  ~15 min) wrote `req_review_01.md`: 38 findings — **3 Blockers** (F1 `req_types.md`
  v0.2 is Draft/unsigned though locked REQs take their numbers from it; F2 REQ-012
  "resting turns" contradicts REQ-016/018 tray turns; F3 TYPE-007 window-width class is
  circular with REQ-035 portrait-only), 17 Shoulds (lifecycle/kill/update, interrupted
  drag, language change, Android Auto Backup vs REQ-010, permission definition, min
  Android version, Finnish wording, …), 18 Notes. Orchestrator re-checked F1–F3 against
  the files: all three hold. Every Should carries a build ASSUMPTION.
- Step 3: presented digest + trace + Blockers with proposed dispositions; asked
  "Accept collection v1.1 for TangramNoAds (G1)?" — **awaiting Jami's answer**.
- Found while waiting: **no JDK, Android SDK or Android Studio on this computer**
  (winget is available) — the G3 toolchain proof cannot run until one is installed;
  told Jami so he can install Android Studio in parallel (KICKOFF §0).

## 2026-10-02 06:05 — G1 accepted; toolchain installed; G3 dry run started

**Human (verbatim), two messages in this interaction:**

> Toolchain installed: Android Studio with SDK at C:\Users\Jami\AppData\Local\Android\Sdk. JAVA_HOME and ANDROID_HOME are set as user env vars. JAVA_HOME is Android Studio's bundled JBR, which is Java 25, so pin the Gradle wrapper to 9.1+ (or set up a JDK 21 toolchain if you need an older Gradle). Do a G3 dry run (gradlew assembleDebug testDebugUnitTest) as soon as the skeleton exists rather than waiting for the gate.

> (F1) yes plz lock requirements and continue with all reqs. (F2) OK good plan (F3) OK (F4) OK + rest OK .. YES

**G1 sign-off record:** "Accept collection v1.1 for TangramNoAds (G1)?" — **YES**, approver
**Jami**, **2026-10-02**. Scope as restated above the question: `Requirements/` v1.1 —
REQ-001..051 (47 locked, 4 withdrawn), `features.md` (51 locked, 2 ideas) **and
`req_types.md` v0.2** (TYPE-001, 003–007 with their ASSUMPTION values; TYPE-002 withdrawn)
— review F1 closed by this answer; F2 and F3 accepted under the stated ASSUMPTIONs;
F4–F20 Shoulds accepted under the reviewer's ASSUMPTIONs ("+ rest OK"); F22 accepted
(post-playtest value changes arrive as CHGs). "continue with all reqs" = build every
locked REQ, WO by WO in build-map order, without waiting (KICKOFF step 8).

**What was done (summary):**

- G1 recorded (above); `trace_check.py --freeze` → contract baseline FROZEN, 54 files
  (REQ files, features.md, req_types.md v0.2). `req_review_01.md` triage filled; build-side
  capture actions CA-1…CA-4 listed (Requirements/ untouched). 17 decision rows in
  `decisions.md` (the G1 readings F2–F18 + toolchain pins + app id + inline exceptions).
- **G3 toolchain proof PASSED on Jami's computer**: skeleton (Gradle 9.8.0 wrapper with
  SHA-256 verified against services.gradle.org, AGP 9.4.1, Kotlin 2.4.20, Compose BOM
  2026.09.00, minSdk 26, compile/target 37, app id `io.github.jamisuni.tangram`, hello-world
  Compose activity, en+fi strings, 1 scaffolding test) → `.\gradlew.bat assembleDebug
  testDebugUnitTest` BUILD SUCCESSFUL in 1m 12s, app-debug.apk 9 MB, 1 test / 0 failures.
  Env notes: session predates the user env vars (set per call); cmd needs `.\gradlew.bat`.
- Merged debug manifest already shows the REQ-010 traps: androidx.core injects
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, allowBackup unset → guardrail G-01 + V-01.
- **P2 authored** (G2 = ai+inform): `architecture.md` v0.9 (G-01…G-10, verifiers, 10-module
  component map, ownership rules O-01…O-07, registry: IPuzzleLibrary locked, IProgressStore
  notify), ADR-001…006 in `adr/`, `build-map.md` v1.0 (code homes = Gradle modules, #Language
  row, WO sequence WO-001…009), `design-inputs.md` v0.2 (G1 interpretation layer). Contracts
  defined in code (`contracts/` + kernel value types) — compile standalone, `assembleDebug
  test` green. Fresh-eyes P2 review dispatched (design-reviewer) before locking.
- `workorders/WO-001.md` opened (Full mode). 7 framework-friction entries in `proposals.md`.

## 2026-10-02 06:59 — status question

**Human (verbatim):**

> something happening?

**What was done (summary):**

- Answered: the WO-001 design review (design-reviewer subagent, new context) is still
  running (22 min; report not yet written). Nothing blocked on Jami.
- Since the last entry: P2 review 01 (0 B / 8 S / 6 N) applied and spot-checked
  "forward" → `architecture.md` v1.0 locked (G2, governance row 4 ai+inform),
  `build-map.md` v1.1, ADR-002/005/006 updated, contracts re-documented
  (`PuzzleProgress.restarted()`), kernel value types moved to `kernel.model` (notify),
  `.swdev/guard.json` contract paths + test globs, baseline re-frozen (68 files).
- Two own mistakes caught: a shell rewrite of notify-tier `build-map.md` bypassed the
  guard (drift) and Python wrote CRLF into 11 LF files — both fixed, lesson codified in
  AGENTS.md duties + proposals.md.
- WO-001 design written and revised for the P2 deltas (9 acceptance IDs, golden route,
  manifest/V-01, poseOf); `tasks.md` drafted with TASK-001…007 pending the design review.

## 2026-10-02 07:46 — stop requested (computer shutdown in ~10 min)

**Human (verbatim):**

> plz stop for this run when you get change and let me know when all quiet... I need to shutdown computer about 10mins... can still finish ones those running

**What was done (summary):**

- No new dispatches. Two agents were allowed to finish: TASK-005b (LockSearch, kernel)
  and TASK-T (acceptance tests + held-out slice). Their outcome is recorded below if they
  land before shutdown; otherwise see "Resume" — both are restartable from their briefs.
- WO-001 state at the stop: design reviewed (forward), plan approved (G3, tasks.md v0.5),
  TASK-000/001/002/003a/003b/004/005a/007 **done and re-verified** (91 kernel tests green,
  verifiers V-01/05/06 green, prototype tests ALL PASS after the DA-7 corner fix).
  Remaining: TASK-005b (in flight), TASK-T (in flight), TASK-006 (DropResolver), then code
  review → Test & Verify (held-out) → traceability audit → WO close + checkpoint 1.
- **Resume next session:** read `AGENTS.md` "Current phase", `workorders/WO-001.md` (log +
  ledger) and `tasks.md` statuses. If TASK-005b/TASK-T are not marked done, check
  `kernel/src/main/kotlin/…/kernel/lock/` and `play/src/test/…/acceptance/` +
  `.swdev/heldout/WO-001/` for partial output and re-dispatch from their rows. The
  Playwright venv lived in the session scratchpad and is gone after this session:
  re-create it (decisions.md row "16 — waivers / verification channel") or install
  Playwright globally before the WO-001 close (rule 10 re-run).
- Nothing committed (F3): all of today's work is uncommitted in the working tree.
- **Landed before shutdown:** TASK-T done (visible 31 tests for REQ-019 A1/A3, REQ-020 A1,
  REQ-021 A2, REQ-051 A1/A2; held-out 15 tests for REQ-019 A2/A4, REQ-021 A1 in
  `.swdev/heldout/WO-001/`) and TASK-005b done (116 kernel tests green, round trip on all 13
  puzzles, margin 1.26e-3). All agents finished — **all quiet**. Next session starts with
  TASK-006 (`play.DropResolver`, must pass the visible acceptance tests unedited).

## 2026-10-02 18:21 — resume after the shutdown

**Human (verbatim):**

> plz continue

**What was done (summary):**

- Session-start checks: SWDev v0.15 = AGENTS.md (no drift); the shell now sees JAVA_HOME /
  ANDROID_HOME (new process); the session scratchpad (Playwright venv) survived;
  `:kernel:test` 116 green.
- WO-001 continues at TASK-006 (`play.DropResolver`), then code review, Test & Verify
  (held-out), traceability audit, close + checkpoint 1.
- TASK-006 done (DropResolver; 31 visible acceptance tests pass unedited). Code review
  (fresh): 0 B / 2 S / 7 N, no product-code defect → V-06/V-05 fail-closed (17 self-tests),
  two decision-asserting tests re-tagged (DA-1, DA-4). Test & Verify: held-out first run
  15/15 PASS, 46 play + 116 kernel tests, rule-10 ALL PASS. Traceability audit GREEN for the
  WO scope (orchestrator corrected the auditor's counts: 43 out-of-scope REQs).
- **WO-001 closed** (`workorders/WO-001.md`: DoD 14/14, metrics, 2 notify deltas, 0 waivers,
  0 held-out failures). STATUS.md, build-map.md status, tasks.md, AGENTS.md phase updated.
  2 corrected-check entries filed in proposals.md.
- **Checkpoint 1** surfaced to Jami (REQs satisfied, decisions.md, deltas, build state, VC
  snapshot prompt). Continued without waiting: WO-002 #Content opened, Design Author running.

## 2026-10-02 ~19:45 — WO-002 closed (autonomous, after "plz continue")

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- WO-002 #Content: design (+ review: 0 B / 4 S / 7 N → revised → forward), plan (+ review:
  2 B / 6 S / 5 N → tasks.md v0.7 → forward), TASK-008 (packaging), TASK-009 (validator verdicts
  in the golden), TASK-010 (exact parser + library behind the locked IPuzzleLibrary), TASK-T2
  (independent tests, 4 held-out). Code review forward (0 B / 1 S / 6 N). Test & Verify: held-out
  15/15 on first execution (after a test-side import fix — corrected check). Trace GREEN for the
  WO scope (orchestrator re-count: 39 FAIL = 2 carried + 37 later WOs).
- Lesson codified: "Test tokens are coverage claims" (AGENTS.md duty + token audit). Two
  corrected-check entries in proposals.md. Traceability auditor promoted to sonnet.
- Checkpoint 2 surfaced to Jami; next WO-003 (first playable APK).

## 2026-10-02 20:41 — WO-003 mid-point: logic done, acceptance move-in (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- WO-003 opened (first playable APK): design (+ review → revised → forward), plan tasks.md v0.9
  (+ review → forward, G3 passed), API 26 launch waiver recorded (exit: Jami installs the image).
- TASK-T3 (independent tests): 20 visible IDs staged in `.swdev/staged/WO-003/`, 12 held out.
- TASK-011 build step (Compose, androidTest; channel proven on API 37), TASK-012 kernel
  additions (123 → 127 kernel tests), TASK-013 PlayLayout, TASK-014 PathData/timeline/confetti,
  TASK-015a/b PlaySession, TASK-016 strings (en+fi), TASK-017 GestureMachine + HitTest: 100 play tests green.
- Staged JVM acceptance tests moved in: kernel 4/4 green; play stopped at one adapter compile
  error — `badgeRect` is nullable by the plan but the design text said non-null (unlogged plan
  amendment). DA-32 logged, design text amended, TASK-T3 resumed for a corrected check.
- Next: green move-in → CR-1 (fresh code review of 012–017) → TASK-018a/b (Compose, device
  tests) → TASK-019 (app shell) → TASK-020 → review, Test & Verify, trace, close, checkpoint 3.

## 2026-10-02 ~21:20 — WO-003: CR-1 forward, Compose drawing started (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- JVM acceptance move-in finished: TASK-T3 corrected check (adapter-only, DA-32/34), TASK-017b
  `PieceDrawing.scale` (DA-33); 16/16 moved-in acceptance tests green; token audit clean.
- CR-1 (fresh code review of TASK-012…017b): revise 0 B / 5 S / 8 N → fixes by the owning
  implementers → spot-check **forward**. DA-35 (badge/touch end at the solve), DA-36 (top-most
  piece wins). Deferred: F4 validator half → TASK-020; N2 + size-change cancel → TASK-018b.
- Held-out adapter pre-audit blocked by the permission check (read of `.swdev/heldout/` denied
  to the resumed author); not routed around — moved into the Test & Verify brief (tasks.md).
- Re-run: kernel 127, content 46, play 127, 0 failures. TASK-018a (Compose drawing) dispatched.

## 2026-10-02 ~21:45 — pause requested (resume tomorrow)

**Human (verbatim):** time to pause again work in 10mins... after current jobs run to finish.. and store info where going on to continue tomorrow

**What was done (summary):**

- Since the last entry: TASK-018a Compose drawing done after a rejected haiku attempt and a sonnet redo
  (staffing row in decisions.md). DA-37 restored the tray-cell dashed outline: REQ-012 and REQ-043 A2
  require it, and the orchestrator had misread REQ-013. The correction is logged next to the staffing row.
- Verified by the orchestrator: kernel 127, play 130 tests, 0 failures; V-05 and V-06 PASS.
- TASK-018b (PlayArea, pointer adapter, frame loop, tags, BoardTransform; sonnet) was running at the
  pause and was asked to hand back within ~8 min. Its final state is recorded in tasks.md.
- No new jobs started. The API 37 emulator was booted for 018b and is shut down at the pause.
- STATUS.md has a "▶ Resume here" list with the exact next steps; tasks.md is current.
- TASK-018b hand-back (at the pause): `PlayArea.kt` written, play JVM 130 green. Open items:
  - the espresso-core 3.5.0 → 3.7.0 pin needed for device tests on API 37 (a build file);
  - staged device-test pixel failures (BoardPixels 7/26, SolvedPicture 35/39) in a scratch copy, for TASK-018a;
  - 3 staged androidTest adapter fixes for TASK-T3;
  - a frame-loop deviation to rule on.

  All are in `tasks.md` (TASK-018b row) and `STATUS.md` "▶ Resume here". The emulator is stopped and no jobs are running.

## 2026-10-03 — resume WO-003

**Human (verbatim):** plz continue

**What was done (summary):** resumed from STATUS.md "▶ Resume here": the espresso pin, the staged
device-test adapter fixes, the frame-loop ruling, then the play device run on the emulator (details in the
entries below and in the tasks.md TASK-018b row).

## 2026-10-03 ~08:00 — WO-003: device tests in, app shell and tools done (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- TASK-018b redone with queued pointer events dispatched in the frame loop (DA-38: the first cut
  stamped taps with stale/0 time, so shakes and glides could vanish). Espresso-core 3.7.0 pinned
  for device tests on API 37 (DA-39). 6 adapter device tests green in the real repo.
- Play device move-in: 65/91 → drawing fix (picture scaled twice) and silhouette union in units →
  76/91. 15 contested fixture samples upheld after the orchestrator re-derived them with the reference
  geometry (DA-40 outline corners, DA-41 mini-2 white frame) → TASK-T3 corrected check → 89/91.
  2 genuine req023_A2 interior points (cat, sailboat) are back with the drawing implementer.
- TASK-019 app shell (sonnet) done. The release unit-test variant was enabled (build step), so the
  release-stub proof runs. App device tests 4/4: the first puzzle is solved by real touch and shows its picture.
- TASK-020 done: the APK puzzle check, V-07, the V13 path-data check in the validator, the rule-10 chain ALL PASS,
  and the API 26 waiver stands. Orchestrator re-verified the cheap checks.
- Owner note queued for checkpoint 3: shapes-mini-2's white frame vanishes on the white board.

## 2026-10-03 ~09:15 — WO-003: reviews forward, held-out first run (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- Last 2 SolvedPicture failures: picture-internal edges drawn without anti-aliasing by the hardware canvas,
  fixed by rendering the picture once in software (DA-42). Play device 91/91, app 4/4.
- WO code review (TASK-018a…020 + build steps): revise 0 B / 4 S / 9 N. Fixes:
  - F1 clock reseed after recreation;
  - F2 spy-intent release proof (shown to fail);
  - F3 same-composition cancel tests;
  - N2, N6, N7, N8.

  Re-check forward. DA-43 (API 26 waiver row), DA-44. Carried notes go to WO-004/005/006 and the playtest.
- Re-run: JVM 307, device 98, 0 failures; rule-10 chain ALL PASS. The first-launch screenshot is kept in
  `reviews/screens/` (landscape launch ends in portrait, no crash).
- Test & Verify (sonnet, staffing row): visible baseline exact; held-out first run 11/12. The one failure is
  a test-scenario defect: it solves the mini, then drags, which DA-35 forbids. Ruling DA-45: no escaped
  product defect. Six adapter-class corrected checks. TASK-T3 is correcting the scenario.
- Workorder WO-003 ledger and log brought up to date. Next: re-run, the traceability audit, close, checkpoint 3.

## 2026-10-03 ~10:00 — WO-003 closed; checkpoint 3 (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- Held-out REQ-014 A1 scenario corrected (DA-45) → held-out 12/12. Play JVM 146, device 102.
- Traceability audit (sonnet): 1 gap, REQ-002 A2 with no covering test under `play` (the design placed it in `app`
  only) → TASK-T3 `NoModeAcceptanceTest` → trace-check re-run: 26 FAIL = exactly the later-WO set, 21/47 pass,
  0 in-scope gaps, no drift. KDoc REQ references added to 3 plumbing files.
- build-map v1.2 (status, notify tier). One slip: a `sed` write to its header, redone with the Edit tool.
- AGENTS.md: 2 lessons codified (seams and test adapters; implementer staffing and hand-back diffs). 4 proposals.md
  entries. workorders/WO-003.md closed (DoD 11/11 with waiver DA-43; metrics). STATUS.md has "▶ Next".
- **WO-003 closed.** Checkpoint 3 surfaced to Jami. WO-004 (#Browsing + store) next, without waiting.

## 2026-10-03 ~12:00 — WO-004 design forward, plan written (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- WO-004 opened (#Browsing + store). The design (sonnet) went through review: recirculate 0 B / 8 S / 9 N
  → rev 1 → spot-check forward with E1–E6 → rev 2.
  - E1: the reviewer emulated the kernel arithmetic and found that coprime denominators in a save could
    overflow, so the v1 bound is now dyadic (DA-63).
  - Followed the accepted decisions F4: tray turns are saved even for New puzzles.
  - DA-58 withdrawn, so `IProgressStore` semantics are unchanged.
- DA-46…66 logged. DA-64 is a governance row-13 guardrail conflict (overwrite only if both the move-aside
  and the copy fail), ai+inform, for checkpoint 4. The v1 format is ai+inform (row 11).
- tasks.md v1.1: TASK-T4, TASK-021…029 + the CR-2 checkpoint. Plan review dispatched.

## 2026-10-03 ~15:00 — WO-004: logic done, v1 save format frozen (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- Plan v1.2 spot-check forward → G3 passed. TASK-T4a (test author) wrote the frozen v1 fixtures. TASK-021
  added the store and browse modules. TASK-022 added the kernel functions plus the DA-63 safety proof (worst case
  < 2^57). TASK-023 (store) ∥ TASK-024 (play hooks) ∥ TASK-025 (browse logic) all green, with the frozen
  test passed unedited. TASK-T4 staged its tests (6 held-out IDs, no overlap). MOVE-JVM was green on the first
  run (store 63, browse 44).
- CR-2: revise 0 B / 2 S / 8 N → fixes:
  - atomic-only rename;
  - explicit serial names;
  - never prune newer-version files;
  - one never-run conditional test exposed and fixed;
  - two more format meanings pinned before the lock.

  Spot-check forward: "v1 format fit to freeze: yes".
- **LOCK-V1:** the v1 fixtures + frozen test are locked tier; baseline re-frozen; any later change is a migration
  hard-stop.
- Now: TASK-026 (play: solved bar slot, thumbnails) ∥ TASK-027 (browse: top bar, grid, solved bar, strings).

## 2026-10-03 ~18:00 — WO-004: built and green, closing reviews (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- TASK-026 (play: solved bar slot, thumbnails) ∥ TASK-027 (browse UI: top bar, long press, grid, solved bar,
  17 strings en+fi). The independent browse device test needed imports-only and setContent-once corrected checks;
  the import class recurred, so it is codified in AGENTS.md.
- TASK-028 (app wiring: SessionHost never throws, onPause interrupt → persist, onStop sync, Back closes grid,
  DA-23 retired) + TASK-028b (dead build stanza removed).
- MOVE-APP: two failures diagnosed by the implementer with logging and pixel dumps. Both were fixture defects:
  the board was inferred from pixels and swallowed the Restart pill; a probe sat on a picture edge.
  DA-70 ruling (map via BoardTransform) → TASK-T4 fixed its visible and held-out kits → 15/15.
- TASK-029: JVM 461 + device 140 all green; verifiers PASS; no drift. The WO close code review is running.

## 2026-10-03 ~19:30 — WO-004: reviews closed, held-out 6/6 (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- WO code review revise 0 B / 4 S / 7 N, then fixes, then the spot-check was forward with O1, then O1 closed:
  - F1: the fixed top-left Restart pill could overlap the silhouette and restart a puzzle silently. DA-71 adds a
    free-corner slot in `play`, measured by size, with a sweep over 13 puzzles × 4 sizes × 3 widths. The first cut
    used a 48 dp square; the orchestrator caught that the pill is a wide text pill.
  - F2: `state` and `isDragging` now have their own snapshot state, so no recomposition per frame.
  - F3: app lifecycle tests.
  - F4: the SessionHost fallback is now null, never another puzzle's session.
  - N1/N2/N4/N5/N7 fixed.
  - O1: a device test with fail-then-pass shows the hidden pill takes no touch in Solved.
- Full re-run: JVM 468–469, device 147–148, all green; V-01/05/06/07 PASS.
- Test & Verify (sonnet): baseline exact. **Held-out first run 6/6, 0 escaped defects, no corrections** (the first
  clean held-out first run in this project).
- Traceability audit (sonnet) is running; then the WO-004 close and checkpoint 4.

## 2026-10-03 ~20:00 — WO-004 closed; checkpoint 4 (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- Traceability audit GREEN: 26/47 locked REQs pass, 21 later-WO, 0 gaps in the WO-001…004 scope, no drift; the
  frozen v1 files match the baseline.
- **WO-004 closed:**
  - `workorders/WO-004.md` DoD 11/11; contract deltas (LOCK-V1, `IProgressStore` behavioural notes DA-47/64,
    build-map v1.3); metrics.
  - STATUS.md "▶ Next (after WO-004)", AGENTS.md phase line, tasks.md v1.2/1.3.
- proposals.md: the WO-004 close entry (what worked: held-out 6/6 first run; the friction left).
- Checkpoint 4 surfaced to Jami. WO-005 (#DevTools) next, without waiting.

## 2026-10-03 — pause requested (land the plane; continue later)

**Human (verbatim):** thats good session.. hopefully.. but its time to land plane again for moment, as I need close computer.. plz finish current sessions (no rush). Save progress. Let me know when can shutdown... we continue bit later when Im back home

**What was done (summary):**

- WO-004 closed earlier this session: checkpoint 4 surfaced (v1 save format frozen, DA-46…71).
- WO-005 (#DevTools) opened:
  - the design is written (`designs/WO-005-design.md`, DA-72…84: a `DebugAids` source-set pair, a validated
    `solveByAid`, the DEV pill in the DA-71 corner slot, V-04 with a real debug-APK positive control);
  - the design review was running at the pause and was allowed to finish; nothing new was started.
- STATUS.md "▶ Resume here (WO-005)" written. The emulator is stopped at the pause.
- Design review hand-back (at the pause): `reviews/WO-005-design-review.md` = **recirculate 1 B / 5 S / 7 N**. The DEV pill must not
  change the board layout: F1 measured animals-cat at 360×640 losing 12 % of its board in debug. Recorded in WO-005 and in STATUS
  "▶ Resume here"; not yet sent back. The emulator is stopped. No jobs running.

## 2026-10-03 (later) — resume WO-005

**Human (verbatim):** plz continue

**What was done (summary):** resumed from STATUS.md "▶ Resume here (WO-005)". The WO-005 design review (recirculate 1 B / 5 S / 7 N) goes back to a design author with orchestrator rulings, then a spot-check, the plan, G3 and the build (entries below).

## 2026-10-03 ~21:30 — WO-005 build under way (autonomous)

**Human (verbatim):** (no new input since "plz continue")

**What was done (summary):**

- WO-005 design rev 1 → spot-check forward (E1–E4) → rev 2. DA-72…89 logged. Governance row 12's capture-side step:
  CA-5 filed (REQ-046), CA-6 backfills the WO-003/004 readings that had been missed (proposals.md entry).
- Plan v1.5 after plan review (1 B / 8 S / 6 N; DA-89 exact build-file exemptions); spot-check forward → G3 passed.
- TASK-030 (devtools module) ✔. TASK-035 (V-04: dex + resource scan, canary, positive control; 46 verifier tests;
  real release APK PASS) ✔. TASK-032 devtools core: the haiku first cut was rejected at the diff read (frozen-seam
  type drift, thin tests); TASK-030b added test-scope content; the sonnet rework ✔.
- An orchestrator slip was found and fixed: a heredoc script had written a real newline + NUL byte into tasks.md
  (the TASK-035 row). Repaired; all project docs scanned clean.
- Running: TASK-031a (play slots + sweep + parity, emulator), TASK-033 (devtools UI), TASK-T5 (tests).

## 2026-10-03 (night) — closing shop for tonight

**Human (verbatim):** time to start closing shop for tonight. Not urgently rush but after current ones dont start new jobs before I tell continue

**What was done (summary):**

- WO-005 #DevTools build is done:
  - TASK-030/030b/030c: the devtools module plus test-scope content.
  - TASK-031a: the secondary slot and overlay, with board parity proven.
  - TASK-031b: `solveByAid`. Trap tests shown to fail with the wrong order; drag interrupted per the design.
  - TASK-032: core, reworked after the haiku cut was rejected.
  - TASK-033: the UI.
  - TASK-034: the `DebugAids` pair. G-04 grep shows only the 2 DA-89 lines; V-04 PASS on the release APK.
  - TASK-035: V-04 (46 verifier tests).
- TASK-T5: tests staged; held-out A1/A3.
- MOVE-JVM is done. One rename, two rulings. The A4 cache proof passed both ways (re-run on edit, fail on a devtools word).
- At close: the CR-3 release-surface code review was running and allowed to finish; no new jobs started. The emulator was stopped.
- Next when Jami says continue: CR-3 findings → MOVE-DEV → TASK-036 checks (V-04 release exit 0 + positive control on a
  fresh debug APK) → Test & Verify (held-out A1/A3) → trace audit → WO-005 close → checkpoint 5.
- CR-3 hand-back (after the close call): **forward, 0 B / 0 S / 8 N**. Release build clean of DevTools, proven both ways:
  V-04 PASS on a fresh release APK; `--positive-control` finds all four markers on a fresh debug APK. Recorded in tasks.md,
  WO-005 and STATUS. No jobs running.

## 2026-10-04 — resume WO-005; API 26 image installed

**Human (verbatim):** plz continue (Android 26 emulator image is there now)

**What was done (summary):** resumed from STATUS.md "▶ Resume here (WO-005)". The CR-3 nits are triaged, then MOVE-DEV, the TASK-036 checks,
**the API 26 runs (exiting waiver DA-43)**, Test & Verify, the trace audit, the close and checkpoint 5 (entries below).

- **CR-3 nits triaged** (DA-91). F1 → TASK-030d: `app/src/debug/**` is now a declared devtools test input, 24 tests green, V-06 PASS. F2 → TASK-035b: V-04 also denies every devtools string value of 6+ characters, minus words the app itself uses (DA-90). The verifier suite has 50 tests, all OK, and the real release APK passes V-04. F8a → TASK-033b (KDoc, after MOVE-DEV). F6 was declined.
- **API 26 (waiver DA-43).** There is no avdmanager here, so I made the `Phone_API_26` AVD by hand from the Medium Phone config (Android 8.0 google_apis x86_64, 1080×2400 at 420 dpi) and booted it headless.
  - **Release launch OK.** The release APK, signed with a throwaway debug key (scratchpad copy only), installed and started: Status ok in 946 ms, `MainActivity` resumed, 0 FATAL, no permissions. The screenshot is in `reviews/screens/WO-005-release-launch-api26.png`.
  - **Device suites on API 26.** browse 25/25, app 25/25, devtools 5/5. **play is 94/112**: 18 edge-of-shape pixel probes fail (BoardPixels A2 ×10, SolvedPicture A1 ×8, all at the same sample points), while the centre and inner-edge probes pass. This is either a real drawing difference at minSdk 26 or the test's screenshot mapping. A sonnet diagnosis agent is on it, with pixel evidence required before any fix.
- **Bookkeeping fix.** Yesterday WO-005's MOVE-JVM result was written into WO-004's MOVE-JVM row. WO-004's own result is restored from the session transcript, and the WO-005 row now holds its result.
- **API 26 diagnosis back: a product defect (DA-92).** The test harness is exact. On Android 8, the renderer blurs any path drawn under the canvas dp scale, leaving a ~5 px soft edge. Drawing the silhouette from a px path brought `play` to **112/112 on API 26**. Android 8 players would still see soft piece edges, so TASK-037 moves every draw that blurs to px and adds a device guard test that must fail without the fix. The API 37 re-run happens at MOVE-DEV.
- **TASK-037 done.** Every path in `play` is drawn in px. That covers pieces, edges, dashed outlines, the preview, badge glyphs, the solved picture and the browse thumbnails, which were blurred on API 26 too. The guard test was proven to fail without the fix. Diff read by the orchestrator. TASK-033b (KDoc) is done.
- **MOVE-DEV done, on both channels.** Seven staged device tests moved in unedited. All four suites are green on API 26 (play 115, browse 25, app 30, devtools 10) and on API 37 (the same 180).
- **DA-43 exited (DA-93).** Today's release APK (V-04 PASS) launches on Android 8.0: Status ok in 738 ms, 0 FATAL, no permissions, crisp first screen. AGENTS.md now requires both device channels plus the API 26 release launch at every WO close.
- **TASK-036 done:**
  - V-04 PASS on the release APK, and the positive control finds all 4 markers on a fresh debug APK (both lines are in WO-005);
  - V-01/05/06/07 PASS;
  - JVM 538 + 2, 0 failures;
  - token grep clean;
  - trace-check's in-scope gap is only the held-out A1/A3.
- **Dispatched in parallel:**
  - Test & Verify (sonnet): visible baseline, then the held-out first run on API 37 and API 26;
  - a fresh code review of the changes made after CR-3, mainly the DA-92 drawing rewrite (read-only, so it doesn't collide with T&V's builds).
- **Fresh code review of the post-CR-3 changes (CR-4): forward, 0 B / 0 S / 7 N** (DA-94). DA-92 is confirmed correct. TASK-035c (F6) is done: V-04 has no silent fallback and prints a note line, with 2 synthetic tests. The verifier suite has 52 tests, all OK. The real release APK exits 0, with 24 values denied and 1 excluded; DA-90 said 25, which is corrected. TASK-037b, the guard-test nits, waits for Test & Verify to free the emulator.
- **Test & Verify first run: HOLD** (DA-95). The held-out JVM tests passed 9/9 and everything passed on API 26. On API 37 one held-out check failed: an absolute "≤ 3 early picture pixels" bound saw 4 anti-aliased edge pixels, while the timeline evidence shows the product is correct. The ruling is a fixture threshold. TASK-T5c, the test author's corrected check, moves to a relative bound (≤ 2 % of the settled picture) and folds in T&V's adapter fix for a held-out test that did not compile. Both copies are byte-identical. TASK-037b (the guard-test nits) is done, with the guard green on API 37. Two lessons go to `proposals.md`: treat the minSdk floor as a device channel from the start, and compile the held-out slice before its first run.
- **Running now:** the T&V re-run (JVM, then all four device suites on API 37 and on API 26) ∥ the traceability audit (read-only).
- **CR-4 follow-ups done:** TASK-035c (verifiers 52 OK) and TASK-037b (the guard waits out the glide; token colour). The test author's corrected check is TASK-T5c (DA-95).
- **T&V re-run PASS** on JVM, API 37 and API 26, with held-out A1 and A3 green on every channel and 0 escaped product defects. **Trace audit GREEN** for WO scope: no orphans, the 72 baseline hashes are equal, and the G-04 grep finds the 2 allowed lines.
- **WO-005 CLOSED.** build-map v1.4 marks #DevTools built and V-04 live, with two device channels (the delta was logged and the baseline re-frozen by the guard). The workorder ledger, contract deltas, DoD (11/11) and metrics are filled in. tasks.md v1.6, STATUS (resume = WO-006) and the AGENTS.md phase line are updated. **Checkpoint 5 surfaced to Jami.**
- Totals at close:
  - JVM 547 (+2 release);
  - device 191 on API 37 and 191 on API 26;
  - verifiers 52.
- Next: WO-006 #Layout, #Language, #Promise, in Full mode, opened right away per the G1 instruction "continue with all reqs".
- **WO-006 opened** (#Layout, #Language, #Promise, Full mode): `workorders/WO-006.md` was written, and the design author (sonnet, fresh) was dispatched with 13 in-scope acceptance IDs, the carried-in REQ-037 `dev-*` exemption, both device channels, and the hard-stop on any v1 save-format change for rotation.
- **WO-006 design rev 0 in.** Most of the WO is evidence for behaviour that already exists. It has two code findings: a REQ-037 tray-cell breach on narrow tablets and a title-language mismatch. The highest risk is the display-override harness. A fresh design review was dispatched.
- **WO-006 design review: recirculate, 0 B / 8 S / 9 N.**
  - Rulings: F4 (the REQ-013 ↔ REQ-037 conflict in short split-screen windows) keeps today's behaviour as the interim and becomes a two-option CHG for Jami. F5 (the *Swedish, Finnish* language list) gets a test on today's code first, no build change, and an owner option. F7 (the no-clip check) applies at font scale 1.0 only. The rest are as the reviewer required.
  - Design rev 1 is in progress.
- **WO-006 design rev 1 in.** All 8 S and 9 N are applied. It adds `tools/device_reset.py` (crash-safe device hygiene before and after every suite), a fail-loud `DisplayRule`, a harness scaffolding gate, the interim rules for F4 and F5, V-08 building its own APK, and DA-96…108 with CA-7 and CA-8. The spot-check re-review was dispatched to the original reviewer.
- **WO-006 design is forward.** Rev 1 passed its spot-check with edits E1–E6, now applied as rev 2. Logged DA-96…108 and CA-7/CA-8; CA-7(b) and CA-8(a) are owner decisions for checkpoint 6. tasks.md v1.7 is the WO-006 plan, written by the orchestrator as planner under a logged exception. A fresh plan review was dispatched.
- **WO-006 plan review: recirculate, 1 B / 10 S / 6 N.** The blocker: the word-list equality check read a device file that was not in the tree at MOVE-JVM6. tasks.md v1.8 applies all 17 findings:
  - strict build order;
  - named smoke and probe checks that can fail;
  - V-08's `class_defs` reader and pre-runs;
  - a per-kind re-run proof;
  - gate, inset and time evidence;
  - contingent triggers;
  - C1–C11 listed;
  - a CLOSE6 row;
  - failure routing.
  The spot-check was dispatched to the same reviewer.
- **WO-006 G3 passed** (row 7, ai). The plan spot-check was forward with E1–E5, all applied. Dispatched TASK-040 (build setup: `open MainActivity`, glob-only test inputs, no "devtools"; it is the only Gradle user). TASK-044 (V-08: a new `class_defs` reader, self-test) and TASK-042 (`device_reset.py`, self-test) run in parallel, code only.
- **TASK-040 done.** The orchestrator read the diff. `ReleaseSeparationTest` re-ran after the edit, and the G-04 grep finds the 2 allowed lines.
- **TASK-044 (V-08) done.** It has a new fail-closed `class_defs` reader, and the verifier suite now has 76 tests, all OK. Both real-APK pre-runs PASS: the own build, and `--apk` on it.
- **TASK-042: code and self-test done** (33 OK). The orchestrator's read-only probe of the clean API 37 AVD found that **auto-rotate is on (`accelerometer_rotation` = 1)**, while the script assumed 0, so it would have "reset" a clean device into a different state. Navigation mode is gestural (2). Both get fixed in 042's live round, after the API 26 baseline is read.
- **Dispatched:** TASK-041 (the locale and font seam, with a smoke test on API 37 and then API 26, and the API 26 baseline readings). TASK-T6c, the JVM tests with the tray sweep first, is still running.
- **TASK-041 done:** the locale and font seam works on API 37 and API 26. `createConfigurationContext` alone is enough on API 26. The release stays clean (V-04 and V-08 PASS).
  - Finding: Android 14+ scales a 20 sp text non-linearly (×1.16 at font 1.3), so tests of text growth use a small-sp node.
- **PRE-V08 PASS** (`--expect-debug`).
- **The API 26 baselines differ from the script's assumptions:** `font_scale` and `user_rotation` are unset (null), and auto-rotate is on. TASK-042's live round fixes the constants, then runs induced-leak, `cmd overlay` and `wm` probes on both channels.
- **T6c done, MOVE-JVM6 done.** The sweep confirms the REQ-037 breach, only on 600–602 dp tablets: square cell 47.78 dp. The re-run proof PASSES for all 5 input kinds, a forbidden word fails the scan, and the 6 reverts are hash-equal. V-05/06/07 PASS.
- **TASK-042 done, live on both channels.** Induced leaks are caught and reset, the `cmd overlay` probe works, and the tablet override is 1920x1200 @ 240 with no clamp. Findings:
  - the API 26 image cannot switch Wi-Fi (`svc wifi` killed), so airplane mode there cuts data only;
  - the nav-mode switch is async, so the script polls for it.
- **Dispatched:**
  - TASK-043 (tablet gap 14 → 12 dp);
  - TASK-T6a (visible device tests, briefed with today's measured facts);
  - TASK-047 (owner Finnish list, with a count check).
- **Queued:** TASK-040b (the device word list as a declared input).
- Process slip: the orchestrator put a `git status` into a compound command. It was denied, and the command was re-run without it (no git used).
- **TASK-043 done:** tablet gap 12 dp. The square cell at 600 dp is 49.3, the smallest cell from 560 dp up is 48.8, and play JVM is 192/192.
- **TASK-040b deferred to MOVE-DEV6.** The new input glob correctly made the scan red, because the file is not yet in the tree. It was reverted, and the tree is green.
- **TASK-047 done:** 27 Finnish strings and the word stems are listed for Jami, with the count verified. TASK-T6a (visible device tests) is still running.
- **TASK-T6a done:** the visible device tests and kit are staged and compile, with exactly 8 tokens. Dispatched TASK-T6b (held-out, including `tools/compare_kits.py`) in parallel with CR-5 (the fresh code review of 040–044, the moved-in JVM tests and the staged kit's restore paths; read-only).
- **TASK-T6b done:** the held-out slice is compiled, and the 8 visible / 5 held-out tokens are disjoint (checked by ID only). `compare_kits.py` shows all 9 kit copies equal. CR-5 (the code review) is still running; then MOVE-DEV6 on both channels.
- **CR-5: revise (narrow), 0 B / 4 S / 17 N** (DA-109). The release build is clean of the debug seam. S1 found that the touch-target walk measured Compose's enlarged touch bounds, which are always ≥ 48 dp, so it was blind. It now measures layout bounds, with a 30 dp negative control. Other fixes in progress:
  - S2: any permission element in release fails V-08;
  - S3: inflected promise words added;
  - S4: the nav restore moved inside the `try`;
  - nits sent to their owners (test author, 044b, 042b; 040/041 after the test author's builds).
  N11 (the design's stale rotation baseline) is written back by the orchestrator.
- **MOVE-DEV6 started.**
  - All CR-5 fixes are done.
  - V-08 PASSES on fresh release and debug APKs.
  - The T6a device tests, the equality test and TASK-040b's glob are in, with the re-run proof for the device word copy.
  - **G-DISPLAY gate on API 37: 8/9.** The display override, mid-process density, nav mode, airplane mode, locale, font and the 30 dp negative control all work, and the device is clean afterwards.
  - The rotation case failed because it rotated a portrait-locked phone window (F3). That is a fixture defect, routed to the test author: use a tablet display first.
- **Gate run 2: 10/10 on API 37.**
- **API 37 visible run:** play 115, browse 25 and devtools 13 all pass; app is 69/75, and `--check` was clean.
  - 2 DA-103 no-clip failures: Finnish Restart pill, state and counter "overflow"; the counter is suspicious.
  - 4 failures from "the ST2 miniature is not in the tray", in the play-through and airplane tests.
  The test author is diagnosing both groups on the device and will classify each as product (→ L-3) or fixture (→ corrected check).
- **The 6 API 37 app failures are fixtures, not the product** (DA-110), each with device evidence:
  - The no-clip check read `hasVisualOverflow`, which is true for every wrap-content text. Real line widths and screenshots show no Finnish text cut at 360 dp.
  - The play-through misread the solving drop, because the solved picture replaces the piece colours. A new `solveByTouch` adapter handles it.
  The affected classes are green. The full app suite is re-running on API 37.
- **API 37 complete for MOVE-DEV6:** play 115, browse 25, app 75 and devtools 13 are all green, with every `--check` clean. Recorded:
  - the language lists: no title/button mismatch, so TASK-046 is not triggered;
  - the gesture insets: +7.6 dp, under the trigger, so TASK-045 is not triggered;
  - the tablet system UI under the override: no taskbar, the DA-96 limit.
- **API 26:** the emulator came up on port 5556. The first gate run was void (wrong serial, caught by the reset script, exit 2). The valid re-run is **8/10**: the tablet rotation does not turn, and the density change mid-process leaves the scenario stuck. The test author is diagnosing whether this is a harness, product (rotation on Android 8) or gate-item-2 problem, which would mean the fallback on API 26.
- **MOVE-DEV6 done:** both channels pass 228/228 (API 37 and API 26), every `--check` is clean, and TASK-045/046 were not triggered. DA-111 covers two API 26 harness fixes. The launcher does not rotate on API 26, but the app does, so **REQ-036 A2 holds on Android 8**. The kit now waits for the home screen to settle after display changes below API 30.
- **TASK-048 done (one build at a time):**
  - V-01, V-04, V-08 PASS on release;
  - API 26 release launch: Status ok in 737 ms, 0 app crashes (the logcat FATALs were the launcher's), no permissions at all;
  - V-04 positive control PASS, V-08 `--expect-debug` PASS;
  - V-05/06/07 PASS; 79 verifier tests OK;
  - JVM 567 + 2;
  - trace-check: the in-scope gap is only the 5 held-out IDs.
- Process slip: a Python string literal turned `app\build\…` into control bytes in the WO-006 evidence line. The bytes were repaired, all docs were scanned clean, and an AGENTS.md rule was added (3rd occurrence).
- **Test & Verify (sonnet) dispatched:** the visible baseline on both channels, then the held-out first run (5 IDs) on API 26 and then API 37.
- **WO-006 T&V PASS** (DA-112). The held-out first run is 5/5 IDs (11/11 cases) on both API 37 and API 26, with 0 corrections and 0 escaped defects, the baselines exact on both channels, and every check clean.
  - N1 found that the "+2 release" JVM results in the WO-005/WO-006 records were stale files from 2026-10-03, because `app` has no release unit-test task. The records are corrected, and no coverage is lost.
  - The traceability audit (sonnet) is dispatched.
- **WO-006 trace audit GREEN.** 13/13 IDs and 13 tokens; no orphans; baseline 72/72 equal; the G-04 grep finds the 2 allowed lines.
- **WO-006 CLOSED (CLOSE6).**
  - build-map v1.5: #Layout, #Language and #Promise built; V-08 live; carried parts C1–C11 written into the WO-006…009 rows. The guard logged the delta and re-froze the baseline.
  - design-inputs 0.3: the tablet gap departure.
  - AGENTS.md: V-08 at every close, device hygiene and the API 26 limits; phase line → WO-007.
  - Workorder: ledger, deltas, DoD 11/11, metrics. tasks.md v1.9. STATUS resumes at WO-007.
  - **Checkpoint 6 surfaced to Jami**, with 5 owner decisions.
- Totals at close:
  - JVM 567;
  - device 239 per channel (API 37 and API 26, including 11 held-out cases);
  - verifiers 79.
- **Next: WO-007 #Settings**, opened right away under the G1 instruction.
- **WO-007 opened** (#Settings, Full mode): `workorders/WO-007.md` written, and the design author (sonnet) dispatched. The key constraints:
  - no permission at all, so haptics go through `performHapticFeedback` and sounds are synthesised;
  - the frozen v1 format, so the sound setting needs another home or a designed migration (row 11);
  - the locked REQ-009 and REQ-049 texts are used verbatim and exempted by key from the promise scan;
  - the six WO-006 carries are re-verified with the same tokens.
- **WO-007 design rev 0 in.** The sound setting reuses the existing v1 `settings.soundOn`, so there is no format change. Also: a new `settings` module, an overlay screen, a 48 dp ⚙, sound synthesised with `AudioTrack`, permission-free haptics, a single gate plus a debug probe for REQ-033, and a two-step reset. A fresh design review was dispatched.
- **WO-007 design review: recirculate, 1 B / 4 S / 8 N.**
  - The blocker: Compose's default system click sound and the long-press vibration bypass the sound gate. With sound off, phones would still click and buzz while every check passed. Ruling: suppress the platform defaults always, with one root lever, and prove it on device; REQ-033 is not narrowed.
  - F3: after a reset, stay on the current puzzle (REQ-032 Statement, F25).
  - F4: keep the `SessionHost` signature, because a held-out test depends on it.
  - Design rev 1 is in progress.
- **WO-007 design rev 1 in.** One root lever, chosen from the bytecode, silences the platform click sound and the long-press haptic always. Also: the call-path scan, stay on the current puzzle after a reset, the held-out test untouched, the `AudioTrack` contract pinned. The spot-check went to the same reviewer.
- **WO-007 design forward.** Rev 2 after the spot-check (E1–E6, the most important being a V-08 check that fails on any new sound or haptic caller in the APK). Logged DA-113…125, CA-9 and the CA-3 addendum. tasks.md v1.10 is the WO-007 plan, with the WO-006 plan-review lessons built in. A fresh plan review was dispatched.
- **WO-007 plan review: recirculate, 2 B / 10 S / 8 N.**
  - Both blockers are timing: the new settings texts would trip the promise scan before their exemption landed, and the two word-list copies would be compared while unequal.
  - v1.11 fixes all 20 findings: a one-move PROMISE-KEYS7 step, the settings tasks in sequence, implementer scaffolding tests, a SHA guard on the held tests, a real input-proof mechanism, and a smoke waiver. DA-126 covers module-scoped parallel builds.
  - The spot-check went to the same reviewer.
  - **Lesson:** the "two copies must land in one move" class has now appeared twice (WO-006 F1, WO-007 F2). It goes to AGENTS.md at the WO-007 close.
- **WO-007 G3 passed** (row 7, ai). The plan spot-check was forward with E1–E4, all applied; in particular the Dialog/Popup deny now excludes the debug-only `devtools`. AGENTS.md gained the "cross-file tests land together" rule (2nd occurrence).
- **Dispatched:** TASK-050 (build setup, alone) ∥ TASK-057 (the V-08 feedback-caller check with `dex_callers.py`, Python only) ∥ the WO-007 test author (T7pk staging + T7c authoring, no builds).
- **WO-007 build:**
  - TASK-050 done: settings module and build inputs; `ReleaseSeparationTest` 9/9 re-ran; G-04 clean.
  - TASK-055 done: `afterReset`, the `trailing` slot.
  - TASK-051 done: the gate and the 5 synthesized cues.
  - TASK-052 (`AudioTrack` outputs plus a smoke test that can fail) dispatched.
  - Still running: TASK-054 (play events), TASK-057 (V-08 caller check), the test author (T7pk + T7c).
  - Held-test SHAs were recorded before 054/056.
- **TASK-054 done:** play device 116/116 on API 37, held SHAs equal. **TASK-057 done:** 90 verifier tests; DA-127 allows Compose's Vibrator capability query by method. **TASK-052 done:** the audio smoke PASSES on both channels and the playback head advances, so no waiver is needed. **TASK-053** (the settings screen) dispatched.
- Note: an agent overwrote the orchestrator's scratch `count.py`, so the counter now lives as `orch_devcount.py`.
- **TASK-053 done:** the settings screen, ⚙ drawn in px, 13 keys en + fi, no Dialog/Popup. **PROMISE-KEYS7 done:** both word-list copies moved in one step, and the promise scan and equality test are green with the two settings texts exempt. **TASK-056** (app wiring + the lever + the probe) and **TASK-059** (the owner list) dispatched.
- **WO-007 build tasks done:**
  - **TASK-059:** the owner list, 12 F14 keys + 1 AI key.
  - **TASK-056:** the app wiring, the lever and the probe. Held SHAs are equal and the release twin passes through.
- **PRE-V08-7 PASS:** the release feedback-caller check matches the WO-007 class names, and the debug build finds all 3 debug-only classes.
- **`app` device run on API 37: 87/90.** The 3 are the expected kit gap: the WO-006 language walks don't yet read `settings`' strings. That is the C1/C2 carry in T7a; the evidence went to the test author.
- **Running now:** the test author (T7c compile, then T7a).
- **T7c compile-checked; T7a authored and compiled** (kit + carried cases; DA-128 for the rule-2/3 declaration conflict; AGENTS.md corrected: `assertIsDisplayed` is an extension and needs an import).
- **MOVE-JVM7 done.** 415 JVM tests, fresh, all green. The re-run proofs pass: `lockedReqTexts` is in the cache key, a `settings` edit re-runs the devtools test, an `app/src/debug` edit re-runs the scan, and a planted `Vibrator` fails 2 scans; all reverts are hash-equal.
  - Lesson: Gradle's `--rerun` applies only to the task named just before it. Use it after each task.
- **CR-6** (fresh code review) dispatched ∥ T7b (held-out authoring).
- **CR-6 fixes all done** (DA-129):
  - 050b: the settings strings are a declared input, proven.
  - 057b: V-08 pins caller + callee; 25 call sites = 25 rows; 93 verifier tests.
  - 052b / 056b / 056c: detach-by-view, counted play exceptions, a guarded event hook.
  - Test author: rule 4 catches scope calls, rule 6 one gate, the install-order check, the keyboard positive control.
- **MOVE-DEV7 started:** 4 new + 13 updated device-test files moved in, the kit 9 equal, held SHAs equal. The API 37 run (smoke → gate → 5 suites) is running.
- **MOVE-DEV7, API 37:**
  - The smoke and gate pass. play, browse and devtools are green.
  - settings is 7/8 and app is 103/115, in three groups:
    - (A) scrolls to a control outside the scroll container: fixture;
    - (B) the keyboard positive control fails: diagnosis; the focus block is only applied while open;
    - (C) the Finnish how-to's "lukittuu" trips the promise lock stem, so TASK-053b rewords it.
  - All checks are clean.
- **MOVE-DEV7 fixes:**
  - (A) the scroll walks were a fixture issue.
  - (B) the keyboard positive control was a harness issue: Compose-injected keys never reach focus in touch mode. With real window keys, ‹ works and the overlay blocks it.
  - (C) the Finnish text was reworded.
  - All recorded under DA-130, and the class re-runs are green.
- The full re-run (API 37, then API 26: smoke → gate → 5 suites) is running in the background.

## 2026-10-04 (evening) — pause for a backup

**Human (verbatim):** plz wait that last one and lets stop there again... I do backup and close computer for moment... how far we are are as 0% to 100% about on progress

**What was done (summary):** the running job, the full WO-007 MOVE-DEV7 re-run on API 37 and then API 26, is left to finish. No new job is started. Then the progress is saved (this entry and STATUS "▶ Resume here"), the emulator is stopped, and Jami is told when it is safe to shut down.
- Progress estimate given to Jami: about **75 %** of the planned build.
  - 6 of 9 work orders are closed, and WO-007 is about 80 %.
  - 34 of 47 locked REQs are covered today; 39 once WO-007 closes.
  - WO-008 (#PlayTime) and WO-009 (#Release) remain.
- **MOVE-DEV7 done.** The full re-run is green on both channels: API 37 and API 26 each 277/277 (play 116, browse 25, settings 8, app 115, devtools 13); smoke and gate PASS; every `--check` clean.
- **Paused here.** No new job was started. STATUS "▶ Resume here" points at TASK-058 → T&V7 → AUDIT7 → CLOSE7 → WO-008. The emulator is stopped and no agent is running. Safe to shut down.

## 2026-10-04 (evening) — installable build for a tablet

**Human (verbatim):** is there some installable version to test now on real tablet?

**What was done (summary):** checked `app/build/outputs/apk/debug/app-debug.apk` (built 14:32, newer than every source change, so it holds all of WO-001…007).
- It is signed with the Android debug key (v2 scheme, verified with apksigner), so it installs on Android 8+ by sideloading.
- Told Jami the path, how to install it, and what to look at on a tablet.
- The release APK is unsigned and not installable; that is for WO-009.
- No new job was started (still paused).

## 2026-10-04 (evening) — stray directory C:\GitHub\I

**Human (verbatim):** plz check if this directory done mistake is need? if not I remove it: C:\GitHub\I

**What was done (summary):** inspected it read-only. It holds one file, `TangramNoAds/.swdev/verifiers/v05_string_parity.py`, written 2026-10-02 07:26 during WO-001. The `\A` of `C:\GitHub\AI` was eaten by a backslash escape (the known slip class).
- It is an **older draft** of V-05 (3826 bytes). The real verifier in the repo is the later, improved version (4295 bytes, 18:30: it reads every values XML file and raises on a parse error).
- Nothing references the stray path.
- **Not needed: safe for Jami to delete.** The AGENTS.md backslash rule gained this example. Nothing was deleted by the agent.

## 2026-10-04 (evening) — first real-device check by Jami

**Human (verbatim):** game seams work, lets see what kiddo says from it... looks good so far!

**What was done (summary):** this is the first owner check on a real tablet: the debug APK with WO-001…007 works. It is recorded as owner evidence, not a requirement change. A child's playtest is coming; its findings will become owner items or capture-side DEF/CHG proposals (the REQs are locked). The session is still paused, with no new job started.

## 2026-10-04 (late) — resume WO-007

**Human (verbatim):** plz continue

**What was done (summary):** resumed from STATUS "▶ Resume here (WO-007)". Next, in order: TASK-058 (release checks, one build at a time) → T&V7 (held-out first run, both channels) → AUDIT7 → CLOSE7 → checkpoint 7 → WO-008 #PlayTime. Entries below.
- **TASK-058 run 1:** V-01, V-04 and V-08 PASS. The API 26 release launch is OK (754 ms, 0 app FATAL, no permissions).
- The release screenshot showed the ⚙ glyph filling its whole 48 dp box, so it looked oversized and crowded the edge. **DA-131 / TASK-053c:** the glyph is now 24 dp, centred; the touch box is unchanged.
- **Re-running:** the `settings` + `app` device suites on API 26 and then API 37. After that, the TASK-058 release steps run again.
- **TASK-058 done (run 2, after the gear fix):**
  - V-01, V-04 and V-08 PASS on release (V-08 with the 25 = 25 caller check).
  - API 26 release launch: 803 ms, 0 FATAL, 0 permission entries; the screenshot shows the ⚙ at icon size.
  - The V-04 positive control and V-08 `--expect-debug` PASS. V-05/06/07 PASS; JVM 675.
  - trace-check: every WO-007 REQ is covered. 39 of 47 locked REQs are covered; the remaining 8 belong to WO-008/009.
- T&V7 dispatched.
- **WO-007 T&V PASS** (DA-132). The held-out first run passes on both channels; one first-run failure was the solve-by-touch fixture trap again, corrected under route (b). 0 escaped product defects. The trap's 2nd occurrence is now an AGENTS.md rule. Traceability audit dispatched.
- **WO-007 trace audit GREEN** (7 IDs + 6 carried parts; 72/72 hashes; v1 unchanged; G-04 clean).
- **WO-007 CLOSED (CLOSE7):**
  - build-map v1.6 (#Settings built; #Locking cues; the WO-008 carries; the guard re-froze the baseline);
  - design-inputs 0.4 (the ⚙);
  - AGENTS.md (the V-08 caller-check duty, `settings` in the device suites, the phase line → WO-008);
  - the workorder: ledger, deltas, DoD 11/11, metrics;
  - tasks.md v1.12; STATUS (resume = WO-008).
- **Checkpoint 7 surfaced to Jami.** Totals: JVM 675; device 277 per channel; verifiers 93. 39 of 47 locked REQs are covered.
- Next: WO-008 #PlayTime.
- **WO-008 opened** (#PlayTime, Full mode). `workorders/WO-008.md` is written. The v1 format already holds `puzzleSeconds`, `bestSeconds`, `PlayTime(day, today, total)` and `timerShown`, so the design must fit them, or any change is a row-11 migration with a test. The design author (sonnet) was dispatched with the carries from WO-005, 006 and 007 and the injectable-clock requirement.
- **WO-008 design rev 0 in.** It has a `time` module with timestamp counting (an injectable clock, a 60 s idle window, no polling when idle). All data fits the frozen v1 fields, so row 11 is not triggered. The highest risk is the order of the four writers of puzzle and best times; the product fork is whether today and total include browsing and settings time. A fresh design review was dispatched.
- **Turn (Jami):** "time to hit bed.. plz dont start anything new now... we start shutting down"
  - Pause: no new jobs. The running design review is allowed to finish, and its result will be recorded without acting on it. STATUS "▶ Resume here" now says WO-008 is in design review, with what to do if the review file is or is not there. The API 37 emulator is stopped (it is not needed until the build).
- **Turn (Jami, 2026-10-05):** "plz continue"
  - Resumed. `reviews/WO-008-design-review.md` did not exist, so the overnight shutdown had stopped the design reviewer mid-read. I resumed the same reviewer from its saved transcript instead of a new one: same fresh context, same brief, no repo changes in between. No emulator is running; none is needed until the build.
- **Turn (Jami):** ""should today's and total play time include time spent browsing and in settings, or only time on a puzzle?"" can be total time (if thats easier) up to you really, but maybe some unactivity checking makes sence so that if havent done anything for minute then stop counting time until next press etc."
  - Owner input on WO-008 owner item 1. It is an idea, not a requirement: the AI decides (row 12), within the locked REQ-029/005 and TYPE-005 text.
  - It matches design rev 0: today and total count all in-app time, including browsing and settings, with a 60 s inactivity cut-off that resumes on the next press. Puzzle time is unaffected.
  - The answer was sent to the running design reviewer, who will check it against the locked text. If it fits, the DA row will cite this answer at rev 1.
- **WO-008 design review: recirculate (1 B / 3 S / 10 N)** (`reviews/WO-008-design-review.md`).
  - F1 (B): the reset order lets a due 10 s flush inside `puzzleShown` write the erased time back, and the keeper caches `bestSeconds` (O-08).
  - S1: the moved-in `HeldResetAppTest` goes red under live counting.
  - S2: three missed cross-file edits (`UntranslatedStringsTest` neutral keys, `FeedbackScan.PRODUCT_MODULES` + `time`, the `TestConfigRule` reset of `timeSource`).
  - S3: the reserved top-right corner is unswept, and the pill/Restart widths are unmeasured.
  - What holds: row 11 not triggered (the frozen fixture already has a Solved 47/41 record), the seams compile, and V-08 needs nothing new.
  - **The owner's item 1 closes:** today/total counting all in-app time is the locked REQ-029 + TYPE-005 reading, so no CHG is needed. The idle model matches the owner's wish. A held finger, key presses and the counted minute go to checkpoint 8 as one yes/no item.
  - WO-008 goal sentence corrected (N1). Rev 1 dispatched to the same design author (order: F1 → S1/S2 → S3 → Notes).
- **WO-008 design rev 1 in:** all 14 findings answered, no disputes.
  - F1: the keeper's inputs only account in memory, and a closed list of writers does the saving. The reset clears the time keeper first. The best is computed from the stored base. `OwnershipOrderTest` has 8 cases.
  - S1: the Test Author corrects `HeldResetAppTest` with a frozen clock, plus a new held `HeldResetTimeAppTest` and DA-144.
  - S2: a cross-file inventory with delivering tasks.
  - S3: an alternative mechanism. The pill is placed after Restart/DEV and steps below them if needed, so DA-71/75 are unchanged. A JVM sweep plus a device-measured check.
  - 12 DA rows (DA-133…144) + CA-10 + the CA-3 addendum; row 11 not triggered.
  - Spot-check dispatched to the same fresh reviewer.
- **WO-008 rev 1 spot-check: forward** with six line edits (E1–E6), no new B/S. The reviewer re-traced F1 (the reset can no longer resurrect time) and confirmed the S3 obstacle mechanism (DA-71/75 unchanged, DEV always placeable, no board shrink).
  - E1: replace `OwnershipOrderTest` case (6).
  - E2: `day` is set only when a whole second is credited, or the corrected `HeldResetAppTest` goes red.
  - E3: the pill must not jump within an attempt. Decided: a template width, with the hidden Restart as an obstacle.
  - E4: sweep the release no-DEV configuration.
  - E5: `HeldResetTimeAppTest` tagged as a decision test.
  - E6: `advanceActive` on the main thread.
  - Rev 2 dispatched to the design author.
- **Turn (Jami):** "plz add as potential future thing, not todays requirement... maybe 'easy' mode could be added with some 'safety robe' spinning around blocks so that when puzzle is finished that robe makes nice round around thru all blocks. Idea would be that it kind of tells if block is wrong place is robe breaks somewhere between two blocks"
- **Turn (Jami):** "thru block.. so block has robe passing thru it already when dropping it"
  - A future idea, explicitly not a requirement. The build side cannot write `Requirements/`, so it is recorded as **CA-11** in `req_review_01.md`: register a new SRC and a feature with status `idea`, both messages verbatim, the AI's reading and the open points (the withdrawn difficulty levels, separate best times, how to switch it on, the rope route per puzzle as a puzzle-format question). Nothing in WO-008 changes; the plan work continues.
- **WO-008 design rev 2** (E1–E6) applied; the orchestrator checked the Rev 2 list against the text. The design is forward.
  - **Logged:** DA-133…144 in `decisions.md`; CA-10 (the time model, with the owner's answer verbatim) and the WO-008 CA-3 addendum in `req_review_01.md`.
  - **Plan v1.13** (`tasks.md`, WO-008 section), orchestrator as planner (logged exception):
    - TASK-060…068 and 069 (checks);
    - the Test Author's T8a/T8b/T8c in landing parts;
    - the quiescent landing steps MOVE-STR8, GATE-063 (the gate tests staged before the keeper), LAND-A and LAND-068;
    - PRE-V08-8, MEASURE-8 (real widths on device), MOVE-JVM8, CR-7, MOVE-DEV8, T&V8, AUDIT8, CLOSE8;
    - a failure-routing table and the carried rows.
  - Fresh plan review dispatched.
- **WO-008 plan review: recirculate (narrow), 1 B / 12 S / 9 N.**
  - **B1 (blocker):** `SettingsScreenAppTest` (REQ-032.A2) holds exact tag sets for both settings screens, and the new timer switch would turn it red at the first device run. This is the third occurrence of the cross-file class; the design inventory also missed it.
  - **Plan v1.14 answers all 22 findings:**
    - the gate gets a compile against a signature-only stub, a rev-0-shaped positive control, gate-set hashes and a faulty-gate route;
    - DA-92 now has a `time`-local scan, with its inputs in 060; 060 runs on sonnet with a full spec and an input proof;
    - `ReleaseSeparationTest` lands at LAND-A;
    - the Test Author's tests land per module at each owner's done-check;
    - DEV-EARLY8 runs the play/time/settings/browse suites on device before review;
    - HELD-COMPILE8;
    - Test Authors compile outside the tree;
    - an isolation table, routing rows and the CLOSE8 items.
  - The design §8 inventory is written back.
  - Logged: DA-145 (quiescent landing) and DA-146 (DA-126 extended; outside-the-tree compiles).
  - Spot-check dispatched.
- **WO-008 plan spot-check: forward** (no new B/S). E1–E5 applied → plan v1.15. **G3 passed** (governance row 7 = ai, logged in tasks.md and the workorder).
  - Build starts: TASK-060 (build setup, sonnet) runs alone.
  - The Test Author starts in parallel on T8c parts (i) (strings checks) and (ii) (the gate, with a stub compile and a positive control outside the tree).
- **TASK-060 done** (build setup).
  - Diff read; V-01 and V-06 PASS; the G-04 grep shows only the 2 DA-89 lines.
  - The input proof passed: an in-place probe edit re-ran both test tasks while the compiles stayed up to date, and a no-change control was up to date.
  - The build files are frozen; their SHAs are recorded.
  - TASK-062 (format strings, haiku) dispatched.
- **TASK-062 done** (format strings in time/settings/browse, fi = en; V-05 PASS; diff read). MOVE-STR8 waits for the Test Author's T8c (i).
- **T8c (i)+(ii) in from the Test Author.**
  - The gate (`OwnershipOrderTest`, 9 cases, and `FlushRulesTest`) compiles outside the tree against a signature-only stub.
  - A rev-0-shaped keeper fails 7 cases (1, 2, 3, 6a, 6b, 7, 8). A reference keeper passes 17/17, and 7 single-fault mutants are each caught.
- **MOVE-STR8 done:** the strings checks landed (diff read); app JVM 52/52.
- Dispatched next: TASK-061 (kernel rule); the Test Author resumed for T8c (iii)–(v) and T8a.
- **TASK-061 built** (kernel time rule; diff read; kernel 154/154). Its module landing of the Test Author's `ActiveSecondTest` / `DurationFormatTest` waits for the author's hand-back.
- **GATE-063 done:** the gate set (5 files) is in `time/src/test`, with its SHAs recorded.
- **TASK-061 done:** the Test Author's kernel tests landed and pass on the real code (kernel 168/168). The held-test SHAs and the settings/browse test-tree hashes are recorded. Wave A dispatched: 063 (keeper) ∥ 065 (play) ∥ 066 (settings) ∥ 067 (browse).
- **TASK-067 done** (browse: best time in h min from one hour; diff read; browse 74/74; tree hashes equal + 1 new scaffolding file).
- T8c (v-t) staged: 7 time test classes, 33 tests, with REQ-005/029/030 tokens per the isolation table. They land with 063. Told 063's implementer of design §4.2's implied constructor read of `store.playTime()` (a design clarification the Test Author surfaced).
- **TASK-066 built** (settings: the timer switch first, the play-time section after sound, 5 F14 labels from the prototype, no new AI Finnish; settings 51/51; tree hashes equal; diff read; for CR-7: `revision` is a public var). The module landing waits for T8c (v-s).
- **TASK-063 done** (the keeper).
  - Gate `OwnershipOrderTest` 9/9 and `FlushRulesTest` 8/8; the gate SHAs are unchanged.
  - Diff read: the writers are exactly the closed list, and no input can write.
  - The Test Author's 7 time classes landed green; `time` 54/54.
- **TASK-065 built** (play: solve listener, timer slot, `timerRect`; play 222/222). The v-p landing is pending.
- DA-147 logged: the timer's template width is measured in `play` at 14 sp + 20 dp, so TASK-064's pill must match.
- **TASK-065 done:** the v-p landing passed on the real code (SolvedListenerTableTest 7/7, TimerPlacementSweepTest 5/5; 2340 cases, 0 fallbacks, 418 stepped below a control, 256 pills touching the outline recorded); play 234/234.
- **TASK-066 done:** the v-s landing passed (SettingsTimerTest 6/6, SettingsPauseTest 4/4); settings 61/61. **Wave A complete** (063, 065, 066, 067). LAND-A waits for T8c (iii) and T8a (i).
- **LAND-A done:** the scans now cover `time`; the settings exact sets (incl. B1's `SettingsScreenAppTest`) gain the timer switch; app 52, devtools 31, time 57 green; androidTest compiles; kits equal; the input proof passed. Device watch: `settings-sound` now sits lower, so check the no-scroll taps on short API 26 screens. TASK-064 (the timer pill) dispatched.
- **TASK-064 done** (the timer pill: inert, 14 sp tnum, 10 dp side padding to match DA-147; diff read; time 57/57; SHAs equal). TASK-068 (app wiring) dispatched; the held SHAs were checked before it.
- T8c (iv) staged under .swdev/staged/WO-008/iv/ (the ReleaseSeparationTest DA-72 third pass-through + TimeStoreRoundTripTest, 4 tests; with the rev-0 keeper the reset test fails). It lands at LAND-068. The Test Author is now on T8a (ii).
- **TASK-068 done** (app wiring; diff read: the reset clears the keeper first, the stop point is before sync, the touch observer never consumes; app 61/61, devtools 31/31; all SHAs equal). LAND-068 (iv) landed: TimeStoreRoundTripTest 4/4 on the real wiring (incl. a reset with a flush due); app 65/65. Waiting for T8a (ii).
- **LAND-068 done:** T8a (ii) landed (the kit, the corrected HeldResetAppTest with no assertion changed, C4/C6/C7, 6 new device test classes). All androidTest compiles OK; app 65/65; kits equal (12); all SHAs equal. Next: PRE-V08-8; T8b (held-out) dispatched to the Test Author.
- **PRE-V08-8 done:** V-08 PASS (release, the feedback-caller check included) and PASS (debug, --expect-debug). DA-148: DEV-EARLY8 runs before HELD-COMPILE8 while T8b is written (both still one build at a time). The API 37 emulator is booting.
- **T8b done** (held-out: 5 classes written outside the tree; the orchestrator did not read them). **DEV-EARLY8 run 1 (API 37):** play 116/116, browse 31/31; time 3/8, settings 11/14, TimerLayoutMeasured 0/6. One product fault class: the tagged pill and the play-time rows carry no text (unmerged children). DA-149; fixes 064b + 066b dispatched; the emulator stays up for the re-run.
- **DEV-EARLY8 run 2:** after 064b/066b, settings 14/14 and time 8/8. TimerLayoutMeasured 5/6: the tablet failure is a fixture fault (the test used the native density, not the override's); routed to the Test Author as a corrected check. Corrected width estimates are all inside the sweep bounds.
- **DEV-EARLY8 done (API 37):** play 116, browse 31, settings 14, time 8, TimerLayoutMeasured 6.
  - The measured widths are within the sweep bounds; the real numbers are recorded in the workorder.
- **HELD-COMPILE8 done:** the five held classes compiled (class files present), the overlay was removed and the held dir hashes are equal. The first unguarded `rm` was blocked by the safety check before anything ran, then redone with guarded paths.
- **MOVE-JVM8 done:** JVM **806/806**; V-05/06/07 PASS; the token isolation holds (4 mixed classes for AUDIT8 to check per method).
- CR-7 dispatched.
- **CR-7: forward** (0 B / 0 S / 9 N; the reviewer traced every writer path, and no path resurrects erased time). DA-150 triage: 068b (N5 a stuck touch cleared at onPause, N6 G-10 guards) and a Test Author template-agreement scan (N2); N8 seam text fixed; N1 and N4 to the owner at checkpoint 8; N3, N7, N9 accepted.
- **068b done** (N5/N6; app 66/66; diff read). Recorded: the implementer's Python edit script was refused by a permission check, and it then made the same in-scope edits with the Edit tool. N2 pending.
- **CR-7-FIX done:** 068b + the N2 TimerTemplateScanTest (2/2); app 68/68. MOVE-DEV8 started on API 37 (gate + 6 suites, background).
- **MOVE-DEV8 API 37: all green** (gate 10, play 116, browse 31, settings 14, time 8, app 137, devtools 13; 18 min; resets clean). API 26 booted as emulator-5554 (confirmed Phone_API_26, sdk 26); its run started.
- **MOVE-DEV8 API 26: all green** (the same 329; 21 min; resets clean). MOVE-DEV8 done on both channels.
- **TASK-069 checks:**
  - V-01, V-04 and V-08 PASS (release).
  - API 26 release launch: ok, 735 ms, 0 FATAL, 0 permission entries; the timer is off by default.
  - V-04 positive control and V-08 debug PASS; V-05/06/07 PASS.
  - trace-check flagged one WO-008 item: REQ-029.A2 has no covering test under its code home `time/`. Routed to the Test Author (DA-151); the rest is WO-009 scope.
- **TASK-069 done.** `PlayTimeReadoutTest` (REQ-029.A2 under `time/`, 7 tests) landed, and trace-check shows no WO-008 item (WO-009 items only).
  - Final JVM: **816/816**; verifiers 93 OK; device_reset 43 OK.
  - T&V8 dispatched.
- **T&V8 PASS:** the held-out first run passed 9/9 WO-008 cases (42/42 held) on both channels, first try; 0 escaped defects; the frozen-clock control fails 10/11; 0 B / 1 S / 5 N (DA-152). HELD-IN: the 5 held classes are in-tree, byte-identical; androidTest compiles. AUDIT8 dispatched.
- **AUDIT8 GREEN** (0 B / 2 S; the S items are close paperwork).
- **CLOSE8 done.** WO-008 #PlayTime is closed, DoD 11/11.
  - build-map v1.7 (guard delta logged).
  - design-inputs 0.5 (item 7: the prototype's time model versus the build).
  - AGENTS.md: the device line gains `time` and the gate; 4 lessons; the phase line → WO-009.
  - The workorder (ledger, DoD, metrics), tasks v1.16, STATUS (resume = WO-009; checkpoint 8 items), and a `proposals.md` entry (trace rule 3 checked late).
  - Totals: JVM 816; device 329 per channel; held-out 9/9 first run, no correction. **43 of 47 locked REQs covered**; the 4 left are WO-009's.
  - Checkpoint 8 surfaced to Jami.
- **WO-009 opened** (#Release, the last WO; Full mode). `workorders/WO-009.md` names the owner floors up front: `reviewedByHuman` is set only by Jami's review; the store channel, account, listing and publishing are Jami's; the release signing key is a security floor. The library gap: 13 puzzles today, REQ-042 needs 20+ over 4+ themes, all human-reviewed. Design author dispatched.
- **WO-009 design rev 0 in.**
  - No product code change.
  - 10 new AI-drafted puzzles (13 → 23, 6 themes), each unreviewed until Jami marks it with a `mark` tool, recorded in a ledger.
  - V-09, a release-time library gate that stays RED until the review.
  - The store-bound universal APK, behind a spike.
  - `release/` drafts: the listing EN+FI, the privacy text and a checklist.
  - 11 owner forks, led by the store channel (Play interim).
  - Highest risk: tokens on drafts and rule tests could read as "ready to publish".
  - Fresh design review dispatched. The design calls the owner "he" in places; the reviewer will flag it for rev 1.
- **Turn (Jami, answers to four WO-009 questions):** "Where should Tangram be published first?"="Google Play (Recommended)", "How many new puzzles should I draft for the first release (it needs at least 20, today there are 13)?"="12 new, 25 total", "Every puzzle must be marked 'reviewed by a person' before release, and only you may do that. How do you want to mark them?"="A mark command (Recommended)", "The app has no launcher icon yet and the store needs graphics. Who makes them?"="AI proposes, you pick"
  - These are owner decisions on human rows; they are logged (DA-153 pre-entry).
  - **Play** means the Play-shaped REQs apply as written, so no channel CHG is needed.
  - **12 new puzzles** gives 25 total.
  - **The `mark` command** is run by Jami only.
  - **Icon:** the AI proposes options and Jami picks. This is an idea, not a requirement; it adds a launcher icon resource.
  - The answers were passed to the running design reviewer.
- **Turn (Jami):** "lets pause again when change comes.. need to leave soon plus almost out of tokens"
  - Pause: no new jobs. The running WO-009 design review may finish; its result will only be recorded.
  - STATUS "▶ Resume here" now reads: WO-009 in design review, with the DA-153 answers and what rev 1 must take in; what to do if the review file is or is not there.
  - No emulator is running. Nothing is committed (WO-007 and WO-008 are closed and uncommitted).
