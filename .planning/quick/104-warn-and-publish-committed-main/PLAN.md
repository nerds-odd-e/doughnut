# Warn and publish committed main

Source: [SEED-009 Story 2](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2),
refined from a real `notebook publish` refusal with two uncommitted files on
2026-09-11.

Status: planned. Story refinement and slice planning requested 2026-09-11;
implementation not requested.

## Goal and scope

A notebook owner with an otherwise eligible committed `main` can run
`donut notebook publish` while staged, unstaged, or untracked work remains in
the checkout. The CLI warns once that the checkout has uncommitted changes and
that they are excluded, then publishes the bundle built from committed `main`.

Publication never auto-commits, stages, stashes, resets, removes, or bundles the
in-progress changes. Success and server rejection both preserve the index,
working tree, untracked files, and local refs exactly. Keep the useful shape of
the current diagnostic—the checkout path and dirty-file count—but replace its
blocking instruction with truthful warning text such as:

```text
donut: warning: <directory> has uncommitted changes (<N> files not clean, including untracked files) — publishing committed main; local changes are not included.
```

The exact singular/plural behavior remains. Warning output belongs on stderr;
ordinary publication success remains on stdout. Binding, attached `main`, active
Git operation, ancestry, authorization, and server validation behavior remain
as currently implemented. Pull mutates the checkout and retains its clean-tree
readiness requirement; this story changes publication only.

## Key examples and proof ownership

| Promise | Observable proof |
| --- | --- |
| Dirty publication warns and continues | CLI `run` test observes one warning followed by a publication POST rather than `process.exit` |
| Only committed `main` is proposed | Existing submission-boundary bundle inspection remains green; dirty readiness scenario confirms the same submission path is reached with committed `main` unchanged |
| Staged, unstaged, and untracked work is excluded and preserved | Readiness-suite data cases compare porcelain status and representative file/index content before and after successful publication |
| Rejection also preserves dirty work | One existing server-rejection CLI scenario starts dirty, observes the warning and server reason, and compares the complete local Git observation before/after |
| Pull still requires clean state | Existing pull-readiness staged, unstaged, and untracked refusal tests remain green unchanged |

## Current design evidence

`notebookPublishSubmission` already runs `git bundle create ... main` into a
command-owned temporary file and never reads the index or working-tree content.
Its existing CLI test inspects the posted bundle and proves its `main` resolves
to the committed local ref. The requested behavior therefore needs no new Git
mutation or alternate bundle path.

`notebookCheckoutReadiness` currently shares one
`assertAttachedCleanMain` path between publishing and receiving. The branch
check and active-operation check remain common safety rules, but cleanliness has
different business outcomes: pull must mutate/rebase a clean checkout, whereas
publish only reads committed `main`. Keep one status observation and give each
operation its own outcome—warning for publish, refusal for receive—without
duplicating Git queries or readiness messages.

## Ordered slices

### 1. Warn and submit committed main from a dirty checkout
Type: Behavior
Status: planned
Sizing: about 5 minutes for test-first implementation, focused cleanup, and the
CLI suite; medium-high confidence. CLI-suite runtime is an external-wait
exception. Stop and refine before 10 minutes of active work if output routing or
Git-state preservation requires a new product decision.

Behavior: given a bound checkout on eligible committed `main` with staged,
unstaged, and/or untracked changes and no active Git operation → the owner runs
`donut notebook publish` → the CLI emits one dirty-checkout warning, submits the
committed `main` proposal, and leaves all local Git state unchanged.

First replace the three readiness cases that expect dirty-state rejection with
CLI-boundary data cases that expect the warning and successful POST. Capture a
concise pre/post observation that distinguishes the committed ref, index,
working-tree content, untracked content, and porcelain status. Confirm the new
proof fails because the current readiness check exits before submission. Reuse
the existing real posted-bundle inspection for the canonical bundle shape
rather than duplicating its clone-and-inspect machinery in every dirtiness case.

Change publish readiness to report the existing dirty-state detail as a warning
and continue after the shared active-operation and attached-main checks. Keep
receive readiness on the existing blocking clean-tree path. Prefer one status
reader and separate publish/receive decisions; do not introduce a second Git
abstraction or a warning framework for this single message.

Extend one existing server-rejection case with dirty local state so it observes
the warning, the server's rejection reason, and exact local preservation. Do not
turn each HTTP status into another dirty-state case. Keep detached/non-main and
unfinished-operation publication refusals, clean publication, bundle content,
and pull readiness coverage intact.

Proof:

```bash
CURSOR_DEV=true nix develop -c pnpm cli:test
```

Safe stop: dirty local state is visible but non-blocking for publication;
committed `main` remains the sole proposal source; no local state is changed;
pull retains its mutation-safe cleanliness rule.

## Sizing and cumulative-design assessment

One Behavior slice owns one visible command outcome and one CLI proof loop.
Splitting warning output from continued submission would leave either a still-
blocked command or a silent behavior change, so neither is a safe slice. No
Structure slice is needed: the existing readiness and submission boundaries are
already appropriate.

The cohesive rule is operation-specific readiness based on whether the command
mutates the checkout. Existing publish submission already reads only committed
`main`; existing pull tests prove why receive cleanliness is distinct. Do not
generalize this into a configurable warning policy or weaken active-operation,
branch, ancestry, or server checks.

No slice-specific concern remains after inspection. The warning channel and
message intent are explicit, the Git source is already proven, and all local
state variations share the same behavior rather than requiring separate
production branches.

## Delivery

Planning only; execution is not authorized by this request. When authorized,
use `dough-execute-plan`: move Story 2 from Backlog to Taken at execution start,
deliver the slice test-first with Jidoka, run a fresh
`dough-post-change-refactor` pass, run coordinator-owned
`./scripts/run.sh pnpm format:changed` once, update this plan, commit, push, and
handle CI asynchronously. No generated API or schema change is expected.

Keep this plan and its story through execution retrospective and story wrap-up.

## Learnings

Planning inspection confirms publication already bundles committed `main`
independently of dirty files. The blocking behavior is confined to the shared
checkout-readiness policy and its three publish tests. No tests were run and no
product behavior was changed during planning.
