# Framework notes — TangramNoAds

Findings about **SwReqCollector itself** while capturing this system: what
was awkward, what a rule got wrong, what the capture loop did not cover,
what a tool should have caught. This is the feedback channel from a real
project back to the framework. It is *not* about the system being described
— that goes in the items.

Framework in use: SwReqCollector format v2 (tools v0.4) at `C:\GitHub\AI\SwReqCollector`.

Worked through in a SwReqCollector session; each note is closed there with
what changed (or why nothing did). Never edit the framework from here.

## Open

| Date | While doing | Finding | Proposal |
|---|---|---|---|
| 2026-09-27 | SRC-004, whole capture | **Retroactive ai-led capture.** §12 assumes PROPOSE → AI-APPROVE → BUILD in that order. Here the study, spec draft and prototype existed *before* the collection, so `ai-approved` records a release that has already happened. | Add a note in `capture-loop.md` §12 for "capture after the first prototype": register the existing proposal and build as a `generated` SRC and approve exactly what the build implements. |
| 2026-09-27 | SRC-004 | The proposal lives outside the collection (`../Spec/`, `../Tangrams/`, part of `../Study/`) because the owner wanted the study folder left untouched. `storage-layout.md` expects `study/` inside the collection. | Allow a proposal SRC to point outside the collection folder, and say so in `storage-layout.md`. |
| 2026-09-27 | SRC-002 and most REQs | The owner said "I give ideas, not requirements", with some items marked "maybe". There is no standard way to record a tentative owner statement: it is either `stated` (too strong) or `inferred` (loses that the owner said it). The capture used a written convention (index §5). | Consider a documented convention, or a `stated-tentative` reading of `stated` that cannot lock until re-confirmed. |
| 2026-09-27 | req_types.md | ASSUMPTION lines inside TYPE entries (TYPE-001, 004, 005, 007) are not listed in `views/trace.md` → "Decisions the agent took instead of asking", only REQ assumptions are. For this game most numbers live in TYPEs. | Have `views.py` also list TYPE assumptions, or state that ai-led numbers belong in REQs. |
| 2026-09-27 | validate run | `idea` leaves (#PieceSets, #Accessibility) warn "leaf feature with no REQs (drill-down pending)". An idea has no REQs by definition (`req-format.md` §2). | Skip the drill-down warning for `idea` features. |
| 2026-09-27 | project folder | `capture-loop.md` §4 says material about other products is stored "nowhere in the project". The project's `Study/` (written before the collection existed, at the owner's request) contains a market scan that names products. The collection never cites it, but the rule is broken at project level. | Clarify whether §4 binds the collection folder or the whole repository, and what to do with pre-existing study material. |

## Closed

| Date | Finding | Outcome (framework change, or reason for none) |
|---|---|---|
| — | — | — |
