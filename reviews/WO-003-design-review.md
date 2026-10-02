# Review — design · WO-003

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:** `designs/WO-003-design.md` (Draft, 2026-10-02) · `workorders/WO-003.md` (2026-10-02) · `build-map.md` v1.1 §1–2 · `architecture.md` v1.0 (§1–§5) · ADR-001/002/006 · `decisions.md` (all rows to 2026-10-02: F2–F20, DA-1…DA-14) · `design-inputs.md` v0.2 §1–2 · REQ-002, 006, 011–023, 035–037, 039, 043, 045, 047, 051 (collection v1.1, locked) · `req_types.md` v0.2 (TYPE-001/003/004/006/007) · `contracts/src/main/**` · `kernel/src/main/**` · `play/src/main/**` · `content/src/main/**` · `app/**` · root/module build files + catalog · `.swdev/guard.json`, `.swdev/verifiers/v06_module_deps.py` · DI: `Spec/01`, `Spec/02`, `tools/prototype_template.html` (layout, input, flip, solve) · framework: directives v0.3, review-report template, handoff-contract v0.2, traceability-rules §5 · Compose `ui-android` 1.12.1 (Gradle cache, bytecode of `SuspendingPointerInputModifierNodeImpl.onCancelPointerInput`)

## Verdict

**recirculate → design-author.** Most of the design is sound. The shape (a pure-Kotlin core plus a thin Compose shell), the frame-frozen release pose, `refit` going through the one search, the kernel additions, the WO-002 F1 equivalence and the tray maths all check out. One Blocker: the pointer adapter assumes the wrong model of how Compose delivers a system cancel. Built as written, the binding F5/DA-5 rule fails on a real device, and no planned test would notice. Six Shoulds sit mostly in the same flagged area (state-machine gaps, adapter coverage on the device) and in the test seams, which the parallel Acceptance Test Author cannot yet write against.
**1 Blocker, 6 Shoulds, 8 Notes.**

## Checklist applied

- [x] **Directives.** D1: one `play` surface; `app` wires; kernel holds TYPE-001/003/006/007. D2: proportionate, and the alternatives table is honest. D3: every kernel/`play` abstraction names its REQ (§8 table). The exceptions are the release-build `puzzle` extra (F6) and two small items (N6). D4: O-06 callbacks and O-09 slots are deferred to their consumers. D5: only allowlisted test libraries are added. D6: n/a at design.
- [x] **Guardrails.** G-01: manifest unchanged, orientation set in code, V-01 is a regression check. G-02: test-scope rows planned (DA-28). G-03: exact `at`; doubles only for drawing, the finger and the clip areas. G-05: `play` strings in en+fi. G-06: main-scope deps OK (V-06 skips `test*`/`androidTest*`, verified). G-07: OK. G-08: see F7. **G-10: F3** (session intents after an external interrupt) and **F6** (reading launcher extras).
- [x] **Contract.** No `I*` change, `kernel.model` untouched (new packages `kernel.lock/state/layout`, `PieceGeometry` additions in `kernel.geometry`), `contracts/` untouched. The binding interpretation rows F5/DA-5/F13 are honoured in intent; the mechanism for F5 is wrong (F1).
- [x] **Scope.** All items trace to the in-scope or carried IDs, except the release-build half of DA-23 (F6).
- [x] **Traceability.** The REQ links in the design are present. Test seams are incomplete (F5). REQ-039 and REQ-045 belong to `#Content` (code home `content`). Their on-screen tests in `play`/`app` are additions; trace-check rule 3 is already met by the WO-002 `content` tests.
- [x] **Hard-stops.** None touched; no security, PII, schema or migration.
- [x] **Evidence re-derived.**
  - Tray scale: 360×780 → ts 24.17, so the smallest cell is SQ at 60.3 × 88.4 dp (REQ-013 A1 holds). 390×844 → 26.80; 1280×800 → 27.5 (row-height limit binds). Both match Spec/02.
  - Turn diameters from the kernel shapes: 4.216, 3.162, 2.981, 2.108, 2.000.
  - TYPE-001 resting turns LT 4 / MT 1 / ST 4 / SQ 1 / PG 0, checked against the kernel's transform.
  - Badge-square overlap geometry (F4).
  - Compose cancel semantics: `javap` on `ui-android` 1.12.1 (F1).
  - The WO-002 F1 equivalence, from `PuzzleParser`/`PackagedPuzzles`.
  - `Rational` is Long-based, so `toDouble` is always finite (N1).
  - V-06 scope rules.
  - `fitAt` callers: `LockMarginTest:93`, `LockSearchTest:288`.
- [x] **The conceptual 20 %.** Effort went into §4–5 (the flagged risk), the badge and the tray, the seams, and the carried items.

## Trajectory & quality

- **Verification actually run?** This is a design. Its numeric claims (turn diameters, V-06 behaviour, the WO-002 F1 equivalence) were re-derived here and are correct. The single wrong claim is the Compose cancel model (F1).
- **Proportionate?** Yes overall: pure core plus thin Compose, one surface, one machine. Minor extras are listed in N6; a closed-form, fixed-seed confetti is acceptable for testability.
- **Path sane?** First design pass; the WO log shows no thrash.

## Findings

### F1 — QUALITY · Blocker · design §4 (pointer adapter) / decisions F5, DA-5 / REQ-014, REQ-020
- **Observation:** §4 says the adapter wraps the gesture in `try { … } finally { machine.cancel() }` "so a system cancel (coroutine cancellation) is an interruption". Compose does not deliver a system cancel that way.
  - An `ACTION_CANCEL` reaches a `pointerInput` block as an ordinary `PointerEvent` in which every pressed pointer goes **up**, and the change is **already consumed**. In `ui-android` 1.12.1, `SuspendingPointerInputModifierNodeImpl.onCancelPointerInput()` builds `PointerInputChange(…, pressed = false, …, previousPressed = old.pressed, isInitiallyConsumed = old.pressed, …)` and dispatches it through the Initial, Main and Final passes. The coroutine keeps running.
  - An adapter written to §4's wording, mapping "pointer went up" (`changedToUpIgnoreConsumed()`, `!pressed`) to `machine.up`, turns every system cancel into a **release**. The piece locks or goes home, and a mini puzzle pulses, which is exactly the prototype behaviour that F5 overrode (design-inputs §2 item 5). During Pressed, the same mapping turns a cancel into a **tap**, so the piece turns.
  - System cancels are frequent on phones: an edge back-gesture taking over a drag that started near the 8 dp tray margin, the notification shade, focus loss.
  - Every planned F5 test calls `cancel()`/`interruptDrag()` directly on the JVM (seam row "F5/DA-5"). No device test injects a cancel, so the bug would reach Jami unseen.
- **Proposed resolution:** Replace the sentence with an adapter contract:
  1. Read changes in the Main pass.
  2. `up` only for `previousPressed && !pressed && !isConsumed` (`changedToUp()`).
  3. A pointer that goes up already consumed maps to `machine.cancel()` (Compose's cancel). Never use `changedToUpIgnoreConsumed()`/`!pressed` for release.
  4. Keep `finally { machine.cancel() }` for node detachment and key restarts.
  5. A cancel in Pressed is not a tap.

  Add instrumented tests in `play/src/androidTest` (tokens for F5/DA-5 and REQ-012 A2):
  - Mid-drag from a tray cell, held over a spot that *would* lock: `performTouchInput { cancel() }` puts the piece back in its tray cell with the pick-up turn and mirror, no lock, and no pulse (use a mini puzzle with the drag over the board).
  - The same test from a board pick-up.
  - A cancel during Pressed leaves the turn unchanged.

### F2 — TEST · Should · design §10 + seam table (device coverage of the pointer adapter)
- **Observation:** The adapter is called "~30 lines that only translate", yet it is the one place that does px→dp conversion, pointer-id routing and multi-pointer forwarding. On device only "one real-touch end-to-end drag" exercises it. A lazy adapter would pass every planned test and still misbehave under a finger in three ways:
  - **Thresholds in px.** If it passes px instead of dp, 12 dp becomes 12 px, about 4.6 dp at 2.625×. Drags still work, but finger jitter turns taps into drags.
  - **Second pointer dropped.** If it forwards only the first pointer, the twist never happens. REQ-017 is JVM-only in the plan.
  - **Badge via semantics.** A badge test that uses `performClick()` goes through the semantics `onClick`, not `HitTest`.
- **Proposed resolution:** Add device tests that use `performTouchInput` only (no semantics actions for player controls):
  - REQ-017 A1: two pointers, pointer 1 rotated 90° about pointer 0 mid-drag, gives turn +2.
  - REQ-015 A1: a 3 s press with 8 dp total movement is a tap.
  - REQ-015 A2: a touch 10 dp outside a placed small triangle picks it up.
  - REQ-018 A1: the badge pressed by touch in the tray and on the board.

  This keeps the instrumented set small and pins the adapter's units and routing.

### F3 — QUALITY · Should · design §4 state table (completeness; G-10)
- **Observation:** The table has Idle / Pressed / Dragging. Several real-finger cases are undefined or ambiguous:
  - (a) `Ignored(id)` appears only in a parenthesis, and its exit and its effect on other fingers are unspecified.
  - (b) A second finger down while **Pressed**: ignored? promoted to a twist once the drag starts? Prototype 0.6 twists here, because its `drag` exists from pointer-down.
  - (c) After the twist finger lifts, can a new second finger twist again? "(and no twist yet)" can be read either way. Does an already-down third finger get promoted?
  - (d) External interrupts (`onPause`, a size change, `onCreate`) clear `session.drag` while the machine still holds `Dragging(id)`. The next `move`/`up` of that finger calls `dragTo`/`release(nowMs): DropOutcome`, a non-null return, on a session with no drag. That is a crash path from touch if the implementation uses `checkNotNull(drag)` (G-10). There are two sources of truth and no rule linking them.
  - (e) The twist boundaries "±22.5°, ±67.5° … passes" are given, but the step formula is `round(angle/45°)`. Kotlin `round` is half-even (+67.5° gives 2) and `Math.round` is half-up (+22.5° gives 1, −22.5° gives 0). Both are asymmetric and differ from "passes", yet the seam row promises "±22.5° boundary behaviour" to an independent test author.
  - (f) `lift` is "the largest y of the centred shape **at the turn**". If it is recomputed per turn, a twist moves the floating centre (TYPE-003: a turn keeps the centre fixed). If it is fixed at drag start, the lowest point can end up under 30 dp above the finger after a twist. Either works, but it is unpinned and REQ-014 A1's JVM assertion depends on it.
- **Proposed resolution:** Make `Ignored(ids)` a state and add rows for (a) to (d):
  - Any down while not Idle that is not the twist candidate is ignored until it lifts. Two pieces can never be pressed at once (this covers multi-touch on the tray).
  - The machine learns of an external interrupt on its next event (for example by checking `session.isDragging` or a drag-generation counter) and moves the finger to Ignored.
  - Every session intent without a drag is a no-op: `release` returns `DropOutcome?` or is never reachable. `interruptDrag` is idempotent.

  For (e), define the step exactly, for example `steps = sign(a)·ceil((|a| − 22.5°)/45°)` for |a| > 22.5°, else 0 (strict "passes"). For (f), choose one rule and log it in DA-17/18.

### F4 — QUALITY · Should · design §3 badge position (REQ-015 rule, REQ-013 rule 3, F13)
- **Observation:** The tray badge's 60 dp touch square sits at "cell top-right corner", which puts about three quarters of it **outside** the PG cell. Re-derived at 390×844 (ts 26.80):
  - Cells: PG x 106.95–203.65; ST1 starts at 211.65.
  - The square reaches about 230 dp, i.e. 18–22 dp into **ST1's cell** over its top 30–34 dp. That is where ST1's **S** mark sits (REQ-043: top-left corner).
  - It also covers an 18–22 dp strip at the bottom of **LT2's cell** in row 1.
  - At 360×780 the figures are similar.
  - Under F13 ("the flip badge wins overlaps") those touches flip PG. That contradicts REQ-015 ("the touch area of a tray piece is its whole tray cell") and REQ-013 ("a cell holds … for the parallelogram, the flip badge").
  - The planned REQ-018 A2 test ("bounds ≥ 60 × 60 dp") cannot see the theft.
- **Proposed resolution:** Position the square so it never covers another piece's cell: keep it within the PG cell's column plus the adjacent gaps, accepting overlap with PG's own cell, where the player's intent is PG-related anyway. Alternatively, record an owner decision with these numbers. Add a JVM `HitTest` row: at 360×780 and 390×844, a touch on ST1's size mark picks ST1 and a touch on LT2's cell bottom picks LT2. On the board, the badge-wins overlap with neighbouring pieces is F13 as decided; state that it is accepted.

### F5 — TEST · Should · design "Test seams" (frozen signatures vs. the table)
- **Observation:** The Acceptance Test Author works from REQs plus the frozen seams only. Several table rows rely on seams that are not in the frozen list or contradict it:
  - **F8 `overBoard`:** no function is named, only `.boardRect`.
  - **REQ-016 A2 / REQ-018 A1:** `shake` is not among the `PlaySession` members.
  - **REQ-014 A2:** "piece scale" has no accessor (`DragPose` has no scale, and `DragMotion` exposes only `centre`).
  - **REQ-051 reduced motion:** `pulseRadius`/`shakeOffset` are not frozen.
  - **REQ-011 A2 and REQ-023 A1 (JVM):** "the polygon list handed to the drawer" / "the clip polygon list (same object)" have no seam.
  - **REQ-013 A2 (device):** "semantics bounds of `tray-cell-<id>`", although §3 draws cells on the Canvas and gives them no nodes.
  - **F5/DA-5:** "a counting subclass" of `DropResolver`, which is a final class, while the frozen `PlaySession(puzzle, reducedMotion)` has no resolver parameter.
  - **REQ-002 A1 PG flips:** no badge rect.
  - **App tests:** `app/src/androidTest` (REQ-002 A1, REQ-045 A2) cannot see `internal` `PlayLayout`, so it has no way to find the board transform for real-touch drops.
  - **REQ-023 A2:** "equal the picture's pixels" has no oracle.
  - **REQ-023/039 A1:** the rows sample "after 1.5 s", but confetti flies until 2400 ms over the board and would hit the "2 px outside" samples.
- **Proposed resolution:** Extend the frozen list:
  - `PlayLayout.overBoard(centreDp)`, `PlayLayout.badgeRect(session)` and `silhouetteDp`.
  - `PlaySession.shake`/`.glides`, plus a drawn-scale accessor (`DragFrame.scale` or `DragMotion.scale`).
  - Timeline functions `pulseRadius`, `shakeOffset`.
  - `PlaySession(puzzle, reducedMotion, resolver = DropResolver(puzzle))` with `open class DropResolver`. Alternatively, assert F5 by observable effects: no lock over a lockable spot, no pulse.
  - Test tags `play-area` and `board` (bounds = `boardRect`), and either `tray-cell-<id>` nodes or no device row for REQ-013 A2.
  - For the app tests, either expose the units→px transform as a semantics property, or move the real-touch solve into `play/src/androidTest` and keep the `app` tests to launch, composition and first puzzle.
  - A frozen `drawPicture` seam so REQ-023 A2 compares against a reference render of the picture alone.
  - Pixel samples after 2400 ms or with `reducedMotion = { true }`.

### F6 — SCOPE · Should · DA-23 (`puzzle` intent extra)
- **Observation:** In a release build the extra is silent scope (D3). No REQ asks for it, the WO says "first puzzle only", and no automated test needs it, because `play` tests build `PlaySession` for any puzzle. It also adds a surface on the exported launcher: reading any extra unparcels the whole `Bundle`, and a foreign app can send one that throws, which is a crash path (G-10). In a **debug** build it is justified: `shapes-mini-1` holds only SQ/ST1/ST2, so without it Jami's playtest (governance row 9, WO DoD) never sees the flip badge, the L/M marks, a full-set tray or LT/MT twists.
- **Proposed resolution:** **Debug-only.**
  - Read it in `app/src/debug` with a release twin returning `null`. This is the ADR-006 source-set pair.
  - Read it as `runCatching { intent.getStringExtra("puzzle") }.getOrNull()`; an unknown id opens the first puzzle.
  - Log it as a verification-channel aid, not a feature.
  - Hand Jami the `adb shell am start -n io.github.jamisuni.tangram/.MainActivity --es puzzle <id>` lines at the checkpoint.
  - WO-004 retires it or keeps it debug-only.

### F7 — TEST · Should · design §6.3 (`tools/check_apk_puzzles.py`) / G-03, AGENTS rule 10
- **Observation:** §6.3 places the check in `tools/` "so architecture.md §2 needs no new row". That is the right call, since `architecture.md` is locked. But G-03 and architecture §5 say the golden exporter and the AGENTS rule-10 checks run **whenever `tools/` changes**. That includes `tools/tests/test_prototype.py`, which needs the Playwright browser channel. WO-001 provisioned that channel only in a session-scratchpad venv (decisions, WO-001 G3). WO-003's verification-channel list does not mention it, and the check runs once with no owner afterwards ("WO-005 may fold it").
- **Proposed resolution:** List "exporter plus rule 10, including the Playwright browser channel" among WO-003's G3 channels, proven before P4. Record the fold into V-04 as a carried item for WO-005 (WO-005 file / build-map §2 row) so the G-08 check on the shipped APK does not rot after one run.

### N1 — QUALITY · Note · design §6.1 / DA-25 (ConvexClip fail-closed)
- **Observation:** The fix is sound: `Fit(+∞, +∞)` is rejected by `find`'s `!(a ≤ T && b ≤ T)`. The stated reason, "one return value cannot be closed in both uses", is not quite right: NaN would be closed both ways (`area − NaN` and `maxOf(x, NaN)` are NaN, and the check is negated). Also, non-finite input cannot occur from real callers, because every `fitAt` input is built from Long-based `Rational`, whose `toDouble()` is always finite.
- **Proposed resolution:** Keep the design's closure, but keep `fitAt`'s signature and behaviour (`LockMarginTest:93` and `LockSearchTest:288` call it) and make `misfit` its body. Fix the rationale sentence and add nothing more.

### N2 — QUALITY · Note · design §4–5 (time base, frame loop, twist noise)
- **Observation:** Animation maths mixes pointer `uptimeMillis` with `withFrameNanos`. On a device both are CLOCK_MONOTONIC, but under Compose UI tests the frame clock is the virtual `mainClock`. "`onFrame` … active only while something animates" could stop the loop while a finger is held still. Preview and drawn pose would then freeze together, and the JVM tests, which call `onFrame` by hand, would not notice.
- **Proposed resolution:** Pin one time base: animation starts are stamped at the next `onFrame`, or the adapter supplies frame-clock times. State that the loop runs every frame while `drag != null`. Optionally, ignore twist angle changes while the fingers are under about 24 dp apart: a 4 dp jitter at 10 dp separation is about 22° (owner's eyes).

### N3 — QUALITY · Note · design §2–3 (numbers and their sources)
- **Observation:** The REQ-013 maths re-derives correctly; see the Checklist. Three things are unpinned:
  - (a) Whether widths use the derived diameters (4.216…) or the REQ table (4.22). The difference is about 0.1 dp.
  - (b) Where `screenHeight` comes from. `Configuration.screenHeightDp` includes or excludes the bars differently on API 26 and 35+. On API 26 a bar-excluded 708 dp makes the row limit bind (ts 23.9), so the same phone gets a different tray per API level.
  - (c) The same question for the window width used by `classFor`.
- **Proposed resolution:** Use the derived diameters and have tests compare to the table within 0.005. Take the window size from `WindowMetrics` / `LocalWindowInfo`, including bars, in `app`.

### N4 — TEST · Note · design §2 `PathData`
- **Observation:** "Exactly the Spec/03 grammar" is under-specified: Spec/03 says only "absolute M/L/Q/C/Z". Shipped files glue letters to numbers and join pairs with commas (`M3.3,3.35 Q3.7,3.55 4,3.35`).
- **Proposed resolution:** Pin the number token (sign, decimals, exponent yes/no), glued letters (accepted), separator runs, and implicit command repetition (rejected), so that negative test cases match.

### N5 — SCOPE · Note · DA-17 (board pick-up)
- **Observation:** The design re-centres a picked-up board piece horizontally on the finger and eases its **position** over 120 ms. REQ-014 asks only for a 120 ms size growth and a float above the finger, and the prototype keeps the horizontal grab offset. For an LT grabbed at a corner at about 50 dp/unit this is a sideways jump of about 100 dp. DA-17 logs only the vertical lift.
- **Proposed resolution:** Log the horizontal rule in DA-17, or keep the grab x-offset for board pieces. Jami decides on the device.

### N6 — DIRECTIVE · Note · D3/D4 small items
- **Observation:**
  - (a) The badge's semantics `onClick` is a second activation path. #Accessibility is an `idea` feature, and the path also lets device tests bypass the touch path (F2).
  - (b) `isDragging` has no consumer in WO-003, while DA-27 defers O-06 for exactly that reason.
- **Proposed resolution:** Keep the role and `contentDescription`, which the tests need. Drop the semantics `onClick` or name its REQ. Keep `isDragging` (O-09 names it) but say so.

### N7 — TEST · Note · design §6.2 / §6.4 (carried checks)
- **Observation:**
  - (a) The WO-002 F1 equivalence is verified. The parser requires `file.name == "$id.json"`, and each index line yields exactly one parsed or rejected file, so `ids == stems ∧ size == lines` is equivalent to `rejected` being empty. The check proves more in `app/src/androidTest`, the APK Jami installs, than in `play`'s test APK.
  - (b) Signing a copy of the unsigned release APK with the debug key after the build changes no dex, resources or manifest, so the API 26 launch tests what was built. The shipped artifact (WO-009: AAB, upload key) is WO-009's concern.
- **Proposed resolution:** Prefer `app/src/androidTest` for the library check. State in the task brief that the release `buildType` gets no `signingConfig`.

### N8 — QUALITY · Note · size / split / channel order
- **Observation:** This is the largest WO so far, and its risk concentrates in tasks 1–3: F1–F3 live there. The WO says the channels are "to be proven before P4", but the design proves them in task 0, which is already P4.
- **Proposed resolution:** Keep **one WO**:
  - Make the post-task-3 logic-only code review **mandatory**, not optional.
  - Prove the instrumented channel before P4 in a scratch copy: the test libraries plus one trivial `androidTest` on the API 37 AVD. The API 26 image is Jami's SDK Manager step, asked at the checkpoint.
  - Cut at the author's seam (003a JVM logic / 003b Compose + shell) only if the instrumented channel or the API 26 image cannot be proven before task 4 starts.

## Handoff — Design Reviewer · WO-003
- **Scope:** REQ-002, 011, 012, 013, 014, 015, 016, 017, 018, 022, 023, 043 + carried REQ-020 A2, 021, 051, 039 A1, 045 A2, F5/DA-5, F8, WO-001 F3, WO-002 F1/F4/N9 · governed touched: IPuzzleLibrary (consumes) · no `I*` change
- **Inputs read:** designs/WO-003-design.md (Draft 2026-10-02) · WO-003 · build-map v1.1 · architecture.md v1.0 · ADR-001/002/006 · decisions.md to 2026-10-02 · design-inputs v0.2 · REQs (collection v1.1) · req_types v0.2 · contracts/kernel/play/content/app sources + build files (HEAD a4bdec1 + working tree) · Spec/01, Spec/02, prototype_template.html · Compose ui-android 1.12.1 bytecode
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-003-design-review.md`
- **Status:** recirculate → design-author (F1: the system-cancel mechanism is wrong for Compose, breaking binding F5/DA-5 undetected; plus 6 Shoulds)
- **Traceability delta:** none (review only; the seam additions requested in F5 will add REQ↔seam links)
- **Notes for next station:** Fix F1 and F3 in §4 and F5 in the seam list first; F6 recommendation is debug-only DA-23; split recommendation is one WO with a mandatory post-task-3 code review (N8). Re-review can be a spot-check of §3–4, the seam list and DA-17/18/23.

## Spot-check — revised design (2026-10-02)

**Read:** the whole of `designs/WO-003-design.md` ("Revised after design review"), with §3–4, the frozen seam list and DA-17/18/23/29/30 checked line by line, plus a whole-file consistency pass. At 360×780 and 390×844 the F4 badge square fits inside the PG column plus the gap.

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 Blocker (cancel model) | **fixed** | §4 adapter contract: Main pass; an unconsumed up releases; a consumed up calls `cancel()`; `finally` only for detach; a cancel during Pressed is not a tap. Dragging row and Interruption bullet agree. Device rows (1)–(3) added. |
| F2 (device coverage of the adapter) | **fixed** | Adapter row (4)–(7) added. §3 says `performTouchInput` only, no semantics `onClick`. |
| F3 (state machine) | **partly** | (a)–(f) all addressed: `Ignored` state; second finger during Pressed; re-twist; external interrupt via `isDragging` or a generation counter; strict symmetric step formula; lift fixed at drag start. Residue: §4 says `release` returns `DropOutcome?`, but the frozen seam list (line 183) still says `release(nowMs): DropOutcome` (E1). |
| F4 (badge overlap) | **fixed** | The tray square stays inside the PG column plus the gap (390: 151.65–211.65; 360: about 135.4–195.4). Board overlap accepted per F13. DA-29 and hit-test rows added. |
| F5 (seams) | **partly** | The list is extended, but stale text contradicts it: (i) §1 line 49 and the list still show `PlaySession(puzzle, reducedMotion)` without the `resolver` parameter (E1). (ii) The REQ-013 A2 row still uses `tray-cell-<id>` nodes, which §3 and the seam list now say do not exist (E2). (iii) The REQ-023 A1 row (and REQ-039 A1 "as REQ-023") still samples at 1.5 s, against the 2400 ms / reduced-motion rule (E3). (iv) `BoardTransform` must be a public key for `app/src/androidTest`, while §1 says everything else is `internal`. It carries only the board transform, but the app's real-touch solve must also *start* drags from tray cells, and no tray-cell position is visible from `app` (E4). (v) `drawPicture(...)` is "frozen" without parameters (E4). |
| F6 (DA-23) | **fixed** | Debug-only: `app/src/debug` `PuzzleExtra.read` inside `runCatching`, a release stub returning `null`, logged as a verification aid (§1, DA-23). |
| F7 (tools/ channel) | **fixed** | §6.3 lists exporter + rule 10 + Playwright as WO-003 channels and carries the V-04 fold to WO-005. The orchestrator still has to add that channel to `workorders/WO-003.md` → Constraints (G3 list). |
| N1 | **partly** | §6.1 is fixed (`fitAt` kept, defence in depth). The DA-25 row's Basis still gives the old "no single return value" reason (E6). |
| N2 | **partly** | One loop for the whole drag, frame-clock stamps and the 24 dp jitter rule are in. But §5 and the seams still feed the intents' `nowMs` and the machine's `timeMs` from pointer events, without saying it is frame-clock time (E5). |
| N3 / N4 / N5 / N6 / N7 | **fixed** | Window metrics including bars, plus DA-30; PathData grammar pinned; DA-17 logs the sideways jump; badge `onClick` dropped and `isDragging` justified; library check moved to `app/src/androidTest`; no `signingConfig`. |
| N8 (split) | **fixed** | One WO; mandatory Code Review after task 3; the proven instrumented channel is a precondition of task 4; a missing API 26 image means a recorded waiver. Stale: the Design Author handoff notes (DA-29/30 not listed, DA-23 and the split described as still open) and §10's "again on the API 26 AVD" without the waiver (E7). |
| New N9 (minor) | — | Under 24 dp finger separation, keep updating the twist's reference angle without accumulating, so a crossing back over 24 dp does not inject the skipped rotation. One clause in DA-18. |

**Line edits (mechanical; the orchestrator may apply them without re-review, as in decisions row "WO-002 design … one stale line"):**
- **E1:** In §1 and the seam list, the signature becomes `PlaySession(puzzle, reducedMotion = { false }, resolver: DropResolver = DropResolver(puzzle))`, and `release(nowMs): DropOutcome?`.
- **E2:** The REQ-013 A2 row becomes JVM only (`layout.cell` before and after 8 taps). Drop the `tray-cell-<id>` device part.
- **E3:** The REQ-023 A1 row reads "after 2400 ms or with `reducedMotion = { true }`" instead of "after 1.5 s". REQ-039 A1 inherits it.
- **E4:** `BoardTransform` is a **public** `SemanticsPropertyKey` in `play`, the only public test seam besides `PlayArea`/`PlaySession`. It carries the units→px board transform **and** the tray cell rects in px. The picture seam is `internal fun DrawScope.drawPicture(picture: Picture, layout: PlayLayout)`, clipped to `layout.silhouetteDp`.
- **E5:** The intents' `nowMs` and the machine's `timeMs` are the latest frame-clock time, supplied by the adapter. `PointerInputChange.uptimeMillis` is never passed in.
- **E6:** DA-25 Basis reads "defence in depth: non-finite input is unreachable (Long-based `Rational`); WO-001 code review F3".
- **E7:** Refresh the Design Author handoff notes (log DA-15…DA-30; DA-23 and the split are decided). §10 says "API 26 when present, else the recorded waiver".

**Spot-check verdict: forward**, conditional on E1–E7 being applied before the Acceptance Test Author is briefed. The Blocker is fixed. F3 and F5 are left with stale text only; no open design question remains. N9 is optional.
