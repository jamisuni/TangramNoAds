# Design — WO-004 · #Browsing + `store`: many puzzles, saved progress

**Author:** Design Author  ·  **Date:** 2026-10-03  ·  **Status:** Revised (rev 1) after `reviews/WO-004-design-review.md` (F1–F8, N1–N9); changed sections are marked "(rev 1)"  ·  **Baseline:** architecture.md v1.0, build-map v1.2, req_types v0.2, decisions.md (to DA-45), designs/WO-003-design.md and its reviews

## Scope

- **Implements:** REQ-003 A1–A2, REQ-024 A1–A3, REQ-025 A1–A3, REQ-026 A1–A2, REQ-050 A1–A3 (code home `browse`, plus `store` behind `IProgressStore`).
- **Carried in, closed here:**
  - From WO-002, the on-screen parts of REQ-040 A1, REQ-041 A1 and REQ-045 A1: a fresh install opens on the rating-1 mini puzzle, the warm-ups follow the minis.
  - From WO-003:
    - saving and restoring the `PlaySession` state (DA-26: "no persistence until WO-004");
    - DA-23, the debug `puzzle` extra (retired, section 7);
    - code review N4 (rev 1): the empty-library half is closed by DA-10 plus one `check` (an empty library is a packaging error, not a crash path from touch or stored data, so the WO line "must not crash (G-10)" is read that way); the unbounded-title half by the ellipsis; its size-mark font-scale half belongs to WO-006 (`tasks.md`), not here;
    - `AppViewModel`'s session wiring (section 7).
  - From P2: **G-09**, the frozen v1 save fixture (section 2).
- **Governed interfaces:** `IProgressStore` (notify): first implementation, **no signature change, no `contract-delta`, and (rev 1) no semantic departure from its KDoc** (section 2, end). One behavioural note goes in the WO's contract-delta table: a disk-level write failure cannot be "stored before it returns" (DA-47). `IPuzzleLibrary` (locked): consumed only. `contracts/` is untouched. No `kernel.model` file is edited (the kernel additions are in `kernel.lock` and `kernel.state`, section 6).
- **Hard-stop domain touched:** the v1 save format is the first persisted shape of REQ-025 data. Freezing it is the "data migration" domain (AGENTS.md hard-stops; governance row 11 is `ai+inform`, so it is the AI's to decide and goes on the checkpoint surface). The freeze is deliberate and reviewed here, because any later change is a migration (G-09).
- **Not designed here (so nobody builds it silently):** the ⚙ slot and settings (WO-007), the timer slot and time counting (WO-008; `puzzleSeconds` and `bestSeconds` are carried through untouched), the DevTools overlay (WO-005), tablet and language acceptance (WO-006), game-event callbacks for sound (WO-007).

## Chosen design

### 0. Shape in one picture

```
app (O-05, O-09)   MainActivity: onPause = interrupt a drag, then persist · BackHandler closes the grid
 │                 AppViewModel: PuzzleLibrary.packaged() · JsonProgressStore(filesDir) · SessionHost · BrowseController
 │                 SessionHost : PuzzleHost over play: builds a PlaySession per visit, restores it, captures it
 │                 TangramApp  : top bar · PlayArea(+ solved-bar slot) · Restart pill · grid overlay
 ▼ wires
browse (#Browsing) BrowseController (REQ-003/024/025/026/050) ── PuzzleHost (seam, implemented by app)
 │                 ProgressRestore.sanitize (REQ-025 A3)       ── reads IProgressStore, IPuzzleLibrary (contracts only)
 │                 (rev 1) the save policy lives here: persist() canonicalizes and strips Solved pieces
 │                 BrowseTopBar · AllPuzzlesOverlay · SolvedBar · RestartButton (Compose, slots for thumbnails)
play (#Solving)    PlaySession + restore() / toProgress() / onChanged / observable state   (new, section 4)
 │                 PuzzleThumbnail (lent to the grid, O-09) · PlayArea solvedBar slot · tray hidden when SOLVED
store (support)    JsonProgressStore : IProgressStore   one versioned JSON document, atomic replace (ADR-005)
kernel             + LockSearch.isValidPlacement · + PuzzleStates.onRestart / onRetry
```

One idea carries the WO: **a puzzle visit is one `PlaySession`, and the session is always built from the store.** (rev 1) `start()`, `open()` and `restart()` all end in one private `showAt(i)` = `index = i; host.show(p, ProgressRestore.sanitize(p, store.progress(p.id)))`, so the code path that restores after a relaunch is, by construction and by a `browse` test of `start()`, the one every navigation exercises.

### 1. Modules and dependencies (G-06, V-06, ADR-002)

| Module | Kind | Why | Main-scope dependencies |
|---|---|---|---|
| `store` | **Kotlin/JVM** (support module, no `#Tag`) | architecture §3 and ADR-005 already say JVM: temp-dir tests with no emulator, and O-03 is a compile-time fact. It uses only `java.io` and `java.nio.file` (both available at minSdk 26) and takes a `File` directory, so it needs no Android type. | `kernel`, `contracts`, `kotlinx-serialization-json` (the ADR-004 tree parser, DA-13 style: no compiler plugin) |
| `browse` | **Android library** (#Browsing code home) | Compose UI and string resources (G-05) | `kernel`, `contracts`, Compose `foundation` (BOM) |
| `app` | Android app | composition root | adds `project(":store")`, `project(":browse")` |

- `browse` never sees `play` or `store`. Its two inputs are `IPuzzleLibrary` and `IProgressStore` (both `contracts`), and the AI-owned seam `PuzzleHost` (section 5). Cross-slice drawing (grid thumbnails) arrives as a slot lambda (O-09).
- Test scope may use `content` (G-06): `browse` tests use the real puzzles. Test fakes of `IProgressStore` and `PuzzleHost` live in `browse/src/test` (`FakeProgressStore`, `FakeHost`, written by the test author from the signatures below).
- No new library. `BackHandler` comes from the existing `activity-compose`; `ViewModel` is already used. `settings.gradle.kts` gains `include(":store")` and `include(":browse")`. V-06's table already lists both modules, V-05 picks up `browse/src/main/res`.
- `app/build.gradle.kts` loses the `androidComponents { beforeVariants … }` stanza that existed only to prove the DA-23 release stub (section 7).

### 2. `store`: the first `IProgressStore` (ADR-005, G-09, G-10, G-01)

**Class.** `class JsonProgressStore(dir: File, nowMs: () -> Long = System::currentTimeMillis) : IProgressStore`. Public constants `FILE_NAME = "progress.json"` and `VERSION = 1`. Concrete-class members (rev 1): `fun sync()` (public, N1), `internal val movedAside: File?`, `internal val partialCopy: File?`, `internal val asideFailed: Boolean`, `internal val failedWrites: Int`. The constructor reads the file once, so the object is the in-memory state; every `save*` and `resetAllProgress()` updates memory and then writes. `app` passes `filesDir` (app-private, covered by `allowBackup="false"` and the all-domain extraction rules, G-01; the manifest is not edited, V-01 stays a regression check).

**Medium: stdlib file, not preferences.** ADR-005 decided it (no DataStore/Room/SharedPreferences, D2/D5). No network, no permission, no backup (rule 1, G-01).

**Write (DA-47, rev 1: N1, N9).** `saveProgress`, `saveLastShownPuzzle`, `savePlayTime` and `saveSettings` **skip the write when the value equals the stored one** (no flash wear for an unchanged New puzzle on every ›). Otherwise: serialize the whole model to one compact UTF-8 JSON document, write `progress.json.tmp`, then `Files.move(tmp, target, ATOMIC_MOVE)` (falling back to `REPLACE_EXISTING` when the file system refuses `ATOMIC_MOVE`). **No `fsync` per save**: a process kill, which is what REQ-025 and F4 are about, cannot lose a completed rename, and a per-lock `fsync` on slow flash would hitch exactly at the lock and solve animations. The honest consequence: after a **power cut** right after a save, the file can be missing the newest data or be unreadable, and an unreadable file means **all** puzzles start fresh (the file is moved aside, not deleted). A kill or a closed app never loses a completed save. The cheap middle way (N1): `JsonProgressStore.sync()` (open the file and `force(true)`, and also force the directory so the rename reaches the disk; rev 2, O2) is called by `MainActivity.onStop`, when no animation runs, so only a power cut between a save and the next `onStop` is exposed. No interface change.
**A disk-level write failure never throws** (G-10): memory stays current, `failedWrites` counts it, and the next save retries the whole document. That case cannot meet the KDoc's "stored before it returns" (the disk refused); it is the one behavioural note in the WO's contract-delta table, with no signature or KDoc change.

**Threading.** Main thread only, synchronous. `save*` returns after the rename, so ADR-005's "stored before it returns" holds. The document is a few KB (20–40 puzzles), so a write is sub-millisecond to a few milliseconds. Nothing is shared with another thread; there is no lock. This is documented in the class KDoc. If a profile ever shows jank, the fallback is the ordered background writer ADR-005 names, behind the same interface.

**The v1 document (frozen by this design; DA-46; rev 1: F7 d).** Names are explicit serial strings written in `store`, never `enum.name`, so renaming a Kotlin enum is not a format change. The example matches the library (`shapes-mini-2` has MT/ST1/ST2), holds a negative rational, `turn` 0 and 7, and `mirrored: true` on PG.

```json
{
  "version": 1,
  "lastShown": "shapes-mini-2",
  "puzzles": {
    "shapes-mini-1": {"state": "solved", "pieces": {}, "puzzleSeconds": 47, "bestSeconds": 41},
    "shapes-mini-2": {
      "state": "in_progress",
      "pieces": {
        "MT":  {"where": "board", "turn": 0, "mirrored": false,
                "at": {"x": {"a": "2", "b": "0"}, "y": {"a": "-1/2", "b": "1/2"}}},
        "ST1": {"where": "tray", "turn": 7, "mirrored": false},
        "ST2": {"where": "tray", "turn": 4, "mirrored": true}
      },
      "puzzleSeconds": 12,
      "bestSeconds": null
    },
    "animals-cat": {
      "state": "in_progress",
      "pieces": {
        "PG":  {"where": "board", "turn": 7, "mirrored": true,
                "at": {"x": {"a": "3", "b": "-1"}, "y": {"a": "1", "b": "0"}}},
        "LT1": {"where": "tray", "turn": 2, "mirrored": false}
      },
      "puzzleSeconds": 95,
      "bestSeconds": 95
    },
    "things-arrow": {"state": "new", "pieces": {"SQ": {"where": "tray", "turn": 3, "mirrored": false}}, "puzzleSeconds": 0, "bestSeconds": null}
  },
  "playTime": {"day": "2026-10-03", "todaySeconds": 340, "totalSeconds": 5120},
  "settings": {"timerShown": true, "soundOn": false}
}
```

| Field | Encoding |
|---|---|
| `version` | integer, `1` |
| `puzzles` | object keyed by the puzzle id (the file name stem, never reused); entries of ids the library no longer has are kept and ignored by the game (F4) |
| `state` | `"new"`, `"in_progress"`, `"solved"` (TYPE-006) |
| `pieces` | object keyed `"LT1","LT2","MT","SQ","PG","ST1","ST2"` (TYPE-001 ids, equal to the puzzle-file ids). Value: `{"where":"tray","turn":0..7,"mirrored":bool}` or `{"where":"board", …same…, "at":{"x":Q2,"y":Q2}}` |
| `Q2` | `{"a":R,"b":R}` meaning `a + b·√2`, exact (ADR-003, G-03) |
| `R` (rational) | a string `"n"` or `"n/d"`, `d ≥ 2`, matched by `-?\d+(/\d+)?`, already normalized (a non-normalized string such as `"2/4"` is entry damage). **Magnitude bound, frozen (rev 2, E1, DA-63): \|n\| ≤ 65536 and d ∈ {1, 2, 4, 8, 16, 32, 64} (dyadic).** Real content uses only d = 1 and 2, numerators up to 12. Dyadic denominators keep every cross-product of the kernel's `multiplyExact` chains (lock search, `Q2.signum`, `READING_ORDER`) within `Long` range; arbitrary in-bound denominators do not (coprime 63 and 61 overflow `Q2.signum` in a `READING_ORDER` comparison, emulated by the reviewer). The D-1 test uses mixed dyadic denominators and opposite-sign coefficient pairs and calls `READING_ORDER` / `Q2.compareTo` directly; a store test reads a d = 63 entry as New; a content/golden check asserts every packaged coordinate's denominator divides 64. (Rev 2, E2) A hair offset such as (47321 − 33461√2)/64 is in bound and harmless: the lock tolerance and fail-closed checks handle it, it is not excluded by the reader |
| `puzzleSeconds`, `bestSeconds` | integers ≥ 0; `bestSeconds` may be `null`; always written |
| `playTime` | `day` is `"yyyy-MM-dd"` or `null`, `todaySeconds`, `totalSeconds` integers ≥ 0 |
| `settings` | booleans `timerShown`, `soundOn` |
| `lastShown` | puzzle id string or `null` |

- **v1 holds everything `IProgressStore` carries, now,** including play time, settings and last-shown, although WO-007/008 are the first to write them. Adding one later is a v2 migration. I checked REQ-005, 029, 030, 031, 033, 034 against this list: per-puzzle running and best seconds, today and all-time with the local day, the two settings, reset. Nothing else is asked for.
- **One profile.** REQ-025's "per player profile; exactly one" is met by the single document. Several profiles would be a v2 (a new `profiles` level and a migration that wraps the v1 content); the v1 layout does not prevent it (REQ-025 asks no more).
- **Writer rules.** Every section is always written, keys in the order above, **nulls written explicitly** (`lastShown`, `day`, `bestSeconds`), so the same model gives the same JSON tree. Unknown keys are ignored on read and not preserved (v1 has no extension slot by design).
- **Reader rules (G-09, G-10, DA-46).**

| Case | Result |
|---|---|
| no file | fresh install: every puzzle `PuzzleProgress.NEW`, `PlayTime.NONE`, default settings, `lastShown` null |
| file unreadable (I/O error), empty, not JSON, not an object, no integer `version`, `version < 1` | **moved aside** to `progress-bad-<nowMs>.json`, game starts fresh |
| `version > 1` (an older debug APK over a newer one) | moved aside to `progress-v<version>-<nowMs>.json`, game starts fresh |
| (rev 2, E4) **a top-level section** (`puzzles`, `playTime`, `settings`, `lastShown`) missing or of the wrong type, or `day`/seconds/booleans malformed | **that section alone reads as its fresh-install value** (the KDoc: "reads as a fresh install for the part that cannot be read"); the other sections load. The original file is copied once to `progress-partial-<nowMs>.json` before the first write of the run (as for a damaged entry), so nothing is lost without a copy |
| one `puzzles` entry unreadable (bad state string, unknown piece id, bad `where`/`turn`/`at`/rational, rational out of the bound, negative seconds, denominator 0, `Long` overflow) | that puzzle reads as `NEW`; the others load. (rev 1) The original file is **copied once** to `progress-partial-<nowMs>.json` before the first write of the run, so a damaged or wrongly rejected entry is never lost without a copy (`partialCopy`) |
| (rev 1, F4) moving aside fails | try to **copy** the document to the aside name; if that also fails, set `asideFailed`, log it, and carry on: later saves **overwrite**. **(rev 2, E3) This is a guardrail conflict with G-09 ("never overwritten") under governance row 13 (`ai+inform`):** in the both-failed case the `IProgressStore` promise "stored before it returns" wins, because a store that silently stops saving loses the player's new work while the old document is unreadable or newer-version data that could not be kept anyway. The orchestrator logs it as a decision and surfaces it at checkpoint 4 (DA-64) |
| `progress(id)` for an id never saved | `PuzzleProgress.NEW` |

  Reading wraps each entry in `runCatching`; the document is never partially applied to a puzzle. (rev 1, N9) After any move-aside or partial copy the store keeps only the **newest 3** files matching `progress-bad-*`, `progress-v*`, `progress-partial-*`.
- **Semantics.** `saveProgress` stores the value as given and a later `progress(id)` returns an equal value. `resetAllProgress()` does exactly what the KDoc lists: every puzzle New (the map emptied, best times erased), play time `NONE`, settings kept. **(rev 1, F4) It does not touch `lastShown`**: which puzzle shows after a reset is WO-007's question (REQ-032 "returns to the same puzzle state" and O-08 versus the prototype's jump to puzzle 1); if WO-007 chooses puzzle 1 it calls `open(0)`, which writes `lastShown` itself. **Open question handed to WO-007's design author, possibly an owner question.**
- **Frozen fixtures (G-09, rev 1: F7).** In `store/src/test/resources/fixtures/`:
  - `progress-v1.json`: a verbatim copy of the document above (delivered by D-2; I fix the shape here, so D-2 may change numbers, never keys);
  - `progress-v1-fresh.json`: exactly what the writer produces for a fresh model, pinning the explicit nulls (`lastShown`, `day`) and the default settings;
  - separate, unfrozen test resources: a damaged-entry document, a damaged-section document, `version: 2`, and the 2^62 document of F2.

  Guards: (1) the reader loads each frozen file to its expected `IProgressStore` values; (2) writing that model reproduces the same JSON tree (a two-way freeze); (3) a pinned SHA-256 of each frozen file's bytes (CRLF normalized to LF), so editing a fixture to make a test pass fails; (4) the world corners of the fixture's board pieces through the kernel shape table are asserted, which freezes the *meaning* of `at` (the `SavedGame.kt` convention), not only its encoding.
  **Orchestrator step (F7 a; plan v1.2: authorship F3 / DA-67, timing F4):** the frozen fixtures, the hash pins and the expected-values test file are authored by **D-8 (TASK-T4a)** from this section's document, not by D-2, which must pass them unedited. **After CR-2 is forward** (so a format fix asked by CR-2 is not a hard-stop) the orchestrator adds exactly `store/src/test/resources/fixtures/progress-v1.json`, `store/src/test/resources/fixtures/progress-v1-fresh.json` and the frozen-fixture test file `store/src/test/kotlin/io/github/jamisuni/tangram/store/FrozenV1FixtureTest.kt` (no glob; the unfrozen resources are named `damaged-entry.json`, `damaged-section.json`, `version-2.json` and `huge-rational.json`, none starting with `progress-v1`) (rev 2, E5) to `.swdev/guard.json` `contract_paths.locked`, re-freezes the baseline and logs it in `decisions.md`. A later edit then stops for the human, as hard-stop 2 intends.
  **Migration base (governance row 11: "a migration test is mandatory").** These frozen v1 files are the input a future v2 migration test loads; the v2 change must ship that test against them.

**Contract delta: none.** `IProgressStore` has exactly the operations this implementation needs: per-puzzle read and save (the grid and the session), `lastShownPuzzle` / `saveLastShownPuzzle` (relaunch, REQ-045 A1 when null), `resetAllProgress` (WO-007), play time and settings (WO-007/008). The "moved aside" outcome, `sync()` and `failedWrites` are visible on the concrete class only, which `app` constructs, so the notify-tier interface does not move, and (rev 1) every documented promise of the KDoc holds except the disk-failure note above. If implementation finds a missing operation (for example a flush), that is a logged `contract-delta` and a Design Reviewer touch, not a silent edit. None is expected.

### 3. What a save holds, and when (rev 1: F3, F6, N9; REQ-025, TYPE-006, F2, F4, DA-5/F5, O-08)

**What.** Per puzzle, `PuzzleProgress(state, pieces, puzzleSeconds, bestSeconds)`. One rule, in one place (`BrowseController.persist`, DA-48):

- `PlaySession.toProgress` **reports the session as it is**: every piece of the puzzle, `OnBoard` or `InTray` with its turn and mirror, in any state.
- `persist()` then stores it with two policy steps, both `browse`'s because #Browsing owns the state rules: **(a)** a `SOLVED` capture is stored with `pieces = emptyMap()` (REQ-026 A1: the game never shows where pieces were, and Retry returns to New anyway); **(b)** `InTray` entries at the TYPE-001 resting turn and unmirrored are dropped (absent means resting, per the `SavedGame` KDoc), so an untouched New puzzle is stored as `NEW` and skipped as "equal".
- **New and In progress keep their pieces** (decisions F4 and ADR-005: "after every lock, return, turn, mirror"; "a kill loses at most the drag in progress"): a New puzzle whose tray pieces were turned keeps those turns when you leave, relaunch or are killed. TYPE-006 says a tray turn does not *start* the puzzle; it does not say the turn is forgotten. The state stays New, so the grid and `nextUnsolved` are unaffected. No owner sign-off is needed.
- `sanitize` (section 5) mirrors it on read: a stored `SOLVED` entry has its pieces dropped; a stored `NEW` entry's `OnBoard` pieces (impossible from the writer) become `InTray`.

`puzzleSeconds` and `bestSeconds` are never computed in this WO: every write is the O-08 read-modify-write `store.progress(id).copy(state = …, pieces = …)`, so WO-008's times pass through untouched.

**When (DA-49).** The binding rule is decisions F4 ("persisted after every lock, return, turn, mirror, Restart, Retry"; "a kill loses at most the drag in progress").

| Moment | Who | What is written |
|---|---|---|
| a drop resolves (lock or return), a tray piece turns or mirrors, a board piece turns or mirrors successfully | `PlaySession` calls its `onChanged` after the intent finished (drag already cleared) | the shown puzzle's progress |
| a refused board turn (shake) | nothing changes, no call | — |
| moving away (‹ › grid Next) | `BrowseController.open` calls `persist()` first (REQ-024 rule) | the shown puzzle's progress, then `saveLastShownPuzzle(new)`; both are skipped by the store when equal to what is stored (N9) |
| `onPause` (app hidden, screen off, rotation) | `MainActivity`: `interruptDrag()` first (F5/DA-5), then `persist()` | the shown puzzle's progress |
| `onStop` | `MainActivity`: `store.sync()` (N1) | nothing new; flushes the file to disk |
| Restart, Retry | `BrowseController.restart()` | `store.progress(id).restarted()` (keeps the best time, REQ-030) |
| NEW to In progress at a drag start | no write of its own | the next settled event, leave or pause carries it. A kill mid-drag loses the drag and keeps the puzzle New, as F4 allows |

`persist()` is `host.capture(store.progress(id))`, then the policy steps above, then `store.saveProgress`. `capture` is `PlaySession.toProgress(base)`, which **calls `interruptDrag()` first**, so a save can never record a piece as "being dragged" (there is no `Dragged` in the format) and never reaches the lock search or a sound (DA-5). The navigation entry points also refuse to run while `host.isDragging` (decisions F5: other controls ignore touches while a piece is dragged), so a save during a drag can only come from `onPause`, where the interrupt is already the rule.

### 4. Restoring a session, and how it meets the WO-003 `PlaySession` API (rev 1: F3, F6, N3, N8; REQ-025, DA-5, DA-35)

**`play` additions (all additive; the frozen WO-003 signatures keep compiling):**

```kotlin
class PlaySession(
    val puzzle: Puzzle,
    reducedMotion: () -> Boolean = { false },
    resolver: DropResolver = DropResolver(puzzle),
    onChanged: () -> Unit = {},                       // NEW, REQ-025/F4: the first consumer of a session event (cf. DA-27)
)
val state: PuzzleState        // unchanged type; now observable (the getter reads the version counter)
val isDragging: Boolean       // unchanged type; now observable
fun toProgress(base: PuzzleProgress): PuzzleProgress  // NEW
fun restore(saved: PuzzleProgress)                    // NEW
```

- **`toProgress(base)`:** `interruptDrag()`, then `base.copy(state = state, pieces = <every piece of the puzzle, OnBoard or InTray with turn and mirror, in any state>)`. It applies no storage policy (that is `browse`'s, section 3). It never calls `onChanged`.
- **`restore(saved)`:** for a **fresh** session (`check(drag == null)`; not called twice). The input is already sanitized (section 5).
  - `NEW`: apply the saved tray entries (turn and mirror); everything else rests (F3).
  - `IN_PROGRESS`: apply each saved piece the puzzle has (board piece to `Where.Board(at)` with its turn and mirror, tray piece to its saved turn and mirror), the rest rest in the tray. **(rev 1, N3, DA-66)** If that leaves every piece on the board (`SolvedCheck.isSolved`), the restore is a **SOLVED restore**: nothing is placed, the picture is settled, no celebration, no best time. So the solved bar never sits over a board that still draws pieces.
  - `SOLVED`: `state = SOLVED`, every piece rests in the tray (nothing is placed, so `placed` is empty and no position can leak; defence in depth beside `browse`'s stripping), and `solved = SolvedAt(t0 = -10_000, reducedMotion())`: `t0` far enough in the past that `SolvedTimeline` is settled (picture alpha 1, pieces hidden, no pop, no confetti) from the first frame, and `animating()` is false so an idle solved board schedules no frames.
  - Pieces in `saved` that the puzzle lacks are ignored. `restore` bumps the version counter and calls no `onChanged`.
- **Drawing when SOLVED (REQ-026, DA-52):** the tray cells, size marks, flip badge and dashed outlines are not drawn; the board shows the picture; the **tray region is given to the solved bar** (section 5). This happens from the moment of the solve, so one rule covers a live solve and a restored one. REQ-023's pop, confetti and 600 ms fade are unchanged.
- **Session per visit (DA-51).** Switching puzzles creates a **new** `PlaySession` (`SessionHost.show`). The drawing keys everything on the session already (`remember(session)`, the frame loop, the gesture machine, `clearPictureCaches` on dispose), so nothing stale carries over and no `reset()` API is needed.
- **`AppViewModel` and rotation (DA-26 amended).** The ViewModel still survives window changes, now holding the controller, host and store, so rotation keeps the session instance, the puzzle and the open grid without reading the store back (rev 1: `onPause` still persists on every rotation, which is harmless and skipped when nothing changed). DA-26's "no persistence" clause ends here; process death and relaunch are the store's job.

### 5. Browsing: `browse` (rev 1: F1, F2, F5, F6, F8, N2, N4–N6; REQ-003, 024, 025, 026, 050)

**The seam `PuzzleHost` (AI-owned, in `browse`; implemented by `app` over `play`).** It exists because `browse` may not depend on `play` (G-06) and the REQ-025 behaviour must be testable in `browse` (trace rule 3).

```kotlin
interface PuzzleHost {
    val state: PuzzleState                       // the shown session's state; observable (snapshot reads)
    val isDragging: Boolean                      // observable
    fun show(puzzle: Puzzle, progress: PuzzleProgress)   // build a fresh session restored from `progress`; make it the shown one
    fun capture(base: PuzzleProgress): PuzzleProgress?   // shown session as progress on top of base (interrupts a drag first); null before the first show
}
```

**`BrowseController`** (plain Kotlin on the main thread; `index` and `gridOpen` are snapshot state so Compose observes them; the whole class is JVM-testable with fakes):

| Member | Behaviour |
|---|---|
| `puzzles`, `index`, `current`, `shownState`, `gridOpen`, `shownBestSeconds` | `puzzles = library.puzzles` (`check(isNotEmpty())`, DA-57); **`index` is 0-based; the counter and REQ-050's "puzzles 3 and 7" are 1-based (`index = n − 1`)**; `shownState = host.state`; `shownBestSeconds = store.progress(current.id).bestSeconds` (not snapshot state: WO-008 must make it observable, for example by re-reading after the time slice writes the best time, or the bar keeps showing `–`; note for WO-008, not built now). **Before `start()` every value is undefined; tests and `app` call `start()` first** |
| `showAt(i)` (private) | `index = i; host.show(p, ProgressRestore.sanitize(p, store.progress(p.id)))`: the one route to `show`, used by `start()`, `open()` and `restart()` (F1) |
| `start()` | `showAt(` the index of `store.lastShownPuzzle()` if the library has it, else 0 `)` (**fresh install: the rating-1 mini**, REQ-040 A1 / REQ-045 A1; F4: an id the library lost is ignored); **writes nothing**. A relaunch therefore sanitizes (REQ-025 rule 4) |
| `previous()`, `next()` | `open((index ∓ 1) wrapped)`; ‹ on the first shows the last, › on the last shows the first (REQ-024 A3); work in every state (A1) |
| `nextUnsolved()` | first puzzle after `index` (wrapping, the current one excluded) whose state is not `SOLVED` (New and In progress both count, and a Retried puzzle is New: F10), else a normal `next()`. REQ-050 A1 and the "every puzzle solved" rule. A lone unsolved current puzzle also falls back to `next()` (DA-54) |
| `open(i)` | no-op if `host.isDragging`, `i` out of range or `i == index`. Otherwise `persist()`, `store.saveLastShownPuzzle(puzzles[i].id)`, `showAt(i)`. **It writes only the leaving puzzle and last-shown** (REQ-050 A3, REQ-003 A2). The sanitized progress is not written back |
| `openGrid()`, `closeGrid()` | the overlay flag (survives rotation with the ViewModel, decisions F3). **`openGrid()` is a no-op while `host.isDragging`** (F8). `open(i)` from the grid also closes it (app calls both) |
| `restart()` | no-op if dragging. `val after = if (state == SOLVED) PuzzleStates.onRetry(state) else PuzzleStates.onRestart(state)`; if unchanged (already New) nothing; else `store.saveProgress(id, store.progress(id).restarted())` and `showAt(index)`. One operation serves **Restart (In progress) and Retry (Solved)** (REQ-025 A2: pieces to the tray, puzzle time 0; REQ-026 A2: empty silhouette; best time kept) |
| `persist()` | `host.capture(store.progress(id))?.let { store.saveProgress(id, canonical(it)) }`, where `canonical` strips the pieces of a SOLVED capture and drops resting, unmirrored `InTray` entries (section 3; F6 puts this policy in `browse`, so the REQ-026 A1 test runs real `browse` code against a `FakeHost` that reports a SOLVED session with all its pieces) |
| `stateOf(i)` | `host.state` for the shown puzzle, `store.progress(id).state` for the rest (the grid) |

**`ProgressRestore.sanitize(puzzle, saved): PuzzleProgress`** (`internal` in `browse`, REQ-025 A3, DA-50). For `IN_PROGRESS`, visit the saved board pieces in TYPE-001 tray order. A piece is kept if `LockSearch.isValidPlacement(silhouette, acceptedSoFar, piece, turn, mirrored, at)` holds (inside the silhouette and overlapping no accepted piece, within TYPE-004's 1e-6 tolerance), else it becomes `InTray(turn, mirrored)` (its saved turn and mirror stay, F2). Pieces the puzzle lacks are dropped. **(rev 1)** A stored `SOLVED` entry has its pieces dropped (F6); a stored `NEW` entry's `OnBoard` pieces become `InTray` (F3). **Fail-closed (F2):** the validity check of each piece runs inside `runCatching`; any exception from the geometry (overflow, anything) sends that piece to the tray, DA-25 style, so stored data can never crash a launch even if a value slipped past the reader's magnitude bound. It lives in `browse`, not in `play`, so that REQ-025 A3 has a covering test in its code home using only `kernel`, `contracts` and the real puzzles. I read A3's "overlaps the current silhouette's edge" and rule 4's "no longer a valid lock" as the TYPE-004 validity test (inside, no overlap), not as "touches an anchor": an update that moves the silhouette fails the inside test. (There is a stronger reason: a correctly saved piece can touch **no** current anchor, because it locked against a piece that was later put back in the tray; an anchor-based reading would reject correct saves.)

**UI (all text from string resources, section 8).**

| Composable | What |
|---|---|
| `BrowseTopBar(controller, layoutClass)` | replaces the WO-003 title placeholder in the same 66 dp (phone) / 74 dp (tablet) slot. Row: ‹ (52 dp square), a centre block, the counter button, › (52 dp square). Centre block: line 1 the title (`inLanguage`, **one line with ellipsis**, closes N4's title half), line 2 the rating dots (5 dots, `rating` filled) and the state text. The counter is its own button, at least 48 dp square, showing `n / total`; pressing it calls `openGrid()` (REQ-050). The state text shows all three states, New included (DA-53). Tags: `top-bar`, `prev-button`, `next-button`, `puzzle-title`, `rating-dots`, `puzzle-counter`, `puzzle-state` |
| › long press (rev 1, N2) | `Modifier.combinedClickable(onClick = next, onLongClick = nextUnsolved)` inside `CompositionLocalProvider(LocalViewConfiguration provides <a delegate of the current one with longPressTimeoutMillis = max(500, platform value)>)` around › only (verified in ui-android 1.12.1: the node's `viewConfiguration` comes from the local). That gives REQ-050's "at least 500 ms" (and a longer accessibility hold delay still works), indication, touch slop and the semantics `onClick`/`onLongClick` for TalkBack for free (D2); no custom gesture code. A hold fires `nextUnsolved()` once and the release then does nothing. The timeout runs on the pointer-input coroutine clock, so the device test advances the **test clock** by at least 500 ms. If `combinedClickable` needs an opt-in in this Compose version, the implementer adds it. Tests press › with `performTouchInput` (never `performClick`, WO-003 F2); `LONG_PRESS_MS = 500` is one constant |
| `AllPuzzlesOverlay(controller, thumbnail, modifier)` | full-size overlay over the **whole screen, including the top bar** (as the prototype's does; while it is open the top bar nodes are covered and tests assert them only after closing), paper-coloured, swallows touches. Header with the "All puzzles" title and a Close button (touch area ≥ 48 dp, text "Done"/"Valmis" from the prototype, rev 1 N5), then a `LazyVerticalGrid(GridCells.Adaptive(84.dp))` with one cell per puzzle in library order (REQ-050 A2, same order as ‹ ›). Every cell is at least 84 dp square (REQ-050 rule 4 wants ≥ 64 dp, REQ-037), shows its number top-left, has an accent border when it is the shown puzzle, and calls `thumbnail(puzzle, solved, Modifier)`: `solved = false` is the flat silhouette, `true` the picture (REQ-050 ASSUMPTION: the picture at thumbnail size, never the piece layout). In-progress cells add a dot node on top. A press runs `controller.open(i)` and `closeGrid()`. Initial scroll puts the shown puzzle's cell on screen. Tags: `all-puzzles`, `grid-close`, `grid-cell-<puzzleId>`, `grid-dot-<puzzleId>`. Each cell's `contentDescription` is `"<n>. <title> · <state>"` |
| `RestartButton(onClick, modifier)` | **(rev 1, F5)** the pill of Spec/02 may be drawn 44 dp high, but **its touch area is at least 48 × 48 dp**: REQ-037 (locked) covers every player control and Contract wins over Spec/02. A scaffolding bounds check (`// guardrail`, no REQ token; REQ-037 A1 stays WO-006's) pins it. Tag `restart-button`; `app` shows it at the board's top-left only while the shown state is In progress (REQ-025 rule 2) |
| `SolvedBar(bestSeconds, onRetry, onNext, modifier)` | Retry, best time and Next (filled green, the primary action), each with a touch area ≥ 48 dp high. The `best-time` node's text is **the value only** (`–` when none; otherwise `m:ss` from a string-resource pattern, G-05); the "best time" label is a separate untagged text. Hours and the Finnish unit strings (REQ-029's "h and min", F14 "t"/"min") are **not built now**: WO-008 owns time formatting and lifts one format (N4). Tags `solved-bar`, `retry-button`, `best-time`, `solved-next-button`. Next is the same as ›. |

**Every controller entry a control can reach is guarded** (rev 1, F8): `open` (so ‹, ›, the long press, grid cells, Next), `restart` and `openGrid` are no-ops while `host.isDragging` (decisions F5), in one place, unit-testable. WO-007's ⚙ must go through a guarded entry too. Note for WO-007: Spec/02's 44 dp ⚙ also needs a 48 dp touch area (REQ-037).

### 6. `play` and `kernel` changes this WO needs

| Addition | Where | Required by |
|---|---|---|
| `LockSearch.isValidPlacement(silhouette, others, piece, turn, mirrored, at): Boolean`: true iff `fitAt` gives `insideDeficit ≤ TOLERANCE` and `maxOverlap ≤ TOLERANCE`; entries of `others` with the piece's own id are ignored; non-finite is false (fail-closed, DA-25). (rev 1, F2) A kernel scaffolding test runs `find` and `isValidPlacement` at the reader's bound extremes (|n| = 65536, d = 64, and 1/64 steps) and asserts no exception | `kernel.lock` | REQ-025 A3 (O-01: one validity rule, the lock search's); G-10 |
| `PuzzleStates.onRestart(state)` (In progress to New, else unchanged), `onRetry(state)` (Solved to New, else unchanged) | `kernel.state` | TYPE-006, O-07 ("restart and retry arrive with WO-004, their first caller"); REQ-025 A2, REQ-026 A2 |
| `PlaySession.toProgress`, `restore`, `onChanged`, observable `state` / `isDragging` | `play` | section 4 |
| SOLVED drawing rule (no tray, no marks, no badge, no outlines) | `play.draw` | REQ-026 A1, rule 2 |
| `@Composable fun PlayArea(…, modifier = Modifier, solvedBar: (@Composable BoxScope.() -> Unit)? = null)`: appended last, so existing calls compile; the slot fills the region from `layout.trayTop` to the bottom while `state == SOLVED` | `play` | REQ-026 rule 2 (play owns `trayTop`; `app` cannot guess it) |
| `@Composable fun PuzzleThumbnail(puzzle: Puzzle, solved: Boolean, modifier: Modifier = Modifier)`: flat silhouette (`VisualTokens.SILHOUETTE`) or the picture clipped to the same silhouette path, fitted into its box with padding, plus `PlayLayout.forThumbnail(widthDp, heightDp, puzzle)` (a layout with no tray) so `silhouettePath` and `drawPicture` are reused, never copied | `play` | REQ-050 A2 (O-09: play lends its drawing). It draws the vector `drawPicture`, not the cached bitmap of DA-42 (one slot would thrash across cells; at thumbnail size the AA difference is invisible) |

Kernel and `play` tests of these are scaffolding (no REQ token); the acceptance IDs are covered in `browse` and `app` (section "Test seams").

### 7. `app`: wiring and the carried WO-003 items (rev 1: N1, N7, N8)

- **`AppViewModel(filesDir: File)`** (Factory takes `filesDir` from the activity): `library = PuzzleLibrary.packaged()`, `store: JsonProgressStore = JsonProgressStore(filesDir)` (concrete type, so `onStop` can call `sync()`), `host = SessionHost(reducedMotion = { !ValueAnimator.areAnimatorsEnabled() })`, `controller = BrowseController(library, store, host)`, `host.onChanged = controller::persist`, then `controller.start()`. It still exists because rotation must keep the session (DA-26).
- **`SessionHost : PuzzleHost`** (in `app`): `val session by mutableStateOf<PlaySession?>`; `show` builds `PlaySession(puzzle, reducedMotion, onChanged = { onChanged() })` and `restore`s it; `capture` and the two properties delegate to the session (`NEW` / `false` before the first show).
- **`TangramApp(controller, host)`** composes: paper background, `safeDrawing` insets, `BrowseTopBar`, then a `Box(weight 1)` holding `PlayArea(host.session, …, solvedBar = { SolvedBar(controller.shownBestSeconds, controller::restart, controller::next) })` and, while `shownState == IN_PROGRESS`, `RestartButton` at `TopStart`; last, `AllPuzzlesOverlay(controller, thumbnail = { p, solved, m -> PuzzleThumbnail(p, solved, m) })` while `gridOpen`. `BackHandler(enabled = gridOpen) { closeGrid() }` (REQ-050 "or the back button"; O-05: only `app` knows back). With the grid closed, back leaves the app, as in WO-003.
- **`MainActivity`:** `onCreate` as now (it calls `interruptDrag()` after recreation); `onPause`: `host.session?.interruptDrag()` then `controller.persist()`; `onStop`: `model.store.sync()`. No manifest change.
- **DA-23 retired (DA-56).** Browsing reaches every puzzle, including the full-set tray, so the debug aid has no job left. Delete `app/src/debug/.../PuzzleExtra.kt`, `app/src/release/.../PuzzleExtra.kt`, `app/src/testRelease/.../PuzzleExtraReleaseScaffoldingTest.kt` and the `beforeVariants` stanza in `app/build.gradle.kts`. The exported launcher then reads no extra in any build type. `STATUS.md` still gives Jami the `--es puzzle` adb line; the orchestrator removes it at the close (the grid replaces it).
- **N4 (DA-57).** `IPuzzleLibrary` promises a non-empty list and `PuzzleLibrary` already `check`s it (DA-10): an empty library is a packaging error that the build tests and the on-device library check (DA-22) catch. `BrowseController` repeats the same `check` with a clear message instead of an opaque `first()` exception; no empty-library screen is built (D2). The unbounded title is fixed by the top bar's one-line ellipsis.
- **Process death.** A relaunch constructs a new ViewModel, reads the file, `start()` opens `lastShown` and `show` restores the session. An open grid is not restored (it is closed on relaunch; REQ-050 only asks that closing returns to the same puzzle).
- **WO-003 app tests.** The existing `app/src/androidTest` tests assume a fresh first puzzle. With persistence they need a clean store: add a `ResetStoreRule` (deletes `filesDir/progress*.json` before the activity launches; used through a `RuleChain` outside the compose rule). Conditions (rev 1, N7, DA-62): the **WO-004 Acceptance Test Author (D-8)** makes the change, never an implementer; the diff to the WO-003 app tests is **setup only** (the outer rule), every assertion unchanged, and the orchestrator reads that diff; D-7's brief says those tests are expected to fail until D-8 lands and that D-7 must not edit them; the WO close lists it on the "corrected checks" line as a test-environment amendment (no checker was wrong, so no `proposals.md` entry). **Seeding recipe for tests:** after the rule deletes the files and before the activity launches, a test writes through `JsonProgressStore(InstrumentationRegistry.getInstrumentation().targetContext.filesDir)`; "close and relaunch" is a second `ActivityScenario.launch`. The rule is a test helper, not a product hook.

### 8. New user-visible strings (G-05, rule 13; V-05 on `browse`)

`browse/src/main/res/values/strings.xml` and `values-fi/strings.xml`. `app` and `play` add none. **Prototype Finnish is verbatim (F14); rows marked AI are Finnish the AI wrote and go to the owner list (rev 1: three rows left: `uusi` and the two descriptions; "Sulje" is replaced by the prototype's "Valmis"; the screen-reader texts avoid "/", which Finnish text-to-speech reads as "kautta").**

| Key | en | fi | Source |
|---|---|---|---|
| `prev_puzzle` | Previous puzzle | Edellinen tehtävä | prototype |
| `next_puzzle` | Next puzzle (hold: next unsolved) | Seuraava tehtävä (pidä pohjassa: seuraava ratkaisematon) | prototype |
| `all_puzzles` | All puzzles | Kaikki tehtävät | prototype |
| `done` (grid button, replaces `close_overlay`) | Done | Valmis | prototype |
| `state_new` | new | uusi | **AI** |
| `state_in_progress` | in progress | kesken | prototype |
| `state_solved` | solved ✓ | ratkaistu ✓ | prototype |
| `restart` | Restart | Aloita alusta | prototype |
| `retry` | Retry | Uudestaan | prototype |
| `next` | Next | Seuraava | prototype |
| `best_time` | best time | paras aika | prototype |
| `best_time_none` | – | – | symbol |
| `puzzle_counter` | %1$d / %2$d | %1$d / %2$d | format only |
| `puzzle_counter_description` | Puzzle %1$d of %2$d. Show all puzzles | Tehtävä %1$d, yhteensä %2$d. Näytä kaikki tehtävät | **AI** (worded for text-to-speech: no "/") |
| `rating_description` | Difficulty %1$d of 5 | Vaikeus %1$d viidestä | **AI** |
| `grid_cell_description` | %1$d. %2$s · %3$s | %1$d. %2$s · %3$s | format only |
| `time_minutes_seconds` | %1$d:%2$02d | %1$d:%2$02d | format only (best-time value; hours are WO-008's) |

None of these words match REQ-002 A2's mode vocabulary. Puzzle titles stay in the puzzle files (`title[lang]`).

### 9. Build changes: one first step (the only task that edits build files)

`settings.gradle.kts`: `include(":store")`, `include(":browse")`. `store/build.gradle.kts`: `kotlin.jvm` plugin, `implementation(project(":kernel"))`, `(":contracts")`, `libs.kotlinx.serialization.json`, `testImplementation(libs.junit)`. `browse/build.gradle.kts`: a mirror of `play/build.gradle.kts` (android-library, kotlin-compose, Compose foundation, kernel, contracts; test scope adds `content`, the androidTest libraries and the espresso 3.7.0 pin of DA-39). `app/build.gradle.kts`: add `store` and `browse`, drop the `beforeVariants` stanza, add `androidTestImplementation(project(":store"))` only if a test needs it (a test may also use the public `JsonProgressStore` through the main classpath, which `app` already has). Catalog: no change. Manifest: no change.

### 10. Verification channels

The same as WO-003: JVM unit tests (`store`, `browse`, `kernel`, `play`) and Compose device tests on `Medium_Phone_API_37.0`; the API 26 release launch stays waived (DA-43). Test & Verify runs `assembleDebug test connectedDebugAndroidTest`, V-01/V-05/V-06/V-07, trace-check, and the fixture guards of section 2. "Kill" is covered at two levels: a JVM test builds a second `JsonProgressStore` on the same directory (what a new process sees), and an `app` device test closes the activity scenario and launches a fresh one (a new ViewModel reading the file). A literal `am force-stop` needs UiAutomator, which is not on the allowlist, so it is not claimed.

## Alternatives considered

| Fork | Chosen | Not chosen, and why |
|---|---|---|
| **Where browsing orchestration lives** | `BrowseController` in `browse` with the `PuzzleHost` seam | (a) In `app`/`AppViewModel`: simplest, but REQ-024/025/026/050's logic would sit outside its code home and trace rule 3 could not find a covering test there. (b) `browse` depending on `play`: breaks G-06. The seam costs four members and buys JVM tests of save, restore, restart and wrap with fakes |
| **Session lifecycle** | a new `PlaySession` per visit, always restored from the store | a map of live sessions (second source of truth, memory held, tests of "which wins"); a `session.reset(puzzle)` (stale-state surface, re-opens CR-1 F2 class bugs). New-per-visit makes the relaunch path the everyday path |
| **Save cadence** | after each settled event (binding F4) + leave + `onPause` | a debounce or pause-only save: contradicts F4 and risks the player's work; not a fork in practice |
| **fsync per save** | atomic rename, no per-save fsync, `sync()` from `onStop` (DA-47) | fsync every save: survives power loss, but a measurable hitch at the lock/solve on slow flash for a protection REQ-025 does not ask for; `onStop` narrows the exposure to a power cut between a save and the next stop |
| **Solved puzzle's saved pieces** | none, stripped by `browse` (DA-48) | keep the final positions: matches "saved per piece" literally, but exposes where pieces were (REQ-026 A1) and does nothing for Retry. REQ-025's statement is about an unsolved puzzle. Policy in `play`'s `toProgress`: then the REQ-026 A1 test in `browse` could only test a fake |
| **Damaged top-level section** (rev 2, E4) | that section reads as fresh, the rest loads, original copied once to `progress-partial-*` (DA-64) | the whole document moved aside: stricter than the KDoc's "reads as a fresh install for the part that cannot be read" |
| **Where `sanitize` lives** | `browse` (kernel + contracts only) | `PlaySession.restore` (A3 would then be covered only in `play`, rule 3); `kernel` (it needs no piece of the lock search beyond the one public predicate, so the policy sits with the caller) |
| **Solved bar placement** | a slot in `PlayArea`, filled from `trayTop` down | `app` positioning with a guessed height (breaks on tablets and font scale); `play` knowing `browse` (G-06) |
| **Grid thumbnails** | `play` lends `PuzzleThumbnail` through a slot (O-09) | re-drawing silhouettes in `browse` (the O-09 "lent, never duplicated" rule); pre-rendered bitmaps (the `Tangrams/previews` files are build outputs and are not packaged, G-08) |
| **`puzzle` extra (DA-23)** | retire | keep it debug-only: harmless, but it keeps an unused input path and its release stub, test and build stanza alive for no reader now that browsing exists |
| **Long-press placement** | `combinedClickable` with a `LocalViewConfiguration` delegate of `max(500, platform)` (rev 1; my first reason for rejecting it, "not adjustable", was wrong) | a custom `pointerInput`: more code, and it would have to reproduce indication, slop and semantics |
| **Empty library (N4)** | fail loudly at one place (DA-10 consistent) | a graceful "no puzzles" screen: new UI and two strings for a state the contract and the build tests exclude |
| **Rational and Q2 encoding** | strings `"n/d"` in `{"a","b"}` | JSON numbers (would round-trip through a double and break G-03) or 4-int arrays (unreadable in a bug report) |

I do **not** recommend presenting options to the human early: the one irreversible choice (the v1 shape) is fixed by the contract's own field list and ADR-005, and the rest is reversible.

## Test seams *(frozen here; each seam names the task that delivers it)*

Task names are the design's suggested cut (D-0 … D-9, section "Suggested cut"). The Planner maps them to `TASK-0nn` and, per AGENTS.md, writes any change back into this design and `decisions.md` before the test author starts. Test fixtures use literal coordinates and the real puzzles through test-scope `content`. **Test adapters fail loudly (`error(…)`), never `?.invoke`.** A test that pins a decision carries `// decision DA-n` and no `REQ-NNN.An` token.

| Seam (exact signature) | Visible to | Delivered by |
|---|---|---|
| `class JsonProgressStore(dir: File, nowMs: () -> Long = System::currentTimeMillis) : IProgressStore`; `companion { const val FILE_NAME: String = "progress.json"; const val VERSION: Int = 1 }`; `fun sync()` (public, returns Unit, never throws); `internal val movedAside: File?`, `internal val partialCopy: File?` (both null when not applicable), `internal val asideFailed: Boolean`, `internal val failedWrites: Int` | `store/src/test`; `app` androidTest and `MainActivity` (public part) | D-2 |
| `store/src/test/resources/fixtures/progress-v1.json`, `…/progress-v1-fresh.json` (frozen, hash-pinned, guard-locked after D-2); `…/damaged-entry.json`, `…/damaged-section.json`, `…/version-2.json`, `…/huge-rational.json`, `…/denominator-63.json` (unfrozen, renamed in rev 2 so no `progress-v1*` glob catches them) | `store/src/test` | the two frozen fixtures + `FrozenV1FixtureTest.kt`: **D-8 / TASK-T4a** (the first is the section 2 document; plan v1.2, DA-67); the unfrozen ones: D-2 / TASK-023, which passes the frozen ones unedited |
| `LockSearch.isValidPlacement(silhouette: Silhouette, others: List<PlacedPiece>, piece: PieceId, turn: Turn, mirrored: Boolean, at: ExactPoint): Boolean` | `kernel/src/test`; `browse` (main) | D-1 |
| `PuzzleStates.onRestart(state: PuzzleState): PuzzleState`, `PuzzleStates.onRetry(state: PuzzleState): PuzzleState` | `kernel/src/test`; `browse` (main) | D-1 |
| `PlaySession(puzzle: Puzzle, reducedMotion: () -> Boolean = { false }, resolver: DropResolver = DropResolver(puzzle), onChanged: () -> Unit = {})`; `fun toProgress(base: PuzzleProgress): PuzzleProgress` (non-null; every piece, any state; interrupts a drag first); `fun restore(saved: PuzzleProgress)` (fresh session only; complete In progress restores as Solved); `val state: PuzzleState`; `val isDragging: Boolean` (both observable); `onChanged` is called after a drop resolves, a tray turn or mirror, a successful board turn or mirror, and never from `restore`, `toProgress` or `interruptDrag` | `play/src/test`, `play/src/androidTest` | D-3 |
| `@Composable fun PlayArea(session: PlaySession, layoutClass: LayoutClass, trayRows: List<List<PieceId>>, screenHeight: Dp, modifier: Modifier = Modifier, solvedBar: (@Composable BoxScope.() -> Unit)? = null)` | `play/src/androidTest`, `app` | D-4 |
| `@Composable fun PuzzleThumbnail(puzzle: Puzzle, solved: Boolean, modifier: Modifier = Modifier)` | `play/src/androidTest`, `app` | D-4 |
| `interface PuzzleHost { val state: PuzzleState; val isDragging: Boolean; fun show(puzzle: Puzzle, progress: PuzzleProgress); fun capture(base: PuzzleProgress): PuzzleProgress? }` | `browse/src/test`, `browse/src/androidTest`, `app` | D-5 |
| `class BrowseController(library: IPuzzleLibrary, store: IProgressStore, host: PuzzleHost)` with `val puzzles: List<Puzzle>`, `val index: Int`, `val current: Puzzle`, `val shownState: PuzzleState`, `val gridOpen: Boolean`, `val shownBestSeconds: Long?`, `fun start()`, `fun previous()`, `fun next()`, `fun nextUnsolved()`, `fun open(index: Int)`, `fun openGrid()`, `fun closeGrid()`, `fun restart()`, `fun persist()`, `fun stateOf(index: Int): PuzzleState`. `previous`, `next`, `nextUnsolved`, `open`, `restart` and `openGrid` are no-ops while `host.isDragging`; `start()`, `open` and `restart` all go through a private `showAt(i)`; every value is undefined before `start()` | same | D-5 |
| `internal object ProgressRestore { fun sanitize(puzzle: Puzzle, saved: PuzzleProgress): PuzzleProgress }` | `browse/src/test` | D-5 |
| `BrowseTopBar(controller: BrowseController, layoutClass: LayoutClass, modifier: Modifier = Modifier)`; `AllPuzzlesOverlay(controller: BrowseController, thumbnail: @Composable (Puzzle, Boolean, Modifier) -> Unit, modifier: Modifier = Modifier)`; `RestartButton(onClick: () -> Unit, modifier: Modifier = Modifier)`; `SolvedBar(bestSeconds: Long?, onRetry: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier)`; tags `top-bar`, `prev-button`, `next-button`, `puzzle-title`, `rating-dots` (content description `rating_description`), `puzzle-counter` (text `n / total`, 1-based), `puzzle-state` (text = the state string), `restart-button`, `solved-bar`, `retry-button`, `best-time` (text = the value only: `–` or `m:ss`), `solved-next-button`, `all-puzzles` (covers the top bar while open), `grid-close`, `grid-cell-<puzzleId>`, `grid-dot-<puzzleId>` | `browse/src/androidTest`, `app/src/androidTest` | D-6 |
| `class SessionHost(reducedMotion: () -> Boolean) : PuzzleHost` with `val session: PlaySession?` (snapshot state) and `var onChanged: () -> Unit`; `AppViewModel(filesDir: File)` with `library`, `store`, `host`, `controller`; `TangramApp(controller: BrowseController, host: SessionHost)` | `app` | D-7 |

**Value shapes, stated exactly (rev 1).** `PlaySession.toProgress(base).pieces` has exactly one key per piece of the puzzle in every state (non-null, never `Dragged`). What the **store** holds after `BrowseController.persist()`: SOLVED gives `emptyMap()`; NEW and IN_PROGRESS keep their entries minus `InTray` entries at the resting turn, unmirrored (absent means resting). `bestSeconds` and `puzzleSeconds` come through `toProgress` from `base` unchanged. `shownBestSeconds: Long?` is `null` when none (nothing in WO-004 sets one). `stateOf(index)` is never null. `PuzzleHost.capture` is null only before the first `show`. `lastShownPuzzle()` is null on a fresh install only (a reset does not touch it). `JsonProgressStore.sync()` returns Unit and never throws. Controller values are undefined before `start()`; `index` is 0-based, the counter 1-based. The `board`/`play-area` tags and `BoardTransform` of WO-003 are unchanged.

### Acceptance IDs and where the covering test lives *(rule 3: the code home `browse` always; `app` adds, never replaces)*

| ID | Covering test in its code home | Added cross-slice test (`app/src/androidTest`) |
|---|---|---|
| REQ-003 A1 | `browse/src/test`: from every index, `open(j)` reaches every `j` with all puzzles New (fake store and host) | the grid has a cell per puzzle and each opens its puzzle; › walks the whole list without solving anything |
| REQ-003 A2 | `browse/src/test`: leave and return gives the same state (fake host captures 3 pieces, return passes them back) | the same with real touch: 3 pieces, ›, ‹, same 3 pieces |
| REQ-024 A1 | `browse/src/test` (`next()` from New and from In progress) + `browse/src/androidTest` (tap `next-button`, title changes) | with the real library |
| REQ-024 A2 | `browse/src/androidTest`: `puzzle-title` equals the puzzle's title in the device language and `puzzle-counter` equals `n / total`, for the first, a middle and the last puzzle | on the real top bar after each ›/‹ |
| REQ-024 A3 | `browse/src/test` (`next()` at the last index gives 0; `previous()` at 0 gives last) + `browse/src/androidTest` | on the real list |
| REQ-025 A1 | `browse/src/test`: capture, leave, return restores the same pieces; `store` round-trip is covered by G-09 tests (decision-tagged) | 3 pieces placed by touch, leave and return, and close then relaunch the activity: same places (pixel or size-mark oracle) |
| REQ-045 A1 (rev 2, E6) | `browse/src/test` `start()` row (i): empty store: `index` 0, `show(first, NEW)` (the first puzzle, tray of its own pieces). Token only here, as REQ-045's *library-level* A1 is WO-002's and this is the controller's fresh-install step | fresh store on screen, as below |
| REQ-025 A3 via `start()` (rev 2, E6) | `browse/src/test` `start()` row (ii): `lastShown` a known id with a displaced board piece: that `index`, the piece arrives `InTray` (the relaunch path sanitizes) | — |
| `start()` rows (iii), (iv) (rev 2, E6) | `// decision F4` tags, no REQ token: (iii) unknown `lastShown` gives `index` 0; (iv) `start()` writes nothing | — |
| REQ-025 A2 | `browse/src/test`: `restart()` leaves the store entry New, `pieces` empty, `puzzleSeconds` 0, best time kept | after the Restart pill, size marks of the triangles return (REQ-043: a mark exists only while the piece is in the tray) and no piece colour is on the board |
| REQ-025 A3 | `browse/src/test`: `sanitize` with a real puzzle and a displaced piece: that piece `InTray`, the others unchanged; (F2) a piece whose `at` is beyond the reader bound handed straight to `sanitize` goes to the tray without an exception (the 2^62 reader case is a `store` test, decision-tagged) | a store seeded with the displaced piece before launch: the piece is in the tray on screen |
| REQ-026 A1 | `browse/src/test` (real `browse` code, F6): a `FakeHost` that captures a SOLVED session **with all its pieces** gives a store entry with `pieces == emptyMap()`; a stored SOLVED entry that has pieces reaches `show()` with an empty map | solve by touch, leave, return: no piece colour in the board region, picture present |
| REQ-026 A2 | `browse/src/test`: `restart()` from SOLVED gives New, empty `pieces`, best kept | `retry-button` by touch: empty silhouette, tray cells and marks back |
| REQ-050 A1 | `browse/src/test` (puzzles numbered 3 and 7 unsolved, the others solved, shown puzzle 3 i.e. `index` 2: `nextUnsolved()` gives `index` 6; all solved gives a normal next) + `browse/src/androidTest` (`performTouchInput`: down, advance the test clock at least 500 ms, up, lands on the unsolved puzzle; a shorter press is a normal next) | with seeded store states |
| REQ-050 A2 | `browse/src/androidTest`: one `grid-cell-<id>` per puzzle in library order; the fake thumbnail slot receives `solved = true` only for solved puzzles; `grid-dot-<id>` only for In progress | real thumbnails: a solved puzzle's cell has pixels other than the silhouette colour, a New one is flat |
| REQ-050 A3 | `browse/src/test`: `open(j)` writes only the leaving puzzle and `lastShown`; every other puzzle's stored progress is equal before and after | the same on the real store file |
| REQ-040 A1, REQ-045 A1 on screen | — (content leaves; code home is `content`, covered in WO-002) | fresh store: `puzzle-title` is the first library puzzle, `rating-dots` shows rating 1, `BoardTransform.trayCellsPx` has 3 cells |
| REQ-041 A1 on screen | — | fresh store, › through the list: the sequence of titles equals the library's order and the library's kinds are mini, then four warm-ups |

Scaffolding tests with no REQ token: kernel (`isValidPlacement` incl. the bound extremes, restart/retry), store (G-09 fixture guards, damaged document and section, damaged entry with partial copy, 2^62 and out-of-bound rationals, aside-failure overwrite, aside pruning, write failure, skip-equal, reset leaves `lastShown`, `sync`), play (`restore` incl. the complete In-progress case, `toProgress`, `onChanged` timing, observability), browse (`// decision F5`: `open`, `restart` and `openGrid` are no-ops under a dragging `FakeHost`; `persist` canonical form; Restart pill touch area ≥ 48 dp `// guardrail`), `PuzzleThumbnail` pixels, `AllPuzzlesOverlay` wiring, `SessionHost`. The orchestrator greps the new test files for `REQ-NNN.An` before Test & Verify (AGENTS.md).

## Risks & edge notes

- **Highest risk, for the Design Reviewer: the v1 save format and the restore path (sections 2–4).** It is the one irreversible choice (a later change is a hard-stop migration, G-09) and its failures are silent until a player's progress is gone. Questions to attack: is there any field REQ-005/029–034 or WO-007/008 will need that v1 lacks (I found none); are the reader rules (including the rational bound, DA-63) too lenient or too strict; is dropping the per-save `fsync` an acceptable trade (DA-47). Mitigations (rev 1): the two-way fixture freeze with pinned hashes and a `guard.json` lock after D-2, per-entry damage tolerance with a partial copy, whole-document move-aside, a fail-closed `sanitize`, the same `showAt` restore path on every navigation and on relaunch.
- **Second: the save/lifecycle ordering** (section 3): a save during a drag, `onPause` ordering, `onChanged` re-entrancy. `toProgress` interrupts first and never calls `onChanged`; `open`/`restart` refuse while dragging; `restore` calls nothing. A JVM test must drive a fake session through `release → onChanged → capture` without recursion.
- **Data-format freeze, summary.** Anything WO-007/008 find missing is a v2 plus migration and migration test, a hard-stop. Their design authors should check REQ-029–034 against the section 2 table at their design step.
- **Process death.** Covered by the store (every settled event is on disk); at most the drag in progress and the New to In progress flip of an interrupted first drag are lost (F4). The open grid is not restored.
- **Rotation during browse.** The ViewModel keeps the controller, host, session and `gridOpen`; the long press in flight is cancelled by the pointer restart (no fire); `onPause` persists before the recreation. A rotation mid-drag is the WO-003 F5 path, then `persist()`.
- **A save during a drag.** Impossible from the UI (guard) and safe from `onPause` (interrupt first). `capture` also interrupts, so the format never needs a `dragged` value.
- **A large grid on a small phone.** 360 dp wide gives three columns of about 100 dp, and 20 puzzles are seven rows: a vertical scroll, with the shown puzzle scrolled into view. The cells are at least 84 dp, above REQ-050's 64 dp. A `LazyVerticalGrid` draws only visible cells. Watch: `silhouettePath` keeps a single-slot cache, so a thumbnail draw rebuilds its union each time; fine for a settled grid, and a bounded cache is the first fix if scrolling stutters.
- **Top bar width at 360 dp.** ‹ 52 + counter ≈ 64 + › 52 leaves about 190 dp for the title and the dots/state line, and WO-007's ⚙ will take another 44. The long titles today are 13 characters; the one-line ellipsis keeps the bar intact at large font scale. If the second line gets cramped with the ⚙, the state text can become a glyph without a REQ change.
- **Solved bar replaces the tray at the moment of the solve.** The last piece's glide and the pop run over the board while the tray area already shows the bar. If the playtest dislikes that, delaying the bar to the picture fade is a one-condition change in `PlayArea`.
- **WO-003 tests re-run.** The solved-state drawing change can affect a WO-003 device test that samples the tray after a solve (none is known); such a failure is a test-adapter question to be judged against DA-35/DA-37, not an automatic product defect. The existing app tests need the `ResetStoreRule` (section 7).
- **Hard-stop 2 enforcement.** Without the `guard.json` lock of the v1 fixtures (section 2, orchestrator step after CR-2 is forward; plan v1.2) the freeze is honour-system; the step is in the close checklist.
- **Time is untouched.** WO-004 writes no `puzzleSeconds` or `bestSeconds`; the solved bar shows `–` until WO-008. Do not read that as a defect.
- **Hard-stop reminder for implementers.** Do not edit `contracts/`, `kernel/…/model/`, or the frozen fixture. A "needed" change there stops the pipeline.

## Assumptions to log in `decisions.md` (agent proposals, class 12; continue at DA-46)

*Rev 2 changes: DA-63 (dyadic denominators) and DA-64 (section tolerance, G-09 conflict named) reworded.* *Rev 1 changes: DA-46, 47, 48, 50, 53, 54, 55, 57, 60, 62 reworded; **DA-58 withdrawn** (replaced by an open question to WO-007; the number stays, no renumbering); **DA-63…DA-66 added**.*

| # | Assumption | Basis |
|---|---|---|
| DA-46 | **The v1 save document** (section 2): one JSON object with `version`, `lastShown`, `puzzles`, `playTime`, `settings`; explicit serial names; exact rationals as normalized `"n/d"` strings in `{"a","b"}`; nulls written explicitly; all `IProgressStore` content from v1; one profile. Reader rules per the table: bad document or bad section means move aside whole, bad entry means that puzzle New (with a partial copy). Frozen by the fixtures `progress-v1.json` and `progress-v1-fresh.json`, their SHA-256 pins and the guard.json lock; they are the base of a future v2 migration test | G-09, ADR-005, REQ-025, F4; hard-stop 2 (row 11 `ai+inform`, migration test mandatory) |
| DA-47 | Write = temp file plus atomic rename on the main thread, skipped when the value equals the stored one; **no per-save fsync**, but `sync()` from `onStop`. Consequence: a power cut right after a save can lose that save or make the file unreadable, in which case **all** progress starts fresh (the file is moved aside, not deleted). A kill or a closed app never loses a completed save. A disk-level write failure never throws, memory stays current, the next save retries; it is the one behavioural note (no signature or KDoc change) in the contract-delta table | ADR-005, G-10, REQ-025 (kill, not power loss), N1, N9 |
| DA-48 | **Storage policy lives in `browse`'s `persist`/`sanitize`:** `PlaySession.toProgress` reports every piece in any state; `persist` stores a Solved puzzle with no pieces (REQ-026 A1) and drops resting unmirrored tray entries; New and In progress keep their pieces, so the tray turns of a New puzzle survive (F4) | TYPE-006, REQ-025, REQ-026 A1, decisions F2/F4, ADR-005 cadence |
| DA-49 | Save events: after each settled session event (`onChanged`, tray turns included), before leaving a puzzle, in `onPause` (interrupt first), on Restart/Retry; `sync()` in `onStop`; NEW to In progress at a drag start is written with the next event | decisions F4, DA-5/F5, O-08 |
| DA-50 | REQ-025 A3 reading: a saved board piece is kept iff it is inside the silhouette and overlaps no already-accepted piece (TYPE-004 validity, 1e-6), visited in tray order; otherwise it goes to the tray keeping its turn and mirror; any geometry exception also sends it to the tray; the sanitized data is not written back until the next save. Implemented as `LockSearch.isValidPlacement` + `ProgressRestore.sanitize`. Basis includes: a correctly saved piece can touch no current anchor (it locked against a piece later returned to the tray), so an anchor-based reading would reject correct saves | REQ-025 A3 and rule 4, O-01, G-10 |
| DA-51 | A puzzle visit is one `PlaySession`, always built by `PuzzleHost.show` from the store; ‹ › grid Next Restart Retry and relaunch share that path | REQ-003, REQ-025, DA-26 |
| DA-52 | On SOLVED (live or restored) the tray, marks, badge and dashed outlines are not drawn and the tray region shows the solved bar; a restored solved session has a settled picture and no placed pieces | REQ-026 rule 2 and A1, REQ-023 |
| DA-53 | Top bar: one-line ellipsized title, then rating dots and the state text (all three states, New included, as "new"); **every player control has a touch area ≥ 48 dp** (counter, ‹ ›, Restart, Retry, Next, grid Done), REQ-037 over Spec/02; WO-007's ⚙ needs 48 dp too | REQ-024 rules, REQ-050, REQ-037 (locked, Contract wins) |
| DA-54 | Long press on › is `combinedClickable` under a `LocalViewConfiguration` with `longPressTimeoutMillis = max(500, platform)`; it fires once and the release then does nothing; with no unsolved puzzle other than the current one (or all solved) it behaves like a normal press (the prototype would stay on the current puzzle) | REQ-050 rule 1; "› always moves on" (REQ-024 rationale); N2 |
| DA-55 | The grid: overlay over the whole screen **including the top bar**, with a "Done"/"Valmis" button, `Adaptive(84 dp)` cells, number label, accent border on the shown puzzle, a dot on In progress, picture on Solved; selecting a cell opens it and closes the grid; `openGrid` obeys the drag guard (F5); the open grid survives rotation (F3) but not process death | REQ-050, F3, F5 |
| DA-56 | DA-23 retired: the `puzzle` extra, its release stub, its release test and the build stanza are deleted | DA-23's own "drop at WO-004 if browsing makes it redundant"; D3 |
| DA-57 | N4: an empty library is the packaging error DA-10 describes, not a crash path from touch or stored data (so the WO's "must not crash (G-10)" is read that way); `BrowseController` fails with a clear `check`, no empty screen; the title is ellipsized; the size-mark half of N4 is WO-006's | WO-003 CR N4, DA-10, G-10 |
| DA-58 | **Withdrawn (rev 1, F4).** `resetAllProgress()` does exactly what the KDoc lists and leaves `lastShown` alone; which puzzle shows after a reset is an open question for WO-007 (REQ-032 vs the prototype) | `IProgressStore` doc, D4 |
| DA-59 | Restart and Retry are one operation `restart()`, with the TYPE-006 transitions in kernel `PuzzleStates.onRestart/onRetry`; on a New puzzle it is a no-op | O-07, REQ-025 A2, REQ-026 A2, F10 |
| DA-60 | Finnish strings written by the AI (section 8) go to the owner list: `uusi`, the counter and rating descriptions (worded for text-to-speech, no "/"); the grid button reuses the prototype's "Valmis" | AGENTS rule 13, F14 |
| DA-61 | New modules: `store` is Kotlin/JVM (stdlib file I/O, tree-parser serialization-json), `browse` is an Android library; `app` wires both; no new library | ADR-002, ADR-005, G-06, D5 |
| DA-62 | `app/src/androidTest` runs with a `ResetStoreRule` because the app now persists. The WO-004 Acceptance Test Author (never an implementer) amends the WO-003 app tests; the diff is setup only (outer `RuleChain` rule, every assertion unchanged) and the orchestrator reads it; D-7 must not edit them; the close lists it as a test-environment amendment on the "corrected checks" line | AGENTS lessons (test adapters), DA-26 ended |
| DA-63 | **v1 reader magnitude bound (frozen, rev 2 E1):** a stored rational is valid only as a normalized `"n"`/`"n/d"` with \|n\| ≤ 65536 and **d ∈ {1, 2, 4, 8, 16, 32, 64}** (dyadic: coprime denominators such as 63 and 61 overflow `Q2.signum` in a `READING_ORDER` comparison), else that puzzle entry is New; pinned by a kernel test with mixed dyadic denominators and opposite-sign coefficient pairs that calls `READING_ORDER` / `Q2.compareTo` directly, a store test where a d = 63 entry reads as New, and a content/golden check that every packaged coordinate's denominator divides 64 (real content uses only 1 and 2, numerators up to 12) | F2, G-09, G-10, ADR-003 (exact arithmetic throws on overflow) |
| DA-64 | Damage handling (rev 2): a bad document or a newer version is moved aside whole (`progress-bad-*`, `progress-v<N>-*`); a bad **section** reads as fresh for that section only and a bad entry reads as New, each with the original copied once to `progress-partial-*` before the first write; if the move-aside and the copy both fail, `asideFailed` is set and later saves overwrite. **Guardrail conflict with G-09 "never overwritten", governance row 13 `ai+inform` (E3):** `IProgressStore`'s "stored before it returns" wins in that both-failed case; the orchestrator logs it and surfaces it at checkpoint 4. Only the newest 3 aside files are kept | G-09, G-10, `IProgressStore` KDoc, F7, N9 |
| DA-65 | `persist` stores a canonical form (Solved without pieces, resting unmirrored tray entries dropped) and the store skips writes equal to what it holds, so an unchanged puzzle costs no I/O | N9, `SavedGame` KDoc ("absent means resting") |
| DA-66 | A restore of an In progress puzzle that is complete is a Solved restore (settled picture, nothing placed, no celebration, no best time) | N3, REQ-026 A1, REQ-022 |

## Suggested cut for the Planner

Size: medium, smaller than WO-003; the logic is JVM-testable and the UI is thin. Proposed tasks (arrow = compile dependency):

- **D-0 Build step** (the only one that edits build files, section 9).
- **D-1 Kernel:** `LockSearch.isValidPlacement`, `PuzzleStates.onRestart/onRetry`, `kernel/src/test`. (haiku is fine: mechanical)
- **D-2 `store`:** `JsonProgressStore` (incl. `sync()`, skip-equal, damage and aside rules, the magnitude bound), the codec, the unfrozen fixtures, `store/src/test` scaffolding (the frozen fixtures, their test and hash pins come from D-8 / TASK-T4a and are passed unedited — plan v1.2, DA-67). After D-0. Parallel with D-3, D-5. **sonnet** (format and I/O). **Right after D-2 the orchestrator locks the v1 fixtures and the frozen-fixture test in `.swdev/guard.json` and re-freezes the baseline (section 2).**
- **D-3 `play` session adapters:** `onChanged`, observable `state`/`isDragging`, `toProgress`, `restore` (incl. the SOLVED settle), `play/src/test`. After D-0. **sonnet**.
- **D-5 `browse` logic:** `PuzzleHost`, `BrowseController`, `ProgressRestore`, `browse/src/test` with `FakeProgressStore`/`FakeHost`. After D-0 and D-1. **sonnet** (lifecycle logic).
- **MANDATORY code-review checkpoint after D-1, D-2, D-3, D-5** (all JVM): the format, the restore path and the save ordering get reviewed before any UI exists.
- **D-4 `play` UI:** SOLVED drawing rule, `PlayArea` slot, `PuzzleThumbnail` + `PlayLayout.forThumbnail`. After D-3. **sonnet**; the orchestrator reads the diff (device-only behaviour).
- **D-6 `browse` UI:** top bar with the long-press gesture, grid overlay, solved bar, restart pill, strings en/fi (V-05). After D-5. **sonnet**; the orchestrator reads the diff.
- **D-7 `app`:** `AppViewModel`, `SessionHost`, `TangramApp`, `MainActivity` (`onPause`, `onStop`), `BackHandler`, retire DA-23. After D-4, D-6 and D-2. **sonnet**; the orchestrator reads the diff. Its brief says the WO-003 app tests are expected to fail until D-8 lands and D-7 must not edit them.
- **D-8 Acceptance Test Author** (independent, in parallel from the seams above): visible tests in `browse/src/test`, `browse/src/androidTest`, `app/src/androidTest` (incl. the `ResetStoreRule` and the setup-only amendment of the WO-003 app tests, DA-62); held-out slice for REQ-025 A3, REQ-050 A1/A3 and REQ-026 A1.
- **D-9 Checks:** V-01/V-05/V-06/V-07, trace-check, the fixture guards, a token grep of the new tests, the API 37 device run.

## Handoff — Design Author · WO-004

- **Scope:** REQ-003, 024, 025, 026, 050 (all acceptance IDs) + carried REQ-040/041/045 A1 on screen, DA-26 (persistence), DA-23 (retired), N4, G-09 fixture · governed touched: `IProgressStore` (first implementation, **no contract delta**), `IPuzzleLibrary` (consumes)
- **Inputs read:** AGENTS.md · WO-004 · build-map §1–2 · architecture.md (G-01…G-10, §3, O-03/07/08/09, §4, §5) · ADR-002/004/005 · decisions.md DA-1…DA-45 + F2–F20 · REQ-003/005/024/025/026/029–034/040/041/045/050 · req_types TYPE-006 · features #Browsing · design-inputs, Spec/01 §6, Spec/02, the prototype's browse/grid/long-press code · contracts progress and puzzle files · `content`/`kernel`/`play`/`app` sources (PlaySession, PlayArea, PlayLayout, PictureDrawing, DropResolver, LockSearch, PuzzleStates, AppViewModel, MainActivity, TangramApp) · designs/WO-003-design.md and its two reviews · V-05/V-06
- **Result:** `C:\GitHub\AI\TangramNoAds\designs\WO-004-design.md`
- **Status:** forward to the Design Reviewer: attack sections 2–4 first (the v1 format, the restore path, save ordering), then the `PuzzleHost` seam and the DA-50 reading of REQ-025 A3
- **Traceability delta:** REQ-003/024/025/026/050 ↔ `browse.BrowseController` / `ProgressRestore` / `BrowseTopBar` / `AllPuzzlesOverlay` / `SolvedBar` / `RestartButton`; REQ-025 persistence ↔ `store.JsonProgressStore` + `play.PlaySession.toProgress/restore`; TYPE-006 restart/retry ↔ `kernel.PuzzleStates`; REQ-025 A3 ↔ `kernel.LockSearch.isValidPlacement`; REQ-050 A2 ↔ `play.PuzzleThumbnail`; no task, code or test links yet
- **Notes for next station (rev 1):** (1) log DA-46…DA-66 (DA-58 withdrawn, see the table); (2) the Finnish rows marked AI go on the owner list; (3) REQ ambiguities handled by assumption, none blocking: DA-48 (Solved stores no positions), DA-50 (A3 reading), DA-53 (New shown as a state), DA-54 (lone unsolved current puzzle); (4) open question for WO-007's design author: which puzzle shows after a reset (REQ-032 vs the prototype); (5) the first playtest may want a bar-timing change or a bounded thumbnail cache, one-line follow-ups; (6) the close checklist gets: "WO-003 app tests amended setup-only by D-8, diff read by the orchestrator", and the orchestrator step "lock the v1 fixtures in guard.json after D-2"
