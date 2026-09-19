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

- `e2e_test/features/learning_session/commissioned_learning_session.feature` — 15 scenarios (incl. two `Scenario Outline`s), 00:48 baseline, ~3.2s/test vs the ~2.5s/test 81-spec suite average — each scenario exercises a distinct commissioned-tracker scheduling or tutor-feedback-parsing path off a minimal shared Background (one notebook, three notes); no shared setup or duplicated proof found to consolidate further — 2026-09-18
- `e2e_test/features/cli/cli_notebook_existing_note_edits.feature` — 6 scenarios, 00:28-00:29, ~4.8s/test vs suite average — each scenario issues several real CLI subprocess + git operations (clone, commit, pull, rebase-continue, publish; up to 6 in one scenario), which is the CLI/git-sync protocol behavior under test; no hardcoded `cy.wait(ms)` sleeps found anywhere in `e2e_test/` — 2026-09-18
