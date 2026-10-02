# ADR-002 — One Gradle module per build-map code home (+ two support modules)

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** AI under governance.md row 4 (ai+inform, checkpoint 1)  ·  **Serves:** D1 for all locked REQs; trace-check rule 3

## Context

D1 asks for vertical slices with one code home per `#Tag` subtree
(`build-map.md`) and a kernel for the TYPE rules. trace-check (rule 3) accepts
a covering test only if its path starts with the feature's code home. Android
keeps tests in a parallel tree (`<module>/src/test`, `<module>/src/androidTest`),
so a package directory inside one `app` module can never contain its tests.

## Decision

Each code home is a Gradle module at the project root: `kernel`, `content`
(pure Kotlin/JVM) and `play`, `browse`, `time`, `settings`, `devtools`, `app`
(Android). Two **support modules** carry no `#Tag`: `contracts` holds exactly
the governed files, so the guard protects a whole module (architecture.md §4),
and `store` keeps persistence JVM-testable and makes O-03 ("only `store`
touches storage") a compile-time fact. A module is created by the first work
order that needs it, not ahead of time (D4). Module dependencies follow
guardrail G-06. Settings shared by the modules (bytecode 17, compile/min/target
SDK) are written once in the root build script as soon as a third module needs
them.

## Alternatives considered

| Alternative | Why not |
|---|---|
| One `app` module, packages as slices | fails trace-check rule 3 (tests live in `src/test`, outside the package dir) |
| One module with per-slice source roots (`play/main`, `play/test`, … all in one build file) | passes rule 3 with one build file, but nothing stops one slice compiling against another: G-06 would need an import-lint we do not have; the deciding factor is compile-time isolation |
| One module per leaf feature (37) | far more build files than the slices need (D2) |
| Layered modules (`ui`, `domain`, `data`) | horizontal cuts, against D1 |

## Consequences

- Easier: tests sit under their code home; Gradle enforces who may call whom
  (a slice cannot compile against another slice); parallel tasks rarely
  share files.
- Harder: ~10 small build files; JVM modules must pin bytecode 17 so Android
  can dex them.
- The DoD command becomes `gradlew assembleDebug test` (`test` runs every JVM
  and Android unit test; `testDebugUnitTest` alone skips the JVM modules).
