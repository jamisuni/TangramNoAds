# STATUS: where TangramNoAds stands

**Updated:** 2026-10-01 (owner sign-off; adopted under SWDev) · **Phase:** requirements **locked**; prototype playable; technology parked; **build side scaffolded under SWDev v0.15** (intake path C — `AGENTS.md` § "Build under SWDev"), G1 acceptance pending.

## Snapshot
| What | State |
|---|---|
| Prototype | 0.6, `Spec/prototype/tangram-prototype.html` (generated from `tools/prototype_template.html`). Finnish or English by browser language. The 0.5 artifact "TangramNoAds Prototype" (https://claude.ai/artifact/7gLS46TqxRJXa214A8chva) is stale until republished |
| Puzzles | 13 in `Tangrams/`: 2 mini (3 pieces), 4 warm-ups, 7 full (house, arrow, cat, chocolate bar, sailboat, mountain, gift box). Every file has `kind` and both titles. All pass V1–V6, V8–V10, V12; none human-reviewed yet |
| Requirements | `Requirements/` v1.1: 53 features, REQ-001..051 (**47 `locked`**, signed by Jami 2026-10-01; 4 `withdrawn`); 51 of 53 features locked, 2 ideas, TYPE-001..007 (TYPE-002 withdrawn), SRC-001..015, DEF-001. Priorities not set |
| Tests | `tools/tests/test_prototype.py`: all 13 puzzles solve on phone 390×844 and tablets 1280×800 / 800×1280; tray, tap, twist, wrap, long-press jump, grid, Finnish, load check, mini-puzzle pulse, settings, DEV aid. All pass (2026-09-28) |
| Owner use so far | Web browser with a mouse (rounds 4–6). **No phone or tablet playtest yet** |
| Review | The agent's concept review (2026-09-28) was accepted as a whole in round 6; its accepted proposals are listed in `Requirements/evidence/src-011-conversation-round6.md` |

## Round history
| Round | When | What Jami said (evidence) | What changed |
|---|---|---|---|
| 0 | 09-27 07:31 | Traditional tangram, miniature tray at the bottom, phone + tablet layouts (SRC-001) | Study, spec draft 0.1, first prototype |
| 1 | 09-27 08:13 | Ideas: 8+, no free drops, tie to sides and pieces, ‹ › browsing, difficulty levels, active play time (SRC-002) | Slot-free anchor locking, draft 0.2 |
| 2 | 09-27 08:39 | No gift boxes / hidden anchors, no donations, "enjoy, it's absolutely free" (SRC-003) | Outline-corner anchors only, V11, draft 0.3 |
| 3 | 09-27 09:06–09:18 | Puzzles in their own folder, very easy starters, abstract shapes fine, other piece sets later; move into SwReqCollector (SRC-008) | `Tangrams/`, warm-ups, rectangle, `Requirements/` collection |
| 4 | 09-27 10:19 | No resizing (rule stays), show piece size in tray, spin icons in tray cells, a 3-piece puzzle (SRC-009) | S/M/L marks, ↺ ↻ buttons, mini puzzles (prototype 0.4) |
| 5 | 09-27 10:52–12:36 | Mirror-button question; keep ↺ ↻ for now; DEV cheat with passcode 0417 (SRC-010) | DEV solution reveal (prototype 0.5), handoff files |
| 6 | 09-28 06:29 | Concept review accepted: **remove the ↺ ↻ buttons** (tapping does it), **remove Easy / Medium / Hard**, flip badge always, store name **"Tangram, absolutely free"**, "I agree with all your proposals" (SRC-011) | Prototype 0.6; REQ-004/027/028/044 withdrawn; TYPE-004 one lock rule; wrap; local-midnight fix (DEF-001); `kind` field + V12; the classic square back (V11 was never a restriction); Finnish + English; #Release (privacy text, store checklist); long-press › and grid overview; load check; corner pulse in mini puzzles; haptic tick; REQ-042 = 20 puzzles |
| 7 | 10-01 18:08 | "plz lock all 47 requiments as approved" (SRC-014) | All 47 ai-approved REQs locked with Jami's sign-off; 13 inferred items confirmed as stated; collection v1.0 |
| 7b | 10-01 18:20 | "Plz do lock also" the features (SRC-015) | 51 features locked; the two ideas (#PieceSets, #Accessibility) stay ideas; collection v1.1 |

## What round 6 corrected
- **V11 cannot fail a valid tangram.** The uncovered region always has a convex corner, that corner is always an anchor (an outline corner or a placed piece's vertex), and the piece covering it must have a vertex there. "No hidden anchors" is a rule of the lock (REQ-019), not of the content. The Gift box (classic square) is back; `Study/06` and `Study/08` keep the old belief (frozen), the decision log in `Spec/05` records the correction.
- **Two prototype-vs-REQ disagreements** are settled: the list wraps (REQ-024 now says so); "Active today" resets at local midnight (DEF-001 fixed in 0.6).
- **REQ-037 vs REQ-044** (48 dp minimum vs 29 dp buttons) is gone with the buttons; the DEV button is exempt as a testing aid.

## Next steps
1. **Phone and tablet playtest by Jami** (open `Spec/prototype/tangram-prototype.html` on a device; a Finnish phone shows the Finnish text). Register what he says as the next SRC. The REQs are locked, so a finding becomes a defect (DEF-NNN) or a change delta (`Requirements/changes/CHG-NNN.md`), never a silent edit. Things to feel on a real screen: the lock distance (0.65 units, ≥ 30 dp), the landing preview's "silly but valid" spots, the flip badge next to the tray cell, the corner pulse in the mini puzzles.
2. **One open question** in `Requirements/requirements.md` §5: which developer account publishes (a personal Play account created after 13 Nov 2023 needs a 12-tester, 14-day closed test; F-Droid has no such rule). The Finnish store title was settled by the lock.
3. **Content toward 20 reviewed puzzles** (REQ-042): about seven more full puzzles over the four themes, warm-up pictures worth seeing (N6 in the review), and Jami's review of each (`reviewedByHuman`, with the DEV reveal).
4. **Priorities:** the lock is done (2026-10-01); priorities are still `none`. Jami sets them if the build needs an order. The agent never does this.
5. **Technology:** ~~open the parked topic~~ **decided 2026-10-01: Kotlin + Jetpack Compose (native), the prototype's logic ported, conditional on building on Jami's computer** (`governance.md` §2). Still open: the store account and channel (§5 open question in `Requirements/requirements.md`).
6. Republish the prototype artifact from 0.6 if the web link is still wanted.

## Under SWDev (2026-10-01)
Adopted via SWDev **intake path C**: `Requirements/` *is* the build spec (every locked level is Contract; SWDev reads the format natively, `trace_check.py`: 51 REQs, 47 locked, 37 build units). Build-side files beside the collection: `architecture.md` (placeholder), `build-map.md` (code homes per subtree, TBD until the stack is chosen), `design-inputs.md` (prototype, UI sheets, puzzle pipeline + Contract-delta), `tasks.md`, `progress.md`, `proposals.md`, `.swdev/guard.json`, `.claude/`. **Governance profile in force (ai-mastered, `governance.md` v0.2); stack decided: Kotlin + Jetpack Compose, native, if it builds on Jami's computer.** Next, in a Claude Code terminal session: **`KICKOFF.md`** (toolchain checklist + copy-paste prompt) → fresh-eyes review → "Accept collection v1.1 for TangramNoAds (G1)?" → freeze → P2 (architecture, ADR-001 = the stack, code homes) → WO-001 #Locking with the G3 toolchain proof first.

## Ready for a separate build framework?
Closer than after round 5. A build agent pointed at this folder finds: the requirements with sources (now including language, store obligations and the overview), one lock parameter set with its scoring written down (TYPE-004), the puzzle format with a validator and exact geometry, 13 puzzles, the layout rules, a working reference prototype in two languages and its tests. What is **not** ready:
- ~~Nothing is locked.~~ **Done 2026-10-01:** all 47 live REQs are locked with Jami's sign-off (SRC-014), so they pass the lock gate the build side expects. Priorities are not set.
- **The export tier** is `level: BIZ` REQs (REQ-001..007, REQ-048). The FUN and UI REQs carry the detail a builder needs; decide with the build framework how they travel.
- **Technology is parked**, and there has been no device playtest yet.
- **Framework conformance:** `Study/01–04` and `07` contain notes about third-party products, which SwReqCollector forbids in a project. They are never cited; Jami decides whether they stay (the Study folder is frozen). Three new framework findings from this round are in `Requirements/framework-notes.md`.

## How to run things
Python 3; Playwright with Chromium for the browser tools (not installed on the dev VM: the browser steps were run in the agent's workspace and the outputs written back).
```bash
python tools/validate_puzzles.py Tangrams          # V1–V6, V8–V10, V12; V11 prints the build order
python tools/render_puzzle.py Tangrams Tangrams/previews && python tools/svg2png.py Tangrams/previews/*.svg
python tools/build_prototype.py                    # rebuild the prototype
python tools/tests/test_prototype.py [--fast]      # browser tests
python tools/make_ui_sheets.py                     # Spec/ui sheets
python "C:\GitHub\AI\SwReqCollector\tools\validate.py" Requirements   # 0 errors required
python "C:\GitHub\AI\SwReqCollector\tools\views.py" Requirements      # after any REQ change
```
