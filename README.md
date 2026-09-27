# TangramNoAds

A classic 7-piece **Tangram for players aged 8 and up** on Android phones and tablets. The controls are easy and the puzzles are honestly challenging. **Enjoy, it's absolutely free:** no ads, no purchases, no donations, no network.

> Status: study and prototype phase (draft 0.3, 2026-09-27, round 2). The spec will later move into SwReqCollector. Technology choices are parked.

## Try it
Open **`Spec/prototype/tangram-prototype.html`** in a browser, ideally on a phone and a tablet.
- Drag pieces onto the silhouette. A piece locks to the shape's corners or to pieces already placed, or goes back to the tray.
- **Tap** to turn 45°. **Hold a piece and twist a second finger** to turn in 45° steps. On a computer, use the mouse wheel or **R** while dragging.
- **‹ ›** to browse and skip. ⚙ for Easy / Medium / Hard, timer, sound and play-time stats.

## Folder map
| Path | What |
|---|---|
| `Study/01–04` | Round 0 research: market scan, touch UX research, tangram geometry, first design ideas |
| `Study/05-round1-feedback.md` | Jami's round-1 ideas and how the design answers them |
| `Study/06-snapping-by-anchors.md` | Slot-free locking: how a dropped piece finds its place |
| `Study/07-play-time-and-donations.md` | Active play-time tracking (donations dropped, with the reason) |
| `Study/08-round2-decisions.md` | Round 2: outline-corner anchors only, no donations |
| `Study/archive/` | Draft 0.1 spec, drawings and tools; removed puzzles |
| `Spec/00–05` | Vision, gameplay, UI layout, puzzle format, requirements, open questions |
| `Spec/ui/` | Annotated screenshots of the prototype plus the locking diagram |
| `Spec/puzzles/` | 5 sample puzzles (all 7 pieces plus picture art), JSON Schema, review sheets |
| `Spec/prototype/` | The playable prototype (generated) |
| `tools/` | Geometry reference, validator, preview renderer, prototype builder, UI sheet generator |
| `AGENTS.md` | Rules for AI agents working in this repo |

## Tools
```bash
python tools/validate_puzzles.py Spec/puzzles                      # rules V1–V11 + build order, exit 1 on failure (no dependencies)
python tools/render_puzzle.py Spec/puzzles Spec/puzzles/previews   # silhouette | pieces | picture sheets (SVG)
python tools/build_prototype.py                                    # rebuild the HTML prototype
python tools/make_ui_sheets.py                                     # Spec/ui/*.png from the prototype (needs Playwright + Chromium)
```

## Licence
GPL-3.0 (see `LICENSE`).
