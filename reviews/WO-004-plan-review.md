# Review — plan · WO-004

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (plan-reviewer)
**Inputs:** `tasks.md` (section "WO-004": TASK-T4, TASK-021…029, CR-2; WO-003 section and change log for shape) · `workorders/WO-004.md` · `designs/WO-004-design.md` (rev 2: "Test seams", acceptance table, "Suggested cut", DA-46…66) · `reviews/WO-004-design-review.md` (with spot-check E1–E6) · `reviews/WO-003-plan-review.md` · AGENTS.md (WO-003 lessons) · `governance.md` rows 8, 11, 12, 13 · `.swdev/guard.json` · `app/build.gradle.kts`, `settings.gradle.kts` · `app/src/**` file list and a grep of the WO-003 app tests · `decisions.md` (DA-46…66 are logged). `.swdev/heldout/` not read (it does not exist yet; `.swdev/staged/WO-004` does not exist yet either).

## Verdict

**recirculate → planner (orchestrator).** No Blocker. The shape is right:
- the cut follows the design's seams, and the order is sound (build step, kernel and `store`, `play` and `browse` logic, mandatory CR-2, then the UI, then `app`, then checks);
- the compile dependencies are correct (`browse` needs kernel and contracts only, `play` needs kernel, `app` needs all);
- the WO-003 lessons are mostly applied (sonnet for UI, diff reading, parallel only across modules, DA-62 written down).

But eight Shoulds remain. Most repeat a WO-003 plan-review class:
- A-ID Serves cells and the ID index are missing (F1).
- The staged tests move in only at the very end (F5).
- The UI done-checks run no device test and the emulator precondition is gone (F6, F7).
- The DA-56 split is incoherent (F2).
- The frozen v1 fixtures are authored by the one who writes the codec (F3), and the guard-lock step is vague (F4).
- Tasks 022 and 023 are parallel across a kernel edit (F8).

All fixes are edits to `tasks.md` (plus one write-back line in the design).
**0 Blockers, 8 Shoulds, 5 Nits.**

## Checklist applied

- [x] **Directives.**
  - D1: one seam per task; 026 ∥ 027 are different modules.
  - D3: every row names REQs or an enabler guardrail. DA-56 and DA-57 are named in rows.
  - D5: no new library; the build step is one row.
- [x] **Guardrails.** G-01 (manifest untouched), G-05 (027), G-06 (021 plus V-06), G-09 (023), G-10 (023, 025). The G-09 conflict (DA-64) is named in row 023.
- [x] **Contract.** `IProgressStore` is implemented without a signature change. The guard lock of the frozen fixtures is in the plan (details in F4).
- [x] **Scope.** Nothing unrequested.
- [x] **Mode and toolchain (G3).**
  - Full is justified (new persistent format, first implementation of a governed interface, two new modules).
  - Channels:
    - JVM and the API 37 device channel are proven by WO-003.
    - The `store` JVM channel is trivial.
    - The waiver (API 26) is recorded in `WO-004.md`.
    - The one new thing is `browse` as a new instrumented module: nothing proves its wiring before 029 (F7).
- [x] **Evidence re-derived.**
  - Seam map. Every seam of the design's "Test seams" table has exactly one delivering row:

    | Task | Seams delivered |
    |---|---|
    | 022 | `isValidPlacement`, `onRestart`/`onRetry` |
    | 023 | `JsonProgressStore`, both frozen and five unfrozen fixtures |
    | 024 | `PlaySession` additions |
    | 026 | `PlayArea` slot, `PuzzleThumbnail`, `forThumbnail` |
    | 025 | `PuzzleHost`, `BrowseController`, `ProgressRestore` |
    | 027 | UI composables and tags |
    | 028 | `SessionHost`, `AppViewModel`, `TangramApp` |

    No seam is orphaned. The `D-n` to `TASK-0nn` mapping is implicit in the row text and not written back into the design (N, F13).
  - WO-003 app tests. A grep shows none of them references `AppViewModel`, `TangramApp` or the `puzzle` extra. So 028's new signatures do not break their compilation; they can only fail at run time (persisted state), and only on the device (F9).
  - The design's `FakeProgressStore` and `FakeHost` are T4's, but row 025 asks the implementer for `browse/src/test` "with FakeProgressStore / FakeHost" (F5).
  - Play JVM stands at 146 (STATUS), device at 103, so "≥ 146" is right. The WO-003 play device suite is the regression net for 024 and 026 (F6).
  - `settings.gradle.kts` lists five modules; `app/build.gradle.kts` still has the `beforeVariants` stanza that exists only for `PuzzleExtraReleaseScaffoldingTest` (F2).
- [x] **The conceptual 20 %.** Spent on the DA-56 split, who authors the frozen fixture, when the staged tests move in, and the device checks of the three UI tasks.

## Trajectory & quality

- **Verification actually run?** This is a plan; its commands are the WO-003 ones, proven. The new channel (`browse` instrumented) is not proven in the plan (F7).
- **Proportionate?** Yes: ten rows against a medium WO, no oversized task.
- **Path sane?** Yes. Hardest-first is respected (format and restore before any UI, CR-2 between).

## Findings

### F1 — TRACE · Should · tasks.md WO-004 Serves column; no ID → task → T4 index (WO-003 plan review F1 again)
- **Observation:** Several rows name REQs, not acceptance IDs, and the plan has no ID → task → T4 index, which WO-003 got in v0.9 for exactly this reason. Coverage holds at REQ level, but at A-ID level these owners are missing:
  - 025 serves "REQ-003, REQ-024" without A-IDs, and "REQ-050 A1" only. The covering test of **REQ-050 A3** (`open(j)` writes only the leaving puzzle and `lastShown`) is in `browse/src/test`, so the implementing task is 025, but A3 is named only on 027 ("on screen").
  - **REQ-025 A2** has an on-screen half, the `RestartButton` pill, in 027. 027's Serves does not name REQ-025 at all.
  - **REQ-025 A1** on screen (close and relaunch the activity) is delivered by 028 (`onPause`, the ViewModel reading the store), and 028 does not name it.
  - T4's row says "every ID in the design's acceptance table" without listing them (16 IDs: REQ-003 A1–A2, 024 A1–A3, 025 A1–A3, 026 A1–A2, 050 A1–A3, and 040/041/045 A1).
- **Proposed resolution:** Replace the Serves cells with A-ID cells. Add one index line, "ID → code task(s) → T4 file", and re-check both directions from it. Say that `start()` rows (iii) and (iv) and the `// decision F5` rows carry no REQ token.

### F2 — DEP · Should · TASK-021 / TASK-028 (DA-56 `beforeVariants` removal) and the "only build-file editor" rule
- **Observation:**
  - Row 021 says "drop the stanza only together with TASK-028's DA-56 removal (leave it until then, note in the hand-back)". Row 028 says "tell TASK-021's owner to drop the stanza in the same change".
  - But 021 is `done` long before 028, and 021's owner is a finished sub-agent. The result is a dangling instruction: either 028's implementer edits `app/build.gradle.kts` (breaking the rule that only the build step edits build files) or nobody removes it.
  - The order matters. The stanza can go only after `PuzzleExtraReleaseScaffoldingTest` is deleted (otherwise `:app:testReleaseUnitTest` has a test but no variant); and the files can go only after `MainActivity` stops calling `PuzzleExtra`, which is 028's own rewrite.
- **Proposed resolution:** Pick one:
  - (a) A tiny **TASK-028b build step**, serial after 028 (dispatched by the orchestrator, haiku is fine), whose only job is to remove the stanza, with the done-check `grep -c beforeVariants app/build.gradle.kts` = 0 and `assembleDebug test` green.
  - (b) Let 028 own the single-stanza edit and record it in 028's row as a named exception to the build-step rule.

  Either way: remove the cross-reference from 021, add a done-check to 028 that `PuzzleExtra` is gone from `app/src/**`, and let the final `assembleDebug test` prove the release variant still builds.

### F3 — TEST · Should · TASK-023 / TASK-T4 (who authors the frozen fixture)
- **Observation:**
  - Row 023 has the implementer author `progress-v1.json`, `progress-v1-fresh.json`, the frozen-fixture test, **and** the SHA-256 pins, and its done-check is "fixture SHA-256 pins match". The same agent writes the codec and the fixture that defines the format, then pins the hash of that fixture in its own test.
  - That is circular: the guard "the reader loads each frozen file to its expected values" can pass because the fixture was fitted to the code, and the pin proves only that the file did not change after it was written.
  - The design fixes the document verbatim in §2 ("D-2 may change numbers, never keys"), so it can be authored independently of the codec. `progress-v1-fresh.json` follows from the writer rules (explicit nulls, default settings) in the same section.
  - Independence matters most here: this is the one irreversible choice of the WO (hard-stop 2).
- **Proposed resolution:**
  - T4 authors the two frozen JSON files, the frozen-fixture test (expected values, two-way tree check, world-corner assertion, SHA pins) and stages them. The orchestrator moves them into `store/src/test` **before** 023 is dispatched.
  - 023's implementer must make the codec pass them unedited, and may not write the frozen files.
  - The unfrozen resources stay with 023.
  - This changes the seam's delivering task (design table row 2 says D-2): write the change into the design and `decisions.md` before T4 starts (AGENTS WO-003 seam lesson).

### F4 — GUARDRAIL · Should · TASK-023 guard-lock step (E5), timing against CR-2
- **Observation:**
  - The row says the orchestrator locks "exactly the two frozen fixtures + the frozen-fixture test" but does not write the three literal paths. It does not name the test file at all (the design says "the frozen-fixture test file" and, in another sentence, "the hash pin and the expected-values test file", which may be two files).
  - The lock is in the description, not in the done-check. Nothing verifies that `guard.json` and the baseline changed, or that `decisions.md` has the entry.
  - 029's "fixture guards" check does not re-check that the lock is still in place.
  - The lock lands before CR-2, whose stated purpose is to review "the format". A CR-2 finding on the codec or the format then needs an edit to a locked file, which is a hard stop for the human.
- **Proposed resolution:**
  - Write the literal paths: `store/src/test/resources/fixtures/progress-v1.json`, `store/src/test/resources/fixtures/progress-v1-fresh.json` and the one frozen-fixture test file (name it now). Say which file holds the pins.
  - Add to 023's done-check: `guard.json` `contract_paths.locked` contains exactly those three entries (no glob), the baseline is re-frozen, and the `decisions.md` entry exists.
  - Choose and write down the timing: lock after 023 as the design says and accept that a CR-2 format change is a hard stop (say so in the CR-2 row), or lock after CR-2 forward, which gives the format its review before the freeze. I prefer the second, but the design chose the first.
  - Add the lock check to 029.
  - The migration-test requirement (governance row 11) needs no task now, since v1 has no migration. Say so in one line: the frozen files are the base for the first v2 migration test (see F13).

### F5 — TEST · Should · TASK-029 (the only move-in), CR-2 dependencies, fakes collision (WO-003 F4 class)
- **Observation:**
  - Every staged acceptance test (JVM and device) moves in at TASK-029, after 028. The WO-003 history is that the JVM move-in exposed adapter and signature defects (the `badgeRect` nullable seam, the silent adapters, the missing `PieceDrawing.scale`), and that is why WO-003 moved the JVM tests at 017 and the play device tests at 018b.
  - Here a seam mismatch in `store` (022 and 023) or `browse` (025) reaches CR-2 and the UI tasks unseen, and then fans out at 029 across five implementers.
  - CR-2 reviews "the format, the restore path and the save ordering" with no independent acceptance test having run.
  - CR-2's dependencies list 022…025 but not T4, so nothing guarantees the staged tests exist by then.
  - Collision: row 025's description says its scaffolding tests run "with FakeProgressStore / FakeHost". The design assigns those two fakes to T4 (`browse/src/test`). If the implementer writes classes with the same names, the move-in fails with duplicate declarations; if it does not, its tests need other fakes.
- **Proposed resolution:**
  - Move in the JVM staged tests (store, browse, plus the kernel/play ones if any) after 023/025 and before CR-2. Make T4 a dependency of CR-2.
  - Move in the device tests at the task that creates their API: `play` device tests at 026, `browse/src/androidTest` at 027, the `app` tests at 029.
  - Give the implementers' own fakes in 025 different names (for example `StubStore`, `StubHost`), or have the orchestrator move T4's two fakes in first and tell 025 to use them.
  - Repeat the WO-003 paragraph: implementers build the frozen signatures verbatim and may read `.swdev/staged/WO-004/**` (never `heldout`).

### F6 — TEST · Should · TASK-026, 027, 028 done-checks; emulator precondition missing
- **Observation:**
  - The WO-004 header carries no emulator paragraph (WO-003 had it: the orchestrator starts and stops `Medium_Phone_API_37.0`, otherwise the task hands back `implemented (unverified)`). It does not repeat the `JAVA_HOME`/`ANDROID_HOME` note either (that one is in AGENTS.md).
  - 026 changes the drawing rule when SOLVED, and 024 makes `state`/`isDragging` observable. Both touch the code behind the 103 WO-003 play device tests, and the design's own risk list says "WO-003 tests re-run". Their done-checks are JVM or assemble only.
  - 027 holds the device-only behaviour (the long-press with the test clock, the 48 dp areas, the grid overlay covering the top bar) and its done-check runs nothing on a device. "Diff read by the orchestrator" is the stated fallback, which the AGENTS lesson allows only when the done-check **cannot** exercise the behaviour; here it can, once F5 moves the tests in.
  - 028's done-check is `assembleDebug test`, which runs no device test either.
- **Proposed resolution:**
  - Add the emulator precondition paragraph to the header.
  - Add `:play:connectedDebugAndroidTest` (≥ 103 green) to 026.
  - Add `:browse:connectedDebugAndroidTest` with the moved-in staged tests to 027. If the emulator is not up, the task is `implemented (unverified)`.
  - Add `:app:connectedDebugAndroidTest` to 029 only, with the F9 note.

### F7 — TEST · Should · TASK-021 done-check (the new instrumented module is not proven)
- **Observation:** `browse` is the first new instrumented module since `play`. WO-003 plan review F7 closed by making the build step compile both androidTest variants and run one trivial instrumented test. 021 runs `assembleDebug test` and V-06 only. A wiring error (runner, `ui-test-manifest`, the espresso 3.7.0 pin of DA-39, the `content` test dependency) would surface at the first move-in of `browse/src/androidTest`, in a feature implementer's context.
- **Proposed resolution:** Add `:browse:assembleDebugAndroidTest :app:assembleDebugAndroidTest` to 021's done-check, plus one trivial instrumented test in `browse` run once on the API 37 AVD (scaffolding, deleted when the real ones arrive). Say that `browse/src/main` needs its manifest for an empty module to build.

### F8 — DEP · Should · TASK-022 ∥ TASK-023 (a kernel edit under a compiling dependent)
- **Observation:** The "parallel only across modules" rule is respected on paper, but `store` depends on `kernel` and 022 edits `kernel` main (new members in `kernel.lock` and `kernel.state`). While 022 is mid-edit, 023's `:store:test` compiles that kernel source set; a half-written file breaks the other agent's build with an error it did not cause. WO-001's plan serialized the kernel tasks for exactly this reason ("a half-written file of one would break the other's `:kernel:test`"). Two Gradle invocations in one checkout also contend for the build lock.
- **Proposed resolution:** Serialize: 023 depends on 022 (022 is small, so the cost is low). Alternatively keep the parallelism and state that 022 lands its kernel files in one atomic write each and that a 023 build failure that points into `kernel` is retried. The 024 ∥ 025 pair is fine: both compile a kernel that 022 has finished.

### F9 — TEST · Nit · TASK-028 / TASK-029 (the DA-62 red interval)
- **Observation:** Row 028 says "must NOT edit the WO-003 app tests (they fail until TASK-T4's amendment moves in)". That is the only place the expected red interval appears. It is not in "Order", in 029, or in "Failure routing". 028's done-check cannot show it (JVM only). And the failure routing says an implementation cause goes to the owning implementer: a reader who runs the device tests after 028 could route the four expected-red WO-003 tests (they assume a fresh first puzzle; with persistence the first test solves puzzle 1 and the later ones see Solved) back to 028's implementer as a defect.
- **Proposed resolution:** State once, in "Order" and in 029: "Expected red interval: from 028's hand-back to 029's move-in, the WO-003 `app/src/androidTest` tests may fail on the device because the app now persists. This is not a defect and not routed to 028. At 029 the orchestrator first reads and moves in T4's setup-only amendment (outer `ResetStoreRule`, assertions unchanged), then runs them." Also say that T4's amendment is a setup-only diff read by the orchestrator, listed on the close "corrected checks" line (already in T4; repeat in 029).

### F10 — TRACE · Nit · rows 025, 027 (items the design names but the rows omit)
- 025 does not carry the DA-57 `check(isNotEmpty())` with a clear message, a test (an empty library gives a clear `IllegalStateException`), or the `puzzles`/`start()` values-undefined rule. 028's Serves names DA-57, which is the wrong owner.
- 027 omits the one-line ellipsis of the title (N4's title half) and the scaffolding bounds test of the Restart touch area (`// guardrail`, no REQ token).
- 027 says "16 string keys"; the design's §8 table has 17 rows. Say "the §8 table".
- Fix: add these to the owning rows.

### F11 — TEST · Nit · TASK-T4 held-out weighting
- **Observation:** Named held-out IDs are REQ-025 A3, REQ-050 A1, REQ-050 A3, REQ-026 A1, so 4 of 16 (25 %), below "about 1 in 3" (5 or 6). The risk weighting is right for restore, the lookup and the REQ-026 pixel rule, but the design's own highest risk is the save and relaunch round trip, and **REQ-025 A1** (pieces come back after a close and a relaunch) is not in the list. The row does not require "no overlap with the visible tests", or that the held-out is self-contained with loud adapters.
- **Proposed resolution:**
  - Add REQ-025 A1 (a `store` reopen plus the device relaunch if self-contained) and aim for 5 or 6 IDs.
  - REQ-024 A3 (wrap) is the cheap sixth.
  - State "no ID has both a visible and a held-out test".
  - Repeat the WO-003 held-out paragraph: adapters go into the Test & Verify brief because the orchestrator cannot pre-audit `.swdev/heldout`.

### F12 — QUALITY · Nit · staffing and review scope
- The header names sonnet for "format, I/O, lifecycle and UI", which covers 023–028, but no row states a model. 022 is the exception: the design calls it "haiku is fine", yet it carries the DA-63 safety proof (mixed dyadic denominators, opposite-sign pairs, `READING_ORDER` called directly) and the content check, and WO-003 had a rejected haiku attempt. Put "sonnet" in row 022 and state per row which tasks are haiku-eligible (021, 028b).
- CR-2 covers 022…025 only. The `onPause` / `onChanged` / `onStop` ordering, which the design's second risk names, is wired in 028 and reviewed only by the orchestrator's diff read and the close code review. Say that the close Code Reviewer reads 026–028 and the `app` wiring explicitly.

### F13 — TRACE · Nit · write-back and carried items
- The design's seam table still says `D-n`; AGENTS.md (WO-003 lesson) says every seam names its delivering task at planning. Add one line to the design (or to `tasks.md` under the table): `D-0→021, D-1→022, D-2→023, D-3→024, D-5→025, D-4→026, D-6→027, D-7→028, D-8→T4, D-9→029`, plus whatever F3 changes (the frozen fixtures move to T4). The design header still says "rev 1"; tasks says "rev 2".
- "Carried OUT" omits two design notes: WO-007/008's design authors must check REQ-029–034 against the v1 field table (any gap is a v2 and a hard-stop migration with a mandatory migration test against the frozen v1 files, governance row 11), and WO-008's `shownBestSeconds` observability and time format is there already. Add the first.
- `WO-004.md` "Contract deltas" is empty; the one behavioural note (disk failure, DA-47) and the logged DA-64 conflict belong there at close. A reminder row in 029 is enough.

## Answers to the review brief, in short

1. **Coverage.** Every ID lands on an implementing task at REQ level and on T4. A-ID gaps: F1. Every seam has one delivering task, so none is orphaned (F13 asks for the explicit mapping line).
2. **Dependencies.** Compile dependencies are right; 026 ∥ 027 and 024 ∥ 025 are different modules and fine. 022 ∥ 023 is not (F8). The build step is the only editor except the DA-56 stanza, which is incoherent as split (F2); the clean fix is a tiny 028b build step.
3. **The v1 freeze.** The guard-lock step is in the plan but without the literal paths or a done-check (F4). A frozen fixture that defines the format should be authored by the independent T4, not by the codec's implementer (F3). The migration-test requirement needs no task now; record the base in one line (F4, F13).
4. **Done-checks.** `:store:test` is right for a Kotlin/JVM module, `:browse:testDebugUnitTest` for the Android library. Missing device checks: F6, F7.
5. **DA-62.** Present in 028, but the red interval is explained in one parenthesis only (F9). The WO-003 app tests do not compile against anything 028 changes, so the red is runtime only.
6. **Held-out.** About 25 %, risk-weighted except the relaunch round trip (F11).
7. **Staffing and CR-2.** CR-2 placement (after the JVM logic, before the UI) is right; its dependencies lack T4 (F5). Staffing per row: F12.
8. **Dropped from the design.** DA-47 `onStop` sync (028, present), the `STATUS.md` line (029, present), the content dyadic check (022, present). DA-57 N4 is only half-present (F10).

## Handoff — Plan Reviewer · WO-004
- **Scope:** REQ-003, 024, 025, 026, 050 + carried REQ-040/041/045 A1 on screen, DA-26/DA-56/DA-57, G-09 · governed: `IProgressStore` (notify, first implementation), `IPuzzleLibrary` (consumed)
- **Inputs read:** see the header
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-004-plan-review.md`
- **Status:** recirculate → planner (0 B / 8 S / 5 N). Shoulds:
  - F1: A-ID Serves cells and the ID → task → T4 index are missing (REQ-025 A1/A2, REQ-050 A3, REQ-003/024 bare)
  - F2: the DA-56 stanza removal is split incoherently between 021 and 028
  - F3: the frozen v1 fixtures are authored by the codec's implementer; T4 should author them
  - F4: the guard-lock step lacks literal paths, a done-check and a timing decision against CR-2
  - F5: all staged tests move in only at 029; move the JVM tests in before CR-2 (T4 a dependency) and the device tests at 026/027; fakes name collision
  - F6: 026/027/028 run no device tests and the emulator precondition is missing
  - F7: 021 does not prove the new `browse` instrumented wiring
  - F8: 022 ∥ 023 is a kernel edit under a compiling dependent
- **Traceability delta:** none (review only); fixes change the task-to-ID links and, via F3, the delivering task of the frozen fixtures.
- **Notes for next station:** a spot-check of F1–F8 is enough. Apply F3's seam change to the design and `decisions.md` before T4 starts.

## Spot-check (re-review)

**Date:** 2026-10-03 · **Read:** `tasks.md` WO-004 section, plan v1.2 (all rows, header, index, Order, failure routing, close, Carried OUT). Checked only the changes the coordinator listed.

**Verdict: forward.** F1–F13 are closed. One Note, non-blocking.

| Finding | Status | Evidence |
|---|---|---|
| F1 | closed | A-ID Serves cells everywhere; ID → task index; D-n → TASK map. REQ-050 A3 → 025/027, REQ-025 A2 → 027, REQ-025 A1 → 028 are named. Both directions re-checked, no gap |
| F2 | closed | TASK-028b (haiku) drops the stanza after 028; 028 deletes files only; 021 no longer cross-refers |
| F3 | closed | T4a authors the two frozen JSON files and `FrozenV1FixtureTest.kt` from design §2; 023 must pass them unedited; DA-67 logged |
| F4 | closed | LOCK-V1 after CR-2 forward, literal three paths, done-check (guard.json, baseline hashes, no trace drift) |
| F5 | closed | MOVE-JVM before CR-2, CR-2 depends on it and on T4 (through MOVE-JVM); `Scaffold…` names; device move-ins at 026/027/MOVE-APP |
| F6 | closed | Emulator paragraph in header; device done-checks in 026, 027, MOVE-APP |
| F7 | closed | 021 compiles `:browse:assembleDebugAndroidTest` and runs a trivial instrumented test |
| F8 | closed | 022 → 023 serialized (but see the parallel ruling below) |
| F9 | closed | Red interval in 028's row and in Order |
| F10–F13 | closed | Rows carry DA-57 + test, ellipsis, bounds test, 17 keys; Model column (022 sonnet); close scope; Carried OUT migration note |

**Ruling on TASK-023 ∥ 024 ∥ 025: sound.** After 022 the kernel is finished and nobody edits it again until CR-2. `store` compiles kernel + contracts, `play` compiles kernel, `browse` compiles kernel + contracts (+ `content` in test scope). None of the three depends on another, and none touches a shared build file (021 is done). The done-checks are all module-scoped (`:store:test`, `:play:testDebugUnitTest`, `:browse:testDebugUnitTest`), so none compiles `app`, which depends on all three. That is the F8 hazard, and it is absent. The orders also hold on the reverse: 026/027 wait for LOCK-V1, which waits for CR-2.

**Note (non-blocking, N14):** three concurrent `gradlew` calls in one checkout contend for the Gradle build lock and may time out. Tell the implementers to use only the module-scoped commands in their rows (never root `test` or `assembleDebug`) while the three run, and to retry once on a lock timeout. Also, 027's "then the orchestrator moves in the staged `browse` device tests" should happen before the implementer's device done-check runs (say "before dispatch").
