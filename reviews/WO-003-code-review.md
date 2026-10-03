# Review — code · WO-003 work-order review (TASK-018a, 018b, 019, 020 and the build steps)

**Date:** 2026-10-03  ·  **Reviewer:** fresh context (code-reviewer)
**Inputs:** AGENTS.md, CR-1 (`reviews/WO-003-CR-1-code-review.md`), the WO-003 design and `tasks.md` WO-003 rows, `decisions.md` DA-15…DA-42 and the staffing rows, REQ-012/018/035/036/043 read in full (the other REQs in the brief only where a finding touched them), the in-scope sources (`play/.../draw/*`, `PlayArea.kt`, `PlaySession.kt`, `app/src/**` main/debug/release/testRelease, tools, V-07, build files), and the two implementer scaffolding tests. The acceptance tests were not audited (fixture rulings DA-40/41 are the orchestrator's; nothing in them was seen to weaken its REQ). `.swdev/heldout/` not read.

## Verdict

**revise (spot-check only)** — no Blocker. The drawing, the frame loop and the app shell re-derive clean against the REQs. Four Should findings: one visible glitch after a window change on a solved puzzle (F1), two scaffolding tests that cannot fail for the thing they are named after (F2, F3), and one unlogged waiver (F4). None changes a Contract or a seam. Re-review by spot-check.

**0 Blockers · 4 Should · 9 Nit/Note**

## Evidence re-derived

- `gradlew :play:testDebugUnitTest :app:testDebugUnitTest :app:testReleaseUnitTest --rerun-tasks`: BUILD SUCCESSFUL, 91 tasks executed, no compiler warnings. Result XMLs: play 130 tests, app debug 1, app release 2, 0 failures, 0 errors (matches the claim).
- Python: `tools/tests` 14 OK (`test_path_data`, `test_check_apk_puzzles`, the rest); the one error is `test_prototype.py` importing `playwright`, which is not in this shell's Python (the scratchpad venv channel, DA row 45), not a defect. `.swdev/verifiers/test_verifiers.py`: 25 OK. `v07_no_junk_paths.py .`: PASS.
- `tools/check_apk_puzzles.py` run by me on both built APKs: PASS for `app-debug.apk` and `app-release-unsigned.apk` (13 puzzles + `index.txt`, byte for byte). Its self-test builds fixture zips and does fail on changed, missing, extra, schema-shipped and bad-index cases (all asserted).
- Manifest unchanged (not in the diff). Release APK: no `uses-permission`, `allowBackup=false` present. No change to permissions, network, analytics, backup (rule 1, G-01).
- Grep of `play/src/main`, `app/src/main`, `kernel/src/main` for `currentTimeMillis|uptimeMillis|nanoTime|SystemClock|eventTime|Instant`: nothing. `withFrameNanos` is the only time base (PlayArea.kt:118).
- CRLF count over every in-scope source, build file and tool: 0 (LF).
- Token audit: no `REQ-NNN.An` text in `PlayAreaAdapterScaffoldingTest`, `SessionObservationScaffoldingTest`, `PuzzleExtraReleaseScaffoldingTest` or any `play/src/test` scaffolding file; the only tokens are in the acceptance files, where they belong.
- V13 vs `PathData.kt`: compared line by line (`validate_puzzles.py:193-257` against `PathData.kt:21-84`): same command set and arity, same separator rule (spaces, one comma, a minus starts a number), same ASCII-only digit tests (no `isdigit()`/Unicode digit hole), non-finite rejected on both sides (Python `float("1"+"0"*400)` is `inf`, rejected like Kotlin), same trailing check. I found no input on which they differ. The case table is a hand copy, see N5.
- Not run: the single permitted device class (nothing needed device proof; F1 is derived from the code and flagged as such).

## Checklist applied

- [x] **Directives / guardrails** — G-01 (above), G-02 (the espresso pin is `androidTestImplementation` only, test scope, nothing ships; no new runtime library), G-05 (V-05 PASS per the brief; mark texts via string resources), G-06 (play main: kernel, contracts, Compose foundation only), G-10 (see attacks 7, 8).
- [x] **Contract** — no `I*` or `kernel.model` change in this scope; `PlayArea`/`BoardTransform` are the design's listed seams.
- [x] **Scope** — nothing unrequested found beyond N9 (the drawn size-mark chip is canvas drawing plus a semantic node, where the design said composables; logged by neither a DA row nor a task note).
- [x] **Traceability / token rule** — clean.
- [x] **Hard-stops** — none touched (no saved-state migration; persistence is WO-004).
- [x] **Evidence re-derived** — above.
- [x] **Conceptual 20 %** — attacks below.

## Attack results

1. **DA-38 queue.** Events are queued on the main thread and drained fully, in order, inside `withFrameNanos` with that frame's `ms`, then `session.onFrame(ms)` (PlayArea.kt:118-131), so the release uses the previously displayed frame (calling rule 1). Wake-up: `pending` is set at enqueue; the `snapshotFlow` value `pending || animating` can only go false after `pending = false`, which is written after the drain, so no queued event can be abandoned by `collectLatest`. A sleeping loop does not cut animations: `animating` covers drag, glides, shake, pulse and the solve window (to t0 + 2.5 s); each of those writes `version`, which the flow reads. Cost is at most one wasted frame at idle. Clean.
2. **Cancel on leave / size change.** `DisposableEffect(machine)` cancels directly (no time needed); the `finally { enqueue(Cancel) }` is inside `awaitEachGesture` (CR-1 N2 kept); a size change clears the queue, calls `machine.cancel()` and `interruptDrag()` (CR-1 F2 residual met). A Cancel after a normal Up is a no-op (the machine already dropped the drag). Clean. Test substance of N2 is F3.
3. **Time base / caches.** No wall clock anywhere. `cachedPolys/cachedPath` and `pictureKey/pictureImage` are top-level main-thread-only vars (see N2 for their lifetime). `silhouettePath`'s identity key is sound: `silhouetteDp` is a `val` of one `PlayLayout`.
4. **DA-42 cache key.** Picture, layout, density, width, height (PictureDrawing.kt:125): complete (layout carries board scale and origin, so a tray-only change that makes a new layout also rebuilds; harmless). Float density compares by `equals` through boxing: correct. Fade: the bitmap is opaque inside the clip and transparent outside, so `drawImage(alpha = a)` is the same as group alpha for every pixel including the anti-aliased clip edge; it equals the spec "picture fades 0 to 1 over the pieces" (DA-19). Memory (width x height x 4: about 9.8 MB at 390x~700 dp x 3, about 16 MB at 2560x1600) is in the DA-42 row; nothing recycles it, which is safe here (no use-after-recycle with a RenderThread draw) but see N2. Hitch: N1.
5. **Union path (REQ-011, DA-40).** Union in units on a 2^-20 grid, then transformed to dp. Vertex coordinates stay below 16 units for the shipped content, so `toFloat()` of a grid value is exact (24-bit mantissa); a coincidence failure needs a true value within about 1e-15 of a half-grid boundary, which dyadic and irrational coordinates cannot produce in practice. The failure fallback (`addPath` of every polygon) draws something (G-10). The picture clip is the same `Path` object by construction (REQ-039 A1).
6. **Tray, marks, badge, preview, pulse (REQ-012/043/018, DA-20/37).** Tray order, colours and resting turns come from `TrayRules` via `PlayLayout`; the dashed outline is drawn for every non-Tray piece (board or dragged) at the resting turn, unmirrored, at `trayScale`, in the miniature's centre (DA-37); marks only for Tray pieces and only for the three triangles (REQ-043 A1/A2); the badge square is `BADGE_DP` = 60 dp and `badgeRect` is null once solved (DA-35); preview is a dashed white 2.5 dp outline with no fill, pulse a 9 dp accent circle with a white ring, static under reduced motion (DA-20). Reduced motion gates only pop, confetti and the pulse animation, as DA-19 says.
7. **G-10 from touch and drawing.** `PieceDrawing.scale` 2-argument throw: reachable only through its `Tray`/`Board` branches from the 3-argument total variant, so unreachable for `Dragged`. `checkNotNull(layout)` in `beginDrag`/`refit`: reachable from touch only after `GestureMachine.down` has set `session.layout` (PlaySession.kt:149, 242 vs GestureMachine.kt:82); a null provider returns before. `layout.cell(...)` calls in drawing and in `BoardTransformData` are guarded by `hasCell` or by the filtered `trayRows`. A zero-size area gives `dpPerUnit` 0 and NaN geometry but no exception on any path read (`ImageBitmap` is guarded by `w <= 0 || h <= 0`).
8. **Lifecycle (DA-26, F5).** `AppViewModel` holds the library and the session only; the `reducedMotion` lambda calls the static `ValueAnimator.areAnimatorsEnabled()` (API 26), so no `Activity` or `Context` is captured. `interruptDrag()` runs in `onCreate` and `onPause`. The debug extra is read inside `runCatching`, the release twin never touches the intent (ADR-006, DA-23); but the proof is F2.
9. **Edge-to-edge and insets.** `enableEdgeToEdge` before `setContent`; `safeDrawing` padding on the column, so the tray clears the gesture bar (REQ-035 A2); layout class and screen height come from window metrics including bars (DA-30); the title is `inLanguage(locales[0].language)`, fi else en (REQ-012 rule, REQ-047). Portrait lock from `Resources.getSystem().configuration.smallestScreenWidthDp` through `LayoutRules.lockPortrait` (F3, TYPE-007). Clean apart from N8.
10. **Build steps.** espresso-core 3.7.0 is `androidTestImplementation` in `play` and `app` only (DA-39, G-02). The `androidComponents` block enables the release host test only (`selector().withBuildType("release")`); it builds, and `testReleaseUnitTest` runs 2 tests. No `signingConfig` on release, so the release APK is unsigned (the API 26 launch is waived, F4).
11. **Solved timeline (REQ-023, DA-19).** Pop 300 ms x1.06 about each piece's centre, picture 0 until 600 ms then linear to 1 at 1400 ms, pieces stop being drawn at alpha 1, confetti 2.4 s and never under reduced motion, badge gone at the solve. Matches `SolvedTimeline` and the DA rows. Two findings come out of the timeline: F1 and N6.

## Findings

### F1 — LIFECYCLE / VISUAL · Should · play/PlayArea.kt:81 (with play/draw/PlayDrawing.kt:113) · owner TASK-018b
- **Observation:** `nowMs` is `remember(session) { mutableLongStateOf(0L) }`, so it restarts at 0 whenever the composition is rebuilt for a surviving session: any recreation (locale or dark-mode change, font scale, tablet rotation or window resize; the ViewModel keeps the session, DA-26). `PlaySession.solved.t0` is a real frame-clock ms (large). `drawPlay` computes `sinceSolve = nowMs - t0`, which is hugely negative on the first composition: `pictureAlpha` is 0, `piecesHidden` is false, pop and confetti are off. A puzzle that was already solved therefore redraws as coloured pieces on the board (no picture) until the first frame callback writes the real `nowMs`, then flips back to the picture: one or two frames of flicker at every window change on a solved puzzle. The picture bitmap is rebuilt on the same frame (layout changed), so the hitch lands on the flicker. Derived from the code; not run on a device. The acceptance tests do not rotate after a solve, which is why 91/91 and 4/4 do not show it.
- **REQ/decision:** REQ-023 A1/A2 (the solved screen is the picture), REQ-036 A2 / DA-26 (a window change keeps the state), DA-19.
- **Proposed resolution:** seed the clock from the session, not from 0: store the last frame ms in `PlaySession.onFrame` (an `internal var lastFrameMs`) and use `mutableLongStateOf(session.lastFrameMs)`, or draw nothing time-dependent for a solved session until the first frame. Add a device scaffolding test: solve, advance past 2.5 s, recreate the composition (toggle `show`), assert on the first frame that the pieces are not drawn (e.g. a pixel sample inside a piece reads the picture, not the piece colour).

### F2 — TEST SUBSTANCE · Should · app/src/testRelease/.../PuzzleExtraReleaseScaffoldingTest.kt:12-16 · owner TASK-019
- **Observation:** the proof "the release stub never reads the intent" works by allocating an `Intent` with `sun.misc.Unsafe` so that any call throws "not mocked" (true today: `isReturnDefaultValues` is set nowhere, verified by grep). But the stub's own result is always `null`, and the likeliest regression is copying the debug twin into the release source set. The debug twin wraps the call in `runCatching { intent?.getStringExtra("puzzle") }.getOrNull()`: `runCatching` catches the "not mocked" `RuntimeException` and returns null, so `assertNull` still passes. The test therefore cannot fail for the very change it exists to catch. It also depends on a hidden setting (returnDefaultValues) that nothing asserts. `Unsafe.allocateInstance` itself is fine on the JDK 17/21 toolchain.
- **REQ/decision:** DA-23, ADR-006, G-04 spirit (a debug aid never in release).
- **Proposed resolution:** replace the trap by a spy. Define in the test `open class SpyIntent : Intent() { var touched = false; override fun getStringExtra(name: String?): String? { touched = true; return "x" } }`, allocate it through `Unsafe` (so no constructor runs), call `PuzzleExtra.read(spy)`, and assert both `assertNull(result)` and `assertFalse(spy.touched)`. A copied debug twin would then return "x" and set the flag, so it fails. (Optionally also scan the release `PuzzleExtra.class` bytes for `getStringExtra`.)

### F3 — TEST SUBSTANCE · Should · play/src/androidTest/.../PlayAreaAdapterScaffoldingTest.kt:87-110 and :134 · owner TASK-018b
- **Observation (a):** `gestureWhoseUpNeverArrivesDoesNotFreezeTheNextOne` is meant to prove CR-1 N2 (a leaked id must not freeze the next gesture, healed by the in-gesture `finally { enqueue(Cancel) }`). It hides the composable with `show = false` and shows it again. That throws away the `remember(session)` state, so the second gesture runs on a brand-new `GestureMachine` and cannot see any leaked id whatever the `finally` does. Delete the `finally` block and the test still passes. What it does prove (the `DisposableEffect` cancels on leave: `drag` is null after the detach) is real, but the name and comment claim more.
- **Observation (b):** line 134, `assertTrue(!cfg.contains(SemanticsProperties.Text) || true)`, is always true and asserts nothing; it should be deleted.
- **REQ/decision:** CR-1 N2 / F2 residual; F5/DA-5.
- **Proposed resolution:** (a) split it: keep the detach half under an honest name (`leavingTheCompositionCancelsTheDrag`); for N2 stay in the same composition: after `consumedUpIsACancel`'s `cancel()` (and after a gesture whose last `up` is injected while another finger is still down), perform a tap on a tray piece and assert the turn advanced, which fails if a leaked id survives. (b) remove the line.

### F4 — PROCESS · Should · decisions.md (WO-003 rows) · owner orchestrator (TASK-020 close)
- **Observation:** the API 26 release-launch waiver exists in `progress.md` (lines about 233, 305), `STATUS.md` and the TASK-020 row ("recorded waiver (no image)"), but `decisions.md` has no waiver row for it. The only API 26 rows are the minSdk decision and the P2 row 34 ("an API 26-32 emulator is a verification channel from WO-003"). AGENTS says governed decisions, waivers included (class 16, as row 23 and row 45 do), are appended to `decisions.md` with what, why and how to reverse. minSdk 26 is therefore currently unproven, and the one API-26-only call in the shell (`ValueAnimator.areAnimatorsEnabled`, API 26) is by reading fine but not run.
- **REQ/decision:** governance.md class 16; decisions row 34.
- **Proposed resolution:** add the row (what: release launch on API 26 not run; why: no system image installed, release is unsigned; reverse: Jami installs the image, run the debug build and the release-launch step; reported at the WO close checkpoint).

### N1 — PERFORMANCE · Nit · play/draw/PictureDrawing.kt:119-133 · owner TASK-018a
- The bitmap is rendered synchronously in the draw pass of the first frame whose picture alpha is above 0 (t0 + 600 ms), on the main thread: a few tens of milliseconds for a phone-size bitmap at the moment the picture starts to fade, and larger on a tablet. DA-42 names the possible hitch and accepts it; it can be avoided because there is 600 ms of slack. Fix: warm it as soon as `solved != null` (draw it once with alpha 0 in the pop phase), or better build it on `Dispatchers.Default` from a `LaunchedEffect(solved)` (a software `CanvasDrawScope` does not need the UI thread; `silhouettePath` would then need its own copy or a lock, since its cache is main-thread only).

### N2 — RESOURCE · Nit · play/draw/PictureDrawing.kt:35-36, 110-111 · owner TASK-018a
- The two caches are process-wide `private var`s: after the player leaves the screen they keep a `Picture`, a `PlayLayout` and up to about 16 MB of bitmap alive for the life of the process (no Activity reference, so not a context leak). Fix: hold the caches in a small class created with `remember(session)` and dropped by `onDispose`, or clear them from the `DisposableEffect` in `PlayArea`.

### N3 — PERFORMANCE · Nit · play/PlayArea.kt:176 and play/draw/PlayCanvas.kt:36 · owner TASK-018b
- `nowMs` is passed by value, so every animated frame recomposes the whole `BoxWithConstraints` content: `BoardTransformData` and `trayCellsPx` are rebuilt, six `stringResource` lookups run, the semantics boxes are recreated. It works and is cheap at this size, but reading the clock inside the `Canvas` draw (pass a `State<Long>` or `() -> Long`) would make a frame a draw-only pass.

### N4 — ROBUSTNESS · Nit · app/AppViewModel.kt:15 and TangramApp.kt:52 · owner TASK-019
- `library.puzzles.first()` throws on an empty library (a crash at launch, G-10 spirit); bundled content is covered by the on-device library check (DA-22), so this is theoretical. The title is a fixed 66/74 dp box of unbounded `BasicText`: at large font scale or a long fi title it wraps and overflows the box. `maxLines = 1` with ellipsis is a one-liner. The size-mark text is 12 sp in a 20 dp chip (PlayDrawing.kt:262): above about 1.6x font scale the letter outgrows the chip (REQ-043 legibility).

### N5 — TOOLS · Nit · tools/tests/test_path_data.py:11-24 · owner TASK-020
- The accept/reject table is a hand copy of the Kotlin one; nothing detects drift between `SolvedPiecesScaffoldingTest.kt:38-39` and the Python lists (the reject lists already differ in order and by one repeated `"M1"`). The grammar mirrors today (see Evidence). If the grammar changes again, one shared JSON file read by both tests would make "same accept/reject set" mechanical.

### N6 — VISUAL · Nit · play/draw/PlayDrawing.kt:204-212 vs :171 · owner TASK-018a
- At the solve the last piece glides for 180 ms without the pop factor, then at 180 ms joins the board loop where `pop` is about 0.95 into the sine (scale about 1.057): an instant 5.7 % size step on the piece the player is watching. Multiply the glide scale by `pop` as the board loop does.

### N7 — TOOLS · Nit · tools/check_apk_puzzles.py:23-50 · owner TASK-020
- False-pass edges, all remote: an empty `Tangrams/` expects only `tangrams/index.txt` (a bare "\n") and would pass an APK with no puzzles (add "at least one puzzle"); duplicate zip entries of the same name collapse into the last one; the script is a manual step with an APK path, not tied to the build variant (it passed on both debug and release here). V-07's self-test is sound (`test_verifiers.py:437-491` builds each junk name in a temp dir and asserts it is found, plus the CLI exit codes), and, like V-06, it is a manual verifier.

### N8 — SHELL · Nit · app/MainActivity.kt:25-30 · owner TASK-019
- `requestedOrientation` is set after `super.onCreate`: a phone launched while in landscape is created, then recreated in portrait (the ViewModel survives, so only a visible flash; with F1 fixed it is harmless). Accepted pattern for a runtime policy that a manifest cannot express; note it for the playtest.

### N9 — SILENT DEVIATION · Note · play/draw/PlayCanvas.kt, PlayDrawing.kt:248-270 · owner TASK-018a
- Design section 2/7 says the size marks and the badge are real composables (`BasicText` chips); the code draws chips, text and badge on the canvas and adds empty semantic boxes (`size-mark-<id>` with text and content description, `flip-badge` with bounds) for the tests and accessibility. Behaviour matches the REQs and the device tests pass; only the design sentence is out of date. One line in the task row or a DA row would close it.

## Trajectory & quality

- **Verification actually run?** Yes: counts, the three test tasks under `--rerun-tasks`, both APK checks, the Python and verifier suites reproduce. The device suites were not re-run by me (91/91 and 4/4 are taken as stated).
- **Proportionate?** Yes. The split (pure session/gesture/timeline in `play`, drawing in `play.draw`, adapter in `PlayArea`) is the right size; the DA-38 queue and DA-42 bitmap are logged, justified deviations from the design, each with its cost named.
- **Path sane?** Mostly. The weak spot is test substance again, not code: three scaffolding assertions that cannot fail for their stated purpose (F2, F3, N-level `|| true`), the same family as CR-1 F3. Token discipline was kept (none on scaffolding). The remaining behavioural gap (F1) is a lifecycle state that was fixed for the ViewModel (DA-26) but not for the animation clock that depends on it.
- **Attacks that came back clean:** no lost or reordered pointer events; no wall clock; no crash path from touch or from the drawing's data; `PieceDrawing.scale` throw unreachable; release APK carries no permission and the puzzles byte-match; V13 mirrors the Kotlin grammar; LF everywhere; no `REQ-NNN.An` on a scaffolding test; the dashed outline, marks, badge and preview behave as REQ-012/043/018 and DA-20/35/37 ask.

## Handoff

**Status:** revise (spot-check re-review; no Blocker)
**Artifacts:** `C:\GitHub\AI\TangramNoAds\reviews\WO-003-code-review.md`
**Findings:** 0 Blockers · 4 Should (F1 TASK-018b, F2 TASK-019, F3 TASK-018b, F4 orchestrator/TASK-020) · 9 Nit/Note (N1-N9)
**Next:** route F1 and F3 to the 018b implementer, F2 to the 019 implementer, F4 to the orchestrator (decisions row); N1-N9 optional before Test & Verify (N1, N2 are the DA-42 follow-ups). Orchestrator spot-checks, then Test & Verify.
**Reviewed artifacts edited:** none (only the scratchpad `wo3cr.txt` was written).

## Spot-check (re-review)

**Scope:** the changed code only (PlayArea, PlaySession, PictureDrawing, PlayDrawing, MainActivity, the four tests and the APK checker), read against the fixes. The orchestrator's re-run counts (JVM play 131, device play 94, app 4, rule-10 chain) are taken as given; I did not re-run them. Code was not edited.

| Finding | Status | Evidence / remark |
|---|---|---|
| F1 clock reset | Resolved | `PlaySession.lastFrameMs` is written in `onFrame` (PlaySession.kt:199); `PlayArea.kt:82` seeds `nowMs` from it. The loop keeps running until t0 + 2.5 s, so the last `onFrame` of a solved session is past the picture window and the seeded draw shows the picture. A new session seeds 0 as before. Device test `recreatingTheCompositionOfASolvedSessionKeepsThePicture` compares the first frame after recreation with the settled one (and was shown to fail with the seed at 0). |
| F2 release proof | Resolved | Spy subclass allocated with `Unsafe` records `getStringExtra`, `getExtras`, `hasExtra`; asserts `touched` is false and the result null. A copied debug body sets the flag even inside `runCatching`. A debug twin using another getter (e.g. `getIntExtra`) would not be caught; acceptable, the debug twin reads a string. |
| F3 N2 test | Resolved, one accepted residual | Same-composition tests (cancel mid-press, cancel mid-drag, then a tap turns the piece in the same `PlayArea`), an honestly named detach test, and `|| true` is gone. Residual (removing only the in-gesture `finally` leaves all tests green): judged acceptable. Injected touch always ends in an up or a consumed up, so the leaked-id state is reachable only by coroutine cancellation, and `pointerInput` restarts only when `session` or `machine` change, which also replaces the machine; detach is covered by the `DisposableEffect` test. The `finally` is a redundant net, keep it as defence in depth. Nit: the comment should say so. |
| F4 waiver | Resolved (orchestrator) | DA-43 row per the coordinator; not re-read by me beyond that statement. |
| N2 caches | Resolved | `clearPictureCaches()` in `onDispose` (PlayArea.kt:110). A recreation disposes then rebuilds, so a solved session rebuilds its bitmap once on the first draw (the N1 hitch, already carried to the playtest notes); correct either way, since the cache is keyed. |
| N6 glide pop | Resolved | PlayDrawing.kt:211 `sc * pop`. |
| N7 checker | Resolved | Empty-repo guard (`NO PUZZLES`), duplicates, case-insensitive prefix with the odd-case name landing as EXTRA, nested paths via the same prefix test; 11 self-tests. Traced `check()`: no new false pass found. |
| N8 orientation | Resolved | Set before `super.onCreate`; the orchestrator confirmed on the emulator that a landscape cold start requests PORTRAIT and does not crash, with one relaunch inherent to the F3 policy. |
| N9 | Resolved | Logged as DA-44 per the coordinator. |
| N1, N3, N4, N5 | Carried (accepted) | To the playtest notes, WO-006/WO-004 and WO-005 as triaged; none blocks Test & Verify. |

### New issues introduced by the fixes
None found.

### Verdict
**forward.** No Blocker or Should remains open; carried Nits are tracked in tasks.md.
