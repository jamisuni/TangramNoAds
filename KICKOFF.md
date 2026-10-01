# KICKOFF — TangramNoAds under SWDev (Claude Code terminal, Windows)

**State on 2026-10-01 evening:** requirements locked (v1.1), project adopted
under SWDev v0.15 (intake path C), governance profile **ai-mastered, in
force** (`governance.md` v0.2), stack decided by Jami: **Kotlin + Jetpack
Compose, native Android — conditional on it building on this computer.**
Nothing built yet. This file is the hand-over so the first terminal session
starts without reconstruction. Delete it once WO-001 is closed.

---

## 0. Before the session — toolchain on this computer (Jami, ~30 min once)

The first work order cannot enter implementation until a trivial compile +
test has **actually run here** (SWDev G3 toolchain proof). Have ready:

1. **Android Studio** (current stable) — installs the bundled JDK, the
   Android SDK, platform tools and an emulator. Accept the SDK licences
   once (`sdkmanager --licenses` from the SDK's `cmdline-tools\latest\bin`).
2. **One device**: a phone with USB debugging on, or an emulator image
   (a Pixel-class phone *and* a tablet profile — REQ-035/036 want both
   layouts).
3. **`JAVA_HOME`** pointing at the bundled JDK (or a JDK 17+), and
   `ANDROID_HOME` at the SDK, so `gradlew` works from a plain terminal.
4. Git stays yours: agents never commit (F3); you snapshot at WO closes.

The session's G3 proof is then: `gradlew assembleDebug` and
`gradlew testDebugUnitTest` on a hello-world Compose app it generates. If
that cannot run here, the profile says **stop and ask**, never switch
stacks silently.

## 1. Copy-paste — the first terminal session

Open the terminal in `C:\GitHub\AI\TangramNoAds`, with `C:\GitHub\AI\SWDev`
and `C:\GitHub\AI\SwReqCollector` readable, and paste:

```text
You are the SWDev build agent for TangramNoAds. Read AGENTS.md (all of it —
rules 1–13 and the "Build under SWDev" section), then governance.md (IN
FORCE, ai-mastered: rows marked `ai` you decide and log to decisions.md;
rows marked `human` you stop and ask; floors never yours), then STATUS.md,
build-map.md, design-inputs.md (read its Contract-delta), and the SWDev
files AGENTS.md lists. The spec is Requirements/ (SwReqCollector format
v2, locked 2026-10-01): never write into that folder.

Phase P1 — acceptance of the locked collection (intake path C):
  1. python C:\GitHub\AI\SWDev\framework\skills\trace-check\trace_check.py --project .
     Expect 51 REQs, 47 locked, RED only for uncovered acceptance and the
     missing baseline. If the swreq_tools path in .swdev/guard.json is
     wrong on this machine, fix it there.
  2. Dispatch the requirements-reviewer subagent, fresh context, read-only
     on Requirements/ (index, features.md, reqs/, req_types.md, views/).
     Output req_review_01.md in the project root; findings become DEF/CHG
     proposals for the capture side — never edit a REQ.
  3. Present me views/digest.md + views/trace.md + the review's Blockers,
     then ask exactly: "Accept collection v1.1 for TangramNoAds (G1)?"
  4. On my yes: record approver + date in progress.md and run
     python C:\GitHub\AI\SWDev\framework\skills\trace-check\trace_check.py --project . --freeze

Phase P2 — architecture (ai+inform under governance.md rows 4/6; the stack
is DECIDED: Kotlin + Jetpack Compose, native, port the prototype's logic,
never a WebView — ADR-001, decided by Jami 2026-10-01):
  5. Author architecture.md (directives ack; project guardrails from
     AGENTS.md rules 1, 6, 11, 12, 13; component map; Governed Interface
     Registry with tiers — proposal: IPuzzleLibrary over
     Tangrams/puzzle.schema.json = locked, the rest notify), fill the TBD
     code homes in build-map.md, register any new design input. Log every
     decision you take instead of asking to decisions.md. Surface the
     result as checkpoint 1 and CONTINUE (ai+inform) unless a floor is hit.
  6. Add architecture.md, Contracts/** and Tangrams/puzzle.schema.json to
     contract_paths.locked in .swdev/guard.json; re-freeze.

Phase P3 — first work order (ai under rows 7/8):
  7. WO-001 = the #Locking subtree (#AnchorLock, #GoHome, #LandingPreview,
     #CornerTeach: REQ-019 A1..A4, REQ-020 A1..A2, REQ-021 A1..A2, REQ-051
     A1..A2; TYPE-004; realizing REQ-002) + the kernel TYPEs it needs.
     G3 toolchain proof FIRST: generate the Compose hello-world, run
     gradlew assembleDebug and gradlew testDebugUnitTest on THIS computer.
     If it fails, stop and ask me (row 5 condition). Then the fountain:
     stations as subagents, acceptance tests authored from the REQs (never
     from the prototype), held-out slice, trace-check green at close.
  8. Checkpoint at WO close (row 17): REQs satisfied with acceptance
     results, decisions.md rows, contract deltas, a debug APK I can
     install. Prompt me for a git snapshot. Then continue to the next WO
     in build-map.md order without waiting, unless I say stop.

Capture-side hygiene: any DEF/CHG you file goes to Requirements/ by its
CLAUDE.md rules (register the source, validate.py 0 errors, views.py).

End every turn with exactly one of:
  All ready, whats next Jami?   /   Need help, answer above question JAMI
```

## 2. What is already decided (do not re-ask)

| Topic | Decision | Where recorded |
|---|---|---|
| Export tier | whole locked collection, every level, is Contract | SWDev principle 2; `req-format.md` §6 |
| Governance | ai-mastered, in force; floors F1–F4; checkpoint per WO close | `governance.md` v0.2 |
| Stack | Kotlin + Jetpack Compose, native; port the prototype's logic | `governance.md` §2 row 5 → ADR-001 at P2 |
| Prototype lane | allowed for ai-mastered projects (moot here, all locked) | `gates-and-autonomy.md` §8 |
| Fresh-eyes review | run once before P2 | `governance.md` row 3 |
| WO order | #Locking → rest of #Solving → #Browsing → #Layout/#Settings → #PlayTime → #Content → #Release/#DevTools | `governance.md` row 8, `build-map.md` |
| First governed interface | `IPuzzleLibrary` over `puzzle.schema.json`, locked tier (proposal) | `build-map.md`, `design-inputs.md` DI-5 |

## 3. Known loose ends

- `tools/tests/test_prototype.py` are the *prototype's* tests — not
  scanned (`test_globs`), never acceptance tests.
- `priority: none` on every REQ — WO order comes from the dependency graph
  (row 8); set priorities on the capture side only if you want a different
  order.
- Phase D (`Requirements/changes/CHG-NNN.md`) is not built on the capture
  side yet; until it is, a CHG proposal is a note in the index §6 +
  `proposals.md`, and SWDev's guard hook is the only post-lock protection.
- `docs/framework-map.json` / atlas in SWDev are stale as of v0.15
  (`/sw-factory-human-brief` regenerates them).
- SwReqCollector's `pytest` suite was not run today (no PyPI from either
  machine); run `python -m pytest tests` there once — the day's changes
  were prose-only.
