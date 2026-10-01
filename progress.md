# Progress Log — TangramNoAds (build side, SWDev)

Append-only. One entry per interaction:

- the human's input, **VERBATIM**
- a **~10-line** summary of what the AI did

This log is the ground truth the SWDev framework reads to improve itself
(see `C:\GitHub\AI\SWDev\framework\feedback\`). Capture-side history lives
in `Requirements/` (sources, evidence, change log) and `STATUS.md`.

---

## 2026-10-01 18:52 — adopted under SWDev v0.15 (intake path C)

**Human (verbatim):**

> k.. I wanna be able to run full planned requirement, feature etc from project under SwDev.. plz do steps as you propose what you can do automatic.. we need to figure here project mode where AI is more control on these smaller projects if I label something to AI-mastered project. For questions you need answer to tune frameworks, plz give me more human readable description from problem, proposal and question

**What was done (summary):**

- Owner decision recorded: the whole locked collection (BIZ/FUN/UI) is the
  spec for the build — SWDev S-01 (`SWDev/swreq-intake-review-2026-10.md`).
- SWDev v0.15 landed in the framework repo (trace-check v0.4 reads the
  collection natively; `build-map.md` + `design-inputs.md` templates;
  intake path C in `initnewproject.md` §2b; principle 2 reworded).
- This project scaffolded beside `Requirements/` (nothing written into it):
  `architecture.md` (placeholder), `build-map.md` (subtree rows, code homes
  TBD until technology is chosen), `design-inputs.md` (DI-1…DI-7 with the
  Contract-delta), `tasks.md`, `proposals.md`, this file,
  `.swdev/guard.json` (locked REQ files denied to the build side),
  `.claude/settings.json` (SWDev hooks), `AGENTS.md` § "Build under SWDev".
- trace-check on this project: 51 REQs parsed (47 locked, BIZ 7 / FUN 29 /
  UI 11), 53 features (37 build units), req-lint 0 errors, views fresh —
  RED only for uncovered acceptance (no product code yet) and the missing
  contract baseline (frozen by Jami at G1 acceptance).
- Phase: P1 — acceptance of the locked collection. Pending: fresh-eyes
  review, "Accept collection v1.1 for TangramNoAds (G1)?", `--freeze`.
- Open for the owner: the AI-mastered build mode
  (`SWDev/decisions-needed-2026-10.md` Q1–Q6) and the technology topic.

## 2026-10-01 20:12 — governance in force, stack decided

**Human (verbatim):**

> A. Kotlin + Jetpack Compose (native) is ok if can build it my computer. Anyway questions OK but need to jump Claude Code windows terminal soon for builds. And we do not build today as running out of tokens / time.. but get everything here ready with you

**What was done (summary):**

- `governance.md` → v0.2 **in force** (ai-mastered), set by Jami: floors
  F1–F4 confirmed, data migration dialable, prototype lane yes, checkpoint
  per WO close, fresh-eyes review to run (answers to
  `SWDev/decisions-needed-2026-10.md`).
- Row 5 technology = **owner-decided: Kotlin + Jetpack Compose, native**,
  conditional on the G3 toolchain proof passing on Jami's computer; if it
  cannot, stop and ask — never switch stacks silently. To be recorded as
  ADR-001 in `architecture.md` at P2.
- Decisions mirrored in SWDev (`gates-and-autonomy.md` §8, `TODO.md` T-05)
  and SwReqCollector (`req-format.md` §6, `capture-loop.md` §12.4, plan,
  CHANGELOG — the prototype lane).
- `KICKOFF.md` written: toolchain checklist + the copy-paste prompt for
  the first Claude Code terminal session (P1 acceptance → P2 → WO-001
  #Locking). No build today.
- trace-check: governance profile IN FORCE recognised; RED only for
  uncovered acceptance + missing baseline (freeze at G1).

