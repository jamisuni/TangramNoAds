# Review — design · WO-004

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:** `designs/WO-004-design.md` (Draft, 2026-10-03) · `workorders/WO-004.md` · AGENTS.md (incl. the WO-003 lessons) · REQ-003, 024, 025, 026, 050; carried REQ-040, 041, 045; touched REQ-005, 012, 023, 029–034, 037, 046 (collection v1.1, locked) · `req_types.md` v0.2 (TYPE-001, 003–006) · `architecture.md` v1.0 (G-01…G-10, O-01…O-09, §4 registry, §5 test placement) · ADR-002/004/005 · `decisions.md` F2–F20, DA-1…DA-45 · `build-map.md` v1.2 · `design-inputs.md` §1–2 · `contracts/.../progress/{IProgressStore,SavedGame}.kt`, `contracts/.../puzzle/*` · `kernel/.../{model/Exact.kt, model/Pieces.kt, geometry/Arithmetic.kt, geometry/PieceGeometry.kt, geometry/Silhouette.kt, lock/LockSearch.kt, lock/SolvedCheck.kt, state/PuzzleStates.kt}` · `play/.../{PlaySession,PlayArea}.kt`, `play/.../draw/PictureDrawing.kt` · `app/src/**` (main, debug, release, testRelease, androidTest) + `app/build.gradle.kts`, `settings.gradle.kts`, `play/build.gradle.kts` · `.swdev/guard.json`, V-05, V-06 · `Spec/02-ui-layout.md` · `tools/prototype_template.html` (storage, `loadPuzzle`/`persistPieces`, `restart`, `nextUnsolved`, grid, long press, reset) · `Tangrams/shapes-mini-{1,2}.json` · Compose `ui-android` 1.12.1 (bytecode of `LayoutNode.setCompositionLocalMap`, `SuspendingPointerInputModifierNodeImpl.getViewConfiguration`) · framework: review-report template, traceability-rules §5, orchestration §5. `.swdev/heldout/` not read.

## Verdict

**recirculate → design-author.** No Blocker. The shape is right:
- one `PlaySession` per visit, always built from the store;
- `browse` behind the `PuzzleHost` seam;
- a JVM `store` with an in-memory model and atomic replace.

The v1 field list is complete for WO-005…009 (checked against REQ-005, 029–034 and 046), and no `IProgressStore` signature moves. Still, eight Shoulds need a design pass before the Planner and the parallel test author start:
- four sit in the flagged area, the restore path and the freeze (F1, F2, F3, F7);
- two contradict the Contract: the binding F4 reading (F3) and locked REQ-037 (F5);
- three change frozen seams a fresh test author would otherwise mis-guess (F1, F6, F8).

Each fix is small. Re-review can be a spot-check of §2–§5, the seam and acceptance tables and DA-46…62.
**0 Blockers, 8 Shoulds, 9 Nits.**

## Checklist applied

- [x] **Directives.**
  - D1: `browse` is the #Browsing slice; `store` a support module; `app` wires.
  - D2: proportionate overall. Simpler options are offered in F6 (one policy point), F7(b) (a damaged section is a bad document) and N2 (`combinedClickable`).
  - D3: every new abstraction names its REQ:
    - `PuzzleHost`: G-06 + trace rule 3;
    - `ProgressRestore`: REQ-025 A3;
    - `isValidPlacement`: O-01;
    - `onRestart`/`onRetry`: TYPE-006, O-07;
    - `PuzzleThumbnail`: REQ-050, O-09.
  - D4: DA-58 decides WO-007's post-reset behaviour now (F4).
  - D5: no new library. `BackHandler` comes from activity-compose 1.13.0; serialization-json is already allowlisted (ADR-004, DA-13 tree parser).
- [x] **Guardrails.**
  - G-01: the save sits in `filesDir`; `allowBackup="false"` and both extraction blocks exclude `file`; the manifest is untouched; no network.
  - G-03: `at` is stored exact; doubles appear only in `sanitize`'s inside/overlap test, which G-03 allows.
  - G-05: en+fi for every new key (wording: N5).
  - G-06: the main deps of `browse` and `store` are `kernel` + `contracts` (+ libraries); V-06's table already lists both modules.
  - **G-09: F7. G-10: F2** (a crash path from stored data) and F1 (the relaunch path).
- [x] **Contract.**
  - No `I*` signature changes; `contracts/` and `kernel.model` are untouched.
  - Two semantic departures from the `IProgressStore` KDoc (F4).
  - Binding readings: F4 is narrowed by DA-48 (F3); the F5 guard has a gap (F8).
  - Locked REQ-037 is contradicted (F5).
- [x] **Scope.** Everything traces. DA-56 removes scope (the debug extra), it adds none.
- [x] **Traceability.** REQ links are present, and every ID has a code-home row on paper. But the REQ-026 A1 `browse` test would test the fake (F6), and `start()` has no `browse` test (F1). The carried REQ-040/041/045 A1 have code home `content`, whose WO-002 tests already carry the tokens (`content/.../acceptance/Req038Req040Req041Req045VisibleTest.kt`). So app-only on-screen tests are additions under architecture §5 rules 3–4.
- [x] **Hard-stops.** The v1 freeze is declared (governance row 11, ai+inform). Its enforcement: F7.
- [x] **Evidence re-derived.**
  - `IProgressStore`: its ten operations match the list at the end of §2. The KDoc promises "Every save is stored before it returns" (line 17) and `lastShownPuzzle()` "`null` on a fresh install" (lines 52–53); the `resetAllProgress` doc (lines 60–66) does not name the last-shown puzzle.
  - `PlaySession` today: `state` is a plain `var` (not observable); `interruptDrag()` is public and idempotent; `SolvedAt(t0, reducedMotion)`. `PlayArea.animating` is `nowMs − t0 < CONFETTI_MS + 100`, and a new session seeds `nowMs` from `lastFrameMs = 0`. So `t0 = −10 000` is settled from the first frame, as §4 claims.
  - Exact arithmetic:
    - `Rational` plus/minus/times/compareTo all use `Math.multiplyExact` (Exact.kt:16–18, Arithmetic.kt:15–36);
    - `PieceGeometry.corners = offsets + at`;
    - `Q2.signum` squares both parts;
    - Exact.kt:10 makes overflow a thrown programmer error (F2).
  - Prototype:
    - reset calls `loadPuzzle(0)`;
    - `nextUnsolved` stays on the current puzzle when it is the only unsolved one;
    - `persistPieces` drops a New puzzle's tray turns;
    - the grid closes with `done` ("Done"/"Valmis");
    - an invalid saved piece goes to the tray in its resting turn. DA-50 keeps the turn, which follows F2, and the prototype is not the spec.
  - `Tangrams/shapes-mini-2.json` holds MT/ST1/ST2, but §2's example puts SQ on it (F7).
  - No WO-003 device test samples the tray after a solve, and no app test uses the `puzzle` extra. So DA-52 and DA-56 break no existing test. The app tests do assume a fresh first puzzle, so the `ResetStoreRule` is needed.
  - `.swdev/guard.json` `contract_paths` holds no `store` path (F7).
  - Compose 1.12.1: `LayoutNode.setCompositionLocalMap` sets the node's `viewConfiguration` from `LocalViewConfiguration`, and the pointer-input scope reads it (N2).
  - Grid: `Adaptive(84 dp)` at 360 dp with 16 dp padding gives 3 columns of about 109 dp, as §Risks says.
- [x] **The conceptual 20 %.** Spent on §2–§4 (format, reader rules, restore, save ordering), the `PuzzleHost` seam, the DA-46…62 readings and the acceptance table.

## Trajectory & quality

- **Verification actually run?** This is a design; I checked its factual claims above. Three are wrong or unsupported:
  - the reason for rejecting `combinedClickable` (N2);
  - "no contract delta", which holds for signatures, not for semantics (F4);
  - "the relaunch path is the one every navigation exercises", which the seams do not guarantee (F1).
- **Proportionate?** Yes. The seam costs four members, `store` is one class, and no library is added. The section-level read tolerance goes beyond ADR-005, and that is where data can vanish silently (F7).
- **Path sane?** First pass; no thrash.

## Findings

### F1 — QUALITY · Should · design §5 `start()` (line 200) vs `open(i)` (line 203) / REQ-025 rule 4 + A3, G-10
- **Observation:**
  - `open(i)` restores through `ProgressRestore.sanitize`. `start()` only "calls `show`", and `open(i)` is a no-op for `i == index`, so `start()` cannot reuse it. Nothing pins that the launch path sanitizes.
  - The launch path is exactly the case REQ-025 rule 4 exists for: an app update changes a puzzle file, and the next launch opens `lastShown`, usually the puzzle the player was working on.
  - §0's claim ("the code path that restores after a relaunch is the one every navigation exercises") holds only if `start()` sanitizes. `restart()` (line 205) is a third route to `show`, with its own argument.
  - The acceptance table has no `browse` test of `start()`. The REQ-025 A3 row tests `sanitize` alone, and fresh install, `lastShown` and unknown `lastShown` are covered only on the device.
- **Proposed resolution:**
  - One private `showAt(i)` = `index = i; host.show(p, ProgressRestore.sanitize(p, store.progress(p.id)))`, used by `start()`, `open()` and `restart()`. State it in §5 and in the seam table.
  - Add `browse` JVM rows:
    - (i) empty store: `index` 0, `show(puzzles.first(), NEW)`, nothing written;
    - (ii) `lastShown` = a known id with a displaced board piece: that index, and the piece arrives `InTray`;
    - (iii) unknown `lastShown`: `index` 0;
    - (iv) `start()` writes nothing.

### F2 — GUARDRAIL · Should · design §2 reader rules, §5 `sanitize` / G-09 ("stored data never crashes the app"), G-10
- **Observation:**
  - The reader checks a rational only for syntax: `-?\d+(/\d+)?`, `Long` range, denominator ≠ 0. It accepts any magnitude.
  - The kernel's exact arithmetic assumes small lattice values and throws on overflow: `Rational` plus/minus/times/compareTo use `Math.multiplyExact` (Arithmetic.kt:15–36, Exact.kt:16–18), and Exact.kt:10 makes overflow a programmer error.
  - **Launch crash.** `sanitize` → `isValidPlacement` → `fitAt` → `PieceGeometry.corners = offsets + at`. Take a stored `at` component of `"1/4611686018427387904"` (2^62): it parses, but adding a piece offset such as `2` computes `multiplyExact(2, 2^62)` and throws inside `sanitize`.
    - If that puzzle is `lastShown`, **every launch crashes** until the player clears the app's data.
    - The per-entry `runCatching` (line 119) wraps only the parsing.
  - **Later crash from touch.** A value that survives `sanitize` must sit within the 1e-6 area tolerance. A shift that small needs a denominator in the millions. Such a value becomes an anchor, and `Q2.signum` (it squares both parts) can then overflow in `READING_ORDER` tie-breaks during a lock search: a crash from touch.
  - Two kernel premises assumed puzzle files and locks are the only sources of exact values: `LockSearch.find`'s KDoc ("nothing here throws for touch input") and DA-25's "unreachable". WO-004 adds a third source, the save file.
  - Only a hand-edited or oddly corrupted document gets here. But G-09 and G-10 are absolute, and §Risks lists "per-entry damage tolerance" as the mitigation.
- **Proposed resolution:** do both, and decide them now, because the reader rules are frozen with v1.
  - (a) **A v1 reader bound.** A rational whose |numerator| or denominator exceeds a fixed bound is entry damage, so the puzzle reads as New.
    - Choose the bound from the kernel's worst case: a lock search over a board of in-bound values must not overflow. Real positions have denominators ≤ 8 and small numerators.
    - Prove the bound, or pin it with a kernel test that runs `find` and `isValidPlacement` at its extremes.
  - (b) **`sanitize` fails closed.** Any exception from the geometry sends that piece to the tray (DA-25 style).
  - Tests: a store test with the 2^62 document, and a `browse` test where `sanitize` receives an out-of-bound piece.

### F3 — CONTRACT · Should · design §3 "What" table, DA-48 (New half) / decisions F4 (binding), ADR-005 cadence
- **Observation:**
  - DA-48 saves `pieces = {}` for a New puzzle. Tray turns and mirrors made before any piece leaves the tray are therefore lost on leave, relaunch or kill.
  - decisions F4 is one of the owner-accepted readings that `architecture.md` names part of the Contract. It says: "persisted after every lock, return, **turn, mirror** …; a kill loses **at most the drag in progress**". ADR-005's cadence repeats "turn, mirror".
  - TYPE-006 says a tray turn does not *start* the puzzle; it does not say the turn is forgotten. REQ-003 A2 ("leaving and returning … never loses its state") points the same way.
  - The prototype drops the turns too (`persistPieces`), but rule 9 says the prototype is not the spec. DA-48's Basis does not mention F4.
  - The v1 format already allows `pieces` for `"new"`. Following F4 needs no format change and no migration.
- **Proposed resolution:**
  - Save the tray entries of a New puzzle; its state stays New, so the grid and `nextUnsolved` are unaffected. `restore(NEW)` applies them.
  - That gives one rule, "pieces for New and In progress, none for Solved", with fewer branches (where it lives: F6).
  - If the author keeps DA-48's New half, it narrows an owner-accepted reading and goes to Jami (Owner items).

### F4 — CONTRACT · Should · design §2 "Semantics", DA-58, value shapes (line 318) / `IProgressStore` KDoc (notify tier), D4
- **Observation:**
  - **DA-58 changes the interface's documented meaning.** It makes `resetAllProgress()` clear `lastShown`, and the value-shape paragraph says `lastShownPuzzle()` is null "after a reset". The KDoc promises null "on a fresh install", and the reset doc lists what is erased without naming the last-shown puzzle. Meanwhile §2 says "Contract delta: none".
  - **It decides WO-007's behaviour now (D4).** Post-reset navigation belongs to WO-007.
  - **It is wrong if WO-007 keeps the player where they are.**
    - REQ-032 says closing settings "returns to the same puzzle state", and O-08 says that after a reset `app` reloads the slices from the store.
    - Then `lastShown` is null while puzzle X is shown, and a relaunch opens puzzle 1. That breaks F4's "relaunch opens the last shown puzzle".
    - The prototype reaches puzzle 1 by navigating (`loadPuzzle(0)`, which also rewrites its `idx`), not by clearing a store field.
  - **A second KDoc departure.** The "suppress all writes for this run" mode (reader table, line 114) and an ordinary write failure (line 61) both break "Every save is stored before it returns" (KDoc line 17). The design does not mention it.
- **Proposed resolution:**
  - Drop the `lastShown` clear: the store's reset erases exactly what the KDoc lists.
  - Carry "which puzzle shows after a reset" to WO-007 as a design question: REQ-032 vs the prototype, possibly an owner question. If WO-007 goes to puzzle 1, `open(0)` writes `lastShown` itself.
  - Record the write-failure exception in DA-47. Then either add one KDoc sentence (a logged notify-tier doc delta) or list it in the WO's contract-delta table as "behavioural note, no signature change".

### F5 — CONTRACT · Should · design §5 `RestartButton` (line 218) / REQ-037 (locked) vs Spec/02 (DI-2)
- **Observation:**
  - §5 makes the Restart pill "≥ 44 dp high, REQ-037's 48 dp applies to controls the REQ names; the pill follows Spec/02 and DA-24".
  - REQ-037's Statement covers "**every** interactive control that a player uses", and A1 says "No player control in any screen has a touch area below 48 dp". The controls named in its Rules are examples.
  - Spec/02 is a design input, and Contract wins (`design-inputs.md` §2). DA-24 covers only tokens "the REQs leave open".
  - WO-006's REQ-037 A1 test would fail on this pill.
- **Proposed resolution:**
  - Give the pill a touch area of at least 48 × 48 dp; the drawn pill may stay 44 dp high inside it.
  - Add a scaffolding bounds check, tagged `// guardrail`/decision (REQ-037 A1 stays WO-006's).
  - Note for WO-007: Spec/02's 44 dp ⚙ has the same problem.

### F6 — TEST · Should · acceptance table row REQ-026 A1 (line 332), §3/§4 placement / trace rule 3, AGENTS "test tokens are coverage claims", WO-003 seam lesson
- **Observation:**
  - The "a Solved puzzle saves no pieces" rule lives in `play` (`PlaySession.toProgress`, §3–4).
  - REQ-026 A1's covering test in its code home is `browse/src/test`: "a solved capture is saved with empty pieces". There, `capture` is the `FakeHost`, which the test author writes, so the assertion can only check what the fake returns:
    - a realistic fake (SOLVED with pieces) makes the test **fail**, because no `browse` code strips the pieces;
    - a fake that returns `{}` makes it pass vacuously.
  - That is the WO-003 mis-guess class, and the "directory theater" that trace rule 3 is meant to prevent. The only real coverage is the `app` device test.
- **Proposed resolution:**
  - Make the storage policy `browse`'s, where #Browsing (REQ-026, SolvedView) lives:
    - `persist()` stores `pieces = {}` when the captured state is SOLVED;
    - `sanitize` drops the pieces of a stored SOLVED entry on read (a hand-edited or older entry).
  - `PlaySession.toProgress` then reports the session as it is: every piece, in any state. That is one rule in one place, and with F3 it is the only state-dependent rule.
  - New `browse` row: "`FakeHost` captures SOLVED with 3 pieces → the store holds `{}`; a stored SOLVED entry with pieces reaches `show()` with `{}`".
  - Keep `restore(SOLVED)` placing nothing as defence in depth (`play` scaffolding).
  - Update §3, §4, the seam rows and DA-48.

### F7 — GUARDRAIL · Should · design §2 frozen fixture + reader rules / G-09, ADR-005, hard-stop 2
- **Observation:**
  - **(a) The freeze is honour-system.** The fixture, its pinned SHA-256 and the expected values all live in `store/src/test`, and `.swdev/guard.json` has no `store` path.
    - A future WO that "needs" a format change can edit all three in one diff, and no guard, verifier or hard-stop fires.
    - The hash only catches an edit that forgets the hash.
  - **(b) A partly readable document is overwritten without a copy.**
    - A damaged section (line 115; for example `puzzles` not an object) reads as fresh, and the next save rewrites the file. Every puzzle's progress is then gone, with no copy left.
    - ADR-005 sanctions per-*entry* tolerance only. Section-level tolerance is new, and for `puzzles` it amounts to an unreadable document, which G-09 says is "moved aside, never overwritten".
    - The same holds, at a smaller scale, for a damaged entry, and for any future reader bug that rejects valid entries.
  - **(c) The null encodings are not frozen.** The fixture pins `bestSeconds: null`, but not `lastShown: null`, `day: null` or the all-defaults `settings` (explicit `null` vs a missing key).
  - **(d) The example is not a real state.** It puts SQ on `shapes-mini-2`, whose pieces are MT/ST1/ST2. It has no negative rational, although a locked `at` (anchor − offset) is often negative.
- **Proposed resolution:**
  - (a) After D-2 creates the fixtures and their guard test:
    - add `store/src/test/resources/fixtures/progress-v1*.json` and the frozen-fixture test file to `contract_paths.locked` (or `hard_stop_paths`);
    - re-freeze the baseline and log it in `decisions.md`.

    An edit then stops for the human, as hard-stop 2 intends. Record this orchestrator step in the design now.
  - (b) Pick one, and add the case to the reader table and the store tests:
    - when any section or entry is dropped on read, copy the original once to `progress-partial-<nowMs>.json` before the first write;
    - or, simpler (D2), treat a damaged top-level section as a bad document (move it aside) and keep only ADR-005's per-entry tolerance.
  - (c) Add a second frozen fixture, `progress-v1-fresh.json`: exactly what the writer produces for a fresh model.
  - (d) Make the example match the library (SQ on `shapes-mini-1`, or MT on `shapes-mini-2`). Include one negative rational, `turn` 0 and 7, and `mirrored: true` on PG.
  - Optional: the guard-1 test also asserts the world corners of the fixture's board piece. That freezes the *meaning* of `at` through the kernel shape table (the `SavedGame.kt` convention), not only its encoding.

### F8 — CONTRACT · Should · design §5 "controls do nothing while dragging" (line 221), `openGrid()` (line 204) / decisions F5 (binding), O-09
- **Observation:**
  - §5 says every control is inert during a drag "because the controller guards `host.isDragging` (one place)". The table guards only `open(i)` (and so ‹, ›, the long press and the grid's Next) and `restart()`. `openGrid()` is a bare flag.
  - A second finger on the counter while a piece is dragged therefore opens a full-screen overlay over a live drag. The drag keeps routing to the `PlayArea` and resolves unseen under the overlay.
  - F5 says other controls ignore touches while a piece is dragged.
- **Proposed resolution:**
  - `openGrid()` is a no-op while `host.isDragging`.
  - Add a `browse` JVM row, tagged `// decision F5`: `open`, `restart` and `openGrid` under a dragging `FakeHost`.
  - State in §5 that every controller entry point a control can reach is guarded. WO-007's ⚙ needs the same.

### N1 — QUALITY · Nit · design §2 "Write", DA-47 (no fsync)
- The trade is acceptable for REQ-025, which is about a kill, not power loss. But the stated consequence is too mild. After a power cut the file can be unreadable, and then **all** progress of every puzzle starts fresh (moved aside), not just the recent saves. Without `fsync`, an atomic rename does not guarantee the new data on every Android filesystem.
- A cheap middle way: still no `fsync` per lock, but `app` calls a concrete-class `sync()` (file data + rename) from `onStop`, when no animation runs. No interface change.
- At least, state the real consequence in DA-47 for the checkpoint.

### N2 — DIRECTIVE · Nit · design §5 › long press; Alternatives row "Long-press placement" (line 293)
- The reason given for rejecting `combinedClickable` ("platform long-press timeout is 400 ms and not adjustable") is wrong.
  - In ui-android 1.12.1, `LayoutNode.setCompositionLocalMap` takes the node's `viewConfiguration` from `LocalViewConfiguration`, and the pointer-input scope reads the node's value.
  - So `CompositionLocalProvider(LocalViewConfiguration provides <a delegate with longPressTimeoutMillis = 500>)` around › gives a 500 ms long press, with semantics `onClick`/`onLongClick`, indication and touch slop for free (D2).
- Whichever is built:
  - use `max(500, platform value)`, so a player's longer accessibility "touch & hold delay" still works and REQ-050's "at least 500 ms" holds;
  - measure the threshold on the pointer-input coroutine clock (`withTimeout`/`delay`), never wall time or event uptime, so the "advance the clock ≥ 500 ms" device test drives it;
  - press › in the REQ-024 A1 and REQ-050 A1 device tests with `performTouchInput`, never `performClick` (WO-003 F2).
- If the custom gesture stays, its semantics `onClick`/`onLongClick` are justified, because without them the node has no action at all. Say so, given the WO-003 N6 precedent.

### N3 — QUALITY · Nit · design §4 `restore(IN_PROGRESS)` when complete
- `state = onPieceLocked(IN_PROGRESS, isSolved)` can produce SOLVED. Example: an update removes a piece from a mini puzzle and the saved pieces now cover it.
- No `SolvedAt` is set and the pieces stay placed. With DA-52 the tray is hidden and the solved bar shows above a board that draws the pieces and no picture. REQ-026 A1 then fails in this edge case, and nothing writes the state until the player leaves.
- Rule: a restore that ends SOLVED is a SOLVED restore (settled picture, nothing placed, no celebration, no best time).

### N4 — QUALITY · Nit · design §5 `SolvedBar`, `shownBestSeconds` / WO-008 integration
- `shownBestSeconds` reads the store, which is not snapshot state. When WO-008 writes the best time in response to the solve event, the bar has already composed at the state change and keeps showing "–".
  - Leave a note for WO-008, or read it through something observable (e.g. re-read it after the time slice writes).
- The private formatter uses `h:mm:ss` from one hour. REQ-029's rule, the prototype's `fmt` and decisions F14 ("Finnish time units 't' and 'min'") use `m:ss`, then hours and minutes.
  - Use that, with unit strings in en and fi, so WO-008 lifts one format, not two.

### N5 — QUALITY · Nit · design §8 strings
- `close_overlay` ("Close"/"Sulje") is AI-written Finnish. The prototype already closes the grid with `done` ("Done"/"Valmis"), and decisions F14 adopts prototype Finnish verbatim. Reuse it and drop one owner-review row.
- Finnish text-to-speech reads "/" as "kautta". Suggested fi wording:
  - `puzzle_counter_description`: "Tehtävä %1$d, yhteensä %2$d. Näytä kaikki tehtävät";
  - `rating_description`: "Vaikeus %1$d viidestä" (en unchanged).
- Pin the text of the `best-time` node: the label plus the value, or the value only.
- Keep the time pattern in a string resource, not a Kotlin literal (G-05).

### N6 — TEST · Nit · seam table: details a fresh author could still mis-guess
- **Puzzle numbers.** REQ-050 A1 says "puzzles 3 and 7", which are the counter's 1-based numbers. The row on line 334 says "from index 3 gives 7" with a 0-based `index`. State `index = n − 1`.
- **Before `start()`.** The controller's values before `start()` are unspecified. Say "undefined; tests call `start()` first".
- **App seeding recipe.** The outer `ResetStoreRule` deletes `progress*.json`. The test then writes through `JsonProgressStore(targetContext.filesDir)` before the activity launches. "Close and relaunch" is a second `ActivityScenario.launch`.
- **Grid and top bar.** DA-55 should say the grid overlay covers the top bar, as the prototype's does. Otherwise a REQ-024 A2 author may assert the title while the grid is open.

### N7 — TEST · Nit · DA-62 (amending the WO-003 app tests)
- The station is right: the WO-004 Acceptance Test Author (D-8), not an implementer. Add:
  - the diff is setup only (the outer `ResetStoreRule` in a `RuleChain`), with every assertion unchanged, and the orchestrator reads that diff;
  - D-7's brief says the WO-003 app tests are expected to fail until D-8 lands, and that D-7 must not edit them;
  - the WO close lists it on the "corrected checks" line as a test-environment amendment. No checker was wrong, so it needs no `proposals.md` miscalibration entry.

### N8 — QUALITY · Nit · wording and consistency
- **DA-50.** It is right for a stronger reason than the one it gives. A correctly saved piece can have **no** corner on any current anchor: it locked against a piece that was later put back in the tray. A "valid lock including an anchor" reading would reject correct saves, so inside-plus-no-overlap is the only safe reading. Put that in DA-50's Basis.
- **§4 DA-26 amended.** "Without touching the store" conflicts with `onPause` persisting on every rotation.
- **DA-57 vs the WO line.** DA-57 "fails with a clear `check`", while the WO-004 scope line says "must not crash (G-10)". Reconcile the WO line; DA-10 makes an empty library a packaging error.
- **STATUS.md.** It still gives Jami the `--es puzzle` adb line, which DA-56 removes.
- **WO-003 code review N4.** Its size-mark half belongs to WO-006 (`tasks.md`). §Scope can say so.

### N9 — QUALITY · Nit · write volume
- Every › rewrites the whole document twice (the leaving puzzle, then `lastShown`), even when the leaving puzzle is an unchanged New one. `saveProgress` could skip the write when the value is equal: less main-thread I/O and flash wear, same semantics.
- `progress-bad-*` and `progress-v*` files pile up without limit. Keeping the newest few is enough.

## Owner items (Jami)

Nothing blocks planning: every fix above can be decided under governance rows 11–13. For the checkpoint-4 surface (ai+inform):
1. **The v1 save document (DA-46).** This is the one irreversible choice of the WO: once it ships, any change is a migration (hard-stop 2). Its fields cover REQ-005, 029–034 and 046; checked here, nothing is missing.
2. **No fsync (DA-47).** A power cut right after a save can reset *all* progress (the file is moved aside, not deleted). A kill or a closed app never loses a completed save. N1 gives the cheap middle way.
3. **Conditional.** If the design author keeps DA-48's rule that a New puzzle's tray turns are forgotten, it narrows decisions F4, which Jami accepted ("a kill loses at most the drag in progress"). That needs his yes, or a CHG.
4. **For WO-007, not now.** After "Reset all progress", which puzzle shows? REQ-032 says closing settings "returns to the same puzzle state"; the prototype goes to puzzle 1. See F4.
5. **Finnish and the debug extra.** The Finnish rows written by the AI (DA-60), with the N5 rewording, and the removal of the `--es puzzle` adb line he was given (DA-56).

## Handoff — Design Reviewer · WO-004
- **Scope:** REQ-003, 024, 025, 026, 050 + carried REQ-040/041/045 A1 on screen, DA-26 (persistence), DA-23 (retired), WO-003 CR N4, G-09 fixture · governed: `IProgressStore` (first implementation; signatures unchanged, semantics F4), `IPuzzleLibrary` (consumed)
- **Inputs read:** see the header (design Draft 2026-10-03; working tree after the WO-003 close)
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-004-design-review.md`
- **Status:** recirculate → design-author (0 Blockers / 8 Shoulds / 9 Nits). Each Should in one line:
  - F1: the relaunch path need not sanitize;
  - F2: stored rationals can overflow the exact kernel (a launch crash loop);
  - F3: DA-48 narrows binding F4;
  - F4: DA-58 changes `IProgressStore` semantics;
  - F5: the Restart pill is under REQ-037's 48 dp;
  - F6: the REQ-026 A1 `browse` test would test the fake;
  - F7: the G-09 freeze is honour-system, and partly read documents are overwritten without a copy;
  - F8: `openGrid` is not guarded during a drag.
- **Traceability delta:** none (review only). F1, F6 and F8 add `browse` test rows; F6 moves the Solved-pieces policy from `play` to `browse`.
- **Notes for next station:**
  - Fix F1–F8 in §2–§5, the seam and acceptance tables, and DA-46…62. The text of DA-47, 48, 50, 55, 57, 58 and 62 changes.
  - F7(a) is an orchestrator step after D-2 (`guard.json`); write it into the design now.
  - Re-review can be a spot-check of §2–§5, the two tables and the DA rows.

## Spot-check (re-review)

**Date:** 2026-10-03 · **Read:** `designs/WO-004-design.md` rev 1, every section marked "(rev 1)", line by line:
- §Scope;
- §0;
- §2: class, write, v1 document, field table, reader table, semantics, fixtures, contract delta;
- §3, §4, §5;
- §7, §8;
- the Alternatives rows;
- the seam table, value shapes and acceptance table;
- the scaffolding list and the risks;
- DA-46…DA-66 and the suggested cut.

The orchestrator's rulings on F3 and F4, and governance row 11, were taken as given.

**Re-derived for the DA-63 question:**
- **Values real play can reach.** I ran an exact ℚ(√2) closure over all 13 `Tangrams/*.json` (the prototype embeds the same 13 files, `tools/build_prototype.py:29`, so it adds no values). Inputs:
  - anchors: every solution vertex plus every corner of anything placed;
  - every piece the puzzle uses, all 8 turns, both mirrors;
  - three generations of "lock against a corner of something placed";
  - kept only when every corner stays in the puzzle's bounding box;
  - all four rationals of `at` checked (`x.a`, `x.b`, `y.a`, `y.b`, the √2 coefficients included).

  Result: **max |n| = 12, denominators {1, 2}**. Only `vehicles-sailboat` has a half. Every kernel offset (`PieceShapes.kt` × `COS`/`SIN`) has integer coefficients. Each relock generation adds about 8 at most to a numerator, so a played pose cannot get near the bound.
- **Overflow under the bound as written.** I emulated the kernel's `Long` arithmetic (`multiplyExact`/`addExact` exactly as in Exact.kt and Arithmetic.kt) on `READING_ORDER` → `Q2.signum`, with in-bound values minus a typical offset:
  - with **1 ≤ d ≤ 64**, random pairs overflow. Hand pair: p.y = 65536/63 − (46341/63)√2 against q.y = −65535/61 + (46340/61)√2 overflows at about 2^63.4;
  - with **d dividing 64**, 200 000 random pairs never overflow; the worst-case bound is about 2^59.
- **Hair offsets.** (47321 − 33461√2)/64 ≈ 1.65·10⁻⁷ units is in bound (and dyadic), and it passes the 1e-6 area tolerance.

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (relaunch path) | **fixed** | One private `showAt(i)` used by `start()`, `open()` and `restart()` (§0, §5 table, seam row); `start()` rows (i)–(iv) added. Token placement of those rows: E6 |
| F2 (stored rationals vs exact kernel) | **partly** | Launch path closed: `sanitize` runs each piece in `runCatching` (fail-closed), and a 2^62 store test exists. But the DA-63 sentence "at the bound, squares and cross-products stay below 2^46 … cannot overflow on any value the reader accepts" is **false for 1 ≤ d ≤ 64**: coprime denominators (63, 61, …) overflow `Q2.signum` in `READING_ORDER`. That is a crash from touch during a lock search on a board restored from such a document. The arithmetic holds only for a common (dyadic) denominator. The planned D-1 test (d = 64, 1/64 steps) cannot see it (E1). "No tolerance-sized hair offsets exist" is also false (E2). Every real pose is accepted with a margin of more than 5000× |
| F3 (DA-48 vs F4) | **fixed** | `toProgress` reports every piece; `persist` keeps New and In progress pieces and drops only resting, unmirrored tray entries ("absent means resting", `SavedGame` KDoc); `restore(NEW)` applies tray turns. The example has a `new` entry with a turned SQ |
| F4 (IProgressStore semantics) | **fixed**, one residue | DA-58 withdrawn; reset leaves `lastShown` alone; the open question goes to WO-007. **Disk-failure note: no `IProgressStore` delta is needed** (see below). Residue: DA-64's "later saves overwrite" after both the move and the copy fail departs from G-09's "moved aside, never overwritten", and DA-64 does not say so (E3) |
| F5 (REQ-037) | **fixed** | Restart touch area ≥ 48 × 48 dp with the pill drawn at 44 dp; DA-53 extends ≥ 48 dp to every control; scaffolding bounds check; WO-007 ⚙ note |
| F6 (REQ-026 A1 test) | **fixed** | Policy in `browse` (`persist` canonical form, `sanitize` drops a SOLVED entry's pieces); the REQ-026 A1 row now drives real `browse` code with a `FakeHost` that captures all pieces; `restore(SOLVED)` stays as defence in depth |
| F7 (G-09 mechanics) | **fixed**, residue | Guard-lock step after D-2 is written into §2, the cut and the close checklist; damaged entry → New + one partial copy; `progress-v1-fresh.json` pins the explicit nulls; the example matches the library; guard (4) freezes the meaning of `at`; migration base noted. Residue: E4 (the chosen section rule against the KDoc), E5 (guard glob), O1 (non-canonical entry in the example) |
| F8 (`openGrid` guard) | **fixed** | §5 rule, seam row (`previous`, `next`, `nextUnsolved`, `open`, `restart`, `openGrid`), `// decision F5` test |
| N1 (fsync) | **fixed** | Real consequence stated (all progress after a power cut); `sync()` from `onStop`; never throws. Optional O2 |
| N2 (long press) | **fixed** | `combinedClickable` under a `LocalViewConfiguration` delegate, `max(500, platform)`, test clock, `performTouchInput` only |
| N3 (complete In-progress restore) | **fixed** | DA-66: a SOLVED restore, settled picture, nothing placed |
| N4 (best time, format) | **fixed** | WO-008 note on observability; `m:ss` from a string resource; hours deferred to WO-008 |
| N5 (strings) | **fixed** | "Done"/"Valmis" reused; TTS-friendly Finnish descriptions; `best-time` is the value only; three AI rows remain for the owner |
| N6 (seam details) | **fixed** | `index = n − 1` in the REQ-050 A1 row; values undefined before `start()`; seeding recipe; the grid covers the top bar |
| N7 (DA-62) | **fixed** | Setup-only diff, the orchestrator reads it, D-7 hands off, close-checklist line |
| N8 (wording) | **fixed** | DA-50 basis, DA-26, DA-57 reconciled, STATUS line at close, N4 size-mark half → WO-006 |
| N9 (write volume) | **fixed** | Skip-equal writes for all four `save*`; newest 3 aside files kept |

**The disk-failure "behavioural note": no `IProgressStore` delta is needed.**
- **What the KDoc says.** It promises "stored before it returns" and "never throws". A refused disk write (ENOSPC, EIO) cannot meet both; the design keeps "never throws" (G-10) and retries on the next save.
- **Why no KDoc change is needed.**
  - That is the only behaviour any implementation can have.
  - REQ-025 and F4 scope the promise to closing, killing and updating the app, not hardware failure.
  - Callers have nothing to act on: no REQ asks for a "storage full" message.
- **Where it is recorded.** A row in the WO's contract-delta table, with no signature or KDoc change, is the right record, and is itself the logged notify-tier entry.
- **Optional.** One KDoc sentence would make it visible to the WO-007/008 authors who read the interface rather than the WO; that would be a logged doc delta.
- **The DA-64 overwrite is a different matter.** It departs from a locked guardrail, not from the KDoc (E3).

**Line edits.** The orchestrator may apply these without re-review, as in the WO-003 spot-check; E4 is a choice between two named options.
- **E1 — DA-63, §2 `R` row, §6 kernel row, the seam/scaffolding lists (F2).**
  - The bound becomes **|n| ≤ 65536 and d ∈ {1, 2, 4, 8, 16, 32, 64}** (d divides 64; still "n" or a normalized "n/d"). Every real value (d ∈ {1, 2}) stays accepted.
  - Replace the "below 2^46" sentence with: "with dyadic denominators the worst intermediate of `Q2.signum` stays below about 2^59; arbitrary denominators up to 64 would overflow (63 against 61)".
  - The D-1 kernel test uses **mixed** dyadic denominators and opposite-sign `a`/`b` pairs, and calls `READING_ORDER`/`Q2.compareTo` directly (not only `find`, which reaches them only on ties). The store test adds a `d = 63` entry → New.
  - Add one content or golden assertion: every packaged solution coefficient has a denominator dividing 64 (true today). Then a correct save of a future puzzle can never fall outside the reader's bound.
- **E2 — DA-63 text.** Delete "Every position is also a lattice value of denominator ≤ 64, so no tolerance-sized hair offsets exist". Instead: a hand-edited in-bound value can sit about 10⁻⁷ off the play lattice (e.g. (47321 − 33461√2)/64) and pass `sanitize`. That is harmless (within TYPE-004's tolerance, no crash, unreachable in play).
- **E3 — DA-64.** State that `asideFailed` → overwrite deviates from G-09's "never overwritten". Give the reason: a rename and a copy inside the app's own directory both failed, and the KDoc promise wins. Log it as a guardrail conflict under governance row 13 (ai+inform) for the checkpoint-4 surface. That is the honest record of the orchestrator's F4 ruling.
- **E4 — §2 reader table, the "Damaged top-level section" Alternatives row, DA-46, DA-64 (F7 b).**
  - The KDoc says unreadable data "reads as a fresh install **for the part that cannot be read**". The chosen rule (a damaged section → the whole document moved aside, so every puzzle resets because, say, `soundOn` is malformed) reads more than that part as fresh. That contradicts §Scope's and §2's "no semantic departure from its KDoc".
  - My F7(b) offered this option without checking that phrase.
  - Preferred: a damaged section reads as fresh **for that section only**, with the same once-per-run `progress-partial-*` copy that entries already get. This meets the KDoc, G-09 (nothing is overwritten without a copy) and the original data-loss concern.
  - Otherwise keep the whole-document rule and list it next to the disk-failure note as a second behavioural note.
- **E5 — §2 orchestrator step and the seam row (F7 a).** The glob `progress-v1*.json` also locks the three **unfrozen** resources (`progress-v1-damaged-entry.json`, `progress-v1-damaged-section.json`, `progress-v1-huge-rational.json`). Lock exactly `progress-v1.json` and `progress-v1-fresh.json` (plus the frozen-fixture test file), or rename the unfrozen ones without the `progress-v1` prefix.
- **E6 — REQ-025 A1 row (token hygiene, new in rev 1).**
  - The `start()` rows (i), (iii) and (iv) pin decisions F4/REQ-045 behaviour: tag them `// decision F4`, or REQ-045.A1 for (i) if it asserts the 3-piece mini.
  - Row (ii), a displaced piece on relaunch, is REQ-025 A3's meaning, not A1's.

  Written under A1, these rows invite a wrong `REQ-025.A1` token (AGENTS "test tokens are coverage claims").

**Optional (no re-review, no blocker):**
- **O1.** In the example, `animals-cat`'s `LT1` tray entry (turn 4, unmirrored) is LT's resting pose, which `persist` drops. That is fine for a store fixture (the store keeps values as given), but say so in one line or turn it to 3, so a reader does not take it for writer output.
- **O2.** `sync()` could also force the directory (best effort, caught), so the rename itself is on disk, not only the file's data.

**Spot-check verdict: forward**, conditional on E1–E6 being applied before the Planner briefs the Acceptance Test Author.
- E1 is the one with a safety consequence: the reader's frozen bound must be dyadic, or a crash from touch remains possible from a hand-edited save.
- The rest are wording, token placement or glob precision.
- No new Blocker or Should-level design question remains; F1, F3, F5, F6 and F8 and all nine Nits are resolved.
