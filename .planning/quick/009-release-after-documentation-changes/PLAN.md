# Release after documentation changes

## Source

[SEED-038#story-1](../../seeds/SEED-038-release-after-documentation-changes.md#story-1).
The owner authorized a story when needed, implementation, commit and push to main.

## Goal and scope

Skip docs/planning-only branch CI without losing tagged application releases:
reuse an applicable successful ancestor's artifacts while preserving release
identity and test gates. No release tag is created by this execution. Shared
OpenDO watcher changes are outside this repository change.

## Execution identity

- Caller-selected current checkout/branch: `/Users/terryyin/git/doughnut`, `main`.
- Originating/integration checkout is the same user-selected checkout; no worktree.
- Authorized target: `origin` (`git@github.com:nerds-odd-e/doughnut.git`), `refs/heads/main`.
- Starting revision: `4f02d12cbc117df753ca39d0b0f75e9645d89643`, verified equal to fetched origin/main.
- Replanning allowed by the owner's instruction. No backlog claim: newly selected
  story is unqueued; preserve all existing Taken and queued work.
- Runtime preparation: frozen pnpm install completed; existing exact-CI command
  tests passed 19/19 in this checkout.
- CI observer: GitHub `ci.yml` / `donut CI`, target `nerds-odd-e/doughnut:main`,
  coordinator `release-docs`, mailbox `/tmp/dough-ci-501/watch-Uk5tp3`, PID 92312,
  terminal session 95195, yielded cell 24. Armed before first publication.

## Current decisions and existing solutions

Extend `application-release-ci.mjs` and reconciliation, existing Git/HTTP command
fixtures, and existing artifact downloads/publication. Read ignored paths from
the tagged commit's `.github/workflows/ci.yml`; do not duplicate the folder list.
The shared watcher classifier owns observer coverage, not release admission, and
its narrow parser cannot read the existing branches filter. Do not make production
release depend on installed agent-skill code. Use the existing YAML dependency
with an explicit lightweight release-runtime install if needed.

Select the nearest applicable CI evidence, including pending/failure, rather than
searching only for green runs. Exact-SHA runs retain precedence. All non-ignored
paths, including deletions and both sides of renames, participate in equivalence.
Unavailable CI/artifacts preserve explicit recovery rather than silently bypassing
tests. No new automatic build orchestration is needed for the ignored-only case.

ADR 0006 (failure handling) applies: propagate unexplained failures; handle only
the intended waiting/blocked release outcomes. ADR 0007 (environment isolation)
applies: proof uses disposable Git repositories and mock external HTTP/GCS; no
production deployment or persistent application data changes. Current North Star
topics concern notebook content and do not constrain this CI change.

## Outside-in proof

`CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test`
exercises release CLI boundaries with real temporary Git history and external
HTTP/GCS fixtures. Extend reconciliation scenarios for reused artifacts, mixed
changes, lineage, and latest-attempt failure; check publication source identity.
Workflow tests verify native path filters, branch-only CI and unchanged tag
triggers. No live release is authorized or required.

## Ordered slices

### 1. Admit an equivalent tested ancestor for a tagged release
Type: Behavior
Status: done
Behavior: A tag without exact-SHA CI, with an applicable ancestor and changes only
in ignored paths, admits that ancestor's successful artifacts and records both
release and build provenance; pending/failed/non-equivalent evidence cannot deploy.
Proof: Release command tests above, including real Git diffs and publication records.
Sizing: Target 5 minutes of implementation; allow up to 10 minutes for the
cohesive admission/provenance test boundary. Reassess if a larger redesign emerges.

Sizing reassessment: at ~10 active minutes, new reuse scenarios passed and 109/114
focused tests passed; five old output-shape assertions need `ciSha`. The estimate
missed backend embedded-commit health validation, strict persisted-record schema,
and notification recovery consumers. Keep a bounded ~3-minute completion exception
for these necessary consumers: splitting their provenance update would deliver
an internally inconsistent release. No new workflow/build machinery is added.

Accepted proof: `CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test`
passed 118 release cases plus 4 entry cases. Subsequent tagged-policy and multi-doc
history proof passed 15 reconciliation cases. Independent refactor centralized
artifact provenance and split reuse/state-admission tests; its focused command
`CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release-ci.test.mjs scripts/ci/application-release-reconciliation.test.mjs scripts/ci/application-release-artifact-reuse.test.mjs scripts/ci/application-release-state.test.mjs scripts/ci/application-release-state-admission.test.mjs scripts/ci/application-release-selection.test.mjs`
passed 58 cases. `CURSOR_DEV=true nix develop -c bash scripts/test/deploy-backend-jar-to-gcp-mig.sh.test`
passed. Coordinator inspected real Git setup and reconciliation assertions in
`application-release-artifact-reuse.test.mjs`, independent mock binary SHA and
saved record/tagged routing assertions in publication tests, and replay/bootstrap
and recovery-message assertions in state/notification tests. Refactor returned
`REFACTOR COMPLETE`; whitespace check and coordinator formatting passed.

Implementation uses first-parent main history and stops at the first nonignored
tree difference. CI `ciSha` is carried independently from tagged `sha`; backend
health and deployment metadata use `DEPLOY_BUILD_SHA`, while SPA routing keeps
the tagged SHA. Existing persisted records without `ci_sha` remain readable.

### 2. Skip documentation branch CI and document release recovery
Type: Behavior
Status: planned
Behavior: Pushes restricted to docs/planning create no CI run; application tags
continue through ancestor-aware admission. Operators can identify and explicitly
rerun the artifact source build if its artifacts expire.
Proof: Workflow trigger tests and full focused release suite; updated release
runbook and release skill agree with the implemented provenance/recovery contract.
Sizing: Target 5 minutes plus focused verification.

## Delivery

Each slice: inspect proof, independent dough-post-change-refactor agent, coordinator
`./scripts/run.sh pnpm format:changed` once, update this plan, check-only commit
hook, push main, register accepted SHA with the single CI observer. Then automatic
retrospective and bounded completion observation. Retain plan/source for wrap-up.
