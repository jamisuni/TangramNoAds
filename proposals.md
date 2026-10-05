# Framework Proposals — from TangramNoAds

Append-only. **Blind-write** anything you want the SWDev framework to examine:
a recurring friction, a missing rule, an awkward gate, a question the playbook
should have asked. No framework approval is needed to write here; the framework
harvests this file during its own review.

> Never edit the framework directly. This file is the project's only outbound
> channel to it.

**What belongs here** (projects under-file — when in doubt, write it):
corrected checks — any test or review the pipeline had to fix, naming what
the checker over-specified; environment gaps that stalled a DoD item (a
missing browser IS framework signal, not "local setup"); a question the
playbook should have asked; a station skipped "per precedent"; any rule you
had to interpret rather than follow. A WO that closes with "no friction"
still writes the one-line entry saying so — silence is indistinguishable
from starvation.

---

## YYYY-MM-DD — <short title>

- **Context:** <what happened>
- **Proposal:** <what the framework should consider changing>
- **Severity:** low | medium | high

## 2026-10-02 — trace-check cannot be green at a WO close until the last WO

- **Context:** AGENTS.md / definitions.md require "trace-check green at every work-order close", but trace-check fails on *every* locked REQ with uncovered acceptance. With 47 locked REQs and WO-001 covering 4, the project stays RED until WO-009; there is no `--scope`/`--wo` option.
- **Proposal:** add a WO scope to trace-check (e.g. `--reqs REQ-019,REQ-020,…` or `--wo workorders/WO-001.md` reading its Scope line): GREEN = no FAIL for the in-scope REQs + contract integrity + req-lint + views fresh; out-of-scope uncovered REQs reported as "planned". Until then this project records a WO-scoped green as a waiver at each close.
- **Severity:** high

## 2026-10-02 — Android: trace-check rule 3 forces one Gradle module per code home

- **Context:** rule 3 requires a covering test's path to start with the feature's code home. Android (and Maven/Gradle JVM projects generally) keep tests in a parallel tree (`<module>/src/test`), so a package-level code home inside one module can never pass. TangramNoAds chose one Gradle module per code home (ADR-002), which is defensible but was forced by the checker, not by D1. Also the path-C `test_globs` example (`src/**/tests/**`) does not fit Android at all.
- **Proposal:** let `build-map.md` name a code home as a *module* or as a *package* with a test-tree mapping (e.g. `play` ⇒ `play/src/main/**` + `play/src/test/**`), or make rule 3 accept `src/test/<same package path>`. Add an Android/Gradle `test_globs` example (`*/src/test/*`, `*/src/androidTest/*`).
- **Severity:** medium

## 2026-10-02 — Principle 16 has no "working in the background" signal

- **Context:** while a long subagent (the 15-min fresh-eyes review) ran, the session had to either keep making tool calls or end the turn with "All ready, whats next Jami?" — which claims idleness while work is in flight — or "Need help…", which needs a question.
- **Proposal:** a third signal, e.g. `Working — I'll come back when <station> finishes`, or a rule that background work never ends a turn without a status line naming it.
- **Severity:** low

## 2026-10-02 — Capture-side CHG proposals have no home when Requirements/ is read-only

- **Context:** the owner made `Requirements/` read-only for the build session; Phase D (`changes/CHG-NNN.md`) is not tooled; KICKOFF said to put CHG proposals in the index §6 (inside the read-only folder). The 38 review findings' resolutions were parked build-side in `req_review_01.md` as "capture-side actions CA-1…CA-4".
- **Proposal:** define a build-side outbox for capture-side changes (e.g. `capture-outbox.md` with DEF/CHG drafts in SwReqCollector format) that a SwReqCollector session ingests; reference it from gates-and-autonomy hard-stop 3/5.
- **Severity:** medium

## 2026-10-02 — interface-definition.md is C#-only; Kotlin needed a marker decision

- **Context:** the governed-marker rule (`I*` vs `i*`) fights Kotlin naming like C#; the file only gives C# guidance (`Contracts` namespace, XML docs, Task/CancellationToken). TangramNoAds used: `I` prefix + a `contracts` Gradle module + the banner; KDoc instead of XML docs; synchronous calls for a few-KB local store.
- **Proposal:** a short language-fit table in interface-definition.md (C#, Kotlin/Java, TypeScript, Go): marker, doc syntax, async idiom.
- **Severity:** low

## 2026-10-02 — Environment gaps the kickoff did not anticipate (Windows + Claude Code)

- **Context:** (1) user env vars set after the Claude Code session started (`JAVA_HOME`, `ANDROID_HOME`) are invisible to its shells — every Gradle call must set them; (2) the shell sets `NoDefaultCurrentDirectoryInExePath`, so `cmd /c gradlew.bat` fails ("not recognized") and needs `.\gradlew.bat`; (3) `/c/...` paths in exported env vars are not converted for cmd.exe child processes — use `C:\...` form.
- **Proposal:** add these three lines to the G3 toolchain-proof notes (gates-and-autonomy.md §1 or the adapter's CLAUDE.md), and let `swdev_bash_approve.py` know `gradlew`/`gradlew.bat` as a benign build command.
- **Severity:** low

## 2026-10-02 — No Work-Order Planner subagent in the pack

- **Context:** the handoff chain starts with a Work-Order Planner, but `.claude/agents/` has no planner; decomposition falls to the orchestrator inline (a logged exception every WO).
- **Proposal:** ship a `wo-planner` subagent (mid tier), or state in orchestrator.md that decomposition is orchestrator work checked by the plan-reviewer.
- **Severity:** low

## 2026-10-02 — The guard hook cannot see shell writes to notify-tier files

- **Context:** the orchestrator rewrote `build-map.md` (notify tier) with a Python script via Bash. The PreToolUse guard matches only Edit/Write/MultiEdit/NotebookEdit, so no `contract-delta` was logged and trace-check (correctly) reported drift; the fresh-eyes spot-check caught it. A locked-tier file could be changed the same way without a deny. Also: Windows Python writes CRLF in text mode, silently converting LF files.
- **Proposal:** (a) the guard (or the Bash approver) flags Bash commands whose arguments name a `contract_paths` file, at least logging them; (b) trace-check's drift message could say "edited outside the guard? re-do the edit with Edit/Write or record the delta"; (c) adapter CLAUDE.md: "contract-path files are edited only with Edit/Write". Codified in this project as an AGENTS.md duty line.
- **Severity:** medium

## 2026-10-02 — Corrected check (WO-001): decision-asserting tests carried REQ tokens

- **Context:** the Acceptance Test Author tagged two tests with `REQ-019.A1` / `REQ-051.A1` that assert AI design decisions (DA-1 tie-break, DA-4 pulse only over the board) — readings the REQs do not mandate. The code reviewer caught it (WO-001 code review F2); the tests were re-tagged `decisionDa1_…` / `decisionDa4_…` with a `// decision DA-n` comment. Miscalibration: "assert only what is mandated" was applied to assertions but not to the *token*, so traceability over-claimed REQ coverage.
- **Proposal:** traceability-rules §2: a test that pins a logged AI decision (decisions.md) carries a decision reference (e.g. `DEC:DA-1`), never a `REQ-NNN.An` token; the Acceptance Test Author brief template says so; trace-check could list decision-tagged tests separately so the owner sees which behaviour rests on AI readings.
- **Severity:** medium

## 2026-10-02 — Corrected check (WO-001): a verifier that could not fail on alternate syntax

- **Context:** V-06 (module-dependency guardrail) passed `implementation(projects.browse)` and `api(project(path = ":browse"))` — its self-test proved it could fail only on the `project(":x")` spelling. Found by the code reviewer (F1) with a scratch project; fixed fail-closed + 4 new self-tests. Same class in V-05 (XML parse errors swallowed).
- **Proposal:** guardrail-authoring.md § Verifiers: a verifier's self-test must cover every syntax the checked language allows for the construct (or the verifier fails closed on anything it cannot parse); "fail closed on unparseable input" as a standing verifier rule.
- **Severity:** medium

## 2026-10-02 — Corrected check (WO-002): a held-out test that could never have compiled

- **Context:** the Acceptance Test Author writes held-out tests before any implementation exists and keeps them outside every source tree, so nothing ever compiles them until Test & Verify copies them in. In WO-002 one held-out file lacked an import for the kernel's `Q2` operator extensions; at its "first run" all four held-out files failed to compile and NO held-out test executed. The test author fixed the import only (no assertion change) and Test & Verify re-ran: 15/15 PASS. Miscalibration: the held-out slice had no compile gate, so a trivial authoring slip could silently cost the WO its held-out evidence (or tempt a station to "fix" tests).
- **Proposal:** task-decomposition.md § held-out: once the implementation exists, the Test Author compile-checks the held-out files (compile only — e.g. a `compileTestKotlin` on a throwaway copy — never run, never read main code) before Test & Verify; and Test & Verify may apply an *imports-only* fix, logged as a corrected check, without that counting as "seeing" the slice.
- **Severity:** medium

## 2026-10-02 — Recurring: test tokens over-claimed coverage again (WO-002)

- **Context:** WO-002's tests again carried `REQ-NNN.An` text where the assertion was a design decision (DA-8), a test-harness self-check, a parser guardrail (G-03), or a prose mention (`REQ-039.A2` in a comment — a release-time criterion that trace-check would have counted as covered). Caught by the orchestrator's token grep and the code review; all retagged. Second occurrence after WO-001 → codified as an AGENTS.md duty ("Test tokens are coverage claims") plus an orchestrator token audit before Test & Verify.
- **Proposal:** (as WO-001's entry) a decision/guardrail reference syntax in traceability-rules §2, and trace-check ignoring tokens inside a comment line that also carries `decision`/`guardrail`/`not a coverage claim`, or reporting such lines for review.
- **Severity:** medium

## 2026-10-02 — Path-mangled junk files from agents' shell commands (Windows + Git Bash)

- **Context:** twice today an agent's bash command with Windows paths (`> C:\Users\…\build-output.txt`, `cp C:\GitHub\…\x.kt C:\GitHub\…\dir`) lost its backslashes and created files named after the mangled path — one in the project root, two inside `play/src/test` (holding copies of held-out tests with REQ tokens, i.e. invisible false coverage for trace-check). Found by the orchestrator and an implementer, removed after inspection; codified as an AGENTS.md line + a planned verifier V-07.
- **Proposal:** adapter CLAUDE.md / brief templates: "in bash use forward-slash paths"; the Bash approver could flag commands containing `C:\` outside quotes; a stock "no junk paths" verifier in the template.
- **Severity:** medium

## 2026-10-02 — Corrected check (WO-003): staged acceptance tests vs the frozen seam list at move-in

- **Context:** At the JVM move-in, the independent test author's staged tests met the real code for the first time, and four seam issues surfaced. (1) `badgeRect(session)` was frozen as `RectDp` in the design but amended to `RectDp?` in the plan without a record, so the adapter did not compile (DA-32). (2) A reflective `attach()` looked for `setLayout`, but Kotlin mangles `internal` setters (`setLayout$play_debug`). The miss was **silent**, so every drag was a no-op and three tests failed with misleading "not locked" messages. (3) The frozen seam `PieceDrawing.scale` was never assigned to any task, so the orchestrator pulled it forward as TASK-017b (DA-33). (4) One test over-asserted REQ-013 A3 by demanding equal cell *height*, while the REQ fixes only the scale and width; the assertion was removed (DA-34). All test-side fixes were made by the test author in the adapter layer, plus one assertion removal on the orchestrator's ruling.
- **Proposal:** task-decomposition.md: (a) every frozen seam names its delivering task, and the plan review checks that the seam list is covered both ways, as with acceptance IDs; (b) a seam amended at planning is written back into the design and the decision log before the test author starts; (c) test-author guidance: a reflective adapter must fail loudly on a miss, never skip silently, and a test in the same module should call `internal` members directly instead of reflecting.
- **Severity:** medium

## 2026-10-02 — Haiku slice-implementer on a UI task: "complete" with core parts stubbed

- **Context:** WO-003 TASK-018a (Compose drawing) went to the default `slice-implementer` model (haiku), because the orchestrator forgot the sonnet override it had used for TASK-017. The hand-back said "complete", with green builds, but the code had:
  - dp drawn as px;
  - wall-clock animation instead of the frame clock;
  - a component built ahead of its task that writes state from the draw phase (re-opening a code-review fix);
  - a feature the REQ forbids (a ghost outline in emptied tray cells);
  - a re-invented animation instead of the existing seam;
  - the frozen picture seam left as an empty body.

  Only the orchestrator reading the code caught it; the JVM suite could not, since drawing is checked on device. The task was redone on sonnet (decisions.md, staffing row).
- **Proposal:**
  - (a) orchestration.md staffing: name the task classes that need a stronger implementer by default (UI/rendering, concurrency, anything checked only on device), rather than leaving it to the dispatcher's memory.
  - (b) handoff-contract.md: a hand-back must list every stub or empty body it leaves, and "complete" with a stub is a protocol violation.
  - (c) the orchestrator reads the diff of every implementer hand-back whose done-check cannot exercise the behaviour (device-only, visual).
- **Severity:** medium

## 2026-10-03 — Corrected checks (WO-003): device pixel fixtures and the held-out first run

- **Context:** After the device move-in and the held-out first run, the independent tests needed these corrections. Every one kept its assertion's meaning; the orchestrator ruled on each against its REQ and re-derived the evidence itself.
  - (1) Staged device adapters had the same seam bugs as the JVM ones: a nullable `badgeRect`, a missing type argument, a bad import, and silent skips.
  - (2) **DA-40:** pixel samples taken as "inner edges" were really silhouette outline corners. All 15 were checked in Python against the reference geometry. The samples now move along the edge.
  - (3) **DA-41:** a "picture distinguishable from the background" check failed on art that is white at its outline. It now compares with the reference render.
  - (4) **Held-out first run 11/12.** It needed six adapter-class repairs: a missing constant, nullable `badgeRect`, a type argument, a silent `setLayout` lookup, an unfrozen value shape, and `setContent` called twice. Plus one scenario defect (**DA-45**): the test solved the puzzle, then dragged, which the solved-state rule forbids. 0 escaped product defects.
  - (5) Separately, the held-out pre-audit after the seam lessons was blocked by the permission check, which denied a resumed author's read of `.swdev/heldout/`. It was not routed around; the adapter classes went into the Test & Verify brief instead.
- **Proposal:**
  - (a) test-author guidance: pixel fixtures must classify sample points against the outline geometry (corners), not only by the edge normal;
  - (b) "different from the background" is not a picture-presence oracle; compare with a reference render;
  - (c) a held-out compile and adapter dry-run (compile only, plus an adapter smoke run that executes no assertion) by the author after the API exists, before Test & Verify, so that first-run results measure the product, not the adapters;
  - (d) say explicitly whether a resumed test author may re-read its own held-out files.
- **Severity:** medium

## 2026-10-03 — WO-004 close: what worked, and the friction left

- **Context:**
  - What worked. WO-004's held-out slice passed **6/6 on its first run with no corrections**, after WO-001…003 each needed test-side repairs. The difference came from four WO-003 lessons applied up front:
    - every frozen seam names its delivering task;
    - the test author compiles staged device tests as soon as the API exists;
    - the held-out adapters were swept for known defect classes before the first run;
    - the independent author, not the implementer, wrote the frozen format fixtures (DA-67).
  - The friction left:
    - (1) the orchestrator's own brief wording twice disagreed with the design (DA-69 "after the fade"; a 48 dp square slot for a ~130 dp text pill). An implementer and the orchestrator's diff read caught them.
    - (2) The Compose UI-test import mistakes recurred, so the rule is now codified.
    - (3) Agents' `git` calls and some heredoc writes are denied by the permission check, so Test & Verify could not diff amended tests against their originals. The orchestrator had diffed them at move-in.
    - (4) Two device test failures that looked like product defects were fixture defects (DA-70). An implementer proved this with logging and pixel dumps instead of patching the product.
- **Proposal:**
  - (a) orchestration.md: before dispatch, the orchestrator checks its brief against the design's text for the seams it names. A brief that paraphrases a frozen seam is a defect source.
  - (b) test-author guidance: device tests map coordinates through the product's public test seam (here `BoardTransform`), never by inferring geometry from pixels.
  - (c) For Test & Verify: when git is not available to agents, the orchestrator hands over the before/after diff of every amended visible test.
- **Severity:** low

## 2026-10-03 — Governance row 12's "file a capture-side CHG" step was skipped for two WOs

- **Context:** governance row 12 (ambiguous requirement, `ai`) says "decide, write the ASSUMPTION, **file a capture-side DEF/CHG proposal**". WO-003 and WO-004 logged about ten REQ readings as row-12 decisions but filed no capture-side proposals. WO-005's design review caught the gap (O1). The orchestrator first ruled "no CHG needed" for a small departure, then corrected itself after re-reading the row. The proposals are now backfilled as CA-5/CA-6 in `req_review_01.md`. The build side cannot write `Requirements/`, and SwReqCollector Phase D is not tooled, so a "capture-side proposal" has no natural home and is easy to forget.
- **Proposal:**
  - (a) Add the CHG step to the decision-logging template itself: a "capture-side action" column, with values filed / n/a and why.
  - (b) trace-check, or a verifier, flags any row-12 decision without a CA/CHG reference.
  - (c) Give "capture-side proposal" one defined location for path C projects whose spec dir is read-only to the build side.
- **Severity:** medium

## 2026-10-04 — The minSdk floor was waived for three WOs and hid a real rendering defect

- **Context:** the API 26 release launch was waived from WO-003 (DA-43) because no image was installed, so every device check ran on API 37 only. When Jami installed the image, the first run on API 26 found a real product defect. Android 8's renderer blurs any path drawn under a canvas scale, giving soft piece and silhouette edges for every Android 8 player (DA-92). The waiver had covered only "the release launch", but the actual gap was "nothing ever ran at the floor". The fix and its guard test took about an hour. The defect itself had been present since WO-003.
- **Proposal:**
  - (a) Treat the minSdk floor as a **device channel**, not a launch check. The framework's device-test guidance names two channels, newest and floor, from the first WO that has device tests.
  - (b) A waiver on the floor channel blocks a release candidate.
  - (c) When a project cannot install the floor image itself, the kickoff checklist asks the owner for it at G1, not at the first WO close.
- **Severity:** medium

## 2026-10-04 — A held-out test that did not compile reached Test & Verify

- **Context:** WO-005's held-out `HeldSolveNowStoreTest` (an `app` JVM test) called `PlaySession.onFrame`, which is `internal` to `play`, so it could never compile in `app`. Nobody compiles a held-out slice before its first run, because the orchestrator may not read it. T&V had to make an adapter-only correction (DA-95). The visible slice has an "early compile" lesson (AGENTS.md); the held-out slice has none.
- **Proposal:** the acceptance-test author compiles the held-out slice against the tree before handing back. It can copy the slice into a scratch copy of the module test set, compile, and delete the copy. The author's hand-back then states "held-out compiled: yes". Add this to the test-author agent definition.
- **Severity:** low

## 2026-10-05 — Trace rule 3 (the code home) is checked only at the close

- **Context:** WO-008's design placed REQ-029 A2's module-level covering test in `settings`, because the screen lives there. The feature's code home is `time`. The design review, the plan review and CR-7 all passed it. Only trace-check at TASK-069 reported "directory theater", which cost a late follow-up test (DA-151).
- **Proposal:** the design template's acceptance table gets a column "covering test under the code home (trace rule 3)", one per ID. The plan reviewer's checklist asks for it, and the orchestrator can run `trace_check.py` once right after the module landings, not only at the close.
- **Severity:** low
