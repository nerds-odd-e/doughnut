# Shard skill validation

Observed 2026-11-16 at `82efd34b6e0e348dbbb62ceb27454b854966277a`,
repository `nerds-odd-e/doughnut`. Skill authoring and evidence analysis took
approximately 9 active minutes. No timing assignment changed.

`./scripts/run.sh python3 /Users/terryyin/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/donut-rebalance-e2e-shards`
passed. This checks discoverable skill structure; the observations below check
its actual evidence-reading workflow.

## Refreshed evidence

GET the known controlling origin `http://127.0.0.1:4173` with matching Origin
header, using `/__authenticated-read?source=doughnut&intent=ci-status` and
`intent=ci-proof&run=<id>&attempt=1&revision=<full-sha>` on every proof read.
Status at 02:17 UTC named failed main run `43726030546/1`; it was insufficient
as a successful speed baseline. Explicit completed-attempt reads supplied:

- [43588251390/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43588251390),
  `2b95a250c33613472728ebd74f93685c39ba6872`, 2026-11-14, overall success.
- [43573637599/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43573637599),
  `f4b8dccbc9929088ebd462db0547eb169640aa11`, 2026-11-14, overall success.
- [43789649141/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43789649141),
  `c594b3d5a135462865708ce7d3aedddaf9a074aa`, 2026-11-16, overall failure;
  all six E2E jobs and their proof succeeded. The independent frontend fixture
  failure does not change E2E selection or workload. Use this only as qualified
  E2E evidence, not an overall-success baseline.

Connected GitHub `fetch` read each `actions/runs/<id>` record and its
`attempts/1/jobs?per_page=100` subresource. All three run records confirmed
final attempt 1, exact revision, completed push, branch and conclusion. Unlike
the narrowed jobs wrapper, this generic API read supplied timestamps and labels.
All six jobs in each attempt succeeded on separate `ubuntu-latest` runners;
revision workflow commands retain built-target Chrome and the same retry policy.
Exact hosted Chrome/OS image versions were not inspected.

## Six-group reconciliation

Read actual workflow/config files with `git show <sha>:<path>` and revision
feature inventories with `git ls-tree -r --name-only <sha> e2e_test/features`.
The historical workflows have the same six literal groups; current selector
has five identical groups and their exact remainder. Its discovery/tag owner
files are unchanged between `c594b3d5` and this validation revision.
Per-file case and duration sums equal each reported part, with no unmatched or
duplicate timed files and no failed/other/untimed cases in these E2E proofs.

Table cells give recorded Cucumber seconds / wrapper seconds / whole-job seconds.
Columns A/B are the two historical attempts above; C is the coverage attempt.

| Group | Historical selected/timed files; cases | Current selected/timed files; cases | A | B | C |
| --- | --- | --- | --- | --- | --- |
| 1 | 11/11; 40 | 11/11; 40 | 53.930 / 128 / 248 | 65.294 / 141 / 288 | 98.495 / 205 / 386 |
| 2 | 17/17; 97 | 17/17; 97 | 117.595 / 222 / 333 | 140.050 / 222 / 367 | 123.909 / 212 / 350 |
| 3 | 6/6; 42 | 6/6; 42 | 89.907 / 176 / 302 | 57.142 / 116 / 255 | 57.012 / 122 / 280 |
| 4 | 15/15; 87 | 15/15; 87 | 108.404 / 205 / 325 | 117.457 / 208 / 369 | 105.223 / 193 / 362 |
| 5 | 16/16; 43 | 16/16; 43 | 112.026 / 222 / 322 | 90.001 / 163 / 306 | 105.065 / 190 / 356 |
| 6 | 32/28; 70 | 33/29; 74 | 139.395 / 242 / 355 | 199.365 / 339 / 484 | 201.399 / 341 / 476 |

Shard 6 finished last at 2026-11-14T11:10:17Z (A),
2026-11-14T08:07:35Z (B) and 2026-11-16T02:09:34Z (C).
The four selected files without cases are CLI features whose actual contents
carry `@ignore`. They are filtered files, not unknown eligible zero-cost work.

## Incompatible-workload disposition

The two historical proofs have 93 timed files / 379 cases; C has 94 / 383.
The added learning-session file contributes four cases / 8.559 recorded seconds
in C; its historical timing is unknown, not zero. Between historical and current
revisions, frontend dependencies also changed: Marked 18.1.3 to 18.2.0,
Vite Vue plugin 6.0.10 to 6.0.11, PNPM 12.19.0 to 12.21.0 and test/Storybook
packages. Backend/application/E2E scenario sources did not change in that range.
Historical A/B changed guidance only and are mutually selection-compatible.

The skill workflow correctly produces a bounded comparison: A/B describe the
old workload; C demonstrates complete expanded coverage and its observed costs.
They do not establish a like-for-like elapsed saving. No move was proposed or
applied in this validation; initial candidate assessment belongs to slice 3.
Refresh any later completed normal CI attempt before that assessment. Do not
merge parts across attempts or treat unknown old learning-session cost as zero.

Raw dashboard downloads, throwaway reconciliation and consumer trace remain in
`/tmp/seed130-skill-validation/`; none is a repository cache. The skill links
the existing proof contract and selector rather than copying their ownership.
