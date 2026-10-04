# WO-005 code review (after CR-3): DA-92 / TASK-037, TASK-035b, TASK-030d, TASK-033b

Reviewer: fresh-context code reviewer. Read-only: no Gradle, no emulator, no git. Method: read the current files plus AGENTS.md, DA-90..93, the tasks.md rows and architecture G-03/G-10. Device and unit results are the author's claims in tasks.md and were NOT re-run here.

## Verdict: forward (0 B / 0 S / 7 N)

## Findings

| ID | Sev | file:line | Finding | Rule/REQ | Suggested fix | Owner |
|----|-----|-----------|---------|----------|---------------|-------|
| F1 | N | CrispEdgesScaffoldingTest.kt:92-93 | The tray colour `0xEFE7D6` is hard-coded and duplicates `VisualTokens.TRAY_CELL` (the file already imports `VisualTokens`). A palette change fails the test for the wrong reason. | DA-92 | Use `VisualTokens.TRAY_CELL.toArgb()`. | slice-implementer |
| F2 | N | CrispEdgesScaffoldingTest.kt:59-64 | `after` is captured right after `place()` ends with `up()` + `advance(32)`. The 180 ms lock glide (`PlayTiming.GLIDE_MS`) is still running, so the piece is drawn at an interpolated pose and scale, not the final one. The 2 px probe (about 0.76 dp at 420 dpi) can be tinted by that offset, which is a possible flake on API 37 and on API 26. The API 37 run is still pending (MOVE-DEV), so only API 26 evidence exists. | DA-92 | `rule.mainClock.advanceTimeBy(300)` before `rule.shot()`. | slice-implementer |
| F3 | N | CrispEdgesScaffoldingTest.kt (whole) | The guard covers only `fillPiece` (placed and tray piece edges), measured as fail-then-pass. A regression of the dashed outline, the landing preview, the badge glyph or the confetti back to a scaled draw would not trip it. The silhouette and picture are covered by the old BoardPixels A2 and SolvedPicture A1 probes. The probes are mid-edge only, never at corners. Acceptable for scaffolding. | DA-92 | Optional: one dashed-outline probe (a tray cell whose piece left). | test author |
| F4 | N | PlayDrawing.kt:261 | The comment "text is measured in px, so positions are dp times density here (the only px code)" is stale. The whole file is now px code, so the comment contradicts the header rule at lines 43-47. | DA-92 | Reword to "text is measured in px, positions are dp times density as everywhere else". | slice-implementer |
| F5 | N | PlayDrawing.kt:257, 280-283 | The chip corner radius `5f` and the badge glyph geometry (7/4/3/2 dp) are bare literals, not `VisualTokens`. This is not new and each is multiplied exactly once. Under G-03 the dp to px step stays a single multiplication. Also `PictureDrawing.kt:193` shadows `d` inside `drawShape` (already noted by the orchestrator). | G-03 (hygiene) | Name the constants and rename the lambda variable at the next touch. | slice-implementer |
| F6 | N | v04_release_apk.py:80-82 | `devtools_values` falls back silently to the repo copy of a strings file when `--project-root` lacks it. That contradicts "a missing file = exit 2". The exclusion glob at line 86 still uses `root`, so the two halves can disagree. Also, the excluded shared values (today only fi "Valmis") are not printed, so a shrinking deny-list is invisible. | G-04, DA-90 | Drop the fallback, or fail when the root differs from the repo root. Print "V-04 note: N values denied, M shared excluded". Add one synthetic fixture test for the shared-value exclusion. The existing test only asserts on the live repo. | verifier owner |
| F7 | N | v04_release_apk.py:85-88 | DA-90 exclusion analysis: see below. It is acceptable, but the residual blind spot should be written into DA-90. | G-04, DA-85 | Add one sentence to DA-90. | orchestrator |

## Scope 1: DA-92 / TASK-037

**(a) Correctness: no value missed or doubled.** I walked every dp value in `drawPlay`, `fillPiece` and `drawBadge`. Each is multiplied by `density` once, and I found no double conversion.
- Board: the round rect, its radius and the cell rects and radii.
- Piece edge stroke: `PIECE_EDGE_DP * density`.
- Dashed outline: width, dash and gap.
- Preview: width, dash and gap.
- Pulse: `r * d` for the circle and `PULSE_RING_DP * d` for the ring.
- Confetti: translate `(centre + dx) * d`, rect offset and size `* d`, and rotate about `Offset.Zero`.
- Badge: disc radius, centre `(right*d - r, top*d + r)`, glyph offsets and stroke.
- Size-mark chip: the rect and the `5f * d` radius.
- Mark text centre: `* density`.
- Polygon path points: `polygonPathPx`, `p * density` once, in Double and then `toFloat`.

Picture side:
- `drawShape`: `pt()` converts dp to px, and `u = dpPerUnit * d` handles widths, radii and half-extents.
- `paintShape` stroke: `units * dpPerUnit * density`.
- `drawPicture` base rect: dp times `d`.
- `drawPictureImage`: the bitmap is `size.width/height` in px, drawn at the origin by the software `CanvasDrawScope(Density(density))`. The nested `drawPicture` reads the same density, so there is no scale mismatch.
- Layer order matches the documented design order: board, silhouette, cells, dashed outlines, tray pieces, placed pieces, preview, pulse, glides, drag, picture, confetti, badge, chips, letters.
- Confetti still runs under translate+rotate. That is not a scale, and it is measured crisp (tasks.md).

**(b) Caching.**
- `silhouettePathPx` is keyed on dp-path identity and density, so a density or layout change rebuilds it.
- `silhouetteDp` is a stored `val` (`PlayLayout.kt:54`), so the identity check does not thrash.
- `clearPictureCaches` nulls the dp slot, the px slot (`pxSource`, `pxPath`), the key and the image. `pxDensity` stays set, but it is harmless once `pxPath` is null.
- It is main-thread only: draw and `remember` run on the UI thread, and the KDoc says so. `PlayArea.kt:142` calls the clear.
- The thumbnail uses `buildSilhouettePath` (uncached) inside `remember(layout, density)`, so it never touches the play area's slot. The `layout` key is itself `remember(puzzle, w, h)`, so there is no stale state. Each cell holds one small Path.

**(c) Per-frame cost.**
- The silhouette is zero-allocation per frame.
- Per-frame allocations are the same set as before: `polygonPathPx` per piece, two `Path`s in the badge, and `floatArrayOf` for dashes.
- `fillPiece` reuses one path for fill and stroke.
- `arrayOf` for the image key is allocated only on a rebuild.
- There are no growing caches: one slot each, and one Path per thumbnail.

**(d) Guard test.** See F1 to F3. It is a real assertion with a meaningful failure message, and the author reports it fails with `fillPiece` reverted.
- Its 2 px probe is justified by the measured blur ramp (tasks.md). It matches the 2 px / 3 px reasoning in the KDoc.
- API 37 soundness is plausible: AA bleed at the outer stroke edge is at most 1 px, and the size-mark chip region and the tray cell margins are excluded. The mid-glide capture (F2) is the only flaky assumption. Not yet run on API 37.
- No pulse should show: pulses are for misses, not locks.

**(e) Rule consistency.** A grep of `src/main` in play, browse, app and devtools finds no `scale { }`, `withTransform` or `graphicsLayer`. The only `scale(` hits are the Matrix pre-scale of a Path (`PictureDrawing.kt:65`, `PuzzleThumbnail.kt:37`). That is the intended transform of a path object, not a canvas scale. `DpScope.kt` and the `VisualTokens.kt` header are comment-only and accurate. The `PlayDrawing.kt` header rule is accurate, apart from the stale comment in F4. The AGENTS.md duty text matches.

**(f) G-03 and G-10.**
- G-03: the one dp to px step is the single multiplication, with no rounding in between. Thumbnail and play area use the same `buildSilhouettePath`, so the union is identical.
- G-10: a failed union still falls back to the added polygons. Empty point lists and unparsable path data (DA-21) are skipped, and nothing new can throw.

**Android 8 drawing now matches API 37 drawing: yes, by construction.** Every path is now built in px and no canvas scale is applied, so the API 26 hardware renderer takes the same crisp path as API 37. Rects, circles and round rects were already crisp and are numerically unchanged. The author reports API 26 `play` at 113/113 and the full suites green at DA-93. I did not re-run any device test. The API 37 run remains pending at MOVE-DEV, so API 37 parity is by reasoning plus the unchanged picture, bitmap and layout maths. The mid-glide risk in F2 is the one thing that could show as a false failure there.

## Scope 2: TASK-035b (V-04)

- `devtools_values` reads both strings files, requires 6 or more chars and unescapes `\'` and `\"`. `\n` and `\u` escapes and whitespace collapsing are not handled, but no current value uses them (checked by grep).
- A missing file raises `RuntimeError`, which `main` turns into exit 2 (modulo the fallback in F6).
- The shared-value exclusion removes a devtools value only when a non-devtools string of the module set equals it exactly. Today that is only fi "Valmis" (devtools_done = browse `done`).
- Could the exclusion blind V-04 to a real leak? Only for a leaked devtools string resource whose text equals an app string, and only in the resource-text scan of that value. A real leak of the devtools module is still caught by all of these:
  - the dex scan for the devtools package, dotted and slash form, and the `0417` string;
  - the `devtools_` key and "Wrong passcode." in the resources;
  - the other 24 values (en + fi) in resources.
- A leaked shared string alone would be a dead resource with no behaviour, because the code (dex) is the DevTools. That is acceptable, and DA-90 records it. A residual risk is that resource-name shortening in a future AGP could remove the `devtools_` key search. That is why the value deny-list exists, and it reinforces the "re-run `--positive-control` after toolchain change" duty (DA-91 F3). See F7.
- New tests: `test_v04_finnish_devtools_string_in_arsc`, `test_v04_long_english_devtools_hint_in_arsc`, `test_v04_devtools_values_threshold` and the "short and common words do not fire" test. They have real assertions: fail on UTF-8 and UTF-16 needles, the threshold is checked, and "Valmis" is asserted excluded. They depend on the live repo strings, which makes them brittle to future string edits (F6). I did not run them.

## Scope 3: TASK-030d

`devtools/build.gradle.kts:59` adds `app/src/debug/**` as a separate `inputs.files(...)` on the test tasks. It is separate from `a4FileSetS` (line 56), with a comment saying it is never part of S. Correct: S does not include the debug aids, so A4 (devtools-name-free set) stays clean.

## Scope 4: TASK-033b

`DevCornerButton.kt:52` now says `TangramApp` does not compose the pill while the puzzle is SOLVED. Matches DA-82 and CR-3 F8a. KDoc only, no behaviour change.

## Counts

Blockers 0, Significant 0, Nits 7. Verdict forward. The orchestrator may fold F1, F2 and F4 into the MOVE-DEV test touch-up.
