# Design inputs — TangramNoAds

**Status:** Current  ·  **Last updated:** 2026-10-01
**Contract:** `Requirements/` collection v1.1 — REQ-001..051 (47 locked, 4 withdrawn), TYPE-001..007, locked 2026-10-01 (SRC-014, SRC-015)

> Inputs the pipeline must honor beside the requirements. Each is named in
> the work orders that use it (`workorders/WO-NNN.md` → Design inputs) and
> in the briefs' "Read exactly". **Contract wins over any input**; §2 lists
> the known disagreements. Every input below except DI-6 is the agent's own
> proposal — registered on the capture side as **SRC-004, Reliability
> `generated`**: it evidences what was proposed, never that anyone wants it.
> The owner has used the prototype only in a web browser with a mouse; no
> phone or tablet playtest yet (`STATUS.md`).

---

## 1. Register

| # | Path | Role | Source / provenance | Status | Used by |
|---|---|---|---|---|---|
| DI-1 | `Spec/prototype/tangram-prototype.html` (source: `tools/prototype_template.html`, built by `tools/build_prototype.py`) | reference prototype | agent, prototype 0.6 (2026-09-28), SRC-004 `generated`; browser tests `tools/tests/test_prototype.py` | current | every play/browse/settings WO — behaviour reference only; **never** a source of acceptance tests |
| DI-2 | `Spec/02-ui-layout.md` + `Spec/ui/01–06*.png` | explanatory spec (layout algorithm, zones, sizes, visual language; annotated screenshots generated from DI-1) | agent, draft 0.4, SRC-004 | current | #Layout, #Solving (tray), #Settings WOs |
| DI-3 | `Spec/01-gameplay.md` | explanatory spec (gameplay in prose) | agent, SRC-004 | current | #Solving, #Browsing WOs |
| DI-4 | `Study/06-snapping-by-anchors.md` (+ `Study/08-round2-decisions.md`) | explanatory spec (the lock algorithm; frozen folder) | agent, round 1–2, SRC-004 | current, **frozen** — contains one corrected belief, see §2 | #Locking WO |
| DI-5 | `Spec/03-puzzle-format.md`, `Tangrams/puzzle.schema.json`, `Tangrams/*.json` (13 puzzles + previews), `tools/validate_puzzles.py` (V1–V6, V8–V10, V12; V11 prints the build order), `tools/tangram_geom.py`, `tools/render_puzzle.py` | content pipeline | agent + owner-reviewed decisions (round 3, 6); puzzles not yet human-reviewed (`provenance.reviewedByHuman` false) | current | #Content WO; **governed-interface candidate** `IPuzzleLibrary` (locked tier) at P2 |
| DI-6 | `AGENTS.md` rules 1, 6, 7, 11, 12, 13 | reference material (owner's standing rules) | owner (Jami) | current | project guardrail material for `architecture.md` §2 |
| DI-7 | `Spec/00-vision.md`, `Spec/05-open-questions.md` (decision log) | reference material | agent + owner decisions | current | orientation only |

Not an input: `Spec/04-requirements.md` (superseded draft; its REQ-XXX-n
labels are not the collection's ids), `Study/01–04`, `Study/07` (third-party
product notes — never cited, never read by workers), `Study/archive/`.

## 2. Contract-delta — where inputs disagree with the Contract

> Contract wins. The prototype-vs-REQ disagreements found in round 6 were
> settled *before* the lock, so this list is short — which is itself the
> statement: as of 2026-10-01 **no known open conflict** between DI-1…DI-5
> and the locked collection.

### Superseded (do NOT follow these)

1. DI-4 `Study/06` + `Study/08`: "puzzles must be buildable edge-first (V11);
   the Gift box puzzle was removed" → **corrected 2026-09-28**: V11 cannot
   reject a valid tangram; it is a printed build order for authors, not a
   gate (REQ-038 lost the edge-first clause; `Tangrams/README.md`;
   AGENTS.md rule 6). Never remove a puzzle "because of V11".
2. DI-1 prototype 0.5 and earlier: tray ↺ ↻ buttons, Easy/Medium/Hard
   levels, per-level lock parameters → **withdrawn** (REQ-044, REQ-004/027/
   028, TYPE-002). Prototype 0.6 already reflects this; the archived
   drawings under `Study/archive/` do not.
3. DI-1 prototype 0.5: "Active today" reset at UTC midnight → **DEF-001**;
   REQ-029 says local midnight; fixed in 0.6.
4. DI-2 §5 type face "e.g. *Baloo 2*" and the exact colour values are the
   agent's choices (TYPE-001 colours are ASSUMPTIONs confirmed by the lock);
   they bind only where a REQ/TYPE names them.

### Open forks (owner decision needed)

- none as of 2026-10-01. *(Watch: the phone/tablet playtest — `STATUS.md`
  next step 1 — may move the lock distance R (TYPE-004, "the phone playtest
  may move R"); that is a capture-side `CHG`, not a design-input question.)*

### Also fine (input and Contract agree — worth noting)

- DI-1's behaviour for lock/go-home/landing preview/corner pulse matches
  REQ-019/020/021/051 and TYPE-004 as locked; the prototype's browser tests
  exercise every puzzle on 390×844 and 1280×800 / 800×1280 (REQ-035/036 A1).
- DI-5's `kind: mini | warmup | full` and both-titles rule match REQ-045,
  REQ-041, REQ-047.

## 3. Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 0.1 | 2026-10-01 | initial register from `README.md`, `STATUS.md`, `Spec/`, `Requirements/sources.md` SRC-004 | project adopted under SWDev v0.15, intake path C |
