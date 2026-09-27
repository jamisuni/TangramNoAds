# STATUS: where TangramNoAds stands

**Updated:** 2026-09-27 (after owner round 5) · **Phase:** requirements capture with a playable prototype; technology parked.

## Snapshot
| What | State |
|---|---|
| Prototype | 0.5, `Spec/prototype/tangram-prototype.html` (generated from `tools/prototype_template.html`). Also published as Jami's private artifact "TangramNoAds Prototype": https://claude.ai/artifact/7gLS46TqxRJXa214A8chva |
| Puzzles | 12 in `Tangrams/`: 2 mini (3 pieces), 4 warm-ups, 5 figures, 1 rectangle. All pass V1–V11; none human-reviewed yet |
| Requirements | `Requirements/`: 49 features, REQ-001..046 (45 `ai-approved`, REQ-042 `draft`), TYPE-001..007, SRC-001..010. **0 locked, no priorities, no sign-off** |
| Tests | `tools/tests/test_prototype.py`: all 12 puzzles solve at Easy/Medium/Hard on phone and tablet sizes; tray, turning, twist and DEV aid checks. All pass |
| Owner use so far | Web browser with a mouse (rounds 4–5). **No phone or tablet playtest yet** |

## Round history
| Round | When | What Jami said (evidence) | What changed |
|---|---|---|---|
| 0 | 07:31 | Traditional tangram, miniature tray at the bottom, phone + tablet layouts (SRC-001) | Study, spec draft 0.1, first prototype |
| 1 | 08:13 | Ideas: 8+, no free drops, tie to sides and pieces, ‹ › browsing, difficulty levels, active play time (SRC-002) | Slot-free anchor locking, draft 0.2 |
| 2 | 08:39 | No gift boxes / hidden anchors, no donations, "enjoy, it's absolutely free" (SRC-003) | Outline-corner anchors only, V11, draft 0.3 |
| 3 | 09:06–09:18 | Puzzles in their own folder, very easy starters, abstract shapes fine, other piece sets later; move into SwReqCollector (SRC-008) | `Tangrams/`, warm-ups, rectangle, `Requirements/` collection |
| 4 | 10:19 | No resizing (rule stays), show piece size in tray, spin icons in tray cells, a 3-piece puzzle (SRC-009) | S/M/L marks, ↺ ↻ buttons, mini puzzles (prototype 0.4) |
| 5 | 10:52–12:36 | Mirror-button question; keep ↺ ↻ for now; DEV cheat with passcode 0417 (SRC-010) | DEV solution reveal (prototype 0.5), handoff files |

## Next steps
1. **Phone and tablet playtest by Jami** (open the prototype on a device). Register what he says as a new SRC and classify every REQ: confirmed → `stated`, wrong → defect or rewrite, unused → withdraw.
2. **Answer the open questions** in `Spec/05-open-questions.md` (Q1 difficulty meaning, Q2 tap-turn, Q5 landing preview, Q6 timer, Q8 puzzle count, Q9 store name, Q12 tray aids, Q13 mirroring).
3. **Owner lock:** Jami sets priorities and signs off the items he wants built (`Requirements/CLAUDE.md`). The agent never does this.
4. **Content:** more puzzles toward the first-release count (REQ-042 is still a draft), and Jami's review of each (`reviewedByHuman`).
5. **Technology:** open the parked topic (Android stack, how the prototype's logic carries over).

## Ready for a separate build framework?
Partly. A build agent pointed at this folder finds everything it needs to *understand* the game: the requirements with sources, the puzzle format with a validator and exact geometry, 12 puzzles, the layout rules, a working reference prototype and its tests. What is **not** ready:
- **Nothing is locked.** In the SwReqCollector model the build side takes only items after the lock gate (owner sign-off). All items are `ai-approved`: good enough for a prototype, not a build contract.
- **Handing `ai-approved` items to a build framework** is itself an open SwReqCollector decision (`Target: both`, see its `plan-v0.2-proposal.md`). Until it is settled, a build from this collection is a prototype arrangement.
- **The export tier** is `level: BIZ` REQs (REQ-001..007). The FUN and UI REQs carry the detail a builder needs; decide with the build framework how they travel.
- **Technology is parked**, and there has been no device playtest yet.
- **Framework conformance:** `Study/01–04` and `07` contain notes about third-party products, which SwReqCollector forbids in a project. They are never cited; Jami decides whether they stay (the Study folder is frozen).

## How to run things
Python 3; Playwright with Chromium for the browser tools.
```bash
python tools/validate_puzzles.py Tangrams          # V1–V11 for every puzzle
python tools/render_puzzle.py Tangrams Tangrams/previews && python tools/svg2png.py Tangrams/previews/*.svg
python tools/build_prototype.py                    # rebuild the prototype
python tools/tests/test_prototype.py [--fast]      # browser tests
python tools/make_ui_sheets.py                     # Spec/ui sheets
python "C:\GitHub\AI\SwReqCollector\tools\validate.py" Requirements   # 0 errors required
python "C:\GitHub\AI\SwReqCollector\tools\views.py" Requirements      # after any REQ change
```
