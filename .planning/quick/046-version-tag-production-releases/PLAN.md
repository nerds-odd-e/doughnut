# Release one chosen Donut version

Source: [SEED-013 Story 1](../../seeds/SEED-013-version-tag-production-releases.md#story-1).
Status: in progress on codex/version-tag-production-releases.
Readiness: ready for direct execution within the reduced single-release scope.
Continuation: [Story 2 plan](../048-release-recovery-and-ordering/PLAN.md).

## Goal, scope and stopping point

Ordinary main pushes run CI without publishing. A stable vMAJOR.MINOR.PATCH tag
selects a main commit and waits for successful exact-SHA CI before publishing
backend, frontend and bundled CLI. Main advancing does not change that selection.
Keep independent CLI releases and common CLI publishing logic. Invalid, unrelated,
two-component and prerelease tags do not deploy the application.

This story finishes with working tag releases enabled. Maintainers issue one
application release at a time, use increasing versions and immutable tags, and
recover via a newly tested correction/revert and next patch if necessary. Retain
one non-canceling application concurrency group. This story does not promise
safe automatic handling of overlapping versions, old reruns, moved tags or
duplicate release events; Story 2 removes that manual operating restriction.

Do not add a durable application-state object, version queue, event reconciliation
on every main CI completion, automatic CI retry or duplicate suppression here.
Missing artifacts and failed validation must still fail before production writes.
No staging, automatic version bump, application-visible version or schema rollback.

## Execution decisions

- Keep ci.yml's main-push trigger, workflow identity, checks and three artifact
  names. Use the selected SHA's latest applicable ci.yml main-push run/attempt;
  do not borrow another workflow, repository, commit or older green conclusion.
- Use one tag-triggered application workflow. It owns the selected tag/SHA and a
  bounded wait for matching CI to appear/finish. Re-query with the same external
  transport, pause between checks, and report timeout/failure explicitly. Choose
  a finite CI wait (initial implementation default: 60 minutes), with job timeout
  long enough for that wait plus the existing deployment. Tests use a fake clock,
  not real waiting. No workflow_run publication trigger remains at completion.
  Story 2 will replace this wait with tag/CI event reconciliation. This intentional
  simpler intermediate design saves building release state before it has a user.
- Select identity once from the tag event and peel annotated tags. Validate main
  ancestry with sufficient history and check that the ref has not changed within
  this invocation before writes. Cross-attempt durable identity enforcement is
  deferred. Reject forced-update/deletion inputs rather than treating them as
  ordinary new release requests.
- Keep current orchestration checkout separate from selected source checkout.
  Deployment payload, startup script, routing input and force-deployment token
  come from the selected source. Pass GITHUB_SHA explicitly to existing scripts.
  Keep the jar's 0.0.1-SNAPSHOT filename and all GCS destinations unchanged.
- Download all three artifacts from the admitted CI run into dedicated paths;
  fail before uploads if any cannot be obtained. Preserve existing download
  actions rather than building another archive transport.
- Extract only the existing frontend → CLI → conditional backend publication
  sequence. Reuse upload-cli-binary-to-gcs.sh in cli-release.yml too; preserve
  its tag-derived CLI_VERSION, destination and ACL. App CLI version remains
  whatever its validated CI bundle contains.
- Preserve URL-map rendering, hash-based backend rollout, health probes and
  failure messages. Record selected tag/SHA/run in normal workflow outputs and
  diagnostics. Story 2 may use successful publication-job evidence to establish
  its initial release state; do not build a speculative ledger now.
- The first released source must contain the new tag trigger: old committed YAML
  does not gain a trigger when main changes. After cutover, earlier main commits
  containing that trigger remain valid release candidates. The previous plan's
  historical CI-rerun workaround relied on workflow_run reconciliation and is
  therefore deferred with that mechanism to Story 2; document this transition
  limit instead of inventing an additional dispatch mechanism for Story 1.
- Follow [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) for routing
  ownership and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  for deliberate failure outcomes. No database/API changes are planned.

## Proof ownership

Drive the actual release entry points with temporary real Git repositories and
fake GitHub/GCS/MIG transport. Keep a focused node:test suite with a script-test
wrapper. Parsed YAML checks supplement command behavior; do not emulate GitHub's
scheduler. The clock/sleep boundary is replaceable for bounded-wait proof.

| Promise | Leaves | Observation |
|---|---|---|
| Ordinary pushes publish nothing; main CI retained | 1, 10 | Trigger/job graph and no production calls |
| Qualifying tag, main ancestry, exact SHA behind HEAD | 2 | Real tag fixtures and selected identity |
| Exact CI success; tag before/after CI; wait terminates on failure/timeout | 3–4 | Output and bounded fake-clock query trace |
| All payload artifacts from matching CI | 5 | Selected run requests/content; no writes if incomplete |
| Shared CLI publisher and unchanged independent CLI version/trigger | 6 | Existing uploader tests and caller wiring |
| Exact source routing/startup/force and conditional backend behavior | 7 | Distinct source/control fixture and existing deployment regressions |
| Honest selected-release failure context | 8 | Rendered notification payload without sending it |
| One active deployment; source/artifact/credential wiring | 9–10 | Parsed workflow and real entry-point replay |
| Working tag release and one-at-a-time/forward-correction runbook | 10 | End-to-end event fixture and documentation walkthrough |

## Ordered leaves

### 1. Stop publication on ordinary main pushes
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release.test.mjs` passed 2/2; parsed main CI, paused application dependency graph, independent CLI tag trigger. Independent refactor: no edits. Local observation only; default-branch effect awaits merge.
Proof: Keep main CI and independent CLI tag wiring; application publication is
unreachable from a normal main CI completion.

Behavior: Ordinary main CI completes → backend/frontend and CLI stay unchanged.
Temporarily disable the old deploy guard, preserving a valid workflow and any
already active deployment. Note the temporary pause; leaf 10 removes it.

### 2. Identify the exact tagged main commit
Type: Behavior
Status: planned
Proof: Entry-command fixtures resolve lightweight/annotated stable tags behind
HEAD and reject invalid shapes/off-main selections.

Behavior: A qualifying tag selects A → report A's peeled SHA without substituting
main HEAD. Add the smallest temporary-Git fixture, release command and script-test
wrapper. Expose selected identity to the workflow while publication is disabled.

### 3. Require successful CI for that exact commit
Type: Behavior
Status: planned
Proof: Transport fixtures admit only the matching repository/ci.yml/main-push
run for the selected SHA, with its latest applicable conclusion.

Behavior: A selected commit has completed CI → report ready with its run identity
only for success. Keep pagination and selection in one bounded lookup; propagate
transport errors. No state store or search for another release candidate.

### 4. Wait for the selected commit's unfinished CI
Type: Behavior
Status: planned
Proof: Tag-before-CI, already-green and newer-rerun fixtures converge to the
correct readiness outcome; fake time proves failure/timeout terminates waiting.

Behavior: Matching CI has not appeared or is unfinished → wait within the finite
budget → proceed only after its success. Reuse leaf 3's query; keep one explicit
owner of sleep, timeout and cancellation. Failure never falls back to older green
CI. This wait is removed by Story 2's final activation leaf.

### 5. Stage the validated payload before any publication
Type: Behavior
Status: planned
Proof: Selected-run artifact fixtures stage backend/frontend/CLI in dedicated
paths; missing or expired content produces no production calls.

Behavior: CI succeeds → obtain that run's complete payload or fail admission.
Keep current artifact names/download actions, source paths and jar filename.
Detailed regeneration/resumption support belongs to Story 2; fail loudly here.

### 6. Give existing publication one shared owner
Type: Structure
Status: planned
Proof: Extracted command retains frontend → CLI → backend operation order;
existing CLI upload tests and independent tag/version wiring remain green.

Internal change: Extract the three existing shell publication steps and replace
cli-release.yml's inline gsutil command with the existing CLI uploader. Preserve
all destinations, ACLs and inputs. This immediately enables leaf 7; introduce no
release-state or general orchestration framework.

### 7. Publish the selected source with current orchestration
Type: Behavior
Status: planned
Proof: Distinct source/control checkouts show selected SHA, routing/startup bytes
and force token in the external-operation trace. Existing backend skip/health
and frontend/CLI upload regressions stay green.

Behavior: Tagged source is A and orchestration is newer B → publish A's admitted
payload. Give leaf 6's command an explicit source root and artifact paths; preserve
current orchestration outside it and resolve the renderer's yaml dependency at
the selected source. Remote publication remains gated until leaf 10.

### 8. Identify the failed release in existing diagnostics
Type: Behavior
Status: planned
Proof: Early CI/artifact and late publication failure fixtures render selected
tag/SHA/CI context when known, without claiming main HEAD or sending Slack.

Behavior: Release processing fails → existing notification identifies the chosen
release and failure stage. Replace workflow_run-only context references; keep
failure coverage for both admission and publication jobs.

### 9. Wire the single-release path before activating it
Type: Structure
Status: planned
Proof: Parsed workflow connects tag identity, bounded CI wait, artifact run ID,
separate source/control checkouts and existing credentials before publication;
one non-canceling application concurrency group and the false gate remain.

Internal change: Connect leaves 2–8's proven commands/actions and remove obsolete
main-HEAD assumptions. This directly enables leaf 10. Keep publication disabled;
no new policy, state or diagnostic behavior is added in this wiring leaf.

### 10. Enable working tag releases with the interim operating rule
Type: Behavior
Status: planned
Proof: Tag-first and CI-first entry-point replays publish the selected payload;
ordinary main push has no publication trigger. Runbook examples match outcomes.

Behavior: Maintainer issues one qualifying tag → workflow waits for its successful
CI and releases that exact application version. Activate the tag trigger, remove
the old workflow_run publication trigger and temporary false guard together.
Update the existing GCP runbook plus concise overview links in README.md,
prod_env.md, prod-frontend-static-lb.md and definition_of_done.md: one application
release at a time, no old reruns/moved tags, next patch for a tested correction,
and no automatic schema rollback. Remove temporary-pause wording. Do not leave
activation for Story 2; this is the independently useful stopping point.

## Verification, sizing and wrap-up

All leaves target about five minutes including their focused proof and cleanup,
medium confidence (leaf 1 high). No advance sizing exceptions. At five minutes
scrutinize; at ten finer-decompose unless an observed focused-test runtime explains
it. This story has simpler integration work than Story 2's state transitions;
leaf count is not a time guarantee.

Proposed focused release suite and existing regressions:

```bash
CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release.test.mjs
CURSOR_DEV=true nix develop -c bash scripts/test/upload-cli-binary-to-gcs.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/upload-frontend-static-to-gcs.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/deploy-backend-jar-to-gcp-mig.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/apply-doughnut-app-service-url-map-wiring.test
```

Use named scenarios per leaf and existing YAML parsing for wiring. Before
activation run the focused release/deployment checks together once. Local replay
does not prove GitHub scheduling or production credentials; observe normal main
CI/no deployment during execution, and treat the operator's first real release
as platform confirmation. Do not create a release tag as a test.

Execution uses execute-plan: Jidoka → fresh post-change-refactor agent → API
regeneration only if needed → coordinator format:changed once → update this PLAN
→ commit/push → asynchronous CI observation. Preserve unrelated files. Finish
and clean up this plan after Story 1 without deleting Story 2 or its requirements;
reduce only the completed home story's detail. Execution authorized; main-only actions belong to the parent coordinator.

## Split provenance

Former leaves 1–5, 8–9, 15 and 17–18 supply this reduced path; the former general
CI reconciliation is replaced here by a bounded single-tag wait. Basic operator
guidance from former leaf 16 is kept in activation. All former durable state,
recovery and ordering promises remain in Story 2. No implementation evidence
existed to invalidate. The user explicitly requested the new two-plan split.

## Execution environment

- Worktree: `/Users/terryyin/.codex/worktrees/c158/doughnut`; coordinator task Story 1 / c158.
- CI observer attempted before first push via `./scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs stream --execution nerds-odd-e/doughnut main`. Nix daemon socket returned Operation not permitted before observer startup; cell 6 exited, no mailbox/PID/session created. CI observation unavailable; pendingCi: unobserved. Branch pushes do not trigger this repository's main-only CI.
- No production tags, settings, deployment calls, or main changes are authorized in this worktree. Post-merge scheduling/first-real-release observation belongs to the parent; local workflow activation remains branch work.
