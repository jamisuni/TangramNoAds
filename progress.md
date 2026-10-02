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
