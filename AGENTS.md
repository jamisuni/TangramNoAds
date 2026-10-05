# AGENTS.md: rules for AI agents in TangramNoAds

Start every session by reading `STATUS.md` (where things stand and what comes next), then this file.

1. **Enjoy, it's absolutely free.** Never add ads, analytics, purchases, donation asks, network access or permissions. If a task seems to need one of these, stop and ask.
2. **Ideas are not requirements.** Jami's messages give ideas, often marked "maybe". Record each round of his messages in `Requirements/` (a verbatim `evidence/src-NNN-*.md`, a `sources.md` entry, then the affected REQs) and log decisions in `Spec/05-open-questions.md`. You may propose better ideas; say so and record them as ASSUMPTIONs.
3. **`Study/` is frozen.** Jami's rule: leave it untouched. It holds the round 0–2 research and design notes; read it for background, never edit it. New notes go to `Requirements/` (evidence and REQs) or `Spec/`.
4. **`C:\GitHub\AI\SwReqCollector` is read-only.** It is the requirements framework this project follows. Never write to it; friction with it goes into `Requirements/framework-notes.md`.
5. **Requirements are locked (2026-10-01, SRC-014).** Never edit a locked REQ silently: record new owner input as a source, then a defect (DEF-NNN) or a change delta `Requirements/changes/CHG-NNN.md` (SwReqCollector Phase D) for Jami to sign.
5b. **Requirements first.** Behaviour comes from `Requirements/` (SwReqCollector format v2, ai-led capture; work there under `Requirements/CLAUDE.md`: validate, regenerate views, never set `priority` or `signoff`). `Spec/01–03` explain the design in prose; `Spec/04-requirements.md` is superseded. Technology is parked until Jami opens that topic.
6. **Geometry is exact** in validation (`tools/tangram_geom.py`, a + b√2). Runtime locking follows `Study/06-snapping-by-anchors.md` and TYPE-004: the anchors are the outline corners and placed-piece corners only. V11 (the edge-first build order) is information for authors, not a gate: every valid tangram has one (see `Tangrams/README.md`), so never remove a puzzle "because of V11" again. Warm-ups must pass V12.
7. **Puzzles live in `Tangrams/`**, separate from the requirements. Every file has `kind` (mini / warmup / full) and both titles (`en`, `fi`). After touching them, always run the validator and the renderer, and look at the preview sheet. Never set `provenance.reviewedByHuman` to `true`. Only the owner approves a puzzle, by playing the review APK and running `python tools/puzzle_review.py mark` (the AI never runs `mark` on the real tree or writes `release/puzzle-review.json`). An approved puzzle is edited only on the owner's word, and that edit resets its flag *(WO-009, DA-156)*. A new puzzle must have no enclosed pocket (validator V15; exactly three old warm-ups are grandfathered, DA-169) and 3–8 visible picture colours (V14).
8. **Generated files:** `Spec/ui/*`, `Tangrams/previews/*`, `Spec/prototype/tangram-prototype.html` and `Requirements/views/*` are outputs. Edit the generators in `tools/` (the prototype source is `tools/prototype_template.html`).
9. **Prototype ≠ spec.** When they disagree, the requirements win; fix the template and rebuild.
10. **Test before calling it done:** `python tools/validate_puzzles.py Tangrams`, `python tools/build_prototype.py`, `python tools/tests/test_prototype.py` (all must pass), and the SwReqCollector `validate.py` on `Requirements/` (0 errors).
11. **The DEV solution reveal (REQ-046) is a testing aid.** Keep it in prototypes and test builds; a release build must not contain it.
12. **No difficulty levels, no tray turn buttons.** Both were removed by Jami in round 6 (REQ-004, REQ-044 withdrawn). Do not bring them back as "helpful" additions; the puzzle rating is the difficulty and a tap turns a piece.
13. **Every text in two languages.** The prototype and the game follow the device language (REQ-047, `fi` and `en`). A new string, a new puzzle title, a new note: both languages, always.


---

## Build under SWDev (added 2026-10-01 — intake path C)

This project is **built** under the **SWDev framework**, located READ-ONLY at
`C:\GitHub\AI\SWDev` (rules 1–13 above stay in force and are project-guardrail
material for `architecture.md` §2). Read the framework before acting; never
edit it; never edit `C:\GitHub\AI\SwReqCollector` either (rule 4).

**Framework version:** SWDev v0.15 · scaffolded 2026-10-01 *(record the
version you copied from; update only on a deliberate re-baseline)*

**Drift check (session start):** compare the version above against
`C:\GitHub\AI\SWDev\README.md`. If they differ, tell the human once and offer
to re-sync the **adapter files only** (`.claude/settings.json`,
`.claude/agents/`); methodology changes never retro-apply mid-WO.

**The spec is the collection.** `Requirements/` (SwReqCollector format v2,
locked 2026-10-01) *is* the spec dir (`.swdev/guard.json`: `spec_dir`,
`spec_format: swreq-v2`). **Every locked REQ of every level (BIZ / FUN /
UI) is Contract.** The build side never writes into `Requirements/`; a
needed change is a `DEF-NNN` / `CHG-NNN` on the capture side (rule 5).
Withdrawn items are retired; `idea` features are not build units;
`priority: none` is accepted — WO order is Jami's kickoff order. There is
no FEAT-NNN: the feature identity is the `#Tag`; code homes and
cross-feature deps live in **`build-map.md`**; UI plans, the reference
prototype and the puzzle pipeline are registered in **`design-inputs.md`**
(read its Contract-delta first — Contract wins).

### On every session start, read (in order)

1. `C:\GitHub\AI\SWDev\framework\00-principles.md`
2. `C:\GitHub\AI\SWDev\framework\methodology\lifecycle.md`
3. `C:\GitHub\AI\SWDev\framework\guardrails\directives.md`
4. `C:\GitHub\AI\SWDev\framework\methodology\gates-and-autonomy.md`
5. `C:\GitHub\AI\SWDev\initnewproject.md` §2b *(intake path C — how this project entered)*
6. *(from P3 on:)* `C:\GitHub\AI\SWDev\framework\methodology\definitions.md`,
   `orchestration.md`, `task-decomposition.md`,
   `C:\GitHub\AI\SWDev\framework\agents\orchestrator.md`, `handoff-contract.md`

Then this project's: **`governance.md`** (the human/AI dial — **in force,
ai-mastered** since 2026-10-01: `ai` rows you decide and log to
`decisions.md`, `human` rows and floors stop for Jami), **`KICKOFF.md`**
(until WO-001 closes), `Requirements/requirements.md` (index),
`Requirements/features.md` (#Tag tree), the `Requirements/reqs/REQ-NNN.md`
files in scope, `Requirements/views/digest.md` + `views/trace.md`,
`build-map.md`, `design-inputs.md`, `architecture.md`, `progress.md`.

### Non-negotiables (full text in SWDev framework/00-principles.md)

- Spec is the source of truth; code derives from locked requirements.
  Requirements say *what*, never *how*, at any level.
- **The Contract** — the locked collection + the Governed Interface Registry
  in `architecture.md` — changes ONLY by common agreement, recorded.
- **Ownership is visible:** `ISomething` = governed contract (registry-listed,
  tiered locked/notify); `iSomething` = AI-owned, freely refactorable.
- Guardrails and directives are constraints, never bent silently.
- Traceability is mandatory: REQ → design → task → code → test; tests carry
  `REQ-NNN.A<n>` tokens.
- No silent scope: build exactly the locked REQs. Every non-trivial
  abstraction names the REQ that requires it.
- Senior-plus, not perfect.
- The pipeline is autonomous between gates; anything breaching the Contract
  leaves the pipeline to a human.

### Hard-stops (pause and ask the human)

- Security / auth / secrets / PII — *(structurally excluded by REQ-001/010:
  any network, permission or third-party code is a Contract breach, rule 1)*
- Data migration of the saved state (REQ-025) / breaking API change
- Ambiguous requirement or guardrail conflict — **or any change to a locked
  REQ/TYPE or a locked-tier governed `I*` interface** (notify-tier changes
  are logged `contract-delta` events, reported at WO close)

### Duties every session

- **Turn-ending signal (principle 16 — every reply):** end with exactly one
  of `All ready, whats next Jami?` or `Need help, answer above question JAMI`
  (the one question immediately above). Never neither, never both.
- **`progress.md`**: append the human's input VERBATIM, then a ~10-line
  summary. One entry per interaction.
- **Capture-side hygiene:** after any change in `Requirements/` (made on the
  capture side) run `validate.py` + `views.py` (`Requirements/CLAUDE.md`).
- **Traceability**: verify with
  `python C:\GitHub\AI\SWDev\framework\skills\trace-check\trace_check.py --project .`
  — required green at every work-order close (RED today: no product tests yet).
- **`proposals.md`**: friction with the framework goes here, never into the
  framework folder.
- **Stations are subagents (P3 on):** dispatch fountain stations as the named
  subagents in `.claude/agents/`; inline station work is a logged exception.
- **No version control** by agents: never `git init`, commit or tag; Jami does
  VC by hand (the orchestrator prompts for a close snapshot).
- **Contract files through the guard** *(lesson, 2026-10-02)*: edit any path in
  `.swdev/guard.json` `contract_paths` only with the Edit/Write tools — the
  guard hook cannot see shell or script writes, so they show up as drift in
  trace-check. Scripts that write project text files use LF line endings
  (`open(path, "w", newline="\n")` in Python on Windows); the repo is LF.
- **Test tokens are coverage claims** *(lesson, WO-001 + WO-002)*: trace-check
  counts any `REQ-NNN.An` text in a test file — comments included — as
  coverage. Put a token only on a test whose assertion is that criterion's
  meaning; a test that pins an AI decision or a guardrail carries
  `// decision DA-n` / `// guardrail G-nn` instead, and prose that merely
  mentions an ID writes it without the dot (`REQ-039 A2`). Every acceptance-
  test-author and code-review brief says so; the orchestrator greps the new
  test files for tokens before Test & Verify.
- **Seams and test adapters** *(lesson, WO-003)*:
  - When the orchestrator plans, every frozen seam in a design names the task that delivers it. A seam changed at planning is written back into the design and `decisions.md` before the test author starts.
  - Test adapters fail loudly on a miss (`error(…)`), never with a silent `?.invoke` / `?: return`. Tests in the same module call `internal` members directly instead of reflecting.
  - A device test that pins a pixel is read against the geometry before the drawing is blamed: an outline corner is partly background by definition (DA-40).
  - Compose UI-test imports *(recurred WO-003 + WO-004)*: `assertExists` / `assertDoesNotExist` are **member** functions of `SemanticsNodeInteraction` (never import them); `assertIsDisplayed`, `assertIsOn` / `assertIsOff` and the other `assert…` helpers are **extension** functions and **need** `import androidx.compose.ui.test.<name>` *(corrected 2026-10-04, WO-007 T7a: the compiler required the import for `assertIsDisplayed`)*; `click()`, `swipe…()`, `longClick()` inside `performTouchInput` **need** `import androidx.compose.ui.test.<name>`. A test author compiles staged device tests as soon as the API exists (`:<module>:assembleDebugAndroidTest`), before the move-in.
- **Implementer staffing and hand-backs** *(lesson, WO-003)*: UI, rendering, concurrency and device-only tasks go to the slice-implementer with a **sonnet** override; the haiku default is for mechanical tasks. When a task's done-check cannot exercise the behaviour (visual or device-only), the orchestrator reads the diff before accepting the hand-back. "Complete" with a stub or an empty body is a rejected hand-back.
- **Release safety at every WO close** *(WO-005, CR-3 F3; G-04)*: build the release APK and run `python .swdev/verifiers/v04_release_apk.py` (exit 0 = no DevTools, canary found), then build a fresh debug APK and run `v04_release_apk.py --positive-control` (exit 0 = the scanner sees all four DevTools markers). One build at a time; record both result lines in the workorder. Re-run `--positive-control` also after any toolchain, AGP or dex-affecting change. **From WO-006 also V-08** (DA-104): `python .swdev/verifiers/v08_promise_apk.py` (it builds its own release APK; exit 0 = no SDK class definitions, no permission element, no debug-only class), then `v08_promise_apk.py --apk app/build/outputs/apk/debug/app-debug.apk --expect-debug` on the fresh debug APK. Record both lines. **From WO-007, V-08's feedback-caller check** (DA-123/125/127) fails on any new caller of a sound or haptic API: a new dependency, or a Compose BOM bump. Before extending its pinned allow-list (caller method + callee), re-judge DA-125's inventory and log a decision row.
- **Two device channels at every WO close** *(WO-005, DA-92/93)*: run the full device suites (`play`, `browse`, `settings`, `time`, `app`, `devtools`; `settings` includes the audio smoke; from WO-008 also the `HarnessScaffoldingTest` display gate first) on `Medium_Phone_API_37.0` **and** on `Phone_API_26` (Android 8.0, the minSdk floor), one emulator at a time, and launch a throwaway-signed copy of the release APK on API 26 (signed copy in the scratchpad only, with the local debug key). API 26 renders differently: a path drawn under a canvas scale blurs there, so `play` draws every path in px (the `PlayDrawing.kt` header rule). Never reintroduce a scaled-canvas path draw.
  **Device hygiene (WO-006, DA-108):**
  - Before and after every device step, run `python tools/device_reset.py --serial S`, which resets and then checks.
  - A non-clean `--check` after a step voids that step: reset, re-run, and record the leftover in the workorder.
  - Confirm serials with `adb devices` after every boot. The API 26 instance can come up as emulator-5556 if port 5554 is still held.

  **API 26 limits** (DA-98, DA-105, DA-111):
  - There is no gesture bar, so REQ-035 A2 is rule-level there and its evidence is API 37.
  - `svc wifi` is killed on that image.
  - The launcher relaunches after every display change. The test kit waits for it to settle.
- **Gradle on this machine:** set `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`
  and `ANDROID_HOME='C:\Users\Jami\AppData\Local\Android\Sdk'` in the command
  if the shell predates them, and call `.\gradlew.bat` (cmd will not run a
  bare `gradlew.bat`). DoD build: `.\gradlew.bat assembleDebug test`.
  In bash, write file paths with forward slashes (`/c/Users/…`): a
  `> C:\Users\…\file` redirect loses its backslashes and lands as a junk
  file named `C:Users…` in the current folder (happened 2026-10-02).
- **Never put a backslash into a Python string literal that writes a project file** *(3rd occurrence, 2026-10-04)*:
  `\b`, `\a`, `\r` and `\n` become control bytes. This corrupted `tasks.md` (NUL, 2026-10-03) and the
  WO-006 evidence line (`app\build\…` → backspace, bell, CR; and a stray draft verifier written to `C:/GitHub/I/TangramNoAds/...` on 2026-10-02, where the `A` of `C:/GitHub/AI` was lost after its backslash). Write Windows paths with forward slashes
  in prose, and put long or quoted text into a file with the Write tool first. After any scripted doc
  edit, scan for bytes < 0x20 other than tab and LF.
- **A device test that solves a puzzle by touch uses the `solveByTouch` pattern** *(2nd occurrence: WO-006 DA-110, WO-007 DA-132)*:
  place all pieces but the last with `placePieces`, then drop the last one **without** judging it by colour. The solved
  picture replaces the piece colours at the moment of the solve, so a colour check on the final drop reads a correct
  solve as a miss. The held-out kit carries the same helper, so a held test cannot reinvent the trap.
- **A test that compares or exempts across files lands together with every file it reads**
  *(planning lesson, 2nd occurrence: WO-006 plan F1, WO-007 plan F1/F2)*. Examples: an equality
  check of two word-list copies, or a scan whose exempt-key list must match new strings. When a plan
  moves tests in per module, list each cross-file test's inputs, and schedule one move (or one task)
  that lands all of them at once, right where the product change that needs them lands. Never put a
  window between "the new strings exist" and "their exemption exists", or between "copy A changed" and
  "copy B changed".
  *(3rd occurrence: WO-008 plan B1, an exact tag set in a device test.)* **How WO-008 met it (DA-145, DA-146):**
  - **Quiescent landing steps:** every step that builds `:app:` or `:devtools:` runs only when no implementer task and no other landing step is active. A cross-file check lands in the first such step after the file it reads, before anything builds on it. Single-module acceptance tests land at the owner task's done-check (a "module landing").
  - **Test authors compile outside the tree:** in a copy of the repository, never by overlaying files in the working tree.
  - **Before authoring, the test author greps every in-tree test for exact tag, text or set assertions** that the new screens touch, and lists them.
- **A debug `TestConfig` clock and other time-driven device tests** *(WO-008)*: a test that needs time sets `TestConfig.timeSource` before launch (`AppLaunch.launch(…, timeSource = …)`), and both `TestConfigRule` copies reset it afterwards. A test that changes screen and then jumps the clock uses `advanceActive` (the clock moves on the main thread, then `accrue()`), never `Thread.sleep`. A device test reads density from the launched app under the display override, never from `targetContext` (WO-008 DA-40 case).
- **A tagged display element carries its own text** *(WO-008, DA-149)*: when a test tag sits on a container (a pill, a label + value row), merge its descendants' semantics into it (`semantics(mergeDescendants = true)`). Otherwise tests and TalkBack read an empty node. Keep it inert: no click action, no role.
- **Guarded removals in bash** *(WO-008)*: `rm` with a variable path must be written as `rm -- "${DIR:?}/${NAME:?}"`. A bare `rm "$VAR/$X"` is blocked by a safety check before anything runs. Overlays of held-out files are copied by path without reading, and removed with the guarded form before any other build starts.
- **Governance:** once `governance.md` is `in force`, a control point it
  assigns to `ai` is exercised, not asked, and every such decision is
  appended to `decisions.md` (what, why, how to reverse); `ai+inform` rows
  also go on the next owner-checkpoint surface (per WO close). Floors —
  the lock, security/PII/money, VC, the profile — are never the AI's.

### Current phase

`P3 — all nine WOs closed; release-ready, waiting for Jami` (2026-10-05). Done: G1, G3 toolchain, G2
(`architecture.md` v1.0), WO-001 #Locking, WO-002 #Content, WO-003 #Solving
(first playable APK), WO-004 #Browsing + `store` (browsing, saved progress,
**v1 save format frozen**), WO-005 #DevTools (debug-only DEV aid, V-04; API 26
channel live, DA-92/93), WO-006 #Layout/#Language/#Promise (phone + tablet,
fi/en, the free promise; V-08; device hygiene), WO-007 #Settings (settings screen,
sound + haptics behind one gate, reset; platform click/vibration off; V-08 caller check)
WO-008 #PlayTime (`time` module: active time, best times, the timer; v1 format untouched)
and WO-009 #Release (Play; 25 puzzles; V-09 release gate; the owner's review tool; the
listing / privacy / checklist drafts; the launcher icon) closed (checkpoints 1–9 surfaced).
**Next: the owner's critical path and RELEASE-DAY** (`STATUS.md` "▶ Resume here",
`release/release-checklist.md`); RELEASE-DAY is a mechanical checklist run, and any
non-mechanical change goes to a short follow-up WO. Update this line as phases advance.
