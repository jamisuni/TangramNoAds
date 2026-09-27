# 08 · Round 2 decisions (2026-09-27 08:39)

| Jami said | What changed |
|---|---|
| "We do not support gift boxes, nor do we need to support placing something in the middle without neighbours." | **Hidden anchors removed.** Pieces lock only to silhouette corners and to corners of placed pieces (`06-snapping-by-anchors.md`). New validator rule **V11**: every puzzle must have an edge-first build order. The *Gift box* (classic square) puzzle was moved to `archive/removed-puzzles/`. |
| "Drop donations, but keep tracking active playtime. Neither of us needs money from this; the theme is 'enjoy, it's absolutely free'." | **Support card removed** from the prototype, spec and requirements. Play-time tracking stays (`07-play-time-and-donations.md`). An "Enjoy, it's absolutely free" note was added to Settings. |

Effect on the prototype (0.3): 5 puzzles (house, cat, arrow, sailboat, mountain). All pass V1–V11 and were solved automatically at Easy, Medium and Hard on phone, tablet-portrait and tablet-landscape sizes. A test drop of the mountain's square before its neighbours correctly returns it to the tray.
