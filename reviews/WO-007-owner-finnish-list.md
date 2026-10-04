# Finnish Review List — WO-007

**For:** Jami (owner, native Finnish speaker)  
**Date:** 2026-10-04

---

## 1. Accepted at G1 (F14)

The following 12 settings keys were accepted at G1 as F14 (prototype 0.6 text adopted verbatim). They do not require further review.

**Source:** Design WO-007 §6, rule: "every settings key **except** `settings_how_to` is F14" (the design lists the labels as prototype strings without naming them individually).

| Key | English | Finnish |
|-----|---------|---------|
| `settings_open_description` | Settings | Asetukset |
| `settings_title` | Settings | Asetukset |
| `settings_done` | Done | Valmis |
| `settings_sound` | Sound | Äänet |
| `settings_sound_on_description` | On | Päällä |
| `settings_sound_off_description` | Off | Pois |
| `settings_reset` | Reset all progress | Nollaa kaikki edistyminen |
| `settings_reset_confirm` | Erase | Poista |
| `settings_reset_cancel` | Keep | Säilytä |
| `settings_free_note` | Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device. | Nauti, se on ihan ilmaista. Ei mainoksia, ei ostoksia, ei verkkoa. Peliaikasi pysyy tällä laitteella. |
| `settings_privacy` | Privacy: this game collects no data. It has no network access, no accounts and no analytics. Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled. | Tietosuoja: tämä peli ei kerää mitään tietoja. Sillä ei ole verkkoyhteyttä, tilejä eikä analytiikkaa. Ratkaisusi ja peliaikasi tallennetaan vain tälle laitteelle, ja ne poistuvat kun peli poistetaan. |
| `settings_reset_question` | Really erase all solved puzzles and times? | Poistetaanko varmasti kaikki ratkaisut ja ajat? |

---

## 2. AI-Written Finnish

One setting is AI-written: the how-to-play text, adapted from the prototype's how-to without the mouse-instruction sentence and version tail.

| Key | English | Finnish |
|-----|---------|---------|
| `settings_how_to` | How to play: drag a piece onto the shape. Pieces keep their size; in the tray the triangles are marked S, M and L. Tap a piece to turn it 45°, in the tray or on the board, or hold it and twist with a second finger. The flip badge mirrors the parallelogram. A piece locks against the shape's corners and other pieces, or goes back to the tray. Hold › to jump to the next unsolved puzzle; the number in the top bar opens all puzzles. | Näin pelataan: vedä pala kuvion päälle. Palat pysyvät oikean kokoisina; tarjottimella kolmiot on merkitty S, M ja L. Napauta palaa kääntääksesi sitä 45°, tarjottimella tai laudalla, tai pidä siitä kiinni ja kierrä toisella sormella. Peilausnappi peilaa suunnikkaan. Pala napsahtaa paikalleen kuvion kulmiin ja muihin paloihin, tai palaa tarjottimelle. Pidä › pohjassa hypätäksesi seuraavaan ratkaisemattomaan tehtävään; yläpalkin numero avaa kaikki tehtävät. (reworded 2026-10-04: a promise-scan hit on 'lukittuu') |

---

## 3. Wording to Check

**Reset confirmation question:** `settings_reset_question`

**Current wording (F14, from prototype):**
- English: "Really erase all solved puzzles and times?"
- Finnish: "Poistetaanko varmasti kaikki ratkaisut ja ajat?"

**Note (design note N1):** The wording does not explicitly mention that **in-progress boards (puzzles with pieces already placed but not yet solved) are also erased**. The reset clears all puzzle progress including in-progress games. The UI provides visual confirmation within the settings screen, but the question text itself does not spell out this detail. This is an F14 adoption and goes on your review list as-is.

---

## 4. The Tones

Five sounds are AI-designed based on the prototype's sound table (DI-1), softened and tuned to REQ-033 and REQ-020/REQ-023:

| Sound | Frequency & character | Length | Notes |
|-------|----------------------|--------|-------|
| **PICK_UP** | Sine glide 520 → 700 Hz | 70 ms | Played when a piece is picked up |
| **TURN** | Sine 760 Hz | 40 ms | Played when a piece is turned or twisted |
| **LOCK** | Triangle 1320 Hz + sine 1760 Hz (delayed 30 ms) | ~90 ms | Played when a piece locks in place; followed by a **haptic tick** that can be felt if the device supports it |
| **RETURN** | Sine glide 300 → 180 Hz (soft low tone) | 180 ms | Played when a piece returns to the tray (and matches the animation length) |
| **SOLVE** | 4-note chime: triangle 523, 659, 784, 1047 Hz, one every 110 ms, each 240 ms | ~670 ms | Played when a puzzle is solved (REQ-023); includes a 100 ms lead-in of silence to separate it from a lock sound |

**Please verify on a phone:**
1. Listen to each of the five sounds in the settings screen (Sound toggle on/off tests them).
2. Are the tones pleasant and appropriate for a puzzle game for players 8+?
3. Is the lock **haptic tick** (vibration) **perceptible** when felt? (It follows the system touch-feedback setting and does not require a permission.)

---

## Count Check

```
(a) Total <string name= keys in settings/src/main/res/values-fi/strings.xml: 13
(b) F14 keys listed above (section 1): 12
(c) AI-written keys listed above (section 2): 1

Verification: (b) + (c) = 12 + 1 = 13 ✓
```

All keys are accounted for.
