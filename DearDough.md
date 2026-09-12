# DearDough Process Findings

## ODF-007 — Implementation subagent ends its turn "waiting" on its own background test instead of blocking for the result

Former local code: DD-001.

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

- Execution: SEED-018 story 3 / quick/108-publish-notebook-edits-faster / a6fcddacad
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.13
  - Evidence: the slice 1 implementation agent's first two reports ("I'll
    wait for the background smoke-test run to finish before continuing." and,
    after one resend, "Waiting for baseline run 1 (1000/1000) to finish.")
    before a second resend produced its real final report; separately, the
    slice 2 post-change-refactor agent's first report ("I've queued a focused
    Cypress verification run ... and I'm waiting for it to complete before
    finalizing the report") even though that agent's initial delegation
    prompt already contained an explicit instruction not to stop and wait
    mid-verification.
  - Observed effect: three extra coordinator round-trips across the
    execution (two resends for the slice 1 agent, one for the slice 2
    refactor agent) before each returned a real final report with actual
    results.
  - Inference: giving the anti-pattern instruction directly in the initial
    delegation prompt (done for the slice 2 refactor agent) did not prevent
    the same pause-and-wait behavior from recurring, suggesting an inline
    instruction alone is not a reliable mitigation for this pattern.

## ODF-008 — Concurrent Vitest processes against the same worktree cause spurious test timeouts under resource contention

Former local code: DD-002.

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

## ODF-010 — `pnpm --frozen-lockfile install` inside project wrapper scripts repeatedly mutated `pnpm-lock.yaml`

Former local code: DD-004.

This project's `./scripts/run.sh` wrapper (used for `pnpm format:changed`,
commit's lint hook, and the SUT healthcheck module) runs `pnpm --frozen-lockfile
--silent install` before the requested command. Despite `--frozen-lockfile`,
this install repeatedly bumped a `@biomejs/biome` transitive-version pin
(2.5.12 → 2.5.13) in `pnpm-lock.yaml` on its own, with no corresponding
`package.json` change. This happened on three separate invocations within one
execution, requiring the coordinator to notice and `git checkout --
pnpm-lock.yaml` each time before staging/committing, to avoid shipping an
unrelated lockfile diff.

### Occurrences

- Execution: quick-107-verify-publication-receiver-in-bulk / d37290fac9
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.11
  - Evidence: `pnpm-lock.yaml` diff showing `@biomejs/biome` `2.5.12` →
    `2.5.13` (and dependent `vite-plugin-checker` resolution strings)
    reappeared after (1) the slice 1 implementation agent's isolated-SUT
    bring-up attempts, (2) the coordinator's SUT healthcheck call, and (3) the
    coordinator's `./scripts/run.sh pnpm format:changed` run — each reverted
    with `git checkout -- pnpm-lock.yaml` before commit.
  - Observed effect: three extra verify-and-revert steps across the
    execution; no functional impact since each drift was caught before
    commit, but the pattern would silently ship as an unrelated diff without
    that check.

## DD-013 — One-off profile capture treated as durable runner plumbing

A tagged publication-profile capture drove a durable `pnpm cy:run` change to
forward Cypress `--expose`/`--config`, including runner tests, then a closing
slice reverted that forwarding because it served measurement rather than the
kept product change.

### Occurrences

- Execution: SEED-018 story 3 / quick/106-publish-large-notebooks-under-one-minute / 78c24f31bb
  - Tool: Cursor
  - Model: Cursor Grok 4.6
  - Open Dough release: 0.3.12
  - Evidence: slice 2 commit `6162492745` added forwarding in
    `scripts/e2e-runner.mjs` and `scripts/e2e-runner.test.mjs`; close commit
    `3613029688` restored those files to `78c24f31bb`; PLAN closing decision
    (2026-09-12) says the plumbing served measurement, not the kept flush change.
  - Observed effect: slice 2 included runner work beyond the property-index
    flush change; slice 4 existed only to undo that forwarding.
  - Inference: isolated tagged captures can use a documented one-off Cypress
    invocation or known baseline waits without changing the owned runner.

## DD-014 — Large-capture plan command copied small-fixture Cypress timeouts

The first large confirmation command used the small HTTP capture's
`taskTimeout=66000` and omitted the already-recorded large-fixture Cypress
`defaultCommandTimeout`, so seed aborted before any publication measurement.

### Occurrences

- Execution: SEED-018 story 3 / quick/106-publish-large-notebooks-under-one-minute / 78c24f31bb
  - Tool: Cursor
  - Model: Cursor Grok 4.6
  - Open Dough release: 0.3.12
  - Evidence: PLAN.md "Slice 3 confirmation attempt (2026-09-12)" — Cypress
    failed at `cy.wrap()` waiting 6000 ms (`e2e_test/config/common.ts`) during
    `When I seed the representative publication baseline`; spec duration 8 s;
    no `timing.json` or profile directory. The same PLAN notes the awake large
    captures used `taskTimeout=43260000,defaultCommandTimeout=600000`. Retry
    with those waits then measured the 60 s miss.
  - Observed effect: one setup-only abort (~40 s runner lifetime) plus
    investigation before the authorized retry.
  - Inference: large-fixture Cypress waits were already in the profiling
    record; copying the small-path `--config` was enough to miss them.

## DD-015 — Plan sizing for a three-run 1,000-note E2E capture slice undercounted actual owned-stack/fixture lifecycle cost

A slice's stated sizing exception ("3-5 additional minutes for owned stack
startup and three baseline captures") was written for the external-wait
portion of a slice whose actual owned-stack bring-up plus 1,000-note fixture
seeding plus three sequential full-scenario Cypress runs took several times
that long in practice, even though each individual measured HTTP request was
itself fast (tens of seconds).

### Occurrences

- Execution: SEED-018 story 3 / quick/108-publish-notebook-edits-faster / a6fcddacad
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.13
  - Evidence: PLAN.md slices 1 and 2 both stated "3-5 additional minutes" for
    their owned-stack/three-capture portion. The delegated agents' reported
    `duration_ms` for the runs that performed a full three-capture 1,000/1,000
    set were 740,778 ms (~12.3 min, slice 1 implementation) and 793,959 ms
    (~13.2 min, slice 2 implementation) — roughly 2.5-3x the stated estimate
    for that single portion of work, before counting coordinator wait/resend
    time between checks.
  - Observed effect: none of the estimate misses triggered an oversized-slice
    stop or replan, since both slices still converged to a passing, in-budget
    result on the "focused implementation" portion; the miss was confined to
    the explicitly-excepted external-wait estimate. No rework resulted, but a
    future planner sizing a similar large-fixture E2E capture slice should
    expect the real external-wait portion to be several times a first guess
    based on the per-request timing alone.
  - Inference: `duration_ms` figures include the delegated agent's own
    investigation/verification time, not a clean measurement of "capture wait
    only," so the multiplier is a qualified upper-bound observation, not an
    exact ratio.

## DD-016 — Reviewing an unmerged execution branch from a different checkout's working tree returns stale pre-change file content

Reading a production file's working tree from a checkout that has not merged
the reviewed execution's branch silently returns the pre-change version, with
no error, when the review is actually meant to inspect the delivered commits
on a separate branch or worktree.

### Occurrences

- Execution: SEED-018 story 3 / quick/108-publish-notebook-edits-faster / a6fcddacad
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.13
  - Evidence: during this retrospective, reading
    `backend/src/main/java/com/odde/donut/services/NotePropertyIndexService.java`
    from the main checkout's working tree (which had not merged
    `quick/108-publish-notebook-edits-faster`) returned the old
    pre-optimization implementation; a subsequent `git grep`/`git show
    a05a66686c:<path>` against the actual reviewed commit was needed to see
    the delivered code.
  - Observed effect: one avoidable re-read and a moment of apparent
    contradiction with the execution's own proof before the mismatch was
    traced to checkout/branch, not a regression.
  - Inference: reviewing an execution that ran in a separate worktree/branch
    should read files via `git show <reviewed-sha>:<path>` (or `git grep
    <ref>`) rather than the coordinator's own working tree, whenever that
    tree has not merged the branch under review.

## DD-017 — CI observer started for a feature branch that this project's workflow never triggers on

`dough-execute-plan`'s CI-observation setup verified only that the workflow
file and name existed and that the notification host bridge was ready; it did
not check the workflow's actual trigger condition. This project's `ci.yml`
triggers only on `push: branches: [main]`, with no `pull_request:` trigger
anywhere, so pushing to an execution branch can never produce a CI run for
the observer to see, regardless of how long it watches.

### Occurrences

- Execution: SEED-018 story 3 / quick/108-publish-notebook-edits-faster / a6fcddacad
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.13
  - Evidence: `.github/workflows/ci.yml` line 6-9 (`on: push: branches:
    [main]`); `gh run list --repo nerds-odd-e/doughnut --json headBranch,...`
    shows recent runs only for `headBranch: "main"`, none for
    `quick/108-publish-notebook-edits-faster` despite two pushes to it.
  - Observed effect: the CI observer ran for the full execution and was
    stopped at completion reporting `pendingCi: unobserved`, phrased (in the
    coordinator's own completion report) as CI merely not having finished
    yet, when in fact no CI run was ever going to happen on that branch.
  - Inference: the runtime-setup step "verify the branch has a
    push-triggered CI workflow" needs an actual trigger-condition check (e.g.
    inspecting the workflow's `on:` block for the target branch), not just
    confirming the workflow file/name resolve, or it will silently watch a
    branch that structurally cannot produce events.
