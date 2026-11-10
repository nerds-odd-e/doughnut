# Export recent note questions with personal selections

**Source:** [selected story](../../seeds/SEED-118-export-recent-quiz-questions.md#export-a-note-with-recent-quiz-questions)
**Identity:** SEED-118#export-a-note-with-recent-quiz-questions

## Goal and scope

Give a learner one Markdown artifact containing note context, up to ten newest
distinct note MCQs, Donut's answer key, and their own latest selected answers,
for external AI review. The seed owns the agreed examples and boundaries.
Creation time, then question ID, selects/orders questions; answer time, then
answer ID, selects the exporting learner's latest selection per MCQ. Both
orders are descending. Include unanswered and retained contested questions;
question selection adds no attempt-status or tracker-type filter.

Keep the unchecked default, existing note-read access, preview/copy/download,
and note-body token budget. The appendix contains complete selected questions.
Only the focus note supplies questions; related notes still supply ordinary
context. A guest has no personal selections. An option change refreshes the
result; failure cannot offer earlier text as the selected option's export.

Deferred: AI analysis, note mutation, separate recall-history reports,
historical note versions, and additional question/attempt metadata. No schema,
Portable content, CLI, MCP, or shared AI-rendering change is needed.

## Decisions, reuse, and structure

- The developer accepted newest-created note questions with latest own answer
  and the separately labeled Donut answer key on 2026-11-10. No scope question
  remains. The key is Donut's stored solution, not a claim of objective truth.
- PFE: extend the existing `/api/notes/{note}/ai-context-markdown` export,
  `NoteAiContextMarkdown` response, focus retrieval/rendering, and
  `JsonExportSection` preview/copy/download owner. Question-generation-request
  JSON and recall-history/statistics exports have different responsibilities.
- `Mcq` already owns stems, choices, key, note, and creation time; `RecallPrompt`
  and `Answer` own personal attempts through `MemoryTracker.user`. Reuse those
  records. A bounded question query owns recency; a current-user-scoped answer
  read owns latest selections. Do not copy the unanswered-recall eligibility
  rule: its contested filter serves scheduling, not this export.
- Compose the optional appendix at the export boundary. Keep the common
  `FocusContextMarkdownRenderer` unchanged so conversations, question generation,
  and automation retain their existing input responsibilities.
- Follow [ADR 0001, domain vocabulary](../../../docs/adrs/0001-ubiquitous-language.md#recall),
  [ADR 0002, authored/private content](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md#decision),
  and [ADR 0006, failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage).
  North Star's [accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
  keeps private learning data server-side; exporting it does not author it into
  the notebook tree. No conflict, new direction, or ADR exception is needed.
- `NoteController.java` is already 255 lines. Slice 1 moves its cohesive
  Markdown-export route and exclusive rendering dependency into a
  `NoteExportController`, preserving the HTTP path, response and method name.
  Keep graph retrieval in `NoteController`. Regenerate the SDK and migrate its
  one production caller and export tests to the generated service owner.
  Preserved proof: controller Markdown/access tests and mounted export tests.
  Do not cut unrelated controller methods or compress lines to meet the limit.
- Other expected edits at preparation: `NoteExportForm.vue` 82 lines,
  `McqRepository.java` 6, `RecallPromptRepository.java` 82, export controller
  tests 38, frontend export tests 88. Keep query/formatting responsibility
  cohesive; recheck every changed file against the 250-line rule.

## Outside-in proof ownership

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Existing Markdown route, usual context, budget and read authorization survive relocation | 1 | Existing controller tests, real MockMvc GET, mounted preview/download and refresh |
| Opt-in appendix has complete question/choices/key, newest ten, stable ties, only focus-note questions | 2 | Export controller with real persisted MCQs, deliberately different insertion/creation order, exact rendered sequence |
| Omitted/false inclusion and a zero-question note retain ordinary context | 2 | Real HTTP query binding/default plus controller output |
| Latest own selection, one entry per reusable MCQ, another learner's history stays private | 3 | Controller with repeated answered prompts, answer-time/ID ties, another learner's newer attempt, and unanswered data |
| Existing guest/read access remains useful | 1, 3 | Ordinary guest export baseline in probe; guest opt-in result and private-note refusal through controller/HTTP boundary |
| Checkbox, preview, copy and download use the same current result; clearing restores ordinary context | 4 | Mounted form using generated SDK contract, real copy component/clipboard boundary, downloaded Blob text |
| Pending/failed refresh cannot export stale text; visible feedback and retry recover | 4 | Deferred SDK responses and error followed by Refresh success; actual action states and preview contents |

Use `makeMe` data at stable boundaries, real persistence/rendering below the
controller, and SDK/file-saver/clipboard mocks only at external frontend
boundaries. Add `McqBuilder.createdAt` for deterministic question chronology;
existing `RecallPromptBuilder.withMcq`, `answerChoiceIndex`, `answerTimestamp`
and tracker `.by(user)` already express repeated and other-learner attempts.
Do not infer authorization of an answer from `assertReadAuthorization(prompt)`,
which checks the note; explicitly scope its data lookup to the current learner.

Controller HTTP binding and mounted browser behavior are the chosen high-level
proof boundaries. Generated API plus named MockMvc queries connect their wire
contract. SDK mocks do not prove a real cross-process browser/server journey;
this plan does not claim that observation or require an E2E stack.

## Premises and observations

Preparation observations used the owned workspace at
`439f3fea6888084507d6fe4a022e9b89c911d08b` on 2026-11-10.

| Consuming operation / premise | Observation and result | Remainder / owner |
| --- | --- | --- |
| Existing mounted form renders returned Markdown and current budget refresh | 14:22 Singapore: `./scripts/run.sh pnpm frontend:test tests/notes/NoteExportForm.spec.ts`; two Chromium tests passed | Checkbox, exact clipboard/Blob content and errors are new slice 4 proof |
| Those tests distinguish a missing rendered export | 14:31 Singapore: same command with a disposable edit replacing returned Markdown with empty text; both failed their preview assertions; source restored exactly | No claim that these tests distinguish the new appendix or private answer rules |
| Existing backend export/authorization and fixtures execute against this checkout's real test database | Read-only trace reaches `NoteController.getAiContextMarkdown`, its controller tests, `Mcq`, `RecallPrompt`, and builders; isolated wrapper and Nix runner located | State-changing database allocation/migration and consuming controller observation are bounded by the early probe below |
| Backend probe's local engine/tool prefix is available | `./scripts/run.sh mysql --protocol=TCP -h127.0.0.1 -P3309 -u root -N -e 'SELECT VERSION()'` returned MySQL 8.4.11; `./scripts/run.sh java -version` returned OpenJDK 25.0.3 | Presence only; neither proves allocation, migrations, controller behavior or generation; early probe remains required |
| Moving the export route retains its HTTP and generated-client contract | Route/response and sole production SDK caller traced; generation starts a backend application and writes generated artifacts | Slice 1 probe and post-move HTTP/generation/typechecks own the actual result; no generation success is claimed now |

**Bounded early probe — before slice 1 production edits.** In the execution
checkout, run `./scripts/run.sh pnpm backend:test_only` against its isolated
test database, including existing Markdown/read-authorization tests. Extend the
boundary fixture to exercise a real GET of the existing export with its token
query and ordinary Bazaar guest access. Observe the actual rendered context;
retain these useful ordinary-export regression cases. Reusable-MCQ/repeated-
answer fixtures belong to slices 2–3's real controller proof, which must query
persisted data rather than assume builder inverse collections are populated.
Observe one scratch missing-Markdown mutation being caught
by the export boundary test, then restore it. Run only the full backend suite,
as required by the backend skill. Record commands, data, results, and restored
scratch state before proceeding. No paid service is required.

Database allocation/migration is state-changing and the generation route starts
an application; their cheap source/command prefix is inspected now, with actual
consuming operations deferred to this first authorized execution step. A
failure stops dependent extraction/export work: diagnose or replan instead of
assuming the route or guest baseline works. New selection/formatting/answer
rules are future red/green behavior proof, not claimed existing behavior.

Host observation at 14:31 Singapore: 48 GiB physical memory, about 1.2 GiB free
and 12.6 GiB inactive; load 9.69/9.04/7.49, with other Node/Java work active.
No backend workload was launched in preparation. Reassess current execution-host
headroom before the full suite; run one owned backend command at a time and
keep backend tests and API generation sequential in this checkout.

## Ordered slices

Accepted interim behavior: slices 2–3 expose the opt-in API before the UI option
arrives in slice 4; slice 2's appendix has question content/key and slice 3 adds
personal selections. Ordinary export remains usable throughout.

### 1. Give Markdown export its cohesive controller boundary
Type: Structure
Status: planned
Change: Run the bounded early probe, then move the existing export route into
`NoteExportController` as described above. Preserve its path, response, token
semantics and read access. Regenerate the client and update the current form
and tests to the generated export service. Enables slice 2's optional appendix
without extending the already-oversized general note controller.
Proof: Existing Markdown and unauthorized-note tests plus real HTTP/default
query and guest baseline; mounted export baseline and frontend typechecks on
the generated client. Keep the graph controller's retained behavior green.
Sizing: about 5 minutes of edits/cleanup; full backend verification and API
generation are the explicit focused-verification wait exception.

### 2. Export the newest note questions with Donut's answer key
Type: Behavior
Status: planned
Behavior: Request the Markdown export with optional `includeRecentQuestions=true`
→ ordinary context plus complete stems, ordered choices, and separately labeled
stored keys for up to ten newest distinct focus-note MCQs. Omitted/false means
ordinary context; zero questions still exports context. Preserve body budget.
Add the default-false query field, bounded recency selection and appendix
rendering at the export owner; regenerate the SDK. Include retained contested
MCQs naturally, without borrowing recall scheduling filters.
Proof: Export controller real persisted data: canonical shape, twelve-question
selection with non-insertion chronology and equal timestamps, another note's
questions, no prompts, zero questions, and omitted/false/true HTTP query binding.
Sibling cases assert their distinguishing delta, not the whole shape again.
Sizing: 5–8 minutes of edits/proof-local cleanup; backend suite and generation
waits are explicit exceptions. Personal selections follow in slice 3.

### 3. Attach only the exporting learner's latest selection
Type: Behavior
Status: planned
Behavior: Selected questions include repeated attempts and other learners'
attempts → one entry per MCQ contains this learner's latest recorded selection,
ordered by answer time then answer ID; unanswered questions remain included
without a personal selection. Guests still get question content and keys.
Use current-user ownership in answer selection; do not make this conditional
on understanding/property tracker kinds or active scheduling eligibility.
Proof: Controller with same-MCQ repeated prompts, equal answer times, a newer
other-learner attempt, an other-only answered question and guest access. Keep
existing unreadable-note refusal. Deliberately different choice text identifies
the selected answer separately from Donut's key.
Sizing: about 5 minutes plus the required backend verification wait.

### 4. Choose inclusion and export the current preview reliably
Type: Behavior
Status: planned
Behavior: Open Export Note Data → unchecked usual context. Select inclusion →
refresh; successful preview, copied text and downloaded Markdown agree with
the selected option. Clear inclusion → usual context. While pending or after
failure, previous text cannot be exported; show shared API feedback and let
existing Refresh retry. Preserve the token-budget interaction and filename.
Use the generated query, existing shared export UI and `apiCallWithLoading`
for user actions. Clear stale text at request start and serialize option changes
through existing loading controls; no request-version/cancellation framework.
Explain that the body budget excludes the added complete question appendix.
Proof: Mounted form default/check/clear, deferred response, error/retry and
budget refresh. Observe exact preview, clipboard argument and Blob text using
real internal components. Run frontend tests and both required typechecks.
Sizing: 5–8 minutes plus full frontend-suite/typecheck verification waits.

## Commands, delivery, and concern review

Run repo commands through `./scripts/run.sh` (the documented Nix wrapper):

```sh
./scripts/run.sh pnpm backend:test_only
./scripts/run.sh pnpm generateTypeScript
./scripts/run.sh pnpm frontend:test tests/notes/NoteExportForm.spec.ts
./scripts/run.sh pnpm frontend:test
./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit
./scripts/run.sh pnpm -C frontend exec tsc --noEmit
```

Backend skill requires the full backend suite for its changes; frontend skill
requires tests plus both typechecks at acceptance. Follow actual consumers of
the changed generated service/query/Markdown through the installed consumer
trace, giving each run/reused/excluded disposition. No migration or API hand edit.

AGENTS.md delivery gates: Jidoka → fresh post-change refactor agent → API
generation when needed → coordinator `./scripts/run.sh pnpm format:changed`
once → plan/evidence update → agent commit/check-only lint hook → push.
Implementation targets ~5 minutes per leaf; scrutinize >5 and decompose before
>10 except the stated verification waits. Preparation grants no execution.

Refinement was not needed: four bounded leaves separate cohesive export
ownership, question selection, private answer selection, and UI interaction.
They share one domain model and mapped proof; the state-changing early probe
bounds backend/generation prerequisites. No remaining slice-specific concern
or human decision was identified in this review.
