# Store listing draft (Google Play)

Draft by the AI (TASK-094, WO-009). The owner reviews and enters it in the Play Console. Nothing here is entered or published yet.

**Every Play fact in this file is marked "verify in the Console".** Play's rules and limits change; the lengths and rules below date from the design of 2026-10-05.

The Finnish texts are AI-written and wait on the owner's Finnish list (`reviews/WO-009-owner-finnish-list.md`, TASK-094b).

## Fields

### title.en

```
title.en: Tangram Playtime
```

Pending CHG (CA-12): REQ-048 A2 still locks "Tangram, absolutely free" until the capture side records the change and the owner re-locks it. The owner picked "Tangram Playtime" on 2026-10-05 (DA-154).
Play bars pricing and promotional words such as "free" in an app title: verify in the Console. The title limit is 30 characters: verify in the Console. This title is 16.

### title.fi

```
title.fi: Tangram Playtime
```

Pending CHG (CA-12). Default: the same name as the English title. The Finnish listing title is on the owner's Finnish list, to be confirmed or replaced by the owner. AI-written default.

### short.en

```
short.en: Absolutely free, no ads, no purchases, no network. A calm tangram puzzle.
```

The "absolutely free, no ads" promise lives here and in `full`, not in the title. The short description limit is 80 characters: verify in the Console. Whether Play accepts the word "free" in the short description: verify in the Console.

### short.fi

```
short.fi: Ilmainen, ei mainoksia, ei ostoksia, ei verkkoa. Rauhallinen tangram-palapeli.
```

AI-written Finnish. Verify the 80-character limit in the Console.

### full.en

```
full.en:
Tangram Playtime is a calm puzzle game for players aged 8 and up. Fit flat pieces together to fill each outline, then watch the picture appear.

Enjoy, it's absolutely free.
- No ads.
- No purchases, tips or donations.
- No network: the game works without a connection.
- No accounts and no data collected.

How it plays
- Drag a piece into the outline. A tap turns it.
- Pieces click into place when they fit.
- Solve a puzzle to reveal its picture.
- The game remembers your progress and your best times on your device only.

The game follows your device language, English or Finnish.

Privacy: this game collects no data. It has no network access, no accounts and no analytics. Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled.
```

The full description limit is 4000 characters: verify in the Console. No puzzle count is stated: the shipped count is the owner's. The last paragraph repeats the in-app privacy sentence verbatim.

### full.fi

```
full.fi:
Tangram Playtime on rauhallinen palapeli 8-vuotiaille ja sitä vanhemmille. Sovita litteitä paloja yhteen täyttämään jokainen ääriviiva ja katso, miten kuva ilmestyy.

Nauti, se on ihan ilmaista.
- Ei mainoksia.
- Ei ostoksia, tippejä eikä lahjoituksia.
- Ei verkkoa: peli toimii ilman yhteyttä.
- Ei tilejä eikä kerättyjä tietoja.

Näin pelataan
- Vedä pala ääriviivan sisään. Napautus kääntää palaa.
- Palat napsahtavat paikalleen, kun ne sopivat.
- Ratkaise palapeli, niin sen kuva paljastuu.
- Peli muistaa edistymisesi ja parhaat aikasi vain sinun laitteellasi.

Peli seuraa laitteen kieltä, suomea tai englantia.

Tietosuoja: tämä peli ei kerää mitään tietoja. Sillä ei ole verkkoyhteyttä, tilejä eikä analytiikkaa. Ratkaisusi ja peliaikasi tallennetaan vain tälle laitteelle, ja ne poistuvat kun peli poistetaan.
```

AI-written Finnish, except the last paragraph, which is the in-app Finnish sentence verbatim.

## Other fields

```
privacy-policy-url: TBD-OWNER
developer: TBD-OWNER
contact: TBD-OWNER
```

- `privacy-policy-url`: the owner's. A policy hosted in the repository works only if the repository is public (the owner's choice): verify in the Console that Play accepts the URL.
- `developer` and `contact`: the developer name and the contact email are personal data and the owner's. Play's Metadata policy on developer names: verify in the Console.
- There is no version line: the version is the owner's word (`versionCode` and `versionName` stay as they are).

## declarations

Console answer sheet. Every row below is a Play fact: verify in the Console.

```
declarations:
  ads: no
  in-app-products: none
  data-collected: none
```

- ads: **no**. True by REQ-001; the app has no ad code. Verify in the Console that the listing shows no "Contains ads" label.
- in-app products: **none**. True by REQ-001. Verify in the Console that the listing shows no "In-app purchases" label.
- data collected: **none**. True by REQ-010 and the release scan's denied-SDK list (V-08). Verify in the Console that the Data safety form reads "no data collected".
- Target audience: includes children under 13, so the Families policies apply: no location permission, no unapproved third-party code, no ads. Verify in the Console.
- Content rating: the questionnaire is completed by the owner before release; Play bars an unrated app. Verify in the Console.
- Permissions: none declared. Verify in the Console's bundle explorer.
- The teacher-approved programme is store-side and not a build item. Verify in the Console whether it exists for this app.
