# Daily probe

**Status:** in progress. Slices 1–12 shipped. Next: slice 13 (Recall Stats
shows Daily probe trend).
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

### 3. Fix the Daily probe trial and scoring contract — Structure `[x]`

Shipped: `frontend/src/models/dailyProbe.ts` — practice/scored sequences, F/J
and arrow mapping, trial records from injected timestamps, speed as mean
reciprocal RT. Contract tests include the protocol sequences and the 3.00 s⁻¹
example. No screen yet.

**Enables slice 4 only.**

### 4. An opted-in learner completes the probe and sees speed before recall — Behavior `[x]`

Shipped: `DailyProbe.vue` before `/recall` when opted in; Continue into
ordinary recall. Speed shown to 2 decimals. Disabled path unchanged. In-memory
only until slice 9. E2E `recall/daily_probe.feature`.

**Enables slice 5.**

### 5. The completed result reports accuracy — Behavior `[x]`

Shipped: `dailyProbeAccuracy` = `round(100 × correct / 20)` integer percent
on scored trials. Result shows `N%`. E2E all-correct path asserts `100%`.

**Enables slice 6.**

### 6. The completed result reports lapses — Behavior `[x]`

Shipped: `dailyProbeLapseCount` — scored RT ≥ 500 ms or timeout; false starts
are not lapses. Result shows the count. E2E all-correct 250 ms path asserts 0.

**Enables slice 7.**

### 7. The completed result reports variability — Behavior `[x]`

Shipped: `dailyProbeVariability` — sample SD of the reciprocal RTs that enter
speed. Display 2 decimals; omit if fewer than 2 values. Protocol example 1.41
s⁻¹. E2E all-correct 250 ms asserts 0.00.

**Enables slice 8.**

### 8. Represent Daily probe attempts and results durably — Structure `[x]`

Shipped: `V300000305__create_daily_probe.sql` — completed rows only (`user_id`
CASCADE, `completed_at`, nullable `speed`/`variability`, `accuracy`,
`lapse_count`, `trials_json`). Entity `DailyProbe`, `DailyProbeRepository`,
`MakeMe.aDailyProbe()`. Persistence test round-trips 20 scored trials. ERD
updated. No endpoint or UI.

**Enables slice 9 only.**

### 9. Completing the probe durably saves the result — Behavior `[x]`

Shipped: POST `/api/daily-probes` when scored trial 20 completes. Owner from
the authenticated learner; `completedAt` from the testability clock; 20
trials required. Result screen shows **Saved**. Client regenerated. E2E
asserts the saved state before Continue.

**Enables slice 10.**

### 10. A second recall session on the same local day bypasses the probe — Behavior `[x]`

Shipped: GET `/api/daily-probes/today?timezone=` returns `{ completed }` for the
authenticated learner's local day. Recall skips the probe when completed.
E2E: second `visit recall` on day 1 shows ordinary recall.

**Enables slice 11.**

### 11. The next local day offers the same fixed probe again — Behavior `[x]`

Shipped: controller midnight (Asia/Shanghai day 1 23:00 → day 2 00:00 →
`completed` false). E2E day 2 visit shows the same probe instruction. GET
today already used local-day bounds; no production change.

**Enables slice 12.**

### 12. Expose completed Daily-probe days to Recall Stats — Structure `[x]`

Shipped: `RecallStatsDTO.dailyProbe` — sparse `{ date, speed, lapses,
variability }` points, oldest-first, request timezone, latest row per local
day. Empty list when none. Client regenerated. Page does not render yet.

**Enables slice 13 only.**

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
