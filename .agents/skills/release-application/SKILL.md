---
name: release-application
description: >-
  Release the Donut application from an authorized immutable stable tag, or
  inspect application deployment status. Use for application releases, not
  independent cli-* releases or gsd-ship PR delivery.
---

# Release Application

Use the [application release runbook](../../../docs/gcp/conditional-backend-deploy.md)
as the canonical procedure and evidence contract. Read it before acting; keep
command details and recovery mechanics there rather than reconstructing them in
this skill.

## Route the request

- An application release publishes the SPA, bundled CLI, routing, and any
  required backend rollout through the Application Release workflow. A `cli-*`
  release is independent. `gsd-ship` delivers a PR; it does not release the
  application.
- A status-only request is read-only. Inspect existing tags, workflow runs,
  publication jobs, and smoke evidence without creating a tag or rerunning a
  workflow.
- Preserve authorization already provided by the user. Do not ask again when
  the exact release identity and authorization are already clear. Before an
  immutable tag push, resolve any genuinely missing intended version, commit
  identity, or authority to publish.

## Release and verify

For a requested release, establish the intended unused, increasing stable
version and the exact tested commit on `main`. Confirm exact-SHA CI success and
the required artifacts for that commit, then push the immutable tag only when
authorized. CI completion does not start Application Release; if a tag was pushed
prematurely, an explicit release workflow rerun is needed after CI succeeds.
Follow the runbook for the commands and admission rules.

After the release request is admitted, inspect the selected tag and SHA, CI run
and attempt, the actual publication job, and the production smoke-check
evidence. Do not infer deployment from a tag, CI success, admission, or an
overall workflow result alone. `waiting`, `blocked`, failed, or skipped
publication is not deployed.

On failure, stop and report the concrete stage and evidence. Recovery is either
a retry of the same immutable identity or a tested forward correction under the
next patch version, as the runbook directs. Never move or delete the existing
tag, automatically retry, or automatically bump a version. Do not create a
GitHub Release; it is outside this workflow.

## Report

State whether the application is deployed; otherwise report the precise state,
including `waiting`, `blocked`, failed, or publication skipped. Include the tag,
exact SHA, selected CI run/attempt, Application Release run and publication-job result, and
production smoke result. Explicitly disclose evidence that could not be
verified and name the next decision or authorized recovery action rather than
masking uncertainty.
