# Trust learning-session and assimilation E2E feedback

**Source:** [selected story](../../seeds/SEED-115-reliable-learning-e2e-feedback.md#trust-learning-session-and-assimilation-e2e-feedback)
**Identity:** SEED-115#trust-learning-session-and-assimilation-e2e-feedback

## Goal and scope

Contributors get reliable feedback about commissioned report scheduling and
note/property assimilation outcomes on fresh and reused local E2E data. Correct
the E2E proof while preserving product behavior, scheduling arithmetic and the
remaining feedback, grade, recall-count, stability and difficulty assertions.

This is the bounded E2E correction identified by the seed's dated SEED-113
execution evidence. Runner/database-reset changes, scheduling changes, new API
or schema contracts, panel reload correction and device usability observation
are outside this story. The queued SEED-113 panel correction is independent;
neither story requires the other to be delivered first.

## Existing solutions and current decisions

- PFE: reuse the UI-triggered report response's `recordedAt` from
  `POST /api/learning-sessions/record`. Extend
  `recallLearningSessionMethods.recordLearningSessionReport` to retain that
  timestamp. Observe each commissioned tracker through the existing
  `testabilityRecall.memoryTrackerForNote` / `NoteController.getNoteInfo` path.
  Put outcome assertion work in the page/testability object and keep steps thin.
  Browser `Date.now()` is unsuitable: the day/hour step moves backend time,
  whereas `@mockBrowserTime` installs a separate browser clock.
- PFE: reuse `I should see a {word} memory tracker` and
  `expectNoteLevelMemoryTracker` for the existing Understanding tracker. Keep
  `expectUnderstandingAdded` and explicit `nextItem` for this visit's success.
  Availability after spelling is its own positive outcome.
- PFE: `NoteAssimilationModes.vue` already owns one
  `[data-testid="note-assimilation-modes"]` root in either placement. Both
  wide and compact optional modes render inside it; `AutoCollapseDropdown`
  inlines this content. Change the existing `assimilationPage/shared.ts`
  selector owner rather than adding a parallel page object. Keep property-row
  selection in `noteRichPropertyAssimilationMethods`.
- These are local test-support corrections within existing product boundaries.
  No consequential architecture decision or North Star change is needed.
  Backend `MemoryTrackerCorrectRecallSchedulingTest`,
  `MemoryTrackerCorrectRecallDueFuzzTest` and
  `MemoryTrackerRebuildFromRecallHistoryTest` retain scheduling arithmetic,
  spread bounds, distinct-ID variation and deterministic replay ownership.
- Do not widen the note scope to spelling dialogs, confirmation dialogs or
  refinement modals. Those surfaces keep their own scopes and loading waits.
  Secondary-action eligibility assertions prove surviving behavior and stay.

## Premises and observations

Observed on 2026-11-09 in this preparation's owned worktree at
`26cd1d591fc83ac7b042b12fbc504f38b32ffecb`. The ordinary baseline used newly
provisioned allocation `wt_6b88e9064b794d369dfc4ab232df7de7`, database
`doughnut_e2e_wt_6b88e9064b794d369dfc4ab232df7de7`, browser origin
`http://127.0.0.1:62019`. The probe reused that allocation. Cypress was
16.2.1, headless Electron 146, Nix Node 26.13.0, local E2E backend; the mixed
batch used the runner-owned private OpenAI mock and no paid service.

| Premise / consuming operation | Observation and result | Remaining implementation proof |
| --- | --- | --- |
| The report scenario fails on an exact interval while its other outcomes hold | Command A below, unmodified E2E source: 3 scenarios passed; report scenario failed only at expected 199 versus observed 240 hours. Read-only query `SELECT id, type, last_recalled_at, next_recall_at FROM memory_tracker ORDER BY id` in this allocation returned commissioned IDs 1/2, last recall `1976-06-03 01:30:00`, next recall `1976-06-13 01:30:00` / `1976-06-03 06:30:00`. | Slice 1 accepts the corrected assertion with its ordinary fixture as well as varied IDs. |
| The actual UI report response supplies a usable report timestamp and both graded trackers are scheduled after it | Disposable edit captured the actual POST response, compared both tracker timestamps, and first assimilated existing Saludos as ordinary in the report scenario to vary allocation. Command C below passed all 32 scenarios. Probe observations: report `1976-06-03T01:30:00.000Z`; Hola ID 2 next `1976-06-10T14:30:00.000Z`; Gracias ID 3 next `1976-06-03T06:30:00.000Z`. | Final helper/step placement, ordinary-fixture corrected run, and final consumer trace belong to execution. |
| The new scheduling proof detects a graded tracker that is not moved after report time | Second disposable probe, same revision/allocation/profile and Saludos input, Command A: after `markAsRecalled`, temporarily set only Gracias's `nextRecallAt` to `now` in `LearningSessionService.record`. The report scenario passed Hola's future check, then failed on Gracias ID 3 with next recall and report both `1976-06-03T01:30:00.000Z`; other three scenarios passed. The new checks preceded the due-list read solely to localize the fault; that read does not mutate scheduling. The valid-state Command C observation above used the same timestamp capture, tracker query and comparison. | Discrimination is established; final implementation still needs slice 1 acceptance. |
| An explicit note root supports the current action/status/secondary consumers, including a simultaneous property panel | Same Command C probe rooted Cypress controls, tracker status and raw-document secondary queries, and opened/focused topic before reading note count 0 and property count 1 in `property_memory_tracker.feature`. All nine features / 32 scenarios passed in 1m41s spec time. | Final candidate must preserve these journeys; no compact-device usability result is claimed. |

All disposable backend and E2E edits were restored exactly before this plan's
final readiness review; only preparation records remain changed. This is planning evidence,
not delivered code or final acceptance. The baseline reproduced the symptom on
fresh data, so the predecessor's attribution to reused IDs alone is not a
complete explanation. Scenario reset uses TRUNCATE; repeated commands alone
prove no ID variation. The explicit fixture variation above establishes it.

Before these sequential batches, the local host had 16 logical CPUs, 48 GiB
RAM, load 10.30/8.54/8.31 and other Node/browser work running. One owned batch
at a time was chosen; unrelated processes were left alone. Recheck actual host
load/memory and known competing work before execution's expensive proof. Do
not assume these observations describe a future or remote runner.

Before the discrimination run, load had fallen to 7.02/8.07/9.48 with 319,316
free memory pages. One standalone feature was run sequentially on the same host.

## Outside-in proof ownership

| Surviving outcome / correction | Owner | Observable proof and inspected boundary |
| --- | --- | --- |
| Recording the report stores feedback, closes due session, credits both trackers and schedules both after report time | 1 | Existing commissioned report scenario, UI submission through `recallLearningSessionMethods`, returned `recordedAt` and persisted tracker query. Preserve both counts, Hola grade 4, tutor prose, Stability 199 and Difficulty 1. Run corrected ordinary fixture alone on fresh data and in a reused-data batch; observe the second ID allocation with a disposable fixture variation. |
| Reopening shows Understanding tracker; spelling leaves Understanding available; immediate success waits for explicit Next item | 2 | Walkthrough reopening and skipped-note scenarios use the existing tracker step; spelling's final scenario uses a positive availability step. Existing walkthrough success/title → Next item → next note/progress proves deliberate advancement. Commands B and C. |
| Skip and Return to sequence retain their eligibility and resulting sequence outcomes | 2, 3 | Existing walkthrough skip/return/direct Understanding journeys and raw-document secondary checks. Remove only prechecks already proved by subsequent successful actions; keep sequence destination, result and eligibility assertions. Commands B and C. |
| Note and property interactions reach their own trackers with both panels open | 3 | Extend the first `property_memory_tracker.feature` scenario before the note count read: explicitly open topic and observe its panel open. Follow the note tracker for count 0, return, and follow property's tracker for count 1. Command C. |
| Shared note queries preserve all current callers | 3 | Command C's nine feature consumers cover direct Understanding, Commissioned, Spelling, table-shaped assimilation, property coexistence/reification, spaced repetition, daily probe and recall/browse setup. Trace the final diff for additional consumers before acceptance. |

## Ordered slices

### 1. Prove commissioned report scheduling for every graded tracker
Type: Structure
Status: planned
Proof: First proof row; Command A with corrected ordinary fixture, then its
ID-varied replay and inclusion in Command C.

Correct the report scenario's scheduling proof using actual report time and
both Hola and Gracias tracker queries. Preserve its other outcomes. Delete
the single-use exact-hours step and `expectHoursBetweenLastAndNextRecall`
helper with any orphan-only support; retain `labeledPair`, which also proves
assimilation-time scheduling. The concrete weakness is an exact spread
interval pinned to one data arrangement and schedule proof of only one graded
tracker. Product behavior stays unchanged.

Sizing: about 5 minutes of edits/cleanup plus the focused E2E startup/runtime
exception. Safe stop: the commissioned feature passes with meaningful scheduling
feedback, independently of the other two corrections.

### 2. Name the assimilation outcomes the scenarios actually establish
Type: Structure
Status: planned
Proof: Second and Skip/Return proof rows; Command B.

Use the existing Understanding tracker step for reopening, name that scenario
positively, and name the spelling completion check as Understanding
availability. Delete redundant prechecks in skip/return and direct-success
journeys where subsequent successful actions already prove the outcome. Delete
superseded step definitions/helpers and orphan-only dependents. Preserve this
visit's success/title/Next item sequence separately from reopened tracker state.
The concrete weakness is misleading enabled/disabled feedback. No product
eligibility rule changes.

Sizing: about 5 minutes plus the focused E2E startup/runtime exception.
Safe stop: the two affected features express the actual outcomes and stay green.

### 3. Scope note interactions to the note panel beside property controls
Type: Structure
Status: planned
Proof: Last three proof rows; Command C.

Unify note action, tracker, success/Next and secondary-action queries under the
existing note root, including raw `doc.querySelectorAll` paths. Reuse already
rooted result/readiness selectors. Delete the property-ancestry exclusion
predicate and its unused re-export, repairing comments and any orphan-only
dependents. Keep property and external dialog scopes unchanged. Strengthen the
existing property recall scenario as mapped above. The concrete weakness is
two competing note-query strategies around controls shared with property rows.

Sizing: about 5 minutes plus the nine-feature E2E startup/runtime exception
(the preparation probe measured 1m41s spec time). Safe stop: all consumers
observe the correct note/property outcomes through one note scope.

## Verification and delivery

Run from the execution checkout through Nix. Non-interactive shells used
`NIX_CONFIG='extra-experimental-features = nix-command flakes'`; Command C
also needs a non-empty placeholder `OPENAI_API_TOKEN` for its private mock.
No paid-service option is needed.

Command A (commissioned standalone):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/learning_session/commissioned_learning_session.feature
```

Command B (outcome wording):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature,e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature
```

Command C (shared-query consumers and reused allocation):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/learning_session/commissioned_learning_session.feature,e2e_test/features/assimilation/assimilation_walkthrough.feature,e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature,e2e_test/features/assimilation/assimilation_page_types.feature,e2e_test/features/recall/property_memory_tracker.feature,e2e_test/features/relationships/reify_property.feature,e2e_test/features/recall/spaced_repetition.feature,e2e_test/features/recall/daily_probe.feature,e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
```

At slice 1, record actual IDs/report/schedule timestamps for the corrected
ordinary fixture and a disposable earlier Saludos allocation; restore that
variation before commit. Run the corrected ordinary feature alone on a fresh
isolated allocation and in the reused-data batch. A new execution worktree's
first invocation normally supplies fresh data. If its allocation was already
used, obtain the fresh observation in a disposable owned checkout at the same
candidate through the documented isolated runner, then retire its allocation
and checkout; never rebuild or reassign an existing recorded database.
Retain literal commands, revision, setup changes, results and limitations with
accepted proof in this plan. Never infer changed IDs from database age.

Consumer trace starting points: changed helper/step names and their phrases,
report recording, `noteLevelTrackerStatusElement`, note actions and secondary
queries. The nine Command C features exercise changed queries.
`assimilation_property_order`, `edit_when_assimilating`, `note_refinement`,
`relationship_edit_and_remove` and `notebook_files` use unchanged root readiness,
open or absence methods; no changed-query effect reaches those observations
when those methods retain their contracts. Reassess these exclusions on the
actual diff with the installed consumer-trace script; record run/reused/excluded
dispositions and cover any additional affected consumer.

Each slice has a separate one-time removal reading across product code, tests,
fixtures, docs and guidance for the exact retired symbols/step phrases. Delete
orphan-only dependencies transitively. For slice 1 search the retired exact-hour
phrase/helper, not the retained stability value 199. Preserve dated historical
observations with their provenance; do not rename an observed action to clear
the sweep. Readings are separate from surviving-outcome proof; add no absence
test or runtime guard for a retired interface.

Local verification is the focused E2E proof above, not the whole E2E suite.
Backend/API and frontend production code remain unchanged; regeneration and
frontend/backend unit runs become applicable only if an actual change requires
them. Apply AGENTS.md's execution wrap-up per slice: Jidoka → fresh independent
post-change refactor → API generation when needed → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update this plan → agent commit
with check-only lint hook → push. Keep this plan for retrospective/Story Wrap Up.

## Plan review

Independent boundary review selected three correction leaves because scheduling,
outcome wording and shared-query scoping each have a distinct proof loop and safe
stop. The common model remains existing report/tracker state and the existing
note/property panel owners; no parallel representation is introduced.
Slice-plan refinement was not needed: boundaries, proof ownership and sizing
are explicit, and the scratch consuming-operation probe settled the integration
premises. No remaining slice-specific concern was identified. The ~5-minute
target includes edits and cleanup; runner startup and focused test runtime are
the stated exception to the 10-minute leaf limit. An implementation overrun
still requires the project's finer-decomposition handling.
