# Smaller CI dependency caches

## Source and authority

**Identity:** SEED-039#story-1

[Prepare CI dependencies faster with a smaller effective cache](../../seeds/SEED-039-faster-ci-feedback.md#story-1).
The owner requested refinement, then slice planning if no open question remained,
with plan refinement only if needed. The owner also authorized keeping, committing,
and merging this session's preparation to main, syncing local and remote main,
and reusing the worktree. Implementation is not authorized by this request.

## Goal and scope

Reduce total dependency-preparation cost for the shared CI setup action, retaining
correct locked installs and required Cypress binaries across warm caches, cold
caches, and dependency updates. Prevent the smaller archive from growing back
through inheritance. Preserve every existing consumer and the current test matrix.
Measure restore, install, and save together; report downstream CI elapsed time
separately from scheduling and test-duration noise.

No test optimization, shard rebalancing, selective installs, per-job browser
policy, package-script cleanup, dependency upgrade, runner migration, production
release, or deletion of shared/persistent caches belongs in this plan.

## Preparation workspace

- Reused session-created workspace: `/Users/terryyin/.codex/worktrees/ci-speed-stories/doughnut`,
  branch `codex/ci-speed-stories`, originally based on `4f02d12cbc117df753ca39d0b0f75e9645d89643`.
- Published preparation base: `017e33316117ed029a39a77f7e962d4bcf79df88`.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`, branch `main`.
- Publication target for authorized preparation: `origin`, `refs/heads/main`.
- This is preparation, not a Taken claim or an execution identity. Recheck
  execution mode, runtime preparation, and publication context when authorized.

## Existing solutions and constraints

PFE decision: change `.github/setup_nodejs_with_cache/action.yml`, the sole shared
owner of CI Node/pnpm setup, frozen recursive installation, and dependency caching.
Its callers are `.github/workflows/ci.yml` (lint/types, both frontend test shards,
other tests, packaging, and all six E2E shards) and `cli-release.yml`. No second
cache abstraction or per-workflow duplicate setup is needed.

The current action caches the pnpm store and the entire `~/.cache/Cypress` under
one lockfile-derived key, with a broad OS-level fallback. `package.json` pins
Cypress 16.1.0 and pnpm 11.27.1. Cypress postinstall is enabled by
`pnpm-workspace.yaml`. Keep the normal install lifecycle and `force_install`
input compatible. Local `scripts/dev_setup.sh` owns developer setup and is not
the CI cache owner. The separate E2E dist cache and JDK cache serve different
responsibilities and are outside the change.

Reuse the existing YAML readers in `scripts/ci/workflow-fixtures.mjs` and existing
CI workflow checks where useful. Those checks currently cover workflow triggers
and notification wiring, not cache performance or real binary availability.
Do not mistake a YAML-shape assertion for runtime cache proof or add tests merely
to restate reversible configuration. New shell logic, if actually necessary,
uses the repository's script/Bach conventions and focused behavioral proof.

ADR 0007, [Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md),
requires disposable runner state for experiments; no shared developer cache pruning
or application data changes. ADR 0006, [Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md),
supports propagating failed installs/verification rather than disguising them.
The current North Star concerns notebook content and adds no constraint here.
No new architecture direction or ADR exception is needed.

## Current decisions and experiment gate

Use one bounded cache lifecycle, not a permanent matrix of competing strategies.
The leading candidate is a fresh cache namespace with pnpm contents keyed to the
locked dependency graph/toolchain and Cypress cached separately for its requested
version/platform. Do not restore the historical combined namespace into the new
caches. Avoid broad fallbacks that perpetuate old browser versions. Prefer exact
dependency reuse initially; only retain a broader reuse policy if measured total
cost and payload bounds justify its extra lifecycle work.

This is an implementation hypothesis, not an observed payload diagnosis:

- The five run links and measured 3,597 MB archive in the seed are baseline
  evidence; their contents have not been inventoried. Before changing policy,
  inspect a restored archive on a disposable Linux runner and record pnpm versus
  Cypress sizes and versions. Compare a clean locked installation to determine
  whether historical binaries, stale package data, or current required data
  dominate. Do not infer that splitting an archive by itself reduces its bytes.
- Reuse the pinned Node/pnpm/Cypress versions and actual action versions. A macOS
  local install is not a Linux cache benchmark. `pnpm/action-setup@v6` already has
  a post action; verify actual pruning/save order before adding redundant cleanup.
- If a clean bounded cache still restores slower than a fresh install, compare
  omitting the costly cache payload within the same outcome. If no simpler policy
  demonstrates improvement, record no demonstrated gain and reassess this plan;
  do not ship complexity or claim success based on archive size alone.

Supporting upstream behavior: [Cypress caching guidance](https://docs.cypress.io/app/continuous-integration/overview#caching)
warns that lax cache keys accumulate binary versions. [GitHub cache documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
states that existing cache contents cannot be changed, so a new key is required
to replace an oversized entry. Neither proves this archive's composition.

## Ordered slices

### 1. Reuse bounded dependency caches without slowing correct installation

Type: Behavior
Status: planned

**Behavior:** Given the committed dependency graph on a fresh CI runner, setup
installs correct dependencies from an empty cache or reuses bounded cached data;
repeat runs prepare materially faster than the observed combined-cache baseline,
and dependency changes do not perpetuate historical cached payloads.

**Work and decision order:**

1. Preserve pre-change timings and inventory under the experiment gate above.
   Record revision, dependency fingerprint, runner image/architecture, tool versions,
   cold/warm state, and exact commands. Reuse the five source runs only where those
   conditions match; otherwise obtain a comparable pre-change runner observation.
2. Make the smallest supported cache-policy change in the shared action. Preserve
   frozen recursive installation, install scripts, and existing caller inputs.
   Use requested/resolved versions from the current package metadata instead of
   duplicating a Cypress or pnpm version constant. Keep namespace, paths, and
   fallback behavior coherent across restore and save.
3. Exercise miss-to-hit reuse and a changed dependency fingerprint on isolated
   Linux runner state. Use an existing supported dependency revision as an install
   fixture for the update case; do not ship a gratuitous package upgrade. Assert
   the current locked Cypress package and binary agree and historical browser
   versions are not inherited. Keep any one-off inventory/fixture instrumentation
   out of the retained product implementation.
4. Compare at least three successful warm observations with comparable baseline
   observations, reporting median and range. Measure cold population separately,
   including save cost, and consider its effect on routine dependency updates.
   Require improvement larger than observed noise in total setup cost. Re-run
   doubtful comparisons; a missing or failed observation leaves proof incomplete.
5. Verify affected consumers, simplify the final configuration, and retain only
   the policy supported by measurement. Record actual results in this plan for
   the later test-optimization story; no test or shard edits are part of delivery.

**Sizing:** One cohesive cache-policy edit and proof loop. Target about five
minutes of active implementation after inventory; scrutinize work beyond five
and refine if active work exceeds ten. Hosted Linux provisioning, cache transfer,
and complete CI runs are a stated external-wait/focused-proof exception and must
be recorded separately. Splitting inventory, cache keys, installation, and proof
into independent delivery leaves would fragment this outcome. If discovery
requires a new cache manager, custom runner, or multiple independent fixes,
return to refinement before broadening implementation.

## Outside-in proof and ownership

Every promise below belongs to the single Behavior slice; no separate test-only
delivery or unowned final verification phase is intended.

| Promise | Observable proof |
| --- | --- |
| Faster preparation, not shifted cost | Real Linux action logs: cache bytes and restore/install/save timings, before/after medians and ranges; job and workflow elapsed with queue delays separated |
| Correct cold and warm setup | Empty isolated namespace then repeat run: successful frozen install, matching Cypress package/binary versions, `pnpm exec cypress verify`, and successful unchanged E2E checks |
| Dependency updates do not regrow inherited payload | Supported prior dependency fixture then current graph in disposable runner state; selected cache keys/paths and inventory show current versions, successful current install and binary verification |
| All consumers retain behavior | Successful ordinary CI including frontend/other tests, lint/types, packaging, and all six E2E shards; `pnpm cli:bundle` proves the release build consumer without a release tag/upload |
| Existing force-install input remains compatible | Run the shared action with `force_install: 'true'` on disposable runner state and verify the installed Cypress binary; do not change its interface |
| Isolation and unchanged test scope | Inspect retained diff: no persistent-cache cleanup, test/coverage reduction, matrix/parallelism change, dependency upgrade, or release trigger modification |

Commands at the relevant boundary (repository tooling locally uses Nix):

```sh
# Existing workflow checks; actual runtime proof comes from Linux Actions.
CURSOR_DEV=true nix develop -c node --test scripts/ci/ci-workflow.test.mjs
CURSOR_DEV=true nix develop -c bash scripts/check_diff_whitespace.sh

# Inside the disposable Linux runner, after the actual shared setup action.
pnpm --frozen-lockfile recursive install
pnpm exec cypress version
pnpm exec cypress cache list
pnpm exec cypress verify
pnpm cli:bundle
```

Inventory uses `pnpm store path` and `du -sk` on that resolved store and the
resolved Cypress cache (`pnpm exec cypress cache path`); record their actual paths.
Capture the action's own install duration separately from any additional
verification install. GitHub collection uses run/job IDs and commit selection,
not the filename filter that returned obsolete history in the original analysis:

```sh
CURSOR_DEV=true nix develop -c gh run list --repo nerds-odd-e/doughnut --commit "$CI_REVISION" --json databaseId,workflowName,conclusion,createdAt,updatedAt
CURSOR_DEV=true nix develop -c gh api "repos/nerds-odd-e/doughnut/actions/runs/$CI_RUN_ID/jobs?per_page=100"
CURSOR_DEV=true nix develop -c gh api "repos/nerds-odd-e/doughnut/actions/jobs/$CI_JOB_ID/logs"
```

Resolve those variables from observed commits/runs during execution. Any temporary
runner probe must invoke the real shared action on an isolated execution branch;
do not claim a local mock measures Actions cache restoration. No probe or profile
has been run by this planning session.

## Delivery and readiness review

On authorized execution use `dough-execute-plan`: Jidoka, a fresh independent
`dough-post-change-refactor` agent, coordinator `./scripts/run.sh pnpm format:changed`
once, plan evidence update, check-only commit hook, and publication with the
workflow's CI observation/repair. API generation is unnecessary for cache-only
configuration. Retain comparative evidence for retrospective and story wrap-up.

Planning review found no remaining slice-boundary or proof-ownership concern:
there is one outcome and one shared owner. The unmeasured payload composition is
an explicit pre-change experiment gate within the slice, not an assumed fact.
The external-wait exception is explicit; active-work overruns trigger refinement.
No additional plan-refinement pass is needed for this single bounded slice.
