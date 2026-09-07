# Observe release reconciliation after merge

Source: [SEED-013 Story 2](../../seeds/SEED-013-version-tag-production-releases.md#story-2).
Status: implementation complete on `codex/release-recovery-ordering`; parent owns
merge and the remaining default-branch observation.

## Goal and scope

Verify that GitHub installs the completed-`donut CI` wakeup only after the branch
is merged to the default branch, and that the first wakeup runs current release
reconciliation under the existing non-cancelling production concurrency group.
This observation must not create a release tag or change production settings,
IAM, Slack configuration, or application payloads.

Local proof already covers durable application state, bootstrap, immutable retry,
artifact recovery, duplicate no-op, moved-tag rejection, numeric pending-version
selection, downgrade protection, immediate CI outcomes, publication, independent
CLI behavior, and workflow contracts. Enduring operator behavior is documented in
`docs/gcp/conditional-backend-deploy.md`.

## Remaining parent-owned slice

### 1. Observe the installed CI-completion wakeup
Type: Behavior
Status: planned
Proof: After merge and push, identify the successful default-branch `donut CI`
run for the merge SHA and its resulting `donut deploy` `workflow_run` execution.
Confirm the deployed workflow comes from `main`, uses `deploy-production` with
`cancel-in-progress: false`, initializes or preserves application tracking, and
reconciles current tags without a polling wait. With no eligible tag, it must
perform no application publication. Record exact run IDs and conclusions.

Behavior: The Story 2 workflow reaches `main` and main CI completes → GitHub
wakes release reconciliation from the default-branch workflow → reconciliation
finishes without creating a test release or holding a runner for unfinished CI.

If the platform run fails, classify it by workflow run, attempt, SHA, job and step
before repair. Do not create a production tag to prove activation.
