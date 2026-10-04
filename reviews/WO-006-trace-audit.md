# WO-006 traceability audit

Auditor: Traceability Auditor, 2026-10-04. Read-only run. No Gradle, no emulator, no git. Format follows `reviews/WO-005-trace-audit.md`. The only thing written is this file.

**Verdict: WO-006 scope GREEN.** All 13 IDs have a token carrier under the code home `app`. There is no orphan code, no drift, and the contract baseline re-hashes 72 of 72. The G-04 grep finds exactly the two DA-89 lines. 0 B / 1 S / 8 N. The one S is close-time bookkeeping, not a linkage gap.
Test & Verify is PASS (`reviews/WO-006-test-verify.md`), so unlike WO-005 there is no open T&V condition.
One caveat that travels with the verdict: REQ-035.A2's device evidence is API 37 only (DA-98). The API 26 pass is rule-level and must not be counted as criterion evidence.

## Run
`trace_check.py --project .`: 51 REQs (47 locked, 4 withdrawn), 188 test files scanned, req-lint OK (0 errors, 5 warnings), views fresh, 72 contract files. **No drift section, no baseline-delta line.** Project-wide RED as expected.
The fail list is 13 REQs: 001 (A2 only), 005, 009, 029, 030, 031, 032, 033, 034, 039 (A2), 042, 048, 049.
- Of the 13 WO-006 IDs, **none is in the list.** The only in-scope REQ shown is **REQ-001 A2**, which is carried out (C11, WO-009) and was never one of the 13.
- The remaining 12 REQs belong to WO-007, WO-008 and WO-009.
- The count is down from 18 at TASK-048, because the 5 held-out IDs have moved in. The TASK-048 note "in-scope uncovered = 5 held-out + REQ-001 A2" is therefore now stale (see CLOSE6).
- One harmless Python `DeprecationWarning` from `trace_check.py:381` (not mine to edit).

Re-run by me (pure Python):
- `.swdev/verifiers/test_verifiers.py`: Ran 79, OK (this includes `TestV08PromiseApk`).
- `tools/tests/test_device_reset.py`: 43 tests, OK.
- `tools/compare_kits.py .swdev/heldout/WO-006 .`: 9 of 9 `equal`.
- The held-out files in `.swdev/heldout/WO-006/.../acceptance/held/` against the in-tree copies by `cmp`: all 14 identical (5 test files + 9 kit files).

## (a) Per-ID chain
Task chain from the `tasks.md` WO-006 section. All ids are `TASK-…` unless noted.
Common spine: TASK-040 / 040b (build inputs), 041 (debug seam), 042 (device reset), T6a (visible kit), T6b (held-out kit), MOVE-DEV6 (device run), T&V6 (held-out first run).
Code home for the ID table is `app`. `play` appears only for the REQ-037 gap (043, DA-100(i)).

| ID | Task chain | Code (home) | Token-carrying test (under `app`) | Device channels | Note |
|---|---|---|---|---|---|
| REQ-006.A1 | T6a, MOVE-DEV6; C10 carried | `app` shell (`MainActivity`, `TangramApp`), `play` | `app/src/androidTest/.../acceptance/layout/PlayThroughAppTest` line 70 (V; three windows) | API 37 + API 26 | real touch solve, Retry, Restart, Next, browse, grid. Settings step carried (C10) |
| REQ-035.A1 | T6a, 043 (gap), MOVE-DEV6 | `app`, `play` `PlayLayout`/`TrayRules` | `…/layout/PhoneLayoutAppTest` line 42 (V) | API 37 + API 26 | 7 cells, rows 3 + 4, no scroll action. `TraySizeSweepTest` is scaffolding (no token) |
| REQ-035.A2 | T6b, MOVE-DEV6, T&V6 | `app`, `play` (bottom margin, no change needed: 045 not triggered) | `…/acceptance/held/HeldGestureInsetAppTest` line 39 (H) | **API 37 = evidence** (gesture mode forced, exclusion 80 px > 0). **API 26 = rule-level only** ("tray above the system bar; A2 not applicable on this API", DA-98) | DA-98 reading: the bottom system area. S1 below |
| REQ-036.A1 | T6a, MOVE-DEV6 | `app`, `play` `TrayRows.TABLET` | `…/layout/TabletLayoutAppTest` line 32 (V) | API 37 + API 26 | one row, tops equal, lefts increasing, 1280x800 and 800x1280 |
| REQ-036.A2 | T6b, T&V6 | `app` `AppViewModel` (no `configChanges`, DA-97) | `…/held/HeldRotationAppTest` line 42 (H) | API 37 + API 26 (API 26 needed DA-111: app launched before rotating) | real rotation, 3 placed pieces keep their colour at the centroid, 4 stay in the tray. Time carried (C9) |
| REQ-037.A1 | T6b, 043, 040, T&V6 | `app`, `play` gap 12 dp | `…/held/HeldTouchTargetAppTest` line 37 (H; 5 windows, fi, font 2.0) | API 37 + API 26 | 30 dp negative and 48 dp positive control in `HarnessScaffoldingTest`. `HeldTouchTargetNativeWindowAppTest` (same file, no token) is a matrix test. Screens carried (C5, C6) |
| REQ-047.A1 | T6a, 041, MOVE-DEV6 | `app` `LocaleOverrideActivity` (debug seam), strings in every module (G-05) | `…/acceptance/language/LanguageFinnishAppTest` line 53 (V) | API 37 + API 26 | settings carried (C1). V-05 and `UntranslatedStringsTest` (DA-103) are the static side |
| REQ-047.A2 | T6b, 041, T&V6 | same | `…/held/HeldLanguageSwedishAppTest` line 28 (H; `sv-SE`) | API 37 + API 26 | settings, free note, privacy carried (C2) |
| REQ-001.A1 | T6a, 044, 048 | `app` (system level); V-01, V-08 | `…/acceptance/promise/PromiseWalkAppTest` line 108 (V) | API 37 + API 26 | shares one `hitsIn` with REQ-008.A1 (N3). Static: `PromiseSourceScanTest` (guardrail G-01). Artifact: V-08. C3, C4 carried |
| REQ-008.A1 | T6a, 044 | same | `PromiseWalkAppTest` line 121 (V) | API 37 + API 26 | as above |
| REQ-008.A2 | T6a, 044 | same | `PromiseWalkAppTest` line 131 (V) | API 37 + API 26 | no extra compose root; the rating words are asserted to be in the shared list |
| REQ-010.A1 | T6a, 048 | `app` (no network code, no permission) | `…/promise/AirplaneModeAppTest` line 43 (V) | API 37 + API 26. **Real airplane mode only on API 37** (`cmd connectivity airplane-mode`) | API 26: flag + `svc data disable`; `svc wifi` is killed (rc 137), so the radio is not cut. The claim rests on the no-INTERNET argument (A2 held test, V-01, V-08, source scan). The helper, header and design say so. C7 carried |
| REQ-010.A2 | T6b, 044, 048, T&V6 | `app` manifest (release: V-01) | `…/held/HeldInstalledPermissionsAppTest` line 18 (H) | API 37 + API 26 | target package, `requestedPermissions` and `permissions` empty, control on package `android`. **Artifact evidence, no token (DA-104):** V-01 PASS, V-08 PASS (release), API 26 release `dumpsys package` (no requested or install permissions). C8 carried |

**Result: 13 of 13 IDs have a token under a valid home.** None is carried by `play` alone, and none is without a device run. Both channels ran every device test: visible 75 on each (play 115, browse 25, devtools 13), plus the held-out 11 cases on each.

Read as DA-98 says: REQ-035.A2 is satisfied at criterion level on API 37 only. REQ-010.A1's API 26 airplane run is device behaviour only, not radio-off evidence.

## (b) Carried parts C1–C11, and what `build-map.md` has now
Each is named with its target WO and re-verification token in `designs/WO-006-design.md` (table, lines 20–32) and in `tasks.md` ("Carried OUT (F11)", lines 333–344). The two lists agree line by line. T&V found them also in the test headers: C1 and C2 in the language tests, C3 and C4 in `PromiseWalkAppTest`, C7 in `AirplaneModeAppTest`, C5 and C6 in the touch-target KDoc, C8 and C11 as WO-009 evidence.

| # | Carried part | Target | Token |
|---|---|---|---|
| C1 | REQ-047 A1 on the settings screen | WO-007 | REQ-047.A1 |
| C2 | REQ-047 A2 on settings, the free note, the privacy text | WO-007 | REQ-047.A2 |
| C3 | REQ-001 A1, REQ-008 A1–A2 on "every screen" (settings, free note, privacy, reset; REQ-009 and REQ-049 texts exempt by key) | WO-007 | same three |
| C4 | REQ-001 A1, REQ-008 A1 on the timer, play time, best times | WO-008 | same |
| C5 | REQ-037 A1 on the settings controls | WO-007 | REQ-037.A1 |
| C6 | REQ-037 A1 on the timer and `time` controls | WO-008 | REQ-037.A1 |
| C7 | REQ-010 A1 "every feature": sound, haptic, reset | WO-007; play time and best times WO-008 | REQ-010.A1 |
| C8 | REQ-010 A2 on the store-bound artifact | WO-009 | V-01 + V-08 (no token; REQ-010.A2 stays with the held test) |
| C9 | puzzle time across rotation | WO-008 | its own REQ-029 / 030 / 031 tokens, never REQ-036.A2 |
| C10 | REQ-006 A1: the settings overlay, rotating with it open | WO-007 | REQ-006.A1 |
| C11 | REQ-001 A2 store labels | WO-009 | release checklist (decisions F17); REQ-001.A2 |

`build-map.md` (v1.4, the baseline-frozen file) **does not have them.** Its WO-006 row reads only "out -> WO-009: REQ-001 A2 (store labels)". The WO-006 file's "Carried OUT" bullet is also shorter than the design. The design says CLOSE6 writes them (design line 35). Exactly what to write is in the CLOSE6 list below, item 1.
WO-009's "Carried in / out" cell reads "in <- WO-002, WO-006", which is already right. WO-007 and WO-008 have **no** "in <-" cell for WO-006. Item 1 adds them.

## (c) Orphan check
Files changed since the build-map freeze (by mtime, no git): exactly the set below, in the main and build code. No other main, release or debug file was touched. No string resource changed, which fits "no behaviour change" for language.

| File | Anchor | Justification |
|---|---|---|
| `app/src/main/.../MainActivity.kt` | TASK-040, DA-102, seam row 1 | `class` to `open class`, body unchanged. The only reason for the abstraction is the debug subclass; an access flag only |
| `app/build.gradle.kts` (`scannedFileGlobs`, `repo.root`) | TASK-040 / 040b, DA-88 pattern, DA-102, G-01 | the unit-test inputs of `PromiseSourceScanTest`; globs only, no module name, the one `devtools` line is DA-89 |
| `app/src/debug/.../TestConfig.kt`, `LocaleOverrideActivity.kt`, `app/src/debug/AndroidManifest.xml` | TASK-041, DA-102, DA-103, REQ-047 test seam | debug source set only, `exported="false"`, no intent filter. Header cites DA-102 and the no-`configChanges` dependency (CR-5 N15) |
| `play/.../PlayLayout.kt` (`gap`) | TASK-043, DA-100(i), REQ-037 A1 | one constant, 14 to 12 dp. The 600 dp breach was proved first by `TraySizeSweepTest`. No in-file anchor comment (N4) |
| `tools/device_reset.py` + `tools/tests/test_device_reset.py` | TASK-042, DA-108 | device hygiene, with a self-test |
| `tools/compare_kits.py` | TASK-T6b, plan review F3 / E2 | prints `equal` or `differs` only |
| `.swdev/verifiers/v08_promise_apk.py` + `TestV08PromiseApk` | TASK-044, DA-104, G-01, REQ-001 / 008 / 010.A2 | dex `class_defs` reader, fail-closed |
| Test side (`app/src/androidTest/.../acceptance/{layout,language,promise,held}`, `app/src/test/.../promise`, `…/language`, `play/src/test/.../TraySizeSweepTest`) | TASK-T6a / T6b / T6c | acceptance, scaffolding and guardrail tests, all named in the tasks |

D3 (unjustified abstractions): **none.** Every seam is in the design's frozen seam table and in a task: `open`, `TestConfig` / `LocaleOverrideActivity`, the gap, `device_reset.py`, V-08, `DisplaySpec` / `DisplayRule`, the walks and helpers. The 9 kit copies in `acceptance/held/` next to `acceptance/layout/` are duplication by design (the held-out kit precedent), and `compare_kits.py` proves them equal. The marker `UsesDisplayRule` is used by 12 files.

## (d) Token audit
- Exactly **13 claim tokens, one per ID** (`REQ-NNN.An`), each on its own file and each followed by a real `@Test`: 006.A1, 035.A1, 035.A2, 036.A1, 036.A2, 037.A1, 047.A1, 047.A2, 001.A1, 008.A1, 008.A2, 010.A1, 010.A2. No token for REQ-001.A2 anywhere (correct, carried).
- No dotted token exists in any of the ten named non-claim files: `HarnessScaffoldingTest`, `GestureInsetProbeScaffoldingTest`, `TabletSmallestWalkAppTest`, `LanguageListDecisionAppTest`, `TraySizeSweepTest`, `PromiseSourceScanTest`, `UntranslatedStringsTest`, `PromiseWordListEqualityTest`, `LocaleSeamSmokeScaffoldingTest`, `HeldTouchTargetNativeWindowAppTest` (the last is a second class inside `HeldTouchTargetAppTest.kt`; it has no token of its own).
- Space-form prose ("REQ-035 A2 not applicable on this API" in `GestureInsetProbeScaffoldingTest`; "REQ-035 A1 / REQ-036 A1 row counts" in `TraySizeSweepTest`) does **not** match trace-check's token regex `\b(?:REQ|TEST)[-_](\d+)[._\-]?A(\d+)\b`. Not a token.
- Each scaffolding, decision or guardrail file carries its marker: `// decision DA-nn` or `// guardrail G-01`. Checked head of each.
- Decision and guardrail coverage, DA-96…112 (a `decision DA-n` tag, a guardrail tag or a verifier test):
  - 96 `HarnessScaffoldingTest`, `DisplaySpec`.
  - 98 `GestureInsetProbeScaffoldingTest`, `HeldGestureInsetAppTest`.
  - 99 `HarnessScaffoldingTest` (30 dp control), `TabletSmallestWalkAppTest`.
  - 100 `TraySizeSweepTest`, `TabletSmallestWalkAppTest`.
  - 101 `LanguageListDecisionAppTest`.
  - 102 `LocaleSeamSmokeScaffoldingTest`, `HarnessScaffoldingTest`.
  - 103 `UntranslatedStringsTest`, `LanguageFinnishAppTest`.
  - 104 `v08_promise_apk.py` + `TestV08PromiseApk` (79 OK).
  - 105 `AirplaneModeAppTest`, `AirplaneMode`.
  - 106 `PromiseSourceScanTest`, `PromiseWordListEqualityTest`, `PromiseWords`.
  - 107 `PlayThroughAppTest`.
  - 108 `test_device_reset.py`.
  - 97 is covered through REQ-036.A2 (`HeldRotationAppTest`) and the no-`configChanges` KDoc in `LocaleOverrideActivity`.
  - 109, 110, 111 and 112 are review and failure-routing rulings with no code of their own. Their fixes are inside the tests that carry 99, 103 and 96.
  - **No gap.**
- The duplicate copies under `.swdev/staged/WO-006/` and `.swdev/heldout/WO-006/` are not test homes and add no claim.

## (e) Contract integrity
- trace-check **reports no drift and no notify line.**
- All 72 files in `.swdev/contract-baseline.json` (`frozen: 2026-10-03`, `delta_refrozen: build-map.md 2026-10-04T08:34:30`) re-hashed with Python SHA-256: **72 of 72 match, 0 mismatches, 0 missing.**
  - `build-map.md` is `172acc2c…`, as in the baseline. It is unchanged since the WO-005 re-freeze, which is the one logged `contract-delta` in `guard-log.jsonl` (08:34:30).
  - `IProgressStore.kt` `0353e36e…`, `IPuzzleLibrary.kt` `4c4f4f3a…`.
- LOCK-V1: `progress-v1.json` `ab1c9d1e…`, `progress-v1-fresh.json` `2be7af50…` and `FrozenV1FixtureTest.kt` are all equal to the baseline. The v1 save format is **unchanged**. No `store` main file changed, and rotation uses the existing ViewModel (DA-97).
- `IPuzzleLibrary` and `IProgressStore` are **not changed** (hashes equal).
- WO-006 "Contract deltas" table is empty: consistent. **No re-freeze is needed by this WO's code.** The only pending delta is CLOSE6's own `build-map.md` Edit, which is a logged notify-tier edit followed by a re-freeze (CLOSE6 item 1).

## (f) G-04 grep
`grep -rniE "devtools|0417"` over S = `<m>/src/main/**` (kernel, contracts, content, store, play, browse, app) + `app/src/release/**` + `app/build.gradle.kts` + `settings.gradle.kts`:
- `app/build.gradle.kts:47: debugImplementation(project(":devtools"))`
- `settings.gradle.kts:25: include(":devtools")`

**Exactly the two DA-89 lines.** `app/src/release` has no hit.
`LocaleOverrideActivity` and `TestConfig` appear only under `app/src/debug` (the two classes and the manifest entry) and `app/src/androidTest` (the tests and the kit). No hit in `main`, `release`, any other module, `tools` or the build files.
The hits in `androidTest` (16 files) are all test or kit files.

## Findings
- **B (blocker): none.**
- **S1 (should): REQ-035.A2's evidence is API 37 only.** DA-98 and T&V S1 say so. The API 26 pass is rule-level ("not applicable on this API"). Keep the "API 37 only" line in the close write-up (build-map #Layout note, workorder log, checkpoint 6) so the green is not read as two-channel evidence for this one ID. Not a linkage gap. No later WO re-runs this.
- **N1.** REQ-010.A1 on API 26 shows device behaviour only: `svc wifi` is killed (rc 137), so the radio is not cut. The REQ-010.A1 claim rests on the no-INTERNET argument (A2 held test, V-01, V-08, source scan). It is stated in the test header, the helper and the design. Keep it in the close note.
- **N2.** `tasks.md` TASK-048 still says "in-scope uncovered = exactly the 5 held-out IDs + REQ-001 A2", and "18 REQs uncovered". After the move-in the live figure is **REQ-001 A2 only** and 13 REQs project-wide. Bookkeeping, the orchestrator's. Also: the `tasks.md` header still reads "WO-006 planned" (Status line 3) and the AUDIT6 row reads "in progress".
- **N3.** REQ-001.A1 and REQ-008.A1 share one walk and one `hitsIn(seen)` assertion (REQ-001.A1 adds the "locked" check). Legitimate (A1 of REQ-008 is a subset), but the two tokens are close to one assertion (T&V N2, CR-5 N14).
- **N4.** `PlayLayout.kt:127` (`gap(c)`) has no in-file anchor to DA-100(i) or REQ-037. The reason is in the tasks row and `decisions.md`. Add a one-line comment when `play` is next touched.
- **N5.** The touch-target walk allows 1 px of layout rounding (T&V N3), so a control of about 47.6 dp passes. The 30 dp negative control does not test that edge. Low risk.
- **N6.** `HeldTouchTargetNativeWindowAppTest` lives in `HeldTouchTargetAppTest.kt`, and has no token and no marker by design (T&V N4). It runs in the fallback's sixth call.
- **N7.** The 9 kit copies in `acceptance/held` and `acceptance/layout` are duplicated by design and compare-checked. After a harness change, `compare_kits.py` must be run before any held-out run.
- **N8.** `trace_check.py:381` prints a `DeprecationWarning` (`re.split` positional `maxsplit`). It is in the SWDev tool (read-only to me).
- Not mine to rule, but I checked them: the `+2 release` count was already corrected in the TASK-048 row (T&V N1); CR-5 N11 (stale rotation text in the design) is already written back.

## CLOSE6: exactly what is owed
Contract note: `build-map.md` is notify tier. Use the Edit tool (not a shell write), so the guard logs the contract-delta. Then re-freeze `.swdev/contract-baseline.json` for the new hash, as at WO-005.

1. **`build-map.md`**
   - **Header:** Status `Current (P3, WO-005 closed)` to `Current (P3, WO-006 closed)`; Version 1.4 to 1.5; last-updated date.
   - **§1 `#Layout` row:**
     - Status: `Built (WO-006, 2026-10-0x)`: REQ-006 / 035 / 036 / 037 verified on phone 390x844 and 360x780 and tablets 1280x800 / 800x1280 / 600x960, API 37 and API 26. Rotation keeps placed pieces through the ViewModel (no v1 change, DA-97).
     - Note: tablet tray gap 12 dp, not Spec's 14 (DA-100(i)); **REQ-035.A2 evidence is API 37 only** (DA-98); short windows under 549.3 dp tall are the REQ-013 / REQ-037 conflict (CA-7(b), owner item).
   - **§1 `#Language` row:** `Built (WO-006)`: fi and en verified by the walk, V-05 and `UntranslatedStringsTest`. No code change. Debug-only `LocaleOverrideActivity` and `TestConfig` (DA-102). Language-list option is an owner item (DA-101 / CA-8(a)); the 27 unreviewed Finnish keys are on `reviews/WO-006-owner-finnish-list.md`. Settings screen carried (C1, C2).
   - **§1 `#Promise` row:** Status `Built (WO-006)`. Replace "WO-006 (REQ-009 in WO-007)" with: REQ-001 A1, 008, 010 at app and release level; **V-08 live** (release APK: no SDK class definitions, no debug-only class, no permission element, with a dex `class_defs` reader and canaries; `--expect-debug` positive control), plus `PromiseSourceScanTest` and the walks. Same style as the `#DevTools` row's "V-04 is live". REQ-009 stays WO-007.
   - Optionally add one phrase to the `(kernel)` / `#Solving` notes: tablet gap 12 dp.
   - **§2 WO-006 row, "Carried in / out":** replace "out -> WO-009: REQ-001 A2 (store labels)" with the full list C1–C11 from table (b), each by name, target WO and token.
     - Add to the **WO-007** row: "in <- WO-006: C1, C2, C3, C5, C7 (sound, haptic, reset), C10".
     - Add to the **WO-008** row: "in <- WO-006: C4, C6, C7 (play time and best times), C9".
     - The **WO-009** row already says "in <- WO-006" (C8, C11): name them.
   - **§3 change log:** a 1.5 row, "build status after WO-006 (pipeline-written, notify tier)", approved AI (checkpoint 6).
   - Also a device-tooling note: `tools/device_reset.py` is the hygiene script for every device step (before and after).
2. **`design-inputs.md` §2** (e.g. a new numbered item beside 5–7, or under "Superseded"): the tablet-gap departure. Spec/02 §3.4 and the prototype use a 14 dp tablet tray gap (the (n + 1)-gap formula); the build uses **12 dp** (phone stays 8 dp) so the square cell on a 600 dp-wide tablet is 49.3 dp, not 47.78 dp, meeting REQ-037's 48 dp. Cite DA-100(i), `TraySizeSweepTest`, and the owner item. Add a change-log line in §3 of that file. (TASK-043's "close note" is the source text.)
3. **`AGENTS.md`**
   - Line 132 ("Release safety at every WO close"): add V-08. Build the release APK, then run `python .swdev/verifiers/v08_promise_apk.py` (exit 0; it builds its own release APK) and, after a fresh debug build, `v08_promise_apk.py --apk app/build/outputs/apk/debug/app-debug.apk --expect-debug` (exit 0). Record both result lines in the workorder.
   - Line 133 ("Two device channels at every WO close"): add `python tools/device_reset.py --serial S` (reset, then `--check`) **before and after every device step**. A non-clean `--check` voids the step and is recorded in the workorder. Also note that `HeldGestureInsetAppTest` is evidence on API 37 only (DA-98).
   - Consider a lessons line: a harness defect found on the first device run goes to the test author as a corrected check (DA-110, DA-111), and `compare_kits.py` after any kit change.
4. **`workorders/WO-006.md`**
   - Status, `In progress` to closed.
   - Ledger rows still blank: Acceptance Test Author (T6a / T6b / T6c), Slice Implementers (040, 041, 042, 043, 044, 047), Code Reviewer (CR-5, `reviews/WO-006-CR-5-code-review.md`), Test & Verify (`reviews/WO-006-test-verify.md`, PASS), Traceability Auditor (this file).
   - Scope "Carried OUT": list C1–C11.
   - Contract deltas: the `build-map.md` edit with its date and the re-freeze (there is no code delta).
   - Waivers: none.
   - DoD boxes: tick them, and note on the verifier line that V-08 and the `--expect-debug` run are green and every `device_reset.py --check` was clean.
   - Metrics: tests (JVM 567 + device 228 per channel + 11 held), findings counts by station (design review 0/8/9, plan review 1/10/6, CR-5 0/4/17, T&V 0/1/5, this audit 0/1/8), corrections (DA-110, DA-111), and the device times.
   - Log: the close line and this audit's line.
5. **`tasks.md`:** header Status line (WO-006 closed); AUDIT6 row done, citing this file; TASK-048's "in-scope uncovered" note corrected to REQ-001 A2 only (13 REQs project-wide); CLOSE6 row done.
6. **`STATUS.md`, `progress.md`**, and **checkpoint 6** with the five owner items: DA-100(ii) / CA-7(b) (short windows), DA-101 / CA-8(a) (language lists), the tablet gap (12 dp), DA-96 (display-override harness; the tablet taskbar not covered), and the Finnish list (27 keys).
7. **Keep the evidence where trace-check cannot see it:** the V-01, V-04, V-08 and API 26 `dumpsys` lines are in the WO log (TASK-048 row) and are the only home for A2's artifact half. Keep them there (as N1 in the WO-005 audit).
8. **Re-freeze** `.swdev/contract-baseline.json` after item 1 (new `build-map.md` hash; record `delta_refrozen`).

**WO-006 scope GREEN.**
