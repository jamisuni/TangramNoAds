# 04 · Design ideas: what makes ours the easiest Tangram to play

> **Round 0 thinking, partly superseded.** After Jami's round-1 feedback (`05-round1-feedback.md`) the game targets ages 8+: the assist ladder, gift pieces and hints (§1, §5) are dropped, and slot snapping (§2) is replaced by anchor locking (`06-snapping-by-anchors.md`). The rest remains useful background.

*Study note: the thinking behind the spec. Ideas tagged **[v1]** are in the first version; **[later]** ideas are parked for SwReqCollector.*

## 1. One puzzle, four ways to play (assist ladder) [v1]

Most apps ship separate "easy" and "hard" puzzle packs. We instead make **every puzzle playable at four assist levels**, so a 3-year-old and an 8-year-old can play the same Cat:

| Level | Icon | Target shows | Pieces arrive | Turning | Snap | Gift pieces* |
|---|---|---|---|---|---|---|
| 1 Colours | 🐣 | Coloured slots with inner lines; the piece colour matches the slot colour | Already turned correctly | Not needed | Huge (whole slot) | up to 4 |
| 2 Lines | 🐰 | Grey slots with inner lines | Already turned correctly | Not needed | Large | up to 2 |
| 3 Shadow | 🦊 | Solid silhouette only | Random turn | **Auto-turn on snap** (tap also turns) | Medium | 0 |
| 4 Master | 🦉 | Solid silhouette only | Random turn and flip | Tap = turn 45°, flip button for PG | Small, turn must match | 0 |

\*Gift pieces are already placed on the board when the puzzle starts, taken in the puzzle's `preplacedOrder` (biggest pieces first). The 7-piece Cat becomes a 3-piece puzzle for a toddler without any extra authoring.

The market scan showed that rotation is the main difficulty. This ladder brings in rotation **one skill at a time**: first none, then "the game turns it for you", then "you turn it".

## 2. Snapping that feels like magnets [v1]

- Snapping is decided at **drop**, not continuously, so there are no jumpy pieces while dragging. During the drag, a matching slot within range **glows softly** ("it will fit here") at levels 1–3.
- Matching is exact (see `03-tangram-geometry.md`): same piece type, and at level 4 the same turn (allowing for the piece's symmetry).
- The same-type pieces (two big and two small triangles) fit either of their slots.
- The snap animation is 180 ms ease-out and carries a "click" sound plus a small sparkle.
- If the piece does not fit, it gently glides back to its tray cell (levels 1–2) or stays where it was dropped (levels 3–4, so older kids can experiment).

## 3. The miniature tray → full-size lift [v1]

- In the tray, pieces are drawn in miniature (about 55–70 % of board size), each centred in its own cell, all the same scale, so a big triangle still looks bigger than a small one.
- When the child touches a piece it **grows to board size** over 120 ms while lifting **above the finger** (occlusion rule K5) with a soft drop shadow.
- The tray cell keeps a faint ghost outline of the piece that came from it, so the child sees where it will go back.
- A placed piece can be dragged off the board again; dropping it on the tray sends it home.

## 4. Turning without two fingers [v1]

- **Tap a piece on the board** → it turns 45° clockwise with a 150 ms animation (levels 3–4). At level 3 the slot also turns it automatically on snap, so tapping is optional.
- **Flip** (PG only, level 4): a round flip badge ⇋ appears next to the selected PG, big enough to hit (48 dp inside a 64 dp hit area).
- **Two-finger twist** is supported but never needed (research: 53 % multi-touch success at ages 3–6).
- At level 4 a tap on a piece in the **tray** also turns it, so a child can pre-turn before dragging.

## 5. Help that never punishes [v1]

- The 💡 hint button has no counter and no cost.
  - 1st press: the next piece's slot pulses and the matching tray piece wiggles.
  - 2nd press within about 10 s: that piece flies into place by itself.
- Idle auto-hint at levels 1–2 after about 15 s (just a wiggle, no auto-placing).
- **Adaptive suggestion** (inspired by Osmo, rule-based, runs on the device): after 2 puzzles in a row with 3+ auto-placements, a small 🐣 bubble offers an easier level; after 3 hint-free solves, 🦊 offers a harder one. The child taps to accept or just ignores it. Grown-ups can lock the level.

## 6. The ending moment [v1]

- When the last piece snaps: 400 ms pause → the silhouette **fills with colour piece by piece** → the figure does a small "alive" animation (bounce and wiggle; a blink if the puzzle has `eyes`) → confetti → a sticker flies into the sticker book.
- Big ▶ Next and ↻ Again buttons; no score screen and no star rating shown to the child (stars exist only for the grown-up progress view).

## 7. No reading needed [v1]

- The child area uses icons only: 🏠 home, 💡 hint, ↶ undo is not needed (drag back instead), ⚙ is only behind the gate.
- Categories are shown as picture tiles (a cat silhouette for Animals, a boat for Vehicles...).
- Puzzle select is a grid of silhouettes; solved ones show in colour.

## 8. Layout adapts to the device [v1]

- **Phone (portrait, locked):** a narrow screen → tray is **2 rows** (4 + 3 cells), board is square-ish above.
- **Tablet portrait:** tray is **1 row of 7** at the bottom, wider cells, and bigger miniatures.
- **Tablet landscape:** the board takes the left-centre, the tray stays at the bottom as **1 wide row**; the top bar shrinks to side corners.
- Only one rule decides the layout: *can 7 tray cells of at least 80 dp fit in one row?* If yes → 1 row, otherwise → 2 rows.

## 9. AI-oriented content pipeline [v1 tooling]

- Puzzles are plain JSON with **exact** coordinates. An AI (or a human) can author a puzzle as simple polygons, and `tools/validate_puzzles.py` proves it is a real tangram (congruent pieces, no overlaps, connected by edges).
- `tools/render_puzzle.py` draws a preview sheet (colours / lines / shadow / solved) for human sign-off; `provenance.reviewedByHuman` stays `false` until someone has looked at it.
- This lets us grow the library quickly with AI help, **without shipping broken puzzles**.

## 10. Ideas parked for later

| Idea | Why it is interesting |
|---|---|
| **Free build mode** + gallery | Kids invent their own shapes; saved locally. |
| **"Make it a puzzle"** | A creation from free-build becomes a playable puzzle for a sibling (validator reused on the device). |
| Spoken encouragement with spatial words ("Turn it!", "Nice corner!") | Research: spatial language boosts learning. Could use device TTS offline, or recorded voice. |
| Alternative-solution acceptance | Accept any arrangement that fills the silhouette exactly (coverage check), not only the stored solution. Needed for Master level, where a smart kid finds another solution. |
| Hungarian / 14-piece Stomachion / mini 5-piece sets | Content variety; the geometry core already supports other piece sets if they have 45° angles. |
| Several child profiles | Siblings on one tablet. |
| Colour-blind friendly patterns on pieces | Accessibility. |
| Physical-set mode | Printable tangram and the app just shows the target: an Osmo-like experience without hardware. |
| AI puzzle generator run by us offline | Generate candidates → validator → render → human review → ship in an update. Never runs in the app, and there is still no network. |
