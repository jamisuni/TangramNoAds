# Requirements — Shared Types & Limits — TangramNoAds

**Status:** Draft  ·  **Version:** 0.1  ·  **Last updated:** 2026-09-27
**Approved by:** —  ·  **Approved on:** —

> Values used by two or more REQs, defined once. In this ai-led collection,
> every number the agent chose is marked *ASSUMPTION* until a human
> confirms it by using the prototype.

---

## Game values

### TYPE-001 — Piece set

- **Definition:** The seven classic tangram pieces, as the player meets them in every puzzle.
- **Confidence:** Inferred (the classic set is implied by "traditional Tangram" in SRC-001; colours, order and resting turns are agent decisions from SRC-004)
- **Sources:** SRC-001, SRC-002, SRC-004
- **Rules:**
  - Exactly 7 pieces: 2 large triangles (LT1, LT2), 1 medium triangle (MT), 1 square (SQ), 1 parallelogram (PG), 2 small triangles (ST1, ST2).
  - Size unit: the full set assembles into a 4 × 4 square (area 16). Areas: large triangle 4, medium triangle 2, square 2, parallelogram 2, small triangle 1.
  - The two large triangles are interchangeable, and so are the two small triangles.
  - Tray order, left to right and top to bottom: LT1, LT2, MT, SQ, PG, ST1, ST2.
  - ASSUMPTION: fixed colours are LT1 #E8505B, LT2 #3D8BFD, MT #F9A826, SQ #FFD23F, PG #FF7AB8, ST1 #2DBE7E, ST2 #9B5DE5.
  - ASSUMPTION: the resting turn in the tray is long side down and pointing up for every triangle, upright for the square, and leaning right for the parallelogram.
- **Open questions:** none

### TYPE-002 — Difficulty level

- **Definition:** The player-selectable difficulty setting.
- **Confidence:** Stated
- **Sources:** SRC-002 ("some easy/medium/hard levels")
- **Rules:**
  - Exactly three values, ordered: Easy < Medium < Hard.
- **Open questions:** none

### TYPE-003 — Turn step

- **Definition:** How a piece's orientation changes.
- **Confidence:** Inferred (the 45° step is the owner's idea, marked "maybe" in SRC-002; mirroring is an agent decision from SRC-004)
- **Sources:** SRC-002, SRC-004
- **Rules:**
  - One step is 45°, so a piece has 8 turns (0°, 45° … 315°).
  - A turn keeps the piece's centre (the average of its corners) fixed.
  - Only the parallelogram has a mirror image that differs from itself; mirroring the other pieces has no visible effect.
- **Open questions:** none

### TYPE-004 — Difficulty parameters

- **Definition:** What each difficulty level changes in the controls. The puzzles are the same at every level.
- **Confidence:** Inferred (agent proposal, SRC-004)
- **Sources:** SRC-004
- **Rules:**
  - ASSUMPTION: lock distance is Easy 0.9, Medium 0.65 and Hard 0.45 units (TYPE-001 unit), and never below 30 dp on screen.
  - ASSUMPTION: turn forgiveness: on Easy a drop may also lock with the turn one step (45°) away; on Medium and Hard the turn must match.
  - ASSUMPTION: parallelogram mirror: automatic on Easy and Medium; on Hard the player mirrors it.
  - ASSUMPTION: landing preview shown on Easy and Medium, not on Hard.
  - ASSUMPTION: default level on first start is Medium.
- **Open questions:** none

### TYPE-005 — Active second

- **Definition:** The unit of play time.
- **Confidence:** Inferred (the owner asked for "ACTIVE time" in SRC-002; the definition is the agent's, SRC-004)
- **Sources:** SRC-002, SRC-004
- **Rules:**
  - A second counts as active only while the game is visible on screen **and** the player's last touch was at most 60 s ago.
  - ASSUMPTION: the 60 s idle limit.
- **Open questions:** none

### TYPE-006 — Puzzle state

- **Definition:** The progress state every puzzle keeps.
- **Confidence:** Stated (SRC-002 names solved, half-way and restart; the three names are the agent's)
- **Sources:** SRC-002, SRC-004
- **Rules:**
  - Exactly three values: New (nothing placed, no time counted), In progress (at least one piece placed or time counted, not solved), Solved.
- **Open questions:** none

### TYPE-007 — Layout class

- **Definition:** Which layout the game uses on a given screen.
- **Confidence:** Inferred (phone and tablet are stated in SRC-001; the boundary is an agent decision, SRC-004)
- **Sources:** SRC-001, SRC-004
- **Rules:**
  - ASSUMPTION: Phone layout when the window is less than 600 dp wide; Tablet layout when it is 600 dp or wider.
- **Open questions:** none

---

## Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-09-27 | initial draft, TYPE-001..007 | first ai-led capture from SRC-001..004 | — |
