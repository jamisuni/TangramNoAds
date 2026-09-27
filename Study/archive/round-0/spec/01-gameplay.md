# 01 · Gameplay rules and interaction

**Status:** draft 0.1. Requirement ids in brackets refer to `04-requirements.md`. Drawings: `ui/04-assist-levels.svg`, `ui/05-drag-storyboard.svg`. The behaviour below is implemented in the clickable prototype `prototype/tangram-prototype.html`.

## 1. Core loop
1. The child picks a world, then a puzzle (silhouette grid, everything unlocked).
2. The play screen shows the **target** (board, upper area) and the **pieces** (tray, bottom).
3. The child drags pieces from the tray onto the target. A piece that fits snaps in.
4. When every slot is filled, the ending plays and a sticker is earned.
5. ▶ Next (big) or ↻ Again (small). 🏠 returns to the world grid.

There is no timer, score, lives or fail state.

## 2. Terms
| Term | Meaning |
|---|---|
| **Piece** | One of LT1, LT2, MT, ST1, ST2, SQ, PG (`03-puzzle-format.md`). A puzzle may use a subset ("starter" puzzles). |
| **Slot** | Where a piece belongs in the solution: its exact polygon. |
| **Turn** | The rotation, a multiple of 45° (rot 0–7). **Flip** = mirror image (only matters for the PG). |
| **Tray** | The bottom panel holding the unplaced pieces as miniatures, one cell per piece, in a fixed order. |
| **Placed** | A piece snapped into a slot. Placed pieces are locked. |
| **Free** | A piece lying on the board but not in a slot (levels 3–4 only). |
| **Gift piece** | A piece already placed when the puzzle starts (easy levels). |

## 3. Assist levels [REQ-LVL-*]

| | 1 · Colours | 2 · Lines | 3 · Shadow | 4 · Master |
|---|---|---|---|---|
| Suggested age | 3–4 | 4–5 | 5–7 | 7+ |
| Target display | each slot tinted with its piece colour, dashed inner lines | grey slots with inner lines | solid silhouette | solid silhouette |
| Tray orientation | as in the solution | as in the solution | random turn, not flipped | random turn; PG mirrored |
| Gift pieces | up to 4 | up to 2 | 0 | 0 |
| Snap radius (centre distance) | 1.5 units | 1.1 units | 0.8 units | 0.5 units |
| Minimum snap radius on screen | 34 dp (all levels) | | | |
| Orientation needed to snap | no (auto) | no (auto) | no: **the slot turns the piece** | **yes**: exact turn and flip |
| Tap on a piece | wiggle (no turn) | wiggle | turn +45° | turn +45° |
| Flip badge on the PG | – | – | – | yes |
| Slot glow while dragging | yes | yes | yes | no |
| Missed drop | piece glides back to the tray | back to tray | stays free on the board | stays free |
| Idle auto-hint (15 s) | yes | yes | no | no |

*1 unit = the small triangle's short side / √2; the full classic square is 4 × 4 units.*

**Gift piece count** = min(level gifts, length of `assist.preplacedOrder`, pieces − 2). A child always places at least 2 pieces.

**Level choice:** the default is level 1 on first launch. The game *suggests* moving up after 3 hint-free solves in a row, and down after 2 puzzles in a row with 3+ auto-placed hints. The suggestion is a small bubble the child can tap or ignore. Grown-ups can lock a level [REQ-LVL-5].

## 4. Moving pieces [REQ-DRG-*]
- **Pick up:** finger down on a tray cell (the whole cell is the hit area) or on a piece (its shape grown by 12 dp). No long-press. A "pop" sound plays, and a tray piece swells slightly (×1.12).
- **Tap or drag:** a touch that moves less than **12 dp** is a *tap*, whatever its duration (3-year-olds hold taps for seconds).
- **Lift:** once the drag starts, a tray piece grows to board size over **120 ms** and is positioned so its **lowest point is 34 dp (phone) / 44 dp (tablet) above the finger**. A free piece keeps its grab offset plus 18 dp of lift. A drop shadow shows it is lifted.
- **During the drag:** the piece follows the finger 1:1, with no lag and no snapping. At levels 1–3 the best matching free slot within range **glows** in the piece's colour.
- **Drop:**
  1. Candidate slots = unfilled slots of the same piece type (LT1 fits either large-triangle slot; ST1/ST2 likewise).
  2. Distance = from the piece centre (vertex average) to the slot centre, in board units.
  3. The nearest candidate within the snap radius wins. At level 4 it must also match exactly: the piece's vertex set at that position equals the slot's vertex set (this handles the symmetric square and PG correctly).
  4. **Snap:** glide into the slot (180 ms ease-out), auto-turn to the slot's turn and flip at levels 1–3, "click" sound, sparkle. The piece is now locked.
  5. **No snap:** levels 1–2 → glide back to the tray with a soft "boing"; levels 3–4 → stay where dropped if inside the board, otherwise back to the tray.
- **Dropping on the tray** always sends a piece home.
- **Placed pieces are locked.** Tapping one gives a happy wiggle. (It fits, so there is no reason to move it again.)
- **One finger at a time.** A second finger is ignored during a drag. An optional two-finger twist to turn a free piece may be added for ages 7+, but it must never be required [REQ-DRG-8].

## 5. Turning and flipping [REQ-ROT-*]
- Tap = +45° clockwise, animated over 150 ms (levels 3–4, on the board **and** in the tray).
- The PG flip badge (level 4): a round ⇋ button, 44–48 dp icon with a **64 dp hit area**, shown next to the selected PG (tray or board). Tapping it mirrors the piece with a scale-X animation.
- A piece becomes "selected" when it was the last one tapped. Tapping the empty board clears the selection.
- Turning is always around the piece's centre, so the piece does not move sideways when turned.

## 6. Hints [REQ-HNT-*]
- 💡 is always visible and free.
- **1st press:** the slot of the *next hint piece* pulses in that piece's colour (4 × 0.9 s) and the piece (in the tray or on the board) wiggles. Next hint piece = the first unfilled slot in the order LT1, LT2, MT, SQ, PG, ST1, ST2.
- **2nd press within 10 s** (same hint): that piece flies into its slot (auto-turned at every level).
- **Idle hint** (levels 1–2): after 15 s without touch, the next piece wiggles and its slot pulses. There is no auto-placing.
- Hint use is counted only for the adaptive level suggestion. It is never shown to the child as a cost.

## 7. The ending [REQ-WIN-*]
1. The last piece snaps, then a 400 ms pause.
2. The whole figure hops, squashes and wiggles (0.9 s). Optional per-puzzle flavour: `celebrate.sound` (e.g. meow) and `celebrate.eyes` (blinking eyes drawn at a given point).
3. Confetti in the 7 piece colours (2.6 s), plus a win chime.
4. A sticker of the solved figure flies to the sticker book (home screen).
5. Big green ▶ Next (bottom-right, 104 dp) and white ↻ Again (76 dp). No score and no stars are shown to the child.
With reduced-motion enabled: no confetti or hop; the colours and sound stay.

## 8. Progress
- Solved puzzles are stored on the device (local storage only). The grid shows them in colour with a ✓.
- The sticker book shows all solved figures. Solving the same puzzle again at a higher level adds a small level dot to its sticker.
- Grown-up area → "Reset progress" (with an in-app confirm step).

## 9. Sounds and haptics
| Event | Sound | Haptic |
|---|---|---|
| pick up | short rising pop | light tick |
| turn / flip | soft blip | none |
| snap | bright click | light tick |
| return home | soft low "boing" (never a buzzer) | none |
| win | 4-note chime | medium |
Sound can be switched off in the grown-up area. Everything also works with the phone muted.
