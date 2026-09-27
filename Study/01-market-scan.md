# 01 · Market scan: Tangram games on Android (and the best of iOS)

*Study note, 2026-09-27. Sources are listed at the bottom. Store figures (ratings, review counts) are as shown on the store pages on this date, so treat them as rough.*

## What is out there

| App | Store | Model | Core mechanics | What players praise | What players complain about |
|---|---|---|---|---|---|
| **Tangram King** (Mobirix) | Play + App Store | Free, ads "every few levels", paid hints | 7 pieces, drag + snap; 5 modes (classic, double, flip, spinning, hard) | Variety of modes, music | "Blaring ads", no paid ad-free option, earlier **snapping bugs** |
| **Tangram Puzzle: Polygrams** (RV AppStudios) | Play | Free, ads + IAP, coin economy | Drag to shadow outline; **no rotation**; boosters (outline, hint, undo); 999+ puzzles; daily challenge, multiplayer | Relaxed, no timer, big library | "Pieces land in random spots", "controls are poop" |
| **Tangram Master** (Little Bear Games) | Play | Free with ads; hints bought with earned coins | "Automatic tile rotation"; Master mode hides the target; 400+ levels; Chinese + Hungarian (European) sets | Reward loop funds hints, relaxing themes | "Controls are a little dodgy", missing cloud save |
| **Tangram – Puzzle Game** (onepixel) | Play | Free, ads + IAP | 4–14 pieces into a square; hint marks the slot area; 1500+ designs in 10 bundles | Big clear pieces ("good for older eyes") | Level progression bugs (solved level won't advance) |
| **Tangram HD** | Play + App Store | Free with ads / Pro removes ads | Snap-to-place, undo | Generous free tier | Ads between puzzles |
| **Puzzle Tangram** (Penerbit Erlangga) | Play | Free, 3+ | Companion to a physical book | – | Very low reach (100+ installs) |
| **Osmo Tangram** (Tangible Play) | iPad / Fire + physical kit | Hardware ~$30–80 | Physical wooden tans, camera checks placement; **difficulty = how visible the piece boundaries are**; worlds on a map; free hints; creation "comes to life" at the end | Top educational value, excellent feedback | Needs hardware + removing the iPad case |

## Patterns every successful title shares

1. **Target on top, pieces below.** The target (silhouette) fills the upper area; pieces wait in a strip or pile below it. This is the layout we copy.
2. **Snap-to-place** is expected. Where snapping is weak or buggy, reviews go straight to "controls are bad". *Snapping quality is the #1 make-or-break feature.*
3. **Difficulty is mostly "how much of the answer is shown":**
   - coloured or outlined slots (inner lines visible) → easy
   - grey slots with inner lines → medium
   - solid silhouette, no inner lines → hard
   - hidden or moving target (Master, spinning) → expert
   Osmo builds its whole difficulty scale on this; the child-development literature describes the same ladder (Parenting Science).
4. **Rotation is the real difficulty wall.** Polygrams dropped rotation completely, Tangram Master rotates automatically, and Osmo notes that learning to turn and flip is "often a major breakthrough". For small children, rotation is where help matters most.
5. **Worlds and categories** (animals, vehicles, letters…) with a lot of content.
6. **The end moment matters.** Osmo's "your creation comes to life" is the most-praised detail in reviews.

## Where the market is weak (our opening)

| Weakness in the market | Our answer |
|---|---|
| Ads, coins and paid hints aimed at kids, "blaring ads" | **No ads, no IAP, no coins, no network.** That is the product name. Hints are free and unlimited. |
| Fiddly controls, pieces landing "in random spots" | Generous, predictable magnetic snap with exact geometry (see `03-tangram-geometry.md`). A piece never ends up somewhere odd: it either snaps or glides home. |
| Rotation too hard for 3–5 year olds, or removed completely | **Assist ladder:** pieces arrive already turned the right way → auto-turn on snap → one-tap turning → full manual. |
| Adult-oriented UI (text menus, small buttons) | Icon-only UI for children, 2 cm+ touch targets, no reading needed. |
| One difficulty for everyone | Every puzzle plays at every assist level, and **pre-placed "gift" pieces** turn a 7-piece puzzle into a 3-piece one for a toddler. |
| Tray pieces too small on tablets, crowded on phones | The tray layout adapts: 2 rows on phones, 1 wide row on tablets. |

## Takeaways for the spec

- Copy the proven layout (target above, miniature tray below). Don't reinvent it.
- Put the engineering effort into **snapping, the rotation assists and the ending moment**, not into the size of the library.
- Launch with fewer puzzles (30–60) that are hand-checked and cover all levels well. Libraries of 1000+ puzzles are a sign of an ad-driven retention model.

## Sources

- [Tangram King – Google Play](https://play.google.com/store/apps/details?id=com.mobirix.tangram&hl=en_US)
- [Tangram Puzzle: Polygrams – Google Play](https://play.google.com/store/apps/details?id=com.rvappstudios.tangram.blocks.puzzle.brain.games&hl=en_US)
- [Tangram Master – Google Play](https://play.google.com/store/apps/details?id=com.littlebeargames.tangram&hl=en_US&gl=US)
- [Tangram – Puzzle Game (onepixel) – Google Play](https://play.google.com/store/apps/details?id=studio.onepixel.tangrambee)
- [Puzzle Tangram (Erlangga) – Google Play](https://play.google.com/store/apps/details?id=com.erlangga.tangram&hl=en)
- [Osmo Tangram – Getting Started guide (PDF)](https://assets.playosmo.com/static/downloads/GettingStartedWithOsmoTangram.pdf)
- [Osmo Tangram – Common Sense Media review](https://www.commonsensemedia.org/app-reviews/osmo-tangram)
- [Best Tangram Apps for Kids – DigiKidz](https://digikidz.com/best-tangram-apps-kids/) (a secondary roundup; its app details did not all match the store pages)
