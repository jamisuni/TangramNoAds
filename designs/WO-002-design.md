# Design — WO-002 · #Content: the puzzle library behind IPuzzleLibrary

**Author:** Design Author  ·  **Date:** 2026-10-02  ·  **Status:** Draft (revised after design review, 2026-10-02)

## Scope

- **Implements:** REQ-007 (A1–A2), REQ-038 (A1–A2), REQ-039 (A1, library level), REQ-040 (A1 library level, A2), REQ-041 (A1 library level, A2), REQ-045 (A1–A2, library / engine level)
- **Governed interfaces:** `IPuzzleLibrary` (implements; `contracts/` untouched) · consumes `Tangrams/puzzle.schema.json` (locked)
- **Hard-stop domains touched:** none. The contract types are enough as they stand: nothing here needs `IPuzzleLibrary` or `Puzzle.kt` to change in shape or meaning. (One reading note, not a change: "never empty" holds because the build tests guarantee it, see section 3.)
- **Guardrails in play:** G-02/ADR-004 (kotlinx-serialization-json, main scope of `content`), G-03 (exact numbers), G-06 (deps kernel + contracts), G-08 (`Tangrams/` is the only source), G-10 (no crash on stored data), O-02 (only `content` reads puzzle files).
- **Carried out (not designed here):**
  - To WO-003: REQ-039 A1 and REQ-045 A2 on screen; the G-10 boundary for path data `d` (the parser keeps `d` verbatim and nothing validates the M/L/Q/C/Z grammar, so path drawing must not crash on a bad `d`); **the Android packaging check**: an instrumented assertion that `PuzzleLibrary.packaged()` has `rejected` empty and `puzzles.size` equal to the `tangrams/index.txt` line count, plus a release-APK check that its `tangrams/*.json` equal `Tangrams/*.json` (or an extension of V-04's APK scan in WO-005).
  - To WO-004: REQ-045 A1, REQ-040 A1 and REQ-041 A1 on screen (a fresh install opens on `puzzles.first()`: a mini puzzle of rating 1, followed by the warm-ups). The library-level parts are in this WO.
  - To WO-009: REQ-039 A2 and REQ-042 (decisions F17).

## Chosen design

### 1. From `Tangrams/*.json` to the app (single source, G-08)

`content/build.gradle.kts` registers one task:

- `packagePuzzles`, registered with `tasks.register<Sync>("packagePuzzles")` (not the deprecated `by tasks.registering`): `from(rootProject.file("Tangrams")) { include("*.json"); exclude("*.schema.json") }` into `build/generated/puzzles/tangrams/`. Its `doLast` writes `tangrams/index.txt` (names from the destination listing, sorted, one per line), with the destination captured as a `File` outside the action so it stays configuration-cache safe.
- `sourceSets.main.resources.srcDir(build/generated/puzzles)` and `tasks.processResources { dependsOn(packagePuzzles) }`.

So the schema, README and `previews/` never match, and nothing is hand-copied (G-08). The files travel as Java resources of the `content` jar; the app gets them by depending on `content` later.

Why an index file: a classpath directory cannot be listed portably (jar, Android APK). The index is generated from the same copy, so it can never disagree with it. `PackagedPuzzles.files()` reads `tangrams/index.txt`, then each `tangrams/<name>` (name including `.json`) as UTF-8 text through `PackagedPuzzles::class.java.classLoader`.

How JVM tests read the same files:

1. **Packaged path** (what ships): `PackagedPuzzles.files()` from the test classpath (main resources are on it). This is what the library tests use.
2. **Source path** (the check against the source): `System.getProperty("tangram.root")`, set in `tasks.test` exactly as `kernel/build.gradle.kts` does, with `inputs.dir(Tangrams)` and `inputs.files(tools/golden/geometry.json)` so `org.gradle.caching=true` cannot replay a pass after a puzzle or the golden changed. One test asserts the packaged file set and bytes equal `Tangrams/*.json` (non-schema), so a stale or hand-edited copy fails (G-08).

### 2. The parser (`tangram-puzzle/1` to contract types)

`internal object PuzzleParser { fun parse(file: PuzzleFile): ParseResult }` where `PuzzleFile(name: String, text: String)` (`name` is the file name **including `.json`**, e.g. `animals-cat.json`; the id must equal the name without `.json`) and `sealed interface ParseResult { Parsed(puzzle: Puzzle); Rejected(fileName: String, reason: String) }`. It walks a `JsonElement` tree from `Json.parseToJsonElement` (no `@Serializable` classes, no compiler plugin: exact numbers need the token text, and a tree gives it as `JsonPrimitive.content`; see the alternatives). All types live in the one package `io.github.jamisuni.tangram.content` (no sub-packages). `PuzzleFile`, `ParseResult` and `PuzzleParser` are `internal`. The parser returns a value, never throws: the whole body sits inside one `try` that turns any `RuntimeException` (malformed JSON, kernel `require`/`ArithmeticException` on absurd numbers, the `Silhouette` check below) into `Rejected`. That is the single G-10 boundary for stored data.

**Exact numbers (G-03, ADR-004).** Never through a double:

- `ExactNumbers.rational(primitive)`: the primitive must be a JSON number (not a quoted string, not null). kotlinx also lets unquoted `NaN`, `Infinity`, `+1`, `0x10` through as literals; `BigDecimal` rejects them, so they become `Rejected`. `BigDecimal(token)`: when `scale >= 0` the value is `unscaledValue / 10^scale`; when `scale < 0` (exponent tokens such as `2E+2`, scale -2) it is `unscaledValue * 10^(-scale)` over 1. Numerator and denominator are taken with `longValueExact()`; an `ArithmeticException` (does not fit `Long`) means `Rejected`. `Rational.of(n, d)` normalises. So `"1.5"` is 3/2, `"0.67"` is 67/100, `2` is 2, `1e-1` is 1/10, `2E+2` is 200. This equals Python `Fraction(str(v))` for tokens of up to about 15 significant digits (Python goes through a float); for all 13 files the two agree, and the test "parsed polygons equal golden polygons" is the real guard.
- A **coordinate** is a number (`a + 0·√2`) or a two-number array `[a, b]` = `a + b·√2` (`[4, -1]` is 4 − √2; each element may be a decimal token such as `-2.5`). A **point** is an array of exactly two coordinates. So `[4, -1]` as a coordinate is 4 − √2, and the same text as a point is (4, −1); the nesting level decides, as in `Q2.parse` / the validator.
- **Thirds** cannot arrive from a file: a JSON token is a decimal, so `0.33` stays 33/100, as in Python. Thirds appear only in computed values (poses, locks), which the kernel does exactly.
- Art numbers are plain `Double` from the token (`PicturePoint`, `Rect`, ...; contract: "pictures use plain numbers"). A picture number must be a non-string primitive whose `toDouble()` is finite (NaN and Infinity are rejected). The parser also enforces the schema ranges `width > 0` and `opacity` in 0..1 (a violation is `Rejected`).

**Fields:**

| File | Contract | Rule |
|---|---|---|
| `format` | - | must be the string `tangram-puzzle/1` |
| `id` | `PuzzleId` | present, kebab-case (schema pattern), **equal to the file stem** (so ids cannot collide) |
| `title` | `PuzzleTitle(en, fi)` | both present, non-blank strings (V8, schema `minLength 1`); other languages ignored |
| `category` | `PuzzleCategory` | lower-case name matched against the enum entries with `Locale.ROOT` |
| `difficulty` | `rating` | a non-string integer token 1..5 (`2.0` and `"2"` rejected, as the validator's `isinstance int`) |
| `kind` | `PuzzleKind` | default `full`; `mini`/`warmup`/`full` else rejected |
| `solution` | `List<SolutionPiece>` | file order kept; piece ids from `PieceId` (exact case), each once; count 1..6 for `mini`, exactly 7 otherwise (V2, V9) |
| `art` | `Picture(Rgb, shapes)` | required; `base` and every `fill`/`stroke` match `#[0-9A-Fa-f]{6}` and become `Rgb(0xRRGGBB)`; `shapes` may be absent (= empty, as the validator allows) |
| `provenance.reviewedByHuman` | `reviewedByHuman` | **true only for the JSON literal `true`**; missing `provenance`, missing flag, a string `"true"`, anything else is `false` (F16, fail closed). Never defaulted to true; agents never set it (G-08) |

**Solution entries.** Two forms, exactly the schema's `oneOf`:

- `polygon`: points parsed as above. The polygon is accepted only when `PieceGeometry.poseOf(piece, polygon)` finds a pose **and** the polygon equals `PieceGeometry.corners(piece, pose.turn, pose.mirrored, pose.at)` as a cyclic sequence, in either direction (review F2). `poseOf` compares vertex *sets*, so alone it accepts a congruent vertex set in the wrong order (a bowtie, or edges that are not 45-degree multiples); the cyclic check is what makes the accepted polygon a simple convex piece. After all solution polygons are read, the parser builds `Silhouette(polygons)` inside the same `try`, so a bad outline (or an overflow in its arithmetic) makes the file `Rejected` instead of crashing a consumer later.
- `rot` + `flip` + `at`: `rot` an integer 0..7 (checked before `Turn(...)`, which would throw), `flip` default false, `rot` default 0, `at` a point. Turned into a polygon with `PieceGeometry.corners(piece, Turn(rot), flip, at)` (the kernel transform, vertex order of the shape, same as `piece_polygon`). Both forms mixed in one entry: rejected (schema).

**Art shapes** by `type` (field names from the schema, mapped to the contract): `polygon` (`points`, at least 3), `rect` (`x y w h`, optional `rx` to `cornerRadius`), `circle` (`c`, `r`), `ellipse` (`c`, `rx`, `ry`), `line` (`from`, `to`, `stroke` required), `path` (`d` kept verbatim, not parsed; its M/L/Q/C/Z grammar is **not** validated here and is carried to WO-003, see Scope). Style: `fill`, `stroke`, `width` to `strokeWidth`, `opacity`; absent = `null`. Unknown type or a missing required field: file rejected. Unknown extra keys are ignored (the validator ignores them too); the structural schema stays the author-side contract.

**Runtime vs build-time.** The parser rejects only what it cannot represent or what would crash a consumer (V1, V2, V3 plus cyclic order, V8, V9 counts, V10 structure, a buildable `Silhouette`). The geometric policy rules (V4 overlap, V5 connectivity, V6 area, V12 warm-up measure) are build-time facts proven over the real files by the golden verdict (section 4); running them at launch would be a second geometry implementation for no REQ.

- **At run time** (`PuzzleLibrary`): a file that is `Rejected` is left out of the list and recorded in `rejected: List<Rejected>`; the game keeps running (G-10). No logging dependency exists in a JVM module; the app may log `rejected` later.
- **Packaging defects are programmer errors (review F1, G-10):** `PackagedPuzzles.files()` does `checkNotNull` on `tangrams/index.txt` and on every listed resource, with a message naming the packaging chain (`packagePuzzles`, `processResources`). `PuzzleLibrary` does `check(puzzles.isNotEmpty())` at construction, with a message pointing at the `content` build tests. These fail loudly at the source instead of moving a crash into `play`/`browse`. One unreadable or invalid *file* is still just left out.
- **At build/test time**: a test asserts `PuzzleLibrary.packaged().rejected` is empty and that every packaged file parses, so an unreadable file fails `gradlew test` (REQ-038.A1; contract text "the content unit test over every puzzle file").

### 3. The library, order, lookup

```kotlin
class PuzzleLibrary internal constructor(files: List<PuzzleFile>) : IPuzzleLibrary {
    internal val rejected: List<ParseResult.Rejected>
    override val puzzles: List<Puzzle>              // sorted once at construction
    override fun puzzle(id: PuzzleId): Puzzle?      // map lookup, null when unknown
    companion object { fun packaged(): PuzzleLibrary = PuzzleLibrary(PackagedPuzzles.files()) }   // the only public entry
}
```

Order (REQ-040): `compareBy<Puzzle>({ it.kind }, { it.rating }, { it.id.value })`. `PuzzleKind` is declared MINI, WARMUP, FULL, so enum order is the kind order (a test pins it, REQ-040.A2, so a reordered enum cannot pass silently); ids are ASCII kebab-case, so `String` order equals the Python sort. A full puzzle of rating 1 therefore stays after every warm-up whatever its id. Unreviewed puzzles are **not filtered**: `reviewedByHuman` is data for the release checklist (WO-009, F17), and today all 13 files carry `false`.

"Never empty" (`IPuzzleLibrary`) is guaranteed by the build tests (packaged count equals the `Tangrams/` count, `rejected` empty). If every file were rejected, or the package is missing, the `check`s above fail loudly at the source. Pinned by tests: `PuzzleLibrary(emptyList())` and an all-invalid file list throw `IllegalStateException`; `PackagedPuzzles.files(loader: ClassLoader = default)` over a loader without the index throws `IllegalStateException` (so the seam takes the loader).

### 4. Validator verdicts in the golden (REQ-038.A1, REQ-041.A2; G-03, F15)

`tools/export_geometry_golden.py` gains, per puzzle file, two keys (the existing keys and order are unchanged, so the kernel's `Golden.kt`, which reads by key, is unaffected):

```json
"difficulty": 1,
"validator": {
  "pass": true,
  "errors": [],
  "exposure": {}          // warmup only: {"LT1": 0.7071..., ...}
}
```

- The exporter calls `validate_puzzles.validate(puzzle)` (the G1-baseline validator, unmodified) on every file. `pass` is `errors == []`; `pass == true` already implies every stage ran (`validate` returns early on failure), so no per-rule list is written (it would be a constant, D3). V11 is "stuck pieces == []" (information only; it still must not report a stuck piece, as before). `difficulty` has exactly one consumer: a test that the parsed `rating` equals the golden `difficulty` (a cheap Kotlin-versus-Python cross-check).
- If any file fails, the exporter prints the errors, exits 1, and writes no golden (today's behaviour for load errors, extended). A broken puzzle therefore cannot produce a green golden; editing a puzzle without re-running the exporter fails the hash freshness check instead.
- `exposure` (warm-ups only) is each piece's share of outline on the silhouette edge, computed with the validator's own `shared_edge_length`/`perimeter` (imported, so no new geometry); the exporter cross-checks "validator says pass" against "every exposure at least 0.5 minus 1e-6" and fails on disagreement.

**REQ-041.A2 decision: golden verdict, not a Kotlin port.** Reasons: (a) V12 is a build-time authoring policy; no REQ needs it at run time, so a Kotlin port would be product code with no consumer (D3); (b) the F15 decision names the G1-baseline validator as the reference; (c) REQ-041's own rule says "validator rule V12 checks the measure", so a Kotlin port would be a second implementation of the named reference. (5 of the 28 warm-up pieces sit exactly at 0.5. The golden path uses the validator's own 1e-6 tolerance; an exact Q2 check would need none, since 1/2 passes `>=` exactly, so a port is possible but earns no REQ.) The Kotlin test re-checks the golden numbers (each exposure at least 0.5 minus 1e-6, 7 entries) for the files that the Kotlin parser reads as `WARMUP`, which also ties the Python `kind` to the Kotlin `kind`. If an exact check is wanted later, it is a `content` test-scope addition using `Q2` comparison; nothing here blocks it. Decision kept: golden verdict.

### 5. Build-order and engine-level checks (REQ-038.A2, REQ-045.A2)

A test helper `lockInOrder(puzzle, order)`: silhouette = `Silhouette(puzzle.solution.map { it.polygon })`; for each piece in `order`, pose = `PieceGeometry.poseOf(piece, polygon)` (O-01, never reimplemented), call `LockSearch.find(silhouette, placed, piece, pose.turn, pose.mirrored, origin = exact `at` as `Vec2`, LockSearch.BASE_DISTANCE)`; assert a lock whose `at` equals `pose.at`; then add the `PlacedPiece`. Returns the placed list.

- **REQ-038.A2 (F15 reading):** for every library puzzle, `order` = the golden's `buildOrder` (the Python-printed V11 line), and the result covers exactly the puzzle's pieces. This is "every stored solution locks piece by piece in build order" over the Kotlin-parsed polygons, which adds a check the kernel's own golden round trip does not make: the parser's output.
- **REQ-045.A2 (engine level, architecture section 5 rule 4):** for each `MINI` puzzle: `lockInOrder` with its golden `buildOrder` places all three pieces and `placed.map { it.piece }.toSet() == puzzle.solution.map { it.piece }.toSet()` (the tray's pieces, REQ-012/045), and the puzzle has a picture (`base` set, shape count equal to the file's). The kernel has no "solved" function yet (REQ-022 is WO-003), so "all pieces of the puzzle locked" is the engine reading; the on-screen solved picture is carried to WO-003.

### 6. Module wiring

- `settings.gradle.kts`: `include(":content")`.
- `content/build.gradle.kts` (pure Kotlin like `kernel`): `plugins { alias(libs.plugins.kotlin.jvm) }`; `dependencies { implementation(project(":kernel")); implementation(project(":contracts")); implementation(libs.kotlinx.serialization.json); testImplementation(libs.junit) }`; `packagePuzzles` and `tasks.test` as in section 1. Bytecode 17 and JVM settings come from the root build. The catalog already has `kotlinx-serialization-json` (1.11.0, added by WO-001 for test scope), so `gradle/libs.versions.toml` and the root build file do not change; **no** serialization compiler plugin is applied (tree parsing needs none; ADR-004's "with the plugin" is for `store`'s data classes).
- Main scope depends on `kernel` and `contracts` only plus the allowlisted library (G-06, V-06's `content` row already allows exactly this).
- **`app` does not wire `content` in this WO.** No screen consumes it (D4); WO-003 adds `implementation(project(":content"))` and the first real use. The Android-packaging risk this postpones is listed below and handed to WO-003.

**Abstractions introduced** *(each names its REQ, D3)*

| Abstraction / seam | Kind | Required by |
|---|---|---|
| module `content` | module | REQ-007, 038-041, 045 (component map section 3, O-02) |
| `PuzzleLibrary` | class implementing `IPuzzleLibrary` | REQ-007/040 (list + order), the contract |
| `PuzzleParser`, `PuzzleFile`, `ParseResult` | internal object / value types | REQ-038/039 (exact solution, picture, flag), G-10 (value result, left out not crash) |
| `ExactNumbers` | internal helper (token to `Rational`/`Q2`/`ExactPoint`) | G-03, ADR-004 |
| `PackagedPuzzles` | internal object (resource reader) | REQ-007, G-08 |
| `packagePuzzles` Gradle task | build logic | G-08 (single source) |
| golden keys `validator{pass,errors,exposure}`, `difficulty` | data in the existing golden | REQ-038.A1, REQ-041.A2 (F15); `difficulty`: parse cross-check (REQ-040) |

No interface other than `IPuzzleLibrary` is introduced; `PuzzleLibrary.rejected` is a property of the class, not a seam.

## Test seams

Public: `PuzzleLibrary.packaged()`, `.puzzles`, `.puzzle(id)`. Internal (visible to `content/src/test`, same module and package `io.github.jamisuni.tangram.content`): `PuzzleLibrary(files)`, `.rejected`, `PuzzleFile`, `ParseResult`, `PuzzleParser.parse`, `PackagedPuzzles.files(loader)`. Shared test helper `GoldenFile` (scaffolding, no acceptance ID): reads `tools/golden/geometry.json` for `sha256`, `kind`, `difficulty`, `validator.pass/errors/exposure`, `buildOrder`, `solution[].polygon` and `solution[].pose` using the same string-number format as the kernel's `Golden.kt` (R `"n"`/`"n/d"`, Q `[R,R]`, P `[Q,Q]`), and the SHA-256 freshness comparison against `Tangrams/` bytes (CRLF normalised). It is a small duplicate of the kernel test helper on purpose: sharing means `java-test-fixtures` in a closed module; promote it when a third module needs it.

| Acceptance | Public function a test calls | What it asserts (without reading the implementation) |
|---|---|---|
| **REQ-007 A1** | `PuzzleLibrary.packaged().puzzles`; `GoldenFile.verdicts()` | for every shipped puzzle: golden `validator.pass` (REQ-038), a picture with `base` set and at least one shape (REQ-039), `rating in 1..5` and equal to the golden `difficulty` (REQ-040); `puzzles.size` equals the number of `Tangrams/*.json` (no file silently left out) |
| **REQ-007 A2** | `PuzzleLibrary.packaged().puzzles` | the list starts with at least one `MINI`, then at least four `WARMUP`, with no `MINI`/`WARMUP` after the first `FULL`; same check on a synthetic `PuzzleLibrary(files)` that shuffles the input order |
| **REQ-038 A1** | `PackagedPuzzles.files()`, `GoldenFile.verdicts()`, `GoldenFile.freshnessProblems(...)` | golden is fresh against `Tangrams/` (hash per file, file sets equal); every file has `validator.pass == true` and `errors` empty (the golden has no `rules` key — removed in §4, design review N7); `PuzzleLibrary.packaged().rejected` is empty. A negative control feeds the assertion function a golden entry with `pass=false` and expects a failure |
| **REQ-038 A2** | test helper `lockInOrder(puzzle, golden.buildOrder)` over each `puzzles` entry | every piece of every puzzle locks, in the printed build order, at exactly its stored `at` (kernel `LockSearch.find` + `PieceGeometry.poseOf`); the placed pieces equal the puzzle's piece set; solution piece count is 7 unless `MINI` (1..6) |
| **REQ-039 A1** (library level) | `puzzle.picture`, `puzzle.solution` | every puzzle has a picture whose `base` is set and whose shape count equals the file's `art.shapes` size (counted independently from the raw JSON tree), so nothing is dropped; every polygon list equals the golden polygons (the silhouette that clips it is exact). The clip itself is an on-screen assertion in WO-003. *Honest limit:* the library carries the picture and its clip polygons; it cannot assert pixels |
| **REQ-040 A1** (library level) | `PuzzleLibrary.packaged().puzzles.first()` | `rating == 1`; "on a fresh install" (the app opens on it) is WO-004 |
| **REQ-040 A2** | `PuzzleLibrary(files)` over synthetic files: a valid `full` puzzle of rating 1 with file/id `animals-ant` (before `shapes-warmup`), built by copying a real file's text and changing id/kind/difficulty, plus the real files | in `puzzles`, the synthetic puzzle is after every `WARMUP` and after every `MINI`; within one kind and rating, ids ascend; kind beats rating (a rating-1 `FULL` follows a rating-4 `WARMUP` if one is synthesised) |
| **REQ-041 A1** (library level) | `PuzzleLibrary.packaged().puzzles` | the first four puzzles after the leading `MINI` run are `WARMUP`, each of rating 1; "on a fresh install" is WO-004 |
| **REQ-041 A2** | `PuzzleLibrary.packaged().puzzles` filtered `kind == WARMUP`; `GoldenFile.verdicts()` | at least four warm-ups; each has golden `validator.pass` and `exposure` with 7 entries, every value at least `0.5 - 1e-6` (the validator's tolerance); the golden numbers are tied to the shipped geometry by the polygon-equality test below (parsed polygons equal golden polygons), which this row relies on |
| **REQ-045 A1** (library level) | `PuzzleLibrary.packaged().puzzles.first()` | `kind == MINI` and `solution.size == 3` (the tray's pieces, REQ-012); "fresh install opens on it" is WO-004 |
| **REQ-045 A2** (engine level) | `lockInOrder` on every `MINI` puzzle | all three pieces lock via the kernel at their stored positions, `placed` piece set equals `solution` piece set, and `picture` is present; the on-screen solved picture is WO-003 |

Further tests for the parser (carry the REQ they protect, no new IDs): token exactness (`1.5` is 3/2; `[4, -1]` as a coordinate is `Q2(4, -1)`; `-2.5`; a 19-digit token is `Rejected`), the `rot`/`flip`/`at` form giving the same polygons as the golden polygons, compared as vertex sets (`corners` starts at vertex 0, file polygons start anywhere; write each golden pose as `rot`/`flip`/`at` with decimal tokens; no real file uses this form yet), an exponent token `2E+2`, `kind` default, `difficulty` to `rating`, missing `provenance` and `"reviewedByHuman": "true"` both give `false`, unknown piece, duplicate piece, wrong piece count for the kind, non-congruent polygon, a congruent polygon with two vertices swapped (e.g. animals-cat's PG `[[4,6],[5,3],[4,4],[5,5]]`; `poseOf` alone accepts it), NaN/Infinity or `width <= 0` / `opacity > 1` in art, bad colour, unknown art type, blank title, id differing from the file name, malformed JSON: each is `Rejected` and the library keeps the other files (G-10); the `check`/`checkNotNull` cases of section 3 throw. Parsed polygons equal the golden polygons for all 13 files (G-03, Kotlin versus Python). Python side: `tools/tests/test_golden_verdict.py` (stdlib `unittest`) shows the exporter's verdict function fails on a broken puzzle (an overlapping solution) and passes the 13 files.

## Alternatives considered

| Alternative | Why not chosen |
|---|---|
| **Packaging.** Android `assets/` (copy `Tangrams/` into the app) | `content` is a pure JVM module (ADR-002); `AssetManager` needs a `Context`, and the JVM tests could not read the same path the app reads. Java resources work in both |
| **Packaging.** Generate a Kotlin source holding all puzzle texts | Works with no index and no classpath lookup, but the files stop being plain files in the artifact and the generator is more code than a `Sync` plus an index |
| **Packaging.** Skip the index; list the resource directory at run time | Not portable (jar, APK); fails silently on device |
| **Parser.** `@Serializable` data classes + compiler plugin | Numbers would arrive as `Double` (or need custom serializers for exact tokens, the `[a,b]`/number union, and the `polygon` vs `rot` forms). A custom serializer for each union costs more than a 150-line tree walk, and the tree gives "left out, never throws" in one place. Costs: no schema-driven strictness for free |
| **Parser.** Hand-written JSON tokenizer | ADR-004 rejected it (more code to test than the dependency costs) |
| **Parse at build time** (generate Kotlin constants) | Hides parse errors from the unit-test view of the real files, and G-10's "left out at run time" wording assumes run-time reading; 13 files of about 2 KB parse in milliseconds |
| **V12.** Kotlin port in `content` | See section 4: build policy with no run-time consumer, near-boundary values, a second implementation to keep equal |
| **V4/V5/V6 (or all validator rules) ported to Kotlin** | The golden verdict proves them over the real files; a port is product code with no run-time need (D3) |
| **Exporter.** Write the golden even when a file fails, with `pass=false` | The kernel's `Golden.kt` expects solutions for every file, and a half-valid golden is a trap; failing the exporter plus hash freshness already makes the build red |
| **Test helper.** Share the kernel's `Golden.kt` through `java-test-fixtures` | Edits a closed module's build file and files for a one-off; revisit when a third consumer exists |
| **Wiring.** Add `content` to `app` now | No consumer; D4. The one thing it would prove early (resources reach the APK) is handed to WO-003, where the first read happens |

## Risks & edge notes

- **Highest risk (flag for the Design Reviewer): the resource packaging chain** (`Sync` task, generated `index.txt`, `processResources` ordering, test classpath, and later the Android APK). It fails *silently* as an empty or short library, and the Android half cannot be proven in this WO's JVM tests. Mitigations in the design: the count test (packaged files equal `Tangrams/*.json`, byte for byte), `inputs.dir(Tangrams)` for the test task so the build cache cannot replay a pass, and the WO-003 carry named in Scope (on-device `rejected` empty and count equals the index; release APK `tangrams/` equals `Tangrams/`). A missing index or listed resource now throws at `PackagedPuzzles.files()`, so the failure is loud, not a short list. Also verify `Sync`'s `doLast` index survives up-to-date checks (run the build twice, then edit a puzzle, then once more).
- **Second risk: `reviewedByHuman` failing open.** `kotlinx`'s `JsonPrimitive.booleanOrNull` accepts the quoted string `"true"`; the parser must test "non-string literal `true`" itself (F16, G-08). A test pins it.
- **Number edges:** `Rational.of` and kernel arithmetic throw on overflow (`Long`): the `longValueExact()` guard in `ExactNumbers` and the single `RuntimeException` boundary keep a hostile or corrupt file a `Rejected`, not a crash. Do not add `catch` blocks anywhere else.
- **`Silhouette` throws on non-45-degree edges and `poseOf` compares vertex sets:** the run-time guarantee comes from the parser's cyclic-order check plus building the `Silhouette` inside its `try`; the guarantee for shipped data comes from the build tests over every packaged file (`lockInOrder` builds a `Silhouette`). Do not drop either as "the validator already checked": the validator's V3 is also set-based and catches a swapped polygon only by accident through V5/V6 (G1 baseline, F15, left unchanged; a tools note for the owner).
- **Golden stays one file.** Adding keys must not change existing key order or values (the kernel tests and `Golden.freshnessProblems` depend on them); `tools/golden/geometry.json` is regenerated with LF, `sort_keys`, indent 1 as today. `shapes-warmup-4` pin and the sampling oracle stay.
- **AGENTS.md rule 10 re-triggers** (the exporter, in `tools/`, changes): Test & Verify runs `validate_puzzles.py Tangrams`, `build_prototype.py`, `tools/tests/test_prototype.py` (Playwright venv in the session scratchpad, proven in WO-001), the new `test_golden_verdict.py`, and SwReqCollector `validate.py` on `Requirements/`. `Tangrams/` itself is not edited (content work is the owner's loop).
- **No `rot` form in real files:** the form is only covered by the golden-pose round trip test; keep it.
- **Not designed:** drawing of picture/silhouette (WO-003), progress (WO-004), any language fallback beyond what `PuzzleTitle.inLanguage` already does (contract).

## Assumptions to log

1. (DA-8) The runtime parser enforces representability and crash-safety only (V1, V2, V3 with cyclic order, V8, V9 counts, V10 structure, a buildable `Silhouette`); V4/V5/V6/V12 are build-time facts proven by the golden verdict. *Why:* no REQ needs them at launch; one geometry reference (Python) for policy. *Reversible:* a Kotlin check can be added later without contract change.
2. (DA-9) REQ-041.A2 is checked through the golden verdict plus per-piece exposure numbers, not a Kotlin port (section 4; 5 of 28 pieces sit at exactly 0.5).
3. (DA-10) "Never empty" (`IPuzzleLibrary`) is guaranteed by the build tests; an empty or partly missing package is a programmer error that fails loudly at its source (`check`/`checkNotNull`), while a single unreadable file is left out.
4. (DA-11) REQ-038.A2 uses the Python-printed build order (the golden's `buildOrder`) and the kernel lock search over the Kotlin-parsed polygons; REQ-045.A2 "solved" is "all pieces of the puzzle locked" because the kernel has no solved check until WO-003 (REQ-022).
5. (DA-12) REQ-039.A1 at library level means "the picture and its clip polygons are carried intact"; the no-paint-outside assertion is on screen (WO-003).
6. (DA-13) No serialization compiler plugin in `content`; kotlinx-serialization-json is used as a tree parser (still the ADR-004 dependency; no catalog change).
7. (DA-14) The spec says a `path` shape may have `stroke` and/or `fill`, while validator V10 demands `stroke` on `path`. The parser follows the schema (`path` requires only `d`); the validator stays stricter at build time. No shipped file is affected (all 13 pass V10); flag for the owner as a harmless spec/validator wording gap.
8. The golden's new `difficulty` and `validator{pass,errors,exposure}` keys are additive; the kernel's `Golden.kt` ignores unknown keys.
9. `app` does not depend on `content` in WO-002.

## Suggested cut for the Planner

**Step 1 (one task, serial, build files only):** `settings.gradle.kts` (`include(":content")`), `content/build.gradle.kts` (plugin, deps, `packagePuzzles`, `tasks.test` props), plus an empty source folder so `:content:test` runs. Check: `gradlew :content:processResources` produces `tangrams/index.txt` listing 13 files and no schema; V-06 verifier green. Nothing else edits a build file in this WO.

**Step 2 (parallel, disjoint files, no compile dependency between the tasks):**
- 2a Python: `tools/export_geometry_golden.py` (verdict + `difficulty`), `tools/tests/test_golden_verdict.py`, regenerate `tools/golden/geometry.json`; run `gradlew :kernel:test` to prove the additive keys are harmless.
- 2b ONE Kotlin task for all of `content/src/main/kotlin/io/github/jamisuni/tangram/content/`: `ExactNumbers.kt`, `PuzzleParser.kt` (with `PuzzleFile`, `ParseResult`), `PuzzleLibrary.kt`, `PackagedPuzzles.kt` (parser and library merged, about 300 lines, because the library calls the parser's types).

**Parallel with step 2 (Acceptance Test Author, from the Test seams table with its pinned package, visibility and `name` convention, and the REQs, not the code):** `content/src/test` tests, `GoldenFile` helper, `lockInOrder` helper; held-out slice kept apart.

**Step 3:** Code Reviewer, then Test & Verify: `gradlew assembleDebug test` (all modules), the rule-10 chain, V-01/V-05/V-06, golden freshness, packaging run-twice check.

## Handoff — Design Author · WO-002
- **Scope:** REQ-007, REQ-038, REQ-039, REQ-040, REQ-041, REQ-045 · governed touched: `IPuzzleLibrary` (implements; unchanged)
- **Inputs read:** WO-002 · REQ-007/038/039/040/041/045 (+ REQ-012, REQ-022 for context) · architecture.md v1.0 · ADR-003, ADR-004 · build-map.md v1.1 · design-inputs.md v0.2 · decisions.md (F15, F16, F17, DA-6) · `IPuzzleLibrary.kt`, `Puzzle.kt` · kernel main sources · `Golden.kt`, `LockRoundTripTest.kt` · `Tangrams/*`, schema, README · `Spec/03-puzzle-format.md` · `validate_puzzles.py`, `tangram_geom.py`, `export_geometry_golden.py`, golden · build files
- **Result:** `C:\GitHub\AI\TangramNoAds\designs\WO-002-design.md`
- **Status:** forward (Design Reviewer); no escalation
- **Traceability delta:** REQ-007/038/039/040/041/045 to `content` (new module: parser, library, packaging), golden keys `validator`/`difficulty`, Python `test_golden_verdict.py`, `content/src/test` (to be written by the Acceptance Test Author per the Test seams table)
- **Notes for next station:** revised after review (F1-F4, N5-N11). Highest risk: the resource packaging chain; the Android half is now in Scope as a WO-003 carry (orchestrator: copy it into WO-003 and build-map; also REQ-040.A1/041.A1 on screen into WO-004). Spot-check DA-8, DA-9, DA-14.
