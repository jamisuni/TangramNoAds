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
