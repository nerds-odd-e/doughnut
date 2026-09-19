# Test optimization candidates

This is Donut's candidate record for the public `dough-test-optimization` skill.
Candidates are hard-to-improve costs retained after a serious measured
experiment. Each entry remains eligible for later profiling until it is resolved.

Permanent E2E profile exclusion uses
`@skipOptimizationDueToKnownNecessarySlowness` on the narrowest Scenario or
Feature. That is a developer decision after review except where an explicit
`--resolve` pass is authorized by the public skill.

Profile E2E with:

```bash
--expose tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'
```

## Frontend profiling note

Frontend Vitest has no profile-exclusion mechanism, and it does not need one:
per-**test** duration is not the frontend cost model. Measured 2026-09-19 on the
339-file suite, the summed duration of every reported test file was ~7.6s against
a ~64.5s wall time, so ~88% of the suite is per-**file** cost (browser page setup
plus module-graph import). The measured marginal cost of one additional spec file
is ~190–280ms — an order of magnitude more than any individual candidate body
below ever was (13–34ms).

Consequences for future profiling passes:

- Do not record a frontend test as a candidate because its body is tens of
  milliseconds. That is inside the noise of the file it lives in.
- The frontend lever is **file count**: merge sibling specs that share a
  responsibility, a harness, and a setup block. Merging also deletes the
  duplicated `vi.mock` / `beforeEach` boilerplate, so it is a design improvement
  and a speedup at once.
- Rejected 2026-09-19: `--no-isolate` (reuse one browser page across files).
  216 of 339 files failed on shared global state and the run took 325s, 5×
  slower. Per-file isolation is required.

## Candidates

<!-- location — measured cost — unique protection — attempted alternatives/evidence — date — decision needed -->

_None. Every recorded candidate has been resolved._
