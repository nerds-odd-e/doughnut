# Remove the test-only image repository

Status: completed
Source: bounded correction from the execution retrospective for the completed
SEED-018 story 5 and plan 110, recoverable at before-cleanup commit
`dee129d2b62619c1ce08a007a68163d437e2b38e`
in `.planning/seeds/SEED-018-publish-large-authored-notebooks.md` and
`.planning/quick/110-publish-edits-with-simpler-attachment-cleanup/PLAN.md`.
Reviewed execution manifest: Taken/provenance commit `9468b251b7` and
implementation commit `5642bb7509` on
`quick/110-publish-edits-with-simpler-attachment-cleanup`.

## Beneficiary and bounded outcome

A future backend maintainer sees only production abstractions with real product
callers. Remove `ImageRepository`, which became an empty Spring Data interface
with no production caller when plan 110 moved orphan selection into
`NoteService`; preserve image upload, lookup assertions, attachment cleanup,
blob cascading, publication performance and rollback behavior exactly.

This is one refactoring correction. It does not change the orphan-selection
query, flush policy, image/blob lifecycle, API, schema, migration, publication
fixture, timing target or product behavior. It does not reopen plan 110's
completed performance comparison.

## Current finding and evidence

Current-tree search finds `ImageRepository` only at its declaration and in
three controller test classes:
`NoteControllerUploadNoteImageTests`,
`TextContentControllerUpdateNoteContentTests`, and
`NotebookGitPublicationAtomicControllerTest`. The interface now declares no
operation beyond inherited `CrudRepository` methods. Therefore it is production
code exercised only as test lookup plumbing, a dead/redundant-code finding under
the project's post-change refactor checks. This residue was exposed by
`5642bb7509`, which removed `NoteService`'s last production dependency on it.

PFE decision: remove the interface and reuse the already-injected JPA
`EntityManager` as the tests' observation mechanism. The affected controller
test hierarchy already uses `EntityManager.find` for persisted entity and blob
observations, including adjacent assertions added by plan 110. No replacement
repository, helper, mock, or new test surface is warranted.

Relevant Accepted decisions are
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) and
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
The correction changes neither failure behavior nor environment ownership.
ADR 0004's notebook-content contract remains preserved but does not dictate the
test lookup mechanism. No ADR change or exception is needed.

## Ordered slices

### 1. Remove the production abstraction that only tests call
Type: Structure
Status: done

Change: Delete
`backend/src/main/java/com/odde/donut/entities/repositories/ImageRepository.java`.
In its three controller-test callers, replace inherited repository lookups with
the existing `EntityManager.find(Image.class, id)` observation and remove the
repository injection/imports. Preserve each test's current externally observable
assertions: uploaded image ownership; kept, removed and other-note images;
orphan blob cascade; and late-failure image/blob restoration. Do not change the
production cleanup implementation or add a test-only production seam.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passes the whole
backend suite, as required by the backend workflow. A current-tree
`rg -n "ImageRepository" backend/src/main backend/src/test` returns no matches,
and diff inspection confirms the only production change is deletion of the
unused repository interface while controller-level behavioral assertions remain.

Sizing: under five minutes. The existing observation mechanism is already
present in all affected test hierarchies; this slice has one deletion/replacement
loop and one required backend-suite wait. The suite duration is an explicit
verification wait, not permission to broaden the correction.

Delivered evidence (2026-09-13): the empty six-line `ImageRepository` interface
is deleted and all three controller-test callers observe `Image` persistence
through their existing `EntityManager`. `rg -n "ImageRepository"
backend/src/main backend/src/test` returned no matches. `CURSOR_DEV=true nix
develop -c pnpm backend:test_only` passed the complete backend suite after the
stopped local MySQL service was started; the first attempt had ended before test
execution with a connection refusal on port 3309. The fresh post-change refactor
made the image/blob restoration assertions direct `notNullValue()` observations,
then the same full backend command passed again in one minute.
`scripts/check_diff_whitespace.sh` passed before and after refactoring. No other
production file, API, schema, timing fixture or product behavior changed. Active
implementation time was approximately 1 minute 37 seconds and refactor time was
approximately four minutes; service and suite waits use the stated exception.

## Promise-to-proof ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| No production-only-for-tests image repository remains | 1 | Repository file deleted and current-tree search has no `ImageRepository` references |
| Image upload and cleanup observations retain their meaning | 1 | Existing controller tests use `EntityManager.find` at the same stable boundary and the full backend suite passes |
| Plan 110 behavior and persistence lifecycle remain unchanged | 1 | No other production file changes; full backend suite preserves upload, cleanup, cascade and rollback behavior |

## Execution and delivery

Execution was explicitly authorized on 2026-09-13 and followed
`dough-execute-plan` in its default fresh-worktree mode: Jidoka, a fresh
post-change refactor pass, no API generation trigger, one coordinator-owned
`./scripts/run.sh pnpm format:changed`, plan update, check-only commit hook,
push and applicable CI-observer handling.

Execution identity (started 2026-09-13): originating checkout
`/Users/terryyin/git/doughnut` on `main`; execution checkout
`/Users/terryyin/git/doughnut/.worktrees/quick-111-remove-test-only-image-repository`
on `quick/111-remove-test-only-image-repository`; integration target `main`.
The execution branch starts from Taken-only commit `a253fd0819`. The destructive
later-outcome check found no conflict: the single slice removes only the empty
repository interface, has no later slice, and preserves every named image/blob
behavior in its own proof. GitHub Actions workflow `.github/workflows/ci.yml`
(`donut CI`) is push-triggered only for `main`, so execution-branch CI
notification coverage is unavailable.

## Planning assessment

The correction has one coherent structural outcome and proof loop. It reuses an
existing test observation mechanism and introduces no new production boundary.
No remaining slice-specific concern was identified beyond the required backend
suite's external wait.
