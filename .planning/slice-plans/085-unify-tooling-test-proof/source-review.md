# Source review

Launch: fresh general-purpose Claude Code subagent (no conversation
inheritance), 2026-11-08, coordinator Rina-chan. Input: only the story text
of SEED-109#complete-test-inventory-and-script-proof (Goal, Scope, Key
examples, Discovery contract) verbatim from
`.planning/seeds/SEED-109-complete-test-inventory-and-script-proof.md`;
instructed to read no project files, change nothing, and return numbered
commitments, qualifications, and exclusions.

Returned exclusions: CLI CI proof, CLI Python inventory, and local proof
collection are out of scope; the `sources` capability belongs to Open Dough;
routing/deployment implementation and configuration stay under `infra/`.
Example file names are illustrations, not a complete list.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Goal: a Doughnut developer can find the CLI Vitest suite in Intent | slice 1 | observed: slice 1 reader probe (job tmp probe/probe.mts calling open-dough c1923d59f performIntentLocationsRead on working-tree settings + git ls-files tree): cli/tests location, no problem, 67 files (36 .test.ts, 31 .test.tsx) identical to git ls-files of both cli/vitest.config.ts include patterns |
| 2 | Goal: one Scripts and Infrastructure group shows, for a selected CI attempt: | slice 8 | |
| 2.1 | which Bash and Node.js checks that attempt ran | slice 8 | |
| 2.2 | how those checks ended | slice 8 | |
| 2.3 | how much test time they recorded | slice 8 | |
| 3 | Register the existing CLI Vitest suite in `.planning/open-dough.json` | slice 1 | observed: slice 1 reader probe (job tmp probe/probe.mts calling open-dough c1923d59f performIntentLocationsRead on working-tree settings + git ls-files tree): cli/tests location, no problem, 67 files (36 .test.ts, 31 .test.tsx) identical to git ls-files of both cli/vitest.config.ts include patterns |
| 3.1 | The registration covers TypeScript test files | slice 1 | observed: slice 1 reader probe (job tmp probe/probe.mts calling open-dough c1923d59f performIntentLocationsRead on working-tree settings + git ls-files tree): cli/tests location, no problem, 67 files (36 .test.ts, 31 .test.tsx) identical to git ls-files of both cli/vitest.config.ts include patterns |
| 3.2 | The registration covers TSX test files | slice 1 | observed: slice 1 reader probe (job tmp probe/probe.mts calling open-dough c1923d59f performIntentLocationsRead on working-tree settings + git ls-files tree): cli/tests location, no problem, 67 files (36 .test.ts, 31 .test.tsx) identical to git ls-files of both cli/vitest.config.ts include patterns |
| 4 | Place the infrastructure test files under the same physical parent as the scripts tests | slice 2 | observed: slice 2: git status shows the three routing tests renamed to scripts/test/path-routing/; pnpm test:path-routing 17/17 pass from there |
| 4.1 | Use `scripts/` as that shared parent | slice 2 | observed: slice 2: git status shows the three routing tests renamed to scripts/test/path-routing/; pnpm test:path-routing 17/17 pass from there |
| 4.2 | Put the three infrastructure tests under `scripts/test/path-routing/` | slice 2 | observed: slice 2: git status shows the three routing tests renamed to scripts/test/path-routing/; pnpm test:path-routing 17/17 pass from there |
| 4.3 | Keep the routing and deployment implementation under `infra/` | slice 2 | observed: slice 2: diff moves only the three *.test.mjs files; infra/gcp/path-routing/*.mjs, doughnut-routing.json, url-maps/renderDoughnutAppServiceUrlMap.mjs and infra/gcp/scripts stay, imported by the moved tests |
| 4.4 | Keep the routing and deployment configuration under `infra/` | slice 2 | observed: slice 2: diff moves only the three *.test.mjs files; infra/gcp/path-routing/*.mjs, doughnut-routing.json, url-maps/renderDoughnutAppServiceUrlMap.mjs and infra/gcp/scripts stay, imported by the moved tests |
| 5 | Discover the following in one Scripts and Infrastructure group: | slice 3 | observed: slice 3 probe3 (open-dough c1923d59f performIntentLocationsRead + pathSelectedByIntentLocations + sourceEntryOfRepositoryPath over candidate settings and tracked tree): one scripts location labelled Scripts and Infrastructure, 101 files (21 shell = git ls-files scripts/test/*.test, 80 javascript = scripts/**/*.test.mjs), no infra location |
| 5.1 | Bash `scripts/test/*.test` files | slice 3 | observed: slice 3 probe3 (open-dough c1923d59f performIntentLocationsRead + pathSelectedByIntentLocations + sourceEntryOfRepositoryPath over candidate settings and tracked tree): one scripts location labelled Scripts and Infrastructure, 101 files (21 shell = git ls-files scripts/test/*.test, 80 javascript = scripts/**/*.test.mjs), no infra location |
| 5.2 | Node.js `*.test.mjs` files beneath the shared parent | slice 3 | observed: slice 3 probe3 (open-dough c1923d59f performIntentLocationsRead + pathSelectedByIntentLocations + sourceEntryOfRepositoryPath over candidate settings and tracked tree): one scripts location labelled Scripts and Infrastructure, 101 files (21 shell = git ls-files scripts/test/*.test, 80 javascript = scripts/**/*.test.mjs), no infra location |
| 6 | Use the actual repository paths after the move: | slice 7 | |
| 6.1 | for browsing | slice 3 | observed: slice 3 probe3 (open-dough c1923d59f performIntentLocationsRead + pathSelectedByIntentLocations + sourceEntryOfRepositoryPath over candidate settings and tracked tree): one scripts location labelled Scripts and Infrastructure, 101 files (21 shell = git ls-files scripts/test/*.test, 80 javascript = scripts/**/*.test.mjs), no infra location; entries are post-move repository paths e.g. scripts/test/path-routing/path-routing-validation.test.mjs |
| 6.2 | for CI proof | slice 7 | |
| 7 | Use the existing attempt-specific CI proof summary as the source for the group's figures: | slice 7 | |
| 7.1 | counts at group level | slice 7 | |
| 7.2 | counts at file level | slice 7 | |
| 7.3 | cumulative recorded test execution time at group level | slice 7 | |
| 7.4 | cumulative recorded test execution time at file level | slice 7 | |
| 8 | Report the checks run by the existing CI jobs | slice 7 | |
| 9 | Count each Bash file invocation as one check, including the work it delegates | slice 4 | observed: slice 4 scripts/test/run_all_script_tests.sh.test: real runner over a fixture tree with CI_PROOF_BASH_REPORT reports one passed/failed row per .test file invocation (a passes, b fails, c passes after failure) with repo-relative path and elapsed seconds covering the whole bash call; exit 1 |
| 10 | Use per-case outcomes from the directly selected Node.js suites | slice 6 | observed: slice 6 scripts/ci/ci-proof-outcomes-node.test.mjs "a Node report gives one check per test case from the directly selected files": real node-test.mjs wrapper + node-test-report.mjs give per-case passed/failed/other rows on the selected file paths (imported cases on entryFile, load error one failed, parent failing after subtests untimed, child-run cases absent); test:path-routing with CI_PROOF_NODE_REPORT gives 17 timed rows |
| 11 | Include the failure results that are available | slice 7 | |
| 12 | Include the durations that are available | slice 7 | |
| 13 | If a producer report is missing, show the gap | slice 5 | observed: slice 5 scripts/ci/ci-proof-outcomes-bash.test.mjs "a missing Bash report fails its conversion and leaves the scripts part missing beside other proof": real fragment and summary action scripts give scripts part 1 missing |
| 13.1 | Show that gap alongside any proof that was reported | slice 5 | observed: slice 5 scripts/ci/ci-proof-outcomes-bash.test.mjs "a missing Bash report fails its conversion and leaves the scripts part missing beside other proof": real fragment and summary action scripts give scripts part 1 missing while frontend/tests stays present with its counts |
| 14 | Example 1: opening Intent lists both `cli/tests/version.test.ts` and `cli/tests/InteractiveCliApp.test.tsx` under the CLI location | slice 1 | observed: slice 1 reader probe (job tmp probe/probe.mts calling open-dough c1923d59f performIntentLocationsRead on working-tree settings + git ls-files tree): cli/tests location, no problem, 67 files (36 .test.ts, 31 .test.tsx) identical to git ls-files of both cli/vitest.config.ts include patterns; list includes cli/tests/version.test.ts and cli/tests/InteractiveCliApp.test.tsx |
| 15 | Example 2: opening Scripts and Infrastructure shows these files together: | slice 3 | observed: slice 3 probe3: listed, file-at admitted and read in full with hint: all three example files in the scripts group |
| 15.1 | `scripts/test/worktree_setup.sh.test` | slice 3 | observed: slice 3 probe3: listed, file-at admitted and read in full with hint shell: scripts/test/worktree_setup.sh.test |
| 15.2 | `scripts/ci/ci-proof-outcomes.test.mjs` | slice 3 | observed: slice 3 probe3: listed, file-at admitted and read in full with hint javascript: scripts/ci/ci-proof-outcomes.test.mjs |
| 15.3 | `scripts/test/path-routing/path-routing-validation.test.mjs` | slice 3 | observed: slice 3 probe3: listed, file-at admitted and read in full with hint javascript: scripts/test/path-routing/path-routing-validation.test.mjs |
| 16 | Example 2 continued: after a CI attempt reports those checks, Intent and that attempt's artifact agree on: | slice 8 | |
| 16.1 | the group count | slice 8 | |
| 16.2 | the file counts | slice 8 | |
| 16.3 | the sum of the recorded durations | slice 8 | |
| 17 | Example 3: when a Bash file or Node.js case fails and reports a duration, that outcome and duration count toward: | slice 7 | |
| 17.1 | its file | slice 7 | |
| 17.2 | the group | slice 7 | |
| 18 | Example 3 continued: when an expected report is unavailable: | slice 7 | |
| 18.1 | the attempt shows missing proof for that part | slice 7 | |
| 18.2 | the attempt keeps the results from the reports that are available | slice 7 | |
| 19 | Discovery contract: the common parent fits the current CI proof contract | slice 5 | observed: slice 5 ci-proof-outcomes-bash.test.mjs "a failed Bash check and its time count toward its file and the scripts entry": unchanged collector assigns scripts/test/*.test rows to the scripts entry; ci.yml expect adds scripts=1 in intent.locations order (ci-workflow.test.mjs) |
| 20 | Discovery contract: declare the `scripts` Intent location with Open Dough 0.3.92 `sources` entries (include + optional syntax) so it lists and opens Bash `.test` and Node.js `.test.mjs` files together | slice 3 | observed: slice 3 probe3 (open-dough c1923d59f performIntentLocationsRead + pathSelectedByIntentLocations + sourceEntryOfRepositoryPath over candidate settings and tracked tree): one scripts location labelled Scripts and Infrastructure, 101 files (21 shell = git ls-files scripts/test/*.test, 80 javascript = scripts/**/*.test.mjs), no infra location |
