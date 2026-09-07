---
id: SEED-013
status: dormant
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

**Status:** Planned; refined from completed Story 1 and ready for separate execution.

**Goal:** A maintainer can retry a failed release or submit a newer version while
another release is active, without moving tags or accidentally replacing a newer
release with older code.

**Scope:** Build on Story 1. Record complete application outcomes, including
frontend-only releases. Retry an interrupted release with its immutable tag/SHA;
permit fresh successful CI for the same SHA when artifacts must be regenerated.
Persist raw tag object as well as peeled commit; reject moved identities;
completed duplicates do not publish again. Keep one application deployment active
and let it finish. Retain the highest numeric
pending version, even while its CI is pending. Old events and retries cannot
replace a newer deployed/admitted version. Reconcile tag and CI-completion
wakeups without holding a runner while CI is pending.

Initialize the new tracking from the existing published application release so
activation cannot rediscover it as new and overwrite an independently released
CLI. Verified absence of any prior tag-driven application publication permits
empty initial tracking; missing evidence of an existing release must fail visibly.
Reuse the implemented one-shot CI query, publication preflight and separate
source/control checkouts. Preserve the independent CLI stream; do not impose
application-version ordering on CLI tags. Keep correction by a tested revert and
a new patch version.

**Key examples:**

- An upload succeeds but a later deployment step fails → retry the same tag/SHA
  after validation → complete that release without moving its tag.
- Artifacts expired → fail with recovery instructions → rerun exact-SHA CI →
  that successful result can resume the same release.
- A tag's SHA changes after selection or a failed attempt → reject before writes.
- The same successful release is encountered again → no publication or CLI
  replacement, even if its old artifacts have expired.
- v1.3.1 is active, with v1.3.2 and v1.3.3 pending → finish v1.3.1, retain
  v1.3.3 and wait for its own successful checks.
- v1.3.3 is deployed when v1.3.2 finishes CI or is retried → keep v1.3.3.
- Upgrade an installation already released by Story 1 → retain its current
  application/CLI until a genuinely new or incomplete release is eligible.

**Evaluation:** Replay interrupted, duplicate, moved-tag and out-of-order release
attempts and observe production-operation traces and durable application state.

**Value / learning:** Removes the first story's manual single-release coordination
rule while preserving exact tested releases and independent CLI publication.

**Effort hypothesis:** M (about 1–2 hours), medium confidence, assuming reuse of
Story 1's tested entry points; state initialization and event ordering are the
main uncertainty. **Depends on:** Story 1's working tag release path.

**Plan:** [Quick 048](../quick/048-release-recovery-and-ordering/PLAN.md).

## Ordering and Scope Reduction

Implement Story 1 first, finishing with working tag deployment enabled. Story 2
is independently deferrable: if it is cancelled, maintainers retain tag-controlled
releases under the documented single-release, forward-version operating rule.
Do not leave application releases disabled until Story 2, or build its ledger
and queue policy speculatively in Story 1. Drop/defer Story 2 first if reducing
scope further; exact-SHA successful CI and complete validated artifacts remain
mandatory in Story 1.

The two effort hypotheses are intentionally comparable. Story 1 carries workflow
and publication integration; Story 2 carries state/recovery/ordering complexity.
Equal effort is a hypothesis, not a guarantee based on equal leaf counts.

## Scope Preserved Across Both Stories

The previously accepted final behavior is preserved across these two stories.
No automatic version bumps, changelog/GitHub Release creation, staging environment,
application-visible version changes, automatic database rollback, or new CLI
version ordering are included. Creating tags is an operator action, not an
agent verification step. Story 1 is implemented; Story 2 remains planned.

## Open Decisions

No new product decision is required to describe the split. The conservative
one-release-at-a-time rule is the explicit proposed intermediate boundary chosen
in response to the developer's scope-reduction request; automated handling of
violations is retained in Story 2. Revisit that boundary before implementation
if simultaneous releases are required in the first increment.

## When to Surface

Story 1 is complete. Execute Story 2 from its plan, which now records actual
entry points and preserved proof. The
developer explicitly authorized sequential fresh-context worktree execution,
merge/push, main-only observation and branch/worktree cleanup for both stories.
This authorization does not include creating production tags as tests.

## Breadcrumbs

- Original developer requirement and accepted tag, CI, CLI and recovery policies.
- Developer request to decompose the 18-leaf story into two balanced stories.
- Existing workflow and GCP runbook references are recorded in the remaining plan.
