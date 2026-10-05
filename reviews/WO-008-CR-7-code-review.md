# WO-008 CR-7 code review (fresh eyes, read-only)

**Reviewer:** CR-7 (fresh context)  ·  **Date:** 2026-10-05  ·  **Scope:** the CR-7 row of `tasks.md` (TASK-060…068, LAND-A, LAND-068, PRE-V08-8, MOVE-JVM8, the in-tree T8a/T8c kit)
**Method:** read the current files (no git, no Gradle, no emulator, no `.swdev/heldout/`). Re-derived from the artifacts, not trusted:
- Every writer of a stored time field, found by grep over all `src/main` trees: `savePlayTime` has one caller (`PlayTimeKeeper.flush`); `resetAllProgress` has one caller (`SettingsController.confirmReset`); the keeper's `flush()` / `accrue()` / `puzzleShown` / `afterReset` callers are exactly the ones the design names (`MainActivity.onPause`, `AppViewModel.onCleared`, the ticker lambda, `SessionHost.show`, `onReset`).
- `computeWithCorner` / `placeSecondary` untouched: `PlayLayout.kt` (2026-10-04 09:47) and `PlaceSecondary.kt` (2026-10-03) predate the first WO-008 edit (2026-10-05 05:43), and neither names `timer` or `WO-008`.
- DA-147's numbers: `PuzzleTimer` = 14 sp, `tnum`, 10 dp + 10 dp padding; the template in `PlayArea` = `"88 h 59 min"`, 14 sp, `tnum`, +20 dp. They match.
- The release/debug split by reading both `DebugAids` twins, `TestConfig` (debug source set only) and `ReleaseSeparationTest`'s pass-through cases.
- The cross-file inputs: `TimeStringsEqualTest` is covered by the `*/src/*/res/values*/strings.xml` glob; `FeedbackScan.PRODUCT_MODULES` and `ReleaseSeparationTest.PRODUCT_MODULES` both list `time`; `a4FileSetS` lists `time/src/main/**`; `UntranslatedStringsTest` finds modules dynamically.
- No CR byte in any `app/src/main` file: the `SessionHost.kt` normalisation left it LF, as the repo requires.

I did not re-run any build; the author's counts (JVM 806/806 and the rest) are unverified by me.

## Verdict: **forward**

0 Blocker / 0 Should / 9 Nit. I attacked the writer lists hardest and found no path that writes a stale `puzzleSeconds` or play time, resurrects erased time, or caches or overwrites a best. The nits are hardening, a guard that would be cheap to add, and two things for the owner's checkpoint-8 look. None needs a design change, and none blocks MOVE-DEV8. N1 (the pill can touch an outline corner) is the only one the owner has to see; the rest can ride CR-7-FIX or be declined with a log row.

## The writer lists (the highest risk), traced

| Path | What I traced | Result |
|---|---|---|
| `open()` | `persist()` (capture, then `mergeInto` for the leaving puzzle, which is still `shownId`) -> `saveLastShown` -> `showAt` -> `host.show` -> `puzzleShown` (adopts, no store call) | Safe. The leaving puzzle's seconds are saved first; the next puzzle adopts the stored value. |
| `restart()` / Retry | writes `restarted()` (0 seconds, best kept) -> `showAt` -> `puzzleShown` adopts 0, clears the pending result | Safe. A later flush can only write the new attempt. |
| `confirmReset` | `store.resetAllProgress()`, `revision++` (snapshot state, no keeper code), then `onReset` -> `time.afterReset()` first -> `controller.afterReset()` -> `showAt` | Safe. `afterReset` writes nothing; it zeroes the carries and `dayState`, re-reads the erased store, resets `lastFlushMs` and `flushOnIdle`. If the store was already empty, `resetAllProgress` returns early, and `afterReset` still zeroes the keeper's unflushed seconds. |
| Aid solve | `solveByAid` -> `onSolved(true)` -> `solvedListener(true)` -> `solved(true)` (freeze, no pending) -> `settled()` -> `persist` -> `mergeInto` keeps `base.bestSeconds` | Safe. No best from an aid solve. |
| Own solve | `release()` -> `solvedListener(false)` -> pending = the frozen seconds -> `settled` -> `persist` -> one `saveProgress` with `min(base best, pending)` | Safe. `base` is read from the store at `persist` time, so a best changed behind the keeper is respected. If the persist fails, the pending result stays and the next persist retries. |
| `onPause` / `onStop` / `onCleared` | `interruptDrag` -> `persist` -> `flush`; `setVisible(false)` (accounts, flushes) -> `store.sync()`; `onCleared`: `ticker.set(false)`, `flush` | Safe. Order is right (flush before sync). After `onStop` the keeper is not visible, so `onCleared`'s flush credits nothing. |
| Process death | seconds since the last flush or settled event are lost (at most 10 s); a New puzzle's first drag is lost until its drop (documented, DA-49) | As designed. |
| A drag during the 10 s flush | `flush()` reads the store, writes `puzzleSeconds` only into a stored In-progress record, never reads the session; the store write is a tmp-file write plus an atomic move (no `fsync` on the main thread) | Safe, and cheap enough on the main thread. |
| A due flush when an input arrives | `account()` has no store path; only `accrue`, `flush`, `setVisible(false)` and `setPuzzleRunning(false)` write | Confirmed against the code, not the author's report. |

Other points checked:
- **`day` only on a credited second (E2):** `credit()` sets or rolls `dayState` only inside `if (seconds > 0)`. `afterReset` ends at `NONE`.
- **Midnight (DA-139):** `todaySeconds` hides a stored day that differs from `source.today()`; the next credited second rolls the day to 0. The total is never reduced.
- **Constructor read:** `readPlayTime()` runs in `init`, after `lastMs` is set; a store failure reads as `NONE`.
- **Ticker lifetime:** `onCountingChanged` fires from `refreshCounting` inside `account()`. It turns off on idle expiry and on `setVisible(false)`, so a stopped Activity runs no ticker. `again` clears `posted` first, so a `set(false)` from inside `onSecond` cannot double-post. `onCleared` cancels it. Idle: no message is pending. While counting: 1 Hz for up to 60 s after the last touch, plus at most six flushes.
- **Backgrounding:** `lastTouchMs` survives a stop. A return inside the 60 s window credits the unused rest without a touch (the pinned `TimerRotationAppTest` case "the last touch was 38 s ago"). Credit stays bounded at 60 s per touch, so REQ-029 A1 holds in every order I tried.

## The touch observer

`pointerInput(time)` on the root `Box`, `PointerEventPass.Initial`, never consumes. The cost per event is one `System.nanoTime`, a pure function, and (at most once per second) a `LocalDate.now()` and a state write; there is no allocation per event. It sees events before every child and runs in the same coroutine scope shape the existing `settings-overlay` swallow uses, so it changes no drag, twist, long-press, grid or settings behaviour on the source. I found no ordering dependency. The device tests (`engage()` = a real `click()`) feed the real observer, not `keeper.touch`.

## `timerRect` and the shared `SubcomposeLayout`

- The Restart rect (`controlRect`) is an obstacle even when Restart is hidden (it is the layout's measured rect); the DEV rect is added only if placed. The step is decided from `max(live, template + 20 dp)`, and the live pill is right-aligned inside the placed rect, so it cannot move within an attempt (the sweep checks this with live widths).
- The slot ids `secondary-corner-control`, `timer`, `timer-template` are unique; `t.width <= 0` (timer off) composes no template; in release the secondary measures 0x0, so `secondaryRect` stays null. Sizes are never written to state, so nothing here can relayout the board.
- Measured widths (DEV-EARLY8 run 3) look consistent: 94.0 / 93.3 dp is the template (73 dp text + 20 dp) at scale 1.0 on two densities; 156.4 dp at fi 2.0 is under the 170 dp bound (nonlinear font scaling); Restart 182.4 dp is under 280.

## Findings

| ID | Sev | File:line | Finding | Rule / REQ | Required change | Owner task |
|---|---|---|---|---|---|---|
| N1 | N | `play/.../PlayArea.kt:303-316`; `TimerPlacement.kt`; `TimerPlacementSweepTest` (recorded count 256 of 2340) | `timerRect` avoids the Restart and DEV rects but not the silhouette. In about 11 % of the sweep (the short boards the design names, under about 320 dp high) the 85 %-opaque pill touches an outline corner. The pill takes no touches, so a drop still works, but the target corner is partly hidden. The design accepted this as an "honest limit" and the WO log marks it for the owner. | REQ-031 A2 (placement), DA-71 principle (clear of the silhouette), DA-140 | Owner decision at checkpoint 8, with the count per area (not only the total) on the surface, and a screenshot of the worst case. Options if declined: fade the pill to about 50 % on touching, or step below the silhouette's bounding corner. No change needed to close CR-7. | CLOSE8 / orchestrator; TASK-065 only if the owner asks |
| N2 | N | `PlayArea.kt:352-356` (`TIMER_TEMPLATE_SP`, `TIMER_TEMPLATE_PAD_DP`, `"88 h 59 min"`) vs `time/.../PuzzleTimer.kt` (14.sp, 10.dp x 2) | The two modules must agree on three literals (`play` cannot import `time`, DA-147). Today they match. The only guard is the device test `TimerLayoutMeasuredAppTest`, so a drift would show only on a device run. | DA-147 ("a shared spec in kernel if they drift"), G-06 | Add a JVM source-scan test (in `app`, which sees both, like `TimeStringsEqualTest`) that the 14 sp, the 20 dp (= 2 x the pill's horizontal padding) and the literal agree. About 20 lines. | Test Author (T8c) |
| N3 | N | `TimerPlacementSweepTest` areas (smallest width 360 dp) | The sweep never goes below 360 dp wide. A 320 dp phone or a "largest display size" setting with Restart at 280 dp and a 170 dp pill is outside the checked space. The fallback is the plain rect, which would overlap Restart. I found no failure by reading; it is just unswept. | DA-140 (a), design 7 | Add one 320 x 560 (and 320 x 480) area to the sweep. If the fallback count is not 0, that is a design finding, not a loosened test. | Test Author (T8c) |
| N4 | N | `time/.../PuzzleTimer.kt:34-37`; `settings/.../SettingsOverlay.kt` (`ValueRow`) | DA-149's merge is right and the nodes stay inert (no click, no role, no pointer input; the pill's `Box` has none). On TalkBack the pill reads as a bare "0:17" with no name; the settings rows read label plus value ("Active today 12:03"), which is sensible. The base semantics are cleared while settings is open, so the pill is hidden behind the overlay as intended. | REQ-008 (usable without sight, spirit), DA-149 | Optional: give the pill a short name in both languages (a new string, so a Finnish text for Jami to confirm; F14). Otherwise log "bare value, accepted". | TASK-064 |
| N5 | N | `PlayTimeKeeper.kt:80` (`touching` cleared only in `setVisible(false)`); `TangramApp.kt` observer | A lost pointer "up" while the Activity stays started (not seen in practice: Android sends a cancel, and Compose turns it into `pressed = false`) would leave `touching = true`, so counting would never idle out. The only reset is `onStop`. Held-finger-as-engaged is DA-134. | TYPE-005, REQ-029 A1, DA-134 | Cheap hardening: also clear `touching` in `MainActivity.onPause`, or when the window loses focus. | TASK-068 |
| N6 | N | `CountingTicker.kt:25-29` (`onSecond()` unguarded); `AppViewModel.kt:51-55` (`onReset` chain) | G-10: nothing throws into the main loop. Today nothing in `accrue()` can throw (the store calls and the listener are caught). But if `onSecond` ever threw, the ticker would die silently: `posted` is false and `schedule()` is skipped. If `time.afterReset()` threw, `controller.afterReset()` would be skipped. | G-10 | `runCatching` around `onSecond()` (and keep `schedule()` in a `finally`); `runCatching` around `time.afterReset()` so the controller call always runs. | TASK-068 |
| N7 | N | `PlayTimeKeeper.kt:152-160` (`flush`) | `flush` compares only the stored record's state. If the session is shown as New over a stored In-progress record (a failed restore or a sanitised save, `SessionHost.show` adopts `PuzzleProgress.NEW`), the first flush writes `puzzleSeconds = 0` into that stored record. It is not user-visible (the next settled event overwrites the record with the New board anyway), but it is a write from a view that disagrees with the stored one. | design 4.3 (2) "only when the stored record is In progress" | Skip the write when the keeper's `puzzleSeconds` was adopted from a different state than the stored one, or accept and note it in DA-137. | TASK-063 |
| N8 | N | `settings/.../SettingsController.kt:41-42` | `revision` is `var ... private set`, a public read-only property in effect; the seam row says `val`. The same for `timerShown` (a `val` over private state, correct). No caller can write it. The seam text and the code differ only in spelling. | seam row 8 (TASK-066) | Either change the seam row to "read-only property" or add a `private var` plus `val revision` getter; no behaviour change. | TASK-066 / orchestrator (design write-back) |
| N9 | N | `.swdev/staged/WO-008/iv/...`; `tasks.md` T8c row (b) | (a) A stray `iv/` folder under `.swdev/staged/WO-008` holds duplicate copies of two moved-in tests (`TimeStoreRoundTripTest`, `ReleaseSeparationTest`). `.swdev` is excluded from the scans, so no token is double-counted, but it is clutter and could be mistaken for a staged source. (b) The gate's positive control (a rev-0-shaped stub failing 7 of 9, a reference passing 17/17, 7 mutants caught) lives outside the tree and is not kept, so Test & Verify cannot re-run it. The in-tree evidence is the real keeper passing 9/9 and 8/8. | AGENTS (evidence you can re-derive) | Delete `iv/`; copy the stub and the mutant list into `.swdev/staged/WO-008/gate-control/` (or the scratchpad path written into the workorder) so T&V8 can re-run them. | orchestrator; Test Author |

## Per-item results (the 8 points of the brief)

1. **Writer lists (§4.3, §5.1).** Matches the code exactly. The writers are `accrue` (when due), `flush`, `setVisible(false)` and `setPuzzleRunning(false)`. `touch`, `puzzleShown`, `solved`, `mergeInto` and `afterReset` have no path to the store. The best is computed from `base`, never cached. See the trace table. N7 is the only gap, and it is cosmetic.
2. **Flush, merge, `afterReset`, `day`, midnight, constructor read.** All hold (see the bullets above). `FlushRulesTest` and `OwnershipOrderTest` (nine cases) assert what the design says, including "Erase without a touch" with a flush due. `TimeStoreRoundTripTest` wires the real `BrowseController`, `SettingsController` and `SessionHost` over a real `JsonProgressStore` with a call recorder. `SessionHostTimeWiringTest` covers `solvedListener` through the host. Tokens sit only on tests that assert the criterion's meaning (spot-checked `SolveTimeTest`, `BestTimeTest`, `IdleCutoffTest`, `TimerRotationAppTest`, `PlayTimeSettingsAppTest`).
3. **Touch observer and ticker.** Initial pass, no consume, no per-event allocation, no change to existing input. Idle: no pending message. See N5 and N6.
4. **`timerRect` and `SubcomposeLayout`.** `computeWithCorner` and `placeSecondary` untouched; DA-147's template matches `PuzzleTimer`; the pill does not move within an attempt; the fallback is never used in the 2340-case sweep. See N1, N2, N3.
5. **Debug / release split (G-04).** `TestConfig` is in `app/src/debug` only; `DebugAids.timeSource` is `TestConfig.timeSource ?: real` in debug and `= real` in release, with identical public signatures; `ReleaseSeparationTest` asserts the pass-through with a debug-body control; `app/src/main` has no reference to `TestConfig`. The `time` dependency is `implementation` in `app`, as it must be.
6. **DA-149.** Both merges are on the tagged node, with no action, role or pointer input, so the pill and the rows stay inert and take no touch. See N4 for TalkBack.
7. **Landed tests, kit, gate.** The in-tree kit's two `TestConfigRule` copies reset `timeSource` in `before()` and `after()`, and `AppLaunch.launch` sets it before the launch. The manual clock is written only on the main thread, inside `advanceActive`. The gate's real-code run is 9/9 and 8/8; its positive control is outside the tree (N9).
8. **The two items you named.** `revision` (N8): harmless. `SessionHost.kt` line endings: no CR in any `app/src/main` file, so the file is LF like the repo; fine.

## What must be kept

- `account()` having no store path, and the closed writer list. Do not add a flush to any input method; `OwnershipOrderTest` is the guard.
- `afterReset()` as the first statement of `onReset`, writing nothing, re-reading the store.
- `mergeInto` computing the best from `base` and keeping no cache; `day` set only on a credited second (E2).
- The root observer on the Initial pass, never consuming.
- `computeWithCorner` and `placeSecondary` untouched; one shared `SubcomposeLayout`; the template-width step (E3).
- The release `DebugAids.timeSource` as a pure pass-through and `TestConfig` debug-only.
- DA-149's merged semantics on the pill and the settings rows, with no click action.
- The `TestConfigRule` reset of `timeSource` in both kit copies.

## Handoff

- **Verdict:** forward, 0 B / 0 S / 9 N.
- **Owners:** N2, N3 -> Test Author (T8c); N5, N6 -> TASK-068; N7 -> TASK-063; N4 -> TASK-064 (optional); N8 -> TASK-066 / orchestrator; N9 -> orchestrator + Test Author; N1 -> the owner at checkpoint 8 (CLOSE8 surface). All can be a short CR-7-FIX or logged as accepted; a `play` fix (only if N1 or N3 is acted on) re-runs DEV-EARLY8's `play` part.
- **Did not do:** no Gradle, no device, no git, no `.swdev/heldout/`.
