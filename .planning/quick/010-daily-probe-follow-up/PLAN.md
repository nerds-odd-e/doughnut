# Daily probe follow-up (bugs, ADR gap, redundant tests)

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until the developer approves.**
**Depends on:** shipped `.planning/quick/007-daily-cognitive-probe/PLAN.md`
**Measurement spec:** [daily-probe-protocol.md](../../notes/daily-probe-protocol.md)

Canonical name: **Daily probe** / `daily_probe` / `DailyProbe` (ADR 0001 / 0003).

## Goal

Fix the user-visible holes left by plan 007, then delete the overlapping tests
and test-only production API that 007 introduced. Stop after any slice.

## Inspection (branch vs `main`)

Scope: `origin/main...HEAD` on the Daily probe work (~61 files). Protocol,
ADR 0003 Daily probe clause, settings copy, and `unit-testing.mdc` /
post-change-refactor checks were the bar.

### Sliced (meaningful)

1. **KeepAlive abandon saves a run.** `DonutApp.vue` keep-alives `RecallPage`.
   `DailyProbe.vue` `onDeactivated` only detaches keys; `setTimeout` keeps
   advancing. Navigate away mid-run and the probe can finish and `POST` off
   screen. Protocol: abandon writes nothing; next visit starts fresh.
   Existing E2E `When I visit recall` uses `cy.visit('/recall')`, which
   destroys KeepAlive and cannot catch this.
2. **Health-defaults PATCH turns Daily probe off.** `UserController.updateUser`
   always `setDailyProbeEnabled(requireNonNullElse(..., false))`.
   `UserDTO.dailyProbeEnabled` defaults to `false`, so a missing JSON field
   is false, not “leave unchanged”. `NotebookHealthPanel.saveAsDefaults`
   PATCHes name / assimilation count / health default and **omits** Daily
   probe. Saving notebook health defaults silently disables the opt-in.
3. **Continue consumes the in-session offer before save.** Result Continue
   is enabled immediately; `persistCompletedProbe()` is not awaited;
   `@complete` sets `completedToday = true` even if POST fails. The learner
   can enter recall with no row and not be re-offered until a full reload.
4. **GET `/api/daily-probes/today` error blanks recall for the session.**
   `useDailyProbeOffer` leaves `completedToday` undefined on error, so
   neither probe nor ordinary recall renders. The watch is only on
   `enabled`, so KeepAlive return does not retry. Protocol: do not silently
   treat as completed or due — show retry.
5. **Turning Daily probe off does not end the trend readout.** ADR 0003 and
   General settings copy say turning it off “ends the probe's own trend
   readout”. Recall Stats still charts `dailyProbe` history regardless of
   `dailyProbeEnabled`.
6. **Redundant tests / test-only API** (post-change-refactor dead_redundant):
   `DailyProbePersistenceTest` repeats the controller persist shape;
   `DailyProbe.spec.ts` re-asserts accuracy / lapses / variability already
   covered in `dailyProbe.spec.ts`; `RecallStatsService.aggregateRows` 4-arg
   overload exists only so tests can omit the series; instruction string is
   copied in the mounted spec and E2E page object instead of importing
   `DAILY_PROBE_INSTRUCTION`.

### Inspected and not slicing

| Finding | Why not a slice |
|---|---|
| Client-supplied summaries stored as-is; trials are the protocol source of truth | Not user-visible today; Java-side scoring would duplicate `dailyProbe.ts`. Recompute from `trials_json` if 008 needs trustworthy history. |
| No unique-per-local-day constraint; two tabs can insert two rows (series keeps latest `completed_at`) | Timezone-dependent; rare. GET `/today` still returns completed after the first row. |
| Probe-only 30/90/All buttons sit with no “Daily trends” heading | Cosmetic; filtering already works. |
| Shared `timeTravelTo` restore+reinstall of `cy.clock` | Needed so `cy.visit` keeps the testability day for the 39-day window jump. Residual risk for other `@mockBrowserTime` specs — do not change the helper in this plan unless a targeted spec fails. |
| Accuracy omitted from Recall Stats trend | Plan 007 asked speed, lapses, variability only. |
| `DailyProbeBuilder` default trials are all `"left"` | Fixture, not production. |
| Empty copy “No reviews yet” | Probe-only users already see the trend instead of that empty state. |
| `.planning/STATE.md` still says 007 is “planned, not started” | `execute-plan` must not write `STATE.md`. Optional GSD hygiene, not product. |

## Design decisions

- **Abandon = deactivate while unfinished:** clear timers, do not POST, reset
  so the next activation is a new run. A finished result screen may finish
  an in-flight save (that run completed).
- **First mount:** Vue calls `onMounted` then `onActivated`. Start the run
  once.
- **Abandon tests must use in-app navigation** (`navigateToRecallPage` /
  sidebar Recall / existing `returnToRecallFromDetour`), never
  `cy.visit('/recall')`.
- **PATCH:** missing `dailyProbeEnabled` means leave unchanged (null on the
  DTO, apply only when non-null). General settings still sends the field.
  Do not change `healthRemoveEmptyFoldersDefault` semantics.
- **Offer failure UI** would push `RecallPage.vue` over 250 lines (it is at
  the cap). Extract the probe vs ordinary-recall branch first, then add
  retry on that component.
- **Hide trend when off:** omit `dailyProbe` from Recall Stats when the
  current user has the flag off. Frontend already hides an empty series.
  History rows stay; turning it back on restores the readout.
- **Names:** Daily probe only — no `cognitive_probe`.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Leaving recall mid-probe writes nothing — Behavior `[ ]`

**Pre:** Opted in; Daily probe is on a scored or practice trial (not the
result screen).
**Trigger:** In-app navigate away from recall, wait long enough that the old
timers would have finished the run, then return via in-app Recall.
**Post:** No `daily_probe` row; the probe is offered again from the
instruction / first stimulus (not Saved / Continue).

Tests: mounted KeepAlive detour on `DailyProbe` (fake timers, assert
`createDailyProbe` not called, run resets); E2E in
`e2e_test/features/recall/daily_probe.feature` using in-app navigation.
Implementation: `DailyProbe.vue` — `onDeactivated` clears timers and resets
unfinished state; do not double-`startTrial` on first mount.

### 2. Saving notebook health defaults leaves Daily probe unchanged — Behavior `[ ]`

**Pre:** Daily probe is on.
**Trigger:** PATCH `/api/user/{id}` without `dailyProbeEnabled` (the
notebook health “save as defaults” body).
**Post:** `dailyProbeEnabled` stays on.

Test: `UserControllerTest` — enable the flag, `updateUser` with name /
assimilation / health default only, assert still enabled. Production caller
`NotebookHealthPanel` can keep omitting the field.

### 3. Continue into recall only after the result is saved — Behavior `[ ]`

**Pre:** All 20 scored trials have an outcome; the result screen is showing.
**Trigger:** Persist succeeds or fails.
**Post:** Continue is usable only after Saved; a failed save shows retry and
does not emit `complete` (ordinary recall is not entered; this session still
owes a completed row).

Tests: `DailyProbe.spec.ts` — delay/error on `createDailyProbe`; Continue
disabled until Saved; retry posts again. Existing E2E already waits for
Saved on the happy path.

### 4. Extract the Daily probe offer branch from RecallPage — Structure `[ ]`

Move `useDailyProbeOffer` plus the `DailyProbe` / ordinary-recall template
branch into a capability-named component (e.g. `DailyProbeGate.vue`) with a
slot for ordinary recall. No new UI. `RecallPage.vue` stays under 250 lines.
Existing `RecallPage.dailyProbe.spec.ts` still pass.

Justifies slice 5 only.

### 5. A failed offer check shows retry, not a blank recall page — Behavior `[ ]`

**Pre:** Daily probe is on; `GET /api/daily-probes/today` fails.
**Trigger:** Learner is on recall (including KeepAlive return to the same
page).
**Post:** Visible retry; neither the probe nor ordinary recall until a
successful GET. Retry then offers or skips as the payload says.

Tests: mounted RecallPage / gate — mock GET error, assert retry CTA, no
`DailyProbe`, no `Quiz`; retry success with `completed: false` shows the
probe. `useDailyProbeOffer` must re-fetch on retry (not only when `enabled`
flips).

### 6. Turning Daily probe off hides its Recall Stats trend — Behavior `[ ]`

**Pre:** At least one completed Daily probe row; Daily probe is then turned
off.
**Trigger:** Open Recall Stats.
**Post:** No Daily probe trend. Review charts are unchanged if reviews
exist; probe-only learners see the existing empty stats state.

Tests: `UserRecallStatsControllerTest` — rows exist, flag off, `dailyProbe`
empty; E2E extend `daily_probe.feature` (complete, turn off, stats has no
trend). Implementation: `RecallStatsService.compute` omits the series when
`!user.getDailyProbeEnabled()`.

### 7. Remove redundant Daily probe tests and the test-only stats overload — Structure `[ ]`

- Delete `DailyProbePersistenceTest` (same persisted shape as
  `DailyProbeControllerTest.completingPersistsOwnerTwentyTrialsSummariesAndCompletedAt`).
- In `DailyProbe.spec.ts`, drop the accuracy / lapses / variability cases
  that only re-assert `dailyProbe.spec.ts`; keep instruction, one canonical
  complete (speed `4.00` + Continue), and Saved / POST of 20 trials.
- Remove the 4-argument `RecallStatsService.aggregateRows` overload; test
  fixtures pass `List.of()`.
- Import `DAILY_PROBE_INSTRUCTION` in the mounted spec and
  `recallDailyProbeMethods.ts` instead of copying the string.

Do not delete: model formula/sequence tests; the three RecallPage entry
preconditions; GET `/today` timezone / other-user / empty / unauthorized
cases; E2E complete vs same-day skip vs next-day vs off vs trend vs window.

Verify with the focused backend controller tests and `pnpm frontend:test`
on the touched specs. No E2E behavior change.

## Out of scope

- Executing this plan
- Backend recomputation of speed / accuracy / lapses / variability
- Unique local-day constraint
- Changing `e2e_test/start/testabilityTimeTravel.ts` further
- Writing `.planning/STATE.md`
- Follow-on analyses in `.planning/quick/008-probe-convergent-analyses/`
