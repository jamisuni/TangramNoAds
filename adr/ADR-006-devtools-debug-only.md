# ADR-006 — DevTools live in a debug-only module

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** AI under governance.md row 4 (ai+inform, checkpoint 1)  ·  **Serves:** REQ-046, AGENTS.md rule 11

## Context

REQ-046 wants a passcode-protected solution reveal in prototype and test
builds and **none** in a release build (A4). decisions.md F18 defines the
build types: debug = test build; anything uploaded to a store track = release.

## Decision

All DevTools code (the DEV button, the passcode, the solution overlay, and
the code that reads stored solutions to place pieces) lives in the `devtools`
module, which `app` links with `debugImplementation` only. `app` has a tiny
`src/debug` / `src/release` pair: the debug one shows the DEV button from
`devtools`, the release one renders nothing. Release code keeps only neutral
hooks (architecture.md G-04): `play` can "place these piece poses and finish
the puzzle", and its solve event carries `byAid` so that `time` sets no best
time for an aid solve (REQ-030, REQ-046.A3). Verifier V-04 builds the release
APK, unzips it and scans dex and resources for the devtools package and the
passcode literal.

## Alternatives considered

| Alternative | Why not |
|---|---|
| A runtime `if (BuildConfig.DEBUG)` switch | the code and passcode still ship in the release APK, against REQ-046.A4 |
| A product flavour | more build variants than one debug-only module needs (D2) |

## Consequences

- Easier: the release build cannot contain the aid, by construction.
- Harder: DevTools reaches the game only through the hooks `app` hands it
  (stored solution via IPuzzleLibrary + the kernel's solution poses, solve-now
  via the neutral hook).
