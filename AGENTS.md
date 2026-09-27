# AGENTS.md: rules for AI agents in TangramNoAds

1. **Enjoy, it's absolutely free.** Never add ads, analytics, purchases, donation asks, network access or permissions. If a task seems to need one of these, stop and ask.
2. **Ideas are not requirements.** Jami's messages give ideas; record them in `Study/` with your take, then update `Spec/` and log decisions in `Spec/05-open-questions.md`.
3. **Spec before code.** Behaviour comes from `Spec/01-gameplay.md`, `02-ui-layout.md` and `04-requirements.md`. Cite REQ ids. Technology is parked until Jami opens that topic.
4. **Geometry is exact** in validation (`tools/tangram_geom.py`, a + b√2). Runtime locking follows `Study/06-snapping-by-anchors.md`: the anchors are the outline corners and placed-piece corners only, and every puzzle must pass V11 (buildable edge-first).
5. **Puzzles:** after touching `Tangrams/`, always run the validator and the renderer, and look at the preview sheet. Never set `provenance.reviewedByHuman` to `true`.
6. **Generated files:** `Spec/ui/*`, `Tangrams/previews/*` and `Spec/prototype/tangram-prototype.html` are outputs. Edit the generators in `tools/` (the prototype source is `tools/prototype_template.html`).
7. **Prototype ≠ spec.** When they disagree, the spec wins; fix the template and rebuild.
8. **Requirements live in `Requirements/`** (SwReqCollector format v2, ai-led capture). Work there under `Requirements/CLAUDE.md`: validate, regenerate views, never set `priority` or `signoff`. `Spec/04-requirements.md` is superseded.
9. **Puzzles live in `Tangrams/`**, separate from the requirements.
