# Requirements Review 01 — TangramNoAds · whole collection (G1, intake path C)

**Date:** 2026-10-02 · **Reviewer:** fresh-context subagent (SWDev Requirements Reviewer, no part in capture) · **Collection:** v1.1 (locked 2026-10-01, SRC-014/015)

**Inputs read:** `Requirements/requirements.md` (index v1.1) · `Requirements/features.md` · `Requirements/reqs/REQ-001…051.md` (47 locked, 4 withdrawn: REQ-004, 027, 028, 044) · `Requirements/req_types.md` (v0.2, **Status: Draft**) · `Requirements/views/digest.md`, `trace.md`, `feature-map*.md` (generated 2026-10-01) · `Tangrams/puzzle.schema.json` (reference check only, because REQ-038/039/040/041/042/045/047 refer to puzzle-file fields).
**Not read:** `evidence/`, `sources.md`, `progress.md`, `decisions.md`, `STATUS.md`, `Study/`, `Spec/`, the prototype, `KICKOFF.md`, the puzzle files, earlier reviews. (`Requirements/CLAUDE.md` was surfaced automatically by the tool harness. It holds capture conventions only and was not used as evidence.)

## Verdict

**Don't accept as-is. Accept once the 3 Blockers are resolved or explicitly accepted** (each has a one-line fix or a one-sentence acceptance). There are 3 Blockers, 17 Shoulds and 18 Notes. For its size, the collection is unusually precise: the lock rule, the puzzle lifecycle, the tray geometry and the layouts are quantified, and the numbers check out (the tray-scale arithmetic in REQ-013 and REQ-035, and the turn diameters). The gaps are mostly lifecycle and platform edges (process kill, interrupted drag, language change, OS backup, the permission definition, the layout class) plus a few literal contradictions between locked items. The build can start under recorded ASSUMPTIONs for every Should. Blocker F1 is closed by the wording of the G1 answer itself.

---

## Findings

### F1 — DEPENDENCY · **Blocker** · `req_types.md` (TYPE-001, TYPE-003 … TYPE-007)
- **Observation:** `req_types.md` says *"Status: Draft · Approved by: — · Approved on: —"*, and index §3 lists the types as `draft`. The v1.0 and v1.1 sign-offs (SRC-014/015) name REQs and features only. Yet the locked Contract takes its substance from these types. TYPE-004 is the whole lock rule (R = 0.65 units / ≥ 30 dp, the exact turn and mirror, the score, the 0.16 window, the 1e-6 tolerance) behind REQ-016/018/019/021. TYPE-006 is the puzzle lifecycle behind REQ-003/025/030/050. TYPE-005 defines an active second for REQ-005/029/030. TYPE-001 gives the piece set, colours and tray order, and TYPE-007 the phone/tablet split. Five types are still `Confidence: Inferred`, and their agent ASSUMPTIONs (colours, resting turns, R, score weights, tolerance, 60 s idle, 600 dp) do not appear in `trace.md`, whose "Inferred items: none" check covers REQs only. So the core numbers of the Contract can change without a CHG, which falls below the principle-5 sign-off floor.
- **Proposed resolution:** At G1 the owner signs `req_types.md` v0.2 as part of the collection: TYPE-001 and TYPE-003…007 `locked` with signoff Jami and the date, TYPE-002 stays withdrawn, and the ASSUMPTION values are listed and explicitly accepted. TYPE-007 is either corrected first (F3) or accepted with F3's ASSUMPTION. Afterwards a type changes only by CHG, like a REQ.
- **Build under ASSUMPTION?** No ASSUMPTION is needed. The G1 answer resolves it if it names the types, e.g. "Accept collection v1.1 **including req_types.md v0.2**". Without that, tests of REQ-019, REQ-030 and the others rest on unsigned values.
- **Triage:** **Accepted at G1** (Jami 2026-10-02: "(F1) yes plz lock requirements … YES"). `req_types.md` v0.2 is Contract and in the frozen baseline. Capture side: mark TYPE-001, 003–007 `locked` + signoff (CA-2).

### F2 — CONFLICT · **Blocker** · REQ-012 (Statement + abstract) vs REQ-016, REQ-018, REQ-013, REQ-020, REQ-025/026/034
- **Observation:** The REQ-012 Statement reads: *"WHILE a puzzle is unsolved, the game SHALL show every piece … not on the board … in the order, colours **and resting turns** of TYPE-001"*. The digest abstract says *"always in the same order, colours and turn"*. But REQ-016 turns a tray piece on tap, REQ-018 mirrors the parallelogram in the tray, REQ-013 sizes each cell *"so that its piece fits in any turn"*, and REQ-025 saves a tray piece's turn and mirror. Read literally, REQ-012 forbids the tray turns that REQ-016 requires. The contradiction also leaves a player-visible behaviour with two readings. When a piece goes home (REQ-020: a missed drop, or a drop back on the tray), does it keep the turn and mirror the player gave it (REQ-013's "any turn") or snap back to its resting turn (REQ-012's literal text)? The same question applies after Restart (REQ-025), Retry (REQ-026) and Reset (REQ-034). A test written from either REQ fails a build that follows the other.
- **Proposed resolution:** One CHG to REQ-012:
  - The Statement becomes *"…in the order and colours of TYPE-001; when a puzzle starts (state New) every tray piece is in its TYPE-001 resting turn, unmirrored"*, and the abstract becomes *"…always in the same order and colours, starting in the same turn"*.
  - A rule is added with the owner's answer to this **question: "When a piece goes back to the tray, should it keep the turn (and mirror) it had, or return to its starting turn?"**
  - A second rule is added: Restart, Retry and Reset put every piece back in its resting turn, unmirrored.
- **Build under ASSUMPTION?** Yes. ASSUMPTION: a piece that goes home keeps its turn and mirror (REQ-013 cells are built for this, and REQ-025 already saves tray turns), and Restart, Retry and Reset restore the resting turns. Record it until the CHG lands.
- **Triage:** **Accepted at G1** ("(F2) OK good plan") under the stated ASSUMPTION — decisions.md. Capture side: CHG to REQ-012 as proposed (CA-3).

### F3 — CONFLICT · **Blocker** · TYPE-007 vs REQ-035, REQ-036, REQ-006
- **Observation:** TYPE-007 picks the layout by *"the window is less than 600 dp wide"*. REQ-035 then locks the Phone class to portrait, while REQ-036 allows both orientations for Tablet. The definition is circular. A 390 × 844 dp phone held sideways at launch has an 844 dp-wide window, so it is classed as Tablet, gets landscape and the one-row tray, and phone landscape is explicitly out of scope (REQ-006). A standard window-size-class implementation produces exactly this. The reverse case also exists: in split-screen a tablet's window can be under 600 dp, which makes it "Phone, portrait only", and orientation cannot be forced in multi-window. Nothing says state survives a window resize or a fold/unfold, because REQ-036.A2 covers rotation only. A test author cannot decide what "a phone launched in landscape" must show.
- **Proposed resolution:** CHG to TYPE-007: classify by the **device's smallest screen width** (the shorter side): under 600 dp is Phone, 600 dp or more is Tablet. Add a rule to REQ-036, or to a shared layout rule: *"Any change of window size or orientation (rotation, split screen, folding) re-lays the screen and keeps every piece's state, the current puzzle and any open overlay."* Also state what a sub-600 dp multi-window shows: the phone layout without forcing orientation.
- **Build under ASSUMPTION?** Yes, with the smallest-width classification and state kept on every window change. Record it and fold it into the F1 sign-off.
- **Triage:** **Accepted at G1** ("(F3) OK") under the stated ASSUMPTION — decisions.md. Capture side: CHG to TYPE-007 + REQ-036 as proposed (CA-3).

### F4 — EDGE · Should · REQ-025, REQ-029, REQ-030, REQ-035 (lifecycle across close, kill and update)
- **Observation:** REQ-025 says the puzzle state *"survives closing the app"*, and REQ-035 says back leaves the app *"the state being already saved"*. The collection never says:
  - (a) which puzzle a relaunch opens (only a fresh install is defined, REQ-045.A1);
  - (b) whether the **running puzzle time** (REQ-030), today's and all-time time, and the settings survive a close or a system kill (REQ-025's saved fields list pieces only);
  - (c) how much may be lost when Android kills the process in the background ("already saved" implies after every change, but nothing tests it);
  - (d) that progress survives an **app update** (REQ-025/G6 covers a *changed* puzzle, not an added or removed one, or the renumbering of "n / total");
  - (e) what happens when saved progress cannot be read (a crash loop on a child's tablet is the failure to rule out).
- **Proposed resolution:** CHG to REQ-025, or a new persistence REQ, with this rule: *"After every lock, return, turn, mirror, Restart, Retry, Reset or setting change, the state is persisted. Killing the app at any moment loses at most the drag in progress. A relaunch opens the last shown puzzle. An app update keeps the progress and times of every puzzle that still exists. Saved progress that cannot be read is discarded for that puzzle (it becomes New) and never stops the game from starting."* Add these acceptance criteria: force-stop after placing 3 pieces, then relaunch: same puzzle, same 3 pieces, same running time; and install an update over a save with progress: progress kept.
- **Build under ASSUMPTION?** Yes, with the rule above as the recorded ASSUMPTION.
- **Triage:** **Accepted at G1** ("(F4) OK") under the stated ASSUMPTION — decisions.md. Capture side: CHG to REQ-025 as proposed (CA-3).

### F5 — EDGE · Should · REQ-014, REQ-017, REQ-020, REQ-036 (interrupted or concurrent drag)
- **Observation:** Nothing says what happens to the piece in hand when a drag ends without a release. That happens when the system cancels the touch (an incoming call, the notification shade, a home or back gesture, the screen turning off, the app going to the background), when a tablet is rotated mid-drag, or when a second finger taps ‹ › / gear / counter. REQ-017 reads *any* second touch during a drag as a twist, so a tap on › would twist instead of navigating, and that is not stated as intended. REQ-012.A2 ("tray, board or being dragged") and REQ-020 ("no loose piece") leave two candidates: the piece goes home, or it returns to where it was picked up. For a board piece these differ visibly.
- **Proposed resolution:** Owner **question: "If a drag is interrupted (call, home button, rotation), should the piece go back to the tray, or back to where it was picked up?"** Then a CHG to REQ-014 or REQ-020: *"A drag that ends without a release (cancelled by the system, app hidden, window change) returns the piece to where it was picked up, silently. While a piece is being dragged, touches on other controls are ignored; a second touch is only a twist (REQ-017)."*
- **Build under ASSUMPTION?** Yes: back to where it was picked up, and other controls inert during a drag.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F6 — EDGE · Should · REQ-047 (language change mid-game)
- **Observation:** REQ-047 follows "the device language" but does not say (a) whether a change made while the game is open or in the background takes effect on return, (b) that puzzle state, an open overlay and the current puzzle survive the switch (Android recreates the screen on a language change, like a rotation, and REQ-036 covers rotation on tablets only), or (c) whether Android's **per-app language** setting (Android 13 and later) counts as "the device language".
- **Proposed resolution:** CHG to REQ-047 with this rule: *"The game uses the language Android reports for the app (system or per-app language). A change while the game is open or in the background takes effect on return and changes no puzzle state, time or setting."* Add **A3**: switch the device from English to Finnish mid-puzzle and return: Finnish texts, same pieces, same running time.
- **Build under ASSUMPTION?** Yes, with the rule above.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F7 — GAP / QUESTION · Should · REQ-010, REQ-049 (OS backup and device transfer vs the privacy text)
- **Observation:** Android's Auto Backup is on by default for apps. When the user has device backup enabled, it copies app data to the user's Google Drive, and device-to-device transfer copies it to a new phone, unless the app opts out of both. With the defaults, REQ-010 (*"SHALL NOT send any data off the device"*) is broken by the OS. The locked privacy text of REQ-049 (*"stored only on this device and are deleted when the game is uninstalled"*), which is shown to parents, becomes untrue: data is restored on reinstall. No acceptance criterion would catch this. It is also a product decision: the owner may *want* a child's progress to follow them to a new phone.
- **Proposed resolution:** Owner **question: "Should puzzle progress be included in Android's own backup and phone-to-phone transfer?"** If no, CHG to add **REQ-010.A3**: *"The game is excluded from Android cloud backup and device-to-device transfer; after uninstall and reinstall, every puzzle is New."* If yes, CHG REQ-010 (backup becomes an allowed exception) and reword REQ-049's privacy text and the REQ-048 data-safety statement to match.
- **Build under ASSUMPTION?** Yes: opted out of both, which is the literal reading of REQ-010. Flag it to the owner because it touches the privacy floor.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F8 — AMBIGUITY · Should · REQ-010.A2, REQ-048.A3 ("declares no permissions")
- **Observation:** *"The installed app declares no permissions, not even network access"* cannot be tested as written, because the standard AndroidX/Compose toolchain merges library manifest entries. A common one is an app-internal, signature-level permission such as `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` from androidx.core. A haptic implementation that calls the vibrator service would also add `VIBRATE`, a normal permission that REQ-033's "without a permission" forbids. A test author cannot tell whether A2 means "zero `<uses-permission>` and `<permission>` entries in the merged release manifest" or "no permission the user or the store can see".
- **Proposed resolution:** CHG to REQ-010.A2 and REQ-048.A3: *"The release build's merged manifest requests no permission, apart from app-internal signature-level permissions generated by platform libraries; in particular no INTERNET, ACCESS_NETWORK_STATE, VIBRATE or location permission."* Alternatively the owner can demand strict zero, which costs a manifest override.
- **Build under ASSUMPTION?** Yes: strip what can be stripped, tolerate only the library-generated signature permission, and record it.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F9 — GAP / QUESTION · Should · REQ-006 (supported devices)
- **Observation:** REQ-006 says *"Android phones … and Android tablets"* with no **minimum Android version**, no smallest supported phone width (the acceptance criteria use 360 × 780, 390 × 844 and 1280 × 800 dp, but nothing covers narrower phones such as 320 dp), and no reference devices for "fully playable" (A1). For a free children's game this is a business choice, because hand-me-down family tablets often run old Android. Test authors need the device matrix.
- **Proposed resolution:** Owner **question: "What is the oldest Android version, and the smallest phone screen, the game must run on?"** Then CHG REQ-006 to add the minimum version and the reference devices for A1.
- **Build under ASSUMPTION?** Yes. The builder records the chosen minimum API level and smallest width in `decisions.md` (e.g. API 26 / 360 dp) until the owner answers.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F10 — QUESTION · Should · TYPE-006, REQ-026, REQ-050, REQ-003 (Retry erases "solved")
- **Observation:** TYPE-006 says *"Retry returns Solved → New (the best time is kept)"*. Once Retry is pressed, the puzzle no longer counts as solved: the grid (REQ-050) shows it as a flat silhouette rather than its picture, the long press on › treats it as unsolved, and the top bar shows New. A player who finished all 20 and retries one to beat their time, then walks away, has 19 solved. That may be intended, but REQ-003's abstract promises *"no progress is lost"*, and nobody has decided whether "has been solved at least once" is progress.
- **Proposed resolution:** Owner **question: "After Retry, should the puzzle still count as solved (picture in the grid, skipped by 'next unsolved') until it is solved again?"** If yes, CHG TYPE-006 (a fourth value, or a separate "ever solved" flag) and REQ-050. If no, add a clarifying rule to REQ-026 so the behaviour is deliberate.
- **Build under ASSUMPTION?** Yes, under literal TYPE-006 (Retry → New).
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F11 — AMBIGUITY · Should · REQ-005.A1/A2, REQ-026, REQ-030 (which time the player sees)
- **Observation:** REQ-005.A1 says *"After solving a puzzle, its solve time is **available**"*, and REQ-030 says the game *"store[s] the time and keep[s] the shortest as the best time"*. But the solved view (REQ-026) and the settings list show only the **best** time. After a slower re-solve, the time just achieved is stored and shown nowhere, so it is unclear whether A1 passes. "Available" is a vague term in an acceptance criterion, and A2 has the same problem.
- **Proposed resolution:** Owner **question: "Right after solving, should the player see this solve's time as well as the best time (e.g. '1:42, best 1:10')?"** Then CHG REQ-005.A1 and A2 to observable wording: *"After solving, the solved view shows [this time and] the best time"* and *"The settings screen shows today's and all-time play time"*. If this solve's time is not shown, drop "store the time" from REQ-030 or say what it is for.
- **Build under ASSUMPTION?** Yes: only the best time is shown, as REQ-026 says, and A1 is read as "the best time is shown".
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F12 — UNTESTABLE · Should · REQ-014.A1
- **Observation:** *"A dragged piece follows the finger **without delay**"* is the "real-time" kind of vague term the playbook says to quantify. The intent is clear, but no test can pass or fail it as written.
- **Proposed resolution:** CHG to A1: *"On every displayed frame of a drag, the piece is drawn at the finger's latest reported position plus the floating offset (REQ-014 ASSUMPTION)."* This is frame-level, not fake milliseconds.
- **Build under ASSUMPTION?** Yes, with this wording as the test's interpretation.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F13 — EDGE · Should · REQ-015, REQ-018, REQ-037 (overlapping touch areas)
- **Observation:** Board pieces lock against each other's corners, so neighbours almost always touch, and their touch areas (*"shape grown by 12 dp"*) overlap along every shared edge. REQ-015 does not say which piece a touch in the overlap picks, and REQ-015.A2 (*"A touch 10 dp outside a small triangle's edge picks it up"*) is only well-defined for an isolated piece. Picking up the wrong neighbour is exactly "controls fighting back" (REQ-002). The flip badge has the same problem: its 60 dp touch area (REQ-018) sits inside the parallelogram's tray cell, the whole of which is the piece's touch area (REQ-015), and on the board it sits "next to" the piece, where it may overlap neighbours or the board edge.
- **Proposed resolution:** CHG REQ-015 with this rule: *"A touch inside a piece's drawn shape picks that piece; otherwise the piece whose edge is nearest, within 12 dp; the flip badge takes precedence over any piece touch area it overlaps."* Add an A3 for two adjacent pieces. Add to REQ-018 where the badge goes when "next to" would leave the board or cover another piece.
- **Build under ASSUMPTION?** Yes, with the rule above.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F14 — GAP / UNTESTABLE · Should · REQ-009, REQ-049, REQ-032, REQ-029, REQ-047 (Finnish wording)
- **Observation:** REQ-009 and REQ-049 lock exact English wording (A1: *"in full"*), and REQ-047 requires both texts in Finnish. The Finnish wording is not in the collection: REQ-049 points to *"its Finnish translation in the prototype"* and REQ-009 says nothing. The "short how-to-play text" (REQ-032) has no wording in either language. REQ-029's *"h and min"* has no Finnish equivalent (*t*/*min*?). A Finnish-locale acceptance test of REQ-009.A1 or REQ-049.A1 has nothing to compare against.
- **Proposed resolution:** CHG REQ-009 and REQ-049 to carry the Finnish text verbatim, approved by the owner. CHG REQ-032 to either lock the how-to-play wording in both languages or declare it a design input outside the Contract (then A-criteria test only that it is present). Add the Finnish time units to REQ-029.
- **Build under ASSUMPTION?** Yes. The prototype's Finnish strings are adopted verbatim, and the builder writes the how-to-play text for review at WO close.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F15 — DEPENDENCY · Should · REQ-038, REQ-041, REQ-042, REQ-045, REQ-046, REQ-047, REQ-051, REQ-002.A1 (Contract defined outside the collection)
- **Observation:** Several Contract meanings live in artifacts that change without a CHG:
  - REQ-038 defines puzzle validity as *"validator rules V1–V6, V8–V10 and V12 in ../tools/validate_puzzles.py"*. V7 is excluded without explanation, and the rule texts are not in the collection.
  - REQ-041 (V12), REQ-045 (V9) and REQ-047 (V8) lean on the same script.
  - REQ-042's themes are *"the categories in ../Spec/03-puzzle-format.md"*, a design document. The schema's `category` enum lists 8 of them.
  - REQ-046 refers to *"design sheets (../Spec/ui/)"*, and REQ-046 and REQ-051 to *"Q10 in ../Spec/05-open-questions.md"*.

  A validator edit can therefore silently change what REQ-038.A1 accepts. Separately, REQ-002.A1 (*"Every puzzle can be completed … by drag, turn and drop only"*) has no mechanical check that each stored solution is actually reachable through the runtime lock (TYPE-004 with its tolerance). REQ-038.A2 is the natural place for that check, but as written it is a tautology (*"it always does"*).
- **Proposed resolution:** CHG REQ-038 to list each referenced V-rule as one plain line (or move them into a new shared type for puzzle validity), and to say why V7 is excluded. CHG REQ-042 to reference the schema's `category` enum, or to list the categories. Replace the Spec references with the decision itself ("no hints for players"). CHG REQ-038.A2 to: *"For every shipped puzzle, dropping each piece exactly at its stored solution position, in the printed build order, locks it by REQ-019/TYPE-004"*. That makes REQ-002.A1 testable.
- **Build under ASSUMPTION?** Yes. The validator at the G1 commit is frozen as the reference, and any change to a V-rule is a CHG.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F16 — UNTESTABLE · Should · REQ-039.A2 vs REQ-042.A1 (human-review gate loophole)
- **Observation:** REQ-039.A2 reads *"No released puzzle has reviewedByHuman: false"*. In `puzzle.schema.json`, `provenance` and `reviewedByHuman` are both optional, so a puzzle with the flag **absent** passes A2, while the rule says a puzzle is released *only after* a person approved it. REQ-042.A1 counts only `true`. A release could therefore pass both criteria and still ship unreviewed puzzles beyond the first 20.
- **Proposed resolution:** CHG REQ-039.A2 to: *"Every puzzle in a release build has reviewedByHuman: true."*
- **Build under ASSUMPTION?** Yes, under that reading.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F17 — UNTESTABLE / PROPOSAL · Should · REQ-001.A2, REQ-048.A1–A2, REQ-042.A1–A3, REQ-039.A2 (release-time acceptance) + index §5 distribution question
- **Observation:** These criteria check a store listing or the release content, so no build-time acceptance test can verify them. The "Contains ads" and "In-app purchases" labels exist only on Google Play, but the channel (Play, F-Droid or a signed APK) is still an **OPEN QUESTION** in index §5 while the collection is locked. On F-Droid, REQ-001.A2 and REQ-048.A1/A2 are not applicable as written.
- **Proposed resolution:** (a) Mark these criteria as **release-checklist verification**: manual evidence at release, still carrying REQ-NNN.An tokens, not checked at WO close. (b) Owner **question: "Which channel ships v1: Google Play, F-Droid, or both?"** Then CHG the A-criteria to be channel-conditional ("On Google Play: …").
- **Build under ASSUMPTION?** Yes. The build is not blocked, and these are verified at release.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F18 — AMBIGUITY · Should · REQ-046 (what a "test build" is)
- **Observation:** *"prototype or test build"* versus *"release build"* is never defined. Index §5 says the phone playtest *"could double as the closed test"*, meaning 12 testers for 14 days on a Play testing track. The usual Play path promotes the tested bundle to production. If the closed-test build carries the DEV aid (passcode 0417), either the promoted production build contains it, which violates A4, or production ships a rebuilt bundle the testers never ran. The rule *"until the **page** or app is restarted"* is also wording left over from the web prototype.
- **Proposed resolution:** Owner **question: "Is the build your closed testers get a release build (no DEV aid) or a test build (DEV aid)?"** Then CHG REQ-046 to define the build types, e.g. *"Any build distributed through a store track is a release build; the DEV aid exists only in builds installed directly"*, and replace "page".
- **Build under ASSUMPTION?** Yes, with that definition.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F19 — PROPOSAL (principle 14 / how-leakage) · Should · REQ-025 (profile rule)
- **Observation:** The rule *"Progress is stored per player profile; the first version has exactly one profile"*, with its parenthetical *"the storage layout should not prevent it"*, has no observable v1 behaviour, so it is untestable. It also asks the design to anticipate release two, which is an abstraction without a v1 REQ (principle 14) and a storage-layout "how" (principle 2). Designers will not know whether they must build a profile abstraction.
- **Proposed resolution:** CHG: keep only the testable part (*"v1 has no profiles: one set of progress per installation, no profile choice anywhere"*) and move the storage-layout wish to a design input or the index's later-release notes.
- **Build under ASSUMPTION?** Yes. The rule is treated as a non-binding design input, and no profile abstraction is built.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F20 — DEPENDENCY · Should · `depends:` fields across the collection
- **Observation:** Several prose references that carry rules are missing from `depends:`, which is what CHG impact analysis on a locked Contract relies on:
  - REQ-014 resolves drops *"by REQ-019 and REQ-020"* but has `depends: []`;
  - REQ-036 imports REQ-035's insets and back rules (*"as on the phone (REQ-035)"*);
  - REQ-024 uses TYPE-006 (the state in the top bar), REQ-040 (order) and REQ-025 (save);
  - REQ-026 uses REQ-030 (best time) and TYPE-006 (Retry);
  - REQ-034 sets TYPE-006 states and erases REQ-029/030 times;
  - REQ-041 relies on REQ-045 and REQ-040;
  - REQ-042 relies on REQ-039;
  - REQ-048.A3 is REQ-010's check;
  - REQ-023 needs REQ-039's picture;
  - REQ-018 relies on REQ-019 and REQ-016 ("still locks", "as REQ-016");
  - REQ-012 relies on REQ-045 and REQ-047;
  - REQ-051 relies on REQ-045;
  - REQ-040 relies on REQ-045 and REQ-041 (the kinds).

  Adding REQ-045 → REQ-038 (*"a valid tangram (REQ-038)"*) would create a REQ-038 ↔ REQ-045 cycle. No live REQ depends on a withdrawn item.
- **Proposed resolution:** One housekeeping CHG that fills the fields above. Break the cycle by keeping the mini exception only in REQ-038 and having REQ-045 depend on REQ-038 alone.
- **Build under ASSUMPTION?** Yes. This does not change behaviour; it matters only for impact analysis.
- **Triage:** **Accepted at G1** ("+ rest OK .. YES") under the stated ASSUMPTION — decisions.md. Capture side: CHG as proposed (CA-3).

### F21 — CONFLICT · Note · `features.md` #Difficulty, #DifficultyChoice, #DifficultyRules, #TurnButtons
- **Observation:** These features are `locked` but carry only withdrawn REQs. The digest, the owner's sign-off surface, shows *"#Difficulty — Easy, Medium, Hard | locked"*, and `trace.md` lists three of them as "awaiting REQ drill-down". This was done deliberately (index v1.1), but a traceability auditor will see four locked leaves with no coverage.
- **Proposed resolution:** Mark them withdrawn if the format allows; otherwise tell the G4 traceability auditor explicitly that they carry no Contract.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F22 — QUESTION · Note · whole collection (locked before the playtest)
- **Observation:** Index §1 names the phone and tablet playtest as the success test, and it has not happened yet. About 20 agent ASSUMPTION values are now Contract and the index expects some to move ("the phone playtest may move R"): R, 12 dp, 60 s idle, 48/52/60/64 dp, 600 dp, offsets, and animation times. Every move will be a CHG plus a test change.
- **Proposed resolution:** At G1 the owner accepts this consciously, e.g. "accept v1.1; post-playtest value changes come as CHGs".
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F23 — AMBIGUITY · Note · REQ-019 abstract, A1, A3, A4; TYPE-004
- **Observation:** The abstract says *"nearest spot"*, but TYPE-004 picks by score: a 0.04 bonus per anchored corner within a window of best |t| + 0.16. A1 (*"locks there exactly"*) fails when two valid positions are in reach. A3 (*"touches no outline corner and has no neighbour to tie to"*) should read *"no anchor within the lock distance"*. A4 lacks *"dropped with the matching turn and mirror within the lock distance"*. TYPE-004 has no tie-break for equal scores, which matters if the Python validator (F15) and the app must agree.
- **Proposed resolution:** CHG the acceptance wording as above; abstract → "the best spot near the drop"; add a deterministic tie-break to TYPE-004.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F24 — CONFLICT (literal) · Note · REQ-011.A1 vs REQ-021, REQ-051, REQ-046
- **Observation:** REQ-011 forbids *"slot outlines or other hints of where pieces go"* on an unsolved puzzle, yet REQ-021 draws a dashed outline where the piece would lock, REQ-051 shows the anchor corners, and REQ-046 overlays the solution in test builds. The intent is "nothing shown *before* a drag", but A1 does not say so.
- **Proposed resolution:** CHG REQ-011.A1 to: *"…while no piece is being dragged; REQ-021, REQ-051 and REQ-046 excepted."*
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F25 — CONFLICT (literal) · Note · REQ-032.A1 vs REQ-034
- **Observation:** *"Opening and closing the settings changes nothing on the board"* is false after a confirmed reset. Nothing says which puzzle is shown after a reset: the current one, now New, or the first puzzle as on a fresh install.
- **Proposed resolution:** CHG A1: *"…unless Reset all progress was confirmed"*. Add a rule for the puzzle shown after a reset.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F26 — AMBIGUITY · Note · REQ-023 vs REQ-033; reduced motion
- **Observation:** *"a 4-note chime and confetti lasting 2.4 s"* reads as a 2.4 s sound, while REQ-033 says *"each effect lasts at most 1 s"*. *"The pieces do not pop"* refers to a pop animation that is never defined. Reduced motion covers only the confetti, the pop and the REQ-051 pulse, not the grow (120 ms), lock and return (180 ms) or shake animations.
- **Proposed resolution:** CHG REQ-023: chime ≤ 1 s, confetti 2.4 s, and define the pop. Owner decides whether reduced motion also shortens the other animations.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F27 — GAP · Note · TYPE-006 transitions
- **Observation:** Several transitions are missing:
  - Reset any → New (REQ-034);
  - the DEV aid's New or In progress → Solved (REQ-046);
  - pieces returned by update invalidation (REQ-025/G6), including which of two overlapping saved pieces goes home.

  "In progress with an empty board" (a piece dragged out, then sent home) is reachable but not stated. In that state Restart is shown and the grid shows a dot.
- **Proposed resolution:** CHG TYPE-006 to list every transition with its governing REQ, and to state the empty In-progress case.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F28 — EDGE · Note · REQ-029, REQ-030, REQ-031 (time edges)
- **Observation:** *"Today's time restarts at local midnight"* does not cover a clock or time-zone change, or a session crossing midnight (DEF-001 shows this area has already gone wrong once). REQ-030's pause list omits the grid overlay (REQ-050) and the DEV overlay. REQ-029's display format covers play times only, not the best time or the on-screen timer. REQ-031 does not say what the timer shows on a New puzzle.
- **Proposed resolution:** CHG REQ-029: *"Today's time belongs to the device's current local date; when that date differs from the stored one, today's time starts at 0."* Add the grid overlay to REQ-030's pause rule, and apply one time format everywhere.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F29 — AMBIGUITY · Note · REQ-002 Statement and A1
- **Observation:** *"by drag, turn and drop only"* leaves out mirroring, so a puzzle that needs the flipped parallelogram fails A1 if read literally.
- **Proposed resolution:** CHG to "drag, turn, mirror and drop".
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F30 — EDGE · Note · REQ-050, REQ-026, REQ-025, REQ-051 (navigation and teaching)
- **Observation:**
  - Long press on › when only the *current* puzzle is unsolved is not covered.
  - REQ-026's "Next" could mean › or "next unsolved".
  - Restart has no confirmation, so one stray tap clears a nearly finished puzzle. The owner should confirm that is acceptable.
  - REQ-051's "goes home" also fires on a *deliberate* drop back on the tray, and "pulse once" could mean once per missed drop or once ever.
- **Proposed resolution:** One clarifying line per REQ.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F31 — AMBIGUITY · Note · REQ-015, REQ-017, REQ-018, REQ-016 (gesture details)
- **Observation:**
  - REQ-015: is "moved less than 12 dp" net or maximum displacement, and does the drag visibly start at 12 dp?
  - REQ-017: what happens if the *first* finger lifts mid-twist?
  - REQ-018: is the badge shown while the parallelogram is being dragged?
  - REQ-016: the anchor set for a board tap-turn must exclude the piece's own corners. This is obvious, but it is unstated.
- **Proposed resolution:** One clarifying line each, or leave to design and record the choices in `decisions.md`.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F32 — AMBIGUITY · Note · abstracts and titles (checked; none misleading enough to block)
- **Observation:**
  - REQ-038's abstract says *"uses all seven pieces"* (the Statement excepts minis).
  - REQ-041's *"every piece's shape shows in the silhouette"* is looser than the half-outline measure.
  - REQ-045's title says "3-piece", but the rule allows 1–6 pieces and only the first mini must have 3.
  - REQ-040's "rating" is the puzzle-file field `difficulty`.
  - REQ-039's "3–8 colours" does not say whether `base` and strokes count.
  - REQ-012's rule about the puzzle title belongs in REQ-047 or REQ-024.
- **Proposed resolution:** Tidy these in the next CHG batch.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F33 — PROPOSAL · Note · how-leakage (principle 2)
- **Observation:**
  - REQ-007 (BIZ): *"Puzzles are held in their own folder"*;
  - REQ-048: *"The repository keeps its name"*;
  - REQ-038: a script path as the rule;
  - REQ-021: *"Updated at most once per displayed frame"* (a performance cap, not a requirement);
  - REQ-046: "page".

  None changes behaviour.
- **Proposed resolution:** Move these to design inputs or delete them in the next CHG batch.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F34 — UNTESTABLE · Note · REQ-048 rules and assumptions
- **Observation:**
  - *"Offered to the store's teacher-approved programme where one exists"* has no acceptance criterion. As far as this reviewer knows, Play's teacher approval is granted by Google's review, not applied for.
  - REQ-048 still carries the Finnish-title ASSUMPTION *"the owner confirms the wording"*, although index §5 says the lock closed it.
  - *"Hosted privacy policy is a page in the project's repository"* only works if the repository is public.
- **Proposed resolution:** Reword or drop the teacher-approved rule, remove the stale ASSUMPTION, and confirm the repository is public, or choose another host.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F35 — GAP · Note · not declared in or out of scope
- **Observation:**
  - the launcher name under the icon (the store name is long) in en/fi, and the icon itself;
  - system font scaling (the top bar with Finnish titles), dark theme and TalkBack (#Accessibility covers only colour-blindness, as an idea);
  - the test tolerance for the locked animation durations (120/180/600/800 ms, 2.4 s).
- **Proposed resolution:** Declare each in or out of scope. For tests, assume ±1 frame on durations.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F36 — PROPOSAL · Note · priorities
- **Observation:** All 47 locked REQs have `priority: none` ("priorities ⏸ owner" in every coverage line).
- **Proposed resolution:** Confirm that all are v1 must-haves, or set priorities so the build can be sliced.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F37 — PROPOSAL · Note · REQ-042 (release gate content)
- **Observation:** The index lists 13 puzzles. REQ-042 needs at least 20, each solved and approved by the owner (REQ-039), so at least 7 more puzzles need owner time before release.
- **Proposed resolution:** Plan it; no requirement change is needed.
- **Triage:** Note — accepted at G1 ("rest OK"); carried into the capture-side CHG batch (CA-3); interpretations needed by a WO are decided in its design and logged to decisions.md.

### F38 — PROPOSAL · Note · views generator (SwReqCollector friction, not this project's to fix)
- **Observation:**
  - The per-feature maps print *"Cross-area realizes: none"*, although REQ-032 → REQ-029, REQ-033 → REQ-002, REQ-034 → REQ-003, REQ-040 → REQ-003 and REQ-047 → REQ-002 cross areas.
  - `trace.md` says *"Open questions: none"* while index §5 holds the distribution OPEN QUESTION.
  - The trace's inferred check ignores TYPEs (see F1).
- **Proposed resolution:** Record in `Requirements/framework-notes.md` for a SwReqCollector session.
- **Triage:** Note — accepted at G1; SwReqCollector tool friction, for `Requirements/framework-notes.md` in a capture-side session (CA-4).

---

## Summary

| Severity | Count | Findings |
|---|---|---|
| **Blocker** | 3 | F1, F2, F3 |
| Should | 17 | F4–F20 |
| Note | 18 | F21–F38 |
| **Total** | **38** | |

By primary category: CONFLICT 5 (F2, F3, F21, F24, F25) · EDGE 6 (F4, F5, F6, F13, F28, F30) · AMBIGUITY 8 (F8, F11, F18, F23, F26, F29, F31, F32) · UNTESTABLE 4 (F12, F16, F17, F34) · DEPENDENCY 3 (F1, F15, F20) · GAP 5 (F7, F9, F14, F27, F35) · QUESTION 2 (F10, F22) · PROPOSAL 5 (F19, F33, F36, F37, F38). Several findings also carry a second tag.

**Can the build proceed under recorded ASSUMPTIONs?** Yes, for F2, F3 and every Should (F4–F20); each finding names its ASSUMPTION. F1 needs no ASSUMPTION: it is closed by the G1 answer naming `req_types.md` v0.2.

## Coverage of this review

- **Checklist items 1–12** were applied to every live area: promise, play, browsing, time, settings, layout, content, release and devtools. Withdrawn items (REQ-004, 027, 028, 044, TYPE-002) were checked only for dangling references. No live REQ depends on them. Historical mentions in REQ-013, REQ-018, REQ-032, REQ-037 and TYPE-004 are informational and fine.
- **Numbers verified:** TYPE-001 areas (sum 16); the REQ-013 turn diameters against the piece geometry (4.22 / 2.98 / 2.11 / 2.00 / 3.16); REQ-013.A1 feasibility (360 × 780 dp: scale ≈ 26.7 dp/unit, smallest cell ≈ 65 dp ≥ 56 dp); REQ-035.A1 and REQ-036.A1 tray fit; REQ-017.A1/A2 step boundaries.
- **Hard-stop domains:** no money, PII or security handling is hiding in the rules. Data deletion is declared (REQ-034, with confirmation and a cancel criterion). The privacy statement has one platform-default trap (F7).
- **Business events:** the game sends no notifications. Celebration and feedback are governed by REQ-023 and REQ-033.
- **Not reviewed:** evidence, sources and the capture history (by rule); the puzzle files themselves (only the schema, for reference resolution); whether the agent's ASSUMPTION values are right for children (that is the playtest's job, F22).

---

**Gate footer:** Under the SWDev Requirements Reviewer rule, **G1 acceptance of collection v1.1 may proceed only when every Blocker (F1, F2, F3) is either resolved by a capture-side CHG or explicitly accepted by the owner, with the accepting ASSUMPTION recorded, before or at the G1 answer.** Shoulds are resolved or consciously deferred, and recorded. Notes never block. Record the outcome of this review round in the index change log.

---

## Triage outcome and capture-side actions (orchestrator, 2026-10-02)

**G1:** accepted by Jami on 2026-10-02 (progress.md, verbatim: "(F1) yes plz lock requirements
and continue with all reqs. (F2) OK good plan (F3) OK (F4) OK + rest OK .. YES"). All three
Blockers are closed: F1 by the answer naming `req_types.md` v0.2; F2 and F3 by the owner's
explicit acceptance of their ASSUMPTIONs. The build proceeds under the ASSUMPTIONs recorded in
`decisions.md`. The contract baseline was frozen right after (`trace_check.py --freeze`).

The build side never writes into `Requirements/` (owner's instruction for this session + guard),
and Phase D (`changes/CHG-NNN.md`) is not tooled yet, so these actions wait here for a
SwReqCollector session:

| # | Action | From |
|---|---|---|
| CA-1 | Register the G1 answer as **SRC-016** (conversation, authoritative) with the verbatim quote above; add an index change-log row "fresh-eyes review 01 triaged, G1 accepted for the build". | footer |
| CA-2 | `req_types.md`: TYPE-001, 003–007 → `locked`, Approved by Jami, 2026-10-02 (SRC-016); index §3 Shared types → locked. | F1 |
| CA-3 | One CHG batch carrying the proposed resolutions of F2–F20 (owner already accepted them as ASSUMPTIONs) and the Note tidy-ups F21–F37. Each REQ/TYPE change re-locks with signoff; the build updates its tests to match. | F2–F37 |
| CA-4 | `Requirements/framework-notes.md`: the views-generator findings of F38 (cross-area realizes, open questions, TYPE inference check). | F38 |
