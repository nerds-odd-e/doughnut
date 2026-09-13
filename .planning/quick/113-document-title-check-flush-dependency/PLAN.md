# Document the destroy()-order dependency behind the title-check flush optimization

Status: planned
Source: dough-execution-retrospective finding on the SEED-018 story 5 execution
(quick/112-publish-additions-with-simpler-title-check, commit `691e7be961`,
not yet merged to main at the time of this correction plan).
Authority: 2026-09-13 retrospective correction planning only; execution has not started.

## Goal and boundary

`NoteTitlePlacementRules.requireNoSoftDeletedTitleAt` reads with
`FlushModeType.COMMIT`, skipping Hibernate's default auto-flush, and correctly
still sees a same-transaction soft-delete (see
`NoteTitlePlacementRulesFlushVisibilityTest`) only because
`NoteService.destroy()` happens to call `entityPersister.merge(note)` *before*
its own `memoryTrackerRepository.findByNote_IdIn(...)` query — that ordinary
query's implicit auto-flush is what makes the pending `deletedAt` durable
before `destroy()` returns. This dependency is invisible at `destroy()`'s own
call site today; nothing there signals that its statement order is load-bearing
for a different class's correctness.

Add a short comment inside `NoteService.destroy()` stating this dependency and
pointing at the regression test that would fail if the order changed. No
behavior change, no new abstraction, no new test — the existing visibility test
already covers the guarantee. Stop after this comment; do not touch `restore()`
or the title-check query itself.

## Evidence and finding

Retrospective review of commit `691e7be961` traced every current
`Note.deletedAt` mutation in the backend (`NoteService.destroy()` and
`NoteService.restore()` are the only two) to check whether the new
`FlushModeType.COMMIT` title-check query could observe a stale (pre-flush)
row:

- `destroy()`: sets `note.setDeletedAt(...)`, merges the note, *then* queries
  `memoryTrackerRepository.findByNote_IdIn(...)` — an ordinary (AUTO-flush)
  repository call that flushes the just-merged note change to the database
  before `destroy()` returns. This is why the added
  `NoteTitlePlacementRulesFlushVisibilityTest` (destroy, then check, no
  intervening query) passes.
- `restore()`: queries `memoryTrackerRepository.findByNote_IdIn(...)` *before*
  setting `note.setDeletedAt(null)` and merging — so, unlike `destroy()`, it
  does not self-flush its own change. Its only caller,
  `NoteController.undoDeleteNote`, calls `entityPersister.flush()` immediately
  afterward and never calls a title check in the same request, so there is no
  live bug today.

The correctness of the shipped optimization is real and covered by a
regression test, but the invariant it depends on is an incidental side effect
of `destroy()`'s statement order, undocumented at that method itself. A future
edit to `destroy()` that reorders those two lines (a plausible-looking, "harmless"
refactor) would silently break `NoteTitlePlacementRules`'s visibility guarantee;
the existing test would catch it only if the full backend suite is run and its
failure is correctly traced back to this dependency rather than treated as
flaky.

## Preserved promises and constraints

No change to observable soft-deleted-title-conflict behavior or to `restore()`.
ADR 0006 (fail loudly / specific conflict messages) unaffected. No new test
data or fixtures, so ADR 0007 is not implicated.

## Ordered slice

### 1. Document the destroy() flush dependency
Type: Structure
Status: planned
Proof: comment-only change; confirm
`NoteTitlePlacementRulesFlushVisibilityTest` and the existing
`SoftDeletedTitleConflictMvcTest` / `NotebookGitDeletedDestinationControllerTest`
still pass unchanged.

Add a short comment (1-3 lines) in `NoteService.destroy()`, at the
`entityPersister.merge(note)` / `memoryTrackerRepository.findByNote_IdIn(...)`
sequence, stating that this order flushes the note's pending soft-delete
before `destroy()` returns, and that
`NoteTitlePlacementRules.requireNoSoftDeletedTitleAt`'s `FlushModeType.COMMIT`
lookup (see `NoteTitlePlacementRulesFlushVisibilityTest`) depends on it not
changing. Comment-only; do not reorder or otherwise modify `destroy()` or touch
`restore()`.

Sizing: comment-only, well under five minutes; no production behavior change,
so no new benchmark or test is warranted.

## Learnings and results

None yet.
