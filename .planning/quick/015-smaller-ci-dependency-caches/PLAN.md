# Smaller CI dependency caches

## Source and authority

**Identity:** SEED-039#story-1

[Prepare CI dependencies faster with a smaller effective cache](../../seeds/SEED-039-faster-ci-feedback.md#story-1).
The owner requested refinement, then slice planning if no open question remained,
with plan refinement only if needed. The owner also authorized keeping, committing,
and merging this session's preparation to main, syncing local and remote main,
and reusing the worktree. The subsequent owner instruction authorized execution;
the retained execution context below records that transition.

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

## Execution context

- Owner authorized execution and a published Taken state on 2026-09-23.
- Trunk publication from retained worktree `/Users/terryyin/.codex/worktrees/ci-speed-stories/doughnut`,
  branch `codex/ci-speed-stories`; implementation target `origin/refs/heads/main`.
- Starting fetched trunk: `853142c3f477588c2664dea5a757885f3850b2b4`.
- Taken claim: `89f4b49b5f589af70db5c98b3277679e35df4d63`, confirmed on origin/main
  and fast-forwarded into clean local main before implementation.
- Replanning permission retained from the owner's refinement instruction; outcome
  and story order remain unchanged.
- `./scripts/run.sh bash scripts/worktree_setup.sh` completed against the locked
  graph. `CURSOR_DEV=true nix develop -c node --test scripts/ci/ci-workflow.test.mjs`
  passed 2/2 in this checkout.
- CI observer: coordinator `ci-cache-story`, GitHub workflow ID `5208694`, name
  `donut CI`, target `nerds-odd-e/doughnut:main`; mailbox
  `/tmp/dough-ci-501/watch-GdUvTL`, PID 10353, terminal session 8763, yielded cell 44.
  Claim registered. Temporary branch probes supply performance proof separately.

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

## Experiment gate and selected policy

The original hypothesis was exact locked-graph pnpm caching plus independently
versioned Cypress caching in a fresh namespace. Before changing policy, inventory
restored and clean Linux runners using current pinned toolchain/action versions.
Measure total setup including restore, install and saves; a macOS local install
is not comparable evidence. Confirm actual pnpm post-action ordering before
adding cleanup. Compare omitting a payload if restoring it costs more than a
fresh installation. Do not ship complexity or claim success from byte savings
alone. The observations below record this gate and select Cypress-only caching.

Supporting upstream behavior: [Cypress caching guidance](https://docs.cypress.io/app/continuous-integration/overview#caching)
warns that lax cache keys accumulate binary versions. [GitHub cache documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
states that existing cache contents cannot be changed, so a new key is required
to replace an oversized entry. Neither proves this archive's composition.

## Ordered slices

### 1. Reuse bounded dependency caches without slowing correct installation

Type: Behavior
Status: done (implementation and probe proof; ordinary CI observation pending publication)

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

Focused local checks: `CURSOR_DEV=true nix develop -c node --test scripts/ci/ci-workflow.test.mjs`
and `CURSOR_DEV=true nix develop -c bash scripts/check_diff_whitespace.sh`.
Runtime probes invoke the real shared action, inventory `pnpm store path` and
`pnpm exec cypress cache path` using `du -sk`, then run `pnpm exec cypress version`,
`pnpm exec cypress cache list`, `pnpm exec cypress verify` and `pnpm cli:bundle`.
GitHub logs are retrieved with `CURSOR_DEV=true nix develop -c gh api
repos/nerds-odd-e/doughnut/actions/jobs/JOB_ID/logs`; resolve jobs from the linked
runs below, not the workflow filename filter that returned stale history.

## Execution observations

Baseline probe [35817649078](https://github.com/nerds-odd-e/doughnut/actions/runs/35817649078)
used disposable branch revision `5cfb76a8018956f02347c4f55eaec8ff7423e624`,
the unchanged shared action and current dependency graph. All four jobs passed
on Ubuntu 24 image `20260907.300.1`, X64, Node 26.10.0, pnpm 11.27.1.
Lockfile SHA256: `4e0bccda463eb1e4b516f439c78033e79b8a436c2a2b3e01202ab5d005d25cf5`.

- Three exact legacy hits restored 3,771,345,708 compressed bytes. Setup before
  inventory took 69.673, 72.910 and 73.899 seconds; cache post-actions added
  approximately five seconds each (no save on exact hits).
- Restored pnpm store occupied approximately 4,397,000 KiB; Cypress occupied
  9,954,132 KiB with 12 versions from 15.15.0 through 16.1.0. This establishes
  accumulated binaries as the largest payload, with stale pnpm data also present.
- Uncached locked install took 18.77 seconds; total setup 28.035 seconds.
  Required pnpm store was 797,464 KiB and Cypress 838,640 KiB (only 16.1.0).
- All jobs verified package/binary version agreement and ran Cypress verification.
  Actual logs show the cache post-action runs before pnpm's post-action, which
  reported `Pruning is unnecessary.` No cleanup step is justified by assumption.
- Temporary probe workflow replaces normal workflows only in its disposable Git
  tree; no probe instrumentation is retained in the product change.

Measured selection: retain only the exact Cypress version/platform cache and
install the locked package graph afresh. The initial separate pnpm cache was
rejected because its restore and post-action overhead made it slower than an
uncached install. This follows the plan's payload-omission experiment gate;
the shared action remains the sole owner, with no new abstraction or slice.

| Policy, three successful observations | Median setup including post-actions | Range |
| --- | --- | --- |
| Original combined cache | 78.420 s | 75.268–79.700 s |
| Separate exact pnpm and Cypress caches | 32.747 s | 31.798–33.204 s |
| Uncached install, concurrent comparison | 27.253 s | 27.050–28.291 s |
| Selected Cypress-only cache | 22.676 s | 22.331–25.290 s |

Setup includes Node/pnpm setup, cache lookup/restore, install and all setup
post-actions (save or hit handling). It excludes checkout, inventory, verification
and job scheduling. Composite action durations come from native log markers;
uncached post duration spans setup cleanup until checkout cleanup. Warm selected
samples are 22.331, 25.290 and 22.676 seconds. The selected median saves 55.744
seconds (71.1%) versus legacy; its range also stays below concurrent uncached
observations. This is dependency-preparation evidence, not a claim that whole CI
becomes 71% faster.

Split-policy [run 35817984989](https://github.com/nerds-odd-e/doughnut/actions/runs/35817984989),
revision `4e1d8c3dcfce12e11a74d26f7ad92f252caad886`, passed cold current,
three warm, prior dependency fixture and force-install/bundle jobs. Cold total
was 42.624 seconds including both saves; the retained Cypress save took 3.629
seconds. The prior fixture `3d4a7af2ecf0d43739075156dc6dfabf4b6804e7`
populated Cypress 16.0.0 and pnpm 11.27.0. Current runners selected only 16.1.0.

Selected-policy [run 35818263221](https://github.com/nerds-odd-e/doughnut/actions/runs/35818263221),
revision `8debaf862b4fcdca73513a227db2f6fafc1e30a6`, passed all seven jobs:
three warm, three concurrent uncached, and force-install plus CLI bundle.
All used the same runner image/toolchain/lock fingerprint as the baseline.
The retained Cypress cache is 225,421,581 bytes (215 MiB), versus 3,771,345,708
bytes (3,597 MiB) originally: 94% less transferred cache data. It restores only
`~/.cache/Cypress/16.1.0`; assertions verified sole cached version, package/binary
agreement, and successful `pnpm exec cypress verify`. No pnpm data is restored
or saved. The previously populated 16.0.0 cache is not inherited. Job
107044539284 exercised `force_install: 'true'` and `pnpm cli:bundle` successfully.
Cold Cypress population proof remains valid because the retained cache key,
path, install lifecycle and save action are unchanged from the split cold run;
ordinary main CI will additionally exercise a fresh main-scoped cache.

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

Delivery review: final action is semantically identical to the successful selected
probe; YAML/Bash syntax and existing workflow tests passed. Independent refactor
found no edits needed. Coordinator formatting passed. No API generation applies.
Active implementation stayed below five minutes; Linux provisioning, transfer and
probe measurements used the stated external-proof exception. Ordinary main CI,
including all unchanged E2E shards, is the remaining post-publication observation;
the execution observer owns its completion receipt. Keep this story Taken through
execution and retrospective, until authorized story wrap-up.
