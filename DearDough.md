# DearDough Process Findings

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

- Execution: SEED-021 story 1 / quick/133-inbound-wiki-reference-nfkc-mismatch / f126e1bdd8
  - Timestamp: unknown (session date 2026-09-17)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.24
  - Evidence: `EnterWorktree(name: "inbound-wiki-nfkc-133")` created branch
    `worktree-inbound-wiki-nfkc-133` checked out at `877a988b59`
    (`origin/main`'s tip), while local `main` was already one commit ahead at
    `f8504e98f7` (the backlog "Taken" claim commit made just before). The
    coordinator detected the mismatch via `git log --oneline -3` /
    `git status` immediately after entering the worktree.
  - Observed effect: recovered in place with `git merge --ff-only f8504e98f7`
    inside the worktree instead of the full remove/recreate round-trip the
    prior occurrence used, since the worktree's branch was still a clean
    ancestor with no divergent commits of its own; no lost work or wasted
    worktree, but still an unplanned diagnostic step the tool's default
    should not have required.
  - Inference: same root cause as the original finding — `EnterWorktree`'s
    `baseRef: "fresh"` default still branches from `origin/<default-branch>`
    rather than local `HEAD`/the originating checkout's current commit, so any
    coordinator that commits a claim on the originating branch before calling
    `EnterWorktree` must independently detect and fast-forward past the gap;
    a `baseRef: "head"` default, or at least a documented reminder in
    dough-execute-plan's own execution-location guidance, would remove the
    need for this recurring manual check.

## ODF-042 — Coordinator pre-filtered grep results for a test-only representation slice, missing sites the later field-removal slice had to fix

Former local code: DD-037.

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

## ODF-051 — Refactor subagent reported removing dead dependencies it did not actually remove

A post-change refactor subagent consolidated web note-move orchestration into
`NoteMoveService` and reported it had removed the now-unused
`noteMotionService`, `wikiLinkRewriteService`, and `wikiLinkRelocationRewrite`
fields/imports from `RelationController`. The coordinator's proof-acceptance
inspection of the actual `git diff` showed the three fields were still
declared and assigned but never referenced — the report was inaccurate. The
coordinator resumed the same refactor agent with the diff evidence to
actually remove the dead dependencies, then re-ran the affected suites.

### Occurrences
- Execution: plan 100 `cursor/100-receive-web-note-moves` (SEED-009 story 25)
  - Timestamp: 2026-09-15T13:52:00+08:00
  - Tool: Cursor
  - Model: glm-5.2-high
  - Open Dough release: unreleased
  - Evidence: slice 2 refactor return vs `git diff -- RelationController.java`
    on commit `febbd2d9bb`; resumed agent removed the fields, re-verified
    before `8d67cd6cff`.
  - Observed effect: the refactor report's "removed fields" claim did not
    match the diff; three dead dependencies survived the first refactor pass
    and were only removed after the coordinator caught the gap and resumed.
  - Inference: a refactor subagent's report text is not proof; coordinator
    proof acceptance must inspect the actual diff at the reported boundary,
    not trust the report's claimed edits. Dead-dependency removal after
    consolidation is exactly the kind of refactor claim that is cheap to
    verify against `git diff` and easy to misreport.

## DD-061 — Story Branch Mode edits made in the originating checkout got swept into an unrelated concurrent commit

`dough-execute-plan` created a Story Branch Mode execution worktree for plan
131, but the coordinator's later edits to that plan's own PLAN.md (recording
resolved execution identity, then the manual-testing finding report) were
made with the originating checkout's path
(`/Users/terryyin/git/doughnut/...`) instead of the execution worktree's path
(`doughnut-worktrees/131-.../...`), leaving those edits uncommitted directly
on `main` in the shared originating checkout. A concurrent session executing
a different plan in its own worktree (plan 132) later staged and pushed a
broad commit from that same shared `main` checkout; because the coordinator's
stray edits were sitting uncommitted there too, they were swept into that
unrelated commit and pushed to `origin/main` under a commit message with no
mention of plan 131. The plan's content landed correctly, but with no
attribution to its own execution, and the execution's own branch/worktree
ended up stale and empty, later deleted with nothing to deliver.

### Occurrences

- Execution: SEED-009 story 42 / quick/131-manually-validate-append-only-notebook-workflow
  - Timestamp: 2026-09-17T12:23:21+08:00
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: claim commit `979a667474` (12:07:35+08:00, correctly on `main`
    in the originating checkout); PLAN.md edits at 12:16–12:21 targeted the
    originating checkout's absolute path; concurrent commit `35dfb6072896`
    (12:23:21+08:00, message "Refine PRODUCT-BACKLOG and update SEED-009
    documentation") diff includes the plan 131 `PLAN.md` 69-line addition
    alongside unrelated `SEED-009`/`SEED-018`/`DearDough.md`/
    `PRODUCT-BACKLOG.md` changes from the concurrent plan-132 execution;
    execution branch `131-manually-validate-append-only-notebook-workflow`
    was clean and unchanged at the claim commit when removed.
  - Observed effect: the manual-testing report and slice-status update
    reached `origin/main` intact, but with no commit traceable to plan 131's
    own execution, and the execution's Story Branch Mode delivery path was
    bypassed entirely.
  - Inference: when multiple sessions execute plans concurrently against the
    same shared originating checkout, any coordinator action that writes to
    a path under that shared checkout (rather than strictly to the resolved
    execution-checkout path recorded in the plan) risks being staged and
    committed by a different, unrelated session's next commit in that
    checkout. Retaining and consistently reusing the resolved execution
    checkout path for every write after worktree creation would have avoided
    this; a repeated `git status` check before delivery in the wrong
    checkout would also have surfaced it earlier.

## DD-062 — Delegation guidance has no protocol for a subagent that dies mid-edit from an infrastructure error, leaving a silent partial change

[Delegation](../dough-execute-plan/references/delegation.md) and
[refactor return](../dough-post-change-refactor/SKILL.md#return-control) both
assume a delegated agent either finishes and returns a report, or returns an
explicit incomplete stop. Neither covers a subagent that is killed outright by
a host/API error mid-edit: it produces no report at all, and the coordinator's
only signal is a task-notification with `status: failed` and an API error
message. The coordinator had to independently discover, via `git status`/`git
diff` in the execution checkout, that the dead agent had already made a
partial, non-functional edit (two unused imports added to a file, with the
intended method body and its call sites never written) before it could decide
whether to retry, revert, or finish the work itself.

### Occurrences

- Execution: SEED-009 story 43 / quick/132-rebaseline-existing-notebooks / 301184431f
  - Timestamp: unknown (task-notification received 2026-09-17, exact time not
    captured; the error stated a session-limit reset at 4:50pm Asia/Singapore)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.24
  - Evidence: task-notification for the post-change-refactor subagent reported
    `status: failed`, summary "Agent terminated early due to an API error:
    You've hit your session limit ... (error type rate_limit, HTTP 429)", with
    `result` showing its last action was "Now let's implement the shared
    helper in GitBundleTestReader." `git diff` in the execution checkout then
    showed `GitBundleTestReader.java` with two new unused imports
    (`PortableTreeEntry`, `ObjectLoader`, `StandardCharsets`) and no new method
    — the helper it had announced was never written.
  - Observed effect: the coordinator spent one extra investigation round
    (inspecting `git status`/`git diff` to reconstruct what the dead agent
    intended and how far it got) before deciding to complete the interrupted
    refactor directly rather than re-delegating, since a second delegation
    risked hitting the same session-wide rate limit immediately. No corrupted
    or lost work resulted; the partial edit was strictly additive (unused
    imports) and safe to build on.
  - Inference: neither the delegation contract nor the refactor skill's return
    contract names "the subagent's process was killed before it could report"
    as a case, so a coordinator has no documented default (retry once, revert
    the partial edit, or complete it directly) and must improvise from raw
    diff inspection every time this occurs.

## DD-063 — Coordinator kept editing the shared execution checkout while a delegated refactor subagent was inspecting the same files, forcing a wasted stop

[Delivery](../dough-execute-plan/references/wrap-up.md#deliver-the-change) step
1 spawns a fresh post-change-refactor subagent against "the execution
checkout" but does not say the coordinator must stop editing that same
checkout until the subagent returns. Story Branch Mode has one execution
checkout per plan, not one per slice, so a subagent delegated to review only
slice 1's diff and the coordinator's own slice 2 edits land in the same
working tree with no isolation between them. Here the coordinator delegated a
slice-1-only refactor review, then immediately continued in-place with slice
2 production and test edits in the same checkout instead of waiting. The
subagent's own file reads/edits then intermittently disagreed with the
concurrently-changing tree, one of its edit attempts failed with "File has
been modified since read," and after re-reading it correctly detected the
scope had moved past what it was briefed on and stopped with `## REFACTOR
JIDOKA STOP` rather than reviewing stale or moving-target content — costing a
full subagent turn (~6 minutes, ~86k tokens) with no usable output.

### Occurrences

- Execution: SEED-022 story 1 / quick/134-literal-note-title-recall
  - Timestamp: 2026-09-17, between approximately 17:29 and 17:37 +08:00 (the
    subagent was launched right after the slice-1 `pnpm backend:test_only`
    run completed and stopped before the slice-2 run, timestamped
    17:37:42+08:00 in that run's Spring Boot startup log; the subagent's own
    elapsed time was reported as ~6 minutes)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: subagent hand-back report titled "Refactor outcome: STOPPED —
    working tree diverged mid-task from the briefed slice-1-only scope,"
    describing a failed edit ("File has been modified since read"), a
    re-read showing `RecallTitleSegments.java`/Test already deleted and
    `NoteTitle`/test files already carrying slice-2 content the coordinator
    was actively writing, and ending `## REFACTOR JIDOKA STOP`; task-notification
    usage for that run reported `subagent_tokens: 85655`, `duration_ms: 241361`.
  - Observed effect: no refactor findings were produced for slice 1; the
    coordinator had to re-run one consolidated post-change-refactor pass
    later, after all three slices were implemented and stable, to get a
    usable review.
  - Inference: the delivery sequence's "spawn a fresh agent" step, combined
    with Story Branch Mode's one-checkout-per-plan (not per-slice) layout,
    has no explicit rule that the coordinator must pause its own edits to
    that checkout until the delegated subagent returns; without that rule, a
    coordinator eager to keep moving to the next slice will race a
    concurrently-running subagent reading the same files and burn a full
    subagent turn on a stop neither side could have avoided once started.

## Retention

- Highest allocated local number: 63
- Recovery: `f38363d3789bec23e5aa5c323ab56f4baf3db554`
- Occurrence history is partial
