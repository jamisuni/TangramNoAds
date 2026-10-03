# Review — code · WO-004 checkpoint CR-2 (TASK-022 … TASK-025 + moved-in JVM tests, logic only)

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (code-reviewer)
**Inputs:** AGENTS.md, WO-004, tasks.md WO-004 section (v1.2), designs/WO-004-design.md (rev 2 + plan write-back), the design and plan reviews, decisions.md DA-46…DA-68, architecture.md G-09/G-10, ADR-005, `contracts/.../progress/{IProgressStore,SavedGame}.kt`, REQ-003/024/025/026/030/031/032/033/034/046/050 (and 040/041/045 A1), the in-scope sources (`LockSearch.kt`, `PuzzleStates.kt`, `JsonProgressStore.kt`, `PlaySession.kt`, `PuzzleHost/BrowseController/ProgressRestore.kt`), the implementer scaffolding, and the moved-in independent tests (`store/.../{FrozenV1FixtureTest,StoreReaderDamageTest,StoreWriterTest}`, `browse/.../{NavigationTest,LeaveReturnTest,FakeHost,FakeProgressStore,BrowseKit}`) read as behavioural reference. `.swdev/heldout/` not read. Out of scope: Compose UI, `app` wiring (not yet written: `AppViewModel` still holds the WO-003 session).

## Verdict

**revise (spot-check only)** — 0 Blockers, 2 Should, 8 Nit/Note. Nothing in the v1 **format** needs to change: the document, the encodings, the DA-63 bound and the frozen test all re-derive clean. Both Should findings are edits inside `JsonProgressStore.kt` main code that leave the format byte-for-byte as it is, so LOCK-V1 does not depend on them. Cheapest order: apply F1 and F2 first (about 15 lines), re-run `:store:test`, then LOCK-V1, so the frozen test runs against the final writer. No re-review needed beyond a diff spot-check.

**v1 format fit to freeze: yes** (reasons in "Attack 1").

**0 Blockers · 2 Should · 8 Nit/Note**

## Evidence re-derived

- `gradlew :kernel:test :content:test :store:test :play:testDebugUnitTest :browse:testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL (31 s, 47 tasks executed). Result XMLs summed by me: kernel 141, content 47, store 63, play 157, browse 44; 0 failures, 0 errors, 0 skipped. Matches the claim. `v06_module_deps.py` PASS.
- Frozen files: `progress-v1.json` and `progress-v1-fresh.json` in the tree are byte-identical to the staged T4a copies; their SHA-256 (CRLF to LF) equal the pins in `FrozenV1FixtureTest` (`ab1c9d1e…b63c`, `2be7af50…e109`); no CR bytes; the fixture equals the document in design section 2. The three independent store tests and the five browse files are byte-identical to the staged copies (moved in unedited).
- Contract: `contracts/.../progress/*` and `kernel/.../model/*` mtimes are 2026-10-02 06:28-07:28 (before WO-004; `store` main is 2026-10-03). No `IProgressStore` signature or KDoc change. `IProgressStore` is not touched by any in-scope file.
- Token audit: the only `REQ-NNN.An` texts in `store/src/test`, `browse/src/test`, the new kernel/content/play scaffolding are the 10 comment lines of `NavigationTest` / `LeaveReturnTest` (independent tests). No scaffolding file carries a token.
- DA-63 re-derived by hand (not only by the test): `Rational.plus/minus` multiply by the other denominator then `Rational.of` reduces, so a difference of two stored values has numerator at most 2·65536·64 = 2^23 and a dyadic denominator at most 64. `Q2.signum` squares both coefficients (2^46 over 2^12) and `Rational.compareTo` cross-multiplies once more: worst product about 2^59 < 2^63. The only squaring path in the kernel is `Q2.signum` (grep: `Q2.times` is used only on the constant rotation table in `PieceGeometry`). Sound.
- Solved settle re-derived: `SolvedAt(-10_000)`; `PlayArea.animating` is `nowMs - t0 < CONFETTI_MS + 100` = 2 500; the first composition's `nowMs` is at least 0, so the difference is at least 10 000 and no frame is scheduled; `PICTURE_DELAY 600 + FADE 800` and `POP 300` and `CONFETTI 2 400` are all below 10 000 (picture alpha 1, pieces hidden, no pop, no confetti).

## Checklist applied

- [x] **Directives / guardrails** — G-02/G-05/G-06 clean (store depends on kernel, contracts, serialization-json only; V-06 PASS). G-09: F2 (identifiers). G-10: read paths verified never to throw (see Attack 2). DA-64's G-09 conflict is the accepted, logged one; no second conflict found.
- [x] **Contract** — no delta (above). The two behaviours the KDoc does not literally promise (disk failure cannot be "stored before it returns"; a save the reader would reject) are F5 and the DA-47 note.
- [x] **Scope** — no unrequested abstraction. `StoreIo` is a test seam named by DA-64; the internal 3-argument constructor is the only way to inject it.
- [x] **Traceability / token rule** — clean.
- [x] **Hard-stops** — the format freeze is the hard-stop domain and is the point of this review. No other hard-stop touched.
- [x] **Evidence re-derived** — above.
- [x] **Conceptual 20 %** — attacks below.

## Attack results

### 1. The v1 format before the freeze — fit to freeze: yes

- **Completeness (WO-005…009).**
  - Play time: `playTime {day, todaySeconds, totalSeconds}` matches `PlayTime` and REQ-029 ("today restarts at local midnight" needs exactly the local `day`).
  - Per-puzzle time and best time: `puzzleSeconds`, `bestSeconds` (explicit null) match REQ-030. REQ-030's aid rule and REQ-046 A3 ("marked solved, no best time") are representable as `state "solved"` with `bestSeconds` null (or unchanged). No separate "solved by aid" flag is needed because nothing in any REQ reads it.
  - Settings: `timerShown`, `soundOn` are the two values of REQ-031/033.
  - Reset: REQ-034 needs nothing beyond what `resetAllProgress` already clears.
  - `lastShown` for decisions F4.
  - I read REQ-005, 029-034, 046 for any stored datum that is not here: none. The one structural omission is a profile level (REQ-025 "exactly one profile now, layout must not prevent several"). A v2 would wrap the v1 content; that is the logged DA-46 reading and does not need to be decided now.
- **Encodings and the DA-63 bound.** `{"a","b"}` strings `"n"`/`"n/d"`, normalized, |n| at most 65536, d in {1,2,4,…,64}: re-derived above and pinned by the kernel proof, the content test and the store reader tests (d = 63, 1/128, 2^62, "3/1", "2/4", "-0"-class strings). The content test `DyadicCoordinatesContentTest` guarantees every packaged coordinate fits, and `at = anchor − offset` stays dyadic because the rotation offsets add only a factor 2.
- **Unknown-field tolerance.** Unknown keys are ignored at the top level and in an entry (`unknownKeysAreIgnoredAndAreNoDamage`) and are not preserved on the next write. That is consistent with G-09 only because every addition must be a version bump; worth one line in the v2 migration brief (a v1 binary that round-trips a "v1.1" document would drop the new field).
- **Does the frozen test freeze the meaning?** Yes for `at`: `worldCornersOfTheFixtureBoardPiecesFreezeTheMeaningOfAt` pins the exact world corners of MT (turn 0) and of the mirrored PG at turn 7 through `PieceGeometry.corners`; I re-derived the PG corners from the kernel's shape table and the F/R formula (they match the literals), so a swapped x/y, a swapped a/b, a flipped rotation sense or a mirror in the wrong axis fails. The `theFixtureHoldsExactlyTheTwoBoardPieces…` test guards the guard. Null encodings: `progress-v1-fresh.json` pins `lastShown`, `day` as explicit null and `puzzles: {}`; `progress-v1.json` pins `bestSeconds: null`. The writer test compares parsed trees (JSON object equality, so a dropped null or a renamed key fails) and asserts the top-level key order.
- **What could drift without failing the frozen test:** (a) key order below the top level, and the pretty/compact layout (accepted by DA-68 (1): trees, not bytes; the reader does not care); (b) the *absent piece means resting* convention and the meaning of a **tray** piece's turn: no fixture piece is absent, and the tray `turn` frame is the same as the board one by convention only (see F7); (c) the move-aside behaviour of a newer version, which lives in the unfrozen `StoreReaderDamageTest`. None of these changes what an existing v1 file means to a reader, so none blocks the freeze.

### 2. Store robustness

- **Reader table vs section 2 / DA-64 / DA-68.** Every row matches, including the strictness of DA-68 (5) (`n/1`, `2/4`, leading zeros and `-0` fail because `Rational.toString()` must equal the input) and the `lastShown` rows. A missing `bestSeconds` key is entry damage (not null), consistent with "always written". Re-derived against `StoreReaderDamageTest` (29 cases) and the scaffolding. Observed one design reading worth recording: a transient I/O error on `readBytes` is treated as an unreadable document and the *good* file is moved aside (design row 2); recoverable by hand from `progress-bad-*`, never lost.
- **`ATOMIC_MOVE` → `REPLACE_EXISTING` fallback: not crash-safe.** F1.
- **`dirty` and skip-equal.** Correct: every save compares only when `!dirty`; a failed write sets `dirty` and the next save of any value (even an equal one, tested by `writeFailureNeverThrowsMemoryStaysCurrentAndNextSaveRetries`) rewrites the whole document. Memory is updated before the write. `sync()` does not retry a dirty store (F4).
- **Eager partial copy.** The copy happens once per construction, before any write, so "before the first write of the run" holds. Side effects in F3.
- **Newest-3 across families.** Implemented by the stamp in the file name (fallback `lastModified`), tested across `bad`/`v2`/`partial` in `onlyTheNewest3AsideFilesSurvive`. The retention can evict the only copy of a newer-version document (F3).
- **`sync()` on the file and the directory.** Both are forced inside separate `runCatching`s; on Windows the directory open fails and is swallowed (so the Windows JVM tests prove nothing about the directory half; the seam test `syncForcesTheFileAndTheDirectoryAndNeverThrows` proves the call order). On Linux/Android `FileChannel.open(dir, READ)` plus `force(true)` is a valid directory fsync. Fine.
- **G-10, read paths.** `init` wraps `load()` in `runCatching(Throwable)` and falls back to fresh values; inside `load` the document, every entry, `playTime` and `settings` are separately caught as `Throwable` (a deeply nested document that overflows the parser's stack lands in the outer catch). `progress`, `settings`, `playTime`, `lastShownPuzzle` read memory only. `PuzzleId(it)` is an unchecked value class, so an empty or odd `lastShown` cannot throw.
- **G-10, write paths.** `write()` catches `Throwable` around encode and around `writeAtomic`; `saveX` itself only compares and assigns. "Stored before it returns" holds for every save except the documented disk failure and the skip of an equal value (which is equal to what is on disk unless `dirty`).
- **Writer vs reader symmetry.** F5.

### 3. Restore path

- **`ProgressRestore.sanitize`.** Tray order (`TrayRules.order`), `isValidPlacement` inside `runCatching`, `accepted` grows only with valid pieces, foreign pieces dropped, Solved gives no pieces (state and best time kept), New `OnBoard` gives `InTray` with turn and mirror kept, In progress tray entries pass through. When two saved pieces overlap, the earlier in tray order wins, not the earlier locked: deterministic and matches DA-50. The `runCatching` is redundant (isValidPlacement is already fail-closed) and its test cannot tell (F6).
- **Construction exceptions.** `Silhouette(...)` and `PlacedPiece(...)` take library data and values `isValidPlacement` already accepted; nothing stored reaches an unguarded call. I could find no way for stored data to throw out of `sanitize` or `PlaySession.restore` (`Turn(0..7)` is range-checked by the reader, `Where.Board(at)` has no check). Defence in depth: F9.
- **`PlaySession.restore` `check(drag == null)`.** Not reachable through `BrowseController`: `showAt` is the only caller of `host.show`, and the design builds a **new** session per `show`, so `drag` is null by construction. `start()` is unguarded but runs at launch only; `open`/`restart`/`openGrid` are guarded by `isDragging`. The assumption "`SessionHost.show` constructs a new session" is `app`'s (TASK-028) and must be checked there: if it reused a session, the `check` would be a crash path from a tap.
- **Solved settle.** No replay (no `onChanged`, `solved` is set directly), no best time (never touches `bestSeconds`), no frames when idle (re-derived above). DA-66 complete In-progress restore goes through the same `settle()`; the *store* keeps `IN_PROGRESS` until the next `persist` rewrites it as Solved without pieces, and `stateOf(i)` for the grid reads that stale value until the puzzle is visited (only reachable after a content update; no action).
- **DA-66.** Re-derived: `board && SolvedCheck.isSolved(...)` after applying; correct.

### 4. Save ordering

- **`onChanged` vs DA-49 / decisions F4.** `release` clears the drag, updates `pieceList`, `state` and `solved`, then calls `settled()` last, so the capture sees the final state; `tapTray`/`flipTray` call it after `update`; `refit` calls it only on success (a refused turn shakes and does not call it). `beginDrag` (including NEW→IN_PROGRESS) calls nothing, `interruptDrag`, `toProgress`, `restore` call nothing. A second finger during a drag is `Ignored` by `GestureMachine` (line 77), so a tray tap cannot call `onChanged` → `persist` → `interruptDrag` under a live drag.
- **`toProgress` interrupts first**, then reports every piece in any state; the drag cannot be recorded.
- **`BrowseController`.** `open`: guard, `persist()` (capture, canonicalize, save), `saveLastShownPuzzle`, then `showAt`: capture happens before the session is replaced, and a kill between the writes leaves a consistent store. `restart()`: guard, decision from `host.state`, one write of `store.progress(id).restarted()` (keeps the best time), then `showAt`; reading the host rather than the store is right because the host is the truth for the shown puzzle. `nextUnsolved` checks `isDragging` and then `open` checks it again (harmless). `start()` writes nothing (tested, F4 row iv). `canonical()` drops only resting, unmirrored tray entries and keeps all board entries.
- No ordering defect found.

### 5. Contract

No signature or KDoc change. `resetAllProgress` leaves `lastShown` (not promised either way by the KDoc). The write-failure departure and the DA-64 both-failed overwrite are the two logged notes. One undocumented consequence for WO-007: a reset leaves `progress-bad-*`, `progress-v*` and `progress-partial-*` aside files holding the previous player's progress (N8).

### 6. Test substance

- **Independent tests** are strong: they assert values, not calls. `req003_A2` runs through the real `sanitize` (3 valid solution poses survive); `decisionF5_everyEntryIsANoOp…` asserts no show and no write under a dragging host; the `FrozenV1FixtureTest` freeze is two-way with a world-corner oracle. Under-assertion: none against the REQs they carry (REQ-025 A1-A3, 026 A1, 050 A1/A3 are held out by design). Over-assertion: `req045_A1` also asserts mini/rating 1/3 pieces (allowed by design E6, content-level but harmless). The adapters fail loudly (`error(…)`), no silent `?.invoke`.
- **Scaffolding.** No tokens. Two "exception" tests cannot tell the exception path from the ordinary invalid path (F6); one conditional assertion (F8). Everything else asserts concrete results (round trips, file listings, write counts, the aside names).
- **DA-63 proof** is convincing: analytic bound plus measured grid plus a negative control (coprime 63/61 really overflows), over `READING_ORDER`, `Q2.compareTo` and `signum`. One gap (N: not run end to end through `find` with an extreme *placed* piece as anchor) is covered by the direct comparator test; no action.

## Findings

### F1 — ROBUSTNESS / G-09 · Should · store/.../JsonProgressStore.kt:436-438 (`RealStoreIo.writeAtomic`) · owner TASK-023
- **Observation:** when `ATOMIC_MOVE` throws `AtomicMoveNotSupportedException`, the code retries with `REPLACE_EXISTING`. In the JDK, a non-atomic `Files.move` over an existing target first **deletes the target** and then renames (UnixCopyFile on Linux/Android, WindowsFileCopy on Windows). A process kill or power cut between the delete and the rename leaves **no `progress.json`**, only `progress.json.tmp`; the reader ignores `.tmp`, so the next launch is a silent fresh install of all progress. So the fallback is not crash-safe, and it is exactly the case where the file system has just told us it cannot rename atomically. In practice the branch is unreachable (temp file and target share a directory, `rename(2)` within one directory is always atomic on the app-private ext4/f2fs), and no test covers it, so it is dead code with the one failure mode the atomic write exists to prevent.
- **REQ/decision:** DA-47 ("atomic rename"), REQ-025 (state survives closing the app), G-09/G-10.
- **Proposed resolution:** delete the `catch (AtomicMoveNotSupportedException)` fallback and let the exception fall to the outer catch (the save is then a counted write failure with `dirty = true`, retried by the next save, memory current). If a fallback is wanted, it must keep the old file until the new one is in place (write `progress.json.new`, never delete the target first), which `Files.move` cannot do. Update the class KDoc / design text "falling back to REPLACE_EXISTING" accordingly (log as a small DA).

### F2 — GUARDRAIL G-09 · Should · store/.../JsonProgressStore.kt:178, 201, 325, 368 · owner TASK-023
- **Observation:** G-09 says the document uses "explicit, stable serial names that are independent of Kotlin identifiers", and design section 2 says "never `enum.name`". The code writes and reads piece keys with `PieceId.name` (`pieces[id.name]`, `PieceId.entries.firstOrNull { it.name == k }`) and writes rationals with `Rational.toString()` and validates them by `r.toString() != s`. `PieceId.name` and `Rational.toString()` are Kotlin identifiers / kernel formatting (kernel `model` is notify tier). Renaming an enum constant or changing `toString` would silently change the on-disk format of v1. The frozen test would catch it, and the Kotlin rename would also have to touch the fixtures' literals, so the risk is detection-after-the-fact, but the guardrail asks for independence, and the cost now is small and format-neutral.
- **REQ/decision:** G-09, DA-46, design section 2 first paragraph.
- **Proposed resolution:** in `store`, a private table `PieceId` ↔ `"LT1","LT2","MT","SQ","PG","ST1","ST2"` (`when` both ways, exhaustive) and a private `formatRational(Rational)` (`"$n"` or `"$n/$d"`), used by writer and reader (the reader compares the parsed rational's formatted string with the input). Output stays byte-identical, so the frozen fixtures and test pass unchanged. Do this before LOCK-V1 so the frozen test runs against the final writer.

### F3 — ROBUSTNESS · Nit · store/.../JsonProgressStore.kt:267, 297-309 · owner TASK-023
- **Observation:** the partial copy is made at construction, once per launch. A document with a damaged section or entry that is never rewritten (a session with no differing save: `start()` writes nothing and skip-equal skips the rest) produces a new identical `progress-partial-<now>.json` on every launch, and `prune()` then keeps only the newest 3 across all families, so three such launches evict an older `progress-v<N>-*` (the only copy of newer-version data) or `progress-bad-*`.
- **Proposed resolution:** make the partial copy lazy (do it in `write()` on the first write when `needPartialCopy`), matching the design wording "before the first write of the run", or skip the copy when the newest partial already has identical bytes. Optional; low likelihood.

### F4 — ROBUSTNESS · Nit · store/.../JsonProgressStore.kt:128-131 (`sync`) · owner TASK-023
- **Observation:** `onStop` is the last chance to persist; if an earlier write failed (`dirty`), `sync()` forces the old file and does not retry the write.
- **Proposed resolution:** `if (dirty) write()` at the top of `sync()`. One line plus a scaffolding test through `ScaffoldIo`.

### F5 — CONTRACT / ROBUSTNESS · Nit · store/.../JsonProgressStore.kt:87-91, 176-201 · owner TASK-023 (doc) / WO-008
- **Observation:** the writer accepts values the reader rejects: negative `puzzleSeconds`/`bestSeconds`/play-time seconds, and an `OnBoard.at` outside the DA-63 bound or with a non-dyadic denominator. Such a save returns normally, is read back correctly in memory, and then reads as New (the entry) or fresh (the section) in the **next process**, silently, which departs from the KDoc "closing, killing or updating the app afterwards loses nothing that was saved". Today it cannot happen: `DyadicCoordinatesContentTest` pins the content and WO-004 writes no times.
- **Proposed resolution:** state the writer's precondition in the class KDoc (non-negative seconds; in-bound dyadic coordinates) and in the WO-008 brief (`savePlayTime`/`puzzleSeconds` must be non-negative). Optionally add a `failedWrites`-style counter or a `log` when an entry fails the reader's own check at write time.

### F6 — TEST · Nit · browse/src/test/.../BrowseControllerScaffoldingTest.kt:311-320; kernel/src/test/.../lock/ValidPlacementScaffoldingTest.kt:79-85 · owner TASK-025 / TASK-022
- **Observation:** `sanitizeGeometryExceptionSendsPieceToTray` (huge = `MAX/2`) and `overflowingPlacementIsFalseNotAThrow` (`MAX/4`) claim to exercise an exact-arithmetic overflow, but by reading `Rational.plus` the sum `huge·1 + offset` does not overflow for these values on every corner (the offsets are small integers or halves; `multiplyExact(MAX/2, 2)` fits), so the asserted result most likely comes from the ordinary "outside the silhouette" path. Both would still pass with `runCatching` / the `catch (e: Exception)` deleted, and `isValidPlacement` already catches, which makes the `runCatching` in `sanitize` redundant. I did not run an instrumented version; this is a reading.
- **Proposed resolution:** precede each with a precondition assert that the same arithmetic does throw (for example `assertThrows(ArithmeticException) { Q2(Rational.of(Long.MAX_VALUE), Rational.ZERO) + Q2(Rational.of(1), Rational.ZERO) }` and use `Rational.of(Long.MAX_VALUE)` in the coefficient under test), or drop the claim from the test name. Keep the redundant `runCatching` (defence in depth) but say so.

### F7 — TEST / FREEZE · Nit · store/src/test/.../FrozenV1FixtureTest.kt · owner TASK-T4a (before lock, optional)
- **Observation:** (a) the frozen test pins what an *absent* piece means only indirectly (no fixture puzzle omits a piece, and `TrayRules.restingTurn` is not asserted anywhere in `store`); `canonical()` and `restore` both depend on "absent = TYPE-001 resting turn, unmirrored", which is the `SavedGame` KDoc's text. (b) The tray `turn` frame (same R·F·local convention as the board) is pinned only by the contract KDoc. Both are covered at kernel/play level by TYPE-001 resting-turn tests, not in a file that is guard-locked.
- **Proposed resolution:** none required for the freeze. If the author wants it cheap: one more case in `FrozenV1FixtureTest` asserting `TrayRules.restingTurn` for the five shapes against the TYPE-001 literals. If not, leave as is: the v1 file meaning does not change either way.

### F8 — TEST · Nit · play/src/test/.../SessionObservationScaffoldingTest.kt:69-73 · owner TASK-024
- **Observation:** the `tapBoard`/`flipBoard` version-bump assertions sit inside `if (s.state != SOLVED && s.placed.any {…})` (the CR-1 F3 class: a conditional that could skip every assertion), and `assertTrue(pose.piece == piece)` at line 73 is a tautology. For the first library puzzle (3 pieces) the condition is true after one lock, so it does run today.
- **Proposed resolution:** `assertTrue(cond)` before the block (or `check`), and delete line 73.

### F9 — G-10 · Nit · browse/.../BrowseController.kt:39-45, 83-88 · owner TASK-025
- **Observation:** `open()` writes `lastShown` before `showAt`, and `start()` has no fallback: if `host.show` ever threw for one puzzle, every relaunch would reopen that puzzle and throw again (crash loop). I verified that no stored datum can reach a throwing call today (see Attack 3), so this is defence in depth only.
- **Proposed resolution:** wrap `ProgressRestore.sanitize` in `runCatching(...).getOrDefault(PuzzleProgress.NEW)` inside `showAt`, so an unforeseen exception shows the puzzle as New instead of looping; add one test with a throwing store entry.

### F10 — NIT · store/.../JsonProgressStore.kt:52-61 · owner TASK-023
- **Observation:** `movedAside`, `partialCopy`, `asideFailed`, `failedWrites` are `internal var`; the frozen seam in design "Test seams" is `internal val`. Harmless, but the setter lets a test write them.
- **Proposed resolution:** `internal var x … private set` (the same module can still read).

### Notes for later work orders (no action in CR-2)
- **N1 (WO-007):** `resetAllProgress()` leaves the `progress-bad-*`, `progress-v*`, `progress-partial-*` files, which can hold the previous player's progress; REQ-034 is about game state and nothing displays them, but a "hand the device to another player" reset may want to delete them.
- **N2 (WO-008):** `shownBestSeconds` is not snapshot state; `puzzleSeconds`/`bestSeconds` must be written through `store.progress(id).copy(…)` (O-08) with non-negative values (F5).
- **N3 (TASK-028):** `SessionHost.show` must build a new `PlaySession` (the `restore` `check` relies on it) and `host.onChanged` must be set before `start()` only if `persist()` before the first `show` stays a no-op (it does: `capture` returns null).
- **N4 (WO-007 v2 brief):** because unknown keys are not preserved, any v1.x additive field must be a version bump.

## Handoff — Code Reviewer · WO-004 CR-2

- **Verdict:** revise (spot-check only). 0 Blockers · 2 Should (F1 fallback not crash-safe, F2 identifiers in the format) · 8 Nit/Note.
- **v1 format fit to freeze: yes.** Document, encodings, DA-63 bound, reader table and the frozen test all hold; the two Should fixes do not change one byte of the format and can land before or after LOCK-V1 (before is cheaper to verify).
- **Next:** TASK-023 applies F1 and F2 (and optionally F3-F5, F10); TASK-025 F6/F9, TASK-024 F8, TASK-022 F6 at its leisure; orchestrator spot-checks the `JsonProgressStore.kt` diff, re-runs `:store:test`, then LOCK-V1.

## Spot-check (re-review)

**Final verdict: forward.** All findings are closed or accepted. 0 open Blockers, 0 open Should. **v1 format fit to freeze: yes** (still).

Re-run by me: `:kernel:test :store:test :play:testDebugUnitTest :browse:testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL (45 tasks executed). The coordinator reports 141/47/68/157/44 with 0 failures. Frozen fixtures: SHA-256 (CRLF to LF) unchanged (`ab1c9d1e…b63c`, `2be7af50…e109`), files byte-identical to the staged copies. `FrozenV1FixtureTest.kt` is byte-identical to its staged copy (the staged copy carries the two new tests). The hash literals are unchanged and the existing assertions are intact.

- **F1 closed.** `RealStoreIo.writeAtomic` is a plain `ATOMIC_MOVE`; any failure deletes the temp file and rethrows into `write()`'s catch (counted, `dirty = true`). New test `refusedAtomicMoveIsACountedWriteFailureWithNoFallback` injects `AtomicMoveNotSupportedException` through `ScaffoldIo`.
- **F2 closed.** `pieceName(id)` is an exhaustive `when` (no `.name`); `formatRational` is local; the reader validates with `formatRational(r) != s`. Output unchanged: frozen writer tests pass untouched.
- **F3 closed (different from my proposal, accepted).** `prune()` now covers `progress-bad-*` and `progress-partial-*` only; `progress-v<N>-*` is never pruned and not counted (test at ScaffoldJsonProgressStoreTest:232-249). Trade-off: newer-version files accumulate without bound; negligible (one per version per downgrade event).
- **F4 closed.** `sync()` calls `write()` first when `dirty`.
- **F10 closed.** `private set` on the four seam members.
- **F6 closed.** The kernel test now asserts the raw `ArithmeticException` from `PieceGeometry.corners` (Long.MAX_VALUE plus a positive offset) before `assertFalse`, so it fails if the catch is removed. Browse: the unreachable-`runCatching` ruling is sound; the renamed test states its limits.
- **F7 closed.** Two assertion-only tests: absent pieces and TYPE-001 resting turns, and the tray-turn frame via `PieceGeometry.offsets` (SQ turn 3, mirrored ST2 turn 4); the literals match the hand derivation in their comments.
- **F8 closed.** `setDragTurn` now sets the solution turn and `assertTrue(placed…)` precedes the board intents, so they run.
- **F9 accepted** as a TASK-028 brief item; **F5 accepted** (writer fed only by sanitized/session values; WO-008 must pass non-negative seconds).

Open items: none for CR-2. Carry to TASK-028: `SessionHost.show` builds a new session and never throws (F9, N3). LOCK-V1 may proceed.
