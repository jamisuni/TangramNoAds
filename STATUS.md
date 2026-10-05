# STATUS: where TangramNoAds stands

**Updated:** 2026-10-05 (WO-009 closed) · **Phase:** requirements **locked** and accepted (G1); architecture locked (G2); **Kotlin + Compose build under SWDev: all nine WOs closed — release-ready, waiting for Jami.** Every puzzle (25 now), browsing, saved progress, the DEV aid, phone + tablet in Finnish and English, free and offline, the settings screen, active play time with best times and a timer, a launcher icon, and the Play release kit (store texts, privacy policy, release checklist, the review tool and gate); verified on Android 8 (API 26) and API 37; **42 of 47 locked REQs proven by tests**; the last 5 need the owner's puzzle review and the live Play listing (waived until RELEASE-DAY, DA-158).

## Snapshot
| What | State |
|---|---|
| Prototype | 0.6, `Spec/prototype/tangram-prototype.html` (generated from `tools/prototype_template.html`). Finnish or English by browser language. The 0.5 artifact "TangramNoAds Prototype" (https://claude.ai/artifact/7gLS46TqxRJXa214A8chva) is stale until republished |
| Puzzles | 13 in `Tangrams/`: 2 mini (3 pieces), 4 warm-ups, 7 full (house, arrow, cat, chocolate bar, sailboat, mountain, gift box). Every file has `kind` and both titles. All pass V1–V6, V8–V10, V12; none human-reviewed yet |
| Requirements | `Requirements/` v1.1: 53 features, REQ-001..051 (**47 `locked`**, signed by Jami 2026-10-01; 4 `withdrawn`); 51 of 53 features locked, 2 ideas, TYPE-001..007 (TYPE-002 withdrawn), SRC-001..015, DEF-001. Priorities not set |
| Tests | `tools/tests/test_prototype.py`: all 13 puzzles solve on phone 390×844 and tablets 1280×800 / 800×1280; tray, tap, twist, wrap, long-press jump, grid, Finnish, load check, mini-puzzle pulse, settings, DEV aid. All pass (2026-09-28) |
| Owner use so far | Web browser with a mouse (rounds 4–6). **No phone or tablet playtest yet** |
| Review | The agent's concept review (2026-09-28) was accepted as a whole in round 6; its accepted proposals are listed in `Requirements/evidence/src-011-conversation-round6.md` |

## Build progress (SWDev, 2026-10-04)
| What | State |
|---|---|
| G1 | collection v1.1 incl. `req_types.md` v0.2 accepted by Jami; fresh-eyes review `req_review_01.md` (38 findings, all triaged; capture-side actions CA-1…CA-4 waiting) |
| Toolchain | Android Studio JBR 25, SDK 37, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 — `.\gradlew.bat assembleDebug test` green on Jami's computer |
| Architecture | `architecture.md` v1.0 (G-01…G-10, ADR-001…006), modules per code home; `IPuzzleLibrary` locked, `IProgressStore` notify |
| WO-001 #Locking | **closed**: exact geometry kernel + TYPE-004 lock search + drop resolution (engine/state level); 116 kernel tests, 46 acceptance tests (15 held-out, all passed on first run); release manifest has no permissions and no backup |
| Reference fix | `tools/tangram_geom.outline_corners` now counts 180° pinch points as corners (DA-7): `shapes-warmup-4` gained the anchor (2,2); prototype rebuilt, its tests ALL PASS |
| WO-002 #Content | **closed**: `content` module — the 13 puzzles packaged from `Tangrams/` at build time, exact parser, ordered library behind the locked `IPuzzleLibrary`; validator verdicts in the golden; 31 content tests + 15 held-out (all passed) |
| WO-009 #Release | **closed 2026-10-05: release-ready, waiting for Jami.** Channel: Google Play; title **"Tangram Playtime"** (pending the CA-12 re-lock).
- **Library:** 25 puzzles (12 new AI drafts, 6 themes, the square), every one still `reviewedByHuman: false` until the owner plays and approves it; new validator rules V14 (colours) and V15 (no pockets).
- **Owner's review kit:** `release/review/app-debug-review.apk` + `release/puzzle-review-sheet.md`; approve with `python tools/puzzle_review.py mark <id>` in PowerShell or cmd.
- **Release gate V-09:** RED until the review (0 of 25 reviewed); its `--pre-review` mode is green.
- **Drafts:** `release/store-listing.md` (EN + FI), `release/privacy-policy.md`, `release/release-checklist.md` (35 rows: the machine checks recorded, every owner step waiting).
- **Icon:** option A (the coloured square), adaptive, fits every launcher mask.
- **Release safety:** the Play bundle's universal APK passes V-01 / V-04 / V-08; the API 26 release launch has no permissions.

Tests: JVM 835; device 410 on each channel; held-out first run 2/2 (REQ-048 A3), 0 corrections; trace GREEN for WO scope with the five owner-evidence IDs waived. Owner items in checkpoint 9 |
| WO-008 #PlayTime | **closed 2026-10-05**. The game now measures **active** play time. A second counts only within one minute of the last touch, and stops until the next press (the owner's wish).
- Today's and all-time play time count every active second in the app; a puzzle's own time counts only while it is in play, and pauses in settings, the grid and when browsing away.
- Each puzzle keeps its solve time and its fastest time as the best; slower re-solves keep the best; a DEV-aid solve sets no best.
- Settings: a timer switch (off on a fresh install), today, total and the best-time list. The timer is a small pill at the board's top-right that steps below Restart or DEV when they are there.
- Everything fits the frozen v1 save format: no migration.

Tests: JVM 816; device 329 on each channel; held-out 9/9 at the first run, **no corrections**; trace GREEN. Owner items in checkpoint 8 |
| WO-007 #Settings | **closed 2026-10-04**. A ⚙ (48 dp touch, 24 dp icon) opens the settings overlay:
- a sound switch; with sound off, no action makes a sound or a tick, and Android's own click sounds and long-press vibration are always off;
- a two-step reset that keeps the current puzzle, now New, and keeps the settings;
- a how-to, plus the free note and privacy text verbatim.

The five sounds are synthesised in code, so there are no audio files. The haptic tick needs no permission. V-08 now pins every sound and haptic caller in the APK. Tests: JVM 675; device 277 on each channel; held-out 4/4 (1 fixture corrected); trace GREEN. Owner items in checkpoint 7 |
| WO-006 #Layout, #Language, #Promise | **closed 2026-10-04**. The game is proven right on a 390×844 phone and on 1280×800 / 800×1280 tablets, by a screen-size override on the two emulators. It keeps the tray one row on tablets and every placed piece on rotation, gives every control ≥ 48 dp, shows Finnish on a Finnish device and English otherwise, and has no ad, price, money ask, rating prompt, permission or network on any screen that exists. One product fix: the tablet tray gap 14 → 12 dp, because the square's tray cell was 47.78 dp on a 600 dp tablet. **V-08** is new: release APK, no ad/billing/network SDK, no permission element. Tests: JVM 567; device 239 on each of API 37 and API 26 (incl. 11 held-out); held-out 5/5 first run, 0 corrections; trace GREEN. **5 owner decisions are open (below)** |
| WO-005 #DevTools | **closed 2026-10-04**: debug builds have a DEV pill in a free board corner. After passcode 0417 it shows each piece's solution place or solves the puzzle at once (Solved, no best time, the normal solved timeline). The release build has none of it, proven by V-04 on the release APK, with its positive control on a debug APK. The **API 26 waiver is exited** (DA-93): the release build launches on Android 8.0, and all device suites run on API 26 too. The first API 26 run found a real defect: Android 8 blurred every scaled path, so the drawing is now px-only (DA-92). Tests: JVM 547 *(correction 2026-10-04, WO-006 T&V N1: the "+2 release" were stale result files from 2026-10-03 09:15; `app` has no release unit-test task, and its only release-specific test was retired with DA-56)*, device 191 on each of API 37 and API 26, verifiers 52. Held-out: 1 fixture correction (DA-95), 0 escaped product defects; trace GREEN |
| WO-004 #Browsing + store | **closed 2026-10-03**: ‹ › with wrap, long-press › to the next unsolved puzzle, the all-puzzles grid with thumbnails, state text, Restart in a free board corner (DA-71), the solved bar (Retry / Next / best time); progress saved on the device in one versioned JSON file (`store`), survives closing the app; **v1 save format frozen** (LOCK-V1: a later change = migration hard-stop). Tests: JVM 469+22 held-out, device 159; held-out first run 6/6 with 0 escaped defects; trace GREEN (26/47 REQs) |
| WO-003 #Solving | **closed 2026-10-03**: the play slice (tray, drag, tap/twist turns, flip badge, landing preview, lock, corner pulse, solved picture with fade and confetti) + a minimal app shell. Tests: JVM kernel 127, content 46, play 146 (incl. 15 held-out), app 3; device (API 37) play 103, app 4 — the first puzzle is solved by real touch. Held-out first run 11/12 with 0 escaped product defects (the failure was a test defect, DA-45). Decisions DA-15…DA-45 + staffing rows; waiver DA-43 (API 26) |
| **First playable APK** | `app/build/outputs/apk/debug/app-debug.apk` (debug). Install it with `adb install -r` or from Android Studio. WO-005 adds the DEV pill (passcode **0417**) to check every puzzle. Browse every puzzle with ‹ › (long-press › jumps to the next unsolved one) and the all-puzzles grid; progress is saved on the device and survives closing the app (the WO-003 debug `puzzle` extra is retired, DA-56) |

## ▶ Resume here (all nine WOs closed; release-ready, waiting for Jami, 2026-10-05)
0. **There is no build work order left.** What remains is the owner's critical path, then RELEASE-DAY: a mechanical checklist run (`tasks.md` WO-009 "RELEASE-DAY"; `release/release-checklist.md`). Any non-mechanical change (a puzzle fix, a rejected mini or warm-up, a late text change) goes to a short follow-up WO.
1. **Owner's critical path (start the long ones now, in parallel):**
   - **Google Play account + identity verification + fee** (days to weeks).
   - **Developer contact, the privacy-policy host and URL, and the repository's visibility.** A public repo exposes `progress.md`, `Requirements/evidence/` and the git history with author emails.
   - **Review all 25 puzzles:** install `release/review/app-debug-review.apk`, solve each by hand, write approve / fix / reject in `release/puzzle-review-sheet.md`, then `python tools/puzzle_review.py mark <id> …` in PowerShell or cmd (Git Bash is refused). Doubtful figures to look at first: `people-person` (a stylised figure), `things-candle` (vase-like outline), the runner and the bird.
   - **Finnish confirmation:** `reviews/WO-009-owner-finnish-list.md` (12 new titles; the listing; the policy labels).
   - **The CA-12 re-lock** of REQ-048's title ("Tangram Playtime") in a SwReqCollector session; CA-10…CA-15 wait there too.
   - **Upload / signing key** (never in the repo), **version name and code**, the **Console entries and declarations**, the **closed test** (12 testers × 14 days for new personal accounts; only reviewed puzzles in any store-track build, DA-166), **production access** (~7 days), **publish**.
   - Optional: a deny rule in your Claude Code settings for the AI's edits of `Tangrams/*.json` and `release/puzzle-review.json` (offered, never applied by the AI; DA-168).
2. **RELEASE-DAY (when the review and account are done):**
   - Pre-publish: the last `mark` → remove rejects on the owner's word (then golden, `assembleDebug test`, V-04, V-09, a device smoke) → the version on the owner's word → rebuild the bundle + universal APK → V-09 strict / V-01 / V-04 / V-08 + the API 26 launch → move in `ReleaseLibraryReviewedTest` (compile outside the tree, sha256 31c3bef5… compared) → JVM + trace-check → the owner signs the scanned bundle, `zip_same_content` proves it equal → the owner uploads.
   - Post-publish: the owner supplies the live evidence (`labels: none` etc.) → `ReleaseEvidenceTest` moves in (sha256 c8b33dc0… compared; waits for CA-12) → JVM + trace-check.
   - The listing screenshots come from a debug-key copy after the review.
3. **Device channels and hygiene:** `Medium_Phone_API_37.0` and `Phone_API_26`, one at a time, with `python tools/device_reset.py --serial S` before and after every step.
4. **Owner decisions still open from earlier checkpoints:**
   - **Checkpoint 9 (WO-009):** the puzzle review; the doubtful figures; the Finnish list; the title's Finnish form; the icon at small size; the critical path above.
   - **Checkpoint 8 (WO-008):** one yes/no on a still held finger, a key press, and the counted minute; the timer's look and corner (it touches the outline on some short boards); the settings wording; the hours display; optional spoken timer label.
   - **Checkpoint 7:** the tones and tick; the after-reset puzzle (DA-120); the Finnish how-to and reset wording; the ⚙ size; platform sounds always off; keyboard Tab into settings.
   - **Checkpoint 6:** split-screen windows (CA-7(b)); `localeFilters` (CA-8(a)); the tablet gap 12 dp; tablets via display override; `reviews/WO-006-owner-finnish-list.md`.
   - The child's playtest. From before: DA-85; CA-5…CA-15 wait for a SwReqCollector session (CA-11 and CA-15 are future ideas: the safety rope; harder levels with non-standard pieces).
   - **Take a git snapshot**: WO-007, WO-008 and WO-009 are closed and uncommitted.

## Round history
| Round | When | What Jami said (evidence) | What changed |
|---|---|---|---|
| 0 | 09-27 07:31 | Traditional tangram, miniature tray at the bottom, phone + tablet layouts (SRC-001) | Study, spec draft 0.1, first prototype |
| 1 | 09-27 08:13 | Ideas: 8+, no free drops, tie to sides and pieces, ‹ › browsing, difficulty levels, active play time (SRC-002) | Slot-free anchor locking, draft 0.2 |
| 2 | 09-27 08:39 | No gift boxes / hidden anchors, no donations, "enjoy, it's absolutely free" (SRC-003) | Outline-corner anchors only, V11, draft 0.3 |
| 3 | 09-27 09:06–09:18 | Puzzles in their own folder, very easy starters, abstract shapes fine, other piece sets later; move into SwReqCollector (SRC-008) | `Tangrams/`, warm-ups, rectangle, `Requirements/` collection |
| 4 | 09-27 10:19 | No resizing (rule stays), show piece size in tray, spin icons in tray cells, a 3-piece puzzle (SRC-009) | S/M/L marks, ↺ ↻ buttons, mini puzzles (prototype 0.4) |
| 5 | 09-27 10:52–12:36 | Mirror-button question; keep ↺ ↻ for now; DEV cheat with passcode 0417 (SRC-010) | DEV solution reveal (prototype 0.5), handoff files |
| 6 | 09-28 06:29 | Concept review accepted: **remove the ↺ ↻ buttons** (tapping does it), **remove Easy / Medium / Hard**, flip badge always, store name **"Tangram, absolutely free"**, "I agree with all your proposals" (SRC-011) | Prototype 0.6; REQ-004/027/028/044 withdrawn; TYPE-004 one lock rule; wrap; local-midnight fix (DEF-001); `kind` field + V12; the classic square back (V11 was never a restriction); Finnish + English; #Release (privacy text, store checklist); long-press › and grid overview; load check; corner pulse in mini puzzles; haptic tick; REQ-042 = 20 puzzles |
| 7 | 10-01 18:08 | "plz lock all 47 requiments as approved" (SRC-014) | All 47 ai-approved REQs locked with Jami's sign-off; 13 inferred items confirmed as stated; collection v1.0 |
| 7b | 10-01 18:20 | "Plz do lock also" the features (SRC-015) | 51 features locked; the two ideas (#PieceSets, #Accessibility) stay ideas; collection v1.1 |

## What round 6 corrected
- **V11 cannot fail a valid tangram.** The uncovered region always has a convex corner, that corner is always an anchor (an outline corner or a placed piece's vertex), and the piece covering it must have a vertex there. "No hidden anchors" is a rule of the lock (REQ-019), not of the content. The Gift box (classic square) is back; `Study/06` and `Study/08` keep the old belief (frozen), the decision log in `Spec/05` records the correction.
- **Two prototype-vs-REQ disagreements** are settled: the list wraps (REQ-024 now says so); "Active today" resets at local midnight (DEF-001 fixed in 0.6).
- **REQ-037 vs REQ-044** (48 dp minimum vs 29 dp buttons) is gone with the buttons; the DEV button is exempt as a testing aid.

## Next steps
1. **Phone and tablet playtest by Jami** (open `Spec/prototype/tangram-prototype.html` on a device; a Finnish phone shows the Finnish text). Register what he says as the next SRC. The REQs are locked, so a finding becomes a defect (DEF-NNN) or a change delta (`Requirements/changes/CHG-NNN.md`), never a silent edit. Things to feel on a real screen: the lock distance (0.65 units, ≥ 30 dp), the landing preview's "silly but valid" spots, the flip badge next to the tray cell, the corner pulse in the mini puzzles.
2. **One open question** in `Requirements/requirements.md` §5: which developer account publishes (a personal Play account created after 13 Nov 2023 needs a 12-tester, 14-day closed test; F-Droid has no such rule). The Finnish store title was settled by the lock.
3. **Content toward 20 reviewed puzzles** (REQ-042): about seven more full puzzles over the four themes, warm-up pictures worth seeing (N6 in the review), and Jami's review of each (`reviewedByHuman`, with the DEV reveal).
4. **Priorities:** the lock is done (2026-10-01); priorities are still `none`. Jami sets them if the build needs an order. The agent never does this.
5. **Technology:** ~~open the parked topic~~ **decided 2026-10-01: Kotlin + Jetpack Compose (native), the prototype's logic ported, conditional on building on Jami's computer** (`governance.md` §2). Still open: the store account and channel (§5 open question in `Requirements/requirements.md`).
6. Republish the prototype artifact from 0.6 if the web link is still wanted.

## Under SWDev (2026-10-01)
Adopted via SWDev **intake path C**: `Requirements/` *is* the build spec (every locked level is Contract; SWDev reads the format natively, `trace_check.py`: 51 REQs, 47 locked, 37 build units). Build-side files beside the collection: `architecture.md` (placeholder), `build-map.md` (code homes per subtree, TBD until the stack is chosen), `design-inputs.md` (prototype, UI sheets, puzzle pipeline + Contract-delta), `tasks.md`, `progress.md`, `proposals.md`, `.swdev/guard.json`, `.claude/`. **Governance profile in force (ai-mastered, `governance.md` v0.2); stack decided: Kotlin + Jetpack Compose, native, if it builds on Jami's computer.** Next, in a Claude Code terminal session: **`KICKOFF.md`** (toolchain checklist + copy-paste prompt) → fresh-eyes review → "Accept collection v1.1 for TangramNoAds (G1)?" → freeze → P2 (architecture, ADR-001 = the stack, code homes) → WO-001 #Locking with the G3 toolchain proof first.

## Ready for a separate build framework?
Closer than after round 5. A build agent pointed at this folder finds: the requirements with sources (now including language, store obligations and the overview), one lock parameter set with its scoring written down (TYPE-004), the puzzle format with a validator and exact geometry, 13 puzzles, the layout rules, a working reference prototype in two languages and its tests. What is **not** ready:
- ~~Nothing is locked.~~ **Done 2026-10-01:** all 47 live REQs are locked with Jami's sign-off (SRC-014), so they pass the lock gate the build side expects. Priorities are not set.
- **The export tier** is `level: BIZ` REQs (REQ-001..007, REQ-048). The FUN and UI REQs carry the detail a builder needs; decide with the build framework how they travel.
- **Technology is parked**, and there has been no device playtest yet.
- **Framework conformance:** `Study/01–04` and `07` contain notes about third-party products, which SwReqCollector forbids in a project. They are never cited; Jami decides whether they stay (the Study folder is frozen). Three new framework findings from this round are in `Requirements/framework-notes.md`.

## How to run things
Python 3; Playwright with Chromium for the browser tools (not installed on the dev VM: the browser steps were run in the agent's workspace and the outputs written back).
```bash
python tools/validate_puzzles.py Tangrams          # V1–V6, V8–V10, V12; V11 prints the build order
python tools/render_puzzle.py Tangrams Tangrams/previews && python tools/svg2png.py Tangrams/previews/*.svg
python tools/build_prototype.py                    # rebuild the prototype
python tools/tests/test_prototype.py [--fast]      # browser tests
python tools/make_ui_sheets.py                     # Spec/ui sheets
python "C:\GitHub\AI\SwReqCollector\tools\validate.py" Requirements   # 0 errors required
python "C:\GitHub\AI\SwReqCollector\tools\views.py" Requirements      # after any REQ change
```
