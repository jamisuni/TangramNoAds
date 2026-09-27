# 05 · Platform and technology (proposal)

**Status:** proposal for discussion. Nothing is decided yet; see `06-open-questions.md`.

## Recommended stack
| Concern | Proposal | Why |
|---|---|---|
| Language / UI | **Kotlin + Jetpack Compose** | Current Android standard; Compose `Canvas` + `pointerInput` handles the drag, draw and animation needs without a game engine. |
| Rendering | Compose `Canvas` drawing `Path`s; piece transforms via `graphicsLayer` or `withTransform` | 7 polygons + 1 target: trivial load, and easily 60 fps. |
| Animation | `Animatable` / `animateFloatAsState` for grow, snap, turn and flip; spring or tween specs from `01-gameplay.md` | |
| Geometry | A small pure-Kotlin module `geometry/` porting `tools/tangram_geom.py` (a `Q2` value class of two `BigFraction` or `Rational(Long, Long)` pairs) | Exact matching; unit-testable on the JVM with no Android. |
| Content | `assets/puzzles/*.json`, parsed with kotlinx.serialization | The same files as `Spec/puzzles`, validated in CI. |
| Persistence | DataStore (Preferences) for solved ids, level, settings | Local only, tiny. |
| Sound | `SoundPool` with short OGG files | Low latency; no network. |
| Window classes | `androidx.window` / `WindowSizeClass` for compact vs medium/expanded | Matches `02-ui-layout.md`. |
| Build | Gradle KTS, version catalog; CI runs unit tests + `python tools/validate_puzzles.py` | |
| Min / target SDK | 26 / the current Play requirement | Covers about 97 % of active devices. |
| Permissions | **None.** Explicitly remove INTERNET if a library adds it (`tools:node="remove"`). | Product promise. |

Suggested module layout:
```
app/            Compose UI, screens, navigation, sound
geometry/       pure Kotlin: Q2, pieces, placement, matching, layout algorithm
content/        puzzle JSON + loader + validation (same rules as the Python tool)
```

## Alternatives considered
| Option | For | Against |
|---|---|---|
| Godot / Unity | Scene tooling, easy particles | Heavy APK; Unity adds analytics/ads baggage to strip; overkill for 7 polygons |
| Flutter | One codebase for iOS later | Another toolchain; the user asked for Android first |
| Kotlin Multiplatform + Compose Multiplatform | Android now, iOS later with shared geometry and UI | Slightly more setup; a good upgrade path — keep `geometry/` free of Android types to allow it |
| Web (PWA) | The prototype already runs | Not a "traditional Android" store app; weaker Families-programme fit |

## The prototype as the executable spec
`Spec/prototype/tangram-prototype.html` (built by `tools/build_prototype.py`) implements the layout algorithm, snapping, levels, hints and the ending in about 540 lines of plain HTML and JavaScript. When the spec and the prototype disagree, **the spec wins**, and the prototype is fixed. When behaviour is unclear, play the prototype on a real phone and a real tablet before deciding.

## Test strategy
- JVM unit tests for `geometry/`: congruence, matching, symmetric turns, interchangeable pieces, the layout numbers from `02-ui-layout.md` §3.
- Puzzle content tests: run every JSON file through the Kotlin validator **and** the Python reference, and require identical results.
- Compose UI tests with injected pointer events for REQ-DRG-* (drag, drop, missed drop, tap vs. hold).
- Screenshot tests (Paparazzi or Roborazzi) for the 4 layouts × 4 levels.
- Family playtest protocol: 3 children aged 3–4, 5–6 and 7–8; observe without helping; note every "adult needed" moment.
