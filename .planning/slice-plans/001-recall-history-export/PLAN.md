# Plan: Recall history export

**Source:** [SEED-100#story-1](../../seeds/SEED-100-recall-history-export.md#story-1)
**Source review:** [source-review.md](source-review.md)
**Identity:** SEED-100#story-1

## Goal and scope

A signed-in learner gets one JSON document with their entire recall history
and all their daily probes, from the recall stats settings tab or from an
authenticated `GET` under `/api` an agent calls with the learner's API token.
Scope, deferred promises, key examples, UI, and architecture notes are in the
source story and are not repeated here.

**Exclusions (from the story):** analysis or derived values, note titles or
content, unanswered recall prompts, session ids, CSV, a format-version field,
import, other users' data, a per-event timezone.

**Assumption:** one in-memory response fits the owner's whole history. The
recall stats page already reads every answered recall of the user (five-year
window) into memory through a projection query in production. Not observed on
production volume; a failure here is an execution learning, not a plan change
to stream.

## Current decisions

- Read through one projection query over `recall_log` (left joins to `answer`,
  the `recall_prompt` that owns that answer, and `memory_tracker` for type and
  note id), as `RecallPromptRepository.findAnsweredRecallAnswerRows` does. Do
  not load `RecallPrompt` or `RecallLog` entities per row: that caused the
  production N+1 timeout `RecallStatsService` was rewritten to avoid.
- The endpoint sits beside `GET /api/user/recall-stats` in `UserController`,
  takes the same required `timezone` parameter, and parses it with
  `TimezoneUtils.parseTimezone`. Timestamps leave the server already formatted
  as ISO-8601 with that zone's offset.
- The event kind is derived from existing columns: an answer present → recall
  prompt; no answer and tutor feedback text present → tutor feedback; neither →
  just review.
- Daily probe trials are read from `trials_json` into `DailyProbeTrialDTO`
  through the entity's `DailyProbeTrialsConverter`.
- The frontend calls the generated client (ADR 0005) and saves the response
  with `file-saver`, as `JsonExportSection` does.

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| A user API token authenticates an ordinary `GET` under `/api` (no new auth needed for the agent path). | `ApiBearerAuthenticationFilter` on `/api/*` for a non-MCP route, then `CurrentUserFetcherFromRequest`, both through `UserService.findUserByToken`. | Scratch MockMvc test in this workspace (reverted): `GET /api/user/recall-stats?timezone=Asia/Bangkok` with `Bearer <makeMe.aUserToken()>` → 200; with `Bearer not-a-token` → 401. `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.ScratchBearerRecallStatsMvcTest'`, 2026-11-03, BUILD SUCCESSFUL. | Settled. In tests `currentUser` is a `@TestBean`, so token→user resolution is not observed through MockMvc; the fetcher uses the same lookup the filter just used. |
| The settings tab has a state after load with no reviews and no probes, where the control must also appear. | `RecallStatsSettingsTab.vue` `recall-stats-empty` branch. | Read the template at `245c17e44f`. | Present; slice 3's spec drives that state. |

## Outside-in proof

| Story example | Owning slice | Proof |
| --- | --- | --- |
| 1. Mixed events and probes, `Asia/Bangkok`, downloaded from the tab | 1 (events), 2 (probes), 3 (download) | Controller test: event count, order, kinds, `+07:00` offsets, timezone and export time; probes in order; frontend spec: control saves the API response as a dated `.json` |
| 2. Two answers on one tracker share ids; second has the state the first left | 1 | Controller test |
| 3. No probes → empty probe list, events present | 2 | Controller test |
| 4. Probe taken, probe later disabled → still exported | 2 | Controller test |
| 5. Excluded answer → present with exclusion time and reason | 1 | Controller test |
| 6. Agent with token gets the JSON; no credential → refused | 1 | MockMvc `GET` with a user token → 200 with the JSON; with no credential → 401 |
| Accidental-match spelling answer → grade and confusion events share an answer id | 1 | Controller test |

## Slices

### 1. Recall events through the export API

Type: Behavior
Status: done
Proof: new `UserRecallHistoryExportControllerTest` (extends `ControllerTestBase`,
calls `UserController` directly, plus one MockMvc case for example 6);
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.UserRecallHistoryExportControllerTest'`

Behavior: a signed-in learner with answered recall prompts (MCQ and spelling,
including an excluded answer and an accidental match), a just review, and a
tutor feedback → `GET` the export with `timezone=Asia/Bangkok` → the response
names the timezone and export time and lists one event per recall log, oldest
first, each with the fields the story lists and timestamps ending `+07:00`;
the probe list is empty. Without a signed-in user or valid token → 401.

Accepted (2026-11-03): `GET /api/user/recall-history-export` returns
`RecallHistoryExport(timezone, exportedAt, recallEvents)`, with the answer
details as a nested `answer` object (null for a just review or tutor feedback).
The test class has 11 passing cases, covering trackers removed from recall and
notes in trash. The regression runs `User*`, `RecallPrompt*`, `MemoryTracker*`,
and `*RecallLog*` also pass. The `dailyProbes` field is not in the response
yet; slice 2 adds the field together with its content.
Learnings: `pnpm backend:test:worktree` takes one `--tests` pattern per run;
MySQL `FLOAT` columns round, so stability assertions compare persisted values.

### 2. Daily probes in the export

Type: Behavior
Status: done
Proof: same test class, same command.

Behavior: a learner with stored daily probes (one taken while the probe was
enabled, the setting now off) → `GET` the export → every probe, oldest first,
with completed time (zone offset), speed, accuracy, lapse count, variability,
and trials; a learner with no probes still gets their events.

Accepted (2026-11-03): the response gains `dailyProbes`. The export's
nested records are published under their own schema names
(`RecallHistoryEvent`, `RecallHistoryAnswer`, `RecallHistoryDailyProbe`)
because springdoc names a schema after the simple class name; this corrects
G1. The refactor moved the `trials_json` shape into one JPA converter, so
`POST /api/daily-probes` now returns `trials` instead of `trialsJson`; no
client read that field. The export tests (14), `*DailyProbe*` (13), and
`*RecallStats*` (62) pass, and frontend `vue-tsc --noEmit` passes.
CI repair (run 41568886799): `lint:all` failed at 6932a70eb6 in
`e2e_test/config/cucumberProofRunner.test.mjs`, a test this story did not
touch. Its `reporter` kept Cypress's timing-dependent column padding and
attached the run totals to whichever spec ran last. The repair normalizes
the padding and collects the totals separately. It adds two minimal tests;
the full file passed 4 of 4 runs.

### 3. Download recall history from the recall stats tab

Type: Behavior
Status: done
Proof: `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts` (mock the
generated `UserController` export call with `mockSdkService`, mock
`file-saver` as `NoteExportForm.spec.ts` does);
`CURSOR_DEV=true nix develop -c env -u NODE_ENV pnpm frontend:test tests/pages/settings/RecallStatsSettingsTab.spec.ts`

Behavior: the recall stats tab has loaded, with data or in its empty state →
the learner activates the download control → the export is requested with the
browser timezone and its response is saved as a `.json` file whose name holds
the export date. Requires the regenerated API client (`generate-api-client`)
after slice 1.

Accepted (2026-11-03): a "Download recall history" button sits at the top
of the tab whenever stats have loaded, with data and in the empty state. It
saves the export response as `recall-history-<local date>.json`, where the
date comes from `localDayIso` in the browser timezone.
`RecallStatsSettingsTab.spec.ts` (6 tests),
`RecallStatsSettingsTab.dailyProbe.spec.ts` (3 tests), and
`vue-tsc --noEmit` pass.

## Execution complete

Product advice:
- No product change is needed from this execution. SEED-100#story-2 waits on
  the condition "A signed-in user can export their entire recall history with
  their daily probe results from production". The condition holds only once
  this story is on `main` and released. Story wrap-up should record the
  dependency as satisfied at that point, not before.
- Known limit, carried from the story: one timezone applies to the whole
  history, so story 2 should state that limit when it reads "morning".
- Watch: the proof-runner test still leaves any `mm:ss` run-total durations
  unnormalized; this is unconfirmed. If that CI flake recurs, inspect the
  `(Run Finished)` rows first.

## Plan concerns

Refinement: not needed; the three slices are separate outcomes with their own
proof loops, each a few minutes of work. No remaining slice-specific concern
was found in this review. Slice 1 is the largest (query, DTO, formatting,
and seven example cases); if it overruns, split answer-detail fields from the
event list at the next safe point.


## Story obligations

### G1. OpenAPI schema names of the export's nested records clash (first reported in slice 1)
Reported: slice 2 — "The nested record RecallHistoryExport.Answer is published as the existing entity Answer schema, so the OpenAPI description of the export lacks questionType, promptCreatedAt and createdAt; RecallHistoryExport.DailyProbe would clash with the entity DailyProbe schema the same way."
Story clause: "an
  authenticated `GET` under `/api`, described in OpenAPI"
Disposition: proved by slice 2: open_api_docs.yaml RecallHistoryEvent/RecallHistoryAnswer/RecallHistoryDailyProbe schemas; UserRecallHistoryExportControllerTest.anAgentWithTheLearnersTokenGetsTheJson answer.questionType and dailyProbes JSON
Commitment: 2a.3
