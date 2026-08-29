# Daily probe follow-up (bugs, ADR gap, redundant tests)

**Status:** in progress (slices 1–4 done).
**Type:** ad-hoc plan (`.planning/quick/`)
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

### 1. Leaving recall mid-probe writes nothing — Behavior `[x]`

KeepAlive deactivate while unfinished calls `abandonUnfinishedRun` (clear
timers, reset, no POST). Next `onActivated` starts a fresh run. First mount
still starts once (`abandoned` is false). Finished result screen is left
alone so an in-flight save can complete.

**Learning:** E2E first entry can still `visit recall`; the KeepAlive path
is note detour + `I return to recalling`, not a second `cy.visit('/recall')`.

### 2. Saving notebook health defaults leaves Daily probe unchanged — Behavior `[x]`

Missing `dailyProbeEnabled` on PATCH is null on `UserDTO`; `updateUser`
applies the flag only when non-null. `healthRemoveEmptyFoldersDefault`
still defaults missing to false. `NotebookHealthPanel` still omits the
field. Generated TS was already `dailyProbeEnabled?: boolean`.

### 3. Continue into recall only after the result is saved — Behavior `[x]`

Result screen `saveStatus` is `unsaved` | `saved` | `failed`. Continue is
disabled until Saved; failed persist shows Retry and does not emit
`complete`. Retry calls `createDailyProbe` again.

### 4. Extract the Daily probe offer branch from RecallPage — Structure `[x]`

`DailyProbeGate.vue` owns `useDailyProbeOffer` plus the probe / ordinary-recall
slot. `RecallPage.vue` wraps existing recall UI in that slot (~240 lines).
No new UI. Existing `RecallPage.dailyProbe.spec.ts` still pass.

Justified slice 5.

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

- Backend recomputation of speed / accuracy / lapses / variability
- Unique local-day constraint
- Changing `e2e_test/start/testabilityTimeTravel.ts` further
- Writing `.planning/STATE.md`
- Follow-on analyses in `.planning/quick/008-probe-convergent-analyses/`
