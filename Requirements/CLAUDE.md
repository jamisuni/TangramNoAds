# CLAUDE.md — TangramNoAds requirements collection

Instructions for any AI agent working in this folder. This folder is a
**requirements collection** captured with the SwReqCollector framework. It
describes **our own system** — the one this project builds; it contains no
code.

**Framework:** SwReqCollector format v2 (tools v0.4) at `C:\GitHub\AI\SwReqCollector`
**Capture purpose:** build
**Capture mode:** ai-led
**System described:** TangramNoAds, a classic tangram game for Android; first capture 2026-09-27 at prototype 0.3

## What is where

| File | What it is | Who writes it |
|---|---|---|
| `requirements.md` | index: context, actors, coverage, project-wide open questions, defect list (DEF-NNN), change log, next free REQ id | owner + agent |
| `features.md` | THE feature tree — one markdown outline, `## #Tag — Title · status`, ~4 lines per feature | owner + agent |
| `reqs/REQ-NNN.md` | one requirement per file: YAML frontmatter + EARS body | agent drafts, owner approves |
| `req_types.md` | shared value rules, `TYPE-NNN`, defined once | agent |
| `sources.md` + `evidence/` | every piece of evidence, `SRC-NNN`, registered before it is cited — `SRC-001` is the project description | agent |
| `study/` | **ai-led only**: the agent's concept study, use cases, views map, mockups — registered as ONE source, Kind `proposal`, Reliability `generated`; evidence of what was proposed, never of intent | agent |
| `views/` | **generated** digest, feature map, trace report — never hand-edited | `views.py` only |
| `framework-notes.md` | friction and findings about the *framework* (not the system) | owner + agent |

## Commands

Run from this folder. On Windows use `python`; elsewhere `python3`.

```
python "C:\GitHub\AI\SwReqCollector\tools\validate.py" .            # must end with 0 errors
python "C:\GitHub\AI\SwReqCollector\tools\validate.py" . --strict   # warnings fail too — use before lock
python "C:\GitHub\AI\SwReqCollector\tools\views.py" .               # regenerate views/ after ANY item change
```

`validate.py` prints the next free ids (`next free: REQ-0NN · …`). Use them;
never reuse or renumber an id.

## Capture mode

The header line says who leads. It is a property of the whole collection
(`requirements.md` carries the same line; `validate.py` reads it there) and
it changes what the agent may do.

- **owner-led** (the default). The owner knows the target. The agent
  extracts, asks and hands over; the owner classifies and locks. Every gap
  is an `OPEN QUESTION`. Follow *Session 1* and the rules below as written.
- **ai-led.** The owner's grasp of the domain is light and nobody has time
  for a question list. The agent proposes the first version under `study/`,
  **decides instead of asking** — every decision an `ASSUMPTION:` line on a
  REQ that stays `confidence: inferred` — and releases the first-version
  cut as `status: ai-approved` with `ai_approval: {by, date}`, so a
  prototype can be built. Humans then *use* the prototype and say what is
  wrong or missing; that feedback is registered as a source and goes
  through the normal classification (requirement / `DEF-NNN` / out of
  scope). Procedure: `C:\GitHub\AI\SwReqCollector\format\capture-loop.md` §12.
  What does **not** change: `ai-approved` is not sign-off and binds nobody;
  an AI-made artifact (`generated` source) never supports `stated` or
  `observed`; ask the few questions a prototype cannot cheaply get wrong
  (legal and safety duties, data migration, money, systems we do not
  control, the size of the first version); `priority` and `signoff` stay
  the owner's; only `status: locked` with the owner's `signoff` binds.

## Session 1 — study the target first

*(Owner-led. An ai-led collection starts with `capture-loop.md` §12
instead.)* The first session's only input is the owner's project description in
`evidence/src-001-project-description.md` (registered as `SRC-001`).
Read it, understand the target, and structure it: actors and purpose into
`requirements.md`, the first feature tree into `features.md`, the
requirements the description actually supports into `reqs/`
(`confidence: stated`; anything you filled in yourself is `inferred`),
and every gap as an `OPEN QUESTION`. Hand the owner the digest, the trace
report and the question list. **No market scan in session 1** — the target
is understood from the inside before anyone looks outward. The full
procedure is `C:\GitHub\AI\SwReqCollector\format\capture-loop.md` §2.

## Rules — non-negotiable

1. **Read the format first**: `C:\GitHub\AI\SwReqCollector\format\req-format.md`, then
   `C:\GitHub\AI\SwReqCollector\format\capture-loop.md` for how a capture session runs.
   The worked example is `C:\GitHub\AI\SwReqCollector\example/`.
2. **No sourceless items.** Register the source in `sources.md` (and drop
   raw material under `evidence/`) *before* a REQ cites it.
3. **Sources are the project's own material**: the owner's description and
   decisions, conversations, the project's documents, mockups, prototypes,
   builds. **Other companies' products are never sources**: never
   registered, never stored under `evidence/`, never named in an item.
   Looking at other products may produce *questions* for the owner; the
   owner's answer is the requirement and the owner is its source
   (`capture-loop.md` §4).
4. **Evidence is not authority.** What a prototype, mockup or legacy
   version does may be a bug. Observed behaviour enters as
   `confidence: observed`, `status: observed-provisional`; the owner
   classifies it as requirement, defect (`DEF-NNN` in the index), or out of
   scope. Nothing binds until the owner signs off (`status: locked` +
   `signoff:`) — `ai-approved` (ai-led collections) releases an item for a
   prototype build and is not sign-off.
5. **Quantify.** No `fast`, `many`, `secure`, `reasonable` in Rules or
   Acceptance. Unknowns are written as `OPEN QUESTION:` / `ASSUMPTION:`
   lines, never guessed. In an ai-led collection the agent decides rather
   than asks — and every decision is a visible `ASSUMPTION:` line on an
   `inferred` REQ, never a silent number.
6. **Inferences are marked.** A conclusion the agent drew rather than was
   told is `confidence: inferred` (whole REQ) or an inline
   `*(Inferred from …)*` on the rule. The trace report lists them; the
   owner confirms or removes them before lock.
7. **Features are simple, REQs carry the detail.** Feature tree first, then
   drill each leaf into REQs. A leaf is testable and visible, as small as
   possible, still valuable on its own.
8. **IDs are never reused or renumbered.** Withdrawn items keep their id
   with `status: withdrawn`.
9. **`views/` is generated.** Regenerate after every item change; if a view
   and an item disagree, the item is truth.
10. **`priority` and `signoff` belong to the owner.** The agent leaves
    `priority: none` and `signoff: null`.
11. **Validate before calling anything done.** `validate.py .` → 0 errors.

## Feedback to the framework

This collection is a client of the framework, not part of it. When the
format, a template or a tool gets in the way — a field that is missing, a
rule that fires wrongly, a step the capture loop does not cover — write it
in `framework-notes.md` here (dated, with the item it happened on). Do
**not** edit anything under `C:\GitHub\AI\SwReqCollector` from this folder. The notes
are worked through in a SwReqCollector session.

Material in this folder is this project's own and is never copied into
the framework repo, its example, or its inbox.
