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

## ODF-044 — Hard-limit refinement after completed work creates bookkeeping without a smaller remaining leaf

Former local code: DD-039.

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

## ODF-046 — Coordinator applied a plan update to the main checkout instead of the Story Branch Mode worktree

Former local code: DD-041.

During planned execution under Story Branch Mode, the coordinator's
file-editing tools default to the main checkout's working directory. A plan
status update was applied to the main checkout's copy of the PLAN file rather
than the execution worktree's copy, requiring a revert in the main checkout and
re-application in the worktree before staging.

### Occurrences

- Execution: SEED-009 story 34 / quick/117-web-trash-navigation-recovery / c94efe0af3
  - Timestamp: 2026-09-13T19:07:00+08:00
  - Tool: Cursor
  - Model: GLM 5.2
  - Open Dough release: 0.3.16
  - Evidence: after slice 1 implementation returned, the coordinator edited
    `/Users/terryyin/git/doughnut/.planning/quick/117-web-trash-navigation-recovery/PLAN.md`
    (main checkout) instead of
    `/Users/terryyin/git/doughnut-worktrees/story-34/.planning/quick/117-web-trash-navigation-recovery/PLAN.md`
    (execution worktree). `git -C /Users/terryyin/git/doughnut status --short
    .planning/` showed the modification in main while the worktree's plan was
    clean; the coordinator reverted main with `git checkout -- <path>` and
    re-applied the edit in the worktree before staging. Slice 1's commit
    (c94efe0af3) was made at 2026-09-13T19:08:45+08:00, bounding the slip to
    just before it.
  - Observed effect: one revert and one re-application of the same one-line
    plan-status edit; no lost work and no contaminated commit, since the
    mismatch was caught by checking both checkouts' status before staging.
  - Inference: during Story Branch Mode, plan updates and other coordinator
    edits must target the execution worktree path explicitly; the editing
    tools do not bind to the worktree from an earlier `cd` used for shell
    commands. Verifying which checkout a file edit landed in (as done here)
    contains the slip to a cheap revert.

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

## ODF-054 — Delegated implementers edited PLAN.md that the coordinator must re-own

Former local code: DD-056.

Delegated slice implementers wrote PLAN.md updates. The coordinator
re-applied or re-owned those updates before delivery, so the plan stayed
coordinator-owned but with extra edit traffic.

### Occurrences

- Execution: SEED-009 story 28 / quick/129-publish-trash-moves / 473550c16e
  - Timestamp: unknown
  - Tool: Cursor
  - Model: Cursor Grok 4.6
  - Open Dough release: 0.3.22
  - Evidence: coordinator process notes for this execution (Story Branch
    Mode worktree `doughnut-worktrees/story-28`); PLAN.md appears in
    product and CI-repair commits throughout `a4507e5304..1c790376e4`.
  - Observed effect: extra coordinator re-ownership of plan status and
    evidence before staging; no contaminated product commit identified.
  - Inference: the execute-plan split (implementer owns code/proof,
    coordinator owns PLAN.md) is not enforced at the agent-edit boundary
    when implementers can write the same path.

## ODF-055 — Post-change refactor elapsed well above the ~5-minute leaf target

Former local code: DD-057.

Some post-change refactor passes ran about 12–25 minutes of active work
against the ~5-minute execution-leaf target (scrutinize above five minutes;
finer-decompose above ten).

### Occurrences

- Execution: SEED-009 story 28 / quick/129-publish-trash-moves / 473550c16e
  - Timestamp: unknown
  - Tool: Cursor
  - Model: Cursor Grok 4.6
  - Open Dough release: 0.3.22
  - Evidence: coordinator process notes for this execution; PLAN.md
    records refactor outcomes for slices 2, 4, 5, 7, and 8 without
    per-refactor elapsed minutes.
  - Observed effect: refactor duration was reported as several times the
    leaf target while product slices still completed.
  - Inference: qualified — the 12–25 minute range is coordinator-reported,
    not a per-slice measured log; backend suite waits (~1m) are already
    treated as test-wait exceptions and do not explain a 12–25 minute
    refactor.

## DD-058 — Delegating `EnterWorktree` to a subagent fails; the coordinator must call it directly

Neither this project's execute-plan guidance nor the harness tool's own
description warns a coordinator away from delegating worktree creation, so a
coordinator following "use ordinary host Git facilities" can waste a full
round trip discovering that `EnterWorktree` must run in the coordinator's own
session.

### Occurrences

- Execution: SEED-009 story 41 / quick/130-hide-trashed-incoming-references / 5062dbe79e
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.24
  - Evidence: a `fork` subagent asked to call `EnterWorktree(name:
    "130-hide-trashed-incoming-references")` returned "EnterWorktree cannot
    create a worktree from a subagent with a cwd override ... it would mutate
    the parent session's process-wide working directory. ... spawn an Agent
    with `cwd` set to it." on 2026-09-17 (date only; exact time unknown).
  - Observed effect: one wasted subagent launch and `TaskStop`, no worktree or
    branch created; the coordinator then called `EnterWorktree` itself and
    succeeded immediately.
  - Inference: [execution location](../dough-execute-plan/references/execution-location.md)
    resolves worktree creation as an ordinary coordinator step but does not
    say the call must happen in the coordinator's own session rather than a
    delegated agent; on Claude Code that distinction is load-bearing.

## DD-059 — Runtime-setup's `.claude/skills` default path does not exist inside a fresh git worktree on this project

This project's `.gitignore` excludes `.claude/*` except `.claude/settings.json`
(`.gitignore:143`), so `.claude/skills/dough-execute-plan` — the path
[runtime setup](../dough-execute-plan/references/runtime-setup.md) names as
the normal Claude Code location for the installed skill directory — is absent
from any freshly created worktree. Only `.agents/skills/dough-execute-plan`
(tracked in Git) resolves there.

### Occurrences

- Execution: SEED-009 story 41 / quick/130-hide-trashed-incoming-references / 5062dbe79e
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.24
  - Evidence: `git check-ignore -v .claude/skills` in the execution worktree
    returned `.gitignore:143:.claude/*  .claude/skills`; `ls
    .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs` failed with "No
    such file or directory" while the same path under `.agents/skills/`
    existed and canonicalized to the worktree root as required.
  - Observed effect: one extra investigation round (gitignore check, path
    listing) before arming the CI observer with the correct runtime path;
    no incorrect observer was armed.
  - Inference: the "normally `.claude/skills/...` for Claude Code" guidance
    is a same-checkout default that silently breaks the first time this
    project's execution moves to a new worktree; the runtime-identity
    canonicalization check caught the mismatch instead of arming against a
    missing script.

## DD-060 — Trunk publication's "fast-forward the local target" step is unreachable from an `EnterWorktree`-isolated coordinator session

[Trunk publication](../dough-execute-plan/references/trunk-publication.md#publish-the-candidate)
assumes the coordinator can update and push a local `main` in a shared
integration checkout. On Claude Code, once `EnterWorktree` has switched the
session into an execution worktree, the harness refuses any command that
targets the originating checkout (`git -C <other-checkout>` or `cd` back to
it), so that literal step cannot run.

### Occurrences

- Execution: SEED-009 story 41 / quick/130-hide-trashed-incoming-references / 5062dbe79e
  - Timestamp: unknown
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.24
  - Evidence: `git -C /Users/terryyin/git/doughnut status --short` from the
    execution worktree returned "This session is isolated in the worktree
    ... Refusing to run it — a worktree-isolated session's git operations
    must target its own worktree."
  - Observed effect: the coordinator instead pushed the exact verified
    candidate SHA straight to the remote branch ref from the execution
    checkout (`git push origin 5062dbe79e:refs/heads/main`), which Git
    accepted as an ordinary fast-forward since the candidate's parent was
    already the fetched `origin/main` tip; no local `main` ref was ever
    touched.
  - Inference: the documented sequence (fetch, reconcile, rebase, fast-forward
    local target, push) is written for a coordinator that shares a working
    directory with the integration checkout; an `EnterWorktree`-isolated
    coordinator instead needs a SHA-to-remote-ref push, which is behaviorally
    equivalent for the already-based-on-current-trunk case but was not named
    as an option.

## DD-061 — Delegation guidance has no protocol for a subagent that dies mid-edit from an infrastructure error, leaving a silent partial change

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

## Retention

- Highest allocated local number: 61
- Recovery: `f38363d3789bec23e5aa5c323ab56f4baf3db554`
- Occurrence history is partial
