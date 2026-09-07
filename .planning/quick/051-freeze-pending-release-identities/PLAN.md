# Freeze pending release identities

Source: Aggregate execution retrospective of the completed version-tag release
implementation; commit evidence is retained below. Enduring contract:
[application release runbook](../../../docs/gcp/conditional-backend-deploy.md).
Status: planned.

## Goal and scope

Make a stable application tag immutable from its first reconciliation, including
while exact-SHA CI is waiting or blocked and while selected artifacts are
unavailable. A moved or deleted observed tag must never become a new release
request, and a durable higher selected version must continue to prevent an older
release from replacing it. Align the overlapping-release guidance in the README
and Definition of Done with the implemented event-driven policy.
Add a discoverable repository `release-application` skill that guides an agent
through a user-requested application release using the canonical release runbook.

Keep Story 2's one non-cancelling `deploy-production` concurrency owner, numeric
version selection, exact-CI/artifact admission, selected-source publication,
bootstrap, completed duplicate no-op, interrupted retry, forward correction and
independent CLI behavior. Do not create production tags, change GitHub settings,
IAM, Slack/GCS configuration, add automatic retries/version bumps, or introduce
a general scheduler.

## Retrospective evidence

- Story 1 product boundary: `8467ffd601..f7bf5efc45`; merged at `a6f2745b8e`.
- Story 2 product/planning boundary: `891eff8ef9..6e6624e54c`; merged at
  `f2cb82d6ce`.
- Current focused proof: `CURSOR_DEV=true nix develop -c bash
  scripts/test/application-release.test` passes 83 command tests and 4 public
  entry tests.
- Installed activation: main CI run `34082470708` succeeded and triggered deploy
  reconciliation run `34083073575`; tracking initialized empty, reconciliation
  returned `none`, and every application publication job was skipped.

The aggregate review found that reconciliation returns `waiting`/`blocked` before
any durable identity write. State becomes identity-bearing only when publication
starts, so replacing a tag before CI readiness—or after artifact admission
fails—can be accepted later as a new identity for the same version. Existing moved
tag tests begin from persisted `publishing` state and do not cover this interval.
The review also found obsolete one-release-at-a-time/CI-waiting instructions in
`README.md` and `docs/teams/definition_of_done.md` after Story 2 removed that rule.

## Outside-in proof map

| Promise | Leaves | Observation |
|---|---|---|
| First-observed stable-tag identity survives non-ready CI | 1–2 | Waiting and blocked public-entry fixtures persist tag/refOid/SHA before returning |
| Artifact failure cannot reopen tag identity | 1, 3–5 | Ready selection survives failed artifact admission; same identity recovers and moved/deleted replay rejects before publication |
| Higher selected version remains the durable ceiling | 2–4 | Higher waiting or ready candidate persists; a late lower wakeup is superseded even if the higher remote tag disappears |
| Operator guidance matches overlapping reconciliation | 6 | README and Definition of Done agree with the release runbook and workflow tests |
| Agents can carry out a requested application release and report actual publication evidence | 7 | Skill validation and a simulated operator walkthrough distinguish release requests, status checks, non-ready CI, skipped publication and successful deployment |

## Ordered slices

### 1. Represent a selected release before publication
Type: Structure
Status: done
Proof: State tests accept every existing `initialized-empty`, `publishing` and
`succeeded` record, plus a selected tag/refOid/SHA without requiring a CI run.
Round trips preserve the exact raw tag object and peeled commit; malformed or
transport-failed records still fail without writes. Keep touched production files
cohesive and below 250 lines by extracting only the state-store detail needed by
the immediately following behaviors.

Internal change: Extend the existing application release state boundary with one
pre-publication selected outcome and an explicit serialized write operation under
the current workflow concurrency owner. Do not create a generic state framework
or change the backend deployment hash record.

Learning: The selected record is the exact tag, raw ref object ID and peeled SHA
without CI run fields. State transport lives in a cohesive store module so the
next slice can freeze that identity without growing the state command past the
repository file-size limit.

### 2. Freeze non-ready release identity
Type: Behavior
Status: done
Proof: Public-command fixtures for absent/in-progress CI and failed/cancelled CI
persist the highest numeric candidate's tag/refOid/SHA before reporting
`waiting`/`blocked`. Replacing its lightweight ref, annotated ref object, or peeled
commit—or deleting it before a later CI wakeup—rejects with zero artifact or
production writes. A newer numeric candidate may replace the selected ceiling;
a late lower event cannot.

Behavior: Reconciliation first observes a qualifying tag whose CI is not ready →
freeze that release identity and version ceiling → return immediately without
holding a runner. Later wakeups may advance only that identity or a higher version.

Learning: Reconciliation admits the current highest tag against durable state
before querying CI. Waiting and blocked outcomes freeze a new or higher identity;
an unchanged selected identity retries, while a missing or changed selected tag
fails before CI lookup and a lower tag is superseded without rewriting state.

### 3. Freeze a ready release before downloads
Type: Behavior
Status: done
Proof: A public reconciliation fixture with ready exact-SHA CI persists the exact
tag/refOid/SHA before returning `ready`; workflow proof keeps artifact download
behind that successful admission. Existing waiting/blocked, publishing/succeeded
retry, completed duplicate and selected-source publication tests remain green.

Behavior: Reconciliation finds ready exact-SHA CI for a new or higher candidate →
freeze its immutable release identity → only then expose it for artifact download.

Learning: The same `continue` admission that freezes waiting and blocked releases
also freezes a ready release before reconciliation returns its CI outcome; the
existing job dependency keeps every artifact download behind that admission.

### 4. Resume the selected release with fresh artifacts
Type: Behavior
Status: done
Proof: A ready selection followed by missing/expired artifact admission leaves the
selected record unchanged. A later wakeup for the same tag/refOid/SHA accepts a
newer successful CI run/attempt and returns `ready` without rewriting identity;
publishing and succeeded retry/duplicate behavior stays green.

Behavior: A selected release loses or lacks its chosen artifacts → a later exact-
identity CI completion supplies fresh artifacts → reconciliation resumes that same
immutable release request rather than rebuilding from another commit.

Learning: No production change was needed. Payload admission fails before its
publication boundary while the selected record remains durable; a later wakeup
for the unchanged identity selects a newer successful run and attempt without a
state rewrite.

### 5. Reject a replacement after artifact failure
Type: Behavior
Status: planned
Proof: Start from a ready selection whose artifact admission fails, then move or
recreate its lightweight ref, annotated ref object or peeled commit—or delete the
tag. The next public reconciliation fails identity admission before CI lookup,
artifact actions or production writes; the selected record is not overwritten.

Behavior: Artifact admission leaves a selected release pending → its tag identity
is changed or removed → a later wakeup rejects the replacement and requires an
immutable retry or next-patch correction.

Refinement learning: The original ready-identity slice exceeded the ten-minute
hard limit and all attempt-owned WIP was reverted after the 97-command/4-entry
baseline stayed green. It combined three proof loops. Reconciliation fixtures
must use `makeReleaseRepository`; the shallow publication checkout proves only
its own payload/publication boundary.

### 6. Publish the overlapping-release policy consistently
Type: Behavior
Status: planned
Proof: Repository search finds no active instruction to issue application releases
one at a time or wait inside the release runner. `README.md` and
`docs/teams/definition_of_done.md` describe increasing immutable tags, highest
pending numeric selection, event-driven CI wakeups, immutable retry and next-patch
forward correction consistently with `docs/gcp/conditional-backend-deploy.md`.

Behavior: A maintainer reads either top-level release overview → receives the
current overlapping-release policy → can submit a newer version or recover the
same immutable release without obsolete manual serialization advice.

### 7. Guide agents through an application release
Type: Behavior
Status: planned
Proof: Validate the new skill's frontmatter and links, then walk through simulated
requests to release an exact tested main commit and to check deployment status.
The release walkthrough identifies the intended unused increasing version,
exact-SHA CI and available artifacts, immutable tag push, selected release
admission, actual publication job and production smoke-check evidence. Waiting,
blocked, failed and skipped publication must not be reported as deployed; a
status-only request must not create a tag or rerun a workflow. Use supplied or
fake results for the walkthrough, without production mutations.

Behavior: A maintainer asks an agent to release Donut → the agent discovers
`release-application` and follows the canonical runbook → completes the authorized
release or reports its specific blocker with tag, SHA and workflow evidence.

Create `.agents/skills/release-application/SKILL.md` with concise name/description
frontmatter and a link to `docs/gcp/conditional-backend-deploy.md`; add a release
entry to `.cursor/agent-map.md`. Keep mechanics in the runbook rather than copying
them. Distinguish application releases from independent `cli-*` releases and
`gsd-ship` PR delivery. Preserve existing user authorization; resolve missing
release identity or authorization before tag push, without imposing repeated
confirmation when already supplied. Follow immutable retry/next-patch correction
guidance and stop on a concrete failure rather than adding automatic retry loops,
version bumps or GitHub Release creation. Report success only after publication
and smoke checks, and disclose any verification that could not be completed.

Sizing: About five minutes for one short skill, its navigation pointer and a
focused walkthrough; no helper scripts or new release machinery are needed.

## Verification and wrap-up

Execution CI observer: coordinator `root-freeze-pending-release-identities`,
checkout `/Users/terryyin/.codex/worktrees/be43/doughnut`, receipt
`/tmp/donut-ci-501/watch-3iElkb`, PID `68481`.

Each leaf targets one commit-sized outcome. Use the existing real-Git/fake-GitHub
and fake-GCS fixtures; no production tag or production payload write is permitted.
Run the focused application-release suite for release implementation changes;
use documentation consistency review for leaf 6 and skill validation plus the
simulated walkthrough for leaf 7. Run the standard execute-plan
refactor/format/check/commit/push wrap-up only when this plan is explicitly
executed. Implementing the skill does not authorize a real application release.

```bash
CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test
```
