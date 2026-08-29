# Daily probe

**Status:** in progress. Slices 1–2 shipped. Protocol and daily-consumption
gates locked — see `.planning/notes/daily-probe-protocol.md`. Next: slice 3.
**Type:** ad-hoc plan (`.planning/quick/`)
**Extracted from:** `.planning/quick/001-morning-cognitive-index/PLAN.md`
(unbuilt slices 26–31).
**Related:** `.planning/quick/008-probe-convergent-analyses/PLAN.md` consumes
probe history only after this plan ships.

The timer, pace, accuracy, consistency, and lapse-count work in `quick/001`
already shipped. Its composite morning index was dropped after the
split-half reliability gate failed on production data (`pairCount: 91`,
`rawCorrelation: 0.076`, `spearmanBrownCorrelation: 0.141`). This plan does
not depend on that composite.

## Goal

Give the learner an opt-in, approximately 60-second **Daily probe** before
recall. The task uses the same fixed stimulus protocol every day and produces
an item-structure-free readout of speed, accuracy, lapses, and variability.
Store raw trials so the summaries can be recomputed later, offer the task at
most once per local day, and show its history on Recall Stats.

## Accepted-ADR constraints and pre-execution gates

- Follow [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md)
  and [ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
  the canonical capability name is **Daily probe**. Permanent artifacts use
  `daily_probe` / `DailyProbe`, not `cognitive_probe` / `CognitiveProbe`.
- **ADR 0003 Daily probe amended in place (slice 2 unblocked).** The probe
  does not validate the Cognitive index. Settings help text follows that
  glossary: turning it off stops new probes and ends the probe's own trend
  readout. Do not mention Cognitive index in product copy.
- **Protocol locked** in
  [daily-probe-protocol.md](../../notes/daily-probe-protocol.md): 4 practice +
  20 scored two-choice left/right trials, F/J (and arrows), 2000 ms timeout and
  ISI, mean reciprocal RT / accuracy / lapses / variability as specified there.
  Do not copy the formulas into an ADR.
- **Daily offer consumed only on completion.** An abandoned probe writes
  nothing. Same local day re-offers until one run finishes. Persistence is
  completed rows only.

These gates are resolved. Measurement lives in the protocol note, not in an
ADR.

## Key design decisions

- **Opt-in, default off.** The time cost needs explicit consent. Help text
  follows ADR 0003: turning it off stops new probes and ends the probe's own
  trend readout. Do not mention Cognitive index.
- **Fixed protocol every day.** The approved stimulus identifiers and order
  are deterministic. Tests advance a controlled clock; they do not spend a
  real minute waiting.
- **Raw trials are the source data.** Each stored trial contains enough data
  to recompute every displayed summary. Persisted summaries are query-friendly
  readouts, not a replacement for `trials_json`.
- **Local day comes from the request timezone.** The backend uses its
  testability-aware current timestamp plus the supplied IANA timezone for
  eligibility and trend grouping. Invalid timezones fail visibly through the
  existing timezone parser.
- **Incomplete attempts never enter trends.** They also never enter the
  table: abandon writes nothing; only a completed 20-trial run is stored.

## Slice sizing and commit contract

Each numbered slice below is one commit-and-push boundary and targets about
five minutes of implementation plus focused tests. Generated client or ERD
output stays in the slice that changes its source contract. If a slice crosses
the 5-minute scrutiny point because the change is larger—not because a single
required test is slow—stop and refine that slice again before continuing.

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Slices

### 1. Store the Daily probe opt-in in the user profile contract — Structure `[x]`

Shipped: `V300000304__add_daily_probe_enabled_to_user.sql` (`TINYINT(1) NOT
NULL DEFAULT '0'`), mapped on `User` / `UserDTO` / `UserController.updateUser`
like `health_remove_empty_folders_default`. Controller tests cover default
false and persist-true. TS `UserBuilder` defaults the field off and has a
setter for slice 2. Client regenerated. ERD unchanged (key columns only).
Backend unit suite green (1908 tests).

**Enables slice 2 only.**

### 2. The learner can opt in or out in General settings — Behavior `[x]`

Shipped: General settings **Daily probe** checkbox (default off) PATCHes with
the profile. Help text: turning it off stops new probes and ends the probe's
own trend readout (ADR 0003; no Cognitive index). Mounted
`GeneralSettingsTab` tests cover default-off and persist-true. E2E
`users/user_profile.feature` Scenario Outline enable/disable, then reload.

**Enables slice 3 only.**

### 3. Fix the Daily probe trial and scoring contract — Structure `[ ]`

Represent the protocol note's fixed stimuli, response keys, trial records,
injected clock, and **speed** (mean reciprocal RT) in one cohesive, pure
Daily-probe module. Contract tests prove deterministic stimuli/order and the
worked 3.00 s⁻¹ speed example. There is no screen or recall-flow change.

- Do not introduce generic experiment/task frameworks.
- Add lapse and variability calculations only in slices 6 and 7; this slice
  contains only what the immediate next behavior needs.
- **Enables slice 4 only.**

### 4. An opted-in learner completes the probe and sees speed before recall — Behavior `[ ]`

**Pre-condition:** Daily probe is enabled and the learner enters recall.
**Trigger:** they complete the fixed trial sequence. **Post-condition:** they
see this run's mean reciprocal response time and can continue into ordinary
recall; a learner with the setting off enters recall unchanged.

- Add the narrow Daily-probe screen/component and the smallest recall-entry
  seam needed to host it.
- Mounted-component test covers timing and completion; E2E starts
  `recall/daily_probe.feature` and covers the enabled path plus the disabled
  regression.
- **Interim through slice 8:** the result exists only in memory and the probe
  may appear again in another session. This is an explicit early-feedback
  increment and disappears when persistence lands in slice 9.

### 5. The completed result reports accuracy — Behavior `[ ]`

**Pre-condition:** the learner has completed a mix of correct and incorrect
Daily-probe trials. **Trigger:** the result appears. **Post-condition:** it
also reports accuracy using the approved unit and rounding.

- Extend the mounted test and the existing Daily-probe E2E scenario; do not
  create another trial runner.

### 6. The completed result reports lapses — Behavior `[ ]`

**Pre-condition:** the completed run contains responses on both sides of the
approved lapse boundary. **Trigger:** the result appears. **Post-condition:**
it also reports the correct lapse count.

- Add the lapse calculation to the stable trial/scoring contract, cover its
  boundary with a pure unit test, and extend the same result UI/E2E path.

### 7. The completed result reports variability — Behavior `[ ]`

**Pre-condition:** the completed run has the response-time pattern from the
approved variability example. **Trigger:** the result appears.
**Post-condition:** it also reports variability using the approved unit and
rounding.

- Add one pure calculation test and extend the existing result UI/E2E path.

### 8. Represent Daily probe attempts and results durably — Structure `[ ]`

Create the `daily_probe` table and cohesive backend persistence model.
Completed rows only: user FK with CASCADE, completion timestamp, all four
summaries, and `trials_json` with the 20 scored trials exactly. No incomplete
status column — abandon leaves no row.

- Compute the Flyway version fresh at execution time.
- Extend `MakeMe` only as needed for concise controller fixtures.
- Regenerate `docs/database-erd.md` with the `database-erd` skill.
- No endpoint or UI behavior changes in this slice.
- **Enables slice 9 only.**

### 9. Completing the probe durably saves the result — Behavior `[ ]`

**Pre-condition:** the learner starts and completes the Daily probe.
**Trigger:** the final trial is submitted. **Post-condition:** the learner sees
that today's result was saved, and the backend retains the exact raw trials
plus their four summaries for that learner.

- Drive backend persistence through the controller boundary with the real DB;
  derive ownership from the authenticated learner instead of accepting a
  client-supplied user id.
- Regenerate the TypeScript client, submit through the normal wrapped API, and
  extend the Daily-probe E2E scenario through the visible saved state.
- This slice removes the in-memory-only interim from slice 4.

### 10. A second recall session on the same local day bypasses the probe — Behavior `[ ]`

**Pre-condition:** the Daily probe is enabled and today's offer has been
consumed according to the approved rule. **Trigger:** the learner starts
another recall session in the same local day. **Post-condition:** ordinary
recall opens without another probe prompt.

- Controller test owns the timezone boundary and same-user lookup.
- E2E uses the testability clock; no browser sleeps.

### 11. The next local day offers the same fixed probe again — Behavior `[ ]`

**Pre-condition:** yesterday's Daily-probe offer was consumed and the setting
remains enabled. **Trigger:** the learner starts recall after local midnight.
**Post-condition:** the probe is offered again with the same approved fixed
stimulus protocol.

- Test a non-UTC timezone around midnight at the controller boundary and one
  next-day E2E path with the testability clock.

### 12. Expose completed Daily-probe days to Recall Stats — Structure `[ ]`

Extend the current-user Recall Stats read model with one cohesive daily series
containing mean reciprocal response time, lapses, and variability for completed
probe results. Group by the request timezone and exclude incomplete attempts.
Regenerate the TypeScript client. The page does not render the series yet.

- Backend controller/service tests cover ordering, timezone grouping, empty
  history, and incomplete-attempt exclusion through the stable boundary.
- **Enables slice 13 only.**

### 13. Recall Stats shows the Daily probe trend — Behavior `[ ]`

**Pre-condition:** the learner has completed Daily probes on multiple local
days. **Trigger:** they open Recall Stats. **Post-condition:** the default
90-day view shows the three approved probe readouts together as one Daily
probe trend, without changing existing recall-derived charts.

- Mounted-page test uses the generated DTO fixture; E2E creates probe history
  through the product flow and asserts the user-visible trend.

### 14. The existing window control filters the Daily probe trend — Behavior `[ ]`

**Pre-condition:** completed Daily-probe history spans the 30-day and 90-day
boundaries. **Trigger:** the learner selects 30d, 90d, or All on the existing
Recall Stats control. **Post-condition:** the Daily-probe trend shows exactly
the matching local-day points.

- Reuse the existing control and date-slicing grammar; do not add a second
  window selector.

## Permanent artifacts (capability-named)

| Artifact | Slices |
|----------|--------|
| `e2e_test/features/users/user_profile.feature` | 2 (extend) |
| `e2e_test/features/recall/daily_probe.feature` | 4–11, 13–14 |
| `daily_probe` schema/entity/repository | 8–12 |
| Daily-probe frontend component and scoring module | 3–9 |

## Per-slice wrap-up

For every slice: Jidoka before/after → test first and confirm the right failure
→ smallest change to green → `post-change-refactor` on the uncommitted change
→ update this plan → commit and push before the next slice. Run all backend
unit tests when backend code changes, focused frontend tests while iterating,
and only the targeted Cypress feature. Unfinished E2E stays `@wip`; never
commit on red.

Migration slices use the `database-erd` skill. A changed controller signature
uses the `generate-api-client` skill. When the final slice ships and the
outcomes live in product code/tests/docs, remove spent plan history rather
than retaining this file as a permanent implementation diary.
