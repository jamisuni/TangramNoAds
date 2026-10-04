# Review — design · WO-007

**Date:** 2026-10-04  ·  **Reviewer:** fresh context (design-reviewer)
**Inputs:**
- **The design and its frame:**
  - `designs/WO-007-design.md` (rev 0, 2026-10-04);
  - `workorders/WO-007.md`;
  - AGENTS.md (rules 1, 11, 13; the seams, test-adapter, token, staffing, release-safety and two-channel duties);
  - `build-map.md` v1.5 §1–2;
  - `architecture.md` v1.0 (G-01…G-10, O-03, O-05, O-06, O-08, O-09, §5 including test placement, ADR-006);
  - `governance.md` v0.2 (rows 11, 12, 13, 14, 15);
  - `req_review_01.md` (F14, F25, F26; CA-1…CA-8);
  - `design-inputs.md`.
- **Requirements (read in full):** REQ-009, 020, 032, 033, 034, 049; carried REQ-001, 006, 008, 010, 037, 047; also REQ-023, 031, 045, 050.
- **Decisions:** F2–F20 (F4, F5, F14 in particular); LOCK-V1; DA-19, 26, 27, 46–49, 53, 58, 62, 65, 72, 73, 83, 88, 89, 90, 92, 96–112.
- **Designs:** `designs/WO-006-design.md` (carried parts C1–C11); WO-003 design §3 (the chime line).
- **Code:**
  - `contracts`: `IProgressStore.kt`, `SavedGame.kt` (`GameSettings`).
  - `store`: `JsonProgressStore.kt`; the frozen fixtures `progress-v1.json` and `progress-v1-fresh.json`; `FrozenV1FixtureTest.kt`; `StoreWriterTest.kt` (the reset test).
  - `browse`: `BrowseController.kt`, `PuzzleHost.kt`, `BrowseTopBar.kt`, `AllPuzzlesOverlay.kt`, `SolvedBar.kt`, the strings, `build.gradle.kts`.
  - `play`: `PlaySession.kt`, `GestureMachine.kt`, `DropResolver.kt`.
  - `app`: `AppViewModel.kt`, `SessionHost.kt`, `MainActivity.kt`, `TangramApp.kt`, both `DebugAids.kt` twins, `build.gradle.kts`; tests `SessionHostScaffoldingTest`, `held/HeldSolveNowStoreTest`, `promise/PromiseWords.kt`, `language/UntranslatedStringsTest`, `LanguageFinnishAppTest`.
  - `devtools`: `build.gradle.kts` (`a4FileSetS`), `ReleaseSeparationTest.kt`, the strings.
  - `.swdev/verifiers/`: `v08_promise_apk.py`, `v04_release_apk.py` (canaries, strings glob), `v05_string_parity.py`, `v06_module_deps.py`.
  - `tools/prototype_template.html` (VERSION 0.6: `sfx`, `haptic`, the en/fi settings tables, lines 156–252).
- **The resolved Compose libraries (read only):**
  - The BOM pom `compose-bom-2026.09.00.pom` pins foundation and ui at **1.12.1**.
  - `foundation-android-1.12.1` and `ui-android-1.12.1` `classes.jar` were unpacked into the session scratchpad (`fnd/`, `ui/`) and read with the JBR `javap`.
- **Style reference:** `reviews/WO-006-design-review.md`.
- **Not read:** `.swdev/heldout/`. No Gradle, emulator or git was run.

## Verdict

**recirculate → design-author (rev 1, then a spot-check).**

Most of the design is right, and several of its claims hold up against the code:
- **The persistence finding is correct.** `GameSettings.soundOn` already lives in v1. `encode()` writes `settings.soundOn`, `decodeSettings()` reads it, the frozen fixture holds `"soundOn": false`, and `FrozenV1FixtureTest` asserts it. So there is no migration, no contract change and no row-11 trigger, and the WO's premise ("must not be added to the v1 progress file") was wrong.
- **No permission and no new library.** `AudioTrack` and `View.performHapticFeedback` need no permission, and both are framework classes.
- **The overlay over a Dialog** keeps one compose root.
- **The no-persist invariant after a reset** is real.
- **The verbatim English texts** are byte-equal to REQ-009 and REQ-049.

But the highest-risk claim fails. **The gate is not the only path to a sound or a haptic** (F1). With the resolved Compose 1.12.1:
- every `clickable`, `combinedClickable` and `toggleable` tap asks Android for the system click sound;
- the long press on › performs a long-press haptic.

Neither path reaches `Feedback`, the scan or the probe. Built as written, REQ-033 A1 would be broken on real devices while every planned check stays green. That is the WO-003 Blocker pattern.

Four Shoulds follow:
- the scan does not make the gate the only path even inside our code (F2);
- the after-reset puzzle choice is argued without the locked REQ-032 text that pulls the other way (F3);
- a seam change silently breaks a REQ-046 acceptance test (F4);
- the `AudioTrack` contract is unfrozen, and its device check cannot fail (F5).

**1 Blocker, 4 Shoulds, 8 Notes.**

## Checklist applied

- [x] **Directives.**
  - **D1.** The screen, outputs and gate are in `settings`; the event source is in `play`; `app` wires them; the reload is in `browse`.
  - **D2.** Synthesis plus `AudioTrack` beats assets, and the existing v1 field beats a second store. F3 asks for D2 to be weighed for the after-reset puzzle.
  - **D3.** Every abstraction names its REQ or decision: `SoundOut` / `HapticOut` (REQ-033, the probe seam), `Feedback` (REQ-033 A1), `FeedbackProbe` (DA-116), `PlayEvent` / `FeedbackEvent` (O-06, G-06).
  - **D4.** There are no placeholder rows and no slot parameters. The long-press haptic and the click sound are unrequested output that the design does not know it ships (F1).
  - **D5.** No new library.
- [x] **Guardrails.**
  - **G-01 / G-02.** No permission, no SDK; `settings` has no manifest.
  - **G-04.** The probe lives in `app/src/debug`, and V-08 gains it. See N5 for the release twin.
  - **G-05.** Every string is in fi and en.
  - **G-06.** `settings: {kernel, contracts}` is already in V-06.
  - **G-07.** There is a difficulty-word test (N6).
  - **G-09.** Untouched (re-verified).
  - **G-10.** The outs are wrapped in `runCatching`, but `reloadStaticData()` fails by return code (F5).
- [x] **Contract.** No `I*` change: `IProgressStore` and `GameSettings` are consumed as they are, and `Puzzle.kt` is untouched. The REQ-009/049 English is verbatim. DA-116's reading of REQ-033 A1 becomes a locked-criterion narrowing if the platform paths stay open (F1).
- [x] **Scope.** Everything traces to the 7 IDs and the carries. REQ-023's chime is built but not named (N2). The WO-008 carry-out is decided and correct.
- [x] **Traceability.**
  - Rule 3 holds: #Settings tests sit in `settings`, REQ-009/049 (code home `—`) sit in `settings` and `app`, and the cross-slice tokens are in `app`.
  - Seams: one casualty is unnamed (F4), and two edits are hedged or unassigned (F2, N5).
  - Token readings: N7.
- [x] **Hard-stops.** No schema change, no migration, no PII, no money. Row 11 was analysed and is not triggered (verified). Row 14 is hit only if F1 is "resolved" by narrowing A1.
- [x] **Evidence re-derived.** See below. Re-reading the code and the resolved libraries showed four claims to be wrong or incomplete:
  - the gate's completeness (F1);
  - the scan's lists (F2);
  - "the `SessionHostScaffoldingTest` adapter" as the only consumer (F4);
  - "the test author edits the test file if it names modules" (it does, N5).
- [x] **The conceptual 20 %.** The effort went to:
  - what actually makes sound and vibration in a Compose app, read from the resolved bytecode;
  - every `PlaySession` path against the five events;
  - the reset's interaction with `persist()`, `onPause` and late callbacks;
  - the v1 encode/decode and fixture;
  - the release/debug split against V-08 and DA-72/89;
  - the governance status of the Finnish text (F14) and of the after-reset puzzle (F25).

### Re-derived

- **v1 already persists the switch.**
  - `JsonProgressStore.kt` writes `settings.soundOn` (the `"settings"` object in `encode()`) and reads it in `decodeSettings()` (`flag("soundOn")`).
  - `progress-v1.json` ends with `"settings": {"timerShown": true, "soundOn": false}`, and `FrozenV1FixtureTest` asserts `GameSettings(timerShown = true, soundOn = false)`.
  - `resetAllProgress()` (lines 121–126) clears `puzzles` and `playTime` and keeps `settings` and `lastShown`. `StoreWriterTest.resetAllProgressDoesExactlyWhatTheKdocListsAndLeavesLastShown` pins it.
  - The store is main-thread, synchronous and skip-equal, with no debounce, so the read-modify-write `saveSettings(settings().copy(soundOn = on))` cannot interleave with a save.
- **Compose 1.12.1 makes sound and haptics by itself** (javap).
  - **The click sound.**
    - `AbstractClickableNode.performClick()` = `playClickSound(); onClick()`.
    - `playClickSound()` runs only if `ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled`, which is set `true` in its static initializer. It then calls `currentValueOf(LocalSoundEffect).playClickSound()`.
    - `ComposeViewContext.getSoundEffect()` returns `AndroidSoundEffect(view)` when `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled`, also initialized `true`. `AndroidSoundEffect.playClickSound()` is `View.playSoundEffect(0)` (`SoundEffectConstants.CLICK`).
    - The touch path `ClickableNode.handleUpEvent(PointerInputChange)` calls `performClick()`. `CombinedClickableNode` calls `playClickSound()` at three sites, and `ToggleableNode extends ClickableNode`.
  - **The long-press haptic.** `combinedClickable-f5TDLPQ$default` sets `hapticFeedbackEnabled = true` (bit 64 → `iconst_1`). `CombinedClickableNode` then calls `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)`. `BrowseTopBar.kt:163` (›, with `onLongClick = controller::nextUnsolved`) relies on that default.
- **The `play` event table is complete.**
  - Every intent in `PlaySession` / `GestureMachine` maps to the design's table:
    - `beginDrag` (it can no-op);
    - `tapTray`, `flipTray`;
    - `tapBoard` / `flipBoard` → `refit` (success, or a silent shake);
    - `setDragTurn`, which is called on **every** twist move, so "only when the turn changes" matters;
    - `release` → `Locked` / `Home`, or `interruptDrag` when there is no layout or frame;
    - `solveByAid`, `interruptDrag`, `restore`, `toProgress`.
  - The prototype plays a `nope` sound on a refused turn or mirror (`prototype_template.html:622, 673`); the design rightly makes it silent (REQ-033 "No sound signals failure").
- **After a reset.** `BrowseController.persist()` captures `host.session`, which `showAt(0)` has already replaced. So no path, including a late `onChanged` from the discarded session, can write the erased board back (N3).
- **The texts.**
  - The REQ-009 Statement and REQ-049 Rule equal the design's English byte for byte, with a straight apostrophe (0x27).
  - The design's Finnish note, privacy text and nine labels equal `prototype_template.html` lines 182–190 (VERSION 0.6) with the `<b>` tags removed, which is what F14 adopted.
  - The design's new strings and its how-to were run by hand against `PromiseWords` (whole word and prefix): no hits.
- **Release.**
  - `ReleaseSeparationTest` hard-codes `PRODUCT_MODULES = listOf("app", "play", "browse", "kernel", "contracts", "content", "store")`.
  - V-08's `DEBUG_ONLY` is an exact-descriptor list.
  - V-04 (strings glob), V-05 (module discovery) and V-06 (already lists `settings`) need no change.
  - No `settings` string of 6 or more characters equals a `devtools` value, so V-04's DA-90 exclusion set is unaffected.
  - `app/src/test/.../held/HeldSolveNowStoreTest.kt:43` uses the internal `SessionHost(newSession, restoreInto)` constructor.
- **The top bar at 360 dp** (estimate; no device run).
  - Widths: ‹ 52 + counter about 50 + › 52 + ⚙ 48. That leaves a centre column of about 150 dp, less 8 dp of padding.
  - The Finnish state row (5 dots = 52 dp, an 8 dp gap, "ratkaistu ✓" at 13 sp, about 65–75 dp) is about 125–135 dp, so it fits; the title ellipsizes as it does today. The design's risk note and contingency are adequate.

## Trajectory & quality

- **Verification actually run?** Mostly.
  - The store, contract and fixture claims were checked against the code and hold exactly.
  - The REQ text claims hold.
  - The completeness claim of §3.2 was never checked against what the UI toolkit itself does. One `javap` (or a read of the Compose release notes) would have shown the click sound and the long-press haptic.
- **Proportionate?** Yes. It is a small module, the gate is one class, and most of the cost is evidence, much of it extended WO-006 walks. The probe is the minimum seam for the end-to-end claim. The `PlayEvent` / `FeedbackEvent` pair is justified by G-06.
- **Path sane?** First pass. The author flagged the right section and asked to be attacked there, and the gaps are concrete.

## Findings

| ID | Sev | Section | Finding | Rule / REQ | Required change |
|---|---|---|---|---|---|
| F1 | **B** | §3.2 "Nothing else in the code base may produce sound or vibration … That is what makes the gate complete"; §3.6; Risks bullet 1; DA-116; CA-9(d) | **The gate is not the only path to a sound or a haptic. Compose itself makes both, and no planned check can see it.** Re-derived from the resolved libraries (BOM 2026.09.00 → foundation and ui 1.12.1): <br>• **A click on every tap.** `AbstractClickableNode.performClick()` plays the click sound before `onClick`. Two flags gate it, `ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled` and `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled`, and both are initialized `true`. The sound comes from `LocalSoundEffect` → `AndroidSoundEffect` → `View.playSoundEffect(SoundEffectConstants.CLICK)`. `ClickableNode`, `CombinedClickableNode` and `ToggleableNode` all take this path. So, with the game's sound off, these taps ask Android for a click: <br>  – ‹, ›, the counter; <br>  – Restart, Retry, Next; <br>  – a grid cell, grid Done; <br>  – the new ⚙, Done, sound switch, Reset, Keep and Erase. <br>The click is audible whenever the device's "Touch sounds" setting is on, which is on by default in AOSP. <br>• **A vibration on a long press.** `combinedClickable`'s `hapticFeedbackEnabled` defaults to `true`, and `CombinedClickableNode` then performs `HapticFeedbackType.LongPress` through `LocalHapticFeedback`. `BrowseTopBar.kt:163` (the long press on ›, REQ-050) uses that default. No REQ asks for this vibration. <br>• **Every layer of evidence is blind to both.** Neither path contains a name the scan lists, and neither reaches `SoundOut` / `HapticOut`. So the scan, `FeedbackGateTest` and the probe-based `HeldSoundOffAppTest` all pass while, with sound off, a tap clicks and a long press on › vibrates. REQ-033 A1 ("no action makes a sound or a haptic tick") would fail in the shipped game with green evidence. <br>• **The reading becomes a REQ change.** DA-116 / CA-9(d) read A1 as "zero requests at our two outs". Kept with these paths open, that reading narrows a locked acceptance criterion, which is a row-14 question for Jami, not a row-12 reading. <br>• **A name scan cannot see library defaults**, and a BOM bump can add more. | REQ-033 A1 and Statement ("SHALL play no sound WHILE sound is switched off"); governance rows 12 / 14; D4 | **(a) Inventory.** List the platform-default feedback in §3: the Compose click sound, the long-press haptic, and `AndroidComposeView`'s focus-navigation sounds (hardware keyboard). Decide each as a row-12 decision. D4 suggests suppressing them, since no REQ asks for a click or a long-press vibration. <br>**(b) Close them with one root lever** that also covers future library defaults. <br>• Primary option: provide `LocalSoundEffect` and `LocalHapticFeedback` at the root of `TangramApp`, either as no-ops or as instances that go through `Feedback`. Both nodes read them with `currentValueOf`, so a root provider covers every node in our content. <br>• The navigation sound uses the view directly. Set `isSoundEffectsEnabled = false` on the view Compose binds (`ComposeViewContext`'s view; verify which one it is). `ViewHapticOut` uses `performHapticFeedback` on the decor view and is unaffected by the sound flag. <br>• Belt: `hapticFeedbackEnabled = false` on ›. <br>**(c) Put the lever into the evidence.** <br>• The scan (F2) allows the lever's names only in that one file. <br>• The device test asserts the lever on every walked screen, for example that `LocalSoundEffect.current` and `LocalHapticFeedback.current` read at a leaf are the root's instances. <br>• If the platform paths are routed through the gate instead, the probe counts them as kinds of their own, with a positive control. <br>**(d) Rewrite DA-116 and CA-9(d)** to say what "every action" covers, platform defaults included. <br>Optional: at the first device step, probe `AudioManager.getActivePlaybackConfigurations()` (API 26, no permission) as a real-output oracle beside the request count. |
| F2 | **S** | §3.2 (the scan); the seam table (no row); the scaffolding list | **The scan as written does not make `Feedback` the only path, even inside our code.** <br>• **The outs can be called around the gate.** They are public and held as `AppViewModel` fields (`haptics: ViewHapticOut`, and the sound out kept for `prepare` / `release`). A call such as `model.haptics.tick()` or `sound.play(e)` from `app` bypasses the gate and contains no scanned name. The probe sees it only if the bypass calls the wrapped instance during a test action. <br>• **The two scans contradict each other.** `Vibrator`, `VibratorManager` and `VIBRATE` are listed as allowed in the two output files, yet they need the permission and `PromiseSourceScanTest` is to forbid them everywhere. <br>• **It is unplaced.** No module, no scanned trees (`*/src/main/**`, `app/src/release/**`, `app/src/debug/**`), no Gradle inputs (DA-88) and no delivering task are given. <br>• **Its tag is wrong.** `// guardrail G-01` is not what it pins; it pins the gate (DA-116 / DA-123). | REQ-033 A1; DA-88; AGENTS seams and token lessons | Specify three lists and one call rule: <br>(i) **Forbidden everywhere:** `Vibrator`, `VibratorManager`, `VibrationEffect`, `VIBRATE`, `MediaPlayer`, `ToneGenerator`, `Ringtone`, `RingtoneManager`, `MediaActionSound`, `playSoundEffect`, `SoundEffectConstants`. <br>(ii) **Allowed only in the two out files:** `AudioTrack`, `AudioAttributes`, `SoundPool` (the fallback), `performHapticFeedback`, `HapticFeedbackConstants`. <br>(iii) **Allowed only in the F1 lever file:** `LocalSoundEffect`, `LocalHapticFeedback`, `isSoundEffectsEnabled`, `isHapticFeedbackEnabled`, `hapticFeedbackEnabled`. <br>(iv) **The call rule:** `SoundOut.play` and `HapticOut.tick` are called only from `Feedback.kt` and `FeedbackProbe.kt`. Alternatively, make the gate the only holder of the outs. <br>Name the module, the trees and the declared inputs, give the scan a seam row with a task, and tag it `// decision DA-116`. |
| F3 | **S** | §5 "The open puzzle and the board"; Alternatives "Which puzzle shows after reset"; DA-120; CA-10(a); owner item 2 | **The first-puzzle choice is argued without the locked text that pulls the other way.** <br>• DA-58 left this question as "REQ-032 vs the prototype". REQ-032's Statement says "WHEN it is closed, the game SHALL return to the same puzzle state". <br>• F25 was accepted at G1 (`req_review_01.md`). It already records the literal conflict between REQ-032 A1 and REQ-034, proposes "CHG A1: …unless Reset all progress was confirmed" plus a rule for which puzzle shows, and is already in CA-3. <br>• DA-120's basis cites REQ-034, REQ-045 A1 and the prototype. But REQ-045 A1 is about a fresh *install*, which a reset is not: settings are kept. The basis names neither REQ-032's Statement nor F25. <br>• "Stay on the current puzzle, now New" is the more literal reading of that Statement, and it is the D2 option: no `lastShown` write, no index change. The Alternatives row dismisses it on the REQ-034 rationale alone. | REQ-032 Statement; REQ-034 rationale; F25 (G1); DA-58; D2; governance row 12 | • Put REQ-032's Statement and F25 into DA-120's basis and the Alternatives row, and weigh "stay" against "first" on the locked text. Either is a legitimate row-12 reading once it is argued. <br>• State the A1 reading in §2 and in the A1 tests: a confirmed reset is excluded (F25). <br>• CA-10(a): cite F25 / CA-3, or keep it only as the concrete rule text for that CHG. <br>• Phrase owner item 2 as the two options. |
| F4 | **S** | §3.1 "its internal `newSession` seam gains the callback parameter (the `SessionHostScaffoldingTest` adapter is updated by the implementer)"; seam row A-1 | **The seam change has an unnamed casualty, and it is an acceptance test.** <br>• `app/src/test/.../held/HeldSolveNowStoreTest.kt:43` builds `SessionHost({ p, changed -> PlaySession(…) }) { s, pr -> s.restore(pr) }` through the same internal constructor. <br>• That test carries `REQ-046.A3` (WO-005's moved-in held-out test). <br>• Changing the lambda's arity breaks the JVM build. The cut would then have P-1 or A-1, both implementers, edit an acceptance test. | AGENTS seams lesson ("every frozen seam names the task that delivers it"); implementers never author acceptance verdicts; DA-62 pattern | • Name both consumers. <br>• Then either keep the old shape source-compatible (for example a secondary internal constructor that adapts `(Puzzle, () -> Unit) -> PlaySession` with a no-op event callback), or assign the adapter-only edit of `HeldSolveNowStoreTest` to the Test Author (T-1c) as a corrected check, with the diff read by the orchestrator. |
| F5 | **S** | §3.3 `AudioTrackSoundOut`; §3.6 item 4; Risks bullets 2 and 3; the S-2 done-check | **The output's threading and lifecycle contract is unfrozen, and its device check cannot fail.** <br>• **Publication and release.** `prepare()` runs on a background thread while `play()` and `release()` run on the main thread. Nothing states how the five tracks are published, nor what happens when `onCleared()` → `release()` arrives before `prepare()` ends: the tracks are then built after the release and leak. <br>• **The leak would pile up in tests, unseen.** The app device suites create one `AppViewModel` per launch, dozens per channel, all in one test process, and AudioFlinger limits the tracks one uid may hold. Every leak and every lost track stays invisible, because the probe counts requests. <br>• **The smoke test cannot fail.** `AudioOutSmokeScaffoldingTest` "asserts … that the tracks initialized", but the Risks say a track that does not initialize is skipped and the test "records 'unobservable' rather than failing". As written, the S-2 done-check passes in every case. <br>• **A failed replay is silent.** `reloadStaticData()` reports failure by its return code, not by an exception, so `runCatching` misses it. That is exactly the device-specific behaviour the fallback is for. <br>• **The low-latency flag will not be honoured.** `PERFORMANCE_MODE_LOW_LATENCY` at 44 100 Hz is normally refused on a 48 kHz output, the emulator's included. | G-10; AGENTS staffing lesson (device-only done-checks); REQ-033 Statement | Freeze the contract in §3.3 and in the seam row: <br>(1) **Publication:** an immutable map behind one `@Volatile` field. <br>(2) **Release:** `release()` sets a flag that `prepare()` checks before publishing, and `prepare()` then releases whatever it built. <br>(3) **Replay:** `play()` checks `reloadStaticData() == SUCCESS` and counts failures (`internal`, for the smoke test). <br>(4) **Sample rate:** the native output rate, or 44.1 kHz without the low-latency flag. <br>Make the smoke test able to fail on the two known AVDs: first confirm that both have an audio output, then assert initialization and that build/release leaks nothing (for example 20 prepare/release cycles, then a track still initializes). Where a channel truly has no audio, write a named waiver; never a silent pass. |
| N1 | N | §6 Finnish bullet and "Other strings"; DA-121, DA-122, DA-119; CA-10(b)(d); owner items 3–4 | **Three rows re-decide what is already decided.** <br>• **F14** was accepted at G1 and is binding per architecture.md's header. It adopts prototype 0.6's Finnish strings verbatim, "incl. REQ-009/049". It also says the how-to-play text is written by the build and listed for review. Its CHG is already in CA-3. <br>• The design's Finnish texts and labels *are* those strings (re-derived, lines 182–190), so they are not "an AI-written text, DI-1", and CA-10(b) re-files F14. <br>• **DA-53** (WO-004) already ruled "WO-007's ⚙ needs 48 dp too". <br>• **The confirmation question undersells.** "Really erase all solved puzzles and times?" / "…kaikki ratkaisut ja ajat?" leaves out the in-progress boards, which are erased too. It is F14 text, so change it only through the owner. | decisions F14, DA-53; architecture.md header | • Cite F14 in DA-121 and DA-122. <br>• On the owner list, mark the two texts and the prototype labels "accepted at G1 (F14)", apart from the AI-written how-to. <br>• Fold CA-10(b) into CA-3. <br>• Make DA-119 a pointer to DA-53; the `design-inputs.md` §2 line stays. <br>• Put the question's wording on the owner list. |
| N2 | N | Scope "Carried IN"; §3.3 table | **REQ-023's chime is a carried locked rule that the design builds without naming it.** <br>• WO-003 design §3 ends "The sound is WO-007". REQ-023 asks for "a 4-note chime … with it", and F26 (G1) reads the chime as at most 1 s. <br>• The SOLVE cue (523 / 659 / 784 / 1047 Hz, about 570 ms) meets both. <br>• A completing drop fires LOCK and SOLVE together, so two tracks mix to a peak of about 0.47, above the stated 0.35. | REQ-023 rules; F26; WO-003 design §3 | • Add "REQ-023 rule (4-note chime), carried from WO-003" to the Scope, the carried table and `SoundSynthTest`'s basis. <br>• Either state the 0.35 cap as per cue, or delay or soften SOLVE after LOCK. |
| N3 | N | §3.1 emission points; §5 "A late `onChanged` … cannot happen" | **(a) The order of emit and save.** `release()`, `tapTray` and the other intents end in `settled()`, which runs `onChanged` → `persist` → an atomic file write on the main thread. An event emitted after `settled()` puts disk I/O between the touch and the sound. <br>**(b) The wording of the invariant.** The no-persist invariant is stronger than stated: `BrowseController.persist()` captures `host.session`, which `showAt(0)` has already replaced. So even a late `onChanged` from the discarded session saves the new, New session. | DA-49; REQ-033 Statement | (a) State "emit, then `settled()`" in §3.1 and in `PlayEventTableTest`. <br>(b) Rephrase the §5 bullet to rest on `persist()` reading `host.session`. |
| N4 | N | §2 opening; DA-118; REQ-032 A1 | **Two input edges under the overlay** (the grid overlay has both today): <br>• **A press that has not yet become a drag.** A finger pressed on a piece but not yet moved 12 dp is not `isDragging`. A second finger can tap ⚙; when the first finger then moves, its pointer stream stays with `PlayArea`, so a drag, a lock or a return happens under the open sheet. <br>• **A hardware keyboard.** `clearAndSetSemantics` hides the base from TalkBack, but not from keyboard focus: Tab and Enter can reach ‹ and ›. | F5; REQ-032 A1 | • For the press: either an input-blocked flag on `PlayArea` that `app` sets while an overlay is open (it cancels the gesture machine), or `canOpen` also requires "no pointer down on the board". <br>• For the keyboard: block focus entry into the base while an overlay is open (`focusProperties` on a focus group). <br>• One line in DA-118. |
| N5 | N | Risks "Release safety"; seam rows `DebugAids` and S-0a | **Three release-side loose ends.** <br>**(a) `ReleaseSeparationTest`.** It hard-codes `PRODUCT_MODULES` (seven modules), so the edit is certain, not "if it names modules". It belongs to the Test Author (T-1c), beside S-0a's `a4FileSetS` line. <br>**(b) The release twin.** No check covers its `sound(real)` and `haptic(real)`. If they returned a silent out, the release game would be mute while every check stays green: the device tests run the debug twin, and V-08 looks only for the probe's absence. <br>**(c) The probe's log.** `FeedbackProbe.sounds` is an unbounded list in every debug build that Jami plays. | G-04; DA-72; DA-89 | (a) Make the edit definite and assign it to T-1c. <br>(b) Add to the DA-72 source test that both release methods return their argument. <br>(c) Count per kind, or cap the list. |
| N6 | N | §6 "Verbatim is enforced"; §2 "no difficulty word" | **Reading `Requirements/` at test time is sound.** The locked REQ is the oracle, the inputs are declared (the DA-88 pattern), and a later CHG makes the test red until the string follows. Three details must be pinned: <br>(1) **The extraction.** REQ-009 is the one double-quoted span in `## Statement`; REQ-049 is the span after `in full: ` in `## Rules`. Exactly one match each, else `error()`. <br>(2) **The unescape.** The JVM test reads `strings.xml` without aapt, so it must unescape exactly as aapt does (`\'`, `\"`, `\n`, `\uXXXX`, XML entities). <br>(3) **The difficulty-word match.** Plain substrings will hit legitimate words later (`taso` in *tasolla*, `level` in *levels*); use `PromiseWords`' whole-word and prefix marking. | AGENTS "adapters fail loudly"; DA-88; DA-106 | Write the three rules into §6 and into seam rows S-0a and T-1c. |
| N7 | N | Acceptance table REQ-034 A1; §3.6 item 3 | **Two token readings are weaker than they look.** <br>**(a)** `ResetFlowTest`'s `REQ-034.A1` asserts the call order ("`resetAllProgress()` once, then `onReset()`"), not "every puzzle shows as New". That is acceptable only as a recorded engine-level reading (architecture §5 item 4), or with a seeded fake store in which every puzzle reads `NEW` afterwards. <br>**(b)** "A `recreate()` with sound off asserts it stays off" proves nothing about persistence: the ViewModel survives `recreate()`. | AGENTS "test tokens are coverage claims"; REQ-010 rule | (a) Record the reading in the WO, or strengthen the test. <br>(b) Use close and relaunch, so that a new `AppViewModel` re-reads the store, to prove the read path. |
| N8 | N | Alternatives "Sound production" (b); Notes for next station item 3 | **The `SoundPool` fallback would break O-03.** It writes generated WAVs to `cacheDir`, so a second module would touch app storage, against O-03's letter ("only `store` touches persistent storage"), and it adds a file to clean up. | O-03; D2 | Name O-03 in the fallback, or choose one that needs no file: a short-lived `MODE_STATIC` track per play, released after `durationMs`, or `MODE_STREAM` on the existing thread. |

### Answers to the brief's questions not covered above

**1. The REQ-033 A1 evidence chain.**
- **The five emission points** are complete and correctly placed (re-derived above). The one subtlety is that `setDragTurn` runs on every twist move, and the design already says "that changes the drag's turn".
- **The probe is honest evidence of what it claims.** It wraps the real outs at construction, it has a per-kind positive control, and the switch is flipped through the real UI. It says so plainly that a headless run cannot hear.
- **What it cannot do** is prove that no other path exists. That was the scan's job, and F1 and F2 are where it fails.

**2. Release.**
- **Nothing debug-only can reach release.** `FeedbackProbe` sits in `app/src/debug`, and the release twin only passes through.
- **What must change:**
  - V-08: `Lio/github/jamisuni/tangram/FeedbackProbe;` joins `DEBUG_ONLY` (V-1).
  - `ReleaseSeparationTest`: `PRODUCT_MODULES` gains `settings` (N5), and `a4FileSetS` gains `settings/src/main/**` (S-0a).
  - V-04, V-05 and V-06: no change.
- **The merged manifest gains no element.** `settings` has no manifest. `AudioTrack` needs no permission (`MODIFY_AUDIO_SETTINGS` is for routing and effects only). `View.performHapticFeedback` is performed by the system and needs no `VIBRATE`. In release mode V-08 flags any permission element.

**3. Persistence.** All verified (Re-derived):
- the v1 round trip, the frozen fixture and the contract are untouched;
- the read-modify-write cannot race, because the store is main-thread only with no debounce;
- "reset keeps settings" is exactly what the store and its KDoc do.

**4. The overlay.**
- **Back.** One `BackHandler`, composed after the grid's. The grid and settings cannot both be open, because each covers the other's opener.
- **Semantics.** Cleared on the base.
- **Rotation.** The state lives in the ViewModel, and `onCreate` already interrupts a drag (C10 fine).
- **Board unchanged (A1).** True by construction, apart from N4.

**5. Reset.**
- The two-step confirmation is in-screen, Keep comes first, and the confirm is idempotent.
- The store's reset is one atomic write; `afterReset`'s `lastShown` write is a second one, and a kill between them only leaves the old (now New) puzzle shown.
- `afterReset` cannot resurrect the board (N3).
- Cancel makes no store call.
- The first-puzzle choice is a row-12 reading that needs F3's basis; it is not a REQ change.

**6. Texts.**
- Reading `Requirements/` at test time is sound (N6).
- The Finnish is F14 text accepted at G1, not AI text (N1).
- `PROMISE_TEXT_KEYS` with the two keys, each cited, is right.

**7. The ⚙ and the `trailing` slot.**
- 48 dp is right; REQ-037 already won in DA-53.
- The `trailing` slot is the seam O-09 names, placed after ›, as in Spec/02 §2.
- The 360 dp budget fits (Re-derived).

**8. Audio.**
- The API levels are right: `AudioTrack.Builder` (23), `setPerformanceMode` (26) and `CLOCK_TICK` (21) need no version branch.
- No audio focus is the right call.
- Threading, release, `reloadStaticData` and the sample rate: F5. The fallback: N8.

**9. Scope.**
- **Carried parts.** C1, C2, C3, C5, C7 and C10 are complete and on the right tokens. The new walk screens, the 48 dp rule for `settings-*` and the canaries are all in §7. Add the REQ-023 chime (N2).
- **The WO-008 carry-out** is right. Note that "paused while open" is a REQ-032 rule, so the close must write it into WO-008's row as carried in (the design says so).
- **The held-out choice is sound.** The probe is exercised visibly through C7's tick request.
- **Seams.** Every seam names a task except F2's scan, F4's casualty and N5(a).
- **Rule 3** holds.
- **DA and CA rows.** No row is a hidden locked-text change; the English is verbatim. DA-116 becomes a hidden REQ-033 A1 change if F1 is "resolved" by narrowing. DA-120 needs F3. DA-119, DA-121 and CA-10(b) duplicate DA-53, F14 and CA-3 (N1).

## What is good and must be kept

- **The v1 finding.** `settings.soundOn` already exists, so there is no format change, no migration and no contract delta. The row-11 table proves it against the code, and the alternatives (DataStore, v2, memory only) are rejected for the right reasons.
- **Synthesis plus `AudioTrack`, and `performHapticFeedback(CLOCK_TICK)`.** No asset, no licence, no network, no permission, no new library. The tones are presented as tunable constants, and the owner's ear is the judge.
- **One gate in front of both outs.** The switch is read in exactly one place, with a JVM positive control, a per-kind positive control on the device, and the plain statement that the real audio is owner evidence.
- **A refused turn or mirror is silent**, departing from the prototype's `nope` sound because REQ-033 says so.
- **The overlay, not a Dialog**, for REQ-008 A2's single-root walk. The state is in the ViewModel, Back goes through `BackHandler`, the base semantics are cleared, and touches are swallowed.
- **The reset path.** In-screen two-step, Keep first, idempotent `confirmReset`, and the `afterReset` no-persist invariant with its `onPause` test.
- **Verbatim English enforced by a test that reads the locked REQ.** `PROMISE_TEXT_KEYS` holds exactly the two keys with citations, nothing else is exempt, and the privacy text has no click action.
- **The carry-out to WO-008 is decided, not "expected"**, with no placeholder rows or slot parameters (D3/D4). `isOpen` is the pause signal it already is.
- **Carried parts are re-verified by extending the existing walks**, never by a new weaker test. The kit learns a scrolling screen (`performScrollTo`, layout bounds).
- **Release.** A debug-only probe through the existing `DebugAids` pair, V-08's debug-only list extended, `settings` without a manifest, and V-06 already allowing it.

## Owner items (Jami)

Nothing needs Jami before rev 1. For the WO-007 checkpoint:
1. **The platform click and the long-press vibration (F1).** Today, with the game's sound off, Android's own touch click still sounds on every button, and a long press on › vibrates. The design will either silence them or route them through the sound switch. If rev 1 chose to keep them while sound is off, that would change REQ-033 A1 and would need his yes.
2. **After a reset (F3):** show the first puzzle (the design) or stay on the current puzzle, now New (closer to REQ-032's "return to the same puzzle state").
3. As the design lists: the tones and whether the tick can be felt; the Finnish how-to (F14 already covers the note, the privacy text and the prototype labels, N1); the ⚙ at 48 dp.

## Handoff — Design Reviewer · WO-007

- **Scope:** REQ-032 A1–A2, REQ-033 A1, REQ-034 A1–A2, REQ-009 A1, REQ-049 A1; carried in: the WO-001 sound and tick, and WO-006 C1, C2, C3, C5, C7 and C10.
  - Governed: `IProgressStore` is consumed unchanged and `GameSettings` is unchanged.
  - The v1 format is untouched (re-verified against the encoder, the decoder, the fixture and `FrozenV1FixtureTest`).
  - No contract delta.
- **Inputs read:** see the header (design rev 0; the working tree after the WO-006 close; the resolved Compose 1.12.1 bytecode).
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-007-design-review.md`
- **Status:** recirculate → design-author (1 Blocker / 4 Shoulds / 8 Notes). The Blocker and the Shoulds, one line each:
  - **F1 (B):** Compose 1.12.1 plays the system click sound on every clickable or toggleable tap and a long-press haptic on ›. Neither reaches the gate, the scan or the probe, so REQ-033 A1 would break with green evidence. It needs a root lever (composition locals or view flags), evidence for that lever, and a rewritten DA-116.
  - **F2 (S):** the scan lists names, not paths. The outs can be called around the gate, the Vibrator lists contradict `PromiseSourceScanTest`, and the scan has no module, trees, Gradle inputs or task, and the wrong tag.
  - **F3 (S):** DA-120 (first puzzle after a reset) ignores REQ-032's "return to the same puzzle state" and G1's F25. D2's "stay" is not weighed. A1's reset exclusion is not stated.
  - **F4 (S):** changing `SessionHost`'s internal `newSession` breaks `HeldSolveNowStoreTest` (REQ-046.A3), which the design does not name; an implementer would edit an acceptance test.
  - **F5 (S):** the `AudioTrack` contract (publication, the prepare/release race, the `reloadStaticData` return code, the sample rate) is unfrozen, and the smoke test, as written, cannot fail.
- **Traceability delta:** none (review only). Rev 1 will bring:
  - F1: a root-lever seam in A-1, scan and device-test changes, and DA-116 / CA-9(d);
  - F2: a seam row for the scan;
  - F4: the `SessionHost` seam shape or a T-1c adapter edit;
  - F5: the `AudioTrackSoundOut` contract and a fail-capable smoke test;
  - N5: the `ReleaseSeparationTest` edit in T-1c.
- **Notes for next station:**
  - Fix F1 first, because it changes DA-116, the scan (F2) and the held-out test's scope. Then F4 and F5, which are seam shapes the Planner needs, then F3, which changes an owner item.
  - A rev 1 re-review can be a spot-check of: §3 (3.1, 3.2, 3.3 and 3.6), §5, §6, the seam table, the acceptance and scaffolding lists, DA-116, DA-119, DA-120, DA-121, CA-9 and CA-10, and the cut.
  - The reviewer's evidence is in `C:\Users\Jami\AppData\Local\Temp\claude\C--GitHub-AI-TangramNoAds\9c42d4d4-131b-4b08-a0c2-faa3e9f21c46\scratchpad\`: `fnd/` and `ui/` hold the unpacked foundation and ui 1.12.1 classes. Read them with `"C:\Program Files\Android\Android Studio\jbr\bin\javap.exe" -p -c` on `androidx/compose/foundation/AbstractClickableNode.class`, `ComposeFoundationFlags.class`, `CombinedClickableNode.class` and `ClickableKt.class`, and on `androidx/compose/ui/platform/AndroidSoundEffect.class`, `ComposeViewContext.class` and `androidx/compose/ui/AndroidComposeUiFlags.class`.

## Spot-check of rev 1

**Date:** 2026-10-04
**Read:** `designs/WO-007-design.md` rev 1 in full, with weight on:
- the rev 1 paragraph;
- §2, §3.1–3.7, §5 and §6;
- the Alternatives rows;
- the seam table and value shapes;
- the acceptance and scaffolding lists;
- the Risks;
- DA-113…DA-125;
- CA-9 and the CA-10 withdrawal;
- the cut.

No Gradle, emulator or git was run, and `.swdev/heldout/` was not read.

**Re-derived for this check:**
- **A whole-APK call-site inventory.** `dex_callers.py` in the session scratchpad is about 100 lines of stdlib Python. It parses `class_defs` → `code_item` → `invoke-*` in every `classes*.dex` and lists every caller of the platform sound and haptic APIs. It was run on today's `app-release-unsigned.apk` and `app-debug.apk` (WO-006 close, not minified, the same BOM). Both give the same set:
  - `View.playSoundEffect` is called only by `AndroidSoundEffect.playClickSound` and `AndroidComposeView$AndroidComposeViewNavigationSoundEffect.invoke`.
  - `View.performHapticFeedback` is called only by `ViewCompat.performHapticFeedback`, which is called only by `PlatformHapticFeedback` and `DefaultHapticFeedback`.
  - Nothing calls `Vibrator.vibrate`, `AudioManager.playSoundEffect` or any `android.media` playback class. The other `android.media` hits are `ImageReader` (layer snapshots), `MediaDrm` and `ApplicationMediaCapabilities` (activity-result helpers) and `NotificationCompat` audio attributes: none plays anything.
  - `HapticFeedback.performHapticFeedback` is called by `CombinedClickableNode` (`handleDownEvent$1`, `handleDownEvent$2` **and `handleDeepPress`**) and by 7 text-selection sites.
  - `SoundEffect.playClickSound` is called only by `AbstractClickableNode.playClickSound` and `DelegatingSoundEffect`.

  So the design's six-row inventory is right for the whole APK, not only the two jars: androidx.core, activity and the rest add no other path. Two details are missing: the deep-press haptic (E3), and the per-window re-provision below (E1).
- **Which view Compose binds.**
  - `ComposeViewContext.getSoundEffect()` builds `AndroidSoundEffect(view)` and its `PlatformHapticFeedback(view)` from the same `ComposeViewContext.view`.
  - `AbstractComposeView` sets that field to `findViewTreeComposeViewRoot(this)`, which walks **up** to the depth of the `view_tree_lifecycle_owner` / `view_tree_saved_state_registry_owner` tags. Under `ComponentActivity.setContent` those tags sit on `window.decorView`.
  - So the click sound and Compose's haptic are performed on the **decor view**, the same view `ViewHapticOut` ticks on. It is not the `AndroidComposeView` (E2).
- **Per-window locals.**
  - `WrappedComposition` (the composition of every `AbstractComposeView`, so of every Dialog and Popup window too) calls `ComposeViewContext.ProvideCompositionLocals$ui`.
  - That call re-provides `LocalSoundEffect` (`providesComputed`) and, through `ProvideCommonCompositionLocals(owner, …)`, `LocalHapticFeedback`. So a root provider does **not** reach into a Dialog (E1).
- **The flags.**
  - `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled` is `public static` (`@JvmField`, `@ExperimentalComposeUiApi`). It is read by `ComposeViewContext.getSoundEffect` (cached on the first compute), by the navigation effect (at every `invoke`, the first instruction) and by `SoundEffectOnInteraction`.
  - `ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled` is read only by `AbstractClickableNode.playClickSound`.
  - `SoundEffect` and `HapticFeedback` are single-method interfaces (`playClickSound()`, `performHapticFeedback(type)`), so the silent objects are complete.
- **Focus.**
  - `FocusProperties` in ui 1.12.1 has `canFocus`, `enter`/`exit` and `onEnter`/`onExit` (`FocusEnterExitScope`).
  - `canFocus` applies to the one focus target it decorates, and `focusGroup()` is itself a non-focusable target. So `focusGroup().focusProperties { canFocus = false }` leaves every child focusable (E4).
- **Today's sources against the scan.** No `.play(`, `.tick(`, `SoundEffect`, `HapticFeedback`, `AudioFormat` or `AudioAttributes` occurs in any `*/src/{main,debug,release}`, so the scan has no day-one false red.

### The author's two disputes

- **(a) View flags rejected: the conclusion is accepted, but the stated reason is wrong.**
  - The design says the click is played on "the unreachable `AndroidComposeView`". The bytecode binds both `AndroidSoundEffect` and `PlatformHapticFeedback` to the view-tree root, which is the decor view here, and the app can reach it.
  - So `decorView.isSoundEffectsEnabled = false` *would* silence the click.
  - The provider is still the better lever, for three correct reasons:
    1. a decor-view `isHapticFeedbackEnabled = false` would also silence the app's own lock tick, because `ViewHapticOut` uses that same view;
    2. a view flag gives no positive control, while the provider's counters do;
    3. the provider is the read point of every node path the inventory shows.
  - Fix the text (E2).
- **(b) Observability: accepted in part.**
  - I agree that the platform's own `playSoundEffect` has no robust on-device observer, and that the lever's interception counters plus the flag read are the right device evidence. They prove that each tapped node asked and that our provider answered.
  - I do not accept "re-run the inventory by hand at every BOM bump" as the guard. A manual duty in AGENTS.md is exactly the kind that slips, and the inventory can be mechanical.
  - The call-site scan above runs in seconds on the APKs V-08 already builds and reads. Pin today's caller set as an allow-list, and any new caller fails V-08 at the next WO close. That covers a BOM bump, a new library and our own code, and only then does someone re-judge the inventory (E3).

### The other checks asked for

- **The lever's coverage.**
  - **Click sounds** from `clickable`, `combinedClickable` (all three `playClickSound` sites, key-up included) and `toggleable` (`ToggleableNode extends ClickableNode`) all read `currentValueOf(LocalSoundEffect)`, so the root provider answers them inside the activity's window.
  - **The long-press and deep-press haptics** read `LocalHapticFeedback`, so they are covered too.
  - **Focus and navigation sounds** check the ui flag at every call, so setting it false covers every later focus move.
  - **Selection haptics** go through `LocalHapticFeedback`. Covered in the activity window, which has no text field in release; not covered inside a Dialog (E1).
  - **Scroll and gesture haptics:** none in the whole APK.
- **Leaving `isInteractionSoundEffectOnClickEnabled` at its default is safe.**
  - In the activity window every click request reaches the silent provider. That is what makes `interceptedClicks` a real positive control.
  - Outside it (a Dialog or Popup window), the window's own `LocalSoundEffect` is computed lazily, after the ui flag is already false, so it resolves to `NoSoundEffect`: still silent, just not counted.
  - Turning the foundation flag off as well would cost the positive control and gain nothing for release.
- **No new Blocker.** The residues are a wrong claim with a debug-only consequence (E1), a wrong reason (E2), a manual duty that should be mechanical (E3) and a focus lever that does not do what it says (E4).
  - E4 concerns only hardware keyboards (rev 0 N4), and the design already plans a device check that would catch it, so it is not a Blocker.
  - **Nothing breaks REQ-033 A1 in the release build:**
    - release has no Dialog or Popup, and the REQ-008 A2 walk forbids one;
    - every activity-window path is closed;
    - the lock tick is untouched.
- **Release safety holds.**
  - `PlatformFeedbackLever` ships in `main` with two bounded `AtomicInteger` counters (`internal`, test-visible). That is acceptable and not test code in the APK's behaviour.
  - `FeedbackProbe` stays in `app/src/debug` and is reached only through the `DebugAids` pair. The release twin is asserted to return its argument (DA-72 source test), and V-08 denies the probe in release and requires it with `--expect-debug`.
  - `ReleaseSeparationTest`'s `PRODUCT_MODULES` and `a4FileSetS` both gain `settings` (T-1c / S-0a).
  - No permission element, no new library, and `settings` has no manifest.
  - One gap: V-08's stale-APK `INPUT_PATTERNS` claims to mirror `scannedFileGlobs` but lacks the new `*/src/debug/**` (E3).

### Rev 0 findings

| Finding | Status | Evidence / residue |
|---|---|---|
| F1 (platform click sound and long-press haptic) — B | **fixed**, residue | `PlatformFeedbackLever`: root providers of silent `LocalSoundEffect` / `LocalHapticFeedback` with counters; the ui flag set false; suppressed always (DA-125, CA-9(e), owner item 5). DA-116 rewritten, A1 not narrowed. `HeldPlatformFeedbackAppTest` has a positive control per tap. Residue: the Dialog claim (E1), the view reason (E2), the manual inventory duty and the deep-press row (E3) |
| F2 (scan on names, not paths) — S | **fixed** | `FeedbackPathScanTest` (module `app`, three trees, glob inputs, identifier tokens, comments skipped, canaries, `// decision DA-116`): a forbidden-everywhere list consistent with `PromiseSourceScanTest`, two allowed-only lists, and the call rule that `.play(` / `.tick(` occur only in four files. Residue: function references (E5) |
| F3 (first puzzle after reset) — S | **fixed** | "Stay, now New" (`afterReset = showAt(indexState)`, no `lastShown` write), weighed on REQ-032's Statement, F25 and D2; A1's reset exclusion stated in §2, the acceptance table and DA-118; owner item 2 has the two options; CA-10(a) folded into F25 / CA-3 |
| F4 (`SessionHost` seam casualty) — S | **fixed** | `PlaySession.onEvent` is a settable property set in `SessionHost.build`; the internal constructor is unchanged; the constraint is written above the seam table; `HeldSolveNowStoreTest` compiles untouched |
| F5 (`AudioTrack` contract; blind smoke) — S | **fixed** | Native rate (low latency only a hint), one volatile immutable map, release-before-prepare checked three times, the `write` and `reloadStaticData` codes counted. The smoke test has an output-device preflight (or a named row-16 waiver), checks the states and failures, and runs 20 cycles plus 20 immediate releases. A released track can still be read from a just-published map, but `play()`'s `STATE_INITIALIZED` check covers that |
| N1 (F14, DA-53, CA-10(b), reset question) | **fixed** | F14 cited, CA-10 withdrawn, DA-119 is a pointer, the question's wording is on the owner list |
| N2 (REQ-023 chime; overlap peak) | **fixed** | Named in the scope and the carried table; the cap is per cue; SOLVE has a 100 ms lead-in after LOCK's ~90 ms |
| N3 (emit before `settled()`; invariant wording) | **fixed** | Both, with an event-table assertion |
| N4 (press and keyboard under the overlay) | **press fixed; keyboard not effective** | `PlayArea(inputEnabled)` cancels the gesture machine, and the held A1 test presses a second finger. The focus lever and its fallback leave nested controls focusable (E4) |
| N5 (`PRODUCT_MODULES`; release twin; probe log) | **fixed** | T-1c owns the edit; the DA-72 assertion; per-kind counts |
| N6 (extraction, unescape, word match) | **fixed** | Pinned in §6 and §2 |
| N7 (REQ-034 A1 token; `recreate`) | **fixed** | Seeded fake store with real clearing; close and relaunch |
| N8 (`SoundPool` in `cacheDir`) | **fixed** | Dropped; the fallback is a per-play track from memory |

### E-items (line edits; the orchestrator may apply them without another re-review)

- **E1 — §3.7 (the lever bullet and row 4), Risks item (2): a Dialog does not inherit the two locals.**
  - Replace "a Dialog included, since sub-compositions inherit locals" and "a `Dialog` inherits them" with the following facts:
    - every Compose window (a Dialog or Popup has its own `AndroidComposeView`) re-provides `LocalSoundEffect` and `LocalHapticFeedback` at its root (`WrappedComposition` → `ProvideCompositionLocals$ui` → `ProvideCommonCompositionLocals`);
    - a click there is still silent, because the ui flag makes that window's lazily computed `SoundEffect` a `NoSoundEffect`;
    - a haptic there is **not** suppressed.
  - State the lever's precondition, "no Dialog or Popup window in the release build", which the REQ-008 A2 no-dialog walk already enforces.
  - Record the one known residue as accepted: the debug-only DevTools passcode `Dialog` (its `BasicTextField` selection haptics; a REQ-046 testing aid).
  - Set the ui flag **before `setContent`**, through a `PlatformFeedbackLever.installProcessFlags()` called first in `MainActivity.onCreate` (the scan's rule-3 file stays the only home of the name), not in a `remember` inside composition. It then precedes every `ComposeViewContext`, and it is not a side effect of composition.
- **E2 — §3.7 "Why not … view flags", Alternatives row (c): correct the reason.**
  - Compose binds `AndroidSoundEffect` and `PlatformHapticFeedback` to `ComposeViewContext.view`, the view-tree root found by `findViewTreeComposeViewRoot`: the decor view under `ComponentActivity.setContent`, which the app can reach.
  - The rejection stands because:
    1. `isHapticFeedbackEnabled = false` on that view would also silence `ViewHapticOut`'s lock tick (same view);
    2. a flag has no positive control;
    3. the provider is the read point of every node path.
- **E3 — §3.7 row 2 and "Covered by", seam row V-1, Z close, the AGENTS.md line: mechanise the inventory.**
  - Row 2 gains `CombinedClickableNode.handleDeepPress` (a deep press also performs a haptic through `LocalHapticFeedback`).
  - V-1 extends V-08 with a **dex call-site check** on the APKs it already builds or reads. The callers of these APIs must equal a pinned allow-list, and any new caller is `V-08 FAIL feedback-caller …`:
    - `View.playSoundEffect`, `View.performHapticFeedback`, `ViewCompat.performHapticFeedback`;
    - `AudioManager.playSoundEffect`, `Vibrator*`;
    - `AudioTrack.play` and the `android.media` playback classes (`SoundPool`, `MediaPlayer`, `ToneGenerator`, `Ringtone`);
    - the Compose interfaces `HapticFeedback.performHapticFeedback` and `SoundEffect.playClickSound`.
  - The allow-list is today's set (above), plus `ViewHapticOut`, `AudioTrackSoundOut` and the lever's silent objects. Self-test: a crafted fixture with one extra caller fails.
  - Starting point: `dex_callers.py` in the session scratchpad.
  - V-08's `INPUT_PATTERNS` gains `^[^/]+/src/debug/.+`, to keep its "same list as `scannedFileGlobs`" promise.
  - The close's AGENTS.md line then reads: "V-08's feedback-caller check fails on any new sound or haptic caller (a Compose BOM bump included); re-judge DA-125's inventory before extending its allow-list".
- **E4 — §2 (keyboard), seam row A-1, `SettingsOverlayScaffoldingTest`: use a lever that blocks children.**
  - `focusGroup().focusProperties { canFocus = false }` is a no-op on children: `focusGroup()` is already a non-focusable target, and `canFocus` applies only to that one node. The written fallback (`canFocus = false` on each top-level child) does not reach ‹ and ›, which sit nested in the top-bar `Row`.
  - Use `Modifier.focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()` on the base while open (the API is present in ui 1.12.1), and clear focus on open (`LocalFocusManager.clearFocus()`), because a base control may already hold focus.
  - The scaffolding test focuses ‹ first, then opens the overlay, then sends Tab and Enter, and asserts the puzzle did not change.
- **E5 — §3.2 rule 4: close the obvious textual bypass.** Also match the function references `::play` and `::tick` outside the four files, so `haptics::tick` passed as a callback cannot route around the gate.
- **E6 — §5, "For stay", last bullet: wording.** Replace "the reviewer's ruling stands" with "DA-120 chooses 'stay' under row 12". The review asked for the weighing, not for a particular answer.

**Spot-check verdict: forward**, conditional on E1–E4 being applied before the Planner briefs A-1, V-1 and the Acceptance Test Author:
- **E3 matters most.** It turns the one remaining manual guard of REQ-033 A1 (the BOM-bump inventory) into a verifier failure.
- **E1 and E2 correct two factual claims** the code review and the owner checkpoint would otherwise inherit.
- **E4 prevents a known red at the first device run.**
- **E5 and E6 are small.**

No new Blocker or Should-level design question remains. F1–F5 and N1–N8 are resolved, with the residues listed above.
