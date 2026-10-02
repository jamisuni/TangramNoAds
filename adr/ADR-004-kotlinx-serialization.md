# ADR-004 — kotlinx.serialization for puzzle files and the saved state

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** AI under governance.md rows 4 and 15 (ai+inform, checkpoint 1)  ·  **Serves:** REQ-007, REQ-038, REQ-025, REQ-010

## Context

Puzzles ship as `tangram-puzzle/1` JSON files (`Tangrams/puzzle.schema.json`,
locked) and the saved state must survive closing the app (REQ-025,
decisions.md F4). Both need a JSON reader that also works in JVM unit tests,
so the real `Tangrams/*.json` files can be tested without a device.

## Decision

Use `kotlinx-serialization-json` with the Kotlin serialization compiler plugin
(same version as Kotlin), in `content` and `store` only. Exact coordinates are
parsed from the JSON token text, never through a double.

## Alternatives considered

| Alternative | Why not |
|---|---|
| `org.json` (in the Android SDK) | stubbed out in JVM unit tests; stringly typed |
| Moshi / Gson | reflection or extra code generation, more dependencies |
| Hand-written parser | more code to test than the dependency costs |

## Consequences

- D5 record: a JetBrains Kotlin library with no network, no permissions and
  no runtime reflection; added to the allowlist (architecture.md §5).
- Number tokens such as `1.5` or `[4, -1]` become exact values (ADR-003).
