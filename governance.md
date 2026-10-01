# Governance profile — TangramNoAds

**Status:** in force  ·  **Preset:** ai-mastered  ·  **Version:** 0.2  ·  **Last updated:** 2026-10-01
**Set by:** Jami, 2026-10-01 20:12 (chat: "Kotlin + Jetpack Compose (native) is ok if can build it my computer. Anyway questions OK") — recorded by the agent  ·  **Owner checkpoint cadence:** every WO close
**Decisions sheet:** `decisions.md`

> Who owns each control point: `human` = stop and ask · `ai` = decide, act,
> log to `decisions.md` · `ai+inform` = as `ai`, surfaced at the next
> checkpoint. Floors F1–F4 are never `ai`. **In force since 2026-10-01.**
> Rules:
> `C:\GitHub\AI\SWDev\framework\methodology\gates-and-autonomy.md` §8.
> Why ai-mastered here: a free offline kids' game with no money, no network
> and no personal data (REQ-001/010), an ai-led collection the owner locked
> wholesale ("I agree with all your proposals"), and the owner's stated
> intent to learn how little human control early prototyping needs.

---

## 1. Control points

| # | Control point | Owner | Floor | Notes / conditions |
|---|---|---|---|---|
| 1 | G1 — requirements lock | human | F1 | done 2026-10-01 (SRC-014/015); the G1 *acceptance* for this project still needs Jami's artifact-named yes |
| 2 | Build from unsigned `ai-approved` items (prototype lane) | yes | — | decided for ai-mastered projects (Q3); moot here — everything is locked; future CHG items follow row 14 |
| 3 | G1 fresh-eyes requirements review | yes | — | run once, read-only, before P2 — the collection never had one (Q5) |
| 4 | G2 — architecture + guardrails sign-off | ai+inform | — | the AI authors `architecture.md` + `build-map.md`, surfaces them at checkpoint 1 |
| 5 | Technology / stack choice | human — **decided: Kotlin + Jetpack Compose (native)** | — | Jami, 2026-10-01; conditional on the G3 toolchain proof passing **on Jami's computer**; if it cannot, stop and ask (do not switch stacks silently). Record as ADR-001 in `architecture.md` |
| 6 | Governed interface tiers at G2 | ai | — | proposal: `IPuzzleLibrary` (puzzle.schema.json) locked; everything else notify |
| 7 | G3 — work-order plan approval | ai | — | |
| 8 | WO sequencing / priorities | ai | — | all `priority: none`; order by Depends-on graph: #Locking → #Solving rest → #Browsing → #Layout/#Settings → #PlayTime → #Content → #Release/#DevTools |
| 9 | G4 — merge review | ai (spot-check 1 in 5) | — | Jami reviews by *playing the build* (as he did the prototype), not by reading diffs |
| 10 | Hard-stop 1 — security / PII / money | human | F2 | structurally excluded by REQ-001/010 and AGENTS.md rule 1 — any network/permission/third-party code is a Contract breach, stop |
| 11 | Hard-stop 2 — schema / migration / breaking API | ai+inform | — | saved state is device-local (REQ-025 names the migration rule); a migration test is mandatory |
| 12 | Hard-stop 3 — ambiguous requirement | ai | — | decide, write the ASSUMPTION, file a capture-side DEF/CHG proposal; never on a row-10 matter |
| 13 | Hard-stop 4 — guardrail / directive conflict | ai+inform | — | project guardrails only (they derive from AGENTS.md rules 1, 6, 11–13); SWDev directives stay human |
| 14 | Hard-stop 5 — change to a locked REQ / TYPE / locked-tier I* | human | F1 | the AI files `Requirements/changes/CHG-NNN.md` proposals (SwReqCollector Phase D format, once it exists; until then a DEF/CHG note in the index §6 + `proposals.md`) |
| 15 | New external dependency | ai | — | D5 justification; **none that adds network, analytics, ads or permissions** (rule 1) |
| 16 | Waivers | ai+inform | — | e.g. no device for a verification channel → waiver with exit condition "Jami's phone playtest" |
| 17 | Owner checkpoint | per WO close | — | summary: REQs satisfied (acceptance results), decisions.md rows since last checkpoint, contract deltas, a build to try |
| 18 | Version-control snapshot | human | F3 | the orchestrator prompts at each WO close |
| 19 | Editing this profile | human | F4 | |

## 2. Constraints the AI must stay inside when it owns a row

- **Platform:** Android phones and tablets (REQ-006, REQ-035/036); a web
  build is welcome as a side effect, never instead.
- **No network, no analytics, no ads, no permissions, no third-party SDK
  that phones home** (REQ-001, REQ-010, AGENTS.md rule 1) — this bounds the
  stack choice and every dependency.
- **Both languages always** (REQ-047, rule 13); **geometry exact** (rule 6,
  `tools/tangram_geom.py` is the reference); **the prototype is a reference,
  not a source** (design-inputs.md DI-1; REQs win).
- **Release build excludes DevTools** (REQ-046, rule 11).
- **Store obligations** (REQ-048/049) are part of "done" for #Release, not
  an afterthought.
- **Stack (owner's decision):** Kotlin + Jetpack Compose, native Android;
  port the prototype's logic (locking engine, layout algorithm, puzzle
  loader) — never wrap the HTML in a WebView. Everything *below* the stack
  (architecture, modules, libraries within rule 1, test framework) is the
  AI's (row 4) and goes to `decisions.md` / ADRs.
- **Builds on Jami's computer** (Windows, Claude Code terminal): the first
  WO's G3 toolchain proof is a trivial `gradlew assembleDebug` +
  `gradlew testDebugUnitTest` run there; until it passes, no P4 work
  (`gates-and-autonomy.md` G3 / §7 waivers).

## 3. Change log

| Version | Date | Change | Reason (stakes / evidence) | By |
|---|---|---|---|---|
| 0.1 | 2026-10-01 | proposed ai-mastered profile; not in force | owner: "AI-mastered mode is a good way to get experience; Tangram is a little side-line project" | agent (Claude) — awaiting Jami |
| 0.2 | 2026-10-01 | **in force**; row 5 = owner-decided Kotlin + Jetpack Compose (conditional on building on Jami's computer); rows 2/3/17 per Q3/Q5/Q4 defaults | Jami's answers to `SWDev/decisions-needed-2026-10.md` | Jami (chat), recorded by the agent |
