# 04 · Requirements (candidate list, draft 0.3)

**Status:** ready to import into SwReqCollector. Each item has an id, a priority (**M**ust / **S**hould / **C**ould) and a check that a person or a test can run. The prototype implements every M and S item except where noted. Technology-specific items (SDK levels, frame-rate targets) are parked until technology is discussed.

## Promise (PRM)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-PRM-1 | M | No advertising, no ad SDKs. | Dependency review + play-through |
| REQ-PRM-2 | M | No money at all: no purchases, coins, lives, energy, paid hints, locked content or donation asks. | UI review |
| REQ-PRM-5 | S | Settings shows "Enjoy, it's absolutely free…" | Visual |
| REQ-PRM-3 | M | No network access; no data leaves the device. | Permission and dependency review |
| REQ-PRM-4 | M | Fully playable offline from the first launch. | Airplane-mode test |

## Tray (TRY)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-TRY-1 | M | All 7 classic pieces, in the fixed order LT1, LT2, MT, SQ, PG, ST1, ST2, fixed colours, fixed resting turn. | Screenshot of any two puzzles: identical trays |
| REQ-TRY-2 | M | Phone: 2 rows (big / small); tablet: 1 row. Cells are sized by turn diameter, with one shared miniature scale. | Layout test (`02-ui-layout.md` §3) |
| REQ-TRY-3 | M | A piece on the board leaves a dashed ghost in its cell. | Visual |
| REQ-TRY-4 | M | There is no resizing of pieces. | UI review |

## Dragging and turning (DRG, ROT)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-DRG-1 | M | A touch that moves < 12 dp is a tap, however long it lasts. | Test |
| REQ-DRG-2 | M | On drag, a tray piece grows to board size in about 120 ms and floats above the finger. | Frame capture |
| REQ-DRG-3 | M | Board pieces can be dragged again, including back to the tray. | Test |
| REQ-ROT-1 | M | Tap on a tray piece → +45° turn. | Test |
| REQ-ROT-2 | M | Tap on a board piece → +45° turn in place; kept only if it still locks nearby, otherwise it turns back with a shake. | Test (prototype: covered) |
| REQ-ROT-3 | M | Two-finger twist during a drag turns in 45° steps (threshold ±22.5°). | Synthetic 2-pointer test (prototype: covered) |
| REQ-ROT-4 | M | Hard: a flip badge (≥ 60 dp touch area) mirrors the parallelogram, in the tray or on the board. | Test |
| REQ-ROT-5 | C | Desktop helpers: mouse wheel / R / F during a drag. | Manual |

## Locking (LCK)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-LCK-1 | M | Pieces lock only to anchor points: the silhouette's outline corners and the corners of placed pieces. There are no hidden anchors, target slots or slot outlines. | Code review against `Study/06`; test: the mountain's square dropped first goes home |
| REQ-LCK-2 | M | A lock position must be fully inside the silhouette and overlap no piece. | Unit test |
| REQ-LCK-3 | M | A drop has exactly two outcomes: lock at the best spot within the lock distance, or return to the tray. No loose pieces. | 50 random drops → each piece is either locked or in the tray |
| REQ-LCK-4 | M | Solved = all 7 on the board. Any exact cover is accepted, not only the stored solution. | Test with an alternative arrangement |
| REQ-LCK-5 | S | Landing preview while dragging (Easy, Medium). | Visual |
| REQ-LCK-6 | M | Geometry uses the exact rules of `tools/tangram_geom.py` (validation) and tolerances ≤ 1e-6 units (runtime). | Unit tests |

## Difficulty (DIF)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-DIF-1 | M | Easy / Medium / Hard settings as in `01-gameplay.md` §5; Medium is the default. | Tests per row |
| REQ-DIF-2 | M | The same puzzles at every difficulty; ‹ › order is by puzzle difficulty (1–5). | Review |

## Browsing and state (BRW)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-BRW-1 | M | Top bar: ‹ name, difficulty dots, n / N, state › and ⚙, on every screen size. | Visual |
| REQ-BRW-2 | M | Every puzzle keeps its own state (new / in progress / solved) and, when in progress, every piece's position, turn and flip. | Leave, browse, return |
| REQ-BRW-3 | M | State survives closing the app. | Kill and relaunch |
| REQ-BRW-4 | M | Restart (in progress) and Retry (solved) reset the pieces; Retry keeps the best time. | Test |
| REQ-BRW-5 | M | A solved puzzle shows only its picture: no piece lines, no positions. | Visual |
| REQ-BRW-6 | M | › can be used at any time (skip); there is no locked content. | Test |
| REQ-BRW-7 | S | The app opens on the last viewed puzzle. | Relaunch |

## Solving (WIN)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-WIN-1 | M | Solve moment: pop + chime + confetti, then the picture fades in (≈ 0.8 s) with exactly the silhouette outline. | Visual |
| REQ-WIN-2 | M | The solved bar shows Retry · best time · Next (Next is the primary action). | Visual |
| REQ-WIN-3 | S | With reduced motion enabled: no confetti or pop. | System setting |

## Time (TIM)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-TIM-1 | M | Active time counts only while the app is visible and the last touch was ≤ 60 s ago. | Idle test |
| REQ-TIM-2 | M | Per-puzzle time pauses when browsing away and resumes on return; the best time is stored per puzzle. | Test |
| REQ-TIM-3 | M | Today's and the total active time are stored locally and shown in Settings. | Visual |
| REQ-TIM-4 | S | Timer on screen: setting Hard only (default) / Always / Never. | Test |

## Settings (SET)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-SET-1 | M | Difficulty, timer display, sound, play-time stats, reset progress (with a confirm step). | UI review |
| REQ-SET-2 | S | A short "how to play" text in Settings. | Review |

## Content (CNT)
| Id | Pri | Requirement | Check |
|---|---|---|---|
| REQ-CNT-1 | M | Every puzzle passes `tools/validate_puzzles.py` (V1–V11): all 7 pieces, an `art` picture, buildable edge-first. | CI step |
| REQ-CNT-2 | M | The release gate is `reviewedByHuman = true` on every shipped puzzle. | CI step |
| REQ-CNT-3 | S | ≥ 40 puzzles at launch, spread over difficulties 1–5. | Count |
| REQ-CNT-4 | S | English and Finnish titles. | Validator warning |
