# 05 · Open questions and decisions

**The open-question list moved** (round 6) to `../Requirements/requirements.md` §5, which is now the only list; this file keeps the decision log. Questions Q1–Q13 of drafts 0.2–0.3 are closed as logged below, except the two that stay open in the index: which developer account publishes the game (a new personal Play account needs a 12-tester closed test), and the Finnish store title.

## Decision log
| Date | Decision | By |
|---|---|---|
| 2026-09-27 | Phone portrait (2-row tray) + tablet (1-row tray); no ads; no network. | Jami |
| 2026-09-27 | Target ages 8+; no toddler modes; no gift pieces; silhouette only. | Jami (round 1) |
| 2026-09-27 | Slot-free locking against the silhouette and other pieces; a drop locks or goes home. | Jami idea, design in `Study/06` |
| 2026-09-27 | ‹ › browsing, per-puzzle state, Restart / Retry, skip with ›. | Jami (round 1) |
| 2026-09-27 | The solved puzzle turns into a stylised picture with the same outline. | Jami (round 1) |
| 2026-09-27 | Technology discussion parked; focus on study and prototype. | Jami (round 1) |
| 2026-09-27 | No hidden anchors: pieces tie only to outline corners and placed pieces. *(The second half of this decision, "puzzles must be buildable edge-first (V11); the Gift box puzzle was removed", rested on a wrong belief and was reversed on 2026-09-28, see below.)* | Jami (round 2) |
| 2026-09-27 | Puzzles live in `Tangrams/`; abstract shapes and rectangles are fine; easy warm-ups first. Non-traditional modes with other piece sets come later. | Jami (round 3) |
| 2026-09-27 | Developer solution reveal behind passcode 0417 (prototype/test builds only). Tray ↺ ↻ kept for now. | Jami (round 5) |
| 2026-09-27 | Piece size is never scaled (rule stays). Tray gets S / M / L size marks on the triangles and ↺ ↻ turn buttons per cell. Mini 3-piece puzzles open the list for fast "solved" testing. | Jami (round 4) |
| 2026-09-27 | No donations of any kind. Theme: "Enjoy, it's absolutely free." Active play time stays (local only). | Jami (round 2) |
| 2026-09-28 | **Tray ↺ ↻ buttons removed** ("I can just press it and it spins"); tap-to-turn stays (Q2, Q12 closed). | Jami (round 6) |
| 2026-09-28 | **Easy / Medium / Hard removed**; the puzzle rating is the only difficulty; one lock rule for everyone (TYPE-004). Timer on screen is a plain on/off setting, off by default (Q1, Q6 closed). | Jami (round 6) |
| 2026-09-28 | **Flip badge ⇋ always shown** next to the parallelogram; no automatic mirroring (Q13 closed). | Jami (round 6) |
| 2026-09-28 | **Store name "Tangram, absolutely free"** (Q9 closed); the Finnish title is still open. | Jami (round 6) |
| 2026-09-28 | **V11 cannot reject a valid tangram** (the uncovered region always has a convex corner, which is always an anchor). It stays as a printed build order; the edge-first clause left REQ-038; the classic square (Gift box) is back in the library (Q11 closed). | agent finding, accepted by Jami (round 6) |
| 2026-09-28 | The puzzle list wraps (REQ-024). "Active today" resets at local midnight; the prototype's UTC date was DEF-001. | Jami (round 6) |
| 2026-09-28 | Puzzle format: `kind: mini / warmup / full` replaces the `mini` flag; V12 checks the warm-up measure; V7 removed; both titles mandatory. | Jami (round 6) |
| 2026-09-28 | Landing preview stays, always on (Q5 closed). In a mini puzzle a missed drop pulses the outline corners once (REQ-051); no hints otherwise (Q10 stands). | Jami (round 6) |
| 2026-09-28 | First release: at least 20 hand-checked puzzles over four themes, including the classic square; 40 is the second release (Q8 closed, REQ-042). | Jami (round 6) |
| 2026-09-28 | Finnish and English following the device language (REQ-047); store obligations as a #Release area with the privacy text in Settings (REQ-048, REQ-049); long-press › and the grid overview (REQ-050); saved pieces that no longer fit go back to the tray (REQ-025); permission-free haptic tick on lock (REQ-033). | Jami (round 6, accepting the concept review) |
| 2026-10-01 | All 47 approved requirements locked (`Requirements/`, v1.0, SRC-014), including the agent's inferred items and assumptions as written. Later changes go through change deltas. | Jami (sign-off) |
| 2026-10-01 | Feature tree locked (51 of 53; SRC-015). The ideas #PieceSets and #Accessibility stay ideas, outside the first version. | Jami (sign-off) |
