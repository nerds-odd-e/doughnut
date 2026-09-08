# Web autosave commit batching

Source: [SEED-009 Story 10](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-10)

## Goal and scope

Give notebook owners one Git commit for consecutive same-note web content
saves separated by less than ten minutes, without delaying durable saves or
rewriting history delivered to clients. The first changed save appends;
subsequent eligible saves replace only its unexposed tip, keeping its parent.
A ten-minute gap, client exposure, or intervening accepted notebook change
ends the batch. Later editing starts a new batch.

Ordinary note content at unchanged root or nested paths only. No-op saves do
not extend the interval. Creation commits stay separate. Retain identity,
learning data, authorization, validation and existing drift behavior.
No UI, background timer, configurable interval, native Git transport, expanded
structural synchronization, drift repair, or broader rebase.

## Current decisions

- Ten-minute rolling amendment; exactly ten minutes starts a new batch.
- Unexposed accepted tips may be amendable; exposed heads and ancestors are
  immutable. Existing bindings default frozen.
- Bundle download and idempotent publish freeze the returned tip under the
  binding lock before clients see that head.
- Candidate head, note id, and last-changed timestamp live on the binding;
  amend only when candidate equals current head, same note/path, and elapsed
  time is nonnegative and below ten minutes.
- Preserve first-commit author metadata; use latest save for committer time.
- No-op saves do not reset the changed-save clock. Server DB state owns
  eligibility (no process cache or timer). ADR 0002 remains Proposed/unedited.

## Ordered slices

### 1. Represent durable eligibility for exposure freezing
Type: Structure
Status: done
Proof: `pnpm backend:verify` — eligibility columns + reload fixture; defaults null.
Structure: `V300000321` nullable amendment fields on `NotebookGitBinding`.

### 2. Freeze the exact history returned by a download
Type: Behavior
Status: done
Proof: `pnpm backend:test_only` — `NotebookGitBundleDownloadFreezeControllerTest`.
Behavior: `NotebookGitBundleDownloadService.selectAndFreeze` write-tx freeze.

### 3. Freeze a head returned by idempotent publication
Type: Behavior
Status: done
Proof: `pnpm backend:test_only` — `NotebookGitIdempotentPublishFreezeControllerTest`.
Behavior: Early-return publish clears eligibility via `clearAmendmentEligibility`.

### 4. Make snapshot parent choice explicit without changing saves
Type: Structure
Status: done
Proof: `pnpm backend:test_only` — `NotebookGitBundleBuilderTest` replaceTip round trip.
Structure: `NotebookGitBundleBuilder.replaceTip` (forceUpdate only on replace path).

### 5. Keep a continuous same-note edit as one durable commit
Type: Behavior
Status: done
Proof: `pnpm backend:test_only` — amendment controller tests (canonical 10:00/10:08/10:16
one-commit chain, boundary/no-op/download/A-B-A/concurrency variations) and updated
queued two-save expectation.
Behavior: `AcceptedSnapshotPersistence.persistOrdinaryNoteContentEdit` append-or-amend
under the binding lock; creation remains append-only.

## Learnings

- SQL tip after Java cutover was `V300000321` (V320 already taken).
- ERD Mermaid omits non-key columns; eligibility fields do not change the ERD diagram.
- Freeze both download and idempotent publish before activating amendment.
