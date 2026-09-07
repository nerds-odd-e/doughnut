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

**Status:** Branch implementation and local proof complete; parent-owned post-merge platform observation pending in Quick 046.

**Goal:** A maintainer deliberately releases an exact tested main commit by
pushing a stable application version tag.

**Scope:** Ordinary main pushes continue CI and publish nothing. An application
tag releases backend, frontend and bundled CLI from that tagged commit, after
its exact CI run has succeeded. The tag may precede or follow CI; main advancing
does not change the selected release. Keep existing CLI tags, CLI version
handling, destination and common publishing logic. Preserve conditional backend
rollout, static routing and existing failure visibility.

**Interim operating boundary:** Maintainers issue one application release at a
time, in increasing version order, keep tags immutable, and do not replay old
application releases. Finish or resolve the current attempt before issuing the
next application tag. The workflow retains a single non-canceling application
deployment group. Automated pending-version selection, durable duplicate/moved-
tag/downgrade protection, and supported recovery of failed attempts belong to
Story 2. This is an explicit reduced first-story boundary, not the final policy.
An unsuccessful attempt fails visibly without publishing unvalidated artifacts;
manual recovery is a newly tested correction/revert and the next patch tag.
During initial cutover, the release source must contain the new tag trigger;
reconciling older pre-cutover sources is deferred with Story 2's CI-event path.

**Key examples:**

- Untagged main push → CI runs → application and CLI download stay unchanged.
- Push v1.3.1 for A after A's CI succeeds → publish A's complete payload.
- Push v1.3.1 before A's CI finishes → wait for A's success, then publish A;
  failed/canceled checks or unavailable artifacts prevent publication.
- Main advances to B while A waits → publish A, not B or an untagged newer build.
- Push v1.3, v1.3.1-rc.1, or an unrelated tag → no application release.
- Push an existing-style CLI tag → preserve its independent CLI-only release.
- A release needs correction → test a corrective/revert commit and issue the
  next patch; schema migrations are not reversed automatically.

**Evaluation:** Observe a tagged end-to-end application release and unchanged
production after an ordinary push; verify both CI/tag arrival orders.

**Value / learning:** Release timing becomes deliberate immediately; verify
exact-commit CI/artifact selection before adding recovery state.

**Effort hypothesis:** M (about 1–2 hours), medium confidence, assuming existing
publication commands remain reusable and a single outstanding release is an
acceptable intermediate operating rule. **Depends on:** no other product story.

**Plan:** [Quick 046](../quick/046-version-tag-production-releases/PLAN.md).

<a id="story-2"></a>

### 2. Recover and coordinate application releases without manual ordering

**Goal:** A maintainer can retry a failed release or submit a newer version while
another release is active, without moving tags or accidentally replacing a newer
release with older code.

**Scope:** Build on Story 1. Record complete application outcomes, including
frontend-only releases. Retry an interrupted release with its immutable tag/SHA;
permit fresh successful CI for the same SHA when artifacts must be regenerated.
Reject moved identities; completed duplicates do not publish again. Keep one
application deployment active and let it finish. Retain the highest numeric
pending version, even while its CI is pending. Old events and retries cannot
replace a newer deployed/admitted version. Reconcile tag and CI-completion
wakeups without holding a runner while CI is pending.

Initialize the new tracking from the existing published application release so
activation cannot rediscover it as new and overwrite an independently released
CLI. Preserve the independent CLI stream; do not impose application-version
ordering on CLI tags. Keep correction by a tested revert and a new patch version.

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
agent verification step. No implementation has started.

## Open Decisions

No new product decision is required to describe the split. The conservative
one-release-at-a-time rule is the explicit proposed intermediate boundary chosen
in response to the developer's scope-reduction request; automated handling of
violations is retained in Story 2. Revisit that boundary before implementation
if simultaneous releases are required in the first increment.

## When to Surface

Execute the narrowed Story 1 when selected. Revisit Story 2 after Story 1 is
complete; rebase its plan on the actual entry-point names and preserve proven
behavior. Planning does not authorize implementation, commits, pushes or tags.

## Breadcrumbs

- Original developer requirement and accepted tag, CI, CLI and recovery policies.
- Developer request to decompose the 18-leaf story into two balanced stories.
- Existing workflow and GCP runbook references are recorded in the two plans.
