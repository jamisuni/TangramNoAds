# Requirements Index — TangramNoAds

**Status:** Locked (owner sign-off 2026-10-01, SRC-014, SRC-015) · **Version:** 1.1 · **Last updated:** 2026-10-01
**Framework:** SwReqCollector format v2 (tools v0.4) at `C:\GitHub\AI\SwReqCollector`
**Capture purpose:** build
**Capture mode:** ai-led

> Next free ID: **REQ-052**. IDs unique project-wide per kind, never
> reused; features are identified by unique #Tags in `features.md`.
> Run `validate.py` + `views.py` after any item change; items are truth,
> views are generated. Sources are this project's own material; other
> products are never sources (`capture-loop.md` §4).

---

## 1. Context & goals

- **System described:** TangramNoAds, a classic 7-piece tangram game for Android phones and tablets; store name **"Tangram, absolutely free"** (REQ-048). First capture: prototype 0.3 stage, 2026-09-27.
- **Problem / purpose:** a tangram for players aged 8 and up that is simple to *control* and honestly challenging to *solve*, and completely free: no ads, no purchases, no money asks. This collection is the requirement source for the build; `../Spec/` and `../Study/` are the agent's earlier drafts and notes.
- **Why ai-led:** the agent proposed the first version (study, spec draft, prototype 0.3) and the owner corrected it in short rounds (SRC-002, SRC-003, SRC-008) and then tried it in a web browser (SRC-009, SRC-010). In round 6 (SRC-011) the owner accepted the agent's concept review as a whole: the difficulty levels and the tray turn buttons were removed, the flip badge is always shown, and the gaps the review found (language, store obligations, puzzle overview, saved-state migration, system UI) became requirements. Items the prototype implements were released `ai-approved`. **On 2026-10-01 the owner signed off and locked all 47 of them (SRC-014)**, before a phone playtest. From here on a change to a locked item is a change delta (`changes/CHG-NNN.md`, SwReqCollector Phase D), never a silent edit.
- **Success looks like:** the owner plays prototype 0.6 on a phone and a tablet, and every item gets a verdict: confirmed (→ `stated`), wrong (→ DEF-NNN or rewrite) or unused (→ `priority: none` / withdrawn).
- **Out of scope (whole capture):** technology and architecture (parked by the owner, SRC-002); difficulty levels (removed, SRC-011); toddler modes and hints; accounts, cloud sync and several profiles (one profile in v1, REQ-025); donations and any other income (SRC-003); non-classic piece sets (idea #PieceSets, later).

## 2. Stakeholders & actors

| Actor / role | Interest | Triggers behavior? | Approves rules? |
|---|---|---|---|
| Player, 8+ (school-age child or adult) | solves puzzles, wants controls that never fight back | yes (all play) | no |
| Parent | trusts the game: free, no ads, nothing collected | changes settings, resets progress | no |
| Owner (Jami) | the product vision; decides, uses the prototype, signs off | yes (reviews, playtests) | **yes** |
| Agent (Claude) | proposes, decides instead of asking, releases `ai-approved` for the prototype | no | no: `ai-approved` is not sign-off |
| App store (Google Play) | a system the project does not control: privacy policy, data safety, content rating, Families policies, testing rules | no | sets obligations (REQ-048) |

## 3. Collection inventory

| What | Where | Count / range | Status |
|---|---|---|---|
| Features | `features.md` | 53 (13 top-level, 42 leaves; 2 are ideas; 4 carry only withdrawn REQs) | **51 `locked`** (signed by Jami 2026-10-01, SRC-015), 2 `idea` (#PieceSets, #Accessibility) |
| Requirements | `reqs/` | REQ-001..051 (areas: promise, play, browsing, difficulty (withdrawn), time, settings, layout, content, release, devtools) | **47 `locked`** (signed by Jami 2026-10-01), 4 `withdrawn` (REQ-004, 027, 028, 044); priorities not set |
| Shared types | `req_types.md` | TYPE-001..007 (TYPE-002 withdrawn) | draft |
| Sources | `sources.md` | SRC-001..015 | all usable; SRC-004 is `generated` |
| Defects | §6 | DEF-001 | open |
| Domain model | `domain.md` | none yet | — |
| Views | `views/` | digest, feature-map, trace | generated |
| Puzzle library | `../Tangrams/` | 13 puzzles (2 mini, 4 warm-ups, 7 full, including the classic square) | not requirements: content that REQ-038..042 govern |

## 4. Capture coverage

**promise:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-001, 008–010) · quantified rules ✅ · entities n/a · timing n/a · edge cases ✅ · unwanted behavior ✅ · assumptions ✅ (no network = agent decision; permission-free haptics) · priorities ⏸ owner.

**play:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-002, 011–023, 043, 051) · quantified rules ✅ (TYPE-001/003/004 with the lock scoring and tolerance) · entities ✅ pieces, puzzles · timing ✅ animation times as ASSUMPTIONs · edge cases ✅ no-room turns, missed drops, the last piece · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**browsing:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-003, 024–026, 034, 050) · quantified rules ✅ TYPE-006 · entities ✅ puzzle state · timing n/a · edge cases ✅ list ends (wrap), all solved (long press), changed puzzle on load · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**difficulty:** withdrawn (REQ-004, 027, 028; TYPE-002). The puzzle rating (REQ-040) is the only difficulty.

**time:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-005, 029–031) · quantified rules ✅ TYPE-005 · entities ✅ · timing ✅ · edge cases ✅ idle, browsing away · unwanted behavior ✅ nothing leaves the device · assumptions ✅ · priorities ⏸ owner.

**settings:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-032–034, 047) · quantified rules ✅ · entities n/a · timing n/a · edge cases ✅ reset confirmation, unknown device language · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**layout:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-006, 013, 035–037) · quantified rules ✅ TYPE-007, sizes as ASSUMPTIONs · entities n/a · timing n/a · edge cases ✅ rotation mid-puzzle, gesture bar, back button · unwanted behavior n/a · assumptions ✅ · priorities ⏸ owner.

**content:** context ✅ · goals ✅ · actors ✅ · scope ✅ · capabilities ✅ (REQ-007, 038–042, 045) · quantified rules ✅ (20 puzzles, four themes; kind field; V12 warm-up measure) · entities ✅ puzzle file · timing n/a · edge cases ✅ · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

**release:** context ✅ · goals ✅ · actors ✅ store · scope ✅ · capabilities ✅ (REQ-048, 049) · quantified rules ✅ · entities n/a · timing ❓ which channel and account (§5) · edge cases n/a · unwanted behavior ✅ no permission, no network · assumptions ✅ · priorities ⏸ owner.

**devtools:** context ✅ · goals ✅ testing aid · actors ✅ owner, testers · scope ✅ never in release · capabilities ✅ (REQ-046) · quantified rules ✅ passcode · entities n/a · timing n/a · edge cases ✅ wrong passcode, best time · unwanted behavior ✅ · assumptions ✅ · priorities ⏸ owner.

## 5. Open questions & assumptions (project-wide)

This is the only list of open questions. `../Spec/05-open-questions.md` keeps the decision log and points here.

- **Capture convention (SRC-002):** the owner calls his messages "ideas, not requirements". Firm wording ("I want", "no …", "lets …") is recorded as `stated`; wording marked "maybe" or with a question mark supports only `inferred` items, which name the idea in their Rationale. An accepted proposal (SRC-011: "I agree with all your proposals") makes the proposal `stated`; the agent's detail choices inside it stay ASSUMPTIONs.
- **Use so far.** Web browser with a mouse (rounds 4–6). **No phone or tablet playtest yet.** Prototype 0.6 (`../Spec/prototype/tangram-prototype.html`) is the build for this cut. Its behaviour is the agent's proposal (SRC-004, `generated`), not evidence of intent. The next session registers the owner's playtest as a source and classifies every item.
- OPEN QUESTION (distribution, SRC-013): which developer account publishes the game? A personal Google Play account created after 13 Nov 2023 must run a closed test with 12 testers for 14 days before production. F-Droid (the game is GPL-3.0, offline, dependency-free) and a signed APK from the repository have no such rule. The phone playtest could double as the closed test.
- Closed by the lock (SRC-014): the Finnish store title is "Tangram, ihan ilmainen" as assumed in REQ-048.
- ASSUMPTION (TYPE-004): one lock parameter set for everyone (R = 0.65 units, ≥ 30 dp; exact turn and mirror) since the levels were removed; the phone playtest may move R.
- ASSUMPTION (REQ-031): the on-screen timer is off by default.
- ASSUMPTION (REQ-047): no in-game language switch; the device language decides.
- ASSUMPTION: the classic seven pieces for every first-version puzzle; other piece sets later (#PieceSets, SRC-008).
- **Closed in round 6 (SRC-011):** Q1 levels (removed), Q2 tap-to-turn (stays), Q5 landing preview (stays, always on), Q6 timer (off by default), Q8 puzzle count (20, REQ-042), Q9 store name, Q11 V11 (moot, never a restriction), Q12 tray buttons (removed) and size marks (stay), Q13 mirror (badge always).
- **Framework conformance note:** `../Study/01`–`04` and `07` contain notes about third-party products. Under `capture-loop.md` §4 such material is never a source and should not live in the project. It is kept out of every SRC here; the owner decides whether those files stay.

## 6. Defect list

> Observed behaviors the human classified as **defects**, never REQs.

| ID | Observed behavior | Sources | Decision |
|---|---|---|---|
| DEF-001 | Prototype 0.5 reset "Active today" at UTC midnight (`toISOString().slice(0,10)`), while REQ-029 says local midnight; in Helsinki the day flipped at 03:00. | SRC-004, SRC-011 (F3) | Defect; the prototype is fixed in 0.6 (local date). REQ-029 unchanged. |

## 7. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | 2026-09-27 | initial ai-led capture: 44 features, REQ-001..042, TYPE-001..007, SRC-001..008; 41 REQs released `ai-approved` for prototype 0.3, REQ-042 draft | move the TangramNoAds study and spec into SwReqCollector format (owner request) | — |
| 0.2 | 2026-09-27 | SRC-009; new #SizeMarks, #TurnButtons, #PuzzleMini with REQ-043..045 (ai-approved for prototype 0.4); REQ-012, 013, 014, 022, 025, 038, 041 and REQ-002 A1 updated for mini puzzles and the confirmed no-resize rule | owner round 4 after web testing | — |
| 0.3 | 2026-09-27 | SRC-010; new #DevTools / #SolutionReveal with REQ-046 (ai-approved for prototype 0.5); REQ-018 and REQ-044 assumptions updated | owner round 5 | — |
| 0.4 | 2026-09-28 | SRC-011..013. Withdrawn: REQ-004, 027, 028 (levels), REQ-044 (tray buttons), TYPE-002. Rewritten: TYPE-004 (one lock parameter set with scoring and tolerance), TYPE-006 (transitions), REQ-018 (badge always), 019, 021, 024 (wrap), 025 (load check, profile note), 030, 031 (off by default), 032, 037 (dev aids exempt), 038 (edge-first clause dropped: V11 is vacuous), 042 (20 puzzles, ai-approved). Rules added: REQ-010, 012, 013, 016, 023, 033, 034, 035, 036, 040, 041, 043, 045. New: #Language REQ-047, #Release REQ-048/049, #PuzzleOverview REQ-050, #CornerTeach REQ-051 (all ai-approved for prototype 0.6). DEF-001. Open-question lists merged here. | owner round 6: concept review accepted | — |
| 1.0 | 2026-10-01 | SRC-014. All 47 `ai-approved` REQs locked with `signoff: {by: Jami, date: 2026-10-01}`; the 13 `inferred` ones (REQ-010, 012, 013, 015, 017, 022, 031, 032, 033, 034, 037, 040, 043) became `stated` by the owner's confirmation; 20 inferred rule markers marked confirmed. Priorities left `none`. | owner sign-off | Jami |
| 1.1 | 2026-10-01 | SRC-015. Feature tree locked: the 48 `ai-approved` features and the 3 `captured` difficulty features (only withdrawn REQs) → `locked`; #PieceSets and #Accessibility stay `idea` (no evidence, no REQs, later than v1). | owner sign-off | Jami |
