# DearDough Process Findings

## DD-001 — Implementation subagent ends its turn "waiting" on its own background test instead of blocking for the result

An implementation subagent launched its own long-running test command
asynchronously and then ended its turn reporting that it was "waiting" or
"pausing" for the background result, instead of blocking until the command
finished and reporting the actual pass/fail outcome, despite explicit
instructions to do so before reporting.

### Occurrences

- Execution: SEED-017 Story 1 / quick-099-receive-compatible-accepted-history / 5ed11cd8e0
  - Tool: Claude Code
  - Open Dough release: 0.3.8
  - Evidence: slice 4 implementation agent's first report ("I've started the
    vitest run and am waiting for its completion notification before
    evaluating results. Pausing here.") and, after being resent the same
    request, its second report ("Pausing for the background run to
    complete."), before the coordinator ran the focused test command directly
    to obtain the actual result (95/95 passed).
  - Observed effect: two extra coordinator round-trips (one resend, one
    direct verification run) before slice 4's wrap-up could proceed.

## DD-002 — Concurrent Vitest processes against the same worktree cause spurious test timeouts under resource contention

Running more than one Vitest process against the same CLI test suite at the
same time (coordinator and/or subagents, or a formatting command running
concurrently with a test run) caused real, passing tests to fail with a
5000ms timeout under load. Each occurrence was confirmed environmental by a
clean solo re-run passing all tests.

### Occurrences

- Execution: SEED-017 Story 1 / quick-099-receive-compatible-accepted-history / 5ed11cd8e0
  - Tool: Claude Code
  - Open Dough release: 0.3.8
  - Evidence: during slice 4 verification, `notebookPull.structuralHistory.suite.ts`'s
    "names the structural path for remote same-path-reversed-rename and
    leaves the checkout unchanged" test timed out while a second vitest
    process ran concurrently; a clean solo re-run of the same suite passed
    95/95. Recurred after slice 4's `format:changed` step, where 5 tests
    failed during a run that overlapped with another process, and a clean
    solo re-run passed 95/95.
  - Observed effect: two rounds of failure triage (checking for concurrent
    processes, re-running solo) before trusting the test suite's result.

## DD-003 — Feature-branch delivery against main-only push CI leaves the merge push unobserved

This repository's `ci.yml` (`donut CI`) triggers only on push to `main`.
Executing on a feature branch, pushing that branch, then merging to `main`
and shutting the CI observer at plan completion means the CI-triggering
push is never observed for repair.

### Occurrences

- Execution: SEED-017 Story 2a / quick/099-publish-compatible-note-deletions / c5c9f2d990
  - Tool: Cursor
  - Model: Composer
  - Open Dough release: 0.3.8
  - Evidence: PLAN learnings recorded main-only CI; feature-branch pushes
    to `quick/099-publish-compatible-note-deletions` had no push-triggered
    workflow; after fast-forward to `main` and push, observer stop reported
    `pendingCi: unobserved` with `recordedThrough: 0`.
  - Observed effect: delivery completed without asynchronous CI failure
    coverage on either the feature-branch pushes or the main merge push.
  - Inference: for worktree→merge-to-main workflows here, attach observation
    to `main` for the merge push (or accept unobserved CI explicitly); do not
    treat feature-branch observer setup as coverage.
