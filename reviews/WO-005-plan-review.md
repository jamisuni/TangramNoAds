# Review — plan · WO-005

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` (section "WO-005": TASK-T5, TASK-030…036, CR-3, MOVE-5; the WO-004 section for shape) · `workorders/WO-005.md` · `designs/WO-005-design.md` rev 2 (Test seams, acceptance table, DA-72…88, Suggested cut, §6 V-04) · `reviews/WO-005-design-review.md` with its spot-check (E1–E4) · `reviews/WO-004-plan-review.md` · AGENTS.md (all) · `architecture.md` G-04 / §5 · `governance.md` · `decisions.md` (DA-88 is logged; CA-5 is in `req_review_01.md`) · `.swdev/verifiers/v06_module_deps.py` (devtools row) · repo greps for `devtools`, `0417`, `poseOf`. `.swdev/heldout/` not read.

## Verdict

**recirculate → planner (orchestrator).** One Blocker, eight Shoulds, six Nits.

The cut itself is right:
- Coverage is complete: A1–A4 land on implementing tasks and on TASK-T5, and every design seam has one delivering task via the D-n to TASK map.
- `devtools` depends on `kernel` and `contracts` only (DA-84, V-06 row), so `play` ∥ `devtools` core is truly module-separate. TASK-031 ∥ TASK-032 is safe.
- TASK-035 is independent (crafted fixtures).
- CR-3 sits before the move-in.
- The `solvePending` before-early-return trap, `Role.Button`, the WO-006/008/009 carries and CA-5 are all kept.

The problems are in what can actually be run and when. The A4 token test has no runnable owner. Staged tests and the DEV-pill regression run only at the very end. The new module has no device-channel proof. The final check line is garbled.

## Checklist applied

- [x] **Directives.** D1: one seam per task, apart from TASK-031 (N6). D3: every row names REQs or G-04/ADR. D5: no new library.
- [x] **Guardrails.** G-04 is carried by 034 (grep), 035 (V-04) and 036 (runs). G-05: 033 (V-05). G-06: 030 (V-06). No leak path found that no task checks (see "Release safety" below).
- [x] **Contract.** No `I*` change. v1 format untouched. The design's "no `SessionHost` change" is not stated in the row (N1).
- [x] **Scope.** Nothing unrequested.
- [x] **Mode and toolchain (G3).**
  - Full is justified (G-04, new module and source-set split, a verifier that must be able to fail).
  - The JVM and API 37 channels are proven by WO-003/004.
  - The new module `devtools` has no instrumented-channel proof (F4).
- [x] **Evidence re-derived.**
  - `PieceGeometry.poseOf` exists in `kernel` (PieceGeometry.kt:76).
  - Today there is no `devtools` or `0417` text in any of the S-set files.
  - After TASK-030 there will be `devtools` text in two of them (F1).
  - `tools/check_apk_puzzles.py` exists.
  - `v06_module_deps.py` already lists `devtools: {kernel, contracts}`.
  - `app/src` holds only `androidTest`, `main` and `test`, so `debug/` and `release/` are created by TASK-034.

## Coverage, both ways

| ID / seam | Lands on | Verdict |
|---|---|---|
| REQ-046.A1 | 032, 033, 034, T5 | ok |
| REQ-046.A2 | 031 (hook), 032 (shapes), 033 (overlay); index omits 034 (the `boardOverlay` wiring) | ok (N2) |
| REQ-046.A3 | 031, 033, 034, T5; the `DevSolution.poses` / `DevSolveNowTest` half is on 032, which does not list A3 | ok (N2) |
| REQ-046.A4 | 030 (inputs), 034 (pair), 035 (artifact), T5 | **the token test itself is unowned (F1)** |
| Seams `BoardSpace`, `PlayArea` slots, `placeSecondary`, `solveByAid`, `onSolved` | 031 | ok |
| `DevToolsState`, `DevSolution`, `DevPasscode` | 032 | ok |
| `DevCornerButton`, overlay, tags, strings | 033 | ok |
| `DebugAids` pair, `AppViewModel.aids`, `TangramApp` | 034 (`MainActivity` is not named, N1) | ok |
| `v04_release_apk.py` API and CLI | 035 | ok |
| build files, S-set inputs | 030 | ok |
| Every row names its REQs / enabler | yes | ok |

V-04's modes and self-test (rev 2 E1, E4) on TASK-035: modes, canary, deny-list, dex magic, exit mapping, `--positive-control`, the fold, E1 fixtures and the canary-minus-one fixtures are all named. Not named in the row, but covered by pointing at "D-5": the fail-closed header and offset rows, the whole-string `0417` rule, the `.png` exclusion, and "the `scanner blind` line names `CANARIES`" (N5).

## Findings

### F1 — COVERAGE / DONE-CHECK · Blocker · TASK-T5, TASK-030, MOVE-5 / design §Test inputs, acceptance row A4
- **Observation:**
  - **The A4 token carrier has no owner.**
    - The only REQ-046.A4 token test is `devtools/src/test/ReleaseSeparationTest` (DA-81).
    - TASK-T5's row never names it, nor the exact file set `S` (rev 2 E2: `<module>/src/main/**` for `app, play, browse, kernel, contracts, content, store`; `app/src/release/**`; `app/build.gradle.kts`; `settings.gradle.kts`; never `build/`, `.gradle/`, `.swdev/`).
    - The ask was that S is on 030 and on T5. It is on 030 only.
  - **TASK-030's done-check cannot run.**
    - It says "the design's edit-a-scanned-file re-run proof", which needs `ReleaseSeparationTest` in the tree: edit `play`, run `:devtools:testDebugUnitTest`, it must re-run and fail.
    - But the test is T5's, staged in `.swdev/staged/WO-005/` and moved in only at MOVE-5, after everything else.
    - So at TASK-030 the proof is impossible. A haiku implementer would write a stand-in test (breaking "implementers never write acceptance verdicts", and risking a token collision).
  - **The design's S rule contradicts itself.**
    - "Case-insensitive `devtools` in S: none" cannot hold: `settings.gradle.kts` must contain `include(":devtools")` and `app/build.gradle.kts` must contain `debugImplementation(project(":devtools"))`, and both are in S.
    - The design also says the test asserts the second is the only place `:devtools` appears.
    - The test author will stage a test that fails on day one, or silently exempt something.
- **Fix:**
  - TASK-T5: name `ReleaseSeparationTest`, set S, and the exact exemptions: the `include(":devtools")` line of `settings.gradle.kts`, and the `debugImplementation(…":devtools"…)` line of `app/build.gradle.kts` (nothing else). Add "`error(…)` if `repo.root` is missing; the same S definition is the build file's input set".
  - TASK-030: replace the done-check with a runnable one: a throwaway proof test written and deleted by the implementer under a name that carries no REQ token (e.g. `ScaffoldInputsProbeTest`), or defer the re-run proof to MOVE-5 and say so. Also say who owns the single shared S definition (030 writes it, T5's test reads the same list).
  - Write the exemption back into the design (rev 2 note) and `decisions.md` before T5 starts (AGENTS: a seam changed at planning is written back).

### F2 — DONE-CHECK · Should · TASK-036
- **Observation:**
  - The row reads "`--positive-control` on a fresh debug APK (exit 1 / all markers found)".
  - The design says exit **0** = every marker kind found; exit 1 = "scanner cannot see <kind>".
  - "exit 1" as a pass condition inverts V-04's meaning. A reader could accept the failing mode.
  - Also missing:
    - the close **evidence line naming the positive-control run** (design §6, DA-86);
    - an order for the two builds (V-04 default builds release, `--positive-control` builds debug, `connectedDebugAndroidTest` uses the debug APK: they must not overlap).
- **Fix:**
  - Write: "`python .swdev/verifiers/v04_release_apk.py` → exit 0 (`V-04 PASS: <apk>`); `python .swdev/verifiers/v04_release_apk.py --positive-control` → exit 0 (all four kinds found: dex package, dex `0417`, arsc `devtools_`, arsc `Wrong passcode.`)".
  - Add: "run them one at a time, before or after the device runs; paste both result lines into the close evidence".

### F3 — ORDER / DONE-CHECK · Should · MOVE-5, TASK-034, CR-3 / AGENTS "staged device tests are compiled early", WO-004 plan review F5
- **Observation:**
  - **Nothing staged moves in before CR-3.**
    - The independent JVM tests (play `SolveByAidTest` and the sweep, `DevToolsStateTest`, `DevSolutionTest`, `DevSolveNowTest`) run for the first time at MOVE-5, after all six implementation tasks and the code review.
    - WO-004 ran a MOVE-JVM before its mandatory review (plan review F5).
    - Consequence: CR-3 reviews code whose independent verdict is not known, and a 031 defect is found three tasks late.
  - **The DEV pill regresses nothing visible until the end.**
    - From TASK-034 every debug `app` screen carries the pill.
    - 034's done-check is `assembleDebug :app:assembleRelease :app:testDebugUnitTest`: no device run, so the existing 15 app device tests (WO-003/004) first see the pill at 036 (and MOVE-5 only runs the staged ones).
    - The design (Risks) rates "existing device tests now see the DEV pill" a known risk.
- **Fix:**
  - Add **MOVE-JVM** (orchestrator): staged `play`, `devtools` and `app/src/test` JVM tests, moved in after TASK-031/032/034 as each API exists; must pass unedited; before CR-3.
  - TASK-034's done-check adds: emulator up, `:app:connectedDebugAndroidTest` (the existing 15) green. Failures are read against DA-40/DA-70 before the product is blamed.
  - TASK-033 and 034 move the staged `devtools` device tests in before dispatch (WO-004 TASK-027 precedent) so the UI done-check exercises them.

### F4 — DONE-CHECK · Should · TASK-030, TASK-033 / WO-003 F7, WO-004 plan review F7 (new instrumented module)
- **Observation:**
  - `devtools` is a new Android library with an androidTest set (espresso 3.7.0 pin, `testInstrumentationRunner`, Compose test deps).
  - TASK-030 has no channel proof, and 033 only runs `:devtools:assembleDebugAndroidTest` (compile).
  - The first time a `devtools` instrumented test runs on a device is MOVE-5. A wiring fault there costs a full cycle at the end.
  - WO-004's TASK-021 solved this with one trivial instrumented test.
- **Fix:**
  - TASK-030 adds a `devtools/src/androidTest` channel-proof test (a name no REQ token, a trivial assertion) and the done-check `:devtools:connectedDebugAndroidTest` with the emulator up. Or move that proof into 033 with the staged `devtools` device tests (F3), and say so.

### F5 — TEST / DONE-CHECK · Should · TASK-031 / design spot-check (a), AGENTS "done-check cannot exercise the behaviour"
- **Observation:**
  - The row names the trap ("`solvePending` resolved in `onFrame` before its no-drag early return"). Good.
  - But nothing in the row's done-check pins it before MOVE-5:
    - The catching test (`SolveByAidTest`: pending until `onFrame(1000)`, then `t0 == 1000`, no drag) is T5's, staged.
    - The design's idle-board device test ("`solvePending` keeps the frame loop alive on an idle board") is a scaffolding item, and the row does not require it.
  - So TASK-031 can hand back green with the trap live, and only MOVE-5 finds it.
- **Fix:**
  - Require in the row two `Scaffold…` tests (not T5's names): a JVM `onFrame` on an idle session after `solveByAid` (no drag) that sets `solved` and clears `solvePending`; and the device test that the REQ-023 fade advances with no touch.
  - Add to the done-check: "diff read for the order inside `onFrame`".

### F6 — HELD-OUT · Should · TASK-T5 / design acceptance table A1, A3
- **Observation:**
  - The choice of A1 and A3 is risk-weighted (the wrong-code/unlock path; the only chain that touches the stored v1 document and the REQ-023 timeline). That is right.
  - **But the overlap rule is not met as described.**
    - The design's *visible* A3 row already lists the app JVM chain (13 puzzles, parsed JSON, `bestSeconds` 41 kept) and the app device "after the fade" test.
    - The held-out description in the plan ("the stored document, the best-time cases, the timeline after an aid solve") is the same content.
    - The A1 held-out "wrong-code path + unlock" equals the visible `DevAidUiTest` and `DevToolsStateTest` rows.
  - T5 as written ("no overlap with visible") gives the author nothing to keep it true.
- **Fix:**
  - Have T5 name the split, e.g.:
    - visible A3 = engine-level `DevSolveNowTest` + `SolveByAidTest` + the app device fade test;
    - held-out A3 = the app JVM stored-document chain and the best-time cases (null kept, 41 kept, the other keys and `version` untouched), JVM + device each;
    - held-out A1 = the wrong-code variants (`"1234"`, `""`, `"04170"`) plus the Back/rotation unlock persistence;
    - the visible A1 keeps the `submit` unit and the one UI tap path.
  - The orchestrator's isolation check lists the overlap by ID, as in WO-004.

### F7 — EMULATOR · Should · header note, TASK-031, TASK-033, MOVE-5, TASK-036
- **Observation:**
  - The header says "one user at a time; `Medium_Phone_API_37.0`". WO-004 also stated the precondition: the boot command and `adb devices` showing `device`, before every device done-check.
  - The order does not enforce one user: TASK-031's done-check includes `:play:connectedDebugAndroidTest`, while 031 ∥ 032/033 and T5's compile run alongside. Only 031 is a device user until F3/F4 are applied, but the plan should say who holds the emulator when two are due.
- **Fix:**
  - Restate the precondition with the boot command and the `adb devices` check.
  - Serialize device-using rows: 031 (and the 034 existing-app run, F3), then MOVE-5, then 036. Add "the device-run rows hold the emulator; no parallel row may run `connected…`".

### F8 — STAFFING · Should · TASK-030 (and TASK-032) / AGENTS "Implementer staffing"
- **Observation:**
  - TASK-030 is haiku. It is not a mechanical include-line:
    - it declares the test-task inputs through `fileTree(rootDir) { include(…) }.withPathSensitivity(RELATIVE)`;
    - it sets `systemProperty("repo.root", …)`;
    - it shares one definition between the test and the build file;
    - it keeps a cache-correctness property that a design review rated Should (F4).
  - WO-004's build step (the equivalent TASK-021) was sonnet.
  - TASK-032 (haiku) carries observable state (`mutableStateOf`) and the exact-geometry `DevSolution.poses`, which must never throw and must equal the polygon as a point set (G-10). The design said "haiku with a read of the diff".
- **Fix:**
  - TASK-030 → sonnet. TASK-032 may stay haiku, but add "orchestrator reads the diff; `poses` equals the polygon point set for all 13 puzzles" to its done-check (the staged `DevSolutionTest` after F3 gives it).

### F9 — VERIFIER · Should · TASK-035 / "hardest/riskiest first", design Risks (1)
- **Observation:**
  - V-04 is the design's stated highest risk (a hand-written dex parser, "a gate that passes because it cannot see"), but its first run on a real APK is TASK-036, after everything.
  - A real release APK from before devtools already exists (`app/build/outputs/apk/release/app-release-unsigned.apk`, 2 dex, `038`). The design review proved the four canaries and a zero-hit scan on it.
  - A parser bug or a false "scanner blind" would otherwise surface only at the end.
- **Fix:**
  - Add to TASK-035's done-check: `python .swdev/verifiers/v04_release_apk.py --apk <that release APK>` → exit 0, and the same file with `--no-puzzles` if the puzzle check needs a rebuilt tree.
  - Optionally one run of `--positive-control` is premature (no devtools in debug yet), so say it is **not** run at 035.

### N1 — BRIEF · Nit · TASK-034
- "`onSolved(byAid)` passthrough only where the design says" is vague. The design says production does **not** forward it: `SessionHost` is unchanged (WO-008 wires it). Write "no `SessionHost` change; `onSolved` stays the default `{}` in production".
- `MainActivity` (passes `model.aids`) is a seam and not named in the row.
- Give the G-04 grep command and its set: `grep -rniE "devtools|0417"` over `src/main` and `src/release` of every module except `devtools` (not `app/src/debug`, not the build files, which legitimately name `:devtools`).

### N2 — INDEX · Nit · "ID → task → TASK-T5 index"
- A2 should list 034 (the `boardOverlay` wiring and its `app` device test); A3 should list 032 (`DevSolution.poses` and `DevSolveNowTest`); 032's Serves cell lists A1 and A2 only.

### N3 — CARRIES · Nit · "Carried OUT"
- WO-009: add "extend V-04 to the `.aab` if one is uploaded" (design §Scope).
- WO-006: add "REQ-047 tests do not scan the dev dialog or the pill text 'DEV'".
- Owner items for the close are not listed: the AI Finnish rows, the G-04 reading (DA-85), the extra `devtools_solve_failed` string (DA-80), and the informational line on the pill's corner (design review O1). Only the Finnish appears (in 033).

### N4 — PARALLEL MECHANICS · Nit · TASK-T5
- "compile staged device tests as soon as the APIs exist" while 031/032/033 edit the same modules in parallel needs a stated mechanism, or two Gradle users collide in one module. WO-004's rule applies: the author compiles in a worktree or copy, or waits for the owning task's hand-back.
- Also repeat the AGENTS brief items the row only names by reference: test tokens go only on the test whose assertion is the criterion's meaning (`// decision DA-n` otherwise), and the Compose import lesson.

### N5 — SPEC POINTER · Nit · TASK-035
- The row lists the main V-04 behaviours but not "design §6 in full is the spec". Add it, so the fail-closed rows (no dex; a header size other than 0x70; a table offset past the file), the whole-string rule, the `.png` exclusion and the `scanner blind … update CANARIES` message are not dropped.

### N6 — SHAPE · Nit · TASK-031
- One sonnet context carries two seams: the layout side (`BoardSpace`, two `PlayArea` slots, `placeSecondary` and its 468-case sweep) and the session state machine (`solveByAid`, pending solve, `onSolved`). Both are in `play`, so they are serial anyway. Splitting into 031a (layout) and 031b (session; riskiest first) would follow the WO-004 024/026 precedent and put the trap behind its own done-check (F5). Acceptable as one task if the row keeps the F5 tests.

## Release safety (G-04)

- **TASK-034's grep:** correct in intent but under-specified (N1). It covers source only, which is right, because the artifact is V-04's job.
- **TASK-036:** the V-04 release run (exit 0) is right; the `--positive-control` condition is garbled (F2).
- **Resource leakage via `debugImplementation` merging:** no unchecked path found.
  - Library strings and resources merge into the debug variant only.
  - V-04's deny-list scans every non-dex entry of the release APK for the package forms, `devtools_` and `Wrong passcode.`.
  - The separation test scans `res/` of every other module and `app/src/release`.
  - The one unreachable-by-tool path (devtools logic re-implemented elsewhere) is on CR-3's brief (N7 of the design review). It is in the row.
- **Note:** V-04's first run on the real release APK should come at 035 (F9), not only at 036.

## Handoff — Plan Reviewer · WO-005
- **Scope:** REQ-046 A1–A4 · TASK-T5, TASK-030…036, CR-3, MOVE-5.
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-005-plan-review.md`
- **Status:** **recirculate → planner** (1 Blocker / 8 Shoulds / 6 Nits).
  - F1 (Blocker): `ReleaseSeparationTest` and S are not on T5, and TASK-030's re-run proof needs a test that is only staged; S's "no `devtools`" rule contradicts `settings.gradle.kts` / `app/build.gradle.kts`.
  - F2: TASK-036's positive-control pass condition reads "exit 1".
  - F3: no staged move-in before CR-3; the existing app device suite is not run after 034.
  - F4: no instrumented-channel proof for the new `devtools` module.
  - F5: TASK-031's done-check does not pin the `solvePending` early-return trap.
  - F6: held-out A1/A3 overlap the design's visible rows.
  - F7: the emulator precondition and the one-user order are not stated.
  - F8: TASK-030 (haiku) is not mechanical.
  - F9: V-04's first real-APK run is at the very end.
- **Traceability delta:** none. All fixes are edits to `tasks.md`, plus a design rev-2 note and a `decisions.md` row for F1.
- **Notes for next station:** after the planner's edits a spot-check of F1, F3 and F4 is enough.

## Spot-check (re-review)

**Date:** 2026-10-03 · **Read:** `tasks.md` "## WO-005" v1.5 in full, the design's "Test inputs of the A4 test" paragraph, and `decisions.md` DA-89 (present). Only the changes were checked.

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (Blocker) | **fixed** | `ReleaseSeparationTest` and S with the two exact DA-89 exemptions are on TASK-T5 (visible A4) and in the header. TASK-030 no longer needs the test: its done-check is runnable. The re-run and fail proof sits in MOVE-JVM, after the test lands. Written back into the design paragraph and `decisions.md`. Residue: N-a below |
| F2 | **fixed** | TASK-036 runs one build at a time. The V-04 release run is exit 0, and `--positive-control` is **exit 0 = all four kinds found**. Both lines go to `WO-005.md` |
| F3 | **fixed**, residue | MOVE-JVM precedes CR-3, and CR-3 depends on it. TASK-034 re-runs `:app:connectedDebugAndroidTest`. Residue: N-b |
| F4 | **fixed** | TASK-030 adds a `devtools` androidTest channel proof, run on the emulator |
| F5 | **fixed** | 031a (layout) and 031b (session). 031b names the two `Scaffold…` trap tests and requires them green |
| F6 | **fixed** | Held-out is A1 and A3 entirely. Visible is A2, A4 and the decision tests. No overlap by ID |
| F7 | **fixed** | The emulator precondition and the boot command are stated. Device rows run in the order 030, 031a, 031b, 034, MOVE-DEV, 036. TASK-033 only compiles `androidTest`, so no second device user exists |
| F8 | **fixed** | TASK-030 is sonnet. TASK-032 keeps its diff read |
| F9 | **fixed** | TASK-035 runs `--apk` on today's release APK (exit 0) |
| N1–N6 | **fixed** | The `SessionHost` ruling, `MainActivity` and the grep are named. The index is corrected. The carries and owner list are added. Per-module move-ins are stated, with compile failures routed back to T5. TASK-035 points at design §6 in full. The 031 split is done |

Re-checked: 031a/031b ∥ 032 stays safe, because `devtools` does not compile `play`. CR-3 depends on MOVE-JVM and TASK-035. TASK-034 depends on 031b and 033. No new Blocker or Should.

**Residue (Nits, no recirculation):**
- **N-a:** the design paragraph still ends with the old sentence "D-0's done-check: edit one scanned file … re-run and fail". The plan moves that proof to MOVE-JVM, so the sentence is stale. Edit the design paragraph's last sentence to say "at MOVE-JVM".
- **N-b:** the `devtools` UI device behaviour (dialog, unlock, overlay nodes) first runs at MOVE-DEV, after CR-3. TASK-033 only compiles `androidTest`, though the channel proof covers wiring. This is accepted (a failure routes back to 033). Optionally move the staged `devtools` device tests in before TASK-033's hand-back.

**Spot-check verdict: forward.** The Planner may brief the Acceptance Test Author and the implementers. N-a is a one-line design edit.
