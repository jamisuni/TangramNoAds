# ADR-001 — Build natively in Kotlin + Jetpack Compose, porting the prototype's logic

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** Jami (stack decision 2026-10-01, governance.md row 5), recorded by the AI  ·  **Serves:** REQ-006, REQ-010, REQ-002, REQ-035, REQ-036

## Context

REQ-006 asks for Android phones and tablets; REQ-010 forbids network use and
every permission; REQ-001/048 forbid ads, purchases and unapproved third-party
code. The only working reference is a single-file HTML prototype (design input
DI-1). The owner chose the stack on 2026-10-01: "Kotlin + Jetpack Compose
(native) is ok if can build it my computer" (governance.md §2, row 5). The G3
toolchain dry run on that computer passed on 2026-10-02 (`gradlew assembleDebug
testDebugUnitTest`, decisions.md).

## Decision

The game is a native Android app in Kotlin with Jetpack Compose. The
prototype's logic (lock engine, layout algorithm, puzzle loader, timers) is
**ported** from `tools/prototype_template.html` and `tools/tangram_geom.py`,
never wrapped: no WebView, no embedded HTML.

## Alternatives considered

| Alternative | Why not |
|---|---|
| WebView around the prototype | excluded by the owner (governance.md §2); a WebView makes "no network" and touch behaviour harder to guarantee |
| Flutter / other cross-platform | not the owner's choice; Android is the only platform in scope (REQ-006) |
| Android Views (XML) | Compose is the current Android UI toolkit and draws the board, tray and art with one canvas model |

## Consequences

- Easier: one language for engine and UI; JVM unit tests for the engine.
- Harder: the prototype's geometry and timing must be re-implemented and
  re-verified against the REQs (never against the prototype).
- Dependencies: AndroidX + Compose only, pinned in `gradle/libs.versions.toml`
  (decisions.md, D5); none adds network, ads, analytics or a permission (G-01).
