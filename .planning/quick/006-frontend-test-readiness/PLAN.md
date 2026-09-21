# Frontend test dependency readiness

## Source and outcome

- Source: [SEED-039#story-2](../../seeds/SEED-039-fast-isolated-worktree-setup.md#story-2).
- Authority: owner requested execution only if one simple slice suffices;
  otherwise write a slice plan. This is the planning-only branch of that request.
- Goal: reduce repeated preparation time before `pnpm frontend:test`, preserving
  dependency validation, selected tests, arguments, and exit status.
- Scope: this caller and the minimum correction to its existing readiness owner.
  Other scripts, Codex environment setup, caches, and IDE lifecycle work are dropped.
- State: two planned Behavior slices; no implementation or proof has run.

## Existing solution and constraints

PFE finding: `scripts/dev_setup.sh:setup_pnpm_deps` already owns readiness.
`scripts/worktree_setup.sh` and normal Nix shell entry reuse it. Its current
fingerprint reads the root manifest, lockfile, and workspace configuration but
omits member manifests. The root `frontend:test` currently always performs a
frozen recursive install. Simply replacing that prefix could bypass mismatch
validation after a member-only dependency edit. Change the existing owner,
then reuse it; do not create a second fingerprint or cache mechanism.

`CURSOR_DEV=true` Nix entry does not install dependencies, so deleting the
prefix and relying on that hook is insufficient. Preserve direct invocation
in an already-provisioned shell too. Setup must not start services or Biome.

Follow Accepted [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
keep mutable worktree state isolated. Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md):
propagate install failures; introduce no recovery framework. The notebook-tree
direction in [NORTH-STAR](../../NORTH-STAR.md) is unaffected; this investment is
only an aid to delivering it.

## Bounded cost check before edits

In the eventual execution checkout, prepare dependencies once with the existing
canonical setup. On the unchanged revision, record a warm invocation of:

`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storeUndoCommand.spec.ts`

Record revision, elapsed time, outcome, and install-stage time/output. If the
install cost is negligible, stop and report the no-change conclusion instead
of spending on either slice. Do not build a profiling harness or infer token
counts from output length. Repeat the same command after slice 2 under matching
conditions; report a single-pair comparison as indicative, not a benchmark.

## Slices

### 1. Revalidate readiness after workspace dependency changes
Type: Behavior
Status: planned
Behavior: Given successful preparation, a dependency edit in a workspace member
causes the public preparation command to validate dependencies again; an
incompatible frozen lockfile cannot be treated as ready.

Extend the existing readiness owner to cover the current workspace's member
manifest inputs deterministically. Use the repository's workspace definition;
do not introduce a separately maintained package list or scan installed trees.
Preserve unchanged-input skipping and existing install/recovery semantics.

Proof: a focused regression through the preparation boundary for a member-only
manifest change, plus the existing unchanged/stale/interrupted readiness cases.
Use the existing shell-test approach, substituting only external pnpm where
appropriate. Do not duplicate existing failure tests. Run
`CURSOR_DEV=true nix develop -c bash scripts/test/dev_setup.sh.test` and the
focused affected public-entry test. Keep literal proof commands/results here.

Sizing: target about five minutes including focused tests. Workspace input
enumeration is the main uncertainty. A larger validation redesign violates the
story's cost limit: stop for reassessment instead of broadening this slice.
Safe stop: existing preparation handles changed member inputs; frontend test
invocation remains unchanged and safe.

### 2. Reuse readiness before frontend test feedback
Type: Behavior
Status: planned
Depends on: slice 1
Behavior: Given prepared, unchanged dependencies, the normal root frontend test
command runs the requested tests without another install; when preparation is
required, it completes successfully before tests can start.

Wire only `frontend:test` to the shared owner with the smallest necessary shell
entry point. Preserve caller arguments and exit status. Do not invoke the full
interactive shell setup or migrate sibling scripts.

Proof: exercise the actual root command boundary for skipped install and task
argument/exit propagation, and verify the caller does not start its task after
failed preparation. Reuse slice 1's validation/recovery proof. Repeat the real
focused frontend command from the baseline; record elapsed time, passing test
selection, and observable absence of installation. A passing test alone does
not prove the install was skipped. Use shell tests for any new shell entry;
if frontend files change, also apply the frontend skill's typecheck rule.

Sizing: target about five minutes including focused checks. Browser startup may
extend the focused test wait; it does not authorize more implementation scope.
At ten minutes of implementation/proof work, stop and finer-decompose unless
only the stated external test wait remains. Safe stop: the whole narrowed
story is useful without changing any other caller.

## Delivery and remaining concerns

Each executed slice follows normal execution: focused proof, fresh independent
post-change refactor, coordinator `./scripts/run.sh pnpm format:changed` once,
commit with the check-only lint hook, push and asynchronous CI observation.
Take the queued story only when execution is authorized. API generation is not
triggered by this scope. Keep this plan for retrospective and story wrap-up.

Remaining concerns: slice 1's workspace discovery must stay small; slice 2's
actual timing benefit is unmeasured. Neither concern warrants expanding scope.
No token-savings claim is established by this plan.
