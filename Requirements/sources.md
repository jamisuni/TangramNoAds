# Source Registry — TangramNoAds

**Last updated:** 2026-09-28

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

### SRC-010 — Owner conversation, round 5

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-27 10:52 and 12:36, while testing prototype 0.4 in a web browser
- **Captured:** 2026-09-27 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-010-conversation-round5.md
- **Status:** usable
- **Notes:** Evidences: a question on whether the parallelogram needs a mirror button (answered, no decision); the tray turn buttons stay for now, possibly not in the long run; a development-time solution reveal behind the passcode 0417; the owner's expectation that this collection can feed a separate build framework.

### SRC-011 — Owner conversation, round 6

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-09-28 06:29, in reply to the agent's concept review of the whole project (requirements, spec, prototype 0.5, puzzles)
- **Captured:** 2026-09-28 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-011-conversation-round6.md
- **Status:** usable
- **Notes:** Evidences, in the owner's own words: the tray ↺ ↻ turn buttons go (tapping turns a piece); the Easy / Medium / Hard levels go (no design for them at the moment; the puzzle rating is the difficulty); the flip badge shows always; the store name "Tangram, absolutely free". Everything else in the review is accepted by reference ("I agree with all your proposals.. plz do"); the evidence file lists those proposals (F1–F8, G1–G8, D4–D8, N1–N3, N6) so the acceptance is traceable. The agent's own detail choices inside the accepted proposals (numbers, defaults, wording) stay ASSUMPTION lines. The distribution-account question was not answered.

### SRC-012 — Google Play User Data policy (privacy policy)

- **Kind:** web page
- **Origin:** Google Play Console Help, "User Data" policy (https://support.google.com/googleplay/android-developer/answer/10144311)
- **Captured:** 2026-09-28 by AI
- **Reliability:** secondary
- **Evidence file:** evidence/src-012-play-user-data-policy-notes.md
- **Status:** usable
- **Notes:** A regulation the release must meet on Google Play. It evidences that every app needs a privacy policy, even one that collects nothing, linked in the Play Console and present as a link or text inside the app. It changes over time and must be re-checked before release.

### SRC-013 — Google Play testing requirements for new personal developer accounts

- **Kind:** web page
- **Origin:** Google Play Console Help, "App testing requirements for new personal developer accounts" (https://support.google.com/googleplay/android-developer/answer/14151465) and the policy announcement of 15 July 2026 (https://support.google.com/googleplay/android-developer/answer/17134731)
- **Captured:** 2026-09-28 by AI
- **Reliability:** secondary
- **Evidence file:** evidence/src-013-play-testing-requirements-notes.md
- **Status:** usable
- **Notes:** Evidences the closed-test obligation for personal accounts created after 13 November 2023 (12 testers, 14 days) and that an unrated app is not permitted on Play. It is planning evidence for the release, not a rule of the game; the index §5 carries the open question which account publishes.

### SRC-014 — Owner sign-off: lock all approved requirements

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-10-01 18:08
- **Captured:** 2026-10-01 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-014-owner-signoff.md
- **Status:** usable
- **Notes:** The owner's sign-off. Evidences the lock of the 47 `ai-approved` REQs and, through it, his confirmation of the agent's inferred items, inferred rules and ASSUMPTIONs in them as written on 2026-10-01. It sets no priorities.

### SRC-015 — Owner sign-off: lock the features

- **Kind:** conversation
- **Origin:** chat with Jami, 2026-10-01 18:20
- **Captured:** 2026-10-01 by Jami + AI
- **Reliability:** authoritative
- **Evidence file:** evidence/src-015-owner-signoff-features.md
- **Status:** usable
- **Notes:** The owner's sign-off for the feature tree: 51 of 53 features locked; the two `idea` features were left as ideas (see the evidence notes).

---

## Change log

| Date | Change |
|---|---|
| 2026-09-27 | initial registry: SRC-001..007 (description, two owner conversations, the agent's proposal, three secondary references) |
| 2026-09-27 | SRC-008 owner conversation round 3 (puzzle folder, warm-ups, abstract shapes, future piece sets) |
| 2026-09-27 | SRC-010 owner conversation round 5 (mirror question, turn buttons kept for now, developer solution reveal) |
| 2026-09-27 | SRC-009 owner conversation round 4 (no resize confirmed, tray size marks, tray turn buttons, 3-piece mini puzzles) |
| 2026-09-28 | SRC-011 owner conversation round 6 (concept review accepted: buttons and levels removed, flip badge always, store name, all review proposals); SRC-012 Play User Data policy; SRC-013 Play testing requirements |
| 2026-10-01 | SRC-014 owner sign-off: lock of all 47 ai-approved REQs |
| 2026-10-01 | SRC-015 owner sign-off: lock of the feature tree (51 of 53) |
