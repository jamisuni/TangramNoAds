# 00 · Vision

**Status:** draft 0.3 · 2026-09-27 · owner: Jami · written with AI (Claude). Feedback rounds: `Study/05-round1-feedback.md`, `Study/08-round2-decisions.md`. Draft 0.1 is archived in `Study/archive/round-0/`.

## Theme
**Enjoy, it's absolutely free.** No ads, no purchases, no donations, no network. Nobody needs money from this game.

## One sentence
A classic 7-piece Tangram for **school-age players (8+) and adults** on Android phones and tablets. It is easy to *control* and honestly challenging to *solve*, and it has **no ads, no purchases, no network and no data collection**.

## Who it is for
| Persona | Age | Needs |
|---|---|---|
| **Mira** | 8–10 | Real tangram puzzles (silhouette only). Wants to turn pieces herself and to "finish them all". Gets frustrated by fiddly controls, not by hard puzzles. |
| **Otto** | 11–14 | Wants Hard mode, a timer and best times. |
| **Parent / adult** | adult | Plays too. Trusts the app: no ads, no pressure, nothing collected. Works offline in the car. |

## The product promise
1. **Free to enjoy. No ads, ever.** No ad SDKs.
2. **No money at all:** no purchases, coins, lives, energy, paid hints or donation asks.
3. **No network, and no data leaves the device.** Play times are stored locally only.
4. **Controls never fight you.** A dropped piece either locks into a sensible place or goes home.
5. **You are never stuck:** › always moves on, and progress is never lost.

## v1 scope (what the prototype explores)
- Classic 7 pieces, silhouette-only puzzles, any correct solution wins.
- Drag and drop, tap to turn 45°, two-finger twist in 45° steps, flip for the parallelogram (Hard).
- Slot-free locking against the silhouette and the other pieces (`Study/06-snapping-by-anchors.md`).
- ‹ › browsing with per-puzzle state (new / in progress / solved), Restart, Retry.
- Easy / Medium / Hard control settings.
- A stylised picture revealed on solve (`art` layer in each puzzle).
- Per-puzzle solve time, best time, and active play time (local only).
- Puzzles are built edge-first: pieces tie to the outline corners and to each other (validator rule V11).
- Phone portrait (2-row tray) and tablet (1-row tray) layouts.

## Out of scope for now
Technology choice (parked), hints, toddler modes, user-made puzzles, profiles, cloud save, iOS. Donations were dropped for good.

## How we will know it works
- Playtest children (8–10) never ask "why won't it go there?" more than once per session.
- Nobody loses a piece or needs help with the controls.
- Players finish a first 7-piece puzzle in under 10 minutes on Easy.
