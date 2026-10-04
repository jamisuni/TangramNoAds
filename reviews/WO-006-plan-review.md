# Review — plan · WO-006

**Date:** 2026-10-04  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` "## WO-006" (plan v1.7: TASK-040…048, T6a/b/c, MOVE-JVM6, MOVE-DEV6, CR-5, T&V6, AUDIT6) · `designs/WO-006-design.md` rev 2 (Test seams, acceptance table, Suggested cut, §1, §4–§6) · `reviews/WO-006-design-review.md` with spot-check E1–E6 · `workorders/WO-006.md` · `AGENTS.md` · `decisions.md` (DA-96…108 present, 13 rows) · `req_review_01.md` (CA-7, CA-8 present) · `reviews/WO-005-plan-review.md` (precedent) · `.swdev/verifiers/v04_release_apk.py` (dex reader). `.swdev/heldout/` not read; no Gradle, no emulator, no git.

## Verdict

**recirculate → planner (orchestrator).** 1 Blocker, 10 Shoulds, 6 Nits.

What is right:
- All 13 IDs land on a task and a test in the right code home. The split is 8 visible and 5 held-out (035 A2, 036 A2, 037 A1, 047 A2, 010 A2), exactly as in the design's table.
- Every seam in the design table has a delivering task.
- E1 (`se-NO,fi-FI`), E2 (`@UsesDisplayRule`, `@Parameters(name = "{0}")`, the sixth call), E5 (a non-clean `--check` voids a step), E6a (`TabletSmallestWalkAppTest` outside the gate) and E6b (V-08 freshness set) are all carried.
- TASK-043 waits for the sweep's verdict.
- G-04 is stated in the header and in 040's done-check.
- V-04, V-08 and the positive control are all in 048.

The problems are the same kind as in WO-005: what can actually run when. One JVM test cannot be moved in at MOVE-JVM6. The held-out kit has no link to the gate or the fallback. Several done-checks cannot fail (042, 044, the MOVE-JVM6 proof). Two parallel starts collide on Gradle.

## Checklist applied

- [x] **Coverage both ways.** Every ID has a delivering task and a covering test. Every task names REQs, a decision or a guardrail (index nit N1).
- [~] **Carried parts.** C1–C11 appear only as the range "C1–C11" (header, 048, AUDIT6). They are not named one by one in the plan (S9).
- [~] **Shape.** One seam per task holds. `[P]` is not used. Two "∥" starts share Gradle or files (S3).
- [x] **Mode (Full) is justified.** G3 toolchain: JVM, API 37 and API 26 are proven by WO-003 to WO-005. The new device mechanisms (`wm` override, `cmd overlay`, `Parameterized` with `tests_regex`, `aapt2`) have no proof before the gate, which sits after CR-5 (S5).
- [x] **Briefs point at artifacts.** Rows cite design seams and DA numbers, not paraphrased spec. Exceptions are in S8.
- [~] **Release safety.** Covered: the globs, the debug-only classes (041 and 044), V-04 and V-08 at 048. Gaps are listed under "Release safety".

## Coverage, both ways

| ID / part | Lands on | Verdict |
|---|---|---|
| REQ-006 A1 | T6a (`PlayThroughAppTest`); test only | ok. Index also lists 041 (N1) |
| REQ-035 A1 | T6a | ok |
| REQ-035 A2 (H) | T6b; contingent code 045 | ok. 045 cannot be verified without the held-out test (S8) |
| REQ-036 A1 | 043 (if breach), T6a | ok |
| REQ-036 A2 (H) | T6b | ok |
| REQ-037 A1 (H) | 043, 045, T6b; visible walk is `TabletSmallestWalkAppTest` | ok |
| REQ-047 A1 | 041, 046, T6a | ok |
| REQ-047 A2 (H) | 041, T6b | ok |
| REQ-001 A1, REQ-008 A1–A2 | 040, 044, T6a (`PromiseWalkAppTest`, `PromiseSourceScanTest` as guardrail) | ok |
| REQ-010 A1 | T6a | ok |
| REQ-010 A2 (H) | 044, T6b, 048 (V-01, V-08, API 26 `dumpsys` line) | ok |
| Seams L-0a, L-0b, L-1, H-1, P-1, T-1a/b/c, C-1, X-1 | 040, 041, 043, 042, 044, T6a/b/c, CR-5, 047 | ok |
| C1–C11 | not enumerated (S9) | partial |
| Z close items | only partly owned (S9) | partial |

## Findings

| ID | Sev | Task | Finding | Rule | Required change |
|---|---|---|---|---|---|
| F1 | **B** | T6c, MOVE-JVM6 | **The word-list equality check cannot run at MOVE-JVM6.** The design (§6 item 4) defines it as a JVM test that reads **both** the `app/src/test` list and the copy in the device tests. The device copy is T6a's, which is not in the tree until MOVE-DEV6. T6c lists only TASK-040 as its dependency, and MOVE-JVM6's done-check is "JVM green". The test would `error()` on a missing file, or the author would be tempted to stub it. It is the same shape as the WO-005 F1 Blocker. | seams lesson (every delivering task can run when planned); trace-check rule 3 | Choose one and write it in the rows. (a) The equality check is a separate file, staged and moved in at MOVE-DEV6 after T6a. MOVE-JVM6's done-check then says it is not moved in. (b) T6c depends on T6a's staged list file. Either way the row says which file pair is read and who owns each copy. |
| F2 | S | MOVE-DEV6, T&V6, T6b | **Dependencies are missing.** MOVE-DEV6 moves T6a's tests in but does not depend on T6a. T6b (the 5 held-out IDs) is a dependency of nothing. T&V6 depends only on 048, so a missing or uncompiled held-out slice would not block the verdict. | task-decomposition (explicit depends-on) | MOVE-DEV6 depends on T6a. T&V6 depends on T6b. Add "held-out compiled: yes" as an input check for T&V6. |
| F3 | S | T6b, MOVE-DEV6 | **The held-out kit is not tied to the gate or the fallback.** (a) The design says the G-DISPLAY gate must pass before held-out kit copies are compiled against the helpers' behaviour. In the plan T6b ∥ T6a and finishes before the gate (MOVE-DEV6, after CR-5). If the gate fails and the fallback is used, the held-out kit's `DisplayRule` copy (`displayPreset`, `@UsesDisplayRule`, `@Parameterized` names) is already written. (b) The T6b row does not require its classes to carry `@UsesDisplayRule` and `@Parameters(name = "{0}")`. Without them the fallback's `tests_regex` and the sixth `notAnnotation` call miss the held-out classes. (c) The kit copies carry their own helper copies. If the annotation lives in another package, `notAnnotation=<package>.UsesDisplayRule` finds only one of them, and E2(iii)'s counts do not add up. | design §1 gate, E2 | T6b's row requires: the same marker FQN as T6a (or the fallback's sixth call names both), `@Parameters(name = "{0}")`, and `displayPreset` handling in its `DisplayRule` copy. Add a plan rule: the kit copies are re-synced from the gate's final `DisplayRule` before T&V6, with "kit copy equals visible helper except package" checked by the orchestrator (not by reading held-out bodies). |
| F4 | S | order step 1, step 3, 044, T6a/T6b | **The "∥" starts break "one Gradle build at a time" and share files.** (a) Step 1 runs 040 ∥ 042 ∥ 044. 044's real-APK pre-run is a release build, which reads the tree while 040 edits `app/build.gradle.kts` and runs its own Gradle. (b) Step 3 runs T6a ∥ T6b. Both compile "a scratch copy into a test set" of `app/src/androidTest`. Two authors copying into the same source set compile each other's files, and both use one build lock. WO-005 N4 had the same note (a worktree or wait). (c) 042's live `--check` and 041's smoke both want the emulator; the order text covers it (step 6), the start line does not. | AGENTS Gradle/emulator duty; shape: `[P]` only with no shared files | (a) 044's pre-run runs after 040's hand-back, before 041 starts, so the APK is clean of the debug classes. (b) T6a and T6b compile in turn, or each uses its own worktree/copy of the repo. The row says which. (c) Put "device rows hold the emulator" next to the start line. |
| F5 | S | TASK-041 | **The device smoke has no named test and no owner.** The done-check is "a Finnish launch shows Kissa; `fontScale = 1.3f` changes a measured text height" on both channels. T6a (the only test author) starts after 041, and implementers do not write acceptance verdicts. The row names no scratch test, no tag-free name, no way to measure the text height, and no steps to remove it afterwards. The `applyOverrideConfiguration` fallback for API 26 (design §5) is not in the row. Reset before/after is written, but 042 does not exist yet (the header says manual). | seams lesson; "implementers never write acceptance verdicts" | Say: the implementer writes a throwaway `…SeamSmokeTest` (no REQ token, `// decision DA-102`), runs it on both channels, and either keeps it as scaffolding or deletes it. Say how the height is read (`onNodeWithTag(…).getBoundsInRoot()` or a `TextLayoutResult`). Add the API 26 fallback and the manual check list (`wm size`, `wm density`, `font_scale`). The orchestrator reads the diff (already in the header). |
| F6 | S | TASK-042 | **The done-check cannot fail.** "Unit tests over canned output" proves the parser, not the device. "A live `--check` on API 37 exit 0 on a clean emulator" passes if `--check` reports nothing at all. The API 26 paths (airplane reset by settings plus `svc`, no overlay) are never run live before MOVE-DEV6. The nav-mode baseline constant for `Medium_Phone_API_37.0` ("observed on the clean AVD when H-1 is written") has no step that records it. | review-report: done-check must be able to fail; E4 | Add, on both channels: induce a leak (`wm size 900x1440`, `font_scale 1.3`, airplane on on API 26) → `--check` exits 1 and lists exactly those → reset → `--check` exits 0. On API 37 also `cmd overlay` probe: enable gestural, read `navigation_mode` 2, restore, read the baseline. Record the baseline constant and the `wm` result on API 26 in the workorder. See also S5. |
| F7 | S | TASK-044 | **The verifier's riskiest part is unnamed and its pre-run is ambiguous.** (a) V-08 must tell a class **defined** in the APK from a class **referenced**. V-04's `parse_dex_strings` reads only the string table (`v04_release_apk.py` line 104), which cannot tell them apart. V-08 therefore needs a `class_defs` / `type_ids` reader, a new hand-written parser with its own fail-closed rows. The row says "imports V-04's dex reader" by pointer to the design and names none of this. (b) "Real release-APK pre-run exit 0": with `--apk <existing APK>` the stale-APK guard (exit 2) will refuse today's APK, because strings and build files are newer. Without `--apk` it builds its own. The row does not say which. (c) `--expect-debug` first runs at 048, so a parser that misses the debug classes shows up at the close (WO-005 F9 again). | hardest first; WO-005 F9 | Name the `class_defs` parse and require fixtures with the same type referenced and then defined (only the definition may fail). Fail closed on a truncated class table. Say the pre-run is `v08_promise_apk.py` without `--apk`, then once with `--apk` on the APK it just built. After 041, add one run of `--expect-debug` on a debug APK to 044's done-check or to MOVE-JVM6. |
| F8 | S | MOVE-JVM6 | **The re-run proof is weaker than the design's input set.** "Edit one scanned file harmlessly" proves one glob at most. The set also holds `Tangrams/*.json` (outside any module), `gradle/libs.versions.toml`, every `strings.xml`, `build.gradle.kts` files and `src/release/**`. A glob set that misses `Tangrams/*.json` or the TOML passes this proof (DA-88 / WO-005 S-set lesson). The revert is by hand with no git: nothing says how a forbidden word is removed with certainty. The row also leaves out V-05, V-06 and V-07 (D-1 step 1) and the three-glob `src/release` case. | DA-88; "done-check can fail" | Prove one file per kind: a `src/main` Kotlin file, `src/release`, a `strings.xml`, the TOML, `Tangrams/*.json`. Each edit makes the test execute (Gradle prints the task as executed, not up-to-date); one forbidden word makes it fail. Back the file up in the scratchpad and compare the hash after the revert. Run V-05/06/07. |
| F9 | S | MOVE-DEV6, TASK-045, TASK-046, T&V6 | **The gate's and the fallback's proof are in the description, not the done-check; the contingents cannot be proven.** (a) The row's done-check is "all green; every `--check` clean; the three language-list results recorded". Missing: the fallback's six counts adding up to the normal run's count (E2 iii, in the description only); the gate's items (`TabletLayoutAppTest`, the five checks); "the first tablet run records the system UI it shows" (design §1); "the three gesture inset values are read from the log". (b) TASK-045's trigger for A2 is "the API 37 inset run", but A2's only test is held-out and first runs at T&V6, after 048. The visible `GestureInsets` case logs and asserts nothing, so the implementer's done-check ("the failing walk green") cannot show the `safeGestures` fix. 045's trigger value ("a mandatory-gesture inset larger than bar + 10 dp") is not in the row. (c) TASK-046 does not name the two call sites (`BrowseTopBar`, `AllPuzzlesOverlay`), the new string in every module (V-05, owner list) or the re-run. (d) 045 and 046 run after CR-5 with no fresh review (only the orchestrator's diff read), and the row says no full re-run. | done-check concrete; contingents specified (check 6) | (a) Move the E2 count rule, gate items and the two "record" lines into MOVE-DEV6's done-check. (b) Add: the orchestrator reads the logged inset values at MOVE-DEV6 and decides 045 before 048; the numeric trigger is in the row; the done-check is the logged value plus a visible assertion on a scaffolding probe (never loosening the held-out test); a breach found at T&V6 re-enters at 045 → 048 → T&V6. (c) Point at design §5 and name the files. (d) Say 045/046 re-run the affected visible suites on both channels and are on CR-5's diff-read list. |
| F10 | S | TASK-040, TASK-047 | **Staffing.** (a) TASK-040 is haiku. The WO-005 plan review F8 moved the same kind of row (TASK-030: test inputs with `fileTree … withPathSensitivity`, `repo.root`, shared definition, a cache-correctness property) to sonnet. Here the globs also have a G-04 constraint (no "devtools" in the file) and the proof only arrives at MOVE-JVM6. A wrong glob is found three rows later. (b) TASK-047 is haiku. Its row does not say the diff is read and it is not on the header's diff-read list (040, 041, 043, 045, 046). It also needs a judgement: which `values-fi` keys are "not already owner-reviewed", and its done-check ("lists every key not already reviewed") cannot fail. | AGENTS staffing lesson; WO-005 F8 | (a) Make 040 sonnet, or keep haiku and add "orchestrator reads the diff against the design's glob list; the `grep -i devtools` over the file is empty" (the grep exists, but the glob content is not checked). (b) Add 047 to the diff-read list, and name the source of "already reviewed" (the WO-005 list, DA-60, DA-80). Add a count check: the list's key count equals `values-fi` keys minus the named reviewed set. |
| F11 | S | header, 048, AUDIT6, Z | **C1–C11 are not enumerated and Z has no owner.** The plan names C1–C11 only as a range. The design wants each part with its target WO written into `build-map.md` §2 at the close, plus the "dev-* exempt / REQ-047 tests do not read the dev dialog" carry. The close items in the design's Z (build-map §2 carries and "V-08 live" in #Promise, `design-inputs.md` §2 gap departure, AGENTS.md's two device-run lines gain V-08 and the reset script, the DoD verifier line, the five-item owner checkpoint) are in no row. `workorders/WO-006.md`'s DoD still lists V-01, V-04, V-05, V-06, V-07 only. | no silent scope (D4); every carry named | Add a "Carried OUT" list to the plan (11 rows, each with its target WO and token) and a close row (or extend 048) that owns the Z edits and the checkpoint. Add V-08 to the workorder's DoD line. |
| F12 | S | plan (whole), MOVE-DEV6, T&V6 | **There is no failure routing.** WO-004 and WO-005 each carried one. Not stated: what happens when the G-DISPLAY gate fails (stop, which task is re-run, who rewrites T6a/T6b), when a held-out test fails (which task, whether 048 is re-run), when a `--check` voids a step twice, and when a staged test fails to compile at move-in (this one is stated: back to its author). | AGENTS failure routing; gates-and-autonomy | Add a short routing block: gate fail → fallback, then T6a/T6b re-sync (F3); product failure → the owning TASK; test-substance failure → the author as a corrected check; harness failure → 042/T6a; held-out failure → the same route, then 048 and T&V6 re-run. |
| N1 | N | ID index | REQ-006.A1 lists 041, which delivers nothing for it. The ID is test only (design). The index also omits 042, 047 and MOVE rows for the enabler IDs. | no silent scope | Remove 041 from REQ-006.A1. |
| N2 | N | 048 | The API 26 release-launch row has no reset or `--check` around it, and the `dumpsys package` line has no pass condition. The step is a device step under E5. | DA-108; design §6 item 3 | State: no requested and no install permission in the output; the line pasted into the workorder; `--check` before and after. |
| N3 | N | TASK-043 | The row is `todo`, but it only runs on a sweep breach. 045/046 are labelled contingent. The T6a/MOVE-DEV6 text "TabletSmallestWalk green once L-1 is in" has no case for "sweep shows no breach, test is green with no code". | clarity | Mark 043 contingent; add the no-breach case. |
| N4 | N | TASK-042, T6a | `device_reset.py --display SPEC` carries its own copy of the five specs, while `DisplaySpec` holds the values too. Two copies of 5 × 5 numbers can drift. | D1 | Add an equality check in 042's self-test against the design table, or say the test author compares them. |
| N5 | N | TASK-T6b | "held-out compiled: yes" is a statement. It cannot be checked without reading the held-out files. | handoff-contract | Ask for the compile command's last result line, and for the scratch set's removal as a listing. The orchestrator checks the isolation by ID (13 IDs, 8 / 5) as in WO-004. |
| N6 | N | MOVE-JVM6, CR-5 | CR-5 is before MOVE-DEV6, so the harness (`DisplayRule` and helpers) is read by no fresh reviewer before device runs. The brief names only product and tool code. The substance audit is at T&V6. | review scope | Add "the helper `after()` paths and the `device_reset.py` leak list" to CR-5, or accept it explicitly. |

## Order and dependencies (the questions asked)

- **Can every task start when the plan says?** Yes, except T6c's equality test (F1), MOVE-DEV6 and T&V6 without their test authors (F2), and the step 1 and step 3 "∥" starts (F4).
- **Sweep decides TASK-043 first.** Yes: T6c puts the sweep first, MOVE-JVM6 reports it, and 043 depends on MOVE-JVM6. Nothing depends on the gap earlier. MOVE-DEV6 depends on 043 "if any". Fine.
- **041's device smoke without 042.** Handled: manual `wm`, `wm density`, `font_scale` checks until 042 exists. The check list is not written in 041's row (F5), and 041's smoke changes no global, so the manual list is enough.
- **G-DISPLAY before held-out tests use a helper.** Not enforced: T6b is written and compiled before the gate (F3). The compile does not depend on behaviour, but a gate failure needs a re-sync rule.
- **`TabletSmallestWalkAppTest` red until L-1.** Handled: outside `HarnessScaffoldingTest`, outside the gate, named in MOVE-DEV6 as green once L-1 is in. Add the no-breach case (N3).
- **One Gradle, one emulator.** The header and the device order (step 6) are right. The starts in steps 1 and 3 are not (F4). Device rows are serialized 041, 042 live, MOVE-DEV6, 045/046, 048, T&V6. Good.

## Done-checks (the questions asked)

| Item | Verdict |
|---|---|
| TASK-041 smoke, both channels | Concrete outcome, but no test, owner or measurement method (F5) |
| TASK-044 real-APK pre-run | Ambiguous (stale guard) and the class-definition parser is unnamed (F7) |
| MOVE-JVM6 re-run proof | One file only; no hash-checked revert (F8) |
| MOVE-DEV6 fallback and count reconciliation (E2) | In the description only (F9) |
| E5 voiding rule | In the header, applied by 041 and 048 only by implication; add to MOVE-DEV6 and T&V6 rows (F9, N2) |
| TASK-048 evidence lines | Good: V-04 exit 0, V-08 exit 0, `--positive-control` exit 0, `--expect-debug` exit 0. Missing: the `dumpsys` pass condition and the `--check` pair (N2) |

## Staffing

- **sonnet** for 041, 042, 044, T6a/b/c, 043, 045, 046, CR-5, T&V6, AUDIT6: right.
- **haiku** 040 and 047: see F10.
- **Diffs the orchestrator reads:** the header says 040, 041, 043, 045, 046. Add 047 (haiku), 042 (device I/O, no behaviour in the done-check beyond canned output until F6), and 044 (verifier; the done-check can exercise it, so only if F7 stays open).

## Release safety

- **G-04 / DA-89:** 040's done-check carries the grep over S. Gap: it runs once at 040. The globs are in a file that 045/046 do not touch, but the `LocaleOverrideActivity` or `TestConfig` text must never land under `app/src/main` or `app/src/release`; S does not cover `app/src/debug`. The only proof is V-08 (release) at 048. 041's own `assembleRelease` plus V-04 is right.
- **Debug-only classes:** V-08 denies them in release and `--expect-debug` requires them in debug. First proof of `--expect-debug` is at 048 (F7c).
- **V-04 and V-08 at the close:** both in 048. V-08 builds its own release APK, so the API 26 copy must be signed from the APK V-08 just built. Say so in 048 (N2).
- **Gradle input globs:** the re-run proof is the only check that the globs are complete (F8).

## Risks the plan does not mitigate

The design lists these (§Risks). The plan has no early probe for any of them. All first run at the gate in MOVE-DEV6, after every implementation task and CR-5.

1. **`wm` on API 26.** `wm size`/`wm density` acceptance, the 2x width clamp and the mid-process density change are first seen at the gate. A one-minute manual probe on `Phone_API_26` during 041's smoke (or 042's live check) costs nothing. See F6.
2. **Locale wrapping on API 26.** Covered by 041's both-channel smoke. The fallback (`applyOverrideConfiguration`) is not in the row (F5).
3. **`cmd overlay` refused.** The design says the test fails and the orchestrator sets the mode by hand. The plan has no row for it, and `device_reset.py` needs the same command to restore the baseline. If refused, reset cannot restore, and every `--check` after a gesture step voids the step. Probe in 042 (F6).
4. **Airplane restore.** First live use is `AirplaneModeAppTest`, after the radios on API 26 are changed by `svc`. A leaked airplane state would void every later step. F6's induced-leak check covers it.
5. **Device time.** The design budgets 25 minutes per channel and 12 to 18 extra for the fallback. The plan has no stop rule, so a doubled run is found only at the end. Add a measured time to MOVE-DEV6's record and a decision point (trim the grid scroll first, per the design).
6. **Held-out compile-early rule (new).** Stated in the header and T6b's done-check. Not checkable (N5), and it conflicts with the G-DISPLAY ordering (F3).
7. **Contingent tasks 045/046.** Under-specified (F9).
8. **The new `Parameterized` and `tests_regex` toolchain.** Only used in the fallback; first use is unproven. If the gate fails, the fallback is the first run of it.

## No silent scope (D4)

Every row traces to a REQ, a DA number or G-01/G-04. No row adds scope. Task 042 (DA-108), 044 (DA-104) and 047 (DA-106, owner list) are enablers and say so.

## Handoff — Plan Reviewer · WO-006

- **Scope:** `tasks.md` WO-006 section (plan v1.7), 13 IDs, carries C1–C11.
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-006-plan-review.md`
- **Status:** **recirculate → planner** (1 Blocker / 10 Shoulds / 6 Nits).
- **Traceability delta:** none. All fixes are edits to `tasks.md`, plus the T6b/T6c briefs and a close row.
- **Notes for next station:** after the planner's edits, a spot-check of F1, F2, F3, F6, F7 and F9 is enough. Do not brief the Acceptance Test Author until F1 and F3 are fixed, since both change what T6a, T6b and T6c are told.

## Spot-check of v1.8

**Date:** 2026-10-04 · **Read:** `tasks.md` "## WO-006" v1.8 in full (header notes, all rows, Carried OUT, failure routing, index, order, change log) and the DoD line of `workorders/WO-006.md`. Only the changes were checked. No Gradle, emulator or git; `.swdev/heldout/` not read.

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (B) | **fixed** | `PromiseWordListEqualityTest` is staged separately, excluded from MOVE-JVM6 and moved in at MOVE-DEV6 (after T6a). T6c names both file paths. MOVE-JVM6's done-check no longer needs it |
| F2 | **fixed** | MOVE-DEV6 depends on T6a. T&V6 depends on T6b. T6b depends on T6a |
| F3 | **fixed**, residue | The marker FQN is a named seam, and T6b carries `@Parameters(name = "{0}")` and `displayPreset`. The kit re-sync and the normalising compare are in the header and in T&V6's input check. Residue: E2 |
| F4 | **fixed**, residue | The strict order is stated. 040 runs alone, 044's pre-run follows 040, 041 follows that, then 042 live, then PRE-V08. T6a compiles before T6b. Residue: E5 |
| F5 | **fixed** | `LocaleSeamSmokeScaffoldingTest` is named, with a `boundsInRoot` height check (≥ 1.2×), kept as scaffolding. The API 26 `applyOverrideConfiguration` fallback is in the row. The manual hygiene list is in the header |
| F6 | **fixed** | 042 has the induced-leak run on both channels (exit 1, then 0), the `cmd overlay` probe with a stop rule, the API 26 `wm` probe, the baseline constants recorded, and the spec-table equality check (N4) |
| F7 | **fixed**, residue | The `class_defs` reader, fail-closed rows, referenced-then-defined fixtures, and two pre-runs (no `--apk`, then `--apk` on the built APK) are in. PRE-V08 gives the `--expect-debug` pre-run. Residue: E4 |
| F8 | **fixed** | MOVE-JVM6 has one edit per input kind (five), "executed, not UP-TO-DATE / FROM-CACHE", one forbidden-word FAIL, scratchpad backups with a SHA-256 compare, and V-05/06/07 |
| F9 | **fixed**, residue | The gate items, the fallback count rule, the system UI, the three inset values and the device time with a stop rule are in MOVE-DEV6's done-check. 045 has a numeric trigger. 045/046 re-run suites and name CR-5b. 046 names the call sites. Residue: E1, E3 |
| F10 | **fixed** | 040 is sonnet with a glob read and a `grep -ci devtools` = 1. 047 is on the diff-read list and has a two-number count check |
| F11 | **fixed** | C1–C11 are listed with target WO and token, plus the WO-005 carry. CLOSE6 owns the Z items and the checkpoint. The workorder DoD names V-08 |
| F12 | **fixed** | The failure-routing block covers the gate, product, substance, harness, held-out, twice-voided and compile cases |
| N1–N6 | **fixed** | The index drops 041 from REQ-006.A1. 048's API 26 row has the reset pair and the `dumpsys` pass condition. 043 is contingent with the no-breach case. 042 compares its specs. T6b/T6a hand back the compile line and the removal listing. CR-5 reads the harness `after()` paths |

**Order versus per-row dependencies:** consistent. 044 → 041 → 042 live → PRE-V08 → T6a → T6b matches the Depends column. MOVE-JVM6 waits for 041's build only in the order text (E5). CR-5 depends on PRE-V08 and T6a. 048 and T&V6 chains hold. One Gradle build and one emulator at a time holds on every device row.

**New Blocker:** none.

### E-items

- **E1 — T6a `HarnessScaffoldingTest`, MOVE-DEV6, TASK-045 (Should).** The new visible `GestureInsets` probe asserts "tray bottom ≤ exclusion top", and it sits inside `HarnessScaffoldingTest`, which is the G-DISPLAY gate. A real REQ-035 A2 breach (the very case 045's `safeGestures` fix is for) would fail the gate, and the failure routing would send the orchestrator to the fallback. This is the E6a mistake again (a product breach blocking a harness gate). Fix: keep the probe's logging in the gate, and put the asserting probe in its own class, outside the gate (like `TabletSmallestWalkAppTest`), named as red until 045.
- **E2 — header "kit compare script" (Nit).** No row owns the writing of the normalising compare script (package stripped, "equal / differs" only). Name the author (T6a's author or the orchestrator) and where it lives. T6b also needs the marker annotation file in its scratch compile set, since T6a's copy is removed before T6b compiles; say so.
- **E3 — CR-5b (Nit).** It is named in 045, 046, 048 and the cut, but has no row. Add one row (fresh sonnet spot-check of the 045/046 diff), with dependencies 045/046.
- **E4 — PRE-V08 (Nit).** The row runs `--apk app/build/outputs/apk/debug/app-debug.apk` but never says to build it ("a fresh debug APK"). Add `:app:assembleDebug` as the first step. It is a build, not an emulator row, so remove it from the header's device-row list.
- **E5 — MOVE-JVM6 (Nit).** The order text says it waits for 041's build; the Depends column lists only T6c and 040. Add TASK-041, so T6c's author compile and MOVE-JVM6 cannot overlap with another build.

**Spot-check verdict: forward.** The planner can brief the Acceptance Test Author and the implementers. E1 is the one to apply before T6a is briefed, because it changes T6a's class split. E2 to E5 are one-line edits.
