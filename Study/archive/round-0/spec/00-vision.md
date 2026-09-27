# 00 · Vision

**Status:** draft 0.1 · 2026-09-27 · owner: Jami · written with AI (Claude); to be moved into SwReqCollector later.

## One sentence
A classic 7-piece Tangram for children aged 3–8 on Android phones and tablets. It is the easiest-to-play tangram on the store, and it has **no ads, no purchases, no network and no data collection**.

## Who it is for

| Persona | Age | Needs |
|---|---|---|
| **Toddler** "Aino" | 3–4 | Big pieces, colour matching, pieces already turned, success within a minute, sounds and colour as rewards. A parent may be sitting next to her. |
| **Pre-schooler** "Leo" | 5–6 | Wants a real challenge but still gets stuck on turning pieces; loves animals and vehicles; plays alone. |
| **School child** "Mira" | 7–8 | Silhouette puzzles, turning and flipping, wants to finish "all of them". |
| **Parent** | adult | Trusts the app: no ads, no in-app purchase traps, no chat, no data. Can set the level. Wants the app to run offline in the car. |

## The product promise (non-negotiable)
1. **No ads, ever.** No ad SDKs in the build.
2. **No in-app purchases, coins, lives, timers or energy.**
3. **No INTERNET permission.** The app works fully offline and collects nothing.
4. **No reading needed** in the child's area.
5. **A child cannot get stuck:** hints are free, and a piece is never lost.
6. **Every puzzle works at every assist level** (see `01-gameplay.md`).

## What "done" looks like for v1 (MVP)
- The classic 7-piece set with exact geometry and magnetic snapping.
- 4 assist levels, gift pieces, free hints, idle hint.
- 30–60 hand-checked puzzles in 5–6 picture worlds (starter, animals, things, vehicles, people, shapes).
- Phone portrait layout (2-row tray) and tablet layout (1-row tray, both orientations).
- A celebration ending and a sticker book.
- A grown-up area behind a hold gate: level lock, sound on/off, reset progress.
- Released on Google Play as a Families-programme app with the Data safety answer "no data collected".

## Out of scope for v1 (parked in `Study/04-design-ideas.md` §10)
Free-build mode, several child profiles, spoken voice, alternative-solution acceptance, other piece sets (Hungarian, 14-piece), cloud save, iOS.

## Success measures (we cannot collect analytics, so these are qualitative)
- A 3-year-old solves a level-1 puzzle **without adult help** on the first try (checked in a family playtest).
- No playtest child ever "loses" a piece or needs an adult to fix the screen.
- Store reviews do not mention "controls", "snapping" or "ads".
