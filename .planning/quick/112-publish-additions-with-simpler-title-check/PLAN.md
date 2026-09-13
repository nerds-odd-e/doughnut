# Publish additions with a simpler title check

Status: done
Source: [SEED-018 story 5](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-5)
Authority: 2026-09-13 story refinement and planning; executed 2026-09-13.

## Goal and boundary

Notebook owners publish 1,000 new notes into a notebook with 1,000 existing
fixture notes with **more than 50% less median waiting**, through simpler,
clearer production code with a net reduction in formatted production lines.
Preserve all current title/deletion behavior. Stop once this boundary is met;
larger incidental gains are welcome, additional optimization rounds are not
part of this story.

This follows the user's task `Optimize performance baseline`
(`01a09544-77d4-7f80-86da-11b7acbaa813`), inspected during refinement. Its
small-first and simplification criteria apply; use additions here because they
exercise title checking. The previous thread's 1,000 updates do not.

No trash, deletion migration, new title policy, background indexing, cache,
publication-specific mode, batch framework, 10,000-note validation, or profiler
redesign. No promises about throughput under concurrent publishers or production
SLAs. Naturally preserved behavior is not a new implementation restriction.

## Evidence and selected existing solution

[Retained profiling](../../../docs/notebook-publication-profiling.md#separate-addition-observation)
reports one 1,000-existing / 1,000-addition run at 13,368.447 ms after attachment
cleanup. Title-triggered flushing accounts for 425/600 request execution samples,
425/432 flush samples. This supports the target, not a fresh latency baseline
or proof that a candidate will halve waiting.

PFE finding: `NoteTitlePlacementRules` already owns the shared conflict rule;
`NoteRepository.findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc` performs
the lookup, and `NoteFactory` plus motion/relocation callers use it. Keep one
rule. `NoteService.deleteOrphanImagesForPersistedContent` demonstrates removing
unnecessary query-triggered flushing locally; property-index replacement also
uses query-local flush control. Reuse the approach only where the title query's
read responsibility supports it, not the image-specific invariants.

Prefer removing redundant persistence work and unnecessary entity loading within
that shared lookup. No exact query or flush hint is prescribed before proving
visibility. Current publication can mutate notes before later checks, and
`NoteService.destroy`/`restore` change managed deletion state. A read that skips
pending changes may silently change conflict behavior. Do not fall back to a
generic integrity-constraint error in place of the current actionable conflict.

Applicable decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
preserves Portable content/path contracts; [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
preserves deliberate conflict messages and context;
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires owned disposable test data. No new North Star topic is warranted for
this local simplification. The deferred trash story imposes no speculative
abstractions: keeping one small rule avoids extra machinery to replace later.

## Execution and proof protocol

Use the current `dough-execute-plan` workflow when execution is authorized:
claim the queued item, use its default owned worktree, record execution identity
here, and preserve unrelated changes in the originating checkout. No claim,
worktree, or commit is made by this planning request.

Run all commands below from the selected execution checkout. Use its owned
MySQL/Hibernate Unit Test and E2E environments; retain runtime versions and
baseline/candidate source revisions with captures. Reuse the existing profiler
and its control notes/folders unchanged. Keep the host awake and run comparisons
sequentially without overlapping test suites.

Benchmark command, three successful baseline runs before mutation and three
candidate runs after the small candidate passes preservation checks:

```bash
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=1000 PUBLICATION_PROFILE_TAGS=@publicationProfileHttp PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

Compare completed HTTP time including transaction commit, excluding setup and
receiver verification. Require identical fixture fingerprints and settings;
each run must verify proposed head, clean receiving checkout, and all 1,000
added documents byte-for-byte. Candidate median must be **< 0.5 × baseline
median**. Historic 13,368.447 ms is context, not the denominator. A timeout is
incomplete; inspect and stop the owned run before reset. If calibration cannot
complete within the practical deadline, use a smaller identical paired fixture,
record the reason and counts, and do not present it as a 1,000-addition result.
Do not increase to 10,000 notes in this round.

Required backend preservation command:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Inspect existing `SoftDeletedTitleConflictMvcTest` and
`NotebookGitDeletedDestinationControllerTest` assertions before changing tests.
They own current HTTP conflict metadata, deleted identity, path context, and
accepted-state preservation. Reuse other existing restore/move tests for those
callers. If the candidate changes query visibility, require the smallest missing
public-boundary regression against the real Unit Test database for a supported
same-transaction state change. Do not mock the query or introduce an internal
test API. Record the specific pending-state scenario and result here before
accepting the flush assumption; a failed proof stops that candidate, not the test.

Small existing HTTP acceptance/late-rejection proof:

```bash
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection' PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

These observe content acceptance and rollback of prior work on late rejection;
their timings are correctness evidence only. Reuse sufficient existing proof,
do not create a broad new scenario matrix or unrelated test cleanup.

## Ordered slice

### 1. Publish additions in less than half the time through a simpler shared lookup
Type: Behavior
Status: done
Behavior: Given the unchanged 1,000-existing / 1,000-addition fixture, publication
through the existing HTTP boundary completes in less than half the fresh baseline
median time, with the same content, title-conflict, and rollback behavior.
Proof: The execution/proof protocol above owns all acceptance promises in this
single performance outcome; production code review and formatted diff additionally
establish the required simplification.

Make one small candidate that removes unnecessary work in the shared lookup.
Verify pending-state visibility before accepting it. Run preservation checks
and compare timings; record results and formatted production line counts in this
plan and lasting findings in the existing profiling document. Count the entire
affected production change, including moved/new files; tests/tooling separately.
Require fewer production lines and a clear explanation of the removed work.
Line compression, unrelated deletion, or complexity hidden in helpers is not
simplification. A faster but more complicated candidate fails this story.

Stop after this outcome passes. If preserving behavior requires a larger design,
or the small candidate misses >50%, retain the finding and stop for reassessment;
do not broaden the story to other hotspots or a bulk/cache fallback.

Sizing: target about five minutes of actual code editing for one small shared
change. Low confidence until transaction visibility is checked. The full backend
suite, six owned JVM benchmark captures, and E2E setup/verification necessarily
exceed the ten-minute leaf limit; these are explicit measurement/test-wait
exceptions for the same indivisible comparison. They do not excuse >10 minutes
of expanding implementation work. At that point park only owned candidate work
and refine/reassess before continuing. Do not split tests from the behavior or
claim a completed slice with an unproved timing or simplicity gate.

Delivery follows the repository contract: Jidoka, fresh
`dough-post-change-refactor` agent, generator only if triggered, coordinator
`./scripts/run.sh pnpm format:changed` once, final formatted size check, plan
update without another routine format, commit with check-only lint hook, push
and asynchronous CI handling. Reuse valid proof unless refactoring invalidates it.

## Slice refinement assessment

One Behavior slice remains after refinement; no preparatory Structure is justified.
Its timing, simpler code, and preserved behavior are inseparable acceptance gates
for one user outcome. Removed the earlier speculative bulk-check fallback and
bounded the visibility proof to supported callers instead of an expanded matrix.
Test/benchmark waiting has an explicit sizing exception; implementation expansion
does not. No product question remains. Technical feasibility (>50% with fewer
lines and correct visibility) is unproved and owned by this slice, not represented
as an achieved result. No execution or benchmark has been performed for this plan.

## Learnings and results

Delivered. The shared lookup in `NoteTitlePlacementRules.requireNoSoftDeletedTitleAt`
switched from a Spring Data query (`NoteRepository.findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc`,
full-entity hydration, default Hibernate AUTO flush) to a direct `EntityManager`
query via the existing `EntityPersister.createQuery` helper, selecting only the
matched note's `id`, with `FlushModeType.COMMIT` and `setMaxResults(1)`. The
now-unused repository method was removed.

Baseline median 11,876.320 ms, candidate median 3,064.883 ms (1,000 existing /
1,000 additions, completed HTTP time, three runs each) — a 74.2% reduction,
well past the required >50%. Formatted production diff: 23 insertions / 29
deletions, net **−6** lines across the two touched files; no new production
file. Full detail and the reproduction command are recorded in
[Additions title-check simplification](../../../docs/notebook-publication-profiling.md#additions-title-check-simplification).

Visibility proof: added `NoteTitlePlacementRulesFlushVisibilityTest` (real
Hibernate/MySQL Unit Test database, no mocking, no internal test-only API),
proving a same-transaction soft-delete performed via `NoteService.destroy(...)`
is still detected by the new `FlushModeType.COMMIT` query with no intervening
query — `destroy()` itself performs an ordinary AUTO-flush query before
returning, which is why visibility holds. Existing `SoftDeletedTitleConflictMvcTest`
and `NotebookGitDeletedDestinationControllerTest` pass unchanged with their
original assertions. Full backend suite (481 test classes) passes.

A fresh `dough-post-change-refactor` pass found no candidates: `none — already clean`.
