# Require fingerprints for new recall questions

**Identity:** SEED-073#require-fingerprints-for-new-recall-questions
**Source:** [Require fingerprints for new recall questions](../../seeds/SEED-073-learning-refinement-and-retrieval.md#require-fingerprints-for-new-recall-questions)

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
Status: planned

Extend the existing batch payload fixture owner so tests exercising successful
import carry the fingerprint from the rendered request or the fixture's authored
focus. Keep malformed-provider inputs deliberate. This prepares the importer
validation in slice 2. **Proof.** Run the batch test family, including
maintenance, import and atomic reimport, since they share the fixture contract.
Production import behavior stays as in the baseline.

### 2. Require a real fingerprint on new questions

Type: Behavior
Status: planned

Make the typed `FocusNoteInput` invariant explicit and validate provenance at
the batch result boundary before persisting. Use the existing failed-row path.
Reuse the on-demand generation input. Keep the stored JSON reader able to
read historical questions with an absent field. Keep waiting reads compatible
until the production purge in slice 4 and retire that interim support in
slice 5. **Proof.** Replace the metadata-free import success example with the
active rejection at the same boundary. Check valid import and on-demand
persistence, and read an existing fingerprint-free answered attempt through
recall history.

### 3. Rehearse the one-time purge on MySQL

Type: Behavior
Status: planned

Supply a temporary operator command with read-only preview and explicit
apply. Apply the same eligibility filter used by preview and recheck the
unanswered condition in the write transaction. Delete selected prompts
first, preserving linked conversations through established detachment.
Then delete only their newly unreferenced MCQs. **Proof.** Use the actual
operator command on the isolated schema with both a shared and an unshared
fingerprint-free waiting question, a current waiting question, a contested
prompt, and an earlier answer with choices and grade. Compare preview to
the resulting queue, history and tracker due time. Record counts on repeat.

### 4. Apply the production cutover

Type: Behavior
Status: planned

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

### 5. Retire temporary migration and legacy waiting support

Type: Structure
Status: planned

After slice 4's production receipt, remove the operator code, its
migration-only proof, and compatibility code and tests specific to the old
waiting data. Keep the importer refusal, authored-content fallback for a
present fingerprint, and readable answered history. **Proof.** Run the
retained generation, reuse and history cases unchanged in intent. Read the
product and tests for retired migration and waiting-only compatibility
references as a one-time acceptance sweep.

## Plan review

Slice plan refinement was unnecessary. Shared fixture preparation is
bounded separately from enforcement. A rehearsed operation precedes the
production checkpoint, and its receipt unblocks retirement. No remaining
slice-specific concern was identified.
