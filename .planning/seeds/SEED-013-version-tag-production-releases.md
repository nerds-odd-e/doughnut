---
id: SEED-013
status: completed
planted: 2026-09-07
planted_during: requirement analysis requested by developer
trigger_when: developer selects version-tag production deployment for implementation
scope: medium
---

# SEED-013: Release Donut to production through version tags

## Why This Matters

For Donut maintainers, automatic publication after ordinary main pushes should
change to deliberate releases of an exact tested version. Stable application tags
use vMAJOR.MINOR.PATCH, such as v1.3.1; prereleases and two-component tags do not
qualify. Existing independent CLI releases remain available. The developer asked
to split the former 18-leaf story into two smaller deliveries with approximately
balanced complexity and a reduced first-story scope.

## Alternatives and Decision

- Defer: retains unwanted publication after main pushes.
- Disable deployment alone: stops unwanted releases but gives no useful release
  path; this is only a temporary implementation boundary.
- A manual deploy button alone: controls timing but does not deliver the requested
  version-tag contract.
- Deliver tag releases under a one-release-at-a-time operating rule, then automate
  recovery and overlapping-release policy: chosen decomposition. It supplies the
  requested end-to-end release value before adding release-state management.

The first learning is whether a tag reliably publishes its exact tested payload
while normal development continues. The second is whether release state can
safely handle retries, duplicate events and competing versions without maintainer
coordination. The split is by maintainer outcome, not backend/frontend layers or
an arbitrary midpoint in the old leaf list.

## Story Decomposition

<a id="story-1"></a>

### 1. Release one chosen Donut version while ordinary pushes publish nothing

**Status:** Complete; merged and pushed to main at a6f2745b8e. Normal main CI
started; the installed workflow is tag-only. Real tagged production publication
remains operator confirmation; no test release tag was created.

**Goal:** A maintainer deliberately releases an exact tested main commit by
pushing a stable application version tag.

**Scope:** Ordinary pushes keep CI without publishing. Stable vMAJOR.MINOR.PATCH
application tags publish the tagged main commit's backend, frontend and bundled
CLI after exact-SHA CI succeeds. Independent CLI behavior stays intact. The
interim rule is one application release at a time, increasing immutable tags,
no old reruns, and correction through a tested change/revert plus next patch.
No durable recovery or ordering guarantees until Story 2. The tagged source must
contain the tag workflow until Story 2 adds default-branch event reconciliation.
Enduring behavior and commands live in docs/gcp/conditional-backend-deploy.md
and scripts/ci/application-release*.mjs tests.

<a id="story-2"></a>

### 2. Recover and coordinate application releases without manual ordering

**Status:** Complete; merged and pushed to main at f2cb82d6ce. Donut CI run
34082470708 succeeded and triggered donut deploy run 34083073575, which
succeeded after initializing and reconciling application release tracking. With
no eligible release tag, every application publication step was skipped. No
test release tag was created.

**Goal:** A maintainer can retry a failed release or submit a newer version while
another release is active, without moving tags or accidentally replacing a newer
release with older code.

**Scope:** Durable application tracking records immutable tag, raw refOid, peeled
SHA, selected CI identity, and publishing/succeeded outcome. Exact-identity
interrupted releases may retry with regenerated exact-SHA artifacts; completed
duplicates are no-ops; moved and superseded releases cannot write. Tag and
completed-main-CI wakeups reconcile the highest numeric current version without
polling, while one non-cancelling production concurrency group lets the active
publication finish. Bootstrap preserves existing application/CLI state, and the
independent CLI release stream remains unordered by application versions.
Corrections still require a tested revert/change and a new patch tag. This story
does not add staging, automatic CI reruns/version bumps, schema rollback, a
general scheduler, or production-tag verification.

## Scope Preserved Across Both Stories

The previously accepted final behavior is implemented across both stories.
No automatic version bumps, changelog/GitHub Release creation, staging environment,
application-visible version changes, automatic database rollback, or new CLI
version ordering are included. Creating tags is an operator action, not an
agent verification step.

## When to Surface

Both stories are complete. Surface this seed again only when release operations
show a gap in the documented stable-tag, retry, or ordering behavior.

## Breadcrumbs

- Original developer requirement and accepted tag, CI, CLI and recovery policies.
- Developer request to decompose the 18-leaf story into two balanced stories.
- Enduring release behavior and operator guidance live in
  `docs/gcp/conditional-backend-deploy.md` and the release workflow tests.
