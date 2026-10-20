# Refresh recall at half-day rollover and choose whether to start the daily probe

**Source:** [Refresh recall at half-day rollover and choose whether to start the daily probe](../../seeds/SEED-079-half-day-recall-refresh.md#story-1)

**Identity:** SEED-079#story-1

## Goal and scope

A learner who leaves Donut open in the browser gets the current half-day's
recall trackers and questions without reloading, including after the tab
slept across the boundary. When a new local day makes the daily probe due,
the learner is offered it on a full-screen entry screen and chooses Start or
Skip before recall questions. The source's Goal, Established behavior, Scope,
UI, and Key examples apply.

Included: a catch-up when the tab becomes visible after the boundary passed
without the timer firing; the Start/Skip entry screen for every probe entry;
a probe re-offer when a catch-up crosses into a new local day; Skip held in
memory for the page's life. Excluded, per the source: persisted skip
preferences, notifications, any change to the half-day definition, the
once-per-local-day probe rule, or the due rules; no new web route; no backend
or API change is planned.

Assumption carried from the source's Open Decisions: when midnight makes the
probe due mid-question or while paused on a last answered question, the entry
screen appears right away and Skip returns the learner to the same place.

Preparation is planning only: no Take or execution is authorized. Observed
checkout: `/Users/terryyin/git/doughnut/.worktrees/refresh-recall-at-half-day-rollover-and-choose-w`,
revision `5338aceb4745f9b3260a20be62ba046067058e27`, 2026-10-20. Execution
starts from current trunk and reconciles shared changes normally. No blocking
supplier is recorded.

## Existing solutions and architectural decisions

PFE choice: extend the existing boundary timer and the existing probe gate;
add no polling, no second refresh mechanism, and no new probe model.

- `useSignedInMenuData` (app shell) arms one timer to `currentRecallWindowEndAt`
  plus a five-second margin and calls `catchUpDueRecalls`, which appends newly
  due trackers after the queue, keeps the learner's position, and re-arms from
  the new window end. Add the visibility re-check beside that timer: on
  `visibilitychange` to visible, catch up only when the clock is past the
  stored window end. After a catch-up the window end moves, so a timer that
  already fired leaves nothing for the re-check to do. The same pattern already
  exists in `FrontendUpdateReminder`.
- `useDailyProbeOffer` owns the probe offer (`/api/daily-probes/today`, once per
  page load) and `DailyProbeGate` renders the probe, the retry screen, or the
  ordinary recall slot. Put the entry screen and the in-memory Skip in this
  gate/composable pair. Re-check the offer from the same composable when the
  recall window end changes into a different local day from the last check;
  the browser's local date and `timezoneParam()` describe the same zone, so no
  new backend field is needed. Noon changes the window end without changing
  the day, so no re-check runs then.
- `DailyProbe` starts its trials on mount; the entry screen mounts it only on
  Start, so its trial logic is unchanged. The `RecallPage` is kept alive by
  `DonutApp`, so Skip held in the gate survives in-app navigation and resets
  on reload without any storage.
- Cross-product reading: CLI and MCP do not expose the daily probe; the
  backend probe endpoints and `RecallService` need no change.

Accepted ADR index and in-file statuses agree for 0000–0007; no supersession
or conflict was found. Relevant decisions:

- [ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
  the Daily probe is opt-in, offered at most once per local day before recall,
  and not a scheduling input. Skip does not complete it; the one offer stays
  open until completed or the day ends. The half-day scheduling meaning is
  untouched.
- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md):
  keep the names Daily probe, recall, memory tracker.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md):
  the entry screen stays on the recall route; no new location.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  observations use disposable test state only.

The existing [North Star](../../NORTH-STAR.md) needs no new topic for this
local recall-page responsibility.

## Decisive premises and observations

Observations ran in the named checkout at the revision above on 2026-10-20,
with real Vue components, the production router, and mocked HTTP boundaries
(unit) or the disposable E2E stack. Scratch specs were removed before the
preparation record was written.

| Premise / consuming operation | Observation and result | Consequence |
| --- | --- | --- |
| After a catch-up that crosses into a new day, the gate still shows ordinary recall and does not re-check the probe | Scratch `tests/pages/RecallPage.scratchRollover.spec.ts`: mounted `RecallPage` with the probe on and today completed, then switched the probe mock to due and ran `useRecallCatchUp().catchUpDueRecalls()`. `getDailyProbeToday` stayed at 1 call and no `DailyProbe` rendered. Passed. | Slice 3 must add the re-check; a catch-up alone never re-offers. |
| A tab that becomes visible after the window end, with the timer not having fired, makes no catch-up today | Scratch `tests/toolbars/DonutMenu.scratchVisibility.spec.ts`: fake timers at 11:00Z, menu data with window end 12:00Z, system time set to 12:30Z without running timers, `visibilitychange` dispatched. `recalling` was not called. Passed. | Slice 1 adds the visibility re-check; the timer alone leaves the slept-tab case open. |
| The existing timer catch-up discriminates "one request after the boundary, none before, none repeated" | `tests/toolbars/DonutMenu.counts.spec.ts` "catches up due recalls once while the panel is closed" passed in the same run (25 existing tests across both suites passed). | Slice 1 keeps this case green and adds the visibility case beside it. |
| The daily-probe E2E route is available here and drives browser time | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/daily_probe.feature` passed 1/1 in 11 s (stack boot included). `@mockBrowserTime` with "it is {int} minutes later in the browser" (`cy.tick`) and "It's day 2" backend travel exist in `spaced_repetition.feature`. | E2E proof for slices 2 and 3 reuses this feature file and these steps. |

Unit observation command (scratch files now removed):

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/pages/RecallPage.spec.ts tests/toolbars/DonutMenu.counts.spec.ts
```

## Outside-in proof ownership

| Source promise | Owner | Observable proof |
| --- | --- | --- |
| Boundary timer retained: one request after the boundary, none before, none repeated | 1 | Existing `DonutMenu.counts.spec.ts` window-end case stays green. |
| Tab visible after the boundary without the timer firing → one catch-up; visible before the boundary → none (example 2) | 1 | New `DonutMenu.counts.spec.ts` cases: system time past the window end, `visibilitychange` → `recalling` once and count updated; before the end → no call. |
| Entry screen shows instruction, Start, Skip, and waits; Start begins trials; ordinary visit uses it (examples 3, 7) | 2 | `RecallPage.spec.ts` probe-entry cases: entry visible, no `DailyProbe` and no `RecallPromptCard` until a choice; Start renders `DailyProbe`. E2E `daily_probe.feature`: existing scenario gains "I start the Daily probe" before the trials. |
| Skip → recall questions immediately with the progress bar back; in-app navigation and reload semantics (examples 4, 5, 6) | 2 | `RecallPage.spec.ts`: Skip renders `RecallPromptCard` and the progress bar; a fresh mount asks again. E2E `daily_probe.feature` new scenario: skip, see ordinary recall, visit a note and recall again without the entry, reload and see the entry again. |
| Catch-up into a new day re-offers the probe; noon does not; after Skip a later day asks again (examples 3, 5, 8) | 3 | `RecallPage.spec.ts`: window end moves to a new local day → `getDailyProbeToday` called again and the entry shows; window end moves within the same day → no call; skipped then new day → entry again. E2E `spaced_repetition.feature` or `daily_probe.feature`: probe done on day 1, day 2 in backend, 1440 minutes later in the browser → the entry screen appears. |
| Mid-question at midnight → entry appears; Skip returns to the same question (example 9) | 3 | `RecallPage.spec.ts`: queue at index 1, rollover catch-up → entry; Skip → the same tracker's prompt is current. |
| Failed probe check keeps the Retry screen | 2 | Existing retry cases in `RecallPage.spec.ts` stay green. |

Proof discriminates when the entry-screen cases fail on today's immediate
trial start and the rollover cases fail on the scratch-confirmed single check.
No removal sweep is needed: nothing is deleted; the immediate trial start is
replaced by the entry screen, and the E2E step "I should see the Daily probe
instruction" stays valid because the entry screen shows the instruction.

## Ordered slices

### 1. Catch up when the tab becomes visible after the boundary
Type: Behavior
Status: planned
Proof: Slice 1 rows in the proof table; `DonutMenu.counts.spec.ts` and frontend typechecks.

Behavior: A signed-in learner's tab is hidden or asleep while the half-day
boundary passes and the timer does not fire → the tab becomes visible → one
due-recall request runs and the queue and count show the new half-day; visible
before the boundary → no request.

Add a `visibilitychange` listener beside the existing timer in
`useSignedInMenuData`, catching up only when the clock is past the stored
window end, and remove it on unmount. Safe stop: the slept-tab case is
covered; probe behavior is unchanged.

Size hypothesis: about 5 minutes including the two new cases.

### 2. Choose Start or Skip at the daily-probe entry
Type: Behavior
Status: planned
Proof: Slice 2 rows in the proof table; `RecallPage.spec.ts`, `daily_probe.feature`, frontend typechecks.

Behavior: An opted-in learner with the probe due arrives at recall → the
full-screen entry shows the instruction with Start and Skip and waits → Start
begins the trials as today; Skip shows the next recall question and the
progress bar immediately, and the page does not ask again until a reload.

Render the entry screen from `DailyProbeGate` when the probe is due and no
choice has been made; hold the choice in `useDailyProbeOffer`. Mount
`DailyProbe` only on Start. Add E2E steps for Start and Skip and a Skip
scenario. Safe stop: the Start/Skip journey works for ordinary entry; a
midnight rollover still needs a reload until slice 3.

Size hypothesis: 5–8 minutes including the E2E scenario. Above target because
the unit and E2E proof of one screen belong to one loop; reassess before 10.

### 3. Re-offer the daily probe when the catch-up enters a new day
Type: Behavior
Status: planned
Proof: Slice 3 rows in the proof table; `RecallPage.spec.ts`, one E2E rollover scenario, frontend typechecks.

Behavior: Probe on and today's probe completed or skipped, tab open → the
boundary catch-up moves the recall window into a new local day → the probe is
re-checked and, when due, the entry screen replaces the recall content before
the next question; Skip returns to the same question. A noon catch-up
re-checks nothing.

In `useDailyProbeOffer`, watch the recall window end; when its local day
differs from the last check's day, clear the held choice and re-check. Safe
stop: completes the source's outcome.

Size hypothesis: 5–8 minutes including the E2E rollover scenario; one proof
loop over the shared day rule. Reassess before 10.

## Verification and delivery

Focused commands:

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/pages/RecallPage.spec.ts tests/toolbars/DonutMenu.counts.spec.ts tests/components/recall/DailyProbe.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/daily_probe.feature,e2e_test/features/recall/spaced_repetition.feature
```

Both typechecks are required by `.agents/skills/frontend/SKILL.md`. The two
E2E features are the consumers of the probe entry and the rollover catch-up;
run them at the slices that change those journeys (2 and 3), not for slice 1.
Unset `NODE_ENV` for frontend tests because the owner's shell sets production.
No backend, API generation, or migration is planned.

Execution follows `AGENTS.md`: Jidoka, fresh dough-post-change-refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, plan update, commit
with the check-only hook, and push under `dough-execute-plan`. Do not execute
this sequence in preparation.

## Current decisions and review

One refresh mechanism (timer plus visibility re-check in the app shell), one
probe offer owner (`useDailyProbeOffer`) holding the day rule and the
in-memory Skip, no storage and no new endpoint. The midnight interrupt
assumption stays as the source records it; if the owner prefers holding the
entry until the current question is answered, slice 3 gains a pending-offer
state and the source's Open Decision is updated first.

The roughly one-hour story estimate includes refactoring, typechecks and
delivery; it is a hypothesis. Every leaf was reviewed for independent
outcomes, proof ownership and cumulative design. No preparatory Structure leaf
or slice-plan refinement is needed. No remaining blocking concern was found.
