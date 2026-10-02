# ADR-005 — The saved state is one versioned document in app-private storage

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** AI under governance.md rows 4 and 11 (ai+inform, checkpoint 1)  ·  **Serves:** REQ-003, REQ-010, REQ-025, REQ-029, REQ-030, REQ-031, REQ-033, REQ-034

## Context

Per-puzzle progress, times and settings must survive closing, killing and
updating the app (REQ-025, decisions.md F4), stay on the device (REQ-010, no
backup: decisions.md F7) and be erasable in one step (REQ-034). The whole
state is small: a few hundred bytes per puzzle, 20–40 puzzles. Changing its
shape later is a migration (hard-stop 2, governance row 11).

## Decision

`store` keeps the state in memory and writes it as one JSON document with a
`version` field to the app's private files directory, replacing the file
atomically (write a temporary file, then rename). Every save writes before it
returns (a few kilobytes, a few milliseconds), so a save that returned
survives any later kill; if profiling ever shows jank, the fallback is an
ordered background writer behind the same interface. Saves follow
architecture.md O-08 (read-modify-write per event; Restart/Retry keep the best
time). **Cadence:** board state on every lock, return, turn, mirror, Restart,
Retry and Reset; time on every puzzle event, every 10 s while counting and when
the app goes to the background — so a system kill (always after the app went
to the background) loses nothing, and only a crash can lose up to 10 s of
counted time. Reading tolerates damage per puzzle: an unreadable puzzle entry
becomes New. An unreadable document, or one whose `version` is newer than the
app knows (e.g. an older debug APK installed over a newer one), is **moved
aside** (`progress.json` → `progress-<version|bad>-<timestamp>.json`) and never
overwritten; the game then starts fresh. Neither case crashes. Stored names
are explicit, stable serial names, never Kotlin identifiers, so renaming a
kernel enum is not a format change. Every shape change bumps `version` and
ships a migration plus a migration test against a frozen v1 fixture (G-09).
The app opts out of Android backup and device transfer (G-01).

## Alternatives considered

| Alternative | Why not |
|---|---|
| Jetpack DataStore | an extra dependency for a document this small (D5) |
| Room / SQLite | a schema and migrations machinery the data does not need (D2) |
| SharedPreferences | no atomic multi-key update, awkward for per-piece data |

## Consequences

- Easier: one migration point; reset is one write; JVM-testable with a temp
  dir; "survives a kill" is deterministic.
- Harder: disk writes on the caller's thread; acceptable at this size, watched
  at the playtest.
