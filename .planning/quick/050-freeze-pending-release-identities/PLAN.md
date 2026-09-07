# Freeze pending release identities

Source: [SEED-013 Stories 1–2](../../seeds/SEED-013-version-tag-production-releases.md#story-decomposition).
Created by the aggregate execution retrospective after both stories merged to
`main`. Status: planned.

## Goal and scope

Make a stable application tag immutable from its first reconciliation, including
while exact-SHA CI is waiting or blocked and while selected artifacts are
unavailable. A moved or deleted observed tag must never become a new release
request, and a durable higher selected version must continue to prevent an older
release from replacing it. Align the overlapping-release guidance in the README
and Definition of Done with the implemented event-driven policy.

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
| Artifact failure cannot reopen tag identity | 1, 3 | Ready candidate with missing artifacts leaves frozen identity; moved/deleted replay rejects before publication |
| Higher selected version remains the durable ceiling | 2–3 | Higher waiting candidate persists; a late lower wakeup is superseded even if the higher remote tag disappears |
| Operator guidance matches overlapping reconciliation | 4 | README and Definition of Done agree with the release runbook and workflow tests |

## Ordered slices

### 1. Represent a selected release before publication
Type: Structure
Status: planned
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

### 2. Freeze non-ready release identity
Type: Behavior
Status: planned
Proof: Public-command fixtures for absent/in-progress CI and failed/cancelled CI
persist the highest numeric candidate's tag/refOid/SHA before reporting
`waiting`/`blocked`. Replacing its lightweight ref, annotated ref object, or peeled
commit—or deleting it before a later CI wakeup—rejects with zero artifact or
production writes. A newer numeric candidate may replace the selected ceiling;
a late lower event cannot.

Behavior: Reconciliation first observes a qualifying tag whose CI is not ready →
freeze that release identity and version ceiling → return immediately without
holding a runner. Later wakeups may advance only that identity or a higher version.

### 3. Freeze ready identity before artifact admission
Type: Behavior
Status: planned
Proof: A ready exact-SHA CI candidate is durably selected before download actions.
Missing/expired artifacts leave that identity selected. After the tag is moved,
recreated, or deleted, a later exact-CI/artifact wakeup fails identity admission
with no production write; the unchanged tag can still recover through a newer
successful CI attempt. Existing publishing/succeeded retry and duplicate tests
remain green.

Behavior: A selected CI run is ready but its payload cannot be admitted → retain
the immutable release request → recover only the same tag/refOid/SHA rather than
treating a replacement ref as a fresh release.

### 4. Publish the overlapping-release policy consistently
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

## Verification and wrap-up

Each leaf targets one commit-sized outcome. Use the existing real-Git/fake-GitHub
and fake-GCS fixtures; no production tag or production payload write is permitted.
Run the focused application-release suite after each behavior and the standard
execute-plan refactor/format/check/commit/push wrap-up only when this plan is
explicitly executed.

```bash
CURSOR_DEV=true nix develop -c bash scripts/test/application-release.test
```
