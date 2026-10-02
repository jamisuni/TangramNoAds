# ADR-003 — Exact ℚ(√2) geometry in the kernel

**Status:** Accepted
**Date:** 2026-10-02  ·  **Approved by:** AI under governance.md row 4 (ai+inform, checkpoint 1)  ·  **Serves:** TYPE-001, TYPE-003, TYPE-004, REQ-019, REQ-021, REQ-022, REQ-038

## Context

Pieces turn in 45° steps (TYPE-003), so coordinates are of the form a + b·√2.
TYPE-004 requires that "a locked piece sits exactly on its anchor and later
locks against it are exact too"; AGENTS.md rule 6 says geometry is exact, with
`tools/tangram_geom.py` as the reference. The prototype used plain floats.

## Decision

The kernel represents puzzle coordinates, piece positions and anchors as exact
numbers a + b·√2 with rational a, b (`Rational` over `Long`, normalized;
`Q2`; `ExactPoint`). A locked position is computed exactly as anchor − corner
offset. Doubles are used only to draw, to read the finger, to compare a
distance with the lock distance R, and for the inside/overlap decisions within
TYPE-004's 1e-6 tolerance. The kernel's outline corners and piece shapes are
checked against `tools/tangram_geom.py` on every puzzle file.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Doubles everywhere (as the prototype) | positions drift after repeated locks; equality of anchors needs ad-hoc epsilons; contradicts TYPE-004's "exact" rule |
| BigInteger rationals | puzzle values are tiny (halves, thirds, quarters); `Long` with gcd normalisation is enough and simpler |

## Consequences

- Easier: anchor equality is plain equality; saved positions are exact.
- Harder: a small number type with tests; overflow is impossible at puzzle
  scale but asserted.
