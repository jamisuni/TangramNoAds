# 01 · Gameplay rules and interaction

**Status:** draft 0.4 (round 6: no difficulty levels, no tray buttons, flip badge always, wrap, overview, Finnish). Requirement ids in brackets refer to `../Requirements/reqs/`. Drawings: `ui/01`–`ui/06`. The playable reference is `prototype/tangram-prototype.html` (0.6). When this file and a REQ disagree, the REQ wins.

## 1. Core loop
1. A puzzle shows an empty **silhouette** (one flat colour, no inner lines) and the **tray** with all 7 pieces.
2. The player drags pieces onto the silhouette, turning them as needed. Each drop **locks** into a valid spot or the piece goes back to the tray.
3. When all 7 pieces are on the board, the silhouette is exactly covered: the puzzle is solved, **whatever arrangement was used**.
4. The pieces turn into a **stylised picture** of the figure, and the solve time is recorded.
5. ▶ Next, or ‹ › to browse. Any puzzle can be skipped with › at any time; the list wraps.

## 2. Pieces and the tray [REQ-012, REQ-013, REQ-043]
- The classic 7: LT1 LT2 (large triangles), MT, SQ, PG, ST1 ST2. A **mini puzzle** (3 pieces, for a fast first success and for testing) shows only its own pieces, in the same order.
- **Fixed order** in the tray: LT1, LT2, MT, SQ, PG, ST1, ST2. **Fixed colours** (`02-ui-layout.md`).
- **Fixed resting turn**: all triangles have the long side down and point up, the square is upright, the parallelogram leans right. The tray looks the same in every puzzle.
- Tray pieces are miniatures (one shared scale, the same in every puzzle). On the board a piece is always its true size. There is no resizing (confirmed by Jami, round 4).
- **Size marks:** the triangles carry **S**, **M** or **L** in the corner of their cell, so the three sizes of the same shape are easy to tell apart.
- A piece on the board leaves a dashed ghost in its tray cell.
- The parallelogram's cell carries the ⇋ flip badge (REQ-018). There are no other controls in a cell: the ↺ ↻ buttons of prototypes 0.4–0.5 went in round 6 (tapping does the same).

## 3. Moving and turning [REQ-014 – REQ-018]
| Action | Result |
|---|---|
| Touch and move ≥ 12 dp | Drag. A tray piece grows to board size (120 ms) and floats **above the finger**. |
| Tap (moves < 12 dp, any duration) on a tray piece | Turn +45° clockwise in the tray, ready before it is dragged. |
| Tap on a board piece | Turn +45° in place. It stays only if it still locks near there; otherwise it turns back with a short shake. |
| Second finger during a drag, then twist | Turn in **45° steps** (a step at every ±22.5° of twist), with a tick sound. |
| Mouse wheel or R (Shift+R backwards) during a drag | Turn ±45° (desktop testing). |
| ⇋ badge (always shown next to the parallelogram) | Mirror the parallelogram, in the tray or on the board (on the board only if it still locks). The lock never mirrors a piece by itself. |
| Drag a board piece back to the tray | It goes home. |

## 4. Locking a dropped piece [REQ-019 – REQ-021, TYPE-004]
Full design: `Study/06-snapping-by-anchors.md`; diagram: `ui/05-lock-rules.png`.

- **Anchor points:** the silhouette's **outline corners** and the **corners of pieces already on the board**. Nothing hidden. A piece dropped in the middle with no neighbour to hold goes home. This is a rule of the lock, not of the content: every valid tangram can be built corner by corner, because the uncovered region always has a convex corner (an anchor) and the piece covering it has a vertex there. (Round 2 believed some figures needed hidden anchors; round 6 corrected that and the classic square is back.)
- On release, search within the lock distance (0.65 units, at least 30 dp) for a spot where **one piece corner sits on an anchor**, the piece is **completely inside the silhouette**, and it **overlaps no other piece**. Score = distance − 0.04 per corner that lands on an anchor; the lowest wins among candidates within 0.16 of the nearest (TYPE-004). The turn and mirror are the player's: the lock never changes them.
- Found → glide there (180 ms), "click". Not found, or dropped outside the board → glide back to the tray with a soft sound. **There are no loose pieces on the board.**
- **Landing preview:** while dragging, a dashed outline shows where the piece would lock. It shows a *valid* spot, not the *correct* one.
- **Mini puzzles teach the rule** (REQ-051): when a drop goes home in a mini puzzle, the outline corners pulse once. No solution is shown.
- **Solved** = all 7 pieces on the board. Because every locked piece is inside and nothing overlaps, the area adds up exactly, so any correct arrangement wins.

## 5. Difficulty [REQ-040]
There are **no difficulty levels** (removed by the owner in round 6; the Easy / Medium / Hard control settings of drafts 0.2–0.3 are withdrawn, REQ-004). Every player gets the same lock rule (TYPE-004). The difficulty is the puzzle itself: each puzzle carries a rating (1–5 dots), and ‹ › lists the puzzles by kind (mini, warm-up, full), then from the easiest rating to the hardest.

## 6. Browsing and puzzle states [REQ-024 – REQ-026, REQ-050]
Drawing: `ui/04-browse-states.png`.

| State | Board shows | Tray area shows | Buttons |
|---|---|---|---|
| **New** | empty silhouette | all 7 pieces | – |
| **In progress** | silhouette + the pieces placed so far (kept exactly: position, turn, flip) | the remaining pieces | Restart |
| **Solved** | the **picture only** (no piece lines), same outline | Retry · best time · Next | Retry, Next |

- The top bar reads ‹ **Name** ●●○○○ · 3 / 6 · state › ⚙.
- Leaving a puzzle (‹ ›, the app closing, the screen turning off) saves its state. Coming back continues it; Restart clears it.
- Retry on a solved puzzle starts it fresh; the best time is kept.
- **Skipping:** › works in every state. There are no locks and no required order. The list wraps: › on the last puzzle shows the first.
- **Finding a puzzle among many** (REQ-050): hold › for half a second to jump to the next unsolved puzzle; press the counter ("9 / 13") in the top bar to open a grid of every puzzle's silhouette (solved ones show their picture, in-progress ones a dot) and pick one.
- **After an update** (REQ-025): a saved piece that no longer fits the current silhouette returns to the tray when the puzzle is loaded.

## 7. Solving moment [REQ-022, REQ-023]
Drawing: `ui/06-solve-and-playtime.png`.
1. The last piece locks → the pieces pop, a chime plays, confetti.
2. After about 0.6 s the **picture** fades in over the pieces (0.8 s): the same outline, stylised art from the puzzle's `art` layer.
3. The tray area becomes Retry · best time · Next.
With reduced motion enabled: no confetti or pop; the picture still appears (REQ-023).

## 8. Time tracking [REQ-029 – REQ-031, TYPE-005, TYPE-006]
Details: `Study/07-play-time-and-donations.md`.
- **Active second** = app visible **and** a touch within the last 60 s.
- **Puzzle time** counts active seconds while a puzzle **in progress** is on screen. A puzzle is in progress from the moment a piece first leaves the tray (turning a piece in the tray does not start it). It pauses while browsing away or in Settings, and continues on return.
- **Timer on screen:** a setting, off by default; the best time always shows on the solved card.
- **Best time** per puzzle; **today** and **total** active time. All shown in Settings → Play time.
- All stored on the device only.

## 9. Absolutely free, and the privacy text [REQ-009, REQ-049]
There is no donation ask, rating request or any other interruption. Settings carries one small note: *"Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."* Below it sits the privacy text the store requires of every app, even one that collects nothing: *"Privacy: this game collects no data. It has no network access, no accounts and no analytics. Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."* Both exist in Finnish and English.

## 9b. Language [REQ-047]
The game follows the device language: Finnish or English, English for everything else. Puzzle titles come from the puzzle file (`title.fi`, `title.en`). Pictures carry no text. The S / M / L size marks are the same in both languages.

## 10. Sounds
| Event | Sound |
|---|---|
| pick up | short rising pop |
| turn step | tiny tick |
| lock | bright click, plus a short haptic tick where the device gives one without a permission |
| goes home / can't turn here | soft low tone (never a buzzer) |
| solve | 4-note chime |
Sound on/off in Settings; the haptic tick follows the sound switch.

## 11. Developer aid (prototype and test builds only) [REQ-046]
- A small dashed **DEV** button in the bottom-left corner of the board asks for the passcode **0417**.
- Unlocked, it offers **Show the solution** (each piece's stored place drawn in its colour with its id; stays on while browsing until switched off) and **Solve this puzzle now** (places every piece and shows the picture; no best time is recorded).
- It is a testing aid, not a player hint (Q10: no hints). Release builds leave it out.
