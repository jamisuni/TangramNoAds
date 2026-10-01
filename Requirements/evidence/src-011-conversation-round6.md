# SRC-011 — Owner conversation, round 6 (2026-09-28 06:29)

Verbatim message from the owner (Jami), in reply to the agent's concept
review of the whole project (requirements, spec, prototype 0.5, puzzles).
The review's proposals are listed below the message, because the owner
accepts them by reference ("I agree with all your proposals").

---

> We can actually remove those spinners [[REQ-037 says every control is at
> least 48 dp; the tray ↺ ↻ buttons are about 29 dp]] as didnt realize
> earlier I can just press it and it spins. D1 - I think those
> easy/medium/hard can be removed, I have no idea atm how to do it. D2 ok.
> D3 can drop already, pressing is enough. D9 I like it 'Tangram,
> absolutely free'. Actually yes I agree with all your proposals.. plz
> do.. good work

---

## What the owner decided in his own words

- The tray ↺ ↻ turn buttons go: tapping a tray piece turns it, and that is
  enough (REQ-044 withdrawn; Q12 closed; Q2 closed: tap-to-turn stays).
- The Easy / Medium / Hard levels go. The owner has no design for them at
  the moment; the puzzle rating (1–5 dots) is the only difficulty
  (REQ-004, REQ-027, REQ-028, TYPE-002 withdrawn; Q1 closed).
- D2 accepted: the ⇋ flip badge for the parallelogram shows always.
- D9 accepted: the store name is **"Tangram, absolutely free"** (Q9 closed).
- Every other proposal of the review is accepted as written ("plz do").

## The proposals accepted by reference

The review (agent, 2026-09-28) is the agent's own material and is not a
source; this list records what "all your proposals" refers to, so the
acceptance can be traced. Items already decided above are not repeated.

| # | Proposal accepted |
|---|---|
| F1 | Rule V11 (buildable edge-first) cannot reject any valid tangram: the uncovered region always has a convex corner, which is always an anchor, and the piece covering it must have a vertex there. Keep V11 only as the printed build order; drop the edge-first clause from REQ-038; close Q11 as moot; correct Spec/01 §4, Spec/03 and Tangrams/README; bring the classic square puzzle back (Study/06 and 08 stay frozen; the correction goes to the decision log). |
| F2 | The prototype wraps the puzzle list, REQ-024 said it does not: decide for **wrap** (‹ on the first puzzle shows the last, › on the last shows the first). |
| F3 | "Active today" resets at UTC midnight in the prototype instead of local midnight (REQ-029): register as DEF-001, fix the prototype. |
| F4 | REQ-037 (48 dp minimum) contradicts REQ-044 (≈29 dp buttons): resolved by removing the buttons; exempt the #DevTools controls in REQ-037. |
| F5 | The lock choice (distance, snug-corner bonus, candidate window) is written into TYPE-004 as assumptions so a build from the REQs locks like the prototype. |
| F6 | Exactness: state in REQ-019 how "coincides" and "overlaps" are decided (exact ℚ(√2) or tolerance ≤ 1e-6 units after exact translation from anchors). |
| F7 | Puzzle format gains `kind: mini | warmup | full` (replacing the `mini` boolean); the list sorts by (kind, rating, id); new validator rule V12 checks the warm-up measure (at least half of every piece's outline on the silhouette edge). |
| F8 | TYPE-006: a puzzle becomes In progress when a piece first leaves the tray; time counts only In progress; Restart returns it to New. |
| G1 | New feature #Language: follow the device language, ship Finnish and English, fall back to English; puzzle titles from `title[lang]`; V8 requires both titles. S / M / L size marks stay letters. |
| G2 | New area #Release: one BIZ REQ for store obligations (privacy policy text, Data safety, content rating, target audience, Families policy, Teacher Approved opt-in) and one UI REQ for a privacy-policy text in Settings next to the free note. |
| G3 | Distribution: record in the index the open question which developer account the game will be published from (a new personal account needs 12 testers for 14 days) and F-Droid as a channel to consider. |
| G4 | With more than about 15 puzzles: a #PuzzleOverview leaf — long-press › jumps to the next unsolved puzzle; tapping the "n / N" counter opens a grid of silhouettes (solved ones in colour). |
| G5 | Best time per level: moot once levels are removed. |
| G6 | REQ-025: a saved piece that is no longer a valid lock against the current silhouette returns to the tray on load. |
| G7 | Fold Spec/02 §6 (back button, insets, gesture bar, orientation) into REQ-035/036, and reduced motion into REQ-023. |
| G8 | One list of open questions: the index §5; Spec/05 keeps the decision log and points at it. |
| D4 | Keep the landing preview (always on now). |
| D5 | Teach the corner rule once without a hint system: in a mini puzzle, when a drop goes home, the outline corners pulse once. |
| D6 | First release: 20–25 hand-checked puzzles over four themes plus the classic square; forty is the second release (REQ-042 becomes ai-approved with 20). |
| D7 | Timer on screen: default off; best time always on the solved card. |
| D8 | Haptics: a short haptic tick on lock is allowed as long as it needs no permission (REQ-010 stays "no permissions"). |
| N1 | Spec/01 stale draft ids (REQ-TRY-*, …) replaced by collection ids. |
| N2 | Validator rule V7 (assist.preplacedOrder) removed as dead. |
| N3 | framework-notes: the ai-led loop should require a "§12.3 asked" checklist (legal duties, outside systems, money, first-version size). |
| N6 | Warm-up pictures are weak rewards: give them a picture worth seeing, or a name for what the shape vaguely is (content work, next session). |
| — | Several player profiles on one tablet are a release-two candidate; REQ-025 notes that progress is stored per profile, with one profile in the first version. |

## Notes

- Everything above is `stated` where the owner named it himself (spinners,
  levels, D2, D9) and `stated` by acceptance for the listed proposals; the
  agent's own detail choices inside them (numbers, wording, defaults) stay
  ASSUMPTION lines on the REQs.
- The owner did not answer the distribution-account question (G3); it stays
  an open question in the index.
