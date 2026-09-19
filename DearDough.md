# DearDough Process Findings

Findings about shared OpenDough skills, supporting guidelines, and skill scripts.
Donut-specific findings are recorded in
[DonutRetrospectiveFindings.md](DonutRetrospectiveFindings.md).

## ODF-030 — One-off profile capture treated as durable runner plumbing

Former local code: DD-013.

A tagged publication-profile capture drove a durable `pnpm cy:run` change to
forward Cypress `--expose`/`--config`, including runner tests, then a closing
slice reverted that forwarding because it served measurement rather than the
kept product change.

### Occurrences

- Execution: quick/106-publish-large-notebooks-under-one-minute / 78c24f31bb
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

- Execution: quick/108-publish-notebook-edits-faster / a6fcddacad
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

- Execution: SEED-031 story 1 / quick/147-resolve-deleted-failure-reports
  - Timestamp: 2026-09-18, ~20:00–22:00 +08:00 (all four slice deliveries)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.25
  - Evidence: the CI observer's own diagnostic context reported
    `{"type":"CI_COVERAGE_UNAVAILABLE","repo":"nerds-odd-e/doughnut","branch":"worktree-147-resolve-deleted-failure-reports",...,"reason":"No CI attempt for pushed revision after 3 discovery polls."}`
    after every one of the four slice pushes to that execution branch; final
    `stop` call reported all four pushed SHAs as `"state":"uncovered"` or
    `"unchecked"`.
  - Observed effect: identical to the prior two occurrences — the observer
    ran for the whole execution watching a branch this repo's `ci.yml` cannot
    trigger CI for, and every single push repeated the same
    `CI_COVERAGE_UNAVAILABLE` diagnostic rather than the setup catching it once.
  - Inference: still unfixed as of release 0.3.25; the runtime-setup
    trigger-condition check named in the original finding would have avoided
    four repeated no-op discovery-poll cycles in this one execution alone.

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

- Execution: quick/112-publish-additions-with-simpler-title-check / 691e7be961
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

- Execution: SEED-028 story 1 / quick/140-admin-job-status-local-time / 5d89d2e286
  - Timestamp: 2026-09-18 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `EnterWorktree(name: "140-admin-job-status-local-time")`
    (no `/` in the requested name, so no sanitization applied this time)
    created branch `worktree-140-admin-job-status-local-time` at `e2c55efa75`
    (`origin/main`'s tip), while local `main` was already two commits ahead
    at `e39d945a48` (plan-write commit `5d89d2e286` plus the backlog "Taken"
    claim commit). Detected via `git merge-base --is-ancestor HEAD main` /
    `git log --oneline -3` immediately after entering the worktree.
  - Observed effect: recovered in place with `git reset --hard main` (the
    worktree branch had no commits of its own yet, confirmed a clean
    ancestor first) followed by `git branch -m` to rename off the
    `worktree-` prefix onto the project's literal `NNN-slug` convention; no
    lost work, one extra diagnostic-and-fix round trip.
  - Inference: same root cause as the two prior occurrences, now observed a
    third time with a literal (non-`/`-containing) requested name, confirming
    the base-ref gap is independent of the branch-name-sanitization half of
    this finding.

- Execution: quick/260920-frontend-proof-type-checking / 0d3804b28f
  - Timestamp: 2026-09-19 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `EnterWorktree(name: "260920-frontend-proof-type-checking")`
    (no `/` in the requested name) created branch
    `worktree-260920-frontend-proof-type-checking` at `419ea6e45b`
    (`origin/main`'s tip), while local `main` was already one commit ahead at
    `0d3804b28f` (the backlog "Taken" claim commit made just before). Detected
    via `git log --oneline -3` immediately after entering the worktree.
  - Observed effect: recovered in place with `git rebase main` (the worktree
    branch had no commits of its own yet, a clean ancestor); no lost work,
    one extra diagnostic-and-fix step.
  - Inference: fourth occurrence of the same root cause across four different
    plans/coordinators; continues to support fixing the tool's default base
    ref rather than relying on per-execution detection.

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
    alongside unrelated `SEED-009`/`DearDough.md`/
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

## DD-064 — A prior execution left the shared main checkout on its own feature branch with uncommitted work, blocking the next plan's execution setup

[Execution location](../dough-execute-plan/references/execution-location.md)
requires Story Branch Mode to create one execution branch and worktree per
plan, distinct from the shared main checkout used for queue claims. Here, a
prior execution of plan 136 (SEED-020) instead ran `git checkout
136-trash-vocabulary` directly in the shared main checkout
(`/Users/terryyin/git/doughnut`) and implemented all 5 of that plan's slices
there, uncommitted, rather than creating a dedicated
`.claude/worktrees/136-...` worktree. When this execution was invoked to run
plan 137 (already claimed and committed on local `main`, one commit ahead of
that stranded branch), the shared checkout could not be used: it was on the
wrong branch with someone else's uncommitted 34-file diff in it, and plan
137's directory was invisible from that branch. The mixup had to be diagnosed
and repaired (stash the plan-136 work, move it into its own worktree, return
the shared checkout to `main`, then create plan 137's own worktree) before
implementation could start, and was independently corroborated by a peer
session tasked with the same repair.

### Occurrences

- Execution: SEED-009 story 44 / quick/137-retire-notebook-rebaseline-migration
  - Timestamp: 2026-09-17, unknown exact time (diagnosed and repaired before
    this execution's delivery commit at 20:10:08 +08:00)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `git branch -vv` and `git worktree list` at session start showed
    the primary checkout `/Users/terryyin/git/doughnut` on branch
    `136-trash-vocabulary` (not `main`) with `git status --short` reporting 34
    changed paths implementing all 5 "done"-marked slices of
    `.planning/quick/136-trash-vocabulary/PLAN.md`, while local `main` (one
    commit ahead, `e362217c4f`) already carried plan 137's claim commit and
    `.planning/quick/137-retire-notebook-rebaseline-migration/PLAN.md`. A peer
    session named "trash vocabulary recovery" independently confirmed it had
    been asked to perform the identical repair.
  - Observed effect: this execution could not start slice delegation until
    the shared checkout was repaired; required a stash, a new worktree
    (`.claude/worktrees/136-trash-vocabulary`) to preserve the stranded work,
    a branch switch of the shared checkout back to `main`, and a fresh
    worktree/branch creation for plan 137 before any implementation began.
  - Inference: the prior plan-136 execution's coordinator ran ordinary `git
    checkout <branch>` on the shared main checkout instead of following
    Story Branch Mode's create-a-dedicated-worktree step, likely because nothing
    in [execution location](../dough-execute-plan/references/execution-location.md)
    warns that checking out an execution branch directly in the shared
    checkout — rather than creating its own worktree — strands that checkout
    for every later execution until manually repaired.

## DD-065 — ci-mailbox.mjs's CLI dispatch silently no-ops when invoked through the `.claude/skills` symlink instead of its `.agents/skills` realpath

`ci-mailbox.mjs`'s main-module guard
(`process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href`)
compares the invoked path against the module's symlink-resolved URL. This
project's own Nix shell setup (`scripts/shell_setup.sh`) deliberately
symlinks `.claude/skills/<name>` to `.agents/skills/<name>` in every
checkout/worktree so Claude Code can discover skills, and
`references/runtime-setup.md` documents `.claude/skills/dough-execute-plan`
as the normal Claude Code script path. Invoking
`node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe` (or
`start`/`register-push`/`stop`) makes Node resolve `import.meta.url` to the
real, post-symlink `.agents/skills/...` path while `process.argv[1]` keeps
the invoked `.claude/skills/...` path, so the strict equality check fails,
the CLI dispatch branch never runs, and the process exits 0 with zero
stdout — no error, no `CI_OBSERVER` receipt, no diagnostic. The identical
command run via the `.agents/skills/...` realpath works correctly and prints
the expected receipt. The failure mode (silent success-looking no-op, not a
stop or error) is worse than `runtime-setup.md`'s existing checkout-identity
guidance anticipates: a coordinator following the documented path literally
gets nothing, which is easy to mistake for a successful no-op rather than an
untriggered CLI guard.

### Occurrences

- Execution: SEED-024 story 1 / quick/135-irreversible-relationship-reduction
  - Timestamp: 2026-09-17, evening +08:00 (during CI-observer arming before
    slice 1 delegation)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    run from the execution worktree root produced no stdout and exit code 0;
    an inline `node -e` script importing the same file's `probeMailbox` export
    directly worked and returned a directory; `node .agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-oZWLUN"}`
    and the PostToolUse hook added `CI_MONITOR_READY` context, confirming the
    realpath invocation was the fix.
  - Observed effect: no lost coverage — the silent no-op was caught by testing
    an inline import before trusting the CLI, and the observer was armed
    successfully via the realpath before any slice was delegated — but it
    cost extra diagnostic steps, and a less cautious run could have proceeded
    believing CI observation was set up when it was not.
  - Inference: the CLI entry guard should compare canonicalized/realpath forms
    of `process.argv[1]` and the module path (or otherwise detect direct
    invocation more robustly than exact string equality against a path that
    may traverse a project-standard symlink), since this project's own setup
    deliberately creates that exact symlink in every checkout.

- Execution: SEED-028 story 1 / quick/140-admin-job-status-local-time / 1694c122d9
  - Timestamp: 2026-09-18 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node '.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    run from the execution worktree root produced no stdout and exit code 0,
    with no `CI_MONITOR_READY` PostToolUse context added. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-mL5Kzr"}`
    and the hook added `CI_MONITOR_READY`.
  - Observed effect: worse than the prior occurrence — this coordinator
    concluded CI observation was genuinely unavailable from the silent no-op
    alone, reported that limitation to the user, and had already pushed
    slice 1's commit unobserved before checking this log's existing DD-065
    entry, recognizing the exact match, retrying via the realpath, and
    starting the observer late (after the push it should have covered).
    No coverage was permanently lost (the observer's startup snapshot still
    discovered the already-pushed commit's run), but the sequence shows the
    silent-no-op failure mode reliably reproduces a false "unavailable"
    conclusion for a coordinator that does not already know to check this
    log before trusting the probe's silence.
  - Inference: same root cause and same fix as the original finding; the
    false-unavailable conclusion this occurrence reached is itself further
    evidence for fixing the CLI guard rather than relying on operators to
    recall this log entry.

- Execution: SEED-033 story 1 / quick/144-compact-spelling-results / 55fc4ba255
  - Timestamp: 2026-09-18 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    run from the execution worktree root (with both a bare relative path and
    an absolute path) produced no stdout and exit code 0, with no
    `CI_MONITOR_READY` PostToolUse context added; a standalone inline script
    confirmed `pathToFileURL` resolution matched in isolation, so the silence
    was not an obvious invocation mistake. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-mAT38C"}`
    and the hook added `CI_MONITOR_READY`.
  - Observed effect: same false-unavailable pattern as the second occurrence —
    this coordinator concluded the bridge was unavailable for this
    non-interactive/background-job session, reported that limitation, and had
    already pushed slice 1's commit unobserved before reaching the
    retrospective's process review, which is what surfaced this log's
    existing DD-065 entry and prompted retrying via the realpath. The
    observer's startup snapshot still discovered the already-pushed commit's
    run once armed and the SHA was registered after the fact, so no coverage
    was permanently lost, but two independent coordinators have now reached
    the same wrong "unavailable" conclusion from the same silent no-op before
    reading this log.
  - Inference: same root cause and fix as the prior occurrences. The repeat
    across three separate executions (two different coordinators reaching the
    false-unavailable conclusion) strengthens the case that this needs the
    CLI guard fixed rather than continuing to rely on retrospective review to
    catch it after the fact.

- Execution: quick/260920-frontend-proof-type-checking / 7b1d80b4e8
  - Timestamp: 2026-09-19 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node ./.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs
    start --execution nerds-odd-e/doughnut worktree-260920-frontend-proof-type-checking`
    run from the execution worktree root (after this session had already
    entered a Nix shell there, which creates the `.claude/skills/<name>`
    symlinks) produced no stdout and exit code 0, with no
    `CI_OBSERVER`/`CI_MONITOR_READY` context added. Individually reading that
    same path with `sed`/`grep`/`cp` returned real file content matching
    `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`, while a bare
    `ls -la .claude/skills` from the worktree root (no subpath) showed zero
    entries beside `.` and `..` — the per-file reads and the directory
    listing disagreed about whether the path existed.
    `node ./.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs start ...`
    then printed the expected `CI_OBSERVER {"directory":...}` receipt and the
    PostToolUse hook added its context.
  - Observed effect: no lost coverage — diagnosed via an inline import script
    that printed the module's actual exports before trusting the CLI's
    silence, then armed and registered the already-pushed commit via the
    realpath before shutting the observer down — but cost several extra
    diagnostic tool calls after the branch had already been pushed, during a
    background/non-interactive session with no live user to consult.
  - Inference: same root cause and fix as the prior three occurrences. The
    disagreement between successful individual-file reads and an empty bare
    directory listing for the same `.claude/skills` path is a new wrinkle
    worth naming: it means confirming the target file is readable is not
    sufficient confirmation that the CLI invocation through that path will
    behave correctly, which makes runtime-setup.md's own "do not reuse the
    installed directory that supplied the initially loaded skill" caution
    easy to satisfy on a shallow check while still hitting this failure mode.

## DD-072 — A concurrent session deleted an active Story Branch execution worktree and branch while a delegated subagent was mid-slice

`dough-execute-plan` created the Story Branch Mode worktree
`.worktrees/145-reset-notebook-git-history` (branch of the same name) from
plan 145's queue claim, armed a CI observer bound to that exact path, and
delegated slice 1. While that implementation subagent was working, the
worktree directory and its branch were both removed by something outside this
execution — a second session was concurrently executing plan 146 in
`.worktrees/146-permanently-delete-trashed-content` from the same shared
repository. The subagent had read the plan and sources but had made no edits
yet; its first edit command failed at `cd` with "no such file or directory",
and it correctly returned a BLOCKED stop rather than falling back to the
shared main checkout or recreating the workspace on its own judgement.

The damage was bounded but the diagnosis was not obvious, because the
subagent's inspection of the shared main checkout caught the other session
mid-operation and showed `.planning/quick/145-reset-notebook-git-history/PLAN.md`
and `.planning/seeds/SEED-030-...md` as deleted in the working tree. Both were
in fact intact at `HEAD`, and a later look showed an entirely different set of
uncommitted paths, confirming the first reading was a transient view of
another execution's in-flight tree rather than a teardown of plan 145.

### Occurrences

- Execution: SEED-030 story 2 / quick/145-reset-notebook-git-history
  - Timestamp: 2026-09-18, ~17:06-17:20 +08:00 (between the plan 146 claim
    commit `c619aba43a` at 17:05:27 +08:00 and slice 1's delivery at 17:30 +08:00)
  - Tool: Claude Code
  - Model: claude-opus-5
  - Open Dough release: unknown
  - Evidence: after the subagent's BLOCKED return, `git worktree list` showed
    only `/Users/terryyin/git/doughnut` and
    `.worktrees/146-permanently-delete-trashed-content`; `git branch -a --list '*145*'`
    was empty; the claim commit `e86f76566a` was still an ancestor of `main`
    and the **Taken** backlog entry plus `PLAN.md` were present at `HEAD`. The
    detached CI observer (pid 89451, `/tmp/dough-ci-501/watch-6PqtuY`) was
    still running with its command path pointing into the deleted directory.
  - Observed effect: no committed or uncommitted work was lost, because the
    subagent had not yet edited anything. Recovery was to recreate the branch
    and worktree from `e86f76566a`, re-run `pnpm --frozen-lockfile recursive install`,
    and resume the same subagent with its already-derived design, which it
    still held. The surviving observer was reused rather than relaunched, since
    recreating the worktree at the identical path restored its binding. Cost
    was roughly one wasted subagent turn plus the reinstall and a verification
    pass over the shared checkout.
  - Inference: the removal's owner was never established, so this is recorded
    as an unexplained concurrent-execution hazard rather than an attributed
    fault. Two guidance gaps are visible regardless of who removed it. First,
    nothing in Story Branch Mode makes an active execution worktree evidently
    in-use to a concurrent session doing worktree cleanup in the same
    repository. Second, a subagent reading the shared integration checkout to
    diagnose its own failure can observe another execution's mid-operation
    working tree and reasonably report it as deletion of its own source
    documents; delegation guidance tells subagents to stay out of the
    integration checkout for edits but not that its working-tree state is
    untrustworthy as evidence.

## DD-074 — The documented `.claude/skills` runtime path did not exist at all in a freshly created execution worktree

[runtime-setup.md](../dough-execute-plan/references/runtime-setup.md) documents
`.claude/skills/dough-execute-plan` as the normal Claude Code location of the
CI observer runtime. In a worktree created by `git worktree add` and used
immediately, that directory does not exist: `.claude/skills/<name>` is a
symlink this project's `scripts/shell_setup.sh` creates when a Nix shell is
entered, and a coordinator that creates the worktree and arms the observer
without first entering a shell in it has only the tracked `.agents/skills`
tree. Invoking the documented path therefore fails loudly with
`MODULE_NOT_FOUND` rather than silently. This is the same documented-path
problem as DD-065 but not the same cause: DD-065's silent no-op comes from the
main-module guard comparing an invoked symlink path against a resolved
realpath, and fixing that guard would not help here, because the file is
absent. The shared fix is for the guidance to name `.agents/skills` (the
tracked realpath) as this project's runtime location, or for worktree setup to
create the symlink before the observer is armed.

### Occurrences

- Execution: SEED-034 story 1 / quick/146-permanently-delete-trashed-content / 6f2a2ff1d8
  - Timestamp: 2026-09-18T17:06+08:00
  - Tool: Claude Code
  - Model: claude-opus-5
  - Open Dough release: unknown
  - Evidence: `node '.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    run from the execution worktree root exited non-zero with
    `Error: Cannot find module` / `code: 'MODULE_NOT_FOUND'`;
    `ls .claude/skills/dough-execute-plan/scripts/` returned
    `No such file or directory` while `ls .agents/skills/dough-execute-plan/scripts/`
    listed the runtime. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-4CslQR"}`
    and the PostToolUse hook added `CI_MONITOR_READY`.
  - Observed effect: no coverage was lost and nothing was misdiagnosed. The
    hard error was unambiguous, so the realpath was used on the next call and
    the observer was armed before the first slice was delegated and before any
    push. Cost was one wasted tool call plus one directory listing.
  - Inference: the loud failure mode is strictly better than DD-065's silent
    one — it cannot be mistaken for "bridge unavailable" — but both arise from
    the same documented path being wrong for this project. Which of the two
    failure modes a run hits appears to depend only on whether a Nix shell has
    been entered in that worktree yet, which is not something the guidance
    mentions.
- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb
  - Timestamp: unknown (during initial CI-observer setup, before the first
    slice was delegated)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `ls .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs` in
    the freshly created worktree returned "No such file or directory", and
    `ls .claude/` there listed only `settings.json`, no `skills/`. The
    coordinator did not check `.agents/skills` first; `git ls-files
    .agents/skills | grep dough-execute-plan` (run later, during this
    retrospective) confirms `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`
    was already present and git-tracked in that same worktree the whole time.
  - Observed effect: instead of hitting this entry's documented
    `MODULE_NOT_FOUND` or discovering the tracked `.agents/skills` realpath,
    the coordinator ran `cp -R /Users/terryyin/git/doughnut/.claude/skills
    <worktree>/.claude/skills` to populate the missing path, then armed the
    observer from that copy. The copy worked and no coverage was lost, but
    the copy was unnecessary work and leaves the worktree's skill copy able
    to drift from the main checkout's local, gitignored `.claude/skills`
    install if either is updated later.
  - Inference: a third failure mode for the same root cause, beyond the loud
    error and DD-065's silent no-op: working around the missing path by
    duplicating a local install rather than using the already-tracked
    `.agents/skills` realpath this entry already recommends naming in the
    guidance. Reinforces that the fix, once applied, would avoid this cost
    too.

## DD-075 — The product backlog moved on the shared integration branch between the coordinator's read and its queue claim

`dough-execute-plan`'s [Take queued work](../dough-execute-plan/SKILL.md#take-queued-work)
requires preflighting the originating checkout for branch, backlog path,
tracked/staged changes and ownership of an isolated claim commit, but it does
not say that the backlog's own content may change between the coordinator
reading it to select an entry and writing the **Taken** move. When two
executions start close together against the same shared integration branch,
the second coordinator's picture of the queue can be stale by the time it
edits: the other execution's claim commit has already moved an entry into
**Taken** and added a plan link. Correctness here depended on the edit being
scripted so that it re-read the file from disk and asserted on the exact entry
text; an edit applied from the earlier in-memory reading would have written a
**Taken** section that silently dropped or duplicated the concurrent claim.

### Occurrences

- Execution: SEED-034 story 1 / quick/146-permanently-delete-trashed-content / 6f2a2ff1d8
  - Timestamp: 2026-09-18T17:05:27+08:00
  - Tool: Claude Code
  - Model: claude-opus-5
  - Open Dough release: unknown
  - Evidence: the coordinator's first read of `.planning/PRODUCT-BACKLOG.md`
    showed an empty `## Taken` section and five entries under
    `## Backlog list`. A concurrent execution then committed
    `e86f76566a` ("Take SEED-030 story 2 for execution through plan 145",
    2026-09-18T17:04:35+08:00) to `main` in the shared originating checkout,
    adding SEED-030 to `## Taken` with a `— plan: [145](...)` suffix. The
    claim edit for this execution ran afterwards and produced a `## Taken`
    section containing both entries; `git diff` showed only the intended
    single-line move, and claim commit `c619aba43a`
    (2026-09-18T17:05:27+08:00) touched only that one line.
  - Observed effect: no incorrect write and no lost claim, but the stale
    reading cost three extra verification calls (`git diff`, `git log`,
    `git reflog` plus `git worktree list`) to establish that the file had
    changed under the coordinator rather than that its own edit was wrong,
    and one corrective edit to match the entry formatting the concurrent
    claim had introduced.
  - Inference: the safe outcome came from re-reading the backlog at write
    time and asserting on the exact entry text, not from any rule. Stating in
    the take-queued-work preflight that the backlog must be re-read
    immediately before the **Taken** edit, and that the resulting staged diff
    must be confirmed to be exactly the intended one-entry move, would make
    this independent of how the edit happens to be applied.

## DD-076 — The CI observer's fixed discovery-poll bound reports lost coverage for revisions whose CI run exists and later succeeds

[runtime-setup.md](../dough-execute-plan/references/runtime-setup.md) polls
30 seconds apart and ends discovery after three consecutive misses (about 90
seconds) before reporting `CI_COVERAGE_UNAVAILABLE`. On this project's GitHub
Actions default, a pushed revision's workflow run can take longer than that
to become visible through `gh run list`/the Actions API, especially across a
run of several pushes in quick succession where earlier runs are still
queued or executing. The observer then reports coverage as lost even though
the run exists, is discoverable moments later, and goes on to succeed.

### Occurrences

- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: across five sequential slice pushes to
    `worktree-148-cohesive-accepted-web-folder-changes`, `register-push` was
    called immediately after each `git push`. Four of the five (`ea903668bb`,
    `c8b70f2122`, `12142a4c99`, `622da78f30`) produced a
    `CI_COVERAGE_UNAVAILABLE` event with reason "No CI attempt for pushed
    revision after 3 discovery polls." A manual
    `gh run list --repo nerds-odd-e/doughnut --branch
    worktree-148-cohesive-accepted-web-folder-changes --json
    databaseId,status,conclusion,headSha,createdAt`, run during the next
    slice's proof-acceptance step (minutes later), found each of those four
    runs already `completed`/`success` (run IDs 35349934027, 35351485386,
    35352772896, 35353547219). The fifth push's run (35354749201) was still
    `queued`/`in_progress` when the observer was stopped at plan completion,
    per the protocol's "never wait for CI."
  - Observed effect: no coverage was actually lost — every run that
    completed by the time of manual inspection was green — but the
    observer's own record shows 4 of 5 revisions as `unproved`/`uncovered`.
    Following only the documented "record lost coverage once and continue"
    step, without the extra manual `gh run list` checks this execution added,
    would have under-reported delivered confidence for every slice but the
    third, and would have given no signal to distinguish "CI hasn't run yet"
    from "CI is actually failing and undiscoverable."
  - Inference: the fixed ~90-second, 3-poll discovery bound appears too tight
    for this project's observed push-to-visible latency when several pushes
    happen within roughly ten minutes of each other, plausibly from runner
    queueing contention. A longer discovery window, a bound expressed as
    "time since push" rather than "poll count," or an explicit guidance step
    to manually re-check `gh run list` once before treating
    `CI_COVERAGE_UNAVAILABLE` as final, would likely have prevented every one
    of these four false negatives.

## Retention

- Highest allocated local number: 76
- Recovery: `f38363d3789bec23e5aa5c323ab56f4baf3db554`
- Occurrence history is partial
