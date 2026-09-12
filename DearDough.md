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

## ODF-009 — Claude Code's `.claude/worktrees/<name>` nesting depth breaks this project's isolated SUT bring-up

Former local code: DD-003.

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

## ODF-011 — Unit-test seams that satisfy a provisioning precondition can hide an ordering defect only the live integration proof exposes

Former local code: DD-005.

The runner-owned E2E wrapper's boundary tests injected a pre-resolved
`runtimeTarget` (or drove the primary, non-isolated checkout) for
`runOwnedE2eInvocation` / `runE2eBatch`. That seam satisfied the
`resolveSutCheckoutTarget` precondition (which requires a complete E2E
allocation) without exercising the real provisioning side effect that
`startOwnedSutLifetime` performs. The wrapper called `resolveSutCheckoutTarget`
*before* `startOwnedSutLifetime`, so a fresh isolated worktree with no
allocation yet failed on first `pnpm cy:run` — but only the live concurrent
proof surfaced this; the boundary suite passed throughout. The fix skips the
pre-start resolve for isolated checkouts (letting `startOwnedSutLifetime`
provision and resolve internally) and adds boundary tests for the
fresh-isolated-worktree path.

### Occurrences

- Execution: SEED-015 Story 8 / quick-105-runner-owned-e2e-lifecycle / be7234f7f2
  - Tool: Cursor
  - Open Dough release: 0.3.12
  - Evidence: slice 13's live concurrent proof failed in the peer worktree
    with "Isolated worktree SUT needs a complete E2E allocation in
    .worktree.local.json ... Missing: identity, e2e.database, e2e.backendPort,
    e2e.vitePort, e2e.lbListenPort" thrown from `resolveSutCheckoutTarget` →
    `loadCompleteIsolatedE2eAllocation`, called before `startOwnedSutLifetime`
    in `runE2eBatch`. Pre-fix `scripts/e2e-runner.test.mjs` (at `47df168656`)
    had zero matches for fresh-worktree / provisioning-path tests; the fix
    commit `b0dad96aaa` added eight (fresh isolated worktree provisions, primary
    regression guard, already-provisioned isolated re-reads allocation).
  - Observed effect: one failed live-proof attempt, a defect fix, a Gradle
    build-cache clear (a crashed `bootRunE2E --build-cache` had left a corrupted
    cache entry that also failed the retry), and a re-run before slice 13's
    proof passed. The boundary suite never caught the gap because the seam
    elided the provisioning side effect.

## DD-012 — Cross-cutting service-wiring slices legitimately exceed the 5-minute execution-leaf target

A slice that wires a new mocked external service across the runner, the Cypress
plugin boundary, the endpoint-context module, and the Cucumber hook is one
coherent responsibility that cannot be split along those layers without
breaking stop-safety. Such a slice naturally runs ~25 minutes of active work
versus the 5–8 minute execution-leaf target, with no mid-slice refinement
warranted. Plans admitting a new mock service should size that wiring slice as
a multi-touchpoint leaf (roughly 15–25 minutes) rather than a 5-minute Behavior
leaf, or decompose only where a stop-safe seam genuinely exists.

### Occurrences

- Execution: SEED-015 Story 10 / quick-105-remaining-active-e2e-isolation / 7614f8418d
  - Tool: Cursor
  - Model: glm-5.2
  - Open Dough release: 0.3.12
  - Evidence: PLAN.md slice 6 "Done 2026-09-12" note — "**Sizing deviation:**
    active work ~25 min vs. 5–8 min target — the Wikidata wiring touched the
    runner, plugin boundary, endpoint context, and the Cucumber hook across one
    coherent responsibility; recorded for retrospective. Slice converged with
    green proof; no refinement warranted mid-slice." Slice 6's plan sizing
    line read "5–8 minutes, medium confidence".
  - Observed effect: one slice ran ~3× its 5–8 min target; the plan flagged it
    for retrospective rather than refining mid-slice, and the slice converged
    green with no rework.
  - Inference: the 5-minute leaf target is the wrong anchor for a single
    cross-layer service-wiring responsibility; future plans should size such
    slices explicitly as multi-touchpoint wiring.
