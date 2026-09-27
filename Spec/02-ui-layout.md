# 02 · UI layout: phone and tablet

**Status:** draft 0.3. The drawings in `ui/` are annotated screenshots of the real prototype (`tools/make_ui_sheets.py`), so they always match it.

| Drawing | Shows |
|---|---|
| `ui/01-phone-play.png` | Phone play screen mid-drag, with the landing preview, annotated |
| `ui/02-tablet-play.png` | Tablet landscape, Hard: timer and flip badge |
| `ui/03-adaptive-layouts.png` | The same game state on a phone, a tablet in portrait and a tablet in landscape |
| `ui/04-browse-states.png` | New / in progress / solved |
| `ui/05-lock-rules.png` | How a dropped piece locks: outline corners, placed-piece corners, going home |
| `ui/06-solve-and-playtime.png` | Pieces → picture, and Settings with play time |

## 1. Screens
There is one main screen (Play) plus one overlay.
1. **Play:** top bar, board, tray (or the solved bar).
2. **Settings** overlay (⚙): difficulty, timer, sound, play time and best times, reset progress (with an in-page confirm step), how to play, and the note "Enjoy, it's absolutely free".

There is no separate home or menu screen: the app opens on the last puzzle you had.

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
4b. **Cell contents:** each cell adds a 26 dp strip at the bottom for the ↺ ↻ turn buttons (≈ 19 dp circles, touch area 10 dp larger), and the triangles get an S / M / L size mark in the top-left corner. Both hide while the piece is on the board.
5. **Board** = between the top bar and the tray. **Board scale** `bs` fits the silhouette with about 10 % padding.

Measured values: phone 390×844 → ts ≈ 27, bs ≈ 42–60 dp/unit depending on the puzzle. Tablet 1280×800 → ts ≈ 27.5, bs ≈ 80–113.

## 4. Sizes
| Element | Size |
|---|---|
| ‹ › buttons | 52 dp square, rounded |
| ⚙ | 44 dp |
| Restart | pill, ≥ 44 dp high, top-left of the board |
| Tray cells | computed; the smallest is about 60 × 114 dp on a 360 dp phone (with the button strip) |
| ↺ ↻ turn buttons | ≈ 19 dp circles, touch area ≈ 29 dp (a mouse and testing aid; see Q12) |
| Piece touch area | its shape grown by 12 dp |
| Flip badge | 38 dp visible, 60 dp touch area |
| Retry / Next | ≥ 48 dp high; Next is filled green and is the primary action |

## 5. Visual language
- **Piece colours** (fixed): LT1 `#E8505B` red, LT2 `#3D8BFD` blue, MT `#F9A826` orange, SQ `#FFD23F` yellow, PG `#FF7AB8` pink, ST1 `#2DBE7E` green, ST2 `#9B5DE5` purple. Pieces have a 2 dp white edge.
- Silhouette `#4A4E69` (flat, no inner lines). Paper `#FBF7EE`, board white, tray `#EFE7D6`, accent `#3D8BFD`, go-green `#2DBE7E`.
- **Picture style** (art layer): flat illustration, 3–8 colours, simple shapes, drawn inside the silhouette only. The silhouette edge stays visible as a thin outline so the "same frame" is clear.
- Type: a rounded display face (e.g. *Baloo 2*) for the puzzle name and buttons; the system font elsewhere.

## 6. Orientation and system UI
- Phone: portrait only. Tablet: both orientations; rotating recomputes the layout and keeps every piece.
- Draw edge to edge, respecting system insets; the tray stays clear of the gesture bar.
- Android back: closes an overlay; otherwise leaves the app normally (state is already saved).
