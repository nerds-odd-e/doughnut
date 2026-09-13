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

## ODF-030 — One-off profile capture treated as durable runner plumbing

Former local code: DD-013.

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

## ODF-031 — Large-capture plan command copied small-fixture Cypress timeouts

Former local code: DD-014.

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

## ODF-032 — Plan sizing for a three-run 1,000-note E2E capture slice undercounted actual owned-stack/fixture lifecycle cost

Former local code: DD-015.

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

## ODF-033 — Reviewing an unmerged execution branch from a different checkout's working tree returns stale pre-change file content

Former local code: DD-016.

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

## ODF-034 — CI observer started for a feature branch that this project's workflow never triggers on

Former local code: DD-017.

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

- Execution: SEED-019 story 1 / quick/114-note-owned-memory-tracker-deletion / be43a2c1e5
  - Tool: Cursor
  - Model: GLM 5.2
  - Open Dough release: 0.3.15
  - Evidence: `.github/workflows/ci.yml` triggers only on `push:
    branches: [main]`; `gh run list --branch 114-note-owned-memory-tracker-deletion`
    returned no runs despite seven pushes to that branch; the CI observer
    (started during slice 1 delivery) ran for the full execution and was
    stopped at completion reporting `pendingCi: unobserved`.
  - Observed effect: the CI observer watched a branch that structurally
    cannot produce CI events for the entire execution; the runtime-setup
    probe verified only that the workflow file/name resolved and the host
    bridge was ready, so the gap was not caught at setup.
  - Inference: same as the original ODF-034 finding — the runtime-setup
    trigger-condition check is still absent; the recurrence confirms the
    anti-pattern persists across releases.

## ODF-035 — `EnterWorktree`'s default base ref and branch-name sanitization conflict with this project's worktree/branch convention

Former local code: DD-018.

The harness's built-in `EnterWorktree` tool, used to set up a planned-execution
worktree for an intended branch name containing `/` (this project's
`quick/<slug>` convention), does not treat that name as a literal branch name:
each `/`-segment is sanitized and a `worktree-` prefix is added, and its
default `baseRef: "fresh"` branches from `origin/<default-branch>` rather than
local `HEAD`, silently omitting a commit already made locally on the
originating branch before the worktree was created.

### Occurrences

- Execution: SEED-018 story 5 / quick/112-publish-additions-with-simpler-title-check / 691e7be961
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.14
  - Evidence: `EnterWorktree(name: "quick/112-publish-additions-with-simpler-title-check")`
    returned branch `worktree-quick+112-publish-additions-with-simpler-title-check`
    checked out at `4b89109c99` (origin/main's tip), while local `main` was
    already one commit ahead at `aaabfcf552` (the backlog "Taken" transition
    commit made just before). The coordinator detected the mismatch via `git
    branch --show-current` / `git log --oneline`, called
    `ExitWorktree(action: "remove")`, then created
    `.worktrees/112-publish-additions-with-simpler-title-check` with plain
    `git worktree add -b quick/112-publish-additions-with-simpler-title-check
    aaabfcf552` under this project's own gitignored `.worktrees/` convention.
  - Observed effect: one full extra round-trip (create, inspect, remove)
    before the correctly named/based worktree existed; no lost work, since
    nothing had yet been written into the discarded worktree.
  - Inference: a coordinator following a project convention that both names
    execution branches literally and requires branching from a fresh
    local-only commit should create the worktree with plain `git worktree add`
    rather than `EnterWorktree`, whenever either constraint applies.

## ODF-036 — Delegated implementation agent's own background watches kept notifying the coordinator after its final report

Former local code: DD-019.

An implementation subagent that ran several sequential long-running background
benchmark commands during one delegated slice appears to have armed a
watch/monitor for each one. After the agent returned its complete final report
to the coordinator, each of those watches individually timed out afterward and
fired its own separate "stale, already complete" task-notification back to the
coordinator, one at a time.

### Occurrences

- Execution: SEED-018 story 5 / quick/112-publish-additions-with-simpler-title-check / 691e7be961
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.14
  - Evidence: after the implementation agent (delegated task for this slice)
    returned its full final report (baseline/candidate benchmark numbers,
    proof log), the coordinator received seven further separate
    task-notifications for the same already-completed task, each one
    self-described in its own result text as a "stale monitor timeout" for one
    specific already-reported benchmark run (baseline runs 1-3, candidate runs
    1-3, and the small acceptance/rejection run), arriving individually over
    the following several minutes while a second delegated agent (the
    post-change-refactor pass) was concurrently running.
  - Observed effect: seven extra coordinator turns, each requiring inspection
    of the notification and a one-line "no action needed" acknowledgment,
    interleaved with the unrelated in-progress refactor-agent notification the
    coordinator was actually waiting on.
  - Inference: each notification's own text confirmed it added no information
    beyond the agent's already-received final report, so the cost was purely
    coordinator attention; a subagent that arms one watch per background
    command it launches, without stopping or consolidating those watches once
    it has already produced its own synchronous final report, generates this
    kind of post-completion notification noise.

## DD-037 — Coordinator pre-filtered grep results for a test-only representation slice, missing sites the later field-removal slice had to fix

A test-only representation slice (replacing soon-to-be-removed field
references with owned observations) was delegated with a pre-filtered file
list: the coordinator classified each `getDeletedAt()` grep hit as a Note
or MemoryTracker assertion and delegated only the files with tracker
assertions. Two sites were missed — one file entirely omitted from the
delegation, and one remaining call in an included file — because the
classification was incomplete. The later slice that removed the field
caught both at compile time, but at the cost of that slice doing extra
test fixes outside its primary schema-removal scope.

### Occurrences

- Execution: SEED-019 story 1 / quick/114-note-owned-memory-tracker-deletion / be43a2c1e5
  - Tool: Cursor
  - Model: GLM 5.2
  - Open Dough release: 0.3.15
  - Evidence: slice 6 (test-only representation) delegation listed five
    files with tracker `deletedAt` assertions but omitted
    `NoteControllerDeleteReduceToSourceTests` (whose
    `relation.getDeletedAt()` / `reloaded.getDeletedAt()` calls were tracker
    assertions the coordinator marked "need to check"); slice 7 (field
    removal) commit `be43a2c1e5` fixed that file plus one remaining
    `retained.getDeletedAt()` call in
    `NotebookGitProposalFolderCreationControllerTest` that slice 6 missed.
  - Observed effect: slice 7's implementation agent fixed two extra test
    files at compile time; no extra coordinator round-trip, but the later
    slice carried representation work the earlier slice was meant to own.
  - Inference: for a test-only representation slice that replaces references
    to a field removed in a later slice, delegate all grep matches and let
    the implementation agent classify each (Note vs tracker), rather than
    the coordinator pre-filtering; the compile-time safety net catches
    misses, but shifting that work to the later slice blurs the slice's
    intended boundary.

## DD-038 — Noncanonical proof handoffs force report-only coordinator round-trips

Delegated slice work completed with the requested tests and evidence, but the
agent returned proof in prose or near-matching YAML instead of the exact
lower-case proof schema required by execution. The coordinator had to request
report-only reformats after the substantive work was already complete.

### Occurrences

- Execution: SEED-009 story 29 / quick/115-web-note-trash-and-undo / 2be6138738
  - Timestamp: unknown
  - Tool: Codex
  - Model: GPT-5
  - Open Dough release: 0.3.16
  - Evidence: slice 6 required two report-only follow-ups after first returning
    custom Command/Result/Focused-proof fields and then title-cased YAML keys;
    slice 7 required one report-only follow-up after returning a custom report
    rather than the required proof block. Each final handoff described the same
    already-completed tests and changes.
  - Observed effect: three extra coordinator-agent round-trips produced no new
    implementation or verification evidence.
  - Inference: exact proof serialization is not reliably enforced at the agent
    boundary even when a literal template is supplied; structured validation
    before accepting the handoff would remove this clerical loop.

## DD-039 — Hard-limit refinement after completed work creates bookkeeping without a smaller remaining leaf

The slice hard-limit protocol fired after the web Trash/Undo outcome and its
proof were already complete, then fired again after its required fresh refactor
completed. With no incomplete implementation left to decompose, each stop
parked exact work and revised execution history without changing the remaining
delivery shape.

### Occurrences

- Execution: SEED-009 story 29 / quick/115-web-note-trash-and-undo / 2be6138738
  - Timestamp: unknown
  - Tool: Codex
  - Model: GPT-5
  - Open Dough release: 0.3.16
  - Evidence: plan 115 slice 9 records about 21–24 active minutes excluding
    suite waits; plan-only commits `ebd4839d26` and `b1a571154b` followed
    separate exact-work stash/restore cycles after implementation/proof and
    after post-change refactoring, while retaining the same single Behavior
    slice because all compatible work was complete.
  - Observed effect: two plan-only commits and two park/restore cycles changed
    the overrun record but produced no smaller executable remaining work.
  - Inference: the hard-limit path needs a stop condition for a fully completed,
    compatible outcome: record the overrun once during final plan update when
    no implementation or proof remains, while retaining escalation for actual
    unfinished work.

## DD-040 — Shared query classified by dominant purpose hid an incompatible production caller

The plan correctly preserved a live-note query for Git/export retention, but its
consumer assessment treated that method as if all callers shared the same
storage meaning. A production commissioned-learning caller also used it and
therefore retained trashed notes in report matching.

### Occurrences

- Execution: SEED-009 story 29 / quick/115-web-note-trash-and-undo / 2be6138738
  - Timestamp: unknown
  - Tool: Codex
  - Model: GPT-5
  - Open Dough release: 0.3.16
  - Evidence: plan 115's consumer table explicitly says
    `findLiveNotesByNotebookIdOrderByIdAsc` serves Git state loading and must
    retain legacy content inclusion; current caller search also finds
    `LearningSessionService.record`, which matches commissioned report titles
    from that query without a later `Note.isAvailable()` or tracker-activity
    check.
  - Observed effect: all planned suites passed while a trashed commissioned note
    remained gradeable by report; retrospective correction plan 116 was needed.
  - Inference: when one shared query has mixed production callers, consumer
    inventory must classify each call site by domain purpose rather than assign
    the method one dominant category; focused proof should cover every
    incompatible category.
