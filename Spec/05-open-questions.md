# 05 · Open questions and decisions

Questions for Jami. Each row gives the current assumption (what the prototype does) until it is decided.

| # | Question | Current assumption | Notes |
|---|---|---|---|
| Q1 | Is Easy / Medium / Hard as a **control-forgiveness** setting right (the same puzzles for all)? Or should difficulty pick **different puzzle sets**? | Control setting; puzzles are ordered by their own 1–5 rating | `Study/05` §"Suggested meaning" |
| Q2 | Keep **tap-to-turn** alongside the two-finger twist? | Yes, both | One-handed phone play needs a one-finger way. |
| Q5 | Should the **landing preview** exist at all (Easy/Medium)? | Yes, off on Hard | It shows a *valid* spot, not the *correct* one. |
| Q6 | Timer on screen by default? | Hard only | Pressure vs motivation for 8-year-olds. |
| Q8 | Puzzle count and themes for v1? | ≥ 40, 6 themes | AI drafts plus human review. |
| Q9 | Store name? "TangramNoAds" is the repo name. | TBD | Check for name collisions. |
| Q11 | V11 rejects figures where a piece can only touch the outline in the middle of a straight side and no neighbour gives it a corner. Is that acceptable for the content we want? | Yes: design puzzles around it | So far all 5 puzzles pass. |
| Q12 | Keep the tray **↺ ↻ buttons** and **S / M / L marks** after testing? Show the buttons on touch screens too? | Keep both, on every device, until the phone playtest | Jami asked for them "at least temporary" for web testing. |
| Q10 | Any hint at all (e.g. "show one piece" after 5 min)? | No; skip instead | Jami: "we don't make it too easy". |

## Decision log
| Date | Decision | By |
|---|---|---|
| 2026-09-27 | Phone portrait (2-row tray) + tablet (1-row tray); no ads; no network. | Jami |
| 2026-09-27 | Target ages 8+; no toddler modes; no gift pieces; silhouette only. | Jami (round 1) |
| 2026-09-27 | Slot-free locking against the silhouette and other pieces; a drop locks or goes home. | Jami idea, design in `Study/06` |
| 2026-09-27 | ‹ › browsing, per-puzzle state, Restart / Retry, skip with ›. | Jami (round 1) |
| 2026-09-27 | The solved puzzle turns into a stylised picture with the same outline. | Jami (round 1) |
| 2026-09-27 | Technology discussion parked; focus on study and prototype. | Jami (round 1) |
| 2026-09-27 | No hidden anchors: pieces tie only to outline corners and placed pieces; puzzles must be buildable edge-first (V11); the Gift box puzzle was removed. | Jami (round 2) |
| 2026-09-27 | Puzzles live in `Tangrams/`; abstract shapes and rectangles are fine; easy warm-ups first. Non-traditional modes with other piece sets come later. | Jami (round 3) |
| 2026-09-27 | Piece size is never scaled (rule stays). Tray gets S / M / L size marks on the triangles and ↺ ↻ turn buttons per cell. Mini 3-piece puzzles open the list for fast "solved" testing. | Jami (round 4) |
| 2026-09-27 | No donations of any kind. Theme: "Enjoy, it's absolutely free." Active play time stays (local only). | Jami (round 2) |
