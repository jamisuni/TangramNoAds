# WO-009 traceability audit

Auditor: Traceability Auditor AUDIT9 (fresh context), 2026-10-05. Read-only run: no Gradle build, no emulator, no git, no flag set, no `mark`, no product code or test edited. Pure Python and shell reads against the tree and the existing APKs. The only thing written is this file. Format follows `reviews/WO-008-trace-audit.md`.

**Verdict: GREEN for WO scope, with the DA-158 waiver (the five F17 IDs).** trace-check is RED on exactly the five waived IDs and on nothing else. Every non-waived WO-009 ID (REQ-042 A2/A3, REQ-048 A3, C8) is linked REQ to task to code or artifact to a token-carrying test. The five waived IDs have staged carriers whose sha256 values match and which T&V9 proved red and green. The token isolation table holds, the held file is byte-identical to the withheld copy, no orphan code, and the contract is intact. Findings: **0 B / 3 S / 7 N**. All three S are close-out bookkeeping (CLOSE9) and none is a linkage gap.

## Run

`trace_check.py --project .`: **exit code 1 (RED)**. 51 REQs (47 locked, 4 withdrawn), 270 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, 72 contract files, profile ai-mastered. No drift section, no baseline-delta line. One harmless `DeprecationWarning` from `trace_check.py:381` (SWDev tool).

FAIL lines, verbatim:
- REQ-001 (Locked): uncovered acceptance A2
- REQ-039 (Locked): uncovered acceptance A2
- REQ-042 (Locked): uncovered acceptance A1
- REQ-048 (Locked): uncovered acceptance A1, A2

That is exactly **REQ-001 A2, REQ-039 A2, REQ-042 A1, REQ-048 A1, REQ-048 A2**, the five waived IDs. No other RED. REQ-042 A2, A3 and REQ-048 A3 are no longer in the list (they left at MOVE-JVM9 and HELD-IN9).

Re-run by me (read-only):

| Check | Result |
|---|---|
| `validate_puzzles.py Tangrams` (V1-V6, V8-V10, V12, V13, V14, V15) | 25/25 valid, 0 FAIL lines, exit 0 |
| `tools/tests` (all but `test_prototype.py`) | 114 tests OK. `test_prototype.py` cannot import `playwright` in this Python (environment, see N6) |
| `test_verifiers.py` | Ran 114, OK |
| `test_v09_acceptance.py` | Ran 34, OK |
| V-01 on the release merged manifest | PASS |
| V-05 / V-06 / V-07 | PASS / PASS / PASS |
| V-04 on the universal APK | PASS (canary found) |
| V-04 `--positive-control` on the debug APK | PASS (4 marker kinds) |
| V-08 on the universal APK | PASS (release); sha256 29a27e97..., equals the TASK-099 line |
| V-08 `--apk <debug> --expect-debug` | PASS (debug) |
| V-09 strict on `Tangrams/` | **FAIL, 0 of 25 reviewed, 6 themes, exit 1 (as designed)** |
| V-09 `--pre-review` on `Tangrams/` | PASS, exit 0 |
| V-09 strict / `--pre-review` on the universal APK | FAIL (0 reviewed) / PASS |
| Control-byte scan (under 0x20 except tab, LF, CR) over `tasks.md`, `decisions.md`, `release/*.md`, `reviews/WO-009-*.md`, `workorders/WO-009.md` | none |

## (a) REQ to task to code or artifact to test

| ID | Tasks | Code or artifact | Covering test (token) | Does it exercise the code? |
|---|---|---|---|---|
| REQ-042.A2 (at least four categories) | 091-0, 091a, 091b, 091c, T9c | the 25 files of `Tangrams/` (6 themes: animals 4, people 2, things 4, vehicles 3, nature 4, shapes 8) | `content/.../release/ReleaseLibraryTest` `req042_A2_...` (1 token, in tree, always on) | Yes. Reads `PuzzleLibrary.packaged()`, the real library, with a non-empty fixture check; fixture `control_*` tests carry no token |
| REQ-042.A3 (`shapes-square` present) | as A2 | `Tangrams/shapes-square.json` | `ReleaseLibraryTest` `req042_A3_...` (1 token) | Yes. Same real library; control without the file and with an unparseable file, no token |
| REQ-048.A3 (installed app declares no permissions) | TASK-099, T9b, HELD-IN9 | `app/src/main/AndroidManifest.xml` (only `tools:node="remove"` lines), V-01 / V-04 / V-08 / V-09 on the universal APK | held method `req048_A3_theInstalledAppStillDeclaresNoPermissions` in `HeldInstalledPermissionsAppTest` (1 token) | Yes. Reads the installed target package, with a control on the platform package. First run 2/2 on API 26 and API 37 (T&V9). The store-bound limit is stated in the class header |
| C8 (REQ-010.A2 on the store-bound artifact) | TASK-099, T9b | same | existing `REQ-010.A2` method, same class (1 token, unchanged) | Yes. V-01 and V-08 on the universal APK PASS (re-run by me), the API 26 launch shows no permission entries |
| C11 (REQ-001.A2) | 094, T9c | `release/store-listing.md`, `release/release-checklist.md` | staged `ReleaseEvidenceTest` (waived) | See the waived table |

Code home note: WO-009 adds no `src/main` code beyond the icon resources and one `android:icon` / `roundIcon` pair in the manifest (DA-164, DA-167). The new "code" is content, tools and docs.

### The five waived IDs

| ID | Staged carrier | sha256 (full) | Match | Red / green proof |
|---|---|---|---|---|
| REQ-042.A1, REQ-039.A2 | `.swdev/staged/WO-009/carriers/content/src/test/.../content/release/ReleaseLibraryReviewedTest.kt` (2 tokens: REQ-042.A1 line 35, REQ-039.A2 line 43) | 31c3bef5eef859def4356b64b7c625fa5df419527bf1817101b83b24c3bd27bc | equals scratchpad `wo9_carriers_sha.txt`, `tasks.md` CR-8-FIX row, T&V9 | T&V9: red 2/2 on :40 and :48 for the real state (0 of 25 `true`), green 2/2 on a scratch with 25 flags `true` and a `mark_files` ledger. Fail-loud canaries first |
| REQ-048.A1, REQ-048.A2, REQ-001.A2 | `.swdev/staged/WO-009/carriers/app/src/test/.../release/ReleaseEvidenceTest.kt` (3 tokens: lines 120, 131, 140) | c8b33dc04f96af343a105dff08165ce4870b7e9f6fbe43126696622a4ca7cff1 | equals scratchpad `wo9_carriers_sha.txt`, `tasks.md` CR-8-FIX row, T&V9 | T&V9: red 3/3 at :76 (status must be `done`), green 3/3 on owner-style evidence, fail-loud on empty labels, an ads label, a `TBD-OWNER` link, a wrong title, a missing file |

Both carriers exist in `carriers/` only. Exit conditions are real and observable (T&V9 section 5): A1 and 039.A2 need the owner's `mark` for every shipped puzzle (V-09 adds the ledger and STALE checks); 048.A1 and 001.A2 need a `done` row with evidence and an explicit `labels:` statement; 048.A2 compares the live title with the text REQ-048 holds now, so it stays red until the CA-12 re-lock (not a vacuous pass).

## (b) Token audit

**Isolation table.** I grepped every dotted token for the WO-009 IDs over all `.kt`, `.py`, `.kts`, `.xml`, `.json`, `.java` files outside `build`, `.git`, `.gradle`, `Study` and `Requirements`:

| Token | Found in | Table says | OK |
|---|---|---|---|
| REQ-042.A2, REQ-042.A3 | `content/src/test/.../release/ReleaseLibraryTest.kt` only | the same | yes |
| REQ-048.A3 | `app/src/androidTest/.../held/HeldInstalledPermissionsAppTest.kt` line 42 only (once) | the same | yes |
| REQ-010.A2 | the same class, line 24 (once), plus the WO-006 held copy under `.swdev/heldout/WO-006` | the same class | yes |
| REQ-042.A1, REQ-039.A2 | **only** the staged `ReleaseLibraryReviewedTest` | staged only | yes |
| REQ-048.A1, REQ-048.A2, REQ-001.A2 | **only** the staged `ReleaseEvidenceTest` | staged only | yes |

- **No waived token anywhere in the tree** (`src`, `tools`, `.swdev/verifiers`, `.swdev/heldout`): zero hits outside the two carriers. The only other places a waived ID appears with a dot are prose (`.md`) and logs. `release/release-checklist.md` writes `REQ-048.A1` and so on as row ids; `.md` is not a test file and trace-check does not count it (the RED list proves it, N5).
- **No token on a fixture or draft test**: `ListingDraftTest`, `PrivacyTextContainsTest`, `ReleaseChecklistTest`, `tools/tests/*`, `test_v09_acceptance.py`, `v09_release_library.py`, `puzzle_review.py` and `zip_same_content.py` carry no REQ token (grep empty). They carry `decision DA-n` markers (`ReleaseChecklistTest` lines 88 and 101: DA-158).
- **`ReleaseLibraryTest` mixed class:** the two token tests assert the criterion on the real library; the `control_*` tests are fixture controls and carry no token.
- **Held file:** `.swdev/heldout/WO-009/.../HeldInstalledPermissionsAppTest.kt` is `cmp`-identical to the in-tree file; sha256 d09828800eba0a54b6037aa207ef6f13fed178cc1af84dfb347cbaadce32bc76 equals the T&V9 value. The pre-overlay base (bf975022...) is recorded in T&V9.
- **Staged copies:** every non-carrier file under `.swdev/staged/WO-009/` (16 files) is `cmp`-equal to its in-tree twin, so the move-in matches what was staged. T9d's edits to token-carrying tests (`BrowsingAppTest`, `PlayThroughAppTest`, `ShowSolutionAppTest`, `PromiseWalkAppTest`, `HeldSolvedFindAppTest`, `BrowseDeviceTest`) add no token of the WO-009 IDs. Counts of earlier IDs were not changed by me to check, and trace-check shows no earlier ID turned RED.

## (c) Orphans and D3

| Item | Anchor |
|---|---|
| `tools/puzzle_review.py` + `tools/tests/test_puzzle_review.py` | DA-156, TASK-092, REQ-042 A1 / REQ-039 A2 (the review record) |
| `tools/zip_same_content.py` + its tests | DA-159, TASK-096, REQ-048 A3 / C8 (signed = scanned) |
| `.swdev/verifiers/v09_release_library.py`, `test_v09_acceptance.py`, V-09 cases in `test_verifiers.py` | DA-157, TASK-093, REQ-042 A1-A3, REQ-039 A2 (release gate) |
| V14 in `validate_puzzles.py` + `test_art_colours.py`, `Spec/03` | DA-162, TASK-091-0, REQ-039 rule |
| V15 + `test_no_holes.py`, `Spec/03`, `V15_EXEMPT` (3 warm-ups by id) | DA-169 (logged at MOVE-DEV9), TASK-091e, REQ-038 / REQ-011 / REQ-039 |
| `app/build.gradle.kts` input `releaseDocs`, `.gitignore` signing block | DA-159, DA-160, TASK-090 (build-file SHAs recorded) |
| Icon resources: `mipmap-anydpi/ic_launcher(_round).xml`, `drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`, manifest `icon` / `roundIcon` | DA-164, DA-167 (the owner picked option A), TASK-098 / 098b. The `0417` and devtools grep over `res` is empty |
| `release/icon-options/*` | DA-153, DA-164, TASK-097 |
| 12 new puzzles (`animals-rabbit`, `-bird`, `-fish`, `people-runner`, `-person`, `things-candle`, `-key`, `vehicles-rocket`, `-car`, `nature-tree`, `-flower`, `-cactus`) | DA-155, TASK-091a-c; 13 + 12 = 25; each has `title.en` and `title.fi`, `author ai:claude`, `reviewedByHuman: false`; `tools/golden/geometry.json` holds 25 puzzles |
| `release/store-listing.md`, `privacy-policy.md`, `release-checklist.md` | DA-160, TASK-094 / 094c |
| `reviews/WO-009-owner-finnish-list.md`, `reviews/WO-009-picture-changes*` | TASK-094b, TASK-091-0 (the owner's change list) |
| T9d edits and `GridScroll.kt` | DA-163 (derived pins, corrected grid loops) |
| Re-rendered previews, `Tangrams/README.md`, `Spec/03-puzzle-format.md`, `tools/golden/geometry.json` | generator output (AGENTS rule 8), TASK-091 batches |

No WO-009 code without a REQ, task or DA. D3: no abstraction with one caller and no future-only hook found. `V15_EXEMPT` is by id (never by kind), as DA-169 requires. D5: no new library or module (bundle task is AGP's own, no `bundletool` row).

## (d) Contract

| Check | Result |
|---|---|
| Baseline re-hash vs `.swdev/contract-baseline.json` | **72 of 72 equal, 0 missing** (also equal after CRLF normalisation; none needed it) |
| `Tangrams/puzzle.schema.json` | in the baseline, equal; mtime 2026-09-28 |
| `IPuzzleLibrary.kt`, `Puzzle.kt`, `IProgressStore.kt`, `SavedGame.kt` | in the baseline, equal |
| v1 save format | no file under `store/` or `contracts/` is newer than 2026-10-04 12:00; the frozen fixtures and `FrozenV1FixtureTest` are in the baseline and equal. Governance row 11 not triggered |
| G-04 grep over set S (7 product modules, `settings`, `time` `src/main`, `app/src/release`, `app/build.gradle.kts`, `settings.gradle.kts`) for `devtools` and `0417` | **exactly 2 lines**: `app/build.gradle.kts:49` and `settings.gradle.kts:25` (the DA-89 lines) |
| V-06 | PASS |
| Manifest | unchanged in kind: only `tools:node="remove"` for the injected permission, no permission, no network. V-01 PASS |

## (e) Owner floors

| Floor | Result |
|---|---|
| `reviewedByHuman` in all 25 real puzzle files | **false in all 25** (a recursive read of every `provenance`, 0 `true`) |
| `release/puzzle-review.json` | absent |
| Signing material in the tree | none: no `*.jks`, `*.keystore`, `keystore.properties`, `*.p12`, `*.pk8`, `*.pepk`, `encrypted_private_key*` or `*.pem` outside `build` and `.git`. `.gitignore` carries all of them with the DA-159 comment |
| Manual checklist rows | **23 of 23 manual rows `waiting-for-owner` with an empty evidence cell** (no `release/evidence/` exists). `ReleaseChecklistTest` enforces both |
| Listing placeholders | `privacy-policy-url`, developer and contact are `TBD-OWNER` (3 occurrences); no version line |

## (f) Evidence lines in `tasks.md`

| Evidence | Where | Re-checked by me |
|---|---|---|
| V-01 | TASK-090, TASK-098, TASK-099 (4) | PASS |
| V-04 and `--positive-control` | TASK-099 (4, 7) | both PASS |
| V-08 and `--expect-debug` | TASK-099 (4, 7) | both PASS |
| V-09 strict (RED as designed) and `--pre-review` | LAND-c, TASK-099 (2, 3, 4) | FAIL `0 of 25 reviewed` exit 1 / PASS exit 0; same on the universal APK |
| Real-file controls for V-09 on a scratch copy | TASK-099 (3) | agrees with T&V9 and `test_v09_acceptance.py` 34 OK; the real tree has no ledger and no flag `true` |
| Device channels | MOVE-DEV9: API 37 gate 10, play 188, browse 31, settings 14, time 8, app 146, devtools 13, API 26 the same, resets clean; run 1 failures (3, all `people-person`) read against the geometry and routed to 091e (V15) | consistent with T&V9 |
| API 26 release launch | TASK-099 (6): status ok, `MainActivity` resumed, 0 FATAL, no permission entries; `reviews/screens/WO-009-release-api26.png` | file exists; permissions confirmed by V-08 PASS |
| Signing control | TASK-099 (5): `jarsigner` with the local debug key on a scratch bundle, `zip_same_content` exit 0, one changed entry exit 1 | `zip_same_content` tests OK (17 + duplicate-entry case) |
| JVM | 835 of 835 (kernel 168, content 52, store 68, play 234, browse 74, settings 61, time 65, app 82, devtools 31) | equals T&V9's measured table |
| Universal APK | bundle 36f78a73..., universal 29a27e97... | universal sha256 re-read by V-08: 29a27e974f695d44... |

## Findings

**B (blocking): none.**

**S (should fix, bookkeeping, no product or test impact; all CLOSE9):**
- **S1: the 12 machine rows of `release/release-checklist.md` are all `waiting-for-owner` with empty evidence**, although TASK-099 and T&V9 recorded REQ-048.A3, REQ-042.A2/A3, C8, V-01, V-04, V-08 and V-09 as run. The workorder DoD asks for "every machine line green", and TASK-094 says machine rows are "filled at TASK-099". The evidence is in `tasks.md`, not in the checklist. CLOSE9 should mark the machine rows `done` with the results, or state that they are re-run at RELEASE-DAY. `ReleaseChecklistTest` does not constrain machine rows, so no test is affected.
- **S2: `workorders/WO-009.md` carries no close entries yet**: the Waivers row (DA-158 with its exit conditions and the two full sha256 values), the DoD boxes, the Station ledger rows for Acceptance Test Author, Slice Implementers and this audit, the Log after the G3 line, the release-safety lines and the Metrics are blank or open. `STATUS.md` and `build-map.md` are CLOSE9's list. This is the same shape as WO-008's S2.
- **S3 (T&V9 S1, carried, routed):** `ReleaseLibraryReviewedTest` reads the `reviewedByHuman` flag only; the ledger and STALE protection live in V-09. RELEASE-DAY step 5 already runs V-09 strict before the carrier moves in (step 6). CLOSE9 and the checklist wording should say so, as DA-170 notes.

**N (notes):**
- **N1:** the `tasks.md` TASK-T9c row still shows the earlier `ReleaseEvidenceTest` prefix `7fc80cc5` and "red at :73". The file is `c8b33dc0...` (red at :76) after the CR-8 N2 fix; the CR-8-FIX row has the right full value. CLOSE9 records both full sha256 values (CR-8 N4, T&V N1).
- **N2:** the checklist has 35 rows, 23 manual and 12 machine. The CR-8-FIX row says "+24 rows" over an original of 12, which would be 36. Cosmetic, not verified which row the count meant.
- **N3:** `.swdev/contract-baseline.json` has an mtime of 15:32 today while `delta_refrozen` still names only `build-map.md` at 08:24 (the WO-008 close). All 72 hashes equal the current files, so there is no contract change. Likely a rewrite by a trace-check run. No guard `contract-delta` after 08:24.
- **N4:** `app/build/outputs/apk/debug/app-debug.apk` moved from 17:37 to 17:49 while I was running, so a build was in progress elsewhere. My V-04 positive control and V-08 `--expect-debug` ran on the debug APK as it stood, and both PASS. The universal release APK is unchanged (29a27e97..., 17:37).
- **N5:** `release/release-checklist.md` and other `.md` docs write the waived ids with a dot as row ids and prose. trace-check ignores `.md`, and the RED list proves it, but any future scan of `.md` files would count them. The test input `releaseDocs` reads these files only as data.
- **N6:** `tools/tests/test_prototype.py` needs `playwright`, which this Python lacks (the project uses the scratchpad Playwright venv). All other `tools/tests` pass. Environment only; the prototype test is AGENTS rule 10's, not WO-009's.
- **N7:** `Tangrams/README.md` rows for the 12 new puzzles were added at 094b (confirmed by the count check recorded there); `people-person` and `things-candle` silhouette doubts are listed for the owner at checkpoint 9 (CR-8 N1).

## Handoff

```
trace-audit: WO-009 -> GREEN for WO scope, with the DA-158 waiver
trace-check: exit 1 (RED), exactly the five waived IDs: REQ-001 A2, REQ-039 A2, REQ-042 A1, REQ-048 A1, REQ-048 A2; no other RED
IDs: REQ-042.A2, A3, REQ-048.A3, C8 linked and tokened (4 of 4); C11 -> staged ReleaseEvidenceTest
waived carriers: 2 of 2 staged, full sha256 match (31c3bef5..., c8b33dc0...), red and green proven at T&V9
tokens: isolation table holds; 0 waived tokens in tree; 0 tokens on fixture or draft tests; held file byte-identical (d0982880...)
orphans: 0; D3: 0 unjustified; D5: none
contract: 72/72, schema and IPuzzleLibrary unchanged, v1 save format untouched, G-04 = 2 DA-89 lines, V-06 PASS
owner floors: reviewedByHuman false in 25 of 25, no puzzle-review.json, no signing material, 23 of 23 manual rows waiting-for-owner with empty evidence
verifiers (re-run): V-01, V-04 (+PC), V-05, V-06, V-07, V-08 (+expect-debug) PASS; V-09 strict FAIL 0 of 25 (as designed), --pre-review PASS
findings: 0 B / 3 S / 7 N  (S1 machine checklist rows not marked, S2 workorder close entries blank, S3 library carrier reads flags only (routed); all CLOSE9)
```
