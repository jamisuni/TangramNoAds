# 01 · Gameplay rules and interaction

**Status:** draft 0.3 (round 2: anchors are outline corners + placed pieces only; no donation ask). Requirement ids in brackets refer to `04-requirements.md`. Drawings: `ui/01`–`ui/06`. The playable reference is `prototype/tangram-prototype.html`.

## 1. Core loop
1. A puzzle shows an empty **silhouette** (one flat colour, no inner lines) and the **tray** with all 7 pieces.
2. The player drags pieces onto the silhouette, turning them as needed. Each drop **locks** into a valid spot or the piece goes back to the tray.
3. When all 7 pieces are on the board, the silhouette is exactly covered: the puzzle is solved, **whatever arrangement was used**.
4. The pieces turn into a **stylised picture** of the figure, and the solve time is recorded.
5. ▶ Next, or ‹ › to browse. Any puzzle can be skipped with › at any time.

## 2. Pieces and the tray [REQ-TRY-*]
- The classic 7: LT1 LT2 (large triangles), MT, SQ, PG, ST1 ST2. A **mini puzzle** (3 pieces, for a fast first success and for testing) shows only its own pieces, in the same order.
- **Fixed order** in the tray: LT1, LT2, MT, SQ, PG, ST1, ST2. **Fixed colours** (`02-ui-layout.md`).
- **Fixed resting turn**: all triangles have the long side down and point up, the square is upright, the parallelogram leans right. The tray looks the same in every puzzle.
- Tray pieces are miniatures (one shared scale, the same in every puzzle). On the board a piece is always its true size. There is no resizing (confirmed by Jami, round 4).
- **Size marks:** the triangles carry **S**, **M** or **L** in the corner of their cell, so the three sizes of the same shape are easy to tell apart.
- A piece on the board leaves a dashed ghost in its tray cell.

## 3. Moving and turning [REQ-DRG-*, REQ-ROT-*]
| Action | Result |
|---|---|
| Touch and move ≥ 12 dp | Drag. A tray piece grows to board size (120 ms) and floats **above the finger**. |
| Tap (moves < 12 dp, any duration) on a tray piece | Turn +45° clockwise in the tray. |
| ↺ / ↻ button in a tray cell | Turn that piece −45° / +45° in the tray, ready before it is dragged (handy with a mouse). |
| Tap on a board piece | Turn +45° in place. It stays only if it still locks near there; otherwise it turns back with a short shake. |
| Second finger during a drag, then twist | Turn in **45° steps** (a step at every ±22.5° of twist), with a tick sound. |
| Mouse wheel or R (Shift+R backwards) during a drag | Turn ±45° (desktop testing). |
| ⇋ badge (Hard only) | Mirror the parallelogram. On Easy and Medium the lock picks the right mirror automatically. |
| Drag a board piece back to the tray | It goes home. |

## 4. Locking a dropped piece [REQ-LCK-*]
Full design: `Study/06-snapping-by-anchors.md`; diagram: `ui/05-lock-rules.png`.

- **Anchor points:** the silhouette's **outline corners** and the **corners of pieces already on the board**. Nothing hidden. A piece dropped in the middle with no neighbour to hold goes home; puzzles are designed to be built edge-first (V11).
- On release, search near the drop point for a spot where **one piece corner sits on an anchor**, the piece is **completely inside the silhouette**, and it **overlaps no other piece**. The nearest such spot wins, with a small bonus for snug fits.
- Found → glide there (180 ms), "click". Not found, or dropped outside the board → glide back to the tray with a soft sound. **There are no loose pieces on the board.**
- **Landing preview** (Easy, Medium): while dragging, a dashed outline shows where the piece would lock. It shows a *valid* spot, not the *correct* one.
- **Solved** = all 7 pieces on the board. Because every locked piece is inside and nothing overlaps, the area adds up exactly, so any correct arrangement wins.

## 5. Difficulty (a setting; the same puzzles for everyone) [REQ-DIF-*]
| | Easy | Medium (default) | Hard |
|---|---|---|---|
| Lock distance | 0.9 units | 0.65 units | 0.45 units |
| A turn that is one step (45°) off | fixed by the lock | must be right | must be right |
| Parallelogram mirror | automatic | automatic | flip it yourself (⇋) |
| Landing preview | on | on | off |
| Timer on screen (setting "Hard only") | hidden | hidden | shown |

*1 unit = the classic square's side / 4. The lock distance never goes below 30 dp on screen.*

Puzzles have their own difficulty (1–5 dots), and ‹ › lists them from easiest to hardest.

## 6. Browsing and puzzle states [REQ-BRW-*]
Drawing: `ui/04-browse-states.png`.

| State | Board shows | Tray area shows | Buttons |
|---|---|---|---|
| **New** | empty silhouette | all 7 pieces | – |
| **In progress** | silhouette + the pieces placed so far (kept exactly: position, turn, flip) | the remaining pieces | Restart |
| **Solved** | the **picture only** (no piece lines), same outline | Retry · best time · Next | Retry, Next |

- The top bar reads ‹ **Name** ●●○○○ · 3 / 6 · state › ⚙.
- Leaving a puzzle (‹ ›, the app closing, the screen turning off) saves its state. Coming back continues it; Restart clears it.
- Retry on a solved puzzle starts it fresh; the best time is kept.
- **Skipping:** › works in every state. There are no locks and no required order.

## 7. Solving moment [REQ-WIN-*]
Drawing: `ui/06-solve-and-playtime.png`.
1. The last piece locks → the pieces pop, a chime plays, confetti.
2. After about 0.6 s the **picture** fades in over the pieces (0.8 s): the same outline, stylised art from the puzzle's `art` layer.
3. The tray area becomes Retry · best time · Next.
With reduced motion enabled: no confetti or pop; the picture still appears.

## 8. Time tracking [REQ-TIM-*]
Details: `Study/07-play-time-and-donations.md`.
- **Active second** = app visible **and** a touch within the last 60 s.
- **Puzzle time** counts active seconds while an unsolved puzzle that has been touched is on screen. It pauses while browsing away or in Settings, and continues on return.
- **Best time** per puzzle; **today** and **total** active time. All shown in Settings → Play time.
- All stored on the device only.

## 9. Absolutely free
There is no donation ask, rating request or any other interruption. Settings carries one small note: *"Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."*

## 10. Sounds
| Event | Sound |
|---|---|
| pick up | short rising pop |
| turn step | tiny tick |
| lock | bright click |
| goes home / can't turn here | soft low tone (never a buzzer) |
| solve | 4-note chime |
Sound on/off in Settings.

## 11. Developer aid (prototype and test builds only) [REQ-046]
- A small dashed **DEV** button in the bottom-left corner of the board asks for the passcode **0417**.
- Unlocked, it offers **Show the solution** (each piece's stored place drawn in its colour with its id; stays on while browsing until switched off) and **Solve this puzzle now** (places every piece and shows the picture; no best time is recorded).
- It is a testing aid, not a player hint (Q10: no hints). Release builds leave it out.
