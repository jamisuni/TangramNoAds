# Requirements Index — TangramNoAds

**Status:** Draft · **Version:** 0.2 · **Last updated:** 2026-09-27
**Framework:** SwReqCollector format v2 (tools v0.4) at `C:\GitHub\AI\SwReqCollector`
**Capture purpose:** build
**Capture mode:** ai-led

> Next free ID: **REQ-046**. IDs unique project-wide per kind, never
> reused; features are identified by unique #Tags in `features.md`.
> Run `validate.py` + `views.py` after any item change; items are truth,
> views are generated. Sources are this project's own material; other
> products are never sources (`capture-loop.md` §4).

---

## 1. Context & goals

- **System described:** TangramNoAds, a classic 7-piece tangram game for Android phones and tablets. First capture: prototype 0.3 stage, 2026-09-27.
- **Problem / purpose:** a tangram for players aged 8 and up that is simple to *control* and honestly challenging to *solve*, and completely free: no ads, no purchases, no money asks. This collection is the requirement source for the build; `../Spec/` and `../Study/` are the agent's earlier drafts and notes.
- **Why ai-led:** the agent proposed the first version (study, spec draft, prototype 0.3) and the owner corrected it in short rounds (SRC-002, SRC-003, SRC-008) and then tried it in a web browser (SRC-009). Items the prototype already implements are released `ai-approved`. Nothing is locked: locking waits until the owner has **used** the prototype.
- **Success looks like:** the owner plays prototype 0.3 on a phone and a tablet, and every item gets a verdict: confirmed (→ `stated`), wrong (→ DEF-NNN or rewrite) or unused (→ `priority: none` / withdrawn).
- **Out of scope (whole capture):** technology and architecture (parked by the owner, SRC-002); toddler modes and hints; accounts, cloud sync and profiles; donations and any other income (SRC-003); non-classic piece sets (idea #PieceSets, later).

## 2. Stakeholders & actors

| Actor / role | Interest | Triggers behavior? | Approves rules? |
|---|---|---|---|
| Player, 8+ (school-age child or adult) | solves puzzles, wants controls that never fight back | yes (all play) | no |
| Parent | trusts the game: free, no ads, nothing collected | changes settings, resets progress | no |
| Owner (Jami) | the product vision; decides, uses the prototype, signs off | yes (reviews, playtests) | **yes** |
| Agent (Claude) | proposes, decides instead of asking, releases `ai-approved` for the prototype | no | no: `ai-approved` is not sign-off |

## 3. Collection inventory

| What | Where | Count / range | Status |
|---|---|---|---|
| Features | `features.md` | 47 (10 top-level, 37 leaves; 2 are ideas) | 1 area and 1 leaf `captured`, 2 `idea`, the rest `ai-approved` |
| Requirements | `reqs/` | REQ-001..045 (areas: promise, play, browsing, difficulty, time, settings, layout, content) | 44 `ai-approved`, 1 `draft` (REQ-042) |
| Shared types | `req_types.md` | TYPE-001..007 | draft |
| Sources | `sources.md` | SRC-001..009 | all usable; SRC-004 is `generated` |
| Domain model | `domain.md` | none yet | — |
| Views | `views/` | digest, feature-map, trace | generated |
| Puzzle library | `../Tangrams/` | 12 puzzles (2 mini, 4 warm-ups, 6 full puzzles) | not requirements: content that REQ-038..041 govern |

## 4. Capture coverage

**promise:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-001, 008–010) · quantified rules ✅ · entities n/a · timing n/a · edge cases ✅ · unwanted behavior ✅ · assumptions ✅ (no network = agent decision) · priorities ⏸ owner.

**play:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-002, 011–023, 043, 044) · quantified rules ✅ (TYPE-001/003/004, ASSUMPTIONs) · entities ✅ pieces, puzzles · timing ✅ animation times as ASSUMPTIONs · edge cases ✅ no-room turns, missed drops · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**browsing:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-003, 024–026, 034) · quantified rules ✅ TYPE-006 · entities ✅ puzzle state · timing n/a · edge cases ❓ list ends (ASSUMPTION: no wrap) · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**difficulty:** context ✅ · goals ✅ · actors ✅ · scope ❓ controls-only vs puzzle sets (Q1) · capabilities ✅ (REQ-004, 027, 028) · quantified rules ✅ TYPE-004 (all ASSUMPTIONs) · entities n/a · timing n/a · edge cases ✅ · unwanted behavior n/a · assumptions ✅ · priorities ⏸ owner.

**time:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-005, 029–031) · quantified rules ✅ TYPE-005 · entities ✅ · timing ✅ · edge cases ✅ idle, browsing away · unwanted behavior ✅ nothing leaves the device · assumptions ✅ · priorities ⏸ owner.

**settings:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-032–034) · quantified rules ✅ · entities n/a · timing n/a · edge cases ✅ reset confirmation · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**layout:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-006, 013, 035–037) · quantified rules ✅ TYPE-007, sizes as ASSUMPTIONs · entities n/a · timing n/a · edge cases ✅ rotation mid-puzzle · unwanted behavior n/a · assumptions ✅ · priorities ⏸ owner.

**content:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-007, 038–042, 045) · quantified rules ❓ puzzle count (REQ-042, draft) · entities ✅ puzzle file · timing n/a · edge cases ✅ V11 · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

## 5. Open questions & assumptions (project-wide)

- **Capture convention (SRC-002):** the owner calls his messages "ideas, not requirements". Firm wording ("I want", "no …", "lets …") is recorded as `stated`; wording marked "maybe" or with a question mark supports only `inferred` items, which name the idea in their Rationale.
- **First use.** The owner has tried the puzzles (SRC-008: too hard for a quick test, so warm-ups were added, REQ-041) and then prototype 0.3 in a web browser (SRC-009): he confirmed the no-resize rule and asked for tray size marks (REQ-043), tray turn buttons (REQ-044) and 3-piece mini puzzles (REQ-045), "at least temporary" for web testing. No phone playtest yet. Prototype 0.4 (`../Spec/prototype/tangram-prototype.html`) is the build for this cut. Its behaviour is the agent's proposal (SRC-004, `generated`), not evidence of intent. The next session registers the owner's playtest as a source and classifies every item.
- OPEN QUESTION (Q1 in `../Spec/05-open-questions.md`): should difficulty change the controls only (current ASSUMPTION, REQ-028) or offer different puzzle sets?
- OPEN QUESTION (Q2): keep tap-to-turn next to the two-finger twist (current ASSUMPTION, REQ-016)?
- OPEN QUESTION (Q5): keep the landing preview on Easy and Medium (REQ-021)?
- OPEN QUESTION (Q6): timer on screen by default only on Hard (REQ-031)?
- OPEN QUESTION (Q8): how many puzzles and themes in the first release (REQ-042, draft)?
- OPEN QUESTION: should the parallelogram's mirror be part of the game at all (REQ-018)? The owner has not mentioned it.
- OPEN QUESTION (Q12): keep the tray turn buttons and size marks after testing, and show the buttons on touch screens too (REQ-043, REQ-044 ASSUMPTIONs)?
- OPEN QUESTION: no network use at all (REQ-010)? This is the agent's reading of "absolute free".
- ASSUMPTION: the classic seven pieces for every first-version puzzle; other piece sets later (#PieceSets, SRC-008).
- **Framework conformance note:** `../Study/01`–`04` and `07` contain notes about third-party products. Under `capture-loop.md` §4 such material is never a source and should not live in the project. It is kept out of every SRC here; the owner decides whether those files stay.

## 6. Defect list

> Observed behaviors the human classified as **defects**, never REQs. None yet: the owner's only feedback so far (SRC-008, puzzles too hard to start with) became a new requirement (REQ-041), not a defect.

| ID | Observed behavior | Sources | Decision |
|---|---|---|---|
| — | — | — | — |

## 7. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-09-27 | initial ai-led capture: 44 features, REQ-001..042, TYPE-001..007, SRC-001..008; 41 REQs released `ai-approved` for prototype 0.3, REQ-042 draft | move the TangramNoAds study and spec into SwReqCollector format (owner request) | — |
| 0.2 | 2026-09-27 | SRC-009; new #SizeMarks, #TurnButtons, #PuzzleMini with REQ-043..045 (ai-approved for prototype 0.4); REQ-012, 013, 014, 022, 025, 038, 041 and REQ-002 A1 updated for mini puzzles and the confirmed no-resize rule | owner round 4 after web testing | — |
