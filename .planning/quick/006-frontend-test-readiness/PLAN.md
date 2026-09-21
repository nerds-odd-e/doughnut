# Frontend test dependency readiness

## Source and outcome

- Source: [SEED-039#story-2](../../seeds/SEED-039-fast-isolated-worktree-setup.md#story-2).
- Authority: owner requested execution only if one simple slice suffices;
  otherwise write a slice plan. This is the planning-only branch of that request.
- Goal: reduce repeated preparation time before `pnpm frontend:test`, preserving
  dependency validation, selected tests, arguments, and exit status.
- Scope: this caller and the minimum correction to its existing readiness owner.
  Other scripts, Codex environment setup, caches, and IDE lifecycle work are dropped.
- State: complete as an evidenced no-change. On 2026-09-21 the owner explicitly
  dropped both unexecuted Behavior slices and requested story wrap-up.

## Execution identity

- Mode: Story Branch Mode.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`, branch
  `main`.
- Execution checkout: `/Users/terryyin/.codex/worktrees/frontend-test-readiness/doughnut`,
  branch `codex/frontend-test-readiness`.
- Authorized target: `origin/main`.
- Published backlog claim: `83c2489755ee8ca0af247bc4d5c50e85d070c153`.
- CI observer: GitHub Actions workflow `ci.yml` / `donut CI` for
  `nerds-odd-e/doughnut` branch `codex/frontend-test-readiness`, coordinator
  `root`, mailbox `/tmp/dough-ci-501/watch-BGg4bn`, PID `86389`, yielded cell
  `28`. The earlier claim publication to `main` remains unobserved by this
  story-branch observer.

## Bounded cost result

At unchanged revision `83c2489755ee8ca0af247bc4d5c50e85d070c153`, after
successful canonical worktree preparation, the warm focused invocation passed:

`/usr/bin/time -p env CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storeUndoCommand.spec.ts`

Result: 11 selected tests passed; elapsed time was 6.77 seconds. The command
still visibly selected the install-prefixed script, but the silent install
produced no interaction or output.

The isolated warm install-stage measurement also passed:

`/usr/bin/time -p env CURSOR_DEV=true nix develop -c pnpm --frozen-lockfile --silent recursive install`

Result: elapsed time was 0.91 seconds with no install output. This sub-second
cost is negligible for the plan's bounded investment decision, so execution
stopped before either slice as required. No before/after performance claim is
established, and implementation requires an explicit reassessment rather than
resuming the planned slices automatically.

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

## Completion decision

The bounded cost result selected the plan's explicit no-change branch. The
owner subsequently dropped all remaining implementation scope. No slices,
follow-ups, product changes, or performance claims remain to deliver.
