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

## DD-003 — Claude Code's `.claude/worktrees/<name>` nesting depth breaks this project's isolated SUT bring-up

Claude Code's `EnterWorktree` tool places a new worktree at
`.claude/worktrees/<name>` under the repository root. For a descriptively
named execution worktree, the resulting checkout path is deep enough that
this project's isolated-SUT bring-up — which derives a Unix domain
socket at `<checkoutRoot>/.sut.local.lock/owner.sock` — exceeds macOS's
~104-byte `sun_path` limit, failing with `EINVAL` before any product or test
code runs. This blocked the plan's own specified Cypress integration proof
for the whole execution; only the Node-level focused unit proof could be
obtained from inside the worktree.

### Occurrences

- Execution: quick-107-verify-publication-receiver-in-bulk / d37290fac9
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.11
  - Evidence: worktree path
    `/Users/terryyin/git/doughnut/.claude/worktrees/107-verify-publication-receiver-in-bulk`
    (86 characters) plus `/.sut.local.lock/owner.sock` (28 characters) = 113
    bytes; `scripts/sut-owner.mjs` hardcodes that socket path relative to
    checkout root with no override; confirmed via `listen EINVAL: invalid
    argument` during isolated-SUT bring-up, traced by a reverted one-line debug
    change to un-ignore stdio, and independently reproduced by the
    coordinator via the SUT healthcheck module reporting a missing E2E
    allocation.
  - Observed effect: the plan's existing-integration Cypress command
    (`--expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'`)
    could not be run from the execution worktree for slice 1; the coordinator
    recorded the gap in the plan and recommended running it from a shallower
    checkout instead of treating the correction as unproven.

## DD-004 — `pnpm --frozen-lockfile install` inside project wrapper scripts repeatedly mutated `pnpm-lock.yaml`

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
