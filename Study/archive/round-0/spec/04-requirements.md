# 04 · Requirements (v1 candidate list)

**Status:** draft 0.1, ready to import into SwReqCollector. Every requirement has an id, a priority (**M**ust / **S**hould / **C**ould) and an acceptance check that a person or an automated test can run. "Level" = assist level (`01-gameplay.md` §3). Drawings in `ui/`.

## Product promise (PRM)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-PRM-1 | M | The app contains no advertising SDK and shows no ads. | Dependency list has no ad SDK; manual play-through of all screens shows none. |
| REQ-PRM-2 | M | No in-app purchases, coins, lives, energy or timers. | No Billing library; UI review. |
| REQ-PRM-3 | M | The merged manifest requests **no** permissions, INTERNET included. | `aapt dump permissions` on the release build lists none. |
| REQ-PRM-4 | M | The app collects and transmits no data. The Play Data safety form says "No data collected / shared". | Network is off in the manifest; privacy policy text reviewed. |
| REQ-PRM-5 | M | The child area contains no text that must be read to play. | UI review of home, grid, play and ending screens. |
| REQ-PRM-6 | M | The app is fully usable offline, from the first launch on. | Install and play in airplane mode. |

## Pieces and geometry (GEO)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-GEO-1 | M | The classic 7 pieces with the exact shapes and areas of `03-puzzle-format.md` §3. | Unit test: areas 4,4,2,1,1,2,2; total 16. |
| REQ-GEO-2 | M | Coordinates use exact a + b√2 arithmetic for matching; floats only for drawing. | Port of `tangram_geom.Q2` passes the same test vectors as the Python reference. |
| REQ-GEO-3 | M | Identical pieces are interchangeable (LT1↔LT2, ST1↔ST2). | Test: drop LT2 on LT1's slot → snaps. |
| REQ-GEO-4 | M | Symmetric turns are accepted (SQ every 90°, PG every 180°). | Test at level 4: SQ turned 90° from its solution turn snaps. |
| REQ-GEO-5 | M | Pieces turn around their centre (vertex average). | Test: turning a free piece leaves its centre unchanged. |

## Assist levels (LVL)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-LVL-1 | M | Four assist levels with the behaviour in the table of `01-gameplay.md` §3. | One test per table row and level. |
| REQ-LVL-2 | M | Every puzzle can be played at every level without extra authoring. | Load every shipped puzzle at levels 1–4 in an instrumented test. |
| REQ-LVL-3 | M | Gift pieces = min(level gifts, preplacedOrder length, pieces − 2), taken in `preplacedOrder`. | Test with a 3-piece and a 7-piece puzzle. |
| REQ-LVL-4 | M | At levels 1–2, tray pieces arrive already turned (and flipped) as in the solution. At level 3 they arrive in a random turn, not flipped. At level 4 in a random turn, with the PG mirrored relative to its solution. | Visual test plus state test. |
| REQ-LVL-5 | S | Adaptive suggestion: suggest one level up after 3 hint-free solves in a row, and one level down after 2 puzzles in a row with ≥ 3 auto-placements. Show it as a dismissible bubble; never change the level silently. Disabled when the level is locked. | Scripted state test. |
| REQ-LVL-6 | M | Default level on first launch is 1. The grown-up area can lock levels 1–4 or set "auto". | UI test. |

## Dragging and snapping (DRG)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-DRG-1 | M | A touch that moves < 12 dp is a tap, however long it lasts. | Test: a 3 s still press = tap. |
| REQ-DRG-2 | M | Hit area: the whole tray cell; for board pieces, the shape grown by 12 dp. | Touch 10 dp outside a small triangle picks it up. |
| REQ-DRG-3 | M | On drag start the piece grows from tray scale to board scale in 120 ms (± 20) and floats with its lowest point 34 dp (phone) / 44 dp (tablet) above the touch point. | Frame capture test. |
| REQ-DRG-4 | M | The dragged piece follows the finger with no easing lag (≤ 1 frame). | Frame capture at 60 Hz. |
| REQ-DRG-5 | M | Snapping is decided only at drop: the nearest unfilled same-type slot whose centre is within the level's snap radius (1.5 / 1.1 / 0.8 / 0.5 units, minimum 34 dp); at level 4 the vertex sets must also match. | Parameterised unit test over all sample puzzles. |
| REQ-DRG-6 | M | A snap animates into place in 180 ms, auto-turning at levels 1–3, and locks the piece. | Visual + state test. |
| REQ-DRG-7 | M | A missed drop never loses a piece: back to the tray (levels 1–2, or outside the board), or stays free on the board (levels 3–4). | Drop at 20 random points → each piece is in the tray, free, or placed. |
| REQ-DRG-8 | M | Everything works with one finger. Extra pointers during a drag are ignored. | Multi-touch test. |
| REQ-DRG-9 | S | At levels 1–3 the best matching slot within range glows while dragging. | Visual test. |
| REQ-DRG-10 | C | Optional two-finger twist turns a free piece (levels 3–4) in 45° steps. | Manual test. |

## Turning and flipping (ROT)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-ROT-1 | M | Levels 3–4: tapping a free or tray piece turns it +45° clockwise with a 150 ms animation. | Test. |
| REQ-ROT-2 | M | Levels 1–2: tapping a piece only wiggles it. | Test. |
| REQ-ROT-3 | M | Level 4: a flip badge (≥ 64 dp hit area) appears next to the selected PG; tapping it mirrors the piece. | Test. |
| REQ-ROT-4 | S | The tray cell size does not change when a piece is turned (cells are sized by turn diameter). | Layout test. |

## Hints (HNT)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-HNT-1 | M | The hint button is always visible, free and unlimited. | UI review. |
| REQ-HNT-2 | M | 1st press: the next slot pulses and its piece wiggles. 2nd press within 10 s: that piece is placed automatically. | Test. |
| REQ-HNT-3 | S | Levels 1–2: an automatic wiggle hint after 15 s idle (no auto-place). | Timer test. |

## Ending and progress (WIN)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-WIN-1 | M | When all slots are filled: 400 ms pause → figure hop/wiggle → confetti → chime → ending buttons (Next 104 dp, Again 76 dp). | Visual test. |
| REQ-WIN-2 | M | Solved puzzles are stored locally and shown in colour with a ✓ in the grid. | Restart the app → still shown. |
| REQ-WIN-3 | S | Sticker book with every solved figure. | UI test. |
| REQ-WIN-4 | S | With reduced motion enabled, no confetti or hop. | Toggle the system setting. |
| REQ-WIN-5 | C | Per-puzzle `celebrate.sound` and `celebrate.eyes`. | Cat puzzle meows and blinks. |

## Layout (LAY)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-LAY-1 | M | Width < 600 dp: portrait-locked phone layout, top bar, 2-row tray (big / small pieces) when > 4 pieces. | Screenshot test at 360×780 and 412×915. |
| REQ-LAY-2 | M | Width ≥ 600 dp: tablet layout, both orientations, corner buttons, 1-row tray. | Screenshots at 800×1280 and 1280×800. |
| REQ-LAY-3 | M | Tray scale and board scale follow the algorithm in `02-ui-layout.md` §3. | Unit test against the prototype's numbers (± 1 dp). |
| REQ-LAY-4 | M | Rotating a tablet keeps every piece's state (tray/free/placed, turn, flip). | Rotate mid-puzzle. |
| REQ-LAY-5 | M | All child-area touch targets ≥ 48 dp; primary buttons ≥ 64 dp. | Accessibility scanner. |
| REQ-LAY-6 | M | Edge-to-edge drawing that respects system insets; the tray stays clear of the gesture bar. | Test on gesture-nav and 3-button-nav devices. |

## Navigation and grown-ups (NAV)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-NAV-1 | M | Home → world grid → play → ending → next, all by picture. Everything is unlocked. | UI walk-through. |
| REQ-NAV-2 | M | The grown-up area opens only after holding ⚙ for 3 s (a ring shows progress). | Test: a short tap does nothing. |
| REQ-NAV-3 | M | Back from play goes to the grid; back from home asks nothing and leaves the app normally. | Test. |
| REQ-NAV-4 | S | Grown-up area: level lock, sound, haptics, reset progress (with an in-app confirm), privacy text, licences. | UI review. |

## Content (CNT)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-CNT-1 | M | Puzzles use the `tangram-puzzle/1` format and pass `tools/validate_puzzles.py`. | CI step. |
| REQ-CNT-2 | M | v1 ships ≥ 30 puzzles, including ≥ 6 starter puzzles (≤ 4 pieces). | Count. |
| REQ-CNT-3 | S | Each world has puzzles of difficulty 1–4 spread across it. | Content report. |
| REQ-CNT-4 | M | Release gate: every shipped puzzle has `provenance.reviewedByHuman = true`. | CI step. |
| REQ-CNT-5 | S | Titles in English and Finnish. | Validator warning if `fi` is missing. |

## Quality (QLT)
| Id | Pri | Requirement | Acceptance check |
|---|---|---|---|
| REQ-QLT-1 | M | 60 fps while dragging on a 2020 mid-range phone (e.g. Samsung A51). | Frame-timing trace. |
| REQ-QLT-2 | M | Cold start to the home screen in < 1.5 s on the same phone. | Macrobenchmark. |
| REQ-QLT-3 | S | APK / AAB download size < 15 MB. | Build output. |
| REQ-QLT-4 | M | Min SDK 26 (Android 8.0), target the current Play-required SDK. | Gradle config. |
| REQ-QLT-5 | S | TalkBack: grown-up area fully accessible; the child area announces puzzle names and piece names. | Manual TalkBack pass. |
