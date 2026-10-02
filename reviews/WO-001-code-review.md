# Review — code · WO-001

**Date:** 2026-10-02  ·  **Reviewer:** fresh context
**Inputs:** workorders/WO-001.md, tasks.md (WO-001 rows), designs/WO-001-design.md, architecture.md §2/§3b/§5, decisions.md (F2, DA-1..DA-7), REQ-019/020/021/051, req_types.md (TYPE-001/003/004); code: kernel/src/{main,test}, play/src/{main,test}, contracts/src/main, build files, app skeleton, .swdev/verifiers, tools/tangram_geom.py (+ the three `newline="\n"` generators), tools/export_geometry_golden.py, tools/golden/geometry.json, .swdev/guard.json + guard-log. NOT read: .swdev/heldout/**, chat history, progress.md.

## Verdict

**recirculate → TASK-007** (one small, targeted fix: V-06 cannot fail on two dependency spellings). No product-code defect found; no Blocker. Test & Verify (held-out run) may start in parallel because F1 and F6 touch only `.swdev/verifiers`. F2 should be triaged by the orchestrator before close.
**0 Blockers, 2 Shoulds, 7 Notes.**

## Evidence re-derived (not taken from the author's report)

- `gradlew assembleDebug test --console=plain --rerun-tasks`: BUILD SUCCESSFUL, 78 tasks executed. Result XMLs: kernel 116 tests (Arithmetic 17, ConvexClip 18, Golden 10, PieceGeometry 28, Silhouette 18, Margin 1, RoundTrip 5, LockSearch 19), play 31 (REQ-019 15, REQ-020 4, REQ-021 5, REQ-051 7), app 1; 0 failures, 0 skipped. Margin test printed: 12,732,408 fits, smallest nonzero area 1.2627e-3.
- `python -m unittest ... test_verifiers.py`: 13 tests OK. `v01_release_manifest.py` (builds the release merged manifest) PASS; `v05` PASS; `v06` PASS.
- `validate_puzzles.py Tangrams`: 13/13 valid. `export_geometry_golden.py`: oracle agrees on 13 files, `shapes-warmup-4` lists (2,2); `cmp` before/after: byte-identical.
- trace-check: RED as expected, 46 FAIL lines, all "uncovered acceptance", no contract drift (only the logged PieceShapes.kt notify delta, guard-log 07:28:43), no rule-3 "directory theater" line, req-lint 0 errors, views fresh. Breakdown: 43 out-of-scope REQs, REQ-019 A2/A4 and REQ-021 A1 (held-out), plus REQ-020 A2 (see F7).
- Independent oracle (my own scratch script, outside the project): for each of the 13 puzzles, sampled 8 mid-wedge directions around every solution vertex and applied the "not all 8 / not one run of 4" rule in floats; its outline-corner set equals `tools/golden/geometry.json` on all 13 files (0 diffs). So Kotlin = Python = my oracle, including (2,2) of shapes-warmup-4.
- CRLF scan (`\r`) over kernel/play/contracts/app sources, verifiers, Gradle files, tools/*.py, golden: none.
- Not re-run by me: AGENTS.md rule 10 browser chain (`tools/tests/test_prototype.py`, svg2png sheets) — Playwright venv is a session scratchpad outside my brief's Run list; the author's claim stands unverified by me.

## Checklist applied

- [x] **Directives** — D1/D2: one search shared by drop, preview, (later) tap-turn; no duplicate geometry. D3: every public type's KDoc names its REQ/TYPE (DropResolver.kt lines 14/26/38/44/48; LockSearch header TYPE-004; Silhouette DA-7/REQ-019/051). The two beyond-design items (`Candidate`/`candidates`, ConvexClip cap) are `internal`, explained in-file, and used by tests/search; acceptable (F3). D4: no speculative seams (no `interrupt`, no Compose). D5: androidx-startup, kotlinx-serialization (test scope) logged in decisions. D6: met.
- [x] **Guardrails** — G-01: merged release manifest has no permission/uses-permission, allowBackup=false, extraction rules exclude all nine domains in both sections, no EmojiCompatInitializer (V-01 PASS on the real tree, manifest read by me). G-03: no double decides an exact order/equality — corners-on-anchors is exact `in anchors`, READING_ORDER is exact Q2 `signum`, candidate identity is exact `at`; doubles only in distance-to-finger, clip areas and score (all permitted by design/DA-3; score ties use TIE_EPS by DA-1). G-06: play main deps = kernel + contracts only (V-06 PASS; but see F1 for the verifier's own gap). G-10: NaN/Inf origin, NaN/<=0 dpPerUnit give null/0.65 (tested); `require`/`multiplyExact` only on programmer error. LF endings: clean.
- [x] **Contract** — `contracts/src/main/**` unchanged (trace-check: no drift); the only delta is `kernel/model/PieceShapes.kt` (new file, Write tool, guard-log row present, WO-001 delta table row present). `Exact.kt`/`Pieces.kt` untouched.
- [x] **Scope** — everything traces to REQ-019/020/021/051 or an enabler (TYPE-001/003/004, G-01/03/06). Nothing unrequested found beyond F3. REQ-020.A2, on-screen REQ-021/051 and F5/DA-5 are correctly carried to WO-003.
- [x] **Traceability** — every `REQ-NNN.An` token sits on a test with assertions (all 31 read in full); in-scope IDs 019 A1/A3, 020 A1, 021 A2, 051 A1/A2 covered; A2/A4/021.A1 are held-out by design.
- [x] **Hard-stops** — nothing touches PII/schema/migration; manifest/backup covered by G-01 + V-01.
- [x] **Evidence re-derived** — see above.
- [x] **Conceptual 20 %** — see the four attack results below.

### Attack results (the brief's items)

1. **ConvexClip cap / non-finite → 0.** The cap is sound: a convex intersection can never exceed either input, so for real overlaps it is a no-op (it only trims rounding and the degenerate-clipper case). "Non-finite → 0.0" is fail-OPEN for the overlap test (0 overlap = valid) and fail-closed for inside (0 inside = deficit = invalid). It is NOT reachable from `LockSearch`: every polygon is built from `ExactPoint.toDouble()` of Long-backed rationals (finite for any representable value), and NaN/huge `origin` never enters the clip (it only filters candidates by distance, `NaN <= R` is false). So REQ-019.A2 cannot be violated through it today; F3 asks for fail-closed so a future change cannot silently invert it.
2. **LockSearch vs TYPE-004.** Verified line by line: candidates keyed by exact `at` (`seen` set), only the dropped turn/mirror (`offsets(shape, turn, mirrored)`), own-id entries excluded from both anchors and obstacles (`others`, and again in `fitAt`), `distance <= lockDistance` plain compare, scan sorted by (distance, reading order) so list order is irrelevant, window = stop condition against the smallest valid |t| (DA-2; design's proof holds: n in [1,4]), score = distance - 0.04 n with exact `n`, DA-1 tie-break distance-then-reading-order with TIE_EPS. Ignoring a `placed` entry with the moving piece's id is right: the piece is lifted, so its old corners must not be anchors nor its old area an obstacle (F31; tests `theMovingPiecesOwnCornersAreNoAnchors`, `aPlacedPieceIsAnObstacleToAnotherPieceButNotToItself` fail on a regression). `lockDistance` = max(0.65, 30/dp), non-positive/NaN/Inf dp gives 0.65.
3. **DropResolver vs the REQs (read from the code, not the tests).** REQ-020: `Home` carries piece + turn + mirror (F2) and `Locked` keeps turn/mirror; the sealed type has exactly two resting states; off-board/tray gives Home. REQ-021: `preview` and `release` call the same private `search`, so they cannot disagree by construction; null = draw nothing; preview returns the same `PlacedPiece` release would lock. REQ-051: pulse is `CornerPulse(silhouette.outlineCorners)` only when `kind == MINI && overBoard` and no lock; never carries piece corners; warm-up/full give null. One literal-reading deviation, logged as DA-4 (tray drop in a mini puzzle goes home without a pulse) — see F8.
4. **Test substance.** No empty/tokened-but-assertionless test. Fixtures are literal coordinates (not produced by the code under test) and the sweeps carry non-vacuity guards (locks >= 20, homes >= 20). Distinguishing power is good (bonus-vs-distance pair 1.49/1.45, mirror in a real hole, turn +-1 in a real hole, half-integer grid). Two convention pins and one near-tautology: F2, F5.

## Trajectory & quality

- **Verification actually run?** Yes. Every task row records a re-run done-check, and the numbers (116/31 tests, margin 1.2627e-3, 13-file round trip, golden byte-identical) reproduce exactly on my rerun.
- **Proportionate?** Yes. The kernel/play code is ~600 lines of main for 9 acceptance IDs plus a tolerance proof (margin test); additions beyond the design are two internal helpers and a clip cap, all flagged by their authors. The verifiers are the least careful part (F1, F6).
- **Path sane?** Yes. One design recirculation (0 B/5 S/6 N) and one plan recirculation (1 B) converged by spot-check; no thrashing in the WO log. TASK-006's implementer reading only part of the tests/REQs did not show: the code matches REQ text on all points above.

## Findings

### F1 — GUARDRAIL · Should · .swdev/verifiers/v06_module_deps.py:~104-115 (TASK-007)
- **Observation:** `parse_build_gradle` computes `invalid_patterns` (`projects.x` accessors and `project(path = ...)`) and then does `pass`; only `implementation|api|...(project(":x"))` is collected. I reproduced it in a scratch project: `play/build.gradle.kts` with `implementation(projects.browse)` and `api(project(path = ":browse"))` gives **V-06 PASS, exit 0**; the same dependency written `implementation(project(":browse"))` gives `V-06 FAIL play -> browse`. The WO DoD requires "their self-test proves each can fail"; this verifier fails only on one spelling of the violation, and the self-test (`test_v06_fails_with_invalid_dependency`) uses that spelling only. G-06 is the compile-time isolation rule of the whole architecture (ADR-002).
- **Proposed resolution:** treat both patterns as dependencies (parse the module name), or fail closed ("unparseable project dependency"); add self-tests for `projects.browse` and `project(path = ":browse")` on a disallowed pair. Owner: TASK-007.
- **Triage:** *(filled on recirculation)*

### F2 — TEST · Should · play/src/test/.../acceptance/Req019AnchorLockAcceptanceTest.kt:174-199 (`a1_equalScoreAndDistanceAreBrokenByReadingOrderOfThePosition`), Req051CornerPulseAcceptanceTest.kt:~97 (`a1_aDropOnTheTrayOrOutsideTheBoardGoesHomeWithoutAPulse`) (TASK-T / orchestrator)
- **Observation:** Both carry a REQ acceptance token but assert a convention no REQ/TYPE mandates: DA-1 (reading-order tie-break; req_review F23 says TYPE-004 has none) and DA-4 (no pulse for a tray/outside drop; literal REQ-051 says "when a drop goes home", and REQ-020 lists tray drops as going home). They are AI interpretations awaiting a capture-side CHG. If the owner later chooses another tie-break or pulses tray drops, REQ-019.A1 / REQ-051.A1 coverage turns red although the locked REQ text did not change; conversely the token over-claims what A1 means ("assert only what is mandated", orchestration section 3).
- **Proposed resolution:** keep the assertions (they pin a deterministic engine) but move them to untokened decision tests named for DA-1 / DA-4 (the kernel already pins DA-1 in `LockSearchTest.tieBreakWorkedExample...`), or keep them in place with the token removed and a `// decisions.md DA-1` / `DA-4` label. The orchestrator owns the call since it concerns how the acceptance author's tests were tagged.
- **Triage:** *(filled on recirculation)*

### F3 — QUALITY · Note · kernel/.../geometry/ConvexClip.kt:62-65 (TASK-005a)
- **Observation:** Beyond design section 6 (which says "always finite"): the area is capped at min(result, subject, clipper) and a non-finite value becomes 0.0. The cap is mathematically a no-op for real overlaps (see attack 1). The non-finite branch is fail-open for `maxOverlap` and unreachable today (inputs are exact values converted to double). `ConvexClipTest.nonFiniteInputNeverLeaksNaNOrInfinity` only asserts `isFinite()`, so it would pass a fail-open or fail-closed result alike. Design and decisions do not mention the cap (only tasks.md does).
- **Proposed resolution:** in `LockSearch.fitAt`, reject a candidate when any input coordinate is non-finite (fail closed) and add a test that a NaN-coordinate placed piece never yields a lock; record the cap as a one-line deviation in the design handoff/decisions. No behaviour change needed now.
- **Triage:** *(filled on recirculation)*

### F4 — QUALITY · Note · kernel/.../lock/LockSearch.kt:74,159 (TASK-005b)
- **Observation:** The own-id filter is applied twice (`find` builds `others`, `fitAt` skips again). Harmless and makes `fitAt` safe for the margin test, but it is a second implementation of the F31 rule.
- **Proposed resolution:** leave, or drop the one in `find`'s `others` for `fitAt` and keep the anchor filter only. Optional.
- **Triage:** *(filled on recirculation)*

### F5 — TEST · Note · play/.../Req020GoHomeAcceptanceTest.kt:16-55 (TASK-T)
- **Observation:** `a1_everyDropEndsLockedOrHomeAndNeverInBetween`: "never in between" is guaranteed by the sealed type, so that part cannot fail. The test still earns its token through the asserted parts (turn/mirror kept on both outcomes, no lock when off board, >= 20 of each ending).
- **Proposed resolution:** none required; do not extend its claim in the traceability report.
- **Triage:** *(filled on recirculation)*

### F6 — QUALITY · Note · .swdev/verifiers/v05_string_parity.py:44-61, v01_release_manifest.py (TASK-007)
- **Observation:** V-05 wraps each `ET.parse` in `except Exception: pass`, so an unparseable `strings.xml` on both sides passes silently and a corrupt `values-fi/strings.xml` only fails by accident (empty set). It also reads only `strings.xml`, not other `values*/*.xml` files. V-01 checks the SOURCE `data_extraction_rules.xml` rather than the packaged one and picks the newest merged manifest by mtime; both acceptable now, but a stale-manifest risk if a build is skipped.
- **Proposed resolution:** V-05: report a parse error as FAIL; glob `values*/*.xml`. V-01: note the source-vs-packaged choice in its docstring. Owner: TASK-007 (can ride along with F1).
- **Triage:** *(filled on recirculation)*

### F7 — TRACE · Note · trace-check output
- **Observation:** 46 FAIL lines, not "38 + 3": 43 out-of-scope REQs, REQ-019.A2/A4, REQ-021.A1 (held-out, expected), and REQ-020.A2, which the WO carries to WO-003 ("no error message", screen matter) — also expected but absent from the brief's list. Nothing else: no contract drift, no rule-3 line.
- **Proposed resolution:** none; the Traceability Auditor should expect REQ-020.A2 red until WO-003.
- **Triage:** *(filled on recirculation)*

### F8 — SCOPE · Note · play/.../DropResolver.kt:52 and decisions DA-4 (WO-003 / capture side)
- **Observation:** (a) DA-4 narrows REQ-051 (see F2); it is logged with a CA-3 follow-up, but is an AI-only decision under governance row 12 and should be shown to the owner at checkpoint 1. (b) `DragPose.overBoard` is the caller's boolean: the engine cannot tell what "over the board" means (finger vs piece centroid vs piece outline). A mini-puzzle player who drops a piece mostly on the board but with the finger on the tray edge gets no lock and no pulse. This is WO-003's definition, not WO-001's, but it decides REQ-019/020/051 behaviour on screen.
- **Proposed resolution:** WO-003 must define `overBoard` (suggest: the piece's centre, not the finger) and re-verify REQ-021.A1 with the "pose of the last displayed preview" calling rule from design section 7; list both in WO-003's brief.
- **Triage:** *(filled on recirculation)*

### F9 — QUALITY · Note · app/ skeleton (orchestrator-written, inline exception)
- **Observation:** The promised checker pass: manifest, `data_extraction_rules.xml`, `MainActivity`, strings en/fi (parity holds), Gradle (`FAIL_ON_PROJECT_REPOS`, bytecode 17, minSdk 26, no network/ads/analytics dependency) show no defect. Only `ToolchainProofTest` (`assertEquals(4, 2 + 2)`) is vacuous scaffolding, carries no token and is labelled as such.
- **Proposed resolution:** delete `ToolchainProofTest` when the first real `app` test lands (WO-003).
- **Triage:** *(filled on recirculation)*

## Handoff

- **From:** Code Reviewer (fresh context), WO-001, 2026-10-02
- **Status:** **recirculate → TASK-007** (F1 Should; F6 Note can ride along). Spot-check by the orchestrator after the fix is enough; no re-review of kernel/play needed.
- **Parallel:** Test & Verify may start now (kernel/play code is unchanged by the proposed fixes). F2 is an orchestrator tagging call (re-tag two tests, or accept with a reason in Triage) and does not block the held-out run.
- **Not verified here:** the AGENTS.md rule-10 browser chain (`test_prototype.py` ALL PASS, preview sheets) — outside this brief's Run list.
- **Findings:** 0 Blocker, 2 Should (F1, F2), 7 Note (F3-F9; F6 rides with F1 on TASK-007).
- **Files:** `C:\GitHub\AI\TangramNoAds\reviews\WO-001-code-review.md`
