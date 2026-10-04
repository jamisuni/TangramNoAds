# WO-006 Finnish Review List (Task X-1, DA-106)

Owner review of AI-written Finnish strings for promise checks, UI text, and DevTools.

## Section 1: Finnish Forbidden-Word Stems (DA-106)

All entries are **PREFIX** type, matching word-start prefixes case-insensitively. Word boundaries are defined by Unicode letter runs; digits, spaces, hyphens, punctuation and format specifiers (`%1$d`) end a word.

From `app/src/test/kotlin/io/github/jamisuni/tangram/promise/PromiseWords.kt`:

| Stem | Type | Purpose | Example NOT Hit |
|------|------|---------|-----------------|
| mainok | PREFIX | Ad/advertisement variants (mainoksia) | — |
| mainos | PREFIX | Ad/advertisement | — |
| ostok | PREFIX | Shop variants (ostoksia) | — |
| ostos | PREFIX | Purchase/shop | — |
| osta | PREFIX | Buy/purchase variants (ostaa, ostettava) | — |
| hinn | PREFIX | Price variants (hinna, hinnat) | — |
| hinta | PREFIX | Price | — |
| tilau | PREFIX | Order/reservation (tilaus, tilaukseen) | — |
| arvostel | PREFIX | Review/rating (arvosteluja) | — |
| arvio | PREFIX | Rating/assessment (arvioita, arvioilta) | — |
| lahjoi | PREFIX | Donate variants (lahjoita, lahjoitukset) | — |
| lahjoit | PREFIX | Donate variants (lahjoituksia) | — |
| raha | PREFIX | Money (rahaa, rahoja) | — |
| euro | PREFIX | Currency (euroja) | — |
| maksu | PREFIX | Payment/pay (maksun, maksut) | maksimi |
| maksa | PREFIX | Pay verb (maksaa, maksavat) | maksimi |
| maksul | PREFIX | Paid/chargeable (maksullinen) | maksimi |
| makse | PREFIX | Pay passive (maksetaan) | maksimi |
| makso | PREFIX | Pay past (maksoi, maksoivat) | maksimi |
| kaupp | PREFIX | Shop/commerce (kauppa, kaupassa) | kaupunki |
| kauppa | PREFIX | Shop/store (kauppaa, kaupan) | kaupunki |
| kaupa | PREFIX | Shop/commerce (kaupan, kaupat, kaupassa) | kaupunki |
| kaupan | PREFIX | Shop/commerce (kaupan → "of the shop") | kaupunki |

**Note:** The stem list avoids "maks" alone (would hit *maksimi*) and "kaup" alone (would hit *kaupunki*). Each stem is long enough to catch inflected forms while missing legitimate words.

---

## Section 2: Finnish UI Strings Not Yet on Owner List

**Modules:** app, play, browse, devtools. **Excluded:** DA-60 keys (browse) and DA-80 keys (devtools, listed separately).

### app module

| Key | Module | English Value | Finnish Value |
|-----|--------|---------------|---------------|
| app_name | app | Tangram | Tangram |

### play module

| Key | Module | English Value | Finnish Value |
|-----|--------|---------------|---------------|
| flip_badge | play | Flip the parallelogram | Peilaa suunnikas |
| size_large_description | play | Big triangle | Iso kolmio |
| size_medium_description | play | Medium triangle | Keskikokoinen kolmio |
| size_small_description | play | Small triangle | Pieni kolmio |
| size_mark_large | play | L | L |
| size_mark_medium | play | M | M |
| size_mark_small | play | S | S |
| board_description | play | Puzzle board | Pelialue |
| tray_description | play | Piece tray | Tarjotin |

### browse module

| Key | Module | English Value | Finnish Value |
|-----|--------|---------------|---------------|
| prev_puzzle | browse | Previous puzzle | Edellinen tehtävä |
| next_puzzle | browse | Next puzzle (hold: next unsolved) | Seuraava tehtävä (pidä pohjassa: seuraava ratkaisematon) |
| all_puzzles | browse | All puzzles | Kaikki tehtävät |
| done | browse | Done | Valmis |
| state_in_progress | browse | in progress | kesken |
| state_solved | browse | solved ✓ | ratkaistu ✓ |
| restart | browse | Restart | Aloita alusta |
| retry | browse | Retry | Uudestaan |
| next | browse | Next | Seuraava |
| best_time | browse | best time | paras aika |
| best_time_none | browse | – | – |
| puzzle_counter | browse | %1$d / %2$d | %1$d / %2$d |
| grid_cell_description | browse | %1$d. %2$s · %3$s | %1$d. %2$s · %3$s |
| time_minutes_seconds | browse | %1$d:%2$02d | %1$d:%2$02d |

### devtools module

| Key | Module | English Value | Finnish Value |
|-----|--------|---------------|---------------|
| devtools_button | devtools | DEV | DEV |
| devtools_ok | devtools | OK | OK |
| devtools_done | devtools | Done | Valmis |

---

## Section 3: Already on the Owner List (Not Repeated)

### DA-60 (WO-004 design, browse module)

Three keys: the new-puzzle state string and two content descriptions written for text-to-speech clarity.

| Key | Module | Location | English Value | Finnish Value |
|-----|--------|----------|---------------|---------------|
| state_new | browse | `decisions.md` DA-60 | new | uusi |
| puzzle_counter_description | browse | `decisions.md` DA-60 | Puzzle %1$d of %2$d. Show all puzzles | Tehtävä %1$d, yhteensä %2$d. Näytä kaikki tehtävät |
| rating_description | browse | `decisions.md` DA-60 | Difficulty %1$d of 5 | Vaikeus %1$d viidestä |

**Note:** DA-60 states: "the grid button reuses the prototype's 'Valmis'" (`done` key).

### DA-80 (WO-005 design, devtools module)

Twelve DevTools UI strings, all AI-written except for the done button. Listed in `decisions.md` DA-80, with review commentary at `reviews/WO-005-design-review.md` N6.

| Key | Module | Location | English Value | Finnish Value |
|-----|--------|----------|---------------|---------------|
| devtools_button_description | devtools | `decisions.md` DA-80 | Developer tools | Kehittäjän työkalut |
| devtools_title | devtools | `decisions.md` DA-80 | Developer | Kehittäjä |
| devtools_hint_locked | devtools | `decisions.md` DA-80 | Testing aid, not part of the game. Enter the passcode to see how the pieces go. | Testausapu, ei osa peliä. Anna tunnuskoodi nähdäksesi, miten palat menevät. |
| devtools_passcode_label | devtools | `decisions.md` DA-80 | Passcode | Tunnuskoodi |
| devtools_wrong_passcode | devtools | `decisions.md` DA-80 | Wrong passcode. | Väärä tunnuskoodi. |
| devtools_cancel | devtools | `decisions.md` DA-80 | Cancel | Peruuta |
| devtools_hint_unlocked | devtools | `decisions.md` DA-80 | Unlocked until the app is restarted. The solution overlay stays on while you browse puzzles. | Auki, kunnes sovellus käynnistetään uudelleen. Ratkaisu pysyy näkyvissä, kun selaat tehtäviä. |
| devtools_show_solution | devtools | `decisions.md` DA-80 | Show the solution | Näytä ratkaisu |
| devtools_hide_solution | devtools | `decisions.md` DA-80 | Hide the solution | Piilota ratkaisu |
| devtools_solve_now | devtools | `decisions.md` DA-80 | Solve this puzzle now | Ratkaise tämä tehtävä nyt |
| devtools_no_best_time | devtools | `decisions.md` DA-80 | A puzzle solved here does not set a best time. | Näin ratkaistusta tehtävästä ei tallennu parasta aikaa. |
| devtools_solve_failed | devtools | `decisions.md` DA-80 | The game engine could not place this puzzle's stored solution. | Pelimoottori ei saanut tämän tehtävän tallennettua ratkaisua paikoilleen. |

**Note:** DA-80 states: "all Finnish is AI-written (only 'Valmis' is prototype Finnish)". The three non-AI-written keys are `devtools_button` (symbol "DEV"), `devtools_ok` (symbol "OK"), and `devtools_done` (prototype "Valmis"), which appear in Section 2.

---

## Count Verification

- **(a) Total `values-fi` keys across all modules:** 42
- **(b) Size of excluded set:** 15
  - browse: 3 keys (DA-60)
  - devtools: 12 keys (DA-80)
- **(c) Keys in Section 2:** 27

**Check:** 27 = 42 − 15 ✓

