# Require fingerprints for new recall questions

**Identity:** SEED-073#require-fingerprints-for-new-recall-questions
**Source:** [Require fingerprints for new recall questions](../../seeds/SEED-073-learning-refinement-and-retrieval.md#require-fingerprints-for-new-recall-questions)
**Source review:** [Source review](source-review.md)

## Execution context

- Story Branch Mode in the established worktree on
  `codex/require-fingerprints-for-new-recall-questions`. The integration checkout
  is `/Users/terryyin/git/doughnut`. The authorized integration target is
  `origin/main`.
- Publisher `dashboard-territory.local-doughnut` and agent `ziqing-chan`.
  Starting revision `361a576fd1c868e2bb4f85e778847560ad27c92e`.
  Claim published to `origin/main` and initial story branch accepted at
  `fe2fbc79c6b909d39726e3afd1b62cca93638274`. The branch observer covers
  subsequent story-branch pushes.
- Codex coordinator `codex-70d44cf2-3262-4d53-b493-6416c327978c`, cell
  `g289:7`, session `66175`, PID `36512`, and observer directory
  `/tmp/dough-ci-501/watch-9X5ndC`. The selected GitHub Actions workflow
  is `ci.yml` for `nerds-odd-e/doughnut`. The first observer stopped at the
  approval checkpoint. The resumed story-branch observer is cell `g323:26`,
  session `70579`, PID `56737`, directory `/tmp/dough-ci-501/watch-sKqSUJ`.

## Goal and scope

Keep waiting multiple-choice questions backed by the focus note input used to
generate them. Remove the old fingerprint-free waiting prompts from production
once, preserve answered history and contests, then remove the temporary
migration and waiting-queue compatibility code.

## Current decisions

- Keep the fingerprint required in `FocusNoteInput` and optional in the stored
  `MultipleChoicesQuestion` JSON. Construct the typed input only when the
  fingerprint is present and valid. Reuse the existing SHA-256 generation
  input and metadata carried by the batch renderer and importer. A missing
  or invalid fingerprint belongs to the existing invalid-result outcome
  described by the importer. Preserve optional note ID and read time when a
  real legacy fingerprint is present. History reads the stored question.
- The purge treats a missing or null fingerprint as missing evidence, including
  an unreadable legacy payload with no extractable fingerprint. It operates on
  unanswered, uncontested MCQ prompts. Use the ownership rule from
  `MemoryTrackerLifecycle` and `McqRepository.deleteUnreferencedByIds`:
  remove the selected prompt and delete its question only if no prompt
  references it. Preserve conversation content with the established prompt
  detachment, and preserve tracker due times and answered grades.
- Store the temporary operator code outside the application startup migration
  chain under the [database migration guidance](../../../.agents/skills/db-migration/SKILL.md#destructive-dml-delete-gated-update).
  Drop the temporary code and proof after production verification. Generation
  happens through the ordinary pathways as trackers come due.
- Follow [One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
  and [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
  Read the existing note projection for provenance and keep recall data in the
  application database. Preserve the answers, grades and schedule governed by
  [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#recall-history-and-current-state).
  Reuse the batch invalid-result outcome under [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md#usage).

## Baseline and proof routes

On 2026-11-23 at `88449786ef`, the isolated MySQL test profile passed all
33 cases in the following selection:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests '*MemoryTrackerBatchRecallPromptControllerTest' \
  --tests '*MemoryTrackerQuestionDeletionControllerTest' \
  --tests '*QuestionGenerationBatchCandidateMemoryTrackersTest' \
  --tests '*QuestionGenerationBatchRowImportServiceTest' \
  --tests '*MemoryTrackerRecallPromptFreshnessControllerTest'
```

The controller imported a metadata-free batch result, saved a waiting prompt,
and generated again at recall. A batch with the rendered fingerprint reused
its saved question. The shared-question deletion case removed a waiting
sibling, kept the answered prompt and conversation evidence, and retained the
shared MCQ. Batch planning reused a matching fingerprint without a timestamp.
The [previous shared-question probe](../../done/SEED-073_replace-stale-waiting-questions.json)
also observed that an unguarded MCQ delete cascades to its answered prompt.

The temporary operator must prove its own selection and writes on the same
MySQL major version as [production](../../../docs/gcp/prod_env.md#4-production-database-cloud-sql-for-mysql).
The live count and access are operator-held observations. In slice 4, the
read-only preview is the first production database step and blocks application
when the target or candidate set differs from the reviewed scope.

## Outside-in proof ownership

| Outcome | Slice | Observation |
| --- | --- | --- |
| Successful batch fixtures carry the request's real provenance | 1 | Existing successful import and maintenance cases pass with provider responses derived from their request or fixture focus. |
| New writes require provenance, and history still reads old questions | 2 | A valid provider result reaches recall with its fingerprint; a missing or invalid fingerprint records a failed row and creates no prompt. The on-demand result carries the real fingerprint. A preexisting answered prompt without one still reads with its original answer evidence. |
| The operator can purge the intended rows while preserving history | 3 | Preview and apply the same temporary command on isolated MySQL. Inspect the surviving waiting queue, earlier answers and grades, contested prompts, shared questions, conversation content and due times. A repeat preview returns an empty eligible set. |
| Production is cut over and the temporary operator can be retired | 4 | Record the live write-enforcing build, preview and apply counts, then verify zero eligible prompts and preserved sampled history. |
| The normal recall paths work after temporary support is removed | 5 | Observe regular batch and on-demand generation, reuse with a matching fingerprint and absent read time, and historical readback of older answers. The one-time removal sweep is a reading, separate from the retained tests. |

## Ordered slices

### 1. Carry request fingerprints in successful batch fixtures

Type: Structure
Status: done

Extend the existing batch payload fixture owner so tests exercising successful
import carry the fingerprint from the rendered request or the fixture's authored
focus. Keep malformed-provider inputs deliberate. This prepares the importer
validation in slice 2. **Proof.** Run the batch test family, including
maintenance, import and atomic reimport, since they share the fixture contract.
Production import behavior stays as in the baseline.

**Accepted proof.** At `fe2fbc79c6` with the slice changes, 132 selected tests
passed against isolated MySQL.

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests '*QuestionGenerationBatch*' \
  --tests '*MemoryTrackerBatch*' \
  --tests '*MemoryTrackerRecallStalePromptRetirementControllerTest'
```

`MemoryTrackerBatchRecallPromptControllerTest` observed the rendered note ID,
read time and real fingerprint on the imported question, reuse by prompt ID,
and continued handling of the deliberate metadata-free result.
`QuestionGenerationBatchMaintenanceServiceTest` observed the completed
provider batch reach a saved prompt. The row and atomic import suites observed
import, rollback and reimport. The independent review left the accepted proof
unchanged. The worktree's Git evidence directory retains `slice1-return.md`,
`slice1-consumer-trace.txt` and `slice1-batch-fixtures.log.run.json`.

Published to the story branch at `50a78657a9c2b53195e85b6ba7b5ddef193f1295`.
CI run [45100262451](https://github.com/nerds-odd-e/doughnut/actions/runs/45100262451)
completed with one failed job. Its MySQL prerequisite downloaded the first
compatibility package, then hit three connection timeouts on the second before
linting or type generation started. Both pinned Ubuntu package URLs returned
HTTP 200 in the local check. The next increment receives its own CI run.

### 2. Require a real fingerprint on new questions

Type: Behavior
Status: done

Make the typed `FocusNoteInput` invariant explicit and validate provenance at
the batch result boundary before persisting. Use the existing failed-row path.
Reuse the on-demand generation input. Keep the stored JSON reader able to
read historical questions with an absent field. Keep waiting reads compatible
until the production purge in slice 4 and retire that interim support in
slice 5. **Proof.** Replace the metadata-free import success example with the
active rejection at the same boundary. Check valid import and on-demand
persistence, and read an existing fingerprint-free answered attempt through
recall history.

**Accepted proof.** At `50a78657a9` with the slice changes, 196 selected tests
passed against isolated MySQL 8.4.11.

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests '*QuestionGenerationBatch*' --tests '*MemoryTrackerBatch*' \
  --tests '*MemoryTrackerRecall*' \
  --tests '*MemoryTrackerQuestionPersistenceControllerTest' \
  --tests '*MemoryTrackerQuestionDeletionControllerTest'
```

The batch recall controller cases observed the original rendered fingerprint
on successful import and reuse by prompt ID. Five responses with missing or
invalid fingerprints each persisted the existing failure and left the MCQ
count and prompt queue unchanged. The on-demand persistence case observed
the actual generation input, and the history case read the saved wording,
choices, key, answer and grade from a fingerprint-free question.
The batch planning and stale-prompt cases passed with the interim waiting
behavior. The worktree's Git evidence directory retains `slice2-return.md`,
`slice2-consumer-trace.md` and `slice2-recall-fingerprints.log.run.json`.
Published to the story branch at `7fc809f31378c7b2b35ae7ec6304d1e46fa62d20`.

### 3. Rehearse the one-time purge on MySQL

Type: Behavior
Status: done

Supply a temporary operator command with read-only preview and explicit
apply. Apply the same eligibility filter used by preview and recheck the
unanswered condition in the write transaction. Delete selected prompts
first, preserving linked conversations through established detachment.
Then delete only their newly unreferenced MCQs. **Proof.** Use the actual
operator command on the isolated schema with both a shared and an unshared
fingerprint-free waiting question, a current waiting question, a contested
prompt, and an earlier answer with choices and grade. Compare preview to
the resulting queue, history and tracker due time. Record counts on repeat.

**Accepted proof.** At `7fc809f313` with the slice changes, both rehearsal
scenarios passed on a disposable MySQL 8.4.11 instance loaded with the
schema-only dump of the migrated worktree test database.

```sh
CURSOR_DEV=true nix develop -c node --test \
  scripts/ops/purge-fingerprint-free-waiting-questions.test.mjs
```

The [operator](../../../scripts/ops/purge-fingerprint-free-waiting-questions.mjs)
previewed four eligible prompts. Apply required a confirmation bound to the
server, schema and exact prompt/MCQ pairs. It refused a different schema and
refused a changed set after a learner answered one prompt, even though the
count stayed at four. A refreshed preview and apply removed four prompts and
three newly unreferenced questions. The repeat preview returned zero.
The persisted checks kept the shared answered question and its original grade,
the contest, current fingerprinted and non-MCQ prompts, the tracker due time,
and the detached conversation's message and note correction.
The worktree's Git evidence directory retains `slice3-return.md`,
`slice3-consumer-trace.md` and the final `slice3-refactor-rehearsal.log.run.json`.

### 4. Apply the production cutover

Type: Behavior
Status: done

**Human checkpoint.** Before any production change, obtain authorization for
the release identity and destructive operation. In Story Branch Mode that
same go-ahead includes [Dough Land](../../../.agents/skills/dough-land/SKILL.md)
`--keep-worktree` for the preceding slices so the release can be selected
from `main`. Follow the [Application Release runbook](../../../docs/gcp/conditional-backend-deploy.md)
and verify the enforcing build is live before applying the purge. Preview
on production to verify the target and capture the eligible prompt count.
Use the reviewed operator command for the authorized set. **Proof.** Record
the release and build SHAs, aggregate affected counts, the final zero-row
preview and preserved sampled history. Retain the receipt for slice 5.

**Deployment receipt.** On 2026-11-23, following approval, Dough Land
integrated story tip `f2ade45609c6798360a0d60eb6178f6f5079d508` at
`a999b7975f4dc3eaff34456bd18f041fbcec1fd1` on `origin/main` and retained
the worktree. The dashboard recorded the accepted integration. Main CI run
[45103906894](https://github.com/nerds-odd-e/doughnut/actions/runs/45103906894),
attempt 1, passed and supplied both release artifacts.

The immutable tag `v1.3.82` selected that same release and backend build
SHA. Application Release run
[45104685033](https://github.com/nerds-odd-e/doughnut/actions/runs/45104685033),
attempt 1, passed admission and the
[GCP publication job](https://github.com/nerds-odd-e/doughnut/actions/runs/45104685033/job/137463296123).
The production health endpoint returned `prod` with the full build SHA.
The live SPA entry script matched the CI artifact, and the CLI download
returned HTTP 200. The unauthenticated sign-in entry returned HTTP 303 to
the GitHub OAuth authorization endpoint.

**Production data receipt.** On 2026-11-23, health again identified
`a999b7975f4dc3eaff34456bd18f041fbcec1fd1` as the serving production build
before the purge. The reviewed operator previewed 8,335 eligible prompts
on Cloud SQL instance `doughnut-db`, database `doughnut`. A separate
read-only query matched the exact preview confirmation and counted 8,334
questions that would have no prompt afterward and one that had an answered
prompt sharing it. Apply confirmed the same set and deleted 8,335 prompts
and 8,334 questions. The repeat read-only preview returned zero eligible
prompts on the same server and database.

After apply, persisted data matched the pre-apply sample for the one shared
answered prompt and 12 legacy answered prompts, including wording, choices,
answer key, learner answer and recorded grades. The audit also preserved
12 contested prompts and contests, 12 fingerprinted waiting prompts,
12 waiting non-MCQ prompts and 20 affected trackers' due times. The
worktree's Git evidence directory retains the operator receipts
`slice4-production-{preview,apply,final}.json` and read-only snapshots
`slice4-production-audit-{before,after}.json`.
The receipt was published to the story branch at
`7d2ac14727e25d2e06e6b739b148a3bb0820e035`.

### 5. Retire temporary migration and legacy waiting support

Type: Structure
Status: done

After slice 4's production receipt, remove the operator code, its
migration-only proof, and compatibility code and tests specific to the old
waiting data. Keep the importer refusal, authored-content fallback for a
present fingerprint, and readable answered history. **Proof.** Run the
retained generation, reuse and history cases unchanged in intent. Read the
product and tests for retired migration and waiting-only compatibility
references as a one-time acceptance sweep.

**Accepted proof.** A separate read-only aggregate on the verified production
database found canonical fingerprints on all 190 remaining unanswered,
uncontested MCQ prompts. The on-demand, regeneration and batch write paths
all supply a validated `FocusNoteInput` before persisting a recall prompt.
The planning query excludes answered and contested prompts and trackers with
unanswered spelling prompts. The typed waiting reader uses that invariant.

At `7d2ac14727` with the slice changes, 196 selected tests passed on isolated
MySQL 8.4.11.

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only \
  --tests '*QuestionGenerationBatch*' --tests '*MemoryTrackerBatch*' \
  --tests '*MemoryTrackerRecall*' \
  --tests '*MemoryTrackerQuestionPersistenceControllerTest' \
  --tests '*MemoryTrackerQuestionDeletionControllerTest'
```

The batch maintenance case planned, submitted and imported a question for
an overdue tracker with an empty queue. The on-demand controller saved a
question with the real generation fingerprint for an empty queue. The batch
recall controller reused the original prompt with and without read time.
The importer refusal, stale waiting replacement and older answered-history
cases retained their existing observations.
The independent refactor split reuse into two named controller cases. Both
passed in the targeted rerun recorded in
`slice5-refactor-retained-reuse.log.run.json`.

**Removal reading.** The temporary operator and its rehearsal were deleted.
A source reading traced the nullable typed input and waiting-only fixture to
their removal, then searched product, test, documentation and guidance paths
for their retired names and found no remaining references. The worktree's
Git evidence directory retains `slice5-production-fingerprint-coverage.json`,
`slice5-return.md`, `slice5-consumer-trace.txt`, `slice5-removal-sweep.txt`
and `slice5-recall-fingerprint-retirement.log.run.json`.

## Execution complete

Product advice: Keep the current backlog order. The deployed write rule and
verified production purge establish the provenance used by recall without
changing stored answered history or scheduling. The retained tests cover both
replacement paths and fingerprint reuse, and the source sweep confirmed that
the temporary operational code and obsolete waiting-only support were removed.
The review found no correction to queue.

## Plan review

Slice plan refinement was unnecessary. Shared fixture preparation is
bounded separately from enforcement. A rehearsed operation precedes the
production checkpoint, and its receipt unblocks retirement. No remaining
slice-specific concern was identified.
