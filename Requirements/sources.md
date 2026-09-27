# Source Registry — TangramNoAds

**Last updated:** 2026-09-27

> Every REQ must cite at least one SRC below. When sources conflict, prefer
> authoritative over secondary, and the later owner statement over the
> earlier one; record the conflict as an OPEN QUESTION on the affected REQ.
> Sources are this project's own material; other products are never
> registered here.

---

### SRC-001 — Project description

- **Kind:** document
- **Origin:** evidence/src-001-project-description.md — the owner's first message, verbatim
- **Captured:** 2026-09-27 by Jami (copied by AI)
- **Reliability:** authoritative
- **Evidence file:** evidence/src-001-project-description.md
- **Status:** usable
- **Notes:** Evidences purpose (an Android Tangram game), the drag-from-a-miniature-tray interaction, "highly easy to use", phone portrait with a narrower bottom and tablet with a wider bottom, and no ads (the project name). Its audience ("for kids") is superseded by SRC-002 (8+). It does not evidence limits or edge cases.

### SRC-002 — Owner conversation, round 1

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-27 08:13, reacting to the agent's first proposal
- **Captured:** 2026-09-27 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-002-conversation-round1.md
- **Status:** usable
- **Notes:** The owner framed these as "ideas, not requirements". Firm wording counts as stated; wording marked "maybe" or with a question mark supports only `inferred` items, and the REQ says so. It evidences: ages 8+ and no toddler modes; silhouette only with no drop places shown; tie-to-sides-and-other-pieces locking with no millimetre precision and no free drops; turning pieces; no resize; ‹ › browsing at the top; solved puzzles shown as a picture with retry; continue or restart a half-done puzzle; easy / medium / hard; skip; coloured pieces and a stylised solved picture; a free game with no money-making and no ads; per-puzzle and total active time tracking. The donation idea in it is withdrawn by SRC-003.

### SRC-003 — Owner conversation, round 2

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-27 08:39, reacting to the agent's prototype 0.2 report
- **Captured:** 2026-09-27 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-003-conversation-round2.md
- **Status:** usable
- **Notes:** Evidences: no support for placing a piece where nothing ties it (no hidden lock points); figures that need that ("gift box") are not supported; no donations of any kind; active play-time tracking kept; theme "enjoy, it's absolutely free".

### SRC-004 — The agent's concept study, specification draft 0.3 and prototype 0.3

- **Kind:** proposal
- **Origin:** ../Spec/ (vision, gameplay, UI layout, puzzle format, requirement candidates, open questions, UI sheets, prototype 0.3), ../Tangrams/ (10 puzzles with previews), ../tools/ (geometry reference, puzzle validator V1–V11, prototype builder) and the design notes ../Study/05-round1-feedback.md, ../Study/06-snapping-by-anchors.md, ../Study/08-round2-decisions.md
- **Captured:** 2026-09-27 by AI (Claude)
- **Reliability:** generated (the agent's own proposal: it evidences what was proposed, never that anyone wants it)
- **Evidence file:** ../Spec/ (entry: ../Spec/00-vision.md; prototype: ../Spec/prototype/tangram-prototype.html)
- **Status:** usable
- **Notes:** Supports `inferred` items only. The prototype was built by the agent. The owner's only use so far is recorded in SRC-008; as a source it evidences nothing firsthand. Every number in it (lock distances, timings, sizes) is an agent decision and appears in the REQs as an ASSUMPTION. Deliberately **not** part of this source: the other files in ../Study/, which contain notes about third-party products (capture-loop §4); they are never cited.

### SRC-005 — Research paper: touch interaction for children aged 3 to 6

- **Kind:** web page
- **Origin:** Vatavu, Cramariuc, Schipor, "Touch interaction for children aged 3 to 6 years: Experimental findings and relationship to motor skills", International Journal of Human-Computer Studies, 2015 (https://mintviz.usv.ro/publications/ijhcs2015.pdf)
- **Captured:** 2026-09-27 by AI
- **Reliability:** secondary
- **Evidence file:** evidence/src-005-touch-research-notes.md
- **Status:** usable
- **Notes:** Measured tap accuracy, tap duration, drag success and multi-touch success. The players studied are younger than our audience (8+), so it evidences the direction of the design rules (generous hit areas, a tap defined by movement rather than duration, never *requiring* two fingers), not their exact values for 8-year-olds.

### SRC-006 — Article: design for kids by stage of physical development

- **Kind:** web page
- **Origin:** Nielsen Norman Group, "Design for Kids Based on Their Stage of Physical Development" (https://www.nngroup.com/articles/children-ux-physical-development/)
- **Captured:** 2026-09-27 by AI
- **Reliability:** secondary
- **Evidence file:** evidence/src-006-kids-ux-notes.md
- **Status:** usable
- **Notes:** Recommends touch targets of at least 2 cm × 2 cm for children under 9. It evidences a minimum size guideline, not our layout.

### SRC-007 — Google Play Families policies

- **Kind:** web page
- **Origin:** Google Play Console Help, "Google Play Families Policies" (https://support.google.com/googleplay/android-developer/answer/9893335)
- **Captured:** 2026-09-27 by AI
- **Reliability:** secondary
- **Evidence file:** evidence/src-007-families-policy-notes.md
- **Status:** usable
- **Notes:** A regulation the project must meet, because the audience includes children under 13. It evidences obligations on data collection, ads and disclosures. It changes over time and must be re-checked before release.

### SRC-008 — Owner conversation, round 3

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-27 09:06 and 09:18, while the collection was being set up
- **Captured:** 2026-09-27 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-008-conversation-round3.md
- **Status:** usable
- **Notes:** Evidences: puzzles are held in their own folder, separate from the requirements; the first puzzles must be very easy (the figures were too hard for the owner to test with); puzzles need not depict anything (random shapes and a rectangle are fine); modes with non-traditional, non-7-piece sets come later.

### SRC-009 — Owner conversation, round 4

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-27 10:19, after testing prototype 0.3 in a web browser
- **Captured:** 2026-09-27 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-009-conversation-round4.md
- **Status:** usable
- **Notes:** The first report from using the prototype. Evidences: pieces are never resized (confirmed, "rule stays"); the tray should show which size a piece will have, especially small, medium and big of the same shape ("maybe", so `inferred`); small turn icons in the tray cells, so a piece can be turned before it is dragged ("maybe", so `inferred`); a very simple 3-piece puzzle for testing the solved state (firm). The owner calls these "at least temporary" aids for web testing.

---

## Change log

| Date | Change |
|---|---|
| 2026-09-27 | initial registry: SRC-001..007 (description, two owner conversations, the agent's proposal, three secondary references) |
| 2026-09-27 | SRC-008 owner conversation round 3 (puzzle folder, warm-ups, abstract shapes, future piece sets) |
| 2026-09-27 | SRC-009 owner conversation round 4 (no resize confirmed, tray size marks, tray turn buttons, 3-piece mini puzzles) |
