# Complete and balance the six CI E2E shards

**Source:** [Keep all E2E specs covered and rebalance the six CI shards](../../seeds/SEED-130-ci-e2e-sharding.md#rebalance-ci-e2e-shards)
**Identity:** SEED-130#rebalance-ci-e2e-shards

## Goal and scope

Contributors receive complete eligible E2E feedback, and maintainers can repeat
an evidence-based rebalance of the six shards. Keep five explicit folder-pattern
groups plus their exact remainder, the existing runner exclusions and CI tags,
and six matrix/proof parts. Deliver a private Donut skill and use it for the
initial assessment. Apply only supported timing moves; incomplete or incompatible
evidence produces a named no-move result, with complete coverage retained.

No seventh job, automatic scheduling, scenario rewrites, fresh profiling run,
dashboard feature, shared Open Dough change, proof-schema change or guaranteed
numerical speedup. Normal delivery CI supplies hosted observations; preparation
authorizes none of this implementation.

## Design and current decisions

- One CI selector owns the five folder-pattern groups and derives the sixth
  by set subtraction. Use positive concrete paths with the existing runner's
  `--spec` interface; do not depend on unobserved negated CLI glob behavior.
  The workflow has six numbered slots whose count agrees with this selector.
  Replacing the current sixth group preserves the other five initial groups.
- Share file-discovery patterns and exclusions with Cypress's existing config.
  A small discovery module can serve `common.ts`, Gherkin validation and the
  selector, preserving the preprocessor's glob/basename-exclusion semantics.
  Leave scenario/tag eligibility with Cucumber, including fully filtered files.
  No copied list of eligible files, tag parser or change to isolated-spec admission.
- Invoke the selector from the workflow before the existing owned E2E runner;
  retain `SUT_RUNTIME_TARGET='{"built":true}'`, Chrome and proof publication.
  Keep matrix order/part identity stable and `fail-fast: false`.
- The project-local skill owns evidence interpretation and supported regrouping,
  not runtime selection or proof production. Use existing dashboard/GitHub reads
  and throwaway analysis outside the checkout. Do not add a persistent evidence
  store or general scheduling framework.
- Borrow heavy-first placement on identical machines as a proposal heuristic:
  sum comparable per-file times by folder, consider the largest costs first,
  then compare every candidate's maximum across all six. Account for variability,
  unobserved files and setup separately; estimates never establish elapsed gains.
- Follow [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  preserve the owned runner and disposable environment boundaries. Follow
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for visible
  selection failures and the explicit missing-evidence outcome. All eight
  indexed ADR statuses agree with their records; no conflict was found.
  `.planning/NORTH-STAR.md` requires no new topic for this reversible CI change.

### PFE and consumer trace

`common.ts` owns the spec patterns; `validateCucumberFeatures.mjs` already uses
the installed preprocessor's glob engine and basename exclusions. Modularize
that discovery seam rather than invent another eligibility inventory. The
validator's existing malformed/empty-feature and exclusion proof must survive.
`e2e-invocation-selection.mjs` → `e2e-cypress-process.mjs` already forwards
positive comma-separated paths, so retain that interface and its lifetime.
`ci-workflow.test.mjs` and `ci_proof_fragments/action.yml` consume matrix length
and order for six case parts; extend workflow proof at those boundaries.
`dough-test-optimization` and `docs/ci-proof-summary.md` explain retained proof
and timing limitations; reuse their formats, not their local-test optimization
mission. No existing Donut skill owns six-shard rebalancing.

Expected existing edits are below the 250-line limit: workflow 245 lines,
`common.ts` 204 and `ci-workflow.test.mjs` 178 at the preparation revision.
Keep cohesive modules below that limit; place new selector cases in their own
file under the existing `test:ci-proof` command so producer-part counts do not grow.

## Outside-in proof ownership

| Promise / example | Owning slice | Observable proof |
| --- | --- | --- |
| Every eligible file belongs exactly once, including `learning_session` and a newly added directory | 1 | Invoke the actual selector for every workflow slot against real checkout files and a temporary fixture tree; compare the union and multiplicities with shared discovery. Assert the current missed file and the new file are selected by the remainder. |
| Explicit groups retain ownership; file exclusions and CI tags still apply | 1 | CLI fixture uses files in named groups and snapshot/non-feature paths. Existing Gherkin validation cases stay green; run the installed preprocessor on CI-tagged fixtures to show eligible scenarios survive and excluded scenarios retain their treatment. |
| CI executes the computed selections through its owned wrapper, with six proof parts | 1 | Workflow contract invokes the selector and existing runner, keeps six slots and six expected case parts; positive-path runner tests pass. Normal hosted delivery observes all six parts. |
| Private Donut skill reads fresh evidence through dashboard or GitHub without repository cache | 2 | Inspect skill routing and run its read/analysis workflow against two real completed attempts, keeping temporary downloads outside the checkout; verify identity and missing/incompatible-evidence dispositions. |
| Initial balance compares all six groups and applies only supported moves | 3 | Invoke the delivered skill, reconcile actual revision selections against file proof, and retain a six-row current/candidate comparison or a specific no-move reason. Coverage proof stays green after any assignment edit. |
| Estimates and elapsed results are distinguished, with timing gaps visible | 3 | Record baseline/candidate run, attempt, revision, selection, case/file coverage, runner conditions, job and test-step time. Compare normal delivery CI where comparable; otherwise explicitly record the unproved speedup and preserve the supported no-move outcome. |

## Decisive premises and preparation observations

Observations below were made on 2026-11-16 in this owned worktree at
`3095fd3c6ac7c6489a874687449b5a76f915dfd5`. No scratch edits remain.

| Premise / consuming operation | Form and observed result | Remainder |
| --- | --- | --- |
| The current workflow omits an eligible spec | The literal inventory command below parsed actual YAML and expanded each group: 98 files, counts 11/17/6/15/16/32, no overlaps; `learning_session/commissioned_learning_session.feature` is unassigned. | None for the coverage symptom. |
| A complement can reach the existing selection prefix while preserving eligibility | `CURSOR_DEV=true nix develop -c node .seed130-selection-probe.mjs` used the installed preprocessor's `getSpecs` and `addCucumberPreprocessorPlugin`, actual file patterns/exclusions and CI tag expression, then `resolveSpecs(['--spec', paths.join(',')], root, false)`. Existing eligible count 94; adding one eligible new-folder file plus ignored/snapshot fixtures yielded 95 eligible, partition counts 11/17/6/15/16/30, every eligible file once, ignored/snapshot fixtures excluded and all 30 remainder paths forwarded. Temporary script/fixtures were deleted. | This observes selection, not browser execution. The probe supplies the resolved config selection fields without invoking application setup; CI's primary profile has no paired-worktree tag override. Hosted execution remains normal delivery proof. |
| Retained CI proof exposes usable per-file times with provenance | GET the controlling dashboard `http://127.0.0.1:4173/__authenticated-read?source=doughnut&intent=ci-proof&run=<id>&attempt=1&revision=<sha>` with same-origin header; extract only bounded aggregates. Successful runs below each have six present case parts, 379 passed cases, 93 timed file rows, zero failed/other/untimed. | The newly covered learning-session file is absent; never assign it zero cost. Refresh proof and comparability on skill invocation. |
| The dashboard and job evidence support distinct timing measures | Dashboard `intent=ci-status` names failed run `43726030546/1`, revision `3b200a8f007ebef50db58261c7dc37ac5c27ae63`; do not use it as a speed baseline. `CURSOR_DEV=true nix develop -c gh api 'repos/nerds-odd-e/doughnut/actions/workflows/ci.yml/runs?status=completed&per_page=12'` located the successful runs. `gh api 'repos/nerds-odd-e/doughnut/actions/runs/<id>/jobs?per_page=100'` extracted six job/runner/step timestamps. The GitHub connector job answer omitted timestamps, requiring that supplement. | Hosted post-change elapsed performance cannot be observed before implementation. Slice 3 owns that observation and honest no-move disposition. |
| The successful runs are selection-compatible at preparation | `git diff --name-only f4b8dccbc9929088ebd462db0547eb169640aa11 2b95a250c33613472728ebd74f93685c39ba6872` showed guidance-only changes. `git diff --name-only 2b95a250c33613472728ebd74f93685c39ba6872 HEAD -- .github/workflows/ci.yml e2e_test scripts/e2e-runner.mjs` was empty. Actual workflow groups/tags inspected. | Before any timing claim, also reconcile changes to backend/frontend/shared test setup and the delivered coverage expansion; do not compare changed workload as though unchanged. |

Literal inventory invocation:

```sh
CURSOR_DEV=true nix develop -c node --input-type=module -e 'import {readFileSync,globSync} from "node:fs"; import {parse} from "yaml"; const groups=parse(readFileSync(".github/workflows/ci.yml","utf8")).jobs["E2E-tests"].strategy.matrix.spec; const files=Array.from(globSync("e2e_test/features/**/*.feature")).sort(); const sets=groups.map(g=>new Set(g.trim().split(/\s+/).flatMap(p=>Array.from(globSync(p)).filter(f=>f.endsWith(".feature"))))); console.log(JSON.stringify({files:files.length,counts:sets.map(s=>s.size),gaps:files.filter(f=>!sets.some(s=>s.has(f))),duplicates:files.filter(f=>sets.filter(s=>s.has(f)).length>1)},null,2));'
```

The unchanged consumer baseline also passed all 16 cases at that revision:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/ci/ci-workflow.test.mjs e2e_test/config/validateCucumberFeatures.test.mjs scripts/e2e-runner-primary-selection.cases.mjs scripts/e2e-runner-cypress-arguments.cases.mjs
```

### Retained timing baseline (diagnostic input, not a promised saving)

Both successful attempts ran on 2026-11-14 with the same six selections. Use
run identities, not these temporary download paths, to obtain evidence again:

- [43588251390/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43588251390),
  revision `2b95a250c33613472728ebd74f93685c39ba6872`.
- [43573637599/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43573637599),
  revision `f4b8dccbc9929088ebd462db0547eb169640aa11`.

| Original group | Recorded step seconds (first / second run) | Whole job seconds (first / second run) |
| --- | --- | --- |
| 1 | 53.930 / 65.294 | 248 / 288 |
| 2 | 117.595 / 140.050 | 333 / 367 |
| 3 | 89.907 / 57.142 | 302 / 255 |
| 4 | 108.404 / 117.457 | 325 / 369 |
| 5 | 112.026 / 90.001 | 322 / 306 |
| 6 | 139.395 / 199.365 | 355 / 484 |

These are Cucumber-recorded durations, not the wrapper-step elapsed times.
For group 6 the wrapper step took 242 / 339 seconds. Queue/setup and browser
costs differ from summed scenario steps. The strongest doubt is that smaller
estimated sums might not improve the last finish; slice 3 must address that
with comparable measurements or a specific no-move report.

## Ordered slices

### 1. Complete eligible coverage across six selections
Type: Behavior
Status: planned
Proof: Selector CLI partition/exclusion cases, workflow contract, existing
Gherkin validation and positive-path runner cases; six hosted proof parts.

Behavior: An existing or newly added eligible spec is discovered → CI computes
five explicit selections and their complement → exactly one of six owned
runner invocations selects it, with existing exclusions and scenario filters.

Give groups/complement one selector owner. Share discovery with the existing
config and validator rather than importing application setup into the selector.
Use fixture files and the real CLI/lower collaborators, avoiding service mocks
that provide the selection being proved. Add the selector tests to
`test:ci-proof`, preserve its existing script proof part, and update the
workflow's maintenance comment. A current-checkout test and a new-directory
fixture must discriminate the original omission. Do not change test scenarios.
Expected active work: one 15-minute slice; command time excluded.

### 2. Repeat a trustworthy on-demand shard assessment
Type: Behavior
Status: planned
Proof: The private skill's actual evidence-reading workflow produces a
provenance-bearing six-group comparison or a bounded gap report.

Behavior: A maintainer requests rebalancing → a project-local Donut skill reads
recent completed evidence → it reconciles selection/coverage/timing, proposes
supported assignments and reports unavailable or incompatible evidence without
changing timing assignments when decisive inputs are missing.

Author `.agents/skills/donut-rebalance-e2e-shards/SKILL.md` using skill-creator
guidance; keep it outside shared Open Dough ownership. Link existing proof docs.
Explain dashboard preference, GitHub supplementation, final-attempt gaps,
unknown file costs, runner comparability, heavy-first candidate evaluation and
the distinction between recorded time, wrapper elapsed time and job finish.
Skill invocation does not grant new publication authority. Use temporary
external storage only, and validate its actual workflow against retained
complete and incomplete/incompatible evidence. No implementation framework is
needed for prose instructions. Expected active work: 15 minutes.

### 3. Apply the supported initial balance and assess its result
Type: Behavior
Status: planned
Proof: Delivered skill invocation, a six-row current/proposed workload comparison,
partition checks after any move, and comparable normal CI job observations or
an explicit evidence-based no-move result.

Behavior: The coverage selector and skill exist → invoke that skill on fresh
comparable completed proof → keep supported folder moves with coverage intact,
or leave timing assignments unchanged with the precise missing evidence.

Obtain a baseline before timing edits. Name attempts/revisions, actual workflow
selections, effective file/case coverage, unknown file costs and runner labels.
Compare candidates across all six groups using the same observed inputs; do
not fit a candidate to one noisy run. Normal delivery CI supplies the new
selection's job/step/last-finish evidence. A newly covered file changes the
workload, so qualify that comparison and do not claim a like-for-like speedup.
If normal delivery provides its timing, it can support the remaining assessment.
Retain only supported timing moves; undo ineffective/regressing timing edits
while keeping complete coverage and the useful skill. If evidence remains
insufficient, record the supported no-move outcome and its limitation without
calling it a speedup. Do not trigger a profiling or paid local E2E run.
Expected active analysis/edit work: 15 minutes; CI wait excluded. Reassess the
plan rather than continuing repeated noisy experiments past that bound.

## Verification and delivery

Run repo commands under `CURSOR_DEV=true nix develop -c` per `AGENTS.md`.
For slice 1: `pnpm test:ci-proof`, `pnpm test:gherkin-validation`, and
`pnpm test:browser-worktree-isolation` (the runner's affected consumer suite).
Add the selector cases to the first command. After a timing-only group edit,
rerun selector/workflow partition proof; reuse unchanged validator/runner proof.
Use normal hosted CI for the real six-job execution and elapsed-time result;
no full local application suite is warranted for file grouping and guidance.

At execution delivery, follow the repository's required fresh refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, agent commit/check-only
hook and push, then the owning execution workflow's CI and wrap-up duties.
No API regeneration is implicated. Preserve evidence provenance and limitations
in this plan during execution; commit no raw reports or observation cache.

## Plan review

Slice-plan refinement is not needed: three bounded Behavior leaves each own
one observable outcome and proof loop. Common design: one file-discovery rule,
one five-group/complement partition, and evidence-informed edits to that
partition. No Structure-only staging or speculative extension mechanism.
No remaining slice-specific concern: unknown timing costs are the story's
explicit no-move branch, not assumed zero or a promised speedup. Execution must
refresh time-sensitive CI evidence. No pre-Take human decision remains.
