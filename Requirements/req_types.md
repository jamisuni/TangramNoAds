# Requirements — Shared Types & Limits — TangramNoAds

**Status:** Draft  ·  **Version:** 0.2  ·  **Last updated:** 2026-09-28
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

> **Withdrawn 2026-09-28 (SRC-011).** The owner removed the Easy / Medium / Hard levels. The puzzle rating (REQ-040) is the only difficulty. Kept for its id.

- **Definition:** The player-selectable difficulty setting (withdrawn).
- **Confidence:** Stated
- **Sources:** SRC-002, SRC-011
- **Rules:**
  - none (withdrawn)
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

### TYPE-004 — Lock parameters

- **Definition:** The numbers that decide where a dropped piece locks (REQ-019) and what the landing preview shows (REQ-021). One set for everyone; the former per-level sets went with the levels (TYPE-002 withdrawn).
- **Confidence:** Inferred (agent proposal, SRC-004; the owner accepted that these numbers are written here so a build locks like the prototype, SRC-011 F5/F6)
- **Sources:** SRC-004, SRC-011
- **Rules:**
  - ASSUMPTION: lock distance R = 0.65 units (TYPE-001 unit), and never below 30 dp on screen.
  - The turn must match: a piece one 45° step off does not lock. The mirror must match: the search never tries the other mirror image (REQ-018).
  - Candidates: for every corner v of the piece and every anchor a, the translation t = a − v with |t| ≤ R. A candidate is valid when the moved piece lies completely inside the silhouette and overlaps no placed piece.
  - ASSUMPTION: choice among valid candidates: score = |t| − 0.04 × (number of piece corners that land exactly on anchors); the lowest score wins; only candidates with |t| ≤ (best |t|) + 0.16 are considered.
  - ASSUMPTION: geometry tolerance: "inside" and "overlaps" are decided with a tolerance of at most 1e-6 units; the locked position is the anchor's exact position minus the piece corner's exact offset, so a locked piece sits exactly on its anchor and later locks against it are exact too.
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
- **Confidence:** Stated (SRC-002 names solved, half-way and restart; the three names and the transitions are the agent's, accepted in SRC-011 F8)
- **Sources:** SRC-002, SRC-004, SRC-011
- **Rules:**
  - Exactly three values: New, In progress, Solved.
  - New → In progress when a piece first leaves the tray (a drag starts on a tray piece). Turning a piece in the tray, browsing past the puzzle or opening the settings does not start it.
  - In progress → Solved when the last piece locks (REQ-022). Restart returns In progress → New; Retry returns Solved → New (the best time is kept).
  - Time is counted only In progress (REQ-030).
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
| 0.2 | 2026-09-28 | TYPE-002 withdrawn; TYPE-004 rewritten as the single lock parameter set with the scoring and tolerance rules; TYPE-006 transitions defined | owner round 6 (SRC-011): levels removed, review accepted | — |
