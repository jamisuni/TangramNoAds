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
