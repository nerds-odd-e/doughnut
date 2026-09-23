# Receive LFS files into an existing working checkout

## Source and hold

- Identity: SEED-035#story-15.
- Source: [mapped replacement story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-15).
- **awaiting story refinement — not ready for slice-plan refinement or execution**.
- Resume with dough-story-refinement to clarify this story's goal, scope, and
  examples, then realign this same plan before slice-plan refinement/execution.
- Mapped from original plan 019 leaves 11, 12, and the receive portion of 14.
  Original leaves 8/10 supply established clone/credential behavior through the
  first story; do not reimplement those mechanisms or duplicate their proof.
- No implementation, completed evidence, or readiness transferred.
- Same session-created preparation workspace as plan 019:
  `/Users/terryyin/.codex/worktrees/attachment-lfs-refinement/doughnut`, branch
  `codex/attachment-lfs-refinement`, base `64ca49163a20154f973f1ad99d16e222966f3c73`.
  Originating/integration checkout `/Users/terryyin/git/doughnut`;
  preparation publication target `origin/main`, authorized by the owner's keep
  instruction. Implementation remains unauthorized.

## Intended outcome and preserved constraints

Owners continue in an existing AI IDE checkout after web/other-checkout changes,
without acquiring another directory. Pull receives current files and supported
rebase preserves unpublished local work. This replaces the first story's safe
pre-mutation LFS pull refusal. It is not a new synchronization model or promise
of automatic binary conflict resolution.

Depends on delivered story 13's standard client setup, transfers, admission,
retention, current-only hydration, and complete publish/fresh-clone loop. Keep
legacy receive behavior, server forward-linear acceptance, private learning
identities, exact retained bytes, and raw/LFS size policy. No migration, new
file UI, custom cache, or broader supported merge shapes.

## Provisional mapped slices

### 1. Receive current files into a clean existing checkout
Type: Behavior
Status: planned

Replace the temporary LFS pull refusal for equal-head and fast-forward receive.
Reuse configured standard LFS and current-only hydration. Hydration failure after
head advancement remains a visible failure; an equal-head retry must complete
files before reporting success. Do not hydrate bare history-inspection repositories.

Mapped proof: CLI `run` tests in `notebookPull.fastForward.suite.ts`, concurrent
change/readiness suites, and installed-CLI LFS journey. Observe real bytes,
clean state, no old-version downloads, and failed-then-equal-head retry. Clone
success does not prove pull's different local mutation lifecycle.
Sizing hypothesis: 8–10 minutes plus C/E below; refine before execution.

### 2. Reconcile supported unpublished work while preserving attachments
Type: Behavior
Status: planned

Extend existing supported local rebase/replay paths with LFS configuration and
hydration. Preserve conflict/refusal/abort semantics, files and unpublished
commits; no server merge or new binary merge algorithm. Do not broaden existing
shape acceptance merely to make a fixture pass.

Mapped proof: real-Git `run` rebase/abort/resolved-continuation suites and
`cli_notebook_web_local_reconciliation.feature`. Observe supported note divergence
with attachments retained and accepted current files available after receiving.
Refinement must settle failure recovery when Git history changed but hydration
did not finish, and how existing readiness checks permit safe retry.
Sizing hypothesis: 8–10 minutes plus C/R; inspect actual first-story integration.

### 3. Complete receive guidance and remove the interim limitation
Type: Behavior
Status: planned

The owner follows ordinary clone/edit/publish/web-edit/pull/reconcile guidance in
one working directory. Remove the first story's temporary refusal, retaining
normal dirty-checkout/stale-history constraints and truthful errors. No broad
LFS switch may accidentally leave one supported receive branch unhydrated.

Mapped proof: installed user journey plus existing raw reconciliation regression.
Prove unchanged, already-based, fast-forward, rebased/absorbed, and supported
replay result branches at the smallest sufficient existing boundary; share
hydration proof for equivalent branches rather than repeat the full E2E chain.
Sizing hypothesis: 5–8 minutes; consider consolidation with slices 1/2 during
refinement if this is only duplicated wording/assertion work.

## Commands and inherited evidence

- C: `CURSOR_DEV=true nix develop -c pnpm cli:test`.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature` (created by story 13).
- R: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_local_reconciliation.feature`.
- Backend change, if actually required: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Before refinement inspect delivered story 13 and reuse only proof matching the
current revision and promise. No tests ran and no commands are passing evidence
here. Scope/effort hypothesis is M (1–2 hours), low confidence pending failure
recovery refinement. Usual execution refactor/format/commit/CI rules apply only
after separate execution authority. This mapped plan is not ready.
