# AGENTS.md: rules for AI agents in TangramNoAds

Start every session by reading `STATUS.md` (where things stand and what comes next), then this file.

1. **Enjoy, it's absolutely free.** Never add ads, analytics, purchases, donation asks, network access or permissions. If a task seems to need one of these, stop and ask.
2. **Ideas are not requirements.** Jami's messages give ideas, often marked "maybe". Record each round of his messages in `Requirements/` (a verbatim `evidence/src-NNN-*.md`, a `sources.md` entry, then the affected REQs) and log decisions in `Spec/05-open-questions.md`. You may propose better ideas; say so and record them as ASSUMPTIONs.
3. **`Study/` is frozen.** Jami's rule: leave it untouched. It holds the round 0–2 research and design notes; read it for background, never edit it. New notes go to `Requirements/` (evidence and REQs) or `Spec/`.
4. **`C:\GitHub\AI\SwReqCollector` is read-only.** It is the requirements framework this project follows. Never write to it; friction with it goes into `Requirements/framework-notes.md`.
5. **Requirements are locked (2026-10-01, SRC-014).** Never edit a locked REQ silently: record new owner input as a source, then a defect (DEF-NNN) or a change delta `Requirements/changes/CHG-NNN.md` (SwReqCollector Phase D) for Jami to sign.
5b. **Requirements first.** Behaviour comes from `Requirements/` (SwReqCollector format v2, ai-led capture; work there under `Requirements/CLAUDE.md`: validate, regenerate views, never set `priority` or `signoff`). `Spec/01–03` explain the design in prose; `Spec/04-requirements.md` is superseded. Technology is parked until Jami opens that topic.
6. **Geometry is exact** in validation (`tools/tangram_geom.py`, a + b√2). Runtime locking follows `Study/06-snapping-by-anchors.md` and TYPE-004: the anchors are the outline corners and placed-piece corners only. V11 (the edge-first build order) is information for authors, not a gate: every valid tangram has one (see `Tangrams/README.md`), so never remove a puzzle "because of V11" again. Warm-ups must pass V12.
7. **Puzzles live in `Tangrams/`**, separate from the requirements. Every file has `kind` (mini / warmup / full) and both titles (`en`, `fi`). After touching them, always run the validator and the renderer, and look at the preview sheet. Never set `provenance.reviewedByHuman` to `true`.
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
  - Compose UI-test imports *(recurred WO-003 + WO-004)*: `assertExists` / `assertDoesNotExist` / `assertIsDisplayed` are **member** functions of `SemanticsNodeInteraction` (never import them); `click()`, `swipe…()`, `longClick()` inside `performTouchInput` **need** `import androidx.compose.ui.test.<name>`. A test author compiles staged device tests as soon as the API exists (`:<module>:assembleDebugAndroidTest`), before the move-in.
- **Implementer staffing and hand-backs** *(lesson, WO-003)*: UI, rendering, concurrency and device-only tasks go to the slice-implementer with a **sonnet** override; the haiku default is for mechanical tasks. When a task's done-check cannot exercise the behaviour (visual or device-only), the orchestrator reads the diff before accepting the hand-back. "Complete" with a stub or an empty body is a rejected hand-back.
- **Release safety at every WO close** *(WO-005, CR-3 F3; G-04)*: build the release APK and run `python .swdev/verifiers/v04_release_apk.py` (exit 0 = no DevTools, canary found), then build a fresh debug APK and run `v04_release_apk.py --positive-control` (exit 0 = the scanner sees all four DevTools markers). One build at a time; record both result lines in the workorder. Re-run `--positive-control` also after any toolchain, AGP or dex-affecting change.
- **Two device channels at every WO close** *(WO-005, DA-92/93)*: run the full device suites (`play`, `browse`, `app`, `devtools`) on `Medium_Phone_API_37.0` **and** on `Phone_API_26` (Android 8.0, the minSdk floor), one emulator at a time, and launch a throwaway-signed copy of the release APK on API 26 (signed copy in the scratchpad only, with the local debug key). API 26 renders differently: a path drawn under a canvas scale blurs there, so `play` draws every path in px (the `PlayDrawing.kt` header rule). Never reintroduce a scaled-canvas path draw.
- **Gradle on this machine:** set `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`
  and `ANDROID_HOME='C:\Users\Jami\AppData\Local\Android\Sdk'` in the command
  if the shell predates them, and call `.\gradlew.bat` (cmd will not run a
  bare `gradlew.bat`). DoD build: `.\gradlew.bat assembleDebug test`.
  In bash, write file paths with forward slashes (`/c/Users/…`): a
  `> C:\Users\…\file` redirect loses its backslashes and lands as a junk
  file named `C:Users…` in the current folder (happened 2026-10-02).
- **Governance:** once `governance.md` is `in force`, a control point it
  assigns to `ai` is exercised, not asked, and every such decision is
  appended to `decisions.md` (what, why, how to reverse); `ai+inform` rows
  also go on the next owner-checkpoint surface (per WO close). Floors —
  the lock, security/PII/money, VC, the profile — are never the AI's.

### Current phase

`P3 — WO-006 #Layout, #Language, #Promise` (2026-10-04). Done: G1, G3 toolchain, G2
(`architecture.md` v1.0), WO-001 #Locking, WO-002 #Content, WO-003 #Solving
(first playable APK), WO-004 #Browsing + `store` (browsing, saved progress,
**v1 save format frozen**) and WO-005 #DevTools (debug-only DEV aid, V-04; API 26
channel live, DA-92/93) closed (checkpoints 1–5 surfaced). **Next: WO-006**
(resume from `STATUS.md` "▶ Resume here"), then the sequence without waiting, unless
Jami says stop. Update this line as phases advance.
