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
