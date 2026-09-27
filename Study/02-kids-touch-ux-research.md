# 02 · How small children use touch screens: research that shapes the design

*Study note, 2026-09-27. Written when the target was ages 3–8. After round 1 the target is **8+**: the multi-touch limits below apply mainly to ages 3–6, so a two-finger twist is fine as long as one-finger turning also exists. The "never lose a piece" and "big targets" rules still apply.*

## Key findings

### Vatavu et al. 2015: "Touch interaction for children aged 3 to 6 years" (IJHCS)
Controlled study, 89 children, phone (3.5") and tablet (10.1").

| Finding | Number | What it means for us |
|---|---|---|
| Tap miss distance | 4.5 mm (age 3) → 3.4 mm (age 5) | Hit areas must be **much bigger than the visible piece**. |
| Drag-and-drop success | 73.5 % (under 4) → 89.2 % (over 5) | Roughly 1 drag in 4 fails at age 3. **A failed drag must cost nothing**: the piece glides back to the tray. |
| Single-touch success vs multi-touch | 92 % vs 53.7 % | **Never require two fingers.** Pinch or twist to rotate can only be an optional extra. |
| Phone vs tablet drag success | 90.8 % vs 72.8 % | Longer drag distances on tablets hurt. The tablet tray must sit close to the board, and the snap radius should grow with physical distance. |
| Drag time, tablet vs phone | 5.2 s vs 2.5 s | Tablets need even more forgiving snapping. |
| Tap duration | 3-year-olds may hold a "tap" for up to 5 s | A tap cannot be defined as "under 300 ms". Treat it as "**didn't move**", whatever the duration. |

The paper's own guidelines that apply to us: avoid multi-touch; adapt tolerances to age; give positive reinforcement; **design for occlusion** (the finger hides what it drags); accept unusual tap durations and give feedback after 1–2 s of contact.

### Nielsen Norman Group: design for kids by physical development
- Touch targets for children under 9: **at least 2 cm × 2 cm** (about 4× the adult minimum). On phones that is about 80 dp.
- Ages 3–5 manage tap, swipe and simple drag (large arm movements); precise gestures are hard.
- Long-distance drags frustrate children, so keep the tray close to the board.

### Parenting Science: tangrams and spatial skills
- Recommended progression: **coloured pieces with inner lines → silhouettes without lines**.
- "Learning to turn and flip shapes is often a major breakthrough."
- Spatial-rotation practice is linked to better maths performance.
- Benefits grow when adults use spatial words ("turn it", "corner", "next to"). A future idea is an optional spoken voice ("Turn it!") in place of written text.

### Google Play Families policy (applies because our audience includes children)
- No transmission of the advertising ID or device identifiers; no precise location.
- If ads were ever used, only certified SDKs, with no personalisation. **We avoid the whole topic by having no ads and no network permission at all.**
- An accurate Data safety form and a privacy policy are still required. The simplest honest statement is "collects nothing".
- The Teacher Approved programme is open to compliant apps and would be a nice goal.

## Design rules derived from the research
(These become requirements in `Spec/04-requirements.md`.)

| # | Rule |
|---|---|
| K1 | One finger does everything. Two-finger twist is optional (for ages 7+). |
| K2 | A tap is "a touch that moves less than 12 dp", whatever its duration. |
| K3 | Touch hit area of a piece = its shape grown by 12 dp (tray: the whole tray cell). |
| K4 | A dropped piece never gets lost: it snaps, stays on the board (higher levels only), or glides back to its tray cell. |
| K5 | While dragging, the piece floats **above the finger** (occlusion), with a soft shadow showing it has been lifted. |
| K6 | Snap radius is set in physical size (mm) and scales with the level. |
| K7 | There are no failure sounds, no red crosses, no timers and no lives. Wrong placement → gentle "boing" and a glide home. |
| K8 | Only icons in the child's area; text only in the grown-up area. |
| K9 | Grown-up area behind a "hold for 3 seconds" gate. |
| K10 | Idle for about 15 s at the easiest levels → the next piece gently wiggles (an automatic hint). |

## Sources
- [Vatavu, Cramariuc, Schipor: Touch interaction for children aged 3 to 6 years (PDF)](https://mintviz.usv.ro/publications/ijhcs2015.pdf)
- [NN/g: Design for Kids Based on Their Stage of Physical Development](https://www.nngroup.com/articles/children-ux-physical-development/)
- [Parenting Science: Tangrams for kids](https://parentingscience.com/tangrams-for-kids/)
- [Google Play Families Policies](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en)
- [Osmo Tangram Getting Started (difficulty prompts, hint usage)](https://assets.playosmo.com/static/downloads/GettingStartedWithOsmoTangram.pdf)
