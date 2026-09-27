# 05 · Round 1: Jami's ideas and how the design answers them

> **Round 2 update:** hidden anchors and donations were both dropped. See `08-round2-decisions.md`. The rows about them below are superseded.

*2026-09-27. Jami's message gave **ideas, not requirements**. For each idea: what was said, my take, and what prototype v0.2 does. Where I suggest something different, the reason is stated so it can be rejected easily.*

## Big changes compared with draft 0.1
| Draft 0.1 | Round 1 direction |
|---|---|
| Ages 3–8, four assist levels, toddler colour matching | **Ages 8+.** Start from the old level 4 (silhouette only, you turn the pieces). No toddler modes. |
| Gift pieces pre-placed | **None.** |
| Pieces snap into hidden *solution slots* | **No slots.** Pieces lock onto the silhouette sides and onto other pieces. Any correct arrangement wins. |
| Hints (pulse, auto-place) | **No hints.** Instead you can **skip**: › moves to the next puzzle at any time. |
| Home → worlds → grid navigation | **‹ puzzle ›** browsing at the top, plus ⚙ settings. |
| Coloured pieces become the solved figure | Coloured pieces **turn into a stylised picture** (a real-looking house, cat…) with exactly the same outline. |
| No analytics at all | Per-puzzle **solve time** and total **active play time**, kept on the device only. They are used for one polite **donation ask per day** after 15 minutes of play. |
| Technology proposal (Kotlin/Compose) | **Parked.** Focus is on studying and prototyping. Moved to `Study/archive/round-0/`. |

## Idea by idea

| # | Jami's idea | My take | Prototype v0.2 |
|---|---|---|---|
| 1 | Drag and drop, as in the store apps | Agreed. It is the expected core. | ✔ |
| 2 | Two-finger spinning in fixed 45° steps | Agreed, and it suits 8+ (the research found multi-touch hard only for ages 3–6). **Addition:** also keep **tap = turn 45°**, because one-handed phone play and mouse testing need a one-finger way. Twist while dragging (thumb holds, second finger twists) is the most natural. | ✔ tap; ✔ two-finger twist (45° steps with a tick); ✔ mouse wheel / R key on desktop |
| 3 | No resizing | Agreed. The pieces always have their real size on the board; only the tray miniatures are smaller. | ✔ |
| 4 | Drop places are not shown ahead of time; the layout is empty | Agreed. Only the silhouette is visible, with no inner lines. | ✔ |
| 5 | Pieces tie to the puzzle sides and to other pieces; no millimetre precision | Agreed. This is the heart of the design, see `06-snapping-by-anchors.md`. **Refinement:** the lock points are the silhouette corners, the corners of pieces already placed, and the (invisible) corner points of the puzzle's own solution. Without the last group, a piece that only touches the edge in its middle (like the square in the gift box) could never lock when placed first. | ✔ |
| 6 | Not free drops: a drop must still be easy | Agreed. **A drop has exactly two outcomes:** the piece locks into a valid spot (fully inside the silhouette, no overlap) near where it was dropped, or it glides back to the tray. It never lies loose at a random angle or position. | ✔ |
| 7 | ‹ and › at the top to move between puzzles | Agreed. The top bar reads ‹ Cat · 3/6 › ⚙. | ✔ |
| 8 | Browsing shows solved puzzles as solved, without revealing piece positions, plus a retry | Agreed. A solved puzzle shows its **picture** (outline only, no piece lines) with ↻ Retry and the best time. | ✔ |
| 9 | Leave a puzzle half way, come back and continue or restart | Agreed. Every puzzle keeps its own state (placed pieces, turns, time). A ⟲ Restart button shows while a puzzle is in progress. | ✔ |
| 10 | Easy / medium / hard, settings menu at the top | Suggestion below. | ✔ as suggested |
| 11 | Fixed standard piece order at the bottom | Agreed: big triangle, big triangle, medium triangle, square, parallelogram, small triangle, small triangle. Every piece has a fixed colour. | ✔ |
| 12 | Colourful pieces, then the picture changes to a stylish version with the same frame | Agreed, and it is a lovely reward. Each puzzle file gets an `art` layer (simple shapes clipped to the silhouette). An AI can draft it and a human approves it. | ✔ 6 puzzles with art |
| 13 | Not too easy, but a simple click moves on if you can't solve it | Agreed: › is always available. Skipped puzzles stay "in progress" or "new" in the list. | ✔ |
| 14 | No money making, no ads | Agreed. | ✔ |
| 15 | Track per-puzzle solve time and total ACTIVE time | Agreed; kept on the device only. "Active" means the app is visible **and** there was a touch in the last 60 s. | ✔ |
| 16 | Ask for a donation nicely once a day, after 15 minutes of play | Possible, but **Google Play limits this**. Details and a recommended design are in `07-play-time-and-donations.md`. In short: the money has to go through Google Play Billing (a "tip" in-app product), and the ask should be addressed to a grown-up. | ✔ mock-up of the ask (no real payment) |

## Suggested meaning of Easy / Medium / Hard
All three use the same silhouette-only board and the same puzzles. The setting only changes how forgiving the controls are:

| | Easy | Medium | Hard |
|---|---|---|---|
| Turn forgiveness | the lock may fix a turn that is **one step (45°) off** | the turn must be right | the turn must be right |
| Parallelogram mirror | automatic | automatic | **you flip it** (⇋ button) |
| Lock distance | 0.9 units (generous) | 0.65 units | 0.45 units |
| Timer on screen | hidden | hidden | shown |

Puzzles also carry their own difficulty (1–5 dots), and ‹ › walks through them from easy to hard. A separate "puzzle difficulty filter" was left out on purpose to keep one simple path.

## Things I would still question
- **Timer visibility.** For 8-year-olds a visible running clock can create pressure. The proposal shows it only on Hard, while the best time always appears on the solved card.
- **The donation ask in a children's game** is the most sensitive part of the whole product (policy and trust). See `07-play-time-and-donations.md`. A "rate us" ask instead of money is worth considering.
- **Alternative solutions.** With slot-free locking, any correct cover wins. This is good, but the stored solution's corner points still guide the locking. Some very unusual solutions might not lock perfectly; playtesting will show whether that matters.
