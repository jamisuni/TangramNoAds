# Review — design · WO-008

**Date:** 2026-10-05 (begun 2026-10-04, resumed after the overnight stop; nothing in the repo changed in between)  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:**
- **The design and its frame:**
  - `designs/WO-008-design.md` (rev 0, 2026-10-04), read in full;
  - `workorders/WO-008.md`;
  - `AGENTS.md` (rules 1–13; the seams, test-adapter, token, cross-file, solve-by-touch, release-safety and two-channel duties);
  - `build-map.md` §1–2 (the #PlayTime row, the WO-005/006/007 carry rows);
  - `architecture.md` v1.0 (G-04, G-06, G-09, G-10, §3 module table, O-03, O-04, O-06, O-08, O-09);
  - `governance.md` rows 11, 12, 14 (and 13, 15);
  - `req_review_01.md` F28, CA-3, CA-9.
- **Requirements (read in full):** REQ-005, 029, 030, 031; TYPE-005 and TYPE-006 (`Requirements/req_types.md`); carried REQ-001, 008, 010, 026, 032, 034, 037, 046.
- **Explanatory inputs:** `Spec/01-gameplay.md` §8, `Spec/02-ui-layout.md` (the play-screen sketch and element table), `Study/07-play-time-and-donations.md` (frozen, read only), `tools/prototype_template.html` (VERSION 0.6: `fmt`, the 1 s `setInterval`, `lastTouch`, the timer and Restart placement, the settings stats).
- **Decisions:** F3, F4, F5, F28, DEF-001, DA-46…DA-77 (DA-49, 52, 59, 66, 70, 71, 72, 73, 74, 75, 77 in particular), LOCK-V1, DA-92, DA-97, DA-113…DA-132.
- **Designs:** `designs/WO-007-design.md` (through its review and spot-check), the WO-006 carried table (C4, C6, C7, C9).
- **Code (working tree as of the WO-007 close):**
  - `contracts`: `SavedGame.kt`, `IProgressStore.kt`, `Puzzle.kt`.
  - `store`: `JsonProgressStore.kt`; `progress-v1.json`; `FrozenV1FixtureTest.kt`.
  - `play`: `PlaySession.kt`, `PlayArea.kt`, `PlayLayout.kt` (`computeWithCorner`), `PlaceSecondary.kt`; `PlaceSecondarySweepTest.kt`, `CornerControlSweepScaffoldingTest.kt`.
  - `browse`: `BrowseController.kt`, `ProgressRestore.kt`, `SolvedBar.kt` (`RestartButton`), the strings.
  - `settings`: `SettingsController.kt`, `build.gradle.kts`; every `SettingsController(` call site (5).
  - `app`: `SessionHost.kt`, `AppViewModel.kt`, `MainActivity.kt`, `TangramApp.kt`, both `DebugAids.kt` twins, `TestConfig.kt`; tests `FeedbackPathScanTest.kt`, `UntranslatedStringsTest.kt`, both `AppLaunch.kt` kit copies (`TestConfigRule`), and the moved-in held tests `HeldResetAppTest.kt`, `HeldResumeRestartAppTest.kt`, `HeldSolvedFindAppTest.kt`, `HeldSolveNowAppTest.kt`.
  - `devtools`: `ReleaseSeparationTest.kt`; `.swdev/verifiers/v08_promise_apk.py` (`DEBUG_ONLY`), `v06_module_deps.py`.
- **Owner input (mid-review, from the coordinator), Jami verbatim:** "can be total time (if thats easier) up to you really, but maybe some unactivity checking makes sence so that if havent done anything for minute then stop counting time until next press etc."
- **Style reference:** `reviews/WO-007-design-review.md`.
- **Not read:** `.swdev/heldout/`. No Gradle, emulator or git was run.

## Verdict

**recirculate → design-author (rev 1, then a spot-check).**

Most of the design is right, and its central claims hold against the code:
- **The v1 fit is real.** `encode()` / `decodeEntry()` / `decodePlayTime()` / `decodeSettings()` already carry `puzzleSeconds`, `bestSeconds`, `playTime.{day, todaySeconds, totalSeconds}` and `settings.timerShown`. The frozen fixture already holds a Solved record with `puzzleSeconds 47` and `bestSeconds 41`, which is exactly the design's meaning (the last attempt's seconds, and the fastest), and `FrozenV1FixtureTest` asserts it. Row 11 is analysed correctly and is not triggered.
- **Timestamps, not ticks, from an injected monotonic clock**, is the right counting model. It is simpler to prove than the prototype's `setInterval` and makes every device test clock-driven.
- **The today/total reading is the literal one**, and the owner has now confirmed it (see "Owner input" below).
- **The seams that WO-007 taught are respected.** `SessionHost`'s internal constructor is unchanged, `solvedListener` is a settable property, and the new `SettingsController` parameters before `onReset` compile against all five existing call sites.

But the flagged highest-risk section does not hold as written. **An erased play time can come back after a confirmed reset (F1).** The design specifies `onReset = { controller.afterReset(); time.afterReset() }`. `controller.afterReset()` reaches `puzzleShown`, which "first accrues", and `accrue()` is the method that flushes every 10 s. A flush that falls due there writes the pre-reset today and total back to the store. `time.afterReset()` then "re-reads" that resurrected value. The design's own sentence "a reset that re-reads before anything can write" is the opposite of the order it specifies, and `OwnershipOrderTest` as described never makes the flush due at that moment, so it stays green.

Three Shoulds follow:
- live counting breaks a moved-in acceptance test, `HeldResetAppTest` (`REQ-034.A1` / `A2`), which the design never names (S1);
- the cross-file and module-list inventory misses `UntranslatedStringsTest`, `FeedbackScan.PRODUCT_MODULES` and `TestConfigRule` (S2);
- the `reserveTopRight` change is never swept in the configuration the app actually runs, and the pill / Restart "never overlaps" check cannot see the measured sizes that make them collide (S3).

**1 Blocker, 3 Shoulds, 10 Notes.**

## Checklist applied

- [x] **Directives.**
  - **D1.** The TYPE-005 rule and the one duration format sit in `kernel` (architecture §3), counting and the pill in `time` (O-04), the rows in `settings`, the format change in `browse`, and the wiring in `app`.
  - **D2.** Interval accrual is simpler to prove than tick counting. The merge-in-`capture` route is simpler than a `PuzzleHost.beforeLeave()`. Two D2 points remain: the keeper's cached `bestSeconds` (F1(d)) and the held-finger flag (N3).
  - **D3.** Every abstraction names its REQ or decision: `TimeSource` (E5 precedent, DA-142), `PlayTimeKeeper` (O-04), `PlayTimeReadout` (G-06, DA-133), `CountingTicker` (DA-134), `reserveTopRight` (REQ-031, DA-140).
  - **D4.** No placeholder rows or speculative slots. The prototype's "Puzzles solved N / M" row is rightly not built, since REQ-032 does not list it. The keeper's public `bestSeconds` has no named reader (F1(d)).
  - **D5.** No new library; the ticker is an `android.os.Handler`.
- [x] **Guardrails.**
  - **G-01 / G-02.** No permission, network or SDK; `time` has no manifest.
  - **G-04.** `TestConfig.timeSource` is debug-only. The release `DebugAids.timeSource(real)` returns `real`, asserted by the DA-72 source test. `TestConfig` is already in V-08's `DEBUG_ONLY`, so V-08 needs nothing new.
  - **G-05.** All labels exist in fi and en (F14 prototype strings), but the identical format strings need the neutral list (S2).
  - **G-06.** `time: {kernel, contracts}` is already in V-06, and only `app` depends on `time`.
  - **G-09.** Untouched (re-verified).
  - **G-10.** The keeper "never throws".
- [x] **Contract.** `IProgressStore`, `SavedGame.kt` and `Puzzle.kt` are consumed unchanged, so there is no contract-delta event. No locked REQ/TYPE text is changed or narrowed. DA-140's corner reserve amends DA-71 / DA-75, which are AI decisions, not Contract (N9).
- [x] **Scope.** Everything traces to the 9 IDs, the carried parts and the named locked rules. Nothing is unrequested except the held-finger softening (DA-134, logged).
- [x] **Traceability.**
  - Rule 3 holds: the module-level covering tests are under `time`, and the cross-slice tokens are in `app`.
  - Token hygiene has two soft spots (N7).
  - Seams: four edits have no delivering task (S1, S2, S3).
- [x] **Hard-stops.** No schema change and no migration (row 11 analysed, not triggered: verified). No PII or money. Row 14 is not hit: DA-134 / 135 / 136 / 140 are row-12 readings, and none narrows a locked line.
- [x] **Evidence re-derived.** See below. Re-reading the code showed these claims to be wrong or incomplete:
  - the reset order (F1);
  - "the existing tests do not pass the flag", which is true for `play`'s JVM sweeps but not for the app (S3);
  - "no implementer edits a test that carries a token", silently untrue for `HeldResetAppTest` (S1);
  - "`PROMISE_TEXT_KEYS` unchanged, so no cross-copy edit" (S2);
  - "at most one small write per 10 s" (N8).
- [x] **The conceptual 20 %.** The effort went to:
  - every ordering of the four writers against `open()`, `restart()`, `afterReset()`, `persist()`, `onPause` / `onStop` and the 10 s flush;
  - the literal REQ / TYPE text of the time semantics, now with the owner's answer;
  - which existing token-carrying tests assume time never moves;
  - the corner algorithm with and without the reserve, at real Restart widths.

### Re-derived

- **Who writes what, today.**
  - `BrowseController.open()` (lines 79–84) is: `persist()`, then `saveLastShownPuzzle`, then `showAt`.
  - `restart()` (96–104) writes `store.progress(id).restarted()` directly and then `showAt`; it never calls `persist()`.
  - `afterReset()` (112) is exactly `showAt(indexState)`.
  - `persist()` (115–119) captures `host.session`.
  - `SettingsController.confirmReset()` (48–53) is `store.resetAllProgress()` then `onReset()`.
  - `JsonProgressStore.resetAllProgress()` (121–126) clears puzzles and play time and keeps `settings` and `lastShown`.
  - `savePlayTime` (99–103) is an unconditional whole-document write, skipped only when the value is equal.
- **The drag and the store.**
  - `PlaySession.beginDrag` (203–206) moves New to In progress **without** `settled()`, so the stored record stays `new` for the whole first drag (DA-49 writes it "with the next event").
  - `toProgress` interrupts a drag, so the design is right that the flush must never go through `persist()`.
- **The solve sites.**
  - The drop solve is `onEvent(LOCK)` → `onEvent(SOLVE)` → `onSolved(false)` → `settled()` (388–404).
  - The aid solve is `onEvent(SOLVE)` → `onSolved(true)` → `settled()` (284–286).
  - A listener called right after `onSolved` therefore precedes the persist that writes the Solved record, as the design states.
  - `restore` never fires either, so DA-66's complete-In-progress-as-Solved restore sets no best.
- **Orderings traced** (with the keeper as specified, and "a flush is due" meaning at least 10 s of counted source time since the last flush):
  - **`open()`:** the merge in `persist` accrues and writes the outgoing record. `puzzleShown` then accrues a few ms more to the *same* outgoing id. A flush due there rewrites that In-progress record with an equal or +1 s value, then the new puzzle is adopted. Safe.
  - **`restart()`:** `restarted()` stores `new` / 0 / best. A flush due inside `puzzleShown` targets the same id, finds the stored state `new` and skips the puzzle write; `savePlayTime` is legitimate there. Safe, because of the In-progress guard.
  - **`confirmReset`:** the store is erased, then `controller.afterReset()` → `show` → `puzzleShown` → accrue. A flush due here writes `savePlayTime(<pre-reset today, total>)`. Then `time.afterReset()` re-reads the store and gets the resurrected value back. **Unsafe (F1).**
  - **Mid-drag flush:** a read-modify-write of the stored record that never touches the session. Safe. During the *first* drag of a New puzzle the stored state is `new`, so that drag's seconds are not flushed (N8).
  - **Process death mid-flush:** each of the flush's two writes is atomic (temp file plus rename). A kill between them leaves the play time saved and the puzzle seconds behind by at most 10 s. Bounded.
  - **Aid solve:** `solved(true)` freezes the seconds counted so far, then the persist merges them with the stored best. Correct, though the stored best only survives because nothing else wrote it since adoption (F1(d)).
  - **Restore fallback** (`show` passes `NEW.copy(bestSeconds = …)`): the keeper adopts 0, and a later flush may write 0 into the broken In-progress record. The next persist writes `new` anyway (DA-50). Harmless.
- **The fixture.** `progress-v1.json` has `shapes-mini-1` Solved 47 / 41, two In-progress records with seconds 12 and 95, and `playTime` 2026-10-03 / 340 / 5120. Every one reads correctly under the design's semantics.
- **Tests that now see time move.** With no manual clock installed, the live keeper counts every second within 60 s of a test tap.
  - **`HeldResetAppTest`** (WO-007 held, moved into `app/src/androidTest/.../held`, tokens `REQ-034.A1` / `A2`) seeds `PlayTime(2026-10-04, 125, 4000)`. It asserts:
    - `PlayTime.NONE` after Erase and again after a close and relaunch (lines 91, 141);
    - exactly the seeded play time after Keep and after Back (line 164).

    The A1 relaunch path flushes the post-Erase taps on `onPause` / `onStop`, so it is red deterministically. A2 is red as soon as any flush runs after the first tap: opening the grid or the settings stops the puzzle clock, and "when the puzzle clock stops" flushes. The day roll from 2026-10-04 makes it red on any later date too. (S1)
  - **`HeldSolvedFindAppTest`** (`REQ-050.A3`) compares whole records, including an In-progress `puzzleSeconds 12` that is shown at launch. It stays green only because under 1 s of puzzle time accrues between the counter's down and the grid's flag emission (N10).
  - `HeldResumeRestartAppTest` (0 seconds after Restart) and `HeldSolveNowAppTest` (best null or kept) hold.
- **Module lists and neutral lists.**
  - `FeedbackScan.PRODUCT_MODULES` (`FeedbackPathScanTest.kt:43`, used by rule 5 at line 50 and the canaries at 240 / 355) lists 8 modules without `time`.
  - `UntranslatedStringsTest.neutral` (lines 24–35) holds `browse:time_minutes_seconds` and nothing for the new identical keys.
  - Both `TestConfigRule` copies (`acceptance/held/AppLaunch.kt:19`, `acceptance/layout/AppLaunch.kt:19`) reset only `languageTags` and `fontScale`.
  - The design names only `ReleaseSeparationTest.PRODUCT_MODULES`. (S2)
- **The corner algorithm.**
  - `computeWithCorner` tries TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, then a STRIP that **shrinks the board** (`PlayLayout.kt:181–193`).
  - `placeSecondary` tries BL, BR, TR, TL, then beside a strip, then null (`PlaceSecondary.kt:23–35`).
  - `PlaceSecondarySweepTest` asserts that null never happens, without the flag.
  - Restart is *measured*: default 120 dp (`PlayArea.kt:301`); "Aloita alusta" at 15 sp SemiBold plus 32 dp padding is about 140 dp at font scale 1.0 and about 250 dp at 2.0 (estimate). (S3)
- **The prototype** (VERSION 0.6), for the semantic fork and the differences list:
  - The 1 s loop adds to `stats.today` / `stats.total` whenever the page is visible and `lastTouch` is under 60 s old, on any screen.
  - It adds to `pr.time` only when not solved, In progress and with every overlay closed (settings, the DEV dialog **and** the grid).
  - `lastTouch` starts at page load and is refreshed by pointer events on the app and by **keydown**.
  - The timer shows `"⏱ " + fmt(time)` at the board's top-right, inset 14 px, and is hidden when solved.
  - The Restart pill sits at the board's top-left.
  - The settings stats include "Puzzles solved N / M".
  - The tab saves every 5 s.

## Trajectory & quality

- **Verification actually run?** Mostly.
  - The store, fixture and contract claims hold exactly, and so do the REQ text claims.
  - The ownership table was argued writer by writer, but never as an interleaving of the writers with the flush that the keeper's own `accrue()` performs. That interleaving is where F1 lives.
  - The test casualties (S1) and lists (S2) needed one grep each for `playTime` / `PRODUCT_MODULES` / the neutral list across the existing tests.
- **Proportionate?** Yes. The product code is small, and most of the cost is tests and walks. The keeper is the minimum stateful piece O-04 asks for, and the readout seam is the minimum G-06 allows.
- **Path sane?** First pass. The author flagged the right section and asked to be attacked there, and the gaps are concrete and cheap to close.

## Findings

| ID | Sev | Section | Finding | Rule / REQ | Required change |
|---|---|---|---|---|---|
| F1 | **B** | §3 "Reset"; §5.1 table row 4 and the `puzzleShown` paragraph; seam row `PlayTimeKeeper` (`accrue()` "flush every 10 s"); Risks bullet 1 ("a reset that re-reads before anything can write"); scaffolding `OwnershipOrderTest` | **An erased play time can come back after a confirmed reset, and the planned test cannot see it.** <br>• **The specified order is "reload browse, then reload time".** `onReset = { controller.afterReset(); time.afterReset() }`. `controller.afterReset()` is `showAt(index)`, which calls `host.show`, which calls `puzzleShown` at its end, which "first accrues". <br>• **Accruing can write.** The only accrual the seam table names is `accrue()`, "accounting; flush every 10 s of source time while counting", and §2.1 says each input "closes the open interval (`accrue`)". When the 10 s mark has passed since the last flush, that accrual calls `flush()`. `flush()` runs `store.savePlayTime(PlayTime(day, today, total))` with the **pre-reset** memory, on a store that `resetAllProgress()` erased a moment ago. <br>• **The reload then reads the resurrected value.** `time.afterReset()` "re-reads the (erased) play time", but the erased value is gone by then. Today and total come back, against REQ-034's Statement ("erase all puzzle, best and play times") and O-08's promise ("`app` reloads … from the store, so nothing erased comes back"). <br>• **The text contradicts itself.** The seam says `puzzleShown` "adopts, never writes", and the value shapes say "`puzzleShown` makes no store write", yet `puzzleShown` must accrue and accruing flushes. An implementer who reuses `accrue()` inside the inputs, the obvious reading, ships the bug. If the flush check runs only on the 1 Hz ticker path, a reset lands inside the due window about once in ten. <br>• **`OwnershipOrderTest` cannot catch it.** It "encodes every one" of the orderings, but none with a flush due at the moment of the reset. `HeldResetAppTest` sees it only by chance. <br>• **(d) A second, latent stale-value path.** The keeper caches `bestSeconds` from the store at `puzzleShown`, and `mergeInto` writes that cached copy back on every capture. That is what O-08 forbids ("no slice keeps a stored record across events"). It is safe today only because no other path writes a best between adoption and capture, which is an invariant nobody pins. The public `bestSeconds` snapshot also has no named reader: the solved bar and the settings list both read the store. | REQ-034 Statement; O-08; §5.1's own guarantee; D2 / D4 | **(a) Separate accounting from writing.** One private `account(now)` closes the interval in memory and is used by every input (`touch`, `setVisible(true)`, `setPuzzleRunning`, `puzzleShown`, `mergeInto`, `solved`). Only the ticker's `accrue()`, the explicit `flush()` and the stop points (`setVisible(false)`, idle expiry, `onPause`) may write. Write this into the seam row. <br>**(b) Make the reset order safe by construction.** Either: <br>• `onReset = { time.afterReset(); controller.afterReset() }`, where `afterReset()` first accounts and discards the open interval, zeroes memory and the carries, re-reads the store, and restarts both the interval and the flush clock at `now`; or <br>• keep the order, and state and test that nothing between `resetAllProgress()` and `time.afterReset()` can write. <br>Fix the Risks sentence to match the chosen order. <br>**(c) Give `OwnershipOrderTest` the due-flush cases**, against a recording fake store that records the call sequence: <br>• a reset with a flush due: no `savePlayTime` / `saveProgress` between `resetAllProgress()` and the end of `onReset`, `NONE` afterwards, and after the next flush only post-reset seconds; <br>• Restart with a flush due (the old attempt is never written); <br>• a leave with a flush due; <br>• an aid solve whose counted seconds are below the stored best (best unchanged); <br>• a first drag of a New puzzle lasting more than 10 s (no write until the drop). <br>Then name `OwnershipOrderTest` as the TASK-063 done-check gate. <br>**(d) Stop caching the best.** `mergeInto` computes `bestSeconds` from `base.bestSeconds` and a pending solve result (own solve: `min`; aid or no solve: `base`). Drop the keeper's `bestSeconds` state unless a reader is named. |
| S1 | **S** | §8 "Carried parts"; "Constraint: … no implementer edits a test that carries a token"; the cut (no task) | **Live counting breaks a moved-in acceptance test that the design never names.** <br>• **What it asserts.** `app/src/androidTest/.../held/HeldResetAppTest.kt` (`REQ-034.A1`, `REQ-034.A2`, WO-007 held, now visible) seeds `PlayTime(2026-10-04, 125, 4000)`. It asserts the stored play time is `PlayTime.NONE` after Erase and after a close and relaunch (lines 91, 141), and exactly the seed after Keep and after Back (line 164). It installs no clock. <br>• **A1 goes red every time.** After Erase the test taps on for several seconds (close, grid, screenshots). `onPause` / `onStop` then flush those seconds, so the relaunch assertion fails. <br>• **A2 goes red almost always.** The taps before and during Keep / Back earn seconds. Opening the grid or the settings stops the puzzle clock, which flushes. The fixed seed day differs from any later run date. <br>• **The reading is not written down.** REQ-034 A2 says "cancelling the confirmation changes nothing". Under DA-135 and the owner's answer, the seconds the player spends looking at the question **are** play time, so the test's exact-equality reading of A2 no longer matches the product. That reading needs a logged decision, not a silent test edit. <br>• **Same class as WO-007's F4.** No implementer may edit the test, and no task in the cut owns the corrected check, so it would surface as a red at MOVE-DEV8 with no route. | AGENTS seams lesson (every frozen seam names its delivering task; corrected checks by the Test Author); REQ-034 A1 / A2; WO-007 design review F4 | • Name the casualty in §8 and in the seam table. <br>• **Corrected check** by the Test Author in TASK-T8a, diff read by the orchestrator, landed with TASK-068: install a frozen `ManualTimeSource` (both `nowMs()` and `today()` pinned to the seeded day), so the existing exact assertions keep their full strength. <br>• Add the F1(c) variant at device level: advance the clock past the flush mark before Erase. Put it in a held class if the Test Author can keep it apart. <br>• Log the A2 reading as a DA row: "changes nothing" means the reset's effects. Seconds earned while the question is shown are counted as always (DA-135, owner input). <br>• List `HeldSolvedFindAppTest` in the Risks (N10). |
| S2 | **S** | §8 last three bullets ("`PROMISE_TEXT_KEYS` unchanged, so no cross-copy edit"); seam rows `time_*` strings and `ReleaseSeparationTest`; cut rules (2)–(4) | **The cross-file and module-list inventory is incomplete. This is the third occurrence of the AGENTS lesson.** <br>**(i) `UntranslatedStringsTest`** (`app/src/test/.../language`, `// decision DA-103`) fails on every fi value equal to its en value unless `"module:key"` is in its `neutral` map. Today the map holds only `browse:time_minutes_seconds`. The design adds identical keys and so needs **five** new entries: `time:time_minutes_seconds`, `time:time_hours_minutes`, `settings:time_minutes_seconds`, `settings:time_hours_minutes`, `browse:time_hours_minutes`. Without them, MOVE-STR8's `:app:testDebugUnitTest` is red. <br>**(ii) `FeedbackScan.PRODUCT_MODULES`** (`FeedbackPathScanTest.kt:43`) drives rule 5 (no `Dialog(` / `Popup(` in a release-shipping module: DA-125's lever precondition) and the per-module canaries. `time` ships in release and is missing. The design only adds `time` to `ReleaseSeparationTest`. <br>**(iii) `TestConfigRule`** (both kit copies, `AppLaunch.kt:19`) resets `languageTags` and `fontScale` only. A manual clock leaked by one test would then freeze time in every later test of the run. | AGENTS cross-file lesson (WO-006 F1, WO-007 F1/F2); DA-103; DA-125; G-05 | Add three rows to the cut, each landed with the change that needs it: <br>• (i) with TASK-062 in MOVE-STR8; <br>• (ii) with TASK-063 (the first `time/src/main` source); <br>• (iii) in TASK-T8a / T8b, in both kit copies. <br>List every module-enumerating or exemption-keyed check that `time` and the new strings touch: `ReleaseSeparationTest`, `FeedbackScan`, `UntranslatedStringsTest`, `ScreenWalk.bundle`, the DA-92 scan, `a4FileSetS`. |
| S3 | **S** | §7 "The Restart and DEV controls leave that corner free", "The defaults leave every existing layout, sweep and test unchanged", "Sizes"; DA-140; scaffolding `TimerCornerSweepTest` | **The corner change is never swept in the configuration the app actually runs, and the overlap check cannot see the sizes that collide.** <br>• **The app always passes the slot**, so `reserveTopRight` is always on in the app, debug and release alike. That keeps DA-74 parity, which is good. But `PlaceSecondarySweepTest` (asserts the DEV pill is never null) and `CornerControlSweepScaffoldingTest` keep running without the flag, so they stop describing the app. <br>• **Restart moves where the top-left is blocked.** Restart used to take TOP_RIGHT there. With the reserve it falls to BOTTOM_LEFT, BOTTOM_RIGHT or a **STRIP, which shrinks the board** for those puzzles and windows: a visible layout change from WO-004…007 that nobody counts. <br>• **The DEV pill can lose its last corner.** It loses TR, and Restart may now take its preferred BL. Neither may DEV, so a null placement (no DEV button: REQ-046's Statement) is possible and unswept. <br>• **The two pills can collide.** The pill sits top-right, and Restart usually sits top-left at its *measured* width. "Aloita alusta" is about 140 dp at font scale 1.0 and about 250 dp at 2.0, ellipsized only at the area width (estimate). The pill grows with font scale: about 100 dp for "12:34" and about 150 dp for "1 h 5 min" at 2.0 (estimates). On a 360 dp phone in Finnish at 2.0, a mode the WO-006 walks already run, Restart ends near x = 251 and an m:ss pill starts near x = 252, so they nearly touch; once the hour format shows they overlap by about 50 dp. A JVM sweep given fixed sizes asserts "never overlaps" over the wrong inputs. <br>• **DA-140 never says that it amends DA-71** (corner order) **and DA-75** (DEV order). | DA-71, DA-74, DA-75; REQ-046 Statement; REQ-031 Statement; REQ-037 (the Restart stays a 48 dp control); AGENTS (a sweep pins what the app runs) | • Run the corner sweeps **with `reserveTopRight = true`** as the app runs them. Assert: Restart never over the silhouette; **DEV never null**; and the strip count against the unreserved baseline, recorded in the workorder. Owned by the Test Author in TASK-T8c and landed with TASK-065. <br>• Give `TimerCornerSweepTest` **worst-case sizes**: Restart in fi and en at font scales 1.0 and 2.0, and the pill at "59:59" and "10 h 59 min". Derive the widths once on a device and pin them as constants with their source. <br>• If an overlap remains, replace the boolean skip with an **obstacle**: a reserved pill rect at its worst-case width that the primary and secondary candidates must clear. <br>• Write "amends DA-71 and DA-75" into DA-140, and put the board-shrink count on the owner item for the timer's look. |
| N1 | N | Alternatives "What today and total count"; DA-135; CA-10(b); owner item 1; WO goal paragraph | **The reading is the REQ's, and the owner has now confirmed it; close the fork.** <br>• **The locked text requires it.** REQ-029's Statement adds *every* active second to today and total. TYPE-005 defines "active" by visibility and a touch alone. REQ-032's pause is "**Puzzle** time". TYPE-006's last rule, "Time is counted only In progress", is scoped by its own "(REQ-030)". Spec/01 §8, Study/07 ("Today: active seconds since local midnight") and the prototype all count the same way. <br>• **Not a row-12 guess.** Counting only solving time would *narrow* REQ-029, which would be a row-14 CHG. <br>• **The owner agrees** ("can be total time … up to you"). <br>• **The WO's goal sentence** ("never while … the player browses away or the settings screen is open") states it the other way round and should be corrected. <br>• **Reversal has a cost.** Totals already counted cannot be recomputed under another reading. | REQ-029, TYPE-005, TYPE-006, REQ-032; governance rows 12 / 14 | • Put TYPE-006's scoped rule, Spec/01 §8, Study/07 and Jami's answer into DA-135's basis. <br>• Close owner item 1 as answered. <br>• Reduce CA-10(b) to a one-line clarification, or drop it. <br>• The orchestrator corrects the WO goal to "puzzle time pauses while…" and records Jami's message verbatim (`progress.md`; capture side per AGENTS rule 2). No REQ text changes. |
| N2 | N | §5.3 "Running"; §5.5; the cut's failure routing; kit (§8) | **The running gate lags one frame, and a device test that jumps the manual clock can credit the jump to the wrong state.** <br>• The `snapshotFlow` emits after the next frame. A test that taps ›, opens the grid or the settings, or drags, and then advances the clock and calls `accrue()` before a frame has run, credits the jump under the **old** flag. That is a false red for REQ-030 A1 / A3, or a false green. <br>• Tests with a paused Compose clock (`startClockPaused()` exists in the kit) never emit at all. | AGENTS (nothing sleeps; flaky means the harness first) | • Put one kit helper in both copies, `advanceActive(ms)`: `waitForIdle` (or advance `mainClock` by a frame), advance the source, `accrue()` on the main thread, `waitForIdle`. <br>• Add "the test advanced the clock before the gate emitted" to the failure-routing table. |
| N3 | N | §2.1 "Touch"; DA-134; owner input | **The held-finger flag is a softening that real hardware mostly makes moot.** <br>• Touchscreens usually report a held finger as a stream of MOVE events, which already refresh `lastTouchMs`. A perfectly still contact also stops refreshing the system's user-activity timer, so the screen timeout ends it at `onStop`. <br>• The flag mainly changes emulator and test behaviour. It also departs from the prototype (no events, so it stops after 60 s) and from Jami's "if havent done anything for minute then stop". | D2; TYPE-005; owner input | Either drop `touching` and keep "the last pointer event" as the touch (one flag less, literal TYPE-005, the owner's words), or keep it and list it as a prototype difference and an owner item. Both are row-12 choices; say which. |
| N4 | N | §5.4; seam row `CountingTicker`; scaffolding `CountingTickerScaffoldingTest` | **The ticker's JVM test cannot construct its default.** `app` unit tests run against android.jar stubs: no `isReturnDefaultValues`, no Robolectric (SessionHost wraps `Log.w` for this reason). `Handler(Looper.getMainLooper())`, or any `Handler` subclass, throws in a JVM test, so the seam's `handler = Handler(...)` makes the scaffolding test unrunnable as planned. | G-10 test practice; AGENTS adapters fail loudly | Seam the ticker over two functions, `post: (Runnable, Long) -> Unit` and `cancel: (Runnable) -> Unit`, defaulting to a main `Handler` in production; or move the test to `app/src/androidTest`. |
| N5 | N | §5.5 "Naming constraint"; DA-142 | **The naming constraint covers the keeper only, and quotes rule 4 short.** Rule 4 also fails on **bare** `play(` / `tick(` calls (CR-6 N2: `bareCall`) in every scanned file. The natural home of a `tick()` is `CountingTicker` in `app`. | DA-116, DA-128 | State it for every new file in `time/src/main` and `app/src/main` (no function, local or reference named `play` / `tick`), and quote all four forms: `.x(`, `::x`, bare `x(`, and declarations. |
| N6 | N | Risks / CLOSE8 "design-inputs.md (the prototype differences: tick counting versus interval accrual, the timer on a solved puzzle)" | **The differences list is wrong in one item and short of six.** The prototype also hides the timer on a solved puzzle (`showT = !solved && …`), so that is not a difference. Missing: <br>• no counting after a launch until the first touch (the prototype starts `lastTouch` at load); <br>• key presses are not touches (the prototype counts `keydown`); <br>• a held finger counts (N3); <br>• the DEV dialog does not pause puzzle time (the prototype's `overlaysClosed` includes `devDlg`); <br>• the ⏱ glyph of Spec/02 and the prototype is dropped (DA-140); <br>• the "Puzzles solved N / M" row is dropped; <br>• the 8 dp inset and the 10 s save versus 14 px and 5 s. | DI-1 / DI-2 (`design-inputs.md` §2) | Replace the CLOSE8 list with these. |
| N7 | N | Acceptance table (REQ-029 A1, REQ-005 A1, REQ-031 A2 / C9, the WO-005 carry) | **Token readings.** <br>(a) `IdleCutoffTest` (`REQ-029.A1`) also asserts "a held finger keeps counting". That is DA-134, not A1's meaning, so it belongs in `ActiveSecondTest` (`// decision DA-134`) or a method without the token. <br>(b) C9 under `REQ-031.A2` is fair only if the rotation test asserts what A2 means: the timer is shown after rotation, and the stop/start gap with the clock advanced credits nothing; then it counts again. "The value is kept" alone is F3 (`// decision F3`, which WO-006's C9 row allows). <br>(c) The aid regression proves "no best" sharply only if the aid solve's counted seconds are **below** the existing best, which is where a `min()` bug shows. Say so in `HeldBestTimeAppTest`. <br>(d) DA-136 says the Solved record's `puzzleSeconds` *is* the solve time. The held REQ-005 A1 step should read it back after a **slower** re-solve too, not only on a first solve. | AGENTS (test tokens are coverage claims) | Write (a)–(d) into the acceptance table. |
| N8 | N | §4.3 loss table and "Disk churn" | **Two statements are slightly off.** <br>• A flush with a shown In-progress puzzle is **two** whole-document writes (`savePlayTime`, then the record), not one. <br>• "At most 10 s" does not cover the first drag of a New puzzle: the stored state is `new` until the drop (DA-49), so a kill mid-drag loses that drag's puzzle seconds. This matches F4 ("a kill loses at most the drag in progress"). | ADR-005, F4, DA-49 | Correct both lines; no design change. |
| N9 | N | DA-133, DA-140, CA-10 | **Row hygiene.** <br>• DA-133 (module shape) is not a REQ ambiguity; tag it "— (build-side)", as DA-61 was. <br>• DA-140 amends DA-71 / DA-75 (S3). <br>• CA-10 is free to use: WO-007's rev 0 CA-10 was withdrawn and never written to `req_review_01.md`. Say so in one line so the two are not confused. <br>• No DA is a human row in disguise. | governance rows 12 / 14 | As stated. |
| N10 | N | Risks | **One more existing test sits near the edge.** `HeldSolvedFindAppTest` (`REQ-050.A3`) compares whole records and stays green only because under 1 s of puzzle time accrues between the counter's down and the grid's flag emission (N2). | AGENTS test-substance lessons | Name it in the Risks, and route any red there through N2 before blaming the product. |

### Answers to the brief's questions not covered above

**1. The four writers.**
- `open()`, `restart()`, the mid-drag flush, process death and the aid solve are safe as traced in "Re-derived". The reset is not (F1).
- `OwnershipOrderTest` is the right instrument but lacks the due-flush cases and two edge cases (F1(c)).
- Keep the flush's In-progress-only guard: it is what makes `restart()` safe.

**2. Time semantics against the locked text.**
- **Today and total count browsing, the grid, the settings overlay and a solved screen.** This is the literal reading of REQ-029, REQ-005 and TYPE-005, not an assumption (N1). Owner item 1 is answered.
- **The idle window.** "5 minutes untouched adds at most 60 s" holds exactly: the window is inclusive, the carries make whole seconds, and the press itself is engaged time.
- **A held finger** is a defensible reading, with N3's alternative.
- **REQ-030 A3.** "Only a drag from the tray starts time" is TYPE-006 verbatim ("a drag starts on a tray piece"), and `beginDrag` is the one place it happens.
- **REQ-030 A1** holds through adoption and the leave merge. The pause while `isOpen` is a derivation, not a hook.

**3. The clock seam.**
- **Sound.** `System.nanoTime` is monotonic and immune to wall-clock and zone changes. The day comes from `LocalDate.now()` at accrual time, and Android resets the process default time zone on a zone change, so it follows. DST is irrelevant to a `LocalDate`.
- **The rollover while open** is attributed within 1 s. An idle screen open across midnight shows yesterday's today until the next state change: negligible.
- **Release safety (G-04).** The debug-only `TestConfig.timeSource` reached through `DebugAids` is release-safe. V-08 needs nothing new, because `TestConfig` is already in `DEBUG_ONLY` and the manual source lives in `androidTest`.
- **Battery.** The ticker runs only while counting, so it is fine.

**4. Persistence.**
- **Row 11: not triggered** (verified).
- **`puzzleSeconds` on a Solved record** keeps the KDoc meaning ("active seconds of the current attempt"). The fixture's Solved 47 / 41 already uses it that way, and DA-77 foresaw "solved before time existed".
- **Old saves** read as never timed, with a null best and 0 seconds.
- **Save cadence.** The cadence is O-08's ("every puzzle event, every 10 s while counting, and when the app goes to the background"). Churn and loss: N8.

**5. The timer pill.**
- **Corner rules.** Placement and the reserve: S3. DA-74 parity holds, because the reserve is on in both builds and the DEV pill never feeds the layout.
- **Not a player control.** The pill is inert, so REQ-037 applies to the switch (C6), and the walk asserts the pill takes no touch.
- **0:00 on New and hidden when solved** both match the prototype.
- **Drawing in px** (`BasicText` plus a background, no canvas path) satisfies DA-92, and the DA-92 scan gains `time`.

**6. Seams.**
- The `SessionHost` constructor is unchanged.
- `solvedListener` sits at both sites, before `settled()`, and never on `restore`.
- The `SettingsController` order is verified against all five call sites, which bind `onReset` positionally by trailing lambda or by name.
- `PlayTimeReadout` is the right G-06 join.
- The `tick` / `play` naming rule: N5.
- **Seams that still have no delivering task:** S1, S2 (three rows) and S3 (the swept configuration).

**7. Scope.**
- **The carried parts are complete and on the right tokens:**
  - C4 and C7 extend the existing walks;
  - C6 adds the switch and the inert assertion;
  - C9 under `REQ-031.A2` is allowed by WO-006's C9 row, given N7(b);
  - the WO-005 aid regression, given N7(c);
  - WO-007's two rows and the pause.
- **The held-out choice is sound.** 6 of 9 IDs are held at app level; the three visible IDs are display and default checks. Add the reset-with-a-due-flush step to a held class if possible (S1).
- **The cut is plannable**, and the lessons are built in:
  - the strict build step (TASK-060 alone);
  - module-scoped parallel builds (DA-126);
  - failure routing, CLOSE8 and the held-out compile proof.

  Missing: the S1–S3 rows.
- **The DA and CA rows** are right in kind (N9). DA-135 is now owner-confirmed. No row hides a locked-text change.

## Owner input (Jami, received mid-review)

Jami wrote: "can be total time (if thats easier) up to you really, but maybe some unactivity checking makes sence so that if havent done anything for minute then stop counting time until next press etc."

This is an idea, not a requirement (CLAUDE.md, AGENTS rule 2), and it changes no locked text. It is read against the locked text:
- **Today and total.** The locked text does **not** rule out counting browsing, the grid, the settings or a solved screen. REQ-029's Statement **requires** it: every active second, as TYPE-005 defines it. So "total time" is what the REQs already say. The design's reading stands, and owner item 1 can be closed. No CHG is needed. Only a narrowing (counting solving time alone) would need one, under row 14.
- **The inactivity wish matches TYPE-005 and the design.** Counting continues up to 60 s after the last touch, then stops, and the next touch resumes it. After a launch nothing counts until the first touch. Five minutes untouched adds exactly 60 s. Three small differences from the owner's wording:
  - a perfectly still held finger keeps counting under DA-134 (N3);
  - a hardware key press is not a "press" that resumes counting (TYPE-005 says touch; keyboard play cannot move pieces);
  - the minute after the last touch is itself counted ("at most 60 s ago" is inclusive).
- **Puzzle time (REQ-030 / 031) is unaffected.** It still counts only while an In-progress puzzle is shown with no settings or grid on top.

## What is good and must be kept

- **Interval accrual from an injected monotonic `TimeSource`.** It neither loses nor invents time on a stall or a dropped tick, a 5-minute jump equals 300 one-second steps, and no test ever sleeps.
- **The v1 fit and its row-11 table.** No field, no version, no migration and no contract delta, checked against the encoder, the decoder, the frozen fixture and `FrozenV1FixtureTest`. `TimeStoreRoundTripTest` uses a real store across a new instance.
- **The merge rides the existing persist.** Board and time are saved in one write, the leave loses no second, and `toProgress` is never called from the ticker, so no drag is interrupted.
- **The flush never creates a record or changes a state**, and it writes `puzzleSeconds` only to an In-progress record. That guard is what makes Restart safe; keep it after F1's split.
- **`puzzleShown` adopts and never writes** (make it true by construction, F1(a)). `NEW` forces 0 in the merge, and the merge applies only to the keeper's current id.
- **`solvedListener` as a settable property**: no constructor change, and `HeldSolveNowStoreTest` / `SessionHostScaffoldingTest` stay byte-equal under the SHA rule.
- **The literal today/total reading**, puzzle time gated by TYPE-006 and the two overlays, and REQ-030 A3 bound to `onTrayDragStarted`.
- **`PlayTimeReadout`** as the one-method G-06 join, with no `settings` → `time` edge. `SettingsController`'s new parameters go before `onReset`.
- **The pill is inert**, drawn in px. It is reserved whether or not it is shown, so nothing moves when the setting flips, and DA-74 parity holds.
- **One duration format** in `kernel`, with an equality test over the three copies of the strings.
- **Release safety.** A debug-only `TestConfig.timeSource` behind the `DebugAids` pair, a release pass-through asserted by the DA-72 test, no new `DEBUG_ONLY` entry needed, and no new dependency.
- **The cut's discipline:** TASK-060 alone, module-scoped builds after it, the SHA rule, written failure routing, and the carried parts as named rows.

## Owner items (Jami)

Nothing needs Jami before rev 1. Item 1 (today/total) is answered: the REQ text and his message agree. For the WO-008 checkpoint:
1. **Inactivity** (his message): the 60 s rule is built as he describes. Ask one yes/no: should a finger resting still on the screen stop counting after a minute too (N3)?
2. **The timer's look and corner** on a phone and a tablet. Ask also:
   - whether he wants the ⏱ glyph of the prototype and Spec/02;
   - how many puzzles lose board space to the reserved corner (S3's recorded count).
3. As the design lists: the 60 s value at the playtest, the settings order and wording (F14 strings), and the `1 h 5 min` display.

## Handoff — Design Reviewer · WO-008

- **Scope:**
  - REQ-005 A1–A2, REQ-029 A1–A2, REQ-030 A1–A3, REQ-031 A1–A2, TYPE-005;
  - carried in: the WO-005 aid regression, WO-006 C4, C6, C7, C9, and WO-007's timer row, play-time section and `isOpen` pause;
  - governed: `IProgressStore` consumed unchanged; the v1 format is untouched (row 11 analysed, not triggered, verified); no contract delta.
- **Inputs read:** see the header (design rev 0; the working tree after the WO-007 close; the owner's message relayed by the coordinator).
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-008-design-review.md`
- **Status:** recirculate → design-author (1 Blocker / 3 Shoulds / 10 Notes). The Blocker and the Shoulds, one line each:
  - **F1 (B):** the reset order is `controller.afterReset()` (→ `puzzleShown` → accrue, which may flush the pre-reset today/total) then `time.afterReset()`, which re-reads the resurrected value. So an erased play time can come back, `OwnershipOrderTest` never makes the flush due there, and the keeper also caches `bestSeconds` against O-08.
  - **S1 (S):** live counting turns `HeldResetAppTest` (`REQ-034.A1` / `A2`, which asserts exact stored play time) red. The design names neither the casualty, nor the corrected check and its task, nor the A2 reading.
  - **S2 (S):** the cross-file inventory misses `UntranslatedStringsTest`'s neutral list (5 identical keys), `FeedbackScan.PRODUCT_MODULES` (no `time`) and `TestConfigRule` (no `timeSource` reset).
  - **S3 (S):** `reserveTopRight` is always on in the app but never swept that way (DEV may become unplaceable, Restart may strip and shrink the board), and the pill / Restart overlap check cannot see the measured sizes (on a 360 dp phone in Finnish at font scale 2.0 they nearly touch, and overlap once the timer shows hours). DA-140 does not say it amends DA-71 / DA-75.
- **Traceability delta:** none (review only). Rev 1 will bring:
  - F1: an account / flush split in the keeper seam, the reset order or invariant, the `OwnershipOrderTest` cases, and best computed from the base;
  - S1: a corrected-check row and a DA for the REQ-034 A2 reading;
  - S2: three task rows;
  - S3: sweep rows with the flag and worst-case sizes, and the DA-140 wording;
  - N1: DA-135's basis and owner item 1 closed.
- **Notes for next station:**
  - Fix F1 first, because it changes the keeper seam that TASK-063, T8c and CR-7 all read. Then S1 and S2, which are cut rows the Planner needs, then S3, which needs device-derived widths.
  - A rev 1 re-review can be a spot-check of: §3 (Reset), §4.3, §5.1, §5.4–5.5, §7, §8, the seam rows `PlayTimeKeeper` / `CountingTicker` / `time_*` strings / app, the acceptance and scaffolding lists, DA-134, 135, 136, 140, CA-10, and the cut.
  - Jami's message is owner input to record verbatim (`progress.md`; capture side per AGENTS rule 2). It confirms the existing REQ reading and changes no REQ text.

## Rev 1 spot-check

**Date:** 2026-10-05
**Read:** `designs/WO-008-design.md` rev 1 in full, with weight on:
- the "Rev 1 changes" table;
- §3, §4.3, §5.1, §5.3–5.5, §7 and §8 (the cross-file inventory);
- the seam rows (`PlayTimeKeeper`, `CountingTicker`, `time_*` strings, `play`, `app`) and the value shapes;
- the acceptance and scaffolding lists (the eight `OwnershipOrderTest` cases);
- the Risks;
- DA-133…DA-144, CA-10 and the CA-3 addendum;
- the cut.

Also checked:
- `workorders/WO-008.md`'s corrected goal;
- `tools/compare_kits.py` (it compares same-named kit files automatically, so new kit files need no list edit);
- G-06's test-scope rule;
- the only `TangramApp(` call site (`MainActivity.kt:36`).

No Gradle, emulator or git was run, and `.swdev/heldout/` was not read.

### F1 — the reset is now safe by construction (verified)

**The split holds.** Every input calls a private `account()` that has no path to the store. The writers are a closed list:
- `accrue()`: the due flush, and the flush when `counting` just became false;
- `flush()`;
- `setVisible(false)`;
- `setPuzzleRunning(false)`;
- `onPause` and `onStop`.

`puzzleShown`, `touch`, `solved`, `mergeInto` and `afterReset` cannot write, whatever is due. With that split, the order inside `onReset` is no longer load-bearing for writes. `time.afterReset()` first is still the better order, because it discards the open interval before anything accounts.

**The four writers, re-traced against the code:**
- **`open()`.** `persist()` → `capture` → `mergeInto` (accounts, then returns a value) is written by `persist`'s one `saveProgress`. Then `saveLastShownPuzzle` and `show` → `puzzleShown`, which accounts to the outgoing puzzle in memory, adopts the new puzzle and clears the pending result. No keeper write. The next frame's `setPuzzleRunning(false)` flush, if the new puzzle is New, targets the new id: its stored state is `new` (or it has no record), so the puzzle write is skipped, and only post-leave play time is written.
- **`restart()`.** `saveProgress(restarted())` → `show` → `puzzleShown` adopts 0. The next frame's `setPuzzleRunning(false)` flush finds the stored record `new` and skips it. The old attempt is never written.
- **`confirmReset`.** `store.resetAllProgress()` → `time.afterReset()` (discard, zero, clear the pending result, re-read `NONE`, restart the flush clock; no write) → `controller.afterReset()` → `puzzleShown` (adopt-only). The confirm tap's Initial-pass `touch()` runs *before* the click and only accounts. Erased today, total, seconds and bests cannot return. A later flush writes only post-reset seconds.
- **The aid solve.** `solved(true)` freezes the seconds counted so far and records no pending result. The merge in `settled()`'s persist keeps `base.bestSeconds`, even when the counted seconds are below it.
- **A drag in progress during a flush.** The flush is a read-modify-write of `puzzleSeconds` on the stored In-progress record. It never calls `toProgress`, so the drag continues. During the first drag of a New puzzle the stored state is `new`, so nothing is written until the drop (4.3 now says so).
- **Process death.** Each write is atomic. A kill between the flush's two writes leaves the puzzle seconds behind by at most 10 s (stated).
- **The best.** It comes from `base.bestSeconds` plus the pending own-solve result, so nothing is cached (O-08). The pending result is cleared by `puzzleShown` and `afterReset`. That clearing matters: a pending result left over from puzzle A would otherwise become B's best at B's first merge, or bring back an erased best after a reset.

**`OwnershipOrderTest` against the rev 0 bug:**
- **Fail on rev 0, as they should:** cases (1) (reset with a flush due: no store call between `resetAllProgress()` and the end of `onReset`), (3) (a leave with a flush due: written exactly once, provided the fake records every call rather than only changes) and (8) (no store call from the five accounting inputs) fail on the rev 0 flush-in-`puzzleShown` bug. Case (7) (a best changed behind the keeper) fails on rev 0's cached best.
- **Correctly aimed at other invariants:** cases (2), (4) and (5).
- **Case (6) cannot fail on the hazard it exists for (E1).** "Retry then a slower solve cannot lower the best" passes even if the pending result is never cleared, because `min` is idempotent there.

### S3 — the obstacle mechanism is confirmed

Placing the pill after Restart and DEV, from their real rects, is better than my rev 0 suggestion:
- it changes no existing placement;
- DA-71 and DA-75 and their sweeps keep describing the app;
- it cannot leave DEV unplaceable or shrink a board;
- DA-74 holds, because the pill's size never feeds the board.

**REQ-031 and F28.** A pill stepped about one control height below a control that holds the corner is still at the board's top-right. That is a fair row-12 reading, carried in CA-10(d). F28 says nothing about position.

**Rotation.** The place is a pure function of the current layout and the measured sizes, so it is consistent across rotation and `recreate()`. Phones are portrait-locked (F3). On a tablet it moves only where Restart or DEV move with the new layout.

**Can the stepped pill leave the board or overlap something?**
- **The tray and the top bar:** no. "Legal" means inside the play area and above `trayTop`, and the pill sits below the top bar by construction.
- **Restart and DEV:** no. DEV is never placed directly below a top-right Restart, so in practice there is at most one step.
- **The silhouette:** yes, it can overlay it. The region below a top-right Restart is checked by nothing, and the pill is translucent and inert there. That is acceptable, and the sweep records how often it happens.

**The sweep and MEASURE-8 close S3.**
- The JVM sweep uses worst-case widths, asserts no overlap and asserts a fallback count of 0.
- `TimerLayoutMeasuredAppTest` runs on 360 dp, Finnish, font scale 2.0, with "10 h 59 min".
- MEASURE-8 raises the bounds from widths measured on a device. My estimate for "10 h 59 min" at 2.0 is about 175 dp, just above the 170 dp design bound, which is exactly what MEASURE-8 is for.

Two gaps remain (E3, E4).

### S1, S2 and the Notes

- **S1: fixed.**
  - Corrected check: a frozen clock with `today()` pinned to the seeded day, so A2's exact play time stays equal and the store skips the write. Assertions unchanged, Test Author in T8a, landed with TASK-068, diff read by the orchestrator.
  - DA-144's reading of A2 is natural, not a narrowing, so it is row 12.
  - One precondition is unpinned (E2).
  - `HeldResetTimeAppTest`'s token: E5.
- **S2: fixed.** Every row of the §8 inventory names a task that lands it together with the file it reacts to:
  - `UntranslatedStringsTest` (five entries) and `TimeStringsEqualTest` with TASK-062 in MOVE-STR8;
  - `FeedbackScan.PRODUCT_MODULES` and the DA-92 list with TASK-063;
  - `ReleaseSeparationTest`, both `TestConfigRule` copies and the corrected `HeldResetAppTest` with TASK-068;
  - the settings tags with TASK-066;
  - `a4FileSetS` in TASK-060.

  `compare_kits.py` pairs same-named files, so `SolveByTouch`, `advanceActive` and the manual source need no list edit.
- **N1–N10: answered.**
  - N1: DA-135's basis is complete, owner item 1 is closed, CA-10(b) is one line with the owner's words, and the WO goal is corrected.
  - N2: `advanceActive` plus a routing row.
  - N3: kept and put to the owner as one yes/no (the coordinator's call; acceptable).
  - N4: `post` / `cancel` seam. Only the `CountingTicker.main` factory touches `Handler`, so a JVM test that never calls it loads fine.
  - N5: the ban covers all four forms.
  - N6: the list replaced.
  - N7 (a)–(d), N8, N9 and N10: in place.
- **New in rev 1.**
  - `CountingTicker(post, cancel, onSecond)` is sound.
  - `advanceActive` is sound, apart from E6.
  - The naming ban is stricter than the scan, which is fine.
  - DA-133…DA-144 are tagged correctly. DA-144 is row 12, not row 14. No row is a human row in disguise.
  - CA-10 is free.
  - The CA-3 addendum is unchanged.

### E-items (line edits; the orchestrator may apply them without another re-review)

- **E1 — Scaffolding list, `OwnershipOrderTest` case (6), and the reset wiring.**
  - Replace case (6) with the two cases the clearing exists for:
    - **(6a)** an own solve on A, then a leave to B and a settled event on B: B's stored best is untouched;
    - **(6b)** an own solve on the shown puzzle, then a reset and a settled event: that puzzle's best stays `null`.
  - State that the eight cases are **keeper-level simulations** over a recording fake store. `time`'s test scope may add only `content` (G-06), and `time/build.gradle.kts` is edited only by TASK-060.
  - Pin the real wiring in `app`: `TimeStoreRoundTripTest`'s reset leg makes a flush due before Erase and wires `onReset` exactly as `AppViewModel` does, with real `BrowseController` and `SettingsController` over a real store wrapped by a call recorder.
- **E2 — §5.1 `account()`, the field map, value shapes: the day rolls only on a credited second.**
  - `account()` sets or rolls `day` only when it credits at least one whole active second. A zero credit leaves `day` as it is, so `NONE` stays `NONE` and "`day == null` before the first active second" holds.
  - Otherwise the corrected `HeldResetAppTest` A1 (frozen clock, asserts `PlayTime.NONE` after Erase and after the relaunch) goes red, because a stop-point flush would write `PlayTime(<pinned day>, 0, 0)`.
  - Add one value-shape line and one `FlushRulesTest` assertion: after `afterReset()` and an account with zero credit, `flush()` writes nothing, because the store skips an equal save.
- **E3 — §7 and DA-140: the pill must not move within an attempt.**
  - **(a)** Say explicitly that the Restart rect counts as an obstacle even while Restart is hidden on a New puzzle (`unplacedUnless`). That is what keeps the pill still at the first drag.
  - **(b)** Make the step decision from a **template width**: the widest text the pill can show in the current language and font scale (for example "59:59" below an hour and "88 h 59 min" from one hour), measured once in the shared `SubcomposeLayout`. Without it, in Finnish at font scale 2.0 on a 360 dp phone, the pill jumps down by one control height at 10:00 (and again at 1 h) next to a wide top-left Restart.
  - Alternatively, record the jump as accepted in DA-140 and on owner item 2.
- **E4 — `TimerPlacementSweepTest` and DA-140: release has no DEV obstacle.** The release twin composes nothing in the secondary slot, so where debug steps the pill below a top-right DEV pill, release keeps it in the corner. The device tests only see debug.
  - Run the sweep for the no-secondary (release) configuration too.
  - Say in DA-140 that the pill's place, not the board, may differ between debug and release.
- **E5 — Acceptance table, `HeldResetTimeAppTest`: token and purpose.**
  - Its assertions (play time and bests erased, only post-reset seconds accumulate) are REQ-034's Statement, not A1's meaning ("every puzzle shows as New"). Tag it `// decision DA-138`, or have it also assert A1's meaning before it carries `REQ-034.A1`.
  - Note that a device test cannot hold a flush due *at the moment* of Erase: the 1 Hz ticker's `accrue()` flushes within a second of a clock advance. The discriminating guards are `OwnershipOrderTest` (1) and (8); the device class is an end-to-end regression check.
- **E6 — §8 `advanceActive`: one thread of record.** Advance the `ManualTimeSource` on the main thread, inside the same `onActivity` as `accrue()`, or make its field `@Volatile`. Then the ticker's main-thread reads never race the instrumentation thread.

**Spot-check verdict: forward**, conditional on E1–E6 being applied before the briefs that read them:
- E1, E2, E5 and E6 before the Acceptance Test Author's T8a / T8b / T8c brief;
- E3 and E4 before TASK-065 and the sweep.

E2 matters most: without it, the corrected `HeldResetAppTest` is red for a reason the design never names.

No new Blocker or Should-level design question remains. F1, S1, S2 and S3 are resolved, and N1–N10 are answered.
