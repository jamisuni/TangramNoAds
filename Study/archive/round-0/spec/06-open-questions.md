# 06 · Open questions and decisions

Questions for Jami. Each proposal is what the spec assumes until decided otherwise.

| # | Question | Current assumption | Notes |
|---|---|---|---|
| Q1 | Kotlin + Compose (Android only) or Kotlin/Compose Multiplatform (Android now, iOS later)? | Android-only Compose, with `geometry/` kept platform-free | See `05-platform-and-tech.md`. |
| Q2 | Is the app free, or paid once (e.g. €2–3) on Play? | Free, no IAP | Paid-once also fits "no ads"; it affects the Families listing only slightly. |
| Q3 | Languages at launch? | English + Finnish (only the grown-up area and titles have text) | The child area is text-free, so more languages are cheap. |
| Q4 | Should level 3 auto-turn on snap, or require tapping like level 4? | Auto-turn (it is the bridge between "turned for you" and "you turn it") | Playtest will tell. |
| Q5 | Level 4: must a child also *flip* the PG, or is flip automatic? | Must flip (the flip badge) | Classic tangram rules allow flipping; some apps skip it. |
| Q6 | Accept **alternative solutions** that fill the silhouette differently? | v1: only the stored solution (with interchangeable identical pieces). Later: coverage check. | Matters mostly for the square and simple shapes at level 4. |
| Q7 | Levels 3–4: may free pieces overlap each other or the silhouette edge? | Yes; only slots matter | Simplest for kids. |
| Q8 | Sticker book: a plain grid, or a scene (farm, sea…) where stickers are placed? | Grid in v1 | The scene is a nice v1.1 reward. |
| Q9 | Sound style: synthesized blips (as in the prototype) or recorded, warm sounds (wood clicks, voices)? | Recorded wooden clicks + a soft chime | The wooden-block feel suits tangram. |
| Q10 | App name on the store? "TangramNoAds" is the repo name. | TBD, e.g. "Tangram Kids – No Ads" | Check trademark and store-name collisions. |
| Q11 | Several children on one device (profiles)? | Not in v1 | |
| Q12 | Content volume for v1? | 36 puzzles: 6 per world × 6 worlds | AI-assisted authoring + human review (`03-puzzle-format.md` §5). |

## Decision log
| Date | Decision | By |
|---|---|---|
| 2026-09-27 | Target phone portrait (2-row tray) + tablet (1-row tray); no ads, no network. | Jami (initial brief) |
