# Complete and balance the six CI E2E shards

**Source:** [Keep all E2E specs covered and rebalance the six CI shards](../../seeds/SEED-130-ci-e2e-sharding.md#rebalance-ci-e2e-shards)
**Source review:** [Independent source commitments](source-review.md)
**Identity:** SEED-130#rebalance-ci-e2e-shards

## Execution context

Established Story Branch Mode start, owned linked checkout
`/Users/terryyin/git/doughnut/.worktrees/keep-all-e2e-specs-covered-and-rebalance-the-six`,
branch `codex/keep-all-e2e-specs-covered-and-rebalance-the-six`, agent
`stanly-chan`, publisher `dashboard-territory.local-doughnut`. Remote `origin`,
trunk `main`; initial published base/candidate
`bbbfa0e5cedc2a15cf80a94daef8cd2d4d856933`, starting revision
`cc15114c2c0193dfab4630f8eb36ee88ba558886`. No separate integration checkout
was supplied. Existing plan authority is retained; no overrun replanning override.

Checkout setup on 2026-11-16: `./scripts/run.sh bash scripts/worktree_setup.sh`
and `./scripts/run.sh pnpm test:gherkin-validation` passed with the locked
dependencies. The shared pre-commit hook is check-only `pnpm lint:changed`.
Workspace Git author remains the assigned agent.

CI: GitHub Actions, repository `nerds-odd-e/doughnut`, verified selector
`ci.yml` / `donut CI`; execution-branch observer coordinator `seed130-stanly`,
directory `/tmp/dough-ci-501/watch-xmPoc4`, PID `42830`, stream session `60226`,
yielded cell `g581:5`. The initial trunk claim has `pendingCi: unobserved`;
this observer owns subsequent execution-branch delivery only.

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

## Preparation evidence

Retained [premises, observations and timing baseline](preparation-evidence.md)
keep their original provenance and limitations.

## Ordered slices

### 1. Complete eligible coverage across six selections
Type: Behavior
Status: done
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
Status: done
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

## Slice 1 accepted proof

[Coverage and CI repair evidence](coverage-evidence.md) retains the local and
hosted observations, refactor acceptance and exact Markdown repair proof.

## Slice 2 accepted evidence

[Skill validation](skill-validation.md) retains the actual dashboard/GitHub
reads, identities, six-row reconciliation and incompatible-workload disposition.
Creator quick_validate and whitespace passed; no timing assignments changed.
Coordinator inspected skill routing, evidence handling and validation table.

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


## Story obligations

### G1. Hosted six-shard execution
Reported: slice 1 — "Local selector, tag and runner proof passes; real browser execution and all six hosted case parts require normal delivery CI."
Story clause: "Across the six selections, every eligible file belongs once."
Disposition: proved by slice 1: dashboard ci-proof 43789649141/1 at c594b3d5, six present case parts,383 passed cases,94 files including learning_session; local CLI fixture proves new directories

### G2. Exact hosted image versions
Reported: slice 2 — "Exact hosted Chrome/OS image versions were not inspected."
Story clause: "Name repository, runs, attempts, revisions, their actual selections and runner conditions."
Disposition: receiving slice 3
