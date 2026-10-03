# Design — WO-005 · #DevTools: the debug-only solution reveal

**Author:** Design Author  ·  **Date:** 2026-10-03  ·  **Status:** Revised (rev 1) after `reviews/WO-005-design-review.md` (1 B / 5 S / 7 N), then (rev 2) after its spot-check (E1–E4 and `Role.Button`); changed sections are marked "(rev 1)" / "(rev 2)"  ·  **Baseline:** architecture.md v1.0 (G-04, V-04 row, O-01, O-06, O-09), build-map v1.3, ADR-006, decisions.md (to DA-71, LOCK-V1), REQ-023 (read for rev 1), designs/WO-004-design.md and its reviews

**Rev 1 in one paragraph.** (F1) The DEV pill no longer shares the Restart slot: a second `PlayArea` slot is placed after the board is fixed, so debug and release boards are identical by construction (sweep result in section 3b). (F2, F3) V-04 gets a release-side "scanner not blind" canary, a self-building `--positive-control` mode that needs every marker kind, a deny-list over every non-dex entry, strict dex versions and total exit-code mapping. (F4) The A4 test declares what it reads as Gradle task inputs. (F5) The aid solve runs the normal REQ-023 timeline; only the best time is withheld. (F6) The DEV pill obeys the drag guard. Nits N1–N7 applied.

## Scope

- **Implements:** #DevTools, code home `devtools` (an Android library, debug only): REQ-046 A1–A4.
  - A3's "no best time" is checked at event level: a solve event carries `byAid`. Real best times arrive in WO-008.
- **Verifier:** V-04 (`.swdev/verifiers/v04_release_apk.py`, G-04), with `tools/check_apk_puzzles.py` folded in (carried in from WO-003, build-map §2).
- **Governed interfaces:**
  - `IPuzzleLibrary` (locked): consumed by `devtools` through the `Puzzle` values `app` hands it. No change.
  - `IProgressStore` (notify): reached only through the existing save path. **No signature change, no semantic change, no contract-delta.**
  - **The v1 save format is untouched** (section 5). Nothing under `contracts/`, `store/`, `kernel/…/model/`, `Requirements/` or the locked fixtures is edited.
- **Carried OUT:**
  - → WO-008: the aid-solve regression on real best times. The `onSolved(byAid)` event built here is its input; `SessionHost` forwards it then.
  - → WO-006: the REQ-037 A1 "every player control ≥ 48 dp" walk must skip the nodes tagged `dev-*` (REQ-037 exempts the #DevTools controls).
  - → WO-009: if release minification is ever switched on, V-04 reports "scanner blind" until its `CANARIES` list is adapted to the renamed classes (rev 2, E4); re-run it on the final artifact, and extend it to the `.aab` if one is uploaded (section 7).
- **Out of scope:** hints for players (REQ-046 Out of scope, Q10), best-time display (WO-008), settings (WO-007).

## Chosen design

### 0. Shape in one picture

```
release APK:  main  ──calls──>  DebugAids (app/src/release: no-op, composes nothing)
debug APK:    main  ──calls──>  DebugAids (app/src/debug: wraps devtools) ──> devtools (debugImplementation)
                 │                                                              │ kernel + contracts only (G-06)
                 │  neutral hooks in main, no devtools word anywhere            │ Puzzle -> shapes / poses
                 ├─ play: BoardSpace + boardOverlay slot (draws nothing itself) │
                 ├─ play: secondaryCornerControl slot (placed AFTER the board)  │
                 └─ play: PlaySession.solveByAid(poses) + onSolved(byAid)  <────┘ (called through a lambda `app` passes)
```

`devtools` never sees `play`, `browse` or `store`. It receives a `Puzzle`, a coordinate-mapping lambda and a `solveNow` lambda, and returns composables. `app` (debug source set) does all the joining.

### 1. Discovery: how `app` finds the aid in debug and gets nothing in release (G-04, ADR-006)

**Chosen: the source-set pair `DebugAids`** (the ADR-006 pattern, used before for DA-23).

- `app/src/main` calls a class named `DebugAids` that exists in **neither** main source set. It is a neutral name (no `devtools`, no passcode).
- `app/src/debug/kotlin/io/github/jamisuni/tangram/DebugAids.kt` is the real one. It holds the `DevToolsState` and forwards to the `devtools` composables.
- `app/src/release/kotlin/io/github/jamisuni/tangram/DebugAids.kt` has the **same public signatures** and empty bodies. It imports nothing from `devtools`.
- `app/build.gradle.kts` gets exactly one new line: `debugImplementation(project(":devtools"))`. Nothing named `implementation(project(":devtools"))` or `releaseImplementation(…)` may ever exist.

```kotlin
// identical in both source sets (types are all main-scope: contracts, kernel, play)
class DebugAids {
    @Composable fun CornerButton(puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean, blocked: () -> Boolean, modifier: Modifier = Modifier) // (rev 1, F6) blocked = the drag guard
    @Composable fun BoardOverlay(puzzle: Puzzle, space: BoardSpace)
}
```

Why this over the alternatives (see "Alternatives considered"):

- Compile-checked in both variants. The release variant compiles the stub, so a signature drift between the pair breaks `assembleRelease`, not a runtime lookup.
- No reflection, no `META-INF/services`, no manifest component, no shared interface that would need a home (the only modules `devtools` may depend on are `kernel` and `contracts`, and `contracts` is governed).
- The release dex contains one tiny class `…/tangram/DebugAids` and no `devtools` name, string or resource.

**The DA-56 history.** DA-56 deleted the `testRelease` source set and the `beforeVariants` stanza that enabled the release host-test variant. **Do not bring it back.** With no release unit-test variant, release behaviour is proved by V-04 on the real release APK and by the source-separation test (section 9, A4), not by a `testRelease` test. Consequence for tests: `app/src/test` compiles for the debug variant only and may call the real `DebugAids` / `devtools`; if someone re-enables release host tests it will stop compiling, which is the intended alarm.

### 2. The neutral hooks in `play` (the only release-code changes, G-04)

All three are neutral: nothing in them names devtools, reads a stored solution or knows a passcode.

**(a) `BoardSpace` and the `boardOverlay` slot** (O-09 already names "`play`'s board takes overlay slots for … the aid's overlay").

```kotlin
class BoardSpace(val dpPerUnit: Double, val originXDp: Double, val originYDp: Double) {
    fun toDp(point: Vec2): Vec2   // point in puzzle units -> dp relative to the play-area origin; same maths as PlayLayout.toDp
}
@Composable fun PlayArea(…, solvedBar: …? = null, cornerControl: …? = null,
    boardOverlay: (@Composable BoxScope.(BoardSpace) -> Unit)? = null,           // appended, in this order:
    secondaryCornerControl: (@Composable BoxScope.() -> Unit)? = null)           // (rev 1, F1) every existing call compiles
```

- The `boardOverlay` slot is composed over the whole play area, **above `PlayCanvas`, below `cornerControl`, `secondaryCornerControl` and `solvedBar`**, and only while `state != SOLVED` (a solved picture is never overlaid; DA-82).
- `PlayArea` adds no pointer input of its own to the slot. A slot that draws without a `pointerInput` / `clickable` takes no touch, so a drag, tap-turn or long press on the piece beneath still reaches the gesture adapter. This is pinned by a test (section 9, risk "overlay vs gestures").
- It is rebuilt with a fresh `BoardSpace` whenever the layout changes (rotation, window change).
- **`secondaryCornerControl` (rev 1, F1) never takes part in the board layout.** `PlayArea` computes the layout exactly as today: `computeWithCorner` for `cornerControl` alone (or `compute` when it is null). Only then is the secondary slot measured and placed, by the pure `PlayLayout.placeSecondary(layout, primary, widthDp, heightDp): RectDp?` of section 3b, whose result never feeds back into the layout. So the board, the scale and the Restart placement are identical with and without a secondary control, in debug and in release, by construction. A `null` result (no legal place) composes the slot unplaced and without semantics. The slot sits above `cornerControl` in z order.

**(b) `PlaySession.solveByAid(poses)`, the "place these poses and finish" hook of G-04 (rev 1: F5, N4).**

```kotlin
fun solveByAid(poses: List<PlacedPiece>): Boolean   // kernel.geometry.PlacedPiece; public; main thread
```

- It returns `false` and changes **nothing** (no state, no piece, no event) unless **all** of these hold:
  - the state is not `SOLVED`;
  - the poses name exactly the puzzle's pieces, each once;
  - each pose passes `LockSearch.isValidPlacement` on `Silhouette(puzzle.solution polygons)`, checked in list order against the poses accepted so far;
  - `SolvedCheck.isSolved(puzzle pieces, poses)` is true (a belt: it adds nothing once "exactly the puzzle's pieces, each once" holds).
- On `true` it behaves like a player's last lock, with the clock deferred to the next frame:
  1. calls `interruptDrag()` first (decisions F5/DA-5);
  2. puts every piece on the board at its pose (`Where.Board(at)`, turn, mirror) and clears any left-over `glides`, `pulse` and `shake` of the live board;
  3. sets `state` through the TYPE-006 transitions that exist today (`PuzzleStates.onPieceLocked(PuzzleStates.onTrayDragStarted(state), true)`, the New → In progress → Solved path an aid solve represents; no new kernel API, O-07);
  4. marks the solve **pending**: `onFrame(nowMs)` turns it into `solved = SolvedAt(nowMs, reducedMotion())` on the **next frame**, and `PlayArea`'s "needs frames" test treats a pending solve as alive. So the aid solve runs the **normal REQ-023 timeline** (pieces pop, 600 → 1400 ms picture fade, confetti 2.4 s; reduced motion respected exactly as for a player solve), and an idle board cannot give it a stale start time (DA-83, rev 1);
  5. calls `invalidate()`, fires `onSolved(true)`, then `onChanged()`.
  The normal `BrowseController.persist` runs from `onChanged`, which is the existing save path (section 5); `canonical` stores a Solved puzzle without its pieces (DA-48), so the board pieces seen during the animation are never saved.
- Only the **best time** is withheld for an aid solve (REQ-046 A3, REQ-030 rule 4), through `onSolved(byAid = true)`; today nothing sets a best time, WO-008 will honour the flag.
- **Why validated poses, not a bare `solveByAid()`.** Release code would otherwise contain a method that marks any puzzle solved when anything calls it, which is a back door that no player action could produce. Taking engine-valid poses makes the hook indistinguishable from a player placing those pieces: any exact cover solves (REQ-022). It also gives the owner real evidence: the aid proves, by the engine's own rules, that a stored solution can be placed. If the engine refuses, the aid says so (section 3, notice).
- The hook never reads `puzzle.solution` to *choose* poses. That stays in `devtools` (G-04: "any code that reads stored solutions in order to place pieces").

**(c) The solve event.**

```kotlin
class PlaySession(puzzle: Puzzle, reducedMotion: () -> Boolean = { false }, resolver: DropResolver = …,
                  onChanged: () -> Unit = {}, onSolved: (byAid: Boolean) -> Unit = {})
```

- `onSolved(false)` fires once from `release()` when a drop makes the puzzle SOLVED (before `onChanged`).
- `onSolved(true)` fires once from `solveByAid` (above).
- It never fires from `restore`, `toProgress`, `interruptDrag` or a settle on restore.
- This is the O-06 event for the one case REQ-046 A3 needs now. DA-27 ("built with its first consumer") is amended for this event only (DA-73). `SessionHost` does **not** forward it yet; WO-008's `time` is the first production consumer and wires it then.

### 3. The `devtools` module (G-04, G-05, G-06; REQ-046)

**Build.** `settings.gradle.kts`: `include(":devtools")`. `devtools/build.gradle.kts` mirrors `browse`'s: `android.library` plus `kotlin.compose`, namespace `io.github.jamisuni.tangram.devtools`.

- Main scope: `implementation(project(":kernel"))`, `implementation(project(":contracts"))`, Compose BOM and foundation. V-06 already lists `devtools: {kernel, contracts}`.
- Test scope: junit, `project(":content")` (real puzzles), and the same androidTest set as `browse` with the espresso 3.7.0 pin (DA-39).
- No new library (D5). The dialog is `androidx.compose.ui.window.Dialog` and the field is `BasicTextField`, both already on the classpath through foundation. **No material3.**
- `devtools` has no `AndroidManifest.xml` component, no permission and no `contracts` type beyond `Puzzle`.

**Contents** (every class public so the debug `DebugAids` can reach it; nothing here is a governed contract):

| Unit | What it is | REQ |
|---|---|---|
| `internal object DevPasscode { const val CODE = "0417"; fun accepts(input: String): Boolean = input == CODE }` | The passcode as one standalone string constant (section 6 explains why standalone). | REQ-046 rules 1 |
| `class DevToolsState` | Plain Kotlin plus `mutableStateOf`: `val unlocked: Boolean`, `val overlayOn: Boolean`, `val dialogOpen: Boolean`, `val notice: DevNotice`; `fun open()`, `fun close()`, `fun submit(code: String): Boolean`, `fun toggleOverlay()`, `fun solveFailed()`. `enum class DevNotice { NONE, WRONG_PASSCODE, SOLVE_FAILED }`. | A1, rules 1–3 |
| `object DevSolution` | `fun shapes(puzzle: Puzzle): List<SolutionShape>` and `fun poses(puzzle: Puzzle): List<PlacedPiece>?`. `SolutionShape(piece: PieceId, colour: Int, polygon: List<Vec2>, label: Vec2)` (units; `colour` is `TrayRules.colour(piece)`, the one colour table; `label` is the polygon centroid). `poses` is `PieceGeometry.poseOf(piece, polygon)` per solution entry (O-01: derived once, in the kernel) and `null` if any piece has none (G-10). | A2, A3 |
| `@Composable DevCornerButton(state, puzzle, solveNow, blocked, modifier)` | The small dashed DEV pill, plus the dialog it opens. `blocked: () -> Boolean` is the drag guard (rev 1, F6). | A1, A3, decisions F5 |
| `@Composable DevSolutionOverlay(state, puzzle, toDp, modifier)` | Draws nothing while `!state.overlayOn`. | A2 |
| `devtools/src/main/res/values{,-fi}/strings.xml` | Section 4. Every name prefixed `devtools_`. | rule 13, G-05 |

**Behaviour, REQ-046 rule by rule.**

1. **The DEV button.**
   - A pill, 32 dp high visible, dashed 1.5 dp outline in the muted colour, "DEV" 11 bold, opacity .75. This follows the prototype (`.devbtn`).
   - **Fixed size (rev 1, F1):** the touch box is exactly **56 × 40 dp** and the label ignores the system font scale (sized in dp-equivalent text, `fontScale` not applied) so the pill's footprint is the same in every language and at every font size. The placement sweep of section 3b relies on that.
   - While the overlay is on, the border is solid in the accent colour and the opacity is 1.
   - REQ-037 exempts it; Spec/02 line 59 says small on purpose.
   - Tag `dev-button`; semantics `Role.Button` (rev 2) with the content description `devtools_button_description`.
   - **Drag guard (rev 1, F6; decisions F5, O-09):** a click while `blocked()` is true does nothing (no dialog), exactly like Restart's `isDragging` guard. `app` passes `{ session.isDragging }`.
   - **Placement: section 3b.**
2. **The dialog** (`Dialog`, tag `dev-dialog`; Back and a tap outside close it; prototype `#devDlg`).
   - **Locked:**
     - the hint text;
     - a numeric, masked field (`KeyboardType.NumberPassword`, `PasswordVisualTransformation`, max 4 characters, tag `dev-passcode`);
     - the notice line (tag `dev-notice`);
     - Cancel and OK (tags `dev-cancel`, `dev-ok`).
   - OK calls `state.submit(code)`.
     - **Wrong code** ("1234", "", "04170"): `notice = WRONG_PASSCODE`, the field is cleared, `unlocked` stays false, nothing else changes, nothing is revealed. The `dev-show-solution` and `dev-solve-now` nodes do not exist in the tree.
     - **0417:** `unlocked = true`, `notice = NONE`, and the same dialog switches to the tools view.
   - **Unlocked:**
     - the hint;
     - a toggle `dev-show-solution` (text "Show the solution" / "Hide the solution");
     - `dev-solve-now` ("Solve this puzzle now");
     - the one-line note that an aid solve sets no best time;
     - `dev-done`.
   - `dev-show-solution` flips `overlayOn` and closes the dialog (as the prototype).
3. **Unlock lifetime.** `DevToolsState` is held by `DebugAids`, which the `AppViewModel` owns (section 3b, DA-76). It survives rotation and browsing and is gone with the process or the activity: "until the app is restarted" (REQ-046 rule 2). Nothing is persisted: no store field, no SharedPreferences.
4. **The solution overlay.**
   - One entry per solution piece. Each is drawn in `TrayRules.colour(piece)` at alpha .55, with a white 2 dp dashed outline (6 / 4 dp) and the piece id (LT1, MT, …) centred at the polygon centroid in 14 sp bold white (prototype `drawHint`).
   - The polygons come from the stored solution, mapped by `toDp`.
   - The overlay is a single `Canvas` with **no** pointer handling. It also publishes one semantics-only node per piece, `dev-solution-<PieceId.name>`, whose bounds are that polygon's bounding box in overlay coordinates (the DA-44 pattern: tests find shapes by tag, pixels stay a drawing concern).
   - It stays on while the player browses (the flag is in `DevToolsState`, outside the per-puzzle `key(session)`) until switched off in the dialog.
   - Never drawn on a SOLVED puzzle (slot rule, section 2a); it returns after Retry.
5. **Solve this puzzle now.**
   - `poses = DevSolution.poses(puzzle)`. If it is `null`, or `solveNow(poses)` returns `false`, then `state.solveFailed()`: the dialog stays open and `dev-notice` shows `devtools_solve_failed`. This is the one behaviour REQ-046 does not spell out (DA-80).
   - If `solveNow(poses)` returns `true`, the dialog closes.
   - `devtools` never touches a store or a time. That is what makes A3's "no best time" structural.
6. **Hidden on a solved puzzle.** `app` passes `Modifier.unplacedUnless(visible)` (the existing helper in `TangramApp.kt`) into `CornerButton`, so the pill is not placed and has no semantics while the puzzle is SOLVED. The overlay is hidden by the slot rule. (The secondary slot does not affect the layout either way, so no size-keeping is needed.) "And in the design sheets (`Spec/ui/`)" is a prototype/sheet rule (the sheets are generated from the prototype); it has nothing to build here.

### 3b. The DEV button's place on the board (rev 1, F1; DA-71, DA-75)

Today `cornerControl` (one slot) holds the Restart pill. `PlayArea` measures the slot's content and puts it in the first board corner the silhouette leaves free (top-left, top-right, bottom-left, bottom-right), else a reserved strip (DA-71). That decision shapes the board. **The DEV pill must not take part in it**: the owner tests the debug board, so debug and release boards have to be identical for every puzzle and size. (The first draft shared the Restart slot; the reviewer ported the layout and showed that it reserves a strip for `animals-cat` on 360 × 640 and shrinks the debug board 12 %, and that the `Column` fallback fails on four puzzles. Dropped.)

**Chosen: a second `PlayArea` slot, `secondaryCornerControl`, placed after the board and the Restart slot are laid out.**

```kotlin
cornerControl = { RestartButton(onClick = controller::restart, modifier = Modifier.unplacedUnless(restartVisible)) }   // unchanged
secondaryCornerControl = { aids.CornerButton(session.puzzle, solveNow = session::solveByAid,
                                             blocked = { session.isDragging },
                                             modifier = Modifier.unplacedUnless(controller.shownState != PuzzleState.SOLVED)) }
boardOverlay = { space -> aids.BoardOverlay(session.puzzle, space) }
```

**The placement function (frozen; pure, JVM-testable, no Compose):**

```kotlin
internal fun PlayLayout.Companion.placeSecondary(layout: PlayLayout, primary: ControlPlacement?, widthDp: Double, heightDp: Double): RectDp?
```

`primary` is the Restart placement `PlayArea` already has (null when there is no `cornerControl`); the primary rect is the slot's **measured** size whether or not Restart is shown, so the secondary does not jump when Restart appears. A candidate rect is **legal** when it lies inside the area, keeps the 4 dp clearance from the silhouette (`touchesSilhouette(rect, silhouetteDp, 4.0)` is false) and from the primary rect (4 dp gap), and lies above `layout.trayTop` (so it can never touch the solved bar, which fills the region below it). Order:

1. **Corners of the board rect, inset 4 dp, in the order bottom-left, bottom-right, top-right, top-left** (bottom-left first = REQ-046's ASSUMPTION).
2. **Beside Restart in its strip**, only when `primary.corner == STRIP`: left = primary.right + 8 dp, top = primary.top; must fit the area width less a 4 dp margin. (The reviewer's `shapes-square` 360 × 600/620 case.)
3. **Otherwise `null`** (rev 2, E3: the compact 32 × 32 step is dropped; it was used in 0 of 468 cases, contradicted the exact 56 × 40 seam and was speculative): the pill is not shown on that puzzle at that size.** It never overlaps the silhouette, Restart or the solved bar, and it never reserves layout space. The sweep below shows `null` is not reached for any packaged puzzle, any tested size, either language or a large font (the sweep asserts the count is 0; if a future puzzle reaches it, the test fails and the author decides).

**Sweep result** (the reviewer's Python port of `PlayLayout.compute/computeWithCorner/touchesSilhouette`, which reproduces the recorded `CornerControlSweepScaffoldingTest` output row for row; Restart placed first with its real width, then the 56 × 40 DEV rect through the algorithm above; 13 puzzles per row):

| Restart width (dp) | what it stands for |
|---|---|
| 82 | English "Restart" (estimate) |
| 118 | Finnish "Aloita alusta" (estimate) |
| 170, 230 | large font scale |

| Area (w × h dp) | DEV corner per puzzle, all four Restart widths |
|---|---|
| 360 × 780, 390 × 844, 800 × 1100, 800 × 1180, 1280 × 700 | **bottom-left on 13 of 13**, always |
| 360 × 640 | bottom-left on 10; bottom-right on `animals-cat` and `shapes-warmup-2` (and `shapes-warmup-1` at 82); top-right on `shapes-warmup-1` at 118 / 170; a mix of the same at 230 |
| 390 × 700 | the same split as 360 × 640 (bottom-left on 10, one to three others bottom-right or top-right) |
| 360 × 620 | bottom-left on 9–10; bottom-right on `animals-cat` / `warmup-2`; top-right on `warmup-1` (118, 170); **`shapes-square`: beside Restart in its strip** (Restart is in a strip there) |
| 360 × 600 | bottom-left on 8–9; bottom-right on three or four warm-ups / the cat; top-right on `warmup-1` (118+); **`shapes-square`: beside Restart in its strip** |

- The `null` outcome (no place) occurs **0 times** across 9 sizes × 4 Restart widths × 13 puzzles (468 cases). With a DEV pill that grew with the font (84 wide at the large setting) one case, `shapes-warmup-2` at 360 × 640, found no corner; fixing the pill's size (section 3, rule 1) removed it.
- **Board parity holds by construction:** `PlayArea` computes the layout from the primary slot alone exactly as today and `placeSecondary` only reads it. A device test also asserts it (section 9): `BoardTransform` (scale and origin) is identical with and without the secondary slot.
- Debug/release difference left: only the pill itself (debug) and its corner. In release the stub composes nothing, so release looks exactly as WO-004.

**Departure from REQ-046's ASSUMPTION**, "the DEV button sits in the bottom-left corner of the board": it is bottom-left on most puzzle/size combinations and the **next free corner** (then beside Restart's strip) only where the silhouette or Restart occupies bottom-left. That is a logged DA (DA-75, rev 1) under governance row 12; **per the orchestrator's ruling no capture-side CHG is needed** for a narrower departure that only applies where bottom-left is not free.

### 4. Strings (G-05, rule 13, V-05)

`devtools/src/main/res/values/strings.xml` and `values-fi/strings.xml`; `app` and `play` add none. **Finnish written by the AI** goes on the owner list (DA-80). The prototype has no Finnish for the dev aid (its dialog is English-only), except "Valmis".

| Key | en | fi | Source |
|---|---|---|---|
| `devtools_button` | DEV | DEV | symbol |
| `devtools_button_description` | Developer tools | Kehittäjän työkalut | **AI** |
| `devtools_title` | Developer | Kehittäjä | **AI** |
| `devtools_hint_locked` | Testing aid, not part of the game. Enter the passcode to see how the pieces go. | Testausapu, ei osa peliä. Anna tunnuskoodi nähdäksesi, miten palat menevät. | **AI** |
| `devtools_passcode_label` | Passcode | Tunnuskoodi | **AI** |
| `devtools_wrong_passcode` | Wrong passcode. | Väärä tunnuskoodi. | en: REQ-046 verbatim; fi **AI** |
| `devtools_ok` | OK | OK | symbol |
| `devtools_cancel` | Cancel | Peruuta | **AI** |
| `devtools_hint_unlocked` | Unlocked until the app is restarted. The solution overlay stays on while you browse puzzles. | Auki, kunnes sovellus käynnistetään uudelleen. Ratkaisu pysyy näkyvissä, kun selaat tehtäviä. | **AI** (rev 1, N6 wording) |
| `devtools_show_solution` | Show the solution | Näytä ratkaisu | **AI** |
| `devtools_hide_solution` | Hide the solution | Piilota ratkaisu | **AI** |
| `devtools_solve_now` | Solve this puzzle now | Ratkaise tämä tehtävä nyt | **AI** |
| `devtools_no_best_time` | A puzzle solved here does not set a best time. | Näin ratkaistusta tehtävästä ei tallennu parasta aikaa. | **AI** (rev 1, N6 wording) |
| `devtools_solve_failed` | The game engine could not place this puzzle's stored solution. | Pelimoottori ei saanut tämän tehtävän tallennettua ratkaisua paikoilleen. | **AI** (rev 1, N6 wording), not in REQ-046 (DA-80) |
| `devtools_done` | Done | Valmis | prototype (`browse` reuses it too) |

The piece ids on the overlay are the file ids (LT1, MT, …), not translated, like the size marks (REQ-047). The passcode text field has no hint text (a hint would leak the code into the UI). The English "Wrong passcode." must equal the REQ's string character for character; its test reads the resource, not a literal.

### 5. Solve-now through the layers, and the v1 save format (hard-stop check)

```
DEV dialog "Solve this puzzle now"
  -> devtools: DevSolution.poses(puzzle)            (kernel PieceGeometry.poseOf, one pose per stored piece)
  -> solveNow lambda from app main                  = session::solveByAid
  -> PlaySession.solveByAid(poses)                  validates with the lock engine, places, SOLVED, normal solve timeline from the next frame, onSolved(true), onChanged()
  -> SessionHost.onChanged -> BrowseController.persist()   (unchanged WO-004 path)
  -> IProgressStore.saveProgress(id, canonical(captured))
```

- `play` and `browse` know nothing about devtools. `browse` is not touched at all: `persist` already runs on every `onChanged` and `canonical` already stores a Solved puzzle without pieces (DA-48).
- **What is stored:** `{"state":"solved","pieces":{},"puzzleSeconds":<unchanged>,"bestSeconds":<unchanged>}`. `persist` is `base.copy(state, pieces)`, so a never-solved puzzle keeps `bestSeconds = null` and a puzzle that was solved legitimately before (then Retried) keeps its earlier best time. The aid never lowers, raises or sets a best time (REQ-030: the aid sets none; the earlier best is kept).
- **The v1 format is unchanged. Confirmed, checked against the code and the fixtures:**
  - `PuzzleProgress.bestSeconds` is `Long?` and its KDoc already says "`null` when there is none (never solved, or solved only by the DEV aid, REQ-046)".
  - The writer emits `"bestSeconds": null` explicitly (`JsonProgressStore` line 189) and the reader accepts a Solved entry with a null best (`decodeEntry`: `best = if (o["bestSeconds"] == JsonNull) null …`, no state/best coupling).
  - Test evidence already in the tree: `ScaffoldJsonProgressStoreTest` reads `{"state":"solved","pieces":{},"puzzleSeconds":1,"bestSeconds":null}` as Solved.
  - The aid-solve is not recorded as "by aid" anywhere, so **no new field, no new value, no version bump**, and `FrozenV1FixtureTest` and the two frozen fixtures are neither touched nor re-pinned. (CR-2 said yes to "a null best time"; this is confirmed.)
- Consequence, stated once: after WO-008, a Solved entry with a null best time is "solved by the aid or solved before time existed". Nothing in the REQs needs the two told apart.
- **A hard-stop would arise only if** a later WO wants to *remember* that a solve was by aid (for example to show a mark). That is a v2 plus migration plus a test loading the frozen v1 files (LOCK-V1). Not needed by REQ-046.
- A test guards the claim end to end (section 9, A3: the app JVM chain asserts the parsed JSON tree of the written file for all 13 puzzles).

### 6. V-04, the release-APK scanner (rev 1: F2, F3, N1; `.swdev/verifiers/v04_release_apk.py`, stdlib only)

**How the passcode is stored, so a scan can find it in debug and prove it absent in release.**

- It is a single `const val CODE = "0417"` in `devtools`. Kotlin inlines the constant at its use site, so the debug dex string table holds the **standalone string `0417`**, and nowhere else in the project does the literal exist (main code and resources never write it; the source-separation test enforces that).
- The scan matches a dex string that is **exactly** `0417` (whole-string equality). Substring matching is rejected: a library constant such as `"…10417…"` or `"0.04170"` would fail the release build for no reason. The trade-off is stated: a passcode concatenated into a longer string would evade the equality test. The `devtools` rule is "the code is one standalone constant", and the **positive control** (below) proves the marker is found in a real, freshly built debug APK.
- The passcode is not a secret (the owner's REQ names it). The guarantee is *absence from release*, not confidentiality in debug.
- **Reading of G-04 (rev 1, N1; DA-85).** G-04 says V-04 scans "dex files and resources for … the passcode literal". Resources are **not** searched for the digits `0417`: the real release and debug APKs both contain the bytes `0417` inside androidx's own vector drawables (`ic_call_decline*.xml`, `res/Xk.xml` / `res/x4.xml` in release), so a literal reading would fail every release build. In resources the aid is covered by the `devtools_` key, the package forms and the English notice instead. Logged as a G-04 reading (governance row 13, `ai+inform`) and put on the owner list.

**Modes and exit codes.** `python v04_release_apk.py [--apk PATH] [--positive-control] [--project-root DIR] [--no-puzzles]`. Exit **0** pass, **1** findings, **2** could not build or read. **Every** exception from the zip, the dex parser, the build or the folded check is caught and mapped (a traceback never decides the exit code): an unreadable zip, a missing APK, a build failure, or an exception out of `check_apk_puzzles.check` (`zipfile.BadZipFile`, `OSError`) is exit 2; a dex that cannot be parsed is a finding (exit 1).

1. **Default (release) mode.** Without `--apk` it builds: it deletes `app/build/outputs/apk/release/`, runs `.\gradlew.bat :app:assembleRelease --console=plain` (cmd.exe on Windows like V-01; timeout 1200 s; sets `JAVA_HOME` and `ANDROID_HOME` to the AGENTS.md defaults when unset) and takes the newest `*.apk` there (today `app-release-unsigned.apk`; no signingConfig). A missing APK is exit 2, never a pass. With `--apk` it scans that file (self-test, ad-hoc).
2. **`--positive-control` mode (rev 1, F2c).** It deletes `app/build/outputs/apk/debug/`, runs `:app:assembleDebug` itself, scans the **fresh** debug APK with the same scanner functions and **requires every marker kind to be found**: (a) a dex string containing the devtools package in slash form, (b) a dex string equal to `0417`, (c) the `devtools_` key in `resources.arsc`, (d) the text `Wrong passcode.` in `resources.arsc`. A missing kind is exit 1 ("scanner cannot see <kind>"). Run at Test & Verify and at close; the close evidence line names it. It is a mode, not part of the always-run self-test, so D-5 stays independent of D-1…D-4 and a stale pre-devtools debug APK cannot make the self-test fail.
3. **Dex scan.**
   - Every entry matching `^classes\d*\.dex$` is parsed with `zipfile` and `struct` only. **Accepted versions: magic `dex\n03[5-9]\0` only** (the real APKs are `038`; a future container format has more than one header and would be read as one table, a silent pass). Any other version, a header size other than 0x70, an out-of-range table or an offset past the file end is an "unparseable dex" **finding**, and that file's raw bytes are also searched for the markers.
   - The header's `string_ids_size` is the u32 at 0x38 and `string_ids_off` the u32 at 0x3C. Each id is a u32 offset to a `string_data_item` (ULEB128 UTF-16 length, then MUTF-8 bytes to NUL), decoded as UTF-8 with replacement. This reads the string tables **and** the class descriptors, because `Lio/github/jamisuni/tangram/devtools/DevToolsState;` *is* a string-table entry.
   - Findings, per dex string: contains `io/github/jamisuni/tangram/devtools` or `io.github.jamisuni.tangram.devtools`; or equals `0417`.
   - **Fail closed:** no dex at all is a finding (a scan of nothing proves nothing).
4. **Non-dex scan (rev 1, F3): a deny-list, not an include-list.** Every zip entry **except** `classes*.dex` (parsed above), the image and audio types `.png .webp .jpg .jpeg .gif .ogg` and `tangrams/` (the folded check owns it byte for byte) is searched as raw bytes, ASCII and UTF-16LE, for: the two package forms, `devtools_` and `Wrong passcode.`. That covers `resources.arsc`, `AndroidManifest.xml`, `res/**`, `assets/**`, `META-INF/**`, `kotlin/**`, `lib/**` and root-level Java resources (a leaked library's resources land there). Checked against today's release APK by the reviewer: no hit and no false positive. `0417` is not searched here (reading above).
5. **Release canary: the scanner must not be blind (rev 1, F2b).** On the release scan, through the **same** code paths, V-04 must also *find*: a dex string containing `Lio/github/jamisuni/tangram/play/`, the exact descriptor `Lio/github/jamisuni/tangram/MainActivity;`, the key `app_name` (referenced from the manifest, so even a resource shrinker keeps it; rev 2, E4) and the text `Restart` in `resources.arsc`. The canary list is one constant in the verifier. A missing canary is exit 1 with `V-04 FAIL scanner blind <what>; if a rename or R8 was intended, update CANARIES in v04_release_apk.py`. This proves visibility on the artifact that ships, and catches R8 renaming, key collapsing and packaging changes the day they happen (release resources already differ from debug: AGP shortens resource paths, `res/Xk.xml`, even without minify; N2).
6. **Folded `check_apk_puzzles`.** V-04 **imports** it (`sys.path` gets `<root>/tools`; `check_apk_puzzles.check(apk, <root>/Tangrams)`) and reports each returned difference as a V-04 finding (exit 1); an exception out of it is exit 2 (above). One release gate, one source of truth for the puzzle comparison; the standalone script stays runnable, so WO-002/003 usage and tests are unchanged. Merging or a subprocess is rejected (duplicate code; a second process for no gain). `--no-puzzles` skips it for fixture APKs.
7. **Output.** `V-04 PASS: <apk>` or one `V-04 FAIL <kind> <entry> <detail>` line per finding.

**Self-test** (`TestV04ReleaseApk` in `.swdev/verifiers/test_verifiers.py`; the verifier is only trusted because it can fail). Fixtures are built at test time into a temp dir (a small `make_dex(strings, version="038")` that writes a header with `string_ids_*` plus string data, and `make_apk(...)` over `zipfile`), so no binary fixture is checked in. Crafted fixtures only; no real APK. **(rev 2, E1) Every detector fixture below is `B` plus exactly ONE change.** Each asserts the expected `V-04 FAIL <kind>` line **and** that no `scanner blind` line appears (a minimal fixture without the canaries would exit 1 as "blind" whatever it contained, and every detector row would pass even if detection were broken). Rows that expect exit 0 are `B` plus the one change and assert `PASS`:

| Case | Expect |
|---|---|
| clean release-like zip: dex with `Lio/github/jamisuni/tangram/MainActivity;`, `Lio/github/jamisuni/tangram/play/PlayArea;`, `DebugAids`, a near miss `…/devtool/…`; arsc with `app_name` and `Restart`: **the canary-complete base `B`** | exit 0 |
| dex containing a `…tangram/devtools/…` descriptor (slash form); the dotted form; in a **second** dex `classes2.dex` | 1 each |
| dex containing the exact string `0417` | 1 |
| dex with `10417` and `0.04170` | 0 (pins the whole-string rule) |
| arsc containing `Wrong passcode.` in UTF-8; in UTF-16LE | 1 each |
| `res/…xml`, arsc, **`META-INF/…`**, **a root-level file**, **`kotlin/…`**, **`lib/…`** containing `devtools_` or a package form | 1 each (the deny-list sees them) |
| a `.png` entry whose bytes contain `devtools_` | 0 (pins the exclusion) |
| arsc containing the bytes `0417` | 0 (pins the G-04 reading, DA-85) |
| **canary-incomplete** (rev 2, E1): `B` minus one canary at a time (no `…/tangram/play/` string; no `MainActivity` descriptor; no `app_name` key; no `Restart` text) | exactly one `scanner blind <that canary>` line, nothing else |
| no dex in the zip; a dex with a bad magic; **a dex of version `040`**; a header size not 0x70; a table offset beyond the file | 1 each (fail closed) |
| puzzles: matching `tangrams/` entries vs a temp `Tangrams/` | 0; with one missing or changed: 1 (the folded check really runs) |
| exit mapping: `--apk` that does not exist; a file that is not a zip; the folded check monkeypatched to raise `OSError` | exit 2 each, no traceback |
| the `--positive-control` evaluation function on a fixture with all four kinds | passes; with any one kind removed: fails naming that kind |

At Test & Verify the orchestrator also runs `--positive-control` for real (it builds the debug APK itself; exit 0 means every marker kind was found), so "V-04 can fail" is shown on the real thing, not only on crafted zips.

### 7. Release safety review (rev 1: N2, N7; G-04, the leak paths)

| Path | Why it cannot leak | Where it is proved |
|---|---|---|
| a `main`-scope reference to a devtools class, string or resource | `main` only knows `DebugAids`, `BoardSpace`, `PlaySession.solveByAid`, `onSolved`; a stray import would not compile in release (no devtools on the classpath) | `assembleRelease` (V-04 builds it); source-separation test |
| release source set | the stub imports nothing from devtools | source-separation test; V-04 |
| resources merged from the debug library | `debugImplementation` resources merge into the debug variant only; `app/src/main/res` never names a `devtools_` key | V-04 non-dex deny-list scan (every entry but dex and images) |
| manifest merge | `devtools` has no manifest component and no permission; V-01 already scans the release manifest | V-01 |
| R8 / minification | **Release is not minified today**: `app/build.gradle.kts` has no `buildTypes` block, so `isMinifyEnabled` is false. Class names and the arsc key pool are intact (the package marker and the `devtools_` key marker work); resource **paths** are already shortened by AGP without minify (`res/Xk.xml`), which is why V-04 carries a release-side canary (section 6, item 5). **If WO-009 turns R8 on (rev 2, E4), V-04 goes "scanner blind" until its canary list is adapted** (class descriptors are renamed; the canary message names `CANARIES` as the place to update). A leaked class would be renamed too, so the package marker would miss it, but the `0417` string and the resource text/key markers would still catch it, and the leak would also need a non-stripped reference | carried to WO-009 |
| neutral hooks in release code (`solveByAid`, `onSolved`) | present in the release dex by design (G-04 allows them), unreachable from any release UI, and gated by engine validation | section 2 |
| the instrumented-test APK | contains devtools, never shipped | n/a |
| devtools **logic re-implemented under another package** in `play`/`app` release code (reading `puzzle.solution` into poses elsewhere) | no `devtools` word or `0417` would appear, so neither V-04 nor the separation test can see it; **review only** (G-04 names it). The brief for the mandatory D-1 + D-4 code-review checkpoint says: *no release code derives placements from `puzzle.solution` except `devtools`, via `PieceGeometry.poseOf`* | code review |

### 8. What `app` changes (all wiring; no new REQ logic)

- `AppViewModel`: `val aids = DebugAids()` (a ViewModel field, so the unlock and the overlay flag survive rotation and browsing, DA-76).
- `TangramApp(controller, host, aids)`: `cornerControl` stays Restart alone; the new `secondaryCornerControl` (the DEV pill, with `blocked = { session.isDragging }`) and the `boardOverlay` lambda of section 3b. `MainActivity` passes `model.aids`.
- `app/build.gradle.kts`: `debugImplementation(project(":devtools"))`.
- `app/src/debug/…/DebugAids.kt` and `app/src/release/…/DebugAids.kt` (section 1).
- No manifest change, no string in `app`, no change to `SessionHost`.

### 9. Verification channels

JVM unit tests (`kernel`, `play`, `devtools`, `app/src/test`, `store` untouched) and Compose device tests on `Medium_Phone_API_37.0` for `devtools` and `app`; the API 26 release launch stays waived (DA-43). Test & Verify runs `assembleDebug test connectedDebugAndroidTest`, V-01, V-04 (builds release itself), `V-04 --positive-control` (builds debug itself), V-05, V-06, V-07, trace-check, and the existing app/play device suites (the DEV pill is now in every debug app screen; see Risks).

## Alternatives considered

| Fork | Chosen | Not chosen, and why |
|---|---|---|
| **How `app` discovers the aid** | The source-set pair `DebugAids` (debug real, release no-op) | (a) `ServiceLoader` over a neutral interface: needs an interface that both `app` and `devtools` can see, and the only candidates are `contracts` (governed, locked/notify) or `kernel` (value types, not UI); adds reflection and a `META-INF/services` resource; a release miss is silent. (b) A debug-only manifest component or `androidx.startup` initializer: new manifest surface for a testing aid, timing and G-01 review for nothing. (c) `Class.forName("…devtools…")` behind `BuildConfig.DEBUG`: puts the devtools name in `main` and the release dex, which G-04 forbids. (d) A product flavour: ADR-006 already rejected it (D2). (e) A `testRelease` proof: DA-56 removed that variant. |
| **Where the DEV button sits (rev 1, F1)** | A second slot `secondaryCornerControl`, placed after the layout is fixed by the pure `placeSecondary` (bottom-left first, then BR, TR, TL, then beside Restart's strip, else not shown) | (a) The same slot as Restart in one `Row` (draft 1): ruled out by the reviewer's port: the wider slot reserves a strip for `animals-cat` on 360 × 640 and shrinks the debug board 12 %, so the owner would test a layout that does not ship; its `Column` fallback fails on four puzzles. (b) A fixed bottom-left float independent of the layout: collides with Restart when Restart takes that corner (DA-71) and with the silhouette. (c) The top bar: not "on the board" (REQ-046). |
| **Where the stored solution becomes poses** | `devtools` (`DevSolution.poses`), through the kernel's `poseOf` | `play` computing it from `Puzzle` itself: simpler call, but release code would then read stored solutions to place pieces, which G-04 says must live only in `devtools`. |
| **Shape of the solve hook** | `solveByAid(poses): Boolean`, validated by the lock engine | (a) `solveByAid()` with no argument: a release method that marks any puzzle solved when called, with no way to tell from a legitimate solve. (b) Setting the state directly (`state = SOLVED`): breaks O-07 and skips REQ-022. (c) Replaying the poses as synthetic drops: runs the whole drag machine (frames, glides) for a testing aid. |
| **What the aid-solve looks like (rev 1, F5)** | The normal REQ-023 timeline (pieces placed, pop, fade, confetti, reduced motion respected), started from the **next frame's** clock through a pending solve; only the best time is withheld | The settled picture at once with no celebration (draft 1, DA-66's restore path): a reading of locked REQ-023 that the owner did not make; DA-66 covers a restore, not a solve. A coroutine hop through `app` for the frame clock: more moving parts than a pending flag resolved in `onFrame`. |
| **A4's covering test** | A source-level separation test under `devtools` (carries the token; its read files are declared Gradle task inputs, rev 1, F4) plus V-04 on the real APK (a verifier, not a token) | (a) A JVM test that reads a release APK: needs `assembleRelease` before `test`, so it either skips silently (fake coverage) or makes `test` slow. (b) Re-implementing V-04's dex scan in Kotlin: a second source of truth (D1). (c) A device test: it can only run on debug. |
| **Folding `check_apk_puzzles`** | V-04 imports and calls it | Merging it into V-04 (duplicate code, and the WO-002/003 usage of the standalone script would break) or running it as a subprocess (a second process for no gain). |
| **Unlock / overlay state** | `DevToolsState` in a `DebugAids` held by `AppViewModel` | `SharedPreferences` or the store: persistence contradicts "until the app is restarted" and would touch the frozen format. A process-wide `object`: leaks between tests and survives activity finish. |
| **Passcode matching in V-04** | Whole dex string equals `0417`; not searched in resources | Any substring: false positives from library constants would block releases (see the self-test case). Raw byte search of the dex: matches inside binary tables. Searching resources for the digits: androidx's own `ic_call_decline*.xml` drawables contain `0417` in both real APKs (DA-85). |
| **V-04's proof that it can see (rev 1, F2)** | A release-side canary (known app markers must be found) plus a self-building `--positive-control` mode that needs all four marker kinds on a fresh debug APK | The positive control as an always-run self-test case on whatever debug APK exists: half the markers unproven, runs against a stale pre-devtools APK, and ties D-5 to D-1…D-4. A dex-only control: leaves the resource scan unproven on a real artifact. |

## Test seams *(frozen here; each seam names the task that delivers it; rev 1: F1, F4, F5, F6, N3, N5)*

Task names are this design's suggested cut (D-0 … D-7); the Planner maps them to `TASK-0nn` and writes any change back into this design and `decisions.md` before the test author starts (AGENTS lesson, WO-003). Test fixtures use literal coordinates and the real puzzles through test-scope `content`. **Test adapters fail loudly (`error(…)`), never `?.invoke`.** Tests in the same module call `internal` members directly. A test that pins a decision carries `// decision DA-n` and no `REQ-NNN.An` token.

| Seam (exact signature, exact nullability) | Visible to | Delivered by |
|---|---|---|
| `class BoardSpace(val dpPerUnit: Double, val originXDp: Double, val originYDp: Double)` with `fun toDp(point: Vec2): Vec2` (`kernel.geometry.Vec2`, non-null in and out); equals `PlayLayout.toDp` for the layout it came from | `play/src/test`, `app` | D-1 |
| **(rev 1, F1)** `@Composable fun PlayArea(session: PlaySession, layoutClass: LayoutClass, trayRows: List<List<PieceId>>, screenHeight: Dp, modifier: Modifier = Modifier, solvedBar: (@Composable BoxScope.() -> Unit)? = null, cornerControl: (@Composable BoxScope.() -> Unit)? = null, boardOverlay: (@Composable BoxScope.(BoardSpace) -> Unit)? = null, secondaryCornerControl: (@Composable BoxScope.() -> Unit)? = null)`. `boardOverlay` is composed only while `session.state != SOLVED`, above the canvas, below the two corner slots and `solvedBar`, and adds no pointer input. `secondaryCornerControl` is laid out **after** the board and the primary slot and never changes them | `play/src/androidTest`, `app` | D-1 |
| **(rev 1, F1)** `internal fun PlayLayout.Companion.placeSecondary(layout: PlayLayout, primary: ControlPlacement?, widthDp: Double, heightDp: Double): RectDp?` (pure, no Compose; order and legality exactly as section 3b; `null` = no legal place; `primary` null = no primary slot; never reads or changes anything but its arguments) | `play/src/test` | D-1 |
| `PlaySession(puzzle: Puzzle, reducedMotion: () -> Boolean = { false }, resolver: DropResolver = DropResolver(puzzle), onChanged: () -> Unit = {}, onSolved: (byAid: Boolean) -> Unit = {})`. **`onSolved` is the last parameter**, so every existing call compiles | `play/src/test`, `play/src/androidTest`, `app` | D-1 |
| **(rev 1, F5, N4)** `fun PlaySession.solveByAid(poses: List<PlacedPiece>): Boolean` (public member). `false` = nothing changed, no event, no `onChanged`. `true` = pieces on the board at the poses (`placed` equals the poses), `state == SOLVED`, a **pending** solve that the next `onFrame(nowMs)` turns into `solved = SolvedAt(nowMs, reducedMotion())` (so the REQ-023 timeline runs from that frame), `onSolved(true)` once, then `onChanged()` once; exact conditions in section 2b. `internal val solvePending: Boolean` (true from the call until that frame; read by `PlayArea`'s "needs frames" test) | `play/src/test`, `devtools` via `app`'s lambda | D-1 |
| `onSolved(false)` fires exactly once per drop that solves; never from `restore`, `toProgress`, `interruptDrag` | `play/src/test` | D-1 |
| `devtools`: `class DevToolsState()` with `val unlocked: Boolean`, `val overlayOn: Boolean`, `val dialogOpen: Boolean`, `val notice: DevNotice` (all observable, `private set`), `fun open()`, `fun close()`, `fun submit(code: String): Boolean` (true iff `code == "0417"`; a wrong code sets `notice = WRONG_PASSCODE` and nothing else; a right code sets `unlocked = true`, `notice = NONE`), `fun toggleOverlay()`, `fun solveFailed()` (sets `notice = SOLVE_FAILED`); `enum class DevNotice { NONE, WRONG_PASSCODE, SOLVE_FAILED }` | `devtools/src/test`, `app` debug | D-2 |
| `devtools`: `data class SolutionShape(val piece: PieceId, val colour: Int, val polygon: List<Vec2>, val label: Vec2)`; `object DevSolution { fun shapes(puzzle: Puzzle): List<SolutionShape> /* one per puzzle.solution entry, in solution order, never null */; fun poses(puzzle: Puzzle): List<PlacedPiece>? /* one per entry, null iff some piece has no pose */ }` | `devtools/src/test` | D-2 |
| **(rev 1, F6)** `devtools`: `@Composable fun DevCornerButton(state: DevToolsState, puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean, blocked: () -> Boolean, modifier: Modifier = Modifier)` (a click while `blocked()` is true is a no-op: no dialog, no state change; touch box exactly 56 × 40 dp, label not scaled by the font scale); `@Composable fun DevSolutionOverlay(state: DevToolsState, puzzle: Puzzle, toDp: (Vec2) -> Vec2, modifier: Modifier = Modifier)`; tags `dev-button`, `dev-dialog`, `dev-passcode`, `dev-ok`, `dev-cancel`, `dev-notice` (its text is the notice string; absent when `NONE`), `dev-show-solution` (text toggles Show/Hide), `dev-solve-now`, `dev-done`, `dev-solution-overlay`, `dev-solution-<PieceId.name>` (one per piece, bounds = polygon bounding box in overlay coordinates; none while `overlayOn` is false) | `devtools/src/androidTest`, `app/src/androidTest` | D-3 |
| `devtools` string keys of section 4 (en + fi) | V-05 | D-3 |
| **(rev 1, F6)** `app`: `class DebugAids { @Composable fun CornerButton(puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean, blocked: () -> Boolean, modifier: Modifier = Modifier); @Composable fun BoardOverlay(puzzle: Puzzle, space: BoardSpace) }`, identical public signatures in `app/src/debug` and `app/src/release`; `AppViewModel.aids: DebugAids`; `TangramApp(controller: BrowseController, host: SessionHost, aids: DebugAids)` | `app/src/test` (debug only), `app/src/androidTest`, `MainActivity` | D-4 |
| **(rev 1, F2, F3)** `.swdev/verifiers/v04_release_apk.py`: `scan_apk(apk_path: str) -> list[str]` (findings; empty = clean; includes the release canary, so a blind scan is a finding), `positive_control(apk_path: str) -> list[str]` (the marker kinds **not** found; empty = all four found), `main(argv) -> int` (0 / 1 / 2), CLI and modes as section 6; self-test class `TestV04ReleaseApk` | `.swdev/verifiers/test_verifiers.py`, the orchestrator | D-5 |
| **(rev 1, F4)** `settings.gradle.kts` `include(":devtools")`; `devtools/build.gradle.kts` (module build, plus the test-input declaration below); `app/build.gradle.kts` `debugImplementation(project(":devtools"))` | every task | D-0 |

**Test inputs of the A4 test (rev 1, F4; the WO-001 §10 / kernel / content precedent).** `devtools/build.gradle.kts` declares, on its unit-test task, every file `ReleaseSeparationTest` reads as an input with relative path sensitivity: `tasks.withType<Test>().configureEach { inputs.files(fileTree(rootDir) { include(…) }).withPathSensitivity(PathSensitivity.RELATIVE); systemProperty("repo.root", rootDir.absolutePath) }`, the include set **identical to the test's file set `S`** *(plan v1.5, DA-89: the test exempts exactly `include(":devtools")` in `settings.gradle.kts` and `debugImplementation(project(":devtools"))` in `app/build.gradle.kts` — nothing else in S may name devtools or hold `0417`; the test is authored by the independent test author (TASK-T5), and its cache re-run proof runs at MOVE-JVM, where the test exists)* (rev 2, E2: `<module>/src/main/**` for `app`, `play`, `browse`, `kernel`, `contracts`, `content`, `store`; `app/src/release/**`; `app/build.gradle.kts`; `settings.gradle.kts`; never `build/`, `.gradle/` or `.swdev/`), one definition shared by the test and the build file. Without this `org.gradle.caching=true` could replay a cached pass for code it never read. The re-run proof runs at **MOVE-JVM** (plan v1.5; D-0 cannot run it, the test is staged until then): edit one scanned file (add the text `devtools` to a comment in `play`), run `:devtools:testDebugUnitTest`; it must **re-run and fail**; revert.

**Value shapes, stated exactly.**
- `DevSolution.poses(puzzle)` has exactly one `PlacedPiece` per `puzzle.solution` entry, in solution order, and each pose's `corners` equal that entry's polygon as an exact point set. It is `null` only if `PieceGeometry.poseOf` returns `null` for some entry. It never throws.
- `DevSolution.shapes(puzzle)` has `puzzle.solution.size` items; `polygon` is the stored polygon converted to `Double` units; `colour == TrayRules.colour(piece)`; `label` is the vertex average.
- After `solveByAid` returns `true` and one frame has run: `state == SOLVED`, `solved != null` with `t0` equal to that frame's clock, `solvePending == false`; after the picture is fully faded the pieces are not drawn (the unchanged `SolvedTimeline`). A later `toProgress` reports the pieces on the board and `persist` stores `pieces = {}` (DA-48).
- What the **store** holds after an aid solve through `BrowseController.persist()`: `PuzzleProgress(SOLVED, emptyMap(), puzzleSeconds = <unchanged>, bestSeconds = <unchanged>)`. A fresh install gives `(SOLVED, {}, 0, null)`.
- `placeSecondary` result: a rect of exactly `widthDp × heightDp`, inside the area, clear of the silhouette and the primary rect by 4 dp, above `layout.trayTop`; or `null`.

### Acceptance IDs and where the covering test lives *(rule 3: the code home `devtools` always; `app` adds cross-slice tokens, never replaces them)*

| ID | Covering test in `devtools` | Added cross-slice test (`app`, with the `play` pieces named) |
|---|---|---|
| REQ-046 A1 | `devtools/src/test` `DevToolsStateTest`: `submit("1234")` is false, `notice == WRONG_PASSCODE`, `unlocked == false`, `overlayOn == false`; `submit("")` and `submit("04170")` likewise; `submit("0417")` is true and `unlocked`. `devtools/src/androidTest` `DevAidUiTest`: tap `dev-button`, type 1234, OK: `dev-notice` text equals the resource `devtools_wrong_passcode`, and `dev-show-solution` and `dev-solve-now` do not exist; type 0417, OK: both exist. **(rev 1, F6)** with `blocked = { true }` a tap on `dev-button` opens no `dev-dialog` (`// decision F5`, no token) | `app/src/androidTest`: the same through the real screen; then ‹ ›, and `ActivityScenario.recreate()`, leave the aid unlocked (REQ-046 rule 2; a new process is not testable and is covered by the "state is a ViewModel field" `// decision DA-76` check). **(rev 1, F6)** a second finger on the DEV pill during a drag opens nothing (`// decision F5`) |
| REQ-046 A2 | `devtools/src/test` `DevSolutionTest`: for **every packaged puzzle** (test-scope `content`) `shapes()` has one item per stored piece with polygon, colour and id as stored. `devtools/src/androidTest`: overlay on gives exactly one `dev-solution-<id>` node per piece and none when off | `app/src/androidTest`: unlock, show the solution: one node per piece of the shown puzzle; › to another puzzle: the overlay is still on with that puzzle's own count (rule 3); switch it off: none; a solved puzzle shows none (`// decision DA-82`). A pixel probe inside each piece, away from outline and label (the DA-40 rule, mapped through `BoardTransform`), differs from the bare silhouette colour |
| REQ-046 A3 **(rev 1, N3, F5)** | *Engine-level reading (architecture §5 item 4, like REQ-045.A2):* the poses `devtools` hands over solve the puzzle through the kernel, and `devtools` touches no store or time. `devtools/src/test` `DevSolveNowTest`: for every packaged puzzle `poses()` is non-null, one per piece, corners equal the polygon, and the sequence passes `LockSearch.isValidPlacement` and `SolvedCheck.isSolved` (the engine accepts the stored solution). `devtools/src/androidTest`: "Solve this puzzle now" calls a fake `solveNow` exactly once with those poses and closes the dialog; a fake that returns `false` leaves the dialog open with `dev-notice` = `devtools_solve_failed` | `app/src/test` (JVM, debug): for **all 13 puzzles** run real `PlaySession` + `DevSolution.poses` + `solveByAid` + one `onFrame` + `BrowseController` + `JsonProgressStore(tempDir)`: `onSolved` got `true` once; the file's parsed JSON entry equals `{"state":"solved","pieces":{},"puzzleSeconds":0,"bestSeconds":null}`; a new store on the same dir reads Solved with a null best; seeded with `bestSeconds = 41` it stays 41; **the written document's other keys and its `version` are untouched (v1)**. `app/src/androidTest`: unlock, Solve now: after the REQ-023 fade (the test waits on the frame clock past 1.4 s, `waitUntil`, not a sleep) `puzzle-state` reads solved, the board region has non-silhouette picture pixels, `retry-button` exists, `best-time` text is the dash, `dev-button` does not exist (`// decision DA-75`), the overlay is hidden; ‹ then › returns to a solved puzzle. `play/src/test` `SolveByAidTest` (token on the event half): valid poses give `true`, SOLVED, `onSolved(true)` once, `onChanged` once, `placed` equals the poses, `solved == null` and `solvePending` until `onFrame(1000)`, then `solved.t0 == 1000`; a missing piece, a duplicate, an overlapping pair, a pose outside the silhouette, poses of another puzzle, and a call on an already SOLVED session each give `false` with no change and no event; a player solve gives `onSolved(false)`; `restore` gives none; reduced motion is the session's own (`SolvedAt.reducedMotion`) |
| REQ-046 A4 **(rev 1, F4)** | `devtools/src/test` `ReleaseSeparationTest` (source level, token): scans exactly the **file set `S`** (rev 2, E2): `<module>/src/main/**` for the seven modules `app`, `play`, `browse`, `kernel`, `contracts`, `content`, `store` (manifests and `res/` are inside it), `app/src/release/**`, `app/build.gradle.kts` and `settings.gradle.kts`; `build/`, `.gradle/` and `.swdev/` are never visited (no recursive search from the root: it would reach `app/build/intermediates/…/merged.dir/values/values.xml` and the merged manifests, which legitimately carry `devtools_` keys in a debug build). Case-insensitive text `devtools` and `0417` in `S`: none; asserts `app/build.gradle.kts` mentions `:devtools` only inside `debugImplementation(…)`; asserts the two `DebugAids` files declare the same public member signatures. It finds the repo root from the `repo.root` system property (the task inputs above) and `error(…)`s if it is missing (no silent pass). **The files it reads are declared task inputs**, so a cached pass cannot be replayed | **V-04 on the real release APK**, run at Test & Verify and at close (a verifier, no token); plus `V-04 --positive-control`. A4's artifact half is V-04; trace-check cannot see it, so the close lists the V-04 runs |

**Scaffolding tests with no REQ token:**
- `play`:
  - `BoardSpace.toDp` equals `PlayLayout.toDp` for several puzzles and sizes;
  - **(rev 1, F1) the `placeSecondary` sweep, `// decision DA-75`:** all 13 packaged puzzles × the areas 360 × 600, 360 × 620, 360 × 640, 360 × 780, 390 × 700, 390 × 844, 800 × 1100, 800 × 1180, 1280 × 700 (phone or tablet class as the existing sweep) × Restart widths 82, 118, 170, 230 dp (EN, FI, two large-font stand-ins), DEV rect 56 × 40: the result is never `null`, never intersects the silhouette (independent separating-axis check, as `CornerControlSweepScaffoldingTest`) with 4 dp clearance, never intersects the primary rect, stays above `trayTop` and inside the area; the chosen corner per puzzle and size is printed (the section 3b table);
  - **(rev 1, F1) board parity:** `PlayLayout.computeWithCorner` output (board rect, scale, origin) is the same value whether or not a secondary slot exists (it never receives one); and a device test composes `PlayArea` with and without `secondaryCornerControl` and asserts the `BoardTransform` semantics (scale, origin) are equal for several puzzles;
  - a slot-order and no-touch check (a drag from the tray locks a piece with the overlay slot present and drawing; `// decision DA-74`);
  - the overlay slot is not composed on a SOLVED session;
  - `solvePending` keeps the frame loop alive on an idle board (the device test waits for the REQ-023 fade after `solveByAid` with no touch).
- `devtools`:
  - `DevToolsState.toggleOverlay`, `open`/`close`, `solveFailed` transitions;
  - the passcode field masks and limits to 4 characters;
  - the DEV pill's touch box is 56 × 40 dp at font scale 1.0 and 2.0;
  - the string keys exist in both languages (V-05 owns parity).
- `app` **(rev 1, N5)**: `DebugAids` release/debug signature equality is covered by the A4 separation test; the placement of the DEV pill against the silhouette and Restart over many sizes is the `play` JVM sweep above (a device test through `MainActivity` runs at one emulator size, so it does not sweep); the `app` device test asserts only, at the emulator's own size, that `dev-button` and `restart-button` bounds do not intersect and `dev-button` does not intersect any silhouette polygon of the shown puzzle (`// decision DA-75`).

## Risks & edge notes *(rev 1)*

- **Highest risk, for the Design Reviewer: V-04's blind spots, i.e. a gate that passes because it cannot see.** The scanner is the only artifact-level proof of A4. Rev 1 answers the review: a release-side canary, a deny-list over every non-dex entry, strict dex versions (`035`–`039`), total exit-code mapping, and a self-building `--positive-control` mode that needs all four marker kinds on a fresh debug APK. Attack what is left: (1) hand-parsing the dex string table on real multi-dex APKs (the reviewer found 2 dex in release, 13 in debug, all `038`; the positive control must actually be run at Test & Verify and at close, it is not skippable evidence); (2) the whole-string rule for `0417` and the decision not to search resources for the digits (DA-85); (3) the canary strings (`Lio/github/jamisuni/tangram/play/`, `MainActivity`, `app_name`, `Restart`) must stay true of the app: renaming a package or a string makes the canary fail loudly, which is the intent and a one-line edit; (4) R8, off today (section 7).
- **Second: the new `secondaryCornerControl` slot and `placeSecondary` in `play` (section 3b).** It replaces the shared slot that failed the parity gate. Board parity holds by construction (the layout is computed before and without it); what is left to attack is the placement rule (order bottom-left, BR, TR, TL, beside Restart's strip, not shown), the sweep that pins "never not shown", and that the pill's fixed 56 × 40 dp size does not scale with the font. A debug-only pill in a free corner also means debug and release differ only by that pill.
- **Third: the validated `solveByAid` with a pending solve (section 2b).** It now runs the real REQ-023 timeline from the next frame's clock. Check the interplay with `animating(nowMs)` (it must treat `solvePending` as alive or an idle board never advances), with DA-52 (the tray is gone at the solve moment, the pieces are drawn on the board until the fade hides them), and that `persist` still stores no pieces (DA-48).
- **Devtools leaking into release through `main`-scope references.** The `main` code names only `DebugAids` and neutral play types. The separation test greps the sources (comments included, so authors write "debug aid"), now with its inputs declared so the result cannot be replayed from the Gradle cache; V-04 greps the artifact; and any real import of a devtools class in `main` fails `assembleRelease` outright. Note `contracts/…/SavedGame.kt` has the comment "DEV aid" (not matched: the scan looks for `devtools`). The one path none of them sees, devtools logic re-implemented under another package, is a review check (section 7).
- **Resources merged from debug libraries.** Merged into the debug variant only. `app/src/main/res` must never reference a `devtools_` key (the separation test checks the res tree; V-04 checks every non-dex entry). `devtools` strings are not translatable-false; V-05 requires en/fi parity, which the section 4 table gives.
- **R8 / minification.** Not enabled (no `buildTypes` block). Class names and the arsc key pool are intact; resource paths are already shortened by AGP (N2). See section 7 for the WO-009 carry.
- **The overlay vs gestures.** The slot sits above the gesture box. A draw-only `Canvas` and semantics-only nodes take no touch, so drags, tap-turns and the long press reach `PlayArea`'s adapter. Pinned by a device test (drag a tray piece onto the board with the overlay on; it locks). If a future overlay needs touch, it must go beside, not over, the board. The overlay also dims dragged and placed pieces beneath it (alpha .55): accepted for a testing aid.
- **The DEV pill and the drag guard (rev 1, F6).** `blocked = { session.isDragging }` makes a touch on the pill during a drag a no-op (decisions F5, O-09); pinned by a `devtools` and an `app` test (`// decision F5`).
- **The frozen format.** Unchanged and cross-checked (section 5). Anything that wants to remember "by aid" is a v2 plus migration (hard-stop 2); REQ-046 does not need it. The app JVM test asserts the exact parsed document so a drift fails there.
- **The aid stays unlocked "until the app is restarted".** `DevToolsState` lives in a ViewModel field: rotation keeps it, finishing the activity (Back out) and reopening the app in the same process makes a new ViewModel and locks again. That is a restart in the user's sense. A process-wide object would survive Back-out and leak between tests.
- **Existing device tests now see the DEV pill.** Every debug `app` screen has it, in a free corner with 4 dp clearance from the silhouette and from Restart. The board and Restart are untouched (F1), so silhouette probes and the Restart position are as before; a rare DEV corner is bottom-right or top-right. The existing suites are re-run at Test & Verify; a failure is read against the lessons (DA-40, DA-70: map through `BoardTransform`, avoid corners) before the product is blamed. New nodes carry new tags, so tests that count controls are unaffected.
- **A hook that is present in release but unreachable.** `solveByAid`, `onSolved` and the `secondaryCornerControl` slot ship (G-04 allows neutral hooks). The review check: nothing in release code calls `solveByAid` except the lambda handed to the stub, which ignores it; the stub composes nothing, so the release secondary slot is empty.
- **WO-006 carry.** REQ-037 A1's control walk must exclude `dev-*` nodes in debug; REQ-047 tests should not scan the dev dialog or the pill text "DEV" (a symbol).
- **The aid on an In-progress puzzle with pieces on the board (rev 1).** `solveByAid` replaces the current pieces with the stored-solution poses (it ignores where the player put them), then runs the normal solve timeline. That matches the prototype's "all pieces to their places".
- **Empty or odd puzzles.** A puzzle whose stored solution has no pose for a piece (`poseOf` null) is a content defect, not a crash: the notice says so (G-10).

## Assumptions to log in `decisions.md` (agent proposals, class 12; continue at DA-72; rev 1: changed rows marked, new rows DA-85…DA-88)

| # | Assumption | Basis |
|---|---|---|
| DA-72 | **Discovery is the `DebugAids` source-set pair** (`app/src/debug` real, `app/src/release` no-op, identical public signatures, neutral name); the only build line is `debugImplementation(project(":devtools"))`; no `ServiceLoader`, reflection or manifest component; the `testRelease` variant stays deleted (DA-56) and release behaviour is proved by V-04 and the source-separation test | G-04, ADR-006, DA-23/DA-56 history, D2 |
| DA-73 **(changed, rev 1)** | **The neutral solve hook is `PlaySession.solveByAid(poses): Boolean`**: validated by the lock engine (`isValidPlacement` chain + exact piece set + `SolvedCheck` belt); on success it places the pieces, sets SOLVED through the TYPE-006 transitions and marks the solve **pending** so the **next frame's clock** starts the normal REQ-023 timeline; `onSolved: (byAid: Boolean) -> Unit` is the last `PlaySession` parameter, fired once for a drop solve (`false`) and an aid solve (`true`), never on restore. **Amends DA-27 for the solve event only** (needed now to check REQ-046 A3 at event level); `SessionHost` forwards it in WO-008 | G-04, O-06, O-07, REQ-046 A3, REQ-030 rule 4, REQ-023, review F5/N4 |
| DA-74 **(changed, rev 1)** | `PlayArea` gets a slot `boardOverlay: (@Composable BoxScope.(BoardSpace) -> Unit)?`, composed above the canvas and below the corner controls and solved bar, **only while the puzzle is not SOLVED**, taking no touches; `BoardSpace` is the public `toDp` mapping; **and a slot `secondaryCornerControl`** laid out after the board and the primary corner slot, never affecting them | O-09, REQ-046 rule 3, DA-52, review F1 |
| DA-75 **(rewritten, rev 1)** | **The DEV pill is a second slot, `secondaryCornerControl`, placed by the pure `PlayLayout.placeSecondary` after the board and Restart are fixed** (4 dp inset and clearance; bottom-left first = REQ-046's ASSUMPTION, then BR, TR, TL; else beside Restart's strip; else not shown (rev 2, E3: no compact step), which the sweep shows never happens for the 13 puzzles × 9 sizes × 4 Restart widths). Fixed size 56 × 40 dp, label not font-scaled. **Debug and release boards are identical by construction.** The only departure from the ASSUMPTION is where bottom-left is not free; per the orchestrator's ruling this is a logged DA and **needs no capture-side CHG**. Hidden on a solved puzzle by `unplacedUnless` | REQ-046 ASSUMPTION (AI-made), DA-71, REQ-037 exemption for DevTools controls, governance row 12, review F1 |
| DA-76 | The aid's state (`unlocked`, `overlayOn`, dialog, notice) is `DevToolsState` inside `DebugAids`, owned by `AppViewModel`: it survives rotation and browsing and ends with the activity/process ("until the app is restarted"); never persisted | REQ-046 rule 2, DA-26 pattern, v1 frozen |
| DA-77 | **An aid solve stores exactly what any solve stores** (Solved, no pieces, `puzzleSeconds` and `bestSeconds` untouched through the existing `persist`): **no v1 change**, nothing recorded as "by aid", an earlier best time is kept, a never-solved puzzle keeps a null best. After WO-008 a Solved entry with a null best means "by the aid or solved before time existed" | LOCK-V1, DA-46, DA-48, DA-65, REQ-030 rule 4, CR-2 |
| DA-78 **(changed, rev 1)** | **The passcode is one standalone string constant `"0417"` in `devtools`**; V-04 finds it as an exact dex string in a debug dex (required by `--positive-control`) and proves it absent in release; it is not a secret, not searched as a substring and not searched in resources (see DA-85) | G-04, REQ-046 |
| DA-79 **(changed, rev 1)** | **V-04** imports `tools/check_apk_puzzles.check` (one release gate, the standalone script stays); dex versions `035`–`039` only, anything else fails closed; a deny-list non-dex scan (every entry but dex and images, ASCII and UTF-16LE); deletes the old APK before building; every exception mapped to an exit code; self-test with crafted fixtures only | G-04, V-04 row, WO-003 N5, review F2/F3 |
| DA-80 **(changed, rev 1)** | `devtools` strings (en/fi) of section 4; **all Finnish is AI-written (only "Valmis" is prototype Finnish) and goes to the owner list, with the review's N6 wording**; `devtools_solve_failed` is added beyond the REQ's text so a stored solution the engine cannot place is visible (the REQ's purpose is to confirm solvability) | rule 13, F14, REQ-046 rationale, G-10, review N6 |
| DA-81 **(changed, rev 1)** | REQ-046 A4's token test is the source-level separation test under `devtools`, **with the files it reads declared as Gradle task inputs** (cache-safe, the WO-001 §10 precedent); its artifact proof is V-04 (no token; trace-check cannot see a verifier), run at Test & Verify and listed at close | trace-check rule 3, G-04, review F4 |
| DA-82 | The overlay follows the prototype (piece colour at alpha .55, white 2 dp dashed outline 6/4 dp, id 14 sp bold white at the centroid), is drawn above pieces, is **not drawn on a SOLVED puzzle** (the prototype hides it too) and comes back after Retry; the REQ is silent on the solved case | REQ-046 rule 3, prototype `drawHint` |
| DA-83 **(replaced, rev 1)** | **An aid solve runs the normal solved timeline** (pieces at their places, pop, 600–1400 ms picture fade, confetti 2.4 s, reduced motion respected), exactly as the prototype does and as REQ-023 says for a solve; **only the best time is withheld** (`byAid`, REQ-046 A3). The start time is the next frame's clock (a pending solve), never a stale one. The draft's "settled picture, no celebration" is withdrawn: it was a reading of locked REQ-023 that nobody made | REQ-023, REQ-046 A3, REQ-030 rule 4, orchestrator ruling on review F5 |
| DA-84 | New module `devtools` is an Android library: main scope `kernel` + `contracts` + Compose foundation only (G-06, V-06 already lists it); `Dialog` and `BasicTextField` from foundation, no material3 and no new library | ADR-002, ADR-006, G-02, D5 |
| DA-85 **(new)** | **G-04 reading:** V-04 does **not** search resources for the passcode digits `0417`; androidx's own `ic_call_decline*.xml` drawables contain those bytes in both the real release and debug APKs, so a literal reading of "scans … resources for the passcode literal" would fail every release build. The aid is covered in resources by the `devtools_` key, the package forms and the English notice. Governance row 13 (`ai+inform`); **on the owner list** | G-04, review N1 and its APK evidence |
| DA-86 **(new)** | **V-04 proves it can see:** a release-side canary (known app markers must be found on the release APK or the verdict is "scanner blind") and a self-building `--positive-control` mode (fresh `assembleDebug`, all four marker kinds required), run at Test & Verify and at close and named on the close evidence line; the always-run self-test uses crafted fixtures only | G-04, V-04 row, review F2 |
| DA-87 **(new)** | **The DEV pill obeys the drag guard** (decisions F5, O-09): `DebugAids.CornerButton` and `DevCornerButton` take `blocked: () -> Boolean`; `app` passes `{ session.isDragging }`; a click while blocked is a no-op | F5, O-09, WO-004 design review F8, review F6 |
| DA-88 **(new)** | The A4 `ReleaseSeparationTest` reads files outside its module, so `devtools/build.gradle.kts` declares them as test-task inputs (relative path sensitivity) and passes the repo root as a system property; the re-run proof (edit a scanned file, the test must re-run) runs at MOVE-JVM (plan v1.5) | `org.gradle.caching=true`, WO-001 §10 / kernel / content precedent, review F4 |

## Suggested cut for the Planner *(rev 1)*

Size: medium-small. Most logic is JVM-testable; the UI is thin; V-04 is independent of the app code.

- **D-0 Build step** (the only task that edits build files): `include(":devtools")`, `devtools/build.gradle.kts` **with the test-input declaration and `repo.root` property (F4)**, `debugImplementation` in `app`. Haiku is fine (mechanical). Done-check: the edit-a-scanned-file re-run proof of section "Test seams".
- **D-1 `play`:** `BoardSpace`, the `boardOverlay` slot, **the `secondaryCornerControl` slot and `placeSecondary` (F1)**, `solveByAid` **with the pending solve and `solvePending` in the frame-loop test (F5)**, `onSolved`, the `placeSecondary` sweep and the parity checks, `play/src/test` and the `play/src/androidTest` parity test. After D-0. **sonnet**; the orchestrator reads the diff (session and device behaviour).
- **D-2 `devtools` core:** `DevPasscode`, `DevToolsState`, `DevSolution`, JVM tests. After D-0. Haiku with a read of the diff (small; the geometry is the kernel's).
- **D-3 `devtools` UI:** `DevCornerButton` **(with `blocked`, fixed 56 × 40 size, F6)**, the dialog, `DevSolutionOverlay`, strings en/fi (V-05). After D-2. **sonnet** (Compose, device behaviour); the orchestrator reads the diff.
- **D-4 `app`:** the `DebugAids` pair **(with `blocked`)**, `AppViewModel.aids`, `TangramApp` (`secondaryCornerControl`, `boardOverlay`), `MainActivity`. After D-1 and D-3. **sonnet**; the orchestrator reads the diff and runs `grep -ri "devtools\|0417"` over every non-devtools `src/main` and `src/release`.
- **D-5 V-04:** `v04_release_apk.py` (modes, canary, deny-list, dex versions, exit mapping, `--positive-control`) plus `TestV04ReleaseApk`, folding the puzzle check. **Independent of D-1…D-4** (crafted fixtures only; the real-APK runs come at the end). **sonnet** (a verifier must be right; it parses a binary format).
- **D-6 Acceptance Test Author** (independent, in parallel from the seams above): visible tests in `devtools/src/test`, `devtools/src/androidTest`, `play/src/test` (`SolveByAidTest`, the sweep), `app/src/test`, `app/src/androidTest`; held-out slice for A1 (wrong-code path and unlock) and A3 (the stored document, the best-time cases, the timeline after the aid solve). Never compiled by implementers before the move-in; the author compiles the staged device tests as soon as the APIs exist (AGENTS lesson).
- **D-7 Checks:** V-01, V-04 (builds release), `V-04 --positive-control` (builds debug), V-05/V-06/V-07, trace-check, a token grep of the new tests, the API 37 device run of the whole `play`, `browse` and `app` suites, and the sweep result.
- **Mandatory code-review checkpoint after D-1 + D-4** (the release surface): names in `main`, the hook's validation, the secondary slot and the sweep, **and (N7) "no release code derives placements from `puzzle.solution` except `devtools`, via `PieceGeometry.poseOf`"**, before D-3's polish and before D-7.

## Handoff — Design Author · WO-005 *(rev 1)*

- **Scope:** REQ-046 A1–A4 (#DevTools, `devtools`) + V-04 with `check_apk_puzzles` folded in · governed touched: `IPuzzleLibrary` (consumes, locked), `IProgressStore` (existing save path only; **no contract delta, v1 untouched**)
- **Inputs read:** as rev 0, plus `reviews/WO-005-design-review.md` and REQ-023; the layout sweep re-run on the reviewer's port (`corner_port.py`) for the new placement rule
- **Result:** `C:\GitHub\AI\TangramNoAds\designs\WO-005-design.md`
- **Status:** rev 1 forward to the Design Reviewer (spot-check §3b, §6, the seam and acceptance tables, the Alternatives rows, DA-73/75/78/79/81/83/85–88).
- **Traceability delta:** REQ-046 ↔ `devtools.DevToolsState` / `DevSolution` / `DevCornerButton` / `DevSolutionOverlay` (+ neutral `play.PlaySession.solveByAid`, `onSolved`, `BoardSpace`, `PlayArea.boardOverlay` / `secondaryCornerControl`, `PlayLayout.placeSecondary`; `app` `DebugAids` pair); G-04 / A4 ↔ V-04 + `ReleaseSeparationTest`; no task, code or test links yet
- **Notes for next station:**
  1. Log DA-72…DA-88 (changed rows: 73, 74, 75, 78, 79, 80, 81, 83; new: 85–88).
  2. The Finnish rows marked **AI** (all but "Valmis", with N6's wording) go on the owner list, together with the G-04 reading of DA-85.
  3. Open for Jami, none blocking: the extra string "could not place" notice (DA-80); the G-04 reading (DA-85). The earlier questions about the pill's corner and the aid-solve look are closed by the F1 and F5 rulings.
  4. WO-006 must skip `dev-*` nodes in the REQ-037 walk; WO-008 wires `onSolved(byAid)` through `SessionHost` into `time`; WO-009 re-runs V-04 on the final artifact and decides about R8 and an `.aab`.
  5. The `testRelease` variant stays deleted (DA-56): do not re-add the stanza to "test the stub".
