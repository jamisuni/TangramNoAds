# 02 · UI layout: phone and tablet

**Status:** draft 0.4 (round 6: no tray buttons, no difficulty, flip badge always, grid overview, Finnish). The drawings in `ui/` are annotated screenshots of the real prototype (`tools/make_ui_sheets.py`), so they always match it. The layout rules live in REQ-013, REQ-035–037; this file explains them.

| Drawing | Shows |
|---|---|
| `ui/01-phone-play.png` | Phone play screen mid-drag, with the landing preview, annotated |
| `ui/02-tablet-play.png` | Tablet landscape, timer on, flip badge |
| `ui/03-adaptive-layouts.png` | The same game state on a phone, a tablet in portrait and a tablet in landscape |
| `ui/04-browse-states.png` | New / in progress / solved |
| `ui/05-lock-rules.png` | How a dropped piece locks: outline corners, placed-piece corners, going home |
| `ui/06-solve-and-playtime.png` | Pieces → picture, and Settings with play time |

## 1. Screens
There is one main screen (Play) plus two overlays.
1. **Play:** top bar, board, tray (or the solved bar).
2. **Settings** overlay (⚙): timer on screen (off by default), sound, play time and best times, reset progress (with an in-page confirm step), how to play, the note "Enjoy, it's absolutely free" and the privacy text.
3. **All puzzles** overlay (press the counter in the top bar): a grid of every puzzle's silhouette, solved ones as their picture, in-progress ones with a dot; one press opens a puzzle.

There is no separate home or menu screen: the app opens on the last puzzle you had. Every text is in Finnish or English, following the device (REQ-047).

## 2. Zones
```
Phone (width < 600 dp, portrait only)        Tablet (width ≥ 600 dp, both orientations)
┌───────────────────────────┐               ┌─────────────────────────────────────────────┐
│ [‹]   Cat  ●●○○○ 2/6  [›] ⚙│ top bar 66 dp │ [‹]            Arrow ●●○○○ 3/6          [›] ⚙│ 74 dp
├───────────────────────────┤               ├─────────────────────────────────────────────┤
│ Restart           ⏱ 1:23 │               │ Restart                              ⏱ 1:23 │
│                           │               │                                             │
│         BOARD             │               │                   BOARD                     │
│   silhouette fitted,      │               │                                             │
│   10 % padding            │               │                                             │
├───────────────────────────┤               ├─────────────────────────────────────────────┤
│ [ LT ] [ LT ] [ MT ]      │ tray row 1    │ [ LT ][ LT ][ MT ][SQ][ PG ][ST][ST]         │ one row
│ [SQ][ PG ][ST][ST]        │ tray row 2    └─────────────────────────────────────────────┘
└───────────────────────────┘
Solved: the tray area becomes  [ ↻ Retry ]  best time 2:21  [ Next › ]
```

## 3. Layout algorithm (deterministic, implemented in the prototype)
Inputs: window width W and height H (dp).
1. `compact = W < 600`. Top bar 66 dp (compact) or 74 dp.
2. **Tray rows:** compact → 2 rows (row 1: LT1 LT2 MT; row 2: SQ PG ST1 ST2); otherwise 1 row. The order is fixed. A mini puzzle lays out only its own pieces (an empty row is dropped).
3. **Turn diameter** d = 2 × the largest centre-to-corner distance (LT 4.22, PG 3.16, MT 2.98, ST 2.11, SQ 2.00 units). A cell of d·s + padding fits the piece at scale s in *any* turn, so cells never change size when a piece is turned in the tray.
4. **Tray scale** `ts` = the largest s where every row of the **full set** fits the width (padding 12, gap 8 / 14 dp, side margin 8 / 16 dp) and each row is ≤ max(84 dp, 16 % of H). Always computed for the full set, so a piece has the same miniature size in every puzzle.
4b. **Cell contents:** the triangles get an S / M / L size mark in the top-left corner, hidden while the piece is on the board; the parallelogram's cell carries the ⇋ flip badge at its top-right corner. There is no button strip (the ↺ ↻ buttons of 0.4–0.5 went in round 6).
5. **Board** = between the top bar and the tray. **Board scale** `bs` fits the silhouette with about 10 % padding.

Measured values (0.6): phone 390×844 → ts ≈ 26.8, bs ≈ 45–65 dp/unit depending on the puzzle; the smallest cell is 66 dp. Tablet 1280×800 → ts ≈ 27.5, bs ≈ 80–113.

## 4. Sizes
| Element | Size |
|---|---|
| ‹ › buttons | 52 dp square, rounded |
| ⚙ | 44 dp |
| Restart | pill, ≥ 44 dp high, top-left of the board |
| Tray cells | computed; the smallest is 60 dp on a 360 dp phone, 66 dp on a 390 dp phone |
| Grid cells (All puzzles) | ≥ 64 dp square |
| DEV button | small and dashed on purpose; a testing aid, exempt from the 48 dp rule (REQ-037) |
| Piece touch area | its shape grown by 12 dp |
| Flip badge | 38 dp visible, 60 dp touch area |
| Retry / Next | ≥ 48 dp high; Next is filled green and is the primary action |

## 5. Visual language
- **Piece colours** (fixed): LT1 `#E8505B` red, LT2 `#3D8BFD` blue, MT `#F9A826` orange, SQ `#FFD23F` yellow, PG `#FF7AB8` pink, ST1 `#2DBE7E` green, ST2 `#9B5DE5` purple. Pieces have a 2 dp white edge.
- Silhouette `#4A4E69` (flat, no inner lines). Paper `#FBF7EE`, board white, tray `#EFE7D6`, accent `#3D8BFD`, go-green `#2DBE7E`.
- **Picture style** (art layer): flat illustration, 3–8 colours, simple shapes, drawn inside the silhouette only. The silhouette edge stays visible as a thin outline so the "same frame" is clear.
- Type: a rounded display face (e.g. *Baloo 2*) for the puzzle name and buttons; the system font elsewhere.

## 6. Orientation and system UI
- Phone: portrait only. Tablet: both orientations; rotating recomputes the layout and keeps every piece. (Recent Android ignores orientation locks on screens 600 dp and wider, which matches the tablet boundary.)
- Draw edge to edge, respecting system insets; the tray stays clear of the gesture bar (REQ-035).
- Android back: closes an overlay; otherwise leaves the app normally (state is already saved) (REQ-035).
- Reduced motion: no pop, no confetti, no pulse animation; the picture still appears (REQ-023).
