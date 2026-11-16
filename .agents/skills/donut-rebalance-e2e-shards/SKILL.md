---
name: donut-rebalance-e2e-shards
description: Assess and rebalance Donut's six CI E2E shards from recent completed CI proof. Use for on-demand folder regrouping or a shard-balance assessment.
---

# Rebalance the six E2E shards

Produce a supported folder assignment or a specific no-move result. This is a
private Donut workflow. It does not grant publication authority or authorize a
profiling run, paid local E2E execution, or changes to scenarios or proof schema.

The selector in [e2e-shards.mjs](../../../scripts/ci/e2e-shards.mjs) owns five
explicit folder-pattern groups and derives their exact remainder as shard six.
The [workflow](../../../.github/workflows/ci.yml) owns six numbered matrix slots.
Preserve that partition, slot order, six case-proof parts, existing file
exclusions and Cucumber scenario/tag filters. Use the existing owned runner.

## Read fresh evidence

On every invocation, identify the repository, intended branch and current
revision, then read recent completed attempts. Prefer the controlling dashboard's
**Intent → CI status** when its origin is known and its evidence is sufficient.
Use the origin supplied by the active launch; do not guess a dashboard port.
Its authenticated reads use a matching `Origin` header:

- `/__authenticated-read?source=doughnut&intent=ci-status`
- `/__authenticated-read?source=doughnut&intent=ci-proof&run=<run-id>&attempt=<n>&revision=<full-sha>`

Check which branch and attempt the status actually names. Supplement a stale,
failed or insufficient selection with GitHub reads; a dashboard's freshness
label does not make its workload comparable. Prefer connected GitHub tools when
they expose the required fields. The API paths below also work with `gh api`
through this repository's tooling wrapper:

- `repos/nerds-odd-e/doughnut/actions/workflows/ci.yml/runs?status=completed&per_page=20`
- `repos/nerds-odd-e/doughnut/actions/runs/<run-id>/attempts/<n>/jobs?per_page=100`
- `repos/nerds-odd-e/doughnut/actions/runs/<run-id>/artifacts?per_page=100`

Read the run's actual final attempt. If dashboard proof is unavailable, download
the matching `ci-proof-summary-attempt-<n>` artifact and extract
`ci-proof-summary.json`. Keep downloads and throwaway analysis in temporary
storage outside the checkout. Use [CI proof summary](../../../docs/ci-proof-summary.md)
for the existing schema, units, artifact read commands and timing semantics.
Create no repository evidence cache, raw-report file or scheduling framework.

## Establish comparability before proposing moves

Name repository, run URL/id, attempt, full revision, date and overall conclusion.
Verify the summary's identity against that attempt. Inspect its own
`e2e_test/features` case evidence: all six parts, outcomes, untimed counts and
per-file durations. Never fill missing final-attempt parts from an earlier
attempt after a failed-job rerun. An expired artifact or unreadable evidence is
a named gap, not an empty or zero-duration result.

Recover the actual workflow selections, selector, discovery rules and CI tag
expression **at each evidence revision**, using read-only Git history or GitHub
files. Do not apply today's folder list to an old proof without reconciling it.
Map each observed file once to its historical group; reconcile per-file case
counts and recorded-duration sums with every part. Explain discovered files
without cases using the existing Cucumber filters; do not invent tag parsing.
List eligible files with missing or untimed cost as unknown, including newly
covered files. Missing timings never mean zero cost.

Inspect relevant application, dependency, shared setup and runner changes
between observations. Record runner labels/OS, browser, built/runtime target,
retry policy and setup conditions from revision configuration and job evidence.
Capture each E2E job's start/finish and wrapper-step start/finish. If a connector
omits timestamps, supplement with the attempt-specific jobs API. A run that
failed elsewhere may supply E2E-only evidence when all six E2E jobs and their
proof succeeded and the unrelated failure cannot affect their workload; state
that qualification. It is not an overall successful-run baseline.

Use multiple comparable observations to expose variability. Changed selections,
case counts, dependencies or runner conditions need an explicit comparability
judgment. A common-file diagnostic may remain useful across changed workloads;
it does not establish a like-for-like elapsed improvement.

## Compare all six groups

For each comparable observation, aggregate per-file recorded times by folder.
As a proposal heuristic, consider the largest folder costs first and place them
on lighter groups, retaining five explicit groups plus the exact remainder.
Evaluate each candidate across **all six** groups using the same observed file
costs; compare maxima, variability and unknown costs against the unchanged
assignment. Avoid fitting a move to one noisy attempt. No automatic runtime
scheduler or additional job is needed.

Present a six-row current/candidate table with file/case counts, recorded-time
estimates and unknowns. Keep Cucumber's summed final-attempt step durations,
wrapper elapsed seconds, whole-job elapsed seconds and the last E2E finish as
separate measures. Queue/setup/browser overhead is not a per-file cost. A lower
estimated maximum is a forecast, not observed elapsed speedup.

When decisive evidence is unavailable, incomplete, incompatible or leaves
unknown costs that prevent a supported decision, report **no timing move**,
name the affected attempts/files/conditions and the evidence needed next, and
leave timing assignments unchanged. Keep complete coverage independently useful.

## Apply and assess an authorized move

Edit only supported assignments in the selector's `explicitGroups`; keep the
sixth group derived. Run `./scripts/run.sh pnpm test:ci-proof` to prove the
partition and workflow agreement after an assignment edit. Ordinary execution
owns refactoring, formatting, commits and publication; this skill adds no push
or landing permission.

Use normal delivery CI for the post-change observation. Reconcile its exact
revision/attempt and workload again, then compare wrapper/job/last-finish times
with the accepted baseline. Retain a supported move; undo an ineffective or
regressing timing edit while preserving complete coverage. If the observation
cannot establish elapsed improvement, say so without a numerical speedup claim.
Retain the decision and concise provenance in the active plan or conversation,
following its existing lifecycle; leave raw evidence temporary.
