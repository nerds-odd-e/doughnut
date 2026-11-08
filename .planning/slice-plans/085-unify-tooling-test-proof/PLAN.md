# See CLI tests and timed tooling checks in Intent

**Identity:** SEED-109#complete-test-inventory-and-script-proof
**Source:** [Refined story](../../seeds/SEED-109-complete-test-inventory-and-script-proof.md#complete-test-inventory-and-script-proof)

## Goal and scope

A Doughnut developer can browse the existing CLI Vitest suite and a single
Scripts and Infrastructure location. For a selected `donut CI` attempt,
the group and its files show the reported outcomes, cumulative recorded
durations, and any missing reports. Use `scripts/` as the physical common
parent. Move the three infrastructure test files to
`scripts/test/path-routing/`; they continue to drive the existing production
routing code and data in `infra/gcp/`. Preserve the current test selections and
verdicts. CLI Python discovery, CLI CI proof, and local proof collection are
outside scope.

## Current decisions

- Use the existing `scripts` entry path for discovery, fragment ownership, and
  the summary. Real paths underneath it work with the unchanged collector.
  Follow the [CI proof recipe](../../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md).
  Keep runner reporting and conversion with Doughnut.
- Keep one check per Bash file invocation, including work it delegates. Use
  Node case outcomes for suites that CI selects directly. Inventory also lists
  files that the selected attempt did not execute. Keep a runner's failed
  results when it supplies them. Keep absent durations untimed. When a Node
  parent reports a real failure after subtests, preserve the failure and avoid
  counting the children's execution time a second time.
- Keep existing workflow verdicts and checks. Malformed conversion fails its
  step under [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  A skipped or unavailable report leaves an expected part missing, and
  available parts still publish. Leave the production route owner under
  `infra/gcp/`, consistent with [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md).
- Before Take, obtain the deployed Open Dough selection contract and verify
  that one physical location lists and opens both Bash `.test` and Node
  `.test.mjs` sources. Open Dough owns that capability. It does not require
  multi-root locations or CI summary changes. The Doughnut registration slice
  consumes the delivered contract.

## Outside-in proof

| Promise | Slice | Observation |
| --- | --- | --- |
| CLI inventory includes both Vitest extensions | CLI inventory | Real Intent locations reader lists the tracked runner selection, including a TS and a TSX file |
| Routing tests share the physical parent and keep their coverage | Routing tests under scripts | `pnpm test:path-routing` passes all current cases from the new paths against the existing routing code and data |
| Bash file outcomes and elapsed test time survive a failure | Bash report, Bash CI proof | Ordinary runner emits a failed and a later passing file row with durations, exits nonzero, and both reach the summary |
| Node case outcomes and recorded durations survive a failure | Node report, Node CI proof | Real Node runner emits pass, failure, and skipped outcomes, preserves a file-load failure, and supplies timed rows where available |
| Group totals, file paths, and missing report agree across the summary and Intent | Node CI proof, selected CI attempt | Summarize timed fixtures with one missing part, then compare a real judged attempt's published artifact and Intent |
| One location lists and opens both Bash and Node sources | Shared discovery | Intent lists representative Bash, script Node, and relocated infrastructure Node files beneath one entry and opens each by its real path |

## Decisive premises

| Premise and consumer | Observation and result |
| --- | --- |
| CLI declaration selects the same tracked files as the runner patterns | At Doughnut `c85490c85c`, used deployed Open Dough v0.3.91 (`d2c6bf368562`) `performIntentLocationsRead` with that revision's Git tree. A `cli/tests` TypeScript location using `**/*.test.ts` and `**/*.test.tsx` returned all 67 matching files, 36 TS and 31 TSX. |
| The current routing test command can consume the relocation | In this worktree at `c85490c85c`, `CURSOR_DEV=true nix develop -c pnpm test:path-routing` passed 17 cases before and after a scratch copy to `scripts/test/path-routing/` with adjusted imports and package command. Reverted the scratch changes. |
| The existing collector assigns proof by physical path | Exercised `fragmentsFromOutcomes` at `c85490c85c` with Bash and Node timed rows. Entry `scripts` retained both rows beneath it and summed 20 ms. It did not attribute the row still under `infra/`. The relocation puts that row within the same tested prefix. |
| Node can expose real failure metadata and time without turning failures into successes | At `c85490c85c`, `CURSOR_DEV=true nix develop -c env NODE_OPTIONS=--test-reporter=<scratch reporter> node --test <fixture files>` returned structured file paths and durations, a skip marker, a parent assertion failure, a load failure, and exit 1. Scratch files were removed. Preserve the actual test command and verify its published report format during the Node report slice. |
| A single real parent suffices for mixed discovery | False on deployed Open Dough v0.3.91. The actual settings parser and selector admit one kind for a location and filter extensions before include globs. Shell selection excluded Bash `.test`; JavaScript selection admitted only the Node files. A same-path duplicate declaration does not create one group. The deployed mixed-source contract is outstanding. |

## Slices

### 1. CLI inventory

Type: Behavior
Status: planned
Proof: Use the pinned Intent locations reader against the candidate settings
and actual Git tree. Compare its CLI list to both include patterns from
`cli/vitest.config.ts`; spot-check `version.test.ts` and
`InteractiveCliApp.test.tsx`.

Behavior: A developer opening Intent for a revision with the CLI declaration
can browse the existing Vitest files under `cli/tests`.

### 2. Routing tests under scripts

Type: Structure
Status: planned
Proof: Run `CURSOR_DEV=true nix develop -c pnpm test:path-routing` after moving
all three files. Trace consumers of the old test paths and update the
package command and test-relative imports and fixture paths. Acceptance reading
checks that direct references to the old test paths have been updated.

Structure: Move only the three test files to `scripts/test/path-routing/`,
enabling shared discovery in the next slice. Existing Bash runner selection
and production paths keep their current owners.

### 3. Shared discovery

Type: Behavior
Status: planned
Proof: Read the candidate's locations through the deployed Intent read and
open `scripts/test/worktree_setup.sh.test`,
`scripts/ci/ci-proof-outcomes.test.mjs`, and
`scripts/test/path-routing/path-routing-validation.test.mjs` from a single
Scripts and Infrastructure group.

Behavior: Once the documented Open Dough contract is deployed, configure one
mixed-source location rooted at `scripts`, with the Bash and Node test
selection. The selected file has its real path and source kind.

### 4. Bash report

Type: Behavior
Status: planned
Proof: Exercise `bash scripts/test/run_all_script_tests.sh` through an isolated
fixture tree with a passing file, a failing file, and a file that runs after
failure. Check repository-relative outcomes, corresponding recorded times, and
exit 1. Confirm the ordinary invocation keeps its current output and verdict.

Behavior: When reporting is requested for CI, the normal Bash runner records
each completed file check, including failures, for the collector.

### 5. Bash CI proof

Type: Behavior
Status: planned
Proof: Extend `pnpm test:ci-proof` at the workflow and summary boundaries.
Run the repository's `fragment --by-file` and `summarize` with the real
Bash report shape. Check the published breakdown after a failed Bash check,
and the missing part when setup or the runner supplies no report.

Behavior: The judged workflow publishes the Bash runner's timed proof as
one part of the `scripts` entry and still judges the runner exit. Reuse the
fragment action and summary, with an explicit part identity for the
nonmatrix producer. At this stop the group carries Bash proof. Node CI proof
extends it with the Node producers and their expected parts.

### 6. Node report

Type: Behavior
Status: planned
Proof: Run the real `node --test` reporting path against pass, fail, skip,
nested-failure, and load-error cases, and run the focused routing command
with reporting enabled. Inspect file paths, statuses, recorded time, and
unchanged exit statuses. Exercise a mixed command with a file outside
`scripts/` and a test that launches a child process to check the report's
invocation boundary.

Behavior: The Node test runner's CI-only report exposes per-file outcomes and
available timing for the directly selected in-scope tests, including the
relocated routing suite. Use structured runner events as the source.
Attach the reporter to the selected invocations and preserve the ordinary
human-readable diagnostics.

### 7. Node CI proof

Type: Behavior
Status: planned
Proof: Extend `pnpm test:ci-proof` at the workflow and summary boundaries.
Run the repository's `fragment --by-file` and `summarize` with Bash proof,
an earlier successful Node part, a failed Node part with a report, and a
later skipped part. Compare counts, paths and durations. Check that artifact
uploads carry each part once.

Behavior: Extend the same attempt's `scripts` entry with reports from the
current `lint:all` commands that exercise files under `scripts/`. Give each
independently skipped command a unique expected part and fresh fragment
staging. Keep existing command order and verdicts. In a mixed command such as
`test:browser-worktree-isolation`, publish proof only for files under
`scripts/` and still judge all its tests. Commands with no files in scope
keep their CI role. Preserve the other proof entries and document the report
units and producers in `docs/ci-proof-summary.md`.

### 8. Selected CI attempt

Type: Behavior
Status: planned
Proof: Use the [artifact read](../../../docs/ci-proof-summary.md#check-a-run)
and Intent CI status on the same completed judged `donut CI` attempt for the
published candidate. Match attempt and revision, then compare counts and
cumulative times for the group and a representative file from each suite.
Reuse focused failure and missing-part proof where the live attempt supplies
only successful reports. A mismatch holds completion.

Behavior: A developer inspecting that attempt can reconcile the group and
file proof in Intent with the published summary.

## Concern evidence

- Slice-plan refinement separated Bash and Node publication so matrix and
  nonmatrix fragment ownership and skipped Node commands have independent
  proof. Hosted wait time is external to the final comparison.
- Shared discovery and the selected CI attempt depend on Open Dough's deployed
  mixed-source contract for a single physical path, including Bash `.test`
  admission. The current deployment rejects this combination. Until the
  capability is delivered and verified, preparation is not ready for Take.
