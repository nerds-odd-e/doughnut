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

## DD-003 — Isolated SUT `/shutdown` retains the owner lock, so worktree retirement refuses until ownership is released

`beginSutOwnerShutdown` is restart-shaped: it stops the owning children and
sets retain-on-exit, leaving `.sut.local.lock` in place. `pnpm worktree:retire
--check` then vetoes that leftover as a stale owner record and will not reclaim
it. After a confirmed dead owner, `releaseSutOwnership` is the teardown that
makes an idle snapshot.

### Occurrences

- Execution: SEED-017 Story 3 / quick/100-declare-isolated-test-capabilities / d8886ebd13
  - Tool: Cursor
  - Model: Cursor Grok 4.6
  - Open Dough release: 0.3.8
  - Evidence: after live completion Cypress in `/Users/terryyin/git/doughnut-quick-100`,
    `beginSutOwnerShutdown` returned `{ ok: true }` and `verifyLiveSutOwner`
    became not ok, but `.sut.local.lock/owner.json` and `owner.sock` remained;
    `pnpm worktree:retire --check` refused with "stale or unverifiable SUT owner
    record (.sut.local.lock; not reclaimed)". `releaseSutOwnership` after
    `live.ok === false` removed the lock; the next `--check` reported an idle
    snapshot and retirement dropped `doughnut_e2e_wt_0419c31c532240c3a3af7388068369fa`.
  - Observed effect: extra investigation of shutdown vs restart lock retention
    before the worktree could be dropped.
  - Inference: `/shutdown` is for `pnpm sut:restart`, not checkout teardown;
    call `releaseSutOwnership` only after the owner is proven not live.

- Execution: SEED-017 Story 2b / quick/101-publish-compatible-note-moves / 8983d9db3f
  - Tool: Codex
  - Model: GPT-5
  - Open Dough release: 0.3.8
  - Evidence: after the worktree-owned Cypress proof, the coordinator called
    `beginSutOwnerShutdown`, waited until `verifyLiveSutOwner` was not ok, then
    had to call `releaseSutOwnership` explicitly before retirement could inspect
    an idle checkout.
  - Observed effect: execution needed a custom shutdown/wait/release command
    instead of a documented checkout-teardown operation before the worktree
    databases and checkout could be removed.

## DD-004 — Completed backend worktree tests leave a dead owner record that blocks retirement

Supported isolated backend test and migration commands leave
`.worktree.local.lock` after their process exits. A later backend command can
reclaim that stale record, but `pnpm worktree:retire --check` correctly refuses
to infer ownership from a dead PID, so normal test completion and safe retirement
do not compose without manual lock removal.

### Occurrences

- Execution: SEED-017 Story 2b / quick/101-publish-compatible-note-moves / 8983d9db3f
  - Tool: Codex
  - Model: GPT-5
  - Open Dough release: 0.3.8
  - Evidence: after successful `pnpm backend:test_only` runs in
    `/Users/terryyin/git/doughnut-quick-101`, `pnpm worktree:retire --check`
    refused dead PID 23796 in `.worktree.local.lock` as "not reclaimed".
    `ps` found no such process; moving that exact lock to Trash made the next
    retirement check report an idle snapshot. Quick plan 102 records the
    bounded correction and preserves retirement's stale-owner refusal.
  - Observed effect: worktree cleanup required extra code inspection, PID
    verification, and manual recoverable lock removal before the two disposable
    databases could be dropped.
  - Inference: the backend command handoff writes its owner PID and `exec`s the
    child, but no normal-exit path releases ownership; this predates quick plan
    101 and is not a note-publication regression.
